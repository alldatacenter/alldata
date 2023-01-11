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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.provider.AuditHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class AuditBatchQueue extends AuditQueue implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(AuditBatchQueue.class);

	private BlockingQueue<AuditEventBase> queue = null;
	private Collection<AuditEventBase> localBatchBuffer = new ArrayList<AuditEventBase>();

	Thread consumerThread = null;
	static int threadCount = 0;
	static final String DEFAULT_NAME = "batch";

	public AuditBatchQueue(AuditHandler consumer) {
		super(consumer);
		setName(DEFAULT_NAME);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.apache.ranger.audit.provider.AuditProvider#log(org.apache.ranger.
	 * audit.model.AuditEventBase)
	 */
	@Override
	public boolean log(AuditEventBase event) {
		// Add to batchQueue. Block if full
		queue.add(event);
		return true;
	}

	@Override
	public boolean log(Collection<AuditEventBase> events) {
		boolean ret = true;
		for (AuditEventBase event : events) {
			ret = log(event);
			if (!ret) {
				break;
			}
		}
		return ret;
	}

	@Override
	public void init(Properties prop, String basePropertyName) {
		String propPrefix = "xasecure.audit.batch";
		if (basePropertyName != null) {
			propPrefix = basePropertyName;
		}

		super.init(prop, propPrefix);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#start()
	 */
	@Override
	synchronized public void start() {
		if (consumerThread != null) {
			logger.error("Provider is already started. name=" + getName());
			return;
		}
		logger.info("Creating ArrayBlockingQueue with maxSize="
				+ getMaxQueueSize());
		queue = new ArrayBlockingQueue<AuditEventBase>(getMaxQueueSize());

		// Start the consumer first
		consumer.start();

		// Then the FileSpooler
		if (fileSpoolerEnabled) {
			fileSpooler.start();
		}

		// Finally the queue listener
		consumerThread = new Thread(this, this.getClass().getName()
				+ (threadCount++));
		consumerThread.setDaemon(true);
		consumerThread.start();

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#stop()
	 */
	@Override
	public void stop() {
		logger.info("Stop called. name=" + getName());
		setDrain(true);
		flush();
		try {
			if (consumerThread != null) {
				logger.info("Interrupting consumerThread. name=" + getName()
						+ ", consumer="
						+ (consumer == null ? null : consumer.getName()));

				consumerThread.interrupt();
			}
		} catch (Throwable t) {
			// ignore any exception
		}
		consumerThread = null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#waitToComplete()
	 */
	@Override
	public void waitToComplete() {
		int defaultTimeOut = -1;
		waitToComplete(defaultTimeOut);
		consumer.waitToComplete(defaultTimeOut);
	}

	@Override
	public void waitToComplete(long timeout) {
		setDrain(true);
		flush();
		long sleepTime = 1000;
		long startTime = System.currentTimeMillis();
		int prevQueueSize = -1;
		int staticLoopCount = 0;
		while ((queue.size() > 0 || localBatchBuffer.size() > 0)) {
			if (prevQueueSize == queue.size()) {
				logger.error("Queue size is not changing. " + getName()
						+ ".size=" + queue.size());
				staticLoopCount++;
				if (staticLoopCount > 5) {
					logger.error("Aborting writing to consumer. Some logs will be discarded."
							+ getName() + ".size=" + queue.size());
					break;
				}
			} else {
				staticLoopCount = 0;
				prevQueueSize = queue.size();
			}
			if (consumerThread != null) {
				consumerThread.interrupt();
			}
			try {
				Thread.sleep(sleepTime);
				if (timeout > 0
						&& (System.currentTimeMillis() - startTime > timeout)) {
					break;
				}
			} catch (InterruptedException e) {
				break;
			}
		}
		consumer.waitToComplete(timeout);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#flush()
	 */
	@Override
	public void flush() {
		if (fileSpoolerEnabled) {
			fileSpooler.flush();
		}
		consumer.flush();
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
			logger.error("Exited thread abnormaly. queue=" + getName(), t);
		}
	}

	public void runLogAudit() {
		long lastDispatchTime = System.currentTimeMillis();
		boolean isDestActive = true;
		while (true) {
			logStatusIfRequired();

			// Time to next dispatch
			long nextDispatchDuration = lastDispatchTime
					- System.currentTimeMillis() + getMaxBatchInterval();

			boolean isToSpool = false;
			boolean fileSpoolDrain = false;
			try {
				if (fileSpoolerEnabled && fileSpooler.isPending()) {
					int percentUsed = queue.size() * 100
							/ getMaxQueueSize();
					long lastAttemptDelta = fileSpooler
							.getLastAttemptTimeDelta();

					fileSpoolDrain = lastAttemptDelta > fileSpoolMaxWaitTime;
					// If we should even read from queue?
					if (!isDrain() && !fileSpoolDrain
							&& percentUsed < fileSpoolDrainThresholdPercent) {
						// Since some files are still under progress and it is
						// not in drain mode, lets wait and retry
						if (nextDispatchDuration > 0) {
							Thread.sleep(nextDispatchDuration);
						}
						lastDispatchTime = System.currentTimeMillis();
						continue;
					}
					isToSpool = true;
				}

				AuditEventBase event = null;

				if (!isToSpool && !isDrain() && !fileSpoolDrain
						&& nextDispatchDuration > 0) {
					event = queue.poll(nextDispatchDuration,
							TimeUnit.MILLISECONDS);
				} else {
					// For poll() is non blocking
					event = queue.poll();
				}

				if (event != null) {
					localBatchBuffer.add(event);
					if (getMaxBatchSize() >= localBatchBuffer.size()) {
						queue.drainTo(localBatchBuffer, getMaxBatchSize()
								- localBatchBuffer.size());
					}
				} else {
					// poll returned due to timeout, so reseting clock
					nextDispatchDuration = lastDispatchTime
							- System.currentTimeMillis()
							+ getMaxBatchInterval();

					lastDispatchTime = System.currentTimeMillis();
				}
			} catch (InterruptedException e) {
				logger.info("Caught exception in consumer thread. Shutdown might be in progress");
				setDrain(true);
			} catch (Throwable t) {
				logger.error("Caught error during processing request.", t);
			}

			addTotalCount(localBatchBuffer.size());
			if (localBatchBuffer.size() > 0 && isToSpool) {
				// Let spool to the file directly
				if (isDestActive) {
					logger.info("Switching to file spool. Queue=" + getName()
							+ ", dest=" + consumer.getName());
				}
				isDestActive = false;
				// Just before stashing
				lastDispatchTime = System.currentTimeMillis();
				fileSpooler.stashLogs(localBatchBuffer);
				addStashedCount(localBatchBuffer.size());
				localBatchBuffer.clear();
			} else if (localBatchBuffer.size() > 0
					&& (isDrain()
							|| localBatchBuffer.size() >= getMaxBatchSize() || nextDispatchDuration <= 0)) {
				if (fileSpoolerEnabled && !isDestActive) {
					logger.info("Switching to writing to destination. Queue="
							+ getName() + ", dest=" + consumer.getName());
				}
				// Reset time just before sending the logs
				lastDispatchTime = System.currentTimeMillis();
				boolean ret = consumer.log(localBatchBuffer);
				if (!ret) {
					if (fileSpoolerEnabled) {
						logger.info("Switching to file spool. Queue="
								+ getName() + ", dest=" + consumer.getName());
						// Transient error. Stash and move on
						fileSpooler.stashLogs(localBatchBuffer);
						isDestActive = false;
						addStashedCount(localBatchBuffer.size());
					} else {
						// We need to drop this event
						addFailedCount(localBatchBuffer.size());
						logFailedEvent(localBatchBuffer);
					}
				} else {
					isDestActive = true;
					addSuccessCount(localBatchBuffer.size());
				}
				localBatchBuffer.clear();
			}

			if (isDrain()) {
				if (!queue.isEmpty() || localBatchBuffer.size() > 0) {
					logger.info("Queue is not empty. Will retry. queue.size)="
							+ queue.size() + ", localBatchBuffer.size()="
							+ localBatchBuffer.size());
				} else {
					break;
				}
				if (isDrainMaxTimeElapsed()) {
					logger.warn("Exiting polling loop because max time allowed reached. name="
							+ getName()
							+ ", waited for "
							+ (stopTime - System.currentTimeMillis()) + " ms");
				}
			}
		}

		logger.info("Exiting consumerThread. Queue=" + getName() + ", dest="
				+ consumer.getName());
		try {
			// Call stop on the consumer
			logger.info("Calling to stop consumer. name=" + getName()
					+ ", consumer.name=" + consumer.getName());

			consumer.stop();
			if (fileSpoolerEnabled) {
				fileSpooler.stop();
			}
		} catch (Throwable t) {
			logger.error("Error while calling stop on consumer.", t);
		}
		logStatus();
		logger.info("Exiting consumerThread.run() method. name=" + getName());
	}
}
