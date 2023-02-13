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

package org.apache.inlong.audit.service;

import org.apache.inlong.audit.config.ClickHouseConfig;
import org.apache.inlong.audit.config.MessageQueueConfig;
import org.apache.inlong.audit.config.StoreConfig;
import org.apache.inlong.audit.db.dao.AuditDataDao;
import org.apache.inlong.audit.service.consume.BaseConsume;
import org.apache.inlong.audit.service.consume.PulsarConsume;
import org.apache.inlong.audit.service.consume.TubeConsume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditMsgConsumerServer implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(AuditMsgConsumerServer.class);
    @Autowired
    private MessageQueueConfig mqConfig;
    @Autowired
    private AuditDataDao auditDataDao;
    @Autowired
    private ElasticsearchService esService;
    @Autowired
    private StoreConfig storeConfig;
    @Autowired
    private ClickHouseConfig chConfig;
    // ClickHouseService
    private ClickHouseService ckService;

    /**
     * Initializing bean
     */
    public void afterPropertiesSet() {
        BaseConsume mqConsume = null;
        List<InsertData> insertServiceList = this.getInsertServiceList();
        if (mqConfig.isPulsar()) {
            mqConsume = new PulsarConsume(insertServiceList, storeConfig, mqConfig);
        } else if (mqConfig.isTube()) {
            mqConsume = new TubeConsume(insertServiceList, storeConfig, mqConfig);
        } else {
            LOG.error("unkown MessageQueue {}", mqConfig.getMqType());
            return;
        }

        if (storeConfig.isElasticsearchStore()) {
            esService.startTimerRoutine();
        }
        if (storeConfig.isClickHouseStore()) {
            ckService.start();
        }
        mqConsume.start();
    }

    /**
     * getInsertServiceList
     * @return
     */
    private List<InsertData> getInsertServiceList() {
        List<InsertData> insertServiceList = new ArrayList<>();
        if (storeConfig.isMysqlStore()) {
            insertServiceList.add(new MySqlService(auditDataDao));
        }
        if (storeConfig.isElasticsearchStore()) {
            insertServiceList.add(esService);
        }
        if (storeConfig.isClickHouseStore()) {
            // create ck object
            ckService = new ClickHouseService(chConfig);
            insertServiceList.add(ckService);
        }
        return insertServiceList;
    }
}
