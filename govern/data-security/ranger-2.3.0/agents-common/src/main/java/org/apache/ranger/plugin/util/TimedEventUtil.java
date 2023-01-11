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

 package org.apache.ranger.plugin.util;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class TimedEventUtil{

	public static void runWithTimeout(final Runnable runnable, long timeout, TimeUnit timeUnit) throws Exception {
		timedTask(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				runnable.run();
				return null;
			}
		}, timeout, timeUnit);
	}

	public static <T> T timedTask(Callable<T> callableObj, long timeout,
			TimeUnit timeUnit) throws Exception{
		
		return callableObj.call();
		
		/*
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future<T> future = executor.submit(callableObj);
		executor.shutdownNow();

		try {
			return future.get(timeout, timeUnit);
		} catch (TimeoutException | InterruptedException | ExecutionException e) {
			if(logger.isDebugEnabled()){
				logger.debug("Error executing task", e);
			}
			Throwable t = e.getCause();
			if (t instanceof Error) {
				throw (Error) t;
			} else if (t instanceof Exception) {
				throw (Exception) e;
			} else {
				throw new IllegalStateException(t);
			}
		}
		*/
		
	}
	

}