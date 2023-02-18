package com.datasophon.common.utils;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.google.common.net.InetAddresses;

import java.util.*;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static com.datasophon.common.Constants.OSNAME_PROPERTIES;
import static com.datasophon.common.Constants.OSNAME_WINDOWS;
import static com.datasophon.common.Constants.WINDOWS_HOST_DIR;

/**
 * 读取hosts文件
 *
 * @author gaodayu
 */
public enum HostUtils {;

    private static String HOSTS_PATH = "/etc/hosts";

    private static final String ENDL = "\r*\n";

    public static final Pattern HOST_NAME_STR = Pattern.compile("[0-9a-zA-Z-.]{1,64}");

    public static boolean checkIP(String ipStr) {
        return InetAddresses.isInetAddress(ipStr);
    }

    private static void checkIPThrow(String ipStr, Map<String, String> ipHost) {
        if (!checkIP(ipStr)) {
            throw new RuntimeException("Invalid IP in file /etc/hosts, IP：" + ipStr);
        }
        if (ipHost.containsKey(ipStr)) {
            throw new RuntimeException("Duplicate ip in file /etc/hosts, IP：" + ipStr);
        }
    }

    private static void checkLine(int splitLength, String line) {
        if (splitLength < 2) {
            throw new RuntimeException("Wrong config in file /etc/hosts:" + line);
        }
    }

    public static boolean checkHostname(String hostname) {
        if (!HOST_NAME_STR.matcher(hostname).matches()) {
            return false;
        }
        return !hostname.startsWith("-") && !hostname.endsWith("-");
    }

    private static void validHostname(String hostname) {
        if (!checkHostname(hostname)) {
            throw new RuntimeException("Invalid hostname in file /etc/hosts, hostname：" + hostname);
        }
    }

    private static void checkHostnameThrow(String hostname, HashMap<String, String> hostIp) {
        validHostname(hostname);
        if (hostIp.containsKey(hostname)) {
            throw new RuntimeException("Duplicate hostname in file /etc/hosts, hostname:" + hostname);
        }
    }

    private static List<String> parse2List() {
        String osName = System.getProperty(OSNAME_PROPERTIES);
        if (osName.startsWith(OSNAME_WINDOWS)){
            HOSTS_PATH = WINDOWS_HOST_DIR + HOSTS_PATH;
        }
        if (!FileUtil.exist(HOSTS_PATH)) {
            throw new RuntimeException("File /etc/hosts not found：" + HOSTS_PATH);
        }

        List<String> lines = Arrays.stream(new FileReader(HOSTS_PATH).readString().split(ENDL))
                .filter(it -> !it.trim().matches("(^#.*)|(\\s*)"))
                .map(it -> it.replaceAll("#.*", "").trim().replaceAll("\\s+", "\t"))
                .collect(Collectors.toList());
        return lines;
    }

    public static void read() {
        List<String> ipHostNameList = parse2List();

        HashMap<String, String> ipHost = new HashMap<>();
        HashMap<String, String> hostIp = new HashMap<>();
        for (String line : ipHostNameList) {
            String[] split = line.split("\\s+");
            int splitLength = split.length;
            checkLine(splitLength, line);

            String ipStr = split[0];
            checkIPThrow(ipStr, ipHost);
            String hostname = split[splitLength - 1];
            checkHostnameThrow(hostname, hostIp);

            ipHost.put(ipStr, hostname);
            hostIp.put(hostname, ipStr);
        }
        CacheUtils.put(Constants.IP_HOST, ipHost);
        CacheUtils.put(Constants.HOST_IP, hostIp);
    }

    public static String findIp(String hostname) {
        validHostname(hostname);

        String ip = "";
        List<String> ipHostNameList = parse2List();
        for (String line : ipHostNameList) {
            String[] split = line.split("\\s+");
            int splitLength = split.length;
            checkLine(splitLength, line);

            if(Objects.equals(split[split.length - 1], hostname)) {
                ip = split[0];
                break;
            }
        }

        return ip;
    }

}