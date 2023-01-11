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

package org.apache.ranger.audit;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.ranger.audit.destination.FileAuditDestination;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.audit.provider.AuditHandler;
import org.apache.ranger.audit.provider.AuditProviderFactory;
import org.apache.ranger.audit.provider.BaseAuditHandler;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.audit.provider.MultiDestAuditProvider;
import org.apache.ranger.audit.queue.AuditAsyncQueue;
import org.apache.ranger.audit.queue.AuditBatchQueue;
import org.apache.ranger.audit.queue.AuditFileSpool;
import org.apache.ranger.audit.queue.AuditQueue;
import org.apache.ranger.audit.queue.AuditSummaryQueue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAuditQueue {

	private static final Logger logger = LoggerFactory.getLogger(TestAuditQueue.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	static private int seqNum = 0;

	@Test
	public void testAuditAsyncQueue() {
		logger.debug("testAuditAsyncQueue()...");
		TestConsumer testConsumer = new TestConsumer();
		AuditAsyncQueue queue = new AuditAsyncQueue(testConsumer);
		Properties props = new Properties();
		queue.init(props);

		queue.start();

		int messageToSend = 10;
		for (int i = 0; i < messageToSend; i++) {
			queue.log(createEvent());
		}
		queue.stop();
		queue.waitToComplete();
		// Let's wait for second
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignore
		}
		assertEquals(messageToSend, testConsumer.getCountTotal());
		assertEquals(messageToSend, testConsumer.getSumTotal());
		assertNull("Event not in sequnce", testConsumer.isInSequence());
	}

	@Test
	public void testAuditSummaryQueue() {
		logger.debug("testAuditSummaryQueue()...");
		TestConsumer testConsumer = new TestConsumer();
		AuditSummaryQueue queue = new AuditSummaryQueue(testConsumer);

		Properties props = new Properties();
		props.put(BaseAuditHandler.PROP_DEFAULT_PREFIX + "."
				+ AuditSummaryQueue.PROP_SUMMARY_INTERVAL, "" + 300);
		queue.init(props, BaseAuditHandler.PROP_DEFAULT_PREFIX);

		queue.start();

		commonTestSummary(testConsumer, queue);
	}

	private void commonTestSummary(TestConsumer testConsumer,
			BaseAuditHandler queue) {
		int messageToSend = 0;
		int pauseMS = 330;

		int countToCheck = 0;
		try {

			queue.log(createEvent("john", "select",
					"xademo/customer_details/imei", true));
			messageToSend++;
			queue.log(createEvent("john", "select",
					"xademo/customer_details/imei", true));
			messageToSend++;
			countToCheck++;
			queue.log(createEvent("jane", "select",
					"xademo/customer_details/imei", true));
			messageToSend++;
			countToCheck++;
			Thread.sleep(pauseMS);

			queue.log(createEvent("john", "select",
					"xademo/customer_details/imei", true));
			messageToSend++;
			queue.log(createEvent("john", "select",
					"xademo/customer_details/imei", true));
			messageToSend++;
			countToCheck++;
			queue.log(createEvent("jane", "select",
					"xademo/customer_details/imei", true));
			messageToSend++;
			countToCheck++;
			Thread.sleep(pauseMS);

			queue.log(createEvent("john", "select",
					"xademo/customer_details/imei", true));
			messageToSend++;
			countToCheck++;
			queue.log(createEvent("john", "select",
					"xademo/customer_details/imei", false));
			messageToSend++;
			countToCheck++;
			queue.log(createEvent("jane", "select",
					"xademo/customer_details/imei", true));
			messageToSend++;
			countToCheck++;
			Thread.sleep(pauseMS);

		} catch (InterruptedException e1) {
			logger.error("Sleep interupted", e1);
		}
		// Let's wait for second
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignore
		}

		queue.waitToComplete();
		queue.stop();
		queue.waitToComplete();
		// Let's wait for second
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignore
		}
		assertEquals(messageToSend, testConsumer.getSumTotal());
		assertEquals(countToCheck, testConsumer.getCountTotal());
	}

	@Test
	public void testAuditSummaryByInfra() {
		logger.debug("testAuditSummaryByInfra()...");

		Properties props = new Properties();
		// Destination
		String propPrefix = AuditProviderFactory.AUDIT_DEST_BASE + ".test";
		props.put(propPrefix, "enable");
		props.put(BaseAuditHandler.PROP_DEFAULT_PREFIX + "." + "summary" + "."
				+ "enabled", "true");
		props.put(propPrefix + "." + BaseAuditHandler.PROP_NAME, "test");
		props.put(propPrefix + "." + AuditQueue.PROP_QUEUE, "none");

		props.put(BaseAuditHandler.PROP_DEFAULT_PREFIX + "."
				+ AuditSummaryQueue.PROP_SUMMARY_INTERVAL, "" + 300);
		props.put(propPrefix + "." + BaseAuditHandler.PROP_CLASS_NAME,
				TestConsumer.class.getName());

		AuditProviderFactory factory = AuditProviderFactory.getInstance();
		factory.init(props, "test");
		AuditQueue queue = (AuditQueue) factory.getAuditProvider();
		BaseAuditHandler consumer = (BaseAuditHandler) queue.getConsumer();
		while (consumer != null && consumer instanceof AuditQueue) {
			AuditQueue cQueue = (AuditQueue) consumer;
			consumer = (BaseAuditHandler) cQueue.getConsumer();
		}
		assertTrue("Consumer should be TestConsumer. class="
				+ consumer.getClass().getName(),
				consumer instanceof TestConsumer);
		TestConsumer testConsumer = (TestConsumer) consumer;
		commonTestSummary(testConsumer, queue);
	}

	@Test
	public void testMultipleQueue() {
		logger.debug("testAuditAsyncQueue()...");
		int destCount = 3;
		TestConsumer[] testConsumer = new TestConsumer[destCount];

		MultiDestAuditProvider multiQueue = new MultiDestAuditProvider();
		for (int i = 0; i < destCount; i++) {
			testConsumer[i] = new TestConsumer();
			multiQueue.addAuditProvider(testConsumer[i]);
		}

		AuditAsyncQueue queue = new AuditAsyncQueue(multiQueue);
		Properties props = new Properties();
		queue.init(props);
		queue.start();

		int messageToSend = 10;
		for (int i = 0; i < messageToSend; i++) {
			queue.log(createEvent());
		}
		queue.stop();
		queue.waitToComplete();
		// Let's wait for second
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignore
		}
		for (int i = 0; i < destCount; i++) {
			assertEquals("consumer" + i, messageToSend,
					testConsumer[i].getCountTotal());
			assertEquals("consumer" + i, messageToSend,
					testConsumer[i].getSumTotal());

		}
	}

	@Test
	public void testAuditBatchQueueBySize() {
		logger.debug("testAuditBatchQueue()...");
		int messageToSend = 10;

		String basePropName = "testAuditBatchQueueBySize_"
				+ MiscUtil.generateUniqueId();
		int batchSize = messageToSend / 3;
		int expectedBatchSize = batchSize
				+ (batchSize * 3 < messageToSend ? 1 : 0);
		int queueSize = messageToSend * 2;
		int intervalMS = messageToSend * 100; // Deliberately big interval
		Properties props = new Properties();
		props.put(basePropName + "." + AuditQueue.PROP_BATCH_SIZE, ""
				+ batchSize);
		props.put(basePropName + "." + AuditQueue.PROP_QUEUE_SIZE, ""
				+ queueSize);
		props.put(basePropName + "." + AuditQueue.PROP_BATCH_INTERVAL, ""
				+ intervalMS);

		TestConsumer testConsumer = new TestConsumer();
		AuditBatchQueue queue = new AuditBatchQueue(testConsumer);
		queue.init(props, basePropName);
		queue.start();

		for (int i = 0; i < messageToSend; i++) {
			queue.log(createEvent());

		}
		// Let's wait for second
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}

		queue.waitToComplete();
		queue.stop();
		queue.waitToComplete();

		assertEquals("Total count", messageToSend, testConsumer.getCountTotal());
		assertEquals("Total sum", messageToSend, testConsumer.getSumTotal());
		assertEquals("Total batch", expectedBatchSize,
				testConsumer.getBatchCount());
		assertNull("Event not in sequnce", testConsumer.isInSequence());

	}

	@Test
	public void testAuditBatchQueueByTime() {
		logger.debug("testAuditBatchQueue()...");

		int messageToSend = 10;

		String basePropName = "testAuditBatchQueueByTime_"
				+ MiscUtil.generateUniqueId();
		int batchSize = messageToSend * 2; // Deliberately big size
		int queueSize = messageToSend * 2;
		int intervalMS = (1000 / messageToSend) * 3; // e.g (1000/10 * 3) = 300
														// ms
		int pauseMS = 1000 / messageToSend + 3; // e.g. 1000/10 + 3 = 103ms
		int expectedBatchSize = (messageToSend * pauseMS) / intervalMS + 1;

		Properties props = new Properties();
		props.put(basePropName + "." + AuditQueue.PROP_BATCH_SIZE, ""
				+ batchSize);
		props.put(basePropName + "." + AuditQueue.PROP_QUEUE_SIZE, ""
				+ queueSize);
		props.put(basePropName + "." + AuditQueue.PROP_BATCH_INTERVAL, ""
				+ intervalMS);

		TestConsumer testConsumer = new TestConsumer();
		AuditBatchQueue queue = new AuditBatchQueue(testConsumer);
		queue.init(props, basePropName);
		queue.start();

		for (int i = 0; i < messageToSend; i++) {
			queue.log(createEvent());
			try {
				Thread.sleep(pauseMS);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		// Let's wait for second
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}
		queue.waitToComplete();
		queue.stop();
		queue.waitToComplete();

		assertEquals("Total count", messageToSend, testConsumer.getCountTotal());
		assertEquals("Total sum", messageToSend, testConsumer.getSumTotal());
		assertEquals("Total batch", expectedBatchSize,
				testConsumer.getBatchCount());
		assertNull("Event not in sequnce", testConsumer.isInSequence());
	}

	@Test
	public void testAuditBatchQueueDestDown() {
		logger.debug("testAuditBatchQueueDestDown()...");
		int messageToSend = 10;

		String basePropName = "testAuditBatchQueueDestDown_"
				+ MiscUtil.generateUniqueId();
		int batchSize = messageToSend / 3;
		int queueSize = messageToSend * 2;
		int intervalMS = Integer.MAX_VALUE; // Deliberately big interval
		Properties props = new Properties();
		props.put(basePropName + "." + BaseAuditHandler.PROP_NAME,
				"testAuditBatchQueueDestDown");

		props.put(basePropName + "." + AuditQueue.PROP_BATCH_SIZE, ""
				+ batchSize);
		props.put(basePropName + "." + AuditQueue.PROP_QUEUE_SIZE, ""
				+ queueSize);
		props.put(basePropName + "." + AuditQueue.PROP_BATCH_INTERVAL, ""
				+ intervalMS);

		// Enable File Spooling
		props.put(basePropName + "." + "filespool.enable", "" + true);
		props.put(basePropName + "." + "filespool.dir", "target");

		TestConsumer testConsumer = new TestConsumer();
		testConsumer.isDown = true;

		AuditBatchQueue queue = new AuditBatchQueue(testConsumer);
		queue.init(props, basePropName);
		queue.start();

		for (int i = 0; i < messageToSend; i++) {
			queue.log(createEvent());

		}
		// Let's wait for second
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}

		queue.waitToComplete(5000);
		queue.stop();
		queue.waitToComplete();

		assertEquals("Total count", 0, testConsumer.getCountTotal());
		assertEquals("Total sum", 0, testConsumer.getSumTotal());
		assertEquals("Total batch", 0, testConsumer.getBatchCount());
		assertNull("Event not in sequnce", testConsumer.isInSequence());
	}

	@Test
	public void testAuditBatchQueueDestDownFlipFlop() {
		logger.debug("testAuditBatchQueueDestDownFlipFlop()...");
		int messageToSend = 10;

		String basePropName = "testAuditBatchQueueDestDownFlipFlop_"
				+ MiscUtil.generateUniqueId();
		int batchSize = messageToSend / 3;
		int queueSize = messageToSend * 2;
		int intervalMS = 5000; // Deliberately big interval
		Properties props = new Properties();
		props.put(
				basePropName + "." + BaseAuditHandler.PROP_NAME,
				"testAuditBatchQueueDestDownFlipFlop_"
						+ MiscUtil.generateUniqueId());

		props.put(basePropName + "." + AuditQueue.PROP_BATCH_SIZE, ""
				+ batchSize);
		props.put(basePropName + "." + AuditQueue.PROP_QUEUE_SIZE, ""
				+ queueSize);
		props.put(basePropName + "." + AuditQueue.PROP_BATCH_INTERVAL, ""
				+ intervalMS);

		// Enable File Spooling
		int destRetryMS = 10;
		props.put(basePropName + "." + AuditQueue.PROP_FILE_SPOOL_ENABLE,
				"" + true);
		props.put(
				basePropName + "." + AuditFileSpool.PROP_FILE_SPOOL_LOCAL_DIR,
				"target");
		props.put(basePropName + "."
				+ AuditFileSpool.PROP_FILE_SPOOL_DEST_RETRY_MS, ""
				+ destRetryMS);

		TestConsumer testConsumer = new TestConsumer();
		testConsumer.isDown = false;

		AuditBatchQueue queue = new AuditBatchQueue(testConsumer);
		queue.init(props, basePropName);
		queue.start();

		try {
			queue.log(createEvent());
			queue.log(createEvent());
			queue.log(createEvent());
			Thread.sleep(1000);
			testConsumer.isDown = true;
			Thread.sleep(1000);
			queue.log(createEvent());
			queue.log(createEvent());
			queue.log(createEvent());
			Thread.sleep(1000);
			testConsumer.isDown = false;
			Thread.sleep(1000);
			queue.log(createEvent());
			queue.log(createEvent());
			queue.log(createEvent());
			Thread.sleep(1000);
			testConsumer.isDown = true;
			Thread.sleep(1000);
			queue.log(createEvent());
			Thread.sleep(1000);
			testConsumer.isDown = false;
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignore
		}
		// Let's wait for second
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}

		queue.waitToComplete(5000);
		queue.stop();
		queue.waitToComplete();

		assertEquals("Total count", messageToSend, testConsumer.getCountTotal());
		assertEquals("Total sum", messageToSend, testConsumer.getSumTotal());
		assertNull("Event not in sequnce", testConsumer.isInSequence());

	}

	/**
	 * See if we recover after restart
	 */
	@Test
	public void testAuditBatchQueueDestDownRestart() {
		logger.debug("testAuditBatchQueueDestDownRestart()...");
		int messageToSend = 10;

		String basePropName = "testAuditBatchQueueDestDownRestart_"
				+ MiscUtil.generateUniqueId();
		int batchSize = messageToSend / 3;
		int queueSize = messageToSend * 2;
		int intervalMS = 3000; // Deliberately big interval
		int maxArchivedFiles = 1;
		Properties props = new Properties();
		props.put(
				basePropName + "." + BaseAuditHandler.PROP_NAME,
				"testAuditBatchQueueDestDownRestart_"
						+ MiscUtil.generateUniqueId());

		props.put(basePropName + "." + AuditQueue.PROP_BATCH_SIZE, ""
				+ batchSize);
		props.put(basePropName + "." + AuditQueue.PROP_QUEUE_SIZE, ""
				+ queueSize);
		props.put(basePropName + "." + AuditQueue.PROP_BATCH_INTERVAL, ""
				+ intervalMS);

		// Enable File Spooling
		int destRetryMS = 10;
		props.put(basePropName + "." + AuditQueue.PROP_FILE_SPOOL_ENABLE,
				"" + true);
		props.put(
				basePropName + "." + AuditFileSpool.PROP_FILE_SPOOL_LOCAL_DIR,
				"target");
		props.put(basePropName + "."
				+ AuditFileSpool.PROP_FILE_SPOOL_DEST_RETRY_MS, ""
				+ destRetryMS);
		props.put(basePropName + "."
				+ AuditFileSpool.PROP_FILE_SPOOL_ARCHIVE_MAX_FILES_COUNT, ""
				+ maxArchivedFiles);

		TestConsumer testConsumer = new TestConsumer();
		testConsumer.isDown = true;

		AuditBatchQueue queue = new AuditBatchQueue(testConsumer);
		queue.init(props, basePropName);
		queue.start();

		for (int i = 0; i < messageToSend; i++) {
			queue.log(createEvent());

		}
		// Let's wait for second or two
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}

		queue.waitToComplete(5000);
		queue.stop();
		queue.waitToComplete();

		testConsumer.isDown = true;

		// Let's wait for second or two
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// ignore
		}

		// Let's now recreate the objects
		testConsumer = new TestConsumer();

		queue = new AuditBatchQueue(testConsumer);
		queue.init(props, basePropName);
		queue.start();

		// Let's wait for second
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}

		queue.waitToComplete(5000);
		queue.stop();
		queue.waitToComplete();

		assertEquals("Total count", messageToSend, testConsumer.getCountTotal());
		assertEquals("Total sum", messageToSend, testConsumer.getSumTotal());
		assertNull("Event not in sequnce", testConsumer.isInSequence());

	}

	@Test
	public void testFileDestination() {
		logger.debug("testFileDestination()...");

		int messageToSend = 10;
		int batchSize = messageToSend / 3;
		int queueSize = messageToSend * 2;
		int intervalMS = 500; // Should be less than final sleep time

		String logFolderName = "target/testFileDestination";
		File logFolder = new File(logFolderName);
		String logFileName = "test_ranger_audit.log";
		File logFile = new File(logFolder, logFileName);

		Properties props = new Properties();
		// Destination
		String filePropPrefix = AuditProviderFactory.AUDIT_DEST_BASE + ".file";
		props.put(filePropPrefix, "enable");
		props.put(filePropPrefix + "." + AuditQueue.PROP_NAME, "file");
		props.put(filePropPrefix + "."
				+ FileAuditDestination.PROP_FILE_LOCAL_DIR, logFolderName);
		props.put(filePropPrefix + "."
				+ FileAuditDestination.PROP_FILE_LOCAL_FILE_NAME_FORMAT,
				"%app-type%_ranger_audit.log");
		props.put(filePropPrefix + "."
				+ FileAuditDestination.PROP_FILE_FILE_ROLLOVER, "" + 10);

		props.put(filePropPrefix + "." + AuditQueue.PROP_QUEUE, "batch");
		String batchPropPrefix = filePropPrefix + "." + "batch";

		props.put(batchPropPrefix + "." + AuditQueue.PROP_BATCH_SIZE, ""
				+ batchSize);
		props.put(batchPropPrefix + "." + AuditQueue.PROP_QUEUE_SIZE, ""
				+ queueSize);
		props.put(batchPropPrefix + "." + AuditQueue.PROP_BATCH_INTERVAL,
				"" + intervalMS);

		// Enable File Spooling
		int destRetryMS = 10;
		props.put(batchPropPrefix + "."
				+ AuditQueue.PROP_FILE_SPOOL_ENABLE, "" + true);
		props.put(batchPropPrefix + "."
				+ AuditFileSpool.PROP_FILE_SPOOL_LOCAL_DIR, "target");
		props.put(batchPropPrefix + "."
				+ AuditFileSpool.PROP_FILE_SPOOL_DEST_RETRY_MS, ""
				+ destRetryMS);

		AuditProviderFactory factory = new AuditProviderFactory();
		factory.init(props, "test");

		// FileAuditDestination fileDest = new FileAuditDestination();
		// fileDest.init(props, filePropPrefix);
		//
		// AuditBatchQueue queue = new AuditBatchQueue(fileDest);
		// queue.init(props, batchPropPrefix);
		// queue.start();

		AuditHandler queue = factory.getAuditProvider();

		for (int i = 0; i < messageToSend; i++) {
			queue.log(createEvent());
		}
		// Let's wait for second
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignore
		}

		queue.waitToComplete();
		queue.stop();
		queue.waitToComplete();

		assertTrue("File created", logFile.exists());
		try {
			List<AuthzAuditEvent> eventList = new ArrayList<AuthzAuditEvent>();
			int totalSum = 0;
			BufferedReader br = new BufferedReader(new FileReader(logFile));
			String line;
			int lastSeq = -1;
			boolean outOfSeq = false;
			while ((line = br.readLine()) != null) {
				AuthzAuditEvent event = MiscUtil.fromJson(line,
						AuthzAuditEvent.class);
				eventList.add(event);
				totalSum += event.getEventCount();
				if (event.getSeqNum() <= lastSeq) {
					outOfSeq = true;
				}
			}
			br.close();
			assertEquals("Total count", messageToSend, eventList.size());
			assertEquals("Total sum", messageToSend, totalSum);
			assertFalse("Event not in sequnce", outOfSeq);

		} catch (Throwable e) {
			logger.error("Error opening file for reading.", e);
			assertTrue("Error reading file. fileName=" + logFile + ", error="
					+ e.toString(), true);
		}

	}

	private AuthzAuditEvent createEvent() {
		AuthzAuditEvent event = new AuthzAuditEvent();
		event.setSeqNum(++seqNum);
		return event;
	}

	private AuthzAuditEvent createEvent(String user, String accessType,
			String resource, boolean isAllowed) {
		AuthzAuditEvent event = new AuthzAuditEvent();
		event.setUser(user);
		event.setAccessType(accessType);
		event.setResourcePath(resource);
		event.setAccessResult(isAllowed ? (short) 1 : (short) 0);

		event.setSeqNum(++seqNum);
		return event;
	}
}
