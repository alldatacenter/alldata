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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.ConcurrentHashSet;
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;
import org.apache.inlong.tubemq.server.common.utils.WebParameterUtils;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper.BrokerConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbsBrokerConfigMapperImpl implements BrokerConfigMapper {
    protected static final Logger logger =
            LoggerFactory.getLogger(AbsBrokerConfigMapperImpl.class);
    // broker config store
    private final ConcurrentHashMap<Integer/* brokerId */, BrokerConfEntity>
            brokerConfCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String/* brokerIP */, Integer/* brokerId */>
            brokerIpIndexCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer/* regionId */, ConcurrentHashSet<Integer>>
            regionIndexCache = new ConcurrentHashMap<>();

    public AbsBrokerConfigMapperImpl() {
        // initial instance
    }

    @Override
    public boolean addBrokerConf(BrokerConfEntity entity,
                                 StringBuilder strBuff, ProcessResult result) {
        // Check whether the brokerId or broker Ip conflict with existing records
        if (brokerConfCache.get(entity.getBrokerId()) != null
                || brokerIpIndexCache.get(entity.getBrokerIp()) != null) {
            result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                    strBuff.append("Existed record found for ")
                            .append(WebFieldDef.BROKERID.name).append("(")
                            .append(entity.getBrokerId()).append(") or ")
                            .append(WebFieldDef.BROKERIP.name).append("(")
                            .append(entity.getBrokerIp()).append(") value!")
                            .toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        // Check whether the configured ports conflict in the record
        if (!WebParameterUtils.isValidPortsSet(entity.getBrokerPort(),
                entity.getBrokerTLSPort(), entity.getBrokerWebPort(), strBuff, result)) {
            return result.isSuccess();
        }
        // Store data to persistent
        if (putConfig2Persistent(entity, strBuff, result)) {
            putRecord2Caches(entity);
        }
        return result.isSuccess();
    }

    @Override
    public boolean updBrokerConf(BrokerConfEntity entity,
                                 StringBuilder strBuff, ProcessResult result) {
        // Check the existence of records by brokerId
        BrokerConfEntity curEntity =
                brokerConfCache.get(entity.getBrokerId());
        if (curEntity == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    strBuff.append("Not found broker configure for ")
                            .append(WebFieldDef.BROKERID.name).append("(")
                            .append(entity.getBrokerId()).append(")!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        // Build the entity that need to be updated
        BrokerConfEntity newEntity = curEntity.clone();
        newEntity.updBaseModifyInfo(entity);
        if (!newEntity.updModifyInfo(entity.getDataVerId(),
                entity.getBrokerPort(), entity.getBrokerTLSPort(),
                entity.getBrokerWebPort(), entity.getRegionId(),
                entity.getGroupId(), entity.getManageStatus(),
                entity.getTopicProps())) {
            result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                    "Broker configure not changed!");
            return result.isSuccess();
        }
        // Check whether the configured ports conflict in the record
        if (!WebParameterUtils.isValidPortsSet(newEntity.getBrokerPort(),
                newEntity.getBrokerTLSPort(), newEntity.getBrokerWebPort(),
                strBuff, result)) {
            return result.isSuccess();
        }
        // Check manage status
        if (!isValidMngStatusChange(newEntity, curEntity, strBuff, result)) {
            return result.isSuccess();
        }
        // Store data to persistent
        if (putConfig2Persistent(newEntity, strBuff, result)) {
            putRecord2Caches(newEntity);
            result.setSuccResult(null);
        }
        return result.isSuccess();
    }

    @Override
    public boolean updBrokerMngStatus(BaseEntity opEntity,
                                      int brokerId, ManageStatus newMngStatus,
                                      StringBuilder strBuff, ProcessResult result) {
        // Check the existence of records by brokerId
        BrokerConfEntity curEntity = brokerConfCache.get(brokerId);
        if (curEntity == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    strBuff.append("Not found broker configure for ")
                            .append(WebFieldDef.BROKERID.name).append("(")
                            .append(brokerId).append(")!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        // check whether changed
        if (curEntity.getManageStatus() == newMngStatus) {
            result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                    strBuff.append("Unchanged  ").append(WebFieldDef.MANAGESTATUS.name)
                            .append("(").append(newMngStatus.getDescription())
                            .append(")!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        // Build the entity that need to be updated
        BrokerConfEntity newEntity = curEntity.clone();
        newEntity.updBaseModifyInfo(opEntity);
        if (!newEntity.updModifyInfo(opEntity.getDataVerId(),
                TBaseConstants.META_VALUE_UNDEFINED, TBaseConstants.META_VALUE_UNDEFINED,
                TBaseConstants.META_VALUE_UNDEFINED, TBaseConstants.META_VALUE_UNDEFINED,
                TBaseConstants.META_VALUE_UNDEFINED, newMngStatus, null)) {
            result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                    "Broker configure not changed!");
            return result.isSuccess();
        }
        // Check manage status
        if (!isValidMngStatusChange(newEntity, curEntity, strBuff, result)) {
            return result.isSuccess();
        }
        // Store data to persistent
        if (putConfig2Persistent(newEntity, strBuff, result)) {
            putRecord2Caches(newEntity);
            result.setSuccResult(null);
        }
        return result.isSuccess();
    }

    @Override
    public boolean delBrokerConf(int brokerId, StringBuilder strBuff, ProcessResult result) {
        BrokerConfEntity curEntity =
                brokerConfCache.get(brokerId);
        // Check the existence of records by brokerId
        if (curEntity == null) {
            result.setSuccResult(null);
            return result.isSuccess();
        }
        // Delete record from persistent
        delConfigFromPersistent(brokerId, strBuff);
        // Clear cache data
        delRecordFromCaches(brokerId);
        result.setSuccResult(null);
        return result.isSuccess();
    }

    @Override
    public Map<Integer, BrokerConfEntity> getBrokerConfInfo(BrokerConfEntity qryEntity) {
        Map<Integer, BrokerConfEntity> retMap = new HashMap<>();
        if (qryEntity == null) {
            for (BrokerConfEntity entity : brokerConfCache.values()) {
                retMap.put(entity.getBrokerId(), entity);
            }
        } else {
            for (BrokerConfEntity entity : brokerConfCache.values()) {
                if (entity != null && entity.isMatched(qryEntity)) {
                    retMap.put(entity.getBrokerId(), entity);
                }
            }
        }
        return retMap;
    }

    @Override
    public Map<Integer, BrokerConfEntity> getBrokerConfInfo(Set<Integer> brokerIdSet,
                                                            Set<String> brokerIpSet,
                                                            BrokerConfEntity qryEntity) {
        Set<Integer> idHitSet = null;
        Set<Integer> ipHitSet = null;
        Set<Integer> totalMatchedSet = null;
        Map<Integer, BrokerConfEntity> retMap = new HashMap<>();
        // get records set by brokerIdSet
        if (brokerIdSet != null && !brokerIdSet.isEmpty()) {
            idHitSet = new HashSet<>();
            BrokerConfEntity entity;
            for (Integer brokerId : brokerIdSet) {
                entity = brokerConfCache.get(brokerId);
                if (entity != null) {
                    idHitSet.add(brokerId);
                }
            }
            if (idHitSet.isEmpty()) {
                return retMap;
            }
        }
        // get records set by brokerIpSet
        if (brokerIpSet != null && !brokerIpSet.isEmpty()) {
            ipHitSet = new HashSet<>();
            for (String brokerIp : brokerIpSet) {
                Integer brokerId = brokerIpIndexCache.get(brokerIp);
                if (brokerId != null) {
                    ipHitSet.add(brokerId);
                }
            }
            if (ipHitSet.isEmpty()) {
                return retMap;
            }
        }
        // get intersection from brokerIdSet and brokerIpSet
        if (idHitSet != null || ipHitSet != null) {
            if (idHitSet == null) {
                totalMatchedSet = new HashSet<>(ipHitSet);
            } else {
                if (ipHitSet == null) {
                    totalMatchedSet = new HashSet<>(idHitSet);
                } else {
                    totalMatchedSet = new HashSet<>();
                    for (Integer record : idHitSet) {
                        if (ipHitSet.contains(record)) {
                            totalMatchedSet.add(record);
                        }
                    }
                }
            }
        }
        // get broker configures
        if (totalMatchedSet == null) {
            for (BrokerConfEntity entity :  brokerConfCache.values()) {
                if (entity == null
                        || (qryEntity != null && !entity.isMatched(qryEntity))) {
                    continue;
                }
                retMap.put(entity.getBrokerId(), entity);
            }
        } else {
            for (Integer brokerId : totalMatchedSet) {
                BrokerConfEntity entity = brokerConfCache.get(brokerId);
                if (entity == null
                        || (qryEntity != null && !entity.isMatched(qryEntity))) {
                    continue;
                }
                retMap.put(entity.getBrokerId(), entity);
            }
        }
        return retMap;
    }

    @Override
    public BrokerConfEntity getBrokerConfByBrokerId(int brokerId) {
        return brokerConfCache.get(brokerId);
    }

    @Override
    public BrokerConfEntity getBrokerConfByBrokerIp(String brokerIp) {
        Integer brokerId = brokerIpIndexCache.get(brokerIp);
        if (brokerId == null) {
            return null;
        }
        return brokerConfCache.get(brokerId);
    }

    @Override
    public Map<Integer, Set<Integer>> getBrokerIdByRegionId(Set<Integer> regionIdSet) {
        Set<Integer> qryKey = new HashSet<>();
        Map<Integer, Set<Integer>> retInfo = new HashMap<>();
        if (regionIdSet == null || regionIdSet.isEmpty()) {
            qryKey.addAll(regionIndexCache.keySet());
        } else {
            qryKey.addAll(regionIdSet);
        }
        for (Integer regionId : qryKey) {
            ConcurrentHashSet<Integer> brokerIdSet =
                    regionIndexCache.get(regionId);
            if (brokerIdSet == null || brokerIdSet.isEmpty()) {
                continue;
            }
            retInfo.put(regionId, brokerIdSet);
        }
        return retInfo;
    }

    /**
     * Clear cached data
     */
    protected void clearCachedData() {
        brokerIpIndexCache.clear();
        regionIndexCache.clear();
        brokerConfCache.clear();
    }

    /**
     * Add or update a record to caches
     *
     * @param entity  need added or updated entity
     */
    protected void putRecord2Caches(BrokerConfEntity entity) {
        brokerConfCache.put(entity.getBrokerId(), entity);
        // add brokerId info
        Integer brokerId = brokerIpIndexCache.get(entity.getBrokerIp());
        if (brokerId == null || brokerId != entity.getBrokerId()) {
            brokerIpIndexCache.put(entity.getBrokerIp(), entity.getBrokerId());
        }
        ConcurrentHashSet<Integer> brokerIdSet = regionIndexCache.get(entity.getRegionId());
        if (brokerIdSet == null) {
            ConcurrentHashSet<Integer> tmpBrokerIdSet = new ConcurrentHashSet<>();
            brokerIdSet = regionIndexCache.putIfAbsent(entity.getRegionId(), tmpBrokerIdSet);
            if (brokerIdSet == null) {
                brokerIdSet = tmpBrokerIdSet;
            }
        }
        brokerIdSet.add(entity.getBrokerId());
    }

    /**
     * Put broker configure information into persistent storage
     *
     * @param entity   need add record
     * @param strBuff  the string buffer
     * @param result   process result with old value
     * @return the process result
     */
    protected abstract boolean putConfig2Persistent(BrokerConfEntity entity,
                                                    StringBuilder strBuff,
                                                    ProcessResult result);

    /**
     * Delete broker configure information from persistent storage
     *
     * @param brokerId  the broker id key
     * @param strBuff   the string buffer
     * @return the process result
     */
    protected abstract boolean delConfigFromPersistent(int brokerId, StringBuilder strBuff);

    /**
     * Delete the record from caches
     *
     * @param brokerId  need deleted broker id
     */
    private void delRecordFromCaches(int brokerId) {
        BrokerConfEntity curEntity =
                brokerConfCache.remove(brokerId);
        if (curEntity == null) {
            return;
        }
        brokerIpIndexCache.remove(curEntity.getBrokerIp());
        ConcurrentHashSet<Integer> brokerIdSet =
                regionIndexCache.get(curEntity.getRegionId());
        if (brokerIdSet == null) {
            return;
        }
        brokerIdSet.remove(brokerId);
    }

    /**
     * Check whether the management status change is legal
     *
     * @param newEntity  the entity to be updated
     * @param curEntity  the current entity
     * @param strBuff    string buffer
     * @param result     check result of parameter value
     * @return  true for valid, false for invalid
     */
    private boolean isValidMngStatusChange(BrokerConfEntity newEntity,
                                           BrokerConfEntity curEntity,
                                           StringBuilder strBuff,
                                           ProcessResult result) {
        if (newEntity.getManageStatus() == curEntity.getManageStatus()) {
            return true;
        }
        if (((newEntity.getManageStatus().getCode() < ManageStatus.STATUS_MANAGE_ONLINE.getCode())
                && (curEntity.getManageStatus().getCode() >= ManageStatus.STATUS_MANAGE_ONLINE.getCode()))
                || ((newEntity.getManageStatus().getCode() > ManageStatus.STATUS_MANAGE_ONLINE.getCode())
                && (curEntity.getManageStatus().getCode() < ManageStatus.STATUS_MANAGE_ONLINE.getCode()))) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                    strBuff.append("Illegal manage status, cannot reverse ")
                            .append(WebFieldDef.MANAGESTATUS.name).append(" from ")
                            .append(curEntity.getManageStatus().getDescription())
                            .append(" to ").append(newEntity.getManageStatus().getDescription())
                            .append(" for the broker(").append(WebFieldDef.BROKERID.name).append("=")
                            .append(curEntity.getBrokerId()).append(")!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        return true;
    }
}
