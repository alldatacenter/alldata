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

package org.apache.ranger.common;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class TimedExecutorConfigurator {

	// these two are important and hence are user configurable.
	static final String Property_MaxThreadPoolSize = "ranger.timed.executor.max.threadpool.size";
	static final String Property_QueueSize = "ranger.timed.executor.queue.size";
	// We need these default-defaults since default-site.xml file isn't inside the jar, i.e. file itself may be missing or values in it might be messed up! :(
	static final int _DefaultMaxThreadPoolSize = 10;
	static final private int _DefaultBlockingQueueSize = 100;


	private int _maxThreadPoolSize;
	private int _blockingQueueSize;
	// The following are hard-coded for now and can be exposed if there is a pressing need.
	private int _coreThreadPoolSize = 1;
	private long _keepAliveTime = 10;
	private TimeUnit _keepAliveTimeUnit = TimeUnit.SECONDS;
	
	public TimedExecutorConfigurator() {
	}

	// Infrequently used class (once per lifetime of policy manager) hence, values read from property file aren't cached.
	@PostConstruct
	void initialize() {
		Integer value = PropertiesUtil.getIntProperty(Property_MaxThreadPoolSize);
		if (value == null) {
			_maxThreadPoolSize = _DefaultMaxThreadPoolSize;
		} else {
			_maxThreadPoolSize = value;
		}

		value = PropertiesUtil.getIntProperty(Property_QueueSize);
		if (value == null) {
			_blockingQueueSize = _DefaultBlockingQueueSize;
		} else {
			_blockingQueueSize = value;
		}
	}
	/**
	 * Provided mostly only testability.
	 * @param maxThreadPoolSize
	 * @param blockingQueueSize
	 */
	public TimedExecutorConfigurator(int maxThreadPoolSize, int blockingQueueSize) {
		_maxThreadPoolSize = maxThreadPoolSize;
		_blockingQueueSize = blockingQueueSize;
	}
	
	public int getCoreThreadPoolSize() {
		return _coreThreadPoolSize;
	}
	public int getMaxThreadPoolSize() {
		return _maxThreadPoolSize;
	}
	public long getKeepAliveTime() {
		return _keepAliveTime;
	}
	public TimeUnit getKeepAliveTimeUnit() {
		return _keepAliveTimeUnit;
	}
	public int getBlockingQueueSize() {
		return _blockingQueueSize;
	}
}
