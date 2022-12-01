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

package org.apache.atlas.repository.impexp;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.util.AtlasGremlinQueryProvider;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.atlas.model.impexp.AtlasExportRequest.MATCH_TYPE_CONTAINS;
import static org.apache.atlas.model.impexp.AtlasExportRequest.MATCH_TYPE_ENDS_WITH;
import static org.apache.atlas.model.impexp.AtlasExportRequest.MATCH_TYPE_FOR_TYPE;
import static org.apache.atlas.model.impexp.AtlasExportRequest.MATCH_TYPE_MATCHES;
import static org.apache.atlas.model.impexp.AtlasExportRequest.MATCH_TYPE_STARTS_WITH;

public class StartEntityFetchByExportRequest {
    private static final Logger LOG = LoggerFactory.getLogger(StartEntityFetchByExportRequest.class);

    static final String DEFAULT_MATCH = "*";
    static final String BINDING_PARAMETER_TYPENAME = "typeName";
    static final String BINDING_PARAMETER_ATTR_NAME = "attrName";
    static final String BINDING_PARAMTER_ATTR_VALUE = "attrValue";

    private AtlasGraph           atlasGraph;
    private AtlasTypeRegistry    typeRegistry;


    private Map<String, String>  matchTypeQuery;

    public StartEntityFetchByExportRequest(AtlasGraph atlasGraph, AtlasTypeRegistry typeRegistry, AtlasGremlinQueryProvider gremlinQueryProvider) {
        this.typeRegistry = typeRegistry;
        this.atlasGraph = atlasGraph;
        initMatchTypeQueryMap(gremlinQueryProvider);
    }

    private void initMatchTypeQueryMap(AtlasGremlinQueryProvider gremlinQueryProvider) {
        matchTypeQuery = new HashMap<String, String>() {{
            put(MATCH_TYPE_STARTS_WITH, gremlinQueryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.EXPORT_TYPE_STARTS_WITH));
            put(MATCH_TYPE_ENDS_WITH, gremlinQueryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.EXPORT_TYPE_ENDS_WITH));
            put(MATCH_TYPE_CONTAINS, gremlinQueryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.EXPORT_TYPE_CONTAINS));
            put(MATCH_TYPE_MATCHES, gremlinQueryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.EXPORT_TYPE_MATCHES));
            put(MATCH_TYPE_FOR_TYPE, gremlinQueryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.EXPORT_TYPE_ALL_FOR_TYPE));
            put(DEFAULT_MATCH, gremlinQueryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.EXPORT_TYPE_DEFAULT));
        }};
    }

    public List<AtlasObjectId> get(AtlasExportRequest exportRequest) {
        List<AtlasObjectId> list = new ArrayList<>();
        for(AtlasObjectId objectId : exportRequest.getItemsToExport()) {
            List<String> guids = get(exportRequest, objectId);
            if (guids.isEmpty()) {
                continue;
            }

            objectId.setGuid(guids.get(0));
            list.add(objectId);
        }

        return list;
    }

    public List<String> get(AtlasExportRequest exportRequest, AtlasObjectId item) {
        List<String> ret = new ArrayList<>();
        String matchType = exportRequest.getMatchTypeOptionValue();

        try {
            if (StringUtils.isNotEmpty(item.getGuid())) {
                ret.add(item.getGuid());
                return ret;
            }

            if (StringUtils.equalsIgnoreCase(matchType, MATCH_TYPE_FOR_TYPE) && StringUtils.isNotEmpty(item.getTypeName())) {
                ret = getEntitiesForMatchTypeType(item, matchType);
                return ret;
            }

            if (StringUtils.isNotEmpty(item.getTypeName()) && MapUtils.isNotEmpty(item.getUniqueAttributes())) {
                ret = getEntitiesForMatchTypeUsingUniqueAttributes(item, matchType);
                return ret;
            }
        }
        catch (AtlasBaseException ex) {
            LOG.error("Error fetching starting entity for: {}", item, ex);
        } finally {
            LOG.info("export(item={}; matchType={}, fetchType={}): found {} entities: options: {}", item,
                    exportRequest.getMatchTypeOptionValue(), exportRequest.getFetchTypeOptionValue(), ret.size(), AtlasType.toJson(exportRequest));
        }

        return ret;
    }

    private List<String> getEntitiesForMatchTypeUsingUniqueAttributes(AtlasObjectId item, String matchType) throws AtlasBaseException {
        final String          queryTemplate = getQueryTemplateForMatchType(matchType);
        final String          typeName      = item.getTypeName();
        final AtlasEntityType entityType    = typeRegistry.getEntityTypeByName(typeName);

        Set<String> ret = new HashSet<>();
        if (entityType == null) {
            throw new AtlasBaseException(AtlasErrorCode.UNKNOWN_TYPENAME, typeName);
        }

        for (Map.Entry<String, Object> e : item.getUniqueAttributes().entrySet()) {
            String attrName  = e.getKey();
            Object attrValue = e.getValue();

            AtlasStructType.AtlasAttribute attribute = entityType.getAttribute(attrName);
            if (attribute == null || attrValue == null) {
                continue;
            }

            List<String> guids = executeGremlinQuery(queryTemplate,
                                    getBindingsForObjectId(typeName, attribute.getQualifiedName(), e.getValue()));

            if (!CollectionUtils.isNotEmpty(guids)) {
                continue;
            }

            ret.addAll(guids);
        }

        return new ArrayList<>(ret);
    }

    private List<String> getEntitiesForMatchTypeType(AtlasObjectId item, String matchType) {
        return executeGremlinQuery(getQueryTemplateForMatchType(matchType), getBindingsForTypeName(item.getTypeName()));
    }

    @VisibleForTesting
    String getQueryTemplateForMatchType(String matchType) {
        return matchTypeQuery.containsKey(matchType)
                ? matchTypeQuery.get(matchType)
                : matchTypeQuery.get(DEFAULT_MATCH);
    }

    private HashMap<String, Object> getBindingsForTypeName(String typeName) {
        return new HashMap<String, Object>() {{
                put(BINDING_PARAMETER_TYPENAME, new HashSet<String>(Arrays.asList(StringUtils.split(typeName, ","))));
            }};
    }

    private HashMap<String, Object> getBindingsForObjectId(String typeName, String attrName, Object attrValue) {
        return new HashMap<String, Object>() {{
                put(BINDING_PARAMETER_TYPENAME, typeName);
                put(BINDING_PARAMETER_ATTR_NAME, attrName);
                put(BINDING_PARAMTER_ATTR_VALUE, attrValue);
            }};
    }

    @VisibleForTesting
    List<String> executeGremlinQuery(String query, Map<String, Object> bindings) {
        try {
            return (List<String>) atlasGraph.executeGremlinScript(getScriptEngine(), bindings, query, false);
        } catch (ScriptException e) {
            LOG.error("Script execution failed for query: ", query, e);
            return null;
        }
    }

    public ScriptEngine getScriptEngine() {
        try {
            return atlasGraph.getGremlinScriptEngine();
        }
        catch (AtlasBaseException e) {
            LOG.error("Error initializing script engine.", e);
        }

        return null;
    }
}
