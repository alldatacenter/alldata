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

package org.apache.inlong.tubemq.server.master.metamanage.metastore.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.server.common.utils.WebParameterUtils;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.TStoreConstants;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.ClusterSettingEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper.ClusterConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbsClusterConfigMapperImpl implements ClusterConfigMapper {
    protected static final Logger logger =
            LoggerFactory.getLogger(AbsClusterConfigMapperImpl.class);
    // data cache
    private final Map<String, ClusterSettingEntity> metaDataCache = new ConcurrentHashMap<>();

    public AbsClusterConfigMapperImpl() {
        // initial instance
    }

    @Override
    public boolean addUpdClusterConfig(ClusterSettingEntity entity,
                                       StringBuilder strBuff, ProcessResult result) {
        ClusterSettingEntity newEntity;
        // Check whether the configure record already exist
        ClusterSettingEntity curEntity = metaDataCache.get(entity.getRecordKey());
        if (curEntity == null) {
            newEntity = entity.clone();
        } else {
            // Build the entity that need to be updated
            newEntity = curEntity.clone();
            newEntity.updBaseModifyInfo(entity);
            if (!newEntity.updModifyInfo(entity.getDataVerId(),
                    entity.getBrokerPort(), entity.getBrokerTLSPort(),
                    entity.getBrokerWebPort(), entity.getMaxMsgSizeInMB(),
                    entity.getQryPriorityId(), entity.enableFlowCtrl(),
                    entity.getGloFlowCtrlRuleCnt(), entity.getGloFlowCtrlRuleInfo(),
                    entity.getClsDefTopicProps())) {
                result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                        "Cluster configure not changed!");
                return result.isSuccess();
            }
        }
        // Check whether the configured ports conflict in the record
        if (!WebParameterUtils.isValidPortsSet(newEntity.getBrokerPort(),
                newEntity.getBrokerTLSPort(), newEntity.getBrokerWebPort(),
                strBuff, result)) {
            return result.isSuccess();
        }
        // Store data to persistent
        if (putConfig2Persistent(newEntity, strBuff, result)) {
            metaDataCache.put(newEntity.getRecordKey(), entity);
        }
        return result.isSuccess();
    }

    @Override
    public boolean delClusterConfig(StringBuilder strBuff, ProcessResult result) {
        ClusterSettingEntity curEntity =
                metaDataCache.get(TStoreConstants.TOKEN_DEFAULT_CLUSTER_SETTING);
        if (curEntity == null) {
            result.setSuccResult(null);
            return true;
        }
        delConfigFromPersistent(strBuff, TStoreConstants.TOKEN_DEFAULT_CLUSTER_SETTING);
        metaDataCache.remove(TStoreConstants.TOKEN_DEFAULT_CLUSTER_SETTING);
        result.setSuccResult(null);
        return true;
    }

    @Override
    public ClusterSettingEntity getClusterConfig() {
        return metaDataCache.get(TStoreConstants.TOKEN_DEFAULT_CLUSTER_SETTING);
    }

    /**
     * Clear cached data
     */
    protected void clearCachedData() {
        metaDataCache.clear();
    }

    /**
     * Add or update a record
     *
     * @param entity  need added or updated entity
     */
    protected void putRecord2Caches(ClusterSettingEntity entity) {
        metaDataCache.put(entity.getRecordKey(), entity);
    }

    /**
     * Put cluster configure information into persistent storage
     *
     * @param entity   need add record
     * @param strBuff  the string buffer
     * @param result process result with old value
     * @return the process result
     */
    protected abstract boolean putConfig2Persistent(ClusterSettingEntity entity,
                                                    StringBuilder strBuff, ProcessResult result);

    /**
     * Delete cluster configure information from persistent storage
     *
     * @param key      the record key
     * @param strBuff  the string buffer
     * @return the process result
     */
    protected abstract boolean delConfigFromPersistent(StringBuilder strBuff, String key);
}
