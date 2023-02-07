/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.daemon;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDaemonService implements Service, Runnable {

    private static final Logger logger =
            LoggerFactory.getLogger(AbstractDaemonService.class);
    private final String name;
    private final long intervalMs;
    private final Thread daemon;
    private final AtomicBoolean shutdown =
            new AtomicBoolean(false);

    public AbstractDaemonService(final String serviceName, final long intervalMs) {
        this.name = serviceName;
        this.intervalMs = intervalMs;
        this.daemon = new Thread(this, serviceName + "-daemon-thread");
        this.daemon.setDaemon(true);
    }

    @Override
    public void run() {
        StringBuilder strBuff =
                new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE);
        logger.info(strBuff.append(name)
                .append("-daemon-thread started").toString());
        strBuff.delete(0, strBuff.length());
        // process daemon task
        while (!isStopped()) {
            try {
                Thread.sleep(intervalMs);
                loopProcess(strBuff);
            } catch (InterruptedException e) {
                strBuff.delete(0, strBuff.length());
                logger.warn(strBuff.append(name)
                        .append("-daemon-thread thread has been interrupted").toString());
                strBuff.delete(0, strBuff.length());
                return;
            } catch (Throwable t) {
                strBuff.delete(0, strBuff.length());
                logger.error(strBuff.append(name)
                        .append("-daemon-thread throw a exception").toString(), t);
                strBuff.delete(0, strBuff.length());
            }
        }
        logger.info(strBuff.append(name)
                .append("-daemon-thread stopped").toString());
        strBuff.delete(0, strBuff.length());
    }

    protected abstract void loopProcess(StringBuilder strBuff);

    @Override
    public void start() {
        this.daemon.start();
    }

    @Override
    public boolean isStopped() {
        return this.shutdown.get();
    }

    @Override
    public boolean stop() {
        if (this.shutdown.get()) {
            return true;
        }
        if (this.shutdown.compareAndSet(false, true)) {
            logger.info(new StringBuilder(256).append(name)
                    .append("-daemon-thread closing ......").toString());
            try {
                if (this.daemon != null) {
                    this.daemon.interrupt();
                    this.daemon.join();
                }
            } catch (Throwable e) {
                //
            }
            logger.info(new StringBuilder(256).append(name)
                    .append("-daemon-thread stopped!").toString());
            return false;
        }
        return true;
    }
}
