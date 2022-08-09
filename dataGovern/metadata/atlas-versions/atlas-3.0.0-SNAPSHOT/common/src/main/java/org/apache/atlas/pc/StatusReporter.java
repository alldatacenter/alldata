/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.pc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class StatusReporter<T, U> {
    private static final Logger LOG = LoggerFactory.getLogger(StatusReporter.class);

    private Map<T,U> producedItems = new LinkedHashMap<>();
    private Set<T> processedSet = new HashSet<>();
    private long timeoutDuration;
    private long lastAck;

    public StatusReporter() {
        this.timeoutDuration = -1;
    }

    public StatusReporter(long timeoutDurationInMs) {
        this.timeoutDuration = timeoutDurationInMs;
    }

    public void produced(T item, U index) {
        this.producedItems.put(item, index);
    }

    public void processed(T item) {
        this.processedSet.add(item);
    }

    public void processed(T[] index) {
        this.processedSet.addAll(Arrays.asList(index));
    }

    public U ack() {
        U ack = null;
        U ret;
        do {
            Map.Entry<T, U> firstElement = getFirstElement(this.producedItems);
            ret = completionIndex(firstElement);
            if (ret != null) {
                ack = ret;
            }
        } while(ret != null);

        return ack;
    }

    private Map.Entry<T, U> getFirstElement(Map<T, U> map) {
        if (map.isEmpty()) {
            return null;
        }

        return map.entrySet().iterator().next();
    }

    private U completionIndex(Map.Entry<T, U> lookFor) {
        U ack = null;
        if (lookFor == null) {
            return ack;
        }

        if (hasTimeoutDurationReached(System.currentTimeMillis())) {
            LOG.warn("Ack: Timeout: {} - {}", lookFor.getKey(), lookFor.getValue());
            return acknowledged(lookFor);
        }

        if (!processedSet.contains(lookFor.getKey())) {
            return ack;
        }

        return acknowledged(lookFor);
    }

    private U acknowledged(Map.Entry<T, U> lookFor) {
        U ack = lookFor.getValue();
        producedItems.remove(lookFor.getKey());
        processedSet.remove(lookFor.getKey());
        return ack;
    }

    private boolean hasTimeoutDurationReached(long now) {
        boolean b = (this.timeoutDuration > -1) && (this.lastAck != 0) && ((now - this.lastAck) >= timeoutDuration);
        lastAck = System.currentTimeMillis();
        return b;
    }

    public int getProducedCount() {
        return this.producedItems.size();
    }

    public int getProcessedCount() {
        return this.processedSet.size();
    }
}
