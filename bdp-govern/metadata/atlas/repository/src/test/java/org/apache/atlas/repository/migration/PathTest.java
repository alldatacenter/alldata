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
package org.apache.atlas.repository.migration;

import com.google.inject.Inject;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.repository.graph.GraphHelper;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.graphdb.GraphDBMigrator;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.type.AtlasBuiltInTypes;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Guice(modules = TestModules.TestOnlyModule.class)
public class PathTest extends MigrationBaseAsserts {
    @Inject
    public PathTest(AtlasGraph graph, GraphDBMigrator migrator) {
        super(graph, migrator);
    }

    @Test
    public void migrationImport() throws IOException, AtlasBaseException {
        final int EXPECTED_TOTAL_COUNT = 92;

        runFileImporter("path_db");

        AtlasVertex v = assertHdfsPathVertices(1);
        assertVertexProperties(v);
        assertMigrationStatus(EXPECTED_TOTAL_COUNT);
    }

    private void assertVertexProperties(AtlasVertex v) {
        final String HASH_CODE_PROPERTY = "hdfs_path.hashCode";
        final String RETENTION_PROPERTY = "hdfs_path.retention";

        AtlasBuiltInTypes.AtlasBigIntegerType bitRef = new AtlasBuiltInTypes.AtlasBigIntegerType();
        AtlasBuiltInTypes.AtlasBigDecimalType bdtRef = new AtlasBuiltInTypes.AtlasBigDecimalType();

        BigInteger bitExpected = bitRef.getNormalizedValue(612361213421234L);
        BigDecimal bdtExpected = bdtRef.getNormalizedValue(125353);

        BigInteger bit = AtlasGraphUtilsV2.getEncodedProperty(v, HASH_CODE_PROPERTY, BigInteger.class);
        BigDecimal bdt = AtlasGraphUtilsV2.getEncodedProperty(v, RETENTION_PROPERTY, BigDecimal.class);

        assertEquals(bit, bitExpected);
        assertEquals(bdt.compareTo(bdtExpected), 0);
    }

    protected AtlasVertex assertHdfsPathVertices(int expectedCount) {
        int i = 0;

        AtlasVertex vertex = null;
        Iterator<AtlasVertex> results = getVertices("hdfs_path", null);
        for (Iterator<AtlasVertex> it = results; it.hasNext(); i++) {
            vertex = it.next();
            assertNotNull(vertex);
        }

        assertEquals(i, expectedCount);
        return vertex;
    }
}
