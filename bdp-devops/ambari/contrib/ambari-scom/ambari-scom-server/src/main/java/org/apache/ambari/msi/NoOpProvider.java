/**
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
package org.apache.ambari.msi;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A NO-OP resource provider for a MSI defined cluster.
 */
public class NoOpProvider extends BaseResourceProvider {

  private final Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();

  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  public void updateProperties(Resource resource, Request request, Predicate predicate) {
    // Do nothing
  }

  @Override
  public int updateProperties(Resource resource, Map<String, Object> properties) {
    // Do nothing
    return -1;
  }


  // ----- Constructors ------------------------------------------------------

  public NoOpProvider(Resource.Type type, ClusterDefinition clusterDefinition) {
    super(type, clusterDefinition);
    keyPropertyIds.put(type, "id");
  }


  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    return Collections.emptySet();
  }
}
