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

package org.apache.ambari.server.api.query.render;


import java.util.Set;

import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultPostProcessor;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.SchemaFactory;

/**
 * Responsible for the rendering of a result.
 * This includes both the content (which properties), and the format
 * of the query result.  Format doesn't refer to json or xml, but
 * instead to the structure of the categories, sub-resources and
 * properties.  Renderer's are registered for a resource type by
 * adding them to the corresponding resource definition.
 */
public interface Renderer {

  /**
   * Set a schema factory on the renderer.
   *
   * @param schemaFactory  factory of schema instances
   */
  void init(SchemaFactory schemaFactory);

  /**
   * Finalize which properties are requested by the query.
   * This is called once per user query regardless of
   * how many sub-queries the original query is decomposed
   * into.
   *
   * @param queryProperties  tree of query information.  Contains query information
   *                         for the root query and all sub-queries (children)
   * @param isCollection     whether the query is a collection
   *
   * @return tree of sets of string properties for each query including any sub-queries
   */
  TreeNode<Set<String>> finalizeProperties(
    TreeNode<QueryInfo> queryProperties, boolean isCollection);

  /**
   * Finalize the query results.
   *
   * @param queryResult result of query in native (default) format
   *
   * @return result in the format dictated by the renderer
   */
  Result finalizeResult(Result queryResult);

  /**
   * Obtain the associated post processor.
   * Post Processors existed prior to renderer's to allow the native result
   * to be augmented before returning it to the user.  This functionality should
   * be merged into the renderer.
   *
   * @param request  original request
   *
   * @return associated post processor
   */
  ResultPostProcessor getResultPostProcessor(Request request);


  /**
   * Obtains the property provider requirements of the given
   * renderer implementation.
   *
   * @return true if property provider support is required
   *         false if property provider support is not required
   */
  boolean requiresPropertyProviderInput();
}
