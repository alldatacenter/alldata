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

package com.qlangtech.tis.plugins.incr.flink.chunjun.kafka.sink;

import com.qlangtech.plugins.incr.flink.chunjun.doris.sink.TestFlinkSinkExecutor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugins.datax.kafka.writer.DataXKafkaWriter;
import com.qlangtech.tis.plugins.datax.kafka.writer.KafkaSelectedTab;
import com.qlangtech.tis.plugins.datax.kafka.writer.protocol.KafkaPlaintext;
import com.qlangtech.tis.plugins.incr.flink.chunjun.kafka.format.TISCanalJsonFormatFactory;
import com.qlangtech.tis.plugins.incr.flink.chunjun.script.ChunjunSqlType;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-17 17:23
 **/
public class TestChujunKafkaSinkFactoryIntegration extends TestFlinkSinkExecutor {

    private static KafkaContainer kafka;
    private static final String TOPIC_NAME = "test.topic";


    @Test
    public void testSinkSync() throws Exception {
        super.testSinkSync();
    }

    @BeforeClass
    public static void initializeKafka() throws Exception {
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.0"));
        kafka.start();
    }

    @Override
    protected SelectedTab createSelectedTab(List<CMeta> metaCols) {
        //  KafkaSelectedTab kfkTable = (KafkaSelectedTab) selectedTab;
        return new KafkaSelectedTab() {
            @Override
            public List<CMeta> getCols() {
                return metaCols;
            }
        };
    }

    @Override
    protected BasicDataSourceFactory getDsFactory() {
        return null;
    }

    @Override
    protected DataxWriter createDataXWriter() {
        DataXKafkaWriter writer = new DataXKafkaWriter();

//        final ObjectNode stubProtocolConfig = mapper.createObjectNode();
//        stubProtocolConfig.put("security_protocol", KafkaProtocol.PLAINTEXT.toString());


        writer.bootstrapServers = kafka.getBootstrapServers();
        writer.topic = "tis.stream." + TOPIC_NAME;
        writer.syncProducer = true;
        writer.protocol = new KafkaPlaintext();
        writer.clientId = "test-client";
        writer.acks = "all";
        writer.enableIdempotence = true;
        writer.compressionType = "none";
        writer.batchSize = 16384;
        writer.lingerMs = "0";
        writer.maxInFlightRequestsPerConnection = 5;
        writer.clientDnsLookup = "use_all_dns_ips";
        writer.bufferMemory = "33554432";
        writer.maxRequestSize = 1048576;
        writer.retries = 2147483647;
        writer.socketConnectionSetupTimeoutMs = "10000";
        writer.socketConnectionSetupTimeoutMaxMs = "30000";
        writer.maxBlockMs = "60000";
        writer.requestTimeoutMs = 30000;
        writer.deliveryTimeoutMs = 120000;
        writer.sendBufferBytes = -1;
        writer.receiveBufferBytes = -1;

        return writer;
    }


    @Override
    protected ChunjunSinkFactory getSinkFactory() {
        ChujunKafkaSinkFactory dorisSinkFactory = new ChujunKafkaSinkFactory();
        ChunjunSqlType chunjunSqlType = new ChunjunSqlType();
        dorisSinkFactory.scriptType = chunjunSqlType;
        dorisSinkFactory.format = new TISCanalJsonFormatFactory();
        return dorisSinkFactory;
    }


    @AfterClass
    public static void afterTestSinkSync() throws Exception {
        //  environment.stop();
        kafka.close();
    }
}
