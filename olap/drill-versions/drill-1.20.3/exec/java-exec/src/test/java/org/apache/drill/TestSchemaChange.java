/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill;

import org.apache.drill.test.BaseTestQuery;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Paths;

public class TestSchemaChange extends BaseTestQuery {
  @BeforeClass
  public static void setupFiles() {
    dirTestWatcher.copyResourceToRoot(Paths.get("schemachange"));
  }

  @Test //DRILL-1605, DRILL-2171
  public void testMultiFilesWithDifferentSchema() throws Exception {
    testBuilder()
        .sqlQuery("select a, b from dfs.`schemachange/multi/*.json`")
        .ordered()
        .baselineColumns("a", "b")
        .baselineValues(1L, null)
        .baselineValues(2L, null)
        .baselineValues(null, true)
        .build()
        .run();
  }
}
