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

import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.graphdb.janus.migration.TypesDefScrubber;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.io.FileUtils;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import static org.apache.atlas.repository.graphdb.janus.migration.TypesDefScrubber.LEGACY_TYPE_NAME_PREFIX;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TypesDefScrubberTest {
    private static final String resourcesDirRelativePath = "/src/test/resources/";
    private final String LEGACY_TYPESDEF_JSON = "legacy-typesdef.json";
    private String resourceDir;

    @BeforeClass
    public void setup() {
        resourceDir = System.getProperty("user.dir") + resourcesDirRelativePath;
    }

    protected AtlasTypesDef getTypesDefFromFile(String s) {
        File f = new File(getFilePath(s));
        try {
            return AtlasType.fromJson(FileUtils.readFileToString(f), AtlasTypesDef.class);
        } catch (IOException e) {
            throw new SkipException("getTypesDefFromFile: " + s, e);
        }
    }

    protected String getFilePath(String fileName) {
        return Paths.get(resourceDir, fileName).toString();
    }

    @Test
    public void performScrub() {
        TypesDefScrubber typesDefScrubber = new TypesDefScrubber();
        AtlasTypesDef td = getTypesDefFromFile(LEGACY_TYPESDEF_JSON);

        int traitPrayIndex = 1;
        int vendorPIIIndex = 2;
        int financeIndex = 3;

        int classificationTraitPrayIndex = 0;
        int classificationVendorPiiIndex = 2;
        int classificationFinancendex = 3;

        String expectedTraitPrayStructName = TypesDefScrubber.getLegacyTypeNameForStructDef(td.getClassificationDefs().get(classificationTraitPrayIndex).getName());
        String expectedVendorPIIStructName = TypesDefScrubber.getLegacyTypeNameForStructDef(td.getClassificationDefs().get(classificationVendorPiiIndex).getName());
        String expectedFinanceStructName = TypesDefScrubber.getLegacyTypeNameForStructDef(td.getClassificationDefs().get(classificationFinancendex).getName());

        assertNewTypesDef(typesDefScrubber.scrub(td), traitPrayIndex, vendorPIIIndex, financeIndex, expectedTraitPrayStructName, expectedVendorPIIStructName, expectedFinanceStructName);

        assertTraitMap(typesDefScrubber, td, classificationTraitPrayIndex, expectedTraitPrayStructName, 0);
        assertTraitMap(typesDefScrubber, td, classificationVendorPiiIndex, expectedVendorPIIStructName, 1);
        assertTraitMap(typesDefScrubber, td, classificationFinancendex, expectedFinanceStructName, 2);
    }

    private void assertTraitMap(TypesDefScrubber typesDefScrubber, AtlasTypesDef td, int classificationIndex, String expectedStructName, int attrIndex) {
        String label = typesDefScrubber.getEdgeLabel(td.getEntityDefs().get(0).getName(), td.getEntityDefs().get(0).getAttributeDefs().get(attrIndex).getName());
        assertTrue(typesDefScrubber.getTraitToTypeMap().containsKey(label));
        assertEquals(typesDefScrubber.getTraitToTypeMap().get(label).getTypeName(), td.getClassificationDefs().get(classificationIndex).getName());
        assertEquals(typesDefScrubber.getTraitToTypeMap().get(label).getLegacyTypeName(), expectedStructName);
    }

    private void assertTraitMap(Map<String,TypesDefScrubber.ClassificationToStructDefName> traitToTypeMap, AtlasTypesDef td) {
    }

    private void assertNewTypesDef(AtlasTypesDef newTypes, int traitPrayIndex, int vendorPIIIndex, int financeIndex, String expectedTraitPrayStructName, String expectedVendorPIIStructName, String expectedFinanceStructName) {
        assertNotNull(newTypes);
        assertEquals(newTypes.getStructDefs().size(), 4);

        assertTrue(newTypes.getStructDefs().get(traitPrayIndex).getName().contains(LEGACY_TYPE_NAME_PREFIX));
        assertTrue(newTypes.getStructDefs().get(vendorPIIIndex).getName().contains(LEGACY_TYPE_NAME_PREFIX));
        assertTrue(newTypes.getStructDefs().get(financeIndex).getName().contains(LEGACY_TYPE_NAME_PREFIX));

        assertEquals(newTypes.getStructDefs().get(traitPrayIndex).getName(), expectedTraitPrayStructName);
        assertEquals(newTypes.getStructDefs().get(vendorPIIIndex).getName(), expectedVendorPIIStructName);
        assertEquals(newTypes.getStructDefs().get(financeIndex).getName(), expectedFinanceStructName);

        assertEquals(newTypes.getStructDefs().get(1).getAttributeDefs().size(), 1);
        assertEquals(newTypes.getEntityDefs().get(0).getAttributeDefs().get(0).getTypeName(), expectedTraitPrayStructName);
        assertEquals(newTypes.getEntityDefs().get(0).getAttributeDefs().get(1).getTypeName(), String.format("array<%s>", expectedVendorPIIStructName));
        assertEquals(newTypes.getEntityDefs().get(0).getAttributeDefs().get(2).getTypeName(), String.format("map<String,%s>", expectedFinanceStructName));
    }
}
