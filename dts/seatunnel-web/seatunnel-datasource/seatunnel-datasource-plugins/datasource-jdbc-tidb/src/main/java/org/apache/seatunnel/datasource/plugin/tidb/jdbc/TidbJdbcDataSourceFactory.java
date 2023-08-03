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

package org.apache.seatunnel.datasource.plugin.tidb.jdbc;

import org.apache.seatunnel.datasource.plugin.api.DataSourceChannel;
import org.apache.seatunnel.datasource.plugin.api.DataSourceFactory;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginInfo;

import com.google.auto.service.AutoService;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
@AutoService(DataSourceFactory.class)
public class TidbJdbcDataSourceFactory implements DataSourceFactory {

    @Override
    public String factoryIdentifier() {
        return TidbDataSourceConfig.PLUGIN_NAME;
    }

    @Override
    public Set<DataSourcePluginInfo> supportedDataSources() {
        return Sets.newHashSet(TidbDataSourceConfig.TIDB_DATASOURCE_PLUGIN_INFO);
    }

    @Override
    public DataSourceChannel createChannel() {
        return new TidbJdbcDataSourceChannel();
    }
}
