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

import com.alibaba.datax.common.ck.ClickHouseCommon;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.IDataxContext;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.clickhouse.ClickHouseDataSourceFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * https://github.com/alibaba/DataX/blob/master/clickhousewriter/src/main/resources/plugin_job_template.json
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-16 21:48
 * @see com.alibaba.datax.plugin.writer.clickhousewriter.TISClickhouseWriter
 **/
@Public
public class DataXClickhouseWriter extends BasicDataXRdbmsWriter<ClickHouseDataSourceFactory>
        implements KeyedPluginStore.IPluginKeyAware {

    public static final String DATAX_NAME = "ClickHouse";

    @FormField(ordinal = 8, type = FormFieldType.INT_NUMBER, validate = {Validator.require})
    public Integer batchByteSize;

    @FormField(ordinal = 9, type = FormFieldType.ENUM, validate = {Validator.require})
    public String writeMode;


//    @FormField(ordinal = 79, type = FormFieldType.TEXTAREA, validate = {Validator.require})
//    public String template;

    // public String dataXName;

//    @Override
//    public void initWriterTable(String targetTabName, List<String> jdbcUrls) throws Exception {
//        InitWriterTable.process(this.dataXName, (BasicDataXRdbmsWriter) this, targetTabName, jdbcUrls);
//    }

    @Override
    public CreateTableSqlBuilder.CreateDDL generateCreateDDL(IDataxProcessor.TableMap tableMapper) {
//        if (!this.autoCreateTable) {
//            return null;
//        }
        final CreateTableSqlBuilder createTableSqlBuilder = new CreateTableSqlBuilder(tableMapper, this.getDataSourceFactory()) {
            @Override
            protected void appendExtraColDef(List<ColWrapper> pks) {
                script.append("   ,`" + ClickHouseCommon.KEY_CLICKHOUSE_CK + "` Int8 DEFAULT 1").append("\n");
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

            @Override
            protected void appendTabMeta(List<ColWrapper> pk) {
                script.append(" ENGINE = CollapsingMergeTree(" + ClickHouseCommon.KEY_CLICKHOUSE_CK + ")").append("\n");
                if (CollectionUtils.isNotEmpty(pk)) {
                    script.append(" ORDER BY ").append(pk.stream().map((p) -> "`" + p.getName() + "`").collect(Collectors.joining(","))).append("\n");
                }
                script.append(" SETTINGS index_granularity = 8192");
            }

            private String convertType(CMeta col) {
                DataType type = col.getType();
                switch (type.type) {
                    case Types.INTEGER:
                    case Types.TINYINT:
                    case Types.SMALLINT:
                        return "Int32";
                    case Types.BIGINT:
                        return "Int64";
                    case Types.FLOAT:
                        return "Float32";
                    case Types.DOUBLE:
                    case Types.DECIMAL:
                        return "Float64";
                    case Types.DATE:
                        return "Date";
                    case Types.TIME:
                    case Types.TIMESTAMP:
                        return "DateTime";
                    case Types.BIT:
                    case Types.BOOLEAN:
                        return "UInt8";
                    case Types.BLOB:
                    case Types.BINARY:
                    case Types.LONGVARBINARY:
                    case Types.VARBINARY:
                    default:
                        return "String";
                }
            }
        };
        return createTableSqlBuilder.build();
        // List<ColumnMetaData> tableMetadata = this.getDataSourceFactory().getTableMetadata(tableMapper.getTo());
        //Set<String> pks = tableMetadata.stream().filter((t) -> t.isPk()).map((t) -> t.getName()).collect(Collectors.toSet());

//        StringBuffer script = new StringBuffer();
//        script.append("CREATE TABLE ").append(tableMapper.getTo()).append("\n");
//        script.append("(\n");
//        CMeta pk = null;
//        int maxColNameLength = 0;
//        for (CMeta col : tableMapper.getSourceCols()) {
//            int m = StringUtils.length(col.getName());
//            if (m > maxColNameLength) {
//                maxColNameLength = m;
//            }
//        }
//        maxColNameLength += 4;
//        for (CMeta col : tableMapper.getSourceCols()) {
//            if (pk == null && col.isPk()) {
//                pk = col;
//            }
//            script.append("    `").append(String.format("%-" + (maxColNameLength) + "s", col.getName() + "`"))
//                    .append(convert2ClickhouseType(col.getType())).append(",").append("\n");
//        }
//        script.append("    `__cc_ck_sign` Int8 DEFAULT 1").append("\n");
//        script.append(")\n");
//        script.append(" ENGINE = CollapsingMergeTree(__cc_ck_sign)").append("\n");
//        // Objects.requireNonNull(pk, "pk can not be null");
//        if (pk != null) {
//            script.append(" ORDER BY `").append(pk.getName()).append("`\n");
//        }
//        script.append(" SETTINGS index_granularity = 8192");
//        CREATE TABLE tis.customer_order_relation
//                (
//                        `customerregister_id` String,
//                        `waitingorder_id` String,
//                        `worker_id` String,
//                        `kind` Int8,
//                        `create_time` Int64,
//                        `last_ver` Int8,
//                        `__cc_ck_sign` Int8 DEFAULT 1
//                )
//        ENGINE = CollapsingMergeTree(__cc_ck_sign)
//        ORDER BY customerregister_id
//        SETTINGS index_granularity = 8192
        //     return script;
    }


//    @Override
//    public void setKey(KeyedPluginStore.Key key) {
//        this.dataXName = key.keyVal.getVal();
//    }

    @Override
    public ClickHouseDataSourceFactory getDataSourceFactory() {
        return super.getDataSourceFactory();
    }

    @Override
    public IDataxContext getSubTask(Optional<IDataxProcessor.TableMap> tableMap) {
        if (!tableMap.isPresent()) {
            throw new IllegalArgumentException("tableMap shall be present");
        }
        IDataxProcessor.TableMap tmapper = tableMap.get();
        ClickHouseDataSourceFactory ds = this.getDataSourceFactory();

        ClickHouseWriterContext context = new ClickHouseWriterContext();
        context.setDataXName(this.dataXName);
        context.setBatchByteSize(this.batchByteSize);
        context.setBatchSize(this.batchSize);
        for (String jdbcUrl : ds.getJdbcUrls()) {
            context.setJdbcUrl(jdbcUrl);
            break;
        }
        if (StringUtils.isEmpty(context.getJdbcUrl())) {
            throw new IllegalStateException("jdbcUrl can not be null");
        }
        context.setUsername(ds.getUserName());
        context.setPassword(ds.password);
        context.setTable(tmapper.getTo());
        context.setWriteMode(this.writeMode);
        //  PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}");
        //  Map<String, String> params = new HashMap<>();
        //params.put("table", tmapper.getTo());
//        PropertyPlaceholderHelper.PlaceholderResolver resolver = (key) -> {
//            return params.get(key);
//        };
        if (StringUtils.isNotEmpty(this.preSql)) {
            // context.setPreSql(helper.replacePlaceholders(this.preSql, resolver));
            context.setPreSql(this.preSql);
        }

        if (StringUtils.isNotEmpty(this.postSql)) {
            // context.setPostSql(helper.replacePlaceholders(this.postSql, resolver));
            context.setPostSql(this.postSql);
        }
        context.setCols(IDataxProcessor.TabCols.create(tableMap.get()));
        return context;
    }


    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(
                DataXClickhouseWriter.class, "DataXClickhouseWriter-tpl.json");
    }

//    public ClickHouseDataSourceFactory getDataSourceFactory() {
//        DataSourceFactoryPluginStore dsStore = TIS.getDataBasePluginStore(new PostedDSProp(this.dbName));
//        return dsStore.getDataSource();
//    }


    @TISExtension()
    public static class DefaultDescriptor extends RdbmsWriterDescriptor {
        public DefaultDescriptor() {
            super();
        }

        @Override
        protected int getMaxBatchSize() {
            return 3072;
        }

        @Override
        public boolean isSupportIncr() {
            return true;
        }

        @Override
        public EndType getEndType() {
            return EndType.Clickhouse;
        }

        @Override
        public boolean isSupportTabCreate() {
            return true;
        }

        @Override
        public String getDisplayName() {
            return DATAX_NAME;
        }
    }
}
