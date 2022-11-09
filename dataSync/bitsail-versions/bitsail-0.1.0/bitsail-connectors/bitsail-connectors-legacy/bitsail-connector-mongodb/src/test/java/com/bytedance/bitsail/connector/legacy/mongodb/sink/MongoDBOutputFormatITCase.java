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

package com.bytedance.bitsail.connector.legacy.mongodb.sink;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.fake.option.FakeReaderOptions;
import com.bytedance.bitsail.connector.legacy.mongodb.option.MongoDBWriterOptions;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;
import com.bytedance.bitsail.test.connector.test.testcontainers.mongodb.TestMongoDBContainer;
import com.bytedance.bitsail.test.connector.test.utils.JobConfUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;

public class MongoDBOutputFormatITCase {

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
    mongodbConnStr = mongoDBContainer.getConnectionStr() + "/" + DB_NAME;
  }

  @Test
  public void testFakeToMongoDB() throws Exception {
    BitSailConfiguration jobConf = JobConfUtils.fromClasspath("fake_to_mongodb.json");

    jobConf.set(FakeReaderOptions.TOTAL_COUNT, TOTAL_COUNT);
    jobConf.set(MongoDBWriterOptions.CLIENT_MODE, "url");
    jobConf.set(MongoDBWriterOptions.MONGO_URL, mongodbConnStr);
    jobConf.set(MongoDBWriterOptions.DB_NAME, DB_NAME);
    jobConf.set(MongoDBWriterOptions.COLLECTION_NAME, COLLECTION_NAME);

    EmbeddedFlinkCluster.submitJob(jobConf);

    Assert.assertEquals(TOTAL_COUNT, mongoDBContainer.countDocuments(DB_NAME, COLLECTION_NAME));
  }

  @After
  public void closeContainer() {
    if (Objects.nonNull(mongoDBContainer)) {
      mongoDBContainer.close();
      mongoDBContainer = null;
    }
  }

}
