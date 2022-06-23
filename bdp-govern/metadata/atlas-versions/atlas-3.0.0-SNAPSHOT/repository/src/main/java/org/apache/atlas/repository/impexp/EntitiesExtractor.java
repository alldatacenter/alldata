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

import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.type.AtlasTypeRegistry;

import java.util.HashMap;
import java.util.Map;

public class EntitiesExtractor {
    static final String PROPERTY_GUID = "__guid";
    private static final String VERTEX_BASED_EXTRACT = "default";
    private static final String INCREMENTAL_EXTRACT = "incremental";
    private static final String RELATION_BASED_EXTRACT = "relationship";

    private Map<String, ExtractStrategy> extractors = new HashMap<>();
    private ExtractStrategy extractor;

    public EntitiesExtractor(AtlasGraph atlasGraph, AtlasTypeRegistry typeRegistry) {
        extractors.put(VERTEX_BASED_EXTRACT, new VertexExtractor(atlasGraph, typeRegistry));
        extractors.put(INCREMENTAL_EXTRACT, new IncrementalExportEntityProvider(atlasGraph));
        extractors.put(RELATION_BASED_EXTRACT, new RelationshipAttributesExtractor(typeRegistry));
    }

    public void get(AtlasEntity entity, ExportService.ExportContext context) {
        if(extractor == null) {
            extractor = extractors.get(VERTEX_BASED_EXTRACT);
        }

        switch (context.fetchType) {
            case CONNECTED:
                extractor.connectedFetch(entity, context);
                break;

            case INCREMENTAL:
                if (context.isHiveDBIncrementalSkipLineage()) {
                    extractors.get(INCREMENTAL_EXTRACT).fullFetch(entity, context);
                    break;
                } else if (context.isHiveTableIncrementalSkipLineage()) {
                    extractors.get(INCREMENTAL_EXTRACT).connectedFetch(entity, context);
                    break;
                }

            case FULL:
            default:
                extractor.fullFetch(entity, context);
        }
    }

    public void setExtractor(AtlasEntityDef atlasEntityDef) {
        extractor = extractUsing(atlasEntityDef);
    }

    public void close() {
        for (ExtractStrategy es : extractors.values()) {
            es.close();
        }
    }

    private ExtractStrategy extractUsing(AtlasEntityDef atlasEntityDef) {
        return (atlasEntityDef == null || atlasEntityDef.getRelationshipAttributeDefs().size() == 0)
                ? extractors.get(VERTEX_BASED_EXTRACT)
                : extractors.get(RELATION_BASED_EXTRACT);
    }
}
