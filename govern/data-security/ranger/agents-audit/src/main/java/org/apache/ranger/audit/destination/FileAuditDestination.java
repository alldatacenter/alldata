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

package org.apache.ranger.audit.destination;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.provider.MiscUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class write the logs to local file
 */
public class FileAuditDestination extends AuditDestination {
	private static final Logger logger = LoggerFactory
			.getLogger(FileAuditDestination.class);

	public static final String PROP_FILE_LOCAL_DIR = "dir";
	public static final String PROP_FILE_LOCAL_FILE_NAME_FORMAT = "filename.format";
	public static final String PROP_FILE_FILE_ROLLOVER = "file.rollover.sec";

	String baseFolder = null;
	String fileFormat = null;
	int fileRolloverSec = 24 * 60 * 60; // In seconds
	private String logFileNameFormat;

	boolean initDone = false;

	private File logFolder;
	PrintWriter logWriter = null;

	private Date fileCreateTime = null;

	private String currentFileName;

	private boolean isStopped = false;

	@Override
	public void init(Properties prop, String propPrefix) {
		super.init(prop, propPrefix);

		// Initialize properties for this class
		// Initial folder and file properties
		String logFolderProp = MiscUtil.getStringProperty(props, propPrefix
				+ "." + PROP_FILE_LOCAL_DIR);
		logFileNameFormat = MiscUtil.getStringProperty(props, propPrefix + "."
				+ PROP_FILE_LOCAL_FILE_NAME_FORMAT);
		fileRolloverSec = MiscUtil.getIntProperty(props, propPrefix + "."
				+ PROP_FILE_FILE_ROLLOVER, fileRolloverSec);

		if (logFolderProp == null || logFolderProp.isEmpty()) {
			logger.error("File destination folder is not configured. Please set "
					+ propPrefix
					+ "."
					+ PROP_FILE_LOCAL_DIR
					+ ". name="
					+ getName());
			return;
		}
		logFolder = new File(logFolderProp);
		if (!logFolder.isDirectory()) {
			logFolder.mkdirs();
			if (!logFolder.isDirectory()) {
				logger.error("FileDestination folder not found and can't be created. folder="
						+ logFolder.getAbsolutePath() + ", name=" + getName());
				return;
			}
		}
		logger.info("logFolder=" + logFolder + ", name=" + getName());

		if (logFileNameFormat == null || logFileNameFormat.isEmpty()) {
			logFileNameFormat = "%app-type%_ranger_audit.log";
		}

		logger.info("logFileNameFormat=" + logFileNameFormat + ", destName="
				+ getName());

		initDone = true;
	}

	@Override
	synchronized public boolean logJSON(Collection<String> events) {
		logStatusIfRequired();
		addTotalCount(events.size());

		if (isStopped) {
			logError("log() called after stop was requested. name=" + getName());
			addDeferredCount(events.size());
			return false;
		}

		try {
			PrintWriter out = getLogFileStream();
			for (String event : events) {
				out.println(event);
			}
			out.flush();
		} catch (Throwable t) {
			addDeferredCount(events.size());
			logError("Error writing to log file.", t);
			return false;
		}
		addSuccessCount(events.size());
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.apache.ranger.audit.provider.AuditProvider#log(java.util.Collection)
	 */
	@Override
	public boolean log(Collection<AuditEventBase> events) {
		if (isStopped) {
			addTotalCount(events.size());
			addDeferredCount(events.size());
			logError("log() called after stop was requested. name=" + getName());
			return false;
		}
		List<String> jsonList = new ArrayList<String>();
		for (AuditEventBase event : events) {
			try {
				jsonList.add(MiscUtil.stringify(event));
			} catch (Throwable t) {
				addTotalCount(1);
				addFailedCount(1);
				logFailedEvent(event);
				logger.error("Error converting to JSON. event=" + event);
			}
		}
		return logJSON(jsonList);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#start()
	 */
	@Override
	public void start() {
		// Nothing to do here. We will open the file when the first log request
		// comes
	}

	@Override
	synchronized public void stop() {
		isStopped = true;
		if (logWriter != null) {
			try {
				logWriter.flush();
				logWriter.close();
			} catch (Throwable t) {
				logger.error("Error on closing log writter. Exception will be ignored. name="
						+ getName() + ", fileName=" + currentFileName);
			}
			logWriter = null;
		}
		logStatus();
	}

	// Helper methods in this class
	synchronized private PrintWriter getLogFileStream() throws Exception {
		closeFileIfNeeded();

		// Either there are no open log file or the previous one has been rolled
		// over
		if (logWriter == null) {
			Date currentTime = new Date();
			// Create a new file
			String fileName = MiscUtil.replaceTokens(logFileNameFormat,
					currentTime.getTime());
			File outLogFile = new File(logFolder, fileName);
			if (outLogFile.exists()) {
				// Let's try to get the next available file
				int i = 0;
				while (true) {
					i++;
					int lastDot = fileName.lastIndexOf('.');
					String baseName = fileName.substring(0, lastDot);
					String extension = fileName.substring(lastDot);
					String newFileName = baseName + "." + i + extension;
					File newLogFile = new File(logFolder, newFileName);
					if (!newLogFile.exists()) {
						// Move the file
						if (!outLogFile.renameTo(newLogFile)) {
							logger.error("Error renameing file. " + outLogFile
									+ " to " + newLogFile);
						}
						break;
					}
				}
			}
			if (!outLogFile.exists()) {
				logger.info("Creating new file. destName=" + getName()
						+ ", fileName=" + fileName);
				// Open the file
				logWriter = new PrintWriter(new BufferedWriter(new FileWriter(
						outLogFile)));
			} else {
				logWriter = new PrintWriter(new BufferedWriter(new FileWriter(
						outLogFile, true)));
			}
			fileCreateTime = new Date();
			currentFileName = outLogFile.getPath();
		}
		return logWriter;
	}

	private void closeFileIfNeeded() {
		if (logWriter == null) {
			return;
		}
		if (System.currentTimeMillis() - fileCreateTime.getTime() > fileRolloverSec * 1000) {
			logger.info("Closing file. Rolling over. name=" + getName()
					+ ", fileName=" + currentFileName);
			try {
				logWriter.flush();
				logWriter.close();
			} catch (Throwable t) {
				logger.error("Error on closing log writter. Exception will be ignored. name="
						+ getName() + ", fileName=" + currentFileName);
			}
			logWriter = null;
			currentFileName = null;
		}
	}

}
