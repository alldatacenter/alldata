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

package org.apache.atlas.repository.graphdb.janus.migration;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.shaded.jackson.databind.JsonNode;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class JsonNodeParsersTest extends BaseUtils {

    @Test
    public void parseVertex() {
        JsonNode nd = getCol1();
        final int COL1_ORIGINAL_ID = 98336;

        Object nodeId = getId(nd);
        TinkerGraph tg = TinkerGraph.open();

        JsonNodeParsers.ParseElement pe = new JsonNodeParsers.ParseVertex();
        pe.setContext(graphSONUtility);
        pe.parse(tg, new MappedElementCache(),  nd);

        Vertex v = tg.vertices().next();
        Vertex vUsingPe = (Vertex) pe.get(tg, nodeId);
        Vertex vUsingOriginalId = (Vertex) pe.getByOriginalId(tg, COL1_ORIGINAL_ID);
        Vertex vUsingOriginalId2 = (Vertex) pe.getByOriginalId(tg, nd);

        updateParseElement(tg, pe, vUsingPe);

        assertNotNull(v);
        assertNotNull(vUsingPe);
        assertNotNull(vUsingOriginalId);

        assertEquals(v.id(), vUsingPe.id());
        assertEquals(nodeId, pe.getId(nd));
        assertFalse(pe.isTypeNode(nd));
        assertEquals(pe.getType(nd), "\"hive_column\"");
        assertEquals(vUsingOriginalId.id(), v.id());
        assertEquals(vUsingOriginalId2.id(), v.id());

        assertProperties(vUsingPe);
    }

    @Test
    public void parseEdge() {
        JsonNode nd = getEdge();
        final String EDGE_ORIGINAL_ID = "8k5i-35tc-acyd-1eko";
        Object nodeId = getId(nd);

        TinkerGraph tg = TinkerGraph.open();
        MappedElementCache cache = new MappedElementCache();
        JsonNodeParsers.ParseElement peVertex = new JsonNodeParsers.ParseVertex();
        peVertex.setContext(graphSONUtility);

        peVertex.parse(tg, cache, getDBV());
        peVertex.parse(tg, cache, getTableV());

        JsonNodeParsers.ParseElement pe = new JsonNodeParsers.ParseEdge();
        pe.setContext(graphSONUtility);
        pe.parse(tg, cache, getEdge());

        updateParseElement(tg, pe, nodeId);

        Edge e = tg.edges().next();
        Edge eUsingPe = (Edge) pe.get(tg, nodeId);
        Edge eUsingOriginalId = (Edge) pe.getByOriginalId(tg, EDGE_ORIGINAL_ID);
        Edge eUsingOriginalId2 = (Edge) pe.getByOriginalId(tg, nd);

        assertNotNull(e);
        assertNotNull(eUsingPe);
        assertNotNull(eUsingOriginalId);

        assertEquals(e.id(), eUsingPe.id());
        assertEquals(nodeId, pe.getId(nd));
        assertFalse(pe.isTypeNode(nd));
        assertEquals(eUsingOriginalId.id(), e.id());
        assertEquals(eUsingOriginalId2.id(), e.id());

        assertProperties(e);
    }

    private void updateParseElement(TinkerGraph tg, JsonNodeParsers.ParseElement pe, Object nodeId) {
        Map<String, Object> props = new HashMap<>();
        props.put("k1", "v1");
        props.put("k2", "v2");
        pe.update(tg, nodeId, props);
    }

    private void assertProperties(Element v) {
        assertNotNull(v);
        assertTrue(v.property("k1").isPresent());
        assertTrue(v.property("k2").isPresent());

        assertEquals(v.property("k1").value(), "v1");
    }
}
