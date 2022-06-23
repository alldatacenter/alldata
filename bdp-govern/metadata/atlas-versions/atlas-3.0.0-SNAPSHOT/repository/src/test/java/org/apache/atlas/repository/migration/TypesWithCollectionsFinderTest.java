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

import com.google.inject.Inject;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.GraphDBMigrator;
import org.apache.atlas.repository.graphdb.janus.migration.TypesWithCollectionsFinder;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Guice(modules = TestModules.TestOnlyModule.class)
public class TypesWithCollectionsFinderTest extends MigrationBaseAsserts {
    @Inject
    protected TypesWithCollectionsFinderTest(AtlasGraph graph, GraphDBMigrator migrator) {
        super(graph, migrator);
    }

    @BeforeClass
    public void setup() throws IOException, AtlasBaseException {
        loadTypesFromJson();

        typeDefStore.createTypesDef(TestResourceFileUtils.readObjectFromJson(".", "typesDef-classification-with-map", AtlasTypesDef.class));
    }

    @Test
    public void fetchAll() {
        Map<String, Map<String, List<String>>> typeAttrMap = TypesWithCollectionsFinder.getVertexPropertiesForCollectionAttributes(typeRegistry);

        assertTrue(typeAttrMap.size() >= 19);

        assertProperties(typeAttrMap, "__AtlasUserProfile", "ARRAY", "__AtlasUserProfile.savedSearches");

        assertProperties(typeAttrMap, "Process", "ARRAY", "Process.inputs");
        assertProperties(typeAttrMap, "Process", "ARRAY", "Process.outputs");

        assertProperties(typeAttrMap, "hdfs_path", "MAP_PRIMITIVE", "hdfs_path.extendedAttributes");

        assertProperties(typeAttrMap, "hive_column_lineage", "ARRAY", "Process.inputs");
        assertProperties(typeAttrMap, "hive_column_lineage", "ARRAY", "Process.outputs");

        assertProperties(typeAttrMap, "hive_db", "MAP_PRIMITIVE", "hive_db.parameters");

        assertProperties(typeAttrMap, "hive_process", "ARRAY", "Process.inputs");
        assertProperties(typeAttrMap, "hive_process", "ARRAY", "Process.outputs");

        assertProperties(typeAttrMap, "hive_storagedesc", "ARRAY", "hive_storagedesc.sortCols");

        assertProperties(typeAttrMap, "hive_serde", "MAP_PRIMITIVE", "hive_serde.parameters");

        assertProperties(typeAttrMap, "hive_table", "ARRAY", "hive_table.partitionKeys");
        assertProperties(typeAttrMap, "hive_table", "ARRAY", "hive_table.columns");
        assertProperties(typeAttrMap, "hive_table", "MAP_PRIMITIVE", "hive_table.parameters");

        assertProperties(typeAttrMap, "tag_with_map_of_map", "MAP_PRIMITIVE", "tag_with_map_of_map.tag_with_map_of_map");
    }

    private void assertProperties(Map<String, Map<String, List<String>>> typeAttrMap, String typeName, String typeCategory, String propertyName) {
        List<String> actualProperties = typeAttrMap.get(typeName).get(typeCategory);

        assertTrue(actualProperties.contains(propertyName));
    }
}
