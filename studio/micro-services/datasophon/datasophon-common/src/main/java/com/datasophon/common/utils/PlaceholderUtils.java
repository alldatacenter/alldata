package com.datasophon.common.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderUtils {
    private static final Logger logger = LoggerFactory.getLogger(PlaceholderUtils.class);

    public static void main(String[] args) {
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("${apiHost}", "ddp1016");
        paramMap.put("${apiPort}", "8081");
//        String regex = "\\$\\{(\\w+)\\s*(([\\+\\-])\\s*(\\d+))?\\}";
        String regex= "\\$\\{(.*?)\\}"
        ;
//        String regex = "\\[.*?\\]";
        String replacePlaceholders = PlaceholderUtils.replacePlaceholders("[\n" +
                "    {\n" +
                "      \"name\": \"apiHost\",\n" +
                "      \"label\": \"DDH管理端地址\",\n" +
                "      \"description\": \"DDH管理端地址\",\n" +
                "      \"required\": true,\n" +
                "      \"type\": \"input\",\n" +
                "      \"value\": \"\",\n" +
                "      \"configurableInWizard\": true,\n" +
                "      \"hidden\": false,\n" +
                "      \"defaultValue\": \"${apiHost}:${apiPort}\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"apiPort\",\n" +
                "      \"label\": \"DDH管理端端口\",\n" +
                "      \"description\": \"DDH管理端端口\",\n" +
                "      \"required\": true,\n" +
                "      \"type\": \"input\",\n" +
                "      \"value\": \"\",\n" +
                "      \"configurableInWizard\": true,\n" +
                "      \"hidden\": false,\n" +
                "      \"defaultValue\": \"${apiPort}\"\n" +
                "    }\n" +
                "  ]", paramMap,regex);

        System.out.println(replacePlaceholders);
        List<String> newEquipmentNoList = PlaceholderUtils.getNewEquipmentNoList("001", "002");
        for (String s : newEquipmentNoList) {
            System.out.println(s);
        }
    }

    public static String replacePlaceholders(String value,
                                             Map<String, String> paramsMap,String regex) {

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        //自旋进行最小匹配，直到无法匹配
        while (matcher.find()) {
            String group = matcher.group();
            //替换匹配内容
//            logger.info("find match value {}",group);
            if (paramsMap.containsKey(group)) {
                value = value.replace(group, paramsMap.get(group));
            }
        }
        return value;
    }

    public static List<String> getMatchValue(String value) {
        String regex = "\\[.*?\\]";
        ArrayList<String> list = new ArrayList<>();

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        //自旋进行最小匹配，直到无法匹配
        while (matcher.find()) {
            String group = matcher.group();
            //替换匹配内容
            list.add(group);
        }
        return list;
    }

    public static List<String> getNewEquipmentNoList(String pre, String last) {
        int length = pre.length();
        ArrayList<String> list = new ArrayList<>();
        Integer start = Integer.parseInt(pre);
        Integer end = Integer.parseInt(last);
        int next = start;
        list.add(pre);
        while (next < end) {
            next = next + 1;
            String nextStr = String.format("%0"+length+"d", next);
            list.add(nextStr);
        }
        return list;
    }

}
