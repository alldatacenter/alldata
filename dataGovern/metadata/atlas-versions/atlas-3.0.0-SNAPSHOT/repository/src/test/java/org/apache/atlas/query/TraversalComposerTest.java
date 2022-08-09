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
package org.apache.atlas.query;

import org.apache.atlas.TestModules;
import org.apache.atlas.query.antlr4.AtlasDSLParser;
import org.apache.atlas.query.executors.GremlinClauseToTraversalTranslator;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphTraversal;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.testng.Assert.assertEquals;

@Guice(modules = TestModules.TestOnlyModule.class)
public class TraversalComposerTest extends BaseDSLComposer {
    @Inject
    public AtlasGraph graph;

    @Test
    public void queries() {
        verify("hive_db",
                "[JanusGraphStep([],[__typeName.eq(hive_db)]), DedupGlobalStep, RangeGlobalStep(0,25)]");

        verify("hive_db where owner = 'hdfs'",
                "[JanusGraphStep([],[__typeName.eq(hive_db), hive_db.owner.eq(hdfs)]), DedupGlobalStep, RangeGlobalStep(0,25)]");

        verify("DB where owner = ['hdfs', 'anon']",
                "[JanusGraphStep([],[__typeName.eq(DB), DB.owner.within([hdfs, anon])]), DedupGlobalStep, RangeGlobalStep(0,25)]");

        verify("hive_db where hive_db.name='Reporting' and hive_db.createTime < '2017-12-12T02:35:58.440Z'",
                "[JanusGraphStep([],[__typeName.eq(hive_db), hive_db.name.eq(Reporting), hive_db.createTime.lt(1513046158440)]), DedupGlobalStep, RangeGlobalStep(0,25)]");

        // note that projections are not handled in the conversion
        verify("DB as d select d.name, d.owner",
                "[JanusGraphStep([],[__typeName.eq(DB)]), DedupGlobalStep@[d], RangeGlobalStep(0,25)]");

        verify("Table groupby(owner) select name, owner, clusterName orderby name",
                "[JanusGraphStep([],[__typeName.eq(Table), Table.owner.neq]), GroupStep(value(Table.owner),[FoldStep]), DedupGlobalStep, RangeGlobalStep(0,25)]");
    }

    private void verify(String dsl, String expected) {
        AtlasDSLParser.QueryContext queryContext = getParsedQuery(dsl);
        String                      actual       = getTraversalAsStr(queryContext);

        assertEquals(actual, expected, dsl);
    }

    private String getTraversalAsStr(AtlasDSLParser.QueryContext queryContext) {
        org.apache.atlas.query.Lookup lookup               = new TestLookup(registry);
        GremlinQueryComposer.Context  context              = new GremlinQueryComposer.Context(lookup);
        AtlasDSL.QueryMetadata        queryMetadata        = new AtlasDSL.QueryMetadata(queryContext);
        GremlinQueryComposer          gremlinQueryComposer = new GremlinQueryComposer(lookup, context, queryMetadata);
        DSLVisitor                    qv                   = new DSLVisitor(gremlinQueryComposer);

        qv.visit(queryContext);

        gremlinQueryComposer.get();

        AtlasGraphTraversal traversal = GremlinClauseToTraversalTranslator.run(graph, gremlinQueryComposer.clauses());

        return traversal.toString();
    }
}
