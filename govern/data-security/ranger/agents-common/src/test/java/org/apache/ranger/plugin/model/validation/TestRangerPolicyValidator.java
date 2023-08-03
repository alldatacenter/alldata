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

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerPolicyResourceSignature;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.validation.RangerValidator.Action;
import org.apache.ranger.plugin.store.ServiceStore;
import org.apache.ranger.plugin.util.RangerObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class TestRangerPolicyValidator {

	/**
	 * Wrapper class only so we clear out the RangerServiceDefHelper before every test.
	 * @author alal
	 *
	 */
	static class RangerPolicyValidatorWrapper extends RangerPolicyValidator {
		public RangerPolicyValidatorWrapper(ServiceStore store) {
			super(store);
		}
		
		boolean isValid(Long id, Action action, List<ValidationFailureDetails> failures) {
			RangerServiceDefHelper._Cache.clear();
			return super.isValid(id, action, failures);
		}
		
		boolean isValid(RangerPolicy policy, Action action, boolean isAdmin, List<ValidationFailureDetails> failures) {
			RangerServiceDefHelper._Cache.clear();
			return super.isValid(policy, action, isAdmin, failures);
		}
	}
	@Before
	public void setUp() throws Exception {
		_store = mock(ServiceStore.class);
		_policy = mock(RangerPolicy.class);
		_validator = new RangerPolicyValidatorWrapper(_store);
		_serviceDef = mock(RangerServiceDef.class);
		_factory = mock(RangerObjectFactory.class);
		_validator._factory = _factory;
	}
	
	final Action[] cu = new Action[] { Action.CREATE, Action.UPDATE };
	final Object[] policyItemsData = new Object[] {
			ImmutableMap.of(  // all good
				"users", new String[] {"user1" ," user2"},
				"groups", new String[] {"group1", "group2"},
				"accesses", new String[] { "r", "w" },
				"isAllowed", new Boolean[] { true, true }),
			ImmutableMap.of(   // no users, access type different case
				"groups", new String[] {"group3", "group4"},
				"accesses", new String[]{"W", "x"},
				"isAllowed", new Boolean[] { true, true }),
			ImmutableMap.of(   // no groups
				"users", new String[] {"user3" ," user4"},
				"accesses", new String[] { "r", "x" },
				"isAllowed", new Boolean[] { true, true }),
			ImmutableMap.of( // isallowed on access types is null, case is different from that in definition
				"users", new String[] {"user7" ," user6"},
				"accesses", new String[] { "a" },
				"isAllowed", new Boolean[] { null, null })
	};
	String[] accessTypes = new String[] { "r", "w", "x", "A" };  // mix of lower and upper case
	String[] accessTypes_bad = new String[] { "r", "w", "xx", }; // two missing (x, a), one new that isn't on bad (xx)
	
	private final Object[][] resourceDefData = new Object[][] {
		//  { name,  excludesSupported, recursiveSupported, mandatory, reg-exp,       parent-level }
			{ "db",  null,              null,               true,      "db\\d+",      null },       // valid values: db1, db22, db983, etc.; invalid: db, db12x, ttx11, etc.; null => false for excludes and recursive
			{ "tbl", true,              true,               true,      null,          "db" },       // regex == null => anything goes; excludes == true, recursive == true
			{ "col", false,             true,               false,     "col\\d{1,2}", "tbl" },      // valid: col1, col47, etc.; invalid: col, col238, col1, etc., excludes == false, recursive == true
	};
	
	private final Object[][] resourceDefData_multipleHierarchies = new Object[][] {
		//  { name,  excludesSupported, recursiveSupported, mandatory, reg-exp,       parent-level }
			{ "db",  null,              null,               true,      "db\\d+",      null },       // valid values: db1, db22, db983, etc.; invalid: db, db12x, ttx11, etc.; null => false for excludes and recursive
			{ "tbl", true,              true,               true,      null,          "db" },       // regex == null => anything goes; excludes == true, recursive == true
			{ "col", false,             true,               false,     "col\\d{1,2}", "tbl" },      // valid: col1, col47, etc.; invalid: col, col238, col1, etc., excludes == false, recursive == true
			{ "udf", true,              true,               true,      null,          "db" }        // same parent as tbl (simulating hive's multiple resource hierarchies)
	};

	private final Object[][] policyResourceMap_good = new Object[][] {
			// resource-name, values, excludes, recursive
			{ "db", new String[] { "db1", "db2" }, null, null },
			{ "tbl", new String[] { "tbl1", "tbl2" }, true, false } // case matters - use only lowercase characters
	};
	
	private final Object[][] policyResourceMap_goodMultipleHierarchies = new Object[][] {
			// resource-name, values, excludes, recursive
			{ "db", new String[] { "db1", "db2" }, null, null },
			{ "udf", new String[] { "udf1", "udf2" }, true, false } // case matters - use only lowercase characters
	};
	
	private final Object[][] policyResourceMap_bad = new Object[][] {
			// resource-name, values, excludes, recursive
			{ "db", new String[] { "db1", "db2" }, null, true },        // mandatory "tbl" missing; recursive==true specified when resource-def does not support it (null)
			{"col", new String[] { "col12", "col 1" }, true, true },    // wrong format of value for "col"; excludes==true specified when resource-def does not allow it (false)
			{"extra", new String[] { "extra1", "extra2" }, null, null } // spurious "extra" specified
	};

	private final Object[][] policyResourceMap_bad_multiple_hierarchies = new Object[][] {
			// resource-name, values, excludes, recursive
			{  "db", new String[] { "db1", "db2" }, null, true },
			{ "tbl", new String[] { "tbl11", "tbl2" }, null, true },
			{ "col", new String[] { "col1", "col2" }, true, true },
			{ "udf", new String[] { "extra1", "extra2" }, null, null } // either udf or tbl/db/col should be specified, not both
	};

	private final Object[][] policyResourceMap_bad_multiple_hierarchies_missing_mandatory = new Object[][] {
			// resource-name, values, excludes, recursive
			{  "db", new String[] { "db1", "db2" }, null, true }
	};

	@Test
	public final void testIsValid_long() throws Exception {
		// this validation should be removed if we start supporting other than delete action
		Assert.assertFalse(_validator.isValid(3L, Action.CREATE, _failures));
		_utils.checkFailureForInternalError(_failures);
		
		// should fail with appropriate error message if id is null
		_failures.clear(); _failures.clear(); Assert.assertFalse(_validator.isValid((Long)null, Action.DELETE, _failures));
		_utils.checkFailureForMissingValue(_failures, "id");
		
		// should not fail if policy can't be found for the specified id
		when(_store.getPolicy(1L)).thenReturn(null);
		when(_store.getPolicy(2L)).thenThrow(new Exception());
		RangerPolicy existingPolicy = mock(RangerPolicy.class);
		when(_store.getPolicy(3L)).thenReturn(existingPolicy);
		_failures.clear(); Assert.assertTrue(_validator.isValid(1L, Action.DELETE, _failures));
		Assert.assertTrue(_failures.isEmpty());
		_failures.clear(); Assert.assertTrue(_validator.isValid(2L, Action.DELETE, _failures));
		Assert.assertTrue(_failures.isEmpty());

		// if policy exists then delete validation should pass, too!
		_failures.clear(); Assert.assertTrue(_validator.isValid(3L, Action.DELETE, _failures));
		Assert.assertTrue(_failures.isEmpty());
	}

	@Test
	public final void testIsValid_errorPaths() throws Exception {
		boolean isAdmin = true;

		// 1. create policy in a non-existing service
		Action action = Action.CREATE;
		when(_policy.getService()).thenReturn("non-existing-service-name");
		when(_store.getServiceByName("non-existing-service-name")).thenReturn(null);

		Assert.assertFalse(action.toString(), _validator.isValid(_policy, action, isAdmin, _failures));

		// 2. update a policy to change the service-name
		RangerPolicy existingPolicy = mock(RangerPolicy.class);
		when(existingPolicy.getId()).thenReturn(8L);
		when(existingPolicy.getService()).thenReturn("service-name");

		RangerService service = mock(RangerService.class);
		when(service.getType()).thenReturn("service-type");
		when(service.getName()).thenReturn("service-name");
		when(_store.getServiceByName("service-name")).thenReturn(service);

		RangerService service2 = mock(RangerService.class);
		when(service2.getType()).thenReturn("service-type");
		when(service2.getName()).thenReturn("service-name2");
		when(_store.getServiceByName("service-name2")).thenReturn(service2);

		when(_policy.getService()).thenReturn("service-name2");

		RangerPolicyResourceSignature policySignature = mock(RangerPolicyResourceSignature.class);
		when(policySignature.getSignature()).thenReturn("hash-1");

		when(_factory.createPolicyResourceSignature(_policy)).thenReturn(policySignature);

		when(_store.getServiceByName("service-name2")).thenReturn(service2);
		action = Action.UPDATE;

		Assert.assertFalse(action.toString(), _validator.isValid(_policy, action, isAdmin, _failures));

		// 3. update a policy to change the policy-type
		when(existingPolicy.getId()).thenReturn(8L);
		when(existingPolicy.getService()).thenReturn("service-name");
		when(existingPolicy.getPolicyType()).thenReturn(Integer.valueOf(0));

		when(_policy.getId()).thenReturn(8L);
		when(_policy.getService()).thenReturn("service-name");
		when(_policy.getPolicyType()).thenReturn(Integer.valueOf(1));

		Assert.assertFalse(action.toString(), _validator.isValid(_policy, action, isAdmin, _failures));
	}

	@Test
	public final void testIsValid_happyPath() throws Exception {
		// valid policy has valid non-empty name and service name
		when(_policy.getService()).thenReturn("service-name");
		// service name exists
		RangerService service = mock(RangerService.class);
		when(service.getType()).thenReturn("service-type");
		when(service.getId()).thenReturn(2L);
		when(_store.getServiceByName("service-name")).thenReturn(service);
		// service points to a valid service-def
		_serviceDef = _utils.createServiceDefWithAccessTypes(accessTypes);
		when(_serviceDef.getName()).thenReturn("service-type");
		when(_store.getServiceDefByName("service-type")).thenReturn(_serviceDef);
		// a matching policy should exist for create when checked by id and not exist when checked by name.
		when(_store.getPolicy(7L)).thenReturn(null);
		RangerPolicy existingPolicy = mock(RangerPolicy.class);
		when(existingPolicy.getId()).thenReturn(8L);
		when(existingPolicy.getService()).thenReturn("service-name");
		when(_store.getPolicy(8L)).thenReturn(existingPolicy);
		// a matching policy should not exist for update.
		// valid policy can have empty set of policy items if audit is turned on
		// null value for audit is treated as audit on.
		// for now we want to turn any resource related checking off
		when(_policy.getResources()).thenReturn(null);
		for (Action action : cu) {
			for (Boolean auditEnabled : new Boolean[] { null, true } ) {
				for (boolean isAdmin : new boolean[] { true, false }) {
					when(_policy.getIsAuditEnabled()).thenReturn(auditEnabled);
					if (action == Action.CREATE) {
						when(_policy.getId()).thenReturn(7L);
						when(_policy.getName()).thenReturn("policy-name-1");
						when(_store.getPolicyId(service.getId(), _policy.getName(), _zoneId)).thenReturn(null);
						Assert.assertTrue("" + action + ", " + auditEnabled, _validator.isValid(_policy, action, isAdmin, _failures));
						Assert.assertTrue(_failures.isEmpty());
					} else {
						// update should work both when by-name is found or not, since nothing found by-name means name is being updated.
						when(_policy.getId()).thenReturn(8L);
						when(_policy.getName()).thenReturn("policy-name-1");
						Assert.assertTrue("" + action + ", " + auditEnabled, _validator.isValid(_policy, action, isAdmin, _failures));
						Assert.assertTrue(_failures.isEmpty());
	
						when(_policy.getName()).thenReturn("policy-name-2");
						when(_store.getPolicyId(service.getId(), _policy.getName(), _zoneId)).thenReturn(null);
						Assert.assertTrue("" + action + ", " + auditEnabled, _validator.isValid(_policy, action, isAdmin, _failures));
						Assert.assertTrue(_failures.isEmpty());
					}
				}
			}
		}
		// if audit is disabled then policy should have policy items and all of them should be valid
		List<RangerPolicyItem> policyItems = _utils.createPolicyItems(policyItemsData);
		when(_policy.getPolicyItems()).thenReturn(policyItems);
		when(_policy.getIsAuditEnabled()).thenReturn(false);
		for (Action action : cu) {
			for (boolean isAdmin : new boolean[] { true, false}) {
				if (action == Action.CREATE) {
					when(_policy.getId()).thenReturn(7L);
					when(_policy.getName()).thenReturn("policy-name-1");
				} else {
					when(_policy.getId()).thenReturn(8L);
					when(_policy.getName()).thenReturn("policy-name-2");
				}
				Assert.assertTrue("" + action , _validator.isValid(_policy, action, isAdmin, _failures));
				Assert.assertTrue(_failures.isEmpty());
			}
		}
		
		// above succeeded as service def did not have any resources on it, mandatory or otherwise.
		// policy should have all mandatory resources specified, and they should conform to the validation pattern in resource definition
		List<RangerResourceDef> resourceDefs = _utils.createResourceDefs(resourceDefData);
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		Map<String, RangerPolicyResource> resourceMap = _utils.createPolicyResourceMap(policyResourceMap_good);
		when(_policy.getResources()).thenReturn(resourceMap);
		// let's add some other policies in the store for this service that have a different signature
		// setup the signatures on the policies
		RangerPolicyResourceSignature policySignature = mock(RangerPolicyResourceSignature.class);
		when(_factory.createPolicyResourceSignature(_policy)).thenReturn(policySignature);
		// setup the store to indicate that no other policy exists with matching signature
		when(policySignature.getSignature()).thenReturn("hash-1");
		when(_store.getPoliciesByResourceSignature("service-name", "hash-1", true)).thenReturn(null);
		// we are reusing the same policies collection here -- which is fine
		for (Action action : cu) {
			if (action == Action.CREATE) {
				when(_policy.getId()).thenReturn(7L);
				when(_policy.getName()).thenReturn("policy-name-1");
			} else {
				when(_policy.getId()).thenReturn(8L);
				when(_policy.getName()).thenReturn("policy-name-2");
			}
			Assert.assertTrue("" + action , _validator.isValid(_policy, action, true, _failures)); // since policy resource has excludes admin privilages would be required
			Assert.assertTrue(_failures.isEmpty());
		}
	}
	
	void checkFailure_isValid(Action action, String errorType, String field) {
		checkFailure_isValid(action, errorType, field, null);
	}
	
	void checkFailure_isValid(Action action, String errorType, String field, String subField) {
		
		for (boolean isAdmin : new boolean[] { true, false}) {
			_failures.clear();
			Assert.assertFalse(_validator.isValid(_policy, action, isAdmin, _failures));
			switch (errorType) {
			case "missing":
				_utils.checkFailureForMissingValue(_failures, field, subField);
				break;
			case "semantic":
				_utils.checkFailureForSemanticError(_failures, field, subField);
				break;
			case "internal error":
				_utils.checkFailureForInternalError(_failures);
				break;
			default:
				Assert.fail("Unsupported errorType[" + errorType + "]");
				break;
			}
		}
	}
	
	@Test
	public final void testIsValid_failures() throws Exception {
		for (Action action : cu) {
			// passing in a null policy should fail with appropriate failure reason
			_policy = null;
			checkFailure_isValid(action, "missing", "policy");
			
			// policy must have a name on it
			_policy = mock(RangerPolicy.class);
			for (String name : new String[] { null, "  " }) {
				when(_policy.getName()).thenReturn(name);
				when(_policy.getResources()).thenReturn(null);
				checkFailure_isValid(action, "missing", "name");
			}
			
			// for update id is required!
			if (action == Action.UPDATE) {
				when(_policy.getId()).thenReturn(null);
				checkFailure_isValid(action, "missing", "id");
			}
		}
		RangerService service = mock(RangerService.class);
		/*
		 * Id is ignored for Create but name should not belong to an existing policy.  For update, policy should exist for its id and should match its name.
		 */
		when(_policy.getName()).thenReturn("policy-name");
		when(_policy.getService()).thenReturn("service-name");

		when(_store.getServiceByName("service-name")).thenReturn(service);
		when(service.getId()).thenReturn(2L);

		RangerPolicy existingPolicy = mock(RangerPolicy.class);
		when(existingPolicy.getId()).thenReturn(7L);
		when(existingPolicy.getService()).thenReturn("service-name");
		List<RangerPolicy> existingPolicies = new ArrayList<>();

		when(_store.getPolicyId(service.getId(), "policy-name", _zoneId)).thenReturn(7L);
		checkFailure_isValid(Action.CREATE, "semantic", "policy name");
		
		// update : does not exist for id
		when(_policy.getId()).thenReturn(7L);
		when(_store.getPolicy(7L)).thenReturn(null);
		checkFailure_isValid(Action.UPDATE, "semantic", "id");

		// Update: name should not point to an existing different policy, i.e. with a different id
		when(_store.getPolicy(7L)).thenReturn(existingPolicy);
		RangerPolicy anotherExistingPolicy = mock(RangerPolicy.class);
		when(anotherExistingPolicy.getId()).thenReturn(8L);
		when(anotherExistingPolicy.getService()).thenReturn("service-name");

		existingPolicies.add(anotherExistingPolicy);
		when(_store.getPolicyId(service.getId(), "policy-name", _zoneId)).thenReturn(8L);
		checkFailure_isValid(Action.UPDATE, "semantic", "id/name");
		
		// policy must have service name on it and it should be valid
		when(_policy.getName()).thenReturn("policy-name");
		for (Action action : cu) {
			for (boolean isAdmin : new boolean[] { true, false }) {
				when(_policy.getService()).thenReturn(null);
				_failures.clear(); Assert.assertFalse(_validator.isValid(_policy, action, isAdmin, _failures));
				_utils.checkFailureForMissingValue(_failures, "service name");
	
				when(_policy.getService()).thenReturn("");
				_failures.clear(); Assert.assertFalse(_validator.isValid(_policy, action, isAdmin, _failures));
				_utils.checkFailureForMissingValue(_failures, "service name");
			}
		}
		
		// service name should be valid
		when(_store.getServiceByName("service-name")).thenReturn(null);
		when(_store.getServiceByName("another-service-name")).thenThrow(new Exception());
		for (Action action : cu) {
			for (boolean isAdmin : new boolean[] { true, false }) {
				when(_policy.getService()).thenReturn(null);
				_failures.clear(); Assert.assertFalse(_validator.isValid(_policy, action, isAdmin, _failures));
				_utils.checkFailureForMissingValue(_failures, "service name");
	
				when(_policy.getService()).thenReturn(null);
				_failures.clear(); Assert.assertFalse(_validator.isValid(_policy, action, isAdmin, _failures));
				_utils.checkFailureForMissingValue(_failures, "service name");
	
				when(_policy.getService()).thenReturn("service-name");
				_failures.clear(); Assert.assertFalse(_validator.isValid(_policy, action, isAdmin, _failures));
				_utils.checkFailureForSemanticError(_failures, "service name");
	
				when(_policy.getService()).thenReturn("another-service-name");
				_failures.clear(); Assert.assertFalse(_validator.isValid(_policy, action, isAdmin, _failures));
				_utils.checkFailureForSemanticError(_failures, "service name");
			}
		}
		
		// policy must contain at least one policy item
		List<RangerPolicyItem> policyItems = new ArrayList<>();
		for (Action action : cu) {
			for (boolean isAdmin : new boolean[] { true, false }) {
				// when it is null
				when(_policy.getPolicyItems()).thenReturn(null);
				_failures.clear(); Assert.assertFalse(_validator.isValid(_policy, action, isAdmin, _failures));
				_utils.checkFailureForMissingValue(_failures, "policy items");
				// or when it is not null but empty.
				when(_policy.getPolicyItems()).thenReturn(policyItems);
				_failures.clear(); Assert.assertFalse(_validator.isValid(_policy, action, isAdmin, _failures));
				_utils.checkFailureForMissingValue(_failures, "policy items");
			}
		}
		
		// these are known good policy items -- same as used above in happypath
		policyItems = _utils.createPolicyItems(policyItemsData);
		when(_policy.getPolicyItems()).thenReturn(policyItems);
		// policy item check requires that service def should exist
		when(service.getType()).thenReturn("service-type");
		when(_store.getServiceDefByName("service-type")).thenReturn(null);
		for (Action action : cu) {
			for (boolean isAdmin : new boolean[] { true, false }) {
				when(_policy.getService()).thenReturn("service-name");
				when(_store.getServiceByName("service-name")).thenReturn(service);
				_failures.clear(); Assert.assertFalse(_validator.isValid(_policy, action, isAdmin, _failures));
				_utils.checkFailureForInternalError(_failures, "policy service def");
			}
		}
		
		// service-def should contain the right access types on it.
		_serviceDef = _utils.createServiceDefWithAccessTypes(accessTypes_bad, "service-type");
		when(_store.getServiceDefByName("service-type")).thenReturn(_serviceDef);
		for (Action action : cu) {
			for (boolean isAdmin : new boolean[] { true, false }) {
				_failures.clear(); Assert.assertFalse(_validator.isValid(_policy, action, isAdmin, _failures));
				_utils.checkFailureForSemanticError(_failures, "policy item access type");
			}
		}
		
		// create the right service def with right resource defs - this is the same as in the happypath test above.
		_serviceDef = _utils.createServiceDefWithAccessTypes(accessTypes, "service-type");
		when(_store.getPolicyId(service.getId(), "policy-name", _zoneId)).thenReturn(null);
		List<RangerResourceDef> resourceDefs = _utils.createResourceDefs(resourceDefData);
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		when(_store.getServiceDefByName("service-type")).thenReturn(_serviceDef);

		// one mandatory is missing (tbl) and one unknown resource is specified (extra), and values of option resource don't conform to validation pattern (col)
		Map<String, RangerPolicyResource> policyResources = _utils.createPolicyResourceMap(policyResourceMap_bad);
		when(_policy.getResources()).thenReturn(policyResources);
		// ensure thta policy is kosher when it comes to resource signature
		RangerPolicyResourceSignature signature = mock(RangerPolicyResourceSignature.class);
		when(_factory.createPolicyResourceSignature(_policy)).thenReturn(signature);
		when(signature.getSignature()).thenReturn("hash-1");
		when(_store.getPoliciesByResourceSignature("service-name", "hash-1", true)).thenReturn(null); // store does not have any policies for that signature hash
		for (Action action : cu) {
			for (boolean isAdmin : new boolean[] { true, false }) {
				_failures.clear(); Assert.assertFalse(_validator.isValid(_policy, action, isAdmin, _failures));
				_utils.checkFailureForSemanticError(_failures, "resource-values", "col"); // for spurious resource: "extra"
				_utils.checkFailureForSemanticError(_failures, "isRecursive", "db"); // for specifying it as true when def did not allow it
				_utils.checkFailureForSemanticError(_failures, "isExcludes", "col"); // for specifying it as true when def did not allow it
			}
		}
		
		// Check if error around resource signature clash are reported.  have Store return policies for same signature
		when(_store.getPoliciesByResourceSignature("service-name", "hash-1", true)).thenReturn(existingPolicies);
		for (Action action : cu) {
			for (boolean isAdmin : new boolean[] { true, false }) {
				_failures.clear(); Assert.assertFalse(_validator.isValid(_policy, action, isAdmin, _failures));
				_utils.checkFailureForSemanticError(_failures, "policy resources");
			}
		}
	}
	
	RangerPolicy anyPolicy() {
		return argThat(new ArgumentMatcher<RangerPolicy>() {

			@Override
			public boolean matches(RangerPolicy argument) {
				return true;
			}
		});
	}
	
	@Test
	public void test_isValidResourceValues() {
		List<RangerResourceDef> resourceDefs = _utils.createResourceDefs(resourceDefData);
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		Map<String, RangerPolicyResource> policyResources = _utils.createPolicyResourceMap(policyResourceMap_bad);
		Assert.assertFalse(_validator.isValidResourceValues(policyResources, _failures, _serviceDef));
		_utils.checkFailureForSemanticError(_failures, "resource-values", "col");
		
		policyResources = _utils.createPolicyResourceMap(policyResourceMap_good);
		Assert.assertTrue(_validator.isValidResourceValues(policyResources, _failures, _serviceDef));
	}
	
	@Test
	public void test_isValidPolicyItems_failures() {
		// null/empty list is good because there is nothing
		Assert.assertTrue(_validator.isValidPolicyItems(null, _failures, _serviceDef));
		_failures.isEmpty();

		List<RangerPolicyItem> policyItems = new ArrayList<>();
		Assert.assertTrue(_validator.isValidPolicyItems(policyItems, _failures, _serviceDef));
		_failures.isEmpty();
		
		// null elements in the list are flagged
		policyItems.add(null);
		Assert.assertFalse(_validator.isValidPolicyItems(policyItems, _failures, _serviceDef));
		_utils.checkFailureForMissingValue(_failures, "policy item");
	}
	
	@Test
	public void test_isValidPolicyItem_failures() {
		// empty access collections are invalid
		RangerPolicyItem policyItem = mock(RangerPolicyItem.class);
		when(policyItem.getAccesses()).thenReturn(null);
		_failures.clear(); Assert.assertFalse(_validator.isValidPolicyItem(policyItem, _failures, _serviceDef));
		_utils.checkFailureForMissingValue(_failures, "policy item accesses");

		List<RangerPolicyItemAccess> accesses = new ArrayList<>();
		when(policyItem.getAccesses()).thenReturn(accesses);
		_failures.clear(); Assert.assertFalse(_validator.isValidPolicyItem(policyItem, _failures, _serviceDef));
		_utils.checkFailureForMissingValue(_failures, "policy item accesses");
		
		// both user and groups can't be null
		RangerPolicyItemAccess access = mock(RangerPolicyItemAccess.class);
		accesses.add(access);
		when(policyItem.getUsers()).thenReturn(null);
		when(policyItem.getGroups()).thenReturn(new ArrayList<String>());
		_failures.clear(); Assert.assertFalse(_validator.isValidPolicyItem(policyItem, _failures, _serviceDef));
		_utils.checkFailureForMissingValue(_failures, "policy item users/user-groups/roles");
	}
	
	@Test
	public void test_isValidPolicyItem_happPath() {
		// A policy item with no access is valid if it has delegated admin turned on and one user/group specified.
		RangerPolicyItem policyItem = mock(RangerPolicyItem.class);
		when(policyItem.getAccesses()).thenReturn(null);
		when(policyItem.getDelegateAdmin()).thenReturn(true);
		// create a non-empty user-list
		List<String> users = Arrays.asList("user1");
		when(policyItem.getUsers()).thenReturn(users);
		_failures.clear(); Assert.assertTrue(_validator.isValidPolicyItem(policyItem, _failures, _serviceDef));
		Assert.assertTrue(_failures.isEmpty());
	}
	@Test
	public void test_isValidItemAccesses_happyPath() {
		
		// happy path
		Object[][] data = new Object[][] {
				{ "a", null }, // valid
				{ "b", true }, // valid
				{ "c", true }, // valid
		};
		List<RangerPolicyItemAccess> accesses = _utils.createItemAccess(data);
		_serviceDef = _utils.createServiceDefWithAccessTypes(new String[] { "a", "b", "c", "d" });
		Assert.assertTrue(_validator.isValidItemAccesses(accesses, _failures, _serviceDef));
		Assert.assertTrue(_failures.isEmpty());
	}
	
	@Test
	public void test_isValidItemAccesses_failure() {
		// null policy item access values are an error
		List<RangerPolicyItemAccess> accesses = new ArrayList<>();
		accesses.add(null);
		_failures.clear(); Assert.assertFalse(_validator.isValidItemAccesses(accesses, _failures, _serviceDef));
		_utils.checkFailureForMissingValue(_failures, "policy item access");

		// all items must be valid for this call to be valid
		Object[][] data = new Object[][] {
				{ "a", null }, // valid
				{ null, null }, // invalid - name can't be null
				{ "c", true }, // valid
		};
		accesses = _utils.createItemAccess(data);
		_serviceDef = _utils.createServiceDefWithAccessTypes(new String[] { "a", "b", "c", "d" });
		_failures.clear(); Assert.assertFalse(_validator.isValidItemAccesses(accesses, _failures, _serviceDef));
	}
	
	@Test
	public void test_isValidPolicyItemAccess_happyPath() {
		
		RangerPolicyItemAccess access = mock(RangerPolicyItemAccess.class);
		when(access.getType()).thenReturn("an-Access"); // valid

		Set<String> validAccesses = Sets.newHashSet(new String[] { "an-access", "another-access" });  // valid accesses should be lower-cased
		
		// both null or true access types are the same and valid
		for (Boolean allowed : new Boolean[] { null, true } ) {
			when(access.getIsAllowed()).thenReturn(allowed);
			Assert.assertTrue(_validator.isValidPolicyItemAccess(access, _failures, validAccesses));
			Assert.assertTrue(_failures.isEmpty());
		}
	}
	
	@Test
	public void test_isValidPolicyItemAccess_failures() {
		
		Set<String> validAccesses = Sets.newHashSet(new String[] { "anAccess", "anotherAccess" });
		// null/empty names are invalid
		RangerPolicyItemAccess access = mock(RangerPolicyItemAccess.class);
		when(access.getIsAllowed()).thenReturn(null); // valid since null == true
		for (String type : new String[] { null, " 	"}) {
			when(access.getType()).thenReturn(type); // invalid
			// null/empty validAccess set skips all checks
			Assert.assertTrue(_validator.isValidPolicyItemAccess(access, _failures, null));
			Assert.assertTrue(_validator.isValidPolicyItemAccess(access, _failures, new HashSet<String>()));
			_failures.clear(); Assert.assertFalse(_validator.isValidPolicyItemAccess(access, _failures, validAccesses));
			_utils.checkFailureForMissingValue(_failures, "policy item access type");
		}
		
		when(access.getType()).thenReturn("anAccess"); // valid
		when(access.getIsAllowed()).thenReturn(false); // invalid
		_failures.clear();Assert.assertFalse(_validator.isValidPolicyItemAccess(access, _failures, validAccesses));
		_utils.checkFailureForSemanticError(_failures, "policy item access type allowed");
		
		when(access.getType()).thenReturn("newAccessType"); // invalid
		_failures.clear(); Assert.assertFalse(_validator.isValidPolicyItemAccess(access, _failures, validAccesses));
		_utils.checkFailureForSemanticError(_failures, "policy item access type");
	}
	
	final Object[][] resourceDef_happyPath = new Object[][] {
			// { "resource-name", "isExcludes", "isRecursive" }
			{ "db", true, true },
			{ "tbl", null, true },
			{ "col", true, false },
	};
	
	private Object[][] policyResourceMap_happyPath = new Object[][] {
			// { "resource-name", "values" "isExcludes", "isRecursive" }
			// values collection is null as it isn't relevant to the part being tested with this data
			{ "db", null, null, true },    // null should be treated as false
			{ "tbl", null, false, false }, // set to false where def is null and def is true
			{ "col", null, true, null}     // set to null where def is false
	};
	
	@Test
	public final void test_isValidResourceFlags_happyPath() {

		Map<String, RangerPolicyResource> resourceMap = _utils.createPolicyResourceMap(policyResourceMap_happyPath);
		List<RangerResourceDef> resourceDefs = _utils.createResourceDefs(resourceDef_happyPath);
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		Assert.assertTrue(_validator.isValidResourceFlags(resourceMap, _failures, resourceDefs, "a-service-def", "a-policy", true));

		// Since one of the resource has excludes set to true, without admin privilages it should fail and contain appropriate error messages
		Assert.assertFalse(_validator.isValidResourceFlags(resourceMap, _failures, resourceDefs, "a-service-def", "a-policy", false));
		_utils.checkFailureForSemanticError(_failures, "isExcludes", "isAdmin");
	}

	private Object[][] policyResourceMap_failures = new Object[][] {
			// { "resource-name", "values" "isExcludes", "isRecursive" }
			// values collection is null as it isn't relevant to the part being tested with this data
			{ "db", null, true, true },    // ok: def has true for both
			{ "tbl", null, true, null },   // excludes: definition does not allow excludes by resource has it set to true
			{ "col", null, false, true }    // recursive: def==null (i.e. false), policy==true
	};
	
	@Test
	public final void test_isValidResourceFlags_failures() {
		// passing true when def says false/null
		List<RangerResourceDef> resourceDefs = _utils.createResourceDefs(resourceDef_happyPath);
		Map<String, RangerPolicyResource> resourceMap = _utils.createPolicyResourceMap(policyResourceMap_failures);
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		// should not error out on
		Assert.assertFalse(_validator.isValidResourceFlags(resourceMap, _failures, resourceDefs, "a-service-def", "a-policy", false));
		_utils.checkFailureForSemanticError(_failures, "isExcludes", "tbl");
		_utils.checkFailureForSemanticError(_failures, "isRecursive", "col");
		_utils.checkFailureForSemanticError(_failures, "isExcludes", "isAdmin");
	}

	@Test
	public final void test_isPolicyResourceUnique() throws Exception {

		// if store does not contain any matching policies then check should succeed
		RangerPolicyResourceSignature signature = mock(RangerPolicyResourceSignature.class);
		String hash = "hash-1";
		when(signature.getSignature()).thenReturn(hash);
		when(_factory.createPolicyResourceSignature(_policy)).thenReturn(signature);
		when(_policy.getService()).thenReturn("service-name");
		List<RangerPolicy> policies = null;
		when(_store.getPoliciesByResourceSignature("service-name", hash, true)).thenReturn(policies);
		policies = new ArrayList<>();
		for (Action action : cu) {
			Assert.assertTrue(_validator.isPolicyResourceUnique(_policy, _failures, action));
			Assert.assertTrue(_validator.isPolicyResourceUnique(_policy, _failures, action));
		}
		/*
		 * If store has a policy with matching signature then the check should fail with appropriate error message.
		 * - For create any match is a problem
		 * - Signature check can never fail for disabled policies!
		 */
		RangerPolicy policy1 = mock(RangerPolicy.class); policies.add(policy1);
		when(_store.getPoliciesByResourceSignature("service-name", hash, true)).thenReturn(policies);
		when(_policy.getIsEnabled()).thenReturn(true); // ensure policy is enabled
		_failures.clear(); Assert.assertFalse(_validator.isPolicyResourceUnique(_policy, _failures, Action.CREATE));
		_utils.checkFailureForSemanticError(_failures, "resources");
		// same check should fail even if the policy is disabled
		when(_policy.getIsEnabled()).thenReturn(false);
		_failures.clear(); Assert.assertFalse(_validator.isPolicyResourceUnique(_policy, _failures, Action.CREATE));
		_utils.checkFailureForSemanticError(_failures, "resources");

		// For Update match with itself is not a problem as long as it isn't itself, i.e. same id.
		when(_policy.getIsEnabled()).thenReturn(true); // ensure policy is enabled
		when(policy1.getId()).thenReturn(103L);
		when(_policy.getId()).thenReturn(103L);
		Assert.assertTrue(_validator.isPolicyResourceUnique(_policy, _failures, Action.UPDATE));

		// matching policy can't be some other policy (i.e. different id) because that implies a conflict.
		when(policy1.getId()).thenReturn(104L);
		Assert.assertFalse(_validator.isPolicyResourceUnique(_policy, _failures, Action.UPDATE));
		_utils.checkFailureForSemanticError(_failures, "resources");
		// same check should pass if the policy is disabled
		when(_policy.getIsEnabled()).thenReturn(false);
		_failures.clear(); Assert.assertFalse(_validator.isPolicyResourceUnique(_policy, _failures, Action.UPDATE));
		_utils.checkFailureForSemanticError(_failures, "resources");

		// And validation should never pass if there are more than one policies with matching signature, regardless of their ID!!
		RangerPolicy policy2 = mock(RangerPolicy.class);
		when(policy2.getId()).thenReturn(103L);  // has same id as the policy being tested (_policy)
		policies.add(policy2);
		when(_policy.getIsEnabled()).thenReturn(true); // ensure policy is enabled
		Assert.assertFalse(_validator.isPolicyResourceUnique(_policy, _failures, Action.UPDATE));
		_utils.checkFailureForSemanticError(_failures, "resources");
		// same check should pass if the policy is disabled
		when(_policy.getIsEnabled()).thenReturn(false);
		_failures.clear(); Assert.assertFalse(_validator.isPolicyResourceUnique(_policy, _failures, Action.UPDATE));
		_utils.checkFailureForSemanticError(_failures, "resources");
	}
	
	@Test
	public final void test_isValidResourceNames_happyPath() {
		String serviceName = "a-service-def";
		// setup service-def
		Date now = new Date();
		when(_serviceDef.getName()).thenReturn(serviceName );
		when(_serviceDef.getUpdateTime()).thenReturn(now);
		List<RangerResourceDef> resourceDefs = _utils.createResourceDefs(resourceDefData_multipleHierarchies);
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		// setup policy
		Map<String, RangerPolicyResource> policyResources = _utils.createPolicyResourceMap(policyResourceMap_goodMultipleHierarchies);				
		when(_policy.getResources()).thenReturn(policyResources);
		Assert.assertTrue(_validator.isValidResourceNames(_policy, _failures, _serviceDef));
	}
	
	@Test
	public final void test_isValidResourceNames_failures() {
		String serviceName = "a-service-def";
		// setup service-def
		Date now = new Date();
		when(_serviceDef.getName()).thenReturn(serviceName );
		when(_serviceDef.getUpdateTime()).thenReturn(now);
		List<RangerResourceDef> resourceDefs = _utils.createResourceDefs(resourceDefData_multipleHierarchies);
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		// setup policy
		Map<String, RangerPolicyResource> policyResources = _utils.createPolicyResourceMap(policyResourceMap_bad);
		when(_policy.getResources()).thenReturn(policyResources);
		Assert.assertFalse("Missing required resource and unknown resource", _validator.isValidResourceNames(_policy, _failures, _serviceDef));
		_utils.checkFailureForSemanticError(_failures, "policy resources");

		// another bad resource map that straddles multiple hierarchies
		policyResources = _utils.createPolicyResourceMap(policyResourceMap_bad_multiple_hierarchies);
		when(_policy.getResources()).thenReturn(policyResources);
		_failures.clear(); Assert.assertFalse("Policy with resources for multiple hierarchies", _validator.isValidResourceNames(_policy, _failures, _serviceDef));
		_utils.checkFailureForSemanticError(_failures, "policy resources", "incompatible");

		// another bad policy resource map that could match multiple hierarchies but is short on mandatory resources for all of those matches
		policyResources = _utils.createPolicyResourceMap(policyResourceMap_bad_multiple_hierarchies_missing_mandatory);
		when(_policy.getResources()).thenReturn(policyResources);
		_failures.clear(); Assert.assertFalse("Policy with resources for multiple hierarchies missing mandatory resources for all pontential matches", _validator.isValidResourceNames(_policy, _failures, _serviceDef));
		_utils.checkFailureForSemanticError(_failures, "policy resources", "missing mandatory");
	}

	@Test
	public void test_isValidResource_additionalResources() throws Exception {
		String                                  serviceName         = "a-service-def";
		Date                                    now                 = new Date();
		List<RangerResourceDef>                 resourceDefs        = _utils.createResourceDefs(resourceDefData_multipleHierarchies);
		Map<String, RangerPolicyResource>       resources           = _utils.createPolicyResourceMap(policyResourceMap_good);
		List<Map<String, RangerPolicyResource>> additionalResources = new ArrayList<>();

		when(_serviceDef.getName()).thenReturn(serviceName );
		when(_serviceDef.getUpdateTime()).thenReturn(now);
		when(_serviceDef.getResources()).thenReturn(resourceDefs);
		when(_policy.getResources()).thenReturn(resources);
		when(_policy.getAdditionalResources()).thenReturn(additionalResources);

		Assert.assertTrue("valid resources and empty additionalResources", _validator.isValidResourceNames(_policy, _failures, _serviceDef));

		additionalResources.add(_utils.createPolicyResourceMap(policyResourceMap_good));
		Assert.assertTrue("valid resources and additionalResources[0]", _validator.isValidResourceNames(_policy, _failures, _serviceDef));

		additionalResources.add(_utils.createPolicyResourceMap(policyResourceMap_bad));
		Assert.assertFalse("valid resources and invalid additionalResources[1]", _validator.isValidResourceNames(_policy, _failures, _serviceDef));
	}

	@Test
	public final void test_isValidServiceWithZone_happyPath() throws Exception{
		boolean isAdmin = true;
		when(_policy.getId()).thenReturn(1L);
		when(_policy.getName()).thenReturn("my-all");
		when(_policy.getService()).thenReturn("hdfssvc");
		when(_policy.getZoneName()).thenReturn("zone1");
		when(_policy.getResources()).thenReturn(null);
		when(_policy.getIsAuditEnabled()).thenReturn(Boolean.TRUE);
		when(_policy.getIsEnabled()).thenReturn(Boolean.FALSE);
		RangerService service = new RangerService();
		service.setType("service-type");
		service.setId(2L);
		Action action = Action.CREATE;
		List<String> tagSvcList = new ArrayList<String>();
		tagSvcList.add("hdfssvc");
		when(_store.getServiceByName("hdfssvc")).thenReturn(service);
		RangerSecurityZone securityZone = new RangerSecurityZone();
		securityZone.setName("zone1");
		securityZone.setId(1L);
		securityZone.setTagServices(tagSvcList);
		when(_store.getSecurityZone("zone1")).thenReturn(securityZone);
		when(_store.getPolicyId(2L, "my-all", 1L)).thenReturn(null);
		RangerServiceDef svcDef = new RangerServiceDef();
		svcDef.setName("my-svc-def");
		when(_store.getServiceDefByName("service-type")).thenReturn(svcDef);
		RangerPolicyResourceSignature policySignature = mock(RangerPolicyResourceSignature.class);
		when(_factory.createPolicyResourceSignature(_policy)).thenReturn(policySignature);
		Assert.assertTrue(_validator.isValid(_policy, action, isAdmin, _failures));
	}

	@Test
	public final void test_isValidServiceWithZone_failurePath() throws Exception{
		boolean isAdmin = true;
		when(_policy.getId()).thenReturn(1L);
		when(_policy.getName()).thenReturn("my-all");
		when(_policy.getService()).thenReturn("hdfssvc1");
		when(_policy.getZoneName()).thenReturn("zone1");
		when(_policy.getResources()).thenReturn(null);
		when(_policy.getIsAuditEnabled()).thenReturn(Boolean.TRUE);
		when(_policy.getIsEnabled()).thenReturn(Boolean.FALSE);
		RangerService service = new RangerService();
		service.setType("service-type");
		service.setId(2L);
		Action action = Action.CREATE;
		List<String> tagSvcList = new ArrayList<String>();
		tagSvcList.add("hdfssvc");
		when(_store.getServiceByName("hdfssvc1")).thenReturn(service);
		RangerSecurityZone securityZone = new RangerSecurityZone();
		securityZone.setName("zone1");
		securityZone.setId(1L);
		securityZone.setTagServices(tagSvcList);
		when(_store.getSecurityZone("zone1")).thenReturn(securityZone);
		when(_store.getPolicyId(2L, "my-all", 1L)).thenReturn(null);
		RangerServiceDef svcDef = new RangerServiceDef();
		svcDef.setName("my-svc-def");
		when(_store.getServiceDefByName("service-type")).thenReturn(svcDef);
		RangerPolicyResourceSignature policySignature = mock(RangerPolicyResourceSignature.class);
		when(_factory.createPolicyResourceSignature(_policy)).thenReturn(policySignature);
		boolean isValid = _validator.isValid(_policy, action, isAdmin, _failures);
		Assert.assertFalse(isValid);
		Assert.assertEquals(_failures.get(0)._errorCode, 3048);
		Assert.assertEquals(_failures.get(0)._reason,"Service name = hdfssvc1 is not associated to Zone name = zone1");
	}
	
	private ValidationTestUtils _utils = new ValidationTestUtils();
	private List<ValidationFailureDetails> _failures = new ArrayList<ValidationFailureDetails>();
	private ServiceStore _store;
	private RangerPolicy _policy;
	private RangerPolicyValidator _validator;
	private RangerServiceDef _serviceDef;
	private RangerObjectFactory _factory;
	private Long _zoneId = RangerSecurityZone.RANGER_UNZONED_SECURITY_ZONE_ID;
}
