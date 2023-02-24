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
package org.apache.atlas.discovery;

import org.apache.atlas.AtlasException;
import org.apache.atlas.model.discovery.AtlasSuggestionsResult;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphIndexClient;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;


public class SuggestionsProviderImpl implements SuggestionsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(SuggestionsProviderImpl.class);

    private final AtlasGraph        graph;
    private final AtlasTypeRegistry typeRegistry;

    public SuggestionsProviderImpl(AtlasGraph graph, AtlasTypeRegistry typeRegistry) {
        this.graph        = graph;
        this.typeRegistry = typeRegistry;
    }

    @Override
    public AtlasSuggestionsResult getSuggestions(String prefixString, String fieldName) {
        AtlasSuggestionsResult result = new AtlasSuggestionsResult(prefixString, fieldName);

        try {
            AtlasGraphIndexClient graphIndexClient = graph.getGraphIndexClient();
            String                indexFieldName   = (fieldName == null) ? null : typeRegistry.getIndexFieldName(fieldName);

            result.setSuggestions(graphIndexClient.getSuggestions(prefixString, indexFieldName));
        } catch (AtlasException e) {
            LOG.error("Error encountered in performing quick suggestions. Will return no suggestions.", e);

            result.setSuggestions(Collections.EMPTY_LIST);
        }

        return result;
    }
}
