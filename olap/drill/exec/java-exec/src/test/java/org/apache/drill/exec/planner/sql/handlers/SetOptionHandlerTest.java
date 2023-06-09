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
package org.apache.drill.exec.planner.sql.handlers;

import org.apache.drill.categories.SqlTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.compile.ClassCompilerSelector;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@Category(SqlTest.class)
public class SetOptionHandlerTest extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher).maxParallelization(1));
  }

  @Test
  public void testSimpleSetQuery() throws Exception {
    String defaultValue = client.queryBuilder()
        .sql("SELECT val from sys.options where name = '%s' limit 1",
            ClassCompilerSelector.JAVA_COMPILER_DEBUG_OPTION)
        .singletonString();

    boolean newValue = !Boolean.parseBoolean(defaultValue);
    try {
      client.alterSession(ClassCompilerSelector.JAVA_COMPILER_DEBUG_OPTION, newValue);

      String changedValue = queryBuilder()
          .sql("SELECT val from sys.options where name = '%s' limit 1",
              ClassCompilerSelector.JAVA_COMPILER_DEBUG_OPTION)
          .singletonString();

      Assert.assertEquals(String.valueOf(newValue), changedValue);
    } finally {
      client.resetSession(ClassCompilerSelector.JAVA_COMPILER_DEBUG_OPTION);
    }
  }

  @Test
  public void testViewSetQuery() throws Exception {
    testBuilder()  // BIT
        .sqlQuery("SET `%s`", ExecConstants.ENABLE_ITERATOR_VALIDATION_OPTION)
        .unOrdered()
        .sqlBaselineQuery("SELECT name, val as value FROM sys.options where name = '%s' limit 1",
            ExecConstants.ENABLE_ITERATOR_VALIDATION_OPTION)
        .go();

    testBuilder()  // BIGINT
        .sqlQuery("SET `%s`", ExecConstants.OUTPUT_BATCH_SIZE)
        .unOrdered()
        .sqlBaselineQuery("SELECT name, val as value FROM sys.options where name = '%s' limit 1",
            ExecConstants.OUTPUT_BATCH_SIZE)
        .go();

    testBuilder()  // FLOAT
        .sqlQuery("SET `%s`", ExecConstants.OUTPUT_BATCH_SIZE_AVAIL_MEM_FACTOR)
        .unOrdered()
        .sqlBaselineQuery("SELECT name, val as value FROM sys.options where name = '%s' limit 1",
            ExecConstants.OUTPUT_BATCH_SIZE_AVAIL_MEM_FACTOR)
        .go();

    testBuilder()  // VARCHAR
        .sqlQuery("SET `%s`", ExecConstants.FILESYSTEM_PARTITION_COLUMN_LABEL)
        .unOrdered()
        .sqlBaselineQuery("SELECT name, val as value FROM sys.options where name = '%s' limit 1",
            ExecConstants.FILESYSTEM_PARTITION_COLUMN_LABEL)
        .go();
  }

  @Test
  public void testViewSetWithIncorrectOption() throws Exception {
    try {
      run("set `non-existing option`");
      fail();
    } catch (UserRemoteException e) {
      assertThat(e.getMessage(), startsWith("VALIDATION ERROR"));
    }
  }
}
