package org.apache.ranger.tagsync.process;

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
import java.io.File;

import org.apache.ranger.plugin.util.RangerMetricsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagSyncMetricsProducer implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(TagSyncMetricsProducer.class);
	private boolean shutdownFlag = false;

	public static void main(String[] args) {
		TagSyncMetricsProducer tagSyncMetrics = new TagSyncMetricsProducer();
		tagSyncMetrics.run();
		// try { tagSyncMetrics.writeJVMMetrics(); } catch (Throwable e) { }
	}

	@Override
	public void run() {
		try {
			TagSyncConfig config = TagSyncConfig.getInstance();
			long sleepTimeBetweenCycleInMillis = config.getTagSyncMetricsFrequency();
			String logFileNameWithPath = config.getTagSyncMetricsFileName();
			LOG.info("Tagsync metrics frequency :  " + sleepTimeBetweenCycleInMillis +" and metrics file : "+logFileNameWithPath );
			if (null != logFileNameWithPath) {
				while (!shutdownFlag) {
					try {
						if (LOG.isDebugEnabled()) {
							LOG.debug("Sleeping Tagsync metrics for [" + sleepTimeBetweenCycleInMillis
									+ "] milliSeconds");
						}
						Thread.sleep(sleepTimeBetweenCycleInMillis);
					} catch (InterruptedException e) {
						LOG.error("Failed to wait for [" + sleepTimeBetweenCycleInMillis
								+ "] milliseconds before attempting to tagsync metrics information", e);
					}
					try {
						writeJVMMetrics(logFileNameWithPath);
					} catch (Throwable t) {
						LOG.error("Failed to write tagsync metrics into file. Error details: ", t);
					}
				}
			} else {
				LOG.info("No file directory found for tagsync metrics log ");
			}
		} catch (Throwable t) {
			LOG.error("Failed to start Tagsync metrics. Error details: ", t);
		} finally {
			LOG.info("Shutting down the TagSync metrics thread");
		}

	}

	private void writeJVMMetrics(String logFileNameWithPath) throws Throwable {
		try {
			File userMetricFile = null;
			userMetricFile = new File(logFileNameWithPath);
			if (!userMetricFile.exists()) {
				userMetricFile.createNewFile();
			}
			RangerMetricsUtil rangerMetricsUtil = new RangerMetricsUtil();
			rangerMetricsUtil.writeMetricsToFile(userMetricFile);

		} catch (Throwable t) {
			LOG.error("TagSyncMetricsProducer.writeJVMMetrics() failed to write metrics into file. Error details: ", t);
			throw t;
		}
	}

}
