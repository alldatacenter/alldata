/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ranger.audit.provider.hdfs;

import java.util.Map;
import java.util.Properties;

import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.provider.BufferedAuditProvider;
import org.apache.ranger.audit.provider.DebugTracer;
import org.apache.ranger.audit.provider.LocalFileLogBuffer;
import org.apache.ranger.audit.provider.Log4jTracer;
import org.apache.ranger.audit.provider.MiscUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HdfsAuditProvider extends BufferedAuditProvider {
	private static final Logger LOG = LoggerFactory.getLogger(HdfsAuditProvider.class);

	public static final String AUDIT_HDFS_IS_ASYNC_PROP           = "xasecure.audit.hdfs.is.async";
	public static final String AUDIT_HDFS_MAX_QUEUE_SIZE_PROP     = "xasecure.audit.hdfs.async.max.queue.size";
	public static final String AUDIT_HDFS_MAX_FLUSH_INTERVAL_PROP = "xasecure.audit.hdfs.async.max.flush.interval.ms";

	public HdfsAuditProvider() {
	}

	public void init(Properties props) {
		LOG.info("HdfsAuditProvider.init()");

		super.init(props);

		Map<String, String> hdfsProps = MiscUtil.getPropertiesWithPrefix(props, "xasecure.audit.hdfs.config.");

		String encoding                                = hdfsProps.get("encoding");

		String hdfsDestinationDirectory                = hdfsProps.get("destination.directory");
		String hdfsDestinationFile                     = hdfsProps.get("destination.file");
		int    hdfsDestinationFlushIntervalSeconds     = MiscUtil.parseInteger(hdfsProps.get("destination.flush.interval.seconds"), 15 * 60);
		int    hdfsDestinationRolloverIntervalSeconds  = MiscUtil.parseInteger(hdfsProps.get("destination.rollover.interval.seconds"), 24 * 60 * 60);
		int    hdfsDestinationOpenRetryIntervalSeconds = MiscUtil.parseInteger(hdfsProps.get("destination.open.retry.interval.seconds"), 60);

		String localFileBufferDirectory               = hdfsProps.get("local.buffer.directory");
		String localFileBufferFile                    = hdfsProps.get("local.buffer.file");
		int    localFileBufferFlushIntervalSeconds    = MiscUtil.parseInteger(hdfsProps.get("local.buffer.flush.interval.seconds"), 1 * 60);
		int    localFileBufferFileBufferSizeBytes     = MiscUtil.parseInteger(hdfsProps.get("local.buffer.file.buffer.size.bytes"), 8 * 1024);
		int    localFileBufferRolloverIntervalSeconds = MiscUtil.parseInteger(hdfsProps.get("local.buffer.rollover.interval.seconds"), 10 * 60);
		String localFileBufferArchiveDirectory        = hdfsProps.get("local.archive.directory");
		int    localFileBufferArchiveFileCount        = MiscUtil.parseInteger(hdfsProps.get("local.archive.max.file.count"), 10);
		// Added for Azure.  Note that exact name of these properties is not known as it contains the variable account name in it.
		Map<String, String> configProps = MiscUtil.getPropertiesWithPrefix(props, "xasecure.audit.destination.hdfs.config.");

		DebugTracer tracer = new Log4jTracer(LOG);

		HdfsLogDestination<AuditEventBase> mHdfsDestination = new HdfsLogDestination<AuditEventBase>(tracer);

		mHdfsDestination.setDirectory(hdfsDestinationDirectory);
		mHdfsDestination.setFile(hdfsDestinationFile);
		mHdfsDestination.setFlushIntervalSeconds(hdfsDestinationFlushIntervalSeconds);
		mHdfsDestination.setEncoding(encoding);
		mHdfsDestination.setRolloverIntervalSeconds(hdfsDestinationRolloverIntervalSeconds);
		mHdfsDestination.setOpenRetryIntervalSeconds(hdfsDestinationOpenRetryIntervalSeconds);
		mHdfsDestination.setConfigProps(configProps);

		LocalFileLogBuffer<AuditEventBase> mLocalFileBuffer = new LocalFileLogBuffer<AuditEventBase>(tracer);

		mLocalFileBuffer.setDirectory(localFileBufferDirectory);
		mLocalFileBuffer.setFile(localFileBufferFile);
		mLocalFileBuffer.setFlushIntervalSeconds(localFileBufferFlushIntervalSeconds);
		mLocalFileBuffer.setFileBufferSizeBytes(localFileBufferFileBufferSizeBytes);
		mLocalFileBuffer.setEncoding(encoding);
		mLocalFileBuffer.setRolloverIntervalSeconds(localFileBufferRolloverIntervalSeconds);
		mLocalFileBuffer.setArchiveDirectory(localFileBufferArchiveDirectory);
		mLocalFileBuffer.setArchiveFileCount(localFileBufferArchiveFileCount);
		
		setBufferAndDestination(mLocalFileBuffer, mHdfsDestination);
	}
}



