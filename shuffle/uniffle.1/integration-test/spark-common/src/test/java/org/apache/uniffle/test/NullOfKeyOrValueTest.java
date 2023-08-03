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

import java.util.Map;

import com.google.common.collect.Lists;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Test;
import scala.Tuple2;

/**
 * This class is to test whether the RSS keep consistent with the vanilla spark shuffle when
 * the key or value is null.
 */
public class NullOfKeyOrValueTest extends SimpleTestBase {

  @Test
  public void nullOfKeyOrValueTest() throws Exception {
    run();
  }

  @Override
  public Map runTest(SparkSession spark, String fileName) throws Exception {
    // take a rest to make sure shuffle server is registered
    Thread.sleep(4000);
    JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());
    JavaPairRDD<String, Integer> javaPairRDD = jsc.parallelizePairs(
        Lists.newArrayList(
            new Tuple2<>("cat1", null),
            new Tuple2<>("dog", 22),
            new Tuple2<>("cat", 33),
            new Tuple2<>("pig", 44),
            new Tuple2<>(null, 55),
            new Tuple2<>("cat", 66)
        ),
        2
    );
    return javaPairRDD.collectAsMap();
  }
}
