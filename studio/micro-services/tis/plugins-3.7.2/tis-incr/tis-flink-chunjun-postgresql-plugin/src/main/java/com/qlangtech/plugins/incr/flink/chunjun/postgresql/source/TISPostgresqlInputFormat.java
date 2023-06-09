/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.plugins.incr.flink.chunjun.postgresql.source;

import com.dtstack.chunjun.connector.jdbc.TableCols;
import com.dtstack.chunjun.connector.postgresql.converter.PostgresqlColumnConverter;
import com.dtstack.chunjun.connector.postgresql.source.PostgresqlInputFormat;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import com.qlangtech.tis.plugins.incr.flink.chunjun.common.ColMetaUtils;
import com.qlangtech.tis.plugins.incr.flink.chunjun.common.DialectUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-29 13:05
 **/
public final class TISPostgresqlInputFormat extends PostgresqlInputFormat {
    private final DataSourceFactory dataSourceFactory;

    private final TableCols tabCols;

    public TISPostgresqlInputFormat(DataSourceFactory dataSourceFactory, List<IColMetaGetter> colsMeta) {
        if (dataSourceFactory == null) {
            throw new IllegalArgumentException("param dataSourceFactory can not be null");
        }
        this.dataSourceFactory = dataSourceFactory;
        this.tabCols = new TableCols(colsMeta);
    }

    @Override
    protected Connection getConnection() throws SQLException {
        return Objects.requireNonNull(dataSourceFactory, "dataSourceFactory can not be null")
                .getConnection(jdbcConf.getJdbcUrl()).getConnection();
    }

    @Override
    protected void initializeRowConverter() {
        this.setRowConverter(DialectUtils.createColumnConverter(
                jdbcDialect, jdbcConf, this.colsMeta, PostgresqlColumnConverter::createInternalConverter
        ));
    }


    @Override
    protected TableCols getTableMetaData() {
        return this.tabCols;// new TableCols(ColMetaUtils.getColMetas(this.dataSourceFactory, this.dbConn, this.jdbcConf));
    }

    @Override
    protected boolean useCustomReporter() {
        return false;//jdbcConf.isIncrement() && jdbcConf.getInitReporter();
    }

}
