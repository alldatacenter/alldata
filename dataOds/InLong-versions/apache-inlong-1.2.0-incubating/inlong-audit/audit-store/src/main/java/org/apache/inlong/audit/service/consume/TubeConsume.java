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

import com.google.common.base.Preconditions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.inlong.audit.config.MessageQueueConfig;
import org.apache.inlong.audit.config.StoreConfig;
import org.apache.inlong.audit.db.dao.AuditDataDao;
import org.apache.inlong.audit.service.ElasticsearchService;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.consumer.ConsumePosition;
import org.apache.inlong.tubemq.client.consumer.ConsumerResult;
import org.apache.inlong.tubemq.client.consumer.PullMessageConsumer;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.client.factory.TubeMultiSessionFactory;
import org.apache.inlong.tubemq.corebase.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class TubeConsume extends BaseConsume {

    private static final Logger LOG = LoggerFactory.getLogger(TubeConsume.class);
    private PullMessageConsumer pullConsumer;
    private TubeMultiSessionFactory sessionFactory;
    private String masterUrl;
    private String topic;
    private int fetchThreadCnt = 4;

    public TubeConsume(AuditDataDao auditDataDao, ElasticsearchService esService, StoreConfig storeConfig,
            MessageQueueConfig mqConfig) {
        super(auditDataDao, esService, storeConfig, mqConfig);
    }

    @Override
    public void start() {
        masterUrl = mqConfig.getTubeMasterList();
        Preconditions.checkArgument(StringUtils.isNotEmpty(masterUrl), "no tube masterUrlList specified");
        topic = mqConfig.getTubeTopic();
        Preconditions.checkArgument(StringUtils.isNotEmpty(topic), "no tube topic specified");
        fetchThreadCnt = mqConfig.getTubeThreadNum();
        Preconditions.checkArgument(StringUtils.isNotEmpty(mqConfig.getTubeConsumerGroupName()),
                "no tube consumer groupName specified");

        initConsumer();

        Thread[] fetchRunners = new Thread[fetchThreadCnt];
        for (int i = 0; i < fetchThreadCnt; i++) {
            fetchRunners[i] = new Thread(new Fetcher(pullConsumer, topic), "TubeConsume_Fetcher_Thread_" + i);
            fetchRunners[i].start();
        }
    }

    private void initConsumer() {
        LOG.info("init tube consumer, topic:{}, masterList:{}", topic, masterUrl);
        ConsumerConfig consumerConfig = new ConsumerConfig(masterUrl, mqConfig.getTubeConsumerGroupName());
        consumerConfig.setConsumePosition(ConsumePosition.CONSUMER_FROM_LATEST_OFFSET);
        try {
            sessionFactory = new TubeMultiSessionFactory(consumerConfig);
            pullConsumer = sessionFactory.createPullConsumer(consumerConfig);
            pullConsumer.subscribe(topic, null);
            pullConsumer.completeSubscribe();
        } catch (TubeClientException e) {
            LOG.error("init tube consumer error {}", e.getMessage());
        }

    }

    public class Fetcher implements Runnable {

        private final PullMessageConsumer pullMessageConsumer;
        private String topic;

        public Fetcher(PullMessageConsumer pullMessageConsumer, String topic) {
            this.pullMessageConsumer = pullMessageConsumer;
            this.topic = topic;
        }

        @Override
        public void run() {
            ConsumerResult csmResult;

            // wait partition status ready
            while (true) {
                if (pullMessageConsumer.isPartitionsReady(5000) || pullMessageConsumer.isShutdown()) {
                    LOG.warn("tube partition is not ready or consumer is shutdown!");
                    break;
                }
            }
            // consume messages
            while (true) {
                if (pullMessageConsumer.isShutdown()) {
                    LOG.warn("consumer is shutdown!");
                    break;
                }

                try {
                    csmResult = pullMessageConsumer.getMessage();
                    if (csmResult.isSuccess()) {
                        List<Message> messageList = csmResult.getMessageList();
                        if (CollectionUtils.isNotEmpty(messageList)) {
                            for (Message message : messageList) {
                                if (StringUtils.equals(message.getTopic(), topic)) {
                                    String body = new String(message.getData(), StandardCharsets.UTF_8);
                                    handleMessage(body);
                                }
                            }
                        }
                        pullMessageConsumer.confirmConsume(csmResult.getConfirmContext(), true);
                    } else {
                        LOG.error("receive messages errorCode is {}, error meddage is {}", csmResult.getErrCode(),
                                csmResult.getErrMsg());
                    }
                } catch (TubeClientException e) {
                    LOG.error("tube consumer getMessage error {}", e.getMessage());
                } catch (Exception e) {
                    LOG.error("handle audit message error {}", e.getMessage());
                }

            }
        }
    }
}
