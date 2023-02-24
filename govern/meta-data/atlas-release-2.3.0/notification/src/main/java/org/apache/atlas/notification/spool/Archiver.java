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

import org.apache.atlas.notification.spool.models.IndexRecord;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Archiver {
    private static final Logger LOG = LoggerFactory.getLogger(Archiver.class);

    private final String source;
    private final File   indexDoneFile;
    private final File   archiveFolder;
    private final int    maxArchiveFiles;

    public Archiver(String source, File indexDoneFile, File archiveFolder, int maxArchiveFiles) {
        this.source          = source;
        this.indexDoneFile   = indexDoneFile;
        this.archiveFolder   = archiveFolder;
        this.maxArchiveFiles = maxArchiveFiles;
    }

    public void archive(IndexRecord indexRecord) {
        moveToArchiveDir(indexRecord);

        removeOldFiles();
    }

    private void moveToArchiveDir(IndexRecord indexRecord) {
        File spoolFile = null;
        File archiveFile = null;

        try {
            spoolFile   = new File(indexRecord.getPath());
            archiveFile = new File(archiveFolder, spoolFile.getName());

            LOG.info("{}: moving spoolFile={} to archiveFile={}", source, spoolFile, archiveFile);

            FileUtils.moveFile(spoolFile, archiveFile);
        } catch (FileNotFoundException excp) {
            LOG.warn("{}: failed while moving spoolFile={} to archiveFile={}", source, spoolFile, archiveFile, excp);
        } catch (IOException excp) {
            LOG.error("{}: failed while moving spoolFile={} to archiveFile={}", source, spoolFile, archiveFile, excp);
        }
    }

    private void removeOldFiles() {
        try {
            File[] logFiles      = archiveFolder == null ? null : archiveFolder.listFiles(pathname -> StringUtils.endsWithIgnoreCase(pathname.getName(), SpoolUtils.FILE_EXT_LOG));
            int    filesToDelete = logFiles == null ? 0 : logFiles.length - maxArchiveFiles;

            if (filesToDelete > 0) {
                try (BufferedReader br = new BufferedReader(new FileReader(indexDoneFile))) {
                    int filesDeletedCount = 0;

                    for (String line = br.readLine(); line != null; line = br.readLine()) {
                        line = line.trim();

                        if (StringUtils.isEmpty(line)) {
                            continue;
                        }

                        try {
                            IndexRecord record      = AtlasType.fromJson(line, IndexRecord.class);
                            File        logFile     = new File(record.getPath());
                            String      fileName    = logFile.getName();
                            File        archiveFile = new File(archiveFolder, fileName);

                            if (!archiveFile.exists()) {
                                LOG.warn("archive file does not exist: {}", archiveFile);

                                continue;
                            }

                            LOG.info("Deleting archive file: {}", archiveFile);

                            boolean ret = archiveFile.delete();

                            if (!ret) {
                                LOG.error("{}: Error deleting archive file. File: {}", source, archiveFile);
                            } else {
                                filesDeletedCount++;
                            }

                            if (filesDeletedCount >= filesToDelete) {
                                break;
                            }
                        } catch (Exception excp) {
                            LOG.error("{}: Error deleting older archive file in index-record: {}", source, line, excp);
                        }
                    }

                    LOG.info("{}: Deleted: {} archived files", source, filesDeletedCount);
                }
            }
        } catch(Exception exception){
            LOG.error("{}: Error deleting older files from archive folder. Folder: {}", source, archiveFolder, exception);
        }
    }
}
