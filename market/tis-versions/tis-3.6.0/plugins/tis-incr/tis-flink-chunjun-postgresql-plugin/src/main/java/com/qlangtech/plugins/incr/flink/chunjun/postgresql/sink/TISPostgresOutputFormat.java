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

package com.qlangtech.plugins.incr.flink.chunjun.postgresql.sink;

import com.dtstack.chunjun.connector.jdbc.converter.JdbcColumnConverter;
import com.dtstack.chunjun.connector.postgresql.sink.PostgresOutputFormat;
import com.dtstack.chunjun.converter.ISerializationConverter;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import com.qlangtech.tis.plugins.incr.flink.chunjun.common.DialectUtils;
import org.apache.flink.connector.jdbc.statement.FieldNamedPreparedStatement;
import org.apache.flink.table.data.RowData;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-14 16:56
 **/
public class TISPostgresOutputFormat extends PostgresOutputFormat {
    private final DataSourceFactory dsFactory;

    public TISPostgresOutputFormat(DataSourceFactory dsFactory, Map<String, IColMetaGetter> cols) {
        super(cols);
        if (dsFactory == null) {
            throw new IllegalArgumentException("param dsFactory can not be null");
        }
        this.dsFactory = dsFactory;
    }

//    @Override
//    protected Map<String, IColMetaGetter> getTableMetaData() {
//        return ColMetaUtils.getColMetasMap(this.dsFactory, this.dbConn, this.jdbcConf);
//    }


    @Override
    protected void initializeRowConverter() {
        //  super.initializeRowConverter();
//        setRowConverter(
//                rowConverter == null
//                        ? jdbcDialect.getColumnConverter(rowType, jdbcConf)
//                        : rowConverter);

        this.setRowConverter(DialectUtils.createColumnConverter(jdbcDialect, jdbcConf, this.colsMeta, JdbcColumnConverter::getRowDataValConverter
                , (flinkCol) -> {
                    ISerializationConverter<FieldNamedPreparedStatement> statementSetter
                            = JdbcColumnConverter.createJdbcStatementValConverter(flinkCol.type.getLogicalType(), flinkCol.getRowDataValGetter());
                    // pg 的bit类型设置比较特殊
                    ISerializationConverter<FieldNamedPreparedStatement> fix = flinkCol.colType.accept(new PGTypeVisitor(flinkCol.getRowDataValGetter()));
                    if (fix != null) {
                        return fix;
                    }
                    return statementSetter;
                }
        ));

    }

    @Override
    protected Connection getConnection() throws SQLException {
        DataSourceFactory dsFactory = Objects.requireNonNull(this.dsFactory, "dsFactory can not be null");
        return dsFactory.getConnection(this.jdbcConf.getJdbcUrl());
    }


    static class PGTypeVisitor implements DataType.TypeVisitor<ISerializationConverter<FieldNamedPreparedStatement>> {

        private RowData.FieldGetter fieldGetter;

        public PGTypeVisitor(RowData.FieldGetter fieldGetter) {
            this.fieldGetter = fieldGetter;
        }

        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> bitType(DataType type) {

            //  try {
//                final org.postgresql.util.PGobject bit1 = new org.postgresql.util.PGobject();
//                bit1.setType("bit");
//                bit1.setValue("1");
//                final org.postgresql.util.PGobject bit0 = new org.postgresql.util.PGobject();
//                bit0.setType("bit");
//                bit0.setValue("0");


            return new ISerializationConverter<FieldNamedPreparedStatement>() {
                @Override
                public void serialize(RowData rowData, int pos, FieldNamedPreparedStatement output) throws Exception {
                    byte v = (byte) fieldGetter.getFieldOrNull(rowData);
                    output.setString(pos, v > 0 ? "1" : "0");
                }
            };
//            } catch (SQLException e) {
//                throw new RuntimeException(e);
//            }
        }

        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> bigInt(DataType type) {
            return null;
        }

        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> doubleType(DataType type) {
            return null;
        }

        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> dateType(DataType type) {
            return null;
        }

        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> timestampType(DataType type) {
            return null;
        }


        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> blobType(DataType type) {
            return null;
        }

        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> varcharType(DataType type) {
            return null;
        }

        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> intType(DataType type) {
            return null;
        }

        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> floatType(DataType type) {
            return null;
        }

        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> decimalType(DataType type) {
            return null;
        }

        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> timeType(DataType type) {
            return null;
        }

        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> tinyIntType(DataType dataType) {
            return null;
        }

        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> smallIntType(DataType dataType) {
            return null;
        }

        @Override
        public ISerializationConverter<FieldNamedPreparedStatement> boolType(DataType dataType) {
            return null;
        }
    }
}
