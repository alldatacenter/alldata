/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.authorization.solr.authorizer;

import com.google.common.base.Preconditions;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CoveringQuery;
import org.apache.lucene.search.LongValuesSource;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SyntaxError;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A custom {@linkplain QParserPlugin} which supports subset queries on a given Solr index.
 * This filter accepts the name of the field whose value should be used for subset matching
 * and the set against which subset queries are to be run ( as a comma separated string values).
 */
public class SubsetQueryPlugin extends QParserPlugin {
    public static final String SETVAL_PARAM_NAME = "set_value";
    public static final String SETVAL_FIELD_NAME = "set_field";
    public static final String COUNT_FIELD_NAME = "count_field";
    public static final String MISSING_VAL_ALLOWED = "allow_missing_val";
    public static final String WILDCARD_CHAR = "wildcard_token";

    @SuppressWarnings("rawtypes")
    @Override
    public void init(NamedList arg0) {
    }

    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        return new QParser(qstr, localParams, params, req) {

            @Override
            public Query parse() throws SyntaxError {
                String fieldName = Preconditions.checkNotNull(localParams.get(SETVAL_FIELD_NAME));
                String countFieldName = Preconditions.checkNotNull(localParams.get(COUNT_FIELD_NAME));
                boolean allowMissingValues = Boolean.parseBoolean(Preconditions.checkNotNull(localParams.get(MISSING_VAL_ALLOWED)));
                String wildcardToken = localParams.get(WILDCARD_CHAR);

                LongValuesSource minimumNumberMatch = LongValuesSource.fromIntField(countFieldName);
                Collection<Query> queries = new ArrayList<>();

                String fieldVals = Preconditions.checkNotNull(localParams.get(SETVAL_PARAM_NAME));
                for (String v : fieldVals.split(",")) {
                    queries.add(new TermQuery(new Term(fieldName, v)));
                }
                if (wildcardToken != null && !wildcardToken.equals("")) {
                    queries.add(new TermQuery(new Term(fieldName, wildcardToken)));
                }
                if (allowMissingValues) {
                    // To construct this query we need to do a little trick tho construct a test for an empty field as follows:
                    // (*:* AND -fieldName:*) ==> parses as: (+*:* -fieldName:*)
                    // It is a feature of Lucene that pure negative queries are not allowed (although Solr allows them as a top level construct)
                    // therefore we need to AND with *:*
                    // We can then pass this BooleanQuery to the CoveringQuery as one of its allowed matches.
                    BooleanQuery.Builder builder = new BooleanQuery.Builder();
                    builder.add(new BooleanClause(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD));
                    builder.add(new BooleanClause(new WildcardQuery(new Term(fieldName, "*")), BooleanClause.Occur.MUST_NOT));

                    queries.add(builder.build());
                }
                return new CoveringQuery(queries, minimumNumberMatch);
            }
        };
    }

}
