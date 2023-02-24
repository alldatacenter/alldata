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

package org.apache.atlas.repository.impexp;

import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static org.apache.atlas.utils.TestLoadModelUtils.loadFsModel;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

@Guice(modules = TestModules.TestOnlyModule.class)
public class ImportTransformsShaperTest extends AtlasTestBase {
    @Inject
    AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @Inject
    private ImportService importService;

    @Inject
    private AtlasEntityStore entityStore;

    private final String TAG_NAME = "REPLICATED";

    @BeforeClass
    public void setup() throws IOException, AtlasBaseException {
        basicSetup(typeDefStore, typeRegistry);
        loadFsModel(typeDefStore, typeRegistry);
    }

    @Test
    public void newTagIsCreatedAndEntitiesAreTagged() throws AtlasBaseException, IOException {
        AtlasImportResult result = ZipFileResourceTestUtils.runImportWithParameters(importService,
                getImporRequest(),
                ZipFileResourceTestUtils.getInputStreamFrom("stocks.zip"));

        AtlasClassificationType classification = typeRegistry.getClassificationTypeByName(TAG_NAME);
        assertNotNull(classification);
        assertEntities(result.getProcessedEntities(), TAG_NAME);
    }

    private void assertEntities(List<String> entityGuids, String tagName) throws AtlasBaseException {
        for (String guid : entityGuids) {
            AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = this.entityStore.getById(guid);

            assertNotNull(entityWithExtInfo);
            assertTag(entityWithExtInfo, tagName);
        }
    }

    private void assertTag(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo, String tagName) {
        if(entityWithExtInfo.getReferredEntities() == null || entityWithExtInfo.getReferredEntities().size() == 0) {
            return;
        }

        for (AtlasEntity entity : entityWithExtInfo.getReferredEntities().values()) {
            assertTag(entity, tagName);
        }
    }

    private void assertTag(AtlasEntity entity, String tagName) {

        assertTrue(entity.getClassifications().size() > 0,
                String.format("%s not tagged", entity.getTypeName()));
        assertEquals(entity.getClassifications().get(0).getTypeName(), tagName);
    }


    private AtlasImportRequest getImporRequest() {
        AtlasImportRequest request = new AtlasImportRequest();
        request.getOptions().put("transforms", "{ \"Referenceable\": { \"*\":[ \"addClassification:REPLICATED\" ] } }");
        return request;
    }

}
