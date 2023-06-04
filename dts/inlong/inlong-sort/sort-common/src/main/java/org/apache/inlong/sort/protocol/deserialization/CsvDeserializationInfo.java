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

package org.apache.inlong.sort.protocol.deserialization;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Csv deserialization info
 */
public class CsvDeserializationInfo implements DeserializationInfo {

    private static final long serialVersionUID = -5035426390567887081L;

    private final char splitter;

    // TODO: support mapping index to field

    @JsonCreator
    public CsvDeserializationInfo(
            @JsonProperty("splitter") char splitter) {
        this.splitter = splitter;
    }

    @JsonProperty("splitter")
    public char getSplitter() {
        return splitter;
    }
}
