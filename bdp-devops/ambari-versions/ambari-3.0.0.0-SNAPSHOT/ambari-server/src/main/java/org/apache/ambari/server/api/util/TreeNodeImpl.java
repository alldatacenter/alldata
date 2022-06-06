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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Basic implementation of TreeNode.
 */
public class TreeNodeImpl<T> implements TreeNode<T> {

  /**
   * name of the node
   */
  private String m_name;

  /**
   * parent of the node
   */
  private TreeNode<T> m_parent;

  /**
   * child nodes
   */
  private Map<String, TreeNode<T>> m_mapChildren = new LinkedHashMap<>();

  /**
   * associated object
   */
  private T m_object;

  /**
   * properties
   */
  private Map<String, Object> m_mapNodeProps;

  /**
   * Constructor.
   *
   * @param parent parent node
   * @param object associated object
   * @param name   node name
   */
  public TreeNodeImpl(TreeNode<T> parent, T object, String name) {
    m_parent = parent;
    m_object = object;
    m_name = name;
  }

  @Override
  public TreeNode<T> getParent() {
    return m_parent;
  }

  @Override
  public Collection<TreeNode<T>> getChildren() {
    return m_mapChildren.values();
  }

  @Override
  public T getObject() {
    return m_object;
  }

  @Override
  public void setName(String name) {
    m_name = name;
  }

  @Override
  public String getName() {
    return m_name;
  }

  @Override
  public void setParent(TreeNode<T> parent) {
    m_parent = parent;
  }

  @Override
  public TreeNode<T> addChild(T child, String name) {
    TreeNodeImpl<T> node = new TreeNodeImpl<>(this, child, name);
    m_mapChildren.put(name, node);

    return node;
  }

  @Override
  public TreeNode<T> addChild(TreeNode<T> child) {
    child.setParent(this);
    m_mapChildren.put(child.getName(), child);

    return child;
  }

  @Override
  public TreeNode<T> removeChild(String name) {
    return m_mapChildren.remove(name);
  }

  @Override
  public void setProperty(String name, Object value) {
    if (m_mapNodeProps == null) {
      m_mapNodeProps = new LinkedHashMap<>();
    }
    m_mapNodeProps.put(name, value);
  }

  @Override
  public Object getProperty(String name) {
    return m_mapNodeProps == null ? null : m_mapNodeProps.get(name);
  }

  @Override
  public String getStringProperty(String name) {
    Object value = getProperty(name);
    return value == null ? null : value.toString();
  }

  @Override
  public void removeProperty(String name) {
    if (m_mapNodeProps != null) {
      m_mapNodeProps.remove(name);
    }
  }

  @Override
  public TreeNode<T> getChild(String name) {
    if (name != null && name.contains("/")) {
      int i = name.indexOf('/');
      String s = name.substring(0, i);
      TreeNode<T> node = m_mapChildren.get(s);
      return node == null ? null : node.getChild(name.substring(i + 1));
    } else {
      return m_mapChildren.get(name);
    }
  }
}
