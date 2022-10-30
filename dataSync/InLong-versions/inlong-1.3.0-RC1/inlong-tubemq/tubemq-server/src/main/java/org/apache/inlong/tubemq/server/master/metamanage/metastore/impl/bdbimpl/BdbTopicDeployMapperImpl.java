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
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbTopicConfEntity;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicDeployEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.AbsTopicDeployMapperImpl;

public class BdbTopicDeployMapperImpl extends AbsTopicDeployMapperImpl {
    // Topic configure store
    private EntityStore topicConfStore;
    private final PrimaryIndex<String/* recordKey */, BdbTopicConfEntity> topicConfIndex;

    public BdbTopicDeployMapperImpl(ReplicatedEnvironment repEnv, StoreConfig storeConfig) {
        super();
        topicConfStore = new EntityStore(repEnv,
                TBDBStoreTables.BDB_TOPIC_CONFIG_STORE_NAME, storeConfig);
        topicConfIndex =
                topicConfStore.getPrimaryIndex(String.class, BdbTopicConfEntity.class);
    }

    @Override
    public void close() {
        clearCachedData();
        if (topicConfStore != null) {
            try {
                topicConfStore.close();
                topicConfStore = null;
            } catch (Throwable e) {
                logger.error("[BDB Impl] close topic deploy configure failure ", e);
            }
        }
        logger.info("[BDB Impl] topic deploy configure closed!");
    }

    @Override
    public void loadConfig(StringBuilder strBuff) throws LoadMetaException {
        long totalCnt = 0L;
        EntityCursor<BdbTopicConfEntity> cursor = null;
        logger.info("[BDB Impl] load topic deploy configure start...");
        clearCachedData();
        try {
            cursor = topicConfIndex.entities();
            for (BdbTopicConfEntity bdbEntity : cursor) {
                if (bdbEntity == null) {
                    logger.warn("[BDB Impl] found Null data while loading topic deploy configure!");
                    continue;
                }
                putRecord2Caches(new TopicDeployEntity(bdbEntity));
                totalCnt++;
            }
        } catch (Exception e) {
            logger.error("[BDB Impl] load topic deploy configure failure ", e);
            throw new LoadMetaException(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logger.info(strBuff.append("[BDB Impl] loaded ").append(totalCnt)
                .append(" topic deploy configure successfully...").toString());
        strBuff.delete(0, strBuff.length());
    }

    protected boolean putConfig2Persistent(TopicDeployEntity entity,
                                           StringBuilder strBuff, ProcessResult result) {
        BdbTopicConfEntity bdbEntity =
                entity.buildBdbTopicConfEntity();
        try {
            topicConfIndex.put(bdbEntity);
        } catch (Throwable e) {
            logger.error("[BDB Impl] put topic deploy configure failure ", e);
            result.setFailResult(DataOpErrCode.DERR_STORE_ABNORMAL.getCode(),
                    strBuff.append("Put topic deploy configure failure: ")
                            .append(e.getMessage()).toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }

    @Override
    protected boolean delConfigFromPersistent(String recordKey, StringBuilder strBuff) {
        try {
            topicConfIndex.delete(recordKey);
        } catch (Throwable e) {
            logger.error("[BDB Impl] delete topic deploy configure failure ", e);
            return false;
        }
        return true;
    }
}
