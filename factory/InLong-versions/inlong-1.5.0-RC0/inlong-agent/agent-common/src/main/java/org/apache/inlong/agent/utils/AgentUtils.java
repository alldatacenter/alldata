/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.apache.inlong.agent.constant.AgentConstants.AGENT_ENABLE_OOM_EXIT;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_LOCAL_IP;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_LOCAL_UUID;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_LOCAL_UUID_OPEN;
import static org.apache.inlong.agent.constant.AgentConstants.CUSTOM_FIXED_IP;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_AGENT_LOCAL_UUID_OPEN;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_ENABLE_OOM_EXIT;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_LOCAL_HOST;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_LOCAL_IP;

/**
 * Agent utils
 */
public class AgentUtils {

    public static final String EQUAL = "=";
    public static final String M_VALUE = "m";
    public static final String ADDITION_SPLITTER = "&";
    public static final String BEIJING_TIME_ZONE = "GMT+8:00";
    public static final String HOUR_PATTERN = "yyyyMMddHH";
    public static final String DAY_PATTERN = "yyyyMMdd";
    public static final String DEFAULT_PATTERN = "yyyyMMddHHmm";
    public static final String DAY = "D";
    public static final String HOUR = "H";
    public static final String HOUR_LOW_CASE = "h";
    public static final String MINUTE = "m";
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentUtils.class);

    /**
     * Get MD5 of file.
     */
    public static String getFileMd5(File file) {
        try (InputStream is = Files.newInputStream(Paths.get(file.getAbsolutePath()))) {
            return DigestUtils.md5Hex(is);
        } catch (Exception ex) {
            LOGGER.warn("cannot get md5 of file: " + file, ex);
        }
        return "";
    }

    /**
     * Get current system time
     */
    public static long getCurrentTime() {
        return System.currentTimeMillis();
    }

    /**
     * Finally close resources
     *
     * @param resource resource which is closable.
     */
    public static void finallyClose(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception ex) {
                LOGGER.info("error while closing: " + resource, ex);
            }
        }
    }

    /**
     * Finally close resources.
     *
     * @param resource resource which is closable.
     */
    public static void finallyClose(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception ex) {
                LOGGER.info("error while closing: " + resource, ex);
            }
        }
    }

    /**
     * Get random int of [seed, seed * 2]
     */
    public static int getRandomBySeed(int seed) {
        return ThreadLocalRandom.current().nextInt(0, seed) + seed;
    }

    /**
     * Get local IP
     */
    public static String getLocalIp() {
        String ip = DEFAULT_LOCAL_IP;
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
        } catch (Exception ex) {
            LOGGER.error("error while get local ip", ex);
        }
        return ip;
    }

    /**
     * Get local host
     */
    public static String getLocalHost() {
        String host = DEFAULT_LOCAL_HOST;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            LOGGER.error("error while get local host", ex);
        }
        return host;
    }

    /**
     * Get uniq id with prefix and index.
     *
     * @return uniq id.
     */
    public static String getUniqId(String prefix, long index) {
        return getUniqId(prefix, "", index);
    }

    /**
     * Get uniq id with timestamp and index.
     *
     * @param id job id
     * @param index job index
     * @return uniq id
     */
    public static String getUniqId(String prefix, String id, long index) {
        long currentTime = System.currentTimeMillis() / 1000;
        return prefix + currentTime + "_" + id + "_" + index;
    }

    /**
     * Get job id, such as "job_1"
     */
    public static String getSingleJobId(String prefix, String id) {
        return prefix + id;
    }

    /**
     * Sleep millisecond
     */
    public static void silenceSleepInMs(long millisecond) {
        try {
            TimeUnit.MILLISECONDS.sleep(millisecond);
        } catch (Exception e) {
            LOGGER.warn("error in silence sleep: ", e);
        }
    }

    /**
     * Sleep seconds
     */
    public static void silenceSleepInSeconds(long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (Exception e) {
            LOGGER.warn("error in silence sleep: ", e);
        }
    }

    /**
     * formatter for current time
     */
    public static String formatCurrentTime(String formatter) {
        return formatCurrentTime(formatter, Locale.getDefault());
    }

    /**
     * Formatter for current time based on zone
     */
    public static String formatCurrentTime(String formatter, Locale locale) {
        ZonedDateTime zoned = ZonedDateTime.now();
        // TODO: locale seems not working
        return DateTimeFormatter.ofPattern(formatter).withLocale(locale).format(zoned);
    }

    /**
     * Formatter with time offset
     *
     * @param formatter formatter string
     * @param day day offset
     * @param hour hour offset
     * @param min min offset
     * @return current time with offset
     */
    public static String formatCurrentTimeWithOffset(String formatter, int day, int hour, int min) {
        ZonedDateTime zoned = ZonedDateTime.now().plusDays(day).plusHours(hour).plusMinutes(min);
        return DateTimeFormatter.ofPattern(formatter).withLocale(Locale.getDefault()).format(zoned);
    }

    public static String formatCurrentTimeWithoutOffset(String formatter) {
        ZonedDateTime zoned = ZonedDateTime.now().plusDays(0).plusHours(0).plusMinutes(0);
        return DateTimeFormatter.ofPattern(formatter).withLocale(Locale.getDefault()).format(zoned);
    }

    /**
     * Parse addition attr, the attributes must be sent in proxy sender
     */
    public static Pair<String, Map<String, String>> parseAddAttr(String additionStr) {
        Map<String, String> attr = new HashMap<>();
        String[] split = additionStr.split(ADDITION_SPLITTER);
        String mValue = "";
        for (String s : split) {
            if (!s.contains(EQUAL)) {
                continue;
            }
            String[] pairs = s.split(EQUAL);
            if (pairs[0].equalsIgnoreCase(M_VALUE)) {
                mValue = pairs[1];
                continue;
            }
            getAttrs(attr, s, pairs);
        }
        return Pair.of(mValue, attr);
    }

    /**
     * Get the attrs in pairs can be complicated in online env
     */
    private static void getAttrs(Map<String, String> attr, String s, String[] pairs) {
        // when addiction attr be like "m=10&__addcol1__worldid="
        if (s.endsWith(EQUAL) && pairs.length == 1) {
            attr.put(pairs[0], "");
        } else {
            attr.put(pairs[0], pairs[1]);
        }
    }

    /**
     * Get addition attributes in additionStr
     */
    public static Map<String, String> getAdditionAttr(String additionStr) {
        Pair<String, Map<String, String>> mValueAttrs = parseAddAttr(additionStr);
        return mValueAttrs.getRight();
    }

    /**
     * Get m value in additionStr
     */
    public static String getmValue(String addictiveAttr) {
        Pair<String, Map<String, String>> mValueAttrs = parseAddAttr(addictiveAttr);
        return mValueAttrs.getLeft();
    }

    /**
     * Check agent ip from manager
     */
    public static String fetchLocalIp() {
        if (StringUtils.isNoneBlank(AgentConfiguration.getAgentConf().get(CUSTOM_FIXED_IP, null))) {
            return AgentConfiguration.getAgentConf().get(CUSTOM_FIXED_IP);
        }
        return AgentConfiguration.getAgentConf().get(AGENT_LOCAL_IP, getLocalIp());
    }

    /**
     * Check agent uuid from manager
     */
    public static String fetchLocalUuid() {
        String uuid = "";
        if (!AgentConfiguration.getAgentConf().getBoolean(AGENT_LOCAL_UUID_OPEN, DEFAULT_AGENT_LOCAL_UUID_OPEN)) {
            return uuid;
        }
        try {
            String localUuid = AgentConfiguration.getAgentConf().get(AGENT_LOCAL_UUID);
            if (StringUtils.isNotEmpty(localUuid)) {
                uuid = localUuid;
                return uuid;
            }
            String result = ExcuteLinux.exeCmd("dmidecode | grep UUID");
            if (StringUtils.isNotEmpty(result)
                    && StringUtils.containsIgnoreCase(result, "UUID")) {
                uuid = result.split(":")[1].trim();
                return uuid;
            }
        } catch (Exception e) {
            LOGGER.error("fetch uuid error", e);
        }
        return uuid;
    }

    /**
     * Convert the time string to mill second.
     */
    public static long timeStrConvertToMillSec(String time, String cycleUnit) {
        long defaultTime = System.currentTimeMillis();
        if (time.isEmpty() || cycleUnit.isEmpty()) {
            return defaultTime;
        }
        String pattern = DEFAULT_PATTERN;
        switch (cycleUnit) {
            case DAY:
                pattern = DAY_PATTERN;
                time = time.substring(0, 8);
                break;
            case HOUR:
            case HOUR_LOW_CASE:
                pattern = HOUR_PATTERN;
                time = time.substring(0, 10);
                break;
            case MINUTE:
                break;
            default:
                LOGGER.error("cycle unit {} is illegal, please check!", cycleUnit);
                break;
        }
        return parseTimeToMillSec(time, pattern);
    }

    /**
     * Convert the time string to mill second
     */
    private static long parseTimeToMillSec(String time, String pattern) {
        try {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            df.setTimeZone(TimeZone.getTimeZone(BEIJING_TIME_ZONE));
            return df.parse(time).getTime();
        } catch (ParseException e) {
            LOGGER.error("convert time string {} to millSec error", time);
        }
        return System.currentTimeMillis();
    }

    /**
     * Create directory if the path not exists.
     *
     * @return the file after creation
     */
    public static File makeDirsIfNotExist(String childPath, String parentPath) {
        File finalPath = new File(parentPath, childPath);
        try {
            boolean result = finalPath.mkdirs();
            LOGGER.info("try to create local path {}, result is {}", finalPath, result);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return finalPath;
    }

    /**
     * Whether the config of exiting the program when OOM is enabled
     */
    public static boolean enableOOMExit() {
        return AgentConfiguration.getAgentConf().getBoolean(AGENT_ENABLE_OOM_EXIT, DEFAULT_ENABLE_OOM_EXIT);
    }
}
