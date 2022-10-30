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
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbGroupFilterCondEntity;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupConsumeCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.AbsConsumeCtrlMapperImpl;

public class BdbConsumeCtrlMapperImpl extends AbsConsumeCtrlMapperImpl {
    // consume control store
    private EntityStore groupConsumeStore;
    private final PrimaryIndex<String/* recordKey */, BdbGroupFilterCondEntity> groupConsumeIndex;

    public BdbConsumeCtrlMapperImpl(ReplicatedEnvironment repEnv, StoreConfig storeConfig) {
        super();
        groupConsumeStore = new EntityStore(repEnv,
                TBDBStoreTables.BDB_GROUP_FILTER_COND_STORE_NAME, storeConfig);
        groupConsumeIndex =
                groupConsumeStore.getPrimaryIndex(String.class, BdbGroupFilterCondEntity.class);
    }

    @Override
    public void close() {
        clearCachedData();
        if (groupConsumeStore != null) {
            try {
                groupConsumeStore.close();
                groupConsumeStore = null;
            } catch (Throwable e) {
                logger.error("[BDB Impl] close consume control configure failure ", e);
            }
        }
        logger.info("[BDB Impl] consume control configure closed!");
    }

    @Override
    public void loadConfig(StringBuilder strBuff) throws LoadMetaException {
        long totalCnt = 0L;
        EntityCursor<BdbGroupFilterCondEntity> cursor = null;
        logger.info("[BDB Impl] load consume control configure start...");
        try {
            clearCachedData();
            cursor = groupConsumeIndex.entities();
            for (BdbGroupFilterCondEntity bdbEntity : cursor) {
                if (bdbEntity == null) {
                    logger.warn("[BDB Impl] found Null data while loading consume control configure!");
                    continue;
                }
                putRecord2Caches(new GroupConsumeCtrlEntity(bdbEntity));
                totalCnt++;
            }
        } catch (Exception e) {
            logger.error("[BDB Impl] load consume control configure failure ", e);
            throw new LoadMetaException(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logger.info(strBuff.append("[BDB Impl] loaded ").append(totalCnt)
                .append(" consume control configure successfully...").toString());
        strBuff.delete(0, strBuff.length());
    }

    protected boolean putConfig2Persistent(GroupConsumeCtrlEntity entity,
                                           StringBuilder strBuff, ProcessResult result) {
        BdbGroupFilterCondEntity bdbEntity =
                entity.buildBdbGroupFilterCondEntity();
        try {
            groupConsumeIndex.put(bdbEntity);
        } catch (Throwable e) {
            logger.error("[BDB Impl] put consume control configure failure ", e);
            result.setFailResult(DataOpErrCode.DERR_STORE_ABNORMAL.getCode(),
                    strBuff.append("Put consume control configure failure: ")
                            .append(e.getMessage()).toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }

    protected boolean delConfigFromPersistent(String recordKey, StringBuilder strBuff) {
        try {
            groupConsumeIndex.delete(recordKey);
        } catch (Throwable e) {
            logger.error("[BDB Impl] delete consume control configure failure ", e);
            return false;
        }
        return true;
    }
}
