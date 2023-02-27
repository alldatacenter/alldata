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


import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.impexp.AtlasExportResult;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.type.AtlasType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ZipSinkTest {
    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private ZipSink zipSink;
    private List<String> defaultExportOrder = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
    private AtlasExportResult defaultExportResult;
    private String knownEntityGuidFormat = "111-222-333-%s";

    private void initZipSinkWithExportOrder() throws AtlasBaseException {
        zipSink = new ZipSink(byteArrayOutputStream);
        zipSink.setExportOrder(defaultExportOrder);
        zipSink.close();
    }

    private AtlasExportResult getDefaultExportResult() {
        AtlasExportRequest request = new AtlasExportRequest();

        List<AtlasObjectId> itemsToExport = new ArrayList<>();
        itemsToExport.add(new AtlasObjectId("hive_db", "qualifiedName", "default"));
        request.setItemsToExport(itemsToExport);

        defaultExportResult = new AtlasExportResult(request, "admin", "1.0.0.0", "root", 100, 0L);
        return defaultExportResult;
    }

    private ZipInputStream getZipInputStreamForDefaultExportOrder() throws AtlasBaseException {
        initZipSinkWithExportOrder();

        ByteArrayInputStream bis = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        return new ZipInputStream(bis);
    }

    private String getZipEntryAsStream(ZipInputStream zis) throws IOException {
        byte[] buf = new byte[1024];
        int n = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((n = zis.read(buf, 0, 1024)) > -1) {
            bos.write(buf, 0, n);
        }

        Assert.assertNotNull(bos);
        return bos.toString();
    }

    @Test
    public void correctInit_succeeds() throws AtlasBaseException {
        initZipSinkWithExportOrder();
        assertTrue(true);
        Assert.assertNotNull(zipSink);
    }

    @Test
    public void zipWithExactlyOneEntry_succeeds() {

        try {
            ZipInputStream zis = getZipInputStreamForDefaultExportOrder();

            try {
                Assert.assertNotNull(zis.getNextEntry());
                Assert.assertNull(zis.getNextEntry());
            } catch (IOException e) {

                assertTrue(false);
            }
        } catch (AtlasBaseException e) {

            assertTrue(false, "No exception should be thrown.");
        }
    }

    @Test
    public void verifyExportOrderEntryName_verifies() throws AtlasBaseException, IOException {

        ZipInputStream zis = getZipInputStreamForDefaultExportOrder();
        ZipEntry ze = zis.getNextEntry();

        assertEquals(ze.getName().replace(".json", ""), ZipExportFileNames.ATLAS_EXPORT_ORDER_NAME.toString());
    }

    @Test
    public void zipWithExactlyOneEntry_ContentsVerified() throws AtlasBaseException, IOException {

        ZipInputStream zis = getZipInputStreamForDefaultExportOrder();
        zis.getNextEntry();

        assertEquals(getZipEntryAsStream(zis).replace("\"", "'"), "['a','b','c','d']");
    }

    @Test
    public void zipWithExactlyTwoEntries_ContentsVerified() throws AtlasBaseException, IOException {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        useZipSinkToCreateEntries(byteOutputStream);

        ByteArrayInputStream bis = new ByteArrayInputStream(byteOutputStream.toByteArray());
        ZipInputStream zipStream = new ZipInputStream(bis);
        ZipEntry entry = zipStream.getNextEntry();

        assertEquals(getZipEntryAsStream(zipStream), "[\"a\",\"b\",\"c\",\"d\"]");
        assertEquals(entry.getName().replace(".json", ""), ZipExportFileNames.ATLAS_EXPORT_ORDER_NAME.toString());

        entry = zipStream.getNextEntry();
        assertEquals(entry.getName().replace(".json", ""), ZipExportFileNames.ATLAS_EXPORT_INFO_NAME.toString());
        assertTrue(compareJsonWithObject(getZipEntryAsStream(zipStream), defaultExportResult));
    }

    @Test
    public void recordsEntityEntries() throws AtlasBaseException {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        ZipSink zs = new ZipSink(byteOutputStream);

        AtlasEntity entity = new AtlasEntity();
        entity.setGuid(String.format(knownEntityGuidFormat, 0));

        zs.add(entity);
        assertTrue(zs.hasEntity(String.format(knownEntityGuidFormat, 0)));

        zs.close();
    }

    @Test
    public void recordsEntityWithExtInfoEntries() throws AtlasBaseException {
        final int max_entries = 3;
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        ZipSink zs = new ZipSink(byteOutputStream);

        AtlasEntity entity = new AtlasEntity();
        entity.setGuid(String.format(knownEntityGuidFormat, 0));

        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = new AtlasEntity.AtlasEntityWithExtInfo(entity);
        addReferredEntities(entityWithExtInfo, max_entries);

        zs.add(entityWithExtInfo);
        for (int i = 0; i <= max_entries; i++) {
            String g = String.format(knownEntityGuidFormat, i);
            assertTrue(zs.hasEntity(g));
        }

        zs.close();
    }

    private void addReferredEntities(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo, int maxEntries) {

        for (int i = 1; i <= maxEntries; i++) {
            AtlasEntity entity1 = new AtlasEntity();
            entity1.setGuid(String.format(knownEntityGuidFormat, i));
            entityWithExtInfo.addReferredEntity(entity1);
        }
    }

    @Test
    public void recordsDoesNotRecordEntityEntries() throws AtlasBaseException {
        initZipSinkWithExportOrder();

        assertNotNull(zipSink);
        assertFalse(zipSink.hasEntity(ZipExportFileNames.ATLAS_EXPORT_ORDER_NAME.toString()));
    }

    private void useZipSinkToCreateEntries(ByteArrayOutputStream byteOutputStream) throws AtlasBaseException {
        ZipSink zs = new ZipSink(byteOutputStream);
        zs.setExportOrder(defaultExportOrder);
        zs.setResult(getDefaultExportResult());
        zs.close();
    }

    private boolean compareJsonWithObject(String s, AtlasExportResult defaultExportResult) {
        String json = AtlasType.toJson(defaultExportResult);
        return json.equals(s);
    }
}
