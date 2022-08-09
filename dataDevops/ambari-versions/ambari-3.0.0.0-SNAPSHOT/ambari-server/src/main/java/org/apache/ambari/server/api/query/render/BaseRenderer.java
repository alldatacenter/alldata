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

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.spi.SchemaFactory;


/**
 * Base renderer.
 * Contains functionality which may be common across implementations.
 */
public abstract class BaseRenderer implements Renderer {

  /**
   * Factory for creating schema instances.
   */
  private SchemaFactory m_schemaFactory;

  @Override
  public void init(SchemaFactory schemaFactory) {
    m_schemaFactory = schemaFactory;
  }

  @Override
  public boolean requiresPropertyProviderInput() {
    // most renderers require the property provider input support
    return true;
  }

  /**
   * Obtain a schema instance based on resource type.
   *
   * @param type  resource type
   *
   * @return schema instance for the provided resource type
   */
  protected Schema getSchema(Resource.Type type) {
    return m_schemaFactory.getSchema(type);
  }

  /**
   * Copies a tree of QueryInfo to a tree of Set<String>.
   * This is useful in {@link Renderer#finalizeProperties(TreeNode, boolean)} for converting
   * the passed in tree of query info to the return type which is a set of property names.
   *
   * @param queryTree     source tree
   * @param propertyTree  target tree
   */
  protected void copyPropertiesToResult(TreeNode<QueryInfo> queryTree, TreeNode<Set<String>> propertyTree) {
    for (TreeNode<QueryInfo> node : queryTree.getChildren()) {
      TreeNode<Set<String>> child = propertyTree.addChild(
          node.getObject().getProperties(), node.getName());
      copyPropertiesToResult(node, child);
    }
  }

  /**
   * Add primary key for the specified resource type to the provided set.
   *
   * @param resourceType  resource type
   * @param properties    set of properties which pk will be added to
   */
  protected void addPrimaryKey(Resource.Type resourceType, Set<String> properties) {
    properties.add(getSchema(resourceType).getKeyPropertyId(resourceType));
  }

  /**
   * Add primary and all foreign keys for the specified resource type to the provided set.
   *
   * @param resourceType  resource type
   * @param properties    set of properties which keys will be added to
   */
  protected void addKeys(Resource.Type resourceType, Set<String> properties) {
    Schema schema = getSchema(resourceType);

    for (Resource.Type type : Resource.Type.values()) {
      String propertyId = schema.getKeyPropertyId(type);
      if (propertyId != null) {
        properties.add(propertyId);
      }
    }
  }

  /**
   * Determine if the query node contains no properties and no children.
   *
   * @param queryNode  the query node to check
   *
   * @return true if the node contains no properties or children; false otherwise
   */
  protected boolean isRequestWithNoProperties(TreeNode<QueryInfo> queryNode) {
    return queryNode.getChildren().isEmpty() &&
        queryNode.getObject().getProperties().size() == 0;
  }

  /**
   * Add available sub resources to property node.
   *
   * @param queryTree     query tree
   * @param propertyTree  property tree
   */
  protected void addSubResources(TreeNode<QueryInfo> queryTree,
                                 TreeNode<Set<String>> propertyTree) {

    QueryInfo queryInfo = queryTree.getObject();
    ResourceDefinition resource = queryInfo.getResource();
    Set<SubResourceDefinition> subResources = resource.getSubResourceDefinitions();
    for (SubResourceDefinition subResource : subResources) {
      Set<String> resourceProperties = new HashSet<>();
      populateSubResourceDefaults(subResource, resourceProperties);
      propertyTree.addChild(resourceProperties, subResource.getType().name());
    }
  }

  /**
   * Populate sub-resource properties.
   *
   * @param subResource  definition of sub-resource
   * @param properties   property set to update
   */
  protected void populateSubResourceDefaults(
      SubResourceDefinition subResource, Set<String> properties) {

    Schema schema = getSchema(subResource.getType());
    Set<Resource.Type> foreignKeys = subResource.getAdditionalForeignKeys();
    for (Resource.Type fk : foreignKeys) {
      properties.add(schema.getKeyPropertyId(fk));
    }
    addPrimaryKey(subResource.getType(), properties);
    addKeys(subResource.getType(), properties);
  }

  /**
   * Add required primary and foreign keys properties based on request type.
   *
   * @param propertyTree  tree of properties
   * @param addIfEmpty    whether keys should be added to node with no properties
   */
  protected void ensureRequiredProperties(
      TreeNode<Set<String>> propertyTree, boolean addIfEmpty) {

    Resource.Type type = Resource.Type.valueOf(propertyTree.getName());
    Set<String> properties = propertyTree.getObject();

    if (!properties.isEmpty() || addIfEmpty) {
      addKeys(type, properties);
    }

    for (TreeNode<Set<String>> child : propertyTree.getChildren()) {
      ensureRequiredProperties(child, addIfEmpty);
    }
  }
}
