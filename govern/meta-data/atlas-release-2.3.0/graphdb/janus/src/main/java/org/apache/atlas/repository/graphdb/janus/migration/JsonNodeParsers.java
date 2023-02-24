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

package org.apache.atlas.repository.graphdb.janus.migration;

import org.apache.atlas.repository.Constants;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.shaded.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.apache.atlas.repository.Constants.EDGE_ID_IN_IMPORT_KEY;
import static org.apache.atlas.repository.Constants.VERTEX_ID_IN_IMPORT_KEY;

public class JsonNodeParsers {
    private static final Logger LOG = LoggerFactory.getLogger(JsonNodeParsers.class);


    static abstract class ParseElement {
        protected GraphSONUtility utility;

        abstract String getMessage();

        abstract Object getId(JsonNode node);

        abstract boolean isTypeNode(JsonNode node);

        abstract String getType(JsonNode node);

        public void setContext(GraphSONUtility utility) {
            this.utility = utility;
        }

        abstract Map<String, Object> parse(Graph gr, MappedElementCache cache, JsonNode Node);

        public void commit(Graph graph) {
            graph.tx().commit();
        }

        abstract Element get(Graph gr, Object id);

        abstract  Element getByOriginalId(Graph gr, Object id);

        public Element getByOriginalId(Graph gr, JsonNode node) {
            return getByOriginalId(gr, getId(node));
        }

        public Element update(Graph gr, Object id, Map<String,Object> schema) {
            Element el = get(gr, id);

            for (Map.Entry<String, Object> entry : schema.entrySet()) {
                el.property(entry.getKey(), entry.getValue());
            }

            return el;
        }

    }

    static class ParseEdge extends ParseElement {
        private static final String MESSAGE_EDGE          = "edge";


        @Override
        public String getMessage() {
            return MESSAGE_EDGE;
        }

        @Override
        Object getId(JsonNode node) {
            return utility.getTypedValueFromJsonNode(node.get(GraphSONTokensTP2._ID));
        }

        @Override
        public Map<String, Object> parse(Graph gr, MappedElementCache cache, JsonNode node) {
            return utility.edgeFromJson(gr, cache, node);
        }

        @Override
        Element get(Graph gr, Object id) {
            return gr.edges(id).next();
        }

        @Override
        Element getByOriginalId(Graph gr, Object id) {
            try {
                return gr.traversal().E().has(EDGE_ID_IN_IMPORT_KEY, id).next();
            } catch (NoSuchElementException ex) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("getByOriginalId: {}: failed: {}", getMessage(), id, ex);
                }
            }
            catch (Exception ex) {
                LOG.error("getByOriginalId: {}: failed: {}", getMessage(), id, ex);
            }

            return null;
        }

        @Override
        public boolean isTypeNode(JsonNode node) {
            return node.get(GraphSONTokensTP2._LABEL).textValue().startsWith(Constants.TYPENAME_PROPERTY_KEY);
        }

        @Override
        public String getType(JsonNode node) {
            return node.get(GraphSONTokensTP2._LABEL).textValue();
        }
    }

    static class ParseVertex extends ParseElement {
        private static final String NODE_VALUE_KEY = "value";
        private static final String MESSAGE_VERTEX = "vertex";

        @Override
        public String getMessage() {
            return MESSAGE_VERTEX;
        }

        @Override
        Object getId(JsonNode node) {
            return utility.getTypedValueFromJsonNode(node.get(GraphSONTokensTP2._ID));
        }

        @Override
        public Map<String, Object> parse(Graph graph, MappedElementCache cache, JsonNode node) {
            return utility.vertexFromJson(graph, node);
        }

        @Override
        Element get(Graph gr, Object id) {
            return gr.vertices(id).next();
        }

        @Override
        Element getByOriginalId(Graph gr, Object id) {
            try {
                return gr.traversal().V().has(VERTEX_ID_IN_IMPORT_KEY, id).next();
            } catch (NoSuchElementException ex) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("getByOriginalId: {}: failed: {}", getMessage(), id, ex);
                }
            } catch (Exception ex) {
                LOG.error("getByOriginalId: {}: failed: {}", getMessage(), id, ex);
            }

            return null;
        }

        @Override
        public boolean isTypeNode(JsonNode node) {
            return node.has(Constants.TYPENAME_PROPERTY_KEY);
        }

        @Override
        public String getType(JsonNode node) {
            return node.has(Constants.ENTITY_TYPE_PROPERTY_KEY) ? node.get(Constants.ENTITY_TYPE_PROPERTY_KEY).get(NODE_VALUE_KEY).toString() : "";
        }
    }
}
