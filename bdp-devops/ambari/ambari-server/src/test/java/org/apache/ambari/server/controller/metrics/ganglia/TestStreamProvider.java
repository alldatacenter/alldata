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
package org.apache.ambari.server.controller.metrics.ganglia;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.internal.URLStreamProvider;

public class TestStreamProvider extends URLStreamProvider {
  // Allow for filename to be set at runtime
  protected String fileName;
  private String lastSpec;
  protected Set<String> specs = new HashSet<>();
  private boolean isLastSpecUpdated;

  public TestStreamProvider(String fileName) {
    super(1000, 1000, ComponentSSLConfiguration.instance());
    this.fileName = fileName;
  }

  @Override
  public TestHttpUrlConnection processURL(String spec, String requestMethod, String body, Map<String, List<String>> headers)
    throws IOException {
    return new TestHttpUrlConnection(readFrom(spec));
  }

  @Override
  public InputStream readFrom(String spec) throws IOException {
    if (!isLastSpecUpdated) {
      lastSpec = spec;
    }
    isLastSpecUpdated = false;
    specs.add(spec);
    
    return ClassLoader.getSystemResourceAsStream(fileName);
  }

  public String getLastSpec() {
    return lastSpec;
  }

  public Set<String> getAllSpecs() {
    return specs;
  }

  @Override
  public InputStream readFrom(String spec, String requestMethod, String params) throws IOException {
    lastSpec = spec + "?" + params;
    isLastSpecUpdated = true;
    return readFrom(spec);
  }
}
