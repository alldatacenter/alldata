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

import org.testng.Assert;
import org.testng.annotations.Test;


import static org.testng.Assert.*;

public class ImportTransformerJSONTest {


    @Test
    public void createAtlasImportTransformFromJson() throws Exception {
        String hiveTableType  = "hive_table";
        String qualifiedName  = "qualifiedName";
        String jsonTransforms = "{ \"hive_table\": { \"qualifiedName\":[ \"lowercase\", \"replace:@cl1:@cl2\" ] } }";

        ImportTransforms transforms = ImportTransforms.fromJson(jsonTransforms);

        assertNotNull(transforms);
        assertEquals(transforms.getTransforms().entrySet().size(), 1);
        assertEquals(transforms.getTransforms().get(hiveTableType).entrySet().size(), 1);
        assertEquals(transforms.getTransforms().get(hiveTableType).get(qualifiedName).size(), 2);
        Assert.assertEquals(transforms.getTransforms().get(hiveTableType).get(qualifiedName).get(0).getTransformType(), "lowercase");
        assertEquals(transforms.getTransforms().get(hiveTableType).get(qualifiedName).get(1).getTransformType(), "replace");
        assertTrue(transforms.getTransforms().get(hiveTableType).get(qualifiedName).get(1) instanceof ImportTransformer.Replace);
        assertEquals(((ImportTransformer.Replace)transforms.getTransforms().get(hiveTableType).get(qualifiedName).get(1)).getToFindStr(), "@cl1");
        assertEquals(((ImportTransformer.Replace)transforms.getTransforms().get(hiveTableType).get(qualifiedName).get(1)).getReplaceStr(), "@cl2");
    }
}
