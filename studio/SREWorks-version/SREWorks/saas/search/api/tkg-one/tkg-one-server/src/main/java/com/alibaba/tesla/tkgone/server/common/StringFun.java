package com.alibaba.tesla.tkgone.server.common;

import com.github.wnameless.json.flattener.JsonFlattener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import redis.clients.jedis.Jedis;

import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
/**
 * @author yangjinghua
 */
@Slf4j
public class StringFun {

//    private static Pattern bigP = Pattern.compile("(etl[\\d\\w]+\\([^()]*\\))");
//    private static Pattern detailP = Pattern.compile("((etl[\\d\\w]+)\\(([^()]*)\\))");
    private static Pattern bigP = Pattern.compile("(etl[\\d\\w]+\\(.+?\\))");
    private static Pattern detailP = Pattern.compile("(etl[\\d\\w]+)\\((.+)\\)");
    private static String argsSplitSymbol = ",";
    private static String funNameStartString = "etl";

    static public String runWithOutException(String string) {
        try {
            return get(string);
        } catch (Exception e) {
            log.error("内部方法执行失败: ", e);
            return string;
        }
    }

    static public String run(String string) throws Exception {
        return get(string);
    }

    static public String get(String string) throws Exception {
        return get(string, 10);
    }

    static public String get(String string, int ttl) throws Exception {
        if (ttl < 0) {
            return string;
        }
        Matcher m = bigP.matcher(string);
        if (m.find()) {
            do {
                String partStr = m.group();
                String dstStr = runFun(partStr);
                string = string.replace(partStr, dstStr);
            } while (m.find());
            return get(string, ttl - 1);
        }
        return string;
    }

    static public String runFun(String string) throws Exception {
        Matcher m = detailP.matcher(string);
        if (!m.find()) {
            throw new Exception("不可能出现这个错误，如果出现说明代码逻辑有问题，请关注!!!");
        }
        String funName = m.group(1);
        String args = m.group(2);
        args = args.replaceAll("<<<<<<", "(").replaceAll(">>>>>>", ")");
        List<String> funArgs = Arrays.stream(args.split(argsSplitSymbol))
            .map(StringUtils::strip).collect(Collectors.toList());
        if (!funName.startsWith(funNameStartString)) {
            return string;
        }
        switch (funName) {
            case "etlJsonGet":
                return etlJsonGet(funArgs);
            case "etlNowCurrentTimeStampMilli":
                return String.valueOf(System.currentTimeMillis());
            case "etlDate2TimeStamp":
                return etlDate2TimeStamp(funArgs);
            case "etlDate2TimeStampMillion":
                return etlDate2TimeStampMillion(funArgs);
            case "etlTimestamp2Date":
                return etlTimestamp2Date(funArgs);
            case "etlExpression2Long":
                return ExpressionToLong.stringToRes(funArgs.get(0));
            case "etlSwitch":
                return etlSwitch(funArgs);
            case "etlGetPasswordHash":
                return etlGetPasswordHash(funArgs);
            case "etlStripStart":
                return StringUtils.stripStart(funArgs.get(0), funArgs.get(1));
            case "etlStripEnd":
                return StringUtils.stripEnd(funArgs.get(0), funArgs.get(1));
            case "etlLeftPad":
                return StringUtils.leftPad(funArgs.get(0), Integer.parseInt(funArgs.get(1)), funArgs.get(2));
            case "etlRightPad":
                return StringUtils.rightPad(funArgs.get(0), Integer.parseInt(funArgs.get(1)), funArgs.get(2));
            case "etlStrip":
                return StringUtils.strip(funArgs.get(0), funArgs.get(1));
            case "etlLower":
                return StringUtils.lowerCase(funArgs.get(0));
            case "etlReplace":
                return StringUtils.replace(funArgs.get(0), funArgs.get(1), funArgs.get(2));
//            case "etlRedisHget":
//                Jedis jedis = RedisHelper.getJedis(funArgs.get(0), Integer.parseInt(funArgs.get(1)), funArgs.get(2), 0);
//                String value = jedis.hget(funArgs.get(3), funArgs.get(4));
//                value = value == null ? "" : value;
//                jedis.close();
//                return value;
            default:
                return string;
        }
    }

    private static String etlJsonGet(List<String> funArgs) {
        String keyLabel = funArgs.remove(funArgs.size() - 1);
        String strObj = String.join(argsSplitSymbol, funArgs);

        Map<String, Object> flattenJson = JsonFlattener.flattenAsMap(strObj);

        String value = String.valueOf(flattenJson.getOrDefault(keyLabel, null));
        if ((keyLabel.equals("app_id") || keyLabel.equalsIgnoreCase("appid")) && value.startsWith("sreworks")) {
            value = value.replace("sreworks", "");
        }

        return value;
    }

    private static String etlDate2TimeStampMillion(List<String> funArgs) {
        if (funArgs.size() == 1) {
            return Tools.date2TimeStampMillion(funArgs.get(0));
        }
        return Tools.date2TimeStampMillion(funArgs.get(0), funArgs.get(1));
    }

    private static String etlDate2TimeStamp(List<String> funArgs) {
        if (funArgs.size() == 1) {
            return Tools.date2TimeStamp(funArgs.get(0));
        }
        return Tools.date2TimeStamp(funArgs.get(0), funArgs.get(1));
    }

    private static String etlTimestamp2Date(List<String> funArgs) {
        if (funArgs.size() == 1) {
            return Tools.timeStamp2Date(funArgs.get(0));
        }
        return Tools.timeStamp2Date(funArgs.get(0), funArgs.get(1));
    }

    // ${panel_title}, null: ,etlDefault: ${panel_title}
    private static String etlSwitch(List<String> funArgs) {
        String defaultString = "";
        String defaultKey = "etlDefault";
        String caseStringSplit = ":";
        String baseString = funArgs.get(0);
        for (int index=1; index<funArgs.size(); index++) {
            List<String> stringList = Arrays.asList(funArgs.get(index).split(caseStringSplit));
            if (stringList.get(0).equals(baseString)) {
                return StringUtils.join(stringList.subList(1, stringList.size()), caseStringSplit);
            }
            if (stringList.get(0).equals(defaultKey)) {
                defaultString = StringUtils.join(stringList.subList(1, stringList.size()), caseStringSplit);
            }
        }
        return defaultString;
    }

    private static String etlGetPasswordHash(List<String> funArgs) throws Exception {
        String userName = funArgs.get(0);
        String password = funArgs.get(1);
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        Date today = Calendar.getInstance().getTime();
        String todayStr = df.format(today);

        String rawHash = String.format("%s%s%s", userName, todayStr, password);
        byte[] bytesOfMessage = rawHash.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] theDigest = md.digest(bytesOfMessage);
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] str = new char[16 * 2];
        int k = 0;
        for (int i = 0; i < 16; i++) {
            byte byte0 = theDigest[i];
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(str);
    }

}
