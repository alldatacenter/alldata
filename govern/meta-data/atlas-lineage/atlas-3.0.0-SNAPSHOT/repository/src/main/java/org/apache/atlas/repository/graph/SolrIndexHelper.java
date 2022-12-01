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
package org.apache.atlas.repository.graph;

import org.apache.atlas.AtlasException;
import org.apache.atlas.listener.ChangedTypeDefs;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphIndexClient;
import org.apache.atlas.type.AtlasBusinessMetadataType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.util.AtlasRepositoryConfiguration;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.DEFAULT_SEARCHWEIGHT;
import static org.apache.atlas.repository.Constants.CLASSIFICATION_TEXT_KEY;
import static org.apache.atlas.repository.Constants.CUSTOM_ATTRIBUTES_PROPERTY_KEY;
import static org.apache.atlas.repository.Constants.LABELS_PROPERTY_KEY;
import static org.apache.atlas.repository.Constants.TYPE_NAME_PROPERTY_KEY;

/**
 This is a component that will go through all entity type definitions and create free text index
 request handler with SOLR. This is a no op class in non-solr index based deployments.
 This component needs to be initialized after type definitions are completely fixed with the needed patches (Ordder 3 initialization).
 */
public class SolrIndexHelper implements IndexChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(SolrIndexHelper.class);

    public static final int DEFAULT_SEARCHWEIGHT_FOR_STRINGS = 3;
    public static final int SEARCHWEIGHT_FOR_CLASSIFICATIONS = 10;
    public static final int SEARCHWEIGHT_FOR_LABELS          = 10;
    public static final int SEARCHWEIGHT_FOR_CUSTOM_ATTRS    = 3;
    public static final int SEARCHWEIGHT_FOR_TYPENAME        = 1;

    private static final int MIN_SEARCH_WEIGHT_FOR_SUGGESTIONS = 8;

    private final AtlasTypeRegistry typeRegistry;


    public SolrIndexHelper(AtlasTypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }
    public boolean initializationCompleted = false;

    @Override
    public void onChange(ChangedTypeDefs changedTypeDefs) {
        if (!AtlasRepositoryConfiguration.isFreeTextSearchEnabled()) {
            return;
        }

        if (changedTypeDefs == null || !(changedTypeDefs.hasEntityDef() || changedTypeDefs.hasBusinessMetadataDef())) {
            LOG.info("SolrIndexHelper.onChange(): no change in entity/business-metadata types. No updates needed.");

            return;
        }

        if(initializationCompleted) {
            try {
                AtlasGraph            graph                          = AtlasGraphProvider.getGraphInstance();
                AtlasGraphIndexClient graphIndexClient               = graph.getGraphIndexClient();
                Map<String, Integer>  indexFieldName2SearchWeightMap = geIndexFieldNamesWithSearchWeights();

                graphIndexClient.applySearchWeight(Constants.VERTEX_INDEX, indexFieldName2SearchWeightMap);
                graphIndexClient.applySuggestionFields(Constants.VERTEX_INDEX, getIndexFieldNamesForSuggestions(indexFieldName2SearchWeightMap));
            } catch (AtlasException e) {
                LOG.error("Error encountered in handling type system change notification.", e);
                throw new RuntimeException("Error encountered in handling type system change notification.", e);
            }
        }
    }

    @Override
    public void onInitStart() {
        LOG.info("SolrIndexHelper Initialization started.");
        initializationCompleted = false;
    }

    @Override
    public void onInitCompletion(ChangedTypeDefs changedTypeDefs) {
        LOG.info("SolrIndexHelper Initialization completed.");
        initializationCompleted = true;
        onChange(changedTypeDefs);
    }

    private List<String> getIndexFieldNamesForSuggestions(Map<String, Integer> indexFieldName2SearchWeightMap) {
        List<String> ret = new ArrayList<>();

        for(Map.Entry<String, Integer> entry: indexFieldName2SearchWeightMap.entrySet()) {
            if(entry.getValue().intValue() >= MIN_SEARCH_WEIGHT_FOR_SUGGESTIONS) {
                String indexFieldName = entry.getKey();

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Adding indexFieldName {} for suggestions.", indexFieldName);
                }

                ret.add(indexFieldName);
            }
        }

        return ret;
    }

    private Map<String, Integer> geIndexFieldNamesWithSearchWeights() {
        Map<String, Integer> ret = new HashMap<>();

        //the following properties are specially added manually.
        //as, they don't come in the entity definitions as attributes.
        ret.put(typeRegistry.getIndexFieldName(CLASSIFICATION_TEXT_KEY), SEARCHWEIGHT_FOR_CLASSIFICATIONS);
        ret.put(typeRegistry.getIndexFieldName(LABELS_PROPERTY_KEY), SEARCHWEIGHT_FOR_LABELS);
        ret.put(typeRegistry.getIndexFieldName(CUSTOM_ATTRIBUTES_PROPERTY_KEY), SEARCHWEIGHT_FOR_CUSTOM_ATTRS);
        ret.put(typeRegistry.getIndexFieldName(TYPE_NAME_PROPERTY_KEY), SEARCHWEIGHT_FOR_TYPENAME);

        for (AtlasEntityType entityType : typeRegistry.getAllEntityTypes()) {
            if (entityType.isInternalType()) {
                continue;
            }

            processType(ret, entityType);
        }

        for (AtlasBusinessMetadataType businessMetadataType : typeRegistry.getAllBusinessMetadataTypes()) {
            processType(ret, businessMetadataType);
        }

        return ret;
    }

    private void processType(Map<String, Integer> indexFieldNameWithSearchWeights, AtlasStructType structType) {
        List<AtlasAttributeDef> attributes = structType.getStructDef().getAttributeDefs();

        if (CollectionUtils.isNotEmpty(attributes)) {
            for (AtlasAttributeDef attribute : attributes) {
                processAttribute(indexFieldNameWithSearchWeights, structType.getAttribute(attribute.getName()));
            }
        } else {
            LOG.debug("No attributes are defined for type {}", structType.getTypeName());
        }
    }

    private void processAttribute(Map<String, Integer> indexFieldNameWithSearchWeights, AtlasAttribute attribute) {
        if (attribute != null && GraphBackedSearchIndexer.isStringAttribute(attribute) && StringUtils.isNotEmpty(attribute.getIndexFieldName())) {
            int searchWeight = attribute.getSearchWeight();

            if (searchWeight == DEFAULT_SEARCHWEIGHT) {
                //We will use default search weight of 3 for string attributes.
                //this will make the string data searchable like in FullTextIndex Searcher using Free Text searcher.
                searchWeight = DEFAULT_SEARCHWEIGHT_FOR_STRINGS;
            } else if (!GraphBackedSearchIndexer.isValidSearchWeight(searchWeight)) { //validate the value provided in the model.
                LOG.warn("Invalid search weight {} for attribute {}. Will use default {}",
                         searchWeight, attribute.getQualifiedName(), DEFAULT_SEARCHWEIGHT_FOR_STRINGS);

                searchWeight = DEFAULT_SEARCHWEIGHT_FOR_STRINGS;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Applying search weight {} for attribute={}: indexFieldName={}", searchWeight, attribute.getQualifiedName(), attribute.getIndexFieldName());
            }

            indexFieldNameWithSearchWeights.put(attribute.getIndexFieldName(), searchWeight);
        }
    }
}