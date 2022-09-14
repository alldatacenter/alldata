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

package org.apache.inlong.audit.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditConfig {
    private static final Logger logger = LoggerFactory.getLogger(AuditConfig.class);
    private static String FILE_PATH = "/data/inlong/audit/";
    private static final int FILE_SIZE = 500 * 1024 * 1024;
    private static final int MAX_CACHE_ROWS = 2000000;
    private static final int MIN_CACHE_ROWS = 100;

    private String filePath;
    private int maxCacheRow;
    private int maxFileSize = FILE_SIZE;

    public AuditConfig(String filePath, int maxCacheRow) {
        if (filePath == null || filePath.length() == 0) {
            this.filePath = FILE_PATH;
        } else {
            this.filePath = filePath;
        }
        if (maxCacheRow < MIN_CACHE_ROWS) {
            this.maxCacheRow = MAX_CACHE_ROWS;
        } else {
            this.maxCacheRow = maxCacheRow;
        }
    }

    public AuditConfig() {
        this.filePath = FILE_PATH;
        this.maxCacheRow = MAX_CACHE_ROWS;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setMaxCacheRow(int maxCacheRow) {
        this.maxCacheRow = maxCacheRow;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getMaxCacheRow() {
        return maxCacheRow;
    }

    public int getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(int maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public String getDisasterFile() {
        return filePath + "/" + disasterFileName;
    }

    private String disasterFileName = "disaster.data";
}
