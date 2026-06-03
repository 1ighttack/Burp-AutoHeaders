package burp;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Config {
    public static List<String> HEADER_LIST = Arrays.asList(
            "X-Forwarded-For","X-Forwarded","Forwarded-For","Forwarded","X-Requested-With","X-Forwarded-Proto", "X-Forwarded-Host",
            "X-remote-IP","X-remote-addr","True-Client-IP","X-Client-IP","Client-IP","X-Real-IP",
            "Ali-CDN-Real-IP","Cdn-Src-Ip","Cdn-Real-Ip","CF-Connecting-IP","X-Cluster-Client-IP",
            "WL-Proxy-Client-IP", "Proxy-Client-IP","Fastly-Client-Ip","True-Client-Ip","X-Originating-IP",
            "X-Host","X-Custom-IP-Authorization","X-Api-Version"
    );

    public static boolean AUTOXFF_STAT = false;
    public static String AUTOXFF_KEY = "X-Forwarded-For";
    public static String AUTOXFF_VALUE = "$RandomIp$";

    public static boolean AUTO_HEADERS_STAT = false;
    public static String AUTO_HEADERS_IP = "$RandomIp$";
    public static final LinkedHashMap<String, Boolean> AUTO_HEADERS_MAP = new LinkedHashMap<>();
    static {
        AUTO_HEADERS_MAP.put("X-Forwarded-For", true);
        AUTO_HEADERS_MAP.put("X-Real-IP", true);
        AUTO_HEADERS_MAP.put("X-Forwarded-Proto", true);
        AUTO_HEADERS_MAP.put("X-Forwarded-Host", true);
        AUTO_HEADERS_MAP.put("Forwarded", true);
        AUTO_HEADERS_MAP.put("CF-Connecting-IP", true);
        AUTO_HEADERS_MAP.put("True-Client-IP", true);
        AUTO_HEADERS_MAP.put("X-Client-IP", true);
        AUTO_HEADERS_MAP.put("X-Requested-With", true);
        AUTO_HEADERS_MAP.put("X-Api-Version", true);
    }

    public static final LinkedHashMap<String, Boolean> CUSTOM_HEADERS = new LinkedHashMap<>();
}
