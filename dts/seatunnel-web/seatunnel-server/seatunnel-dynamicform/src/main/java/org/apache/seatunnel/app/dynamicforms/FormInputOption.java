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

package org.apache.seatunnel.app.dynamicforms;

import org.apache.seatunnel.shade.com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NonNull;

public class FormInputOption extends AbstractFormOption {
    @JsonProperty("type")
    @Getter
    private final FormType formType = FormType.INPUT;

    @Getter private final InputType inputType;

    public FormInputOption(
            @NonNull InputType inputType, @NonNull String label, @NonNull String field) {
        super(label, field);
        this.inputType = inputType;
    }

    public enum InputType {
        @JsonProperty("text")
        TEXT("text"),

        @JsonProperty("password")
        PASSWORD("password"),

        @JsonProperty("textarea")
        TEXTAREA("textarea");

        @Getter private String inputType;

        InputType(String inputType) {
            this.inputType = inputType;
        }
    }
}
