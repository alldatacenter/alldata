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

package org.apache.seatunnel.datasource.plugin.s3;

import org.apache.seatunnel.datasource.plugin.api.DataSourceChannel;
import org.apache.seatunnel.datasource.plugin.api.DataSourceFactory;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginInfo;
import org.apache.seatunnel.datasource.plugin.api.DatasourcePluginTypeEnum;

import com.google.auto.service.AutoService;
import com.google.common.collect.Sets;

import java.util.Set;

@AutoService(DataSourceFactory.class)
public class S3DataSourceFactory implements DataSourceFactory {

    private static final String PLUGIN_NAME = "S3";

    @Override
    public String factoryIdentifier() {
        return PLUGIN_NAME;
    }

    @Override
    public Set<DataSourcePluginInfo> supportedDataSources() {
        DataSourcePluginInfo s3DatasourcePluginInfo =
                DataSourcePluginInfo.builder()
                        .name(PLUGIN_NAME)
                        .type(DatasourcePluginTypeEnum.FILE.getCode())
                        .version("1.0.0")
                        .supportVirtualTables(false)
                        .icon("S3File")
                        .build();

        return Sets.newHashSet(s3DatasourcePluginInfo);
    }

    @Override
    public DataSourceChannel createChannel() {
        return new S3DatasourceChannel();
    }
}
