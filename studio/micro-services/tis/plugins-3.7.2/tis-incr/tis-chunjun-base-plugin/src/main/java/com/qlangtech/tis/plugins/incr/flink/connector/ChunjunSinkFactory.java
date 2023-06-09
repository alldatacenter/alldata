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

package com.qlangtech.tis.plugins.incr.flink.connector;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsColMeta;
import com.dtstack.chunjun.conf.*;
import com.dtstack.chunjun.connector.jdbc.TableCols;
import com.dtstack.chunjun.connector.jdbc.conf.JdbcConf;
import com.dtstack.chunjun.connector.jdbc.converter.JdbcColumnConverter;
import com.dtstack.chunjun.connector.jdbc.dialect.JdbcDialect;
import com.dtstack.chunjun.connector.jdbc.dialect.SupportUpdateMode;
import com.dtstack.chunjun.connector.jdbc.sink.JdbcOutputFormat;
import com.dtstack.chunjun.connector.jdbc.sink.JdbcOutputFormatBuilder;
import com.dtstack.chunjun.connector.jdbc.sink.JdbcSinkFactory;
import com.dtstack.chunjun.constants.ConfigConstant;
import com.dtstack.chunjun.sink.DtOutputFormatSinkFunction;
import com.dtstack.chunjun.sink.SinkFactory;
import com.dtstack.chunjun.sink.WriteMode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.*;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.IncrSelectedTabExtend;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.plugin.incr.IIncrSelectedTabExtendFactory;
import com.qlangtech.tis.plugins.incr.flink.cdc.AbstractRowDataMapper;
import com.qlangtech.tis.plugins.incr.flink.chunjun.common.DialectUtils;
import com.qlangtech.tis.plugins.incr.flink.chunjun.script.ChunjunStreamScriptType;
import com.qlangtech.tis.plugins.incr.flink.chunjun.sink.SinkTabPropsExtends;
import com.qlangtech.tis.realtime.BasicTISSinkFactory;
import com.qlangtech.tis.realtime.TabSinkFunc;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.table.data.RowData;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * WRITER extends BasicDataXRdbmsWriter, DS extends BasicDataSourceFactory
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-10 13:45
 **/
public abstract class ChunjunSinkFactory extends BasicTISSinkFactory<RowData>
        implements IStreamTableMeataCreator.ISinkStreamMetaCreator, IStreamIncrGenerateStrategy {

    public static final String DISPLAY_NAME_FLINK_CDC_SINK = "Chunjun-Sink-";
    public static final String KEY_FULL_COLS = "fullColumn";
    //    描述：sink 端是否支持二阶段提交
//    注意：
//    如果此参数为空，默认不开启二阶段提交，即 sink 端不支持 exactly_once 语义；
//    当前只支持 exactly-once 和 at-least-once
//    必选：否
//    参数类型：String
//    示例："semantic": "exactly-once"
    @FormField(ordinal = 1, type = FormFieldType.ENUM, validate = {Validator.require})
    public String semantic;
    //    描述：一次性批量提交的记录数大小，该值可以极大减少 ChunJun 与数据库的网络交互次数，并提升整体吞吐量。但是该值设置过大可能会造成 ChunJun 运行进程 OOM 情况
//    必选：否
//    参数类型：int
//    默认值：1

    @FormField(ordinal = 4, validate = {Validator.require})
    public ChunjunStreamScriptType scriptType;

    @FormField(ordinal = 6, type = FormFieldType.INT_NUMBER, validate = {Validator.require})
    public int batchSize;

    @FormField(ordinal = 9, type = FormFieldType.INT_NUMBER, validate = {Validator.require})
    public int flushIntervalMills;

    @FormField(ordinal = 12, type = FormFieldType.INT_NUMBER, validate = {Validator.require})
    public Integer parallelism;

    private transient Map<String, SelectedTab> selTabs;


    @Override
    public Map<TableAlias, TabSinkFunc<RowData>> createSinkFunction(IDataxProcessor dataxProcessor) {
        Map<TableAlias, TabSinkFunc<RowData>> sinkFuncs = Maps.newHashMap();


        TableAliasMapper selectedTabs = dataxProcessor.getTabAlias(null);
        if (selectedTabs.isNull()) {
            throw new IllegalStateException("selectedTabs can not be empty");
        }
        IDataxReader reader = dataxProcessor.getReader(null);
        // List<ISelectedTab> tabs = reader.getSelectedTabs();

        // 清空一下tabs的缓存以免有脏数据
        this.selTabs = null;

        selectedTabs.forEach((key, val/*TableAlias*/) -> {

            Objects.requireNonNull(val, "tableName can not be null");
            if (StringUtils.isEmpty(val.getFrom())) {
                throw new IllegalStateException("tableName.getFrom() can not be empty");
            }

            //  AtomicReference<CreateChunjunSinkFunctionResult> sinkFuncRef = new AtomicReference<>();
            //  sinkFunc = null;
            final TableAlias tabName = val;

//            Optional<ISelectedTab> selectedTab = tabs.stream()
//                    .filter((tab) -> StringUtils.equals(tabName.getFrom(), tab.getName())).findFirst();
//            if (!selectedTab.isPresent()) {
//                throw new IllegalStateException("target table:" + tabName.getFrom()
//                        + " can not find matched table in:["
//                        + tabs.stream().map((t) -> t.getName()).collect(Collectors.joining(",")) + "]");
//            }
//
//            final CreateChunjunSinkFunctionResult sinkFunc
//                    = createSinFunctionResult(dataxProcessor, (SelectedTab) selectedTab.get(), tabName.getTo());
//
////            AtomicReference<Object[]> exceptionLoader = new AtomicReference<>();
////            final String targetTabName = val.getTo();
//            BasicDataXRdbmsWriter dataXWriter = (BasicDataXRdbmsWriter) dataxProcessor.getWriter(null);
//            BasicDataSourceFactory dsFactory = (BasicDataSourceFactory) dataXWriter.getDataSourceFactory();
//            if (dsFactory == null) {
//                throw new IllegalStateException("dsFactory can not be null");
//            }
//            DBConfig dbConfig = dsFactory.getDbConfig();
//            dbConfig.vistDbURL(false, (dbName, dbHost, jdbcUrl) -> {
//                try {
//
//                    /**
//                     * 需要先初始化表MySQL目标库中的表
//                     */
//                    dataXWriter.initWriterTable(targetTabName, Collections.singletonList(jdbcUrl));
//// FIXME 这里不能用 MySQLSelectedTab
//                    sinkFuncRef.set(createSinkFunction(dbName, targetTabName
//                            , (SelectedTab) selectedTab.get(), jdbcUrl, dsFactory, dataXWriter));
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
//            sinkFunc = sinkFuncRef.get();
//            if (this.parallelism == null) {
//                throw new IllegalStateException("param parallelism can not be null");
//            }

//            sinkFuncs.put(val, new RowDataSinkFunc(val
//                    , sinkFunc.getSinkFunction()
//                    // , AbstractRowDataMapper.getAllTabColsMeta(this.getColsMeta(val, dsFactory, sinkFunc))
//                    , AbstractRowDataMapper.getAllTabColsMeta(sinkFunc.tableCols.getCols())
//                    , supportUpsetDML()
//                    , this.parallelism));

            sinkFuncs.put(val, createRowDataSinkFunc(dataxProcessor, tabName, true));
        });

        if (sinkFuncs.size() < 1) {
            throw new IllegalStateException("size of sinkFuncs can not be small than 1");
        }
        return sinkFuncs;
    }

    public RowDataSinkFunc createRowDataSinkFunc(IDataxProcessor dataxProcessor
            , final TableAlias tabName, boolean shallInitSinkTable) {

        IDataxReader reader = dataxProcessor.getReader(null);
        List<ISelectedTab> tabs = reader.getSelectedTabs();

        Optional<ISelectedTab> selectedTab = tabs.stream()
                .filter((tab) -> StringUtils.equals(tabName.getFrom(), tab.getName())).findFirst();
        if (!selectedTab.isPresent()) {
            throw new IllegalStateException("target table:" + tabName.getFrom()
                    + " can not find matched table in:["
                    + tabs.stream().map((t) -> t.getName()).collect(Collectors.joining(",")) + "]");
        }

        final CreateChunjunSinkFunctionResult sinkFunc
                = createSinFunctionResult(dataxProcessor
                , (SelectedTab) selectedTab.get(), tabName.getTo(), shallInitSinkTable);

        if (this.parallelism == null) {
            throw new IllegalStateException("param parallelism can not be null");
        }

        return new RowDataSinkFunc(tabName
                , sinkFunc.getSinkFunction()
                , AbstractRowDataMapper.getAllTabColsMeta(Objects.requireNonNull(sinkFunc.tableCols, "tabCols can not be null").getCols())
                , supportUpsetDML()
                , this.parallelism);
    }


    protected CreateChunjunSinkFunctionResult createSinFunctionResult(
            IDataxProcessor dataxProcessor, SelectedTab selectedTab, final String targetTabName, boolean shallInitSinkTable) {

        AtomicReference<Object[]> exceptionLoader = new AtomicReference<>();
        AtomicReference<CreateChunjunSinkFunctionResult> sinkFuncRef = new AtomicReference<>();
        BasicDataXRdbmsWriter dataXWriter = (BasicDataXRdbmsWriter) dataxProcessor.getWriter(null);
        BasicDataSourceFactory dsFactory = (BasicDataSourceFactory) dataXWriter.getDataSourceFactory();
        if (dsFactory == null) {
            throw new IllegalStateException("dsFactory can not be null");
        }
        DBConfig dbConfig = dsFactory.getDbConfig();
        dbConfig.vistDbURL(false, (dbName, dbHost, jdbcUrl) -> {
            try {
                if (shallInitSinkTable) {
                    /**
                     * 需要先初始化表MySQL目标库中的表
                     */
                    dataXWriter.initWriterTable(targetTabName, Collections.singletonList(jdbcUrl));
                }

// FIXME 这里不能用 MySQLSelectedTab
                sinkFuncRef.set(createSinkFunction(dbName, targetTabName, selectedTab, jdbcUrl, dsFactory, dataXWriter));

            } catch (Throwable e) {
                exceptionLoader.set(new Object[]{jdbcUrl, e});
            }
        });

        if (exceptionLoader.get() != null) {
            Object[] error = exceptionLoader.get();
            throw new RuntimeException((String) error[0], (Throwable) error[1]);
        }
        Objects.requireNonNull(sinkFuncRef.get(), "sinkFunc can not be null");
        sinkFuncRef.get().parallelism = this.parallelism;
        return sinkFuncRef.get();
    }


    protected abstract boolean supportUpsetDML();

    protected final SyncConf createSyncConf(SelectedTab tab, Supplier<Map<String, Object>> paramsCreator) {
        SyncConf syncConf = new SyncConf();

        JobConf jobConf = new JobConf();
        ContentConf content = new ContentConf();
        OperatorConf writer = new OperatorConf();
        writer.setName("writer");
        Map<String, Object> params = paramsCreator.get();
        writer.setParameter(params);


        IIncrSelectedTabExtendFactory desc = (IIncrSelectedTabExtendFactory) this.getDescriptor();
        if (desc.getSelectedTableExtendDescriptor() != null) {
            // 有扩展才进行设置，不然会空指针
            ((SinkTabPropsExtends) tab.getIncrSinkProps()).setParams(params);
        }

        List<Map<String, Object>> cols = Lists.newArrayList();
        Map<String, Object> col = null;

        //FIXME: 构建sink端的列不应该使用Source端的colMeta信息，你该需要重构
        for (CMeta cm : tab.getCols()) {
            col = Maps.newHashMap();
            col.put("name", cm.getName());
            col.put("type", parseType(cm));
            cols.add(col);
        }


        params.put(ConfigConstant.KEY_COLUMN, cols);
        params.put(KEY_FULL_COLS, tab.getCols().stream().map((c) -> c.getName()).collect(Collectors.toList()));
        params.put("batchSize", this.batchSize);
        params.put("flushIntervalMills", this.flushIntervalMills);
        params.put("semantic", this.semantic);

        content.setWriter(writer);
        jobConf.setContent(Lists.newLinkedList(Collections.singleton(content)));
        syncConf.setJob(jobConf);
        return syncConf;
    }

    /**
     * @param dbName
     * @param targetTabName
     * @param tab
     * @param jdbcUrl
     * @param dsFactory
     * @param dataXWriter
     * @return
     * @see JdbcSinkFactory
     */
    private CreateChunjunSinkFunctionResult createSinkFunction(
            String dbName, final String targetTabName, SelectedTab tab, String jdbcUrl
            , BasicDataSourceFactory dsFactory, BasicDataXRdbmsWriter dataXWriter) {


        SyncConf syncConf = createSyncConf(tab, () -> {
            Map<String, Object> params = Maps.newHashMap();
            params.put("username", dsFactory.getUserName());
            params.put("password", dsFactory.getPassword());


            Map<String, Object> conn = Maps.newHashMap();
            conn.put("jdbcUrl", jdbcUrl);
            conn.put("table", Lists.newArrayList(targetTabName));
            setSchema(conn, dbName, dsFactory);
            params.put("connection", Lists.newArrayList(conn));
            setParameter(dsFactory, dataXWriter, tab, params, targetTabName);

            return params;
        });

//        JobConf jobConf = new JobConf();
//        ContentConf content = new ContentConf();
//        OperatorConf writer = new OperatorConf();
//        writer.setName("writer");
//        Map<String, Object> params = Maps.newHashMap();
//        params.put("username", dsFactory.getUserName());
//        params.put("password", dsFactory.getPassword());
//
//        IIncrSelectedTabExtendFactory desc = (IIncrSelectedTabExtendFactory) this.getDescriptor();
//        if (desc.getSelectedTableExtendDescriptor() != null) {
//            // 有扩展才进行设置，不然会空指针
//            ((SinkTabPropsExtends) tab.getIncrSinkProps()).setParams(params);
//        }
//
//        List<Map<String, Object>> cols = Lists.newArrayList();
//        Map<String, Object> col = null;
//
//        //FIXME: 构建sink端的列不应该使用Source端的colMeta信息，你该需要重构
//        for (CMeta cm : tab.getCols()) {
//            col = Maps.newHashMap();
//            col.put("name", cm.getName());
//            col.put("type", parseType(cm));
//            cols.add(col);
//        }
//
//
//        params.put(ConfigConstant.KEY_COLUMN, cols);
//        params.put(KEY_FULL_COLS, tab.getCols().stream().map((c) -> c.getName()).collect(Collectors.toList()));
//        params.put("batchSize", this.batchSize);
//        params.put("flushIntervalMills", this.flushIntervalMills);
//        params.put("semantic", this.semantic);
//        Map<String, Object> conn = Maps.newHashMap();
//        conn.put("jdbcUrl", jdbcUrl);
//        conn.put("table", Lists.newArrayList(targetTabName));
//        setSchema(conn, dbName, dsFactory);
//        params.put("connection", Lists.newArrayList(conn));
//        setParameter(dsFactory, dataXWriter, writer, params, targetTabName);
//        content.setWriter(writer);
//        jobConf.setContent(Lists.newLinkedList(Collections.singleton(content)));
//        syncConf.setJob(jobConf);
        CreateChunjunSinkFunctionResult sinkFunc
                = createChunjunSinkFunction(jdbcUrl, targetTabName, dsFactory, dataXWriter, syncConf);
        return sinkFunc;
    }


    protected void setSchema(Map<String, Object> conn, String dbName, BasicDataSourceFactory dsFactory) {
        conn.put("schema", dbName);
    }

    protected void setParameter(BasicDataSourceFactory dsFactory, BasicDataXRdbmsWriter dataXWriter
            , SelectedTab tab, Map<String, Object> params, final String targetTabName) {
    }

    private CreateChunjunSinkFunctionResult createChunjunSinkFunction(
            String jdbcUrl, String targetTabName, BasicDataSourceFactory dsFactory, BasicDataXRdbmsWriter dataXWriter, SyncConf syncConf) {
        CreateChunjunSinkFunctionResult sinkFactory = createSinkFactory(jdbcUrl, targetTabName, dsFactory, dataXWriter, syncConf);
        sinkFactory.initialize();
        return Objects.requireNonNull(sinkFactory, "create result can not be null");
    }

    @Override
    public final IStreamTemplateResource getFlinkStreamGenerateTplResource() {
        return scriptType.createStreamTableCreator(this).getFlinkStreamGenerateTplResource();
    }

    @Override
    public final IStreamIncrGenerateStrategy.IStreamTemplateData decorateMergeData(IStreamTemplateData mergeData) {
        return scriptType.createStreamTableCreator(this).decorateMergeData(mergeData);
    }


    public static class CreateChunjunSinkFunctionResult {
        SinkFunction<RowData> sinkFunction;
        JdbcColumnConverter columnConverter;
        JdbcOutputFormat outputFormat;
        SinkFactory sinkFactory;
        private int parallelism;
        private TableCols tableCols;

        public void initialize() {
            Objects.requireNonNull(sinkFactory, "sinkFactory can not be null").createSink(null);
        }

        public SinkFunction<RowData> getSinkFunction() {
            return sinkFunction;
        }

        public int getParallelism() {
            return this.parallelism;
        }

        public void setParallelism(int parallelism) {
            this.parallelism = parallelism;
        }

        public void setSinkFunction(SinkFunction<RowData> sinkFunction) {
            this.sinkFunction = sinkFunction;
        }

        public JdbcColumnConverter getColumnConverter() {
            return columnConverter;
        }

        public void setColumnConverter(JdbcColumnConverter columnConverter) {
            this.columnConverter = columnConverter;
        }

        public JdbcOutputFormat getOutputFormat() {
            return outputFormat;
        }

        public void setOutputFormat(JdbcOutputFormat outputFormat) {
            this.outputFormat = outputFormat;
        }

        public SinkFactory getSinkFactory() {
            return sinkFactory;
        }

        public void setSinkFactory(SinkFactory sinkFactory) {
            this.sinkFactory = sinkFactory;
        }

        public void setSinkCols(TableCols tableCols) {
            this.tableCols = tableCols;
        }

        public TableCols getSinkColsMeta() {
            return this.tableCols;
        }
    }

    protected CreateChunjunSinkFunctionResult createSinkFactory(String jdbcUrl, String targetTabName, BasicDataSourceFactory dsFactory
            , BasicDataXRdbmsWriter dataXWriter, SyncConf syncConf) {
        final CreateChunjunSinkFunctionResult createResult = new CreateChunjunSinkFunctionResult();

        createResult.setSinkFactory(new JdbcSinkFactory(syncConf, createJdbcDialect(syncConf)) {
            @Override
            public void initCommonConf(ChunJunCommonConf commonConf) {
                super.initCommonConf(commonConf);
                initChunjunJdbcConf(this.jdbcConf);
            }

            @Override
            protected JdbcOutputFormatBuilder getBuilder() {
                return new JdbcOutputFormatBuilder(createChunjunOutputFormat(dataXWriter.getDataSourceFactory(), this.jdbcConf));
            }

            @Override
            protected DataStreamSink<RowData> createOutput(
                    DataStream<RowData> dataSet, OutputFormat<RowData> outputFormat) {
                JdbcOutputFormat routputFormat = (JdbcOutputFormat) outputFormat;

                try (DataSourceMeta.JDBCConnection conn = dsFactory.getConnection(jdbcUrl)) {
                    routputFormat.dbConn = conn.getConnection();
                    routputFormat.initColumnList();
                } catch (SQLException e) {
                    throw new RuntimeException("jdbcUrl:" + jdbcUrl, e);
                }
                TableCols tableCols = new TableCols(routputFormat.colsMeta);

                JdbcColumnConverter rowConverter = (JdbcColumnConverter)
                        DialectUtils.createColumnConverter(jdbcDialect, jdbcConf, tableCols.filterBy(jdbcConf.getColumn()));

                DtOutputFormatSinkFunction<RowData> sinkFunction =
                        new DtOutputFormatSinkFunction<>(outputFormat);
                createResult.setSinkCols(tableCols);
                createResult.setColumnConverter(rowConverter);
                createResult.setSinkFunction(sinkFunction);
                createResult.setOutputFormat(routputFormat);
                //   ref.set(Triple.of(sinkFunction, rowConverter, routputFormat));
                return null;
            }
        });


        return createResult;
    }

    /**
     * 找到chunjun Sink 支持的write方式
     *
     * @return
     */
    public final Set<WriteMode> supportSinkWriteMode() {
        Class<? extends JdbcDialect> dialectClass = this.getJdbcDialectClass();
        if (dialectClass == null) {
            return Sets.newHashSet();
        }
        SupportUpdateMode supportMode = dialectClass.getAnnotation(SupportUpdateMode.class);
        Objects.requireNonNull(supportMode, "dialectClass:" + dialectClass.getClass().getName()
                + " can not find annotation " + SupportUpdateMode.class);
        Set<WriteMode> result = Sets.newHashSet(supportMode.modes());
        result.add(WriteMode.INSERT);
        return result;
    }

    protected abstract Class<? extends JdbcDialect> getJdbcDialectClass();

    protected final JdbcDialect createJdbcDialect(SyncConf syncConf) {
        try {

            Class<? extends JdbcDialect> clazz = getJdbcDialectClass();
            Constructor<?>[] constructors = clazz.getConstructors();
            for (Constructor<?> c : constructors) {

                if (c.getParameterCount() == 1 && c.getParameterTypes()[0] == SyncConf.class) {
                    return (JdbcDialect) c.newInstance(syncConf);
                }
            }

            return new WriteModeFilterJdbcDialect(clazz.newInstance(), supportSinkWriteMode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    protected abstract JdbcOutputFormat createChunjunOutputFormat(DataSourceFactory dsFactory, JdbcConf jdbcConf);


    /**
     * ==========================================================
     * impl: IStreamTableCreator
     * ===========================================================
     */
    @Override
    public IStreamTableMeta getStreamTableMeta(String tableName) {

        // DataxProcessor dataXProcessor = DataxProcessor.load(null, this.dataXName);
        BasicDataXRdbmsWriter writer = (BasicDataXRdbmsWriter) DataxWriter.load(null, this.dataXName);// dataXProcessor.getWriter(null);

        final BasicDataSourceFactory ds = (BasicDataSourceFactory) writer.getDataSourceFactory();


        return new IStreamTableMeta() {
            @Override
            public List<IColMetaGetter> getColsMeta() {
                try {
                    // 在创建增量流程过程中可能 sink端的表还不存在
                    DataxWriter.process(dataXName, tableName, ds.getJdbcUrls());
                    return ds.getTableMetadata(true, EntityName.parse(tableName))
                            .stream().map((c) -> new HdfsColMeta(c.getName(), c.isNullable(), c.isPk(), c.getType()))
                            .collect(Collectors.toList());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * ==========================================================
     * End impl: IStreamTableCreator
     * ===========================================================
     */
    protected Object parseType(CMeta cm) {
        return cm.getType().getS();
    }


    protected final <TT extends BaseSinkFunctionDescriptor> Class<TT> getExpectDescClass() {
        return (Class<TT>) BasicChunjunSinkDescriptor.class;
    }

    protected abstract void initChunjunJdbcConf(JdbcConf jdbcConf);

    public static abstract class BasicChunjunSinkDescriptor extends BaseSinkFunctionDescriptor implements IIncrSelectedTabExtendFactory {
        @Override
        public final String getDisplayName() {
            return DISPLAY_NAME_FLINK_CDC_SINK + this.getTargetType().name();
        }

        @Override
        protected boolean validateAll(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            return super.validateAll(msgHandler, context, postFormVals);
        }

        @Override
        public final PluginVender getVender() {
            return PluginVender.CHUNJUN;
        }


        /**
         * 校验batchSize
         *
         * @param msgHandler
         * @param context
         * @param fieldName
         * @param value
         * @return
         */
        public boolean validateBatchSize(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            int batchSize = Integer.parseInt(value);
            final int miniSize = 1;
            if (batchSize < miniSize) {
                msgHandler.addFieldError(context, fieldName, "不能小于" + miniSize);
                return false;
            }
            return true;
        }

        public boolean validateFlushIntervalMills(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            int interval = Integer.parseInt(value);
            if (interval < 1000) {
                msgHandler.addFieldError(context, fieldName, "不能小于1秒");
                return false;
            }
            return true;
        }

        public boolean validateParallelism(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            int val = Integer.parseInt(value);
            if (val < 1) {
                msgHandler.addFieldError(context, fieldName, "并发度不能小于1");
                return false;
            }
            return true;
        }

        public EndType getChunjunEndType() {
            return this.getTargetType();
        }

        @Override
        public Descriptor<IncrSelectedTabExtend> getSelectedTableExtendDescriptor() {
            return TIS.get().getDescriptor(SinkTabPropsExtends.class);
        }
    }
}
