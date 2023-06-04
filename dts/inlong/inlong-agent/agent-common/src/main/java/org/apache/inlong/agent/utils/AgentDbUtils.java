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

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AgentDbUtils
 */
public class AgentDbUtils {

    private static final Pattern PATTERN =
            Pattern.compile("\\$\\{(((0x)|(0X)|o|O)??[0-9a-fA-F]+?) *, "
                    + "*(((0x)|(0X)|o|O)??[0-9a-fA-F]+?) *(, *[0-9]*?)??}");
    private static final Pattern OCT_PATTERN = Pattern.compile("^o[0-7]+?$");
    private static final Pattern DEC_PATTERN = Pattern.compile("^[0-9]+?$");
    private static final int HEX_MODE = 16;
    private static final int EIGHT_MODE = 8;
    private static final String HEX_PREFIX = "0x";
    private static final String O_PREFIX = "o";

    /**
     * Attempts to establish a connection to the database from the XML configurations. If failed,
     * try use alternative standby database.
     *
     * @return Database connection
     */
    public static Connection getConnectionFailover(String driverClassName,
            String connectionUrl,
            String userName,
            String password) throws Exception {

        int totalRetryTimes = 3;
        int timeInterval = 10;

        connectionUrl = StringUtils.trim(connectionUrl);
        connectionUrl = StringUtils.replace(connectionUrl, "\r", "");
        connectionUrl = StringUtils.replace(connectionUrl, "\n", "");

        /* TODO: try to decrypt password, if failed then use raw password */

        /*
         * NOTE: THIS MAY CAUSE DEADLOAK WHEN MULTIPLE THREADS CALLED AT THE SAME TIME
         * sun.security.jca.ProviderConfig.getProvider(ProviderConfig.java:188)
         */
        synchronized (AgentDbUtils.class) {
            DbUtils.loadDriver(driverClassName);
        }

        Connection conn = null;
        int retryTimes = 0;
        while (conn == null) {
            try {
                conn = DriverManager.getConnection(connectionUrl, userName, password);
            } catch (Exception e) {
                retryTimes++;
                if (retryTimes >= totalRetryTimes) {
                    throw new SQLException(
                            "Failed to connect database after retry " + retryTimes + " times.", e);
                }
                TimeUnit.SECONDS.sleep(timeInterval);
            }
        }

        conn.setAutoCommit(false);
        return conn;
    }

    private static String format(int num, boolean lengthEquals, int length, int mode) {
        String numStr;
        if (mode == HEX_MODE) {
            numStr = Integer.toHexString(num);
            /* sub hex head '0x' */
            length = length - 2;
        } else if (mode == EIGHT_MODE) {
            numStr = Integer.toOctalString(num);
            /* sub oct head 'o' */
            length = length - 1;
        } else {
            numStr = String.valueOf(num);
        }

        /* append string length for lengthEquals = true */
        if (lengthEquals) {
            if (numStr.length() < length) {
                StringBuilder numberFormatStr = new StringBuilder();
                for (int i = 0; i < length - numStr.length(); i++) {
                    numberFormatStr.append(0);
                }
                numberFormatStr.append(numStr);
                numStr = numberFormatStr.toString();
            }
        }
        return numStr;
    }

    private static int parseInt(String parseStr) {

        int parseValue = -1;

        if (parseStr.startsWith(HEX_PREFIX)) {
            parseStr = parseStr.substring(2).trim();
            parseValue = Integer.parseInt(parseStr, 16);
        } else if (parseStr.startsWith(O_PREFIX)) {
            parseStr = parseStr.substring(1).trim();
            parseValue = Integer.parseInt(parseStr, 8);
        } else {
            parseValue = Integer.parseInt(parseStr);
        }

        return parseValue;
    }

    /**
     * Transfer string pattern into a list of real string.
     * For example: ${1, 99} = 1, 2, 3, ... 98,
     * 99 <br> ${01, 99} = 01, 02, ... 98, 99 <br>
     * ${0x0,0xff} = 1, 2, ... fe, ff <br> ${0x00,0xff}
     * = 01, 02, ... fe, ff <br> ${O1,O10} = 1, 2,... 7, 10<br>
     * ${O01,O10} = 01, 02,... 07, 10<br>
     *
     * test_${0x00,0x12,5} = test_00, test_05, test_0a, test_0f<br>
     *
     * @param str source string
     * @return string list.
     */
    public static String[] replaceDynamicSeq(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        int index = 0;
        /* find need replace number string */
        Matcher matcher = PATTERN.matcher(str);
        ArrayList<String> startNum = new ArrayList<String>();
        ArrayList<String> endNum = new ArrayList<String>();
        ArrayList<Integer> modes = new ArrayList<Integer>();
        ArrayList<Integer> steps = new ArrayList<Integer>();
        while (matcher.find()) {
            String matchStr = matcher.group(0);
            matchStr = StringUtils.strip(matchStr, "${");
            matchStr = StringUtils.strip(matchStr, "}");
            String[] patterns = matchStr.split(",");
            String startStr = patterns[0].trim().toLowerCase();
            String endStr = patterns[1].trim().toLowerCase();
            int step = 1;
            if (patterns.length >= 3) {
                String stepStr = patterns[2].trim();
                if (stepStr.length() > 0) {
                    step = parseInt(stepStr);
                }
            }

            boolean bFound = false;
            int mode = -1;

            /* match hex string */
            if (startStr.startsWith("0x") && endStr.startsWith("0x")) {
                bFound = true;
                mode = 16;
            } else if (startStr.startsWith("o") && endStr.startsWith("o")) {
                /* match oct string */
                Matcher startMatch = OCT_PATTERN.matcher(startStr);
                Matcher endMatch = OCT_PATTERN.matcher(endStr);
                if (startMatch.find() && endMatch.find()) {
                    bFound = true;
                    mode = 8;
                }
            } else {
                /* match dec string */
                Matcher startMatch = DEC_PATTERN.matcher(startStr);
                Matcher endMatch = DEC_PATTERN.matcher(endStr);
                if (startMatch.find() && endMatch.find()) {
                    bFound = true;
                    mode = 10;
                }
            }

            /* if not match oct, dec, hex; not do anything */
            /* if matched, bFound = true */
            if (bFound) {
                startNum.add(startStr);
                endNum.add(endStr);
                modes.add(mode);
                steps.add(step);
                matcher.appendReplacement(sb, "\\${" + (index++) + "}");
            }
        }
        matcher.appendTail(sb);
        ArrayList<String>[] tempArray = formatStartNum(startNum, endNum, modes, steps, sb);
        return tempArray[startNum.size()].toArray(new String[0]);
    }

    private static ArrayList<String>[] formatStartNum(ArrayList<String> startNum,
            ArrayList<String> endNum,
            ArrayList<Integer> modes,
            ArrayList<Integer> steps,
            StringBuffer sb) {
        @SuppressWarnings("unchecked")
        ArrayList<String>[] tempArray = new ArrayList[startNum.size() + 1];
        tempArray[0] = new ArrayList<String>();
        tempArray[0].add(sb.toString());
        for (int index = 0; index < startNum.size(); index++) {
            String start = startNum.get(index);
            String end = endNum.get(index);
            int mode = modes.get(index);
            int step = steps.get(index);
            tempArray[index + 1] = new ArrayList<String>();
            boolean lengthEquals = start.length() == end.length();
            for (String currentPath : tempArray[index]) {
                for (int i = parseInt(start); i <= parseInt(end); i = i + step) {
                    tempArray[index + 1].add(currentPath.replaceAll(
                            "\\$\\{" + index + "}",
                            format(i, lengthEquals, end.length(), mode)));
                }
            }
        }
        return tempArray;
    }
}
