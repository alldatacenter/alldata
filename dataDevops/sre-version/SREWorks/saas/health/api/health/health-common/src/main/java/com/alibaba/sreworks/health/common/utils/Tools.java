package com.alibaba.sreworks.health.common.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jialiang.tjl
 */
@Data
public final class Tools {

    public static void sleepMilli(long milliSecond) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliSecond);
        } catch (InterruptedException ignored) {
        }
    }

    public static void sleep(long second) {
        try {
            TimeUnit.SECONDS.sleep(second);
        } catch (InterruptedException ignored) {
        }
    }

    public static void sleepOneSecond() {
        sleep(1);
    }

    public static String getLocalIp() {
        String ip;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            ip = addr.getHostAddress();
        } catch (Exception ex) {
            ip = "";
        }
        return ip;
    }

    public static Boolean isNumber(String str) {
        try {
            new BigDecimal(str);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static List<String> combination(List<String> stringList) {
        if (stringList.size() <= 1) {
            return stringList;
        }
        List<String> retList = new ArrayList<>();
        String first = stringList.get(0);
        for (String word : combination(stringList.subList(1, stringList.size()))) {
            retList.add(first + ":" + word);
            retList.add(word);
        }
        retList.add(first);
        return retList;
    }

    public static String timeStamp2Date(String seconds, String format) {
        if (seconds == null || seconds.isEmpty() || "null".equals(seconds)) {
            return "";
        }
        if (format == null || format.isEmpty()) {
            format = "yyyy-MM-dd HH:mm:ss";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(Long.valueOf(seconds + "000")));
    }

    public static String timeStamp2Date(String seconds) {
        return timeStamp2Date(seconds, "yyyy-MM-dd HH:mm:ss");
    }

    public static String date2TimeStamp(String dataStr) {
        return date2TimeStamp(dataStr, "yyyy-MM-dd HH:mm:ss");
    }

    public static int date2TimeStampInt(String dataStr) {
        return Integer.parseInt(date2TimeStamp(dataStr));
    }

    public static String date2TimeStampMillion(String dateStr, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return String.valueOf(sdf.parse(dateStr).getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String date2TimeStampMillion(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            return String.valueOf(sdf.parse(dateStr).getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String date2TimeStamp(String dateStr, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return String.valueOf(sdf.parse(dateStr).getTime() / 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static List<List<JSONObject>> jsonArrayToListListObject(JSONArray jsonArray) {
        List<List<JSONObject>> jsonObjectListList = new ArrayList<>();
        for (JSONArray tmpArray : jsonArray.toJavaList(JSONArray.class)) {
            jsonObjectListList.add(tmpArray.toJavaList(JSONObject.class));
        }
        return jsonObjectListList;
    }

    public static void writeToFile(String filePath, String content) throws Exception {
        if (filePath.endsWith(File.separator)) {
            throw new Exception("目标文件不能为目录");
        }
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new Exception("创建目标文件所在目录失败！");
            }
        }

        if (file.exists()) {
            if (!file.delete()) {
                throw new Exception("文件已经存在，删除失败");
            }
        }
        if (!file.createNewFile()) {
            throw new Exception("文件创建失败");
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write(content);
        out.flush();
        out.close();
    }

    public static List<String> readLinesFromFile(String filePath) throws Exception {
        List<String> stringList = new ArrayList<>();
        String line;
        InputStream inputStream = new FileInputStream(filePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        line = reader.readLine();
        while (line != null) {
            stringList.add(line);
            line = reader.readLine();
        }
        reader.close();
        inputStream.close();
        return stringList;
    }

    public static String readFromFile(String filePath) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        InputStream inputStream = new FileInputStream(filePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        line = reader.readLine();
        while (line != null) {
            stringBuilder.append(line);
            stringBuilder.append("\n");
            line = reader.readLine();
        }
        reader.close();
        inputStream.close();
        return stringBuilder.toString();
    }

    public static JSONObject deepMerge(JSONObject source, JSONObject target) {
        for (String key : source.keySet()) {
            Object value = JSONObject.toJSON(source.get(key));
            if (!target.containsKey(key)) {
                target.put(key, value);
            } else {
                if (value instanceof JSONObject) {
                    JSONObject valueJson = (JSONObject) value;
                    deepMerge(valueJson, target.getJSONObject(key));
                } else {
                    target.put(key, value);
                }
            }
        }
        return target;
    }

    public static Long currentTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    public static String currentDataString() {
        return currentDateString("yyyy-MM-dd HH:mm:ss");
    }

    public static String currentDateString(String format) {
        return new SimpleDateFormat(format).format(System.currentTimeMillis());
    }

    public static String getErrorInfoFromException(Exception e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            sw.close();
            pw.close();
            return sw.toString();
        } catch (Exception e2) {
            return String.format("ErrorGetInfoFromException(%s): %s", e2.getMessage(), e.getMessage());
        }
    }

    public static Object richDoc(Object object) {
        final int maxDeep = 4;
        return richDoc(object, maxDeep);
    }

    public static Object richDoc(Object object, int deep) {
        if (0 == deep) {
            return poolValue(object);
        }
        object = JSONObject.toJSON(object);
        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            for (String key : jsonObject.keySet()) {
                Object obj = richDoc(jsonObject.get(key), deep - 1);
                if (null != obj) {
                    jsonObject.put(key, obj);
                }
            }
            return jsonObject;
        } else if (object instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) object;
            for (int index = 0; index < jsonArray.size(); index++) {
                Object obj = richDoc(jsonArray.get(index), deep - 1);
                if (null != obj) {
                    jsonArray.set(index, obj);
                }
            }
            return jsonArray;
        } else if (object instanceof String) {
            try {
                Object tmp = JSONObject.parse((String) object);
                if (tmp == null) {
                    return object;
                } else {
                    return richDoc(tmp, deep - 1);
                }
            } catch (Exception e) {
                return object;
            }
        } else if (object instanceof BigInteger) {
            return ((BigInteger) object).longValue();
        } else if (object instanceof Number) {
            return object;
        } else if (object instanceof Boolean) {
            return object;
        } else {
            if (object == null) {
                return null;
            }
            return JSONObject.toJSONString(object);
        }
    }

    public static JSONObject poolDoc(JSONObject jsonObject) {

        for (String key : jsonObject.keySet()) {

            Object value = JSONObject.toJSON(jsonObject.get(key));
            jsonObject.put(key, poolValue(value));
        }
        return jsonObject;
    }

    public static Object poolValue(Object value) {
        if (value instanceof String) {
            return value;
        } else if (value instanceof BigInteger) {
            return ((BigInteger) value).longValue();
        } else if (value instanceof Number) {
            return value;
        } else if (value instanceof Boolean) {
            return value;
        } else {
            return JSONObject.toJSONString(value);
        }
    }

    public static ThreadPoolExecutor createThreadPool(int maxPoolSize, String threadPoolName) {
        BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>();
        return createThreadPoolWithQueue(maxPoolSize, threadPoolName, blockingQueue);
    }

    public static ThreadPoolExecutor createThreadPoolWithQueue(int maxPoolSize, String threadPoolName,
            BlockingQueue<Runnable> blockingQueue) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(String.format("%s-%s", threadPoolName, maxPoolSize) + "-%d").build();
        return new ThreadPoolExecutor(maxPoolSize, maxPoolSize, 0L, TimeUnit.MILLISECONDS, blockingQueue, threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    public static String objectToString(Object content) {
        content = JSONObject.toJSON(content);
        if (content instanceof String) {
            return (String) content;
        } else {
            return JSONObject.toJSONString(content);
        }
    }

    public static Boolean propertiesMatch(JSONObject param, JSONObject data) {
        return propertiesMatch(param, data, null);
    }

    public static Boolean propertiesMatch(JSONObject param, JSONObject data, String connectRelation) {
        if (param == null) {
            param = new JSONObject();
        }
        if (data == null) {
            data = new JSONObject();
        }
        List<String> connectRelationList = Arrays.asList("AND", "OR", "NOT");
        connectRelation = connectRelationList.contains(connectRelation) ? connectRelation : "AND";
        switch (connectRelation) {
            case "AND":
                for (String key : param.keySet()) {
                    Boolean eachKeyMatch = connectRelationList.contains(key)
                            ? propertiesMatch(param.getJSONObject(key), data, key)
                            : param.getString(key).equals(data.getString(key));
                    if (!eachKeyMatch) {
                        return false;
                    }
                }
                return true;
            case "OR":
                for (String key : param.keySet()) {
                    Boolean eachKeyMatch = connectRelationList.contains(key)
                            ? propertiesMatch(param.getJSONObject(key), data, key)
                            : param.getString(key).equals(data.getString(key));
                    if (eachKeyMatch) {
                        return true;
                    }
                }
                return false;
            case "NOT":
                for (String key : param.keySet()) {
                    Boolean eachKeyMatch = connectRelationList.contains(key)
                            ? propertiesMatch(param.getJSONObject(key), data, key)
                            : param.getString(key).equals(data.getString(key));
                    if (eachKeyMatch) {
                        return false;
                    }
                }
                return true;
            default:
                return false;
        }
    }

    public static boolean isJsonEmpty(Object value) {
        if (value == null) {
            return true;
        }
        value = JSONObject.toJSON(value);
        boolean isValueAsArrayNull = value instanceof JSONArray && CollectionUtils.isEmpty((JSONArray) value);
        boolean isValueAsJsonNull = value instanceof JSONObject && CollectionUtils.isEmpty((JSONObject) value);
        return isValueAsArrayNull || isValueAsJsonNull;
    }

    public static String getMD5(String str) throws Exception {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            throw new Exception("MD5加密出现错误");
        }
    }

    public static int getWeek() {
        int englishWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (englishWeek == 1) {
            return 7;
        } else {
            return englishWeek - 1;
        }
    }

    public static Pattern p = Pattern.compile("\\s*(([^:]+):([^:]+))(\\s+\\S+:.*)");

    public static JSONObject stringToDict(String s) {
        JSONObject dict = new JSONObject();
        Matcher m = p.matcher(s);
        while (m.find()) {
            dict.put(m.group(2).trim(), m.group(3).trim());
            m = p.matcher(m.group(4));
        }
        return dict;
    }

}
