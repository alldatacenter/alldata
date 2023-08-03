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

package org.apache.atlas.repository.migration;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.RequestContext;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.migration.MigrationImportStatus;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.impexp.ImportService;
import org.apache.atlas.repository.impexp.ZipExportFileNames;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileFilter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

public class ZipFileMigrationImporter implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ZipFileMigrationImporter.class);

    private static final String APPLICATION_PROPERTY_MIGRATION_NUMER_OF_WORKERS = "atlas.migration.mode.workers";
    private static final String APPLICATION_PROPERTY_MIGRATION_BATCH_SIZE = "atlas.migration.mode.batch.size";
    private static final String DEFAULT_NUMBER_OF_WORKERS = "4";
    private static final String DEFAULT_BATCH_SIZE = "100";
    private static final String ZIP_FILE_COMMENT_ENTITIES_COUNT = "entitiesCount";
    private static final String ZIP_FILE_COMMENT_TOTAL_COUNT = "total";
    private static final String FILE_EXTENSION_ZIP = ".zip";
    private final static String ENV_USER_NAME = "user.name";
    private final static String ARCHIVE_DIR = "archive";

    private final ImportService         importService;
    private List<String>                filesToImport;
    private DataMigrationStatusService  dataMigrationStatusService;
    private MigrationImportStatus       migrationImportStatus;
    private File                        archiveDir;

    /**
     * Input:
     * fileName : can support wildcards. If it contains wildcards then all matching files will be imported
     */
    public ZipFileMigrationImporter(ImportService importService, String fileName) {
        this.importService = importService;
        this.dataMigrationStatusService = new DataMigrationStatusService(AtlasGraphProvider.getGraphInstance());

        initialize(fileName);
    }

    private void initialize(String fileName) {
        this.filesToImport = getAllFilesToImport(fileName);

        if (CollectionUtils.isNotEmpty(this.filesToImport)) {
            createArchiveDirectory(fileName);
        }
    }

    @Override
    public void run() {
        for (String fileToImport : filesToImport) {
            try {
                detectFileToImport(fileToImport);

                int streamSize = getStreamSizeFromComment(fileToImport);
                migrationImportStatus = getCreateMigrationStatus(fileToImport, streamSize);
                performImport(fileToImport, streamSize, Long.toString(migrationImportStatus.getCurrentIndex()));
                dataMigrationStatusService.setStatus("DONE");

                moveZipFileToArchiveDir(fileToImport);
            } catch (IOException e) {
                LOG.error("Migration Import: IO Error!", e);
                dataMigrationStatusService.setStatus("FAIL");
            } catch (AtlasBaseException e) {
                LOG.error("Migration Import: Error!", e);
                dataMigrationStatusService.setStatus("FAIL");
            }
        }
    }

    /**
     * Input:
     * fileName : If it contains wildcards then all matching files will be discovered
     */
    private List<String> getAllFilesToImport(String fileName) {
        List<String> ret = new ArrayList<>();
        File fileToImport = new File(fileName);

        if (fileToImport.exists() && fileToImport.isFile()) {
            //Input file present so no need to expand
            LOG.info("Migration Import: zip file for import: " + fileToImport);

            ret.add(fileToImport.getAbsolutePath());
        } else {
            //The fileName might have wildcard
            String dirPath = new File(fileToImport.getParent()).getAbsolutePath();
            File importDataDir = new File(dirPath);

            if (importDataDir.exists() && importDataDir.isDirectory()) {
                String fileNameWithWildcard = fileToImport.getName();
                FileFilter fileFilter = new WildcardFileFilter(fileNameWithWildcard);

                File[] importFiles = importDataDir.listFiles(fileFilter);

                if (ArrayUtils.isNotEmpty(importFiles)) {
                    Arrays.sort(importFiles);

                    LOG.info("Migration Import: zip files for import: ");

                    for (File importFile : importFiles) {
                        if (isValidImportFile(importFile)) {
                            LOG.info(importFile.getName() + " with absolute path - " + importFile.getAbsolutePath());
                            ret.add(importFile.getAbsolutePath());
                        } else {
                            LOG.warn("Ignoring " + importFile.getAbsolutePath() + " as it is not a file or does not end with extension " + FILE_EXTENSION_ZIP);
                        }
                    }
                } else {
                    LOG.warn("Migration Import: No files to import");
                }
            }
        }

        return ret;
    }

    private boolean isValidImportFile(File importFile) {
        return importFile.isFile() && StringUtils.endsWithIgnoreCase(importFile.getName(), FILE_EXTENSION_ZIP);
    }

    private void createArchiveDirectory(String fileName) {
        File fileToImport = new File(fileName);
        String parentPath = new File(fileToImport.getParent()).getAbsolutePath();

        this.archiveDir = new File(parentPath + File.separator + ARCHIVE_DIR);

        if (this.archiveDir.exists() && !this.archiveDir.canWrite()) {
            LOG.warn("Migration Import: No write permission to archive directory {}", this.archiveDir.getAbsolutePath());
            this.archiveDir = null;
        } else if (!this.archiveDir.exists() && !this.archiveDir.getParentFile().canWrite()) {
            LOG.warn("Migration Import: No permission to create archive directory {}", this.archiveDir.getAbsolutePath());
            this.archiveDir = null;
        } else {
            this.archiveDir.mkdirs();
            LOG.info("Migration Import: archive directory for zip files: {}", this.archiveDir.getAbsolutePath());
        }
    }

    private void moveZipFileToArchiveDir(String srcFilePath) {
        if (this.archiveDir == null) {
            return;
        }

        File sourceFile = new File(srcFilePath);
        String newFile = archiveDir.getAbsolutePath() + File.separator + sourceFile.getName();

        if (!sourceFile.canWrite()) {
            LOG.warn("Migration Import: No permission to archive the zip file {}", sourceFile.getAbsolutePath());
            this.archiveDir = null;
        } else {
            if (sourceFile.renameTo(new File(newFile))) {
                sourceFile.delete();
                LOG.info("Migration Import: Successfully archived the zip file: " + srcFilePath + " to " + this.archiveDir.getAbsolutePath());
            } else {
                LOG.warn("Migration Import: Failed to archive the zip file: " + srcFilePath);
            }
        }
    }

    private MigrationImportStatus getCreateMigrationStatus(String fileName, int streamSize) {
        MigrationImportStatus status = null;
        try {
            status = new MigrationImportStatus(fileName, DigestUtils.md5Hex(new FileInputStream(fileName)));
        } catch (IOException e) {
            LOG.error("Exception occurred while creating migration import", e);
        }

        status.setTotalCount(streamSize);

        MigrationImportStatus statusRetrieved = dataMigrationStatusService.getCreate(status);

        LOG.info("DataMigrationStatusService: Position: {}", statusRetrieved.getCurrentIndex());
        dataMigrationStatusService.setStatus("STARTED");
        return statusRetrieved;
    }

    private void detectFileToImport(String fileToImport) throws IOException {
        FileWatcher fileWatcher = new FileWatcher(fileToImport);
        fileWatcher.start();
    }

    private int getStreamSizeFromComment(String fileToImport) {
        int ret = 1;
        try {
            ZipFile zipFile = new ZipFile(fileToImport);
            String comment = zipFile.getComment();
            ret = processZipFileStreamSizeComment(comment);
            zipFile.close();
        } catch (IOException e) {
            LOG.error("Error opening ZIP file: {}", fileToImport, e);
        }

        return ret;
    }

    private int processZipFileStreamSizeComment(String comment) {
        if (StringUtils.isEmpty(comment)) {
            return 1;
        }

        Map map = AtlasType.fromJson(comment, Map.class);
        int entitiesCount = (int) map.get(ZIP_FILE_COMMENT_ENTITIES_COUNT);
        int totalCount = (int) map.get(ZIP_FILE_COMMENT_TOTAL_COUNT);
        LOG.info("ZipFileMigrationImporter: Zip file: Comment: streamSize: {}: total: {}", entitiesCount, totalCount);

        return entitiesCount;
    }

    private void performImport(String fileToImport, int streamSize, String startPosition) throws AtlasBaseException {
        try {
            LOG.info("Migration Import: {}: Starting at: {}...", fileToImport, startPosition);
            InputStream fs = new FileInputStream(fileToImport);
            RequestContext.get().setUser(getUserNameFromEnvironment(), null);

            importService.run(fs, getImportRequest(fileToImport, streamSize, startPosition),
                    getUserNameFromEnvironment(),
                    InetAddress.getLocalHost().getHostName(),
                    InetAddress.getLocalHost().getHostAddress());

        } catch (Exception ex) {
            LOG.error("Migration Import: Error loading zip for migration!", ex);
            throw new AtlasBaseException(ex);
        } finally {
            LOG.info("Migration Import: {}: Done!", fileToImport);
        }
    }

    private String getUserNameFromEnvironment() {
        return System.getProperty(ENV_USER_NAME);
    }

    private AtlasImportRequest getImportRequest(String fileToImport, int streamSize, String position) throws AtlasException {
        AtlasImportRequest request = new AtlasImportRequest();

        request.setOption(AtlasImportRequest.OPTION_KEY_MIGRATION_FILE_NAME, fileToImport);
        request.setSizeOption(streamSize);
        request.setOption(AtlasImportRequest.OPTION_KEY_MIGRATION, "true");
        request.setOption(AtlasImportRequest.OPTION_KEY_NUM_WORKERS, getPropertyValue(APPLICATION_PROPERTY_MIGRATION_NUMER_OF_WORKERS, DEFAULT_NUMBER_OF_WORKERS));
        request.setOption(AtlasImportRequest.OPTION_KEY_BATCH_SIZE, getPropertyValue(APPLICATION_PROPERTY_MIGRATION_BATCH_SIZE, DEFAULT_BATCH_SIZE));
        request.setOption(AtlasImportRequest.START_POSITION_KEY, (StringUtils.isEmpty(position) ? "0" : position));

        return request;
    }

    private String getPropertyValue(String property, String defaultValue) throws AtlasException {
        return ApplicationProperties.get().getString(property, defaultValue);
    }
}
