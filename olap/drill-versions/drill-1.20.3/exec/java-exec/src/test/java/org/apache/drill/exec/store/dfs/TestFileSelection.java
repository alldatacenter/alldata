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
package org.apache.drill.exec.store.dfs;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.exec.util.DrillFileSystemUtil;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.test.BaseTestQuery;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

public class TestFileSelection extends BaseTestQuery {
  private static final List<FileStatus> EMPTY_STATUSES = ImmutableList.of();
  private static final List<Path> EMPTY_FILES = ImmutableList.of();
  private static final String EMPTY_ROOT = "";

  @Test
  public void testCreateReturnsNullWhenArgumentsAreIllegal() {
    for (final Object statuses : new Object[] { null, EMPTY_STATUSES}) {
      for (final Object files : new Object[]{null, EMPTY_FILES}) {
        for (final Object root : new Object[]{null, EMPTY_ROOT}) {
          FileSelection selection = FileSelection.create((List<FileStatus>) statuses, (List<Path>) files,
                  DrillFileSystemUtil.createPathSafe((String) root));
          assertNull(selection);
        }
      }
    }
  }

  @Test
  public void testBackPathBad() throws Exception {
    final String[][] badPaths =
        {
            {"/tmp", "../../bad"},   //  goes beyond root and outside parent; resolves to /../bad
            {"/tmp", "../etc/bad"},  //  goes outside parent; resolves to /etc/bad
            {"", "/bad"},            //  empty parent
            {"/", ""},               //  empty path
        };


    for (int i = 0; i < badPaths.length; i++) {
      boolean isPathGood = true;
      try {
        String parent = badPaths[i][0];
        String subPath = DrillStringUtils.removeLeadingSlash(badPaths[i][1]);
        String path = new Path(parent, subPath).toString();
        FileSelection.checkBackPaths(parent, path, subPath);
      } catch (IllegalArgumentException e) {
        isPathGood = false;
      }
      if (isPathGood) {
        fail("Failed to catch invalid file selection paths.");
      }
    }
  }

  @Test
  public void testBackPathGood() throws Exception {
    final String[][] goodPaths =
        {
            {"/tmp", "../tmp/good"},
            {"/", "/tmp/good/../../good"},
            {"/", "etc/tmp/../../good"},   //  no leading slash in path
            {"/", "../good"},              //  resolves to /../good which is OK
            {"/", "/good"}
        };

    for (int i = 0; i < goodPaths.length; i++) {
      try {
        String parent = goodPaths[i][0];
        String subPath = DrillStringUtils.removeLeadingSlash(goodPaths[i][1]);
        String path = new Path(parent, subPath).toString();
        FileSelection.checkBackPaths(parent, path, subPath);
      } catch (IllegalArgumentException e) {
        fail("Valid path not allowed by selection path validation.");
      }
    }
  }

}
