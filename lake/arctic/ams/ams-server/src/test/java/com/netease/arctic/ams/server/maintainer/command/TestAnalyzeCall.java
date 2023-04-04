/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.maintainer.command;

import com.netease.arctic.TableTestHelpers;
import org.junit.Assert;
import org.junit.Test;

public class TestAnalyzeCall extends CallCommandTestBase {

  @Test
  public void testAvailable()  {
    Assert.assertEquals(
        "TABLE_NAME:\n" +
            "\ttest_table\n" +
            "TABLE IS AVAILABLE\n",
        call());
  }

  @Test
  public void testFileLose() {
    String removeFile = removeFile();
    getArcticTable().io().deleteFile(removeFile);
    Assert.assertTrue(call().contains(removeFile));
  }

  @Test
  public void testManifestLose() {
    String removeManifest = removeManifest();
    getArcticTable().io().deleteFile(removeManifest);
    Assert.assertTrue(call().contains(removeManifest));
  }

  @Test
  public void testManifestListLose() {
    String removeManifestList = getArcticTable().asKeyedTable().changeTable().currentSnapshot().manifestListLocation();
    getArcticTable().io().deleteFile(removeManifestList);
    Assert.assertTrue(call().contains(removeManifestList));
  }

  @Test
  public void testMetadataLose() {
    removeMetadata();
    Assert.assertTrue(call().contains(TableAnalyzeResult.ResultType.METADATA_LOSE.name()));
  }

  private String call() {
    return callFactory.generateAnalyzeCall(TableTestHelpers.TEST_TABLE_ID.toString()).call(new Context());
  }
}
