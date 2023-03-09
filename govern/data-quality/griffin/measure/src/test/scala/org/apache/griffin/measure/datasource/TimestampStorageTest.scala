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

package org.apache.griffin.measure.datasource

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._

class TimestampStorageTest extends AnyFlatSpec with Matchers {

  "timestamp storage" should "be able to insert a timestamp" in {
    val timestampStorage = TimestampStorage()
    timestampStorage.insert(1L)
    timestampStorage.all should be(Set(1L))
  }

  it should "be able to insert timestamps" in {
    val timestampStorage = TimestampStorage()
    timestampStorage.insert(Seq(1L, 2L, 3L))
    timestampStorage.all should be(Set(1L, 2L, 3L))
  }

  it should "be able to remove a timestamp" in {
    val timestampStorage = TimestampStorage()
    timestampStorage.insert(Seq(1L, 2L, 3L))
    timestampStorage.remove(1L)
    timestampStorage.all should be(Set(2L, 3L))
  }

  it should "be able to remove timestamps" in {
    val timestampStorage = TimestampStorage()
    timestampStorage.insert(Seq(1L, 2L, 3L))
    timestampStorage.remove(Seq(1L, 2L))
    timestampStorage.all should be(Set(3L))
  }

  it should "be able to get timestamps in range [a, b)" in {
    val timestampStorage = TimestampStorage()
    timestampStorage.insert(Seq(6L, 2L, 3L, 4L, 8L))
    timestampStorage.fromUntil(2L, 6L) should be(Set(2L, 3L, 4L))
  }

  it should "be able to get timestamps in range (a, b]" in {
    val timestampStorage = TimestampStorage()
    timestampStorage.insert(Seq(6L, 2L, 3L, 4L, 8L))
    timestampStorage.afterTil(2L, 6L) should be(Set(3L, 4L, 6L))
  }

  it should "be able to get timestamps smaller than b" in {
    val timestampStorage = TimestampStorage()
    timestampStorage.insert(Seq(6L, 2L, 3L, 4L, 8L))
    timestampStorage.until(8L) should be(Set(2L, 3L, 4L, 6L))
  }

  it should "be able to get timestamps bigger than or equal a" in {
    val timestampStorage = TimestampStorage()
    timestampStorage.insert(Seq(6L, 2L, 3L, 4L, 8L))
    timestampStorage.from(4L) should be(Set(4L, 6L, 8L))
  }

}
