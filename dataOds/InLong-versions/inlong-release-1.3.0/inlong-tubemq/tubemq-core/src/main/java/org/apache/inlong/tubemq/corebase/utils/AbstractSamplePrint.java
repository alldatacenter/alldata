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

import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractSamplePrint {

    protected final StringBuilder sBuilder = new StringBuilder(512);
    protected AtomicLong lastLogTime = new AtomicLong(0);
    protected AtomicLong totalPrintCount = new AtomicLong(0);
    protected long sampleDetailDur = 30000;
    protected long maxDetailCount = 10;
    protected long sampleResetDur = 1200000;
    protected long maxTotalCount = 15;
    protected long maxUncheckDetailCount = 15;
    protected AtomicLong totalUncheckCount = new AtomicLong(10);

    public AbstractSamplePrint() {

    }

    public AbstractSamplePrint(long sampleDetailDur,
                               long sampleResetDur,
                               long maxDetailCount,
                               long maxTotalCount) {
        this.sampleDetailDur = sampleDetailDur;
        this.sampleResetDur = sampleResetDur;
        this.maxDetailCount = maxDetailCount;
        this.maxTotalCount = maxTotalCount;
        this.maxUncheckDetailCount = maxDetailCount;
    }

    public abstract void printExceptionCaught(Throwable e);

    public abstract void printExceptionCaught(Throwable e, String hostName, String nodeName);

    public abstract void printWarn(String err);

    public abstract void printError(String err);
}
