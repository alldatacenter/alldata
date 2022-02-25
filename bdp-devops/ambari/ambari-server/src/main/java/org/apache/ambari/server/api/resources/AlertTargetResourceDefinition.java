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
 * The {@link AlertTargetResourceDefinition} class is used to register alert
 * targets to be returned via the REST API.
 */
public class AlertTargetResourceDefinition extends BaseResourceDefinition {

  public static final String VALIDATE_CONFIG_DIRECTIVE = "validate_config";
  public static final String OVERWRITE_DIRECTIVE = "overwrite_existing";

  /**
   * Constructor.
   */
  public AlertTargetResourceDefinition() {
    super(Resource.Type.AlertTarget);
  }

  /**
   *
   */
  @Override
  public String getPluralName() {
    return "alert_targets";
  }

  /**
   *
   */
  @Override
  public String getSingularName() {
    return "alert_target";
  }

  @Override
  public Collection<String> getCreateDirectives() {
    Collection<String> directives = super.getCreateDirectives();
    directives.add(VALIDATE_CONFIG_DIRECTIVE);
    directives.add(OVERWRITE_DIRECTIVE);
    return directives;
  }
}
