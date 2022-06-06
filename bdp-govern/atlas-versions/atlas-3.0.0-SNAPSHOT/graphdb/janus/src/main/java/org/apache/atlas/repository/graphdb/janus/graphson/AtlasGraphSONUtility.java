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

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasElement;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.graphdb.janus.graphson.AtlasElementPropertyConfig.ElementPropertiesRule;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This class was largely removed from tinkerpop 1. We're adding it back here to
 * avoid changing the format of the JSON that we produce.
 *
 * Helps write individual graph elements to TinkerPop JSON format known as
 * GraphSON.
 */
public final class AtlasGraphSONUtility {

    private static final JsonNodeFactory JSON_NODE_FACTORY = JsonNodeFactory.instance;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AtlasGraphSONMode mode;
    private final List<String> vertexPropertyKeys;
    private final List<String> edgePropertyKeys;

    private final ElementPropertiesRule vertexPropertiesRule;
    private final ElementPropertiesRule edgePropertiesRule;
    private final boolean normalized;

    private final boolean includeReservedVertexId;
    private final boolean includeReservedEdgeId;
    private final boolean includeReservedVertexType;
    private final boolean includeReservedEdgeType;
    private final boolean includeReservedEdgeLabel;
    private final boolean includeReservedEdgeOutV;
    private final boolean includeReservedEdgeInV;

    /**
     * A GraphSONUtility that includes the specified properties.
     */
    private AtlasGraphSONUtility(final AtlasGraphSONMode mode, final Set<String> vertexPropertyKeySet,
            final Set<String> edgePropertyKeySet) {

        AtlasElementPropertyConfig config = AtlasElementPropertyConfig.includeProperties(vertexPropertyKeySet,
                edgePropertyKeySet);

        this.vertexPropertyKeys = config.getVertexPropertyKeys();
        this.edgePropertyKeys = config.getEdgePropertyKeys();
        this.vertexPropertiesRule = config.getVertexPropertiesRule();
        this.edgePropertiesRule = config.getEdgePropertiesRule();
        this.normalized = config.isNormalized();

        this.mode = mode;

        this.includeReservedVertexId = includeReservedKey(mode, AtlasGraphSONTokens.INTERNAL_ID, vertexPropertyKeys,
                this.vertexPropertiesRule);
        this.includeReservedEdgeId = includeReservedKey(mode, AtlasGraphSONTokens.INTERNAL_ID, edgePropertyKeys,
                this.edgePropertiesRule);
        this.includeReservedVertexType = includeReservedKey(mode, AtlasGraphSONTokens.INTERNAL_TYPE, vertexPropertyKeys,
                this.vertexPropertiesRule);
        this.includeReservedEdgeType = includeReservedKey(mode, AtlasGraphSONTokens.INTERNAL_TYPE, edgePropertyKeys,
                this.edgePropertiesRule);
        this.includeReservedEdgeLabel = includeReservedKey(mode, AtlasGraphSONTokens.INTERNAL_LABEL, edgePropertyKeys,
                this.edgePropertiesRule);
        this.includeReservedEdgeOutV = includeReservedKey(mode, AtlasGraphSONTokens.INTERNAL_OUT_V, edgePropertyKeys,
                this.edgePropertiesRule);
        this.includeReservedEdgeInV = includeReservedKey(mode, AtlasGraphSONTokens.INTERNAL_IN_V, edgePropertyKeys,
                this.edgePropertiesRule);
    }

    /*
     * Creates GraphSON for a single graph element.
     */
    private JSONObject jsonFromElement(final AtlasElement element) throws JSONException {
        final ObjectNode objectNode = this.objectNodeFromElement(element);

        try {
            return new JSONObject(new JSONTokener(MAPPER.writeValueAsString(objectNode)));
        } catch (IOException ioe) {
            // repackage this as a JSONException...seems sensible as the caller will only know about
            // the jettison object not being created
            throw new JSONException(ioe);
        }
    }

    /**
     * Creates GraphSON for a single graph element.
     */
    private ObjectNode objectNodeFromElement(final AtlasElement element) {
        final boolean isEdge = element instanceof AtlasEdge;
        final boolean showTypes = mode == AtlasGraphSONMode.EXTENDED;
        final List<String> propertyKeys = isEdge ? this.edgePropertyKeys : this.vertexPropertyKeys;
        final ElementPropertiesRule elementPropertyConfig = isEdge ? this.edgePropertiesRule
                : this.vertexPropertiesRule;

        final ObjectNode jsonElement = createJSONMap(
                createPropertyMap(element, propertyKeys, elementPropertyConfig, normalized), propertyKeys, showTypes);

        if ((isEdge && this.includeReservedEdgeId) || (!isEdge && this.includeReservedVertexId)) {
            putObject(jsonElement, AtlasGraphSONTokens.INTERNAL_ID, element.getId());
        }

        // it's important to keep the order of these straight.  check AtlasEdge first and then AtlasVertex because there
        // are graph implementations that have AtlasEdge extend from AtlasVertex
        if (element instanceof AtlasEdge) {
            final AtlasEdge edge = (AtlasEdge) element;

            if (this.includeReservedEdgeId) {
                putObject(jsonElement, AtlasGraphSONTokens.INTERNAL_ID, element.getId());
            }

            if (this.includeReservedEdgeType) {
                jsonElement.put(AtlasGraphSONTokens.INTERNAL_TYPE, AtlasGraphSONTokens.EDGE);
            }

            if (this.includeReservedEdgeOutV) {
                putObject(jsonElement, AtlasGraphSONTokens.INTERNAL_OUT_V, edge.getOutVertex().getId());
            }

            if (this.includeReservedEdgeInV) {
                putObject(jsonElement, AtlasGraphSONTokens.INTERNAL_IN_V, edge.getInVertex().getId());
            }

            if (this.includeReservedEdgeLabel) {
                jsonElement.put(AtlasGraphSONTokens.INTERNAL_LABEL, edge.getLabel());
            }
        } else if (element instanceof AtlasVertex) {
            if (this.includeReservedVertexId) {
                putObject(jsonElement, AtlasGraphSONTokens.INTERNAL_ID, element.getId());
            }

            if (this.includeReservedVertexType) {
                jsonElement.put(AtlasGraphSONTokens.INTERNAL_TYPE, AtlasGraphSONTokens.VERTEX);
            }
        }

        return jsonElement;
    }

    /**
     * Creates a Jettison JSONObject from a graph element.
     *
     * @param element
     *            the graph element to convert to JSON.
     * @param propertyKeys
     *            The property getPropertyKeys() at the root of the element to
     *            serialize. If null, then all getPropertyKeys() are serialized.
     * @param mode
     *            the type of GraphSON to be generated.
     */
    public static JSONObject jsonFromElement(final AtlasElement element, final Set<String> propertyKeys,
                                             final AtlasGraphSONMode mode)
        throws JSONException {

        final AtlasGraphSONUtility graphson = element instanceof AtlasEdge
                ? new AtlasGraphSONUtility(mode, null, propertyKeys)
                : new AtlasGraphSONUtility(mode, propertyKeys, null);
        return graphson.jsonFromElement(element);
    }

    private static ObjectNode objectNodeFromElement(final AtlasElement element, final List<String> propertyKeys,
                                                    final AtlasGraphSONMode mode) {
        final AtlasGraphSONUtility graphson = element instanceof AtlasEdge
                ? new AtlasGraphSONUtility(mode, null, new HashSet<String>(propertyKeys))
                : new AtlasGraphSONUtility(mode, new HashSet<String>(propertyKeys), null);
        return graphson.objectNodeFromElement(element);
    }

    private static boolean includeReservedKey(final AtlasGraphSONMode mode, final String key,
                                              final List<String> propertyKeys, final ElementPropertiesRule rule) {
        // the key is always included in modes other than compact.  if it is compact, then validate that the
        // key is in the property key list
        return mode != AtlasGraphSONMode.COMPACT || includeKey(key, propertyKeys, rule);
    }

    private static boolean includeKey(final String key, final List<String> propertyKeys,
                                      final ElementPropertiesRule rule) {
        if (propertyKeys == null) {
            // when null always include the key and shortcut this piece
            return true;
        }

        // default the key situation.  if it's included then it should be explicitly defined in the
        // property getPropertyKeys() list to be included or the reverse otherwise
        boolean keySituation = rule == ElementPropertiesRule.INCLUDE;

        switch (rule) {
        case INCLUDE:
            keySituation = propertyKeys.contains(key);
            break;
        case EXCLUDE:
            keySituation = !propertyKeys.contains(key);
            break;
        default:
            throw new RuntimeException("Unhandled rule: " + rule);
        }

        return keySituation;
    }

    private static ArrayNode createJSONList(final List<Object> list, final List<String> propertyKeys,
                                            final boolean showTypes) {
        final ArrayNode jsonList = JSON_NODE_FACTORY.arrayNode();
        for (Object item : list) {
            if (item instanceof AtlasElement) {
                jsonList.add(objectNodeFromElement((AtlasElement) item, propertyKeys,
                        showTypes ? AtlasGraphSONMode.EXTENDED : AtlasGraphSONMode.NORMAL));
            } else if (item instanceof List) {
                jsonList.add(createJSONList((List<Object>) item, propertyKeys, showTypes));
            } else if (item instanceof Map) {
                jsonList.add(createJSONMap((Map<String, Object>) item, propertyKeys, showTypes));
            } else if (item != null && item.getClass().isArray()) {
                jsonList.add(createJSONList(convertArrayToList(item), propertyKeys, showTypes));
            } else {
                addObject(jsonList, item);
            }
        }
        return jsonList;
    }

    private static ObjectNode createJSONMap(final Map<String, Object> map, final List<String> propertyKeys,
                                            final boolean showTypes) {
        final ObjectNode jsonMap = JSON_NODE_FACTORY.objectNode();
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            if (value != null) {
                if (value instanceof List) {
                    value = createJSONList((List<Object>) value, propertyKeys, showTypes);
                } else if (value instanceof Map) {
                    value = createJSONMap((Map<String, Object>) value, propertyKeys, showTypes);
                } else if (value instanceof AtlasElement) {
                    value = objectNodeFromElement((AtlasElement) value, propertyKeys,
                            showTypes ? AtlasGraphSONMode.EXTENDED : AtlasGraphSONMode.NORMAL);
                } else if (value.getClass().isArray()) {
                    value = createJSONList(convertArrayToList(value), propertyKeys, showTypes);
                }
            }

            putObject(jsonMap, key.toString(), getValue(value, showTypes));
        }
        return jsonMap;

    }

    private static void addObject(final ArrayNode jsonList, final Object value) {
        if (value == null) {
            jsonList.add((JsonNode) null);
        } else if (value.getClass() == Boolean.class) {
            jsonList.add((Boolean) value);
        } else if (value.getClass() == Long.class) {
            jsonList.add((Long) value);
        } else if (value.getClass() == Integer.class) {
            jsonList.add((Integer) value);
        } else if (value.getClass() == Float.class) {
            jsonList.add((Float) value);
        } else if (value.getClass() == Double.class) {
            jsonList.add((Double) value);
        } else if (value.getClass() == Byte.class) {
            jsonList.add((Byte) value);
        } else if (value.getClass() == Short.class) {
            jsonList.add((Short) value);
        } else if (value.getClass() == String.class) {
            jsonList.add((String) value);
        } else if (value instanceof ObjectNode) {
            jsonList.add((ObjectNode) value);
        } else if (value instanceof ArrayNode) {
            jsonList.add((ArrayNode) value);
        } else {
            jsonList.add(value.toString());
        }
    }

    private static void putObject(final ObjectNode jsonMap, final String key, final Object value) {
        if (value == null) {
            jsonMap.put(key, (JsonNode) null);
        } else if (value.getClass() == Boolean.class) {
            jsonMap.put(key, (Boolean) value);
        } else if (value.getClass() == Long.class) {
            jsonMap.put(key, (Long) value);
        } else if (value.getClass() == Integer.class) {
            jsonMap.put(key, (Integer) value);
        } else if (value.getClass() == Float.class) {
            jsonMap.put(key, (Float) value);
        } else if (value.getClass() == Double.class) {
            jsonMap.put(key, (Double) value);
        } else if (value.getClass() == Short.class) {
            jsonMap.put(key, (Short) value);
        } else if (value.getClass() == Byte.class) {
            jsonMap.put(key, (Byte) value);
        } else if (value.getClass() == String.class) {
            jsonMap.put(key, (String) value);
        } else if (value instanceof ObjectNode) {
            jsonMap.put(key, (ObjectNode) value);
        } else if (value instanceof ArrayNode) {
            jsonMap.put(key, (ArrayNode) value);
        } else {
            jsonMap.put(key, value.toString());
        }
    }

    private static Map<String, Object> createPropertyMap(final AtlasElement element, final List<String> propertyKeys,
                                                         final ElementPropertiesRule rule, final boolean normalized) {
        final Map<String, Object> map = new HashMap<String, Object>();
        final List<String> propertyKeyList;
        if (normalized) {
            final List<String> sorted = new ArrayList<String>(element.getPropertyKeys());
            Collections.sort(sorted);
            propertyKeyList = sorted;
        } else {
            propertyKeyList = new ArrayList<String>(element.getPropertyKeys());
        }

        if (propertyKeys == null) {
            for (String key : propertyKeyList) {
                final Object valToPutInMap = element.getProperty(key, Object.class);
                if (valToPutInMap != null) {
                    map.put(key, valToPutInMap);
                }
            }
        } else {
            if (rule == ElementPropertiesRule.INCLUDE) {
                for (String key : propertyKeys) {
                    final Object valToPutInMap = element.getProperty(key, Object.class);
                    if (valToPutInMap != null) {
                        map.put(key, valToPutInMap);
                    }
                }
            } else {
                for (String key : propertyKeyList) {
                    if (!propertyKeys.contains(key)) {
                        final Object valToPutInMap = element.getProperty(key, Object.class);
                        if (valToPutInMap != null) {
                            map.put(key, valToPutInMap);
                        }
                    }
                }
            }
        }

        return map;
    }

    private static Object getValue(Object value, final boolean includeType) {

        Object returnValue = value;

        // if the includeType is set to true then show the data types of the properties
        if (includeType) {

            // type will be one of: map, list, string, long, int, double, float.
            // in the event of a complex object it will call a toString and store as a
            // string
            String type = determineType(value);

            ObjectNode valueAndType = JSON_NODE_FACTORY.objectNode();
            valueAndType.put(AtlasGraphSONTokens.TYPE, type);

            if (type.equals(AtlasGraphSONTokens.TYPE_LIST)) {

                // values of lists must be accumulated as ObjectNode objects under the value key.
                // will return as a ArrayNode. called recursively to traverse the entire
                // object graph of each item in the array.
                ArrayNode list = (ArrayNode) value;

                // there is a set of values that must be accumulated as an array under a key
                ArrayNode valueArray = valueAndType.putArray(AtlasGraphSONTokens.VALUE);
                for (int ix = 0; ix < list.size(); ix++) {
                    // the value of each item in the array is a node object from an ArrayNode...must
                    // get the value of it.
                    addObject(valueArray, getValue(getTypedValueFromJsonNode(list.get(ix)), includeType));
                }

            } else if (type.equals(AtlasGraphSONTokens.TYPE_MAP)) {

                // maps are converted to a ObjectNode.  called recursively to traverse
                // the entire object graph within the map.
                ObjectNode convertedMap = JSON_NODE_FACTORY.objectNode();
                ObjectNode jsonObject = (ObjectNode) value;
                Iterator<?> keyIterator = jsonObject.fieldNames();
                while (keyIterator.hasNext()) {
                    Object key = keyIterator.next();

                    // no need to getValue() here as this is already a ObjectNode and should have type info
                    convertedMap.put(key.toString(), jsonObject.get(key.toString()));
                }

                valueAndType.put(AtlasGraphSONTokens.VALUE, convertedMap);
            } else {

                // this must be a primitive value or a complex object.  if a complex
                // object it will be handled by a call to toString and stored as a
                // string value
                putObject(valueAndType, AtlasGraphSONTokens.VALUE, value);
            }

            // this goes back as a JSONObject with data type and value
            returnValue = valueAndType;
        }

        return returnValue;
    }

    private static Object getTypedValueFromJsonNode(JsonNode node) {
        Object theValue = null;

        if (node != null && !node.isNull()) {
            if (node.isBoolean()) {
                theValue = node.booleanValue();
            } else if (node.isDouble()) {
                theValue = node.doubleValue();
            } else if (node.isFloatingPointNumber()) {
                theValue = node.floatValue();
            } else if (node.isInt()) {
                theValue = node.intValue();
            } else if (node.isLong()) {
                theValue = node.longValue();
            } else if (node.isTextual()) {
                theValue = node.textValue();
            } else if (node.isArray()) {
                // this is an array so just send it back so that it can be
                // reprocessed to its primitive components
                theValue = node;
            } else if (node.isObject()) {
                // this is an object so just send it back so that it can be
                // reprocessed to its primitive components
                theValue = node;
            } else {
                theValue = node.textValue();
            }
        }

        return theValue;
    }

    private static List<Object> convertArrayToList(final Object value) {
        final ArrayList<Object> list = new ArrayList<Object>();
        int arrlength = Array.getLength(value);
        for (int i = 0; i < arrlength; i++) {
            Object object = Array.get(value, i);
            list.add(object);
        }
        return list;
    }

    private static String determineType(final Object value) {
        String type = AtlasGraphSONTokens.TYPE_STRING;
        if (value == null) {
            type = AtlasGraphSONTokens.TYPE_UNKNOWN;
        } else if (value.getClass() == Double.class) {
            type = AtlasGraphSONTokens.TYPE_DOUBLE;
        } else if (value.getClass() == Float.class) {
            type = AtlasGraphSONTokens.TYPE_FLOAT;
        } else if (value.getClass() == Byte.class) {
            type = AtlasGraphSONTokens.TYPE_BYTE;
        } else if (value.getClass() == Short.class) {
            type = AtlasGraphSONTokens.TYPE_SHORT;
        } else if (value.getClass() == Integer.class) {
            type = AtlasGraphSONTokens.TYPE_INTEGER;
        } else if (value.getClass() == Long.class) {
            type = AtlasGraphSONTokens.TYPE_LONG;
        } else if (value.getClass() == Boolean.class) {
            type = AtlasGraphSONTokens.TYPE_BOOLEAN;
        } else if (value instanceof ArrayNode) {
            type = AtlasGraphSONTokens.TYPE_LIST;
        } else if (value instanceof ObjectNode) {
            type = AtlasGraphSONTokens.TYPE_MAP;
        }

        return type;
    }

    static class ElementFactory {

    }
}
