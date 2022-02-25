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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.query.render.DefaultRenderer;
import org.apache.ambari.server.api.query.render.MetricsPaddingRenderer;
import org.apache.ambari.server.api.query.render.MinimalRenderer;
import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;

/**
 * Base resource definition.  Contains behavior common to all resource types.
 */
public abstract class BaseResourceDefinition implements ResourceDefinition {

  /**
   * Resource type.  One of {@link Resource.Type}
   */
  private Resource.Type m_type;

  /**
   * The sub-resource type definitions.
   */
  private final Set<SubResourceDefinition> subResourceDefinitions = new HashSet<>();

  /**
   * A map of directives for the different request types, each entry is expected to be modifiable by sub resources.
   */
  private final Map<DirectiveType, Collection<String>> directives = new HashMap<>();

  /**
   * Constructor.
   *
   * @param resourceType resource type
   */
  public BaseResourceDefinition(Resource.Type resourceType) {
    this(resourceType, null, null);
  }

  /**
   * Constructor.
   *
   * @param resourceType  the resource type
   * @param subTypes      the sub-resource types
   */
  public BaseResourceDefinition(Resource.Type resourceType, Resource.Type ... subTypes) {
    this(resourceType, (subTypes == null) ? null : Arrays.asList(subTypes), null);
  }

  /**
   * Constructor.
   *
   * @param resourceType the resource type
   * @param subTypes     the sub-resource types
   * @param directives   a map of directives for request types for this resource
   */
  public BaseResourceDefinition(Resource.Type resourceType,
                                Collection<Resource.Type> subTypes,
                                Map<DirectiveType, ? extends Collection<String>> directives) {
    m_type = resourceType;

    if (subTypes != null) {
      for (Resource.Type subType : subTypes) {
        subResourceDefinitions.add(new SubResourceDefinition(subType));
      }
    }

    initializeDirectives(DirectiveType.READ, directives);
    initializeDirectives(DirectiveType.CREATE, directives);
    initializeDirectives(DirectiveType.UPDATE, directives);
    initializeDirectives(DirectiveType.DELETE, directives);
  }

  @Override
  public Resource.Type getType() {
    return m_type;
  }

  @Override
  public Set<SubResourceDefinition> getSubResourceDefinitions() {
    return subResourceDefinitions;
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = new ArrayList<>();
    listProcessors.add(new BaseHrefPostProcessor());
    return listProcessors;
  }

  @Override
  public Renderer getRenderer(String name) {
    if (name == null || name.equals("default")) {
      return new DefaultRenderer();
    } else if (name.equals("minimal")) {
      return new MinimalRenderer();
    } else if (name.contains("null_padding")
              || name.contains("no_padding")
              || name.contains("zero_padding")) {
      return new MetricsPaddingRenderer(name);
    } else {
      throw new IllegalArgumentException("Invalid renderer name for resource of type " + m_type);
    }
  }

  ClusterController getClusterController() {
    return ClusterControllerHelper.getClusterController();
  }

  @Override
  public Collection<String> getReadDirectives() {
    // return a collection which can be modified by sub resources
    return directives.get(DirectiveType.READ);
  }

  @Override
  public Collection<String> getCreateDirectives() {
    // return a collection which can be modified by sub resources
    return directives.get(DirectiveType.CREATE);
  }

  @Override
  public Collection<String> getUpdateDirectives() {
    // return a collection which can be modified by sub resources
    return directives.get(DirectiveType.UPDATE);
  }

  @Override
  public Collection<String> getDeleteDirectives() {
    // return a collection which can be modified by sub resources
    return directives.get(DirectiveType.DELETE);
  }

  @Override
  public boolean equals(Object o) {
    boolean result =false;
    if(this == o) result = true;
    if(o instanceof BaseResourceDefinition){
        BaseResourceDefinition other = (BaseResourceDefinition) o;
        if(m_type == other.m_type )
            result = true;
    }
    return result;
  }

  @Override
  public int hashCode() {
    return m_type.hashCode();
  }

  @Override
  public boolean isCreatable() {
    // by default all resources are creatable
    return true;
  }

  class BaseHrefPostProcessor implements PostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      Resource r = resultNode.getObject();
      TreeNode<Resource> parent = resultNode.getParent();

      if (parent.getName() != null) {

        int i = href.indexOf("?");
        if (i != -1) {
          href = href.substring(0, i);
        }

        if (!href.endsWith("/")) {
          href = href + '/';
        }

        Schema schema = getClusterController().getSchema(r.getType());
        Object id = r.getPropertyValue(schema.getKeyPropertyId(r.getType()));

        String hrefIdPart = urlencode(id);

        href = parent.getStringProperty("isCollection").equals("true") ?
            href + hrefIdPart : href + parent.getName() + '/' + hrefIdPart;
      }
      resultNode.setProperty("href", href);
    }

    /**
     * URL encodes the id (string) value
     *
     * @param id the id to URL encode
     * @return null if id is null, else the URL encoded value of the id
     */
    protected String urlencode(Object id) {
      if (id == null)
        return "";
      else {
        try {
          return new URLCodec().encode(id.toString());
        } catch (EncoderException e) {
          return id.toString();
        }
      }
    }
  }

  /**
   * Initializes the specified collection of directives into a modifiable set of directives for the specified type of directives.
   *
   * @param type       a request type
   * @param directives the map of directives from which to copy
   */
  private void initializeDirectives(DirectiveType type, Map<DirectiveType, ? extends Collection<String>> directives) {
    HashSet<String> requestDirectives = new HashSet<>();

    if ((directives != null) && directives.get(type) != null) {
      requestDirectives.addAll(directives.get(type));
    }

    this.directives.put(type, requestDirectives);
  }

  public enum DirectiveType {
    CREATE,
    READ,
    UPDATE,
    DELETE
  }
}
