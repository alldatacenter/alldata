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
import org.apache.atlas.repository.graphdb.AtlasEdgeDirection;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.GraphDBMigrator;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.IOException;


@Guice(modules = TestModules.TestOnlyModule.class)
public class HiveParititionTest extends  MigrationBaseAsserts {

    @Inject
    public HiveParititionTest(AtlasGraph graph, GraphDBMigrator migrator) {
        super(graph, migrator);
    }

    @Test
    public void fileImporterTest() throws IOException, AtlasBaseException {
        final int EXPECTED_TOTAL_COUNT = 144;
        final int EXPECTED_DB_COUNT = 1;
        final int EXPECTED_TABLE_COUNT = 2;
        final int EXPECTED_COLUMN_COUNT = 7;

        runFileImporter("parts_db");

        assertHiveVertices(EXPECTED_DB_COUNT, EXPECTED_TABLE_COUNT, EXPECTED_COLUMN_COUNT);

        assertTypeCountNameGuid("hive_db", 1, "parts_db", "ae30d78b-51b4-42ab-9436-8d60c8f68b95");
        assertTypeCountNameGuid("hive_process", 1, "", "");
        assertEdges("hive_db", "parts_db", AtlasEdgeDirection.IN, 1, "");
        assertEdges("hive_table", "t1", AtlasEdgeDirection.OUT, 1, "hive_table_db");
        assertEdges("hive_table", "tv1", AtlasEdgeDirection.OUT, 1, "hive_table_db");

        assertMigrationStatus(EXPECTED_TOTAL_COUNT);
    }
}
