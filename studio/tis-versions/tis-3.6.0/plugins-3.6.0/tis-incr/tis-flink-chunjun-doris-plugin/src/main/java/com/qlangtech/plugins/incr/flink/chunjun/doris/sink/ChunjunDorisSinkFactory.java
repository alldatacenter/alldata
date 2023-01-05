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

package com.qlangtech.plugins.incr.flink.chunjun.doris.sink;

import com.alibaba.fastjson.JSONObject;
import com.dtstack.chunjun.conf.OperatorConf;
import com.dtstack.chunjun.conf.SyncConf;
import com.dtstack.chunjun.connector.doris.options.DorisConfBuilder;
import com.dtstack.chunjun.connector.doris.options.DorisKeys;
import com.dtstack.chunjun.connector.doris.options.LoadConf;
import com.dtstack.chunjun.connector.doris.sink.DorisHttpOutputFormatBuilder;
import com.dtstack.chunjun.connector.doris.sink.DorisSinkFactory;
import com.dtstack.chunjun.connector.jdbc.TableCols;
import com.dtstack.chunjun.connector.jdbc.conf.JdbcConf;
import com.dtstack.chunjun.connector.jdbc.dialect.JdbcDialect;
import com.dtstack.chunjun.connector.jdbc.sink.JdbcOutputFormat;
import com.dtstack.chunjun.sink.DtOutputFormatSinkFunction;
import com.google.common.collect.Sets;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
import com.qlangtech.tis.compiler.streamcode.CompileAndPackage;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.datax.BasicDorisStarRocksWriter;
import com.qlangtech.tis.plugin.datax.IncrSelectedTabExtend;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.datax.doris.DataXDorisWriter;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.doris.DorisSourceFactory;
import com.qlangtech.tis.plugins.incr.flink.chunjun.sink.SinkTabPropsExtends;
import com.qlangtech.tis.plugins.incr.flink.chunjun.sink.UniqueKeySetter;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.Preconditions;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-15 14:32
 **/
public class ChunjunDorisSinkFactory extends ChunjunSinkFactory {



    @Override
    protected Class<? extends JdbcDialect> getJdbcDialectClass() {
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    protected JdbcOutputFormat createChunjunOutputFormat(DataSourceFactory dsFactory, JdbcConf conf) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean supportUpsetDML() {
        return true;
    }

    // private transient List<IColMetaGetter> colsMeta;

//    @Override
//    protected List<IColMetaGetter> getColsMeta(TableAlias tableName
//            , BasicDataSourceFactory dsFactory, CreateChunjunSinkFunctionResult sinkFunc) {
//        //return super.getColsMeta(tableName, dsFactory, sinkFunc);
//
//        if (colsMeta == null) {
//
//            Map<String, IColMetaGetter> colsMap = Maps.newHashMap();
//            // dsFactory.visitFirstConnection((conn) -> {
//            colsMap.putAll(ColMetaUtils.getColMetasMap(ChunjunDorisSinkFactory.this, new JdbcConf() {
//                @Override
//                public String getTable() {
//                    return tableName.getTo();
//                }
//            }));
//            //});
//            // 为了保证RowData创建的字段顺序( 由FieldConfig顺序决定 )
//            DorisSinkFactory sinkFactory = (DorisSinkFactory) sinkFunc.getSinkFactory();
//            colsMeta = Lists.newArrayList();
//            for (FieldConf field : sinkFactory.options.getColumn()) {
//                colsMeta.add(Objects.requireNonNull(colsMap.get(field.getName())
//                        , "fileName:" + field.getName() + " relevant colMeta can not be null"));
//            }
//            if (CollectionUtils.isEmpty(colsMeta)) {
//                throw new IllegalStateException("colsMeta can not be empty,tableName:" + tableName.toString());
//            }
//        }
//        return colsMeta;
//    }

    @Override
    protected void setParameter(BasicDataSourceFactory dsFactory
            , BasicDataXRdbmsWriter dataXWriter, OperatorConf writer, Map<String, Object> params, final String targetTabName) {
        DorisSourceFactory dorisDS = (DorisSourceFactory) dsFactory;
        DataXDorisWriter dataxWriter = (DataXDorisWriter) dataXWriter;

        BasicDorisStarRocksWriter.Separator separator = dataxWriter.getSeparator();
        JSONObject loadProps = dataxWriter.getLoadProps();
        Properties lp = new Properties();
        for (Map.Entry<String, Object> entry : loadProps.entrySet()) {
            if (BasicDorisStarRocksWriter.Separator.COL_SEPARATOR.equals(entry.getKey())
                    || BasicDorisStarRocksWriter.Separator.ROW_DELIMITER.equals(entry.getKey())) {
                continue;
            }
            lp.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
        }
        params.put(DorisKeys.LOAD_PROPERTIES_KEY, lp);
        params.put(DorisKeys.FE_NODES_KEY, dorisDS.getLoadUrls());
        params.put(DorisKeys.FIELD_DELIMITER_KEY, separator.getColumnSeparator());
        params.put(DorisKeys.LINE_DELIMITER_KEY, separator.getRowDelimiter());
        if (params.get(DorisKeys.BATCH_SIZE_KEY) == null) {
            throw new IllegalStateException("DorisKeys:" + DorisKeys.BATCH_SIZE_KEY + " can not be empty");
        }
        // params.put(DorisKeys.BATCH_SIZE_KEY, dataxWriter.maxBatchRows);

        params.put(DorisKeys.DATABASE_KEY, dsFactory.dbName);
        params.put(DorisKeys.TABLE_KEY, targetTabName);

        super.setParameter(dsFactory, dataXWriter, writer, params, targetTabName);
    }


    /**
     * @param cm
     * @return
     * @see BasicDorisStarRocksWriter.DorisType
     */
    @Override
    protected Object parseType(CMeta cm) {
        // DorisType
        return cm.getType().accept(BasicDorisStarRocksWriter.columnTokenRecognise);
    }

    @Override
    protected CreateChunjunSinkFunctionResult createSinkFactory(String jdbcUrl, String targetTabName, BasicDataSourceFactory dsFactory
            , BasicDataXRdbmsWriter dataXWriter, SyncConf syncConf) {
        IStreamTableMeta tabMeta = this.getStreamTableMeta(targetTabName);
        final CreateChunjunSinkFunctionResult createSinkResult = createDorisSinkFunctionResult(syncConf, tabMeta);
        return createSinkResult;
    }



    private static CreateChunjunSinkFunctionResult createDorisSinkFunctionResult(SyncConf syncConf, IStreamTableMeta tabMeta) {
        if (syncConf == null) {
            throw new IllegalArgumentException("param syncConf can not be null");
        }
        final TableCols sinkTabCols = new TableCols(tabMeta.getColsMeta());
        final CreateChunjunSinkFunctionResult createSinkResult = new CreateChunjunSinkFunctionResult();

        createSinkResult.setSinkFactory(new DorisSinkFactory(syncConf) {

            @Override
            protected DorisConfBuilder createDorisConfBuilder(OperatorConf parameter, LoadConf loadConf) {
                DorisConfBuilder builder = super.createDorisConfBuilder(parameter, loadConf);
                final OperatorConf params = syncConf.getWriter();
                List<String> fullCols = sinkTabCols.getColKeys();// (List<String>) params.getVal(KEY_FULL_COLS);
                if (CollectionUtils.isEmpty(fullCols)) {
                    throw new IllegalStateException("fullCols can not be empty");
                }
                builder.setFullCols(fullCols);
                builder.setUniqueKey((List<String>) params.getVal(SinkTabPropsExtends.KEY_UNIQUE_KEY));
                return builder;
            }

            @Override
            protected DorisHttpOutputFormatBuilder createDorisHttpOutputFormatBuilder() {
                DorisHttpOutputFormatBuilder builder = super.createDorisHttpOutputFormatBuilder();
                List<String> cols = sinkTabCols.getColKeys();// options.getColumn().stream().map((field) -> field.getName()).collect(Collectors.toList());
                builder.setColumns(cols);
                TISDorisColumnConverter columnConverter = TISDorisColumnConverter.create(sinkTabCols);
                columnConverter.setColumnNames(cols);
                if (CollectionUtils.isEmpty(options.getFullColumn())) {
                    throw new IllegalStateException("options.getFullColumn() can not be empty");
                }
                columnConverter.setFullColumn(options.getFullColumn());
                builder.setRowConverter(columnConverter);
                return builder;
            }

            @Override
            protected DataStreamSink<RowData> createOutput(DataStream<RowData> dataSet, OutputFormat<RowData> outputFormat) {

                Preconditions.checkNotNull(outputFormat);
                SinkFunction<RowData> sinkFunction =
                        new DtOutputFormatSinkFunction<>(outputFormat);
                createSinkResult.setSinkCols(sinkTabCols);
                createSinkResult.setSinkFunction(sinkFunction);
                return null;
            }
        });
        return createSinkResult;
    }


    @Override
    protected void initChunjunJdbcConf(JdbcConf jdbcConf) {

    }


    @Override
    public ICompileAndPackage getCompileAndPackageManager() {
        return new CompileAndPackage(Sets.newHashSet(
                //  "tis-sink-hudi-plugin"
                ChunjunDorisSinkFactory.class
                // "tis-datax-hudi-plugin"
                // , "com.alibaba.datax.plugin.writer.hudi.HudiConfig"
        ));
    }


    @TISExtension
    public static final class DftDesc extends BasicChunjunSinkDescriptor {
        @Override
        protected IEndTypeGetter.EndType getTargetType() {
            return IEndTypeGetter.EndType.Doris;
        }

        @Override
        public Descriptor<IncrSelectedTabExtend> getSelectedTableExtendDescriptor() {
            return TIS.get().getDescriptor(UniqueKeySetter.class);
        }

    }
}
