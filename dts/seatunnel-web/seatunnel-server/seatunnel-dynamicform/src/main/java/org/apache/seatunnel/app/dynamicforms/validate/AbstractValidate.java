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

package org.apache.seatunnel.app.dynamicforms.validate;

import org.apache.seatunnel.shade.com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.seatunnel.app.dynamicforms.FormLocale;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

import java.util.Arrays;
import java.util.List;

@Data
public class AbstractValidate<T extends AbstractValidate> {
    private final List<String> trigger = Arrays.asList("input", "blur");

    // support i18n
    private String message = "required";

    public enum RequiredType {
        @JsonProperty("non-empty")
        NON_EMPTY("non-empty"),

        @JsonProperty("union-non-empty")
        UNION_NON_EMPTY("union-non-empty"),

        @JsonProperty("mutually-exclusive")
        MUTUALLY_EXCLUSIVE("mutually-exclusive");

        @Getter private String type;

        RequiredType(String type) {
            this.type = type;
        }
    }

    public T withMessage(@NonNull String message) {
        this.message = message;
        return (T) this;
    }

    public T withI18nMessage(@NonNull String message) {
        this.message = FormLocale.I18N_PREFIX + message;
        return (T) this;
    }
}
