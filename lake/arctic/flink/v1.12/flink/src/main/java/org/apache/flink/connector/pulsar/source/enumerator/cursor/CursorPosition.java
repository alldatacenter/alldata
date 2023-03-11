/*
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

package org.apache.flink.connector.pulsar.source.enumerator.cursor;

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.connector.pulsar.source.enumerator.PulsarSourceEnumerator;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.impl.ChunkMessageIdImpl;

import java.io.Serializable;
import java.util.List;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * The class for defining the start or stop position. We only expose the constructor for end user.
 */
@PublicEvolving
public final class CursorPosition implements Serializable {
    private static final long serialVersionUID = -802405183307684549L;

    private final Type type;

    private final MessageId messageId;
    private final boolean include;

    private final Long timestamp;

    /**
     * Start consuming from the given message id. The message id couldn't be the {@code
     * MultipleMessageIdImpl}.
     *
     * @param include Whether the cosponsored position will be (in/ex)cluded in the consuming
     *     result.
     */
    public CursorPosition(MessageId messageId, boolean include) {
        checkNotNull(messageId, "Message id couldn't be null.");

        this.type = Type.MESSAGE_ID;
        this.messageId = messageId;
        this.include = include;
        this.timestamp = null;
    }

    /**
     * Start consuming from the given timestamp position. The cosponsored position will be included
     * in the consuming result.
     */
    public CursorPosition(Long timestamp) {
        checkNotNull(timestamp, "Timestamp couldn't be null.");

        this.type = Type.TIMESTAMP;
        this.messageId = null;
        this.include = true;
        this.timestamp = timestamp;
    }

    /** This method is used to create the initial position in {@link PulsarSourceEnumerator}. */
    @Internal
    public boolean createInitialPosition(
            PulsarAdmin pulsarAdmin, String topicName, String subscriptionName)
            throws PulsarAdminException {
        List<String> subscriptions = pulsarAdmin.topics().getSubscriptions(topicName);

        if (!subscriptions.contains(subscriptionName)) {
            pulsarAdmin
                    .topics()
                    .createSubscription(topicName, subscriptionName, MessageId.earliest);

            // Reset cursor to desired position.
            MessageId initialPosition = getMessageId(pulsarAdmin, topicName);
            pulsarAdmin
                    .topics()
                    .resetCursor(topicName, subscriptionName, initialPosition, !include);

            return true;
        }

        return false;
    }

    /**
     * This method is used to reset the consuming position in {@code
     * PulsarPartitionSplitReaderBase}.
     */
    @Internal
    public void seekPosition(PulsarAdmin pulsarAdmin, String topicName, String subscriptionName)
            throws PulsarAdminException {
        if (!createInitialPosition(pulsarAdmin, topicName, subscriptionName)) {
            // Reset cursor to desired position.
            MessageId initialPosition = getMessageId(pulsarAdmin, topicName);
            pulsarAdmin
                    .topics()
                    .resetCursor(topicName, subscriptionName, initialPosition, !include);
        }
    }

    private MessageId getMessageId(PulsarAdmin pulsarAdmin, String topicName)
            throws PulsarAdminException {
        if (type == Type.TIMESTAMP) {
            return pulsarAdmin.topics().getMessageIdByTimestamp(topicName, timestamp);
        } else if (messageId instanceof ChunkMessageIdImpl) {
            return ((ChunkMessageIdImpl) messageId).getFirstChunkMessageId();
        } else {
            return messageId;
        }
    }

    @Override
    public String toString() {
        if (type == Type.TIMESTAMP) {
            return "timestamp: " + timestamp;
        } else {
            return "message id: " + messageId + " include: " + include;
        }
    }

    /**
     * The position type for reader to choose whether timestamp or message id as the start position.
     */
    @Internal
    public enum Type {
        TIMESTAMP,

        MESSAGE_ID
    }
}
