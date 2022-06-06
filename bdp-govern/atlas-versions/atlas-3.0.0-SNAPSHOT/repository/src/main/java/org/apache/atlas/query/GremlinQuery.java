/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.query;

import org.apache.atlas.repository.graphdb.AtlasGraphTraversal;

import java.util.Objects;

public class GremlinQuery {
    private final String                 queryStr;
    private final AtlasDSL.QueryMetadata queryMetadata;
    private final GremlinClauseList      clauses;
    private final SelectClauseComposer   selectComposer;

    private AtlasGraphTraversal traversal;

    public GremlinQuery(String gremlinQuery, AtlasDSL.QueryMetadata queryMetadata, GremlinClauseList clauses, SelectClauseComposer selectComposer) {
        this.queryStr       = gremlinQuery;
        this.queryMetadata  = queryMetadata;
        this.clauses        = clauses;
        this.selectComposer = selectComposer;
    }

    public String queryStr() {
        return queryStr;
    }

    public GremlinClauseList getClauses() {
        return clauses;
    }

    public SelectClauseComposer getSelectComposer() {
        return selectComposer;
    }

    public boolean hasValidSelectClause() {
        return Objects.nonNull(selectComposer) && !selectComposer.getIsSelectNoop();
    }

    public AtlasDSL.QueryMetadata getQueryMetadata() {
        return queryMetadata;
    }

    public void setResult(AtlasGraphTraversal traversal) {
        this.traversal = traversal;
    }

    public AtlasGraphTraversal getTraversal() {
        return traversal;
    }

    public boolean hasSelectList() {
        return queryMetadata.hasSelect();
    }
}
