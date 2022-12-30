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
import org.apache.inlong.audit.protocol.AuditData;
import org.apache.inlong.audit.service.InsertData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class BaseConsume {

    private static final Logger LOG = LoggerFactory.getLogger(BaseConsume.class);

    private final Gson gson = new Gson();

    protected List<InsertData> insertServiceList;
    protected StoreConfig storeConfig;
    protected MessageQueueConfig mqConfig;

    public BaseConsume(List<InsertData> insertServiceList, StoreConfig storeConfig,
            MessageQueueConfig mqConfig) {
        this.insertServiceList = insertServiceList;
        this.storeConfig = storeConfig;
        this.mqConfig = mqConfig;
    }

    public abstract void start();

    /**
     * handleMessage
     * @param body
     * @throws Exception
     */
    protected void handleMessage(String body) throws Exception {
        AuditData msgBody = gson.fromJson(body, AuditData.class);
        this.insertServiceList.forEach((service) -> {
            try {
                service.insert(msgBody);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });
    }

}
