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

package com.qlangtech.tis.plugin.ds.cassandra;

import com.alibaba.citrus.turbine.Context;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.collect.Lists;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang.StringUtils;

import java.net.Inet4Address;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-21 10:11
 **/
@Public
public class CassandraDatasourceFactory extends DataSourceFactory {

    public static final String DATAX_NAME = "Cassandra";

    @FormField(identity = true, ordinal = 0, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.identity})
    public String name;

    /**
     * 节点描述
     */
    @FormField(ordinal = 1, type = FormFieldType.TEXTAREA, validate = {Validator.require})
    public String nodeDesc;

    @FormField(ordinal = 3, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer})
    public int port;

    @FormField(ordinal = 5, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.db_col_name})
    public String userName;

    @FormField(ordinal = 7, type = FormFieldType.PASSWORD, validate = {})
    public String password;

    // 数据库名称
    @FormField(ordinal = 9, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.identity})
    public String dbName;


    @FormField(ordinal = 10, type = FormFieldType.ENUM, validate = {Validator.identity})
    public Boolean useSSL;

    @Override
    public String identityValue() {
        return this.name;
    }

    @Override
    public DataDumpers getDataDumpers(TISTable table) {
        List<String> jdbcUrls = Lists.newArrayList();
        for (String host : this.getHosts()) {
            jdbcUrls.add(host);
        }
        return DataDumpers.create(jdbcUrls, table);
    }

    @Override
    public List<ColumnMetaData> getTableMetadata(EntityName table) {
        List<ColumnMetaData> colsMeta = Lists.newArrayList();
        AtomicInteger index = new AtomicInteger();
        processSession((session) -> {
            ColumnMetaData cmeta = null;
            ResultSet resultSet = session.execute(
                    "SELECT column_name,type FROM system_schema.columns WHERE keyspace_name = '"
                            + this.dbName + "' AND table_name = '" + table.getTabName() + "'");
            Iterator<Row> rows = resultSet.iterator();
            Row row = null;
            while (rows.hasNext()) {
                row = rows.next();
                //int index, String key, int type, boolean pk
                cmeta = new ColumnMetaData(index.getAndIncrement(), row.getString(0)
                        , new DataType(convertType(row.getString(1))), false, true);
                // tables.add(row.getString(0));
                colsMeta.add(cmeta);
            }
        });

        return colsMeta;
    }

    private int convertType(String type) {
        switch (type) {
            case "int":
            case "tinyint":
            case "smallint":
            case "varint":
            case "bigint":
            case "time":
                return Types.BIGINT;
            case "float":
            case "double":
            case "decimal":
                return Types.DOUBLE;
            case "ascii":
            case "varchar":
            case "text":
            case "uuid":
            case "timeuuid":
            case "duration":
            case "list":
            case "map":
            case "set":
            case "tuple":
            case "udt":
            case "inet":
                return Types.VARCHAR;
            case "date":
            case "timestamp":
                return Types.DATE;
            case "bool":
                return Types.BOOLEAN;
            case "blob":
                return Types.BLOB;
        }
        throw new IllegalStateException("illegal:" + type);
    }

    @Override
    public List<String> getTablesInDB() {
        List<String> tables = Lists.newArrayList();
        processSession((session) -> {
            ResultSet resultSet = session.execute("SELECT table_name FROM system_schema.tables WHERE keyspace_name = '" + this.dbName + "' ");
            Iterator<Row> rows = resultSet.iterator();
            Row row = null;
            while (rows.hasNext()) {
                row = rows.next();
                tables.add(row.getString(0));
            }
        });
        return tables;
    }

    private void processSession(ISessionVisit sessionVisit) {
        Cluster cluster = null;
        Session session = null;
        if (StringUtils.isNotEmpty(this.userName)) {
            Cluster.Builder clusterBuilder = Cluster.builder().withCredentials(userName, password).withPort(this.port).addContactPoints(getHosts());
            if (useSSL != null && useSSL) {
                clusterBuilder = clusterBuilder.withSSL();
            }
            cluster = clusterBuilder.build();
        } else {
            cluster = Cluster.builder().withPort(this.port).addContactPoints(getHosts()).build();
        }
        session = cluster.connect(dbName);

        try {
            sessionVisit.visit(session);
        } finally {
            session.close();
            cluster.close();
        }

    }

    interface ISessionVisit {
        void visit(Session session);
    }


    public String[] getHosts() {
        Objects.requireNonNull(nodeDesc, "nodeDesc can not be null");
        return StringUtils.split(nodeDesc, ",");
    }


    @Override
    public Connection getConnection(String jdbcUrl) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @TISExtension
    public static class CassandraDatasourceDescriptor extends BaseDataSourceFactoryDescriptor {
        @Override
        protected String getDataSourceName() {
            return DATAX_NAME;
        }

        @Override
        protected boolean supportFacade() {
            return false;
        }

        public boolean validateNodeDesc(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {

            String[] hosts = StringUtils.split(value, ",");
            try {
                for (String host : hosts) {
                    Inet4Address.getByName(host);
                }
            } catch (Throwable e) {
                msgHandler.addFieldError(context, fieldName, e.getMessage());
                return false;
            }
            return true;
        }
    }

}
