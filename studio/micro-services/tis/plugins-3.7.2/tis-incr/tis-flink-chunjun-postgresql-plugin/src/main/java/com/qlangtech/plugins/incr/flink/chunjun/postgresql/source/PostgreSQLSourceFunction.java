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

import com.dtstack.chunjun.conf.SyncConf;
import com.dtstack.chunjun.connector.jdbc.source.JdbcInputFormatBuilder;
import com.dtstack.chunjun.connector.jdbc.source.JdbcSourceFactory;
import com.dtstack.chunjun.connector.postgresql.source.PostgresqlSourceFactory;
import com.dtstack.chunjun.source.DtInputFormatSourceFunction;
import com.qlangtech.plugins.incr.flink.chunjun.postgresql.dialect.TISPostgresqlDialect;
import com.qlangtech.tis.datax.IStreamTableMeataCreator;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import com.qlangtech.tis.plugins.incr.flink.chunjun.source.ChunjunSourceFunction;
import org.apache.flink.api.common.io.InputFormat;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.Preconditions;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-27 08:32
 **/
public class PostgreSQLSourceFunction extends ChunjunSourceFunction {


    public PostgreSQLSourceFunction(ChunjunPostgreSQLSourceFactory sourceFactory) {
        super(sourceFactory);
    }

    @Override
    protected JdbcSourceFactory createChunjunSourceFactory(
            SyncConf conf, BasicDataSourceFactory sourceFactory
            , IStreamTableMeataCreator.IStreamTableMeta cmetas, AtomicReference<SourceFunction<RowData>> sourceFunc) {
        //List<IColMetaGetter> colsMeta = reader.getStreamTableMeta(sourceTabName).getColsMeta();
        return new ExtendPostgresqlSourceFactory(conf, null, cmetas.getColsMeta(), sourceFactory) {
            protected DataStream<RowData> createInput(
                    InputFormat<RowData, InputSplit> inputFormat, String sourceName) {
                Preconditions.checkNotNull(sourceName);
                Preconditions.checkNotNull(inputFormat);
                DtInputFormatSourceFunction<RowData> function =
                        new DtInputFormatSourceFunction<>(inputFormat, getTypeInformation());
                sourceFunc.set(function);
                return null;
            }
        };
    }

    private static class ExtendPostgresqlSourceFactory extends PostgresqlSourceFactory {
        private final DataSourceFactory dataSourceFactory;
        public ExtendPostgresqlSourceFactory(SyncConf syncConf, StreamExecutionEnvironment env
                , List<IColMetaGetter> colsMeta, DataSourceFactory dataSourceFactory) {
            super(syncConf, env, new TISPostgresqlDialect(syncConf), colsMeta);
            this.fieldList = syncConf.getReader().getFieldList();
            this.dataSourceFactory = dataSourceFactory;
        }
        @Override
        protected JdbcInputFormatBuilder getBuilder() {
            return new JdbcInputFormatBuilder(new TISPostgresqlInputFormat(dataSourceFactory, sourceColsMeta));
        }
    }
}
