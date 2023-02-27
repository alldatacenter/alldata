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
package org.apache.atlas.util;

public class AtlasGremlin3QueryProvider extends AtlasGremlin2QueryProvider {
    @Override
    public String getQuery(final AtlasGremlinQuery gremlinQuery) {
        // In case any overrides are necessary, a specific switch case can be added here to
        // return Gremlin 3 specific query otherwise delegate to super.getQuery
        switch (gremlinQuery) {
            case ENTITY_ACTIVE_METRIC:
                return "g.V().has('__typeName', within(%s)).has('__state', 'ACTIVE').groupCount().by('__typeName').toList()";
            case ENTITY_DELETED_METRIC:
                return "g.V().has('__typeName', within(%s)).has('__state', 'DELETED').groupCount().by('__typeName').toList()";
            case EXPORT_TYPE_STARTS_WITH:
                return "g.V().has('__typeName',typeName).filter({it.get().value(attrName).startsWith(attrValue)}).has('__guid').values('__guid').toList()";
            case EXPORT_TYPE_ENDS_WITH:
                return "g.V().has('__typeName',typeName).filter({it.get().value(attrName).endsWith(attrValue)}).has('__guid').values('__guid').toList()";
            case EXPORT_TYPE_CONTAINS:
                return "g.V().has('__typeName',typeName).filter({it.get().value(attrName).contains(attrValue)}).has('__guid').values('__guid').toList()";
            case EXPORT_TYPE_MATCHES:
                return "g.V().has('__typeName',typeName).filter({it.get().value(attrName).matches(attrValue)}).has('__guid').values('__guid').toList()";
            case EXPORT_TYPE_DEFAULT:
                return "g.V().has('__typeName',typeName).has(attrName, attrValue).has('__guid').values('__guid').toList()";
            case EXPORT_BY_GUID_FULL:
                return "g.V().has('__guid', startGuid).bothE().bothV().has('__guid').project('__guid', 'isProcess').by('__guid').by(map {it.get().values('__superTypeNames').toSet().contains('Process')}).dedup().toList()";
            case EXPORT_BY_GUID_CONNECTED_IN_EDGE:
                return "g.V().has('__guid', startGuid).inE().outV().has('__guid').project('__guid', 'isProcess').by('__guid').by(map {it.get().values('__superTypeNames').toSet().contains('Process')}).dedup().toList()";
            case EXPORT_BY_GUID_CONNECTED_OUT_EDGE:
                return "g.V().has('__guid', startGuid).outE().inV().has('__guid').project('__guid', 'isProcess').by('__guid').by(map {it.get().values('__superTypeNames').toSet().contains('Process')}).dedup().toList()";
            case EXPORT_TYPE_ALL_FOR_TYPE:
                return "g.V().has('__typeName', within(typeName)).has('__guid').values('__guid').toList()";
            case FULL_LINEAGE_DATASET:
                return "g.V().has('__guid', guid).repeat(__.inE(incomingEdgeLabel).as('e1').outV().outE(outgoingEdgeLabel).as('e2').inV()).emit().select('e1', 'e2').toList()";
            case PARTIAL_LINEAGE_DATASET:
                return "g.V().has('__guid', guid).repeat(__.inE(incomingEdgeLabel).as('e1').outV().outE(outgoingEdgeLabel).as('e2').inV()).times(dataSetDepth).emit().select('e1', 'e2').toList()";
            case FULL_LINEAGE_PROCESS:
                return "g.V().has('__guid', guid).outE(outgoingEdgeLabel).store('e').inV().repeat(__.inE(incomingEdgeLabel).store('e').outV().outE(outgoingEdgeLabel).store('e').inV()).cap('e').unfold().toList()";
            case PARTIAL_LINEAGE_PROCESS:
                return "g.V().has('__guid', guid).outE(outgoingEdgeLabel).store('e').inV().until(loops().is(eq(processDepth))).repeat(__.inE(incomingEdgeLabel).store('e').outV().outE(outgoingEdgeLabel).store('e').inV()).cap('e').unfold().toList()";
            case TO_RANGE_LIST:
                return ".range(startIdx, endIdx).toList()";
            case RELATIONSHIP_SEARCH:
                return "g.V().has('__guid', guid).bothE(relation).has('__state', within(states)).otherV().has('__state', within(states))";
            case RELATIONSHIP_SEARCH_ASCENDING_SORT:
                return ".order().by(sortAttributeName, asc)";
            case RELATIONSHIP_SEARCH_DESCENDING_SORT:
                return ".order().by(sortAttributeName, desc)";
            case GREMLIN_SEARCH_RETURNS_VERTEX_ID:
                return "g.V().range(0,1).toList()";
            case GREMLIN_SEARCH_RETURNS_EDGE_ID:
                return "g.E().range(0,1).toList()";
        }
        return super.getQuery(gremlinQuery);
    }
}
