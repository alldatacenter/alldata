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

package org.apache.atlas.repository.store.graph.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasEntityHeaders;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.repository.audit.EntityAuditRepository;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.AtlasJson;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.util.CollectionUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.atlas.repository.store.graph.v2.ClassificationAssociator.Updater.PROCESS_ADD;
import static org.apache.atlas.repository.store.graph.v2.ClassificationAssociator.Updater.PROCESS_DELETE;
import static org.apache.atlas.repository.store.graph.v2.ClassificationAssociator.Updater.PROCESS_UPDATE;
import static org.apache.atlas.repository.store.graph.v2.ClassificationAssociator.Updater.STATUS_DONE;
import static org.apache.atlas.repository.store.graph.v2.ClassificationAssociator.Updater.STATUS_SKIPPED;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.FileAssert.fail;

public class ClassificationAssociatorTest {
    private static final String TABLE_GUID = "df122fc3-5555-40f8-a30f-3090b8a622f8";
    private static String TEST_FILES_SUBDIR = "classification-association";
    private static String MESSAGE_SEPARATOR = ":";
    private static String ENTITY_NAME_SEPARATOR = "->";

    private class ClassificationAssociatorUpdaterForSpy extends ClassificationAssociator.Updater {
        private final String entityFileName;

        public ClassificationAssociatorUpdaterForSpy(AtlasGraph atlasGraph, AtlasTypeRegistry typeRegistry, AtlasEntityStore entitiesStore) {
            super(atlasGraph, typeRegistry, entitiesStore);
            this.entityFileName = StringUtils.EMPTY;
        }

        public ClassificationAssociatorUpdaterForSpy(AtlasGraph atlasGraph, AtlasTypeRegistry typeRegistry, AtlasEntityStore entitiesStore, String entityFileName) {
            super(atlasGraph, typeRegistry, entitiesStore);
            this.entityFileName = entityFileName;
        }

        @Override
        AtlasEntityHeader getByUniqueAttributes(AtlasEntityType entityType, String qualifiedName, Map<String, Object> attrValues) {
            try {
                if (StringUtils.isEmpty(entityFileName)) {
                    return null;
                }

                return getEntityHeaderFromFile(entityFileName);
            } catch (IOException e) {
                fail(entityFileName + " could not be loaded.");
                return null;
            }
        }
    }

    @Test
    public void auditScanYieldsNothing_EmptyHeadersReturned() {
        AtlasEntityHeaders actualEntityHeaders = setupRetriever("header-empty", 0, 0, null);

        assertNotNull(actualEntityHeaders);
        assertEquals(actualEntityHeaders.getGuidHeaderMap().size(),0);
    }

    @Test
    public void auditScanYieldsOneEntity_EntityHeadersHasOneElementWithClassification() {
        AtlasEntityHeaders actualEntityHeaders = setupRetriever("header-Tx", 0, 0, TABLE_GUID);

        assertNotNull(actualEntityHeaders);
        assertEquals(actualEntityHeaders.getGuidHeaderMap().size(), 1);
        assertTrue(actualEntityHeaders.getGuidHeaderMap().containsKey(TABLE_GUID));
        assertEquals(actualEntityHeaders.getGuidHeaderMap().get(TABLE_GUID).getGuid(), TABLE_GUID);
        assertNotNull(actualEntityHeaders.getGuidHeaderMap().get(TABLE_GUID).getClassifications());
        assertEquals(actualEntityHeaders.getGuidHeaderMap().get(TABLE_GUID).getClassifications().size(), 1);
    }

    private AtlasEntityHeaders setupRetriever(String headersFile, int fromTimestamp, int toTimestamp, final String tableGuid) {
        AtlasEntityHeader entityHeaderWithClassification = null;
        try {
            Set<String> guids = new HashSet<String>();
            entityHeaderWithClassification = TestResourceFileUtils.readObjectFromJson(TEST_FILES_SUBDIR, headersFile, AtlasEntityHeader.class);
            if (!StringUtils.isEmpty(tableGuid)) {
                guids.add(tableGuid);
            }

            EntityAuditRepository auditRepository = mock(EntityAuditRepository.class);
            when(auditRepository.getEntitiesWithTagChanges(anyLong(), anyLong())).thenReturn(guids);

            EntityGraphRetriever entityGraphRetriever = mock(EntityGraphRetriever.class);
            when(entityGraphRetriever.toAtlasEntityHeaderWithClassifications(TABLE_GUID)).thenReturn(entityHeaderWithClassification);

            ClassificationAssociator.Retriever retriever = new ClassificationAssociator.Retriever(entityGraphRetriever, auditRepository);
            return retriever.get(fromTimestamp, toTimestamp);
        }
        catch (Exception ex) {
            fail("Exception!");
            return null;
        }
    }

    @Test
    public void updaterIncorrectType_ReturnsError() throws IOException {
        AtlasEntityHeaders entityHeaderMap = getEntityHeaderMapFromFile("header-PII");
        AtlasEntityStore entitiesStore = mock(AtlasEntityStore.class);

        AtlasTypeRegistry typeRegistry = mock(AtlasTypeRegistry.class);
        when(typeRegistry.getEntityTypeByName(anyString())).thenReturn(null);

        AtlasGraph atlasGraph = mock(AtlasGraph.class);
        ClassificationAssociator.Updater updater = new ClassificationAssociator.Updater(atlasGraph, typeRegistry, entitiesStore);
        String summary = updater.setClassifications(entityHeaderMap.getGuidHeaderMap());

        assertTrue(summary.contains("hive_"));
        assertTrue(summary.contains(STATUS_SKIPPED));
    }

    @Test
    public void updaterCorrectTypeEntityNotFound_Skipped() throws IOException {
        AtlasEntityHeaders entityHeaderMap = getEntityHeaderMapFromFile("header-PII");
        AtlasEntityType hiveTable = mock(AtlasEntityType.class);
        AtlasEntityStore entitiesStore = mock(AtlasEntityStore.class);
        AtlasTypeRegistry typeRegistry = mock(AtlasTypeRegistry.class);
        AtlasGraph atlasGraph = mock(AtlasGraph.class);

        when(typeRegistry.getEntityTypeByName(anyString())).thenReturn(hiveTable);
        when(hiveTable.getTypeName()).thenReturn("hive_column");

        ClassificationAssociatorUpdaterForSpy updater = new ClassificationAssociatorUpdaterForSpy(atlasGraph, typeRegistry, entitiesStore);
        String summary = updater.setClassifications(entityHeaderMap.getGuidHeaderMap());

        TypeReference<String[]> typeReference = new TypeReference<String[]>() {};
        String[] summaryArray = AtlasJson.fromJson(summary, typeReference);
        assertEquals(summaryArray.length, 1);
        assertSummaryElement(summaryArray[0], "Entity", STATUS_SKIPPED, "");
    }


    @Test
    public void updaterEntityWithUniqueName() throws IOException, AtlasBaseException {
        AtlasEntityDef ed = getAtlasEntityDefFromFile("col-entity-def-unique-name");

        AtlasEntityHeaders entityHeaderMap = getEntityHeaderMapFromFile("header-PII-no-qualifiedName");
        AtlasEntityStore entitiesStore = mock(AtlasEntityStore.class);
        AtlasTypeRegistry typeRegistry = new AtlasTypeRegistry();
        AtlasTypeRegistry.AtlasTransientTypeRegistry ttr = typeRegistry.lockTypeRegistryForUpdate();
        ttr.addTypes(CollectionUtils.newSingletonArrayList(ed));
        AtlasGraph atlasGraph = mock(AtlasGraph.class);

        ClassificationAssociatorUpdaterForSpy updater = new ClassificationAssociatorUpdaterForSpy(atlasGraph, ttr, entitiesStore, "col-entity-PII");
        String summary = updater.setClassifications(entityHeaderMap.getGuidHeaderMap());

        TypeReference<String[]> typeReference = new TypeReference<String[]>() {};
        String[] summaryArray = AtlasJson.fromJson(summary, typeReference);
        assertEquals(summaryArray.length, 1);
        assertSummaryElement(summaryArray[0], "Update", STATUS_DONE, "PII");
    }

    @Test
    public void updaterTests() throws IOException {
        updaterAssert("header-None", "col-entity-None");
        updaterAssert("header-PII", "col-entity-None", PROCESS_ADD + ":PII");
        updaterAssert("header-PII", "col-entity-PII", new String[]{PROCESS_UPDATE + ":PII"});
        updaterAssert("header-None", "col-entity-PII", new String[]{PROCESS_DELETE + ":PII"});
        updaterAssert("header-PII-VENDOR_PII", "col-entity-PII-FIN_PII",
                PROCESS_DELETE + ":FIN_PII",
                            PROCESS_UPDATE + ":PII",
                            PROCESS_ADD + ":VENDOR_PII");
        updaterAssert("header-None", "col-entity-None", new String[]{});
        updaterAssert("header-FIN_PII", "col-entity-PII",
                        PROCESS_DELETE + ":PII",
                                    PROCESS_ADD + ":FIN_PII");
    }

    @Test
    public void updater_filterPropagatedClassifications() throws IOException {
        updaterAssert("header-Tx-prop-T1", "col-entity-T1-prop-Tn",
                PROCESS_DELETE + ":T1",
                            PROCESS_ADD + ":Tx");
        updaterAssert("header-Tx-prop-T1-No-Guid", "col-entity-T1-prop-Tn-No-Guid",
                PROCESS_DELETE + ":Tn",
                             PROCESS_UPDATE + ":T1",
                             PROCESS_ADD + ":Tx");
    }


    private void assertSummaryElement(String summaryElement, String operation, String status, String classificationName) {
        String[] splits = StringUtils.split(summaryElement, MESSAGE_SEPARATOR);
        String[] nameSplits = StringUtils.split(splits[3], ENTITY_NAME_SEPARATOR);
        if (nameSplits.length > 1) {
            assertEquals(nameSplits[1].trim(), classificationName);
        }

        assertEquals(splits[0], operation);
        assertEquals(splits[4], status);
    }

    private String[] setupUpdater(String entityHeaderFileName, String entityFileName, int expectedSummaryLength) throws IOException {
        AtlasEntityHeaders entityHeaderMap = getEntityHeaderMapFromFile(entityHeaderFileName);

        AtlasEntityType hiveTable = mock(AtlasEntityType.class);
        AtlasEntityStore entitiesStore = mock(AtlasEntityStore.class);
        AtlasTypeRegistry typeRegistry = mock(AtlasTypeRegistry.class);
        AtlasGraph atlasGraph = mock(AtlasGraph.class);

        when(typeRegistry.getEntityTypeByName(anyString())).thenReturn(hiveTable);
        when(hiveTable.getTypeName()).thenReturn("hive_column");

        ClassificationAssociatorUpdaterForSpy updater = new ClassificationAssociatorUpdaterForSpy(atlasGraph, typeRegistry, entitiesStore, entityFileName);
        String summary = updater.setClassifications(entityHeaderMap.getGuidHeaderMap());

        TypeReference<String[]> typeReference = new TypeReference<String[]>() {};
        String[] summaryArray = AtlasJson.fromJson(summary, typeReference);
        assertEquals(summaryArray.length, expectedSummaryLength);

        return summaryArray;
    }

    private AtlasEntityHeader getEntityHeaderFromFile(String entityJson) throws IOException {
        return TestResourceFileUtils.readObjectFromJson(TEST_FILES_SUBDIR, entityJson, AtlasEntityHeader.class);
    }

    private AtlasEntityHeaders getEntityHeaderMapFromFile(String filename) throws IOException {
        return TestResourceFileUtils.readObjectFromJson(TEST_FILES_SUBDIR, filename, AtlasEntityHeaders.class);
    }
    private AtlasEntityDef getAtlasEntityDefFromFile(String filename) throws IOException {
        return TestResourceFileUtils.readObjectFromJson(TEST_FILES_SUBDIR, filename, AtlasEntityDef.class);
    }

    private void updaterAssert(String incoming, String entity, String... opNamePair) throws IOException {
        String[] summary = setupUpdater(incoming, entity, opNamePair.length);

        for (int i = 0; i < opNamePair.length; i++) {
            String s = opNamePair[i];
            String[] splits = StringUtils.split(s, ":");
            assertSummaryElement(summary[i], splits[0], STATUS_DONE, splits[1]);
        }
    }
}
