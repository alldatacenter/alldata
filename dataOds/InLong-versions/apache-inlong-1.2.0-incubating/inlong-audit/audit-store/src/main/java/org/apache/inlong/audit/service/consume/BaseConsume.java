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

import com.google.gson.Gson;
import org.apache.inlong.audit.config.MessageQueueConfig;
import org.apache.inlong.audit.config.StoreConfig;
import org.apache.inlong.audit.db.dao.AuditDataDao;
import org.apache.inlong.audit.db.entities.AuditDataPo;
import org.apache.inlong.audit.db.entities.ESDataPo;
import org.apache.inlong.audit.protocol.AuditData;
import org.apache.inlong.audit.service.ElasticsearchService;

import java.util.Date;

public abstract class BaseConsume {

    private final Gson gson = new Gson();

    protected AuditDataDao auditDataDao;
    protected ElasticsearchService esService;
    protected StoreConfig storeConfig;
    protected MessageQueueConfig mqConfig;

    public BaseConsume(AuditDataDao auditDataDao, ElasticsearchService esService, StoreConfig storeConfig,
            MessageQueueConfig mqConfig) {
        this.auditDataDao = auditDataDao;
        this.esService = esService;
        this.storeConfig = storeConfig;
        this.mqConfig = mqConfig;
    }

    public abstract void start();

    protected void handleMessage(String body) throws Exception {
        AuditData msgBody = gson.fromJson(body, AuditData.class);
        if (storeConfig.isMysqlStore()) {
            AuditDataPo po = new AuditDataPo();
            po.setIp(msgBody.getIp());
            po.setThreadId(msgBody.getThreadId());
            po.setDockerId(msgBody.getDockerId());
            po.setPacketId(msgBody.getPacketId());
            po.setSdkTs(new Date(msgBody.getSdkTs()));
            po.setLogTs(new Date(msgBody.getLogTs()));
            po.setAuditId(msgBody.getAuditId());
            po.setCount(msgBody.getCount());
            po.setDelay(msgBody.getDelay());
            po.setInlongGroupId(msgBody.getInlongGroupId());
            po.setInlongStreamId(msgBody.getInlongStreamId());
            po.setSize(msgBody.getSize());
            auditDataDao.insert(po);
        }
        if (storeConfig.isElasticsearchStore()) {
            ESDataPo esPo = new ESDataPo();
            esPo.setIp(msgBody.getIp());
            esPo.setThreadId(msgBody.getThreadId());
            esPo.setDockerId(msgBody.getDockerId());
            esPo.setSdkTs(new Date(msgBody.getSdkTs()).getTime());
            esPo.setLogTs(new Date(msgBody.getLogTs()));
            esPo.setAuditId(msgBody.getAuditId());
            esPo.setCount(msgBody.getCount());
            esPo.setDelay(msgBody.getDelay());
            esPo.setInlongGroupId(msgBody.getInlongGroupId());
            esPo.setInlongStreamId(msgBody.getInlongStreamId());
            esPo.setSize(msgBody.getSize());
            esPo.setPacketId(msgBody.getPacketId());
            esService.insertData(esPo);
        }
    }

}
