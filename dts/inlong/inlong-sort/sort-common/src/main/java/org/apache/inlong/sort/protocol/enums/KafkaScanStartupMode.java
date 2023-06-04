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

package org.apache.inlong.sort.protocol.enums;

import java.util.Locale;

/**
 * kafka consumer scan startup mode enum
 */
public enum KafkaScanStartupMode {

    EARLIEST_OFFSET("earliest-offset"),
    LATEST_OFFSET("latest-offset"),
    SPECIFIC_OFFSETS("specific-offsets"),
    TIMESTAMP_MILLIS("timestamp"),
    GROUP_OFFSETS("group-offsets");

    KafkaScanStartupMode(String value) {
        this.value = value;
    }

    private final String value;

    public String getValue() {
        return value;
    }

    public static KafkaScanStartupMode forName(String name) {
        for (KafkaScanStartupMode startupMode : values()) {
            if (startupMode.getValue().equals(name.toLowerCase(Locale.ROOT))) {
                return startupMode;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported KafkaScanStartupMode=%s for Inlong", name));
    }

}
