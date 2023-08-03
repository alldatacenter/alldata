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

import lombok.Builder;
import lombok.Data;

import static com.google.common.base.Preconditions.checkNotNull;

@Data
@Builder
public class DataSourcePluginInfo {

    public String name;

    public String icon;

    public String version;

    /** @see DatasourcePluginTypeEnum */
    private Integer type;

    /** whether support virtual tables, default false */
    private Boolean supportVirtualTables;

    public DataSourcePluginInfo(
            String name, String icon, String version, Integer type, Boolean supportVirtualTables) {
        this.name = checkNotNull(name, "name can not be null");
        this.icon = checkNotNull(icon, "icon can not be null");
        this.version = checkNotNull(version, "version can not be null");
        this.type = checkNotNull(type, "type can not be null");
        this.supportVirtualTables = supportVirtualTables != null && supportVirtualTables;
    }
}
