package com.platform.devops.autogen;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wulinhao on 2020/04/13.
 */
public class TmpUtils {
    public static String upperFist(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static String underlineToUpperFirst(String s) {
        String[] temp = s.split("_");
        for (int i = 0; i < temp.length; i++) {
            temp[i] = upperFist(temp[i]);
        }
        return StringUtils.join(temp, "");
    }

    public static String underlineToLowerUpperFirst(String s) {
        String[] temp = s.split("_");
        for (int i = 0; i < temp.length; i++) {
            if (i != 0) temp[i] = upperFist(temp[i]);
        }
        return StringUtils.join(temp, "");
    }

    static String findMatch(String pattern, String want) {
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(want);
        if (m.find()) {
            return m.group(0);
        } else return null;
    }

    public static class SplitLines {
        public List<String> headPart = new ArrayList<>();
        public List<String> lastPart = new ArrayList<>();
    }

    public static SplitLines splitLinesWithMark(List<String> content, String startMark, String endMark) {
        SplitLines sl = new SplitLines();
        int status = 0;
        for (String s : content) {
            if (s.startsWith(startMark)) {
                status = 1;
            } else if (s.startsWith(startMark)) {
                status = 2;
            } else {
                if (status == 0) {
                    sl.headPart.add(s);
                } else if (status == 2) {
                    sl.lastPart.add(s);
                }
            }
        }
        return sl;
    }


}
