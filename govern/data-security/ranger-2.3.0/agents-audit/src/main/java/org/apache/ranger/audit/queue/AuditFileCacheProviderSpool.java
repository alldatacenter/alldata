/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.audit.queue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.provider.AuditHandler;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.audit.provider.MiscUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class temporarily stores logs in Local file system before it despatches each logs in file to the AuditBatchQueue Consumer.
 * This gets instantiated only when AuditFileCacheProvider is enabled (xasecure.audit.provider.filecache.is.enabled).
 * When AuditFileCacheProvider is all the logs are stored in local file system before sent to destination.
 */

public class AuditFileCacheProviderSpool implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AuditFileCacheProviderSpool.class);

    public enum SPOOL_FILE_STATUS {
        pending, write_inprogress, read_inprogress, done
    }

    public static final String PROP_FILE_SPOOL_LOCAL_DIR				= "filespool.dir";
    public static final String PROP_FILE_SPOOL_LOCAL_FILE_NAME 			= "filespool.filename.format";
    public static final String PROP_FILE_SPOOL_ARCHIVE_DIR 				= "filespool.archive.dir";
    public static final String PROP_FILE_SPOOL_ARCHIVE_MAX_FILES_COUNT	= "filespool.archive.max.files";
    public static final String PROP_FILE_SPOOL_FILENAME_PREFIX 			= "filespool.file.prefix";
    public static final String PROP_FILE_SPOOL_FILE_ROLLOVER 			= "filespool.file.rollover.sec";
    public static final String PROP_FILE_SPOOL_INDEX_FILE 				= "filespool.index.filename";
    public static final String PROP_FILE_SPOOL_DEST_RETRY_MS 			= "filespool.destination.retry.ms";
    public static final String PROP_FILE_SPOOL_BATCH_SIZE               = "filespool.buffer.size";

    public static final String AUDIT_IS_FILE_CACHE_PROVIDER_ENABLE_PROP = "xasecure.audit.provider.filecache.is.enabled";
    public static final String FILE_CACHE_PROVIDER_NAME 				= "AuditFileCacheProviderSpool";

    AuditHandler consumerProvider = null;

    BlockingQueue<AuditIndexRecord> indexQueue 		= new LinkedBlockingQueue<AuditIndexRecord>();
    List<AuditIndexRecord> 			indexRecords	= new ArrayList<AuditIndexRecord>();

    // Folder and File attributes
    File 	logFolder 			= null;
    String	logFileNameFormat 	= null;
    File    archiveFolder 		= null;
    String 	fileNamePrefix 		= null;
    String 	indexFileName 		= null;
    File 	indexFile 			= null;
    String 	indexDoneFileName 	= null;
    File 	indexDoneFile 		= null;
    int 	retryDestinationMS 	= 30 * 1000; // Default 30 seconds
    int 	fileRolloverSec 	= 24 * 60 * 60; // In seconds
    int 	maxArchiveFiles 	= 100;
    int 	errorLogIntervalMS 	= 30 * 1000; // Every 30 seconds
    int     auditBatchSize      = 1000;
    long 	lastErrorLogMS 		= 0;
    boolean isAuditFileCacheProviderEnabled = false;
    boolean closeFile 			= false;
    boolean isPending 			= false;
    long	lastAttemptTime 	= 0;
    boolean initDone 			= false;

    PrintWriter		 logWriter = null;
    AuditIndexRecord currentWriterIndexRecord	= null;
    AuditIndexRecord currentConsumerIndexRecord = null;

    BufferedReader logReader = null;
    Thread destinationThread = null;

    boolean isWriting 	= true;
    boolean isDrain 	= false;
    boolean isDestDown 	= false;
    boolean isSpoolingSuccessful = true;

    private Gson gson = null;

    public AuditFileCacheProviderSpool(AuditHandler consumerProvider) {
        this.consumerProvider = consumerProvider;
    }

    public void init(Properties prop) {
        init(prop, null);
    }

    public boolean init(Properties props, String basePropertyName) {
        logger.debug("==> AuditFileCacheProviderSpool.init()");

        if (initDone) {
            logger.error("init() called more than once. queueProvider="
                    + "" + ", consumerProvider="
                    + consumerProvider.getName());
            return true;
        }
        String propPrefix = "xasecure.audit.filespool";
        if (basePropertyName != null) {
            propPrefix = basePropertyName;
        }

        try {
            gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                    .create();
            // Initial folder and file properties
            String logFolderProp = MiscUtil.getStringProperty(props, propPrefix
                    + "." + PROP_FILE_SPOOL_LOCAL_DIR);
            logFileNameFormat = MiscUtil.getStringProperty(props,
                    basePropertyName + "." + PROP_FILE_SPOOL_LOCAL_FILE_NAME);
            String archiveFolderProp = MiscUtil.getStringProperty(props,
                    propPrefix + "." + PROP_FILE_SPOOL_ARCHIVE_DIR);
            fileNamePrefix = MiscUtil.getStringProperty(props, propPrefix + "."
                    + PROP_FILE_SPOOL_FILENAME_PREFIX);
            indexFileName = MiscUtil.getStringProperty(props, propPrefix + "."
                    + PROP_FILE_SPOOL_INDEX_FILE);
            retryDestinationMS = MiscUtil.getIntProperty(props, propPrefix
                    + "." + PROP_FILE_SPOOL_DEST_RETRY_MS, retryDestinationMS);
            fileRolloverSec = MiscUtil.getIntProperty(props, propPrefix + "."
                    + PROP_FILE_SPOOL_FILE_ROLLOVER, fileRolloverSec);
            maxArchiveFiles = MiscUtil.getIntProperty(props, propPrefix + "."
                    + PROP_FILE_SPOOL_ARCHIVE_MAX_FILES_COUNT, maxArchiveFiles);
            isAuditFileCacheProviderEnabled = MiscUtil.getBooleanProperty(props, AUDIT_IS_FILE_CACHE_PROVIDER_ENABLE_PROP, false);
            logger.info("retryDestinationMS=" + retryDestinationMS
                    + ", queueName=" + FILE_CACHE_PROVIDER_NAME);
            logger.info("fileRolloverSec=" + fileRolloverSec + ", queueName="
                    + FILE_CACHE_PROVIDER_NAME);
            logger.info("maxArchiveFiles=" + maxArchiveFiles + ", queueName="
                    + FILE_CACHE_PROVIDER_NAME);

            if (logFolderProp == null || logFolderProp.isEmpty()) {
                logger.error("Audit spool folder is not configured. Please set "
                        + propPrefix
                        + "."
                        + PROP_FILE_SPOOL_LOCAL_DIR
                        + ". queueName=" + FILE_CACHE_PROVIDER_NAME);
                return false;
            }
            logFolder = new File(logFolderProp);
            if (!logFolder.isDirectory()) {
                boolean result = logFolder.mkdirs();
                if (!logFolder.isDirectory() || !result) {
                    logger.error("File Spool folder not found and can't be created. folder="
                            + logFolder.getAbsolutePath()
                            + ", queueName="
                            + FILE_CACHE_PROVIDER_NAME);
                    return false;
                }
            }
            logger.info("logFolder=" + logFolder + ", queueName="
                    + FILE_CACHE_PROVIDER_NAME);

            if (logFileNameFormat == null || logFileNameFormat.isEmpty()) {
                logFileNameFormat = "spool_" + "%app-type%" + "_"
                        + "%time:yyyyMMdd-HHmm.ss%.log";
            }
            logger.info("logFileNameFormat=" + logFileNameFormat
                    + ", queueName=" + FILE_CACHE_PROVIDER_NAME);

            if (archiveFolderProp == null || archiveFolderProp.isEmpty()) {
                archiveFolder = new File(logFolder, "archive");
            } else {
                archiveFolder = new File(archiveFolderProp);
            }
            if (!archiveFolder.isDirectory()) {
                boolean result = archiveFolder.mkdirs();
                if (!archiveFolder.isDirectory() || !result) {
                    logger.error("File Spool archive folder not found and can't be created. folder="
                            + archiveFolder.getAbsolutePath()
                            + ", queueName="
                            + FILE_CACHE_PROVIDER_NAME);
                    return false;
                }
            }
            logger.info("archiveFolder=" + archiveFolder + ", queueName="
                    + FILE_CACHE_PROVIDER_NAME);

            if (indexFileName == null || indexFileName.isEmpty()) {
                if (fileNamePrefix == null || fileNamePrefix.isEmpty()) {
                    fileNamePrefix = FILE_CACHE_PROVIDER_NAME + "_"
                            + consumerProvider.getName();
                }
                indexFileName = "index_" + fileNamePrefix + "_" + "%app-type%"
                        + ".json";
                indexFileName = MiscUtil.replaceTokens(indexFileName,
                        System.currentTimeMillis());
            }

            indexFile = new File(logFolder, indexFileName);
            if (!indexFile.exists()) {
                boolean ret = indexFile.createNewFile();
                if (!ret) {
                    logger.error("Error creating index file. fileName="
                            + indexFile.getPath());
                    return false;
                }
            }
            logger.info("indexFile=" + indexFile + ", queueName="
                    + FILE_CACHE_PROVIDER_NAME);

            int lastDot = indexFileName.lastIndexOf('.');
            if (lastDot < 0) {
                lastDot = indexFileName.length() - 1;
            }
            indexDoneFileName = indexFileName.substring(0, lastDot)
                    + "_closed.json";
            indexDoneFile = new File(logFolder, indexDoneFileName);
            if (!indexDoneFile.exists()) {
                boolean ret = indexDoneFile.createNewFile();
                if (!ret) {
                    logger.error("Error creating index done file. fileName="
                            + indexDoneFile.getPath());
                    return false;
                }
            }
            logger.info("indexDoneFile=" + indexDoneFile + ", queueName="
                    + FILE_CACHE_PROVIDER_NAME);

            // Load index file
            loadIndexFile();
            for (AuditIndexRecord auditIndexRecord : indexRecords) {
                if (!auditIndexRecord.status.equals(SPOOL_FILE_STATUS.done)) {
                    isPending = true;
                }
                if (auditIndexRecord.status
                        .equals(SPOOL_FILE_STATUS.write_inprogress)) {
                    currentWriterIndexRecord = auditIndexRecord;
                    logger.info("currentWriterIndexRecord="
                            + currentWriterIndexRecord.filePath
                            + ", queueName=" + FILE_CACHE_PROVIDER_NAME);
                }
                if (auditIndexRecord.status
                        .equals(SPOOL_FILE_STATUS.read_inprogress)) {
                    indexQueue.add(auditIndexRecord);
                }
            }
            printIndex();
            for (int i = 0; i < indexRecords.size(); i++) {
                AuditIndexRecord auditIndexRecord = indexRecords.get(i);
                if (auditIndexRecord.status.equals(SPOOL_FILE_STATUS.pending)) {
                    File consumerFile = new File(auditIndexRecord.filePath);
                    if (!consumerFile.exists()) {
                        logger.error("INIT: Consumer file="
                                + consumerFile.getPath() + " not found.");
                    } else {
                        indexQueue.add(auditIndexRecord);
                    }
                }
            }

        } catch (Throwable t) {
            logger.error("Error initializing File Spooler. queue="
                    + FILE_CACHE_PROVIDER_NAME, t);
            return false;
        }

        auditBatchSize = MiscUtil.getIntProperty(props, propPrefix
                + "." + PROP_FILE_SPOOL_BATCH_SIZE, auditBatchSize);

        initDone = true;

        logger.debug("<== AuditFileCacheProviderSpool.init()");
        return true;
    }

    /**
     * Start looking for outstanding logs and update status according.
     */
    public void start() {
        if (!initDone) {
            logger.error("Cannot start Audit File Spooler. Initilization not done yet. queueName="
                    + FILE_CACHE_PROVIDER_NAME);
            return;
        }

        logger.info("Starting writerThread, queueName="
                + FILE_CACHE_PROVIDER_NAME + ", consumer="
                + consumerProvider.getName());

        // Let's start the thread to read
        destinationThread = new Thread(this, FILE_CACHE_PROVIDER_NAME + "_"
                + consumerProvider.getName() + "_destWriter");
        destinationThread.setDaemon(true);
        destinationThread.start();
    }

    public void stop() {
        if (!initDone) {
            logger.error("Cannot stop Audit File Spooler. Initilization not done. queueName="
                    + FILE_CACHE_PROVIDER_NAME);
            return;
        }
        logger.info("Stop called, queueName=" + FILE_CACHE_PROVIDER_NAME
                + ", consumer=" + consumerProvider.getName());

        isDrain = true;
        flush();

        PrintWriter out = getOpenLogFileStream();
        if (out != null) {
            // If write is still going on, then let's give it enough time to
            // complete
            for (int i = 0; i < 3; i++) {
                if (isWriting) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    continue;
                }
                try {
                    logger.info("Closing open file, queueName="
                            + FILE_CACHE_PROVIDER_NAME + ", consumer="
                            + consumerProvider.getName());

                    out.flush();
                    out.close();
                    break;
                } catch (Throwable t) {
                    logger.debug("Error closing spool out file.", t);
                }
            }
        }
        try {
            if (destinationThread != null) {
                destinationThread.interrupt();
            }
            destinationThread = null;
        } catch (Throwable e) {
            // ignore
        }
    }

    public void flush() {
        if (!initDone) {
            logger.error("Cannot flush Audit File Spooler. Initilization not done. queueName="
                    + FILE_CACHE_PROVIDER_NAME);
            return;
        }
        PrintWriter out = getOpenLogFileStream();
        if (out != null) {
            out.flush();
        }
    }

    /**
     * If any files are still not processed. Also, if the destination is not
     * reachable
     *
     * @return
     */
    public boolean isPending() {
        if (!initDone) {
            logError("isPending(): File Spooler not initialized. queueName="
                    + FILE_CACHE_PROVIDER_NAME);
            return false;
        }

        return isPending;
    }

    /**
     * Milliseconds from last attempt time
     *
     * @return
     */
    public long getLastAttemptTimeDelta() {
        if (lastAttemptTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - lastAttemptTime;
    }

    synchronized public void stashLogs(AuditEventBase event) {

        if (isDrain) {
            // Stop has been called, so this method shouldn't be called
            logger.error("stashLogs() is called after stop is called. event="
                    + event);
            return;
        }
        try {
            isWriting = true;
            PrintWriter logOut = getLogFileStream();
            // Convert event to json
            String jsonStr = MiscUtil.stringify(event);
            logOut.println(jsonStr);
            logOut.flush();
            isPending = true;
            isSpoolingSuccessful = true;
        } catch (Throwable  t) {
            isSpoolingSuccessful = false;
            logger.error("Error writing to file. event=" + event, t);
        } finally {
            isWriting = false;
        }

    }

    synchronized public void stashLogs(Collection<AuditEventBase> events) {
        for (AuditEventBase event : events) {
            stashLogs(event);
        }
        flush();
    }

    synchronized public void stashLogsString(String event) {
        if (isDrain) {
            // Stop has been called, so this method shouldn't be called
            logger.error("stashLogs() is called after stop is called. event="
                    + event);
            return;
        }
        try {
            isWriting = true;
            PrintWriter logOut = getLogFileStream();
            logOut.println(event);
        } catch (Exception ex) {
            logger.error("Error writing to file. event=" + event, ex);
        } finally {
            isWriting = false;
        }

    }

    synchronized public boolean isSpoolingSuccessful() {
        return isSpoolingSuccessful;
    }

    synchronized public void stashLogsString(Collection<String> events) {
        for (String event : events) {
            stashLogsString(event);
        }
        flush();
    }

    /**
     * This return the current file. If there are not current open output file,
     * then it will return null
     *
     * @return
     * @throws Exception
     */
    synchronized private PrintWriter getOpenLogFileStream() {
        return logWriter;
    }

    /**
     * @return
     * @throws Exception
     */
    synchronized private PrintWriter getLogFileStream() throws Exception {
        closeFileIfNeeded();
        // Either there are no open log file or the previous one has been rolled
        // over
        if (currentWriterIndexRecord == null) {
            Date currentTime = new Date();
            // Create a new file
            String fileName = MiscUtil.replaceTokens(logFileNameFormat,
                    currentTime.getTime());
            String newFileName = fileName;
            File outLogFile = null;
            int i = 0;
            while (true) {
                outLogFile = new File(logFolder, newFileName);
                File archiveLogFile = new File(archiveFolder, newFileName);
                if (!outLogFile.exists() && !archiveLogFile.exists()) {
                    break;
                }
                i++;
                int lastDot = fileName.lastIndexOf('.');
                String baseName = fileName.substring(0, lastDot);
                String extension = fileName.substring(lastDot);
                newFileName = baseName + "." + i + extension;
            }
            fileName = newFileName;
            logger.info("Creating new file. queueName="
                    + FILE_CACHE_PROVIDER_NAME + ", fileName=" + fileName);
            // Open the file
            logWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                    outLogFile),"UTF-8")));

            AuditIndexRecord tmpIndexRecord = new AuditIndexRecord();

            tmpIndexRecord.id = MiscUtil.generateUniqueId();
            tmpIndexRecord.filePath = outLogFile.getPath();
            tmpIndexRecord.status = SPOOL_FILE_STATUS.write_inprogress;
            tmpIndexRecord.fileCreateTime = currentTime;
            tmpIndexRecord.lastAttempt = true;
            currentWriterIndexRecord = tmpIndexRecord;
            indexRecords.add(currentWriterIndexRecord);
            saveIndexFile();

        } else {
            if (logWriter == null) {
                // This means the process just started. We need to open the file
                // in append mode.
                logger.info("Opening existing file for append. queueName="
                        + FILE_CACHE_PROVIDER_NAME + ", fileName="
                        + currentWriterIndexRecord.filePath);
                logWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                        currentWriterIndexRecord.filePath, true),"UTF-8")));
            }
        }
        return logWriter;
    }

    synchronized private void closeFileIfNeeded() throws FileNotFoundException,
            IOException {
        // Is there file open to write or there are no pending file, then close
        // the active file
        if (currentWriterIndexRecord != null) {
            // Check whether the file needs to rolled
            rollOverSpoolFileByTime();

            if (closeFile) {
                // Roll the file
                if (logWriter != null) {
                    logWriter.flush();
                    logWriter.close();
                    logWriter = null;
                    closeFile = false;
                }
                currentWriterIndexRecord.status = SPOOL_FILE_STATUS.pending;
                currentWriterIndexRecord.writeCompleteTime = new Date();
                saveIndexFile();
                logger.info("Adding file to queue. queueName="
                        + FILE_CACHE_PROVIDER_NAME + ", fileName="
                        + currentWriterIndexRecord.filePath);
                indexQueue.add(currentWriterIndexRecord);
                currentWriterIndexRecord = null;
            }
        }
    }

    private void rollOverSpoolFileByTime() {
        if (System.currentTimeMillis()
                - currentWriterIndexRecord.fileCreateTime.getTime() > fileRolloverSec * 1000) {
            closeFile = true;
            logger.info("Closing file. Rolling over. queueName="
                    + FILE_CACHE_PROVIDER_NAME + ", fileName="
                    + currentWriterIndexRecord.filePath);
        }
    }

    /**
     * Load the index file
     *
     * @throws IOException
     */
    void loadIndexFile() throws IOException {
        logger.info("Loading index file. fileName=" + indexFile.getPath());
        BufferedReader br = null;
        try {
             br = new BufferedReader(new InputStreamReader(new FileInputStream(indexFile), "UTF-8"));
            indexRecords.clear();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    AuditIndexRecord record = gson.fromJson(line,
                            AuditIndexRecord.class);
                    indexRecords.add(record);
                }
            }
        } finally {
            if (br!= null) {
                br.close();
            }
        }
    }

    synchronized void printIndex() {
        logger.info("INDEX printIndex() ==== START");
        Iterator<AuditIndexRecord> iter = indexRecords.iterator();
        while (iter.hasNext()) {
            AuditIndexRecord record = iter.next();
            logger.info("INDEX=" + record + ", isFileExist="
                    + (new File(record.filePath).exists()));
        }
        logger.info("INDEX printIndex() ==== END");
    }

    synchronized void removeIndexRecord(AuditIndexRecord indexRecord)
            throws FileNotFoundException, IOException {
        Iterator<AuditIndexRecord> iter = indexRecords.iterator();
        while (iter.hasNext()) {
            AuditIndexRecord record = iter.next();
            if (record.id.equals(indexRecord.id)) {
                logger.info("Removing file from index. file=" + record.filePath
                        + ", queueName=" + FILE_CACHE_PROVIDER_NAME
                        + ", consumer=" + consumerProvider.getName());

                iter.remove();
                appendToDoneFile(record);
            }
        }
        saveIndexFile();
        // If there are no more files in the index, then let's assume the
        // destination is now available
        if (indexRecords.size() == 0) {
            isPending = false;
        }
    }

    synchronized void saveIndexFile() throws FileNotFoundException, IOException {
        PrintWriter out = new PrintWriter(indexFile,"UTF-8");
        for (AuditIndexRecord auditIndexRecord : indexRecords) {
            out.println(gson.toJson(auditIndexRecord));
        }
        out.close();
        // printIndex();

    }

    void appendToDoneFile(AuditIndexRecord indexRecord)
            throws FileNotFoundException, IOException {
        logger.info("Moving to done file. " + indexRecord.filePath
                + ", queueName=" + FILE_CACHE_PROVIDER_NAME + ", consumer="
                + consumerProvider.getName());
        String line = gson.toJson(indexRecord);
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                indexDoneFile, true),"UTF-8")));
        out.println(line);
        out.flush();
        out.close();

        // After Each file is read and audit events are pushed into pipe, we flush to reach the destination immediate.
        consumerProvider.flush();

        // Move to archive folder
        File logFile = null;
        File archiveFile = null;
        try {
            logFile = new File(indexRecord.filePath);
            String fileName = logFile.getName();
            archiveFile = new File(archiveFolder, fileName);
            logger.info("Moving logFile " + logFile + " to " + archiveFile);
            boolean result = logFile.renameTo(archiveFile);
            if (!result) {
                logger.error("Error moving log file to archive folder. Unable to rename"
                        + logFile + " to archiveFile=" + archiveFile);
            }
        } catch (Throwable t) {
            logger.error("Error moving log file to archive folder. logFile="
                    + logFile + ", archiveFile=" + archiveFile, t);
        }

        // After archiving the file flush the pipe
        consumerProvider.flush();

        archiveFile = null;
        try {
            // Remove old files
            File[] logFiles = archiveFolder.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.getName().toLowerCase().endsWith(".log");
                }
            });

            if (logFiles != null && logFiles.length > maxArchiveFiles) {
                int filesToDelete = logFiles.length - maxArchiveFiles;
                BufferedReader br = new BufferedReader(new FileReader(
                        indexDoneFile));
                try {
                    int filesDeletedCount = 0;
                    while ((line = br.readLine()) != null) {
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            AuditIndexRecord record = gson.fromJson(line,
                                    AuditIndexRecord.class);
                            logFile = new File(record.filePath);
                            String fileName = logFile.getName();
                            archiveFile = new File(archiveFolder, fileName);
                            if (archiveFile.exists()) {
                                logger.info("Deleting archive file "
                                        + archiveFile);
                                boolean ret = archiveFile.delete();
                                if (!ret) {
                                    logger.error("Error deleting archive file. archiveFile="
                                            + archiveFile);
                                }
                                filesDeletedCount++;
                                if (filesDeletedCount >= filesToDelete) {
                                    logger.info("Deleted " + filesDeletedCount
                                            + " files");
                                    break;
                                }
                            }
                        }
                    }
                } finally {
                    br.close();
                }
            }
        } catch (Throwable t) {
            logger.error("Error deleting older archive file. archiveFile="
                    + archiveFile, t);
        }

    }

    void logError(String msg) {
        long currTimeMS = System.currentTimeMillis();
        if (currTimeMS - lastErrorLogMS > errorLogIntervalMS) {
            logger.error(msg);
            lastErrorLogMS = currTimeMS;
        }
    }

    class AuditIndexRecord {
        String id;
        String filePath;
        int linePosition = 0;
        SPOOL_FILE_STATUS status = SPOOL_FILE_STATUS.write_inprogress;
        Date fileCreateTime;
        Date writeCompleteTime;
        Date doneCompleteTime;
        Date lastSuccessTime;
        Date lastFailedTime;
        int failedAttemptCount = 0;
        boolean lastAttempt = false;

        @Override
        public String toString() {
            return "AuditIndexRecord [id=" + id + ", filePath=" + filePath
                    + ", linePosition=" + linePosition + ", status=" + status
                    + ", fileCreateTime=" + fileCreateTime
                    + ", writeCompleteTime=" + writeCompleteTime
                    + ", doneCompleteTime=" + doneCompleteTime
                    + ", lastSuccessTime=" + lastSuccessTime
                    + ", lastFailedTime=" + lastFailedTime
                    + ", failedAttemptCount=" + failedAttemptCount
                    + ", lastAttempt=" + lastAttempt + "]";
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        try {
            //This is done to clear the MDC context to avoid issue with Ranger Auditing for Knox
            MDC.clear();
            runLogAudit();
        } catch (Throwable t) {
            logger.error("Exited thread without abnormaly. queue="
                    + consumerProvider.getName(), t);
        }
    }

    public void runLogAudit() {
        // boolean isResumed = false;
        while (true) {
            try {
                if (isDestDown) {
                    logger.info("Destination is down. sleeping for "
                            + retryDestinationMS
                            + " milli seconds. indexQueue=" + indexQueue.size()
                            + ", queueName=" + FILE_CACHE_PROVIDER_NAME
                            + ", consumer=" + consumerProvider.getName());
                    Thread.sleep(retryDestinationMS);
                }
                // Let's pause between each iteration
                if (currentConsumerIndexRecord == null) {
                    currentConsumerIndexRecord = indexQueue.poll(
                            retryDestinationMS, TimeUnit.MILLISECONDS);
                } else {
                    Thread.sleep(retryDestinationMS);
                }

                if (isDrain) {
                    // Need to exit
                    break;
                }
                if (currentConsumerIndexRecord == null) {
                    closeFileIfNeeded();
                    continue;
                }

                boolean isRemoveIndex = false;
                File consumerFile = new File(
                        currentConsumerIndexRecord.filePath);
                if (!consumerFile.exists()) {
                    logger.error("Consumer file=" + consumerFile.getPath()
                            + " not found.");
                    printIndex();
                    isRemoveIndex = true;
                } else {
                    // Let's open the file to write
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(
                            currentConsumerIndexRecord.filePath),"UTF-8"));
                    try {
                        int startLine = currentConsumerIndexRecord.linePosition;
                        String line;
                        int currLine = 0;
                        List<AuditEventBase> events = new ArrayList<>();
                        while ((line = br.readLine()) != null) {
                            currLine++;
                            if (currLine < startLine) {
                                continue;
                            }
                            AuditEventBase event = MiscUtil.fromJson(line, AuthzAuditEvent.class);
                            events.add(event);

                            if (events.size() == auditBatchSize) {
                                boolean ret = sendEvent(events,
                                        currentConsumerIndexRecord, currLine);
                                if (!ret) {
                                    throw new Exception("Destination down");
                                }
                                events.clear();
                            }
                        }
                        if (events.size() > 0) {
                            boolean ret = sendEvent(events,
                                    currentConsumerIndexRecord, currLine);
                            if (!ret) {
                                throw new Exception("Destination down");
                            }
                            events.clear();
                        }
                        logger.info("Done reading file. file="
                                + currentConsumerIndexRecord.filePath
                                + ", queueName=" + FILE_CACHE_PROVIDER_NAME
                                + ", consumer=" + consumerProvider.getName());
                        // The entire file is read
                        currentConsumerIndexRecord.status = SPOOL_FILE_STATUS.done;
                        currentConsumerIndexRecord.doneCompleteTime = new Date();
                        currentConsumerIndexRecord.lastAttempt = true;

                        isRemoveIndex = true;
                    } catch (Exception ex) {
                        isDestDown = true;
                        logError("Destination down. queueName="
                                + FILE_CACHE_PROVIDER_NAME + ", consumer="
                                + consumerProvider.getName());
                        lastAttemptTime = System.currentTimeMillis();
                        // Update the index file
                        currentConsumerIndexRecord.lastFailedTime = new Date();
                        currentConsumerIndexRecord.failedAttemptCount++;
                        currentConsumerIndexRecord.lastAttempt = false;
                        saveIndexFile();
                    } finally {
                        br.close();
                    }
                }
                if (isRemoveIndex) {
                    // Remove this entry from index
                    removeIndexRecord(currentConsumerIndexRecord);
                    currentConsumerIndexRecord = null;
                    closeFileIfNeeded();
                }
            } catch (InterruptedException e) {
                logger.info("Caught exception in consumer thread. Shutdown might be in progress");
            } catch (Throwable t) {
                logger.error("Exception in destination writing thread.", t);
            }
        }
        logger.info("Exiting file spooler. provider=" + FILE_CACHE_PROVIDER_NAME
                + ", consumer=" + consumerProvider.getName());
    }

    private boolean sendEvent(List<AuditEventBase> events, AuditIndexRecord indexRecord,
                              int currLine) {
        boolean ret = true;
        try {
            ret = consumerProvider.log(events);
            if (!ret) {
                // Need to log error after fixed interval
                logError("Error sending logs to consumer. provider="
                        + FILE_CACHE_PROVIDER_NAME + ", consumer="
                        + consumerProvider.getName());
            } else {
                // Update index and save
                indexRecord.linePosition = currLine;
                indexRecord.status = SPOOL_FILE_STATUS.read_inprogress;
                indexRecord.lastSuccessTime = new Date();
                indexRecord.lastAttempt = true;
                saveIndexFile();

                if (isDestDown) {
                    isDestDown = false;
                    logger.info("Destination up now. " + indexRecord.filePath
                            + ", queueName=" + FILE_CACHE_PROVIDER_NAME
                            + ", consumer=" + consumerProvider.getName());
                }
            }
        } catch (Throwable t) {
            logger.error("Error while sending logs to consumer. provider="
                    + FILE_CACHE_PROVIDER_NAME + ", consumer="
                    + consumerProvider.getName() + ", log=" + events, t);
        }

        return ret;
    }

}
