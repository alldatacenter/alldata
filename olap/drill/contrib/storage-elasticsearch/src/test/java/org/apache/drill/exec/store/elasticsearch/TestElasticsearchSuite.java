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
package org.apache.drill.exec.store.elasticsearch;

import org.apache.drill.categories.SlowTest;
import org.apache.drill.test.BaseTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Category(SlowTest.class)
@RunWith(Suite.class)
@Suite.SuiteClasses({ElasticComplexTypesTest.class, ElasticInfoSchemaTest.class, ElasticSearchPlanTest.class, ElasticSearchQueryTest.class})
public class TestElasticsearchSuite extends BaseTest {

  protected static ElasticsearchContainer elasticsearch;

  private static final AtomicInteger initCount = new AtomicInteger(0);

  private static volatile boolean runningSuite = false;

  @BeforeClass
  public static void initElasticsearch() {
    synchronized (TestElasticsearchSuite.class) {
      if (initCount.get() == 0) {
        startElasticsearch();
      }
      initCount.incrementAndGet();
      runningSuite = true;
    }
  }

  public static boolean isRunningSuite() {
    return runningSuite;
  }

  @AfterClass
  public static void tearDownCluster() {
    synchronized (TestElasticsearchSuite.class) {
      if (initCount.decrementAndGet() == 0 && elasticsearch != null) {
        elasticsearch.stop();
      }
    }
  }

  private static void startElasticsearch() {
    DockerImageName imageName = DockerImageName.parse("elasticsearch:7.14.2")
      .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");
    TestElasticsearchSuite.elasticsearch = new ElasticsearchContainer(imageName)
      .withStartupTimeout(Duration.ofMinutes(2));
    TestElasticsearchSuite.elasticsearch.start();
  }

  public static String getAddress() {
    return elasticsearch.getHttpHostAddress();
  }
}
