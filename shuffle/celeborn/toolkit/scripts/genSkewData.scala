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

import scala.util.Random
import spark.implicits._

val datascala=214748364

val df = spark.sparkContext.parallelize(1 to datascala, 1024).map(i => { 
        val random = new Random() 
        val oriKey = random.nextInt(4096) 
        val key = if (oriKey < 3800) random.nextInt(1) else oriKey
        val fas = random.nextInt(1200000)
        val fa = Range(fas, fas + 100).mkString(",")
        val fbs = random.nextInt(60000)
        val fb = Range(fbs, fbs + 100).mkString(",")
        val fcs = random.nextInt(30000)
        val fc = Range(fcs, fcs + 100).mkString(",")
        val fds = random.nextInt(1000)
        val fd = Range(fds, fds + 100).mkString(",")
        (key, fa, fb, fc, fd)
        }
    ).toDF("fa", "f1", "f2", "f3", "f4")
    df.createOrReplaceTempView("view1")
val df2 = spark.sparkContext.parallelize(1 to 64, 64).map(i => {
        val random = new Random()
        // we need to ensure that the join result be null , so keys can not match.
        val oriKey = random.nextInt(2048) + 4096
        val key = oriKey
        val fas = random.nextInt(1200000)
        val fa = Range(fas, fas + 100).mkString(",")
        val fbs = random.nextInt(60000)
        val fb = Range(fbs, fbs + 100).mkString(",")
        val fcs = random.nextInt(30000)
        val fc = Range(fcs, fcs + 100).mkString(",")
        val fds = random.nextInt(1000)
        val fd = Range(fds, fds + 100).mkString(",")
        (key, fa, fb, fc, fd)
        }
    ).toDF("fb", "f6", "f7", "f8", "f9")
df2.createOrReplaceTempView("view2")
spark.sql("drop table if exists table1")
spark.sql("create table table1 as select * from view1")
spark.sql("drop table if exists table2")
spark.sql("create table table2 as select * from view2")
sys.exit(0)