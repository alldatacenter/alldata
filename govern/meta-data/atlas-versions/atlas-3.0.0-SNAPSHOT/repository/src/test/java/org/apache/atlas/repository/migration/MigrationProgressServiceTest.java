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
package org.apache.atlas.repository.migration;

import org.apache.atlas.model.impexp.MigrationStatus;
import org.apache.atlas.repository.graphdb.*;
import org.apache.atlas.repository.graphdb.janus.migration.ReaderStatusManager;
import org.apache.atlas.repository.impexp.MigrationProgressService;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.testng.annotations.Test;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class MigrationProgressServiceTest {

    private final long  currentIndex    = 100l;
    private final long  totalIndex      = 1000l;
    private final long  increment       = 1001l;
    private final String statusSuccess  = ReaderStatusManager.STATUS_SUCCESS;

    private GraphDBMigrator createMigrator(TinkerGraph tg) {
        GraphDBMigrator gdm = mock(GraphDBMigrator.class);
        when(gdm.getMigrationStatus()).thenAnswer(invocation -> ReaderStatusManager.get(tg));
        return gdm;
    }

    @Test
    public void absentStatusNodeReturnsDefaultStatus() {
        MigrationProgressService mps = getMigrationStatusForTest(null, null);
        MigrationStatus ms = mps.getStatus();

        assertNotNull(ms);
        assertTrue(StringUtils.isEmpty(ms.getOperationStatus()));
        assertEquals(ms.getCurrentIndex(), 0);
        assertEquals(ms.getTotalCount(), 0);
    }

    @Test
    public void existingStatusNodeRetrurnStatus() {
        final long currentIndex = 100l;
        final long totalIndex = 1000l;
        final String status = ReaderStatusManager.STATUS_SUCCESS;

        TinkerGraph tg = createUpdateStatusNode(null, currentIndex, totalIndex, status);
        MigrationProgressService mps = getMigrationStatusForTest(null, tg);
        MigrationStatus ms = mps.getStatus();

        assertMigrationStatus(totalIndex, status, ms);
    }

    @Test
    public void cachedStatusReturnedIfQueriedBeforeCacheExpiration() {
        TinkerGraph tg = createUpdateStatusNode(null, currentIndex, totalIndex, statusSuccess);

        MigrationProgressService mps = getMigrationStatusForTest(null, tg);
        MigrationStatus ms = mps.getStatus();

        createUpdateStatusNode(tg, currentIndex + increment, totalIndex + increment, ReaderStatusManager.STATUS_FAILED);
        MigrationStatus ms2 = mps.getStatus();

        assertEquals(ms.hashCode(), ms2.hashCode());
        assertMigrationStatus(totalIndex, statusSuccess, ms);
    }

    private MigrationProgressService getMigrationStatusForTest(Configuration cfg, TinkerGraph tg) {
        return new MigrationProgressService(cfg, createMigrator(tg));
    }

    @Test
    public void cachedUpdatedIfQueriedAfterCacheExpiration() throws InterruptedException {
        final String statusFailed = ReaderStatusManager.STATUS_FAILED;

        TinkerGraph tg = createUpdateStatusNode(null, currentIndex, totalIndex, statusSuccess);
        long cacheTTl = 100l;
        MigrationProgressService mps = getMigrationStatusForTest(getStubConfiguration(cacheTTl), tg);
        MigrationStatus ms = mps.getStatus();

        assertMigrationStatus(totalIndex, statusSuccess, ms);

        createUpdateStatusNode(tg, currentIndex + increment, totalIndex + increment, ReaderStatusManager.STATUS_FAILED);
        Thread.sleep(2 * cacheTTl);

        MigrationStatus ms2 = mps.getStatus();

        assertNotEquals(ms.hashCode(), ms2.hashCode());

        assertMigrationStatus(totalIndex + increment, statusFailed, ms2);
    }

    private Configuration getStubConfiguration(long ttl) {
        Configuration cfg = mock(Configuration.class);
        when(cfg.getLong(anyString(), anyLong())).thenReturn(ttl);
        return cfg;
    }

    private TinkerGraph createUpdateStatusNode(TinkerGraph tg, long currentIndex, long totalIndex, String status) {
        if(tg == null) {
            tg = TinkerGraph.open();
        }

        ReaderStatusManager rsm = new ReaderStatusManager(tg, tg);
        rsm.update(tg, currentIndex, false);
        rsm.end(tg, totalIndex, status);
        return tg;
    }

    private void assertMigrationStatus(long totalIndex, String status, MigrationStatus ms) {
        assertNotNull(ms);
        assertEquals(ms.getOperationStatus(), status);
        assertEquals(ms.getCurrentIndex(), totalIndex);
        assertEquals(ms.getTotalCount(), totalIndex);
    }
}
