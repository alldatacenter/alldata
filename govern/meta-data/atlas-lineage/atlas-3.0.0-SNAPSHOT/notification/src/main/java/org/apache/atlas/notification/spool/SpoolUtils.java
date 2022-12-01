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

import org.apache.atlas.notification.spool.models.IndexRecord;
import org.apache.atlas.notification.spool.models.IndexRecords;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;

public class SpoolUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SpoolUtils.class);

    private static final String           USER_SPECIFIC_PATH_NAME_FORMAT = "%s-%s";
    public  static final String           DEFAULT_CHAR_SET              = "UTF-8";
    private static final String           DEFAULT_LINE_SEPARATOR        = System.getProperty("line.separator");
    private static final String           FILE_EXT_JSON                 = ".json";
    public  static final String           FILE_EXT_LOG                  = ".log";
    private static final String           SPOOL_FILE_NAME_FORMAT_PREFIX = "%s.%s%s";
    private static final String           INDEX_FILE_CLOSED_SUFFIX      = "_closed.json";
    private static final String           INDEX_FILE_PUBLISH_SUFFIX     = "_publish.json";
    private static final String           INDEX_FILE_NAME_FORMAT        = "index-%s-%s" + FILE_EXT_JSON;
    private static final String           SPOOL_FILE_NAME_FORMAT        = "spool-%s-%s-%s" + FILE_EXT_LOG;
    private static final String           RECORD_EMPTY = StringUtils.leftPad(StringUtils.EMPTY, IndexRecord.RECORD_SIZE) + SpoolUtils.getLineSeparator();

    public static File getCreateFile(File file, String source) throws IOException {
        if (createFileIfNotExists(file, source)) {
            LOG.info("SpoolUtils.getCreateFile(source={}): file={}", source, file.getAbsolutePath());
        }

        return file;
    }

    public static boolean createFileIfNotExists(File file, String source) throws IOException {
        boolean ret = file.exists();

        if (!ret) {
            ret = file.createNewFile();

            if (!ret) {
                LOG.error("SpoolUtils.createFileIfNotExists(source={}): error creating file {}", source, file.getPath());

                ret = false;
            }
        }

        return ret;
    }

    public static File getCreateDirectoryWithPermissionCheck(File file, String user) {
        File ret = getCreateDirectory(file);

        LOG.info("SpoolUtils.getCreateDirectory({}): Checking permissions...");
        if (!file.canWrite() || !file.canRead()) {
            File fileWithUserSuffix = getFileWithUserSuffix(file, user);
            LOG.error("SpoolUtils.getCreateDirectory({}, {}): Insufficient permissions for user: {}! Will create: {}",
                    file.getAbsolutePath(), user, user, fileWithUserSuffix);
            ret = getCreateDirectory(fileWithUserSuffix);
        }

        return ret;
    }

    private static File getFileWithUserSuffix(File file, String user) {
        if (!file.isDirectory()) {
            return file;
        }

        String absolutePath = file.getAbsolutePath();
        if (absolutePath.endsWith(File.pathSeparator)) {
            absolutePath = StringUtils.removeEnd(absolutePath, File.pathSeparator);
        }

        return new File(String.format(USER_SPECIFIC_PATH_NAME_FORMAT, absolutePath, user));
    }

    public static File getCreateDirectory(File file) {
        File ret = file;

        if (!file.isDirectory()) {
            boolean result = file.mkdirs();

            if (!file.isDirectory() || !result) {
                LOG.error("SpoolUtils.getCreateDirectory({}): cannot be created!", file);

                ret = null;
            }
        }

        return ret;
    }

    public static PrintWriter createAppendPrintWriter(File filePath) throws UnsupportedEncodingException, FileNotFoundException {
        return new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(filePath, true), DEFAULT_CHAR_SET)));
    }

    public static String getIndexFileName(String source, String handlerName) {
        return String.format(SpoolUtils.INDEX_FILE_NAME_FORMAT, source, handlerName);
    }

    public static String getIndexDoneFile(String filePath) {
        return StringUtils.substringBeforeLast(filePath, SpoolUtils.FILE_EXT_JSON) + SpoolUtils.INDEX_FILE_CLOSED_SUFFIX;
    }

    public static String getIndexPublishFile(String filePath) {
        return StringUtils.substringBeforeLast(filePath, SpoolUtils.FILE_EXT_JSON) + SpoolUtils.INDEX_FILE_PUBLISH_SUFFIX;
    }

    public static boolean fileExists(IndexRecord record) {
        return record != null && new File(record.getPath()).exists();
    }

    static String getSpoolFileName(String source, String handlerName, String guid) {
        return String.format(SPOOL_FILE_NAME_FORMAT, source, handlerName, guid);
    }

    public static String getSpoolFilePath(SpoolConfiguration cfg, String spoolDir, String archiveFolder, String suffix) {
        File   ret         = null;
        String fileName    = getSpoolFileName(cfg.getSourceName(), cfg.getMessageHandlerName(), suffix);
        int    lastDot   = StringUtils.lastIndexOf(fileName, '.');
        String baseName  = fileName.substring(0, lastDot);
        String extension = fileName.substring(lastDot);

        for (int sequence = 1; true; sequence++) {
            ret = new File(spoolDir, fileName);

            File archiveLogFile = new File(archiveFolder, fileName);

            if (!ret.exists() && !archiveLogFile.exists()) {
                break;
            }

            fileName = String.format(SPOOL_FILE_NAME_FORMAT_PREFIX, baseName, sequence, extension);
        }

        return ret.getPath();
    }

    public static String getLineSeparator() {
        return DEFAULT_LINE_SEPARATOR;
    }

    public static String getRecordForWriting(IndexRecord record) {
        String json = AtlasType.toJson(record);

        return StringUtils.rightPad(json, IndexRecord.RECORD_SIZE) + SpoolUtils.getLineSeparator();
    }

    public static String getEmptyRecordForWriting() {
        return RECORD_EMPTY;
    }

    public static IndexRecords createRecords(String[] items) {
        IndexRecords records = new IndexRecords();

        if (items != null && items.length > 0) {
            try {
                for (String item : items) {
                    if (StringUtils.isNotBlank(item)) {
                        IndexRecord record = AtlasType.fromJson(item, IndexRecord.class);

                        records.getRecords().put(record.getId(), record);
                    }
                }
            } catch (Exception ex) {
                LOG.error("SpoolUtils.createRecords(): error loading records.", ex);
            }
        }

        return records;
    }
}
