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

package org.apache.inlong.tubemq.server.master.utils;

import org.apache.inlong.tubemq.corebase.utils.AbstractSamplePrint;
import org.slf4j.Logger;

public class BrokerStatusSamplePrint extends AbstractSamplePrint {
    /**
     * Log limit class
     */
    private final Logger logger;

    public BrokerStatusSamplePrint(final Logger logger) {
        super();
        this.logger = logger;
    }

    public BrokerStatusSamplePrint(final Logger logger,
                                   long sampleDetailDur, long sampleResetDur,
                                   long maxDetailCount, long maxTotalCount) {
        super(sampleDetailDur, sampleResetDur, maxDetailCount, maxTotalCount);
        this.logger = logger;
    }

    @Override
    public void printExceptionCaught(Throwable e) {
        //
    }

    @Override
    public void printExceptionCaught(Throwable e, String hostName, String nodeName) {
        //
    }

    @Override
    public void printWarn(String err) {
        printMsg(true, err);
    }

    @Override
    public void printError(String err) {
        printMsg(false, err);
    }

    private void printMsg(boolean isWarn, String err) {
        final long now = System.currentTimeMillis();
        final long diffTime = now - lastLogTime.get();
        final long curPrintCnt = totalPrintCount.incrementAndGet();
        if (curPrintCnt < maxTotalCount) {
            if (diffTime < sampleDetailDur && curPrintCnt < maxDetailCount) {
                if (isWarn) {
                    logger.warn(err);
                } else {
                    logger.error(err);
                }
            }
        }
        if (diffTime > sampleResetDur) {
            if (this.lastLogTime.compareAndSet(now - diffTime, now)) {
                totalPrintCount.set(0);
            }
        }
    }

}
