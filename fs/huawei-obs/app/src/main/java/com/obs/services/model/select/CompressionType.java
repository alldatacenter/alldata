/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model.select;

/**
 * Compression type of the input file
 */
public enum CompressionType {
    NONE("NONE"),
    GZIP("GZIP"),
    BZIP2("BZIP2");

    private final String compressionType;

    /**
     * Constructor
     * 
     * @param compressionType
     *            Compression type
     */
    private CompressionType(String compressionType) {
        this.compressionType = compressionType;
    }

    /**
     * Formats the current compression type to a string
     * 
     * @return Compression type as a string
     */
    public String toString() {
        return this.compressionType;
    }
}
