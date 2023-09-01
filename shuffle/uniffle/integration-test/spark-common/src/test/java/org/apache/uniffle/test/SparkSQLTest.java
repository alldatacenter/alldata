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

package org.apache.uniffle.test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SparkSQLTest extends SparkIntegrationTestBase {

  private static final Logger LOG = LoggerFactory.getLogger(SparkSQLTest.class);

  @Test
  public void resultCompareTest() throws Exception {
    run();
    checkShuffleData();
  }

  @Override
  public Map runTest(SparkSession spark, String fileName) {
    Dataset<Row> df = spark.read().schema("name STRING, age INT").csv(fileName);
    df.createOrReplaceTempView("people");
    Dataset<Row> queryResult = spark.sql("SELECT name, count(age) FROM people group by name order by name");
    Map<String, Long> result = Maps.newHashMap();
    queryResult.javaRDD().collect().stream().forEach(row -> {
          result.put(row.getString(0), row.getLong(1));
        }
    );
    return result;
  }

  @Override
  public String generateTestFile() throws Exception {
    return generateCsvFile();
  }

  @Override
  public void updateSparkConfCustomer(SparkConf sparkConf) {
    sparkConf.set("spark.sql.shuffle.partitions", "4");
    updateRssStorage(sparkConf);
  }

  public abstract void updateRssStorage(SparkConf sparkConf);

  public abstract void checkShuffleData() throws Exception;

  protected String generateCsvFile() throws Exception {
    int rows = 1000;
    String tempDir = Files.createTempDirectory("rss").toString();
    File file = new File(tempDir, "test.csv");
    file.createNewFile();
    LOG.info("Create file:" + file.getAbsolutePath());
    file.deleteOnExit();
    try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
      for (int i = 0; i < rows; i++) {
        writer.println(generateRecord());
      }
    }
    LOG.info("finish test data for word count file:" + file.getAbsolutePath());
    return file.getAbsolutePath();
  }

  private String generateRecord() {
    Random random = new Random();
    char ch = (char) ('a' + random.nextInt(26));
    int repeats = random.nextInt(10);
    return StringUtils.repeat(ch, repeats) + "," + random.nextInt(100);
  }
}
