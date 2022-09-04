package com.alibaba.sreworks.teammanage.server.utils;

import org.apache.commons.lang3.StringUtils;

public class StarUtil {

    public static String replaceString2Star(String content) {

        if (StringUtils.isEmpty(content) || content.length() <= 4) {
            return "****";
        }
        int subLen = content.length() / 4 + 1;
        char[] cardChar = content.toCharArray();

        for (int j = subLen; j < content.length() - subLen; j++) {
            cardChar[j] = '*';
        }

        return new String(cardChar);

    }

}
