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
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbBrokerConfEntity;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.AbsBrokerConfigMapperImpl;

public class BdbBrokerConfigMapperImpl extends AbsBrokerConfigMapperImpl {
    // broker config store
    private EntityStore brokerConfStore;
    private final PrimaryIndex<Integer/* brokerId */, BdbBrokerConfEntity> brokerConfIndex;

    public BdbBrokerConfigMapperImpl(ReplicatedEnvironment repEnv, StoreConfig storeConfig) {
        super();
        brokerConfStore = new EntityStore(repEnv,
                TBDBStoreTables.BDB_BROKER_CONFIG_STORE_NAME, storeConfig);
        brokerConfIndex =
                brokerConfStore.getPrimaryIndex(Integer.class, BdbBrokerConfEntity.class);
    }

    @Override
    public void close() {
        clearCachedData();
        if (brokerConfStore != null) {
            try {
                brokerConfStore.close();
                brokerConfStore = null;
            } catch (Throwable e) {
                logger.error("[BDB Impl] close broker configure failure ", e);
            }
        }
        logger.info("[BDB Impl] broker configure closed!");
    }

    @Override
    public void loadConfig(StringBuilder strBuff) throws LoadMetaException {
        long totalCnt = 0L;
        EntityCursor<BdbBrokerConfEntity> cursor = null;
        logger.info("[BDB Impl] load broker configure start...");
        // clear cached data
        clearCachedData();
        // load data from bdb
        try {
            cursor = brokerConfIndex.entities();
            for (BdbBrokerConfEntity bdbEntity : cursor) {
                if (bdbEntity == null) {
                    logger.warn("[BDB Impl] found Null data while loading broker configure!");
                    continue;
                }
                putRecord2Caches(new BrokerConfEntity(bdbEntity));
                totalCnt++;
            }
        } catch (Exception e) {
            logger.error("[BDB Impl] load broker configure failure ", e);
            throw new LoadMetaException(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logger.info(strBuff.append("[BDB Impl] loaded ").append(totalCnt)
                .append(" broker configure successfully...").toString());
        strBuff.delete(0, strBuff.length());
    }

    protected boolean putConfig2Persistent(BrokerConfEntity entity,
                                           StringBuilder strBuff,
                                           ProcessResult result) {
        BdbBrokerConfEntity bdbEntity =
                entity.buildBdbBrokerConfEntity();
        try {
            brokerConfIndex.put(bdbEntity);
        } catch (Throwable e) {
            logger.error("[BDB Impl] put broker configure failure ", e);
            result.setFailResult(DataOpErrCode.DERR_STORE_ABNORMAL.getCode(),
                    strBuff.append("Put broker configure failure: ")
                            .append(e.getMessage()).toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }

    protected boolean delConfigFromPersistent(int brokerId, StringBuilder strBuff) {
        try {
            brokerConfIndex.delete(brokerId);
        } catch (Throwable e) {
            logger.error("[BDB Impl] delete broker configure failure ", e);
            return false;
        }
        return true;
    }
}
