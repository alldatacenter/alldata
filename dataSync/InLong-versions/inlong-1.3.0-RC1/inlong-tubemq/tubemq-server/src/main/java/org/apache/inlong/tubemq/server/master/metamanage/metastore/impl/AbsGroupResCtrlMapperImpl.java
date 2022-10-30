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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper.GroupResCtrlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbsGroupResCtrlMapperImpl implements GroupResCtrlMapper {
    protected static final Logger logger =
            LoggerFactory.getLogger(AbsGroupResCtrlMapperImpl.class);
    private final ConcurrentHashMap<String/* groupName */, GroupResCtrlEntity>
            groupBaseCtrlCache = new ConcurrentHashMap<>();

    public AbsGroupResCtrlMapperImpl() {
        // Initial instant
    }

    @Override
    public boolean addGroupResCtrlConf(GroupResCtrlEntity entity,
                                       StringBuilder strBuff, ProcessResult result) {
        // Checks whether the record already exists
        GroupResCtrlEntity curEntity = groupBaseCtrlCache.get(entity.getGroupName());
        if (curEntity != null) {
            result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                    strBuff.append("Existed record found for groupName(")
                            .append(entity.getGroupName()).append(")!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        // Store data to persistent
        if (putConfig2Persistent(entity, strBuff, result)) {
            groupBaseCtrlCache.put(entity.getGroupName(), entity);
        }
        return result.isSuccess();
    }

    @Override
    public boolean updGroupResCtrlConf(GroupResCtrlEntity entity,
                                       StringBuilder strBuff, ProcessResult result) {
        // Checks whether the record already exists
        GroupResCtrlEntity curEntity = groupBaseCtrlCache.get(entity.getGroupName());
        if (curEntity == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    strBuff.append("Not found group control configure for groupName(")
                            .append(entity.getGroupName()).append(")!").toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        // Build the entity that need to be updated
        GroupResCtrlEntity newEntity = curEntity.clone();
        newEntity.updBaseModifyInfo(entity);
        if (!newEntity.updModifyInfo(entity.getDataVerId(),
                entity.isEnableResCheck(), entity.getAllowedBrokerClientRate(),
                entity.getQryPriorityId(), entity.isFlowCtrlEnable(),
                entity.getRuleCnt(), entity.getFlowCtrlInfo())) {
            result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                    "Group control configure not changed!");
            return result.isSuccess();
        }
        // Store data to persistent
        if (putConfig2Persistent(newEntity, strBuff, result)) {
            groupBaseCtrlCache.put(newEntity.getGroupName(), newEntity);
            result.setSuccResult(null);
        }
        return result.isSuccess();
    }

    @Override
    public boolean delGroupResCtrlConf(String groupName, StringBuilder strBuff, ProcessResult result) {
        GroupResCtrlEntity curEntity =
                groupBaseCtrlCache.get(groupName);
        if (curEntity == null) {
            result.setSuccResult(null);
            return true;
        }
        delConfigFromPersistent(groupName, strBuff);
        groupBaseCtrlCache.remove(groupName);
        result.setSuccResult(null);
        return true;
    }

    @Override
    public GroupResCtrlEntity getGroupResCtrlConf(String groupName) {
        return groupBaseCtrlCache.get(groupName);
    }

    @Override
    public Map<String, GroupResCtrlEntity> getGroupResCtrlConf(Set<String> groupNameSet,
                                                               GroupResCtrlEntity qryEntry) {
        Map<String, GroupResCtrlEntity> retMap = new HashMap<>();
        if (groupNameSet == null || groupNameSet.isEmpty()) {
            for (GroupResCtrlEntity entry : groupBaseCtrlCache.values()) {
                if (entry == null || (qryEntry != null && !entry.isMatched(qryEntry))) {
                    continue;
                }
                retMap.put(entry.getGroupName(), entry);
            }
        } else {
            GroupResCtrlEntity entry;
            for (String groupName : groupNameSet) {
                entry = groupBaseCtrlCache.get(groupName);
                if (entry == null || (qryEntry != null && !entry.isMatched(qryEntry))) {
                    continue;
                }
                retMap.put(entry.getGroupName(), entry);
            }
        }
        return retMap;
    }

    /**
     * Clear cached data
     */
    protected void clearCachedData() {
        groupBaseCtrlCache.clear();
    }

    /**
     * Add or update a record
     *
     * @param entity  the entity to be added or updated
     */
    protected void putRecord2Caches(GroupResCtrlEntity entity) {
        groupBaseCtrlCache.put(entity.getGroupName(), entity);
    }

    /**
     * Put group control configure information into persistent store
     *
     * @param entity   need add record
     * @param strBuff  the string buffer
     * @param result process result with old value
     * @return the process result
     */
    protected abstract boolean putConfig2Persistent(GroupResCtrlEntity entity,
                                                    StringBuilder strBuff, ProcessResult result);

    /**
     * Delete group control configure information from persistent storage
     *
     * @param recordKey  the record key
     * @param strBuff    the string buffer
     * @return the process result
     */
    protected abstract boolean delConfigFromPersistent(String recordKey, StringBuilder strBuff);
}
