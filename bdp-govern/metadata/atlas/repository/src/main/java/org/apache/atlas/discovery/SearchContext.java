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
package org.apache.atlas.discovery;


import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graph.GraphHelper;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.util.AtlasRepositoryConfiguration;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.atlas.discovery.SearchProcessor.ALL_TYPE_QUERY;
import static org.apache.atlas.model.discovery.SearchParameters.ALL_CLASSIFICATIONS;
import static org.apache.atlas.model.discovery.SearchParameters.ALL_CLASSIFICATION_TYPES;
import static org.apache.atlas.model.discovery.SearchParameters.ALL_ENTITY_TYPES;
import static org.apache.atlas.model.discovery.SearchParameters.FilterCriteria;
import static org.apache.atlas.model.discovery.SearchParameters.NO_CLASSIFICATIONS;
import static org.apache.atlas.model.discovery.SearchParameters.WILDCARD_CLASSIFICATIONS;

/*
 * Search context captures elements required for performing a basic search
 * For every search request the search context will determine the execution sequence of the search processor(s) and the
 * possible chaining of processor(s)
 */
public class SearchContext {
    private static final Logger LOG      = LoggerFactory.getLogger(SearchContext.class);

    private final AtlasTypeRegistry       typeRegistry;
    private final AtlasGraph              graph;
    private final Set<AtlasEntityType>    entityTypes;
    private final Set<String>             indexedKeys;
    private final Set<String>             entityAttributes;
    private final SearchParameters        searchParameters;
    private final Set<AtlasClassificationType> classificationTypes;
    private final Set<String>                  classificationNames;
    private final Set<String>             typeAndSubTypes;
    private final Set<String>             classificationTypeAndSubTypes;
    private final String                  typeAndSubTypesQryStr;
    private final String                  classificationTypeAndSubTypesQryStr;
    private boolean                       terminateSearch = false;
    private SearchProcessor               searchProcessor;
    private Integer                       marker;

    public final static AtlasClassificationType MATCH_ALL_WILDCARD_CLASSIFICATION = new AtlasClassificationType(new AtlasClassificationDef(WILDCARD_CLASSIFICATIONS));
    public final static AtlasClassificationType MATCH_ALL_CLASSIFIED              = new AtlasClassificationType(new AtlasClassificationDef(ALL_CLASSIFICATIONS));
    public final static AtlasClassificationType MATCH_ALL_NOT_CLASSIFIED          = new AtlasClassificationType(new AtlasClassificationDef(NO_CLASSIFICATIONS));
    public final static AtlasClassificationType MATCH_ALL_CLASSIFICATION_TYPES    = AtlasClassificationType.getClassificationRoot();
    public final static AtlasEntityType         MATCH_ALL_ENTITY_TYPES            = AtlasEntityType.getEntityRoot();
    public final static String                  TYPENAME_DELIMITER                = ",";


    public SearchContext(SearchParameters searchParameters, AtlasTypeRegistry typeRegistry, AtlasGraph graph, Set<String> indexedKeys) throws AtlasBaseException {
        this.searchParameters   = searchParameters;
        this.typeRegistry       = typeRegistry;
        this.graph              = graph;
        this.indexedKeys        = indexedKeys;
        this.entityAttributes   = new HashSet<>();
        this.entityTypes        = getEntityTypes(searchParameters.getTypeName());
        this.classificationNames = getClassificationNames(searchParameters.getClassification());
        this.classificationTypes = getClassificationTypes(this.classificationNames);

        AtlasVertex glossaryTermVertex = getGlossaryTermVertex(searchParameters.getTermName());

        // Validate if the term exists
        if (StringUtils.isNotEmpty(searchParameters.getTermName()) && glossaryTermVertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.UNKNOWN_GLOSSARY_TERM, searchParameters.getTermName());
        }

        // Invalid attributes or unsupported attribute in a type, will raise an exception with 400 error code
        if (CollectionUtils.isNotEmpty(entityTypes)) {
            for (AtlasEntityType entityType : entityTypes) {
                validateAttributes(entityType, searchParameters.getEntityFilters());

                validateAttributes(entityType, searchParameters.getSortBy());
            }
        }

        //Wildcard tag with filter will raise an exception with 400 error code
        if (CollectionUtils.isNotEmpty(classificationNames) && hasAttributeFilter(searchParameters.getTagFilters())) {
            for (String classificationName : classificationNames){
                //in case of       '*'  , filters are allowed, but
                //in case of regex 'PI*', filters are not allowed ( if present in any of the requested tag)
                if (classificationName.contains(WILDCARD_CLASSIFICATIONS) && !classificationName.equals(WILDCARD_CLASSIFICATIONS)) {
                    throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "TagFilters specified with wildcard tag name");
                }
            }
        }

        // Invalid attributes will raise an exception with 400 error code
        if (CollectionUtils.isNotEmpty(classificationTypes)) {
            for (AtlasClassificationType classificationType : classificationTypes) {
                validateAttributes(classificationType, searchParameters.getTagFilters());
            }
        }

        if (StringUtils.isNotEmpty(searchParameters.getMarker())) {
            marker = MarkerUtil.decodeMarker(searchParameters);
        }

        //remove other types if builtin type is present
        filterStructTypes();

        //gather all classifications and its corresponding subtypes
        Set<String> classificationTypeAndSubTypes  = new HashSet<>();
        String classificationTypeAndSubTypesQryStr = null;

        if (CollectionUtils.isNotEmpty(classificationTypes) && classificationTypes.iterator().next() != MATCH_ALL_NOT_CLASSIFIED ) {
            for (AtlasClassificationType classificationType : classificationTypes) {

                if (classificationType == MATCH_ALL_CLASSIFICATION_TYPES) {
                    classificationTypeAndSubTypes       = Collections.emptySet();
                    classificationTypeAndSubTypesQryStr = ALL_TYPE_QUERY;
                    break;
                } else {
                    Set<String> allTypes = searchParameters.getIncludeSubClassifications() ? classificationType.getTypeAndAllSubTypes() : Collections.singleton(classificationType.getTypeName());
                    classificationTypeAndSubTypes.addAll(allTypes);                }
            }

            if (CollectionUtils.isNotEmpty(classificationTypeAndSubTypes)) {
                classificationTypeAndSubTypesQryStr = AtlasAttribute.escapeIndexQueryValue(classificationTypeAndSubTypes, true);
            }
        } else {
            classificationTypeAndSubTypes       = Collections.emptySet();
            classificationTypeAndSubTypesQryStr = "";
        }
        this.classificationTypeAndSubTypes       = classificationTypeAndSubTypes;
        this.classificationTypeAndSubTypesQryStr = classificationTypeAndSubTypesQryStr;

        //gather all types and its corresponding subtypes
        Set<String> typeAndSubTypes  = new HashSet<>();
        String typeAndSubTypesQryStr = null;

        if (CollectionUtils.isNotEmpty(entityTypes)) {
            for (AtlasEntityType entityType : entityTypes) {

                if (entityType.equals(MATCH_ALL_ENTITY_TYPES)) {
                    typeAndSubTypes       = Collections.emptySet();
                    typeAndSubTypesQryStr = ALL_TYPE_QUERY;
                    break;
                } else {
                    Set<String> allTypes  = searchParameters.getIncludeSubTypes() ? entityType.getTypeAndAllSubTypes() : Collections.singleton(entityType.getTypeName());
                    typeAndSubTypes.addAll(allTypes);
                }
            }

            if (CollectionUtils.isNotEmpty(typeAndSubTypes)) {
                typeAndSubTypesQryStr = AtlasAttribute.escapeIndexQueryValue(typeAndSubTypes, true);
            }
        } else {
            typeAndSubTypes       = Collections.emptySet();
            typeAndSubTypesQryStr = "";
        }
        this.typeAndSubTypes       = typeAndSubTypes;
        this.typeAndSubTypesQryStr = typeAndSubTypesQryStr;

        if (glossaryTermVertex != null) {
            addProcessor(new TermSearchProcessor(this, getAssignedEntities(glossaryTermVertex)));
        }

        if (needFullTextProcessor()) {
            if (AtlasRepositoryConfiguration.isFreeTextSearchEnabled()) {
                LOG.debug("Using Free Text index based search.");

                addProcessor(new FreeTextSearchProcessor(this));
            } else {
                LOG.debug("Using Full Text index based search.");

                addProcessor(new FullTextSearchProcessor(this));
            }
        }

        if (needClassificationProcessor()) {
            addProcessor(new ClassificationSearchProcessor(this));
        }

        if (needEntityProcessor()) {
            addProcessor(new EntitySearchProcessor(this));
        }
    }

    public SearchParameters getSearchParameters() { return searchParameters; }

    public AtlasTypeRegistry getTypeRegistry() { return typeRegistry; }

    public AtlasGraph getGraph() { return graph; }

    public Set<String> getIndexedKeys() { return indexedKeys; }

    public Set<String> getEntityAttributes() { return entityAttributes; }

    public Set<AtlasClassificationType> getClassificationTypes() { return classificationTypes; }

    public Set<String> getEntityTypeNames() { return typeAndSubTypes; }

    public Set<String> getClassificationTypeNames() { return classificationTypeAndSubTypes; }

    public String getEntityTypesQryStr() { return typeAndSubTypesQryStr; }

    public String getClassificationTypesQryStr() { return classificationTypeAndSubTypesQryStr; }

    public Set<AtlasEntityType> getEntityTypes() { return entityTypes; }

    public SearchProcessor getSearchProcessor() { return searchProcessor; }

    public Set<String> getClassificationNames() {return classificationNames;}

    public Integer getMarker() { return marker; }

    public boolean includeEntityType(String entityType) {
        return typeAndSubTypes.isEmpty() || typeAndSubTypes.contains(entityType);
    }

    public boolean includeClassificationTypes(Collection<String> traitNames) {
        final boolean ret;

        if (classificationTypes.iterator().next() == MATCH_ALL_NOT_CLASSIFIED) {
            ret = CollectionUtils.isEmpty(traitNames);
        } else if (classificationTypes.iterator().next() == MATCH_ALL_CLASSIFICATION_TYPES) {
            ret = CollectionUtils.isNotEmpty(traitNames);
        } else {
            ret = CollectionUtils.containsAny(classificationTypeAndSubTypes, traitNames);
        }

        return ret;
    }

    public boolean terminateSearch() { return terminateSearch; }

    public void terminateSearch(boolean terminateSearch) { this.terminateSearch = terminateSearch; }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("searchParameters=");

        if (searchParameters != null) {
            searchParameters.toString(sb);
        }

        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    boolean needFullTextProcessor() {
        return StringUtils.isNotEmpty(searchParameters.getQuery());
    }

    boolean needClassificationProcessor() {
        return (CollectionUtils.isNotEmpty(classificationTypes) && (CollectionUtils.isEmpty(entityTypes) || hasAttributeFilter(searchParameters.getTagFilters()))) || isWildCardSearch() ;
    }

    boolean isWildCardSearch () {
        if (CollectionUtils.isNotEmpty(classificationNames)) {
            return classificationNames.stream().anyMatch(classification -> classification.contains(WILDCARD_CLASSIFICATIONS));
        }
        return false;
    }

    boolean needEntityProcessor() {
        return CollectionUtils.isNotEmpty(entityTypes);
    }

    private void validateAttributes(final AtlasStructType structType, final FilterCriteria filterCriteria) throws AtlasBaseException {
        if (filterCriteria != null) {
            FilterCriteria.Condition condition = filterCriteria.getCondition();

            if (condition != null && CollectionUtils.isNotEmpty(filterCriteria.getCriterion())) {
                for (FilterCriteria criteria : filterCriteria.getCriterion()) {
                    validateAttributes(structType, criteria);
                }
            } else {
                String attributeName = filterCriteria.getAttributeName();
                validateAttributes(structType, attributeName);
            }
        }
    }

    private void validateAttributes(final AtlasStructType structType, final String... attributeNames) throws AtlasBaseException {
        for (String attributeName : attributeNames) {
            if (StringUtils.isNotEmpty(attributeName) && (structType == null || structType.getAttributeType(attributeName) == null)) {
                if (structType == null) {
                    throw new AtlasBaseException(AtlasErrorCode.UNKNOWN_TYPENAME, "NULL");
                }

                String name = structType.getTypeName();
                if (name.equals(MATCH_ALL_ENTITY_TYPES.getTypeName())) {
                    name = ALL_ENTITY_TYPES;
                } else if (name.equals(MATCH_ALL_CLASSIFICATION_TYPES.getTypeName())) {
                    name = ALL_CLASSIFICATION_TYPES;
                }
                throw new AtlasBaseException(AtlasErrorCode.UNKNOWN_ATTRIBUTE, attributeName, name);
            }
        }
    }

    public boolean hasAttributeFilter(FilterCriteria filterCriteria) {
        return filterCriteria != null &&
               (CollectionUtils.isNotEmpty(filterCriteria.getCriterion()) || StringUtils.isNotEmpty(filterCriteria.getAttributeName()));
    }

    private void addProcessor(SearchProcessor processor) {
        if (searchProcessor == null) {
            searchProcessor = processor;
        } else {
            searchProcessor.addProcessor(processor);
        }
    }

    private AtlasClassificationType getClassificationType(String classificationName) {
        AtlasClassificationType ret;

        if (StringUtils.equals(classificationName, MATCH_ALL_WILDCARD_CLASSIFICATION.getTypeName())) {
            ret = MATCH_ALL_WILDCARD_CLASSIFICATION;
        } else if (StringUtils.equals(classificationName, MATCH_ALL_CLASSIFIED.getTypeName())) {
            ret = MATCH_ALL_CLASSIFIED;
        } else if (StringUtils.equals(classificationName, MATCH_ALL_NOT_CLASSIFIED.getTypeName())) {
            ret = MATCH_ALL_NOT_CLASSIFIED;
        } else if (StringUtils.equals(classificationName, ALL_CLASSIFICATION_TYPES)){
            ret = MATCH_ALL_CLASSIFICATION_TYPES;
        } else {
            ret = typeRegistry.getClassificationTypeByName(classificationName);
        }

        return ret;
    }

    private Set<AtlasClassificationType> getClassificationTypes(Set<String> classificationNames) {
        if (CollectionUtils.isNotEmpty(classificationNames)) {
            return classificationNames.stream().map(n ->
                    getClassificationType(n)).filter(Objects::nonNull).collect(Collectors.toSet());
        }

        return null;
    }

    private Set<String> getClassificationNames(String classification) throws AtlasBaseException {
        Set<String> classificationNames = new HashSet<>();

        if (StringUtils.isNotEmpty(classification)) {
            String[] types    = classification.split(TYPENAME_DELIMITER);
            Set<String> names = new HashSet<>(Arrays.asList(types));

            names.forEach(name -> {
                AtlasClassificationType type = getClassificationType(name);
                if (type != null || name.contains(WILDCARD_CLASSIFICATIONS)) {
                    classificationNames.add(name);
                }
            });

            // Validate if the classification exists
            if (CollectionUtils.isEmpty(classificationNames)) {
                throw new AtlasBaseException(AtlasErrorCode.UNKNOWN_CLASSIFICATION, classification);

            } else if (classificationNames.size() != names.size()) {
                names.removeAll(classificationNames);

                LOG.info("Could not search for {} , invalid classifications", String.join(TYPENAME_DELIMITER, names));
            }
        }

        return classificationNames;
    }

    private AtlasEntityType getEntityType(String entityName) {
        return StringUtils.equals(entityName, ALL_ENTITY_TYPES) ? MATCH_ALL_ENTITY_TYPES :
                                                                  typeRegistry.getEntityTypeByName(entityName);
    }

    private Set<AtlasEntityType> getEntityTypes(String typeName) throws AtlasBaseException {

        Set<AtlasEntityType> entityTypes = null;
        //split multiple typeNames by comma
        if (StringUtils.isNotEmpty(typeName)) {

            String[] types        = typeName.split(TYPENAME_DELIMITER);
            Set<String> typeNames = new HashSet<>(Arrays.asList(types));
            entityTypes           = typeNames.stream().map(n ->
                                    getEntityType(n)).filter(Objects::nonNull).collect(Collectors.toSet());

            // Validate if the type name is incorrect
            if (CollectionUtils.isEmpty(entityTypes)) {
                throw new AtlasBaseException(AtlasErrorCode.UNKNOWN_TYPENAME,typeName);

            } else if (entityTypes.size() != typeNames.size()) {
                Set<String> validEntityTypes = new HashSet<>();
                for (AtlasEntityType entityType : entityTypes) {
                    String name = entityType.getTypeName();
                    if (name.equals(MATCH_ALL_ENTITY_TYPES.getTypeName())) {
                        validEntityTypes.add(ALL_ENTITY_TYPES);
                        continue;
                    }
                    validEntityTypes.add(entityType.getTypeName());
                }

                typeNames.removeAll(validEntityTypes);

                LOG.info("Could not search for {} , invalid typeNames", String.join(TYPENAME_DELIMITER, typeNames));
            }
        }

        return entityTypes;
    }

    private void filterStructTypes(){
        //if typeName contains ALL_ENTITY_TYPES, remove others as OR condition will not effect any other
        if (CollectionUtils.isNotEmpty(entityTypes) && entityTypes.contains(MATCH_ALL_ENTITY_TYPES)) {
            entityTypes.clear();
            entityTypes.add(MATCH_ALL_ENTITY_TYPES);
        }

        //No Builtin Classification can be together
        if (CollectionUtils.isNotEmpty(classificationTypes)) {
            if (classificationTypes.contains(MATCH_ALL_NOT_CLASSIFIED)) {
                classificationTypes.clear();
                classificationTypes.add(MATCH_ALL_NOT_CLASSIFIED);

                classificationNames.clear();
                classificationNames.add(MATCH_ALL_NOT_CLASSIFIED.getTypeName());
            } else if (classificationTypes.contains(MATCH_ALL_WILDCARD_CLASSIFICATION) || classificationTypes.contains(MATCH_ALL_CLASSIFICATION_TYPES) || classificationTypes.contains(MATCH_ALL_CLASSIFIED)) {
                classificationTypes.clear();
                classificationTypes.add(MATCH_ALL_CLASSIFICATION_TYPES);

                classificationNames.clear();
                classificationNames.add(ALL_CLASSIFICATION_TYPES);
            }
        }
    }


    private AtlasVertex getGlossaryTermVertex(String termName) {
        AtlasVertex ret = null;

        if (StringUtils.isNotEmpty(termName)) {
            AtlasEntityType termType = getTermEntityType();
            AtlasAttribute  attrName = termType.getAttribute(TermSearchProcessor.ATLAS_GLOSSARY_TERM_ATTR_QNAME);
            AtlasGraphQuery query    = graph.query().has(Constants.ENTITY_TYPE_PROPERTY_KEY, termType.getTypeName())
                                                    .has(attrName.getVertexPropertyName(), termName)
                                                    .has(Constants.STATE_PROPERTY_KEY, AtlasEntity.Status.ACTIVE.name());

            Iterator<AtlasVertex> results = query.vertices().iterator();

            ret = results.hasNext() ? results.next() : null;
        }

        return ret;
    }

    private List<AtlasVertex> getAssignedEntities(AtlasVertex glossaryTerm) {
        List<AtlasVertex>   ret      = new ArrayList<>();
        AtlasEntityType     termType = getTermEntityType();
        AtlasAttribute      attr     = termType.getRelationshipAttribute(TermSearchProcessor.ATLAS_GLOSSARY_TERM_ATTR_ASSIGNED_ENTITIES, EntityGraphRetriever.TERM_RELATION_NAME);
        Iterator<AtlasEdge> edges    = GraphHelper.getEdgesForLabel(glossaryTerm, attr.getRelationshipEdgeLabel(), attr.getRelationshipEdgeDirection());

        boolean excludeDeletedEntities = searchParameters.getExcludeDeletedEntities();
        if (edges != null) {
            while (edges.hasNext()) {
                AtlasEdge edge = edges.next();

                AtlasVertex inVertex = edge.getInVertex();
                if (excludeDeletedEntities && AtlasGraphUtilsV2.getState(inVertex) == AtlasEntity.Status.DELETED) {
                    continue;
                }
                ret.add(inVertex);
            }
        }

        return ret;
    }

    private AtlasEntityType getTermEntityType() {
        return typeRegistry.getEntityTypeByName(TermSearchProcessor.ATLAS_GLOSSARY_TERM_ENTITY_TYPE);
    }

    public static class MarkerUtil {
        private final static int IDX_HASH_CODE = 0;
        private final static int IDX_OFFSET    = 1;

        private final static String MARKER_DELIMITER = ":";

        @VisibleForTesting
                final static String MARKER_START     = "*";

        @VisibleForTesting
                final static int    MARKER_END       = -1;

        public static String getNextEncMarker(SearchParameters searchParameters, Integer nextOffset) {
            if (nextOffset == null) {
                return null;
            }

            if (nextOffset == MARKER_END) {
                return String.valueOf(nextOffset);
            }

            String value = searchParameters.hashCode() + MARKER_DELIMITER + nextOffset;
            return Base64.getEncoder().encodeToString(value.getBytes());
        }

        public static Integer decodeMarker(SearchParameters searchParameters) throws AtlasBaseException {
            if (searchParameters == null || searchParameters.getOffset() > 0) {
                throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "Marker can be used only if offset=0.");
            }

            String encodedMarker = searchParameters.getMarker();
            if (StringUtils.equals(encodedMarker, MARKER_START)) {
                return 0;
            }

            try {
                byte[] inputMarkerBytes = Base64.getDecoder().decode(encodedMarker);
                String inputMarker      = new String(inputMarkerBytes);
                if (StringUtils.isEmpty(inputMarker) || !inputMarker.contains(MARKER_DELIMITER)) {
                    throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "Invalid marker found! Marker does not contain delimiter: " + MARKER_DELIMITER);
                }

                String[] str = inputMarker.split(MARKER_DELIMITER);
                if (str == null || str.length != 2) {
                    throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "Invalid marker found! Decoding using delimiter did not yield correct result!");
                }

                int hashCode        = Integer.parseInt(str[IDX_HASH_CODE]);
                int currentHashCode = searchParameters.hashCode();
                if (hashCode == currentHashCode && Integer.parseInt(str[IDX_OFFSET]) >= 0) {
                    return Integer.parseInt(str[IDX_OFFSET]);
                }

                throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "Invalid Marker! Parsing resulted in error.");
            } catch (Exception e) {
                throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "Invalid marker!");
            }
        }
    }
}
