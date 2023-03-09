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

package org.apache.griffin.measure

import java.io.File

import org.apache.commons.io.FileUtils
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.internal.SQLConf
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

trait SparkSuiteBase extends AnyFlatSpec with BeforeAndAfterAll with BeforeAndAfterEach {

  @transient var spark: SparkSession = _
  @transient var sc: SparkContext = _
  @transient var conf: SparkConf = _

  override def beforeAll() {
    super.beforeAll()
    cleanTestHiveData()
    conf = new SparkConf(false)
    spark = SparkSession.builder
      .master("local[4]")
      .appName("Griffin Job Suite")
      .config(SQLConf.SHUFFLE_PARTITIONS.key, "4")
      .config("spark.default.parallelism", "4")
      .config("spark.sql.crossJoin.enabled", "true")
      .config(conf)
      .enableHiveSupport()
      .getOrCreate()
    sc = spark.sparkContext
  }

  override def afterAll() {
    try {
      spark.sparkContext.stop()
      SparkSession.clearActiveSession()
      if (spark != null) {
        spark.stop()
      }
      spark = null
      if (sc != null) {
        sc.stop()
      }
      sc = null
      conf = null

      cleanTestHiveData()
    } finally {
      super.afterAll()
    }
  }

  def cleanTestHiveData(): Unit = {
    val metastoreDB = new File("metastore_db")
    if (metastoreDB.exists) {
      FileUtils.forceDelete(metastoreDB)
    }
    val sparkWarehouse = new File("spark-warehouse")
    if (sparkWarehouse.exists) {
      FileUtils.forceDelete(sparkWarehouse)
    }
  }
}
