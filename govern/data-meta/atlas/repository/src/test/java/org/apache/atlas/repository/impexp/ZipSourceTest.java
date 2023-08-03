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

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.getZipSource;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class ZipSourceTest {
    @DataProvider(name = "zipFileStocks")
    public static Object[][] getDataFromZipFile() throws IOException, AtlasBaseException {
        FileInputStream fs = ZipFileResourceTestUtils.getFileInputStream("stocks.zip");

        return new Object[][] {{ new ZipSource(fs) }};
    }

    @DataProvider(name = "zipFileStocksFloat")
    public static Object[][] getDataFromZipFileWithLongFloats() throws IOException, AtlasBaseException {
        FileInputStream fs = ZipFileResourceTestUtils.getFileInputStream("stocks-float.zip");

        return new Object[][] {{ new ZipSource(fs) }};
    }

    @DataProvider(name = "sales")
    public static Object[][] getDataFromQuickStart_v1_Sales(ITestContext context) throws IOException, AtlasBaseException {
        return getZipSource("sales-v1-full.zip");
    }

    @Test(expectedExceptions = AtlasBaseException.class)
    public void improperInit_ReturnsNullCreationOrder() throws IOException, AtlasBaseException {
        byte bytes[] = new byte[10];
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ZipSource zs = new ZipSource(bais);
        List<String> s = zs.getCreationOrder();
        Assert.assertNull(s);
    }

    @Test(dataProvider = "zipFileStocks")
    public void examineContents_BehavesAsExpected(ZipSource zipSource) throws AtlasBaseException {
        List<String> creationOrder = zipSource.getCreationOrder();

        assertNotNull(creationOrder);
        assertEquals(creationOrder.size(), 4);

        AtlasTypesDef typesDef = zipSource.getTypesDef();
        assertNotNull(typesDef);
        assertEquals(typesDef.getEntityDefs().size(), 6);

        useCreationOrderToFetchEntitiesWithExtInfo(zipSource, creationOrder);
        useCreationOrderToFetchEntities(zipSource, creationOrder);
        attemptToFetchNonExistentGuid_ReturnsNull(zipSource, "non-existent-guid");
        verifyGuidRemovalOnImportComplete(zipSource, creationOrder.get(0));
    }

    private void useCreationOrderToFetchEntities(ZipSource zipSource, List<String> creationOrder) {
        for (String guid : creationOrder) {
            AtlasEntity e = zipSource.getByGuid(guid);
            assertNotNull(e);
        }
    }

    private void verifyGuidRemovalOnImportComplete(ZipSource zipSource, String guid) {
        AtlasEntity e = zipSource.getByGuid(guid);
        assertNotNull(e);

        zipSource.onImportComplete(guid);

        e = zipSource.getByGuid(guid);
        Assert.assertNull(e);
    }

    private void attemptToFetchNonExistentGuid_ReturnsNull(ZipSource zipSource, String guid) {
        AtlasEntity e = zipSource.getByGuid(guid);
        Assert.assertNull(e);
    }

    private void useCreationOrderToFetchEntitiesWithExtInfo(ZipSource zipSource, List<String> creationOrder) throws AtlasBaseException {
        for (String guid : creationOrder) {
            AtlasEntity.AtlasEntityExtInfo e = zipSource.getEntityWithExtInfo(guid);
            assertNotNull(e);
        }
    }

    @Test(dataProvider = "zipFileStocks")
    public void iteratorBehavor_WorksAsExpected(ZipSource zipSource) throws IOException, AtlasBaseException {
        Assert.assertTrue(zipSource.hasNext());

        List<String> creationOrder = zipSource.getCreationOrder();
        for (int i = 0; i < creationOrder.size(); i++) {
            AtlasEntity e = zipSource.next();

            assertNotNull(e);
            assertEquals(e.getGuid(), creationOrder.get(i));
        }

        assertFalse(zipSource.hasNext());
    }

    @Test(dataProvider = "sales")
    public void iteratorSetPositionBehavor(InputStream inputStream) throws IOException, AtlasBaseException {
        ZipSource zipSource = new ZipSource(inputStream);
        Assert.assertTrue(zipSource.hasNext());

        List<String> creationOrder = zipSource.getCreationOrder();
        int moveToPosition_2 = 2;
        zipSource.setPosition(moveToPosition_2);

        assertEquals(zipSource.getPosition(), moveToPosition_2);
        assertTrue(zipSource.getPosition() < creationOrder.size());

        assertTrue(zipSource.hasNext());
        for (int i = 1; i < 4; i++) {
            zipSource.next();
            assertEquals(zipSource.getPosition(), moveToPosition_2 + i);
        }

        assertTrue(zipSource.hasNext());
    }

    @Test(dataProvider = "zipFileStocksFloat")
    public void attemptToSerializeLongFloats(ZipSource zipSource) throws IOException, AtlasBaseException {
        Assert.assertTrue(zipSource.hasNext());
        assertTrue(zipSource.hasNext());
        assertTrue(zipSource.hasNext());

        AtlasEntity.AtlasEntityWithExtInfo e = zipSource.getNextEntityWithExtInfo();
        assertNotNull(e);
        assertTrue(e.getEntity().getClassifications().size() > 0);
        assertNotNull(e.getEntity().getClassifications().get(0).getAttribute("fv"));
        assertEquals(e.getEntity().getClassifications().get(0).getAttribute("fv").toString(), "3.4028235E+38");

        assertTrue(zipSource.hasNext());
    }

    @Test(dataProvider = "zipFileStocks")
    public void applyTransformation(ZipSource zipSource) throws IOException, AtlasBaseException {
        ImportTransforms transforms = getTransformForHiveDB();
        zipSource.setImportTransform(transforms);

        Assert.assertTrue(zipSource.hasNext());
        List<String> creationOrder = zipSource.getCreationOrder();
        for (int i = 0; i < creationOrder.size(); i++) {
            AtlasEntity e = zipSource.next();
            if(e.getTypeName().equals("hive_db")) {
                Object o = e.getAttribute("qualifiedName");
                String s = (String) o;

                assertNotNull(e);
                assertTrue(s.contains("@cl2"));
                break;
            }
        }
    }

    private ImportTransforms getTransformForHiveDB() {
        ImportTransforms tr = ImportTransforms.fromJson("{ \"hive_db\": { \"qualifiedName\": [ \"replace:@cl1:@cl2\" ] } }");

        return tr;
    }
}
