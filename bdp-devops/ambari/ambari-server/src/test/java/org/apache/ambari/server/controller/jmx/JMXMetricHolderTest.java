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
package org.apache.ambari.server.controller.jmx;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class JMXMetricHolderTest {
  private JMXMetricHolder metrics = new JMXMetricHolder();

  @Before
  public void setUp() {
    metrics.setBeans(asList(
      new HashMap<String, Object>() {{
        put("name", "bean1");
        put("value", "val1");
      }},
      new HashMap<String, Object>() {{
        put("name", "bean2");
        put("value", "val2");
      }},
      new HashMap<String, Object>() {{
        put("name", "nested");
        put("value", new HashMap<String, Object>() {{
          put("key1", "nested-val1");
          put("key2", "nested-val2");
        }});
      }}
    ));
  }

  @Test
  public void testFindSingleBeanByName() throws Exception {
    assertThat(metrics.find("bean1/value"), is(Optional.of("val1")));
    assertThat(metrics.find("bean2/value"), is(Optional.of("val2")));
    assertThat(metrics.find("bean3/notfound"), is(Optional.empty()));
  }

  @Test
  public void testFindMultipleBeansByName() throws Exception {
    List<Object> result = metrics.findAll(asList("bean1/value", "bean2/value", "bean3/notfound"));
    assertThat(result, hasItems("val1", "val2"));
  }


  @Test
  public void testFindNestedBean() throws Exception {
    List<Object> result = metrics.findAll(asList("nested/value[key1]", "nested/value[key2]"));
    assertThat(result, hasItems("nested-val1", "nested-val2"));
  }
}