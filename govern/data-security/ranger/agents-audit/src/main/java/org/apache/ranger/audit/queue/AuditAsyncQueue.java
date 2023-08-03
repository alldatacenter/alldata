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
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.provider.AuditHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * This is a non-blocking queue with no limit on capacity.
 */
public class AuditAsyncQueue extends AuditQueue implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(AuditAsyncQueue.class);

	LinkedBlockingQueue<AuditEventBase> queue = new LinkedBlockingQueue<AuditEventBase>();
	Thread consumerThread = null;

	static final int MAX_DRAIN = 1000;
	static int threadCount = 0;
	static final String DEFAULT_NAME = "async";

	public AuditAsyncQueue(AuditHandler consumer) {
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
		// Add to the queue and return ASAP
		if (queue.size() >= getMaxQueueSize()) {
			return false;
		}
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

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#start()
	 */
	@Override
	public void start() {
		if (consumer != null) {
			consumer.start();
		} else {
			logger.error("consumer is not set. Nothing will be sent to any consumer. name="
					+ getName());
		}

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
		while (true) {
			try {
				AuditEventBase event = null;
				if (!isDrain()) {
					// For Transfer queue take() is blocking
					event = queue.take();
				} else {
					// For Transfer queue poll() is non blocking
					event = queue.poll();
				}
				if (event != null) {
					Collection<AuditEventBase> eventList = new ArrayList<AuditEventBase>();
					eventList.add(event);
					queue.drainTo(eventList, MAX_DRAIN - 1);
					consumer.log(eventList);
				}
			} catch (InterruptedException e) {
				logger.info("Caught exception in consumer thread. Shutdown might be in progress");
			} catch (Throwable t) {
				logger.error("Caught error during processing request.", t);
			}
			if (isDrain()) {
				if (queue.isEmpty()) {
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
		logger.info("Exiting polling loop. name=" + getName());

		try {
			// Call stop on the consumer
			logger.info("Calling to stop consumer. name=" + getName()
					+ ", consumer.name=" + consumer.getName());

			// Call stop on the consumer
			consumer.stop();
		} catch (Throwable t) {
			logger.error("Error while calling stop on consumer.", t);
		}
		logger.info("Exiting consumerThread.run() method. name=" + getName());
	}

}
