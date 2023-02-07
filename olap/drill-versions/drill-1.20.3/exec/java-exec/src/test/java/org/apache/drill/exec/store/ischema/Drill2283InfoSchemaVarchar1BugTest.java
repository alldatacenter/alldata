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
package org.apache.drill.exec.store.ischema;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.UnlikelyTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(UnlikelyTest.class)
public class Drill2283InfoSchemaVarchar1BugTest extends BaseTestQuery {

  @Test
  public void testInfoSchemaStringsNotOfLength1() throws Exception {
    testBuilder()
        .sqlQuery("SELECT CATALOG_NAME, "
                  + "     CATALOG_NAME = 'DRILL' AS happened_to_be_right_1, "
                  + "     CATALOG_NAME = 'DRIL'  AS was_wrong_1, "
                  + "     CATALOG_NAME = 'DRI'   AS was_wrong_2, "
                  + "     CATALOG_NAME = 'DR'    AS was_wrong_3, "
                  + "     CATALOG_NAME = 'DX'    AS happened_to_be_right_2, "
                  + "     CATALOG_NAME = 'D'     AS happened_to_be_right_3, "
                  + "     CATALOG_NAME = 'X'     AS happened_to_be_right_4"
                  + " "
                  + " FROM INFORMATION_SCHEMA.CATALOGS "
                  + " WHERE CATALOG_NAME LIKE '%DRILL%'"
                  + " LIMIT 1")
        .ordered()
        .baselineColumns("CATALOG_NAME",
                         "happened_to_be_right_1",
                         "was_wrong_1",
                         "was_wrong_2",
                         "was_wrong_3",
                         "happened_to_be_right_2",
                         "happened_to_be_right_3",
                         "happened_to_be_right_4")
        .baselineValues("DRILL",
                        true,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false)
        .build().run();
  }
}
