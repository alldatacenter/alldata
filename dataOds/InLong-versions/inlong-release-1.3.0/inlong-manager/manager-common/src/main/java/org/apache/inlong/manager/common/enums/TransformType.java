/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.common.enums;

import lombok.Getter;

import java.util.Locale;

public enum TransformType {

    /**
     * Replace string field based on regex
     */
    STRING_REPLACER("string_replacer"),

    /**
     * Split field by separator
     */
    SPLITTER("splitter"),

    /**
     * Filter stream records on given regulations
     */
    FILTER("filter"),

    /**
     * Remove duplication records on given fields
     */
    DE_DUPLICATION("de_duplication"),

    /**
     * Joins different sources in one stream
     */
    JOINER("joiner"),

    /**
     * Encrypt records on given fields
     */
    ENCRYPT("encrypt");

    @Getter
    private final String type;

    TransformType(String type) {
        this.type = type;
    }

    public static TransformType forType(String type) {
        for (TransformType transformType : values()) {
            if (transformType.getType().equals(type) || transformType.getType().toUpperCase(Locale.ROOT).equals(type)) {
                return transformType;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported transformType=%s", type));
    }

}
