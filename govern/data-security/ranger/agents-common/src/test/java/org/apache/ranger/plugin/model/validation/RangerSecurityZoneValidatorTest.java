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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerSecurityZone.RangerSecurityZoneService;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerContextEnricherDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.plugin.store.SecurityZoneStore;
import org.apache.ranger.plugin.store.ServiceStore;
import org.apache.ranger.plugin.util.SearchFilter;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RangerSecurityZoneValidatorTest {
	ServiceStore _store = mock(ServiceStore.class);
	SecurityZoneStore _securityZoneStore = mock(SecurityZoneStore.class);
	@InjectMocks
	RangerSecurityZoneValidator rangerSecurityZoneValidator = new RangerSecurityZoneValidator(_store, _securityZoneStore);

	@Test
        public void testValidateSecurityZoneForCreate() throws Exception{
                SearchFilter filter = getSerachFilter();
                List<RangerSecurityZone> rangerSecurityZoneList = new ArrayList<RangerSecurityZone>();
		RangerService rangerSvc = getRangerService();
		RangerServiceDef rangerSvcDef = rangerServiceDef();
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
		rangerSecurityZoneList.add(suppliedSecurityZone);
		
		Mockito.when(_store.getSecurityZone("MyZone")).thenReturn(null);
		Mockito.when(_store.getServiceByName("hdfsSvc")).thenReturn(rangerSvc);
		Mockito.when(_store.getServiceDefByName("1")).thenReturn(rangerSvcDef);
		Mockito.when(_securityZoneStore.getSecurityZones(filter)).thenReturn(rangerSecurityZoneList);

		rangerSecurityZoneValidator.validate(suppliedSecurityZone, RangerValidator.Action.CREATE);
		Mockito.verify(_store).getSecurityZone("MyZone");
	}
	
	@Test
        public void testValidateSecurityZoneForUpdate() throws Exception{
                SearchFilter filter = getSerachFilter();
		List<RangerSecurityZone> rangerSecurityZoneList = new ArrayList<RangerSecurityZone>();
		RangerService rangerSvc = getRangerService();
		RangerServiceDef rangerSvcDef = rangerServiceDef();
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
		rangerSecurityZoneList.add(suppliedSecurityZone);
		
		Mockito.when(_store.getSecurityZone(1L)).thenReturn(suppliedSecurityZone);
		Mockito.when(_store.getServiceByName("hdfsSvc")).thenReturn(rangerSvc);
		Mockito.when(_store.getServiceDefByName("1")).thenReturn(rangerSvcDef);
		Mockito.when(_securityZoneStore.getSecurityZones(filter)).thenReturn(rangerSecurityZoneList);
		
		rangerSecurityZoneValidator.validate(suppliedSecurityZone, RangerValidator.Action.UPDATE);
		Mockito.verify(_store, Mockito.atLeastOnce()).getSecurityZone(1L);
	}
	
	
	@Test
        public void testValidateSecurityZoneForDelete() throws Exception{
                List<ValidationFailureDetails> failures = new ArrayList<ValidationFailureDetails>();
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
		
		Mockito.when(_store.getSecurityZone(1L)).thenReturn(suppliedSecurityZone);
		
		rangerSecurityZoneValidator.isValid(1L, RangerValidator.Action.DELETE, failures);
		Mockito.verify(_store).getSecurityZone(1L);
	}
	
	@Test
        public void testValidateSecurityZoneForDeleteThrowsError() throws Exception{
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
		try{
			rangerSecurityZoneValidator.validate(suppliedSecurityZone, RangerValidator.Action.DELETE);
		}catch(IllegalArgumentException ex){
			Assert.assertEquals(ex.getMessage(), "isValid(RangerPolicy, ...) is only supported for create/update");
		}
	}
	
	@Test
        public void testValidateSecurityZoneWithoutNameForCreate() throws Exception{
		RangerSecurityZone suppliedSecurityZone = new RangerSecurityZone();
                suppliedSecurityZone.setName(null);
                try{
                        rangerSecurityZoneValidator.validate(suppliedSecurityZone, RangerValidator.Action.CREATE);
                }catch(Exception ex){
                        Assert.assertEquals(ex.getMessage(), "(0) Validation failure: error code[3035], reason[Internal error: missing field[name]], field[name], subfield[null], type[missing] ");
                }
        }

        @Test
        public void testValidateSecurityZoneForCreateWithExistingNameThrowsError() throws Exception{
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
                RangerSecurityZone existingSecurityZone = getRangerSecurityZone();
		
                Mockito.when(_store.getSecurityZone("MyZone")).thenReturn(existingSecurityZone);
                try{
                        rangerSecurityZoneValidator.validate(suppliedSecurityZone, RangerValidator.Action.CREATE);
                }catch(Exception ex){
                        Assert.assertEquals(ex.getMessage(), "(0) Validation failure: error code[3036], reason[Another security zone already exists for this name: zone-id=[1]]], field[name], subfield[null], type[] ");
                }
        }

        @Test
        public void testValidateSecurityZoneNotExistForUpdateThrowsError() throws Exception {
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
                Mockito.when(_store.getSecurityZone(1L)).thenReturn(null);
                try {
                        rangerSecurityZoneValidator.validate(suppliedSecurityZone,RangerValidator.Action.UPDATE);
                } catch (Exception ex) {
                        Assert.assertEquals(ex.getMessage(),"(0) Validation failure: error code[3037], reason[No security zone found for [1]], field[id], subfield[null], type[] ");
                }
        }

        @Test
        public void testValidateSecurityZoneWitoutServicesAdminUserAdminUserGroupAuditUserAuditUserGroupForCreateThrowsError() throws Exception{
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
                suppliedSecurityZone.setAdminUserGroups(null);
                suppliedSecurityZone.setAdminUsers(null);
                suppliedSecurityZone.setAuditUserGroups(null);
                suppliedSecurityZone.setAuditUsers(null);
                suppliedSecurityZone.setServices(null);
		
		Mockito.when(_store.getSecurityZone("MyZone")).thenReturn(null);
		try {
			rangerSecurityZoneValidator.validate(suppliedSecurityZone,
					RangerValidator.Action.CREATE);
		} catch (Exception ex) {
			Assert.assertEquals(
					ex.getMessage(),
					"(0) Validation failure: error code[3044], reason[No services specified for security-zone:[MyZone]], field[services], subfield[null], type[missing] (1) Validation failure: error code[3038], reason[both users and user-groups collections for the security zone were null/empty], field[security zone admin users/user-groups], subfield[null], type[missing] (2) Validation failure: error code[3038], reason[both users and user-groups collections for the security zone were null/empty], field[security zone audit users/user-groups], subfield[null], type[missing] ");
		}
	}

	@Test
        public void testValidateSecurityZoneWitoutResourcesForCreateThrowsError() throws Exception{
		RangerSecurityZoneService rangerSecurityZoneService = new RangerSecurityZoneService();
		RangerService rangerSvc = getRangerService();
		RangerServiceDef rangerSvcDef = rangerServiceDef();
		Mockito.when(_store.getServiceDefByName("1")).thenReturn(rangerSvcDef);

		Map<String, RangerSecurityZone.RangerSecurityZoneService> map = new HashMap<String, RangerSecurityZone.RangerSecurityZoneService>();
		map.put("hdfsSvc", rangerSecurityZoneService);
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
		suppliedSecurityZone.setServices(map);
		
		Mockito.when(_store.getSecurityZone("MyZone")).thenReturn(null);
		Mockito.when(_store.getServiceByName("hdfsSvc")).thenReturn(rangerSvc);

		try {
			rangerSecurityZoneValidator.validate(suppliedSecurityZone,
					RangerValidator.Action.CREATE);
		} catch (Exception ex) {
			Assert.assertEquals(
					ex.getMessage(),
					"(0) Validation failure: error code[3039], reason[No resources specified for service [hdfsSvc]], field[security zone resources], subfield[null], type[missing] ");
		}
	}
	
	@Test
        public void testValidateSecurityZoneWitoutRangerServiceForCreateThrowsError() throws Exception{
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
		
		Mockito.when(_store.getSecurityZone("MyZone")).thenReturn(null);
		Mockito.when(_store.getServiceByName("hdfsSvc")).thenReturn(null);
		
		try {
			rangerSecurityZoneValidator.validate(suppliedSecurityZone,
					RangerValidator.Action.CREATE);
		} catch (Exception ex) {
			Assert.assertEquals(
					ex.getMessage(),
					"(0) Validation failure: error code[3040], reason[Invalid service [hdfsSvc]], field[security zone resource service-name], subfield[null], type[] ");
		}
	}
	
	@Test
        public void testValidateSecurityZoneWitoutRangerServiceDefForCreateThrowsError() throws Exception{
		RangerService rangerSvc = getRangerService();
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
		
		Mockito.when(_store.getSecurityZone("MyZone")).thenReturn(null);
		Mockito.when(_store.getServiceByName("hdfsSvc")).thenReturn(rangerSvc);
		Mockito.when(_store.getServiceDefByName("1")).thenReturn(null);
		
		try {
			rangerSecurityZoneValidator.validate(suppliedSecurityZone,
					RangerValidator.Action.CREATE);
		} catch (Exception ex) {
			Assert.assertEquals(
					ex.getMessage(),
					"(0) Validation failure: error code[3041], reason[Invalid service-type [1]], field[security zone resource service-type], subfield[null], type[] ");
		}
	}
	
	@Test
        public void testValidateSecurityZoneWitoutRangerServiceDefResourceForCreateThrowsError() throws Exception{
		RangerService rangerSvc = getRangerService();
		RangerServiceDef rangerSvcDef = rangerServiceDef();
		rangerSvcDef.setResources(null);
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();

		Mockito.when(_store.getSecurityZone("MyZone")).thenReturn(null);
		Mockito.when(_store.getServiceByName("hdfsSvc")).thenReturn(rangerSvc);
		Mockito.when(_store.getServiceDefByName("1")).thenReturn(rangerSvcDef);
		
		try {
			rangerSecurityZoneValidator.validate(suppliedSecurityZone,
					RangerValidator.Action.CREATE);
		} catch (Exception ex) {
			Assert.assertEquals(
					ex.getMessage(),
					"(0) Validation failure: error code[3042], reason[Invalid resource hierarchy specified for service:[hdfsSvc], resource-hierarchy:[[hdfs]]], field[security zone resource hierarchy], subfield[null], type[] ");
		}
	}

/*
	@Test
        public void testValidateSecurityZoneWitoutRangerServiceDefResourceValueWildCardCharacterForCreateThrowsError() throws Exception{
		List<String> resourceList = new ArrayList<String>();
		resourceList.add("*");
		HashMap<String, List<String>> resourcesMap = new HashMap<String, List<String>>();
		resourcesMap.put("hdfs", resourceList);
		List<HashMap<String, List<String>>> resources = new ArrayList<HashMap<String,List<String>>>();
		resources.add(resourcesMap);
		RangerService rangerSvc = getRangerService();
		RangerServiceDef rangerSvcDef = rangerServiceDef();
		RangerSecurityZoneService rangerSecurityZoneService = new RangerSecurityZoneService();
		rangerSecurityZoneService.setResources(resources);
		Map<String, RangerSecurityZone.RangerSecurityZoneService> map = new HashMap<String, RangerSecurityZone.RangerSecurityZoneService>();
		map.put("hdfsSvc", rangerSecurityZoneService);
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
		suppliedSecurityZone.setServices(map);
		
		Mockito.when(_store.getSecurityZone("MyZone")).thenReturn(null);
		Mockito.when(_store.getServiceByName("hdfsSvc")).thenReturn(rangerSvc);
		Mockito.when(_store.getServiceDefByName("1")).thenReturn(rangerSvcDef);

		
		try {
			rangerSecurityZoneValidator.validate(suppliedSecurityZone,
					RangerValidator.Action.CREATE);
		} catch (Exception ex) {
			Assert.assertEquals(
					ex.getMessage(),
					"(0) Validation failure: error code[3043], reason[All wildcard values specified for resources for service:[hdfsSvc]], field[security zone resource values], subfield[null], type[] ");
		}
	}
*/

        @Test
        public void testValidateWhileFetchingSecurityZoneForCreateThrowsError() throws Exception{
                 SearchFilter filter = getSerachFilter();
		RangerService rangerSvc = getRangerService();
		
                RangerServiceDef rangerSvcDef = rangerServiceDef();
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
		
		Mockito.when(_store.getSecurityZone("MyZone")).thenReturn(null);
		Mockito.when(_store.getServiceByName("hdfsSvc")).thenReturn(rangerSvc);
		Mockito.when(_store.getServiceDefByName("1")).thenReturn(rangerSvcDef);
		Mockito.when(_securityZoneStore.getSecurityZones(filter)).thenThrow(new NullPointerException());
		try {
			rangerSecurityZoneValidator.validate(suppliedSecurityZone,
					RangerValidator.Action.CREATE);
		} catch (Exception ex) {
			Assert.assertEquals(
					ex.getMessage(),
					"(0) Validation failure: error code[3045], reason[Internal Error:[null]], field[null], subfield[null], type[] ");
		}
	}
	
	@Test
        public void testIsValidSecurityZoneForDeleteWithWrongActionTypeReturnFalse() throws Exception{
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
		List<ValidationFailureDetails> failures = new ArrayList<ValidationFailureDetails>();
		boolean isValid =	rangerSecurityZoneValidator.isValid(suppliedSecurityZone.getName(),
					RangerValidator.Action.UPDATE, failures);
		
		Assert.assertFalse(isValid);
	}
	
	@Test
        public void testIsValidSecurityZoneForDeleteWithoutNameReturnFalse() throws Exception{
		RangerSecurityZone suppliedSecurityZone = new RangerSecurityZone();
                suppliedSecurityZone.setName(null);
		List<ValidationFailureDetails> failures = new ArrayList<ValidationFailureDetails>();
		boolean isValid =	rangerSecurityZoneValidator.isValid(suppliedSecurityZone.getName(),
					RangerValidator.Action.DELETE, failures);
		
		Assert.assertFalse(isValid);
	}

	@Test
        public void testIsValidSecurityZoneForDeleteWithWrongNameReturnFalse() throws Exception{
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
		Mockito.when(_store.getSecurityZone(suppliedSecurityZone.getName())).thenReturn(null);
		List<ValidationFailureDetails> failures = new ArrayList<ValidationFailureDetails>();
		boolean isValid =	rangerSecurityZoneValidator.isValid(suppliedSecurityZone.getName(),
					RangerValidator.Action.DELETE, failures);
		Assert.assertFalse(isValid);
	}

	@Test
        public void testIsValidSecurityZoneIdForDeleteWithWrongActionTypeReturnFalse() throws Exception{
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
		List<ValidationFailureDetails> failures = new ArrayList<ValidationFailureDetails>();
		boolean isValid =	rangerSecurityZoneValidator.isValid(suppliedSecurityZone.getId(),
					RangerValidator.Action.UPDATE, failures);
		Assert.assertFalse(isValid);
	}
	
	
	@Test
        public void testIsValidSecurityZoneForDeleteWithWrongIdReturnFalse() throws Exception{
                RangerSecurityZone suppliedSecurityZone = getRangerSecurityZone();
		Mockito.when(_store.getSecurityZone(suppliedSecurityZone.getId())).thenReturn(null);
		List<ValidationFailureDetails> failures = new ArrayList<ValidationFailureDetails>();
		boolean isValid =	rangerSecurityZoneValidator.isValid(suppliedSecurityZone.getId(),
					RangerValidator.Action.DELETE, failures);
		
		Assert.assertFalse(isValid);
	}

	
	
	private RangerService getRangerService() {
		Map<String, String> configs = new HashMap<String, String>();
		configs.put("username", "servicemgr");
		configs.put("password", "servicemgr");
		configs.put("namenode", "servicemgr");
		configs.put("hadoop.security.authorization", "No");
		configs.put("hadoop.security.authentication", "Simple");
		configs.put("hadoop.security.auth_to_local", "");
		configs.put("dfs.datanode.kerberos.principal", "");
		configs.put("dfs.namenode.kerberos.principal", "");
		configs.put("dfs.secondary.namenode.kerberos.principal", "");
		configs.put("hadoop.rpc.protection", "Privacy");
		configs.put("commonNameForCertificate", "");

		RangerService rangerService = new RangerService();
		rangerService.setId(1L);
		rangerService.setConfigs(configs);
		rangerService.setCreateTime(new Date());
		rangerService.setDescription("service policy");
		rangerService.setGuid("1427365526516_835_0");
		rangerService.setIsEnabled(true);
		rangerService.setName("hdfsSvc");
		rangerService.setPolicyUpdateTime(new Date());
		rangerService.setType("1");
		rangerService.setUpdatedBy("Admin");
		rangerService.setUpdateTime(new Date());

		return rangerService;
	}
	
	private RangerServiceDef rangerServiceDef() {
		
		RangerResourceDef rangerResourceDef = new RangerResourceDef();
		rangerResourceDef.setName("hdfs");
		
		List<RangerServiceConfigDef> configs = new ArrayList<RangerServiceConfigDef>();
		List<RangerResourceDef> resources = new ArrayList<RangerResourceDef>();
		resources.add(rangerResourceDef);
		List<RangerAccessTypeDef> accessTypes = new ArrayList<RangerAccessTypeDef>();
		List<RangerPolicyConditionDef> policyConditions = new ArrayList<RangerPolicyConditionDef>();
		List<RangerContextEnricherDef> contextEnrichers = new ArrayList<RangerContextEnricherDef>();
		List<RangerEnumDef> enums = new ArrayList<RangerEnumDef>();

		RangerServiceDef rangerServiceDef = new RangerServiceDef();
		rangerServiceDef.setId(1L);
		rangerServiceDef.setImplClass("RangerServiceHdfs");
		rangerServiceDef.setName("HDFS Repository");
		rangerServiceDef.setLabel("HDFS Repository");
		rangerServiceDef.setDescription("HDFS Repository");
		rangerServiceDef.setRbKeyDescription(null);
		rangerServiceDef.setUpdatedBy("Admin");
		rangerServiceDef.setUpdateTime(new Date());
		rangerServiceDef.setConfigs(configs);
		rangerServiceDef.setResources(resources);
		rangerServiceDef.setAccessTypes(accessTypes);
		rangerServiceDef.setPolicyConditions(policyConditions);
		rangerServiceDef.setContextEnrichers(contextEnrichers);
		rangerServiceDef.setEnums(enums);

		return rangerServiceDef;
	}

        private RangerSecurityZone getRangerSecurityZone(){
                List<String> resourceList = new ArrayList<String>();
                resourceList.add("/path/myfolder");

                HashMap<String, List<String>> resourcesMap = new HashMap<String, List<String>>();
                resourcesMap.put("hdfs", resourceList);

                List<HashMap<String, List<String>>> resources = new ArrayList<HashMap<String,List<String>>>();
                resources.add(resourcesMap);

                List<String> adminUsers = new ArrayList<String>();
                adminUsers.add("adminUser1");

                List<String> adminGrpUsers = new ArrayList<String>();
                adminGrpUsers.add("adminGrpUser1");

                List<String> aduitUsers = new ArrayList<String>();
                aduitUsers.add("aduitUser1");

                List<String> aduitGrpUsers = new ArrayList<String>();
                aduitUsers.add("aduitGrpUser1");

                RangerSecurityZoneService rangerSecurityZoneService = new RangerSecurityZoneService();
                rangerSecurityZoneService.setResources(resources);
                Map<String, RangerSecurityZone.RangerSecurityZoneService> map = new HashMap<String, RangerSecurityZone.RangerSecurityZoneService>();
                map.put("hdfsSvc", rangerSecurityZoneService);

                RangerSecurityZone rangerSecurityZone = new RangerSecurityZone();
                rangerSecurityZone.setId(1L);
                rangerSecurityZone.setAdminUsers(adminUsers);
                rangerSecurityZone.setAuditUsers(aduitUsers);
                rangerSecurityZone.setAdminUserGroups(adminGrpUsers);
                rangerSecurityZone.setAuditUserGroups(aduitGrpUsers);
                rangerSecurityZone.setName("MyZone");
                rangerSecurityZone.setServices(map);
                rangerSecurityZone.setDescription("MyZone");


                return rangerSecurityZone;
        }

        private SearchFilter getSerachFilter(){
                 SearchFilter filter = new SearchFilter();

         filter.setParam(SearchFilter.SERVICE_NAME, "hdfsSvc");
         filter.setParam(SearchFilter.ZONE_NAME, "MyZone");

         return filter;
        }

}
