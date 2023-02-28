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
}
