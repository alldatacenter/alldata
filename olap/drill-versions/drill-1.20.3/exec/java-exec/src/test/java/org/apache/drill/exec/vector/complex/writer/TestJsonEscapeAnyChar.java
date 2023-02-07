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
package org.apache.drill.exec.vector.complex.writer;

import org.apache.commons.io.FileUtils;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestJsonEscapeAnyChar extends ClusterTest {

  private File testFile;
  private static final String TABLE = "escape.json";
  private static final String JSON_DATA = "{\"name\": \"ABC\\S\"}";
  private static final String QUERY = String.format("select * from dfs.`%s`", TABLE);

  @Before
  public void setup() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
    testFile = new File(dirTestWatcher.getRootDir(), TABLE);
    FileUtils.writeStringToFile(testFile, JSON_DATA);
  }

  @Test
  public void testwithOptionEnabled() throws Exception {

    try {
      enableJsonReaderEscapeAnyChar();
      testBuilder()
        .sqlQuery(QUERY)
        .unOrdered()
        .baselineColumns("name")
        .baselineValues("ABCS")
        .build()
        .run();
    } finally {
      resetJsonReaderEscapeAnyChar();
    }
  }

  @Test
  public void testwithOptionDisabled() throws Exception {
    try {
      queryBuilder().sql(QUERY)
        .run();
    } catch (UserRemoteException e) {
      assertThat(e.getMessage(), containsString("DATA_READ ERROR: Error parsing JSON - Unrecognized character escape"));
    }
  }

  private void enableJsonReaderEscapeAnyChar() {
    client.alterSession(ExecConstants.JSON_READER_ESCAPE_ANY_CHAR, true);
  }

  private void resetJsonReaderEscapeAnyChar() {
    client.alterSession(ExecConstants.JSON_READER_ESCAPE_ANY_CHAR, false);
  }

  @After
  public void teardown() throws Exception {
    FileUtils.deleteQuietly(testFile);
  }
}
