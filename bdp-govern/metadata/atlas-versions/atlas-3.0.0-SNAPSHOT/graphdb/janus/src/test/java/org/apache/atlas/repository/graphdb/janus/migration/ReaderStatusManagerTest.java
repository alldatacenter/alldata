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

package org.apache.atlas.repository.graphdb.janus.migration;

import org.apache.atlas.model.impexp.MigrationStatus;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class ReaderStatusManagerTest {
    @Test
    public void createsNewStatusNode() {
        TinkerGraph tg = TinkerGraph.open();
        ReaderStatusManager sm = new ReaderStatusManager(tg, tg);
        assertEquals(sm.getStartIndex(), 0L);

        assertNotNull(tg.traversal().V(sm.migrationStatusId).next());

        MigrationStatus ms = ReaderStatusManager.get(tg);
        assertEquals(ms.getCurrentIndex(), 0L);
        assertEquals(ms.getTotalCount(), 0L);
        assertEquals(ms.getOperationStatus(), ReaderStatusManager.STATUS_NOT_STARTED);
        assertNotNull(ms.getStartTime());
        assertNotNull(ms.getEndTime());
    }

    @Test
    public void verifyUpdates() {
        long expectedTotalCount = 1001L;
        String expectedOperationStatus = ReaderStatusManager.STATUS_SUCCESS;

        TinkerGraph tg = TinkerGraph.open();
        ReaderStatusManager sm = new ReaderStatusManager(tg, tg);

        sm.update(tg, 1000L, ReaderStatusManager.STATUS_IN_PROGRESS);
        sm.end(tg, expectedTotalCount, expectedOperationStatus);

        MigrationStatus ms = ReaderStatusManager.get(tg);
        assertEquals(ms.getCurrentIndex(), expectedTotalCount);
        assertEquals(ms.getTotalCount(), expectedTotalCount);
        assertEquals(ms.getOperationStatus(), expectedOperationStatus);
    }
}
