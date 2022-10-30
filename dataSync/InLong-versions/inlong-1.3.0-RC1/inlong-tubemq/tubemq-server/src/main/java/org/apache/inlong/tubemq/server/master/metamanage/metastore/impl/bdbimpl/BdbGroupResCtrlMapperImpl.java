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
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbGroupFlowCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.AbsGroupResCtrlMapperImpl;

public class BdbGroupResCtrlMapperImpl extends AbsGroupResCtrlMapperImpl {
    // consumer group configure store
    private EntityStore groupConfStore;
    private final PrimaryIndex<String/* groupName */, BdbGroupFlowCtrlEntity> groupBaseCtrlIndex;

    public BdbGroupResCtrlMapperImpl(ReplicatedEnvironment repEnv, StoreConfig storeConfig) {
        super();
        groupConfStore = new EntityStore(repEnv,
                TBDBStoreTables.BDB_GROUP_FLOW_CONTROL_STORE_NAME, storeConfig);
        groupBaseCtrlIndex =
                groupConfStore.getPrimaryIndex(String.class, BdbGroupFlowCtrlEntity.class);
    }

    @Override
    public void close() {
        clearCachedData();
        if (groupConfStore != null) {
            try {
                groupConfStore.close();
                groupConfStore = null;
            } catch (Throwable e) {
                logger.error("[BDB Impl] close group control configure failure ", e);
            }
        }
        logger.info("[BDB Impl] group control configure closed!");
    }

    @Override
    public void loadConfig(StringBuilder strBuff) throws LoadMetaException {
        long totalCnt = 0L;
        EntityCursor<BdbGroupFlowCtrlEntity> cursor = null;
        logger.info("[BDB Impl] load group control configure start...");
        clearCachedData();
        try {
            cursor = groupBaseCtrlIndex.entities();
            for (BdbGroupFlowCtrlEntity bdbEntity : cursor) {
                if (bdbEntity == null) {
                    logger.warn("[BDB Impl] null data while loading group control configure!");
                    continue;
                }
                putRecord2Caches(new GroupResCtrlEntity(bdbEntity));
                totalCnt++;
            }
        } catch (Exception e) {
            logger.error("[BDB Impl] load group control configure failure ", e);
            throw new LoadMetaException(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logger.info(strBuff.append("[BDB Impl] loaded ").append(totalCnt)
                .append(" group control configure successfully...").toString());
        strBuff.delete(0, strBuff.length());
    }

    protected boolean putConfig2Persistent(GroupResCtrlEntity entity,
                                           StringBuilder strBuff, ProcessResult result) {
        BdbGroupFlowCtrlEntity bdbEntity =
                entity.buildBdbGroupFlowCtrlEntity();
        try {
            groupBaseCtrlIndex.put(bdbEntity);
        } catch (Throwable e) {
            logger.error("[BDB Impl] put group control configure failure ", e);
            result.setFailResult(DataOpErrCode.DERR_STORE_ABNORMAL.getCode(),
                    strBuff.append("Put group control configure failure: ")
                            .append(e.getMessage()).toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }

    protected boolean delConfigFromPersistent(String recordKey, StringBuilder strBuff) {
        try {
            groupBaseCtrlIndex.delete(recordKey);
        } catch (Throwable e) {
            logger.error("[BDB Impl] delete control configure failure ", e);
            return false;
        }
        return true;
    }
}
