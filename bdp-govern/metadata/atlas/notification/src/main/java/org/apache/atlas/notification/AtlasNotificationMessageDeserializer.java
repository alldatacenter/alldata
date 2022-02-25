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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.model.notification.AtlasNotificationBaseMessage;
import org.apache.atlas.model.notification.AtlasNotificationBaseMessage.CompressionKind;
import org.apache.atlas.model.notification.AtlasNotificationMessage;
import org.apache.atlas.model.notification.AtlasNotificationStringMessage;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.model.notification.MessageVersion;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.atlas.AtlasConfiguration.NOTIFICATION_SPLIT_MESSAGE_BUFFER_PURGE_INTERVAL_SECONDS;
import static org.apache.atlas.AtlasConfiguration.NOTIFICATION_SPLIT_MESSAGE_SEGMENTS_WAIT_TIME_SECONDS;

/**
 * Deserializer that works with notification messages.  The version of each deserialized message is checked against an
 * expected version.
 */
public abstract class AtlasNotificationMessageDeserializer<T> implements MessageDeserializer<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasNotificationMessageDeserializer.class);


    public static final String VERSION_MISMATCH_MSG =
        "Notification message version mismatch. Expected %s but recieved %s. Message %s";

    private final TypeReference<T> messageType;
    private final TypeReference<AtlasNotificationMessage<T>> notificationMessageType;
    private final MessageVersion                             expectedVersion;
    private final Logger                                     notificationLogger;


    private final Map<String, SplitMessageAggregator> splitMsgBuffer = new HashMap<>();
    private final long                                splitMessageBufferPurgeIntervalMs;
    private final long                                splitMessageSegmentsWaitTimeMs;
    private long                                      splitMessagesLastPurgeTime    = System.currentTimeMillis();
    private final AtomicLong                          messageCountTotal             = new AtomicLong(0);
    private final AtomicLong                          messageCountSinceLastInterval = new AtomicLong(0);
    private long                                      msgCreated;
    private boolean                                   spooled;
    // ----- Constructors ----------------------------------------------------

    /**
     * Create a notification message deserializer.
     *
     * @param expectedVersion         the expected message version
     * @param notificationLogger      logger for message version mismatch
     */
    public AtlasNotificationMessageDeserializer(TypeReference<T> messageType,
                                                TypeReference<AtlasNotificationMessage<T>> notificationMessageType,
                                                MessageVersion expectedVersion, Logger notificationLogger) {
        this(messageType, notificationMessageType, expectedVersion, notificationLogger,
             NOTIFICATION_SPLIT_MESSAGE_SEGMENTS_WAIT_TIME_SECONDS.getLong() * 1000,
             NOTIFICATION_SPLIT_MESSAGE_BUFFER_PURGE_INTERVAL_SECONDS.getLong() * 1000);
    }

    public AtlasNotificationMessageDeserializer(TypeReference<T> messageType,
                                                TypeReference<AtlasNotificationMessage<T>> notificationMessageType,
                                                MessageVersion expectedVersion,
                                                Logger notificationLogger,
                                                long splitMessageSegmentsWaitTimeMs,
                                                long splitMessageBufferPurgeIntervalMs) {
        this.messageType                       = messageType;
        this.notificationMessageType           = notificationMessageType;
        this.expectedVersion                   = expectedVersion;
        this.notificationLogger                = notificationLogger;
        this.splitMessageSegmentsWaitTimeMs    = splitMessageSegmentsWaitTimeMs;
        this.splitMessageBufferPurgeIntervalMs = splitMessageBufferPurgeIntervalMs;
    }

    public TypeReference<T> getMessageType() {
        return messageType;
    }

    public TypeReference<AtlasNotificationMessage<T>> getNotificationMessageType() {
        return notificationMessageType;
    }

    // ----- MessageDeserializer ---------------------------------------------
    public long getMsgCreated() {
        return this.msgCreated;
    }

    public boolean getSpooled() {
        return this.spooled;
    }

    @Override
    public T deserialize(String messageJson) {
        final T ret;

        messageCountTotal.incrementAndGet();
        messageCountSinceLastInterval.incrementAndGet();
        this.msgCreated = 0;
        this.spooled = false;

        AtlasNotificationBaseMessage msg = AtlasType.fromV1Json(messageJson, AtlasNotificationMessage.class);

        if (msg == null || msg.getVersion() == null) { // older style messages not wrapped with AtlasNotificationMessage
            ret = AtlasType.fromV1Json(messageJson, messageType);
        } else  {
            this.msgCreated = ((AtlasNotificationMessage) msg).getMsgCreationTime();
            this.spooled = ((AtlasNotificationMessage) msg).getSpooled();

            String msgJson = messageJson;

            if (msg.getMsgSplitCount() > 1) { // multi-part message
                AtlasNotificationStringMessage splitMsg = AtlasType.fromV1Json(msgJson, AtlasNotificationStringMessage.class);

                checkVersion(splitMsg, msgJson);

                String msgId = splitMsg.getMsgId();

                if (StringUtils.isEmpty(msgId)) {
                    LOG.error("Received multi-part message with no message ID. Ignoring message");

                    msg = null;
                } else {
                    final int splitIdx   = splitMsg.getMsgSplitIdx();
                    final int splitCount = splitMsg.getMsgSplitCount();

                    final SplitMessageAggregator splitMsgs;

                    if (splitIdx == 0) {
                        splitMsgs = new SplitMessageAggregator(splitMsg);

                        splitMsgBuffer.put(splitMsgs.getMsgId(), splitMsgs);
                    } else {
                        splitMsgs = splitMsgBuffer.get(msgId);
                    }

                    if (splitMsgs == null) {
                        LOG.error("Received msgID={}: {} of {}, but first message didn't arrive. Ignoring message", msgId, splitIdx + 1, splitCount);

                        msg = null;
                    } else if (splitMsgs.getTotalSplitCount() <= splitIdx) {
                        LOG.error("Received msgID={}: {} of {} - out of bounds. Ignoring message", msgId, splitIdx + 1, splitCount);

                        msg = null;
                    } else {
                        LOG.info("Received msgID={}: {} of {}", msgId, splitIdx + 1, splitCount);

                        boolean isReady = splitMsgs.add(splitMsg);

                        if (isReady) { // last message
                            splitMsgBuffer.remove(msgId);

                            boolean isValidMessage = true;

                            StringBuilder sb = new StringBuilder();

                            for (int i = 0; i < splitMsgs.getTotalSplitCount(); i++) {
                                splitMsg = splitMsgs.get(i);

                                if (splitMsg == null) {
                                    LOG.warn("MsgID={}: message {} of {} is missing. Ignoring message", msgId, i + 1, splitCount);

                                    isValidMessage = false;

                                    break;
                                }

                                sb.append(splitMsg.getMessage());
                            }

                            if (isValidMessage) {
                                msgJson = sb.toString();

                                if (CompressionKind.GZIP.equals(splitMsg.getMsgCompressionKind())) {
                                    byte[] encodedBytes = AtlasNotificationBaseMessage.getBytesUtf8(msgJson);
                                    byte[] bytes        = AtlasNotificationBaseMessage.decodeBase64AndGzipUncompress(encodedBytes);

                                    msgJson = AtlasNotificationBaseMessage.getStringUtf8(bytes);

                                    LOG.info("Received msgID={}: splitCount={}, compressed={} bytes, uncompressed={} bytes", msgId, splitCount, encodedBytes.length, bytes.length);
                                } else {
                                    byte[] encodedBytes = AtlasNotificationBaseMessage.getBytesUtf8(msgJson);
                                    byte[] bytes        = AtlasNotificationBaseMessage.decodeBase64(encodedBytes);

                                    msgJson = AtlasNotificationBaseMessage.getStringUtf8(bytes);

                                    LOG.info("Received msgID={}: splitCount={}, length={} bytes", msgId, splitCount, bytes.length);
                                }

                                msg = AtlasType.fromV1Json(msgJson, AtlasNotificationBaseMessage.class);
                            } else {
                                msg = null;
                            }
                        } else { // more messages to arrive
                            msg = null;
                        }
                    }
                }
            }

            if (msg != null) {
                if (CompressionKind.GZIP.equals(msg.getMsgCompressionKind())) {
                    AtlasNotificationStringMessage compressedMsg = AtlasType.fromV1Json(msgJson, AtlasNotificationStringMessage.class);

                    byte[] encodedBytes = AtlasNotificationBaseMessage.getBytesUtf8(compressedMsg.getMessage());
                    byte[] bytes        = AtlasNotificationBaseMessage.decodeBase64AndGzipUncompress(encodedBytes);

                    msgJson = AtlasNotificationBaseMessage.getStringUtf8(bytes);

                    LOG.info("Received msgID={}: compressed={} bytes, uncompressed={} bytes", compressedMsg.getMsgId(), encodedBytes.length, bytes.length);
                }

                AtlasNotificationMessage<T> atlasNotificationMessage = AtlasType.fromV1Json(msgJson, notificationMessageType);

                checkVersion(atlasNotificationMessage, msgJson);

                ret = atlasNotificationMessage.getMessage();
            } else {
                ret = null;
            }
        }


        long now                = System.currentTimeMillis();
        long timeSinceLastPurge = now - splitMessagesLastPurgeTime;

        if(timeSinceLastPurge >= splitMessageBufferPurgeIntervalMs) {
            purgeStaleMessages(splitMsgBuffer, now, splitMessageSegmentsWaitTimeMs);

            LOG.info("Notification processing stats: total={}, sinceLastStatsReport={}", messageCountTotal.get(), messageCountSinceLastInterval.getAndSet(0));

            splitMessagesLastPurgeTime = now;
        }

        return ret;
    }

    @VisibleForTesting
    static void purgeStaleMessages(Map<String, SplitMessageAggregator> splitMsgBuffer, long now, long maxWaitTime) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> purgeStaleMessages(bufferedMessageCount=" + splitMsgBuffer.size() + ")");
        }

        List<SplitMessageAggregator> evictionList = null;

        for (SplitMessageAggregator aggregrator : splitMsgBuffer.values()) {
            long waitTime = now - aggregrator.getFirstSplitTimestamp();

            if (waitTime < maxWaitTime) {
                continue;
            }

            if(evictionList == null) {
                evictionList = new ArrayList<>();
            }

             evictionList.add(aggregrator);
        }

        if(evictionList != null) {
            for (SplitMessageAggregator aggregrator : evictionList) {
                LOG.error("evicting notification msgID={}, totalSplitCount={}, receivedSplitCount={}", aggregrator.getMsgId(), aggregrator.getTotalSplitCount(), aggregrator.getReceivedSplitCount());
                splitMsgBuffer.remove(aggregrator.getMsgId());
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== purgeStaleMessages(bufferedMessageCount=" + splitMsgBuffer.size() + ")");
        }
    }

    // ----- helper methods --------------------------------------------------

    /**
     * Check the message version against the expected version.
     *
     * @param notificationMessage the notification message
     * @param messageJson         the notification message json
     *
     * @throws IncompatibleVersionException  if the message version is incompatable with the expected version
     */
    protected void checkVersion(AtlasNotificationBaseMessage notificationMessage, String messageJson) {
        int comp = notificationMessage.compareVersion(expectedVersion);

        // message has newer version
        if (comp > 0) {
            String msg = String.format(VERSION_MISMATCH_MSG, expectedVersion, notificationMessage.getVersion(), messageJson);

            notificationLogger.error(msg);

            throw new IncompatibleVersionException(msg);
        }

        // message has older version
        if (comp < 0) {
            notificationLogger.info(String.format(VERSION_MISMATCH_MSG, expectedVersion, notificationMessage.getVersion(), messageJson));
        }
    }
}
