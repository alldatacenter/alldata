package com.alibaba.tesla.gateway.log.util;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class LogParserTimeUtils {
    private static final String REG = "(\\d+-\\d+-\\d+\\s+\\d+:\\d+:\\d+\\s+\\d+)";
    private static final Pattern PATTERN = Pattern.compile(REG);

    //2020-05-08 12:07:59 247
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss SSS");

    public static String parserTime(String content){
        Matcher matcher = PATTERN.matcher(content);
        if(matcher.find()){
            return matcher.group(1);
        }
        return null;
    }

    public static LocalDateTime parserTimeToDate(String content){
        Matcher matcher = PATTERN.matcher(content);
        String dateString = null;
        if(matcher.find()){
            dateString =  matcher.group(1);
        }
        if (StringUtils.isBlank(dateString)) {
            return null;
        }
        return LocalDateTime.parse(dateString,formatter );
    }
}
