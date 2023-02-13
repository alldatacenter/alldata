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

package org.apache.inlong.sdk.sort.fetcher.pulsar;

import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Messages;
import org.apache.pulsar.client.api.PulsarClientException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wrapper of pulsar consumer.
 */
public class PulsarConsumer {

    private final ConcurrentHashMap<String, Tuple2<InLongTopic, MessageId>> offsetCache = new ConcurrentHashMap<>();
    private final Consumer<byte[]> consumer;
    private long stopTime = -1;

    public PulsarConsumer(Consumer<byte[]> consumer) {
        this.consumer = consumer;
    }

    public void close() throws PulsarClientException {
        this.consumer.close();
        this.offsetCache.clear();
    }

    public void pause() {
        this.consumer.pause();
    }

    public void resume() {
        this.consumer.resume();
    }

    public Messages<byte[]> batchReceive() throws PulsarClientException {
        return this.consumer.batchReceive();
    }

    public CompletableFuture<Void> acknowledgeAsync(MessageId messageId) {
        return this.consumer.acknowledgeAsync(messageId);
    }

    public long getStopTime() {
        return stopTime;
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }

    public InLongTopic getTopic(String msgOffset) {
        Tuple2<InLongTopic, MessageId> tuple = offsetCache.get(msgOffset);
        return tuple == null ? null : tuple.getF0();
    }

    public MessageId getMessageId(String msgOffset) {
        Tuple2<InLongTopic, MessageId> tuple = offsetCache.get(msgOffset);
        return tuple == null ? null : tuple.getF1();
    }

    public boolean remove(String offsetKey) {
        return offsetCache.remove(offsetKey) != null;
    }

    public void put(String offsetKey, InLongTopic topic, MessageId messageId) {
        offsetCache.put(offsetKey, new Tuple2<>(topic, messageId));
    }

    public boolean isEmpty() {
        return offsetCache.isEmpty();
    }

    public boolean isConnected() {
        return consumer.isConnected();
    }
}
