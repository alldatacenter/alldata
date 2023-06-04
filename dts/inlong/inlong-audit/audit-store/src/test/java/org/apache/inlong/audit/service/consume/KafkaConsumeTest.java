/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.audit.service.consume;

import org.apache.inlong.audit.config.ClickHouseConfig;
import org.apache.inlong.audit.config.MessageQueueConfig;
import org.apache.inlong.audit.config.StoreConfig;
import org.apache.inlong.audit.db.dao.AuditDataDao;
import org.apache.inlong.audit.service.ClickHouseService;
import org.apache.inlong.audit.service.ElasticsearchService;
import org.apache.inlong.audit.service.InsertData;
import org.apache.inlong.audit.service.MySqlService;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KafkaConsumeTest {

    private KafkaConsumer consumer;
    private AuditDataDao auditDataDao;
    private ElasticsearchService esService;
    private ClickHouseConfig ckConfig;
    private StoreConfig storeConfig;
    private MessageQueueConfig mqConfig;
    private String topic = "inlong-audit";
    private ConsumerRecords records;

    @Before
    public void setUp() {
        consumer = mock(KafkaConsumer.class);
        consumer.subscribe(Collections.singleton(topic));
        records = mock(ConsumerRecords.class);
        when(consumer.poll(Duration.ofMillis(100))).thenReturn(records);
    }

    /**
     * test kafka consumer
     */
    @Test
    public void testConsumer() {
        List<InsertData> insertServiceList = this.getInsertServiceList();
        Thread consumeFetch = new Thread(new KafkaConsume(insertServiceList, storeConfig, mqConfig).new Fetcher(
                consumer, topic, true, 100), "Fetch_Thread");
        consumeFetch.start();
        consumeFetch.interrupt();
    }

    /**
     * getInsertServiceList
     *
     * @return InsertDataList
     */
    private List<InsertData> getInsertServiceList() {
        List<InsertData> insertData = new ArrayList<>();
        insertData.add(new MySqlService(auditDataDao));
        insertData.add(esService);
        insertData.add(new ClickHouseService(ckConfig));
        return insertData;
    }
}
