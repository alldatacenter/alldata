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

package org.apache.inlong.sort.standalone.config.loader;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.apache.inlong.common.pojo.sdk.CacheZone;
import org.apache.inlong.common.pojo.sdk.CacheZoneConfig;
import org.apache.inlong.common.pojo.sdk.Topic;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.QueryConsumeConfig;
import org.apache.inlong.sdk.sort.entity.CacheZoneCluster;
import org.apache.inlong.sdk.sort.entity.ConsumeConfig;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassResourceQueryConsumeConfig
 */
public class ClassResourceQueryConsumeConfig implements QueryConsumeConfig {

    public static final Logger LOG = LoggerFactory.getLogger(ClassResourceQueryConsumeConfig.class);

    /**
     * queryCurrentConsumeConfig
     *
     * @param  sortTaskId
     * @return
     */
    @Override
    public ConsumeConfig queryCurrentConsumeConfig(String sortTaskId) {
        String fileName = sortTaskId + ".conf";
        try {
            String confString = IOUtils.toString(getClass().getClassLoader().getResource(fileName),
                    Charset.defaultCharset());
            ObjectMapper objectMapper = new ObjectMapper();
            CacheZoneConfig cacheZoneConfig = objectMapper.readValue(confString, CacheZoneConfig.class);
            //
            Map<String, List<InLongTopic>> newGroupTopicsMap = new HashMap<>();
            for (Map.Entry<String, CacheZone> entry : cacheZoneConfig.getCacheZones().entrySet()) {
                CacheZone cacheZone = entry.getValue();

                List<InLongTopic> topics = newGroupTopicsMap.computeIfAbsent(cacheZoneConfig.getSortTaskId(),
                        k -> new ArrayList<>());
                CacheZoneCluster cacheZoneCluster = new CacheZoneCluster(cacheZone.getZoneName(),
                        cacheZone.getServiceUrl(), cacheZone.getAuthentication());
                for (Topic topicInfo : cacheZone.getTopics()) {
                    InLongTopic topic = new InLongTopic();
                    topic.setInLongCluster(cacheZoneCluster);
                    topic.setTopic(topicInfo.getTopic());
                    topic.setTopicType(cacheZone.getZoneType());
                    topics.add(topic);
                }
            }
            return new ConsumeConfig(newGroupTopicsMap.get(sortTaskId));
        } catch (UnsupportedEncodingException e) {
            LOG.error("fail to load properties, file ={}, and e= {}", fileName, e);
        } catch (Exception e) {
            LOG.error("fail to load properties, file ={}, and e= {}", fileName, e);
        }
        return null;
    }

    @Override
    public void configure(ClientContext context) {
        // do nothing
    }

}
