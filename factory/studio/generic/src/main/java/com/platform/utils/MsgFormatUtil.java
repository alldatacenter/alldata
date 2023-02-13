package com.platform.utils;

import cn.hutool.core.util.StrUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息模板格式化
 */
public class MsgFormatUtil {

    private static String REGEX = "(\\{([a-zA-Z]+)\\})";

    public static String TEMPALTE_NICKNAME = "nickname";
    public static String TEMPALTE_DATETIME = "datetime";
    public static String TEMPALTE_BUSINESS_NAME = "businessName";
    public static String TEMPALTE_BUSINESS_KEY = "businessKey";

    /**
     * 根据模板及参数获得内容
     * @param tempalte
     * @param parameters
     * @return
     */
    public static String getContent(String tempalte, Map<String, String> parameters) {
        if (StrUtil.isBlank(tempalte)) {
            tempalte = "业务名称:{businessName},发起人:{nickname},业务编号:{businessKey}";
        }
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(tempalte);
        StringBuffer stringBuffer = new StringBuffer();
        while (m.find()){
            String key = m.group(2);
            String value = null;
            if (parameters.containsKey(key)){
                value = parameters.get(key);
            }
            value = (value == null) ? "" : value;
            m.appendReplacement(stringBuffer, value);
        }
        m.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    public static void main(String[] args) {
        String tempalte = "{name}你好,今年{age}岁";
        Map<String,String> parameters = new HashMap<>();
        parameters.put("name", "chris");
        parameters.put("age", "22");
        System.out.println(getContent(tempalte, parameters));
    }
}
