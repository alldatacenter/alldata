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

import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.type.AtlasType;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class AtlasImportRequestTest {
    @Test
    public void serializeAtlasImportRequstFromJsonWithEmptyOptions() {
        String jsonData = "{ \"options\": {} }";

        AtlasImportRequest request = AtlasType.fromJson(jsonData, AtlasImportRequest.class);

        assertNotNull(request);
        assertNotNull(request.getOptions());
        assertNull(request.getOptions().get(AtlasImportRequest.TRANSFORMS_KEY));

        ImportTransforms tr = ImportTransforms.fromJson(request.getOptions().get(AtlasImportRequest.TRANSFORMS_KEY));

        assertNull(tr);
    }

    @Test
    public void serializeOptions_VerifyAccessors() {
        String guid = "\"abcd\"";
        String pos = "\"1\"";
        String trueVal = "\"true\"";

        String jsonData = "{ \"options\": " +
                "               {" +
                "\"startGuid\":" + guid + "," +
                "\"startPosition\":" + pos + "," +
                "\"updateTypeDefinition\":" + trueVal +
                "}" +
                "}";

        AtlasImportRequest request = AtlasType.fromJson(jsonData, AtlasImportRequest.class);

        assertNotNull(request);
        assertNotNull(request.getStartGuid());
        assertNotNull(request.getStartPosition());
        assertNotNull(request.getUpdateTypeDefs());

        assertEquals(request.getStartGuid(), guid.replace("\"", ""));
        assertEquals(request.getStartPosition(), pos.replace("\"", ""));
        assertEquals(request.getUpdateTypeDefs(), trueVal.replace("\"", ""));
    }

    @Test
    public void optionsDefaultsTest() {
        String jsonData = "{ \"options\": " +
                "               {" +
                    "}" +
                "}";

        AtlasImportRequest request = AtlasType.fromJson(jsonData, AtlasImportRequest.class);

        assertNotNull(request);
        assertNull(request.getStartGuid());
        assertNull(request.getStartPosition());
        assertNull(request.getUpdateTypeDefs());
    }

    @Test
    public void serializeAtlasImportRequstFromJsonWithEmptyTransforms() {
        String jsonData = "{ \"options\": { \"transforms\": \"{ }\" } }";

        AtlasImportRequest request = AtlasType.fromJson(jsonData, AtlasImportRequest.class);

        assertNotNull(request);
        assertNotNull(request.getOptions());
        assertNotNull(request.getOptions().get(AtlasImportRequest.TRANSFORMS_KEY));

        ImportTransforms tr = ImportTransforms.fromJson(request.getOptions().get(AtlasImportRequest.TRANSFORMS_KEY));

        assertNotNull(tr);
        assertNotNull(tr.getTransforms());
        assertEquals(tr.getTransforms().size(), 0);
    }

    @Test
    public void serializeAtlasImportRequstFromJsonWith1Transform() {
        String jsonData = "{ \"options\": { \"transforms\": \"{ \\\"hive_db\\\": { \\\"qualifiedName\\\": [ \\\"replace:@cl1:@cl2\\\" ] } }\" } }";

        AtlasImportRequest request = AtlasType.fromJson(jsonData, AtlasImportRequest.class);

        assertNotNull(request);
        assertNotNull(request.getOptions());
        assertNotNull(request.getOptions().get(AtlasImportRequest.TRANSFORMS_KEY));

        ImportTransforms tr = ImportTransforms.fromJson(request.getOptions().get(AtlasImportRequest.TRANSFORMS_KEY));

        assertNotNull(tr);
        assertNotNull(tr.getTransforms());
        assertEquals(tr.getTransforms().size(), 1);
        assertTrue(tr.getTransforms().containsKey("hive_db"));
        assertEquals(tr.getTransforms("hive_db").entrySet().size(), 1);
        assertTrue(tr.getTransforms("hive_db").containsKey("qualifiedName"));
        assertEquals(tr.getTransforms("hive_db").get("qualifiedName").size(), 1);
    }

    @Test
    public void serializeAtlasImportRequstFromJson() {
        String jsonData = "{ \"options\": { \"transforms\": \"{ \\\"hive_db\\\": { \\\"qualifiedName\\\": [ \\\"replace:@cl1:@cl2\\\" ] }, \\\"hive_table\\\": { \\\"qualifiedName\\\": [ \\\"lowercase\\\", \\\"replace:@cl1:@cl2\\\" ] } }\" } } }";

        AtlasImportRequest request = AtlasType.fromJson(jsonData, AtlasImportRequest.class);

        assertNotNull(request);
        assertNotNull(request.getOptions());
        assertNotNull(request.getOptions().get(AtlasImportRequest.TRANSFORMS_KEY));

        ImportTransforms tr = ImportTransforms.fromJson(request.getOptions().get(AtlasImportRequest.TRANSFORMS_KEY));

        assertNotNull(tr);
        assertNotNull(tr.getTransforms());
        assertEquals(tr.getTransforms().size(), 2);
        assertTrue(tr.getTransforms().containsKey("hive_db"));
        assertEquals(tr.getTransforms("hive_db").entrySet().size(), 1);
        assertTrue(tr.getTransforms("hive_db").containsKey("qualifiedName"));
        assertEquals(tr.getTransforms("hive_db").get("qualifiedName").size(), 1);
        assertTrue(tr.getTransforms().containsKey("hive_table"));
        assertEquals(tr.getTransforms("hive_table").entrySet().size(), 1);
        assertTrue(tr.getTransforms("hive_table").containsKey("qualifiedName"));
        assertEquals(tr.getTransforms("hive_table").get("qualifiedName").size(), 2);
    }
}
