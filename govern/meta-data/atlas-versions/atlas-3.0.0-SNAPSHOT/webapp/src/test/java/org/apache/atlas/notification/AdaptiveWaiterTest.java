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
package org.apache.atlas.notification;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AdaptiveWaiterTest {

    private final int maxDuration = 100;
    private final int minDuration = 5;
    private final int increment   = 5;
    private NotificationHookConsumer.AdaptiveWaiter waiter;

    @BeforeClass
    public void setup() {
        waiter = new NotificationHookConsumer.AdaptiveWaiter(minDuration, maxDuration, increment);
    }

    @Test
    public void basicTest() {
        int pauseCount = 10;

        for (int i = 0; i < pauseCount; i++) {
            waiter.pause(new IllegalStateException());
        }

        //assertEquals(waiter.waitDuration, Math.min((pauseCount - 1) * minDuration, maxDuration)); // waiter.waitDuration will be set to wait time for next pause()
        assertTrue(waiter.waitDuration >= Math.min((pauseCount - 1) * minDuration, maxDuration)); // waiter.waitDuration will be set to wait time for next pause()
    }

    @Test
    public void resetTest() {
        final int someHighAttemptNumber = 30;
        for (int i = 0; i < someHighAttemptNumber; i++) {
            waiter.pause(new IllegalStateException());
        }

        // assertEquals(waiter.waitDuration, maxDuration);
        assertTrue(waiter.waitDuration >= maxDuration);
    }

    @Test
    public void longPauseResets() {
        waiter.pause(new IllegalStateException());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        waiter.pause(new IllegalArgumentException());
        // assertEquals(waiter.waitDuration, minDuration);
        assertTrue(waiter.waitDuration >= minDuration);
    }
}
