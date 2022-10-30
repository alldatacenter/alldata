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

package org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.bdbimpl;

import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.server.common.exception.LoadMetaException;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbTopicAuthControlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.AbsTopicCtrlMapperImpl;

public class BdbTopicCtrlMapperImpl extends AbsTopicCtrlMapperImpl {
    // Topic control store
    private EntityStore topicCtrlStore;
    private final PrimaryIndex<String/* topicName */, BdbTopicAuthControlEntity> topicCtrlIndex;

    public BdbTopicCtrlMapperImpl(ReplicatedEnvironment repEnv, StoreConfig storeConfig) {
        super();
        topicCtrlStore = new EntityStore(repEnv,
                TBDBStoreTables.BDB_TOPIC_AUTH_CONTROL_STORE_NAME, storeConfig);
        topicCtrlIndex =
                topicCtrlStore.getPrimaryIndex(String.class, BdbTopicAuthControlEntity.class);
    }

    @Override
    public void close() {
        clearCachedData();
        if (topicCtrlStore != null) {
            try {
                topicCtrlStore.close();
                topicCtrlStore = null;
            } catch (Throwable e) {
                logger.error("[BDB Impl] close topic control failure ", e);
            }
        }
        logger.info("[BDB Impl] topic configure closed!");
    }

    @Override
    public void loadConfig(StringBuilder strBuff) throws LoadMetaException {
        long totalCnt = 0L;
        EntityCursor<BdbTopicAuthControlEntity> cursor = null;
        logger.info("[BDB Impl] load topic configure start...");
        // clear cache data
        clearCachedData();
        // load data from bdb
        try {
            cursor = topicCtrlIndex.entities();
            for (BdbTopicAuthControlEntity bdbEntity : cursor) {
                if (bdbEntity == null) {
                    logger.warn("[BDB Impl] found Null data while loading topic control!");
                    continue;
                }
                putRecord2Caches(new TopicCtrlEntity(bdbEntity));
                totalCnt++;
            }
        } catch (Exception e) {
            logger.error("[BDB Impl] load topic control failure ", e);
            throw new LoadMetaException(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logger.info(strBuff.append("[BDB Impl] loaded ").append(totalCnt)
                .append(" topic control configure successfully...").toString());
        strBuff.delete(0, strBuff.length());
    }

    protected boolean putConfig2Persistent(TopicCtrlEntity entity,
                                           StringBuilder strBuff, ProcessResult result) {
        BdbTopicAuthControlEntity bdbEntity =
                entity.buildBdbTopicAuthControlEntity();
        try {
            topicCtrlIndex.put(bdbEntity);
        } catch (Throwable e) {
            logger.error("[BDB Impl] put topic control failure ", e);
            result.setFailResult(DataOpErrCode.DERR_STORE_ABNORMAL.getCode(),
                    strBuff.append("Put topic control configure failure: ")
                            .append(e.getMessage()).toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }

    protected boolean delConfigFromPersistent(String topicName, StringBuilder strBuff) {
        try {
            topicCtrlIndex.delete(topicName);
        } catch (Throwable e) {
            logger.error("[BDB Impl] delete topic control failure ", e);
            return false;
        }
        return true;
    }
}
