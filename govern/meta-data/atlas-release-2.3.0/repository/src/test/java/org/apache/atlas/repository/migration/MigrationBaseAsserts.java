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
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.graph.GraphBackedSearchIndexer;
import org.apache.atlas.repository.graph.GraphHelper;
import org.apache.atlas.repository.graphdb.*;
import org.apache.atlas.repository.store.bootstrap.AtlasTypeDefStoreInitializer;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.IOException;
import java.util.Iterator;

import static org.apache.atlas.utils.TestLoadModelUtils.loadModelFromJson;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class MigrationBaseAsserts extends AtlasTestBase {
    private   static final String TYPE_NAME_PROPERTY   = "__typeName";
    private   static final String R_GUID_PROPERTY_NAME = "_r__guid";
    protected static final String ASSERT_NAME_PROPERTY = "Asset.name";

    private final GraphDBMigrator migrator;
    private final AtlasGraph      graph;

    @Inject
    protected AtlasTypeDefStore typeDefStore;

    @Inject
    protected AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasTypeDefStoreInitializer storeInitializer;

    @Inject
    private GraphBackedSearchIndexer indexer;

    protected MigrationBaseAsserts(AtlasGraph graph, GraphDBMigrator migrator) {
        this.graph    = graph;
        this.migrator = migrator;
    }

    @BeforeClass
    public void initialize() throws Exception {
        super.initialize();
    }

    @AfterClass
    public void clear() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }

    protected void loadTypesFromJson() throws IOException, AtlasBaseException {
        loadModelFromJson("0000-Area0/0010-base_model.json", typeDefStore, typeRegistry);
        loadModelFromJson("1000-Hadoop/1020-fs_model.json", typeDefStore, typeRegistry);
        loadModelFromJson("1000-Hadoop/1030-hive_model.json", typeDefStore, typeRegistry);
    }

    protected void runFileImporter(String directoryToImport) throws IOException, AtlasBaseException {
        loadTypesFromJson();
        String directoryName = TestResourceFileUtils.getDirectory(directoryToImport);
        DataMigrationService.FileImporter fi = new DataMigrationService.FileImporter(migrator, typeDefStore, typeRegistry,
                storeInitializer, directoryName, indexer);

        fi.run();
    }

    protected void assertHiveVertices(int dbCount, int tableCount, int columnCount) {
        int i = 0;

        Iterator<AtlasVertex> results = getVertices("hive_db", null);
        for (Iterator<AtlasVertex> it = results; it.hasNext(); i++) {
            assertNotNull(it.next());
        }
        assertEquals(i, dbCount);

        i = 0;
        results = getVertices("hive_table", null);
        for (Iterator<AtlasVertex> it = results; it.hasNext(); i++) {
            assertNotNull(it.next());
        }
        assertEquals(i, tableCount);

        i = 0;
        results = getVertices("hive_column", null);
        for (Iterator<AtlasVertex> it = results; it.hasNext(); i++) {
            assertNotNull(it.next());
        }

        assertTrue(i > 0);
        assertEquals(i, columnCount);
    }

    protected Iterator<AtlasVertex> getVertices(String typeName, String name) {
        AtlasGraphQuery query = graph.query().has(TYPE_NAME_PROPERTY, typeName);

        if(!StringUtils.isEmpty(name)) {
            query = query.has(ASSERT_NAME_PROPERTY, name);
        }

        return query.vertices().iterator();
    }

    protected AtlasVertex getVertex(String typeName, String name) {
        Iterator<AtlasVertex> iterator = getVertices(typeName, name);

        return iterator.hasNext() ? iterator.next() : null;
    }

    protected void assertEdges(String typeName, String assetName, AtlasEdgeDirection edgeDirection, int expectedItems, String edgeTypeName) {
        Iterator edgeIterator = getVertex(typeName, assetName).getEdges(edgeDirection).iterator();
        assertEdges(edgeIterator, expectedItems, edgeTypeName);
    }

    protected void assertEdges(Iterator<AtlasEdge> results, int expectedItems, String edgeTypeNameExpected) {
        int count = 0;
        AtlasEdge e = null;
        boolean searchedEdgeFound = false;
        for (Iterator<AtlasEdge> it = results; it.hasNext();) {
            e = it.next();
            String typeName = AtlasGraphUtilsV2.getEncodedProperty(e, TYPE_NAME_PROPERTY, String.class);
            searchedEdgeFound = StringUtils.isEmpty(edgeTypeNameExpected) || typeName.equals(edgeTypeNameExpected);
            if (searchedEdgeFound) {
                count++;
                break;
            }
        }

        assertNotNull(AtlasGraphUtilsV2.getEncodedProperty(e, R_GUID_PROPERTY_NAME, Object.class));
        assertNotNull(AtlasGraphUtilsV2.getEncodedProperty(e, "tagPropagation", Object.class));

        if(StringUtils.isNotEmpty(edgeTypeNameExpected)) {
            assertTrue(searchedEdgeFound, edgeTypeNameExpected);
        }

        assertEquals(count, expectedItems, String.format("%s", edgeTypeNameExpected));
    }

    protected void assertEdgesWithLabel(Iterator<AtlasEdge> results, int startIdx, String edgeTypeName) {
        int count = 0;
        AtlasEdge e = null;
        for (Iterator<AtlasEdge> it = results; it.hasNext() && count < startIdx; count++) {
            e = it.next();
        }

        assertNotNull(AtlasGraphUtilsV2.getEncodedProperty(e, R_GUID_PROPERTY_NAME, Object.class));
        assertNotNull(AtlasGraphUtilsV2.getEncodedProperty(e, "tagPropagation", Object.class));

        if(StringUtils.isNotEmpty(edgeTypeName)) {
            assertEquals(e.getLabel(), edgeTypeName, edgeTypeName);
        }
    }

    protected void assertTypeCountNameGuid(String typeName, int expectedItems, String name, String guid) {
        Iterator<AtlasVertex> results = getVertices(typeName, name);

        int count = 0;
        for (Iterator<AtlasVertex> it = results; it.hasNext(); ) {
            AtlasVertex v = it.next();

            assertEquals(GraphHelper.getTypeName(v), typeName);

            if(StringUtils.isNotEmpty(guid)) {
                assertEquals(GraphHelper.getGuid(v), guid, name);
            }

            if(StringUtils.isNotEmpty(name)) {
                assertEquals(AtlasGraphUtilsV2.getEncodedProperty(v, ASSERT_NAME_PROPERTY, String.class), name, name);
            }

            count++;
        }

        assertEquals(count, expectedItems, String.format("%s:%s", typeName, name));
    }

    protected void assertMigrationStatus(int expectedTotalCount) {
        AtlasVertex v = getVertex("__MigrationStatus", "");

        assertTrue(AtlasGraphUtilsV2.getEncodedProperty(v, "currentIndex", Number.class).intValue() >= expectedTotalCount);
    }
}
