/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.  You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.inlong.common.monitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

public class CounterGroup {

    private String name;
    private HashMap<String, AtomicLong> counters = new HashMap();

    public CounterGroup() {
    }

    public synchronized Long get(String name) {
        return this.getCounter(name).get();
    }

    public synchronized Long incrementAndGet(String name) {
        return this.getCounter(name).incrementAndGet();
    }

    public synchronized Long addAndGet(String name, Long delta) {
        return this.getCounter(name).addAndGet(delta);
    }

    public synchronized void add(CounterGroup counterGroup) {
        synchronized (counterGroup) {
            Iterator iterator = counterGroup.getCounters().entrySet().iterator();

            while (iterator.hasNext()) {
                Entry<String, AtomicLong> entry = (Entry) iterator.next();
                this.addAndGet((String) entry.getKey(), ((AtomicLong) entry.getValue()).get());
            }

        }
    }

    public synchronized void set(String name, Long value) {
        this.getCounter(name).set(value);
    }

    public synchronized AtomicLong getCounter(String name) {
        if (!this.counters.containsKey(name)) {
            this.counters.put(name, new AtomicLong());
        }

        return (AtomicLong) this.counters.get(name);
    }

    public synchronized String toString() {
        return "{ name:" + this.name + " counters:" + this.counters + " }";
    }

    public synchronized String getName() {
        return this.name;
    }

    public synchronized void setName(String name) {
        this.name = name;
    }

    public synchronized HashMap<String, AtomicLong> getCounters() {
        return this.counters;
    }

    public synchronized void setCounters(HashMap<String, AtomicLong> counters) {
        this.counters = counters;
    }
}

