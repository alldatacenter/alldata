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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class JaxbMapKeyListAdapter extends
    XmlAdapter<JaxbMapKeyList[], Map<String, List<String>>> {

  @Override
  public JaxbMapKeyList[] marshal(Map<String, List<String>> map)
      throws Exception {
    if (map==null) {
      return null;
    }
    JaxbMapKeyList[] list = new JaxbMapKeyList[map.size()] ;
    int index = 0;
    for (Map.Entry<String, List<String>> stringListEntry : map.entrySet()) {
      JaxbMapKeyList jaxbMap = new JaxbMapKeyList(stringListEntry.getKey(), stringListEntry.getValue());
      list[index++] = jaxbMap;
    }
    return list;
  }

  @Override
  public Map<String, List<String>> unmarshal(JaxbMapKeyList[] list)
      throws Exception {
    if (list == null) {
      return null;
    }
    Map<String, List<String>> m = new TreeMap<>();
    for (JaxbMapKeyList jaxbMap : list) {
      m.put(jaxbMap.key, jaxbMap.value);
    }
    return m;
  }
}
