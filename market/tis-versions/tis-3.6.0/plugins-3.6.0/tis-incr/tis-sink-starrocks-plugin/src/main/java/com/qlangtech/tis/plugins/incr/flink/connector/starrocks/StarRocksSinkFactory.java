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

package com.qlangtech.tis.plugins.incr.flink.connector.starrocks;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
import com.qlangtech.tis.compiler.streamcode.CompileAndPackage;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.datax.TableAliasMapper;
import com.qlangtech.tis.extension.PluginWrapper;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.BasicDorisStarRocksWriter;
import com.qlangtech.tis.plugin.datax.starrocks.DataXStarRocksWriter;
import com.qlangtech.tis.plugin.ds.DBConfig;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.doris.DorisSourceFactory;
import com.qlangtech.tis.plugin.ds.starrocks.StarRocksSourceFactory;
import com.qlangtech.tis.plugins.incr.flink.cdc.AbstractRowDataMapper;
import com.qlangtech.tis.realtime.BasicTISSinkFactory;
import com.qlangtech.tis.realtime.TabSinkFunc;
import com.qlangtech.tis.realtime.transfer.UnderlineUtils;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.utils.TisMetaProps;
import com.starrocks.connector.flink.StarRocksSink;
import com.starrocks.connector.flink.table.sink.StarRocksSinkOptions;
import com.starrocks.connector.flink.table.sink.StarRocksSinkSemantic;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.annotation.Public;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.description.BlockElement;
import org.apache.flink.configuration.description.TextElement;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.data.RowData;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.starrocks.connector.flink.table.sink.StarRocksSinkOptions.*;

/**
 * https://docs.starrocks.com/zh-cn/main/loading/Flink-connector-starrocks
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-31 20:11
 **/
@Public
public class StarRocksSinkFactory extends BasicTISSinkFactory<RowData> {

    public static final String DISPLAY_NAME_FLINK_CDC_SINK = "Flink-StarRocks-Sink";
    private static final int DEFAULT_PARALLELISM = 1;

    @FormField(ordinal = 0, type = FormFieldType.ENUM, validate = Validator.require)
    public String sinkSemantic;

    @FormField(ordinal = 1, type = FormFieldType.INT_NUMBER, validate = {})
    public Integer sinkConnectTimeout;

    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER, validate = {})
    public Long sinkBatchMaxSize;

    @FormField(ordinal = 3, type = FormFieldType.INT_NUMBER, validate = {})
    public Long sinkBatchMaxRows;

    @FormField(ordinal = 4, type = FormFieldType.INT_NUMBER, validate = {})
    public Long sinkBatchFlushInterval;

    @FormField(ordinal = 5, type = FormFieldType.INT_NUMBER, validate = {})
    public Long sinkMaxRetries;


    public static List<Option> allSinkSemantic() {
        return Arrays.stream(StarRocksSinkSemantic.values())
                .map((s) -> new Option(StringUtils.capitalize(s.getName()), s.getName()))
                .collect(Collectors.toList());
    }

    private static ConfigOption cfg(String cfgField) {
        try {
            cfgField = StringUtils.upperCase(UnderlineUtils.addUnderline(cfgField).toString());
            Field field = StarRocksSinkOptions.class.getField(cfgField);
            return (ConfigOption) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("field:" + cfgField, e);
        }
    }

    public static String dft(String cfgField) {
        return String.valueOf(cfg(cfgField).defaultValue());
    }

    public static String desc(String cfgField) {
        List<BlockElement> blocks = cfg(cfgField).description().getBlocks();
        for (BlockElement element : blocks) {
            return ((TextElement) element).getFormat();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public ICompileAndPackage getCompileAndPackageManager() {
        TisMetaProps metaProps = Config.getMetaProps();
        return new CompileAndPackage(Lists.newArrayList(
                new PluginWrapper.Dependency("tis-sink-starrocks-plugin", metaProps.getVersion(), false)
                , new PluginWrapper.Dependency("tis-datax-doris-starrocks-plugin", metaProps.getVersion(), false)));
    }

    @Override
    public Map<TableAlias, TabSinkFunc<RowData>> createSinkFunction(IDataxProcessor dataxProcessor) {

        Map<TableAlias, TabSinkFunc<RowData>> sinkFuncs = Maps.newHashMap();
        // TableAlias tableName = null;
        // Map<String, TableAlias> tabAlias = dataxProcessor.getTabAlias();
        DataXStarRocksWriter dataXWriter = (DataXStarRocksWriter) dataxProcessor.getWriter(null);
        Objects.requireNonNull(dataXWriter, "dataXWriter can not be null");

        BasicDorisStarRocksWriter.Separator separator = dataXWriter.getSeparator();

        IDataxReader reader = dataxProcessor.getReader(null);
        List<ISelectedTab> tabs = reader.getSelectedTabs();


        StarRocksSourceFactory dsFactory = dataXWriter.getDataSourceFactory();
        DBConfig dbConfig = dsFactory.getDbConfig();


        TableAliasMapper selectedTabs = dataxProcessor.getTabAlias();
        if (selectedTabs.isNull()) {
            throw new IllegalStateException("selectedTabs can not be empty");
        }
        selectedTabs.forEach((key, val) -> {
            TableAlias tableName = val;

            Objects.requireNonNull(tableName, "tableName can not be null");
            if (StringUtils.isEmpty(tableName.getFrom())) {
                throw new IllegalStateException("tableName.getFrom() can not be empty");
            }

            final TableAlias tabName = tableName;
            AtomicReference<Object[]> exceptionLoader = new AtomicReference<>();
            final String targetTabName = tableName.getTo();
            dbConfig.vistDbURL(false, (dbName, dbHost, jdbcUrl) -> {
                try {
                    Optional<ISelectedTab> selectedTab = tabs.stream()
                            .filter((tab) -> StringUtils.equals(tabName.getFrom(), tab.getName())).findFirst();
                    if (!selectedTab.isPresent()) {
                        throw new IllegalStateException("target table:" + tabName.getFrom()
                                + " can not find matched table in:["
                                + tabs.stream().map((t) -> t.getName()).collect(Collectors.joining(",")) + "]");
                    }
                    /**
                     * 需要先初始化表starrocks目标库中的表
                     */
                    dataXWriter.initWriterTable(targetTabName, Collections.singletonList(jdbcUrl));

                    Pair<List<FlinkCol>, List<String>> colsMeta = getColMeta(dsFactory, selectedTab.get());

                    // sinkFuncRef.set();

                    SinkFunction<RowData> sf = createSinkFunction(dbName, targetTabName, colsMeta, jdbcUrl, dsFactory, separator);

                    RowDataSinkFunc sinkFunc = new RowDataSinkFunc(
                            tabName, sf, colsMeta.getLeft(), true, DEFAULT_PARALLELISM);
//            sinkFunc.setSourceFilter("removeUpdateBeforeEvent", new FilterUpdateBeforeEvent());
                    sinkFuncs.put(tabName, sinkFunc);

                } catch (Throwable e) {
                    exceptionLoader.set(new Object[]{jdbcUrl, e});
                }
            });


            if (exceptionLoader.get() != null) {
                Object[] error = exceptionLoader.get();
                throw new RuntimeException((String) error[0], (Throwable) error[1]);
            }
            // Objects.requireNonNull(sinkFuncRef.get(), "sinkFunc can not be null");
        });
//        for (Map.Entry<String, TableAlias> tabAliasEntry : selectedTabs.entrySet()) {
//
//
//        }

        if (sinkFuncs.size() < 1) {
            throw new IllegalStateException("size of sinkFuncs can not be small than 1");
        }
        return sinkFuncs;
    }

    private Pair<List<FlinkCol>, List<String>> getColMeta(
            DorisSourceFactory dsFactory, ISelectedTab tab) {

        List<String> pks = tab.getCols().stream()
                .filter((c) -> c.isPk()).map((c) -> c.getName())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(pks)) {
            throw new IllegalStateException("pks can not be empty");
        }
        List<IColMetaGetter> colsMeta = dsFactory.getTableMetadata(EntityName.parse(tab.getName()))
                .stream().map((col) -> {
                    return IColMetaGetter.create(col.getName(), col.getType(), pks.contains(col.getName()));
                }).collect(Collectors.toList());

        List<FlinkCol> cols = AbstractRowDataMapper.getAllTabColsMeta(colsMeta);
        return Pair.of(cols, pks);
    }

    private SinkFunction<RowData> createSinkFunction(
            String dbName, final String targetTabName, Pair<List<FlinkCol>, List<String>> colsMeta, String jdbcUrl
            , DorisSourceFactory dsFactory, BasicDorisStarRocksWriter.Separator separator) {
//import org.apache.flink.table.types.DataType;
        TableSchema.Builder schemaBuilder = TableSchema.builder();
        List<String> pks = colsMeta.getRight();
        List<FlinkCol> cols = (colsMeta.getLeft());
        String[] fieldKeys = new String[cols.size()];
        if (fieldKeys.length < 1) {
            throw new IllegalArgumentException("fieldKeys.length can not small than 1");
        }
        int index = 0;
        // List<String> pks = Lists.newArrayList();
        for (FlinkCol cm : cols) {
            // schemaBuilder.field(cm.getName(), mapFlinkColType(pks.contains(cm.getName()), cm.getType()));
            schemaBuilder.field(cm.name, cm.type);
            // schemaBuilder.field(cm.getName(), cm.type);
            fieldKeys[index++] = cm.name;
        }
        if (!pks.isEmpty()) {
            schemaBuilder.primaryKey(pks.toArray(new String[pks.size()]));
        }


        return StarRocksSink.sink(
                // the table structure
                schemaBuilder.build(),
                // the sink options
                createRocksSinkOptions(dbName, targetTabName, jdbcUrl, dsFactory, separator)
                // set the slots with streamRowData
                , new TISStarRocksSinkRowBuilder(cols)
        );
    }


    private StarRocksSinkOptions createRocksSinkOptions(String dbName, String targetTabName, String jdbcUrl
            , DorisSourceFactory dsFactory, BasicDorisStarRocksWriter.Separator separator) {
        Builder builder = StarRocksSinkOptions.builder()
                .withProperty(JDBC_URL.key(), jdbcUrl)
                .withProperty(LOAD_URL.key(), dsFactory.getLoadUrls().stream().collect(Collectors.joining(";")))
                .withProperty(TABLE_NAME.key(), targetTabName)
                .withProperty(DATABASE_NAME.key(), dbName)
                .withProperty(SINK_PROPERTIES_PREFIX + "column_separator", separator.getColumnSeparator())
                .withProperty(SINK_PROPERTIES_PREFIX + "row_delimiter", separator.getRowDelimiter())
                .withProperty(SINK_SEMANTIC.key(), StarRocksSinkSemantic.fromName(this.sinkSemantic).getName())
                .withProperty(USERNAME.key(), dsFactory.getUserName());


        if (this.sinkConnectTimeout != null) {
            builder.withProperty(SINK_CONNECT_TIMEOUT.key(), String.valueOf(this.sinkConnectTimeout));
        }
        if (this.sinkBatchMaxSize != null) {
            builder.withProperty(SINK_BATCH_MAX_SIZE.key(), String.valueOf(this.sinkBatchMaxSize));
        }
        if (this.sinkBatchMaxRows != null) {
            builder.withProperty(SINK_BATCH_MAX_ROWS.key(), String.valueOf(this.sinkBatchMaxRows));
        }
        if (this.sinkBatchFlushInterval != null) {
            builder.withProperty(SINK_BATCH_FLUSH_INTERVAL.key(), String.valueOf(this.sinkBatchFlushInterval));
        }
        if (this.sinkMaxRetries != null) {
            builder.withProperty(SINK_MAX_RETRIES.key(), String.valueOf(this.sinkMaxRetries));
        }
        builder.withProperty(PASSWORD.key(), StringUtils.trimToEmpty(dsFactory.getPassword()));

        StarRocksSinkOptions sinkOptions = builder.build();
        sinkOptions.enableUpsertDelete();
        return sinkOptions;
    }

    @TISExtension
    public static class DefaultSinkFunctionDescriptor extends BaseSinkFunctionDescriptor {
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME_FLINK_CDC_SINK;
        }

        public boolean validateColumnSeparator(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            return validateRowDelimiter(msgHandler, context, fieldName, value);
        }

        @Override
        public PluginVender getVender() {
            return PluginVender.TIS;
        }

        public boolean validateRowDelimiter(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
//            if (StringUtils.length(StringEscapeUtils.unescapeJava(value)) != 1) {
//                msgHandler.addFieldError(context, fieldName, "分隔符长度必须为1");
//                return false;
//            }
            return true;
        }

        @Override
        protected IEndTypeGetter.EndType getTargetType() {
            return IEndTypeGetter.EndType.StarRocks;
        }
    }
}
