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
import java.util.Collections;
import java.util.Set;

import org.apache.ambari.server.controller.internal.VersionDefinitionResourceProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;

import com.google.common.collect.Sets;

/**
 * The Resource Definition used for Version Definition files.
 */
public class VersionDefinitionResourceDefinition extends BaseResourceDefinition {

  public VersionDefinitionResourceDefinition() {
    super(Resource.Type.VersionDefinition);
  }

  @Override
  public String getPluralName() {
    return "version_definitions";
  }

  @Override
  public String getSingularName() {
    return "version_definition";
  }

  @Override
  public Set<SubResourceDefinition> getSubResourceDefinitions() {
    return Collections.singleton(new SubResourceDefinition(Type.OperatingSystem));
  }

  @Override
  public Collection<String> getCreateDirectives() {
    return Sets.newHashSet(Request.DIRECTIVE_DRY_RUN,
        VersionDefinitionResourceProvider.DIRECTIVE_SKIP_URL_CHECK);
  }

}
