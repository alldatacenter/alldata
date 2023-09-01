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
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

public abstract class RepartitionTest extends SparkIntegrationTestBase {

  private static final Logger LOG = LoggerFactory.getLogger(RepartitionTest.class);

  @Test
  public void resultCompareTest() throws Exception {
    run();
  }

  @Override
  public Map runTest(SparkSession spark, String fileName) {
    return repartitionApp(spark, fileName);
  }

  @Override
  public String generateTestFile() throws Exception {
    return generateTextFile(1000, 5000);
  }

  @Override
  public void updateSparkConfCustomer(SparkConf sparkConf) {
    updateRssStorage(sparkConf);
  }

  public abstract void updateRssStorage(SparkConf sparkConf);

  protected String generateTextFile(int wordsPerRow, int rows) throws Exception {
    String tempDir = Files.createTempDirectory("rss").toString();
    File file = new File(tempDir, "wordcount.txt");
    file.createNewFile();
    LOG.info("Create file:" + file.getAbsolutePath());
    file.deleteOnExit();
    try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
      for (int i = 0; i < rows; i++) {
        writer.println(getLine(wordsPerRow));
      }
    }
    LOG.info("finish test data for word count file:" + file.getAbsolutePath());
    return file.getAbsolutePath();
  }

  private String generateString(int length) {
    Random random = new Random();
    char ch = (char) ('a' + random.nextInt(26));
    int repeats = random.nextInt(length);
    return StringUtils.repeat(ch, repeats);
  }

  private String getLine(int wordsPerRow) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < wordsPerRow; i++) {
      sb.append(generateString(10));
      sb.append(" ");
    }
    return sb.toString();
  }

  private Map repartitionApp(SparkSession spark, String fileName) {
    JavaRDD<String> lines = spark.read().textFile(fileName).javaRDD();
    JavaRDD<String> words = lines.flatMap(s -> Arrays.asList(s.split(" ")).iterator());
    JavaPairRDD<String, Integer> ones = words.mapToPair(s -> new Tuple2<>(s, 1)).repartition(5);
    JavaPairRDD<String, Integer> counts = ones.reduceByKey((i1, i2) -> i1 + i2);
    return counts.sortByKey().collectAsMap();
  }
}
