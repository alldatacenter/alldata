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

package org.apache.celeborn.tests.flink;

import org.apache.flink.api.common.{ExecutionMode, InputDependencyConstraint, RuntimeExecutionMode}
import org.apache.flink.configuration.{ConfigConstants, Configuration, ExecutionOptions, RestOptions}
import org.apache.flink.runtime.jobgraph.JobType
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.graph.GlobalStreamExchangeMode
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.service.deploy.MiniClusterFeature;

class WordCountTest extends AnyFunSuite with Logging with MiniClusterFeature
  with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    logInfo("test initialized , setup rss mini cluster")
    val masterConf = Map(
      "celeborn.master.host" -> "localhost",
      "celeborn.master.port" -> "9097")
    val workerConf = Map("celeborn.master.endpoints" -> "localhost:9097")
    setUpMiniCluster(masterConf, workerConf)
  }

  override def afterAll(): Unit = {
    logInfo("all test complete , stop rss mini cluster")
    shutdownMiniCluster()
  }

  test("celeborn flink integration test - word count") {
    // set up execution environment
    val configuration = new Configuration
    val parallelism = 8
    configuration.setString(
      "shuffle-service-factory.class",
      "org.apache.celeborn.plugin.flink.RemoteShuffleServiceFactory")
    configuration.setString("celeborn.master.endpoints", "localhost:9097")
    configuration.setString("execution.batch-shuffle-mode", "ALL_EXCHANGES_BLOCKING")
    configuration.setBoolean(ConfigConstants.LOCAL_START_WEBSERVER, true)
    configuration.set(ExecutionOptions.RUNTIME_MODE, RuntimeExecutionMode.BATCH)
    configuration.setString("taskmanager.memory.network.min", "1024m")
    configuration.setString(RestOptions.BIND_PORT, "8081-8089")
    val env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(configuration)
    env.getConfig.setExecutionMode(ExecutionMode.BATCH)
    env.getConfig.setParallelism(parallelism)
    env.getConfig.setDefaultInputDependencyConstraint(InputDependencyConstraint.ALL)
    env.disableOperatorChaining()
    // make parameters available in the web interface
    WordCountHelper.execute(env, parallelism)

    val graph = env.getStreamGraph
    graph.setGlobalStreamExchangeMode(GlobalStreamExchangeMode.ALL_EDGES_BLOCKING)
    graph.setJobType(JobType.BATCH)
    env.execute(graph)
  }
}
