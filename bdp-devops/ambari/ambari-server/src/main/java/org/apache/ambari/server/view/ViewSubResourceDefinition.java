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

package org.apache.ambari.server.view;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.api.resources.BaseResourceDefinition;
import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.view.configuration.ResourceConfig;

/**
 * View sub-resource definition based on a view resource configuration.
 */
public class ViewSubResourceDefinition extends BaseResourceDefinition {
  /**
   * The associated view definition.
   */
  private final ViewEntity viewDefinition;

  /**
   * The configuration.
   */
  private final ResourceConfig resourceConfiguration;

  /**
   * The sub resource definitions.
   */
  Set<SubResourceDefinition> definitions;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a sub-resource definition.
   *
   * @param viewDefinition         the view definition
   * @param resourceConfiguration  the resource configuration
   */
  public ViewSubResourceDefinition(ViewEntity viewDefinition, ResourceConfig resourceConfiguration) {
    super(new Resource.Type(viewDefinition.getQualifiedResourceTypeName(resourceConfiguration.getName())));

    this.viewDefinition        = viewDefinition;
    this.resourceConfiguration = resourceConfiguration;
  }


  // ----- ResourceDefinition ------------------------------------------------

  @Override
  public String getPluralName() {
    return resourceConfiguration.getPluralName();
  }

  @Override
  public String getSingularName() {
    return resourceConfiguration.getName();
  }

  @Override
  public synchronized Set<SubResourceDefinition> getSubResourceDefinitions() {
    if (definitions == null) {
      definitions = new HashSet<>();
      List<String> subResourceNames = resourceConfiguration.getSubResourceNames();
      if (subResourceNames != null) {
        for (String subType : subResourceNames) {
          Resource.Type type = Resource.Type.valueOf(
              viewDefinition.getQualifiedResourceTypeName(subType));
          definitions.add(new SubResourceDefinition(type));
        }
      }
    }
    return definitions;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Get the associated resource configuration.
   *
   * @return the resource configuration
   */
  public ResourceConfig getResourceConfiguration() {
    return resourceConfiguration;
  }
}
