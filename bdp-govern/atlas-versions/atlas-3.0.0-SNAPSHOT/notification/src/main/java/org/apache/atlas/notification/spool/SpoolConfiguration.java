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
package org.apache.atlas.notification.spool;

import org.apache.commons.configuration.Configuration;

import java.io.File;

public class SpoolConfiguration {
    private static final int    PROP_RETRY_DESTINATION_MS_DEFAULT               = 30000; // Default 30 seconds
    private static final int    PROP_FILE_ROLLOVER_SEC_DEFAULT                  = 60; // 60 secs
    private static final int    PROP_FILE_SPOOL_ARCHIVE_MAX_FILES_COUNT_DEFAULT = 100;
    private static final int    PROP_PAUSE_BEFORE_SEND_MS_DEFAULT               = 60;
    private static final String PROP_FILE_SPOOL_ARCHIVE_DIR_DEFAULT             = "archive";
    private static final String PROP_FILE_SPOOL_LOCAL_DIR_DEFAULT               = "/tmp/spool";
    private static final int    PROP_FILE_MESSAGE_BATCH_SIZE_DEFAULT            = 100;
    private static final String PROP_HIVE_METASTORE_NAME_DEFAULT                = "HiveMetastoreHookImpl";
    private static final String PROPERTY_PREFIX_SPOOL                           = "atlas.hook.spool.";
    public  static final String PROP_FILE_SPOOL_LOCAL_DIR                       = PROPERTY_PREFIX_SPOOL + "dir";
    private static final String PROP_FILE_SPOOL_ARCHIVE_DIR                     = PROPERTY_PREFIX_SPOOL + "archive.dir";
    private static final String PROP_FILE_SPOOL_ARCHIVE_MAX_FILES_COUNT         = PROPERTY_PREFIX_SPOOL + "archive.max.files";
    public  static final String PROP_FILE_SPOOL_FILE_ROLLOVER_SEC               = PROPERTY_PREFIX_SPOOL + "file.rollover.sec";
    public  static final String PROP_FILE_SPOOL_DEST_RETRY_MS                   = PROPERTY_PREFIX_SPOOL + "destination.retry.ms";
    private static final String PROP_MESSAGE_BATCH_SIZE                         = PROPERTY_PREFIX_SPOOL + "destination.message.batchsize";
    public  static final String PROP_FILE_SPOOL_PAUSE_BEFORE_SEND_SEC           = PROPERTY_PREFIX_SPOOL + "pause.before.send.sec";
    private static final String PROP_HIVE_METASTORE_NAME                        = PROPERTY_PREFIX_SPOOL + "hivemetastore.name";

    private final Configuration config;

    private final String messageHandlerName;
    private final int    maxArchivedFilesCount;
    private final int    messageBatchSize;
    private final int    retryDestinationMS;
    private final int    fileRollOverSec;
    private final int    fileSpoolMaxFilesCount;
    private       String spoolDirPath;
    private       String archiveDir;
    private final int    pauseBeforeSendSec;
    private final String hiveMetaStoreName;
    private       String sourceName;
     private      String user;

    public SpoolConfiguration(Configuration cfg, String messageHandlerName) {
        this.config = cfg;
        this.messageHandlerName     = messageHandlerName;
        this.maxArchivedFilesCount  = cfg.getInt(PROP_FILE_SPOOL_ARCHIVE_MAX_FILES_COUNT, PROP_FILE_SPOOL_ARCHIVE_MAX_FILES_COUNT_DEFAULT);
        this.messageBatchSize       = cfg.getInt(PROP_MESSAGE_BATCH_SIZE, PROP_FILE_MESSAGE_BATCH_SIZE_DEFAULT);
        this.retryDestinationMS     = cfg.getInt(PROP_FILE_SPOOL_DEST_RETRY_MS, PROP_RETRY_DESTINATION_MS_DEFAULT);
        this.pauseBeforeSendSec     = cfg.getInt(PROP_FILE_SPOOL_PAUSE_BEFORE_SEND_SEC, PROP_PAUSE_BEFORE_SEND_MS_DEFAULT);
        this.fileRollOverSec        = cfg.getInt(PROP_FILE_SPOOL_FILE_ROLLOVER_SEC, PROP_FILE_ROLLOVER_SEC_DEFAULT) * 1000;
        this.fileSpoolMaxFilesCount = cfg.getInt(PROP_FILE_SPOOL_ARCHIVE_MAX_FILES_COUNT, PROP_FILE_SPOOL_ARCHIVE_MAX_FILES_COUNT_DEFAULT);
        this.spoolDirPath           = cfg.getString(SpoolConfiguration.PROP_FILE_SPOOL_LOCAL_DIR, PROP_FILE_SPOOL_LOCAL_DIR_DEFAULT);
        this.archiveDir             = cfg.getString(PROP_FILE_SPOOL_ARCHIVE_DIR, new File(getSpoolDirPath(), PROP_FILE_SPOOL_ARCHIVE_DIR_DEFAULT).toString());
        this.hiveMetaStoreName      = cfg.getString(PROP_HIVE_METASTORE_NAME, PROP_HIVE_METASTORE_NAME_DEFAULT);
    }

    public void setSource(String source, String user) {
        this.sourceName = source;
        this.user       = user;
    }

    public String getSourceName() {
        return this.sourceName;
    }

    public int getMaxArchiveFiles() {
        return maxArchivedFilesCount;
    }

    public int getRetryDestinationMS() {
        return retryDestinationMS;
    }

    public int getFileRolloverSec() {
        return this.fileRollOverSec;
    }

    public int getFileSpoolMaxFilesCount() {
        return fileSpoolMaxFilesCount;
    }

    public String getSpoolDirPath() {
        return spoolDirPath;
    }

    public File getSpoolDir() {
        return new File(getSpoolDirPath());
    }

    public void setSpoolDir(String absolutePath) {
        this.spoolDirPath = absolutePath;
    }

    public File getArchiveDir() {
        this.archiveDir = config.getString(PROP_FILE_SPOOL_ARCHIVE_DIR, new File(getSpoolDirPath(), PROP_FILE_SPOOL_ARCHIVE_DIR_DEFAULT).toString());
        return new File(this.archiveDir);
    }

    public String getMessageHandlerName() {
        return this.messageHandlerName;
    }

    public int getMessageBatchSize() {
        return messageBatchSize;
    }

    public File getIndexFile() {
        String fileName = SpoolUtils.getIndexFileName(getSourceName(), getMessageHandlerName());

        return new File(getSpoolDir(), fileName);
    }

    public File getIndexDoneFile() {
        String fileName     = SpoolUtils.getIndexFileName(getSourceName(), getMessageHandlerName());
        String fileDoneName = SpoolUtils.getIndexDoneFile(fileName);

        return new File(getSpoolDir(), fileDoneName);
    }

    public File getIndexPublishFile() {
        String fileName     = SpoolUtils.getIndexFileName(getSourceName(), getMessageHandlerName());
        String fileDoneName = SpoolUtils.getIndexPublishFile(fileName);

        return new File(getSpoolDir(), fileDoneName);
    }

    public int getPauseBeforeSendSec() {
        return pauseBeforeSendSec;
    }

    public boolean isHiveMetaStore() {
        return this.sourceName.equals(this.hiveMetaStoreName);
    }

    public String getUser() {
        return this.user;
    }
}
