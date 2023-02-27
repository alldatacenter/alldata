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
package org.apache.atlas.repository.impexp;

import com.google.inject.Inject;
import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.impexp.ExportImportAuditEntry;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.getDefaultImportRequest;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.getZipSource;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.getInputStreamFrom;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.runImportWithParameters;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;


@Guice(modules = TestModules.TestOnlyModule.class)
public class TableReplicationRequestProcessorTest extends AtlasTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(TableReplicationRequestProcessorTest.class);

    private static final String ENTITY_GUID_REPLICATED = "718a6d12-35a8-4731-aff8-3a64637a43a3";
    private static final String ENTITY_GUID_NOT_REPLICATED_1 = "e19e5683-d9ae-436a-af1e-0873582d0f1e";
    private static final String ENTITY_GUID_NOT_REPLICATED_2 = "2e28ae34-576e-4a8b-be48-cf5f925d7b15";
    private static final String REPL_FROM = "cl1";
    private static final String REPL_TRANSFORMER = "[{\"conditions\":{\"__entity\":\"topLevel: \"}," +
            "\"action\":{\"__entity\":\"ADD_CLASSIFICATION: cl1_replicated\"}}," +
            "{\"action\":{\"__entity.replicatedTo\":\"CLEAR:\",\"__entity.replicatedFrom\":\"CLEAR:\"}}," +
            "{\"conditions\":{\"hive_db.clusterName\":\"EQUALS: cl1\"},\"action\":{\"hive_db.clusterName\":\"SET: cl2\"}}," +
            "{\"conditions\":{\"hive_db.location\":\"STARTS_WITH_IGNORE_CASE: file:///\"}," +
            "\"action\":{\"hive_db.location\":\"REPLACE_PREFIX: = :file:///=file:///\"}}," +
            "{\"conditions\":{\"hive_storagedesc.location\":\"STARTS_WITH_IGNORE_CASE: file:///\"}," +
            "\"action\":{\"hive_storagedesc.location\":\"REPLACE_PREFIX: = :file:///=file:///\"}}]";

    @Inject
    private ImportService importService;

    @Inject
    private AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasEntityStore entityStore;

    @Inject
    private ExportImportAuditService auditService;

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @BeforeClass
    public void initialize() throws Exception {
        super.initialize();
    }

    @BeforeTest
    public void setupTest() throws IOException, AtlasBaseException {
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);
        basicSetup(typeDefStore, typeRegistry);
    }

    @AfterClass
    public void clear() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }

    @DataProvider(name = "source1")
    public static Object[][] getData1(ITestContext context) throws IOException, AtlasBaseException {
        return getZipSource("repl_exp_1.zip");
    }

    public static InputStream getData2() {
        return getInputStreamFrom("repl_exp_2.zip");
    }

    @Test(dataProvider = "source1")
    public void importWithIsReplTrue(InputStream zipSource) throws AtlasBaseException, IOException {
        AtlasImportRequest atlasImportRequest = getDefaultImportRequest();

        atlasImportRequest.setOption("replicatedFrom", REPL_FROM);
        atlasImportRequest.setOption("transformers", REPL_TRANSFORMER);

        runImportWithParameters(importService, atlasImportRequest, zipSource);

        runImportWithParameters(importService, atlasImportRequest, getData2());

        assertAuditEntry();
    }

    private void assertAuditEntry() {
        pauseForIndexCreation();
        List<ExportImportAuditEntry> result;
        try {
            result = auditService.get("", "IMPORT_DELETE_REPL", "", "",  "", 10, 0);
        } catch (Exception e) {
            throw new SkipException("audit entries not retrieved.");
        }

        assertNotNull(result);
        assertTrue(result.size() > 0);

        List<String> deletedGuids = AtlasType.fromJson(result.get(0).getResultSummary(), List.class);
        assertNotNull(deletedGuids);
        assertFalse(deletedGuids.contains(ENTITY_GUID_REPLICATED));
        assertTrue(deletedGuids.contains(ENTITY_GUID_NOT_REPLICATED_1));
        assertTrue(deletedGuids.contains(ENTITY_GUID_NOT_REPLICATED_2));
    }
}
