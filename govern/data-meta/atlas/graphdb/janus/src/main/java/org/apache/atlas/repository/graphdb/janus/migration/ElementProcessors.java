/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.repository.graphdb.janus.migration;

import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef.PropagateTags;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.atlas.repository.Constants.ATTRIBUTE_INDEX_PROPERTY_KEY;
import static org.apache.atlas.repository.Constants.ATTRIBUTE_KEY_PROPERTY_KEY;
import static org.apache.atlas.repository.Constants.CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY;
import static org.apache.atlas.repository.Constants.CLASSIFICATION_EDGE_NAME_PROPERTY_KEY;
import static org.apache.atlas.repository.Constants.CLASSIFICATION_ENTITY_GUID;
import static org.apache.atlas.repository.Constants.CLASSIFICATION_VERTEX_PROPAGATE_KEY;
import static org.apache.atlas.repository.Constants.ENTITY_TYPE_PROPERTY_KEY;
import static org.apache.atlas.repository.Constants.RELATIONSHIPTYPE_TAG_PROPAGATION_KEY;
import static org.apache.atlas.repository.Constants.STATE_PROPERTY_KEY;
import static org.apache.atlas.repository.graphdb.janus.migration.TypesDefScrubber.*;

public class ElementProcessors {
    private static final Logger LOG = LoggerFactory.getLogger(ElementProcessors.class);

    public  static final String   PRIMITIVE_MAP_CATEGORY       = "MAP_PRIMITIVE";
    public  static final String   NON_PRIMITIVE_MAP_CATEGORY   = "MAP";
    public  static final String   NON_PRIMITIVE_ARRAY_CATEGORY = "ARRAY";
    private static final String[] NON_PRIMITIVE_KEYS           = { ElementProcessors.NON_PRIMITIVE_ARRAY_CATEGORY };

    private final Map<String, RelationshipCacheGenerator.TypeInfo>  relationshipLookup;
    private final Map<String, Map<String, List<String>>>            postProcessMap;
    private final Map<String, ClassificationToStructDefName>        traitToTypeMap;

    private final NonPrimitiveListPropertyProcessor nonPrimitiveListPropertyProcessor = new NonPrimitiveListPropertyProcessor();
    private final NonPrimitiveMapPropertyProcessor  nonPrimitiveMapPropertyProcessor  = new NonPrimitiveMapPropertyProcessor();
    private final PrimitiveMapPropertyProcessor     primitiveMapPropertyProcessor     = new PrimitiveMapPropertyProcessor();
    private final EdgeCollectionPropertyProcessor   edgeCollectionPropertyProcessor   = new EdgeCollectionPropertyProcessor();
    private final EdgeRelationshipPropertyProcessor edgeRelationshipPropertyProcessor = new EdgeRelationshipPropertyProcessor();
    private final EdgeTraitTypesPropertyProcessor   edgeTraitTypesPropertyProcessor   = new EdgeTraitTypesPropertyProcessor();

    public ElementProcessors(AtlasTypeRegistry typeRegistry, TypesDefScrubber scrubber) {
        this(RelationshipCacheGenerator.get(typeRegistry),
             TypesWithCollectionsFinder.getVertexPropertiesForCollectionAttributes(typeRegistry),
             scrubber.getTraitToTypeMap());
    }

    ElementProcessors(Map<String, RelationshipCacheGenerator.TypeInfo> lookup,
                      Map<String, Map<String, List<String>>> postProcessMap,
                      Map<String, ClassificationToStructDefName> traitToTypeMap) {
        this.relationshipLookup = lookup;
        this.postProcessMap     = postProcessMap;
        this.traitToTypeMap     = traitToTypeMap;
    }

    public static String[] getNonPrimitiveCategoryKeys() {
        return NON_PRIMITIVE_KEYS;
    }

    public Map<String,Map<String, List<String>>> getPropertiesToPostProcess() {
        return postProcessMap;
    }

    public String addIndexKeysForCollections(Vertex out, Object edgeId, String label, Map<String, Object> edgeProperties) {
        return edgeCollectionPropertyProcessor.update(out, edgeId, label, edgeProperties);
    }

    public void processCollections(String typeNameKey, Map<String,Object> vertexProperties) {
        if (!vertexProperties.containsKey(typeNameKey)) {
            return;
        }

        String typeName = (String) vertexProperties.get(typeNameKey);

        if (!postProcessMap.containsKey(typeName)) {
            return;
        }

        primitiveMapPropertyProcessor.update(typeName, vertexProperties);
        nonPrimitiveMapPropertyProcessor.update(typeName, vertexProperties);
        nonPrimitiveListPropertyProcessor.update(typeName, vertexProperties);
    }

    public String updateEdge(Vertex in, Vertex out, Object edgeId, String label, Map<String,Object> props) {
        return edgeRelationshipPropertyProcessor.update(in, out, edgeId, label, props);
    }

    private class NonPrimitiveMapPropertyProcessor {
        final String category = NON_PRIMITIVE_MAP_CATEGORY;

        public void update(String typeName, Map<String,Object> vertexProperties) {
            if (!postProcessMap.containsKey(typeName)) {
                return;
            }

            if (!postProcessMap.get(typeName).containsKey(category)) {
                return;
            }

            List<String> propertyTypeList = postProcessMap.get(typeName).get(category);

            for (String property : propertyTypeList) {
                if (!vertexProperties.containsKey(property)) {
                    continue;
                }

                List<Object> list = (List<Object>) vertexProperties.get(property);

                if (list == null) {
                    continue;
                }

                for (Object listEntry : list) {
                    String key      = (String) listEntry;
                    String valueKey = getMapKey(property, key);

                    if (vertexProperties.containsKey(valueKey)) {
                        vertexProperties.remove(valueKey);
                    }
                }

                vertexProperties.remove(property);
            }
        }

        private String getMapKey(String property, String key) {
            return String.format("%s.%s", property, key);
        }
    }

    private class PrimitiveMapPropertyProcessor {
        final String category = PRIMITIVE_MAP_CATEGORY;

        public void update(String typeName, Map<String, Object> vertexProperties) {
            if (!postProcessMap.get(typeName).containsKey(category)) {
                return;
            }

            List<String> propertyTypeList = postProcessMap.get(typeName).get(category);

            for (String property : propertyTypeList) {
                if (!vertexProperties.containsKey(property)) {
                    continue;
                }

                List<Object> list = (List<Object>) vertexProperties.get(property);

                if (list == null) {
                    continue;
                }

                Map<String, Object> map = getAggregatedMap(vertexProperties, property, list);

                vertexProperties.put(property, map);
            }
        }

        private Map<String, Object> getAggregatedMap(Map<String, Object> vertexProperties, String property, List<Object> list) {
            Map<String, Object> map = new HashMap<>();

            for (Object listEntry : list) {
                String key      = (String) listEntry;
                String valueKey = getMapKey(property, key);

                if (vertexProperties.containsKey(valueKey)) {
                    Object value = getValueFromProperties(valueKey, vertexProperties);

                    vertexProperties.remove(valueKey);

                    map.put(key, value);
                }
            }

            return map;
        }

        private String getMapKey(String property, String key) {
            return String.format("%s.%s", property, key);
        }

        private Object getValueFromProperties(String key, Map<String, Object> vertexProperties) {
            if (!vertexProperties.containsKey(key)) {
                return null;
            }

            return vertexProperties.get(key);
        }
    }

    private class NonPrimitiveListPropertyProcessor {
        private final String category = NON_PRIMITIVE_ARRAY_CATEGORY;

        private void update(String typeName, Map<String,Object> props) {
            if(!postProcessMap.get(typeName).containsKey(category)) {
                return;
            }

            List<String> propertyTypeList = postProcessMap.get(typeName).get(category);
            for (String property : propertyTypeList) {
                if(!props.containsKey(property)) {
                    continue;
                }

                Map<String, String> listMap = getUpdatedEdgeList(props.get(property));

                if(listMap == null) {
                    continue;
                }

                props.put(property, listMap);
            }
        }

        private Map<String, String> getUpdatedEdgeList(Object o) {
            Map<String, String> listMap = new HashMap<>();

            if(!(o instanceof List)) {
                return null;
            }

            List list = (List) o;

            for (int i = 0; i < list.size(); i++) {
                listMap.put((String) list.get(i), Integer.toString(i));
            }

            return listMap;
        }
    }

    private class EdgeTraitTypesPropertyProcessor {
        private void update(String label, Vertex in) {
            if (traitToTypeMap.size() == 0) {
                return;
            }

            if (!in.property(ENTITY_TYPE_PROPERTY_KEY).isPresent()) {
                return;
            }

            String typeName = (String) in.property(ENTITY_TYPE_PROPERTY_KEY).value();
            String key      = label;

            if (!traitToTypeMap.containsKey(key)) {
                key = StringUtils.substringBeforeLast(key, ".");

                if(!traitToTypeMap.containsKey(key)) {
                    return;
                }
            }

            if (!traitToTypeMap.get(key).getTypeName().equals(typeName)) {
                return;
            }

            in.property(ENTITY_TYPE_PROPERTY_KEY, traitToTypeMap.get(key).getLegacyTypeName());
        }
    }

    private class EdgeRelationshipPropertyProcessor {
        public String update(Vertex in, Vertex out, Object edgeId, String label, Map<String, Object> props) {
            edgeTraitTypesPropertyProcessor.update(label, in);

            if(addRelationshipTypeForClassification(in, out, label, props)) {
                label = Constants.CLASSIFICATION_LABEL;
            } else {
                addRelationshipTypeName(label, props);

                label = addIndexKeysForCollections(out, edgeId, label, props);
            }

            addMandatoryRelationshipProperties(label, props);

            return label;
        }

        private String getRelationshipTypeName(String label) {
            return relationshipLookup.containsKey(label) ? relationshipLookup.get(label).getTypeName() : "";
        }

        private PropagateTags getDefaultPropagateValue(String label) {
            return relationshipLookup.containsKey(label) ?
                    relationshipLookup.get(label).getPropagateTags() :
                    AtlasRelationshipDef.PropagateTags.NONE;
        }

        private boolean addRelationshipTypeForClassification(Vertex in, Vertex out, String label, Map<String, Object> props) {
            if (in.property(ENTITY_TYPE_PROPERTY_KEY).isPresent()) {
                String inTypeName  = (String) in.property(ENTITY_TYPE_PROPERTY_KEY).value();

                if (StringUtils.isNotEmpty(inTypeName)) {
                    if (inTypeName.equals(label)) {
                        props.put(ENTITY_TYPE_PROPERTY_KEY, inTypeName);
                        props.put(CLASSIFICATION_EDGE_NAME_PROPERTY_KEY, inTypeName);

                        addEntityGuidToTrait(in, out);

                        return true;
                    }
                } else {
                    LOG.info("Could not find typeName for trait: {}", label);
                }
            }

            return false;
        }

        private void addEntityGuidToTrait(Vertex in, Vertex out) {
            String entityGuid = "";

            if (out.property(Constants.GUID_PROPERTY_KEY).isPresent()) {
                entityGuid = (String) out.property(Constants.GUID_PROPERTY_KEY).value();
            }

            if(StringUtils.isNotEmpty(entityGuid)) {
                in.property(CLASSIFICATION_ENTITY_GUID, entityGuid);
                in.property(CLASSIFICATION_VERTEX_PROPAGATE_KEY, false);
            }
        }

        private void addRelationshipTypeName(String edgeLabel, Map<String, Object> props) {
            String typeName = getRelationshipTypeName(edgeLabel);

            if (StringUtils.isNotEmpty(typeName)) {
                props.put(ENTITY_TYPE_PROPERTY_KEY, typeName);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Could not find relationship type for: {}", edgeLabel);
                }
            }
        }

        private void addMandatoryRelationshipProperties(String label, Map<String, Object> props) {
            props.put(Constants.RELATIONSHIP_GUID_PROPERTY_KEY, UUID.randomUUID().toString());
            props.put(RELATIONSHIPTYPE_TAG_PROPAGATION_KEY, String.valueOf(getDefaultPropagateValue(label)));
            props.put(CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, false);
        }
    }

    private class EdgeCollectionPropertyProcessor {
        private static final int LABEL_INDEX = 0;
        private static final int KEY_INDEX   = 1;

        public String update(Vertex out, Object edgeId, String label, Map<String, Object> edgeProperties) {
            String[] labelKeyPair = getNonPrimitiveArrayFromLabel(out, (String) edgeId, label);

            if (labelKeyPair != null) {
                edgeProperties.put(ATTRIBUTE_INDEX_PROPERTY_KEY, Integer.valueOf(labelKeyPair[KEY_INDEX]));

                return label;
            }

            labelKeyPair = getNonPrimitiveMapKeyFromLabel(out, label);

            if (labelKeyPair != null) {
                label = labelKeyPair[LABEL_INDEX];

                edgeProperties.put(ATTRIBUTE_KEY_PROPERTY_KEY, labelKeyPair[KEY_INDEX]);
            }

            return label;
        }

        private String[] getNonPrimitiveArrayFromLabel(Vertex v, String edgeId, String label) {
            if (!v.property(ENTITY_TYPE_PROPERTY_KEY).isPresent()) {
                return null;
            }

            String typeName     = (String) v.property(ENTITY_TYPE_PROPERTY_KEY).value();
            String propertyName = StringUtils.remove(label, Constants.INTERNAL_PROPERTY_KEY_PREFIX);

            if(!containsNonPrimitiveCollectionProperty(typeName, propertyName, NON_PRIMITIVE_ARRAY_CATEGORY)) {
                return null;
            }

            Map<String, String> edgeIdIndexList = (Map<String, String>) v.property(propertyName).value();

            if (edgeIdIndexList.containsKey(edgeId)) {
                return getLabelKeyPair(label, edgeIdIndexList.get(edgeId));
            }

            return null;
        }

        // legacy edge label is in format: __<type name>.<key>
        //      label: in new format which is type name
        // this method extracts:
        //      key: what remains of the legacy label string when '__' and type name are removed
        private String[] getNonPrimitiveMapKeyFromLabel(Vertex v, String label) {
            if (!v.property(ENTITY_TYPE_PROPERTY_KEY).isPresent()) {
                return null;
            }

            String typeName = (String) v.property(ENTITY_TYPE_PROPERTY_KEY).value();

            if(!postProcessMap.containsKey(typeName)) {
                return null;
            }

            if(!postProcessMap.get(typeName).containsKey(NON_PRIMITIVE_MAP_CATEGORY)) {
                return null;
            }

            String       propertyName = StringUtils.remove(label, Constants.INTERNAL_PROPERTY_KEY_PREFIX);
            List<String> properties   = postProcessMap.get(typeName).get(NON_PRIMITIVE_MAP_CATEGORY);

            for (String p : properties) {
                if (propertyName.startsWith(p)) {
                    return getLabelKeyPair(
                            String.format("%s%s", Constants.INTERNAL_PROPERTY_KEY_PREFIX, p),
                            StringUtils.remove(propertyName, p).substring(1).trim());
                }
            }

            return null;
        }

        private boolean containsNonPrimitiveCollectionProperty(String typeName, String propertyName, String categoryType) {
            if (!postProcessMap.containsKey(typeName)) {
                return false;
            }

            if (!postProcessMap.get(typeName).containsKey(categoryType)) {
                return false;
            }

            List<String> properties = postProcessMap.get(typeName).get(categoryType);

            for (String p : properties) {
                if (p.equals(propertyName)) {
                    return true;
                }
            }

            return false;
        }

        private String[] getLabelKeyPair(String label, String value) {
            return new String[] { label, value };
        }
    }
}
