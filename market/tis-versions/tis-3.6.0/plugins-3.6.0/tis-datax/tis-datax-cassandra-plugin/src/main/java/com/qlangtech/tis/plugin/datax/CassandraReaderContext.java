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
import com.qlangtech.tis.plugin.datax.common.RdbmsReaderContext;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.IDataSourceDumper;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.cassandra.CassandraDatasourceFactory;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-06 15:18
 **/
public class CassandraReaderContext extends RdbmsReaderContext<DataXCassandraReader, CassandraDatasourceFactory> {

    private final SelectedTab tab;

    public CassandraReaderContext(String jobName, SelectedTab tab, IDataSourceDumper dumper, DataXCassandraReader reader) {
        super(jobName, tab.getName(), dumper, reader);
        this.tab = tab;
    }

    public boolean isContainAllowFiltering() {
        return this.plugin.allowFiltering != null;
    }

    public boolean isAllowFiltering() {
        return this.plugin.allowFiltering;
    }

    public boolean isContainConsistancyLevel() {
        return StringUtils.isNotEmpty(this.plugin.consistancyLevel);
    }

    public String getConsistancyLevel() {
        return this.plugin.consistancyLevel;
    }

    public String getTable() {
        return this.tab.getName();
    }

    public boolean isContainWhere() {
        return StringUtils.isNotEmpty(this.tab.where);
    }

    public String getWhere() {
        return this.tab.where;
    }

    public List<CMeta> getColumn() {
        return this.tab.getCols();
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
}
