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

import org.testng.annotations.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class WorkItemConsumerTest {

    static class IntegerConsumerSpy extends WorkItemConsumer<Integer> {
        boolean commitDirtyCalled = false;
        private boolean updateCommitTimeCalled;

        public IntegerConsumerSpy(BlockingQueue<Integer> queue) {
            super(queue);
            setCountDownLatch(new CountDownLatch(1));
        }

        @Override
        protected void doCommit() {

        }

        @Override
        protected void processItem(Integer item) {

        }

        @Override
        protected void commitDirty() {
            commitDirtyCalled = true;
            super.commitDirty();
        }

        @Override
        protected void updateCommitTime(long commitTime) {
            updateCommitTimeCalled = true;
        }

        public boolean isCommitDirtyCalled() {
            return commitDirtyCalled;
        }

        public boolean isUpdateCommitTimeCalled() {
            return updateCommitTimeCalled;
        }
    }


    @Test
    public void callingRunOnEmptyQueueCallsDoesNotCallCommitDirty() {
        BlockingQueue<Integer> bc = new LinkedBlockingQueue<>(5);

        IntegerConsumerSpy ic = new IntegerConsumerSpy(bc);
        ic.run();

        assertTrue(bc.isEmpty());
        assertTrue(ic.isCommitDirtyCalled());
        assertFalse(ic.isUpdateCommitTimeCalled());
    }


    @Test
    public void runOnQueueRemovesItemFromQueuCallsCommitDirty() {
        BlockingQueue<Integer> bc = new LinkedBlockingQueue<>(5);
        bc.add(1);

        IntegerConsumerSpy ic = new IntegerConsumerSpy(bc);
        ic.run();

        assertTrue(bc.isEmpty());
        assertTrue(ic.isCommitDirtyCalled());
        assertTrue(ic.isUpdateCommitTimeCalled());
    }
}
