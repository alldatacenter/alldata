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
package com.qlangtech.tis.realtime.transfer;

import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class UnderlineUtils {

    private UnderlineUtils() {
    }

    public static StringBuffer removeUnderline(String value) {
        StringBuffer parsedName = new StringBuffer();
        char[] nameAry = value.toCharArray();
        boolean findUnderChar = false;
        for (int i = 0; i < nameAry.length; i++) {
            if (nameAry[i] == '_') {
                findUnderChar = true;
            } else {
                if (findUnderChar) {
                    parsedName.append(Character.toUpperCase(nameAry[i]));
                    findUnderChar = false;
                } else {
                    parsedName.append(nameAry[i]);
                }
            }
        }
        return parsedName;
    }

    public static StringBuffer addUnderline(String value) {
        StringBuffer parsedName = new StringBuffer();
        char[] nameAry = value.toCharArray();
        for (int i = 0; i < nameAry.length; i++) {
            if (Character.isUpperCase(nameAry[i])) {
                parsedName.append('_').append(Character.toLowerCase(nameAry[i]));
            } else {
                parsedName.append(nameAry[i]);
            }
        }
        return parsedName;
    }

    public static String getJavaName(String collection) {
//        Matcher matcher = PATTERN_COLLECTION_NAME.matcher(collection);
//        if (!matcher.matches()) {
//            throw new IllegalStateException("collection:" + collection + " is not match the Pattern:" + PATTERN_COLLECTION_NAME);
//        }
//        return matcher.replaceFirst("S4$2");
        // return StringUtils.capitalize(collection);
        return StringUtils.capitalize(removeUnderline(collection).toString());
    }
}
