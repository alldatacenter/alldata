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

package org.apache.inlong.tubemq.server.broker.utils;

import java.io.IOException;
import org.apache.inlong.tubemq.corebase.utils.AbstractSamplePrint;
import org.slf4j.Logger;

/**
 * Compressed print disk exception's statistics.
 */
public class DiskSamplePrint extends AbstractSamplePrint {
    private final Logger logger;

    public DiskSamplePrint(final Logger logger) {
        super();
        this.logger = logger;
    }

    public DiskSamplePrint(final Logger logger,
                           long sampleDetailDur, long sampleResetDur,
                           long maxDetailCount, long maxTotalCount) {
        super(sampleDetailDur, sampleResetDur, maxDetailCount, maxTotalCount);
        this.logger = logger;
    }

    @Override
    public void printExceptionCaught(Throwable e) {
        if (e != null) {
            if ((e instanceof IOException)) {
                final long now = System.currentTimeMillis();
                final long diffTime = now - lastLogTime.get();
                final long curPrintCnt = totalPrintCount.incrementAndGet();
                if (curPrintCnt < maxTotalCount) {
                    if (diffTime < sampleDetailDur && curPrintCnt < maxDetailCount) {
                        logger.error("[File Store] Append message in file failed ", e);
                    } else {
                        logger.error(sBuilder
                                .append("[File Store] Append message in file failed 2 ")
                                .append(e.toString()).toString());
                        sBuilder.delete(0, sBuilder.length());
                    }
                }
                if (diffTime > sampleResetDur) {
                    if (this.lastLogTime.compareAndSet(now - diffTime, now)) {
                        totalPrintCount.set(0);
                    }
                }
            } else {
                final long curPrintCnt = totalUncheckCount.incrementAndGet();
                if (curPrintCnt < maxUncheckDetailCount) {
                    logger.error("[File Store] Append message in file failed 3 is ", e);
                } else {
                    logger.error(sBuilder
                            .append("[File Store] Append message in file failed 4 is ")
                            .append(e.toString()).toString());
                    sBuilder.delete(0, sBuilder.length());
                }
            }
        }
    }

    @Override
    public void printExceptionCaught(Throwable e, String storeKey, String partitionId) {
        if (e != null) {
            if ((e instanceof IOException)) {
                final long now = System.currentTimeMillis();
                final long diffTime = now - lastLogTime.get();
                final long curPrintCnt = totalPrintCount.incrementAndGet();
                if (curPrintCnt < maxTotalCount) {
                    if (diffTime < sampleDetailDur && curPrintCnt < maxDetailCount) {
                        logger.warn(sBuilder
                            .append("[File Store] Get message failure for IOException, storeKey=")
                            .append(storeKey).append(", partitionId=").append(partitionId).toString(), e);
                    } else {
                        logger.warn(sBuilder
                            .append("[File Store] Get message failure for IOException 2, storeKey=")
                            .append(storeKey).append(", partitionId=").append(partitionId)
                            .append(", error = ").append(e.toString()).toString());
                    }
                    sBuilder.delete(0, sBuilder.length());
                }
                if (diffTime > sampleResetDur) {
                    if (this.lastLogTime.compareAndSet(now - diffTime, now)) {
                        totalPrintCount.set(0);
                    }
                }
            } else {
                final long curPrintCnt = totalUncheckCount.incrementAndGet();
                if (curPrintCnt < maxUncheckDetailCount) {
                    logger.warn(sBuilder
                        .append("[File Store] Get message failure for Exception 3, storeKey=")
                        .append(storeKey).append(", partitionId=").append(partitionId).toString(), e);
                } else {
                    logger.warn(sBuilder
                        .append("[File Store] Get message failure for Exception 4, storeKey=")
                        .append(storeKey).append(", partitionId=").append(partitionId)
                        .append(", error = ").append(e.toString()).toString());
                }
                sBuilder.delete(0, sBuilder.length());
            }
        }
    }

    @Override
    public void printWarn(String err) {
        //
    }

    @Override
    public void printError(String err) {
        //
    }
}
