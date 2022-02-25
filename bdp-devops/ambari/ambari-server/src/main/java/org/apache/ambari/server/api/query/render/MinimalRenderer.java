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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultPostProcessor;
import org.apache.ambari.server.api.services.ResultPostProcessorImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

/**
 * Minimal Renderer.
 *
 * All href properties are stripped from the result.
 *
 * For the root resource, this renderer behaves identically to the
 * default renderer wrt resource properties and sub-resources. If
 * no root properties or sub-resources are specified all top level
 * properties and all sub-resources are included in the result. If
 * any root properties or any sub-resources are requested, then only
 * those will be included in the result.
 *
 * For sub-resource, only primary keys and any requested properties
 * are included in the result.
 *
 * This renderer can be specified for any resource using
 * 'format=minimal' or the older syntax 'minimal_response=true'.
 */
public class MinimalRenderer extends BaseRenderer implements Renderer {

  /**
   * Type of root resource.
   */
  private Resource.Type m_rootType;

  /**
   * Whether the request is for a collection.
   */
  private boolean m_isCollection;

  /**
   * Map of requested properties.
   */
  private Map<Resource.Type, Set<String>> m_originalProperties =
    new HashMap<>();

  // ----- Renderer ----------------------------------------------------------

  @Override
  public TreeNode<Set<String>> finalizeProperties(
      TreeNode<QueryInfo> queryTree, boolean isCollection) {

    QueryInfo queryInfo = queryTree.getObject();
    TreeNode<Set<String>> resultTree = new TreeNodeImpl<>(
      null, queryInfo.getProperties(), queryTree.getName());

    copyPropertiesToResult(queryTree, resultTree);

    m_rootType     = queryTree.getObject().getResource().getType();
    m_isCollection = isCollection;

    boolean addKeysToEmptyResource = true;
    if (! isCollection && isRequestWithNoProperties(queryTree)) {
      addSubResources(queryTree, resultTree);
      addKeysToEmptyResource = false;
    }
    processRequestedProperties(queryTree);
    ensureRequiredProperties(resultTree, addKeysToEmptyResource);

    return resultTree;
  }

  @Override
  public Result finalizeResult(Result queryResult) {
    // can't just return result, need to strip added properties.
    processResultNode(queryResult.getResultTree());
    return queryResult;
  }

  @Override
  public ResultPostProcessor getResultPostProcessor(Request request) {
    return new MinimalPostProcessor(request);
  }

  // ----- private instance methods ------------------------------------------

  /**
   * Recursively save all requested properties to check the result
   * properties against.
   *
   * @param queryTree  query tree to process
   */
  private void processRequestedProperties(TreeNode<QueryInfo> queryTree) {
    QueryInfo queryInfo = queryTree.getObject();
    if (queryInfo != null) {
      Resource.Type type = queryInfo.getResource().getType();
      Set<String> properties = m_originalProperties.get(type);
      if (properties == null) {
        properties = new HashSet<>();
        m_originalProperties.put(type, properties);
      }
      properties.addAll(queryInfo.getProperties());
      for (TreeNode<QueryInfo> child : queryTree.getChildren()) {
        processRequestedProperties(child);
      }
    }
  }

  /**
   * Recursively strip all unwanted properties from the result nodes.
   * During normal processing, foreign keys are always added to the request which need
   * to be stripped unless they were requested.
   *
   * @param node  node to process for extra properties
   */
  private void processResultNode(TreeNode<Resource> node) {
    Resource resource = node.getObject();
    if (resource != null && ( resource.getType() != m_rootType || m_isCollection)) {
      Resource.Type type = resource.getType();
      Set<String> requestedProperties = m_originalProperties.get(type);
      Map<String, Map<String, Object>> properties = resource.getPropertiesMap();

      Iterator<Map.Entry<String, Map<String, Object>>> iter;
      for(iter = properties.entrySet().iterator(); iter.hasNext(); ) {
        Map.Entry<String, Map<String, Object>> entry = iter.next();
        String categoryName = entry.getKey();
        Iterator<String> valueIter;

        for(valueIter = entry.getValue().keySet().iterator(); valueIter.hasNext(); ) {
          String propName = valueIter.next();
          // if property was not requested and it is not a pk, remove
          String absPropertyName = PropertyHelper.getPropertyId(categoryName, propName);
          if ((requestedProperties == null ||
              (! requestedProperties.contains(absPropertyName) &&
              ! isSubCategory(requestedProperties, categoryName))) &&
              ! getPrimaryKeys(type).contains(absPropertyName)) {
            valueIter.remove();
          }
        }
        if (entry.getValue().isEmpty()) {
          iter.remove();
        }
      }
    }
    for (TreeNode<Resource> child : node.getChildren()) {
      processResultNode(child);
    }
  }

  /**
   * Checks if properties contains a category for the subCategoryName
   * Example: metrics/yarn/Queue/root/ActiveApplications and metrics/yarn/Queue
   *
   * @param properties categories
   * @param subCategoryName subcategory
   * @return
   */
  private boolean isSubCategory(Set<String> properties, String subCategoryName) {
    for (String property : properties) {
      if (subCategoryName.startsWith(property)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Obtain the primary keys for the specified type.
   * This method is necessary because some resource types, specifically
   * the configuration type, don't have a proper pk even though one is
   * registered.  Instead, multiple properties are used as a 'composite'
   * key even though this is not supported by the framework.
   *
   * @param type  resource type
   *
   * @return set of pk's for a type
   */
  private Set<String> getPrimaryKeys(Resource.Type type) {
    Set<String> primaryKeys = new HashSet<>();

    if (type == Resource.Type.Configuration) {
      primaryKeys.add("type");
      primaryKeys.add("tag");
    } else {
      Map<Resource.Type, String> keys = PropertyHelper.getKeyPropertyIds(type);
      if (keys != null) {
        String pk = PropertyHelper.getKeyPropertyIds(type).get(type);
        if (pk != null) {
          primaryKeys = Collections.singleton(pk);
        }
      }
    }
    return primaryKeys;
  }

  // ----- inner classes -----------------------------------------------------

  /**
   * Post processor which doesn't generate href properties in the result tree.
   */
  private static class MinimalPostProcessor extends ResultPostProcessorImpl {
    private MinimalPostProcessor(Request request) {
      super(request);
    }

    @Override
    protected void finalizeNode(TreeNode<Resource> node) {
      node.removeProperty("href");
    }
  }
}
