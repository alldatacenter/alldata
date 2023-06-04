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

package com.bytedance.bitsail.connector.legacy.jdbc.sink;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.jdbc.options.JdbcWriterOptions;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;
import com.bytedance.bitsail.test.connector.test.utils.JobConfUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Created 2022/10/18
 */
public class OracleSinkITCase {
  private static final Logger LOG = LoggerFactory.getLogger(OracleSinkITCase.class);

  public static final String ORACLE_DOCKER_IMAGER = "gvenzl/oracle-xe:18.4.0-slim";

  private OracleContainer container;

  @Before
  public void before() {
    container = new OracleContainer(ORACLE_DOCKER_IMAGER)
        .withDatabaseName("TEST")
        .withUsername("TEST")
        .withPassword("TEST_PASSWORD")
        .withInitScript("scripts/fake_to_oracle_sink.sql")
        .withLogConsumer(new Slf4jLogConsumer(LOG));

    /*
     * This test may get Error of 'SP2-0306: Invalid option.' when running on Apple M chips.
     * Please follow instructions in 'OracleITCaseAppleChipWorkaround.md' for more details.
     */
    Startables.deepStart(Stream.of(container)).join();
  }

  @After
  public void after() {
    container.close();
  }

  @Test
  public void testInsertModeOracle() throws Exception {
    BitSailConfiguration globalConfiguration = JobConfUtils.fromClasspath("scripts/fake_to_oracle_sink.json");

    Map<String, Object> connection = Maps.newHashMap();
    connection.put("db_url", container.getJdbcUrl());
    connection.put("host", container.getHost());
    connection.put("port", container.getFirstMappedPort());
    globalConfiguration.set(JdbcWriterOptions.CONNECTIONS, Lists.newArrayList(connection));
    EmbeddedFlinkCluster.submitJob(globalConfiguration);
  }
}