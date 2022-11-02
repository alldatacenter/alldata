/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.state;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.junit.Test;

public class UriInfoTest {
  @Test
  public void testChoosesHttpByDefault() throws Exception {
    UriInfo uri = new UriInfo();
    uri.setHttpUri("${config1/http-host}/path");
    assertThat(resolved(uri), is("http://http-host/path"));
  }

  @Test
  public void testChoosesHttpsBasedOnProperties() throws Exception {
    UriInfo uri = new UriInfo();
    uri.setHttpUri("${config1/http-host}/path");
    uri.setHttpsUri("${config1/https-host}/path");
    uri.setHttpsProperty("${config1/use-http}");
    uri.setHttpsPropertyValue("YES");
    assertThat(resolved(uri), is("https://https-host/path"));
  }

  private Map<String, Map<String, String>> config() {
    return new HashMap<String, Map<String, String>>() {{
        put("config1", new HashMap<String, String>() {{
          put("http-host", "http-host");
          put("https-host", "https-host");
          put("use-http", "YES");
        }});
      }};
  }

  private String resolved(UriInfo uri) throws AmbariException {
    return uri.resolve(config()).toString();
  }
}