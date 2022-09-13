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

package org.apache.inlong.common.monitor;

import java.util.concurrent.atomic.AtomicInteger;

public class LogCounter {

    private AtomicInteger counter = new AtomicInteger(0);

    private int start = 10;
    private int control = 1000;
    private int reset = 60 * 1000;

    private long lastLogTime = System.currentTimeMillis();

    public LogCounter(int start, int control, int reset) {
        this.start = start;
        this.control = control;
        this.reset = reset;
    }

    public boolean shouldPrint() {
        if (System.currentTimeMillis() - lastLogTime > reset) {
            counter.set(0);
            this.lastLogTime = System.currentTimeMillis();
        }

        if (counter.incrementAndGet() > start && counter.get() % control != 0) {
            return false;
        }

        return true;
    }
}
