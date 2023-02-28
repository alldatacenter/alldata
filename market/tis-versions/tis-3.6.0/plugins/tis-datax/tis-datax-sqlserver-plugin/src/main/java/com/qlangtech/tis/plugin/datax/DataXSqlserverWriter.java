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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.IDataxContext;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.sqlserver.SqlServerDatasourceFactory;

import java.sql.Types;
import java.util.List;
import java.util.Optional;

/**
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 * @see com.alibaba.datax.plugin.writer.sqlserverwriter.SqlServerWriter
 **/
@Public
public class DataXSqlserverWriter extends BasicDataXRdbmsWriter<SqlServerDatasourceFactory> {

    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXSqlserverWriter.class, "DataXSqlserverWriter-tpl.json");
    }

//    @Override
//    public void initWriterTable(String targetTabName, List<String> jdbcUrls) throws Exception {
//        InitWriterTable.process(this.dataXName, targetTabName, jdbcUrls);
//    }

    @Override
    public IDataxContext getSubTask(Optional<IDataxProcessor.TableMap> tableMap) {
        if (!tableMap.isPresent()) {
            throw new IllegalArgumentException("param tableMap shall be present");
        }
        SqlServerWriterContext writerContext = new SqlServerWriterContext(this, tableMap.get());
        return writerContext;
    }


    @Override
    public CreateTableSqlBuilder.CreateDDL generateCreateDDL(IDataxProcessor.TableMap tableMapper) {
        if (!this.autoCreateTable) {
            return null;
        }
        // https://www.cnblogs.com/mingfei200169/articles/427591.html
        final CreateTableSqlBuilder createTableSqlBuilder = new CreateTableSqlBuilder(tableMapper, this.getDataSourceFactory()) {

            private String convertType(CMeta col) {
                //https://www.cnblogs.com/liberty777/p/10748570.html
                StringBuffer createSql = new StringBuffer(getSqlServerType(col));
                if (col.isPk()) {
                    createSql.append(" primary key ");
                }
                return createSql.toString();
            }

            @Override
            protected ColWrapper createColWrapper(CMeta c) {
                return new ColWrapper(c) {
                    @Override
                    public String getMapperType() {
                        return convertType(this.meta);
                    }
                };
            }

            private String getSqlServerType(CMeta col) {
                DataType type = col.getType();
                switch (type.type) {
                    case Types.INTEGER:
                    case Types.TINYINT:
                    case Types.SMALLINT:
                        return "int";
                    case Types.BIGINT:
                        return "bigint";
                    case Types.FLOAT:
                    case Types.DOUBLE:
                    case Types.DECIMAL:
                        return "decimal(8,4)";
                    case Types.DATE:
                    case Types.TIME:
                    case Types.TIMESTAMP:
                        return "datetime";
                    case Types.BIT:
                    case Types.BOOLEAN:
                        return "bit";
                    case Types.BLOB:
                    case Types.BINARY:
                    case Types.LONGVARBINARY:
                    case Types.VARBINARY:
                        return "varbinary(" + type.columnSize + ")";
                    default:
                        return "varchar(" + type.columnSize + ")";
                }
            }


            @Override
            protected void appendExtraColDef(List<ColWrapper> pk) {

            }

            @Override
            protected void appendTabMeta(List<ColWrapper> pk) {

            }
        };

        return createTableSqlBuilder.build();
    }

    @TISExtension()
    public static class DefaultDescriptor extends RdbmsWriterDescriptor {
        public DefaultDescriptor() {
            super();
        }

        @Override
        public boolean isSupportIncr() {
            return false;
        }

        @Override
        public EndType getEndType() {
            return EndType.SqlServer;
        }

        @Override
        public boolean isSupportTabCreate() {
            return true;
        }

        @Override
        public String getDisplayName() {
            return DataXSqlserverReader.DATAX_NAME;
        }
    }
}
