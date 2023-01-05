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
package com.qlangtech.tis.solrdao;

import org.apache.commons.lang.StringUtils;

import java.sql.Timestamp;
import java.util.Date;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-02-25 17:00
 */
public enum SolrJavaType {
    INT(Integer.class, 1), FLOAT(Float.class, 2), DOUBLE(Double.class, 3), LONG(Long.class, 4) //
    , TIMESTAMP(Timestamp.class, 5), DATE(Date.class, 6), DATETIME(Date.class, 7), STRING(String.class, 8);

    public static SolrJavaType parse(String fieldType) {
        if (isTypeMatch(fieldType, "int")) {
            return INT;
        } else if (isTypeMatch(fieldType, "float")) {
            return FLOAT;
        } else if (isTypeMatch(fieldType, "double")) {
            return DOUBLE;
        } else if (isTypeMatch(fieldType, "long")) {
            return LONG;
        } else if (isTypeMatch(fieldType, "timestamp")) {
            return TIMESTAMP;
        } else if (isTypeMatch(fieldType, "datetime")) {
            return DATETIME;
        } else if (isTypeMatch(fieldType, "date")) {
            return DATE;
        }
        return STRING;
    }

    private static boolean isTypeMatch(String fieldType, String matchLetter) {
        return StringUtils.indexOfAny(fieldType, new String[]{matchLetter, StringUtils.capitalize(matchLetter)}) > -1;
    }

    private final Class<?> javaType;
    private final int typeCode;
    // private final Method valueof;

    private SolrJavaType(Class<?> javaType, int typeCode) {
        this.javaType = javaType;
        this.typeCode = typeCode;
//        try {
//            if (javaType == String.class) {
//                valueof = javaType.getMethod("valueOf", Object.class);
//            } else {
//                valueof = javaType.getMethod("valueOf", String.class);
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }

    public int getTypeCode() {
        return typeCode;
    }

    public String getSimpleName() {
        return StringUtils.lowerCase(javaType.getSimpleName());
    }
}
