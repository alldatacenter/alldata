/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.utils;

public enum RegexDef {

    TMP_FILTER(0, "^[_A-Za-z0-9]+$",
            "must only contain characters,numbers,and underscores"),
    TMP_STRING(1, "^[a-zA-Z]\\w+$",
            "must begin with a letter,can only contain characters,numbers,and underscores"),
    TMP_NUMBER(2, "^-?[0-9]\\d*$", "must only contain numbers"),
    TMP_GROUP(3, "^[a-zA-Z][\\w-]+$",
            "must begin with a letter,can only contain characters,numbers,hyphen,and underscores"),
    TMP_CONSUMERID(4, "^[_A-Za-z0-9\\.\\-]+$",
            "must begin with a letter,can only contain characters,numbers,dot,scores,and underscores"),
    TMP_IPV4ADDRESS(5,
            "((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))",
            "must matches the IP V4 address regulation");

    private final int id;
    private final String pattern;
    private final String errMsgTemp;

    RegexDef(int id, String pattern, String errMsgTemp) {
        this.id = id;
        this.pattern = pattern;
        this.errMsgTemp = errMsgTemp;
    }

    public int getId() {
        return id;
    }

    public String getPattern() {
        return pattern;
    }

    public String getErrMsgTemp() {
        return errMsgTemp;
    }
}
