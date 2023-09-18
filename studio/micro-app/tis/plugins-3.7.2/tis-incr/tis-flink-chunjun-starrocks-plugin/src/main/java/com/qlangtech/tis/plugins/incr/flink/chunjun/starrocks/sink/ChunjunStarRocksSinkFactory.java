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

package com.qlangtech.tis.plugins.incr.flink.chunjun.starrocks.sink;

import com.dtstack.chunjun.conf.ChunJunCommonConf;
import com.dtstack.chunjun.conf.SyncConf;
import com.dtstack.chunjun.connector.jdbc.TableCols;
import com.dtstack.chunjun.connector.jdbc.conf.JdbcConf;
import com.dtstack.chunjun.connector.jdbc.dialect.JdbcDialect;
import com.dtstack.chunjun.connector.jdbc.sink.JdbcOutputFormat;
import com.dtstack.chunjun.connector.starrocks.conf.LoadConf;
import com.dtstack.chunjun.connector.starrocks.conf.StarRocksConf;
import com.dtstack.chunjun.connector.starrocks.sink.StarRocksSinkFactory;
import com.dtstack.chunjun.converter.AbstractRowConverter;
import com.dtstack.chunjun.sink.DtOutputFormatSinkFunction;
import com.google.common.collect.Sets;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
import com.qlangtech.tis.compiler.streamcode.CompileAndPackage;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.datax.IncrSelectedTabExtend;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.datax.starrocks.DataXStarRocksWriter;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.starrocks.StarRocksSourceFactory;
import com.qlangtech.tis.plugins.incr.flink.chunjun.sink.UniqueKeySetter;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.Preconditions;

import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-12-11 22:05
 **/
public class ChunjunStarRocksSinkFactory extends ChunjunSinkFactory {

    @Override
    protected boolean supportUpsetDML() {
        return true;
    }

    @Override
    protected Class<? extends JdbcDialect> getJdbcDialectClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected CreateChunjunSinkFunctionResult createSinkFactory(String jdbcUrl, String targetTabName, BasicDataSourceFactory dsFactory
            , BasicDataXRdbmsWriter dataXWriter, SyncConf syncConf) {
        IStreamTableMeta tabMeta = this.getStreamTableMeta(targetTabName);
        DataXStarRocksWriter rocksWriter = (DataXStarRocksWriter) dataXWriter;

        final CreateChunjunSinkFunctionResult createSinkResult = createSinkFunctionResult(jdbcUrl, rocksWriter, dsFactory, targetTabName, syncConf, tabMeta, this);
        return createSinkResult;
    }


    private static CreateChunjunSinkFunctionResult createSinkFunctionResult(final String jdbcUrl, DataXStarRocksWriter rocksWriter, BasicDataSourceFactory dsFactory
            , String targetTabName, SyncConf syncConf, IStreamTableMeta tabMeta, ChunjunStarRocksSinkFactory sinkFactory) {
        if (syncConf == null) {
            throw new IllegalArgumentException("param syncConf can not be null");
        }

        StarRocksSourceFactory starRocksDSFactory = (StarRocksSourceFactory) dsFactory;
        final TableCols sinkTabCols = new TableCols(tabMeta.getColsMeta());
        final CreateChunjunSinkFunctionResult createSinkResult = new CreateChunjunSinkFunctionResult();

        createSinkResult.setSinkFactory(new StarRocksSinkFactory(syncConf) {
            @Override
            protected AbstractRowConverter createRowConverter() {
                return TISStarRocksColumnConverter.create(
                        Objects.requireNonNull(sinkTabCols, "SinkColsMeta can not be null"));
            }

            @Override
            public void initCommonConf(ChunJunCommonConf commonConf) {
                super.initCommonConf(commonConf);
                StarRocksConf sconf = (StarRocksConf) commonConf;
                sconf.setUrl(jdbcUrl);
                sconf.setUsername(dsFactory.getUserName());
                sconf.setPassword(StringUtils.trimToEmpty(dsFactory.getPassword()));
                sconf.setDatabase(dsFactory.getDbName());
                sconf.setTable(targetTabName);
                sconf.setFeNodes(starRocksDSFactory.getLoadUrls());
                sconf.setSemantic(sinkFactory.semantic);

                LoadConf cfg = new LoadConf();
                if (sinkFactory.batchSize > 0) {
                    // cfg.setBatchMaxRows(new Long(rocksWriter.maxBatchRows));
                    cfg.setBatchMaxRows(new Long(sinkFactory.batchSize));
                }
                sconf.setLoadConf(cfg);
            }
//            @Override
//            public DataStreamSink<RowData> createSink(DataStream<RowData> dataSet) {
//                return super.createSink(dataSet);
//            }
//            @Override
//            protected DorisConfBuilder createDorisConfBuilder(OperatorConf parameter, LoadConf loadConf) {
//                DorisConfBuilder builder = super.createDorisConfBuilder(parameter, loadConf);
//                final OperatorConf params = syncConf.getWriter();
//                List<String> fullCols = sinkTabCols.getColKeys();// (List<String>) params.getVal(KEY_FULL_COLS);
//                if (CollectionUtils.isEmpty(fullCols)) {
//                    throw new IllegalStateException("fullCols can not be empty");
//                }
//                builder.setFullCols(fullCols);
//                builder.setUniqueKey((List<String>) params.getVal(SinkTabPropsExtends.KEY_UNIQUE_KEY));
//                return builder;
//            }

//            @Override
//            protected DorisHttpOutputFormatBuilder createDorisHttpOutputFormatBuilder() {
//                DorisHttpOutputFormatBuilder builder = super.createDorisHttpOutputFormatBuilder();
//                List<String> cols = sinkTabCols.getColKeys();// options.getColumn().stream().map((field) -> field.getName()).collect(Collectors.toList());
//                builder.setColumns(cols);
//                TISDorisColumnConverter columnConverter = TISDorisColumnConverter.create(sinkTabCols);
//                columnConverter.setColumnNames(cols);
//                if (CollectionUtils.isEmpty(options.getFullColumn())) {
//                    throw new IllegalStateException("options.getFullColumn() can not be empty");
//                }
//                columnConverter.setFullColumn(options.getFullColumn());
//                builder.setRowConverter(columnConverter);
//                return builder;
//            }

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
    protected JdbcOutputFormat createChunjunOutputFormat(DataSourceFactory dsFactory, JdbcConf jdbcConf) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void initChunjunJdbcConf(JdbcConf jdbcConf) {

    }

    @Override
    public ICompileAndPackage getCompileAndPackageManager() {
        return new CompileAndPackage(Sets.newHashSet(
                ChunjunStarRocksSinkFactory.class
        ));
    }

    @TISExtension
    public static final class DefaultDesc extends BasicChunjunSinkDescriptor {
        @Override
        protected IEndTypeGetter.EndType getTargetType() {
            return EndType.StarRocks;
        }

        @Override
        public Descriptor<IncrSelectedTabExtend> getSelectedTableExtendDescriptor() {
            return TIS.get().getDescriptor(UniqueKeySetter.class);
        }

    }
}
