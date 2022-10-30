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

import com.sleepycat.je.rep.utilint.ServiceDispatcher;
import java.io.IOException;
import org.apache.inlong.tubemq.corebase.utils.AbstractSamplePrint;
import org.slf4j.Logger;

public class MetaConfigSamplePrint extends AbstractSamplePrint {
    /**
     * Log limit class
     */
    private final Logger logger;

    public MetaConfigSamplePrint(final Logger logger) {
        super();
        this.logger = logger;
    }

    public MetaConfigSamplePrint(final Logger logger,
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
        if (e != null) {
            if ((e instanceof IOException)) {
                // IOException log limit
                final long now = System.currentTimeMillis();
                final long diffTime = now - lastLogTime.get();
                final long curPrintCnt = totalPrintCount.incrementAndGet();
                if (curPrintCnt < maxTotalCount) {
                    if (diffTime < sampleDetailDur && curPrintCnt < maxDetailCount) {
                        logger.error(sBuilder.append("[MetaConfig Error] Connect to node:[")
                                .append(hostName).append(",").append(nodeName)
                                .append("] IOException error").toString(), e);
                    } else {
                        logger.error(sBuilder.append("[MetaConfig Error] Connect to node:[")
                                .append(hostName).append(",").append(nodeName)
                                .append("] IOException error is ").append(e.toString()).toString());
                    }
                    sBuilder.delete(0, sBuilder.length());
                }
                if (diffTime > sampleResetDur) {
                    if (this.lastLogTime.compareAndSet(now - diffTime, now)) {
                        totalPrintCount.set(0);
                    }
                }
            } else {
                if (e instanceof ServiceDispatcher.ServiceConnectFailedException) {
                    // Print some part log
                    final long curPrintCnt = totalUncheckCount.incrementAndGet();
                    if (curPrintCnt < maxUncheckDetailCount) {
                        logger.error(sBuilder.append("[MetaConfig Error] Connect to node:[")
                                .append(hostName).append(",").append(nodeName)
                                .append("] ServiceConnectFailedException error").toString(), e);
                    } else {
                        logger.error(sBuilder.append("[MetaConfig Error] Connect to node:[")
                                .append(hostName).append(",").append(nodeName)
                                .append("] ServiceConnectFailedException error is ")
                                .append(e.toString()).toString());
                    }
                    sBuilder.delete(0, sBuilder.length());
                } else {
                    logger.error(sBuilder.append("[MetaConfig Error] Connect to node:[")
                            .append(hostName).append(",").append(nodeName)
                            .append("] throw error").toString(), e);
                    sBuilder.delete(0, sBuilder.length());
                }
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
