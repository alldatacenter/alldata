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
package org.apache.atlas.query.executors;

import org.apache.atlas.query.GremlinClause;
import org.apache.atlas.query.GremlinClauseList;
import org.apache.atlas.query.GremlinQueryComposer;
import org.apache.atlas.query.IdentifierHelper;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphTraversal;
import org.apache.atlas.type.Constants;
import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class GremlinClauseToTraversalTranslator {
    private static final Logger LOG = LoggerFactory.getLogger(GremlinClauseToTraversalTranslator.class);

    public static AtlasGraphTraversal run(AtlasGraph graph, GremlinClauseList clauseList) {
        return new ClauseTranslator(graph).process(clauseList);
    }

    private static class ClauseTranslator {
        private static final String ATTR_PROPERTY_NAME               = "__name";
        private static final String EDGE_NAME_CLASSIFIED_AS          = "classifiedAs";
        private static final String EDGE_NAME_TRAIT_NAMES            = "__traitNames";
        private static final String EDGE_NAME_PROPAGATED_TRAIT_NAMES = "__propagatedTraitNames";
        private static final String[] STR_TOKEN_SEARCH               = new String[]{"[", "]", "'", "\""};
        private static final String[] STR_TOKEN_REPLACE              = new String[]{"", "", "", ""};

        private final AtlasGraph graph;

        public ClauseTranslator(AtlasGraph graph) {
            this.graph = graph;
        }

        public AtlasGraphTraversal process(GremlinClauseList clauseList) {
            Stack<List<AtlasGraphTraversal>> subTraversals = new Stack<>();
            AtlasGraphTraversal              ret           = process(null, subTraversals, clauseList);

            if (!subTraversals.isEmpty()) {
                String errorMessage = "Sub-traversals found not to be empty! " + subTraversals.toString();

                LOG.warn(errorMessage);

                throw new RuntimeException(errorMessage);
            }

            return ret;
        }

        private AtlasGraphTraversal process(AtlasGraphTraversal traversal,
                                            Stack<List<AtlasGraphTraversal>> collected,
                                            GremlinClauseList clauseList) {
            int size = clauseList.getList().size();

            for (int index = 0; index < size; index++) {
                if (clauseList.hasSubClause(index)) {
                    List<GremlinClauseList> subClauses = clauseList.getSubClauses(index);

                    collected.push(new ArrayList<>());

                    for (GremlinClauseList sc : subClauses) {
                        process(traversal, collected, sc);
                    }
                }

                traversal = traverse(traversal, collected, clauseList.get(index));
            }

            return traversal;
        }

        private AtlasGraphTraversal traverse(AtlasGraphTraversal traversal,
                                             Stack<List<AtlasGraphTraversal>> trLists,
                                             GremlinQueryComposer.GremlinClauseValue clauseValue) {
            GremlinClause clause = clauseValue.getClause();
            String[]      values = clauseValue.getValues();

            switch (clause) {
                case G:
                    break;

                case V:
                    traversal = graph.V();
                    break;

                case AS:
                    traversal.as(values[0]);
                    break;

                case AND: {
                    if (trLists != null && !trLists.peek().isEmpty()) {
                        List<AtlasGraphTraversal> subTraversals = trLists.pop();

                        traversal.and(subTraversals.toArray(new Traversal[0]));
                    } else {
                        throw new RuntimeException("subTraversals not expected to be NULL: " + clause.toString());
                    }
                }
                break;

                case OR: {
                    if (trLists != null && !trLists.peek().isEmpty()) {
                        List<AtlasGraphTraversal> subTraversals = trLists.pop();

                        traversal.or(subTraversals.toArray(new Traversal[0]));
                    } else {
                        throw new RuntimeException("subTraversals not expected to be NULL: " + clause.toString());
                    }
                }
                break;

                case HAS_PROPERTY:
                    traversal.has(values[0]);
                    break;

                case HAS_NOT_PROPERTY:
                    traversal.hasNot(values[0]);
                    break;

                case HAS_OPERATOR:
                    P predicate = getPredicate(values[1], values[2], clauseValue.getRawValue());

                    traversal.has(values[0], predicate);
                    break;

                case HAS_NOT_OPERATOR:
                    traversal.or(
                            traversal.startAnonymousTraversal().has(values[0], P.neq(values[1])),
                            traversal.startAnonymousTraversal().hasNot(values[0]));
                    break;

                case HAS_TYPE:
                    traversal.has(Constants.TYPE_NAME_PROPERTY_KEY, values[0]);
                    break;

                case HAS_WITHIN:
                    traversal.has(values[0], values[1]);
                    break;

                case IN:
                    traversal.in(removeRedundantQuotes(values[0]));
                    break;

                case OUT:
                    traversal.out(removeRedundantQuotes(values[0]));
                    break;

                case ANY_TRAIT:
                    traversal.or(
                            traversal.startAnonymousTraversal().has(EDGE_NAME_TRAIT_NAMES),
                            traversal.startAnonymousTraversal().has(EDGE_NAME_PROPAGATED_TRAIT_NAMES));
                    break;

                case TRAIT:
                    traversal.outE(EDGE_NAME_CLASSIFIED_AS).has(ATTR_PROPERTY_NAME, P.within(values[0])).outV();
                    break;

                case NO_TRAIT:
                    traversal.and(
                            traversal.startAnonymousTraversal().hasNot(EDGE_NAME_TRAIT_NAMES),
                            traversal.startAnonymousTraversal().hasNot(EDGE_NAME_PROPAGATED_TRAIT_NAMES));
                    break;

                case DEDUP:
                    traversal.dedup();
                    break;

                case LIMIT:
                    traversal.limit(Scope.global, Long.valueOf(values[0]));
                    break;

                case TO_LIST:
                    traversal.getAtlasVertexList();
                    break;

                case NESTED_START:
                    traversal = traversal.startAnonymousTraversal();

                    trLists.peek().add(traversal);
                    break;

                case HAS_TYPE_WITHIN:
                    String[] subTypes = StringUtils.split(removeRedundantQuotes(values[0]), ',');

                    traversal.has("__typeName", P.within(subTypes));
                    break;

                case GROUP_BY:
                    traversal.has(values[0]).group().by(values[0]);
                    break;

                case ORDER_BY:
                    traversal.has(values[0]).order().by(values[0]);
                    break;

                case ORDER_BY_DESC:
                    traversal.has(values[0]).order().by(values[0], Order.desc);
                    break;

                case STRING_CONTAINS:
                    traversal.textRegEx(values[0], removeRedundantQuotes(values[1]));
                    break;

                case TEXT_CONTAINS:
                    traversal.textContainsRegEx(values[0], removeRedundantQuotes(values[1]));
                    break;

                case RANGE:
                    traversal.dedup();

                    long low  = Long.parseLong(values[1]);
                    long high = low + Long.parseLong(values[2]);

                    traversal.range(Scope.global, low, high);
                    break;

                case SELECT_FN:
                case SELECT_NOOP_FN:
                case SELECT_ONLY_AGG_GRP_FN:
                case INLINE_TRANSFORM_CALL:
                case SELECT_MULTI_ATTR_GRP_FN:
                    break;

                case TERM:
                    String term = String.format("AtlasGlossaryTerm.%s", values[0]);
                    traversal.where(
                             traversal.startAnonymousTraversal()
                            .in(org.apache.atlas.repository.Constants.TERM_ASSIGNMENT_LABEL)
                            .has(term, P.eq(values[1]))
                            );
                    break;

                default:
                    LOG.warn("Clause not translated: {}. Can potentially lead to incorrect results.", clause);
                    break;
            }
            return traversal;
        }

        private P getPredicate(String operator, String strRhs,Object rhs) {
            switch (operator.toUpperCase()) {
                case "LT":
                    return P.lt(rhs);

                case "GT":
                    return P.gt(rhs);

                case "LTE":
                    return P.lte(rhs);

                case "GTE":
                    return P.gte(rhs);

                case "EQ":
                    return P.eq(rhs);

                case "NEQ":
                    return P.neq(rhs);

                case "WITHIN":
                    String[] strs = csvToArray(strRhs);
                    return P.within(strs);

                default:
                    LOG.warn("Operator: {} not translated.", operator);
                    return null;
            }
        }

        private String[] csvToArray(String strRhs) {
            String csvRow = StringUtils.replaceEach(strRhs, STR_TOKEN_SEARCH, STR_TOKEN_REPLACE);
            return csvRow.split(",");
        }

        private String removeRedundantQuotes(String value) {
            return IdentifierHelper.removeQuotes(value);
        }
    }
}
