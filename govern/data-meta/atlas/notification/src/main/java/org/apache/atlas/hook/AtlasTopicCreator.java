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

package org.apache.atlas.hook;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.atlas.utils.KafkaUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

/**
 * A class to create Kafka topics used by Atlas components.
 *
 * Use this class to create a Kafka topic with specific configuration like number of partitions, replicas, etc.
 */
public class AtlasTopicCreator {

    private static final Logger LOG = LoggerFactory.getLogger(AtlasTopicCreator.class);

    public static final String ATLAS_NOTIFICATION_CREATE_TOPICS_KEY = "atlas.notification.create.topics";

    /**
     * Create an Atlas topic.
     *
     * The topic will get created based on following conditions:
     * {@link #ATLAS_NOTIFICATION_CREATE_TOPICS_KEY} is set to true.
     * The topic does not already exist.
     * Note that despite this, there could be multiple topic creation calls that happen in parallel because hooks
     * run in a distributed fashion. Exceptions are caught and logged by this method to prevent the startup of
     * the hooks from failing.
     * @param atlasProperties {@link Configuration} containing properties to be used for creating topics.
     * @param topicNames list of topics to create
     */
    public void createAtlasTopic(Configuration atlasProperties, String... topicNames) {
        if (atlasProperties.getBoolean(ATLAS_NOTIFICATION_CREATE_TOPICS_KEY, true)) {
            if (!handleSecurity(atlasProperties)) {
                return;
            }
            try(KafkaUtils kafkaUtils = getKafkaUtils(atlasProperties)) {
                int numPartitions = atlasProperties.getInt("atlas.notification.partitions", 1);
                int numReplicas = atlasProperties.getInt("atlas.notification.replicas", 1);
                kafkaUtils.createTopics(Arrays.asList(topicNames), numPartitions, numReplicas);
            } catch (Exception e) {
                LOG.error("Error while creating topics e :" + e.getMessage(), e);
            }
        } else {
            LOG.info("Not creating topics {} as {} is false", StringUtils.join(topicNames, ","),
                    ATLAS_NOTIFICATION_CREATE_TOPICS_KEY);
        }
    }

    @VisibleForTesting
    protected boolean handleSecurity(Configuration atlasProperties) {
        if (AuthenticationUtil.isKerberosAuthenticationEnabled(atlasProperties)) {
            String kafkaPrincipal = atlasProperties.getString("atlas.notification.kafka.service.principal");
            String kafkaKeyTab = atlasProperties.getString("atlas.notification.kafka.keytab.location");
            org.apache.hadoop.conf.Configuration hadoopConf = new org.apache.hadoop.conf.Configuration();
            SecurityUtil.setAuthenticationMethod(UserGroupInformation.AuthenticationMethod.KERBEROS, hadoopConf);
            try {
                String serverPrincipal = SecurityUtil.getServerPrincipal(kafkaPrincipal, (String) null);
                UserGroupInformation.setConfiguration(hadoopConf);
                UserGroupInformation.loginUserFromKeytab(serverPrincipal, kafkaKeyTab);
            } catch (IOException e) {
                LOG.warn("Could not login as {} from keytab file {}", kafkaPrincipal, kafkaKeyTab, e);
                return false;
            }
        }
        return true;
    }

    // This method is added to mock the creation of kafkaUtils object while writing the test cases
    KafkaUtils getKafkaUtils(Configuration configuration) {
        return new KafkaUtils(configuration);
    }

    public static void main(String[] args) throws AtlasException {
        Configuration configuration = ApplicationProperties.get();
        AtlasTopicCreator atlasTopicCreator = new AtlasTopicCreator();
        atlasTopicCreator.createAtlasTopic(configuration, args);
    }
}
