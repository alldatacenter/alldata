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

package org.apache.inlong.tubemq.server.common.utils;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * Abstract class which contains a Thread and delegates the common Thread methods to that instance.
 * Copied from <a href="http://hbase.apache.org">Apache HBase Project</a>
 */
public abstract class HasThread implements Runnable {
    private final Thread thread;

    public HasThread() {
        this.thread = new Thread(this);
    }

    public HasThread(String name) {
        this.thread = new Thread(this, name);
    }

    public Thread getThread() {
        return thread;
    }

    @Override
    public abstract void run();

    // // Begin delegation to Thread

    public final String getName() {
        return thread.getName();
    }

    public final void setName(String name) {
        thread.setName(name);
    }

    public void interrupt() {
        thread.interrupt();
    }

    public final boolean isAlive() {
        return thread.isAlive();
    }

    public boolean isInterrupted() {
        return thread.isInterrupted();
    }

    public final void setDaemon(boolean on) {
        thread.setDaemon(on);
    }

    public final void setPriority(int newPriority) {
        thread.setPriority(newPriority);
    }

    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        thread.setUncaughtExceptionHandler(eh);
    }

    public void start() {
        thread.start();
    }

    public final void join() throws InterruptedException {
        thread.join();
    }

    public final void join(long millis, int nanos) throws InterruptedException {
        thread.join(millis, nanos);
    }

    public final void join(long millis) throws InterruptedException {
        thread.join(millis);
    }
    // // End delegation to Thread
}
