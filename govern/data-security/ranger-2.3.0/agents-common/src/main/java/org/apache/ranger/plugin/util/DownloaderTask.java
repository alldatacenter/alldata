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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

public final class  DownloaderTask extends TimerTask {
    private static final Logger LOG = LoggerFactory.getLogger(DownloaderTask.class);

    private final DownloadTrigger timerTrigger = new DownloadTrigger();
    private final BlockingQueue<DownloadTrigger> queue;

    public DownloaderTask(BlockingQueue<DownloadTrigger> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            queue.put(timerTrigger);
            timerTrigger.waitForCompletion();
        } catch (InterruptedException excp) {
            LOG.error("Caught exception. Exiting thread");
        }
    }
}
