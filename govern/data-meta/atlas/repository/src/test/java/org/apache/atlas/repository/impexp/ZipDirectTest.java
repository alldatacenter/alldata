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
import org.apache.atlas.model.impexp.AtlasExportResult;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class ZipDirectTest {
    @Test(expectedExceptions = AtlasBaseException.class)
    public void loadFileEmpty() throws IOException, AtlasBaseException {
        InputStream inputStream = ZipFileResourceTestUtils.getFileInputStream("zip-direct-1.zip");
        new ZipSourceDirect(inputStream, 1);
    }

    @Test
    public void loadFile() throws IOException, AtlasBaseException {
        final int EXPECTED_ENTITY_COUNT = 3;

        InputStream inputStream = ZipFileResourceTestUtils.getFileInputStream("zip-direct-2.zip");
        ZipSourceDirect zipSourceDirect = new ZipSourceDirect(inputStream, EXPECTED_ENTITY_COUNT);

        assertNotNull(zipSourceDirect);
        assertNotNull(zipSourceDirect.getTypesDef());
        assertTrue(zipSourceDirect.getTypesDef().getEntityDefs().size() > 0);
        assertNotNull(zipSourceDirect.getExportResult());

        int count = 0;
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo;
        while((entityWithExtInfo = zipSourceDirect.getNextEntityWithExtInfo()) != null) {
            assertNotNull(entityWithExtInfo);
            count++;
        }

        assertEquals(count, EXPECTED_ENTITY_COUNT);
    }

    @Test
    public void entitiesParserTest() throws IOException {
        String object1 = "{\"type\":\"hdfs_path\"}";
        String object2 = "{\"type\":\"hive_db\"}";
        String entities = "[" + object1 + "," + object2 + ",{}]";
        InputStream inputStream = new ByteArrayInputStream(entities.getBytes());
        ZipSourceDirect.EntitiesArrayParser entitiesArrayParser = new ZipSourceDirect.EntitiesArrayParser(inputStream);

        Object o = entitiesArrayParser.next();

        assertNotNull(o);
        assertEquals(o, object1);

        o = entitiesArrayParser.next();
        assertEquals(o, object2);

        o = entitiesArrayParser.next();
        assertNull(o);
    }
}
