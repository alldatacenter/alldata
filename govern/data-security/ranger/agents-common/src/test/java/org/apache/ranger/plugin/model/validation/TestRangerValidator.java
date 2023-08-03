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


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.plugin.model.validation.RangerValidator.Action;
import org.apache.ranger.plugin.store.ServiceStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestRangerValidator {

	static class RangerValidatorForTest extends RangerValidator {

		public RangerValidatorForTest(ServiceStore store) {
			super(store);
		}
		
		boolean isValid(String behavior) {
			return "valid".equals(behavior);
		}
	}
	
	@Before
	public void before() {
		_store = mock(ServiceStore.class);
		_validator = new RangerValidatorForTest(_store);
		_failures = new ArrayList<>();
	}

	@Test
	public void test_ctor_firewalling() {
		try {
			// service store can't be null during construction
			new RangerValidatorForTest(null);
			Assert.fail("Should have thrown exception!");
		} catch (IllegalArgumentException e) {
			// expected exception
		}
	}
	
	@Test
	public void test_validate() {
		// default implementation should fail.  This is abstract class.  Sub-class must do something sensible with isValid
		try {
			_validator.validate(1L, Action.CREATE);
			Assert.fail("Should have thrown exception!");
		} catch (Exception e) {
			// ok expected exception
			String message = e.getMessage();
			Assert.assertTrue(message.contains("internal error"));
		}
	}

	@Test
	public void test_getServiceConfigParameters() {
		// reasonable protection against null values
		Set<String> parameters = _validator.getServiceConfigParameters(null);
		Assert.assertNotNull(parameters);
		Assert.assertTrue(parameters.isEmpty());
		
		RangerService service = mock(RangerService.class);
		when(service.getConfigs()).thenReturn(null);
		parameters = _validator.getServiceConfigParameters(service);
		Assert.assertNotNull(parameters);
		Assert.assertTrue(parameters.isEmpty());
		
		when(service.getConfigs()).thenReturn(new HashMap<String, String>());
		parameters = _validator.getServiceConfigParameters(service);
		Assert.assertNotNull(parameters);
		Assert.assertTrue(parameters.isEmpty());

		String[] keys = new String[] { "a", "b", "c" };
		Map<String, String> map = _utils.createMap(keys);
		when(service.getConfigs()).thenReturn(map);
		parameters = _validator.getServiceConfigParameters(service);
		for (String key: keys) {
			Assert.assertTrue("key", parameters.contains(key));
		}
	}
	
	@Test
	public void test_getRequiredParameters() {
		// reasonable protection against null things
		Set<String> parameters = _validator.getRequiredParameters(null);
		Assert.assertNotNull(parameters);
		Assert.assertTrue(parameters.isEmpty());

		RangerServiceDef serviceDef = mock(RangerServiceDef.class);
		when(serviceDef.getConfigs()).thenReturn(null);
		parameters = _validator.getRequiredParameters(null);
		Assert.assertNotNull(parameters);
		Assert.assertTrue(parameters.isEmpty());

		List<RangerServiceConfigDef> configs = new ArrayList<>();
		when(serviceDef.getConfigs()).thenReturn(configs);
		parameters = _validator.getRequiredParameters(null);
		Assert.assertNotNull(parameters);
		Assert.assertTrue(parameters.isEmpty());
		
		Object[][] input = new Object[][] {
				{ "param1", false },
				{ "param2", true },
				{ "param3", true },
				{ "param4", false },
		};
		configs = _utils.createServiceConditionDefs(input);
		when(serviceDef.getConfigs()).thenReturn(configs);
		parameters = _validator.getRequiredParameters(serviceDef);
		Assert.assertTrue("result does not contain: param2", parameters.contains("param2"));
		Assert.assertTrue("result does not contain: param3", parameters.contains("param3"));
	}
	
	@Test
	public void test_getServiceDef() {
		try {
			// if service store returns null or throws an exception then service is deemed invalid
			when(_store.getServiceDefByName("return null")).thenReturn(null);
			when(_store.getServiceDefByName("throw")).thenThrow(new Exception());
			RangerServiceDef serviceDef = mock(RangerServiceDef.class);
			when(_store.getServiceDefByName("good-service")).thenReturn(serviceDef);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception during mocking!");
		}
		
		Assert.assertNull(_validator.getServiceDef("return null"));
		Assert.assertNull(_validator.getServiceDef("throw"));
		Assert.assertFalse(_validator.getServiceDef("good-service") == null);
	}

	@Test
	public void test_getPolicy() throws Exception {
		// if service store returns null or throws an exception then return null policy
		when(_store.getPolicy(1L)).thenReturn(null);
		when(_store.getPolicy(2L)).thenThrow(new Exception());
		RangerPolicy policy = mock(RangerPolicy.class);
		when(_store.getPolicy(3L)).thenReturn(policy);
		
		Assert.assertNull(_validator.getPolicy(1L));
		Assert.assertNull(_validator.getPolicy(2L));
		Assert.assertTrue(_validator.getPolicy(3L) != null);
	}
	
	@Test
	public final void test_getPoliciesForResourceSignature() throws Exception {
		// return null if store returns null or throws an exception
		String hexSignature = "aSignature";
		String serviceName = "service-name";
		boolean isPolicyEnabled = true;
		when(_store.getPoliciesByResourceSignature(serviceName, hexSignature, isPolicyEnabled)).thenReturn(null);
		Assert.assertNotNull(_validator.getPoliciesForResourceSignature(serviceName, hexSignature));
		when(_store.getPoliciesByResourceSignature(serviceName, hexSignature, isPolicyEnabled)).thenThrow(new Exception());
		Assert.assertNotNull(_validator.getPoliciesForResourceSignature(serviceName, hexSignature));

		// what ever store returns should come back
		hexSignature = "anotherSignature";
		List<RangerPolicy> policies = new ArrayList<>();
		RangerPolicy policy1 = mock(RangerPolicy.class);
		policies.add(policy1);
		RangerPolicy policy2 = mock(RangerPolicy.class);
		policies.add(policy2);
		when(_store.getPoliciesByResourceSignature(serviceName, hexSignature, isPolicyEnabled)).thenReturn(policies);
		List<RangerPolicy> result = _validator.getPoliciesForResourceSignature(serviceName, hexSignature);
		Assert.assertTrue(result.contains(policy1) && result.contains(policy2));
	}

	@Test
	public void test_getService_byId() throws Exception {
		// if service store returns null or throws an exception then service is deemed invalid
		when(_store.getService(1L)).thenReturn(null);
		when(_store.getService(2L)).thenThrow(new Exception());
		RangerService service = mock(RangerService.class);
		when(_store.getService(3L)).thenReturn(service);
		
		Assert.assertNull(_validator.getService(1L));
		Assert.assertNull(_validator.getService(2L));
		Assert.assertTrue(_validator.getService(3L) != null);
	}

	@Test
	public void test_getService() {
		try {
			// if service store returns null or throws an exception then service is deemed invalid
			when(_store.getServiceByName("return null")).thenReturn(null);
			when(_store.getServiceByName("throw")).thenThrow(new Exception());
			RangerService service = mock(RangerService.class);
			when(_store.getServiceByName("good-service")).thenReturn(service);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception during mocking!");
		}
		
		Assert.assertNull(_validator.getService("return null"));
		Assert.assertNull(_validator.getService("throw"));
		Assert.assertFalse(_validator.getService("good-service") == null);
	}
	
	@Test
	public void test_getAccessTypes() {
		// passing in null service def
		Set<String> accessTypes = _validator.getAccessTypes((RangerServiceDef)null);
		Assert.assertTrue(accessTypes.isEmpty());
		// that has null or empty access type def
		RangerServiceDef serviceDef = mock(RangerServiceDef.class);
		when(serviceDef.getAccessTypes()).thenReturn(null);
		accessTypes = _validator.getAccessTypes(serviceDef);
		Assert.assertTrue(accessTypes.isEmpty());

		List<RangerAccessTypeDef> accessTypeDefs = new ArrayList<>();
		when(serviceDef.getAccessTypes()).thenReturn(accessTypeDefs);
		accessTypes = _validator.getAccessTypes(serviceDef);
		Assert.assertTrue(accessTypes.isEmpty());
		
		// having null accesstypedefs
		accessTypeDefs.add(null);
		accessTypes = _validator.getAccessTypes(serviceDef);
		Assert.assertTrue(accessTypes.isEmpty());
		
		// access type defs with null empty blank names are skipped, spaces within names are preserved
		String[] names = new String[] { null, "", "a", "  ", "b ", "		", " C", "	D	" };
		accessTypeDefs.addAll(_utils.createAccessTypeDefs(names));
		accessTypes = _validator.getAccessTypes(serviceDef);
		Assert.assertEquals(4, accessTypes.size());
		Assert.assertTrue(accessTypes.contains("a"));
		Assert.assertTrue(accessTypes.contains("b "));
		Assert.assertTrue(accessTypes.contains(" C"));
		Assert.assertTrue(accessTypes.contains("	D	"));
	}
	
	@Test
	public void test_getResourceNames() {
		// passing in null service def
		Set<String> accessTypes = _validator.getMandatoryResourceNames((RangerServiceDef)null);
		Assert.assertTrue(accessTypes.isEmpty());
		// that has null or empty access type def
		RangerServiceDef serviceDef = mock(RangerServiceDef.class);
		when(serviceDef.getResources()).thenReturn(null);
		accessTypes = _validator.getMandatoryResourceNames(serviceDef);
		Assert.assertTrue(accessTypes.isEmpty());

		List<RangerResourceDef> resourceDefs = new ArrayList<>();
		when(serviceDef.getResources()).thenReturn(resourceDefs);
		accessTypes = _validator.getMandatoryResourceNames(serviceDef);
		Assert.assertTrue(accessTypes.isEmpty());
		
		// having null accesstypedefs
		resourceDefs.add(null);
		accessTypes = _validator.getMandatoryResourceNames(serviceDef);
		Assert.assertTrue(accessTypes.isEmpty());
		
		// access type defs with null empty blank names are skipped, spaces within names are preserved
		Object[][] data = {
		//  { name,  excludes     recursive    mandatory, reg-exp,       parent-level }
        //	         Supported?,  Supported?,
			{ "a",   null,        null,        true  }, // all good
			null,                                       // this should put a null element in the resource def!
			{ "b",   null,        null,        null  }, // mandatory field is null, i.e. false
			{ "c",   null,        null,        false }, // non-mandatory field false - upper case
			{ "D",   null,        null,        true  }, // resource specified in upper case
			{ "E",   null,        null,        false }, // all good
		};
		resourceDefs.addAll(_utils.createResourceDefs(data));
		accessTypes = _validator.getMandatoryResourceNames(serviceDef);
		Assert.assertEquals(2, accessTypes.size());
		Assert.assertTrue(accessTypes.contains("a"));
		Assert.assertTrue(accessTypes.contains("d")); // name should come back lower case
		
		accessTypes = _validator.getAllResourceNames(serviceDef);
		Assert.assertEquals(5, accessTypes.size());
		Assert.assertTrue(accessTypes.contains("b"));
		Assert.assertTrue(accessTypes.contains("c"));
		Assert.assertTrue(accessTypes.contains("e"));
	}

	@Test
	public void test_getValidationRegExes() {
		// passing in null service def
		Map<String, String> regExMap = _validator.getValidationRegExes((RangerServiceDef)null);
		Assert.assertTrue(regExMap.isEmpty());
		// that has null or empty access type def
		RangerServiceDef serviceDef = mock(RangerServiceDef.class);
		when(serviceDef.getResources()).thenReturn(null);
		regExMap = _validator.getValidationRegExes(serviceDef);
		Assert.assertTrue(regExMap.isEmpty());

		List<RangerResourceDef> resourceDefs = new ArrayList<>();
		when(serviceDef.getResources()).thenReturn(resourceDefs);
		regExMap = _validator.getValidationRegExes(serviceDef);
		Assert.assertTrue(regExMap.isEmpty());
		
		// having null accesstypedefs
		resourceDefs.add(null);
		regExMap = _validator.getValidationRegExes(serviceDef);
		Assert.assertTrue(regExMap.isEmpty());
		
		// access type defs with null empty blank names are skipped, spaces within names are preserved
		String[][] data = {
				{ "a", null },     // null-regex
				null,              // this should put a null element in the resource def!
				{ "b", "regex1" }, // valid
				{ "c", "" },       // empty regex
				{ "d", "regex2" }, // valid
				{ "e", "   " },    // blank regex
				{ "f", "regex3" }, // all good
		};
		resourceDefs.addAll(_utils.createResourceDefsWithRegEx(data));
		regExMap = _validator.getValidationRegExes(serviceDef);
		Assert.assertEquals(3, regExMap.size());
		Assert.assertEquals("regex1", regExMap.get("b"));
		Assert.assertEquals("regex2", regExMap.get("d"));
		Assert.assertEquals("regex3", regExMap.get("f"));
	}

	@Test
	public void test_getIsAuditEnabled() {
		// null policy
		RangerPolicy policy = null;
		boolean result = _validator.getIsAuditEnabled(policy);
		Assert.assertFalse(result);
		// null isAuditEnabled Boolean is supposed to be TRUE!!
		policy = mock(RangerPolicy.class);
		when(policy.getIsAuditEnabled()).thenReturn(null);
		result = _validator.getIsAuditEnabled(policy);
		Assert.assertTrue(result);
		// non-null value
		when(policy.getIsAuditEnabled()).thenReturn(Boolean.FALSE);
		result = _validator.getIsAuditEnabled(policy);
		Assert.assertFalse(result);

		when(policy.getIsAuditEnabled()).thenReturn(Boolean.TRUE);
		result = _validator.getIsAuditEnabled(policy);
		Assert.assertTrue(result);
	}
	
	@Test
	public void test_getServiceDef_byId() throws Exception {
		// if service store returns null or throws an exception then service is deemed invalid
		when(_store.getServiceDef(1L)).thenReturn(null);
		when(_store.getServiceDef(2L)).thenThrow(new Exception());
		RangerServiceDef serviceDef = mock(RangerServiceDef.class);
		when(_store.getServiceDef(3L)).thenReturn(serviceDef);
		
		Assert.assertNull(_validator.getServiceDef(1L));
		Assert.assertNull(_validator.getServiceDef(2L));
		Assert.assertTrue(_validator.getServiceDef(3L) != null);
	}

	@Test
	public void test_getEnumDefaultIndex() {
		RangerEnumDef enumDef = mock(RangerEnumDef.class);
		Assert.assertEquals(-1, _validator.getEnumDefaultIndex(null));
		when(enumDef.getDefaultIndex()).thenReturn(null);
		Assert.assertEquals(0, _validator.getEnumDefaultIndex(enumDef));
		when(enumDef.getDefaultIndex()).thenReturn(-5);
		Assert.assertEquals(-5, _validator.getEnumDefaultIndex(enumDef));
	}
	
	@Test
	public void test_getImpliedGrants() {
		
		// passing in null gets back a null
		Collection<String> result = _validator.getImpliedGrants(null);
		Assert.assertNull(result);
		
		// null or empty implied grant collection gets back an empty collection
		RangerAccessTypeDef accessTypeDef = mock(RangerAccessTypeDef.class);
		when(accessTypeDef.getImpliedGrants()).thenReturn(null);
		result = _validator.getImpliedGrants(accessTypeDef);
		Assert.assertTrue(result.isEmpty());
		
		List<String> impliedGrants = new ArrayList<>();
		when(accessTypeDef.getImpliedGrants()).thenReturn(impliedGrants);
		result = _validator.getImpliedGrants(accessTypeDef);
		Assert.assertTrue(result.isEmpty());

		// null/empty values come back as is
		impliedGrants = Arrays.asList(new String[] { null, "", " ", "		" });
		when(accessTypeDef.getImpliedGrants()).thenReturn(impliedGrants);
		result = _validator.getImpliedGrants(accessTypeDef);
		Assert.assertEquals(4, result.size());
		
		// non-empty values get lower cased
		impliedGrants = Arrays.asList(new String[] { "a", "B", "C	", " d " });
		when(accessTypeDef.getImpliedGrants()).thenReturn(impliedGrants);
		result = _validator.getImpliedGrants(accessTypeDef);
		Assert.assertEquals(4, result.size());
		Assert.assertTrue(result.contains("a"));
		Assert.assertTrue(result.contains("b"));
		Assert.assertTrue(result.contains("c	"));
		Assert.assertTrue(result.contains(" d "));
	}
	
	@Test
	public void test_isValid_string() {
		String fieldName = "value-field-Name";
		String collectionName = "value-collection-Name";
		Set<String> alreadySeen = new HashSet<>();
		// null/empty string value is invalid
		for (String value : new String[] { null, "", "  " }) {
			Assert.assertFalse(_validator.isUnique(value, alreadySeen, fieldName, collectionName, _failures));
			_utils.checkFailureForMissingValue(_failures, fieldName);
		}
		// value should not have been seen so far.
		String value = "blah";
		_failures.clear(); Assert.assertTrue(_validator.isUnique(value, alreadySeen, fieldName, collectionName, _failures));
		Assert.assertTrue(_failures.isEmpty());
		Assert.assertTrue(alreadySeen.contains(value));

		// since "blah" has already been seen doing this test again should fail
		_failures.clear(); Assert.assertFalse(_validator.isUnique(value, alreadySeen, fieldName, collectionName, _failures));
		_utils.checkFailureForSemanticError(_failures, fieldName, value);
		
		// not see check is done in a case-insenstive manner
		value = "bLaH";
		_failures.clear(); Assert.assertFalse(_validator.isUnique(value, alreadySeen, fieldName, collectionName, _failures));
		_utils.checkFailureForSemanticError(_failures, fieldName, value);
	}
	
	@Test
	public void test_isValid_long() {
		String fieldName = "field-Name";
		String collectionName = "field-collection-Name";
		Set<Long> alreadySeen = new HashSet<>();
		Long value = null;
		// null value is invalid
		Assert.assertFalse(_validator.isUnique(value, alreadySeen, fieldName, collectionName, _failures));
		_utils.checkFailureForMissingValue(_failures, fieldName);

		// value should not have been seen so far.
		value = 7L;
		_failures.clear(); Assert.assertTrue(_validator.isUnique(value, alreadySeen, fieldName, collectionName, _failures));
		Assert.assertTrue(_failures.isEmpty());
		Assert.assertTrue(alreadySeen.contains(value));

		// since 7L has already been seen doing this test again should fail
		_failures.clear(); Assert.assertFalse(_validator.isUnique(value, alreadySeen, fieldName, collectionName, _failures));
		_utils.checkFailureForSemanticError(_failures, fieldName, value.toString());
	}
	
	@Test
	public void test_isValid_integer() {
		String fieldName = "field-Name";
		String collectionName = "field-collection-Name";
		Set<Integer> alreadySeen = new HashSet<>();
		Integer value = null;
		// null value is invalid
		Assert.assertFalse(_validator.isUnique(value, alreadySeen, fieldName, collectionName, _failures));
		_utils.checkFailureForMissingValue(_failures, fieldName);

		// value should not have been seen so far.
		value = 49;
		_failures.clear(); Assert.assertTrue(_validator.isUnique(value, alreadySeen, fieldName, collectionName, _failures));
		Assert.assertTrue(_failures.isEmpty());
		Assert.assertTrue(alreadySeen.contains(value));

		// since 7L has already been seen doing this test again should fail
		_failures.clear(); Assert.assertFalse(_validator.isUnique(value, alreadySeen, fieldName, collectionName, _failures));
		_utils.checkFailureForSemanticError(_failures, fieldName, value.toString());
	}
	
	private RangerValidatorForTest _validator;
	private ServiceStore _store;
	private ValidationTestUtils _utils = new ValidationTestUtils();
	private List<ValidationFailureDetails> _failures;
}
