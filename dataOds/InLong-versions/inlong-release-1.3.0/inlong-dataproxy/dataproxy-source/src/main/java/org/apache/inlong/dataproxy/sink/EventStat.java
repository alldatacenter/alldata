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

package org.apache.inlong.dataproxy.sink;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.flume.Event;
import org.apache.inlong.dataproxy.base.OrderEvent;
import org.apache.inlong.dataproxy.utils.MessageUtils;

public class EventStat {

    private static long RETRY_INTERVAL_MS = 1 * 1000L;
    private Event event;
    private AtomicInteger myRetryCnt = new AtomicInteger(0);
    private boolean isOrderMessage = false;

    public EventStat(Event event) {
        this.event = event;
        this.isOrderMessage = MessageUtils
                .isSyncSendForOrder(event) && (event instanceof OrderEvent);
    }

    public EventStat(Event event, int retryCnt) {
        this.event = event;
        this.myRetryCnt.set(retryCnt);
        this.isOrderMessage = MessageUtils
                .isSyncSendForOrder(event) && (event instanceof OrderEvent);
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public int getRetryCnt() {
        return myRetryCnt.get();
    }

    public void setRetryCnt(int retryCnt) {
        this.myRetryCnt.set(retryCnt);
    }

    public void incRetryCnt() {
        this.myRetryCnt.incrementAndGet();
    }

    public boolean isOrderMessage() {
        return isOrderMessage;
    }

    public boolean shouldDrop() {
        return false;
    }

    public void reset() {
        this.event = null;
        this.myRetryCnt.set(0);
    }
}
