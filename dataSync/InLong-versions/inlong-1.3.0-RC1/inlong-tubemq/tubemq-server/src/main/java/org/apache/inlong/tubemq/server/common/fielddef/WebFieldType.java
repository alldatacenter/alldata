/**
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

package org.apache.inlong.tubemq.server.common.fielddef;

public enum WebFieldType {

    UNKNOWN(-1, "Unknown field type"),
    STRING(1, "String"),
    INT(2, "int"),
    LONG(3, "long"),
    BOOLEAN(4, "Boolean"),
    DATE(5, "Date"),
    COMPSTRING(6, "Compound string"),
    COMPINT(7, "Compound integer"),
    COMPLONG(8, "Compound long"),
    JSONDICT(9, "Json dict"),
    JSONSET(10, "Json set"),
    DELPOLICY(11, "Delete policy");

    private int value;
    private String desc;

    WebFieldType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static WebFieldType valueOf(int value) {
        for (WebFieldType fieldType : WebFieldType.values()) {
            if (fieldType.getValue() == value) {
                return fieldType;
            }
        }

        return UNKNOWN;
    }

    public int getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

}
