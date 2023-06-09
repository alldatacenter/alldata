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
package com.qlangtech.tis.plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-06-12 14:40
 */
public interface ValidatorCommons {

    Pattern PATTERN_URL = Pattern.compile("(https?|hdfs)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");

    Pattern PATTERN_NONE_BLANK = Pattern.compile("([^\\s]+)");
    String MSG_NONE_BLANK_ERROR = "内容不能包含空格";

    Pattern PATTERN_RELATIVE_PATH = Pattern.compile("([\\w\\d\\.]+/)*([\\w\\d\\.]*(\\*)?){1}");
    String MSG_RELATIVE_PATH_ERROR = "不符合相对路径格式要求";

    Pattern PATTERN_ABSOLUTE_PATH = Pattern.compile("/" + PATTERN_RELATIVE_PATH.toString());
    String MSG_ABSOLUTE_PATH_ERROR = "不符合绝对路径格式要求";
    // 数据库列名
    Pattern PATTERN_DB_COL_NAME = Pattern.compile("(^_([a-zA-Z0-9]_?)*$)|(^[a-zA-Z0-9](_?[a-zA-Z0-9])*_?$)");

    String MSG_DB_COL_NAME_ERROR = "格式不正确，提示:'首位可以是字母或下划线。首位之后可以是字母，数字以及下划线。下划线后不能接下划线'";


    String MSG_URL_ERROR = "不符合URL的规范";

    //Pattern pattern_identity = Pattern.compile("[a-z]{1}[\\da-z_\\-]+");

    Pattern pattern_identity = Pattern.compile("[A-Z\\da-z_\\-]+");

    Pattern pattern_integer = Pattern.compile("[1-9]{1}[\\d]{0,}|0");

    String MSG_INTEGER_ERROR = "必须是整型数字";

    String MSG_IDENTITY_ERROR = "必须由小写字母，大写字母，数字、下划线、减号组成";

    Pattern host_pattern = Pattern.compile("[\\da-z]{1}[\\da-z.]+:\\d+");
    String MSG_HOST_IP_ERROR = "必须由IP、HOST及端口号组成";

    Pattern host_without_port_pattern = Pattern.compile("[\\da-z]{1}[\\da-z.]+");
    String MSG_HOST_IP_WITHOUT_PORT_ERROR = "必须由IP或者HOST组成";

    String MSG_EMPTY_INPUT_ERROR = "必须填写";

    Pattern pattern_user_name = Pattern.compile("[A-Z\\da-z_\\-\\.\\$]+");
    String MSG_USER_NAME_ERROR = "必须由小写字母，大写字母，数字、下划线、点、减号组成";

    public static void main(String[] args) {
        Matcher matcher = pattern_integer.matcher("0");
        System.out.println(matcher.matches());

        matcher = pattern_integer.matcher("1230");
        System.out.println(matcher.matches());

        matcher = pattern_integer.matcher("0123");
        System.out.println(matcher.matches());
    }
}
