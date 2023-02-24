/**
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
package org.apache.atlas.repository.graphdb.janus.graphson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Configure how the GraphSON utility treats edge and vertex properties.
 */
public class AtlasElementPropertyConfig {

    /**
     * Rules for element properties.
     */
    public enum ElementPropertiesRule {
        INCLUDE, EXCLUDE
    }

    private final List<String> vertexPropertyKeys;
    private final List<String> edgePropertyKeys;
    private final ElementPropertiesRule vertexPropertiesRule;
    private final ElementPropertiesRule edgePropertiesRule;
    private final boolean normalized;

    /**
     * A configuration that includes all properties of vertices and edges.
     */
    public static final AtlasElementPropertyConfig ALL_PROPERTIES = new AtlasElementPropertyConfig(null, null,
            ElementPropertiesRule.INCLUDE, ElementPropertiesRule.INCLUDE, false);

    public AtlasElementPropertyConfig(final Set<String> vertexPropertyKeys, final Set<String> edgePropertyKeys,
            final ElementPropertiesRule vertexPropertiesRule, final ElementPropertiesRule edgePropertiesRule) {
        this(vertexPropertyKeys, edgePropertyKeys, vertexPropertiesRule, edgePropertiesRule, false);
    }

    public AtlasElementPropertyConfig(final Set<String> vertexPropertyKeys, final Set<String> edgePropertyKeys,
            final ElementPropertiesRule vertexPropertiesRule, final ElementPropertiesRule edgePropertiesRule,
            final boolean normalized) {
        this.vertexPropertiesRule = vertexPropertiesRule;
        this.vertexPropertyKeys = sortKeys(vertexPropertyKeys, normalized);
        this.edgePropertiesRule = edgePropertiesRule;
        this.edgePropertyKeys = sortKeys(edgePropertyKeys, normalized);
        this.normalized = normalized;
    }

    /**
     * Construct a configuration that includes the specified properties from
     * both vertices and edges.
     */
    public static AtlasElementPropertyConfig includeProperties(final Set<String> vertexPropertyKeys,
                                                               final Set<String> edgePropertyKeys) {
        return new AtlasElementPropertyConfig(vertexPropertyKeys, edgePropertyKeys, ElementPropertiesRule.INCLUDE,
                ElementPropertiesRule.INCLUDE);
    }

    public static AtlasElementPropertyConfig includeProperties(final Set<String> vertexPropertyKeys,
                                                               final Set<String> edgePropertyKeys,
                                                               final boolean normalized) {
        return new AtlasElementPropertyConfig(vertexPropertyKeys, edgePropertyKeys, ElementPropertiesRule.INCLUDE,
                ElementPropertiesRule.INCLUDE, normalized);
    }

    /**
     * Construct a configuration that excludes the specified properties from
     * both vertices and edges.
     */
    public static AtlasElementPropertyConfig excludeProperties(final Set<String> vertexPropertyKeys,
                                                               final Set<String> edgePropertyKeys) {
        return new AtlasElementPropertyConfig(vertexPropertyKeys, edgePropertyKeys, ElementPropertiesRule.EXCLUDE,
                ElementPropertiesRule.EXCLUDE);
    }

    public static AtlasElementPropertyConfig excludeProperties(final Set<String> vertexPropertyKeys,
                                                               final Set<String> edgePropertyKeys,
                                                               final boolean normalized) {
        return new AtlasElementPropertyConfig(vertexPropertyKeys, edgePropertyKeys, ElementPropertiesRule.EXCLUDE,
                ElementPropertiesRule.EXCLUDE, normalized);
    }

    public List<String> getVertexPropertyKeys() {
        return vertexPropertyKeys;
    }

    public List<String> getEdgePropertyKeys() {
        return edgePropertyKeys;
    }

    public ElementPropertiesRule getVertexPropertiesRule() {
        return vertexPropertiesRule;
    }

    public ElementPropertiesRule getEdgePropertiesRule() {
        return edgePropertiesRule;
    }

    public boolean isNormalized() {
        return normalized;
    }

    private static List<String> sortKeys(final Set<String> keys, final boolean normalized) {
        final List<String> propertyKeyList;
        if (keys != null) {
            if (normalized) {
                final List<String> sorted = new ArrayList<String>(keys);
                Collections.sort(sorted);
                propertyKeyList = sorted;
            } else {
                propertyKeyList = new ArrayList<String>(keys);
            }
        } else {
            propertyKeyList = null;
        }

        return propertyKeyList;
    }
}
