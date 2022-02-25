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
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.ResultPostProcessor;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Resource Definition.
 * Provides information specific to a specific resource type.
 */
public interface ResourceDefinition {
  /**
   * Obtain the plural name of the resource.
   *
   * @return the plural name of the resource
   */
  String getPluralName();

  /**
   * Obtain the singular name of the resource.
   *
   * @return the singular name of the resource
   */
  String getSingularName();

  /**
   * Obtain the type of resource.  Is one of {@link Resource.Type}.
   *
   * @return the type of resource
   */
  Resource.Type getType();

  /**
   * Obtain a set of all child resource types.
   *
   * @return set of sub-resource definitions
   */
  Set<SubResourceDefinition> getSubResourceDefinitions();

  /**
   * Obtain any resource post processors.  A resource processor is used to provide resource specific processing of
   * results and is called by the {@link ResultPostProcessor} while post processing a result.
   *
   * @return list of resource specific result processors
   */
  List<PostProcessor> getPostProcessors();

  /**
   * Obtain the associated renderer based on name.
   *
   * @param name  name of the renderer to obtain
   *
   * @return associated renderer instance
   * @throws IllegalArgumentException if name is invalid for this resource
   */
  Renderer getRenderer(String name) throws IllegalArgumentException;

  /**
   * Obtain the set of read directives for the resource.  A get directive is
   * information that can be provided in the query string of a GET operation for
   * the resource.  These directives are not predicates but are put into the
   * map of request info properties used by the resource provider when getting
   * the resource.
   */
  Collection<String> getReadDirectives();

  /**
   * Obtain the set of create directives for the resource.  A create directive is
   * information that can be provided in the query string of a POST operation for
   * the resource.  These directives are not predicates but are put into the
   * map of request info properties used by the resource provider when creating
   * the resource.
   */
  Collection<String> getCreateDirectives();

  /**
   * Obtain the set of update directives for the resource.  An update directive is
   * information that can be provided in the query string of a PUT operation for
   * the resource.  These directives are not predicates but are put into the
   * map of request info properties used by the resource provider when updating
   * the resource.
   */
  Collection<String> getUpdateDirectives();

  /**
   * Obtain the set of delete directives for the resource.  A delete directive is
   * information that can be provided in the query string of a DELETE operation for
   * the resource.  These directives are not predicates but are put into the
   * map of request info properties used by the resource provider when updating
   * the resource.
   */
  Collection<String> getDeleteDirectives();

  /**
   * Defines if resource is actually created on the server side during POST
   * operation.
   *
   * @see <a
   *      href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.5">
   *      http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.5</a>
   * @return {@code true} if resource is creatable, {@code false} otherwise
   */
  boolean isCreatable();

  /**
   * Resource specific result processor.
   * Used to provide resource specific processing of a result.
   */
  interface PostProcessor {
    void process(Request request, TreeNode<Resource> resultNode, String href);
  }
}
