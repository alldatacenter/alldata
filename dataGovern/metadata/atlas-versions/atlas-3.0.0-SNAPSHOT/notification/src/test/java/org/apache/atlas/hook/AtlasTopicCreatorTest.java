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

import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.utils.KafkaUtils;
import org.apache.commons.configuration.Configuration;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AtlasTopicCreatorTest {

    private final String ATLAS_HOOK_TOPIC     = AtlasConfiguration.NOTIFICATION_HOOK_TOPIC_NAME.getString();
    private final String ATLAS_ENTITIES_TOPIC = AtlasConfiguration.NOTIFICATION_ENTITIES_TOPIC_NAME.getString();

    @Test
    public void shouldNotCreateAtlasTopicIfNotConfiguredToDoSo() {

        Configuration configuration = mock(Configuration.class);
        when(configuration.getBoolean(AtlasTopicCreator.ATLAS_NOTIFICATION_CREATE_TOPICS_KEY, true)).
                thenReturn(false);

        AtlasTopicCreator atlasTopicCreator = new AtlasTopicCreator();
        AtlasTopicCreator spyAtlasTopicCreator = Mockito.spy(atlasTopicCreator);
        spyAtlasTopicCreator.createAtlasTopic(configuration, ATLAS_HOOK_TOPIC);
        Mockito.verify(spyAtlasTopicCreator, times(0)).handleSecurity(configuration);

    }

    @Test
    public void shouldCreateTopicIfConfiguredToDoSo() {
        Configuration configuration = mock(Configuration.class);

        KafkaUtils mockKafkaUtils = Mockito.mock(KafkaUtils.class);
        when(configuration.getBoolean(AtlasTopicCreator.ATLAS_NOTIFICATION_CREATE_TOPICS_KEY, true)).
                thenReturn(true);
        when(configuration.getString("atlas.authentication.method.kerberos")).thenReturn("false");
        AtlasTopicCreator atlasTopicCreator = new AtlasTopicCreator();

        AtlasTopicCreator spyAtlasTopicCreator = Mockito.spy(atlasTopicCreator);
        Mockito.doReturn(mockKafkaUtils).when(spyAtlasTopicCreator).getKafkaUtils(configuration);

        spyAtlasTopicCreator.createAtlasTopic(configuration, ATLAS_HOOK_TOPIC);

        try {
            verify(mockKafkaUtils).createTopics(anyList(), anyInt(), anyInt());
        } catch (ExecutionException | InterruptedException e) {
            Assert.fail("Caught exception while verifying createTopics: " + e.getMessage());
        }

    }

    @Test
    public void shouldCloseResources() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getBoolean(AtlasTopicCreator.ATLAS_NOTIFICATION_CREATE_TOPICS_KEY, true)).
                thenReturn(true);
        when(configuration.getString("atlas.authentication.method.kerberos")).thenReturn("false");
        KafkaUtils mockKafkaUtils = Mockito.mock(KafkaUtils.class);

        AtlasTopicCreator atlasTopicCreator = new AtlasTopicCreator();

        AtlasTopicCreator spyAtlasTopicCreator = Mockito.spy(atlasTopicCreator);
        Mockito.doReturn(mockKafkaUtils).when(spyAtlasTopicCreator).getKafkaUtils(configuration);

        spyAtlasTopicCreator.createAtlasTopic(configuration, ATLAS_HOOK_TOPIC);

        verify(mockKafkaUtils).close();

    }

    @Test
    public void shouldNotProcessTopicCreationIfSecurityFails() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getBoolean(AtlasTopicCreator.ATLAS_NOTIFICATION_CREATE_TOPICS_KEY, true)).
                thenReturn(true);
        KafkaUtils mockKafkaUtils = Mockito.mock(KafkaUtils.class);
        AtlasTopicCreator atlasTopicCreator = new AtlasTopicCreator();

        AtlasTopicCreator spyAtlasTopicCreator = Mockito.spy(atlasTopicCreator);
        Mockito.doReturn(mockKafkaUtils).when(spyAtlasTopicCreator).getKafkaUtils(configuration);
        Mockito.doReturn(false).when(spyAtlasTopicCreator).handleSecurity(configuration);

        spyAtlasTopicCreator.createAtlasTopic(configuration, ATLAS_HOOK_TOPIC, ATLAS_ENTITIES_TOPIC);

        try {
            verify(mockKafkaUtils, times(0)).createTopics(anyList(), anyInt(), anyInt());
        } catch (ExecutionException | InterruptedException e) {
            Assert.fail("Caught exception while verifying createTopics: " + e.getMessage());
        }
    }
}
