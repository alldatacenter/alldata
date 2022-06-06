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

public class AtlasGremlin2QueryProvider extends AtlasGremlinQueryProvider {
    @Override
    public String getQuery(final AtlasGremlinQuery gremlinQuery) {
        switch (gremlinQuery) {
            case ENTITY_ACTIVE_METRIC:
                return "g.V().has('__typeName', T.in, [%s]).has('__state', 'ACTIVE').groupCount{it.getProperty('__typeName')}.cap.toList()";
            case ENTITY_DELETED_METRIC:
                return "g.V().has('__typeName', T.in, [%s]).has('__state', 'DELETED').groupCount{it.getProperty('__typeName')}.cap.toList()";

                case EXPORT_BY_GUID_FULL:
                return "g.V('__guid', startGuid).bothE().bothV().has('__guid').transform{[__guid:it.__guid,isProcess:(it.__superTypeNames != null) ? it.__superTypeNames.contains('Process') : false ]}.dedup().toList()";
            case EXPORT_BY_GUID_CONNECTED_IN_EDGE:
                return "g.V('__guid', startGuid).inE().outV().has('__guid').transform{[__guid:it.__guid,isProcess:(it.__superTypeNames != null) ? it.__superTypeNames.contains('Process') : false ]}.dedup().toList()";
            case EXPORT_BY_GUID_CONNECTED_OUT_EDGE:
                return "g.V('__guid', startGuid).outE().inV().has('__guid').transform{[__guid:it.__guid,isProcess:(it.__superTypeNames != null) ? it.__superTypeNames.contains('Process') : false ]}.dedup().toList()";
            case EXPORT_TYPE_ALL_FOR_TYPE:
                return "g.V().has('__typeName',T.in,typeName).has('__guid').__guid.toList()";
            case EXPORT_TYPE_STARTS_WITH:
                return "g.V().has('__typeName',typeName).filter({it.getProperty(attrName).startsWith(attrValue)}).has('__guid').__guid.toList()";
            case EXPORT_TYPE_ENDS_WITH:
                return "g.V().has('__typeName',typeName).filter({it.getProperty(attrName).endsWith(attrValue)}).has('__guid').__guid.toList()";
            case EXPORT_TYPE_CONTAINS:
                return "g.V().has('__typeName',typeName).filter({it.getProperty(attrName).contains(attrValue)}).has('__guid').__guid.toList()";
            case EXPORT_TYPE_MATCHES:
                return "g.V().has('__typeName',typeName).filter({it.getProperty(attrName).matches(attrValue)}).has('__guid').__guid.toList()";
            case EXPORT_TYPE_DEFAULT:
                return "g.V().has('__typeName',typeName).has(attrName, attrValue).has('__guid').__guid.toList()";
            case FULL_LINEAGE_DATASET:
                return "g.V('__guid', '%s').as('src').in('%s').out('%s')." +
                        "loop('src', {((it.path.contains(it.object)) ? false : true)}, " +
                        "{((it.object.'__superTypeNames') ? " +
                        "(it.object.'__superTypeNames'.contains('DataSet')) : false)})." +
                        "path().toList()";
            case PARTIAL_LINEAGE_DATASET:
                return "g.V('__guid', '%s').as('src').in('%s').out('%s')." +
                        "loop('src', {it.loops <= %s}, {((it.object.'__superTypeNames') ? " +
                        "(it.object.'__superTypeNames'.contains('DataSet')) : false)})." +
                        "path().toList()";

            case BASIC_SEARCH_TYPE_FILTER:
                return ".has('__typeName', within(typeNames))";
            case BASIC_SEARCH_CLASSIFICATION_FILTER:
                return ".or(has('__traitNames', within(traitNames)), has('__propagatedTraitNames', within(traitNames)))";
            case BASIC_SEARCH_STATE_FILTER:
                return ".has('__state', state)";
            case TO_RANGE_LIST:
                return " [startIdx..<endIdx].toList()";
            case GUID_PREFIX_FILTER:
                return ".filter{it.'__guid'.matches(guid)}";
            case COMPARE_LT:
                return ".has('%s', lt(%s))";
            case COMPARE_LTE:
                return ".has('%s', lte(%s))";
            case COMPARE_GT:
                return ".has('%s', gt(%s))";
            case COMPARE_GTE:
                return ".has('%s', gte(%s))";
            case COMPARE_EQ:
                return ".has('%s', eq(%s))";
            case COMPARE_NEQ:
                return ".has('%s', neq(%s))";
            case COMPARE_MATCHES:
                return ".filter({it.getProperty('%s').matches(%s)})";
            case COMPARE_STARTS_WITH:
                return ".filter({it.getProperty('%s').startsWith(%s)})";
            case COMPARE_ENDS_WITH:
                return ".filter({it.getProperty('%s').endsWith(%s)})";
            case COMPARE_CONTAINS:
                return ".filter({it.getProperty('%s').contains(%s)})";
            case COMPARE_IS_NULL:
                return ".hasNot('%s')";
            case COMPARE_NOT_NULL:
                return ".has('%s')";
            case RELATIONSHIP_SEARCH:
                return "g.V('__guid', guid).both(relation).has('__state', within(states))";
            case RELATIONSHIP_SEARCH_ASCENDING_SORT:
                return ".order{it.a.getProperty(sortAttributeName) <=> it.b.getProperty(sortAttributeName)}";
            case RELATIONSHIP_SEARCH_DESCENDING_SORT:
                return ".order{it.b.getProperty(sortAttributeName) <=> it.a.getProperty(sortAttributeName)}";
            case GREMLIN_SEARCH_RETURNS_VERTEX_ID:
                return "g.V.range(0,0).collect()";
            case GREMLIN_SEARCH_RETURNS_EDGE_ID:
                return "g.E.range(0,0).collect()";
        }
        // Should never reach this point
        return null;
    }

}
