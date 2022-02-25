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
package org.apache.ambari.server.utils;

import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class JaxbMapKeyValAdapter extends
    XmlAdapter<JaxbMapKeyVal[], Map<String, String>> {

  @Override
  public JaxbMapKeyVal[] marshal(Map<String, String> m) throws Exception {
    if (m==null) {
      return null;
    }
    JaxbMapKeyVal[] list = new JaxbMapKeyVal[m.size()] ;
    int index = 0;
    for (Map.Entry<String, String> entry : m.entrySet()) {
      JaxbMapKeyVal jaxbMap = new JaxbMapKeyVal(entry.getKey(), entry.getValue());
      list[index++] = jaxbMap;
    }
    return list;
  }

  @Override
  public Map<String, String> unmarshal(JaxbMapKeyVal[] jm) throws Exception {
    if (jm == null) {
      return null;
    }
    Map<String, String> m = new TreeMap<>();
    for (JaxbMapKeyVal jaxbMap : jm) {
      m.put(jaxbMap.key, jaxbMap.value);
    }
    return m;
  }

}
