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

package org.apache.ambari.server.api.util;

import java.util.Collection;

/**
 * Tree where each node can have a name, properties and an associated object.
 */
public interface TreeNode<T> {
  /**
   * Obtain the parent node or null if this node is the root.
   *
   * @return the parent node or null if this node is the root
   */
  TreeNode<T> getParent();

  /**
   * Obtain the list of child nodes.
   *
   * @return a list of child nodes or an empty list if a leaf node
   */
  Collection<TreeNode<T>> getChildren();

  /**
   * Obtain the object associated with this node.
   *
   * @return the object associated with this node or null
   */
  T getObject();

  /**
   * Obtain the name of the node.
   *
   * @return the name of the node or null
   */
  String getName();

  /**
   * Set the name of the node.
   *
   * @param name the name to set
   */
  void setName(String name);

  /**
   * Set the parent node.
   *
   * @param parent the parent node to set
   */
  void setParent(TreeNode<T> parent);

  /**
   * Add a child node for the provided object.
   *
   * @param child the object associated with the new child node
   * @param name  the name of the child node
   * @return the newly created child node
   */
  TreeNode<T> addChild(T child, String name);

  /**
   * Add the specified child node.
   *
   * @param child the child node to add
   * @return the added child node
   */
  TreeNode<T> addChild(TreeNode<T> child);

  /**
   * Remove a child from this resource.
   *
   * @param name  name of child to remove
   *
   * @return the previous value associated with key, or null if there was no mapping for key
   */
  TreeNode<T> removeChild(String name);

  /**
   * Set a property on the node.
   *
   * @param name  the name of the property
   * @param value the value of the property
   */
  void setProperty(String name, Object value);

  /**
   * Get the specified node property.
   *
   * @param name property name
   * @return the requested property value or null
   */
  Object getProperty(String name);

  /**
   * Get the specified node property as a String.
   *
   * @param name property name
   * @return the requested property value (as a String) or null
   */
  String getStringProperty(String name);

  /**
   * Remove a property from the node.
   *
   * @param name  name of property to be removed
   */
  void removeProperty(String name);

  /**
   * Find a child node by name.
   * The name may contain '/' to delimit names to find a child more then one level deep.
   * To find a node named 'bar' that is a child of a child named 'foo', use the name 'foo/bar'.
   *
   * @param name  the name of the child.  May contain the '/' path separator.
   *
   * @return the requested node or null if the child was not found
   */
  TreeNode<T> getChild(String name);
}
