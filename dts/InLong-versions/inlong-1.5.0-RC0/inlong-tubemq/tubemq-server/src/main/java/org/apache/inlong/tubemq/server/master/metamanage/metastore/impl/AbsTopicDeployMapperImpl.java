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

package org.apache.inlong.tubemq.server.master.metamanage.metastore.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.ConcurrentHashSet;
import org.apache.inlong.tubemq.corebase.utils.KeyBuilderUtils;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStatus;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicDeployEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper.TopicDeployMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbsTopicDeployMapperImpl implements TopicDeployMapper {

    protected static final Logger logger =
            LoggerFactory.getLogger(AbsTopicDeployMapperImpl.class);
    // data cache
    private final ConcurrentHashMap<String/* recordKey */, TopicDeployEntity> topicDeployCache =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer/* brokerId */, ConcurrentHashSet<String>> brokerId2RecordCache =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String/* topicName */, ConcurrentHashSet<String>> topicName2RecordCache =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer/* brokerId */, ConcurrentHashSet<String>> brokerId2TopicNameCache =
            new ConcurrentHashMap<>();

    public AbsTopicDeployMapperImpl() {
        // Initial instant
    }

    @Override
    public boolean addTopicDeployConf(TopicDeployEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        // Checks whether the record already exists
        TopicDeployEntity curEntity =
                topicDeployCache.get(entity.getRecordKey());
        if (curEntity != null) {
            if (curEntity.isValidTopicStatus()) {
                result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                        strBuff.append("Existed record found for brokerId-topicName(")
                                .append(curEntity.getRecordKey()).append(")!").toString());
            } else {
                result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                        strBuff.append("Softly deleted record found for brokerId-topicName(")
                                .append(curEntity.getRecordKey())
                                .append("), please resume or remove it first!").toString());
            }
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        // valid whether system topic
        if (!isValidSysTopicConf(entity, strBuff, result)) {
            return result.isSuccess();
        }
        // check deploy status if still accept publish and subscribe
        if (!entity.isValidTopicStatus()
                && (entity.isAcceptPublish() || entity.isAcceptSubscribe())) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                    strBuff.append("The values of acceptPublish and acceptSubscribe must be false")
                            .append(" when add brokerId-topicName(")
                            .append(entity.getRecordKey()).append(") record!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        // Store data to persistent
        if (putConfig2Persistent(entity, strBuff, result)) {
            putRecord2Caches(entity);
        }
        return result.isSuccess();
    }

    @Override
    public boolean updTopicDeployConf(TopicDeployEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        // Checks whether the record already exists
        TopicDeployEntity curEntity =
                topicDeployCache.get(entity.getRecordKey());
        if (curEntity == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    strBuff.append("Not found topic deploy configure for brokerId-topicName(")
                            .append(entity.getRecordKey()).append(")!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        // Build the entity that need to be updated
        TopicDeployEntity newEntity = curEntity.clone();
        newEntity.updBaseModifyInfo(entity);
        if (!newEntity.updModifyInfo(entity.getDataVerId(),
                entity.getTopicId(), entity.getBrokerPort(),
                entity.getBrokerIp(), entity.getDeployStatus(),
                entity.getTopicProps())) {
            result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                    "Topic deploy configure not changed!");
            return result.isSuccess();
        }
        // valid whether system topic
        if (!isValidSysTopicConf(newEntity, strBuff, result)) {
            return result.isSuccess();
        }
        // check deploy status
        if (!isValidValuesChange(newEntity, curEntity, strBuff, result)) {
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
    public boolean updTopicDeployStatus(BaseEntity opEntity, int brokerId,
            String topicName, TopicStatus topicStatus,
            StringBuilder strBuff, ProcessResult result) {
        // Checks whether the record already exists
        TopicDeployEntity curEntity = getTopicConf(brokerId, topicName);
        if (curEntity == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    strBuff.append("Not found topic deploy configure for brokerId-topicName(")
                            .append(brokerId).append("-").append(topicName)
                            .append(")!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        // Build the entity that need to be updated
        TopicDeployEntity newEntity = curEntity.clone();
        newEntity.updBaseModifyInfo(opEntity);
        if (!newEntity.updModifyInfo(opEntity.getDataVerId(),
                TBaseConstants.META_VALUE_UNDEFINED, TBaseConstants.META_VALUE_UNDEFINED,
                null, topicStatus, null)) {
            result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                    "Topic deploy configure not changed!");
            return result.isSuccess();
        }
        // check deploy status
        if (!isValidValuesChange(newEntity, curEntity, strBuff, result)) {
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
    public boolean delTopicDeployConf(String recordKey, StringBuilder strBuff, ProcessResult result) {
        TopicDeployEntity curEntity =
                topicDeployCache.get(recordKey);
        if (curEntity == null) {
            result.setSuccResult(null);
            return result.isSuccess();
        }
        // check deploy status if still accept publish and subscribe
        if (curEntity.isAcceptPublish() || curEntity.isAcceptSubscribe()) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                    strBuff.append("The values of acceptPublish and acceptSubscribe must be false")
                            .append(" before delete brokerId-topicName(")
                            .append(curEntity.getRecordKey()).append(") record!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        delConfigFromPersistent(recordKey, strBuff);
        delRecordFromCaches(recordKey);
        result.setSuccResult(null);
        return result.isSuccess();
    }

    @Override
    public boolean delTopicConfByBrokerId(Integer brokerId, StringBuilder strBuff, ProcessResult result) {
        ConcurrentHashSet<String> recordKeySet =
                brokerId2RecordCache.get(brokerId);
        if (recordKeySet == null || recordKeySet.isEmpty()) {
            result.setSuccResult(null);
            return result.isSuccess();
        }
        // check deploy status if still accept publish and subscribe
        TopicDeployEntity curEntity;
        for (String recordKey : recordKeySet) {
            curEntity = topicDeployCache.get(recordKey);
            if (curEntity == null) {
                continue;
            }
            if (curEntity.isAcceptPublish() || curEntity.isAcceptSubscribe()) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        strBuff.append("The values of acceptPublish and acceptSubscribe must be false")
                                .append(" before delete brokerId-topicName(")
                                .append(curEntity.getRecordKey()).append(") record!").toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
        }
        // delete records
        for (String recordKey : recordKeySet) {
            delConfigFromPersistent(recordKey, strBuff);
            delRecordFromCaches(recordKey);
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }

    @Override
    public boolean hasConfiguredTopics(int brokerId) {
        ConcurrentHashSet<String> keySet =
                brokerId2RecordCache.get(brokerId);
        return (keySet != null && !keySet.isEmpty());
    }

    @Override
    public boolean isTopicDeployed(String topicName) {
        ConcurrentHashSet<String> deploySet = topicName2RecordCache.get(topicName);
        return (deploySet != null && !deploySet.isEmpty());
    }

    @Override
    public List<TopicDeployEntity> getTopicConf(TopicDeployEntity qryEntity) {
        List<TopicDeployEntity> retEntities = new ArrayList<>();
        if (qryEntity == null) {
            retEntities.addAll(topicDeployCache.values());
        } else {
            for (TopicDeployEntity entity : topicDeployCache.values()) {
                if (entity != null && entity.isMatched(qryEntity, true)) {
                    retEntities.add(entity);
                }
            }
        }
        return retEntities;
    }

    @Override
    public TopicDeployEntity getTopicConf(int brokerId, String topicName) {
        String recordKey =
                KeyBuilderUtils.buildTopicConfRecKey(brokerId, topicName);
        return topicDeployCache.get(recordKey);
    }

    @Override
    public TopicDeployEntity getTopicConfByeRecKey(String recordKey) {
        return topicDeployCache.get(recordKey);
    }

    @Override
    public Map<String, List<TopicDeployEntity>> getTopicConfMap(Set<String> topicNameSet,
            Set<Integer> brokerIdSet,
            TopicDeployEntity qryEntity) {
        List<TopicDeployEntity> items;
        Map<String, List<TopicDeployEntity>> retEntityMap = new HashMap<>();
        // get matched keys by topicNameSet and brokerIdSet
        Set<String> matchedKeySet = getMatchedRecords(topicNameSet, brokerIdSet);
        // filter record by qryEntity
        if (matchedKeySet == null) {
            for (TopicDeployEntity entry : topicDeployCache.values()) {
                if (entry == null || (qryEntity != null && !entry.isMatched(qryEntity, true))) {
                    continue;
                }
                items = retEntityMap.computeIfAbsent(
                        entry.getTopicName(), k -> new ArrayList<>());
                items.add(entry);
            }
        } else {
            TopicDeployEntity entry;
            for (String recKey : matchedKeySet) {
                entry = topicDeployCache.get(recKey);
                if (entry == null || (qryEntity != null && !entry.isMatched(qryEntity, true))) {
                    continue;
                }
                items = retEntityMap.computeIfAbsent(
                        entry.getTopicName(), k -> new ArrayList<>());
                items.add(entry);
            }
        }
        return retEntityMap;
    }

    @Override
    public Map<Integer, List<TopicDeployEntity>> getTopicDeployInfoMap(Set<String> topicNameSet,
            Set<Integer> brokerIdSet) {
        List<TopicDeployEntity> items;
        Map<Integer, List<TopicDeployEntity>> retEntityMap = new HashMap<>();
        if (brokerIdSet != null) {
            for (Integer brokerId : brokerIdSet) {
                retEntityMap.put(brokerId, new ArrayList<>());
            }
        }
        // get matched keys by topicNameSet and brokerIdSet
        Set<String> matchedKeySet = getMatchedRecords(topicNameSet, brokerIdSet);
        // get record by keys
        if (matchedKeySet == null) {
            matchedKeySet = new HashSet<>(topicDeployCache.keySet());
        }
        for (String recordKey : matchedKeySet) {
            TopicDeployEntity entity = topicDeployCache.get(recordKey);
            if (entity == null) {
                continue;
            }
            items = retEntityMap.computeIfAbsent(
                    entity.getBrokerId(), k -> new ArrayList<>());
            items.add(entity);
        }
        return retEntityMap;
    }

    @Override
    public Map<String, List<TopicDeployEntity>> getTopicConfMapByTopicAndBrokerIds(Set<String> topicSet,
            Set<Integer> brokerIdSet) {
        TopicDeployEntity tmpEntity;
        List<TopicDeployEntity> itemLst;
        Map<String, List<TopicDeployEntity>> retEntityMap = new HashMap<>();
        // get matched keys by topicNameSet and brokerIdSet
        Set<String> matchedKeySet = getMatchedRecords(topicSet, brokerIdSet);
        // get records by matched keys
        if (matchedKeySet == null) {
            for (TopicDeployEntity entity : topicDeployCache.values()) {
                if (entity == null) {
                    continue;
                }
                itemLst = retEntityMap.computeIfAbsent(
                        entity.getTopicName(), k -> new ArrayList<>());
                itemLst.add(entity);
            }
        } else {
            for (String key : matchedKeySet) {
                tmpEntity = topicDeployCache.get(key);
                if (tmpEntity == null) {
                    continue;
                }
                itemLst = retEntityMap.computeIfAbsent(
                        tmpEntity.getTopicName(), k -> new ArrayList<>());
                itemLst.add(tmpEntity);
            }
        }
        return retEntityMap;
    }

    @Override
    public Map<String, TopicDeployEntity> getConfiguredTopicInfo(int brokerId) {
        TopicDeployEntity tmpEntity;
        Map<String, TopicDeployEntity> retEntityMap = new HashMap<>();
        ConcurrentHashSet<String> records = brokerId2RecordCache.get(brokerId);
        if (records == null || records.isEmpty()) {
            return retEntityMap;
        }
        for (String key : records) {
            tmpEntity = topicDeployCache.get(key);
            if (tmpEntity == null) {
                continue;
            }
            retEntityMap.put(tmpEntity.getTopicName(), tmpEntity);
        }
        return retEntityMap;
    }

    @Override
    public Map<Integer, Set<String>> getConfiguredTopicInfo(Set<Integer> brokerIdSet) {
        Set<String> topicSet;
        ConcurrentHashSet<String> deploySet;
        Map<Integer, Set<String>> retEntityMap = new HashMap<>();
        if (brokerIdSet == null || brokerIdSet.isEmpty()) {
            for (Map.Entry<Integer, ConcurrentHashSet<String>> entry : brokerId2TopicNameCache.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                topicSet = new HashSet<>();
                if (entry.getValue() != null) {
                    topicSet.addAll(entry.getValue());
                }
                retEntityMap.put(entry.getKey(), topicSet);
            }
        } else {
            for (Integer brokerId : brokerIdSet) {
                if (brokerId == null) {
                    continue;
                }
                topicSet = new HashSet<>();
                deploySet = brokerId2TopicNameCache.get(brokerId);
                if (deploySet != null) {
                    topicSet.addAll(deploySet);
                }
                retEntityMap.put(brokerId, topicSet);
            }
        }
        return retEntityMap;
    }

    @Override
    public Map<String, Map<Integer, String>> getTopicBrokerInfo(Set<String> topicNameSet) {
        ConcurrentHashSet<String> keySet;
        Map<Integer, String> brokerInfoMap;
        Map<String, Map<Integer, String>> retEntityMap = new HashMap<>();
        if (topicNameSet == null || topicNameSet.isEmpty()) {
            for (TopicDeployEntity entry : topicDeployCache.values()) {
                if (entry == null) {
                    continue;
                }
                brokerInfoMap = retEntityMap.computeIfAbsent(
                        entry.getTopicName(), k -> new HashMap<>());
                brokerInfoMap.put(entry.getBrokerId(), entry.getBrokerIp());
            }
        } else {
            for (String topicName : topicNameSet) {
                if (topicName == null) {
                    continue;
                }
                brokerInfoMap = retEntityMap.computeIfAbsent(topicName, k -> new HashMap<>());
                keySet = topicName2RecordCache.get(topicName);
                if (keySet != null) {
                    for (String key : keySet) {
                        TopicDeployEntity entry = topicDeployCache.get(key);
                        if (entry != null) {
                            brokerInfoMap.put(entry.getBrokerId(), entry.getBrokerIp());
                        }
                    }
                }
            }
        }
        return retEntityMap;
    }

    @Override
    public Set<Integer> getDeployedBrokerIdByTopic(Set<String> topicNameSet) {
        ConcurrentHashSet<String> keySet;
        Set<Integer> retSet = new HashSet<>();
        if (topicNameSet == null || topicNameSet.isEmpty()) {
            return retSet;
        }
        for (String topicName : topicNameSet) {
            if (topicName == null) {
                continue;
            }
            keySet = topicName2RecordCache.get(topicName);
            if (keySet != null) {
                for (String key : keySet) {
                    TopicDeployEntity entry = topicDeployCache.get(key);
                    if (entry != null) {
                        retSet.add(entry.getBrokerId());
                    }
                }
            }
        }
        return retSet;
    }

    @Override
    public Set<String> getDeployedTopicSet() {
        return new HashSet<>(topicName2RecordCache.keySet());
    }

    /**
     * Clear cached data
     */
    protected void clearCachedData() {
        topicName2RecordCache.clear();
        brokerId2RecordCache.clear();
        brokerId2TopicNameCache.clear();
        topicDeployCache.clear();
    }

    /**
     * Add or update a record
     *
     * @param entity  need added or updated entity
     */
    protected void putRecord2Caches(TopicDeployEntity entity) {
        topicDeployCache.put(entity.getRecordKey(), entity);
        // add topic index map
        ConcurrentHashSet<String> keySet =
                topicName2RecordCache.get(entity.getTopicName());
        if (keySet == null) {
            ConcurrentHashSet<String> tmpSet = new ConcurrentHashSet<>();
            keySet = topicName2RecordCache.putIfAbsent(entity.getTopicName(), tmpSet);
            if (keySet == null) {
                keySet = tmpSet;
            }
        }
        keySet.add(entity.getRecordKey());
        // add brokerId index map
        keySet = brokerId2RecordCache.get(entity.getBrokerId());
        if (keySet == null) {
            ConcurrentHashSet<String> tmpSet = new ConcurrentHashSet<>();
            keySet = brokerId2RecordCache.putIfAbsent(entity.getBrokerId(), tmpSet);
            if (keySet == null) {
                keySet = tmpSet;
            }
        }
        keySet.add(entity.getRecordKey());
        // add brokerId topic map
        keySet = brokerId2TopicNameCache.get(entity.getBrokerId());
        if (keySet == null) {
            ConcurrentHashSet<String> tmpSet = new ConcurrentHashSet<>();
            keySet = brokerId2TopicNameCache.putIfAbsent(entity.getBrokerId(), tmpSet);
            if (keySet == null) {
                keySet = tmpSet;
            }
        }
        keySet.add(entity.getTopicName());
    }

    /**
     * Put topic deploy configure information into persistent store
     *
     * @param entity   need add record
     * @param strBuff  the string buffer
     * @param result process result with old value
     * @return the process result
     */
    protected abstract boolean putConfig2Persistent(TopicDeployEntity entity,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Delete topic deploy configure information from persistent storage
     *
     * @param recordKey  the record key
     * @param strBuff    the string buffer
     * @return the process result
     */
    protected abstract boolean delConfigFromPersistent(String recordKey, StringBuilder strBuff);

    private void delRecordFromCaches(String recordKey) {
        TopicDeployEntity curEntity =
                topicDeployCache.remove(recordKey);
        if (curEntity == null) {
            return;
        }
        // add topic index
        ConcurrentHashSet<String> keySet =
                topicName2RecordCache.get(curEntity.getTopicName());
        if (keySet != null) {
            keySet.remove(recordKey);
            if (keySet.isEmpty()) {
                topicName2RecordCache.remove(curEntity.getTopicName(), new ConcurrentHashSet<>());
            }
        }
        // delete brokerId index
        keySet = brokerId2RecordCache.get(curEntity.getBrokerId());
        if (keySet != null) {
            keySet.remove(recordKey);
            if (keySet.isEmpty()) {
                brokerId2RecordCache.remove(curEntity.getBrokerId(), new ConcurrentHashSet<>());
            }
        }
        // delete broker topic map
        keySet = brokerId2TopicNameCache.get(curEntity.getBrokerId());
        if (keySet != null) {
            keySet.remove(curEntity.getTopicName());
            if (keySet.isEmpty()) {
                brokerId2TopicNameCache.remove(curEntity.getBrokerId(), new ConcurrentHashSet<>());
            }
        }
    }

    private Set<String> getMatchedRecords(Set<String> topicNameSet,
            Set<Integer> brokerIdSet) {
        ConcurrentHashSet<String> keySet;
        Set<String> topicKeySet = null;
        Set<String> brokerKeySet = null;
        Set<String> matchedKeySet = null;
        // get deploy records set by topicName
        if (topicNameSet != null && !topicNameSet.isEmpty()) {
            topicKeySet = new HashSet<>();
            for (String topicName : topicNameSet) {
                keySet = topicName2RecordCache.get(topicName);
                if (keySet != null && !keySet.isEmpty()) {
                    topicKeySet.addAll(keySet);
                }
            }
            if (topicKeySet.isEmpty()) {
                return Collections.emptySet();
            }
        }
        // get deploy records set by brokerId
        if (brokerIdSet != null && !brokerIdSet.isEmpty()) {
            brokerKeySet = new HashSet<>();
            for (Integer brokerId : brokerIdSet) {
                keySet = brokerId2RecordCache.get(brokerId);
                if (keySet != null && !keySet.isEmpty()) {
                    brokerKeySet.addAll(keySet);
                }
            }
            if (brokerKeySet.isEmpty()) {
                return Collections.emptySet();
            }
        }
        // get intersection from topicKeySet and brokerKeySet
        if (topicKeySet != null || brokerKeySet != null) {
            if (topicKeySet == null) {
                matchedKeySet = new HashSet<>(brokerKeySet);
            } else {
                if (brokerKeySet == null) {
                    matchedKeySet = new HashSet<>(topicKeySet);
                } else {
                    matchedKeySet = new HashSet<>();
                    for (String record : topicKeySet) {
                        if (brokerKeySet.contains(record)) {
                            matchedKeySet.add(record);
                        }
                    }
                }
            }
        }
        return matchedKeySet;
    }

    /**
     * Check whether the change of deploy values is valid
     * Attention, the newEntity and newEntity must not equal
     *
     * @param newEntity  the entity to be updated
     * @param curEntity  the current entity
     * @param strBuff    string buffer
     * @param result     check result of parameter value
     * @return  true for valid, false for invalid
     */
    private boolean isValidValuesChange(TopicDeployEntity newEntity,
            TopicDeployEntity curEntity,
            StringBuilder strBuff,
            ProcessResult result) {
        // check if shrink data store block
        if (newEntity.getNumPartitions() != TBaseConstants.META_VALUE_UNDEFINED
                && newEntity.getNumPartitions() < curEntity.getNumPartitions()) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                    strBuff.append("Partition number less than before,")
                            .append(" new value is ").append(newEntity.getNumPartitions())
                            .append(", current value is ").append(curEntity.getNumPartitions())
                            .append("in brokerId-topicName(").append(curEntity.getRecordKey())
                            .append(") record!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        if (newEntity.getNumTopicStores() != TBaseConstants.META_VALUE_UNDEFINED
                && newEntity.getNumTopicStores() < curEntity.getNumTopicStores()) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                    strBuff.append("TopicStores number less than before,")
                            .append(" new value is ").append(newEntity.getNumTopicStores())
                            .append(", current value is ").append(curEntity.getNumTopicStores())
                            .append("in brokerId-topicName(").append(curEntity.getRecordKey())
                            .append(") record!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        // check whether the deploy status is equal
        if (newEntity.getTopicStatus() == curEntity.getTopicStatus()) {
            if (!newEntity.isValidTopicStatus()) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        strBuff.append("Softly deleted record cannot be changed,")
                                .append(" please resume or hard remove for brokerId-topicName(")
                                .append(newEntity.getRecordKey()).append(") record!").toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
            return true;
        }
        // check deploy status case from valid to invalid
        if (curEntity.isValidTopicStatus() && !newEntity.isValidTopicStatus()) {
            if (curEntity.isAcceptPublish()
                    || curEntity.isAcceptSubscribe()) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        strBuff.append("The values of acceptPublish and acceptSubscribe must be false")
                                .append(" before change status of brokerId-topicName(")
                                .append(curEntity.getRecordKey()).append(") record!").toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
            if (newEntity.getTopicStatus().getCode() > TopicStatus.STATUS_TOPIC_SOFT_DELETE.getCode()) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        strBuff.append("Please softly deleted the brokerId-topicName(")
                                .append(newEntity.getRecordKey()).append(") record first!").toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
            return true;
        }
        // check deploy status case from invalid to invalid
        if (!curEntity.isValidTopicStatus() && !newEntity.isValidTopicStatus()) {
            if (!((curEntity.getTopicStatus() == TopicStatus.STATUS_TOPIC_SOFT_DELETE
                    && newEntity.getTopicStatus() == TopicStatus.STATUS_TOPIC_SOFT_REMOVE)
                    || (curEntity.getTopicStatus() == TopicStatus.STATUS_TOPIC_SOFT_REMOVE
                            && newEntity.getTopicStatus() == TopicStatus.STATUS_TOPIC_HARD_REMOVE))) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        strBuff.append("Illegal transfer status from ")
                                .append(curEntity.getTopicStatus().getDescription())
                                .append(" to ").append(newEntity.getTopicStatus().getDescription())
                                .append(" for the brokerId-topicName(")
                                .append(newEntity.getRecordKey()).append(") record!").toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
            if (newEntity.isAcceptPublish()
                    || newEntity.isAcceptSubscribe()) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        strBuff.append("The values of acceptPublish and acceptSubscribe must be false")
                                .append(" before change status of brokerId-topicName(")
                                .append(newEntity.getRecordKey()).append(") record!").toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
            return true;
        }
        // check deploy status case from invalid to valid
        if (!curEntity.isValidTopicStatus() && newEntity.isValidTopicStatus()) {
            if (curEntity.getTopicStatus() != TopicStatus.STATUS_TOPIC_SOFT_DELETE) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        strBuff.append("Illegal transfer status from ")
                                .append(curEntity.getTopicStatus().getDescription())
                                .append(" to ").append(newEntity.getTopicStatus().getDescription())
                                .append(" for the brokerId-topicName(")
                                .append(newEntity.getRecordKey()).append(") record!").toString());
                strBuff.delete(0, strBuff.length());
                return !result.isSuccess();
            }
            if (newEntity.isAcceptPublish()
                    || newEntity.isAcceptSubscribe()) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        strBuff.append("The values of acceptPublish and acceptSubscribe must be false")
                                .append(" before change status of brokerId-topicName(")
                                .append(newEntity.getRecordKey()).append(") record!").toString());
                strBuff.delete(0, strBuff.length());
                return !result.isSuccess();
            }
            return false;
        }
        return false;
    }

    /**
     * Verify the validity of the configuration value for the system topic
     *
     * @param deployEntity   the topic configuration that needs to be added or updated
     * @param strBuff  the print info string buffer
     * @param result   the process result return
     * @return true if valid otherwise false
     */
    private boolean isValidSysTopicConf(TopicDeployEntity deployEntity,
            StringBuilder strBuff, ProcessResult result) {
        if (!TServerConstants.OFFSET_HISTORY_NAME.equals(deployEntity.getTopicName())) {
            return true;
        }
        if (deployEntity.getNumTopicStores() != TServerConstants.OFFSET_HISTORY_NUMSTORES) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                    strBuff.append("For system topic")
                            .append(TServerConstants.OFFSET_HISTORY_NAME)
                            .append(", the TopicStores value(")
                            .append(TServerConstants.OFFSET_HISTORY_NUMSTORES)
                            .append(") cannot be changed!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        if (deployEntity.getNumPartitions() != TServerConstants.OFFSET_HISTORY_NUMPARTS) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                    strBuff.append("For system topic")
                            .append(TServerConstants.OFFSET_HISTORY_NAME)
                            .append(", the Partition value(")
                            .append(TServerConstants.OFFSET_HISTORY_NUMPARTS)
                            .append(") cannot be changed!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        return true;
    }
}
