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

import org.apache.inlong.audit.db.dao.AuditDataDao;
import org.apache.inlong.audit.db.entities.AuditDataPo;
import org.apache.inlong.audit.protocol.AuditData;

import java.util.Date;

/**
 * MySqlService
 */
public class MySqlService implements InsertData {

    private final AuditDataDao dao;

    public MySqlService(AuditDataDao dao) {
        this.dao = dao;
    }

    @Override
    public void insert(AuditData msgBody) {
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
        dao.insert(po);
    }

}
