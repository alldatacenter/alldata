/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.notification.spool;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.hook.FailedMessagesLogger;
import org.apache.atlas.model.notification.AtlasNotificationMessage;
import org.apache.atlas.notification.AbstractNotification;
import org.apache.atlas.notification.NotificationConsumer;
import org.apache.atlas.type.AtlasType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutput;
import java.util.List;

public class Spooler extends AbstractNotification {
    private static final Logger LOG = LoggerFactory.getLogger(Spooler.class);

    private final SpoolConfiguration   configuration;
    private final IndexManagement      indexManagement;
    private       FailedMessagesLogger failedMessagesLogger;
    private       boolean              isDrain;

    public Spooler(SpoolConfiguration configuration, IndexManagement indexManagement) {
        this.configuration   = configuration;
        this.indexManagement = indexManagement;
    }

    public void setFailedMessagesLogger(FailedMessagesLogger failedMessagesLogger) {
        this.failedMessagesLogger = failedMessagesLogger;
    }

    public void setDrain() {
        this.isDrain = true;
    }

    @Override
    public <T> List<NotificationConsumer<T>> createConsumers(org.apache.atlas.notification.NotificationInterface.NotificationType notificationType, int numConsumers) {
        return null;
    }

    @Override
    public void sendInternal(NotificationType type, List<String> messages) {
        for (int i = 0; i < messages.size(); i++) {
            AtlasNotificationMessage e = AtlasType.fromV1Json(messages.get(i), AtlasNotificationMessage.class);
            e.setSpooled(true);

            messages.set(i, AtlasType.toV1Json(e));
        }

        boolean ret = write(messages);
        if (failedMessagesLogger != null && !ret) {
            writeToFailedMessages(messages);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isReady(NotificationType type) {
        return true;
    }

    @VisibleForTesting
    boolean write(List<String> messages) {
        final boolean ret;

        try {
            if (!getDrain()) {
                indexManagement.setSpoolWriteInProgress();

                ret = writeInternal(messages);
            } else {
                LOG.error("Spooler.write(source={}): called after stop is called! Write will not be performed!", configuration.getSourceName(), messages);

                ret = false;
            }
        } finally {
            indexManagement.resetSpoolWriteInProgress();
        }

        return ret;
    }

    private void writeToFailedMessages(List<String> messages) {
        if (failedMessagesLogger != null) {
            for (String message : messages) {
                failedMessagesLogger.log(message);
            }
        }
    }

    private boolean writeInternal(List<String> messages) {
        boolean ret = false;

        try {
            byte[]     lineSeparatorBytes = SpoolUtils.getLineSeparator().getBytes(SpoolUtils.DEFAULT_CHAR_SET);
            DataOutput pw                 = indexManagement.getSpoolWriter();

            for (String message : messages) {
                pw.write(message.getBytes(SpoolUtils.DEFAULT_CHAR_SET));
                pw.write(lineSeparatorBytes);
            }

            indexManagement.flushSpoolWriter();

            ret = true;
        } catch (Exception exception) {
            LOG.error("Spooler.writeInternal(source={}): error writing to file. messages={}", configuration.getSourceName(), messages, exception);

            ret = false;
        }

        return ret;
    }

    private boolean getDrain() {
        return this.isDrain;
    }
}
