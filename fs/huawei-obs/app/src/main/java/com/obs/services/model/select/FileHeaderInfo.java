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
 * The content of the first line of a  CSV file
 */
public enum FileHeaderInfo {
    USE("USE"),
    IGNORE("IGNORE"),
    NONE("NONE");

    private final String headerInfo;

    /**
     * Constructor
     * 
     * @param headerInfo
     *            Content of the first line
     */
    private FileHeaderInfo(String headerInfo) {
        this.headerInfo = headerInfo;
    }

    /**
     * Formats the current header info definition to a string
     * 
     * @return Header info setting as a string
     */
    public String toString() {
        return this.headerInfo;
    }
}
