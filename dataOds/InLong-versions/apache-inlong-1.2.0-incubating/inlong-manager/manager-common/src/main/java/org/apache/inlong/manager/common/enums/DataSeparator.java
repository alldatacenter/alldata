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

/**
 * Enum of data separator and related ASCII code.
 */
public enum DataSeparator {

    VERTICAL_BAR("|", 124),
    COMMA(",", 44),
    COLON(":", 58),
    SEMICOLON(";", 59),
    DASH("-", 45),
    SOH("\001", 1),
    STX("\002", 2),
    ETX("\003", 3);

    private final String separator;

    private final Integer asciiCode;

    DataSeparator(String separator, int asciiCode) {
        this.asciiCode = asciiCode;
        this.separator = separator;
    }

    public static DataSeparator forAscii(int asciiCode) {
        for (DataSeparator value : values()) {
            if (value.getAsciiCode() == asciiCode) {
                return value;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported ascii for %s", asciiCode));
    }

    public String getSeparator() {
        return this.separator;
    }

    public Integer getAsciiCode() {
        return this.asciiCode;
    }
}
