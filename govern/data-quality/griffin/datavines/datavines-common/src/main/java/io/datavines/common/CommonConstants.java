/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common;

import io.datavines.common.utils.StringUtils;

public class CommonConstants {

    public static final String DOT = ".";

    /**
     * QUESTION ?
     */
    public static final String QUESTION = "?";

    /**
     * comma ,
     */
    public static final String COMMA = ",";

    /**
     * COLON :
     */
    public static final String COLON = ":";

    /**
     * SINGLE_SLASH /
     */
    public static final String SINGLE_SLASH = "/";

    /**
     * DOUBLE_SLASH //
     */
    public static final String DOUBLE_SLASH = "//";

    /**
     * SEMICOLON ;
     */
    public static final String SEMICOLON = ";";

    /**
     * EQUAL SIGN
     */
    public static final String EQUAL_SIGN = "=";

    /**
     * underline  "_"
     */
    public static final String UNDERLINE = "_";

    /**
     * SINGLE_QUOTES "'"
     */
    public static final String SINGLE_QUOTES = "'";

    /**
     * double brackets left
     */
    public static final String DOUBLE_BRACKETS_LEFT = "{{";

    /**
     * double brackets left
     */
    public static final String DOUBLE_BRACKETS_RIGHT = "}}";

    /**
     * double brackets left
     */
    public static final String DOUBLE_BRACKETS_LEFT_SPACE = "{ {";

    /**
     * double brackets left
     */
    public static final String DOUBLE_BRACKETS_RIGHT_SPACE = "} }";

    /**
     * UTF-8
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * http connect time out
     */
    public static final int HTTP_CONNECT_TIMEOUT = 60 * 1000;

    /**
     * http connect request time out
     */
    public static final int HTTP_CONNECTION_REQUEST_TIMEOUT = 60 * 1000;

    /**
     * httpclient socket time out
     */
    public static final int SOCKET_TIMEOUT = 60 * 1000;

    /**
     * ACCEPTED
     */
    public static final String ACCEPTED = "ACCEPTED";

    /**
     * SUCCEEDED
     */
    public static final String SUCCEEDED = "SUCCEEDED";

    /**
     * NEW
     */
    public static final String NEW = "NEW";

    /**
     * NEW_SAVING
     */
    public static final String NEW_SAVING = "NEW_SAVING";

    /**
     * SUBMITTED
     */
    public static final String SUBMITTED = "SUBMITTED";

    /**
     * FAILED
     */
    public static final String FAILED = "FAILED";

    /**
     * KILLED
     */
    public static final String KILLED = "KILLED";

    /**
     * RUNNING
     */
    public static final String RUNNING = "RUNNING";

    public static final int SLEEP_TIME_MILLIS = 1000;

    /**
     * date format of yyyy-MM-dd HH:mm:ss
     */
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    /**
     * network interface preferred
     */
    public static final String NETWORK_INTERFACE_PREFERRED = "network.interface.preferred";

    /**
     * network IP gets priority, default inner outer
     */
    public static final String NETWORK_PRIORITY_STRATEGY = "network.priority.strategy";

    public static final Boolean KUBERNETES_MODE = !StringUtils.isEmpty(System.getenv("KUBERNETES_SERVICE_HOST")) && !StringUtils.isEmpty(System.getenv("KUBERNETES_SERVICE_PORT"));

    public static final String REG_EMAIL_FORMAT = "^[a-z_0-9.-]{1,64}@([a-z0-9-]{1,200}.){1,5}[a-z]{1,6}$";

    public static final String REG_USER_PASSWORD = ".{6,20}";

    public static final String SMALL = "small";

    public static final String AND = " AND ";

    public static final String TABLE = "table";

    public static final String TABLE2 = "table2";

    public static final String DATABASE2 = "database2";

    /**
     * month_begin
     */
    public static final String MONTH_BEGIN = "month_begin";
    /**
     * add_months
     */
    public static final String ADD_MONTHS = "add_months";
    /**
     * month_end
     */
    public static final String MONTH_END = "month_end";
    /**
     * week_begin
     */
    public static final String WEEK_BEGIN = "week_begin";
    /**
     * week_end
     */
    public static final String WEEK_END = "week_end";

    public static final String TODAY = "today";

    public static final String YESTERDAY = "yesterday";

    public static final String TOMORROW = "tomorrow";

    public static final String MONTH_FIRST_DAY = "month_first_day";

    /**
     * month_last_day
     */
    public static final String MONTH_LAST_DAY = "month_last_day";

    /**
     * week_first_day
     */
    public static final String WEEK_FIRST_DAY = "week_first_day";

    /**
     * week_last_day
     */
    public static final String WEEK_LAST_DAY = "week_last_day";

    /**
     * year_week
     */
    public static final String YEAR_WEEK = "year_week";
    /**
     * timestamp
     */
    public static final String TIMESTAMP = "timestamp";

    public static final char SUBTRACT_CHAR = '-';
    /**
     * hyphen
     */
    public static final String HYPHEN = "-";


    public static final char ADD_CHAR = '+';

    public static final char MULTIPLY_CHAR = '*';

    public static final char DIVISION_CHAR = '/';

    public static final char LEFT_BRACE_CHAR = '(';

    public static final char RIGHT_BRACE_CHAR = ')';

    public static final String ADD_STRING = "+";

    public static final String MULTIPLY_STRING = "*";

    public static final String DIVISION_STRING = "/";

    public static final String LEFT_BRACE_STRING = "(";

    public static final char P = 'P';

    public static final char N = 'N';

    public static final String SUBTRACT_STRING = "-";

    public static final String PARAMETER_FORMAT_TIME = "yyyyMMddHHmmss";

    /**
     * system date(yyyyMMddHHmmss)
     */
    public static final String PARAMETER_DATETIME = "system.datetime";

    /**
     * system date(yyyymmdd) today
     */
    public static final String PARAMETER_CURRENT_DATE = "system.biz.curdate";

    /**
     * system date(yyyymmdd) yesterday
     */
    public static final String PARAMETER_BUSINESS_DATE = "system.biz.date";

    /**
     * new
     * schedule time
     */
    public static final String PARAMETER_SCHEDULE_TIME = "schedule.time";

    public static final String LOCAL = "local";

    public static final String SPARK = "spark";
}
