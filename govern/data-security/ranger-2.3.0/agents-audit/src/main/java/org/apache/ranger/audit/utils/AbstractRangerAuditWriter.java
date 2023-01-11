package org.apache.ranger.audit.utils;

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

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.ranger.audit.provider.MiscUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * This is Abstract class to have common properties of Ranger Audit HDFS Destination Writer.
 */
public abstract class AbstractRangerAuditWriter implements RangerAuditWriter {
    private static final Logger logger = LoggerFactory.getLogger(AbstractRangerAuditWriter.class);

    public static final String    PROP_FILESYSTEM_DIR              = "dir";
    public static final String    PROP_FILESYSTEM_SUBDIR           = "subdir";
    public static final String    PROP_FILESYSTEM_FILE_NAME_FORMAT = "filename.format";
    public static final String    PROP_FILESYSTEM_FILE_ROLLOVER    = "file.rollover.sec";
    public static final String    PROP_FILESYSTEM_ROLLOVER_PERIOD  = "file.rollover.period";
    public static final String    PROP_FILESYSTEM_FILE_EXTENSION   = ".log";
    public Configuration		  conf						       = null;
    public FileSystem		      fileSystem				       = null;
    public Map<String, String>    auditConfigs				       = null;
    public Path				      auditPath					       = null;
    public PrintWriter            logWriter                        = null;
    public RollingTimeUtil        rollingTimeUtil                  = null;
    public String			      auditProviderName			       = null;
    public String			      fullPath                         = null;
    public String 				  parentFolder					   = null;
    public String			      currentFileName	               = null;
    public String 			      logFileNameFormat			       = null;
    public String		          logFolder                        = null;
    public String 				  fileExtension					   = null;
    public String                 rolloverPeriod                   = null;
    public String				  fileSystemScheme				   = null;
    public Date                   nextRollOverTime                 = null;
    public int                    fileRolloverSec			       = 24 * 60 * 60; // In seconds
    public boolean                rollOverByDuration               = false;
    public volatile FSDataOutputStream ostream                     = null;   // output stream wrapped in logWriter
    private boolean               isHFlushCapableStream            = false;

    @Override
    public void init(Properties props, String propPrefix, String auditProviderName, Map<String,String> auditConfigs) {
        // Initialize properties for this class
        // Initial folder and file properties
        logger.info("==> AbstractRangerAuditWriter.init()");
        this.auditProviderName = auditProviderName;
        this.auditConfigs	   = auditConfigs;

        init(props,propPrefix);

        logger.info("<== AbstractRangerAuditWriter.init()");
    }

    public void createFileSystemFolders() throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("==> AbstractRangerAuditWriter.createFileSystemFolders()");
        }
        // Create a new file
        Date   currentTime    = new Date();
        String fileName       = MiscUtil.replaceTokens(logFileNameFormat,	currentTime.getTime());
        parentFolder   		  = MiscUtil.replaceTokens(logFolder,	currentTime.getTime());
        fullPath              = parentFolder + Path.SEPARATOR + fileName;
        String defaultPath    = fullPath;
        conf                  = createConfiguration();
        URI	      uri         = URI.create(fullPath);
        fileSystem            = FileSystem.get(uri, conf);
        auditPath             = new Path(fullPath);
        fileSystemScheme	  = getFileSystemScheme();
        logger.info("Checking whether log file exists. "+ fileSystemScheme + "Path= " + fullPath + ", UGI=" + MiscUtil.getUGILoginUser());
        int i = 0;
        while (fileSystem.exists(auditPath)) {
            i++;
            int    lastDot   = defaultPath.lastIndexOf('.');
            String baseName  = defaultPath.substring(0, lastDot);
            String extension = defaultPath.substring(lastDot);
            fullPath         = baseName + "." + i + extension;
            auditPath        = new Path(fullPath);
            logger.info("Checking whether log file exists. "+ fileSystemScheme + "Path= " + fullPath);
        }
        logger.info("Log file doesn't exists. Will create and use it. "+ fileSystemScheme + "Path= " + fullPath);

        // Create parent folders
        createParents(auditPath, fileSystem);

        currentFileName = fullPath;

        if (logger.isDebugEnabled()) {
            logger.debug("<== AbstractRangerAuditWriter.createFileSystemFolders()");
        }
    }

    public  Configuration createConfiguration() {
        Configuration conf = new Configuration();
        for (Map.Entry<String, String> entry : auditConfigs.entrySet()) {
            String key   = entry.getKey();
            String value = entry.getValue();
            // for ease of install config file may contain properties with empty value, skip those
            if (StringUtils.isNotEmpty(value)) {
                conf.set(key, value);
            }
            logger.info("Adding property to "+ fileSystemScheme + " + config: " + key + " => " + value);
        }

        logger.info("Returning " + fileSystemScheme + "Filesystem Config: " + conf.toString());
        return conf;
    }

    public void createParents(Path pathLogfile, FileSystem fileSystem)
            throws Exception {
        logger.info("Creating parent folder for " + pathLogfile);
        Path parentPath = pathLogfile != null ? pathLogfile.getParent() : null;

        if (parentPath != null && fileSystem != null
                && !fileSystem.exists(parentPath)) {
            fileSystem.mkdirs(parentPath);
        }
    }

    public void init(Properties props, String propPrefix) {

        if (logger.isDebugEnabled()) {
            logger.debug("==> AbstractRangerAuditWriter.init()");
        }

        String logFolderProp = MiscUtil.getStringProperty(props, propPrefix	+ "." + PROP_FILESYSTEM_DIR);
        if (StringUtils.isEmpty(logFolderProp)) {
            logger.error("File destination folder is not configured. Please set "
                    + propPrefix + "."
                    + PROP_FILESYSTEM_DIR + ". name="
                    + auditProviderName);
            return;
        }

        String logSubFolder = MiscUtil.getStringProperty(props, propPrefix + "." + PROP_FILESYSTEM_SUBDIR);
        if (StringUtils.isEmpty(logSubFolder)) {
            logSubFolder = "%app-type%/%time:yyyyMMdd%";
        }

        logFileNameFormat = MiscUtil.getStringProperty(props, propPrefix + "." + PROP_FILESYSTEM_FILE_NAME_FORMAT);
        fileRolloverSec   = MiscUtil.getIntProperty(props, propPrefix + "." + PROP_FILESYSTEM_FILE_ROLLOVER, fileRolloverSec);

        if (StringUtils.isEmpty(fileExtension)) {
            setFileExtension(PROP_FILESYSTEM_FILE_EXTENSION);
        }

        if (logFileNameFormat == null || logFileNameFormat.isEmpty()) {
            logFileNameFormat = "%app-type%_ranger_audit_%hostname%" + fileExtension;
        }

        logFolder = logFolderProp + "/" + logSubFolder;

        logger.info("logFolder=" + logFolder + ", destName=" + auditProviderName);
        logger.info("logFileNameFormat=" + logFileNameFormat + ", destName="+ auditProviderName);
        logger.info("config=" + auditConfigs.toString());

        rolloverPeriod  = MiscUtil.getStringProperty(props, propPrefix + "." + PROP_FILESYSTEM_ROLLOVER_PERIOD);
        rollingTimeUtil = RollingTimeUtil.getInstance();

        //file.rollover.period is used for rolling over. If it could compute the next roll over time using file.rollover.period
        //it fall back to use file.rollover.sec for find next rollover time. If still couldn't find default will be 1day window
        //for rollover.
        if(StringUtils.isEmpty(rolloverPeriod) ) {
            rolloverPeriod = rollingTimeUtil.convertRolloverSecondsToRolloverPeriod(fileRolloverSec);
        }

        try {
            nextRollOverTime = rollingTimeUtil.computeNextRollingTime(rolloverPeriod);
        } catch ( Exception e) {
            logger.warn("Rollover by file.rollover.period failed...will be using the file.rollover.sec for "+ fileSystemScheme + " audit file rollover...", e);
            rollOverByDuration = true;
            nextRollOverTime   = rollOverByDuration();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== AbstractRangerAuditWriter.init()");
        }

    }

    public void closeFileIfNeeded() throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> AbstractRangerAuditWriter.closeFileIfNeeded()");
        }

        if (logWriter == null) {
            return;
        }

        if ( System.currentTimeMillis() >= nextRollOverTime.getTime() ) {
            logger.info("Closing file. Rolling over. name=" + auditProviderName
                    + ", fileName=" + currentFileName);
            try {
                logWriter.flush();
                logWriter.close();
            } catch (Throwable t) {
                logger.error("Error on closing log writter. Exception will be ignored. name="
                        + auditProviderName + ", fileName=" + currentFileName);
            }

            logWriter = null;
            ostream   = null;
            currentFileName = null;

            if (!rollOverByDuration) {
                try {
                    if(StringUtils.isEmpty(rolloverPeriod) ) {
                        rolloverPeriod = rollingTimeUtil.convertRolloverSecondsToRolloverPeriod(fileRolloverSec);
                    }
                    nextRollOverTime = rollingTimeUtil.computeNextRollingTime(rolloverPeriod);
                } catch ( Exception e) {
                    logger.warn("Rollover by file.rollover.period failed...will be using the file.rollover.sec for " + fileSystemScheme + " audit file rollover...", e);
                    nextRollOverTime = rollOverByDuration();
                }
            } else {
                nextRollOverTime = rollOverByDuration();
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== AbstractRangerAuditWriter.closeFileIfNeeded()");
        }
    }

    public   Date rollOverByDuration() {
        long rollOverTime = rollingTimeUtil.computeNextRollingTime(fileRolloverSec,nextRollOverTime);
        return new Date(rollOverTime);
    }

    public PrintWriter createWriter() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("==> AbstractRangerAuditWriter.createWriter()");
        }

        if (logWriter == null) {
            // Create the file to write
            logger.info("Creating new log file. auditPath=" + fullPath);
            createFileSystemFolders();
            ostream               = fileSystem.create(auditPath);
            logWriter             = new PrintWriter(ostream);
            isHFlushCapableStream = ostream.hasCapability(StreamCapabilities.HFLUSH);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== AbstractRangerAuditWriter.createWriter()");
        }

        return logWriter;
    }

    public void closeWriter() {
        if (logger.isDebugEnabled()) {
            logger.debug("==> AbstractRangerAuditWriter.closeWriter()");
        }

        logWriter = null;
        ostream = null;

        if (logger.isDebugEnabled()) {
            logger.debug("<== AbstractRangerAuditWriter.closeWriter()");
        }
    }

    @Override
    public void flush() {
        if (logger.isDebugEnabled()) {
            logger.debug("==> AbstractRangerAuditWriter.flush()");
        }
        if (ostream != null) {
            try {
                synchronized (this) {
                    if (ostream != null)
                        // 1) PrinterWriter does not have bufferring of its own so
                        // we need to flush its underlying stream
                        // 2) HDFS flush() does not really flush all the way to disk.
                        if (isHFlushCapableStream) {
                            //Checking HFLUSH capability of the stream because of HADOOP-13327.
                            //For S3 filesysttem, hflush throws UnsupportedOperationException and hence we call flush.
                            ostream.hflush();
                        } else {
                            ostream.flush();
                        }                    logger.info("Flush " + fileSystemScheme + " audit logs completed.....");
                }
            } catch (IOException e) {
                logger.error("Error on flushing log writer: " + e.getMessage() +
                        "\nException will be ignored. name=" + auditProviderName + ", fileName=" + currentFileName);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("<== AbstractRangerAuditWriter.flush()");
        }
    }

    public boolean logFileToHDFS(File file) throws Exception {
        boolean ret = false;
        if (logger.isDebugEnabled()) {
            logger.debug("==> AbstractRangerAuditWriter.logFileToHDFS()");
        }

        if (logWriter == null) {
            // Create the file to write
            createFileSystemFolders();
            logger.info("Copying the Audit File" + file.getName() + " to HDFS Path" + fullPath);
            Path destPath = new Path(fullPath);
            ret = FileUtil.copy(file,fileSystem,destPath,false,conf);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== AbstractRangerAuditWriter.logFileToHDFS()");
        }
        return ret;
    }

    public String getFileSystemScheme() {
        String ret = null;
        ret  = logFolder.substring(0, (logFolder.indexOf(":")));
        ret  = ret.toUpperCase();
        return ret;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension  = fileExtension;
    }
}
