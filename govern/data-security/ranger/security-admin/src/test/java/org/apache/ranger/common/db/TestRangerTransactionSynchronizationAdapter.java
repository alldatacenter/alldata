/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ranger.common.db;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

public class TestRangerTransactionSynchronizationAdapter {

    @Test
    public void testNestedRunnableAfterCompletion() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            RangerTransactionSynchronizationAdapter rtsa = new RangerTransactionSynchronizationAdapter();
            rtsa.txManager = Mockito.mock(PlatformTransactionManager.class);
            AtomicInteger count = new AtomicInteger(0);
            rtsa.executeOnTransactionCompletion(() -> {
                count.incrementAndGet();
                rtsa.executeOnTransactionCompletion(count::incrementAndGet);
            });
            rtsa.afterCompletion(0);
            Assert.assertEquals(1, count.get());
        } finally {
            TransactionSynchronizationManager.clear();
        }

    }
}
