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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ranger.plugin.errors.ValidationErrorCode;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.plugin.model.validation.RangerValidator.Action;
import org.apache.ranger.plugin.store.ServiceStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestRangerServiceValidator {

	final Action[] cud = new Action[] { Action.CREATE, Action.UPDATE, Action.DELETE };
	final Action[] cu = new Action[] { Action.CREATE, Action.UPDATE };
	final Action[] ud = new Action[] { Action.UPDATE, Action.DELETE };

	@Before
	public void before() {
		_store = mock(ServiceStore.class);
		_action = Action.CREATE; // by default we set action to create
		_validator = new RangerServiceValidator(_store);
	}

	void checkFailure_isValid(RangerServiceValidator validator, RangerService service, Action action, List<ValidationFailureDetails> failures, String errorType, String field) {
		checkFailure_isValid(validator, service, action, failures, errorType, field, null);
	}
	
	void checkFailure_isValid(RangerServiceValidator validator, RangerService service, Action action, List<ValidationFailureDetails> failures, String errorType, String field, String subField) {
		failures.clear();
		Assert.assertFalse(validator.isValid(service, action, failures));
		switch (errorType) {
		case "missing":
			_utils.checkFailureForMissingValue(failures, field, subField);
			break;
		case "semantic":
			_utils.checkFailureForSemanticError(failures, field, subField);
			break;
		case "internal error":
			_utils.checkFailureForInternalError(failures);
			break;
		default:
			Assert.fail("Unsupported errorType[" + errorType + "]");
			break;
		}
	}

	@Test
	public void testIsValidServiceNameCreationWithOutSpecialCharacters() throws Exception{
		String serviceName        = "c1_yarn";
		String serviceDisplayName = serviceName;

		RangerService rangerService = new RangerService();
		rangerService.setName(serviceName);
		rangerService.setDisplayName(serviceDisplayName);
		rangerService.setType("yarn");
		rangerService.setTagService("");

		RangerServiceConfigDef configDef = new RangerServiceConfigDef();
		configDef.setMandatory(true);
		
		List<RangerServiceConfigDef> listRangerServiceConfigDef = new ArrayList<RangerServiceDef.RangerServiceConfigDef>();
		listRangerServiceConfigDef.add(configDef);
		
		
		configDef.setName("myconfig1");
		
		Map<String,String> testMap = new HashMap<String, String>();
		testMap.put("myconfig1", "myconfig1");
		
		rangerService.setConfigs(testMap);
		
		
		RangerServiceDef rangerServiceDef = new RangerServiceDef();
		rangerServiceDef.setConfigs(listRangerServiceConfigDef);
		
		when(_store.getServiceDefByName("yarn")).thenReturn(rangerServiceDef);
		boolean  valid = _validator.isValid(rangerService, Action.CREATE, _failures);
		Assert.assertEquals(0, _failures.size());
		Assert.assertTrue(valid);

	}

	@Test
	public void testIsValidServiceNameUpdationWithOutSpecialCharacters() throws Exception{
		String serviceName = "c1_yarn";
		String serviceDisplayName = serviceName;

		RangerService rangerService = new RangerService();
		rangerService.setId(1L);
		rangerService.setName(serviceName);
		rangerService.setDisplayName(serviceDisplayName);
		rangerService.setType("yarn");
		rangerService.setTagService("");

		RangerServiceConfigDef configDef = new RangerServiceConfigDef();
		configDef.setMandatory(true);
		
		List<RangerServiceConfigDef> listRangerServiceConfigDef = new ArrayList<RangerServiceDef.RangerServiceConfigDef>();
		listRangerServiceConfigDef.add(configDef);
		
		
		configDef.setName("myconfig1");
		
		Map<String,String> testMap = new HashMap<String, String>();
		testMap.put("myconfig1", "myconfig1");
		
		rangerService.setConfigs(testMap);
		
		
		RangerServiceDef rangerServiceDef = new RangerServiceDef();
		rangerServiceDef.setConfigs(listRangerServiceConfigDef);
	
		when(_store.getService(1L)).thenReturn(rangerService);
		when(_store.getServiceDefByName("yarn")).thenReturn(rangerServiceDef);
		boolean  valid = _validator.isValid(rangerService, Action.UPDATE, _failures);
		Assert.assertEquals(0, _failures.size());
		Assert.assertTrue(valid);

	}

	@Test
	public void testIsValidServiceNameUpdationWithSpecialCharacters() throws Exception{
		String serviceName  = "<alert>c1_yarn</alert>";

		ValidationErrorCode vErrCod = ValidationErrorCode.SERVICE_VALIDATION_ERR_SPECIAL_CHARACTERS_SERVICE_NAME;
		String errorMessage         = vErrCod.getMessage(serviceName);
		int errorCode               = vErrCod.getErrorCode();

		RangerService rangerService = new RangerService();
		rangerService.setId(1L);
		rangerService.setName(serviceName);
		rangerService.setType("yarn");
		rangerService.setTagService("");

		RangerServiceConfigDef configDef = new RangerServiceConfigDef();
		configDef.setMandatory(true);
		
		List<RangerServiceConfigDef> listRangerServiceConfigDef = new ArrayList<RangerServiceDef.RangerServiceConfigDef>();
		listRangerServiceConfigDef.add(configDef);
		
		
		configDef.setName("myconfig1");
		
		Map<String,String> testMap = new HashMap<String, String>();
		testMap.put("myconfig1", "myconfig1");
		
		rangerService.setConfigs(testMap);
		
		
		RangerServiceDef rangerServiceDef = new RangerServiceDef();
		rangerServiceDef.setConfigs(listRangerServiceConfigDef);
	
		when(_store.getService(1L)).thenReturn(rangerService);
		when(_store.getServiceDefByName("yarn")).thenReturn(rangerServiceDef);
		boolean  valid = _validator.isValid(rangerService, Action.UPDATE, _failures);
		ValidationFailureDetails failureMessage = _failures.get(0);
		Assert.assertFalse(valid);
		Assert.assertEquals("name",failureMessage.getFieldName());
		Assert.assertEquals(errorMessage, failureMessage._reason);
		Assert.assertEquals(errorCode, failureMessage._errorCode);

	}

	@Test
	public void testIsValidServiceNameCreationWithSpecialCharacters() throws Exception{
		String serviceName = "<script>c1_yarn</script>";

		ValidationErrorCode vErrCod = ValidationErrorCode.SERVICE_VALIDATION_ERR_SPECIAL_CHARACTERS_SERVICE_NAME;
		String errorMessage         = vErrCod.getMessage(serviceName);
		int errorCode               = vErrCod.getErrorCode();

		RangerService rangerService = new RangerService();
		rangerService.setName(serviceName);
		rangerService.setType("yarn");
		rangerService.setTagService("");

		RangerServiceConfigDef configDef = new RangerServiceConfigDef();
		configDef.setMandatory(true);
		
		List<RangerServiceConfigDef> listRangerServiceConfigDef = new ArrayList<RangerServiceDef.RangerServiceConfigDef>();
		listRangerServiceConfigDef.add(configDef);
		
		
		configDef.setName("myconfig1");
		
		Map<String,String> testMap = new HashMap<String, String>();
		testMap.put("myconfig1", "myconfig1");
		
		rangerService.setConfigs(testMap);
		
		
		RangerServiceDef rangerServiceDef = new RangerServiceDef();
		rangerServiceDef.setConfigs(listRangerServiceConfigDef);
		
		when(_store.getServiceDefByName("yarn")).thenReturn(rangerServiceDef);
		boolean  valid = _validator.isValid(rangerService, _action, _failures);
		ValidationFailureDetails failureMessage = _failures.get(0);
		Assert.assertFalse(valid);
		Assert.assertEquals("name",failureMessage.getFieldName());
		Assert.assertEquals(errorMessage, failureMessage._reason);
		Assert.assertEquals(errorCode, failureMessage._errorCode);
	}

	@Test
	public void testIsValidServiceNameCreationWithSpaceCharacter() throws Exception{
		String serviceName  = "Cluster 1_c1_yarn";
		String serviceDisplayName = serviceName;

		ValidationErrorCode vErrCod = ValidationErrorCode.SERVICE_VALIDATION_ERR_SPECIAL_CHARACTERS_SERVICE_NAME;
		String errorMessage         = vErrCod.getMessage(serviceName);
		int errorCode               = vErrCod.getErrorCode();

		RangerService rangerService = new RangerService();
		rangerService.setName(serviceName);
		rangerService.setDisplayName(serviceDisplayName);
		rangerService.setType("yarn");
		rangerService.setTagService("");

		RangerServiceConfigDef configDef = new RangerServiceConfigDef();
		configDef.setMandatory(true);

		List<RangerServiceConfigDef> listRangerServiceConfigDef = new ArrayList<RangerServiceDef.RangerServiceConfigDef>();
		listRangerServiceConfigDef.add(configDef);

		configDef.setName("myconfig1");

		Map<String,String> testMap = new HashMap<String, String>();
		testMap.put("myconfig1", "myconfig1");

		rangerService.setConfigs(testMap);

		RangerServiceDef rangerServiceDef = new RangerServiceDef();
		rangerServiceDef.setConfigs(listRangerServiceConfigDef);

		when(_store.getServiceDefByName("yarn")).thenReturn(rangerServiceDef);
		boolean  valid = _validator.isValid(rangerService, _action, _failures);
		ValidationFailureDetails failureMessage = _failures.get(0);
		Assert.assertFalse(valid);
		Assert.assertEquals("name",failureMessage.getFieldName());
		Assert.assertEquals(errorMessage, failureMessage._reason);
		Assert.assertEquals(errorCode, failureMessage._errorCode);
	}

	@Test
	public void testIsValidServiceNameUpdationWithSpaceCharacter() throws Exception{
		String serviceName  = "Cluster 1_c1_yarn";
		String serviceDisplayName = serviceName;

		ValidationErrorCode vErrCod = ValidationErrorCode.SERVICE_VALIDATION_ERR_SPECIAL_CHARACTERS_SERVICE_NAME;
		String errorMessage         = vErrCod.getMessage(serviceName);
		int errorCode               = vErrCod.getErrorCode();

		RangerService rangerService = new RangerService();
		rangerService.setId(1L);
		rangerService.setName(serviceName);
		rangerService.setDisplayName(serviceDisplayName);
		rangerService.setType("yarn");
		rangerService.setTagService("");

		RangerServiceConfigDef configDef = new RangerServiceConfigDef();
		configDef.setMandatory(true);

		List<RangerServiceConfigDef> listRangerServiceConfigDef = new ArrayList<RangerServiceDef.RangerServiceConfigDef>();
		listRangerServiceConfigDef.add(configDef);

		configDef.setName("myconfig1");

		Map<String,String> testMap = new HashMap<String, String>();
		testMap.put("myconfig1", "myconfig1");

		rangerService.setConfigs(testMap);

		RangerServiceDef rangerServiceDef = new RangerServiceDef();
		rangerServiceDef.setConfigs(listRangerServiceConfigDef);

		String serviceNameWithoutSpace  = "Cluster_1_c1_yarn";
		String serviceDisplayNameWithoutSpace = serviceNameWithoutSpace;
		RangerService rangerServiceWithoutSpace = new RangerService();
		rangerServiceWithoutSpace.setId(1L);
		rangerServiceWithoutSpace.setName(serviceNameWithoutSpace);
		rangerServiceWithoutSpace.setDisplayName(serviceDisplayNameWithoutSpace);
		rangerServiceWithoutSpace.setType("yarn");
		rangerServiceWithoutSpace.setTagService("");

		//Case: previous service name does not have space, updating with name containing space
		when(_store.getService(1L)).thenReturn(rangerServiceWithoutSpace);
		when(_store.getServiceDefByName("yarn")).thenReturn(rangerServiceDef);
		boolean  valid = _validator.isValid(rangerService, Action.UPDATE, _failures);
		ValidationFailureDetails failureMessage = _failures.get(0);
		Assert.assertFalse(valid);
		Assert.assertEquals("name",failureMessage.getFieldName());
		Assert.assertEquals(errorMessage, failureMessage._reason);
		Assert.assertEquals(errorCode, failureMessage._errorCode);

		//Case: previous service name does have space, updating with name containing space
		when(_store.getService(1L)).thenReturn(rangerService);
		when(_store.getServiceDefByName("yarn")).thenReturn(rangerServiceDef);
		boolean  validWithSpace = _validator.isValid(rangerService, Action.UPDATE, _failures);
		Assert.assertTrue(validWithSpace);
	}

	@Test
	public void testIsValidServiceNameCreationWithGreater255Characters() throws Exception{
		String serviceName = "c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1";

		ValidationErrorCode vErrCod = ValidationErrorCode.SERVICE_VALIDATION_ERR_SPECIAL_CHARACTERS_SERVICE_NAME;
		String errorMessage         = vErrCod.getMessage(serviceName);
		int errorCode               = vErrCod.getErrorCode();

		RangerService rangerService = new RangerService();
		rangerService.setName(serviceName);
		rangerService.setType("yarn");
		rangerService.setTagService("");

		RangerServiceConfigDef configDef = new RangerServiceConfigDef();
		configDef.setMandatory(true);
		
		List<RangerServiceConfigDef> listRangerServiceConfigDef = new ArrayList<RangerServiceDef.RangerServiceConfigDef>();
		listRangerServiceConfigDef.add(configDef);
		
		
		configDef.setName("myconfig1");
		
		Map<String,String> testMap = new HashMap<String, String>();
		testMap.put("myconfig1", "myconfig1");
		
		rangerService.setConfigs(testMap);
		
		
		RangerServiceDef rangerServiceDef = new RangerServiceDef();
		rangerServiceDef.setConfigs(listRangerServiceConfigDef);
		
		when(_store.getServiceDefByName("yarn")).thenReturn(rangerServiceDef);
		boolean  valid = _validator.isValid(rangerService, _action, _failures);
		ValidationFailureDetails failureMessage = _failures.get(0);
		Assert.assertFalse(valid);
		Assert.assertEquals("name",failureMessage.getFieldName());
		Assert.assertEquals(errorMessage, failureMessage._reason);
		Assert.assertEquals(errorCode, failureMessage._errorCode);

	}

	@Test
	public void testIsValidServiceNameUpdationWithGreater255Characters() throws Exception{
		String serviceName = "c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1_yarn_c1";

		ValidationErrorCode vErrCod = ValidationErrorCode.SERVICE_VALIDATION_ERR_SPECIAL_CHARACTERS_SERVICE_NAME;
		String errorMessage         = vErrCod.getMessage(serviceName);
		int errorCode               = vErrCod.getErrorCode();

		RangerService rangerService = new RangerService();
		rangerService.setId(1L);
		rangerService.setName(serviceName);
		rangerService.setType("yarn");
		rangerService.setTagService("");

		RangerServiceConfigDef configDef = new RangerServiceConfigDef();
		configDef.setMandatory(true);
		
		List<RangerServiceConfigDef> listRangerServiceConfigDef = new ArrayList<RangerServiceDef.RangerServiceConfigDef>();
		listRangerServiceConfigDef.add(configDef);
		
		
		configDef.setName("myconfig1");
		
		Map<String,String> testMap = new HashMap<String, String>();
		testMap.put("myconfig1", "myconfig1");
		
		rangerService.setConfigs(testMap);
		
		
		RangerServiceDef rangerServiceDef = new RangerServiceDef();
		rangerServiceDef.setConfigs(listRangerServiceConfigDef);
	
		when(_store.getService(1L)).thenReturn(rangerService);
		when(_store.getServiceDefByName("yarn")).thenReturn(rangerServiceDef);
		boolean  valid = _validator.isValid(rangerService, Action.UPDATE, _failures);
		ValidationFailureDetails failureMessage = _failures.get(0);
		Assert.assertFalse(valid);
		Assert.assertEquals("name",failureMessage.getFieldName());
		Assert.assertEquals(errorMessage, failureMessage._reason);
		Assert.assertEquals(errorCode, failureMessage._errorCode);

	}

	@Test
	public void testIsValid_failures() throws Exception {
		RangerService service = mock(RangerService.class);
		// passing in a null service to the check itself is an error
		Assert.assertFalse(_validator.isValid((RangerService)null, _action, _failures));
		_utils.checkFailureForMissingValue(_failures, "service");

		// id is required for update
		when(service.getId()).thenReturn(null);
		// let's verify the failure and the sort of error information that is returned (for one of these)
		// Assert.assert that among the failure reason is one about id being missing.
		checkFailure_isValid(_validator, service, Action.UPDATE, _failures, "missing", "id");
		when(service.getId()).thenReturn(7L);

		for (Action action : cu) {
			// null, empty of blank name renders a service invalid
			for (String name : new String[] { null, "", " 	" }) { // spaces and tabs
				when(service.getName()).thenReturn(name);
				checkFailure_isValid(_validator, service, action, _failures, "missing", "name");
			}
			// same is true for the type
			for (String type : new String[] { null, "", "    " }) {
				when(service.getType()).thenReturn(type);
				checkFailure_isValid(_validator, service, action, _failures, "missing", "type");
			}
		}
		when(service.getName()).thenReturn("aName");

		// if non-empty, then the type should exist!
		when(_store.getServiceDefByName("null-type")).thenReturn(null);
		when(_store.getServiceDefByName("throwing-type")).thenThrow(new Exception());
		for (Action action : cu) {
			for (String type : new String[] { "null-type", "throwing-type" }) {
				when(service.getType()).thenReturn(type);
				checkFailure_isValid(_validator, service, action, _failures, "semantic", "type");
			}
		}
		when(service.getType()).thenReturn("aType");
		RangerServiceDef serviceDef = mock(RangerServiceDef.class);
		when(_store.getServiceDefByName("aType")).thenReturn(serviceDef);
		
		// Create: No service should exist matching its id and/or name
		RangerService anExistingService = mock(RangerService.class);
		when(_store.getServiceByName("aName")).thenReturn(anExistingService);
		checkFailure_isValid(_validator, service, Action.CREATE, _failures, "semantic", "name");

		// Update: service should exist matching its id and name specified should not belong to a different service
		when(_store.getService(7L)).thenReturn(null);
		when(_store.getServiceByName("aName")).thenReturn(anExistingService);
		checkFailure_isValid(_validator, service, Action.UPDATE, _failures, "semantic", "id");

		when(_store.getService(7L)).thenReturn(anExistingService);
		RangerService anotherExistingService = mock(RangerService.class);
		when(anotherExistingService.getId()).thenReturn(49L);
		when(_store.getServiceByName("aName")).thenReturn(anotherExistingService);
		checkFailure_isValid(_validator, service, Action.UPDATE, _failures, "semantic", "id/name");
	}
	
	@Test
	public void test_isValid_missingRequiredParameter() throws Exception {
		// Create/Update: simulate a condition where required parameters are missing
		Object[][] input = new Object[][] {
				{ "param1", true },
				{ "param2", true },
				{ "param3", false },
				{ "param4", false },
		};
		List<RangerServiceConfigDef> configDefs = _utils.createServiceConditionDefs(input);
		RangerServiceDef serviceDef = mock(RangerServiceDef.class);
		when(serviceDef.getConfigs()).thenReturn(configDefs);
		// wire this service def into store
		when(_store.getServiceDefByName("aType")).thenReturn(serviceDef);
		// create a service with some require parameters missing
		RangerService service = mock(RangerService.class);
		when(service.getType()).thenReturn("aType");
		when(service.getName()).thenReturn("aName");
		// required parameters param2 is missing
		String[] params = new String[] { "param1", "param3", "param4", "param5" };
		Map<String, String> paramMap = _utils.createMap(params);
		when(service.getConfigs()).thenReturn(paramMap);
		// service does not exist in the store
		when(_store.getServiceByName("aService")).thenReturn(null);
		for (Action action : cu) {
			// it should be invalid
			checkFailure_isValid(_validator, service, action, _failures, "missing", "configuration", "param2");
		}
	}

	@Test
	public void test_isValid_happyPath() throws Exception {
		// create a service def with some required parameters
		Object[][] serviceDefInput = new Object[][] {
				{ "param1", true },
				{ "param2", true },
				{ "param3", false },
				{ "param4", false },
				{ "param5", true },
		};
		List<RangerServiceConfigDef> configDefs = _utils.createServiceConditionDefs(serviceDefInput);
		RangerServiceDef serviceDef = mock(RangerServiceDef.class);
		when(serviceDef.getConfigs()).thenReturn(configDefs);
		// create a service with some parameters on it
		RangerService service = mock(RangerService.class);
		when(service.getName()).thenReturn("aName");
		when(service.getDisplayName()).thenReturn("aDisplayName");
		when(service.getType()).thenReturn("aType");
		// contains an extra parameter (param6) and one optional is missing(param4)
		String[] configs = new String[] { "param1", "param2", "param3", "param5", "param6" };
		Map<String, String> configMap = _utils.createMap(configs);
		when(service.getConfigs()).thenReturn(configMap);
		// wire then into the store
		// service does not exists
		when(_store.getServiceByName("aName")).thenReturn(null);
		// service def exists
		when(_store.getServiceDefByName("aType")).thenReturn(serviceDef);

		Assert.assertTrue(_validator.isValid(service, Action.CREATE, _failures));

		// for update to work the only additional requirement is that id is required and service should exist
		// if name is not null and it points to a service then it should match the id
		when(service.getId()).thenReturn(7L);
		RangerService existingService = mock(RangerService.class);
		when(existingService.getId()).thenReturn(Long.valueOf(7L));
		when(_store.getService(7L)).thenReturn(existingService);
		when(_store.getServiceByName("aName")).thenReturn(existingService);
		Assert.assertTrue(_validator.isValid(service, Action.UPDATE, _failures));
		// name need not point to a service for update to work, of course.
		when(_store.getServiceByName("aName")).thenReturn(null);
		Assert.assertTrue(_validator.isValid(service, Action.UPDATE, _failures));
	}

	@Test
	public void test_isValid_withId_errorConditions() throws Exception {
		// api that takes in long is only supported for delete currently
		Assert.assertFalse(_validator.isValid(1L, Action.CREATE, _failures));
		_utils.checkFailureForInternalError(_failures);
		// passing in a null id is a failure!
		_validator = new RangerServiceValidator(_store);
		_failures.clear(); Assert.assertFalse(_validator.isValid((Long)null, Action.DELETE, _failures));
		_utils.checkFailureForMissingValue(_failures, "id");
		// if service with that id does not exist then that, is ok because delete is idempotent
		when(_store.getService(1L)).thenReturn(null);
		when(_store.getService(2L)).thenThrow(new Exception());
		_failures.clear(); Assert.assertTrue(_validator.isValid(1L, Action.DELETE, _failures));
		Assert.assertTrue(_failures.isEmpty());

		_failures.clear(); Assert.assertTrue(_validator.isValid(2L, Action.DELETE, _failures));
		Assert.assertTrue(_failures.isEmpty());
	}
	
	@Test
	public void test_isValid_withId_happyPath() throws Exception {
		_validator = new RangerServiceValidator(_store);
		RangerService service = mock(RangerService.class);
		when(_store.getService(1L)).thenReturn(service);
		Assert.assertTrue(_validator.isValid(1L, Action.DELETE, _failures));
	}
	
	private ServiceStore _store;
	private RangerServiceValidator _validator;
	private Action _action;
	private ValidationTestUtils _utils = new ValidationTestUtils();
	private List<ValidationFailureDetails> _failures = new ArrayList<>();
}
