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

package org.apache.inlong.manager.service.core.impl;

import com.google.gson.Gson;
import jodd.util.StringUtil;
import org.apache.inlong.common.pojo.sdk.CacheZone;
import org.apache.inlong.common.pojo.sdk.Topic;
import org.apache.inlong.manager.dao.entity.SortSourceConfigEntity;
import org.apache.inlong.manager.dao.mapper.SortSourceConfigEntityMapper;
import org.apache.inlong.manager.service.core.SortSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of {@link SortSourceService}.
 */
@Service
public class SortSourceServiceImpl implements SortSourceService {

    private static final String KEY_TOPIC_PARTITION_COUNT = "partitionCnt";
    private static final String KEY_ZONE_SERVICE_URL = "serviceUrl";
    private static final String KEY_ZONE_AUTHENTICATION = "authentication";
    private static final String KEY_ZONE_TYPE = "zoneType";

    @Autowired
    private SortSourceConfigEntityMapper sortSourceConfigEntityMapper;

    @Override
    public Map<String, CacheZone> getCacheZones(String clusterName, String taskName) {
        List<SortSourceConfigEntity> configList =
                sortSourceConfigEntityMapper.selectByClusterAndTask(clusterName, taskName);

        // group configs by zone name
        Map<String, List<SortSourceConfigEntity>> zoneConfigMap =
                configList.stream().collect(Collectors.groupingBy(SortSourceConfigEntity::getZoneName));

        return zoneConfigMap.values()
                .stream()
                .map(this::toCacheZone)
                .collect(Collectors.toMap(CacheZone::getZoneName, cacheZone -> cacheZone));
    }

    /**
     * Build one {@link CacheZone} from list of configs including zone config and configs of each topic.
     *
     * <p>
     *     The way we differ zone config and topic config is
     *     if the params <b>topic</b> in {@link SortSourceConfigEntity} is blank.
     *     If topic is blank, it's the <b>zone config</b>,
     *     otherwise, it's <b>topic config</b>.
     * </p>
     *
     * @param configList List of configs.
     * @return CacheZone.
     */
    private CacheZone toCacheZone(List<SortSourceConfigEntity> configList) {
        // get list of Topic
        List<Topic> topicList
                = configList.stream()
                .filter(config -> StringUtil.isNotBlank(config.getTopic()))
                .map(this::toTopic)
                .collect(Collectors.toList());

        // get zone config
        List<SortSourceConfigEntity> zoneConfigs
                = configList.stream()
                .filter(config -> StringUtil.isBlank(config.getTopic()))
                .collect(Collectors.toList());

        if (zoneConfigs.size() != 1) {
            throw new IllegalStateException("The size of zone config should be 1, but found " +  zoneConfigs.size());
        }

        SortSourceConfigEntity zoneConfig = zoneConfigs.get(0);
        Map<String, String> zoneProperties = this.jsonProperty2Map(zoneConfig.getExtParams());

        return CacheZone.builder()
                .zoneName(zoneConfig.getZoneName())
                .serviceUrl(zoneProperties.remove(KEY_ZONE_SERVICE_URL))
                .authentication(zoneProperties.remove(KEY_ZONE_AUTHENTICATION))
                .topics(topicList)
                .zoneType(zoneProperties.remove(KEY_ZONE_TYPE))
                .cacheZoneProperties(zoneProperties)
                .build();
    }

    /**
     * Build one {@link Topic} from the corresponding config.
     *
     * @param topicConfig Config of topic.
     * @return Topic.
     */
    private Topic toTopic(SortSourceConfigEntity topicConfig) {
        Map<String, String> topicProperties = this.jsonProperty2Map(topicConfig.getExtParams());
        int partitionCnt = Integer.parseInt(topicProperties.remove(KEY_TOPIC_PARTITION_COUNT));
        return Topic.builder()
                .topic(topicConfig.getTopic())
                .partitionCnt(partitionCnt)
                .topicProperties(topicProperties)
                .build();
    }

    /**
     * Convert property json string to map format.
     *
     * @param jsonProperty Properties in json string format.
     * @return Properties in map format.
     */
    private Map<String, String> jsonProperty2Map(String jsonProperty) {
        return new Gson().fromJson(jsonProperty, Map.class);
    }

}
