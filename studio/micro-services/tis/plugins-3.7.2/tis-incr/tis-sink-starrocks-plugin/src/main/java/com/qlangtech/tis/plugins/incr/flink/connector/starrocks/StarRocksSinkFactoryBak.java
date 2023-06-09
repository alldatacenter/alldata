///**
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.qlangtech.tis.plugins.incr.flink.connector.starrocks;
//
//import com.alibaba.citrus.turbine.Context;
//import com.google.common.collect.Lists;
//import com.google.common.collect.Maps;
//import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
//import com.qlangtech.tis.compiler.streamcode.CompileAndPackage;
//import com.qlangtech.tis.datax.IDataXPluginMeta;
//import com.qlangtech.tis.datax.IDataxProcessor;
//import com.qlangtech.tis.datax.IDataxReader;
//import com.qlangtech.tis.extension.PluginWrapper;
//import com.qlangtech.tis.extension.TISExtension;
//import com.qlangtech.tis.manage.common.Config;
//import com.qlangtech.tis.manage.common.Option;
//import com.qlangtech.tis.plugin.annotation.FormField;
//import com.qlangtech.tis.plugin.annotation.FormFieldType;
//import com.qlangtech.tis.plugin.annotation.Validator;
//import com.qlangtech.tis.plugin.datax.BasicDorisStarRocksWriter;
//import com.qlangtech.tis.plugin.datax.starrocks.DataXStarRocksWriter;
//import com.qlangtech.tis.plugin.ds.DBConfig;
//import com.qlangtech.tis.plugin.ds.DataType;
//import com.qlangtech.tis.plugin.ds.ISelectedTab;
//import com.qlangtech.tis.plugin.ds.doris.DorisSourceFactory;
//import com.qlangtech.tis.plugin.ds.starrocks.StarRocksSourceFactory;
//import com.qlangtech.tis.realtime.BasicTISSinkFactory;
//import com.qlangtech.tis.realtime.TabSinkFunc;
//import com.qlangtech.tis.realtime.transfer.DTO;
//import com.qlangtech.tis.realtime.transfer.UnderlineUtils;
//import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
//import com.qlangtech.tis.utils.TisMetaProps;
//import com.starrocks.connector.flink.StarRocksSink;
//import com.starrocks.connector.flink.row.sink.StarRocksSinkOP;
//import com.starrocks.connector.flink.table.sink.StarRocksSinkOptions;
//import com.starrocks.connector.flink.table.sink.StarRocksSinkSemantic;
//import org.apache.commons.collections.MapUtils;
//import org.apache.commons.lang.StringUtils;
//import org.apache.flink.annotation.Public;
//import org.apache.flink.configuration.ConfigOption;
//import org.apache.flink.configuration.description.BlockElement;
//import org.apache.flink.configuration.description.TextElement;
//import org.apache.flink.streaming.api.functions.sink.SinkFunction;
//import org.apache.flink.table.api.DataTypes;
//import org.apache.flink.table.api.TableSchema;
//import org.apache.flink.table.types.AtomicDataType;
//import org.apache.flink.table.types.logical.*;
//
//import java.lang.reflect.Field;
//import java.util.*;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.stream.Collectors;
//
//import static com.starrocks.connector.flink.table.sink.StarRocksSinkOptions.*;
//
///**
// * https://docs.starrocks.com/zh-cn/main/loading/Flink-connector-starrocks
// *
// * @author: 百岁（baisui@qlangtech.com）
// * @create: 2021-10-31 20:11
// **/
//@Public
//public class StarRocksSinkFactory extends BasicTISSinkFactory<DTO> {
//
//    public static final String DISPLAY_NAME_FLINK_CDC_SINK = "Flink-StarRocks-Sink";
//    private static final int DEFAULT_PARALLELISM = 1;
//
//    @FormField(ordinal = 0, type = FormFieldType.ENUM, validate = Validator.require)
//    public String sinkSemantic;
//
//    @FormField(ordinal = 1, type = FormFieldType.INT_NUMBER, validate = {})
//    public Integer sinkConnectTimeout;
//
//    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER, validate = {})
//    public Long sinkBatchMaxSize;
//
//    @FormField(ordinal = 3, type = FormFieldType.INT_NUMBER, validate = {})
//    public Long sinkBatchMaxRows;
//
//    @FormField(ordinal = 4, type = FormFieldType.INT_NUMBER, validate = {})
//    public Long sinkBatchFlushInterval;
//
//    @FormField(ordinal = 5, type = FormFieldType.INT_NUMBER, validate = {})
//    public Long sinkMaxRetries;
//
//
//    public static List<Option> allSinkSemantic() {
//        return Arrays.stream(StarRocksSinkSemantic.values())
//                .map((s) -> new Option(StringUtils.capitalize(s.getName()), s.getName()))
//                .collect(Collectors.toList());
//    }
//
//    private static ConfigOption cfg(String cfgField) {
//        try {
//            cfgField = StringUtils.upperCase(UnderlineUtils.addUnderline(cfgField).toString());
//            Field field = StarRocksSinkOptions.class.getField(cfgField);
//            return (ConfigOption) field.get(null);
//        } catch (Exception e) {
//            throw new RuntimeException("field:" + cfgField, e);
//        }
//    }
//
//    public static String dft(String cfgField) {
//        return String.valueOf(cfg(cfgField).defaultValue());
//    }
//
//    public static String desc(String cfgField) {
//        List<BlockElement> blocks = cfg(cfgField).description().getBlocks();
//        for (BlockElement element : blocks) {
//            return ((TextElement) element).getFormat();
//        }
//        return StringUtils.EMPTY;
//    }
//
//    @Override
//    public ICompileAndPackage getCompileAndPackageManager() {
//        TisMetaProps metaProps = Config.getMetaProps();
//        return new CompileAndPackage(Lists.newArrayList(
//                new PluginWrapper.Dependency("tis-sink-starrocks-plugin", metaProps.getVersion(), false)
//                , new PluginWrapper.Dependency("tis-datax-doris-plugin", metaProps.getVersion(), false)));
//    }
//
//    @Override
//    public Map<TableAlias, TabSinkFunc<DTO>> createSinkFunction(IDataxProcessor dataxProcessor) {
//
//        Map<TableAlias, TabSinkFunc<DTO>> sinkFuncs = Maps.newHashMap();
//        TableAlias tableName = null;
//        // Map<String, TableAlias> tabAlias = dataxProcessor.getTabAlias();
//        DataXStarRocksWriter dataXWriter = (DataXStarRocksWriter) dataxProcessor.getWriter(null);
//        Objects.requireNonNull(dataXWriter, "dataXWriter can not be null");
//
//        BasicDorisStarRocksWriter.Separator separator = dataXWriter.getSeparator();
//
//        IDataxReader reader = dataxProcessor.getReader(null);
//        List<ISelectedTab> tabs = reader.getSelectedTabs();
//
//
//        //  StarRocksSourceFactory dsFactory =
//        StarRocksSourceFactory dsFactory = dataXWriter.getDataSourceFactory();
//        DBConfig dbConfig = dsFactory.getDbConfig();
//
//
//        Map<String, TableAlias> selectedTabs = dataxProcessor.getTabAlias();
//        if (MapUtils.isEmpty(selectedTabs)) {
//            throw new IllegalStateException("selectedTabs can not be empty");
//        }
//
//        for (Map.Entry<String, TableAlias> tabAliasEntry : selectedTabs.entrySet()) {
//            tableName = tabAliasEntry.getValue();
//
//            Objects.requireNonNull(tableName, "tableName can not be null");
//            if (StringUtils.isEmpty(tableName.getFrom())) {
//                throw new IllegalStateException("tableName.getFrom() can not be empty");
//            }
//
//            AtomicReference<SinkFunction<DTO>> sinkFuncRef = new AtomicReference<>();
//            final TableAlias tabName = tableName;
//            AtomicReference<Object[]> exceptionLoader = new AtomicReference<>();
//            final String targetTabName = tableName.getTo();
//            dbConfig.vistDbURL(false, (dbName, dbHost, jdbcUrl) -> {
//                try {
//                    Optional<ISelectedTab> selectedTab = tabs.stream()
//                            .filter((tab) -> StringUtils.equals(tabName.getFrom(), tab.getName())).findFirst();
//                    if (!selectedTab.isPresent()) {
//                        throw new IllegalStateException("target table:" + tabName.getFrom()
//                                + " can not find matched table in:["
//                                + tabs.stream().map((t) -> t.getName()).collect(Collectors.joining(",")) + "]");
//                    }
//                    /**
//                     * 需要先初始化表starrocks目标库中的表
//                     */
//                    dataXWriter.initWriterTable(targetTabName, Collections.singletonList(jdbcUrl));
//
//                    sinkFuncRef.set(createSinkFunction(dbName, targetTabName, selectedTab.get(), jdbcUrl, dsFactory, separator));
//
//                } catch (Throwable e) {
//                    exceptionLoader.set(new Object[]{jdbcUrl, e});
//                }
//            });
//            if (exceptionLoader.get() != null) {
//                Object[] error = exceptionLoader.get();
//                throw new RuntimeException((String) error[0], (Throwable) error[1]);
//            }
//            Objects.requireNonNull(sinkFuncRef.get(), "sinkFunc can not be null");
//            DTOSinkFunc sinkFunc = new DTOSinkFunc(tableName, sinkFuncRef.get(), true, DEFAULT_PARALLELISM);
////            sinkFunc.setSourceFilter("removeUpdateBeforeEvent", new FilterUpdateBeforeEvent());
//            sinkFuncs.put(tableName, sinkFunc);
//        }
//
//        if (sinkFuncs.size() < 1) {
//            throw new IllegalStateException("size of sinkFuncs can not be small than 1");
//        }
//        return sinkFuncs;
//    }
//
//    private SinkFunction<DTO> createSinkFunction(
//            String dbName, final String targetTabName, ISelectedTab tab, String jdbcUrl
//            , DorisSourceFactory dsFactory, BasicDorisStarRocksWriter.Separator separator) {
////import org.apache.flink.table.types.DataType;
//        TableSchema.Builder schemaBuilder = TableSchema.builder();
//        String[] fieldKeys = new String[tab.getCols().size()];
//        if (fieldKeys.length < 1) {
//            throw new IllegalArgumentException("fieldKeys.length can not small than 1");
//        }
//        int index = 0;
//        List<String> pks = Lists.newArrayList();
//        for (CMeta cm : tab.getCols()) {
//            if (cm.isPk()) {
//                pks.add(cm.getName());
//            }
//            schemaBuilder.field(cm.getName(), mapFlinkColType(cm.isPk(), cm.getType()));
//            fieldKeys[index++] = cm.getName();
//        }
//        if (!pks.isEmpty()) {
//            schemaBuilder.primaryKey(pks.toArray(new String[pks.size()]));
//        }
//
//
//        return StarRocksSink.sink(
//                // the table structure
//                schemaBuilder.build(),
//                // the sink options
//                createRocksSinkOptions(dbName, targetTabName, jdbcUrl, dsFactory, separator)
//                // set the slots with streamRowData
//                , (slots, streamRowData) -> {
//                    for (int i = 0; i < fieldKeys.length; i++) {
//                        slots[i] = (DTO.EventType.DELETE == streamRowData.getEventType())
//                                ? streamRowData.getBefore().get(fieldKeys[i])
//                                : streamRowData.getAfter().get(fieldKeys[i]);
//                    }
//                    StarRocksSinkOP sinkOp = getSinkOP(streamRowData.getEventType());
//                    slots[fieldKeys.length] = sinkOp.ordinal();
//                }
//        );
//    }
//
//    private static StarRocksSinkOP getSinkOP(DTO.EventType evt) {
//        switch (evt) {
//            case DELETE:
//                return StarRocksSinkOP.DELETE;
//            case UPDATE_AFTER:
//            default:
//                return StarRocksSinkOP.UPSERT;
//        }
//    }
//
//    private StarRocksSinkOptions createRocksSinkOptions(String dbName, String targetTabName, String jdbcUrl
//            , DorisSourceFactory dsFactory, BasicDorisStarRocksWriter.Separator separator) {
//        StarRocksSinkOptions.Builder builder = StarRocksSinkOptions.builder()
//                .withProperty(JDBC_URL.key(), jdbcUrl)
//                .withProperty(LOAD_URL.key(), dsFactory.getLoadUrls().stream().collect(Collectors.joining(";")))
//                .withProperty(TABLE_NAME.key(), targetTabName)
//                .withProperty(DATABASE_NAME.key(), dbName)
//                .withProperty(SINK_PROPERTIES_PREFIX + "column_separator", separator.getColumnSeparator())
//                .withProperty(SINK_PROPERTIES_PREFIX + "row_delimiter", separator.getRowDelimiter())
//                .withProperty(SINK_SEMANTIC.key(), StarRocksSinkSemantic.fromName(this.sinkSemantic).getName())
//                .withProperty(USERNAME.key(), dsFactory.getUserName());
//
//
//        if (this.sinkConnectTimeout != null) {
//            builder.withProperty(SINK_CONNECT_TIMEOUT.key(), String.valueOf(this.sinkConnectTimeout));
//        }
//        if (this.sinkBatchMaxSize != null) {
//            builder.withProperty(SINK_BATCH_MAX_SIZE.key(), String.valueOf(this.sinkBatchMaxSize));
//        }
//        if (this.sinkBatchMaxRows != null) {
//            builder.withProperty(SINK_BATCH_MAX_ROWS.key(), String.valueOf(this.sinkBatchMaxRows));
//        }
//        if (this.sinkBatchFlushInterval != null) {
//            builder.withProperty(SINK_BATCH_FLUSH_INTERVAL.key(), String.valueOf(this.sinkBatchFlushInterval));
//        }
//        if (this.sinkMaxRetries != null) {
//            builder.withProperty(SINK_MAX_RETRIES.key(), String.valueOf(this.sinkMaxRetries));
//        }
//
//        //if (StringUtils.isNotEmpty(dsFactory.getPassword())) {
//        builder.withProperty(PASSWORD.key(), StringUtils.trimToEmpty(dsFactory.getPassword()));
//        //}
//
//        StarRocksSinkOptions sinkOptions = builder.build();
//        sinkOptions.enableUpsertDelete();
//        return sinkOptions;
//    }
//
//    private static org.apache.flink.table.types.DataType mapFlinkColType(boolean pk, com.qlangtech.tis.plugin.ds.DataType type) {
//        if (type == null) {
//            throw new IllegalArgumentException("param type can not be null");
//        }
//        final boolean isNullable = !pk;
//        return type.accept(new com.qlangtech.tis.plugin.ds.DataType.TypeVisitor<org.apache.flink.table.types.DataType>() {
//            @Override
//            public org.apache.flink.table.types.DataType intType(DataType type) {
//                //          return DataTypes.INT();
//                return new AtomicDataType(new IntType(isNullable));
//            }
//
//            @Override
//            public org.apache.flink.table.types.DataType bigInt(DataType type) {
//                //return DataTypes.BIGINT();
//                return new AtomicDataType(new BigIntType(isNullable));
//            }
//
//            @Override
//            public org.apache.flink.table.types.DataType doubleType(DataType type) {
//                // return DataTypes.DOUBLE();
//                return new AtomicDataType(new DoubleType(isNullable));
//            }
//
//            @Override
//            public org.apache.flink.table.types.DataType dateType(DataType type) {
//                // return DataTypes.DATE();
//                return new AtomicDataType(new DateType(isNullable));
//            }
//
//            @Override
//            public org.apache.flink.table.types.DataType timestampType(DataType type) {
//                //return DataTypes.TIMESTAMP();
//                return new AtomicDataType(new TimestampType(isNullable, 6));
//            }
//
//            @Override
//            public org.apache.flink.table.types.DataType bitType(DataType type) {
//                return DataTypes.BOOLEAN();
//            }
//
//            @Override
//            public org.apache.flink.table.types.DataType blobType(DataType type) {
//                // return DataTypes.VARBINARY(type.columnSize);
//                return varcharType(type);
//            }
//
//            @Override
//            public org.apache.flink.table.types.DataType varcharType(DataType type) {
//                //return DataTypes.VARCHAR(type.columnSize);
//                return new AtomicDataType(new VarCharType(isNullable, type.columnSize));
//            }
//        });
//    }
//
////    @TISExtension
////    public static class DefaultSinkFunctionDescriptor extends BaseSinkFunctionDescriptor {
////        @Override
////        public String getDisplayName() {
////            return DISPLAY_NAME_FLINK_CDC_SINK;
////        }
////
////        public boolean validateColumnSeparator(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
////            return validateRowDelimiter(msgHandler, context, fieldName, value);
////        }
////
////        public boolean validateRowDelimiter(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
//////            if (StringUtils.length(StringEscapeUtils.unescapeJava(value)) != 1) {
//////                msgHandler.addFieldError(context, fieldName, "分隔符长度必须为1");
//////                return false;
//////            }
////            return true;
////        }
////
////        @Override
////        protected IEndTypeGetter.EndType getTargetType() {
////            return IEndTypeGetter.EndType.StarRocks;
////        }
////    }
//}
