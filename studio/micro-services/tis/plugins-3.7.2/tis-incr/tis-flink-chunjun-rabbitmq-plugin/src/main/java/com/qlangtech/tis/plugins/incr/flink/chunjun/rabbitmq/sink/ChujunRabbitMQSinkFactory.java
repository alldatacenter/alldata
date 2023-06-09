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

package com.qlangtech.tis.plugins.incr.flink.chunjun.rabbitmq.sink;

import com.dtstack.chunjun.conf.FieldConf;
import com.dtstack.chunjun.conf.SyncConf;
import com.dtstack.chunjun.connector.jdbc.TableCols;
import com.dtstack.chunjun.connector.jdbc.conf.JdbcConf;
import com.dtstack.chunjun.connector.jdbc.dialect.JdbcDialect;
import com.dtstack.chunjun.connector.jdbc.sink.JdbcOutputFormat;
import com.dtstack.chunjun.converter.ISerializationConverter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
import com.qlangtech.tis.compiler.streamcode.CompileAndPackage;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.IncrSelectedTabExtend;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import com.qlangtech.tis.plugins.incr.flink.cdc.AbstractRowDataMapper;
import com.qlangtech.tis.plugins.incr.flink.chunjun.kafka.format.FormatFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;
import com.qlangtech.tis.realtime.transfer.DTO;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Preconditions;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * https://blog.csdn.net/weixin_39801446/article/details/124470698
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-25 20:40
 **/
public class ChujunRabbitMQSinkFactory extends ChunjunSinkFactory {

    @FormField(ordinal = 2, validate = {Validator.require})
    public FormatFactory format;


    @Override
    protected boolean supportUpsetDML() {
        return true;
    }

    @Override
    protected Class<? extends JdbcDialect> getJdbcDialectClass() {
        return null;
    }

    @Override
    protected JdbcOutputFormat createChunjunOutputFormat(DataSourceFactory dsFactory, JdbcConf jdbcConf) {
        return null;
//        CreateChunjunSinkFunctionResult sinkFuncRef = new CreateChunjunSinkFunctionResult();
//        KafkaSelectedTab kfkTable = (KafkaSelectedTab) selectedTab;
//
//        DataXKafkaWriter dataXWriter = (DataXKafkaWriter) dataxProcessor.getWriter(null);
//
//        KafkaConf kafkaConf = new KafkaConf();
//        //
//        kafkaConf.setPartitionAssignColumns(kfkTable.partitionFields);
//        kafkaConf.setTableFields(
//                kfkTable.getCols().stream().map((col) -> col.getName()).collect(Collectors.toList()));
//        kafkaConf.setTableName(targetTabName);
//        kafkaConf.setTopic(dataXWriter.topic);
//
//        kafkaConf.setProducerSettings(dataXWriter.buildKafkaConfig());
//
//        SyncConf syncConf = createSyncConf(selectedTab, () -> {
//            Map<String, Object> params = Maps.newHashMap();
//            return params;
//        });
//RMQSink<IN>
//        RabbitMQSSinkFactory sinkFactory = new KafkaSinkFactory(syncConf, kafkaConf) {
//            @Override
//            protected KafkaProducer createKafkaProducer(Properties props, RowSerializationSchema rowSerializationSchema) {
//
//                TISKafkaProducer kafkaProducer = new TISKafkaProducer(
//                        kafkaConf.getTopic(),
//                        rowSerializationSchema,
//                        props,
//                        FlinkKafkaProducer.Semantic.AT_LEAST_ONCE,
//                        FlinkKafkaProducer.DEFAULT_KAFKA_PRODUCERS_POOL_SIZE);
//
//                sinkFuncRef.setSinkFunction(kafkaProducer);
//                return kafkaProducer;
//            }
//
//            @Override
//            protected Function<FieldConf, ISerializationConverter<Map<String, Object>>> getSerializationConverterFactory() {
//                return (fieldCfg) -> {
//                    IColMetaGetter cmGetter = IColMetaGetter.create(fieldCfg.getName(), fieldCfg.getType());
//                    //colsMetas.add(cmGetter);
//                    FlinkCol flinkCol = AbstractRowDataMapper.mapFlinkCol(cmGetter, fieldCfg.getIndex());
//                    return new KafkaSerializationConverter(flinkCol);
//                };
//            }
//
//            @Override
//            protected RowSerializationSchema createRowSerializationSchema(KafkaColumnConverter keyConverter) {
//
//                Function<FieldConf, ISerializationConverter<Map<String, Object>>> serializationConverterFactory = getSerializationConverterFactory();
//                Preconditions.checkNotNull(serializationConverterFactory, "serializationConverterFactory can not be null");
//                return new RowSerializationSchema(
//                        kafkaConf,
//                        new CustomerFlinkPartition<>(),
//                        keyConverter,
//                        KafkaColumnConverter.create(this.syncConf, kafkaConf, serializationConverterFactory)) {
//
//                    private DTO.EventType parseEvnet(RowKind rowKind) {
//                        switch (rowKind) {
//                            case UPDATE_BEFORE:
//                                // return DTO.EventType.UPDATE_BEFORE;
//                                throw new IllegalStateException("unsupport type:" + rowKind);
//                            case UPDATE_AFTER:
//                                return DTO.EventType.UPDATE_AFTER;
//                            case DELETE:
//                                return DTO.EventType.DELETE;
//                            case INSERT:
//                                return DTO.EventType.ADD;
//                        }
//
//                        throw new IllegalStateException("illegal rowKind:" + rowKind);
//                    }
//
//                    @Override
//                    public Map<String, Object> createRowVals(String tableName, RowKind rowKind, Map<String, Object> data) {
//                        return DataXKafkaWriter.createRowVals(tableName, parseEvnet(rowKind), data);
//                    }
//                };
//            }
//        };
//
//
//        sinkFuncRef.setSinkFactory(sinkFactory);
//        sinkFuncRef.initialize();
//        sinkFuncRef.setSinkCols(new TableCols(selectedTab.getCols()));
//
//        //Objects.requireNonNull(sinkFuncRef.get(), "sinkFunc can not be null");
//        sinkFuncRef.setParallelism(this.parallelism);
//        return sinkFuncRef;
    }

    @Override
    protected void initChunjunJdbcConf(JdbcConf jdbcConf) {

    }

    @Override
    public ICompileAndPackage getCompileAndPackageManager() {
        return new CompileAndPackage(Sets.newHashSet(ChujunRabbitMQSinkFactory.class));
    }

    @TISExtension
    public static class DefaultDesc extends BasicChunjunSinkDescriptor {
        @Override
        protected EndType getTargetType() {
            return EndType.RabbitMQ;
        }

        @Override
        public Descriptor<IncrSelectedTabExtend> getSelectedTableExtendDescriptor() {
            return null;
        }
    }
}
