/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.resources;


import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasExportResult;
import org.apache.atlas.model.impexp.AtlasServer;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.repository.impexp.ZipSource;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.apache.atlas.web.integration.BaseResourceIT;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class AdminExportImportTestIT extends BaseResourceIT {
    private final String FILE_TO_IMPORT = "stocks-base.zip";
    private final String FILE_TO_IMPORT_EMPTY = "empty-1.zip";
    private final String EXPORT_REQUEST_FILE = "export-incremental";
    private final String SOURCE_SERVER_NAME = "cl1";

    static final String IMPORT_TRANSFORM_CLEAR_ATTRS =
            "{ \"Asset\": { \"*\":[ \"clearAttrValue:replicatedTo,replicatedFrom\" ] } }";
    static final String IMPORT_TRANSFORM_SET_DELETED =
            "{ \"Referenceable\": { \"*\":[ \"setDeleted\" ] } }";

    @Test
    public void isActive() throws AtlasServiceException {
        assertEquals(atlasClientV2.getAdminStatus(), "ACTIVE");
    }

    @Test(dependsOnMethods = "isActive")
    public void importData() throws AtlasServiceException {
        performImport(FILE_TO_IMPORT, 37);
        assertReplicationData("cl1");
    }

    @Test(dependsOnMethods = "isActive")
    public void importEmptyData() throws AtlasServiceException {
        AtlasImportResult result = performImportUsing(FILE_TO_IMPORT_EMPTY, new AtlasImportRequest());
        assertNotNull(result);
        assertEquals(AtlasExportResult.OperationStatus.FAIL.toString(), result.getOperationStatus().toString());
    }

    @Test(dependsOnMethods = "importData")
    public void exportData() throws AtlasServiceException, IOException, AtlasBaseException {
        final int EXPECTED_CREATION_ORDER_SIZE = 6;

        AtlasExportRequest request = TestResourceFileUtils.readObjectFromJson(".", EXPORT_REQUEST_FILE, AtlasExportRequest.class);
        InputStream exportedStream = atlasClientV2.exportData(request);
        assertNotNull(exportedStream);

        ZipSource zs = new ZipSource(exportedStream);
        assertNotNull(zs.getExportResult());
        assertTrue(zs.getCreationOrder().size() >= EXPECTED_CREATION_ORDER_SIZE, "expected creationOrderSize > " + EXPECTED_CREATION_ORDER_SIZE + ", but found " + zs.getCreationOrder().size());
    }

    @Test
    public void unAuthExportData() throws IOException {
        AtlasClientV2 unAuthClient = new AtlasClientV2(atlasUrls, new String[]{"admin", "wr0ng_pa55w0rd"});
        AtlasExportRequest request = TestResourceFileUtils.readObjectFromJson(".", EXPORT_REQUEST_FILE, AtlasExportRequest.class);
        try {
            InputStream exportedStream = unAuthClient.exportData(request);
        } catch(AtlasServiceException e) {
            assertNotNull(e.getStatus(), "expected server error code in the status");
        }
    }

    private void performImport(String fileToImport, int expectedProcessedEntitiesCount) throws AtlasServiceException {
        AtlasImportRequest request = new AtlasImportRequest();
        request.getOptions().put(AtlasImportRequest.OPTION_KEY_REPLICATED_FROM, SOURCE_SERVER_NAME);
        request.getOptions().put(AtlasImportRequest.TRANSFORMS_KEY, IMPORT_TRANSFORM_CLEAR_ATTRS);

        performImport(fileToImport, request, expectedProcessedEntitiesCount);
    }

    private void performImport(String fileToImport, AtlasImportRequest request, int expectedProcessedEntitiesCount) throws AtlasServiceException {

        AtlasImportResult result = performImportUsing(fileToImport, request);
        assertNotNull(result);
        assertEquals(result.getOperationStatus(), AtlasImportResult.OperationStatus.SUCCESS);
        assertNotNull(result.getMetrics());
        assertEquals(result.getProcessedEntities().size(), expectedProcessedEntitiesCount, "processedEntities: expected=" + expectedProcessedEntitiesCount + ", found=" + result.getProcessedEntities().size() + ". result=" + result);
    }

    private AtlasImportResult performImportUsing(String fileToImport, AtlasImportRequest request) throws AtlasServiceException {
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(TestResourceFileUtils.getTestFilePath(fileToImport));
        } catch (IOException e) {
            assertFalse(true, "Exception: " + e.getMessage());
        }

        return atlasClientV2.importData(request, fileInputStream);
    }

    private void assertReplicationData(String serverName) throws AtlasServiceException {
        AtlasServer server = atlasClientV2.getServer(serverName);
        assertNotNull(server);
        assertNotNull(server.getAdditionalInfo());
        assertTrue(server.getAdditionalInfo().size() > 0);
    }

    @AfterClass
    public void teardown() {
        AtlasImportRequest request = new AtlasImportRequest();
        request.getOptions().put(AtlasImportRequest.TRANSFORMS_KEY, IMPORT_TRANSFORM_SET_DELETED);

        try {
            performImport(FILE_TO_IMPORT, request, 32); // initial import has 5 entities already in deleted state, hence current import will have 32 processed-entities
        } catch (AtlasServiceException e) {
            throw new SkipException("performTeardown: failed! Subsequent tests results may be affected.");
        }
    }
}
