/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

import java.util.concurrent.atomic.AtomicInteger;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;

/**
 * Implementation class of the data transmission listener that can monitor the running status of subprocesses
 * 
 * @since 3.20.3
 */
public abstract class MonitorableProgressListener implements ProgressListener {
    private static final ILogger ILOG = LoggerBuilder.getLogger(MonitorableProgressListener.class);

    // Record the number of running subtasks.
    private AtomicInteger runningTask = new AtomicInteger(1);

    /**
     * Check whether the request task is running.
     * <br>
     * This method is used by the parent thread to monitor whether the current thread has completely stopped
     *  running after the current thread executes the interrupt() method.
     * 
     * @return If there are still running subtasks, true is returned. Otherwise, false is returned.
     * @since 3.20.3
     */
    public final boolean isRunning() {
        return this.runningTask.get() > 0;
    }

    /**
     * Wait until the request task is completely executed. Before the task execution completes, 
     * this method is blocked.<br>
     * <br>
     * This method is used to check whether the upload subtask is complete after the current thread
     *  executes the interrupt() method.<br>
     * 
     * @return If the task is complete, true is returned. Otherwise, false is returned.
     * @since 3.20.3
     * @throws InterruptedException
     * This exception is thrown when the thread is in waiting, sleep, 
     * or occupied state before or during the activity and the thread is interrupted.
     */
    public final boolean waitingFinish() throws InterruptedException {
        return waitingFinish(-1L);
    }

    /**
     * Wait until the request task is completely executed. Before the task is completely executed, 
     * the method is blocked until the timeout interval is exceeded.<br>
     * <br>
     * This method is used to check whether the upload subtask is complete after the current thread
     *  executes the interrupt() method.<br>
     * 
     * @param timeout
     *            Timeout interval, in milliseconds. If the parameter value is smaller than or equal to 0, 
     *            the waiting never times out.
     * @return If the task is complete, true is returned. Otherwise, false is returned.
     * @since 3.20.3
     * @throws InterruptedException
     * This exception is thrown when the thread is in waiting, sleep, or occupied state before or during
     *  the activity and the thread is interrupted.
     */
    public final boolean waitingFinish(long timeout) throws InterruptedException {
        long start = System.currentTimeMillis();
        if (ILOG.isDebugEnabled()) {
            ILOG.debug("this.runningTask = " + this.runningTask);
        }
        while (this.runningTask.get() > 0) {
            if (System.currentTimeMillis() - start > timeout && timeout > 0) {
                if (ILOG.isWarnEnabled()) {
                    ILOG.warn("DownloadFileReqeust is not finish. " + this.toString());
                }
                return false;
            }

            Thread.sleep(100L);
        }

        return true;
    }

    /**
     * Start a subtask.<br>
     * <br>
     * <b>Note: Generally, users are not advised to call this method, 
     * because calling this method may lead to failures of the waitingFinish and isRunning methods.
     * The SDK uses this method to adjust the number of running subtasks. 
     * The user determines whether the requested task is complete.</b><br>
     * <br>
     * <b>Reference: </b>{@link #waitingFinish(long)}, {@link #isRunning()}
     * 
     * @since 3.20.3
     */
    public final void startOneTask() {
        this.runningTask.incrementAndGet();
    }

    /**
     * End a subtask.<br>
     * <br>
     * <b>Note: Generally, users are not advised to call this method, 
     * because calling this method may lead to failures of the waitingFinish and isRunning methods.
     * The SDK uses this method to adjust the number of running subtasks. 
     * The user determines whether the requested task is complete.</b><br>
     * <br>
     * <b>Reference: </b>{@link #waitingFinish(long)}, {@link #isRunning()}
     * 
     * @since 3.20.3
     */
    public final void finishOneTask() {
        this.runningTask.decrementAndGet();
    }

    /**
     * Reset the listener.<br>
     * <br>
     * This method is used when a request is repeatedly used.
     * 
     * @since 3.20.3
     */
    public final void reset() {
        this.runningTask.set(1);
    }
}
