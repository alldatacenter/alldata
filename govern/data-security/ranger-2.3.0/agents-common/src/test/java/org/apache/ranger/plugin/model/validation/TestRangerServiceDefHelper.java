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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.validation.RangerServiceDefHelper.Delegate;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TestRangerServiceDefHelper {

	@Before
	public void before() {
		_serviceDef = mock(RangerServiceDef.class);
		when(_serviceDef.getName()).thenReturn("a-service-def");
		// wipe the cache clean
		RangerServiceDefHelper._Cache.clear();
	}
	
	@Test
	public void test_getResourceHierarchies() {
		/*
		 * Create a service-def with following resource graph
		 *
		 *   Database -> UDF
		 *       |
		 *       v
		 *      Table -> Column
		 *         |
		 *         v
		 *        Table-Attribute
		 *
		 *  It contains following hierarchies
		 *  - [ Database UDF]
		 *  - [ Database Table Column ]
		 *  - [ Database Table Table-Attribute ]
		 */
		RangerResourceDef Database = createResourceDef("Database", "");
		RangerResourceDef UDF = createResourceDef("UDF", "Database");
		RangerResourceDef Table = createResourceDef("Table", "Database");
		RangerResourceDef Column = createResourceDef("Column", "Table", true);
		RangerResourceDef Table_Attribute = createResourceDef("Table-Attribute", "Table", true);
		// order of resources in list sould not matter
		List<RangerResourceDef> resourceDefs = Lists.newArrayList(Column, Database, Table, Table_Attribute, UDF);
		// stuff this into a service-def
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		// now assert the behavior
		_helper = new RangerServiceDefHelper(_serviceDef);
		assertTrue(_helper.isResourceGraphValid());
		Set<List<RangerResourceDef>> hierarchies = _helper.getResourceHierarchies(RangerPolicy.POLICY_TYPE_ACCESS);
		// there should be
		List<RangerResourceDef> hierarchy = Lists.newArrayList(Database, UDF);
		assertTrue(hierarchies.contains(hierarchy));
		hierarchy = Lists.newArrayList(Database, Table, Column);
		assertTrue(hierarchies.contains(hierarchy));
		hierarchy = Lists.newArrayList(Database, Table, Table_Attribute);
		assertTrue(hierarchies.contains(hierarchy));
	}
	
	@Test
	public final void test_isResourceGraphValid_detectCycle() {
		/*
		 * Create a service-def with cycles in resource graph
		 *  A --> B --> C
		 *  ^           |
		 *  |           |
		 *  |---- D <---
		 */
		RangerResourceDef A = createResourceDef("A", "D"); // A's parent is D, etc.
		RangerResourceDef B = createResourceDef("B", "C");
		RangerResourceDef C = createResourceDef("C", "D");
		RangerResourceDef D = createResourceDef("D", "A");
		// order of resources in list sould not matter
		List<RangerResourceDef> resourceDefs = Lists.newArrayList(A, B, C, D);
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		_helper = new RangerServiceDefHelper(_serviceDef);
		assertFalse("Graph was valid!", _helper.isResourceGraphValid());
	}

	@Test
	public final void test_isResourceGraphValid_forest() {
		/*
		 * Create a service-def which is a forest
		 *   Database -> Table-space
		 *       |
		 *       v
		 *      Table -> Column
		 *
		 *   Namespace -> package
		 *       |
		 *       v
		 *     function
		 *
		 * Check that helper corrects reports back all of the hierarchies: levels in it and their order.
		 */
		RangerResourceDef database = createResourceDef("database", "");
		RangerResourceDef tableSpace = createResourceDef("table-space", "database", true);
		RangerResourceDef table = createResourceDef("table", "database");
		RangerResourceDef column = createResourceDef("column", "table", true);
		RangerResourceDef namespace = createResourceDef("namespace", "");
		RangerResourceDef function = createResourceDef("function", "namespace", true);
		RangerResourceDef Package = createResourceDef("package", "namespace", true);
		List<RangerResourceDef> resourceDefs = Lists.newArrayList(database, tableSpace, table, column, namespace, function, Package);
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		_helper = new RangerServiceDefHelper(_serviceDef);
		assertTrue(_helper.isResourceGraphValid());
		Set<List<RangerResourceDef>> hierarchies = _helper.getResourceHierarchies(RangerPolicy.POLICY_TYPE_ACCESS);

		Set<List<String>> expectedHierarchies = new HashSet<>();
		expectedHierarchies.add(Lists.newArrayList("database", "table-space"));
		expectedHierarchies.add(Lists.newArrayList("database", "table", "column"));
		expectedHierarchies.add(Lists.newArrayList("namespace", "package"));
		expectedHierarchies.add(Lists.newArrayList("namespace", "function"));

		for (List<RangerResourceDef> aHierarchy : hierarchies) {
			List<String> resourceNames = _helper.getAllResourceNamesOrdered(aHierarchy);
			assertTrue(expectedHierarchies.contains(resourceNames));
			expectedHierarchies.remove(resourceNames);
		}
		assertTrue("Missing hierarchies: " + expectedHierarchies.toString(), expectedHierarchies.isEmpty()); // make sure we got back all hierarchies
	}

	@Test
	public final void test_isResourceGraphValid_forest_singleNodeTrees() {
		/*
		 * Create a service-def which is a forest with a few single node trees
		 *
		 *   Database
		 *
		 *   Server
		 *
		 *   Namespace -> package
		 *       |
		 *       v
		 *     function
		 *
		 * Check that helper corrects reports back all of the hierarchies: levels in it and their order.
		 */
		RangerResourceDef database = createResourceDef("database", "");
		RangerResourceDef server = createResourceDef("server", "");
		RangerResourceDef namespace = createResourceDef("namespace", "");
		RangerResourceDef function = createResourceDef("function", "namespace", true);
		RangerResourceDef Package = createResourceDef("package", "namespace", true);
		List<RangerResourceDef> resourceDefs = Lists.newArrayList(database, server, namespace, function, Package);
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		_helper = new RangerServiceDefHelper(_serviceDef);
		assertTrue(_helper.isResourceGraphValid());
		Set<List<RangerResourceDef>> hierarchies = _helper.getResourceHierarchies(RangerPolicy.POLICY_TYPE_ACCESS);

		Set<List<String>> expectedHierarchies = new HashSet<>();
		expectedHierarchies.add(Lists.newArrayList("database"));
		expectedHierarchies.add(Lists.newArrayList("server"));
		expectedHierarchies.add(Lists.newArrayList("namespace", "package"));
		expectedHierarchies.add(Lists.newArrayList("namespace", "function"));

		for (List<RangerResourceDef> aHierarchy : hierarchies) {
			List<String> resourceNames = _helper.getAllResourceNamesOrdered(aHierarchy);
			assertTrue(expectedHierarchies.contains(resourceNames));
			expectedHierarchies.remove(resourceNames);
		}
		assertTrue("Missing hierarchies: " + expectedHierarchies.toString(), expectedHierarchies.isEmpty()); // make sure we got back all hierarchies
	}
	@Test
	public final void test_cacheBehavior() {
		// wipe the cache clean
		RangerServiceDefHelper._Cache.clear();
		// let's add one entry to the cache
		Delegate delegate = mock(Delegate.class);
		Date aDate = getNow();
		String serviceName = "a-service-def";
		when(delegate.getServiceFreshnessDate()).thenReturn(aDate);
		when(delegate.getServiceName()).thenReturn(serviceName);
		RangerServiceDefHelper._Cache.put(serviceName, delegate);
		
		// create a service def with matching date value
		_serviceDef = mock(RangerServiceDef.class);
		when(_serviceDef.getName()).thenReturn(serviceName);
		when(_serviceDef.getUpdateTime()).thenReturn(aDate);
		
		// since cache has it, we should get back the one that we have added
		_helper = new RangerServiceDefHelper(_serviceDef);
		assertTrue("Didn't get back the same object that was put in cache", delegate == _helper._delegate);
		
		// if we change the date then that should force helper to create a new delegate instance
		/*
		 * NOTE:: current logic would replace the cache instance even if the one in the cache is newer.  This is not likely to happen but it is important to call this out
		 * as in rare cases one may end up creating re creating delegate if threads have stale copies of service def.
		 */
		when(_serviceDef.getUpdateTime()).thenReturn(getLastMonth());
		_helper = new RangerServiceDefHelper(_serviceDef);
		assertTrue("Didn't get a delegate different than what was put in the cache", delegate != _helper._delegate);
		// now that a new instance was added to the cache let's ensure that it got added to the cache
		Delegate newDelegate = _helper._delegate;
		_helper = new RangerServiceDefHelper(_serviceDef);
		assertTrue("Didn't get a delegate different than what was put in the cache", newDelegate == _helper._delegate);
	}

    @Test
    public void test_getResourceHierarchies_with_leaf_specification() {
		/*
		 * Leaf Spec for resources:
		 *      Database: non-leaf
		 *      UDF: Not-specified
		 *      Table: Leaf
		 *      Column: Leaf
		 *      Table-Attribute: Leaf
		 *
		 * Create a service-def with following resource graph
		 *
		 *   Database -> UDF
		 *       |
		 *       v
		 *      Table -> Column
		 *         |
		 *         v
		 *        Table-Attribute
		 *
		 *  It contains following hierarchies
		 *  - [ Database UDF]
		 *  - [ Database Table Column ]
		 *  - [ Database Table ]
		 *  - [ Database Table Table-Attribute ]
		 */
        RangerResourceDef Database = createResourceDef("Database", "", false);
        RangerResourceDef UDF = createResourceDef("UDF", "Database");
        RangerResourceDef Table = createResourceDef("Table", "Database", true);
        RangerResourceDef Column = createResourceDef("Column", "Table", true);
        RangerResourceDef Table_Attribute = createResourceDef("Table-Attribute", "Table", true);
        // order of resources in list should not matter
        List<RangerResourceDef> resourceDefs = Lists.newArrayList(Column, Database, Table, Table_Attribute, UDF);
        // stuff this into a service-def
        when(_serviceDef.getResources()).thenReturn(resourceDefs);
        // now assert the behavior
        _helper = new RangerServiceDefHelper(_serviceDef);
        assertTrue(_helper.isResourceGraphValid());
        Set<List<RangerResourceDef>> hierarchies = _helper.getResourceHierarchies(RangerPolicy.POLICY_TYPE_ACCESS);
        // there should be
        List<RangerResourceDef> hierarchy = Lists.newArrayList(Database, UDF);
        assertTrue(hierarchies.contains(hierarchy));
        hierarchy = Lists.newArrayList(Database, Table, Column);
        assertTrue(hierarchies.contains(hierarchy));
        hierarchy = Lists.newArrayList(Database, Table, Table_Attribute);
        assertTrue(hierarchies.contains(hierarchy));
        hierarchy = Lists.newArrayList(Database, Table);
        assertTrue(hierarchies.contains(hierarchy));
        hierarchy = Lists.newArrayList(Database);
        assertFalse(hierarchies.contains(hierarchy));
    }

    @Test
    public void test_invalid_resourceHierarchies_with_leaf_specification() {
		/*
		 * Leaf Spec for resources:
		 *      Database: non-leaf
		 *      UDF: Not-specified
		 *      Table: Leaf
		 *      Column: non-Leaf
		 *      Table-Attribute: Leaf
		 *
		 * Create a service-def with following resource graph
		 *
		 *   Database -> UDF
		 *       |
		 *       v
		 *      Table -> Column
		 *         |
		 *         v
		 *        Table-Attribute
		 *
		 *  It should fail as the hierarchy is invalid ("Error in path: sink node:[Column] is not leaf node")
         *
		 */
        RangerResourceDef Database = createResourceDef("Database", "", false);
        RangerResourceDef UDF = createResourceDef("UDF", "Database");
        RangerResourceDef Table = createResourceDef("Table", "Database", true);
        RangerResourceDef Column = createResourceDef("Column", "Table", false);
        RangerResourceDef Table_Attribute = createResourceDef("Table-Attribute", "Table", true);
        // order of resources in list should not matter
        List<RangerResourceDef> resourceDefs = Lists.newArrayList(Column, Database, Table, Table_Attribute, UDF);
        // stuff this into a service-def
        when(_serviceDef.getResources()).thenReturn(resourceDefs);
        // now assert the behavior
        _helper = new RangerServiceDefHelper(_serviceDef);
        assertFalse(_helper.isResourceGraphValid());
    }

	RangerResourceDef createResourceDef(String name, String parent) {
	    return createResourceDef(name, parent, null);
	}

    RangerResourceDef createResourceDef(String name, String parent, Boolean isValidLeaf) {
        RangerResourceDef resourceDef = mock(RangerResourceDef.class);
        when(resourceDef.getName()).thenReturn(name);
        when(resourceDef.getParent()).thenReturn(parent);
        when(resourceDef.getIsValidLeaf()).thenReturn(isValidLeaf);
        return resourceDef;
    }

	Date getLastMonth() {
		Calendar cal = GregorianCalendar.getInstance();
		cal.add( Calendar.MONTH, 1);
		Date lastMonth = cal.getTime();
		return lastMonth;
	}
	
	Date getNow() {
		Calendar cal = GregorianCalendar.getInstance();
		Date now = cal.getTime();
		return now;
	}
	
	private RangerServiceDef _serviceDef;
	private RangerServiceDefHelper _helper;
}
