/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.mongodb.source;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.mongodb.option.MongoDBReaderOptions;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;
import com.bytedance.bitsail.test.connector.test.testcontainers.mongodb.TestMongoDBContainer;
import com.bytedance.bitsail.test.connector.test.utils.JobConfUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MongoDBInputFormatITCase {

  private static final int TOTAL_COUNT = 300;
  private static final String DB_NAME = "test_db";
  private static final String COLLECTION_NAME = "test_collection";

  private TestMongoDBContainer mongoDBContainer;
  private String mongodbConnStr;

  @Before
  public void startContainer() {
    mongoDBContainer = new TestMongoDBContainer();
    mongoDBContainer.start();
    mongoDBContainer.createCollection(DB_NAME, COLLECTION_NAME);
    mongodbConnStr = mongoDBContainer.getConnectionStr();
  }

  @Test
  public void testMongoDBToPrint() throws Exception {
    insertDocument();

    BitSailConfiguration jobConf = JobConfUtils.fromClasspath("mongodb_to_print.json");

    jobConf.set(MongoDBReaderOptions.HOST, "localhost");
    jobConf.set(MongoDBReaderOptions.PORT, getPort(mongodbConnStr));
    jobConf.set(MongoDBReaderOptions.DB_NAME, DB_NAME);
    jobConf.set(MongoDBReaderOptions.COLLECTION_NAME, COLLECTION_NAME);
    jobConf.set(MongoDBReaderOptions.SPLIT_PK, "_id");

    EmbeddedFlinkCluster.submitJob(jobConf);
  }

  @After
  public void closeContainer() {
    if (Objects.nonNull(mongoDBContainer)) {
      mongoDBContainer.close();
      mongoDBContainer = null;
    }
  }

  private void insertDocument() {
    List<Map<String, Object>> docFieldList = new ArrayList<>();
    for (int i = 0; i < TOTAL_COUNT; ++i) {
      Map<String, Object> docField = new HashMap<>();
      docField.put("string_field", "str_" + i);
      docField.put("int_field", i);
      docFieldList.add(docField);
    }
    mongoDBContainer.insertDocuments(DB_NAME, COLLECTION_NAME, docFieldList);
  }

  private int getPort(String url) {
    return Integer.parseInt(url.split(":")[2]);
  }
}
