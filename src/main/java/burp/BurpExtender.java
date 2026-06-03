package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BurpExtender implements BurpExtension, ProxyRequestHandler {
    private static final String PLUGIN_NAME = "AutoHeaders";
    private static final String VERSION = "2.1";
    private MontoyaApi api;
    private JPanel rootPanel;
    private final List<JCheckBox> checkBoxes = new ArrayList<>();
    private JLabel statusLabel;
    private JLabel ipLabel;
    private JButton toggleBtn;
    private Runnable refreshStatus;
    private Runnable refreshToggle;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName(PLUGIN_NAME);
        api.logging().logToOutput("[+] " + PLUGIN_NAME + " " + VERSION + " loaded.");
        api.proxy().registerRequestHandler(this);
        rootPanel = new JPanel(new BorderLayout());
        rebuildUI();
        api.userInterface().registerSuiteTab(PLUGIN_NAME, rootPanel);
    }

    // ==================== ProxyRequestHandler ====================

    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
        try {
            if (!Config.AUTO_HEADERS_STAT) {
                return ProxyRequestReceivedAction.continueWith(interceptedRequest);
            }

            byte[] originalBytes = interceptedRequest.toByteArray().getBytes();
            String raw = new String(originalBytes);
            int bodyIdx = raw.indexOf("\r\n\r\n");
            if (bodyIdx < 0) {
                return ProxyRequestReceivedAction.continueWith(interceptedRequest);
            }

            String ip = Config.AUTO_HEADERS_IP.equals("$RandomIp$") ? Utils.getRandomIp() : Config.AUTO_HEADERS_IP;

            // Collect headers to inject (overwrites existing ones)
            java.util.Set<String> overwriteKeys = new java.util.LinkedHashSet<>();
            StringBuilder extraHeaders = new StringBuilder();
            for (Map.Entry<String, Boolean> entry : Config.AUTO_HEADERS_MAP.entrySet()) {
                if (entry.getValue()) {
                    overwriteKeys.add(entry.getKey().toLowerCase());
                    extraHeaders.append(entry.getKey()).append(": ").append(ip).append("\r\n");
                }
            }
            for (Map.Entry<String, Boolean> entry : Config.CUSTOM_HEADERS.entrySet()) {
                if (entry.getValue()) {
                    overwriteKeys.add(entry.getKey().toLowerCase());
                    extraHeaders.append(entry.getKey()).append(": ").append(ip).append("\r\n");
                }
            }

            if (extraHeaders.length() == 0) {
                return ProxyRequestReceivedAction.continueWith(interceptedRequest);
            }

            // Remove existing headers that will be overwritten
            String headerBlock = raw.substring(0, bodyIdx);
            String[] lines = headerBlock.split("\r\n", -1);
            StringBuilder preserved = new StringBuilder(lines[0]);
            for (int i = 1; i < lines.length; i++) {
                int colon = lines[i].indexOf(':');
                if (colon > 0) {
                    String key = lines[i].substring(0, colon).trim().toLowerCase();
                    if (overwriteKeys.contains(key)) {
                        continue;
                    }
                }
                preserved.append("\r\n").append(lines[i]);
            }

            String modified = preserved.toString() + "\r\n" + extraHeaders + "\r\n" + raw.substring(bodyIdx + 4);
            HttpRequest newRequest = HttpRequest.httpRequest(interceptedRequest.httpService(), modified);
            return ProxyRequestReceivedAction.continueWith(newRequest);
        } catch (Exception e) {
            return ProxyRequestReceivedAction.continueWith(interceptedRequest);
        }
    }

    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest) {
        return ProxyRequestToBeSentAction.continueWith(interceptedRequest);
    }

    // ==================== UI ====================

    private void rebuildUI() {
        syncCheckBoxesToConfig();
        rootPanel.removeAll();

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // ---- Title ----
        JLabel titleLabel = new JLabel(PLUGIN_NAME + "  v" + VERSION);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(12));

        // ---- Status ----
        JPanel statusPanel = new JPanel(new GridBagLayout());
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)),
                        " Status ", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
                        statusPanel.getFont().deriveFont(Font.BOLD, 15f)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        statusLabel = new JLabel();
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 17f));
        ipLabel = new JLabel();
        ipLabel.setFont(ipLabel.getFont().deriveFont(15f));

        refreshStatus = () -> {
            boolean on = Config.AUTO_HEADERS_STAT;
            statusLabel.setText("Status: " + (on ? "ON" : "OFF"));
            statusLabel.setForeground(on ? new Color(0, 160, 0) : Color.RED);
            String mode = Config.AUTO_HEADERS_IP.equals("$RandomIp$") ? "Random IP (per request)" : Config.AUTO_HEADERS_IP;
            ipLabel.setText("Value: " + mode);
        };
        refreshStatus.run();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.insets = new Insets(2, 0, 2, 0);
        gbc.gridy = 0; statusPanel.add(statusLabel, gbc);
        gbc.gridy = 1; statusPanel.add(ipLabel, gbc);

        content.add(statusPanel);
        content.add(Box.createVerticalStrut(12));

        // ---- Headers ----
        JPanel headerSection = new JPanel();
        headerSection.setLayout(new BoxLayout(headerSection, BoxLayout.Y_AXIS));
        headerSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)),
                        " Headers ", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
                        headerSection.getFont().deriveFont(Font.BOLD, 15f)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        headerSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        checkBoxes.clear();

        // Preset
        JLabel presetLabel = new JLabel("Preset Headers");
        presetLabel.setFont(presetLabel.getFont().deriveFont(Font.BOLD, 16f));
        presetLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerSection.add(presetLabel);
        headerSection.add(Box.createVerticalStrut(4));

        for (Map.Entry<String, Boolean> entry : Config.AUTO_HEADERS_MAP.entrySet()) {
            String key = entry.getKey();
            JCheckBox cb = new JCheckBox(key, entry.getValue());
            cb.setFont(cb.getFont().deriveFont(Font.BOLD, 18f));
            cb.setAlignmentX(Component.LEFT_ALIGNMENT);
            cb.addItemListener(e -> Config.AUTO_HEADERS_MAP.put(key, cb.isSelected()));
            headerSection.add(cb);
            checkBoxes.add(cb);
        }

        headerSection.add(Box.createVerticalStrut(12));

        // Custom
        JLabel customLabel = new JLabel("Custom Headers");
        customLabel.setFont(customLabel.getFont().deriveFont(Font.BOLD, 16f));
        customLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerSection.add(customLabel);
        headerSection.add(Box.createVerticalStrut(4));

        for (String key : Config.CUSTOM_HEADERS.keySet()) {
            JCheckBox cb = new JCheckBox(key, Config.CUSTOM_HEADERS.get(key));
            cb.setFont(cb.getFont().deriveFont(Font.BOLD, 18f));
            cb.setAlignmentX(Component.LEFT_ALIGNMENT);
            String k = key;
            cb.addItemListener(e -> Config.CUSTOM_HEADERS.put(k, cb.isSelected()));
            headerSection.add(cb);
            checkBoxes.add(cb);
        }

        JScrollPane scrollPane = new JScrollPane(headerSection);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scrollPane);
        content.add(Box.createVerticalStrut(12));

        // ---- Buttons ----
        JPanel btnPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        btnPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)),
                        " Controls ", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
                        btnPanel.getFont().deriveFont(Font.BOLD, 15f)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        toggleBtn = new JButton();
        JButton randomBtn = new JButton("Random IP");
        JButton customBtn = new JButton("Custom Value");
        JButton selectAllBtn = new JButton("Select All");
        JButton deselectAllBtn = new JButton("Deselect All");
        JButton addHeaderBtn = new JButton("Add Header");
        JButton verifyBtn = new JButton("Verify");

        Font btnFont = new JButton().getFont().deriveFont(Font.BOLD, 15f);
        toggleBtn.setFont(btnFont);
        randomBtn.setFont(btnFont);
        customBtn.setFont(btnFont);
        selectAllBtn.setFont(btnFont);
        deselectAllBtn.setFont(btnFont);
        addHeaderBtn.setFont(btnFont);
        verifyBtn.setFont(btnFont);

        refreshToggle = () -> toggleBtn.setText(Config.AUTO_HEADERS_STAT ? "Turn OFF" : "Turn ON");
        refreshToggle.run();

        toggleBtn.addActionListener(e -> {
            Config.AUTO_HEADERS_STAT = !Config.AUTO_HEADERS_STAT;
            api.logging().logToOutput("[+] AutoHeaders " + (Config.AUTO_HEADERS_STAT ? "ON" : "OFF"));
            refreshStatus.run();
            refreshToggle.run();
        });

        randomBtn.addActionListener(e -> {
            Config.AUTO_HEADERS_IP = "$RandomIp$";
            Config.AUTO_HEADERS_STAT = true;
            api.logging().logToOutput("[+] AutoHeaders ON | IP: Random");
            refreshStatus.run();
            refreshToggle.run();
        });

        customBtn.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog(rootPanel, "Input value:",
                    Config.AUTO_HEADERS_IP.equals("$RandomIp$") ? "" : Config.AUTO_HEADERS_IP);
            if (ip != null && !ip.trim().isEmpty()) {
                Config.AUTO_HEADERS_IP = ip.trim();
                Config.AUTO_HEADERS_STAT = true;
                api.logging().logToOutput("[+] AutoHeaders ON | Value: " + Config.AUTO_HEADERS_IP);
                refreshStatus.run();
                refreshToggle.run();
            }
        });

        verifyBtn.addActionListener(e -> {
            new Thread(() -> {
                try {
                    String ip = Config.AUTO_HEADERS_IP.equals("$RandomIp$") ? Utils.getRandomIp() : Config.AUTO_HEADERS_IP;
                    HttpService service = HttpService.httpService("httpbin.org", 443, true);
                    String raw = "GET /headers HTTP/1.1\r\nHost: httpbin.org\r\nConnection: close\r\n\r\n";
                    StringBuilder extra = new StringBuilder();
                    for (Map.Entry<String, Boolean> entry : Config.AUTO_HEADERS_MAP.entrySet()) {
                        if (entry.getValue()) {
                            extra.append(entry.getKey()).append(": ").append(ip).append("\r\n");
                        }
                    }
                    for (Map.Entry<String, Boolean> entry : Config.CUSTOM_HEADERS.entrySet()) {
                        if (entry.getValue()) {
                            extra.append(entry.getKey()).append(": ").append(ip).append("\r\n");
                        }
                    }
                    String modified = raw.replace("\r\n\r\n", "\r\n" + extra + "\r\n");
                    HttpRequest testReq = HttpRequest.httpRequest(service, modified);
                    var resp = api.http().sendRequest(testReq);
                    api.logging().logToOutput("[Verify] IP: " + ip);
                    api.logging().logToOutput("[Verify] Response:\n" + resp.response().toString());
                } catch (Exception ex) {
                    api.logging().logToError("[Verify] Error: " + ex.getMessage());
                }
            }).start();
        });

        addHeaderBtn.addActionListener(e -> {
            syncCheckBoxesToConfig();
            String name = JOptionPane.showInputDialog(rootPanel, "Header name:");
            if (name == null || name.trim().isEmpty()) return;
            name = name.trim();
            if (Config.AUTO_HEADERS_MAP.containsKey(name)) {
                JOptionPane.showMessageDialog(rootPanel, "Already in Preset Headers.", "Duplicate", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (Config.CUSTOM_HEADERS.containsKey(name)) {
                JOptionPane.showMessageDialog(rootPanel, "Already in Custom Headers.", "Duplicate", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Config.CUSTOM_HEADERS.put(name, true);
            rebuildUI();
            api.logging().logToOutput("[+] Added custom header: " + name);
        });

        selectAllBtn.addActionListener(e -> checkBoxes.forEach(cb -> cb.setSelected(true)));
        deselectAllBtn.addActionListener(e -> checkBoxes.forEach(cb -> cb.setSelected(false)));

        btnPanel.add(toggleBtn);
        btnPanel.add(randomBtn);
        btnPanel.add(customBtn);
        btnPanel.add(selectAllBtn);
        btnPanel.add(deselectAllBtn);
        btnPanel.add(addHeaderBtn);
        btnPanel.add(verifyBtn);

        content.add(btnPanel);

        rootPanel.setLayout(new BorderLayout());
        rootPanel.add(content, BorderLayout.CENTER);
        rootPanel.revalidate();
        rootPanel.repaint();
    }

    private void syncCheckBoxesToConfig() {
        int idx = 0;
        for (String key : Config.AUTO_HEADERS_MAP.keySet()) {
            if (idx < checkBoxes.size()) {
                Config.AUTO_HEADERS_MAP.put(key, checkBoxes.get(idx++).isSelected());
            }
        }
        for (String key : new ArrayList<>(Config.CUSTOM_HEADERS.keySet())) {
            if (idx < checkBoxes.size()) {
                Config.CUSTOM_HEADERS.put(key, checkBoxes.get(idx++).isSelected());
            }
        }
    }
}
