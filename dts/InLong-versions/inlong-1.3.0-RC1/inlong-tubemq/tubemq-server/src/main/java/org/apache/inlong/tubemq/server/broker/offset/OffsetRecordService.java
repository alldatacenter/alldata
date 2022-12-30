/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.broker.offset;

import java.util.Map;
import org.apache.inlong.tubemq.corebase.daemon.AbstractDaemonService;
import org.apache.inlong.tubemq.corebase.utils.AddressUtils;
import org.apache.inlong.tubemq.corebase.utils.ServiceStatusHolder;
import org.apache.inlong.tubemq.server.broker.TubeBroker;
import org.apache.inlong.tubemq.server.broker.metadata.TopicMetadata;
import org.apache.inlong.tubemq.server.broker.msgstore.MessageStoreManager;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * History offset service
 *
 */
public class OffsetRecordService extends AbstractDaemonService {
    private static final Logger logger =
            LoggerFactory.getLogger(OffsetRecordService.class);
    private final TubeBroker broker;
    // tube broker's store manager
    private final MessageStoreManager storeManager;
    // tube broker's offset manager
    private final OffsetService offsetManager;
    // tube broker addressId
    private final int brokerAddrId;

    public OffsetRecordService(TubeBroker broker, long scanIntervalMs) {
        super("History-Offset", scanIntervalMs);
        this.broker = broker;
        this.storeManager = this.broker.getStoreManager();
        this.offsetManager = this.broker.getOffsetManager();
        this.brokerAddrId = AddressUtils.ipToInt(broker.getTubeConfig().getHostName());
        super.start();
    }

    @Override
    protected void loopProcess(long intervalMs) {
        StringBuilder strBuff = new StringBuilder(2048);
        logger.info("[Offset-Record Service] start offset-record service thread");
        while (!super.isStopped()) {
            try {
                Thread.sleep(intervalMs);
                // get group offset information
                storeRecord2LocalTopic(strBuff);
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                //
            }
        }
        logger.info("[Offset-Record Service] exit offset-record service thread");
    }

    public void close() {
        if (super.stop()) {
            return;
        }
        StringBuilder strBuff = new StringBuilder(2048);
        storeRecord2LocalTopic(strBuff);
        logger.info("[Offset-Record Service] offset-record service stopped!");
    }

    private void storeRecord2LocalTopic(StringBuilder strBuff) {
        // check node writable status
        if (ServiceStatusHolder.isWriteServiceStop()) {
            return;
        }
        // check topic writable status
        TopicMetadata topicMetadata = storeManager.getMetadataManager()
                .getTopicMetadata(TServerConstants.OFFSET_HISTORY_NAME);
        if (!topicMetadata.isAcceptPublish()) {
            return;
        }
        // get group offset information
        Map<String, OffsetRecordInfo> groupOffsetMap =
                offsetManager.getOnlineGroupOffsetInfo();
        if (groupOffsetMap == null || groupOffsetMap.isEmpty()) {
            return;
        }
        // get topic's publish information
        storeManager.getTopicPublishInfos(groupOffsetMap);
        // store group offset records to offset storage topic
        broker.getBrokerServiceServer().appendGroupOffsetInfo(groupOffsetMap,
                brokerAddrId, System.currentTimeMillis(), 10, 3, strBuff);
    }
}
