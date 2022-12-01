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
package org.apache.atlas.notification;

import org.apache.atlas.model.notification.MessageSource;
import org.apache.atlas.notification.entity.EntityMessageDeserializer;
import org.apache.atlas.notification.hook.HookMessageDeserializer;

import java.util.List;

/**
 * Interface to the Atlas notification framework.
 * <p>
 * Use this interface to create consumers and to send messages of a given notification type.
 * <ol>
 *   <li>Atlas sends entity notifications
 *   <li>Hooks send notifications to create/update types/entities. Atlas reads these messages
 * </ol>
 */
public interface NotificationInterface {

    /**
     * Prefix for Atlas notification related configuration properties.
     */
    String PROPERTY_PREFIX = "atlas.notification";

    /**
     * Atlas notification types.
     */
    enum NotificationType {
        // Notifications from the Atlas integration hooks.
        HOOK(new HookMessageDeserializer()),

        // Notifications to entity change consumers.
        ENTITIES(new EntityMessageDeserializer());

        private final AtlasNotificationMessageDeserializer deserializer;

        NotificationType(AtlasNotificationMessageDeserializer deserializer) {
            this.deserializer = deserializer;
        }

        public AtlasNotificationMessageDeserializer getDeserializer() {
            return deserializer;
        }
    }

    /**
     *
     * @param source: Name of the source
     * @param failedMessagesLogger: Logger for failed messages
     * @return
     */
    void init(String source, Object failedMessagesLogger);

    /**
     *
     * @param user Name of the user under which the processes is running
     */
    void setCurrentUser(String user);

    /**
     * Create notification consumers for the given notification type.
     *
     * @param notificationType  the notification type (i.e. HOOK, ENTITIES)
     * @param numConsumers      the number of consumers to create
     * @param <T>               the type of the notifications
     *
     * @return the list of created consumers
     */
    <T> List<NotificationConsumer<T>> createConsumers(NotificationType notificationType, int numConsumers);

    /**
     * Send the given messages.
     *
     * @param type      the message type
     * @param messages  the messages to send
     * @param <T>       the message type
     *
     * @throws NotificationException if an error occurs while sending
     */
    <T> void send(NotificationType type, T... messages) throws NotificationException;

    /**
     * Send the given messages.
     *
     * @param type      the message type
     * @param messages  the list of messages to send
     * @param <T>       the message type
     *
     * @throws NotificationException if an error occurs while sending
     */
    <T> void send(NotificationType type, List<T> messages) throws NotificationException;

    /**
     * Shutdown any notification producers and consumers associated with this interface instance.
     */
    <T> void send(NotificationType type, List<T> messages, MessageSource source) throws NotificationException;

    void close();

    /**
     *  Check if underlying notification mechanism is ready for use.
     *
     * @param type tye message type
     * @return true if available, false otherwise
     *
     */
    boolean isReady(NotificationType type);
}
