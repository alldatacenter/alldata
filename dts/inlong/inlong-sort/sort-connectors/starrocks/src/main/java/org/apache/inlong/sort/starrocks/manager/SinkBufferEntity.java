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

package org.apache.inlong.sort.starrocks.manager;

import com.starrocks.connector.flink.manager.StarRocksSinkBufferEntity;

/**
 * StarRocks data buffer
 */
public class SinkBufferEntity extends StarRocksSinkBufferEntity {

    private String dirtyLogTag;
    private String dirtyLabel;
    private String dirtyIdentify;
    private String columns;

    public SinkBufferEntity(String database, String table, String labelPrefix) {
        super(database, table, labelPrefix);
    }

    public String getColumns() {
        return columns;
    }

    public void setColumns(String columns) {
        this.columns = columns;
    }

    public String getDirtyLogTag() {
        return dirtyLogTag;
    }

    public void setDirtyLogTag(String dirtyLogTag) {
        this.dirtyLogTag = dirtyLogTag;
    }

    public String getDirtyLabel() {
        return dirtyLabel;
    }

    public void setDirtyLabel(String dirtyLabel) {
        this.dirtyLabel = dirtyLabel;
    }

    public String getDirtyIdentify() {
        return dirtyIdentify;
    }

    public void setDirtyIdentify(String dirtyIdentify) {
        this.dirtyIdentify = dirtyIdentify;
    }

    @Override
    public SinkBufferEntity asEOF() {
        super.asEOF();
        return this;
    }
}
