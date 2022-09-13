/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.utils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node Read and Write Service status holder.
 */
public class ServiceStatusHolder {
    private static final Logger logger =
            LoggerFactory.getLogger(ServiceStatusHolder.class);
    private static final AtomicBoolean isServiceStopped = new AtomicBoolean(false);
    private static final AtomicBoolean isReadStopped = new AtomicBoolean(false);
    private static final AtomicBoolean isWriteStopped = new AtomicBoolean(false);

    private static int allowedReadIOExcptCnt = 10;
    private static int allowedWriteIOExcptCnt = 10;
    private static long statsDurationMs = 120000;
    private static final AtomicLong curReadIOExcptCnt = new AtomicLong(0);
    private static final AtomicLong lastReadStatsTime =
            new AtomicLong(System.currentTimeMillis());
    private static final AtomicBoolean isPauseRead = new AtomicBoolean(false);

    private static final AtomicLong curWriteIOExcptCnt = new AtomicLong(0);
    private static final AtomicLong lastWriteStatsTime =
            new AtomicLong(System.currentTimeMillis());
    private static final AtomicBoolean isPauseWrite = new AtomicBoolean(false);

    public static void setStatsParameters(int paraAllowedReadIOExcptCnt,
                                          int paraAllowedWriteIOExcptCnt,
                                          long paraStatsDurationMs) {
        allowedReadIOExcptCnt = paraAllowedReadIOExcptCnt;
        allowedWriteIOExcptCnt = paraAllowedWriteIOExcptCnt;
        statsDurationMs = paraStatsDurationMs;
    }

    public static boolean isServiceStopped() {
        return isServiceStopped.get();
    }

    public static void setStoppedStatus(boolean isStopped, String caller) {
        if (isServiceStopped.get() != isStopped) {
            isServiceStopped.set(isStopped);
            if (isStopped) {
                logger.warn(new StringBuilder(256)
                    .append("[Service Status]: global-write stopped by caller ")
                    .append(caller).toString());
            }
        }
    }

    public static boolean addWriteIOErrCnt() {
        long curTime = lastWriteStatsTime.get();
        if (System.currentTimeMillis() - curTime > statsDurationMs) {
            if (lastWriteStatsTime.compareAndSet(curTime, System.currentTimeMillis())) {
                curWriteIOExcptCnt.getAndSet(0);
                if (isPauseWrite.get()) {
                    isPauseWrite.compareAndSet(true, false);
                }
            }
        }
        if (curWriteIOExcptCnt.incrementAndGet() > allowedWriteIOExcptCnt) {
            isPauseWrite.set(true);
            return true;
        }
        return false;
    }

    public static boolean isWriteServiceStop() {
        return isPauseWrite.get() || isWriteStopped.get();
    }

    public static int getWriteServiceReportStatus() {
        return getCurServiceStatus(isPauseWrite.get(), isWriteStopped.get());
    }

    public static boolean addReadIOErrCnt() {
        long curTime = lastReadStatsTime.get();
        if (System.currentTimeMillis() - curTime > statsDurationMs) {
            if (lastReadStatsTime.compareAndSet(curTime, System.currentTimeMillis())) {
                curReadIOExcptCnt.getAndSet(0);
                if (isPauseRead.get()) {
                    isPauseRead.compareAndSet(true, false);
                }
            }
        }
        if (curReadIOExcptCnt.incrementAndGet() > allowedReadIOExcptCnt) {
            isPauseRead.set(true);
            return true;
        }
        return false;
    }

    public static boolean isReadServiceStop() {
        return isPauseRead.get() || isReadStopped.get();
    }

    public static int getReadServiceReportStatus() {
        return getCurServiceStatus(isPauseRead.get(), isReadStopped.get());
    }

    /**
     * Set the read and write service status
     *
     * @param isReadStop      whether stop read service
     * @param isWriteStop     whether stop write service
     * @param caller          the caller
     */
    public static void setReadWriteServiceStatus(boolean isReadStop,
                                                 boolean isWriteStop,
                                                 String caller) {
        if (isReadStopped.get() != isReadStop) {
            isReadStopped.set(isReadStop);
            if (isReadStop) {
                logger.warn(new StringBuilder(256)
                    .append("[Service Status]: global-read stopped by caller ")
                    .append(caller).toString());
            } else {
                if (isPauseRead.get()) {
                    isPauseRead.set(false);
                    curReadIOExcptCnt.set(0);
                    lastReadStatsTime.set(System.currentTimeMillis());
                }
                logger.warn(new StringBuilder(256)
                    .append("[Service Status]: global-read opened by caller ")
                    .append(caller).toString());
            }
        }
        if (isWriteStopped.get() != isWriteStop) {
            isWriteStopped.set(isWriteStop);
            if (isWriteStop) {
                logger.warn(new StringBuilder(256)
                    .append("[Service Status]: global-write stopped by caller ")
                    .append(caller).toString());
            } else {
                if (isPauseWrite.get()) {
                    isPauseWrite.set(false);
                    curWriteIOExcptCnt.set(0);
                    lastWriteStatsTime.set(System.currentTimeMillis());
                }
                logger.warn(new StringBuilder(256)
                    .append("[Service Status]: global-write opened by caller ")
                    .append(caller).toString());
            }
        }
    }

    private static int getCurServiceStatus(boolean pauseStatus, boolean serviceStatus) {
        if (serviceStatus) {
            return 1;
        } else {
            if (pauseStatus) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
