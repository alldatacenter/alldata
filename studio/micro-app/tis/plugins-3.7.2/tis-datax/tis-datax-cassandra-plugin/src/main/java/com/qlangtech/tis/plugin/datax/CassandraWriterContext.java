/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.plugin.datax;

import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.IDataxContext;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.cassandra.CassandraDatasourceFactory;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-21 18:18
 **/
public class CassandraWriterContext implements IDataxContext {
    private final DataXCassandraWriter writer;
    private final IDataxProcessor.TableMap tabMapper;
    private final CassandraDatasourceFactory dsFactory;

    public CassandraWriterContext(DataXCassandraWriter writer, IDataxProcessor.TableMap tabMapper) {
        this.writer = writer;
        this.tabMapper = tabMapper;
        this.dsFactory = writer.getDataSourceFactory();
    }

    public String getKeyspace() {
        return this.dsFactory.dbName;
    }

    public boolean isSupportUseSSL() {
        return this.dsFactory.useSSL != null;
    }

    public boolean isUseSSL() {
        return this.dsFactory.useSSL;
    }

    public String getTable() {
        return this.tabMapper.getTo();
    }

    public List<CMeta> getColumn() {
        return this.tabMapper.getSourceCols();
    }

    public String getHost() {
        return Lists.newArrayList(this.dsFactory.getHosts()).stream().collect(Collectors.joining(","));
    }

    public int getPort() {
        return this.dsFactory.port;
    }

    public boolean isContainUsername() {
        return StringUtils.isNotEmpty(this.dsFactory.userName);
    }

    public boolean isContainPassword() {
        return StringUtils.isNotEmpty(this.dsFactory.password);
    }

    public String getUsername() {
        return this.dsFactory.userName;
    }

    public String getPassword() {
        return this.dsFactory.password;
    }

    public boolean isContainConnectionsPerHost() {
        return this.writer.connectionsPerHost != null && this.writer.connectionsPerHost > 0;
    }

    public int getConnectionsPerHost() {
        return this.writer.connectionsPerHost;
    }

    public boolean isContainMaxPendingPerConnection() {
        return this.writer.maxPendingPerConnection != null && this.writer.maxPendingPerConnection > 0;
    }

    public int getMaxPendingPerConnection() {

        return this.writer.maxPendingPerConnection;
    }

    public boolean isContainConsistancyLevel() {
        return StringUtils.isNotBlank(this.writer.consistancyLevel);
    }

    public String getConsistancyLevel() {
        return this.writer.consistancyLevel;
    }

    public boolean isContainBatchSize() {
        return this.writer.batchSize != null && this.writer.batchSize > 0;
    }

    public int getBatchSize() {
        return this.writer.batchSize;
    }
}
