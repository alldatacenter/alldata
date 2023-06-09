/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.util.exec;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class AtmostOneThreadExecutor extends AbstractExecutorService {
    private Thread worker;
    private final LinkedList<Runnable> q;
    private boolean shutdown;
    private final ThreadFactory factory;

    public AtmostOneThreadExecutor(ThreadFactory factory) {
        this.q = new LinkedList();
        this.factory = factory;
    }

    public AtmostOneThreadExecutor() {
        this(new DaemonThreadFactory());
    }

    public void shutdown() {
        synchronized (this.q) {
            this.shutdown = true;
            if (this.isAlive()) {
                this.worker.interrupt();
            }

        }
    }

    private boolean isAlive() {
        return this.worker != null && this.worker.isAlive();
    }

    public List<Runnable> shutdownNow() {
        synchronized (this.q) {
            this.shutdown = true;
            List<Runnable> r = new ArrayList(this.q);
            this.q.clear();
            return r;
        }
    }

    public boolean isShutdown() {
        return this.shutdown;
    }

    public boolean isTerminated() {
        return this.shutdown && !this.isAlive();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        synchronized (this.q) {
            long now = System.nanoTime();

            for (long end = now + unit.toNanos(timeout); this.isAlive() && end - now > 0L; now = System.nanoTime()) {
                this.q.wait(TimeUnit.NANOSECONDS.toMillis(end - now));
            }
        }

        return this.isTerminated();
    }

    public void execute(Runnable command) {
        synchronized (this.q) {
            if (this.isShutdown()) {
                throw new IllegalStateException("This executor has been shutdown.");
            } else {
                this.q.add(command);
                if (!this.isAlive()) {
                    this.worker = this.factory.newThread(new AtmostOneThreadExecutor.Worker());
                    this.worker.start();
                }

            }
        }
    }

    private class Worker implements Runnable {
        private Worker() {
        }

        public void run() {
            while (true) {
                Runnable task;
                synchronized (AtmostOneThreadExecutor.this.q) {
                    if (AtmostOneThreadExecutor.this.q.isEmpty()) {
                        AtmostOneThreadExecutor.this.worker = null;
                        return;
                    }

                    task = (Runnable) AtmostOneThreadExecutor.this.q.remove();
                }

                task.run();
            }
        }
    }
}
