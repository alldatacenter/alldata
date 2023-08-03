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

package org.apache.inlong.dataproxy.config.holder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Save cluster configure info
 */
public class GroupIdNumConfigHolder extends PropertiesHolder {

    private static final String groupIdNumConfigFileName = "groupid_mapping.properties";
    private static final String GROUPID_VALUE_SPLITTER = "#";
    private static final Logger LOG = LoggerFactory.getLogger(GroupIdNumConfigHolder.class);

    private final ConcurrentHashMap<String, String> groupIdNumMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> streamIdNumMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> groupIdNumEnableMap = new ConcurrentHashMap<>();

    public GroupIdNumConfigHolder() {
        super(groupIdNumConfigFileName);
    }

    public boolean isEnableNum2NameTrans(String groupIdNum) {
        return groupIdNumEnableMap.getOrDefault(groupIdNum, Boolean.FALSE);
    }

    public String getGroupIdNameByNum(String groupIdNum) {
        return groupIdNumMap.get(groupIdNum);
    }

    public String getStreamIdNameByIdNum(String groupIdNum, String streamIdNum) {
        ConcurrentHashMap<String, String> tmpMap = streamIdNumMap.get(groupIdNum);
        if (tmpMap == null) {
            return null;
        }
        return tmpMap.get(streamIdNum);
    }

    public boolean isGroupIdNumConfigEmpty() {
        return groupIdNumMap.isEmpty();
    }

    public boolean isStreamIdNumConfigEmpty() {
        return streamIdNumMap.isEmpty();
    }

    public ConcurrentHashMap<String, String> getGroupIdNumMap() {
        return groupIdNumMap;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<String, String>> getStreamIdNumMap() {
        return streamIdNumMap;
    }

    public ConcurrentHashMap<String, Boolean> getGroupIdNumEnableMap() {
        return groupIdNumEnableMap;
    }

    @Override
    protected Map<String, String> filterInValidRecords(Map<String, String> configMap) {
        Map<String, String> result = new HashMap<>(configMap.size());
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            if (entry == null
                    || StringUtils.isBlank(entry.getKey())
                    || !entry.getKey().contains(GROUPID_VALUE_SPLITTER)) {
                continue;
            }
            String[] keyArray = StringUtils.split(entry.getKey(), GROUPID_VALUE_SPLITTER);
            if (keyArray.length != 3) {
                continue;
            }
            if (StringUtils.isBlank(keyArray[0])
                    || StringUtils.isBlank(keyArray[1])
                    || StringUtils.isBlank(keyArray[2])) {
                continue;
            }
            if (StringUtils.isNotBlank(entry.getValue())) {
                try {
                    MAP_SPLITTER.split(entry.getValue());
                } catch (Throwable e) {
                    continue;
                }
            }
            result.put(entry.getKey().trim(), entry.getValue().trim());
        }
        return result;
    }

    @Override
    protected boolean updateCacheData() {
        boolean tmpEnable;
        Map<String, String> tmpValueMap = new HashMap<>();
        Map<String, String> valueMap = new HashMap<>();
        Map<String, String> tmpGroupIdNumMap = new HashMap<>();
        Map<String, Map<String, String>> tmpStreamIdNumMap = new HashMap<>();
        Map<String, Boolean> tmpGroupIdNumEnableMap = new HashMap<>();
        // parse configure data
        for (Map.Entry<String, String> entry : confHolder.entrySet()) {
            if (entry == null
                    || StringUtils.isBlank(entry.getKey())
                    || !entry.getKey().contains(GROUPID_VALUE_SPLITTER)) {
                continue;
            }
            String[] keyArray = StringUtils.split(entry.getKey(), GROUPID_VALUE_SPLITTER);
            if (keyArray.length != 3) {
                continue;
            }
            if (StringUtils.isBlank(keyArray[0])
                    || StringUtils.isBlank(keyArray[1])
                    || StringUtils.isBlank(keyArray[2])) {
                continue;
            }
            tmpEnable = Boolean.parseBoolean(keyArray[2].trim());
            tmpGroupIdNumMap.put(keyArray[0].trim(), keyArray[1].trim());
            tmpGroupIdNumEnableMap.put(keyArray[0].trim(), tmpEnable);
            // parse value
            if (StringUtils.isNotBlank(entry.getValue())) {
                tmpValueMap.clear();
                valueMap.clear();
                try {
                    tmpValueMap = MAP_SPLITTER.split(entry.getValue());
                } catch (Throwable e) {
                    continue;
                }
                if (tmpValueMap.isEmpty()) {
                    continue;
                }
                for (Map.Entry<String, String> entry1 : tmpValueMap.entrySet()) {
                    if (entry1 == null
                            || StringUtils.isBlank(entry1.getKey())
                            || StringUtils.isBlank(entry1.getValue())) {
                        continue;
                    }
                    valueMap.put(entry1.getKey().trim(), entry.getValue().trim());
                }
                if (!valueMap.isEmpty()) {
                    tmpStreamIdNumMap.put(keyArray[0].trim(), valueMap);
                }
            }
        }
        // update cached groupId num2Name data
        updateCachedGroupIdNumMap(tmpGroupIdNumMap);
        // update cached groupId num2Name enable data
        updateCachedGroupIdNumEnableMap(tmpGroupIdNumEnableMap);
        // update cached streamId num2Name enable data
        updateCachedStreamIdNumMap(tmpStreamIdNumMap);
        return true;
    }

    private void updateCachedGroupIdNumMap(Map<String, String> newGroupIdNumMap) {
        if (newGroupIdNumMap.isEmpty()) {
            groupIdNumMap.clear();
            return;
        }
        for (Map.Entry<String, String> entry : newGroupIdNumMap.entrySet()) {
            if (!entry.getValue().equals(groupIdNumMap.get(entry.getKey()))) {
                groupIdNumMap.put(entry.getKey(), entry.getValue());
            }
        }
        // remove records
        Set<String> rmvKeys = new HashSet<>();
        for (Map.Entry<String, String> entry : groupIdNumMap.entrySet()) {
            if (!newGroupIdNumMap.containsKey(entry.getKey())) {
                rmvKeys.add(entry.getKey());
            }
        }
        for (String tmpKey : rmvKeys) {
            groupIdNumMap.remove(tmpKey);
        }
    }

    private void updateCachedGroupIdNumEnableMap(Map<String, Boolean> newGroupNumEnableMap) {
        if (newGroupNumEnableMap.isEmpty()) {
            groupIdNumEnableMap.clear();
            return;
        }
        for (Map.Entry<String, Boolean> entry : newGroupNumEnableMap.entrySet()) {
            if (!entry.getValue().equals(groupIdNumEnableMap.get(entry.getKey()))) {
                groupIdNumEnableMap.put(entry.getKey(), entry.getValue());
            }
        }
        // remove records
        Set<String> rmvKeys = new HashSet<>();
        for (Map.Entry<String, Boolean> entry : groupIdNumEnableMap.entrySet()) {
            if (!newGroupNumEnableMap.containsKey(entry.getKey())) {
                rmvKeys.add(entry.getKey());
            }
        }
        for (String tmpKey : rmvKeys) {
            groupIdNumEnableMap.remove(tmpKey);
        }
    }

    private void updateCachedStreamIdNumMap(Map<String, Map<String, String>> newStreamIdNumMap) {
        if (newStreamIdNumMap.isEmpty()) {
            streamIdNumMap.clear();
            return;
        }
        Map<String, String> newDataMap;
        Set<String> rmvKeys = new HashSet<>();
        ConcurrentHashMap<String, String> storedMap;
        // insert cached streamId num2Name data
        for (Map.Entry<String, Map<String, String>> entry : newStreamIdNumMap.entrySet()) {
            storedMap = streamIdNumMap.get(entry.getKey());
            if (storedMap == null) {
                storedMap = new ConcurrentHashMap<>(entry.getValue().size());
                storedMap.putAll(entry.getValue());
                streamIdNumMap.put(entry.getKey(), storedMap);
            } else {
                rmvKeys.clear();
                newDataMap = entry.getValue();
                for (Map.Entry<String, String> entry1 : newDataMap.entrySet()) {
                    if (!entry1.getValue().equals(storedMap.get(entry.getKey()))) {
                        storedMap.put(entry1.getKey(), entry1.getValue());
                    }
                }
                for (Map.Entry<String, String> entry1 : storedMap.entrySet()) {
                    if (!newDataMap.containsKey(entry1.getKey())) {
                        rmvKeys.add(entry.getKey());
                    }
                }
                for (String tmpKey : rmvKeys) {
                    storedMap.remove(tmpKey);
                }
                if (storedMap.isEmpty()) {
                    streamIdNumMap.remove(entry.getKey());
                }
            }
        }
        // remove cached streamId num2Name data
        rmvKeys.clear();
        for (Map.Entry<String, ConcurrentHashMap<String, String>> entry : streamIdNumMap.entrySet()) {
            if (!newStreamIdNumMap.containsKey(entry.getKey())) {
                rmvKeys.add(entry.getKey());
            }
        }
        for (String tmpKey : rmvKeys) {
            streamIdNumMap.remove(tmpKey);
        }
    }
}
