/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.utils;

import org.apache.atlas.ApplicationProperties;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.TopicExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class KafkaUtils implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaUtils.class);

    private static final String JAAS_CONFIG_PREFIX_PARAM                     = "atlas.jaas";
    private static final String JAAS_CONFIG_LOGIN_MODULE_NAME_PARAM          = "loginModuleName";
    private static final String JAAS_CONFIG_LOGIN_MODULE_CONTROL_FLAG_PARAM  = "loginModuleControlFlag";
    private static final String JAAS_DEFAULT_LOGIN_MODULE_CONTROL_FLAG       = "required";
    private static final String JAAS_VALID_LOGIN_MODULE_CONTROL_FLAG_OPTIONS = "optional|requisite|sufficient|required";
    private static final String JAAS_CONFIG_LOGIN_OPTIONS_PREFIX             = "option";
    private static final String JAAS_PRINCIPAL_PROP                          = "principal";
    private static final String JAAS_DEFAULT_CLIENT_NAME                     = "KafkaClient";
    private static final String JAAS_TICKET_BASED_CLIENT_NAME                = "ticketBased-KafkaClient";
    private static final String IMPORT_INTERNAL_TOPICS                       = "atlas.kafka.bridge.enable.internal.topics.import";

    public static final String ATLAS_KAFKA_PROPERTY_PREFIX     = "atlas.kafka";
    public static final String KAFKA_SASL_JAAS_CONFIG_PROPERTY = "sasl.jaas.config";

    final protected Properties  kafkaConfiguration;
    final protected AdminClient adminClient;
    final protected boolean     importInternalTopics;

    public KafkaUtils(Configuration atlasConfiguration) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> KafkaUtils() ");
        }

        this.kafkaConfiguration = ApplicationProperties.getSubsetAsProperties(atlasConfiguration, ATLAS_KAFKA_PROPERTY_PREFIX);

        setKafkaJAASProperties(atlasConfiguration, kafkaConfiguration);

        adminClient          = AdminClient.create(this.kafkaConfiguration);
        importInternalTopics = atlasConfiguration.getBoolean(IMPORT_INTERNAL_TOPICS, false);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== KafkaUtils() ");
        }
    }

    public void createTopics(List<String> topicNames, int numPartitions, int replicationFactor)
            throws TopicExistsException, ExecutionException, InterruptedException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> createTopics() ");
        }

        List<NewTopic> newTopicList = topicNames.stream()
                                                .map(topicName -> new NewTopic(topicName, numPartitions, (short) replicationFactor))
                                                .collect(Collectors.toList());

        CreateTopicsResult             createTopicsResult = adminClient.createTopics(newTopicList);
        Map<String, KafkaFuture<Void>> futureMap          = createTopicsResult.values();

        for (Map.Entry<String, KafkaFuture<Void>> futureEntry : futureMap.entrySet()) {
            KafkaFuture<Void> future = futureEntry.getValue();

            future.get();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== createTopics() ");
        }
    }

    public List<String> listAllTopics() throws ExecutionException, InterruptedException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> KafkaUtils.listAllTopics() ");
        }

        ListTopicsResult listTopicsResult = adminClient.listTopics((new ListTopicsOptions()).listInternal(importInternalTopics));
        List<String>     ret              = new ArrayList<>(listTopicsResult.names().get());

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== KafkaUtils.listAllTopics() ");
        }

        return ret;
    }

    public Integer getPartitionCount(String topicName) throws ExecutionException, InterruptedException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> KafkaUtils.getPartitionCount({})", topicName);
        }

        Integer                  ret            = null;
        List<TopicPartitionInfo> partitionList  = getPartitionList(topicName);

        if (partitionList != null) {
            ret = partitionList.size();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== KafkaUtils.getPartitionCount returning for topic {} with count {}", topicName, ret);
        }

        return ret;
    }

    public Integer getReplicationFactor(String topicName) throws ExecutionException, InterruptedException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> KafkaUtils.getReplicationFactor({})", topicName);
        }

        Integer                  ret           = null;
        List<TopicPartitionInfo> partitionList = getPartitionList(topicName);

        if (partitionList != null) {
            ret = partitionList.stream().mapToInt(x -> x.replicas().size()).max().getAsInt();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== KafkaUtils.getReplicationFactor returning for topic {} with replicationFactor {}", topicName, ret);
        }

        return ret;
    }

    private List<TopicPartitionInfo> getPartitionList(String topicName) throws ExecutionException, InterruptedException {
        List<TopicPartitionInfo> ret                  = null;
        DescribeTopicsResult     describeTopicsResult = adminClient.describeTopics(Collections.singleton(topicName));

        if (describeTopicsResult != null) {
            Map<String, KafkaFuture<TopicDescription>> futureMap = describeTopicsResult.values();

            for (Map.Entry<String, KafkaFuture<TopicDescription>> futureEntry : futureMap.entrySet()) {
                KafkaFuture<TopicDescription> topicDescriptionFuture = futureEntry.getValue();
                TopicDescription              topicDescription       = topicDescriptionFuture.get();

                ret = topicDescription.partitions();
            }
        }

        return ret;
    }

    public void close() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> KafkaUtils.close()");
        }
        if (adminClient != null) {
            adminClient.close();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== KafkaUtils.close()");
        }
    }

    public static void setKafkaJAASProperties(Configuration configuration, Properties kafkaProperties) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> KafkaUtils.setKafkaJAASProperties()");
        }

        if (kafkaProperties.containsKey(KAFKA_SASL_JAAS_CONFIG_PROPERTY)) {
            LOG.debug("JAAS config is already set, returning");

            return;
        }

        Properties jaasConfig = ApplicationProperties.getSubsetAsProperties(configuration, JAAS_CONFIG_PREFIX_PARAM);
        // JAAS Configuration is present then update set those properties in sasl.jaas.config

        if (jaasConfig != null && !jaasConfig.isEmpty()) {
            String jaasClientName = JAAS_DEFAULT_CLIENT_NAME;

            // Required for backward compatability for Hive CLI
            if (!isLoginKeytabBased() && isLoginTicketBased()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Checking if ticketBased-KafkaClient is set");
                }

                // if ticketBased-KafkaClient property is not specified then use the default client name
                String        ticketBasedConfigPrefix = JAAS_CONFIG_PREFIX_PARAM + "." + JAAS_TICKET_BASED_CLIENT_NAME;
                Configuration ticketBasedConfig       = configuration.subset(ticketBasedConfigPrefix);

                if (ticketBasedConfig != null && !ticketBasedConfig.isEmpty()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("ticketBased-KafkaClient JAAS configuration is set, using it");
                    }

                    jaasClientName = JAAS_TICKET_BASED_CLIENT_NAME;
                } else {
                    LOG.info("UserGroupInformation.isLoginTicketBased is true, but no JAAS configuration found for client {}. Will use JAAS configuration of client {}", JAAS_TICKET_BASED_CLIENT_NAME, jaasClientName);
                }
            }

            String keyPrefix       = jaasClientName + ".";
            String keyParam        = keyPrefix + JAAS_CONFIG_LOGIN_MODULE_NAME_PARAM;
            String loginModuleName = jaasConfig.getProperty(keyParam);

            if (loginModuleName == null) {
                LOG.error("Unable to add JAAS configuration for client [{}] as it is missing param [{}]. Skipping JAAS config for [{}]", jaasClientName, keyParam, jaasClientName);

                return;
            }

            keyParam = keyPrefix + JAAS_CONFIG_LOGIN_MODULE_CONTROL_FLAG_PARAM;

            String controlFlag = jaasConfig.getProperty(keyParam);

            if (StringUtils.isEmpty(controlFlag)) {
                String validValues = JAAS_VALID_LOGIN_MODULE_CONTROL_FLAG_OPTIONS;

                controlFlag = JAAS_DEFAULT_LOGIN_MODULE_CONTROL_FLAG;

                LOG.warn("Unknown JAAS configuration value for ({}) = [{}], valid value are [{}] using the default value, REQUIRED", keyParam, controlFlag, validValues);
            }

            String       optionPrefix       = keyPrefix + JAAS_CONFIG_LOGIN_OPTIONS_PREFIX + ".";
            String       principalOptionKey = optionPrefix + JAAS_PRINCIPAL_PROP;
            int          optionPrefixLen    = optionPrefix.length();
            StringBuffer optionStringBuffer = new StringBuffer();

            for (String key : jaasConfig.stringPropertyNames()) {
                if (key.startsWith(optionPrefix)) {
                    String optionVal = jaasConfig.getProperty(key);

                    if (optionVal != null) {
                        optionVal = optionVal.trim();

                        try {
                            if (key.equalsIgnoreCase(principalOptionKey)) {
                                optionVal = org.apache.hadoop.security.SecurityUtil.getServerPrincipal(optionVal, (String) null);
                            }
                        } catch (IOException e) {
                            LOG.warn("Failed to build serverPrincipal. Using provided value:[{}]", optionVal);
                        }

                        optionVal = surroundWithQuotes(optionVal);

                        optionStringBuffer.append(String.format(" %s=%s", key.substring(optionPrefixLen), optionVal));
                    }
                }
            }

            String newJaasProperty = String.format("%s %s %s ;", loginModuleName.trim(), controlFlag, optionStringBuffer.toString());

            kafkaProperties.put(KAFKA_SASL_JAAS_CONFIG_PROPERTY, newJaasProperty);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== KafkaUtils.setKafkaJAASProperties()");
        }
    }

    static boolean isLoginKeytabBased() {
        boolean ret = false;

        try {
            ret = UserGroupInformation.isLoginKeytabBased();
        } catch (Exception excp) {
            LOG.warn("Error in determining keytab for KafkaClient-JAAS config", excp);
        }

        return ret;
    }

    public static boolean isLoginTicketBased() {
        boolean ret = false;

        try {
            ret = UserGroupInformation.isLoginTicketBased();
        } catch (Exception excp) {
            LOG.warn("Error in determining ticket-cache for KafkaClient-JAAS config", excp);
        }

        return ret;
    }

    static String surroundWithQuotes(String optionVal) {
        if (StringUtils.isEmpty(optionVal)) {
            return optionVal;
        }

        // Enclose property values in double quotes, so that Kafka can parse it.
        // Escape all double quotes that may occur in the property value.
        String doubleQuoteEscaped = optionVal.replace("\"", "\\\"");
        return String.format("\"%s\"", doubleQuoteEscaped);
    }
}