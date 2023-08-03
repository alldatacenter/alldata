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

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.AtlasException;
import org.apache.atlas.model.notification.AtlasNotificationBaseMessage;
import org.apache.atlas.model.notification.AtlasNotificationMessage;
import org.apache.atlas.model.notification.AtlasNotificationStringMessage;
import org.apache.atlas.model.notification.AtlasNotificationBaseMessage.CompressionKind;
import org.apache.atlas.model.notification.MessageSource;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.model.notification.MessageVersion;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.atlas.model.notification.AtlasNotificationBaseMessage.MESSAGE_COMPRESSION_ENABLED;
import static org.apache.atlas.model.notification.AtlasNotificationBaseMessage.MESSAGE_MAX_LENGTH_BYTES;

/**
 * Abstract notification interface implementation.
 */
public abstract class AbstractNotification implements NotificationInterface {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNotification.class);

    private static String        msgIdPrefix = UUID.randomUUID().toString();
    private static AtomicInteger msgIdSuffix = new AtomicInteger(0);

    /**
     * The current expected version for notification messages.
     */
    public static final MessageVersion CURRENT_MESSAGE_VERSION = new MessageVersion("1.0.0");

    public static final int MAX_BYTES_PER_CHAR = 4;  // each char can encode upto 4 bytes in UTF-8

    /**
     * IP address of the host in which this process has started
     */
    private static String localHostAddress = "";

    /**
     *
     */
    private static String currentUser = "";

    // ----- Constructors ----------------------------------------------------

    public AbstractNotification(Configuration applicationProperties) throws AtlasException {
    }

    @VisibleForTesting
    protected AbstractNotification() {
    }

    @Override
    public void init(String source, Object failedMessagesLogger) {
    }

    // ----- NotificationInterface -------------------------------------------

    @Override
    public <T> void send(NotificationType type, List<T> messages) throws NotificationException {
        send(type, messages, new MessageSource());
    }

    @Override
    public <T> void send(NotificationType type, List<T> messages, MessageSource source) throws NotificationException {
        List<String> strMessages = new ArrayList<>(messages.size());

        for (int index = 0; index < messages.size(); index++) {
            createNotificationMessages(messages.get(index), strMessages, source);
        }

        sendInternal(type, strMessages);
    }

    @Override
    public <T> void send(NotificationType type, T... messages) throws NotificationException {
        send(type, Arrays.asList(messages));
    }

    @Override
    public void setCurrentUser(String user) {
        currentUser = user;
    }

    // ----- AbstractNotification --------------------------------------------
    /**
     * Send the given messages.
     *
     * @param type      the message type
     * @param messages  the array of messages to send
     *
     * @throws NotificationException if an error occurs while sending
     */
    public abstract void sendInternal(NotificationType type, List<String> messages) throws NotificationException;


    // ----- utility methods -------------------------------------------------

    public static String getMessageJson(Object message) {
        AtlasNotificationMessage<?> notificationMsg = new AtlasNotificationMessage<>(CURRENT_MESSAGE_VERSION, message);

        return AtlasType.toV1Json(notificationMsg);
    }

    private static String getHostAddress() {
        if (StringUtils.isEmpty(localHostAddress)) {
            try {
                localHostAddress =  Inet4Address.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                LOG.warn("failed to get local host address", e);

                localHostAddress =  "";
            }
        }

        return localHostAddress;
    }

    private static String getCurrentUser() {
        return currentUser;
    }

    /**
     * Get the notification message JSON from the given object.
     *
     * @param message  the message in object form
     *
     * @param source
     * @return the message as a JSON string
     */
    public static void createNotificationMessages(Object message, List<String> msgJsonList, MessageSource source) {
        AtlasNotificationMessage<?> notificationMsg = new AtlasNotificationMessage<>(CURRENT_MESSAGE_VERSION, message, getHostAddress(), getCurrentUser(), false, source);
        String                      msgJson         = AtlasType.toV1Json(notificationMsg);

        boolean msgLengthExceedsLimit = (msgJson.length() * MAX_BYTES_PER_CHAR) > MESSAGE_MAX_LENGTH_BYTES;

        if (msgLengthExceedsLimit) { // get utf-8 bytes for msgJson and check for length limit again
            byte[] msgBytes = AtlasNotificationBaseMessage.getBytesUtf8(msgJson);

            msgLengthExceedsLimit = msgBytes.length > MESSAGE_MAX_LENGTH_BYTES;

            if (msgLengthExceedsLimit) {
                String          msgId           = getNextMessageId();
                CompressionKind compressionKind = CompressionKind.NONE;

                if (MESSAGE_COMPRESSION_ENABLED) {
                    byte[] encodedBytes = AtlasNotificationBaseMessage.gzipCompressAndEncodeBase64(msgBytes);

                    compressionKind = CompressionKind.GZIP;

                    LOG.info("Compressed large message: msgID={}, uncompressed={} bytes, compressed={} bytes", msgId, msgBytes.length, encodedBytes.length);

                    msgLengthExceedsLimit = encodedBytes.length > MESSAGE_MAX_LENGTH_BYTES;

                    if (!msgLengthExceedsLimit) { // no need to split
                        AtlasNotificationStringMessage compressedMsg = new AtlasNotificationStringMessage(encodedBytes, msgId, compressionKind);

                        msgJson  = AtlasType.toV1Json(compressedMsg); // msgJson will not have multi-byte characters here, due to use of encodeBase64() above
                        msgBytes = null; // not used after this point
                    } else { // encodedBytes will be split
                        msgJson  = null; // not used after this point
                        msgBytes = encodedBytes;
                    }
                }

                if (msgLengthExceedsLimit) {
                    // compressed messages are already base64-encoded
                    byte[] encodedBytes = MESSAGE_COMPRESSION_ENABLED ? msgBytes : AtlasNotificationBaseMessage.encodeBase64(msgBytes);
                    int    splitCount   = encodedBytes.length / MESSAGE_MAX_LENGTH_BYTES;

                    if ((encodedBytes.length % MESSAGE_MAX_LENGTH_BYTES) != 0) {
                        splitCount++;
                    }

                    for (int i = 0, offset = 0; i < splitCount; i++) {
                        int length = MESSAGE_MAX_LENGTH_BYTES;

                        if ((offset + length) > encodedBytes.length) {
                            length = encodedBytes.length - offset;
                        }

                        AtlasNotificationStringMessage splitMsg = new AtlasNotificationStringMessage(encodedBytes, offset, length, msgId, compressionKind, i, splitCount);

                        String splitMsgJson = AtlasType.toV1Json(splitMsg);

                        msgJsonList.add(splitMsgJson);

                        offset += length;
                    }

                    LOG.info("Split large message: msgID={}, splitCount={}, length={} bytes", msgId, splitCount, encodedBytes.length);
                }
            }
        }

        if (!msgLengthExceedsLimit) {
            msgJsonList.add(msgJson);
        }
    }

    private static String getNextMessageId() {
        String nextMsgIdPrefix = msgIdPrefix;
        int    nextMsgIdSuffix = msgIdSuffix.getAndIncrement();

        if (nextMsgIdSuffix == Short.MAX_VALUE) { // get a new UUID after 32,767 IDs
            msgIdPrefix = UUID.randomUUID().toString();
            msgIdSuffix = new AtomicInteger(0);
        }

        return nextMsgIdPrefix + "_" + Integer.toString(nextMsgIdSuffix);
    }
}
