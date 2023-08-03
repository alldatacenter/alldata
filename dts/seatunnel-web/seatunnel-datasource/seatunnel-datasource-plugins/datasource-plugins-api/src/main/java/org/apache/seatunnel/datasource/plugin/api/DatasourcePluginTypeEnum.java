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

package org.apache.seatunnel.datasource.plugin.api;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("checkstyle:RegexpSingleline")
public enum DatasourcePluginTypeEnum {
    DATABASE(1, "database", "传统数据库"),
    FILE(2, "file", "文件"),
    NO_STRUCTURED(3, "no_structured", "非结构化数据（NoSQLs）"),
    STORAGE(4, "storage", "存储"),
    REMOTE_CONNECTION(5, "remote_connection", "远程连接");

    private final Integer code;

    private final String name;

    private final String chineseName;

    DatasourcePluginTypeEnum(Integer code, String name, String chineseName) {
        this.code = checkNotNull(code);
        this.name = checkNotNull(name);
        this.chineseName = checkNotNull(chineseName);
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getChineseName() {
        return chineseName;
    }
}
