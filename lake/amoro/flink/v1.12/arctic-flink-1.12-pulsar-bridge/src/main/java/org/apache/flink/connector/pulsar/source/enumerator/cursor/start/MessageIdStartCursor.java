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

package org.apache.flink.connector.pulsar.source.enumerator.cursor.start;

import org.apache.flink.connector.pulsar.source.enumerator.cursor.CursorPosition;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StartCursor;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.MessageId;

/** This cursor would leave pulsar start consuming from a specific message id. */
public class MessageIdStartCursor implements StartCursor {
    private static final long serialVersionUID = -8057345435887170111L;

    private final MessageId messageId;
    private final boolean inclusive;

    /**
     * The default {@code inclusive} behavior should be controlled in {@link
     * ConsumerBuilder#startMessageIdInclusive}. But pulsar has a bug and doesn't support this
     * feature currently. We have to use admin API to reset the cursor instead.
     *
     * @param messageId The message id for start position.
     * @param inclusive Whether we include the start message id in the consuming result. This works
     *     only if we provide a specified message id instead of {@link MessageId#earliest} or {@link
     *     MessageId#latest}.
     */
    public MessageIdStartCursor(MessageId messageId, boolean inclusive) {
        this.messageId = messageId;
        this.inclusive = inclusive;
    }

    @Override
    public CursorPosition position(String topic, int partitionId) {
        return new CursorPosition(messageId, inclusive);
    }
}
