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

package org.apache.ambari.server.controller.metrics;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.controller.jmx.JMXMetricHolder;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.junit.Assert;
import org.junit.Test;

public class ThreadPoolEnabledPropertyProviderTest {

  @Test
  public void testGetCacheKeyForException() throws Exception {
    ObjectMapper jmxObjectMapper = new ObjectMapper();
    jmxObjectMapper.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, false);
    ObjectReader jmxObjectReader = jmxObjectMapper.reader(JMXMetricHolder.class);

    List<Exception> exceptions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      try{
        jmxObjectReader.readValue("Invalid string");
      } catch (Exception e) {
        // Exception messages will be different with each iteration.
        exceptions.add(e);
      }
    }
    Assert.assertEquals(ThreadPoolEnabledPropertyProvider.getCacheKeyForException(exceptions.get(0)),
        ThreadPoolEnabledPropertyProvider.getCacheKeyForException(exceptions.get(1)));
  }
}
