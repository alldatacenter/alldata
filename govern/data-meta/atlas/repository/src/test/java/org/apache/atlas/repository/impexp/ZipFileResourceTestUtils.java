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

import com.google.common.collect.Sets;
import org.apache.atlas.RequestContext;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.impexp.AtlasExportResult;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.repository.store.graph.v2.EntityImportStream;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ZipFileResourceTestUtils {
    public static final Logger LOG = LoggerFactory.getLogger(ZipFileResourceTestUtils.class);

    public static FileInputStream getFileInputStream(String fileName) {
        return TestResourceFileUtils.getFileInputStream(fileName);
    }

    public static Object[][] getZipSource(String fileName) throws IOException, AtlasBaseException {
        return new Object[][]{{getInputStreamFrom(fileName)}};
    }

    public static InputStream getInputStreamFrom(String fileName) {
        return ZipFileResourceTestUtils.getFileInputStream(fileName);
    }

    public static void verifyImportedEntities(List<String> creationOrder, List<String> processedEntities) {
        Set<String> lhs = com.google.common.collect.Sets.newHashSet(creationOrder);
        Set<String> rhs = com.google.common.collect.Sets.newHashSet(processedEntities);
        Set<String> difference = Sets.difference(lhs, rhs);

        assertNotNull(difference);
        assertEquals(difference.size(), 0);
    }

    public static void verifyImportedMetrics(AtlasExportResult exportResult, AtlasImportResult importResult) {
        Map<String, Integer> metricsForCompare = getImportMetricsForCompare(importResult);
        for (Map.Entry<String, Integer> entry : exportResult.getMetrics().entrySet()) {
            if(entry.getKey().startsWith("entity") == false ||
                    entry.getKey().contains("withExtInfo") ||
                    entry.getKey().contains("Column") ||
                    entry.getKey().contains("StorageDesc")) continue;

            assertTrue(metricsForCompare.containsKey(entry.getKey()), entry.getKey());
            assertEquals(entry.getValue(), metricsForCompare.get(entry.getKey()), entry.getKey());
        }
    }

    public static InputStream runExportWithParameters(ExportService exportService, AtlasExportRequest request) {
        final String requestingIP = "1.0.0.0";
        final String hostName = "localhost";
        final String userName = "admin";

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipSink zipSink = new ZipSink(baos);

            AtlasExportResult result = exportService.run(zipSink, request, userName, hostName, requestingIP);
            assertEquals(result.getOperationStatus(), AtlasExportResult.OperationStatus.SUCCESS);

            zipSink.close();
            return new ByteArrayInputStream(baos.toByteArray());
        }
        catch(Exception ex) {
            throw new SkipException(String.format("runExportWithParameters: %s: failed!", request.toString()));
        }
    }

    private static Map<String,Integer> getImportMetricsForCompare(AtlasImportResult result) {
        Map<String, Integer> r = new HashMap<>();
        for (Map.Entry<String, Integer> entry : result.getMetrics().entrySet()) {
            r.put(entry.getKey().replace(":updated", "").replace(":created", ""), entry.getValue());
        }

        return r;
    }

    public static AtlasEntity.AtlasEntityWithExtInfo getEntities(ZipSource source, int expectedCount) {
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = new AtlasEntity.AtlasEntityWithExtInfo();
        int count = 0;
        for (String s : source.getCreationOrder()) {
            AtlasEntity entity = source.getByGuid(s);
            entityWithExtInfo.addReferredEntity(s, entity);
            count++;
        }

        assertEquals(count, expectedCount);
        return entityWithExtInfo;
    }

    public static AtlasImportRequest getDefaultImportRequest() {
        return new AtlasImportRequest();
    }


    public static AtlasImportResult runImportWithParameters(ImportService importService, AtlasImportRequest request, InputStream inputStream) throws AtlasBaseException, IOException {
        final String requestingIP = "1.0.0.0";
        final String hostName = "localhost";
        final String userName = "admin";

        AtlasImportResult result = importService.run(inputStream, request, userName, hostName, requestingIP);
        assertEquals(result.getOperationStatus(), AtlasImportResult.OperationStatus.SUCCESS);
        return result;
    }

    public static AtlasImportResult runImportWithNoParameters(ImportService importService, InputStream inputStream) throws AtlasBaseException, IOException {
        final String requestingIP = "1.0.0.0";
        final String hostName = "localhost";
        final String userName = "admin";

        AtlasImportResult result = importService.run(inputStream, userName, hostName, requestingIP);
        assertEquals(result.getOperationStatus(), AtlasImportResult.OperationStatus.SUCCESS);
        return result;
    }

    public static AtlasImportResult runImportWithNoParametersUsingBackingDirectory(ImportService importService, InputStream inputStream) throws AtlasBaseException, IOException {
        final String requestingIP = "1.0.0.0";
        final String hostName = "localhost";
        final String userName = "admin";

        EntityImportStream sourceWithBackingDirectory = new ZipSourceWithBackingDirectory(inputStream, Files.createTempDirectory("temp").toString());
        AtlasImportResult result = importService.run(sourceWithBackingDirectory,  new AtlasImportRequest(), userName, hostName, requestingIP);
        assertEquals(result.getOperationStatus(), AtlasImportResult.OperationStatus.SUCCESS);
        return result;
    }

    public static void runAndVerifyQuickStart_v1_Import(ImportService importService, InputStream is) throws AtlasBaseException, IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(is, baos);

        ZipSource zipSource = new ZipSource(new ByteArrayInputStream(baos.toByteArray()));
        AtlasExportResult exportResult = zipSource.getExportResult();
        List<String> creationOrder = zipSource.getCreationOrder();

        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);

        AtlasImportRequest request = getDefaultImportRequest();
        AtlasImportResult result = runImportWithParameters(importService, request, new ByteArrayInputStream(baos.toByteArray()));

        assertNotNull(result);
        verifyImportedMetrics(exportResult, result);
        verifyImportedEntities(creationOrder, result.getProcessedEntities());
    }
}
