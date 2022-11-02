package com.alibaba.sreworks.warehouse.common.utils;

import com.mifmif.common.regex.Generex;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex {

    public static String generateDocumentByRegex(String regex) {
        regex = StringUtils.stripStart(regex, "^");
        regex = StringUtils.stripEnd(regex, "$");
        return new Generex(regex).random();
    }

    public static Boolean checkDocumentByRegex(String document, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(document);
        return matcher.find();
    }

    public static Boolean checkDocumentByPattern(String document, Pattern pattern) {
        Matcher matcher = pattern.matcher(document);
        return matcher.find();
    }

    public static Boolean checkAllDocumentByRegex(List<String> documentList, String regex) {
        for (String document : documentList) {
            if (!checkDocumentByRegex(document, regex)) {
                return false;
            }
        }
        return true;
    }

    public static Boolean checkAllDocumentByPattern(List<String> documentList, Pattern pattern) {
        for (String document : documentList) {
            if (!checkDocumentByPattern(document, pattern)) {
                return false;
            }
        }
        return true;
    }
}
