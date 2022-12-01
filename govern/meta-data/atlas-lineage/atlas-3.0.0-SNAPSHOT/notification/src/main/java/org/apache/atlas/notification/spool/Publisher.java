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
import org.apache.atlas.notification.AbstractNotification;
import org.apache.atlas.notification.NotificationException;
import org.apache.atlas.notification.NotificationInterface;
import org.apache.atlas.notification.spool.models.IndexRecord;
import org.apache.atlas.notification.spool.utils.local.FileLockedReadWrite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.List;

public class Publisher implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Publisher.class);

    private final SpoolConfiguration   configuration;
    private final IndexManagement      indexManagement;
    private final AbstractNotification notificationHandler;
    private final String               notificationHandlerName;
    private final int                  retryDestinationMS;
    private final int                  messageBatchSize;
    private       String               source;
    private       boolean              isDrain;
    private       boolean              isDestDown;

    public Publisher(SpoolConfiguration configuration, IndexManagement indexManagement, AbstractNotification notificationHandler) {
        this.configuration           = configuration;
        this.indexManagement         = indexManagement;
        this.notificationHandler     = notificationHandler;
        this.notificationHandlerName = notificationHandler.getClass().getSimpleName();
        this.retryDestinationMS      = configuration.getRetryDestinationMS();
        this.messageBatchSize        = configuration.getMessageBatchSize();
    }

    public void run() {
        this.source = configuration.getSourceName();

        LOG.info("Publisher.run(source={}): starting publisher {}", this.source, notificationHandlerName);

        try {
            IndexRecord record = null;

            while (true) {
                checkAndWaitIfDestinationDown();
                if (this.isDrain) {
                    break;
                }

                if (isDestDown) {
                    continue;
                }

                record = fetchNext(record);
                if (record != null && processAndDispatch(record)) {
                    indexManagement.removeAsDone(record);

                    record = null;
                } else {
                    indexManagement.rolloverSpoolFileIfNeeded();
                }
            }
        } catch (InterruptedException e) {
            LOG.error("Publisher.run(source={}): {}: Publisher: Shutdown might be in progress!", this.source, notificationHandlerName);
        } catch (Exception e) {
            LOG.error("Publisher.run(source={}): {}: Publisher: Exception in destination writing!", this.source, notificationHandlerName, e);
        }

        LOG.info("Publisher.run(source={}): publisher {} exited!", this.source, notificationHandlerName);
    }

    public void setDestinationDown() {
        this.isDestDown = true;

        this.indexManagement.updateFailedAttempt();
    }

    public void setDrain() {
        this.isDrain = true;
    }

    public boolean isDestinationDown() {
        return isDestDown;
    }

    private void checkAndWaitIfDestinationDown() throws InterruptedException {
        isDestDown = !notificationHandler.isReady(NotificationInterface.NotificationType.HOOK);
        if (isDestDown) {
            LOG.info("Publisher.waitIfDestinationDown(source={}): {}: Destination is down. Sleeping for: {} ms. Queue: {} items",
                     this.source, notificationHandlerName, retryDestinationMS, indexManagement.getQueueSize());

            Thread.sleep(retryDestinationMS);
        }
    }

    private IndexRecord fetchNext(IndexRecord record) {
        if (record == null) {
            try {
                record = indexManagement.next();
            } catch (Exception e) {
                LOG.error("Publisher.fetchNext(source={}): failed!. publisher={}", this.source, notificationHandlerName, e);
            }
        }

        return record;
    }

    @VisibleForTesting
    boolean processAndDispatch(IndexRecord record) throws IOException {
        boolean ret = true;

        if (SpoolUtils.fileExists(record)) {
            FileLockedReadWrite fileLockedRead = new FileLockedReadWrite(source);

            try {
                DataInput    dataInput       = fileLockedRead.getInput(new File(record.getPath()));
                int          lineInSpoolFile = 0;
                List<String> messages        = new ArrayList<>();

                for (String message = dataInput.readLine(); message != null; message = dataInput.readLine()) {
                    lineInSpoolFile++;

                    if (lineInSpoolFile < record.getLine()) {
                        continue;
                    }

                    messages.add(message);

                    if (messages.size() == messageBatchSize) {
                        dispatch(record, lineInSpoolFile, messages);
                    }
                }

                dispatch(record, lineInSpoolFile, messages);

                LOG.info("Publisher.processAndDispatch(source={}): consumer={}: done reading file {}", this.source, notificationHandlerName, record.getPath());

                ret = true;
            } catch (OverlappingFileLockException ex) {
                LOG.error("Publisher.processAndDispatch(source={}): consumer={}: some other process has locked this file {}", this.source, notificationHandlerName, record.getPath(), ex);
                ret = false;
            } catch (FileNotFoundException ex) {
                LOG.error("Publisher.processAndDispatch(source={}): consumer={}: file not found {}", this.source, notificationHandlerName, record.getPath(), ex);
                ret = true;
            } catch (Exception ex) {
                LOG.error("Publisher.processAndDispatch(source={}): consumer={}: failed for file {}", this.source, notificationHandlerName, record.getPath(), ex);
                ret = false;
            } finally {
                fileLockedRead.close();
            }
        } else {
            LOG.error("Publisher.processAndDispatch(source={}): publisher={}: file '{}' not found!", this.source, notificationHandlerName, record.getPath());

            ret = true;
        }

        return ret;
    }

    private void dispatch(IndexRecord record, int lineInSpoolFile, List<String> messages) throws Exception {
        if (notificationHandler == null || messages == null || messages.size() == 0) {
            LOG.error("Publisher.dispatch(source={}): consumer={}: error sending logs", this.source, notificationHandlerName);
        } else {
            dispatch(record.getPath(), messages);

            record.setCurrentLine(lineInSpoolFile);
            indexManagement.update(record);
            isDestDown = false;
        }
    }

    private void dispatch(String filePath, List<String> messages) throws Exception {
        try {
            pauseBeforeSend();

            notificationHandler.sendInternal(NotificationInterface.NotificationType.HOOK, messages);

            if (isDestDown) {
                LOG.info("Publisher.dispatch(source={}): consumer={}: destination is now up. file={}", this.source, notificationHandlerName, filePath);
            }
        } catch (Exception exception) {
            setDestinationDown();

            LOG.error("Publisher.dispatch(source={}): consumer={}: error while sending logs to consumer", this.source, notificationHandlerName, exception);

            throw new NotificationException(exception, String.format("%s: %s: Publisher: Destination down!", this.source, notificationHandlerName));
        } finally {
            messages.clear();
        }
    }

    /**
     * Reason for pauseBeforeSend:
     *  - EntityCorrelation is needed to be able to stitch lineage to the correct entity.
     *  - Background: When messages are added to Kafka queue directly, the ordering is incidentally guaranteed, where
     *     messages from lineage producing hooks reach immediately after messages from entities producing hooks.
     *  - When Spooled messages are posted onto Kafka, this order cannot be guaranteed. The entity correlation logic within Atlas
     *     can attach lineage to the correct entity, provided that the entity participating in the lineage is already present.
     *
     *   This logic of entity correlation works well for majority of cases except where lineage entities are created before regular entities.
     *   In this case, shell entities get created in the absence of real entities. Problem is that there is 1 shell entity for any number of references.
     *   Circumventing this limitation is not easy.
     *
     *   The pauseBeforeSend forces the situation where HiveMetaStore generated messages reach Kafka before lineage-producing hooks.
     *
     * @throws InterruptedException
     */
    private void pauseBeforeSend() throws InterruptedException {
        if (!configuration.isHiveMetaStore()) {
            int waitMs = configuration.getPauseBeforeSendSec() * 1000;
            LOG.info("Waiting before dispatch: {}", waitMs);
            Thread.sleep(waitMs);
        }
    }
}
