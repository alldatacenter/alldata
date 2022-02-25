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

import java.util.Collection;

import org.apache.ambari.server.controller.spi.Resource;

/**
 * Resource Definition for AlertDefinition types.
 */
public class AlertDefResourceDefinition extends BaseResourceDefinition {

  public static final String EXECUTE_IMMEDIATE_DIRECTIVE = "run_now";

  public AlertDefResourceDefinition() {
    super(Resource.Type.AlertDefinition);
  }

  @Override
  public String getPluralName() {
    return "alert_definitions";
  }

  @Override
  public String getSingularName() {
    return "alert_definition";
  }

  @Override
  public Collection<String> getUpdateDirectives() {
    Collection<String> directives = super.getCreateDirectives();
    directives.add(EXECUTE_IMMEDIATE_DIRECTIVE);
    return directives;
  }
}
