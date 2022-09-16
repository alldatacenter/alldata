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

/**
 * Enum of data compress.
 */
public enum CompressFormat {

    NONE("none"),
    DEFLATE("deflate"),
    GZIP("gzip"),
    BZIP2("bzip2"),
    LZ4("lz4"),
    SNAPPY("snappy");

    @Getter
    private final String name;

    CompressFormat(String name) {
        this.name = name;
    }

    /**
     * Get data compress format by name.
     */
    public static CompressFormat forName(String name) {
        for (CompressFormat compressFormat : values()) {
            if (compressFormat.getName().equalsIgnoreCase(name)) {
                return compressFormat;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupport CompressionFormat:%s", name));
    }

}
