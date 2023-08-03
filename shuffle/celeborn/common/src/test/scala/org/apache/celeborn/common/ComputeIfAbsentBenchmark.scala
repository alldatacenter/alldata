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

package org.apache.celeborn.common

import java.util.{HashMap => JHashMap, Map => JMap}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.{Function => JFunction}

import scala.util.Random

import org.apache.celeborn.benchmark.{Benchmark, BenchmarkBase}

/**
 * ComputeIfAbsent benchmark.
 * To run this benchmark:
 * {{{
 *   1. build/sbt "common/test:runMain <this class>"
 *   2. generate result:
 *      CELEBORN_GENERATE_BENCHMARK_FILES=1 build/sbt "common/test:runMain <this class>"
 *      Results will be written to "benchmarks/ComputeIfAbsentBenchmark-results.txt".
 * }}}
 */
object ComputeIfAbsentBenchmark extends BenchmarkBase {

  def test(name: String, map: JMap[Int, AtomicInteger], iters: Int): Unit = {
    runBenchmark(name) {
      val benchmark = new Benchmark(name, iters, output = output)
      benchmark.addCase("putIfAbsent") { _: Int =>
        var i = 0
        while (i < iters) {
          map.putIfAbsent(Random.nextInt(32), new AtomicInteger(0))
          i += 1
        }
      }

      benchmark.addCase("computeIfAbsent") { _: Int =>
        var i = 0
        while (i < iters) {
          map.computeIfAbsent(
            Random.nextInt(32),
            new JFunction[Int, AtomicInteger] {
              override def apply(v1: Int): AtomicInteger = new AtomicInteger(0)
            })
          i += 1
        }
      }
      benchmark.run()
    }
  }

  override def runBenchmarkSuite(mainArgs: Array[String]): Unit = {
    test("HashMap", new JHashMap[Int, AtomicInteger], 1 << 26)
    test("ConcurrentHashMap", new ConcurrentHashMap[Int, AtomicInteger], 1 << 26)
  }
}
