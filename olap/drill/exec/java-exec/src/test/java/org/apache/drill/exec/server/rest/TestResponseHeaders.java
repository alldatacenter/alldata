/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.server.rest;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.RestClientFixture;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.drill.exec.ExecConstants.HTTP_ENABLE;
import static org.apache.drill.exec.ExecConstants.HTTP_JETTY_SERVER_RESPONSE_HEADERS;
import static org.apache.drill.exec.ExecConstants.HTTP_PORT_HUNT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestResponseHeaders extends ClusterTest {

  private static final String BASE_URL = "";

  @SuppressWarnings("serial")
  @BeforeClass
  public static void setUp() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .configProperty(HTTP_ENABLE, true)
        .configProperty(HTTP_PORT_HUNT, true)
        .configProperty(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, false);
    builder.configBuilder().put(HTTP_JETTY_SERVER_RESPONSE_HEADERS, new HashMap<String, String>() {{
      put("MyHeader", "102030");
    }});
    startCluster(builder);
  }

  @Test
  public void checkConfiguredHeaders() throws Exception {
    try (RestClientFixture restClient = cluster.restClientFixture()) {
      MultivaluedMap<String, String> responseHeaders = restClient.getResponseHeaders(BASE_URL);
      assertThat(responseHeaders.get("MyHeader").get(0), equalTo("102030"));
    }
  }
}
