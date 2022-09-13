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

package org.apache.inlong.manager.service.resource.queue.tubemq;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.pojo.cluster.tubemq.TubeClusterInfo;
import org.apache.inlong.manager.pojo.queue.tubemq.ConsumerGroupResponse;
import org.apache.inlong.manager.pojo.queue.tubemq.TopicResponse;
import org.apache.inlong.manager.pojo.queue.tubemq.TubeBrokerInfo;
import org.apache.inlong.manager.pojo.queue.tubemq.TubeHttpResponse;
import org.apache.inlong.manager.common.util.HttpUtils;
import org.apache.inlong.manager.service.cluster.InlongClusterServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * TubeMQ operator, supports creating topics and creating consumer groups.
 */
@Service
public class TubeMQOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongClusterServiceImpl.class);
    private static final Integer SUCCESS_CODE = 0;

    /**
     * TubeMQ const for HTTP URL format
     */
    private static final String TOPIC_NAME = "&topicName=";
    private static final String CONSUME_GROUP = "&consumeGroup=";
    private static final String GROUP_NAME = "&groupName=";
    private static final String BROKER_ID = "&brokerId=";
    private static final String CREATE_USER = "&createUser=";
    private static final String CONF_MOD_AUTH_TOKEN = "&confModAuthToken=";

    private static final String QUERY_TOPIC_PATH = "/webapi.htm?method=admin_query_cluster_topic_view";
    private static final String QUERY_BROKER_PATH = "/webapi.htm?method=admin_query_broker_run_status";
    private static final String ADD_TOPIC_PATH = "/webapi.htm?method=admin_add_new_topic_record";
    private static final String QUERY_CONSUMER_PATH = "/webapi.htm?method=admin_query_allowed_consumer_group_info";
    private static final String ADD_CONSUMER_PATH = "/webapi.htm?method=admin_add_authorized_consumergroup_info";

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Create topic for the given tubemq cluster.
     */
    public void createTopic(@Nonnull TubeClusterInfo tubeCluster, String topicName, String operator) {
        String masterUrl = tubeCluster.getMasterWebUrl();
        LOGGER.info("begin to create tubemq topic {} in master {}", topicName, masterUrl);
        if (StringUtils.isEmpty(masterUrl) || StringUtils.isEmpty(topicName)) {
            throw new BusinessException("tubemq master url or tubemq topic cannot be null");
        }

        if (this.isTopicExist(masterUrl, topicName)) {
            LOGGER.warn("tubemq topic {} already exists in {}, skip to create", topicName, masterUrl);
            return;
        }

        this.createTopicOpt(masterUrl, topicName, tubeCluster.getToken(), operator);
        LOGGER.info("success to create tubemq topic {} in {}", topicName, masterUrl);
    }

    /**
     * Create consumer group for the given tubemq topic and cluster.
     */
    public void createConsumerGroup(TubeClusterInfo tubeCluster, String topic, String consumerGroup, String operator) {
        String masterUrl = tubeCluster.getMasterWebUrl();
        LOGGER.info("begin to create consumer group {} for topic {} in master {}", consumerGroup, topic, masterUrl);
        if (StringUtils.isEmpty(masterUrl) || StringUtils.isEmpty(consumerGroup) || StringUtils.isEmpty(topic)) {
            throw new BusinessException("tubemq master url, consumer group, or tubemq topic cannot be null");
        }

        if (!this.isTopicExist(masterUrl, topic)) {
            LOGGER.warn("cannot create tubemq consumer group {}, as the topic {} not exists in master {}",
                    consumerGroup, topic, masterUrl);
            return;
        }

        if (this.isConsumerGroupExist(masterUrl, topic, consumerGroup)) {
            LOGGER.warn("tubemq consumer group {} already exists for topic {} in master {}, skip to create",
                    consumerGroup, topic, masterUrl);
            return;
        }

        this.createConsumerGroupOpt(masterUrl, topic, consumerGroup, tubeCluster.getToken(), operator);
        LOGGER.info("success to create tubemq consumer group {} for topic {} in {}", consumerGroup, topic, masterUrl);
    }

    /**
     * Check if the topic is exists in the TubeMQ.
     */
    public boolean isTopicExist(String masterUrl, String topicName) {
        LOGGER.info("begin to check if the tubemq topic {} exists", topicName);
        String url = masterUrl + QUERY_TOPIC_PATH + TOPIC_NAME + topicName;
        try {
            TopicResponse topicView = HttpUtils.request(restTemplate, url, HttpMethod.GET,
                    null, new HttpHeaders(), TopicResponse.class);
            if (CollectionUtils.isEmpty(topicView.getData())) {
                LOGGER.warn("tubemq topic {} not exists in {}", topicName, url);
                return false;
            }
            LOGGER.info("tubemq topic {} exists in {}", topicName, url);
            return true;
        } catch (Exception e) {
            String msg = String.format("failed to check if the topic %s exist in ", topicName);
            LOGGER.error(msg + url, e);
            throw new BusinessException(msg + masterUrl + ", error: " + e.getMessage());
        }
    }

    /**
     * Check if the consumer group is exists for the given topic.
     */
    public boolean isConsumerGroupExist(String masterUrl, String topicName, String consumerGroup) {
        LOGGER.info("begin to check if the consumer group {} exists on topic {}", consumerGroup, topicName);
        String url = masterUrl + QUERY_CONSUMER_PATH + TOPIC_NAME + topicName + CONSUME_GROUP + consumerGroup;
        try {
            ConsumerGroupResponse response = HttpUtils.request(restTemplate, url, HttpMethod.GET,
                    null, new HttpHeaders(), ConsumerGroupResponse.class);
            if (CollectionUtils.isEmpty(response.getData())) {
                LOGGER.warn("tubemq consumer group {} not exists for topic {} in {}", consumerGroup, topicName, url);
                return false;
            }
            LOGGER.info("tubemq consumer group {} exists for topic {} in {}", consumerGroup, topicName, url);
            return true;
        } catch (Exception e) {
            String msg = String.format("failed to check if the consumer group %s for topic %s exist in ",
                    consumerGroup, topicName);
            LOGGER.error(msg + url, e);
            throw new BusinessException(msg + masterUrl + ", error: " + e.getMessage());
        }
    }

    /**
     * Get the broker list by the given TubeMQ master URL.
     */
    private TubeBrokerInfo getBrokerInfo(String masterUrl) {
        String url = masterUrl + QUERY_BROKER_PATH;
        try {
            TubeBrokerInfo brokerInfo = HttpUtils.request(restTemplate, url, HttpMethod.GET,
                    null, new HttpHeaders(), TubeBrokerInfo.class);
            if (brokerInfo.getErrCode() != SUCCESS_CODE) {
                String msg = "failed to query tubemq broker from %s, error: %s";
                LOGGER.error(String.format(msg, url, brokerInfo.getErrMsg()));
                throw new BusinessException(String.format(msg, masterUrl, brokerInfo.getErrMsg()));
            }

            // is success, divide the broker by status
            brokerInfo.divideBrokerListByStatus();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("success to query tubemq broker from {}, result {}", url, brokerInfo.getData());
            }
            return brokerInfo;
        } catch (Exception e) {
            String msg = "failed to query tubemq broker from %s";
            LOGGER.error(String.format(msg, url), e);
            throw new BusinessException(String.format(msg, masterUrl) + ", error: " + e.getMessage());
        }
    }

    /**
     * Create topic operation.
     */
    private void createTopicOpt(String masterUrl, String topicName, String token, String operator) {
        LOGGER.info(String.format("begin to create tubemq topic %s in master %s", topicName, masterUrl));
        TubeBrokerInfo brokerView = this.getBrokerInfo(masterUrl);
        List<Integer> allBrokers = brokerView.getAllBrokerIdList();
        if (CollectionUtils.isEmpty(allBrokers)) {
            String msg = String.format("cannot create topic %s, as not any brokers found in %s", topicName, masterUrl);
            LOGGER.error(msg);
            throw new BusinessException(msg);
        }

        // create topic for all brokers
        String url = masterUrl + ADD_TOPIC_PATH + TOPIC_NAME + topicName
                + BROKER_ID + StringUtils.join(allBrokers, ",")
                + CREATE_USER + operator + CONF_MOD_AUTH_TOKEN + token;
        try {
            TubeHttpResponse response = HttpUtils.request(restTemplate, url, HttpMethod.GET,
                    null, new HttpHeaders(), TubeHttpResponse.class);
            if (response.getErrCode() != SUCCESS_CODE) {
                String msg = String.format("failed to create tubemq topic %s, error: %s",
                        topicName, response.getErrMsg());
                LOGGER.error(msg + " in {} for brokers {}", masterUrl, allBrokers);
                throw new BusinessException(msg);
            }

            LOGGER.info("success to create tubemq topic {} in {}", topicName, url);
        } catch (Exception e) {
            String msg = String.format("failed to create tubemq topic %s in %s", topicName, masterUrl);
            LOGGER.error(msg, e);
            throw new BusinessException(msg + ", error: " + e.getMessage());
        }
    }

    /**
     * Create consumer group operation.
     */
    private void createConsumerGroupOpt(String masterUrl, String topicName, String consumerGroup, String token,
            String operator) {
        LOGGER.info(String.format("begin to create consumer group %s for topic %s in master %s",
                consumerGroup, topicName, masterUrl));

        String url = masterUrl + ADD_CONSUMER_PATH + TOPIC_NAME + topicName
                + GROUP_NAME + consumerGroup
                + CREATE_USER + operator + CONF_MOD_AUTH_TOKEN + token;
        try {
            TubeHttpResponse response = HttpUtils.request(restTemplate, url, HttpMethod.GET,
                    null, new HttpHeaders(), TubeHttpResponse.class);
            if (response.getErrCode() != SUCCESS_CODE) {
                String msg = String.format("failed to create tubemq consumer group %s for topic %s, error: %s",
                        consumerGroup, topicName, response.getErrMsg());
                LOGGER.error(msg + ", url {}", url);
                throw new BusinessException(msg);
            }
            LOGGER.info("success to create tubemq topic {} in {}", topicName, url);
        } catch (Exception e) {
            String msg = String.format("failed to create tubemq topic %s in %s", topicName, masterUrl);
            LOGGER.error(msg, e);
            throw new BusinessException(msg + ", error: " + e.getMessage());
        }
    }

}
