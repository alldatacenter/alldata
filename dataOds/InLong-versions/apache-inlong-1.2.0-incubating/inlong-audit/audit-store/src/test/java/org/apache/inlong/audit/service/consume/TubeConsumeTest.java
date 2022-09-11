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

import org.apache.inlong.audit.config.MessageQueueConfig;
import org.apache.inlong.audit.config.StoreConfig;
import org.apache.inlong.audit.db.dao.AuditDataDao;
import org.apache.inlong.audit.service.ElasticsearchService;
import org.apache.inlong.tubemq.client.consumer.ConsumerResult;
import org.apache.inlong.tubemq.client.consumer.PullMessageConsumer;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TubeConsumeTest {

    private PullMessageConsumer pullMessageConsumer;
    private AuditDataDao auditDataDao;
    private ElasticsearchService esService;
    private StoreConfig storeConfig;
    private MessageQueueConfig mqConfig;
    private String topic = "inlong-audit";
    private ConsumerResult consumerResult;

    @Before
    public void setUp() throws TubeClientException {
        pullMessageConsumer = mock(PullMessageConsumer.class);
        consumerResult = mock(ConsumerResult.class);
        pullMessageConsumer.subscribe(topic, null);
        pullMessageConsumer.completeSubscribe();
        when(pullMessageConsumer.isPartitionsReady(5000)).thenReturn(true);
        when(pullMessageConsumer.isShutdown()).thenReturn(true);
        when(pullMessageConsumer.getMessage()).thenReturn(consumerResult);
        when(consumerResult.isSuccess()).thenReturn(false);
    }

    @Test
    public void testConsume() throws InterruptedException {
        Thread consumeFetch = new Thread(new TubeConsume(auditDataDao, esService, storeConfig, mqConfig).new Fetcher(
                pullMessageConsumer, topic), "fetch thread");
        consumeFetch.start();
        consumeFetch.interrupt();
    }
}
