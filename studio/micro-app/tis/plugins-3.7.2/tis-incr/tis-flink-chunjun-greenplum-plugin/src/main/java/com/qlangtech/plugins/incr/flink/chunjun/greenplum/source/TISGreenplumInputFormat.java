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

package com.qlangtech.plugins.incr.flink.chunjun.greenplum.source;

import com.dtstack.chunjun.connector.postgresql.source.PostgresqlInputFormat;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-29 13:05
 **/
public final class TISGreenplumInputFormat extends PostgresqlInputFormat {
    private final DataSourceFactory dataSourceFactory;

    public TISGreenplumInputFormat(DataSourceFactory dataSourceFactory) {
        if (dataSourceFactory == null) {
            throw new IllegalArgumentException("param dataSourceFactory can not be null");
        }
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    protected Connection getConnection() throws SQLException {
        return Objects.requireNonNull(dataSourceFactory, "dataSourceFactory can not be null")
                .getConnection(jdbcConf.getJdbcUrl()).getConnection();
    }

    @Override
    protected boolean useCustomReporter() {
        return false;
    }

}
