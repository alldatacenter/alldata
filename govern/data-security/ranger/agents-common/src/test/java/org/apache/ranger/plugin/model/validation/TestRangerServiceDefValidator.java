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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumElementDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.plugin.model.validation.RangerValidator.Action;
import org.apache.ranger.plugin.store.ServiceStore;
import org.junit.Before;
import org.junit.Test;

public class TestRangerServiceDefValidator {

	@Before
	public void setUp() throws Exception {
		_store = mock(ServiceStore.class);
		_validator = new RangerServiceDefValidator(_store);
		_failures = new ArrayList<>();
		_serviceDef = mock(RangerServiceDef.class);
	}

	final Action[] cu = new Action[] { Action.CREATE, Action.UPDATE };
	
	final Object[][] accessTypes_good = new Object[][] {
			{ 1L, "read",  null },                                // ok, null implied grants
			{ 2L, "write", new String[] {   } },                  // ok, empty implied grants
			{ 3L, "admin", new String[] { "READ",  "write" } }    // ok, admin access implies read/write, access types are case-insensitive
	};

	final Object[][] enums_good = new Object[][] {
			{ 1L, "authentication-type", new String[] { "simple", "kerberos" } },
			{ 2L, "time-unit", new String[] { "day", "hour", "minute" } },
	};
	
	@Test
	public final void test_isValid_happyPath_create() throws Exception {
		
		// setup access types with implied access and couple of enums
		List<RangerAccessTypeDef> accessTypeDefs = _utils.createAccessTypeDefs(accessTypes_good);
		when(_serviceDef.getAccessTypes()).thenReturn(accessTypeDefs);
		List<RangerEnumDef> enumDefs = _utils.createEnumDefs(enums_good);
		when(_serviceDef.getEnums()).thenReturn(enumDefs);

	}
	
	@Test
	public final void testIsValid_Long_failures() throws Exception {
		Long id = null;
		// passing in wrong action type
		boolean result = _validator.isValid((Long)null, Action.CREATE, _failures);
		assertFalse(result);
		_utils.checkFailureForInternalError(_failures);
		// passing in null id is an error
		_failures.clear(); assertFalse(_validator.isValid((Long)null, Action.DELETE, _failures));
		_utils.checkFailureForMissingValue(_failures, "id");
		// It is ok for a service def with that id to not exist!
		id = 3L;
		when(_store.getServiceDef(id)).thenReturn(null);
		_failures.clear(); assertTrue(_validator.isValid(id, Action.DELETE, _failures));
		assertTrue(_failures.isEmpty());		
		// happypath
		when(_store.getServiceDef(id)).thenReturn(_serviceDef);
		_failures.clear(); assertTrue(_validator.isValid(id, Action.DELETE, _failures));
		assertTrue(_failures.isEmpty());
	}

	@Test
	public final void testIsValid_failures() throws Exception {
		// null service def and bad service def name
		for (Action action : cu) {
			// passing in null service def is an error
			assertFalse(_validator.isValid((RangerServiceDef)null, action, _failures));
			_utils.checkFailureForMissingValue(_failures, "service def");
		}
	}
	
	@Test
	public final void test_isValidServiceDefId_failures() throws Exception {
		
		// id is required for update
		assertFalse(_validator.isValidServiceDefId(null, Action.UPDATE, _failures));
		_utils.checkFailureForMissingValue(_failures, "id");
		
		// update: service should exist for the passed in id
		long id = 7;
		when(_serviceDef.getId()).thenReturn(id);
		when(_store.getServiceDef(id)).thenReturn(null);
		assertFalse(_validator.isValidServiceDefId(id, Action.UPDATE, _failures));
		_utils.checkFailureForSemanticError(_failures, "id");

		when(_store.getServiceDef(id)).thenThrow(new Exception());
		assertFalse(_validator.isValidServiceDefId(id, Action.UPDATE, _failures));
		_utils.checkFailureForSemanticError(_failures, "id");
	}
	
	@Test
	public final void test_isValidServiceDefId_happyPath() throws Exception {
		
		// create: null id is ok
		assertTrue(_validator.isValidServiceDefId(null, Action.CREATE, _failures));
		assertTrue(_failures.isEmpty());
		
		// update: a service with same id exist
		long id = 7;
		when(_serviceDef.getId()).thenReturn(id);
		RangerServiceDef serviceDefFromDb = mock(RangerServiceDef.class);
		when(serviceDefFromDb.getId()).thenReturn(id);
		when(_store.getServiceDef(id)).thenReturn(serviceDefFromDb);
		assertTrue(_validator.isValidServiceDefId(id, Action.UPDATE, _failures));
		assertTrue(_failures.isEmpty());
	}
	
	@Test
	public final void test_isValidName() throws Exception {
		Long id = 7L; // some arbitrary value
		// name can't be null/empty
		for (Action action: cu) {
			for (String name : new String[] { null, "", "  " }) {
				when(_serviceDef.getName()).thenReturn(name);
				_failures.clear(); assertFalse(_validator.isValidServiceDefName(name, id, action, _failures));
				_utils.checkFailureForMissingValue(_failures, "name");
			}
		}
	}
	
	@Test
	public final void test_isValidName_create() throws Exception {
		Long id = null; // id should be irrelevant for name check for create.

		// for create a service shouldn't exist with the name
		String name = "existing-service";
		when(_serviceDef.getName()).thenReturn(name);
		when(_store.getServiceDefByName(name)).thenReturn(null);
		assertTrue(_validator.isValidServiceDefName(name, id, Action.CREATE, _failures));
		assertTrue(_failures.isEmpty());
		
		RangerServiceDef existingServiceDef = mock(RangerServiceDef.class);
		when(_store.getServiceDefByName(name)).thenReturn(existingServiceDef);
		_failures.clear(); assertFalse(_validator.isValidServiceDefName(name, id, Action.CREATE, _failures));
		_utils.checkFailureForSemanticError(_failures, "name");
	}
	
	@Test
	public final void test_isValidName_update() throws Exception {
		
		// update: if service exists with the same name then it can't point to a different service
		long id = 7;
		when(_serviceDef.getId()).thenReturn(id);
		String name = "aServiceDef";
		when(_serviceDef.getName()).thenReturn(name);
		when(_store.getServiceDefByName(name)).thenReturn(null); // no service with the new name (we are updating the name to a unique value)
		assertTrue(_validator.isValidServiceDefName(name, id, Action.UPDATE, _failures));
		assertTrue(_failures.isEmpty());

		RangerServiceDef existingServiceDef = mock(RangerServiceDef.class);
		when(existingServiceDef.getId()).thenReturn(id);
		when(existingServiceDef.getName()).thenReturn(name);
		when(_store.getServiceDefByName(name)).thenReturn(existingServiceDef);
		assertTrue(_validator.isValidServiceDefName(name, id, Action.UPDATE, _failures));
		assertTrue(_failures.isEmpty());
		
		long anotherId = 49;
		when(existingServiceDef.getId()).thenReturn(anotherId);
		assertFalse(_validator.isValidServiceDefName(name, id, Action.UPDATE, _failures));
		_utils.checkFailureForSemanticError(_failures, "id/name");
	}

	final Object[][] accessTypes_bad_unknownType = new Object[][] {
			{ 1L, "read",  null },                                // ok, null implied grants
			{ 1L, "write", new String[] {   }  },                 // empty implied grants-ok, duplicate id
			{ 3L, "admin", new String[] { "ReaD",  "execute" } }  // non-existent access type (execute), read is good (case should not matter)
	};

	final Object[][] accessTypes_bad_selfReference = new Object[][] {
			{ 1L, "read",  null },                              // ok, null implied grants
			{ 2L, "write", new String[] {   } },                // ok, empty implied grants
			{ 3L, "admin", new String[] { "write", "admin" } }  // non-existent access type (execute)
	};

	@Test
	public final void test_isValidAccessTypes_happyPath() {
		long id = 7;
		when(_serviceDef.getId()).thenReturn(id);
		List<RangerAccessTypeDef> input = _utils.createAccessTypeDefs(accessTypes_good);
		assertTrue(_validator.isValidAccessTypes(id, input, _failures, Action.CREATE));
		assertTrue(_failures.isEmpty());
	}
	
	@Test
	public final void test_isValidAccessTypes_failures() {
		long id = 7;
		when(_serviceDef.getId()).thenReturn(id);
		// null or empty access type defs
		List<RangerAccessTypeDef> accessTypeDefs = null;
		_failures.clear(); assertFalse(_validator.isValidAccessTypes(id, accessTypeDefs, _failures, Action.CREATE));
		_utils.checkFailureForMissingValue(_failures, "access types");
		
		accessTypeDefs = new ArrayList<>();
		_failures.clear(); assertFalse(_validator.isValidAccessTypes(id, accessTypeDefs, _failures, Action.CREATE));
		_utils.checkFailureForMissingValue(_failures, "access types");

		// null/empty access types
		accessTypeDefs = _utils.createAccessTypeDefs(new String[] { null, "", "		" });
		_failures.clear(); assertFalse(_validator.isValidAccessTypes(id, accessTypeDefs, _failures, Action.CREATE));
		_utils.checkFailureForMissingValue(_failures, "access type name");
		
		// duplicate access types
		accessTypeDefs = _utils.createAccessTypeDefs(new String[] { "read", "write", "execute", "read" } );
		_failures.clear(); assertFalse(_validator.isValidAccessTypes(id, accessTypeDefs, _failures, Action.CREATE));
		_utils.checkFailureForSemanticError(_failures, "access type name", "read");
		
		// duplicate access types - case-insensitive
		accessTypeDefs = _utils.createAccessTypeDefs(new String[] { "read", "write", "execute", "READ" } );
		_failures.clear(); assertFalse(_validator.isValidAccessTypes(id, accessTypeDefs, _failures, Action.CREATE));
		_utils.checkFailureForSemanticError(_failures, "access type name", "READ");
		
		// unknown access type in implied grants list
		accessTypeDefs = _utils.createAccessTypeDefs(accessTypes_bad_unknownType);
		_failures.clear(); assertFalse(_validator.isValidAccessTypes(id, accessTypeDefs, _failures, Action.CREATE));
		_utils.checkFailureForSemanticError(_failures, "implied grants", "execute");
		_utils.checkFailureForSemanticError(_failures, "access type itemId", "1"); // id 1 is duplicated
		
		// access type with implied grant referring to itself
		accessTypeDefs = _utils.createAccessTypeDefs(accessTypes_bad_selfReference);
		_failures.clear(); assertFalse(_validator.isValidAccessTypes(id, accessTypeDefs, _failures, Action.CREATE));
		_utils.checkFailureForSemanticError(_failures, "implied grants", "admin");
	}
	
	final Object[][] enums_bad_enumName_null = new Object[][] {
		//  { id, enum-name,             enum-values }
			{ 1L, "authentication-type", new String[] { "simple", "kerberos" } },
			{ 2L, "time-unit", new String[] { "day", "hour", "minute" } },
			{ 3L, null, new String[] { "foo", "bar", "tar" } }, // null enum-name
	};
	
	final Object[][] enums_bad_enumName_blank = new Object[][] {
			//  { id, enum-name,             enum-values }
			{ 1L, "authentication-type", new String[] { "simple", "kerberos" } },
			{ 1L, "time-unit", new String[] { "day", "hour", "minute" } },
			{ 2L, "  ", new String[] { "foo", "bar", "tar" } }, // enum name is all spaces
	};
	
	final Object[][] enums_bad_Elements_empty = new Object[][] {
			//  { id, enum-name,             enum-values }
			{ null, "authentication-type", new String[] { "simple", "kerberos" } }, // null id
			{ 1L, "time-unit", new String[] { "day", "hour", "minute" } },
			{ 2L, "anEnum", new String[] { } }, // enum elements collection is empty
	};
	
	final Object[][] enums_bad_enumName_duplicate_differentCase = new Object[][] {
			//  { id, enum-name,             enum-values }
			{ 1L, "authentication-type", new String[] { "simple", "kerberos" } },
			{ 1L, "time-unit", new String[] { "day", "hour", "minute" } },
			{ 1L, "Authentication-Type", new String[] { } },// duplicate enum-name different in case
	};
	
	@Test
	public final void test_isValidEnums_happyPath() {
		List<RangerEnumDef> input = _utils.createEnumDefs(enums_good);
		assertTrue(_validator.isValidEnums(input, _failures));
		assertTrue(_failures.isEmpty());
	}
	
	@Test
	public final void test_isValidEnums_failures() {
		// null elements in enum def list are a failure
		List<RangerEnumDef> input = _utils.createEnumDefs(enums_good);
		input.add(null);
		assertFalse(_validator.isValidEnums(input, _failures));
		_utils.checkFailureForMissingValue(_failures, "enum def");
		
		// enum names should be valid
		input = _utils.createEnumDefs(enums_bad_enumName_null);
		_failures.clear(); assertFalse(_validator.isValidEnums(input, _failures));
		_utils.checkFailureForMissingValue(_failures, "enum def name");

		input = _utils.createEnumDefs(enums_bad_enumName_blank);
		_failures.clear(); assertFalse(_validator.isValidEnums(input, _failures));
		_utils.checkFailureForMissingValue(_failures, "enum def name");
		_utils.checkFailureForSemanticError(_failures, "enum def itemId", "1");
		
		// enum elements collection should not be null or empty
		input = _utils.createEnumDefs(enums_good);
		RangerEnumDef anEnumDef = mock(RangerEnumDef.class);
		when(anEnumDef.getName()).thenReturn("anEnum");
		when(anEnumDef.getElements()).thenReturn(null);
		input.add(anEnumDef);
		_failures.clear(); assertFalse(_validator.isValidEnums(input, _failures));
		_utils.checkFailureForMissingValue(_failures, "enum values", "anEnum");

		input = _utils.createEnumDefs(enums_bad_Elements_empty);
		_failures.clear(); assertFalse(_validator.isValidEnums(input, _failures));
		_utils.checkFailureForMissingValue(_failures, "enum values", "anEnum");
		_utils.checkFailureForMissingValue(_failures, "enum def itemId");
	
		// enum names should be distinct -- exact match
		input = _utils.createEnumDefs(enums_good);
		// add an element with same name as the first element
		String name = input.iterator().next().getName();
		when(anEnumDef.getName()).thenReturn(name);
		List<RangerEnumElementDef> elementDefs = _utils.createEnumElementDefs(new String[] {"val1", "val2"});
		when(anEnumDef.getElements()).thenReturn(elementDefs);
		input.add(anEnumDef);
		_failures.clear(); assertFalse(_validator.isValidEnums(input, _failures));
		_utils.checkFailureForSemanticError(_failures, "enum def name", name);

		// enum names should be distinct -- case insensitive
		input = _utils.createEnumDefs(enums_bad_enumName_duplicate_differentCase);
		_failures.clear(); assertFalse(_validator.isValidEnums(input, _failures));
		_utils.checkFailureForSemanticError(_failures, "enum def name", "Authentication-Type");
	
		// enum default index should be right
		input = _utils.createEnumDefs(enums_good);
		// set the index of 1st on to be less than 0
		when(input.iterator().next().getDefaultIndex()).thenReturn(-1);
		_failures.clear(); assertFalse(_validator.isValidEnums(input, _failures));
		_utils.checkFailureForSemanticError(_failures, "enum default index", "authentication-type");
		// set the index to be more than number of elements
		when(input.iterator().next().getDefaultIndex()).thenReturn(2);
		_failures.clear(); assertFalse(_validator.isValidEnums(input, _failures));
		_utils.checkFailureForSemanticError(_failures, "enum default index", "authentication-type");
	}

	@Test
	public final void test_isValidEnumElements_happyPath() {
		List<RangerEnumElementDef> input = _utils.createEnumElementDefs(new String[] { "simple", "kerberos" });
		assertTrue(_validator.isValidEnumElements(input, _failures, "anEnum"));
		assertTrue(_failures.isEmpty());
	}

	@Test
	public final void test_isValidEnumElements_failures() {
		// enum element collection should not have nulls in it
		List<RangerEnumElementDef> input = _utils.createEnumElementDefs(new String[] { "simple", "kerberos" });
		input.add(null);
		assertFalse(_validator.isValidEnumElements(input, _failures, "anEnum"));
		_utils.checkFailureForMissingValue(_failures, "enum element", "anEnum");

		// element names can't be null/empty
		input = _utils.createEnumElementDefs(new String[] { "simple", "kerberos", null });
		_failures.clear(); assertFalse(_validator.isValidEnumElements(input, _failures, "anEnum"));
		_utils.checkFailureForMissingValue(_failures, "enum element name", "anEnum");

		input = _utils.createEnumElementDefs(new String[] { "simple", "kerberos", "		" }); // two tabs
		_failures.clear(); assertFalse(_validator.isValidEnumElements(input, _failures, "anEnum"));
		_utils.checkFailureForMissingValue(_failures, "enum element name", "anEnum");
		
		// element names should be distinct - case insensitive
		input = _utils.createEnumElementDefs(new String[] { "simple", "kerberos", "kerberos" }); // duplicate name - exact match
		_failures.clear(); assertFalse(_validator.isValidEnumElements(input, _failures, "anEnum"));
		_utils.checkFailureForSemanticError(_failures, "enum element name", "kerberos");
		
		input = _utils.createEnumElementDefs(new String[] { "simple", "kerberos", "kErbErOs" }); // duplicate name - different case
		_failures.clear(); assertFalse(_validator.isValidEnumElements(input, _failures, "anEnum"));
		_utils.checkFailureForSemanticError(_failures, "enum element name", "kErbErOs");
	}

	Object[][] invalidResources = new Object[][] {
		//  { id,   level,      name }
			{ null, null,       null }, // everything is null
			{ 1L,     -10, "database" }, // -ve value for level is ok
			{ 1L,      10,    "table" }, // id is duplicate
			{ 2L,     -10, "DataBase" }, // (in different case) but name and level are duplicate
			{ 3L,      30,       "  " } // Name is all whitespace
	};

	Object[][] mixedCaseResources = new Object[][] {
			//  { id,   level,      name }
			{ 4L,     -10, "DBase" }, // -ve value for level is ok
			{ 5L,      10,    "TABLE" }, // id is duplicate
			{ 6L,     -10, "Column" } // (in different case) but name and level are duplicate
	};

	@Test
	public final void test_isValidResources() {
		// null/empty resources are an error
		when(_serviceDef.getResources()).thenReturn(null);
		_failures.clear(); assertFalse(_validator.isValidResources(_serviceDef, _failures, Action.CREATE));
		_utils.checkFailureForMissingValue(_failures, "resources");
		
		List<RangerResourceDef> resources = new ArrayList<>();
		when(_serviceDef.getResources()).thenReturn(resources);
		_failures.clear(); assertFalse(_validator.isValidResources(_serviceDef, _failures, Action.CREATE));
		_utils.checkFailureForMissingValue(_failures, "resources");
		
		resources.addAll(_utils.createResourceDefsWithIds(invalidResources));
		_failures.clear(); assertFalse(_validator.isValidResources(_serviceDef, _failures, Action.CREATE));
		_utils.checkFailureForMissingValue(_failures, "resource name");
		_utils.checkFailureForMissingValue(_failures, "resource itemId");
		_utils.checkFailureForSemanticError(_failures, "resource itemId", "1"); // id 1 is duplicate
		_utils.checkFailureForSemanticError(_failures, "resource name", "DataBase");

		resources.clear(); resources.addAll(_utils.createResourceDefsWithIds(mixedCaseResources));
		_failures.clear(); assertFalse(_validator.isValidResources(_serviceDef, _failures, Action.CREATE));
		_utils.checkFailure(_failures, null, null, null, "DBase",null);
		_utils.checkFailure(_failures, null, null, null, "TABLE",null);
		_utils.checkFailure(_failures, null, null, null, "Column",null);
	}
	
	@Test
	public final void test_isValidResourceGraph() {
		Object[][] data_bad = new Object[][] {
			//  { name,  excludesSupported, recursiveSupported, mandatory, reg-exp, parent-level, level }
				{ "db",            null, null, null, null, "" ,             10 },
				{ "table",         null, null, null, null, "db",            20 },   // same as db's level
				{ "column-family", null, null, null, null, "table",         null }, // level is null!
				{ "column",        null, null, null, null, "column-family", 20 },   // level is duplicate for [db->table->column-family-> column] hierarchy
				{ "udf",           null, null, null, null, "db",            10 },   // udf's id conflicts with that of db in the [db->udf] hierarchy
		};
		
		List<RangerResourceDef> resourceDefs = _utils.createResourceDefs(data_bad);
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		when(_serviceDef.getName()).thenReturn("service-name");
		when(_serviceDef.getUpdateTime()).thenReturn(new Date());
		
		_failures.clear(); assertFalse(_validator.isValidResourceGraph(_serviceDef, _failures));
		_utils.checkFailureForMissingValue(_failures, "resource level");
		_utils.checkFailureForSemanticError(_failures, "resource level", "20"); // level 20 is duplicate for 1 hierarchy
		_utils.checkFailureForSemanticError(_failures, "resource level", "10"); // level 10 is duplicate for another hierarchy

        data_bad = new Object[][] {
                //  { name,  excludesSupported, recursiveSupported, mandatory, reg-exp, parent-level, level }
                { "db",            null, null, null, null, "" ,             10 },
                { "table",         null, null, null, null, "db",            20 },
                { "column-family", null, null, null, null, "table",         15 }, // level is smaller than table!
                { "column",        null, null, null, null, "column-family", 30 },
                { "udf",           null, null, null, null, "db",            15 },
        };
        resourceDefs = _utils.createResourceDefs(data_bad);
        when(_serviceDef.getResources()).thenReturn(resourceDefs);
        when(_serviceDef.getName()).thenReturn("service-name");
        when(_serviceDef.getUpdateTime()).thenReturn(new Date());

        _failures.clear(); assertFalse(_validator.isValidResourceGraph(_serviceDef, _failures));
        _utils.checkFailureForSemanticError(_failures, "resource level", "15"); // level 20 is duplicate for 1 hierarchy

        Object[][] data_good = new Object[][] {
			//  { name,  excludesSupported, recursiveSupported, mandatory, reg-exp, parent-level, level }
				{ "db",     null, null, null, null, "" ,     -10 }, // -ve level is ok
				{ "table",  null, null, null, null, "db",    0 },   // 0 level is ok
				{ "column", null, null, null, null, "table", 10 },  // level is null!
				{ "udf",    null, null, null, null, "db",    0 },   // should not conflict as it belong to a different hierarchy
		};
		resourceDefs = _utils.createResourceDefs(data_good);
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		_failures.clear(); assertTrue(_validator.isValidResourceGraph(_serviceDef, _failures));
		assertTrue(_failures.isEmpty());

        Object[][] data_cycles = new Object[][] {
                //  { name,  excludesSupported, recursiveSupported, mandatory, reg-exp, parent-level, level }
                { "db",     null, null, null, null, "column" ,     -10 }, // -ve level is ok
                { "table",  null, null, null, null, "db",    0 },   // 0 level is ok
                { "column", null, null, null, null, "table", 10 },  // level is null!
                { "udf",    null, null, null, null, "db",    -5 },   // should not conflict as it belong to a different hierarchy
        };

        resourceDefs = _utils.createResourceDefs(data_cycles);
        when(_serviceDef.getResources()).thenReturn(resourceDefs);
        _failures.clear(); assertFalse("Graph was valid!", _validator.isValidResourceGraph(_serviceDef, _failures));
        assertFalse(_failures.isEmpty());
        _utils.checkFailureForSemanticError(_failures, "resource graph");

        data_bad = new Object[][] {
                //  { name,  excludesSupported, recursiveSupported, mandatory, reg-exp, parent-level, level }
                { "db",     null, null, null, null, "" ,     -10 }, // -ve level is ok
                { "table",  null, null, true, null, "db",    0 },   // 0 level is ok; mandatory true here, but not at parent level?
                { "column", null, null, null, null, "table", 10 },  // level is null!
                { "udf",    null, null, null, null, "db",    0 },   // should not conflict as it belong to a different hierarchy
        };
        resourceDefs = _utils.createResourceDefs(data_bad);
        when(_serviceDef.getResources()).thenReturn(resourceDefs);
        _failures.clear(); assertFalse(_validator.isValidResourceGraph(_serviceDef, _failures));
        assertFalse(_failures.isEmpty());

        data_good = new Object[][] {
                //  { name,  excludesSupported, recursiveSupported, mandatory, reg-exp, parent-level, level }
                { "db",     null, null, true, null, "" ,     -10 }, // -ve level is ok
                { "table",  null, null, null, null, "db",    0 },   // 0 level is ok; mandatory true here, but not at parent level?
                { "column", null, null, null, null, "table", 10 },  // level is null!
                { "udf",    null, null, true, null, "db",    0 },   // should not conflict as it belong to a different hierarchy
        };
        resourceDefs = _utils.createResourceDefs(data_good);
        when(_serviceDef.getResources()).thenReturn(resourceDefs);
        _failures.clear(); assertTrue(_validator.isValidResourceGraph(_serviceDef, _failures));
        assertTrue(_failures.isEmpty());
    }
	
	@Test
	public final void test_isValidResources_happyPath() {
		Object[][] data = new Object[][] {
			//  { id,   level,      name }
				{ 1L,     -10, "database" }, // -ve value for level
				{ 3L,       0,    "table" }, // it is ok for id and level to skip values
				{ 5L,      10,   "column" }, // it is ok for level to skip values
		};
		List<RangerResourceDef> resources = _utils.createResourceDefsWithIds(data);
		when(_serviceDef.getResources()).thenReturn(resources);
		assertTrue(_validator.isValidResources(_serviceDef, _failures, Action.CREATE));
		assertTrue(_failures.isEmpty());
	}

	@Test
	public final void test_isValidConfigs_failures() {
		assertTrue(_validator.isValidConfigs(null, null, _failures));
		assertTrue(_failures.isEmpty());
		
		Object[][] config_def_data_bad = new Object[][] {
			//  { id,  name, type, subtype, default-value }	
				{ null, null, "" }, // id and name both null, type is empty
				{ 1L, "security", "blah" }, // bad type for service def
				{ 1L, "port", "int" }, // duplicate id
				{ 2L, "security", "string" }, // duplicate name
				{ 3L, "timeout", "enum", "units", null }, // , sub-type (units) is not among known enum types
				{ 4L, "auth", "enum", "authentication-type", "dimple" }, // default value is not among known values for the enum (sub-type)
		};
		
		List<RangerServiceConfigDef> configs = _utils.createServiceDefConfigs(config_def_data_bad);
		List<RangerEnumDef> enumDefs = _utils.createEnumDefs(enums_good);
		assertFalse(_validator.isValidConfigs(configs, enumDefs, _failures));
		_utils.checkFailureForMissingValue(_failures, "config def name");
		_utils.checkFailureForMissingValue(_failures, "config def itemId");
		_utils.checkFailureForMissingValue(_failures, "config def type");
		_utils.checkFailureForSemanticError(_failures, "config def name", "security"); // there were two configs with same name as security
		_utils.checkFailureForSemanticError(_failures, "config def itemId", "1"); // a config with duplicate had id of 1
		_utils.checkFailureForSemanticError(_failures, "config def type", "security"); // type for config security was invalid
		_utils.checkFailureForSemanticError(_failures, "config def subtype", "timeout"); // type for config security was invalid
		_utils.checkFailureForSemanticError(_failures, "config def default value", "auth"); // type for config security was invalid
	}

	@Test
	public final void test_isValidPolicyConditions() {
		long id = 7;
		when(_serviceDef.getId()).thenReturn(id);
		// null/empty policy conditions are ok
		assertTrue(_validator.isValidPolicyConditions(id,null, _failures, Action.CREATE));
		assertTrue(_failures.isEmpty());
		List<RangerPolicyConditionDef> conditionDefs = new ArrayList<>();
		assertTrue(_validator.isValidPolicyConditions(id, conditionDefs, _failures, Action.CREATE));
		assertTrue(_failures.isEmpty());
		
		Object[][] policyCondition_data = {
			//  { id, name, evaluator }	
				{ null, null, null}, // everything null!
				{ 1L, "condition-1", null }, // missing evaluator
				{ 1L, "condition-2", "" }, // duplicate id, missing evaluator
				{ 2L, "condition-1", "com.evaluator" }, // duplicate name
		};
		
		conditionDefs.addAll(_utils.createPolicyConditionDefs(policyCondition_data));
		_failures.clear(); assertFalse(_validator.isValidPolicyConditions(id, conditionDefs, _failures, Action.CREATE));
		_utils.checkFailureForMissingValue(_failures, "policy condition def itemId");
		_utils.checkFailureForMissingValue(_failures, "policy condition def name");
		_utils.checkFailureForMissingValue(_failures, "policy condition def evaluator");
		_utils.checkFailureForSemanticError(_failures, "policy condition def itemId", "1");
		_utils.checkFailureForSemanticError(_failures, "policy condition def name", "condition-1");
		_utils.checkFailureForMissingValue(_failures, "policy condition def evaluator", "condition-2");
		_utils.checkFailureForMissingValue(_failures, "policy condition def evaluator", "condition-1");
	}
	
	private ValidationTestUtils _utils = new ValidationTestUtils();
	RangerServiceDef _serviceDef;
	List<ValidationFailureDetails> _failures;
	ServiceStore _store;
	RangerServiceDefValidator _validator;
}
