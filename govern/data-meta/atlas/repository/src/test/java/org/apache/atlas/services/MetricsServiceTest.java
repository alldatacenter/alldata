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
package org.apache.atlas.services;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.metrics.AtlasMetrics;
import org.apache.atlas.model.metrics.AtlasMetricsStat;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.impexp.ImportService;
import org.apache.atlas.repository.impexp.ZipFileResourceTestUtils;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.util.AtlasMetricsCounter;
import org.apache.atlas.util.AtlasMetricsUtil;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.apache.atlas.model.metrics.AtlasMetrics.*;
import static org.apache.atlas.services.MetricsService.*;
import static org.apache.atlas.utils.TestLoadModelUtils.loadModelFromJson;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.runImportWithNoParameters;
import static org.testng.Assert.*;

@Guice(modules = TestModules.TestOnlyModule.class)
public class MetricsServiceTest extends AtlasTestBase {

    public static final String IMPORT_FILE = "metrics-entities-data.zip";

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @Inject
    private AtlasTypeRegistry typeRegistry;

    @Inject
    private ImportService importService;

    @Inject
    private MetricsService metricsService;

    @Inject
    private AtlasMetricsUtil metricsUtil;

    TestClock clock = new TestClock(Clock.systemUTC(), ZoneOffset.UTC);

    long msgOffset = 0;


    private final Map<String, Long> activeEntityMetricsExpected = new HashMap<String, Long>() {{
        put("hive_storagedesc", 5L);
        put("AtlasServer", 1L);
        put("hive_column_lineage", 8L);
        put("hive_table", 5L);
        put("hive_column", 13L);
        put("hive_db", 2L);
        put("hive_process", 3L);
    }};

    private final Map<String, Long> deletedEntityMetricsExpected = new HashMap<String, Long>() {{
        put("hive_storagedesc", 1L);
        put("hive_table", 1L);
        put("hive_column", 2L);
        put("hive_db", 1L);
    }};


    private final Map<String, Long> tagMetricsExpected = new HashMap<String, Long>() {{
        put("PII", 1L);
    }};

    private final Map<String, Object> metricExpected = new HashMap<String, Object>() {{
        put(STAT_NOTIFY_COUNT_CURR_HOUR, 11L);
        put(STAT_NOTIFY_FAILED_COUNT_CURR_HOUR, 1L);
        put(STAT_NOTIFY_COUNT_PREV_HOUR, 11L);
        put(STAT_NOTIFY_FAILED_COUNT_PREV_HOUR, 1L);
        put(STAT_NOTIFY_COUNT_CURR_DAY, 33L);
        put(STAT_NOTIFY_FAILED_COUNT_CURR_DAY, 3L);
        put(STAT_NOTIFY_COUNT_PREV_DAY, 11L);
        put(STAT_NOTIFY_FAILED_COUNT_PREV_DAY, 1L);
    }};

    private AtlasMetrics metrics;
    private AtlasMetricsStat blankMetricsStat, metricsStatInGraph;

    @BeforeClass
    public void setup() throws Exception {
        RequestContext.clear();

        super.initialize();

        loadModelFilesAndImportTestData();

        // sleep for sometime for import to complete
        sleep();
    }

    private void sleep() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public void clear() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }

    @Test(groups = "Metrics.CREATE")
    public void testGetMetrics() {
        metrics = metricsService.getMetrics();

        assertNotNull(metrics);

        // general metrics
        assertEquals(metrics.getNumericMetric(GENERAL, METRIC_ENTITY_COUNT).intValue(), 42);
        assertEquals(metrics.getNumericMetric(GENERAL, METRIC_TAG_COUNT).intValue(), 1);
        assertTrue(metrics.getNumericMetric(GENERAL, METRIC_TYPE_UNUSED_COUNT).intValue() >= 10);
        assertTrue(metrics.getNumericMetric(GENERAL, METRIC_TYPE_COUNT).intValue() >= 44);

        // tag metrics
        Map tagMetricsActual           = (Map) metrics.getMetric(TAG, METRIC_ENTITIES_PER_TAG);
        Map activeEntityMetricsActual  = (Map) metrics.getMetric(ENTITY, METRIC_ENTITY_ACTIVE);
        Map deletedEntityMetricsActual = (Map) metrics.getMetric(ENTITY, METRIC_ENTITY_DELETED);

        assertEquals(tagMetricsActual.size(), 1);
        assertEquals(activeEntityMetricsActual.size(), 7);
        assertEquals(deletedEntityMetricsActual.size(), 4);

        assertEquals(tagMetricsActual, tagMetricsExpected);
        assertEquals(activeEntityMetricsActual, activeEntityMetricsExpected);
        assertEquals(deletedEntityMetricsActual, deletedEntityMetricsExpected);
    }

    @Test(groups = "Metrics.CREATE", dependsOnMethods = "testGetMetrics")
    public void testSaveMetricsStat() {
        try {
            blankMetricsStat = new AtlasMetricsStat(metrics);
            metricsStatInGraph = metricsService.saveMetricsStat(blankMetricsStat);
        } catch (AtlasBaseException e) {
            fail("Save metricsStat should've succeeded", e);
        }

        // Duplicate create calls should fail
        try {
            AtlasMetricsStat blankMetricsStatDup = new AtlasMetricsStat(metrics);
            metricsService.saveMetricsStat(blankMetricsStatDup);
            fail("Save duplicate metricsStat should've failed");
        } catch (AtlasBaseException e) {
            assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.METRICSSTAT_ALREADY_EXISTS);
        }
    }

    @Test(groups = "Metrics.CREATE", dependsOnMethods = "testSaveMetricsStat")
    public void testGetMetricsStatByCollectionTime() {
        // collectionTime is empty string
        try {
            AtlasMetricsStat metricsStatRet = metricsService.getMetricsStatByCollectionTime("  ");
            fail("Get metricsStat by collectionTime should've failed, when collectionTime is empty.");
        } catch (AtlasBaseException e) {
            assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.INVALID_PARAMETERS);
        }

        // collectionTime is null
        try {
            AtlasMetricsStat metricsStatRet = metricsService.getMetricsStatByCollectionTime(null);
            fail("Get metricsStat by collectionTime should've failed, when collectionTime is null.");
        } catch (AtlasBaseException e) {
            assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.INVALID_PARAMETERS);
        }

        // collectionTime is NOT existed
        try {
            Long collectionTimeInGraph = System.currentTimeMillis();
            AtlasMetricsStat metricsStatRet = metricsService.getMetricsStatByCollectionTime(String.valueOf(collectionTimeInGraph));
            fail("Get metricsStat by collectionTime should've failed, when collectionTime is NOT existed.");
        } catch (AtlasBaseException e) {
            assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.INSTANCE_BY_UNIQUE_ATTRIBUTE_NOT_FOUND);
        }

        // collectionTime is correct
        try {
            Long collectionTimeInGraph = (Long) metrics.getMetric(GENERAL, METRIC_COLLECTION_TIME);
            AtlasMetricsStat metricsStatRet = metricsService.getMetricsStatByCollectionTime(String.valueOf(collectionTimeInGraph));
            assertNotNull(metricsStatRet);
            assertEquals(metricsStatRet.getGuid(), metricsStatInGraph.getGuid());
            assertEquals(metricsStatRet.getMetricsId(), metricsStatInGraph.getMetricsId());
        } catch (AtlasBaseException e) {
            fail("Get metricsStat by valid collectionTime in Graph should've succeeded.");
        }
    }

    @Test
    public void testNotificationMetrics() {
        Instant now           = Clock.systemUTC().instant();
        Instant dayStartTime  = AtlasMetricsCounter.getDayStartTime(now);
        Instant dayEndTime    = AtlasMetricsCounter.getNextDayStartTime(now);
        Instant hourStartTime = dayEndTime.minusSeconds(60 * 60);

        prepareNotificationData(dayStartTime, hourStartTime);

        clock.setInstant(dayEndTime.minusSeconds(1));

        Map<String, Object> notificationMetricMap = metricsUtil.getStats();

        clock.setInstant(null);

        verifyNotificationMetric(metricExpected, notificationMetricMap);
    }


    private void loadModelFilesAndImportTestData() {
        try {
            loadModelFromJson("0000-Area0/0010-base_model.json", typeDefStore, typeRegistry);
            loadModelFromJson("0000-Area0/patches/001-base_model_replication_attributes.json", typeDefStore, typeRegistry);
            loadModelFromJson("1000-Hadoop/1020-fs_model.json", typeDefStore, typeRegistry);
            loadModelFromJson("1000-Hadoop/1030-hive_model.json", typeDefStore, typeRegistry);
            loadModelFromJson("1000-Hadoop/patches/001-hive_column_add_position.json", typeDefStore, typeRegistry);
            loadModelFromJson("1000-Hadoop/patches/002-hive_column_table_add_options.json", typeDefStore, typeRegistry);
            loadModelFromJson("1000-Hadoop/patches/003-hive_column_update_table_remove_constraint.json", typeDefStore, typeRegistry);

            runImportWithNoParameters(importService, getZipSource(IMPORT_FILE));
        } catch (AtlasBaseException | IOException e) {
            throw new SkipException("Model loading failed!");
        }
    }

    private void prepareNotificationData(Instant dayStartTime, Instant hourStartTime) {
        Instant prevDayStartTime = AtlasMetricsCounter.getDayStartTime(dayStartTime.minusSeconds(1));

        msgOffset = 0;

        clock.setInstant(prevDayStartTime);
        metricsUtil.init(clock);
        clock.setInstant(null);

        processMessage(prevDayStartTime.plusSeconds(3)); // yesterday
        processMessage(dayStartTime.plusSeconds(3));     // today
        processMessage(hourStartTime.minusSeconds(3));   // past hour
        processMessage(hourStartTime.plusSeconds(3));    // this hour
    }

    private void processMessage(Instant instant) {
        clock.setInstant(instant);

        metricsUtil.onNotificationProcessingComplete("ATLAS_HOOK", 0, ++msgOffset, new AtlasMetricsUtil.NotificationStat(true, 1));

        for (int i = 0; i < 10; i++) {
            metricsUtil.onNotificationProcessingComplete("ATLAS_HOOK", 0, msgOffset++, new AtlasMetricsUtil.NotificationStat(false, 1));
        }

        clock.setInstant(null);
    }

    private void verifyNotificationMetric(Map<String, Object> metricExpected, Map<String, Object> notificationMetrics) {
        assertNotNull(notificationMetrics);
        assertNotEquals(notificationMetrics.size(), 0);
        assertTrue(notificationMetrics.size() >= metricExpected.size());

        for (Map.Entry<String, Object> entry : metricExpected.entrySet()) {
            assertEquals(notificationMetrics.get(entry.getKey()), entry.getValue(), entry.getKey());
        }
    }

    public static InputStream getZipSource(String fileName) throws AtlasBaseException {
        return ZipFileResourceTestUtils.getFileInputStream(fileName);
    }

    private static class TestClock extends Clock {
        private final Clock   baseClock;
        private final ZoneId  zone;
        private       Instant instant = null;

        public TestClock(Clock baseClock, ZoneId zone) {
            this.baseClock = baseClock;
            this.zone      = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public TestClock withZone(ZoneId zone) {
            return new TestClock(baseClock, zone);
        }

        @Override
        public Instant instant() {
            return instant != null ? instant : baseClock.instant();
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }
    }
}