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

package org.apache.ambari.server.api.resources;

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Resource;

/**
 * Service resource definition.
 */
public class ServiceResourceDefinition extends BaseResourceDefinition {

  /**
   * Constructor.
   *
   */
  public ServiceResourceDefinition() {
    super(Resource.Type.Service);
  }

  @Override
  public String getPluralName() {
    return "services";
  }

  @Override
  public String getSingularName() {
    return "service";
  }

  @Override
  public Set<SubResourceDefinition> getSubResourceDefinitions() {
    Set<SubResourceDefinition> subs = new HashSet<>();
    subs.add(new SubResourceDefinition(Resource.Type.Component));
    subs.add(new SubResourceDefinition(Resource.Type.Alert));
    //todo: dynamic sub-resource definition
    subs.add(new SubResourceDefinition(Resource.Type.Artifact));

    return subs;
  }
}
