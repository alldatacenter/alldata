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

package org.apache.ranger.plugin.store;

import java.util.List;
import java.util.Map;

import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.plugin.util.ServicePolicies;

public interface ServiceStore {

    String OPTION_FORCE_RENAME = "forceRename";

	void init() throws Exception;

	RangerServiceDef createServiceDef(RangerServiceDef serviceDef) throws Exception;

	RangerServiceDef updateServiceDef(RangerServiceDef serviceDef) throws Exception;

	void deleteServiceDef(Long id, Boolean forceDelete) throws Exception;

	void updateTagServiceDefForAccessTypes() throws Exception;

	RangerServiceDef getServiceDef(Long id) throws Exception;

	RangerServiceDef getServiceDefByName(String name) throws Exception;

	RangerServiceDef getServiceDefByDisplayName(String name) throws Exception;

	List<RangerServiceDef> getServiceDefs(SearchFilter filter) throws Exception;

	PList<RangerServiceDef> getPaginatedServiceDefs(SearchFilter filter) throws Exception;

	RangerService createService(RangerService service) throws Exception;

	RangerService updateService(RangerService service, Map<String, Object> options) throws Exception;

	void deleteService(Long id) throws Exception;

	RangerService getService(Long id) throws Exception;

	RangerService getServiceByName(String name) throws Exception;

	RangerService getServiceByDisplayName(String displayName) throws Exception;

	List<RangerService> getServices(SearchFilter filter) throws Exception;

	PList<RangerService> getPaginatedServices(SearchFilter filter) throws Exception;

	RangerPolicy createPolicy(RangerPolicy policy) throws Exception;

	RangerPolicy updatePolicy(RangerPolicy policy) throws Exception;

	void deletePolicy(RangerPolicy policy, RangerService service) throws Exception;

	void deletePolicy(RangerPolicy policy) throws Exception;

	boolean policyExists(Long id) throws Exception;

	RangerPolicy getPolicy(Long id) throws Exception;

	List<RangerPolicy> getPolicies(SearchFilter filter) throws Exception;

	Long getPolicyId(final Long serviceId, final String policyName, final Long zoneId);

	PList<RangerPolicy> getPaginatedPolicies(SearchFilter filter) throws Exception;

	List<RangerPolicy> getPoliciesByResourceSignature(String serviceName, String policySignature, Boolean isPolicyEnabled) throws Exception;

	List<RangerPolicy> getServicePolicies(Long serviceId, SearchFilter filter) throws Exception;

	PList<RangerPolicy> getPaginatedServicePolicies(Long serviceId, SearchFilter filter) throws Exception;

	List<RangerPolicy> getServicePolicies(String serviceName, SearchFilter filter) throws Exception;

	PList<RangerPolicy> getPaginatedServicePolicies(String serviceName, SearchFilter filter) throws Exception;

	ServicePolicies getServicePoliciesIfUpdated(String serviceName, Long lastKnownVersion, boolean needsBackwardCompatibility) throws Exception;

	Long getServicePolicyVersion(String serviceName);

	ServicePolicies getServicePolicyDeltasOrPolicies(String serviceName, Long lastKnownVersion) throws Exception;

	ServicePolicies getServicePolicyDeltas(String serviceName, Long lastKnownVersion) throws Exception;

	ServicePolicies getServicePolicies(String serviceName, Long lastKnownVersion) throws Exception;

	RangerPolicy getPolicyFromEventTime(String eventTimeStr, Long policyId);

	void setPopulateExistingBaseFields(Boolean populateExistingBaseFields);

	Boolean getPopulateExistingBaseFields();

    RangerSecurityZone getSecurityZone(Long id) throws Exception;

    RangerSecurityZone getSecurityZone(String name) throws Exception;

    long getPoliciesCount(final String serviceName);

    Map<String, String> getServiceConfigForPlugin(Long serviceId);
}
