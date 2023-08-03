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

package org.apache.ranger.plugin.model.validation;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.apache.ranger.plugin.model.validation.RangerServiceDefHelper.DirectedGraph;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TestDirectedGraph {

	@Test
	public void test() {
		/*
		 *  create a graph as follows:
		 *           GG
		 *            \
		 *             v
		 *   AA -> BB -> CC
 		 *         ^     |
 		 *         |     v
		 *         EE <- DD -> FF
		 *         |
		 *         v
		 *         HH
		 *
		 * This directed graph has
		 * - 1 cycle [BB CC DD EE],
		 * - 2 sources [AA GG],
		 * - 2 sinks [HH FF]
		 * - 4 hierarchies { [AA BB CC DD FF], [AA BB CC DD EE HH], [GG CC DD FF], [GG CC DD EE HH] }
		 */
		DirectedGraph graph = new DirectedGraph();
		// first add all of the arcs - from top row to bottom row and from left to right
		//1st row
		graph.addArc("GG", "CC");
		// 2nd row
		graph.addArc("AA", "BB"); graph.addArc("BB", "CC");
		// 3rd row
		graph.addArc("EE", "BB"); graph.addArc("DD", "EE"); graph.addArc("CC", "DD"); graph.addArc("DD", "FF");
		// 4th row
		graph.addArc("EE", "HH");
		
		// now assert the structure
		assertEquals(Sets.newHashSet("FF", "HH"), graph.getSinks());
		assertEquals(Sets.newHashSet("AA", "GG"), graph.getSources());
		// check paths
		assertEquals(Lists.newArrayList("AA", "BB", "CC", "DD", "FF"), graph.getAPath("AA", "FF", new HashSet<String>()));
		assertEquals(Lists.newArrayList(), graph.getAPath("GG", "AA", new HashSet<String>())); // BB is not reachable from GG
		assertEquals(Lists.newArrayList("GG", "CC", "DD", "EE", "BB"), graph.getAPath("GG", "BB", new HashSet<String>()));
		assertEquals(Lists.newArrayList("GG", "CC", "DD", "FF"), graph.getAPath("GG", "FF", new HashSet<String>()));
		assertEquals(Lists.newArrayList("EE", "BB", "CC", "DD", "FF"), graph.getAPath("EE", "FF", new HashSet<String>()));
	}

}
