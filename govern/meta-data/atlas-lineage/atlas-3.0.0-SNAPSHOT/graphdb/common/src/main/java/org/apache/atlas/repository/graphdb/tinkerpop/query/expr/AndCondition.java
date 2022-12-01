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
package org.apache.atlas.repository.graphdb.tinkerpop.query.expr;

import org.apache.atlas.repository.graphdb.tinkerpop.query.NativeTinkerpopQueryFactory;
import org.apache.atlas.repository.graphdb.tinkerpop.query.NativeTinkerpopGraphQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an AndCondition in a graph query.  Only vertices that
 * satisfy the conditions in all of the query predicates will be returned
 *
 * Represents a query with predicates that are 'AND'ed together. These can be
 * executed natively using Titan's GraphQuery mechanism.
 */
public class AndCondition {

    private List<QueryPredicate> children = new ArrayList<>();

    public AndCondition() {

    }

    /**
     * Adds a query predicate that must be met by vertices.
     * @param predicate
     */
    public void andWith(QueryPredicate predicate) {
        children.add(predicate);
    }

    /**
     * Adds multiple predicates that much be met by the vertices.
     *
     * @param predicates
     */
    public void andWith(List<QueryPredicate> predicates) {
        children.addAll(predicates);
    }

    /**
     * Makes a copy of this AndExpr.
     *
     * @return
     */
    public AndCondition copy() {
        AndCondition builder = new AndCondition();
        builder.children.addAll(children);
        return builder;
    }

    /**
     * Gets the query predicates.
     *
     * @return
     */
    public List<QueryPredicate> getTerms() {
        return children;
    }

    /**
     * Creates a NativeTinkerpopGraphQuery that can be used to evaluate this condition.
     *
     * @param factory
     * @return
     */
    public <V, E> NativeTinkerpopGraphQuery<V, E> create(NativeTinkerpopQueryFactory<V, E> factory) {
        NativeTinkerpopGraphQuery<V, E> query = factory.createNativeTinkerpopQuery();
        for (QueryPredicate predicate : children) {
            predicate.addTo(query);
        }
        return query;
    }

    @Override
    public String toString() {
        return "AndExpr [predicates=" + children + "]";
    }
}
