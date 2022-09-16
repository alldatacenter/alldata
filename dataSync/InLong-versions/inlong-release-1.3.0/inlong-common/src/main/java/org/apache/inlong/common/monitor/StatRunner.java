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
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatRunner
        implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(StatRunner.class);
    private boolean shutDownFlag = false;
    private String name;
    private CounterGroup counterGroup;
    private CounterGroupExt counterGroupExt;
    private int intervalSec;
    private Set<String> moniterNames;

    public StatRunner(String name, CounterGroup counterGroup, CounterGroupExt counterExt,
            int intervalSec, Set<String> moniterNames) {
        this.counterGroup = counterGroup;
        this.counterGroupExt = counterExt;
        this.intervalSec = intervalSec;
        this.moniterNames = moniterNames;
        this.name = name;
    }

    /**
     * run
     */
    public void run() {
        HashMap<String, Long> counters = new HashMap();
        HashMap counterExt = new HashMap();

        while (!this.shutDownFlag) {
            try {
                Thread.sleep((long) (this.intervalSec * 1000));
                Iterator iterator = this.moniterNames.iterator();

                String str;
                long cnt;
                while (iterator.hasNext()) {
                    str = (String) iterator.next();
                    cnt = 0L;
                    synchronized (this.counterGroup) {
                        cnt = this.counterGroup.get(str);
                        this.counterGroup.set(str, 0L);
                        counters.put(str, cnt);
                    }
                }

                iterator = this.moniterNames.iterator();

                while (iterator.hasNext()) {
                    str = (String) iterator.next();
                    cnt = (Long) counters.get(str);
                    logger.info("{}.{}={}", new Object[]{this.name, str, cnt});
                }

                counters.clear();
                synchronized (this.counterGroupExt) {
                    Iterator ite = this.counterGroupExt.getCounters().keySet().iterator();

                    while (true) {
                        if (!ite.hasNext()) {
                            this.counterGroupExt.clear();
                            break;
                        }

                        str = (String) ite.next();
                        counterExt.put(str, this.counterGroupExt.get(str));
                    }
                }

                iterator = counterExt.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry<String, Long> entrys = (Entry) iterator.next();
                    logger.info("{}.{}={}",
                            new Object[]{this.name, entrys.getKey(), entrys.getValue()});
                }

                counterExt.clear();
            } catch (Exception var12) {
                logger.warn("statrunner interrupted");
            }
        }

    }

    public void shutDown() {
        this.shutDownFlag = true;
    }
}

