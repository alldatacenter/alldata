/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.realtime.transfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年4月8日
 */
public abstract class BasicIncrStatusReport implements Runnable {

    private boolean closed = false;

    protected final Collection<IOnsListenerStatus> incrChannels;

    private static final Logger logger = LoggerFactory.getLogger(BasicIncrStatusReport.class);

    public BasicIncrStatusReport(Collection<IOnsListenerStatus> incrChannels) {
        this.incrChannels = incrChannels;
    }

    public void setClose() {
        this.closed = true;
    }

    protected boolean isClosed() {
        return closed;
    }

    protected abstract void processSnapshot() throws Exception;

    @Override
    public final void run() {
        try {
            while (!isClosed()) {
                try {
                    processSnapshot();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
            // 清空计数器
            for (IOnsListenerStatus l : incrChannels) {
                l.cleanLastAccumulator();
            }
            logger.info("server push realtime update session has been terminated");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
