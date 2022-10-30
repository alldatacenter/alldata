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
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbClusterSettingEntity;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.ClusterSettingEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.AbsClusterConfigMapperImpl;

public class BdbClusterConfigMapperImpl extends AbsClusterConfigMapperImpl {
    // bdb store
    private EntityStore clsDefSettingStore;
    private final PrimaryIndex<String, BdbClusterSettingEntity> clsDefSettingIndex;

    public BdbClusterConfigMapperImpl(ReplicatedEnvironment repEnv, StoreConfig storeConfig) {
        super();
        clsDefSettingStore = new EntityStore(repEnv,
                TBDBStoreTables.BDB_CLUSTER_SETTING_STORE_NAME, storeConfig);
        clsDefSettingIndex =
                clsDefSettingStore.getPrimaryIndex(String.class, BdbClusterSettingEntity.class);
    }

    @Override
    public void close() {
        // clear cached data
        clearCachedData();
        // release bdb resource
        if (clsDefSettingStore != null) {
            try {
                clsDefSettingStore.close();
                clsDefSettingStore = null;
            } catch (Throwable e) {
                logger.error("[BDB Impl] close cluster configure failure ", e);
            }
        }
        logger.info("[BDB Impl] cluster configure closed!");
    }

    @Override
    public void loadConfig(StringBuilder strBuff) throws LoadMetaException {
        long totalCnt = 0L;
        EntityCursor<BdbClusterSettingEntity> cursor = null;
        logger.info("[BDB Impl] load cluster configure start...");
        // clear cached data
        clearCachedData();
        // load data from bdb
        try {
            cursor = clsDefSettingIndex.entities();
            for (BdbClusterSettingEntity bdbEntity : cursor) {
                if (bdbEntity == null) {
                    logger.warn("[BDB Impl] found Null data while loading cluster configure!");
                    continue;
                }
                putRecord2Caches(new ClusterSettingEntity(bdbEntity));
                totalCnt++;
            }
        } catch (Exception e) {
            logger.error("[BDB Impl] load cluster configure failure ", e);
            throw new LoadMetaException(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logger.info(strBuff.append("[BDB Impl] loaded ").append(totalCnt)
                .append(" cluster configure successfully...").toString());
        strBuff.delete(0, strBuff.length());
    }

    protected boolean putConfig2Persistent(ClusterSettingEntity entity,
                                           StringBuilder strBuff, ProcessResult result) {
        BdbClusterSettingEntity bdbEntity =
                entity.buildBdbClsDefSettingEntity();
        try {
            clsDefSettingIndex.put(bdbEntity);
        } catch (Throwable e) {
            logger.error("[BDB Impl] put cluster configure failure ", e);
            result.setFailResult(DataOpErrCode.DERR_STORE_ABNORMAL.getCode(),
                    strBuff.append("Put cluster configure failure: ")
                            .append(e.getMessage()).toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }

    protected boolean delConfigFromPersistent(StringBuilder strBuff, String key) {
        try {
            clsDefSettingIndex.delete(key);
        } catch (Throwable e) {
            logger.error("[BDB Impl] delete cluster configure failure ", e);
            return false;
        }
        return true;
    }
}
