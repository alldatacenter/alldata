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

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User type enum
 */
public enum UserTypeEnum implements IntListValuable {

    /**
     * Has all privilege
     */
    ADMIN(0),
    /**
     * No privilege to manage the system
     */
    OPERATOR(1),
    ;

    @Getter
    @JsonValue
    private final Integer code;

    UserTypeEnum(Integer code) {
        this.code = code;
    }

    private static final List<Integer> ARRAYS = Arrays.stream(values())
            .map(UserTypeEnum::getCode)
            .collect(Collectors.toList());

    public static UserTypeEnum parseCode(Integer value) {
        return Arrays.stream(UserTypeEnum.class.getEnumConstants())
                .filter(x -> x.getCode().equals(value))
                .findAny()
                .orElse(null);
    }

    public static Integer parseName(String value) {
        for (UserTypeEnum type : UserTypeEnum.values()) {
            if (type.name().equals(value)) {
                return type.code;
            }
        }

        return null;
    }

    public static String name(Integer value) {
        return parseCode(value).name();
    }

    @Override
    public List<Integer> valueList() {
        return ARRAYS;
    }

}
