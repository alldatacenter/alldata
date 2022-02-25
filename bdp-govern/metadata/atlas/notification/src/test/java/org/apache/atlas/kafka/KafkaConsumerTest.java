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

package org.apache.atlas.kafka;

import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.notification.IncompatibleVersionException;
import org.apache.atlas.notification.NotificationInterface.NotificationType;
import org.apache.atlas.model.notification.AtlasNotificationMessage;
import org.apache.atlas.notification.entity.EntityNotificationTest;
import org.apache.atlas.v1.model.notification.HookNotificationV1.EntityUpdateRequest;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.model.notification.MessageVersion;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * KafkaConsumer tests.
 */
public class KafkaConsumerTest {
    private static final String TRAIT_NAME = "MyTrait";

    private static final String ATLAS_HOOK_TOPIC = AtlasConfiguration.NOTIFICATION_HOOK_TOPIC_NAME.getString();
    private static final String[] ATLAS_HOOK_CONSUMER_TOPICS = KafkaNotification.trimAndPurge(AtlasConfiguration.NOTIFICATION_HOOK_CONSUMER_TOPIC_NAMES.getStringArray(ATLAS_HOOK_TOPIC));


    @Mock
    private KafkaConsumer kafkaConsumer;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testReceive() throws Exception {
        for (String topic : ATLAS_HOOK_CONSUMER_TOPICS) {
            String traitName = TRAIT_NAME + "_" + topic;
            Referenceable entity = getEntity(traitName);
            EntityUpdateRequest message = new EntityUpdateRequest("user1", entity);
            List<AtlasKafkaMessage<HookNotification>> messageList = testReceiveHelper(message, topic);
            assertTrue(messageList.size() > 0);

            HookNotification consumedMessage = messageList.get(0).getMessage();

            assertMessagesEqual(message, consumedMessage, entity);
        }
    }


    private List<AtlasKafkaMessage<HookNotification>> testReceiveHelper(EntityUpdateRequest message, String topic) throws Exception {

        String                               json    = AtlasType.toV1Json(new AtlasNotificationMessage<>(new MessageVersion("1.0.0"), message));
        TopicPartition                       tp      = new TopicPartition(topic, 0);
        List<ConsumerRecord<String, String>> klist   = Collections.singletonList(new ConsumerRecord<>(topic, 0, 0L, "mykey", json));
        Map                                  mp      = Collections.singletonMap(tp, klist);
        ConsumerRecords                      records = new ConsumerRecords(mp);

        when(kafkaConsumer.poll(100)).thenReturn(records);

        kafkaConsumer.assign(Collections.singletonList(tp));

        AtlasKafkaConsumer                        consumer    = new AtlasKafkaConsumer(NotificationType.HOOK, kafkaConsumer, false, 100L);
        List<AtlasKafkaMessage<HookNotification>> messageList = consumer.receive();
        return messageList;
    }

    @Test
    public void testNextVersionMismatch() throws Exception {
        Referenceable                        entity  = getEntity(TRAIT_NAME);
        EntityUpdateRequest                  message = new EntityUpdateRequest("user1", entity);
        String                               json    = AtlasType.toV1Json(new AtlasNotificationMessage<>(new MessageVersion("2.0.0"), message));
        TopicPartition                       tp      = new TopicPartition(ATLAS_HOOK_TOPIC,0);
        List<ConsumerRecord<String, String>> klist   = Collections.singletonList(new ConsumerRecord<>(ATLAS_HOOK_TOPIC, 0, 0L, "mykey", json));
        Map                                  mp      = Collections.singletonMap(tp,klist);
        ConsumerRecords                      records = new ConsumerRecords(mp);

        kafkaConsumer.assign(Collections.singletonList(tp));

        when(kafkaConsumer.poll(100L)).thenReturn(records);

        AtlasKafkaConsumer consumer =new AtlasKafkaConsumer(NotificationType.HOOK, kafkaConsumer ,false, 100L);

        try {
            List<AtlasKafkaMessage<HookNotification>> messageList = consumer.receive();

            assertTrue(messageList.size() > 0);

            HookNotification consumedMessage  = messageList.get(0).getMessage();

            fail("Expected VersionMismatchException!");
        } catch (IncompatibleVersionException e) {
            e.printStackTrace();
        }
  }


    @Test
    public void testCommitIsCalledIfAutoCommitDisabled() {
        TopicPartition     tp       = new TopicPartition(ATLAS_HOOK_TOPIC,0);
        AtlasKafkaConsumer consumer = new AtlasKafkaConsumer(NotificationType.HOOK, kafkaConsumer, false, 100L);

        consumer.commit(tp, 1);

        verify(kafkaConsumer).commitSync(Collections.singletonMap(tp, new OffsetAndMetadata(1)));
    }

    @Test
    public void testCommitIsNotCalledIfAutoCommitEnabled() {
        TopicPartition     tp       = new TopicPartition(ATLAS_HOOK_TOPIC,0);
        AtlasKafkaConsumer consumer = new AtlasKafkaConsumer(NotificationType.HOOK, kafkaConsumer, true , 100L);

        consumer.commit(tp, 1);

        verify(kafkaConsumer, never()).commitSync(Collections.singletonMap(tp, new OffsetAndMetadata(1)));
    }

    private Referenceable getEntity(String traitName) {
        return EntityNotificationTest.getEntity("id", new Struct(traitName, Collections.<String, Object>emptyMap()));
    }

    private void assertMessagesEqual(EntityUpdateRequest message,
                                     HookNotification    consumedMessage,
                                     Referenceable       entity) {
        assertEquals(consumedMessage.getType(), message.getType());
        assertEquals(consumedMessage.getUser(), message.getUser());

        assertTrue(consumedMessage instanceof EntityUpdateRequest);

        EntityUpdateRequest deserializedEntityUpdateRequest = (EntityUpdateRequest) consumedMessage;

        Referenceable deserializedEntity = deserializedEntityUpdateRequest.getEntities().get(0);

        assertEquals(deserializedEntity.getId(), entity.getId());
        assertEquals(deserializedEntity.getTypeName(), entity.getTypeName());
        assertEquals(deserializedEntity.getTraits(), entity.getTraits());
        assertEquals(deserializedEntity.getTrait(TRAIT_NAME), entity.getTrait(TRAIT_NAME));
    }
}
