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

package org.apache.celeborn.common.util

import java.util

import org.apache.celeborn.CelebornFunSuite
import org.apache.celeborn.common.protocol.PartitionLocation
import org.apache.celeborn.common.protocol.message.ControlMessages.{GetReducerFileGroupResponse, MapperEnd}
import org.apache.celeborn.common.protocol.message.StatusCode

class UtilsSuite extends CelebornFunSuite {

  test("stringToSeq") {
    val seq1 = Seq("asd", "bcd", "def")
    assert(seq1 == Utils.stringToSeq("asd,bcd,def"))

    val seq2 = Seq("a", "b", "d")
    assert(seq2 == Utils.stringToSeq("a,,b,d,"))
  }

  test("byteStringAsKB") {
    assert(1 == Utils.byteStringAsKb("1KB"))
    assert(1024 == Utils.byteStringAsKb("1MB"))
  }

  test("byteStringAsMb") {
    assert(16 == Utils.byteStringAsMb("16384KB"))
    assert(1 == Utils.byteStringAsMb("1MB"))
  }

  test("byteStringAsGb") {
    assert(16 == Utils.byteStringAsGb("16384MB"))
    assert(1 == Utils.byteStringAsGb("1GB"))
  }

  test("memoryStringToMb") {
    assert(16 == Utils.memoryStringToMb("16MB"))
    assert(16384 == Utils.memoryStringToMb("16GB"))
  }

  test("bytesToString") {
    assert("16.0 KB" == Utils.bytesToString(16384))
    assert("16.0 MB" == Utils.bytesToString(16777216))
    assert("16.0 GB" == Utils.bytesToString(17179869184L))
  }

  test("extractHostPortFromRssUrl") {
    val target = ("abc", 123)
    val result = Utils.extractHostPortFromRssUrl("rss://abc:123")
    assert(target.equals(result))
  }

  test("tryOrExit") {
    Utils.tryOrExit({
      val a = 1
      val b = 3
      a + b
    })
  }

  test("encodeFileNameToURIRawPath") {
    assert("abc%3F" == Utils.encodeFileNameToURIRawPath("abc?"))
    assert("abc%3E" == Utils.encodeFileNameToURIRawPath("abc>"))
  }

  test("classIsLoadable") {
    assert(Utils.classIsLoadable("java.lang.String"))
    assert(false == Utils.classIsLoadable("a.b.c.d.e.f"))
  }

  test("splitPartitionLocationUniqueId") {
    assert((1, 1).equals(Utils.splitPartitionLocationUniqueId("1-1")))
  }

  test("bytesToInt") {
    assert(1229202015 == Utils.bytesToInt(Array(73.toByte, 68.toByte, 34.toByte, 95.toByte)))

    assert(1596081225 == Utils.bytesToInt(Array(73.toByte, 68.toByte, 34.toByte, 95.toByte), false))
  }

  test("getThreadDump") {
    assert(Utils.getThreadDump().nonEmpty)
  }

  test("MapperEnd class convert with pb") {
    val mapperEnd = MapperEnd("application1", 1, 1, 1, 2, 1)
    val mapperEndTrans =
      Utils.fromTransportMessage(Utils.toTransportMessage(mapperEnd)).asInstanceOf[MapperEnd]
    assert(mapperEnd == mapperEndTrans)
  }

  test("GetReducerFileGroupResponse class convert with pb") {
    val fileGroup = new util.HashMap[Integer, util.Set[PartitionLocation]]
    fileGroup.put(0, partitionLocation(0))
    fileGroup.put(1, partitionLocation(1))
    fileGroup.put(2, partitionLocation(2))

    val attempts = Array(0, 0, 1)
    val response = GetReducerFileGroupResponse(StatusCode.STAGE_ENDED, fileGroup, attempts)
    val responseTrans = Utils.fromTransportMessage(Utils.toTransportMessage(response)).asInstanceOf[
      GetReducerFileGroupResponse]

    assert(response.status == responseTrans.status)
    assert(response.attempts.deep == responseTrans.attempts.deep)
    val set =
      (response.fileGroup.values().toArray diff responseTrans.fileGroup.values().toArray).toSet
    assert(set.size == 0)
  }

  def partitionLocation(partitionId: Int): util.HashSet[PartitionLocation] = {
    val partitionSet = new util.HashSet[PartitionLocation]
    for (i <- 0 until 3) {
      partitionSet.add(new PartitionLocation(
        partitionId,
        i,
        "host",
        100,
        1000,
        1001,
        100,
        PartitionLocation.Mode.MASTER))
    }
    partitionSet
  }
}
