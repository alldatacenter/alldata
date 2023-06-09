/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.shai.xmodifier.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Shenghai on 14-11-24.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class StringUtils {

    public static String substringAfter(String str, String separator) {
        if (isEmpty(str)) {
            return str;
        }
        if (separator == null) {
            return "";
        }
        int pos = str.indexOf(separator);
        if (pos == -1) {
            return "";
        }
        return str.substring(pos + separator.length());
    }

    public static String substringBefore(String str, String separator) {
        if (isEmpty(str) || separator == null) {
            return str;
        }
        if (separator.length() == 0) {
            return "";
        }
        int pos = str.indexOf(separator);
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }

    public static String substringBetween(String str, String open, String close) {
        if (str == null || open == null || close == null) {
            return null;
        }
        int start = str.indexOf(open);
        if (start != -1) {
            int end = str.indexOf(close, start + open.length());
            if (end != -1) {
                return str.substring(start + open.length(), end);
            }
        }
        return null;
    }

    public static Cons<String, String> findFirstQuotingString(String str, Cons<String, String> quotingMark, List<Cons<String, String>> escapingMarks) {
        int length = str.length();
        Cons<Integer, Integer> resultIndex = new Cons<Integer, Integer>();
        StringQuoter quoter = new StringQuoter();
        quoter.addAllQuoters(escapingMarks);
        for (int i = 0, i2; i < length; i = (i2 > i) ? i2 : i + 1) {
            String rest = str.substring(i);
            if (!quoter.isQuoting()) {
                if (resultIndex.getLeft() == null && rest.startsWith(quotingMark.getLeft())) {
                    i2 = i + quotingMark.getLeft().length();
                    resultIndex.setLeft(i2);
                    continue;
                }
                if (resultIndex.getLeft() != null && resultIndex.getRight() == null && rest.startsWith(quotingMark.getRight())) {
                    resultIndex.setRight(i);
                    break;
                }
            }
            int check = quoter.check(rest);
            i2 = i + check;
        }
        if (resultIndex.getLeft() != null && resultIndex.getRight() != null) {
            Cons<String, String> result = new Cons<String, String>();
            result.setLeft(str.substring(resultIndex.getLeft(), resultIndex.getRight()));
            result.setRight(str.substring(0, resultIndex.getLeft() - quotingMark.getLeft().length()) + str.substring(resultIndex.getRight() + quotingMark.getRight().length()));
            return result;
        }
        return null;
    }

    public static String removeQuotingString(String str, Cons<String, String> quotingMark, List<Cons<String, String>> escapingMarks) {
        Cons<String, String> result;
        String temp = str;
        do {
            result = findFirstQuotingString(temp, quotingMark, escapingMarks);
            if (result == null) {
                break;
            } else {
                temp = result.getRight();
            }
        } while (true);
        return temp;
    }

    public static Cons<String, String> findFirstQuotingString(String str, Cons<String, String> quotingMark) {
        return findFirstQuotingString(str, quotingMark, null);
    }

    public static String findSubStringByRegEx(String str, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    public static String removeEnd(String str, String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        if (str.endsWith(remove)) {
            return str.substring(0, str.length() - remove.length());
        }
        return str;
    }

    public static String removeStart(String str, String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        if (str.startsWith(remove)) {
            return str.substring(remove.length());
        }
        return str;
    }

    public static String unquote(String string) {
        if (string == null) {
            return null;
        }
        String result = string.trim();
        if (result.startsWith("'") && result.endsWith("'")) {
            result = result.substring(1, result.length() - 1);
        }
        return result;
    }

    public static boolean containsOnly(String str, char[] valid) {
        // All these pre-checks are to maintain API with an older version
        if ((valid == null) || (str == null)) {
            return false;
        }
        if (str.length() == 0) {
            return true;
        }
        if (valid.length == 0) {
            return false;
        }
        return indexOfAnyBut(str, valid) == -1;
    }

    public static int indexOfAnyBut(String str, char[] searchChars) {
        if (isEmpty(str) || ArrayUtils.isEmpty(searchChars)) {
            return -1;
        }
        outer: for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            for (int j = 0; j < searchChars.length; j++) {
                if (searchChars[j] == ch) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public static boolean containsOnly(String str, String validChars) {
        if (str == null || validChars == null) {
            return false;
        }
        return containsOnly(str, validChars.toCharArray());
    }

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static String[] splitBySeparator(String xpath, char separator) {
        return splitBySeparator(xpath, separator, new char[][] { { '\'', '\'' } }, false);
    }

    public static String[] splitBySeparator(String xpath, char separator, boolean keepSeparator) {
        return splitBySeparator(xpath, separator, new char[][] { { '\'', '\'' } }, keepSeparator);
    }

    public static String[] splitBySeparator(String xpath, char separator, char[][] quoterList, boolean keepSeparator) {
        if (xpath == null) {
            return null;
        }
        char[] chars = xpath.toCharArray();
        XPathPatternQuoter quoter = new XPathPatternQuoter();
        for (char[] quoterChar : quoterList) {
            quoter.addQuoter(quoterChar[0], quoterChar[1]);
        }
        int splitPoint = -1;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            quoter.check(c);
            if (!quoter.isQuoting() && c == separator) {
                splitPoint = i;
                break;
            }
        }
        String[] result = new String[2];
        if (splitPoint == -1) {
            result[0] = xpath.trim();
            result[1] = null;
        } else {
            result[0] = xpath.substring(0, splitPoint).trim();
            result[1] = xpath.substring(keepSeparator ? splitPoint : splitPoint + 1);
        }
        return result;
    }

    public static String[] splitBySeparator(String src, String[] separators, char[][] quoterList, boolean keepSeparator) {
        if (src == null) {
            return null;
        }
        Arrays.sort(separators, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return o1.length() < o2.length() ? 1 : -1;
            }
        });
        char[] chars = src.toCharArray();
        XPathPatternQuoter quoter = new XPathPatternQuoter();
        for (char[] quoterChar : quoterList) {
            quoter.addQuoter(quoterChar[0], quoterChar[1]);
        }
        List<Cons<Integer, Integer>> splitPoints = new ArrayList<Cons<Integer, Integer>>();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            quoter.check(c);
            if (!quoter.isQuoting()) {
                int offset = checkMatches(chars, i, separators);
                if (offset > 0) {
                    splitPoints.add(new Cons<Integer, Integer>(i, offset));
                    i = i + offset;
                }
            }
        }
        String[] result = new String[splitPoints.size() + 1];
        for (int i = 0; i < splitPoints.size(); i++) {
            Cons<Integer, Integer> splitPoint = splitPoints.get(i);
            if (i == 0) {
                result[i] = src.substring(0, splitPoint.getLeft());
            } else {
                if (keepSeparator) {
                    result[i] = src.substring(splitPoints.get(i - 1).getLeft(), splitPoint.getLeft());
                } else {
                    result[i] = src.substring(splitPoints.get(i - 1).getLeft() + splitPoints.get(i - 1).getRight(), splitPoint.getLeft());
                }
            }
        }
        int lastSplitPoint = 0;
        if (splitPoints.size() > 0) {
            if (keepSeparator) {
                lastSplitPoint = splitPoints.get(splitPoints.size() - 1).getLeft();
            } else {
                lastSplitPoint = splitPoints.get(splitPoints.size() - 1).getLeft() + splitPoints.get(splitPoints.size() - 1).getRight();
            }
        }
        result[result.length - 1] = src.substring(lastSplitPoint);
        return result;
    }

    private static int checkMatches(char[] chars, int start, String[] separators) {
        for (String separator : separators) {
            char[] chars1 = separator.toCharArray();
            boolean isMatch = true;
            for (int i = 0; i < chars1.length; i++) {
                char c = chars1[i];
                if (c != chars[start + i]) {
                    isMatch = false;
                    break;
                }
            }
            if (isMatch) {
                return chars1.length;
            }
        }
        return -1;
    }

    public static String[] removeMarks(String[] arg) {
        String[] result = new String[arg.length];
        for (int i = 0; i < arg.length; i++) {
            String s = arg[i];
            if (s.contains("(:")) {
                s = StringUtils.removeQuotingString(s, new Cons<String, String>("(:", ")"), Arrays.asList(new Cons<String, String>("(", ")")));
            }
            result[i] = s;
        }
        return result;
    }

    public static String[] splitFirstTwo(String source, String key) {
        if (source == null) {
            return null;
        }
        int index = source.indexOf(key);
        if (index == -1) {
            return new String[] { source, null };
        }
        String a = source.substring(0, index);
        String b = source.substring(index + key.length());
        return new String[] { a, b };
    }

    public static String[] splitLastTwo(String source, String key) {
        if (source == null) {
            return null;
        }
        int index = source.lastIndexOf(key);
        if (index == -1) {
            return new String[] { source, null };
        }
        String a = source.substring(0, index);
        String b = source.substring(index + key.length());
        return new String[] { a, b };
    }

    public static String[] splitTwoWithKey1Key2ByLast(String source, String key1, String key2) {
        if (source == null) {
            return null;
        }
        String left;
        String right;
        int index = findLastKeyIndex(source, key1, key1, null);
        if (index > -1) {
            left = source.substring(0, index);
            right = source.substring(index + key1.length());
        } else {
            index = findLastKeyIndex(source, key2, null, key1);
            if (index > -1) {
                left = source.substring(0, index);
                right = source.substring(index + key2.length());
            } else {
                left = source;
                right = null;
            }
        }
        return new String[] { removeEscape(left, key1, key1), removeEscape(right, key1, key1) };
    }

    public static String[] splitTwoWithKey1Key2ByFirst(String source, String key1, String key2) {
        if (source == null) {
            return null;
        }
        String left;
        String right;
        int index = findFirstKeyIndex(source, key1, key1, null);
        if (index > -1) {
            left = source.substring(0, index);
            right = source.substring(index + key1.length());
        } else {
            index = findFirstKeyIndex(source, key2, null, key1);
            if (index > -1) {
                left = source.substring(0, index);
                right = source.substring(index + key2.length());
            } else {
                left = source;
                right = null;
            }
        }
        return new String[] { removeEscape(left, key1, key1), removeEscape(right, key1, key1) };
    }

    private static String removeEscape(String source, String key, String escape) {
        if (source == null) {
            return null;
        }
        return source.replace(escape + key, key);
    }

    private static int findLastKeyIndex(String source, String key, String escape, String ignore) {
        if (escape == null && ignore == null) {
            return source.lastIndexOf(key);
        }
        String tempSource = source;
        if (escape != null) {
            char[] temp = new char[escape.length() + key.length()];
            Arrays.fill(temp, '_');
            tempSource = tempSource.replace(escape + key, new String(temp));
        }
        if (ignore != null) {
            char[] temp = new char[ignore.length()];
            Arrays.fill(temp, '_');
            tempSource = tempSource.replace(ignore, new String(temp));
        }
        return tempSource.lastIndexOf(key);
    }

    private static int findFirstKeyIndex(String source, String key, String escape, String ignore) {
        if (escape == null && ignore == null) {
            return source.indexOf(key);
        }
        String tempSource = source;
        if (escape != null) {
            char[] temp = new char[escape.length() + key.length()];
            Arrays.fill(temp, '_');
            tempSource = tempSource.replace(escape + key, new String(temp));
        }
        if (ignore != null) {
            char[] temp = new char[ignore.length()];
            Arrays.fill(temp, '_');
            tempSource = tempSource.replace(ignore, new String(temp));
        }
        return tempSource.indexOf(key);
    }

    public static String trimToNull(String s) {
        String ts = trim(s);
        return isEmpty(ts) ? null : ts;
    }

    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    public static String stripEnd(String str, String stripChars) {
        int end;
        if (str == null || (end = str.length()) == 0) {
            return str;
        }
        if (stripChars == null) {
            while ((end != 0) && Character.isWhitespace(str.charAt(end - 1))) {
                end--;
            }
        } else if (stripChars.length() == 0) {
            return str;
        } else {
            while ((end != 0) && (stripChars.indexOf(str.charAt(end - 1)) != -1)) {
                end--;
            }
        }
        return str.substring(0, end);
    }

    public static String[] stripAll(String[] strs, String stripChars) {
        int strsLen;
        if (strs == null || (strsLen = strs.length) == 0) {
            return strs;
        }
        String[] newArr = new String[strsLen];
        for (int i = 0; i < strsLen; i++) {
            newArr[i] = strip(strs[i], stripChars);
        }
        return newArr;
    }

    public static String strip(String str, String stripChars) {
        if (isEmpty(str)) {
            return str;
        }
        str = stripStart(str, stripChars);
        return stripEnd(str, stripChars);
    }

    public static String strip(String str, List<String> stripChars) {
        if (isEmpty(str)) {
            return str;
        }
        for (String stripChar : stripChars) {
            if (str.startsWith(stripChar)) {
                str = str.substring(stripChar.length());
            }
            if (str.endsWith(stripChar)) {
                str = str.substring(0, str.length() - stripChar.length());
            }
        }
        return str;
    }

    public static String stripStart(String str, String stripChars) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        int start = 0;
        if (stripChars == null) {
            while ((start != strLen) && Character.isWhitespace(str.charAt(start))) {
                start++;
            }
        } else if (stripChars.length() == 0) {
            return str;
        } else {
            while ((start != strLen) && (stripChars.indexOf(str.charAt(start)) != -1)) {
                start++;
            }
        }
        return str.substring(start);
    }

    public static List<String> splitToElements(String xpath, List<String> separator, char[][] quoterList, boolean keepSeparator) {
        if (xpath == null) {
            return null;
        }
        List<String> result = new ArrayList<String>();
        List<Integer> pointCuts = new ArrayList<Integer>();
        Collections.sort(separator, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return o1.length() <= o2.length() ? 1 : -1;
            }
        });
        char[] chars = xpath.toCharArray();
        XPathPatternQuoter quoter = new XPathPatternQuoter();
        if (quoterList != null) {
            for (char[] quoterChar : quoterList) {
                quoter.addQuoter(quoterChar[0], quoterChar[1]);
            }
        }
        pointCuts.add(0);
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            quoter.check(c);
            if (!quoter.isQuoting() && separator.contains(String.valueOf(c))) {
                pointCuts.add(i);
            }
        }
        pointCuts.add(xpath.length());
        if (pointCuts.size() == 0) {
            result.add(xpath.trim());
            return result;
        }
        int lastIndex = -1;
        for (Integer pointCut : pointCuts) {
            if (lastIndex == -1) {
                lastIndex = pointCut;
                continue;
            }
            String substring = xpath.substring(lastIndex, pointCut);
            if (!keepSeparator) {
                substring = strip(substring, separator);
            }
            result.add(substring);
            lastIndex = pointCut;
        }
        return result;
    }
}
