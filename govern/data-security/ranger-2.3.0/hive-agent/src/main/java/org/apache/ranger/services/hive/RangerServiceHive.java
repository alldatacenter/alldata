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
package org.apache.ranger.services.hive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ranger.plugin.client.HadoopException;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.service.RangerBaseService;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.services.hive.client.HiveResourceMgr;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerServiceHive extends RangerBaseService {

	private static final Logger LOG = LoggerFactory.getLogger(RangerServiceHive.class);

	public static final String RESOURCE_DATABASE  = "database";
	public static final String RESOURCE_TABLE     = "table";
	public static final String RESOURCE_UDF       = "udf";
	public static final String RESOURCE_COLUMN    = "column";
	public static final String ACCESS_TYPE_CREATE = "create";
	public static final String ACCESS_TYPE_SELECT = "select";
	public static final String ACCESS_TYPE_READ  = "read";
	public static final String ACCESS_TYPE_ALL    = "all";
	public static final String WILDCARD_ASTERISK  = "*";

	public static final String HIVE_DB_DEFAULT   		        = "default";
	public static final String HIVE_DB_INFOMATION_SCHEMA        = "information_schema";
	public static final String DEFAULT_DB_POLICYNAME 		    = "default database tables columns";
	public static final String INFORMATION_SCHEMA_DB_POLICYNAME = "Information_schema database tables columns";



	public RangerServiceHive() {
		super();
	}

	@Override
	public void init(RangerServiceDef serviceDef, RangerService service) {
		super.init(serviceDef, service);
	}

	@Override
	public Map<String,Object> validateConfig() throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		String 	serviceName  	    = getServiceName();
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceHive.validateConfig Service: (" + serviceName + " )");
		}
		if ( configs != null) {
			try  {
				ret = HiveResourceMgr.connectionTest(serviceName, configs);
			} catch (HadoopException e) {
				LOG.error("<== RangerServiceHive.validateConfig Error:" + e);
				throw e;
			}
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceHive.validateConfig Response : (" + ret + " )");
		}
		return ret;
	}

	@Override
	public List<String> lookupResource(ResourceLookupContext context) throws Exception {

		List<String> ret 		   = new ArrayList<String>();
		String 	serviceName  	   = getServiceName();
		String	serviceType		   = getServiceType();
		Map<String,String> configs = getConfigs();
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceHive.lookupResource Context: (" + context + ")");
		}
		if (context != null) {
			try {
				ret  = HiveResourceMgr.getHiveResources(serviceName, serviceType, configs,context);
			} catch (Exception e) {
				LOG.error( "<==RangerServiceHive.lookupResource Error : " + e);
				throw e;
			}
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceHive.lookupResource Response: (" + ret + ")");
		}
		return ret;
	}

	@Override
	public List<RangerPolicy> getDefaultRangerPolicies() throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceHive.getDefaultRangerPolicies()");
		}

		List<RangerPolicy> ret = super.getDefaultRangerPolicies();

		for (RangerPolicy defaultPolicy : ret) {
			final Map<String, RangerPolicyResource> policyResources = defaultPolicy.getResources();

			if (defaultPolicy.getName().contains("all") && StringUtils.isNotBlank(lookUpUser)) {
				RangerPolicyItem policyItemForLookupUser = new RangerPolicyItem();
				policyItemForLookupUser.setUsers(Collections.singletonList(lookUpUser));
				policyItemForLookupUser.setAccesses(Collections.singletonList(new RangerPolicyItemAccess(ACCESS_TYPE_READ)));
				policyItemForLookupUser.setDelegateAdmin(false);
				defaultPolicy.getPolicyItems().add(policyItemForLookupUser);
			}

			if (policyResources.size() == 1 && hasWildcardAsteriskResource(policyResources, RESOURCE_DATABASE)) { // policy for all databases
				RangerPolicyItem policyItemPublic = new RangerPolicyItem();

				policyItemPublic.setGroups(Collections.singletonList(RangerPolicyEngine.GROUP_PUBLIC));
				policyItemPublic.setAccesses(Collections.singletonList(new RangerPolicyItemAccess(ACCESS_TYPE_CREATE)));

				RangerPolicyItem policyItemOwner = new RangerPolicyItem();

				policyItemOwner.setUsers(Collections.singletonList(RangerPolicyEngine.RESOURCE_OWNER));
				policyItemOwner.setAccesses(Collections.singletonList(new RangerPolicyItemAccess(ACCESS_TYPE_ALL)));
				policyItemOwner.setDelegateAdmin(true);

				defaultPolicy.getPolicyItems().add(policyItemPublic);
				defaultPolicy.getPolicyItems().add(policyItemOwner);
			} else if ((policyResources.size() == 2 && hasWildcardAsteriskResource(policyResources, RESOURCE_DATABASE, RESOURCE_TABLE)) ||                  // policy for all tables
					(policyResources.size() == 2 && hasWildcardAsteriskResource(policyResources, RESOURCE_DATABASE, RESOURCE_UDF))   ||                  // policy for all UDFs
					(policyResources.size() == 3 && hasWildcardAsteriskResource(policyResources, RESOURCE_DATABASE, RESOURCE_TABLE, RESOURCE_COLUMN))) { // policy for all columns
				RangerPolicyItem policyItemOwner = new RangerPolicyItem();

				policyItemOwner.setUsers(Collections.singletonList(RangerPolicyEngine.RESOURCE_OWNER));
				policyItemOwner.setAccesses(Collections.singletonList(new RangerPolicyItemAccess(ACCESS_TYPE_ALL)));
				policyItemOwner.setDelegateAdmin(true);

				defaultPolicy.getPolicyItems().add(policyItemOwner);
			}
		}

		//Policy for default db
		RangerPolicy defaultDBPolicy = createDefaultDBPolicy();
		ret.add(defaultDBPolicy);

		// Policy for information_schema db
		RangerPolicy informationSchemaPolicy = createInformationSchemaPolicy();
		ret.add(informationSchemaPolicy);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceHive.getDefaultRangerPolicies()");
		}

		return ret;
	}

	private boolean hasWildcardAsteriskResource(Map<String, RangerPolicyResource> policyResources, String... resourceNames) {
		for (String resourceName : resourceNames) {
			RangerPolicyResource resource = policyResources.get(resourceName);
			List<String>         values   = resource != null ? resource.getValues() : null;

			if (values == null || !values.contains(WILDCARD_ASTERISK)) {
				return false;
			}
		}
		return true;
	}

	private RangerPolicy createDefaultDBPolicy() {
		RangerPolicy defaultDBPolicy = new RangerPolicy();

		defaultDBPolicy.setName(DEFAULT_DB_POLICYNAME);
		defaultDBPolicy.setService(serviceName);
		defaultDBPolicy.setResources(createDefaultDBPolicyResource());
		defaultDBPolicy.setPolicyItems(createDefaultDBPolicyItem());

		return defaultDBPolicy;
	}

	private Map<String, RangerPolicyResource> createDefaultDBPolicyResource() {
		Map<String, RangerPolicyResource> resources = new HashMap<>();

		resources.put(RESOURCE_DATABASE, new RangerPolicyResource(Arrays.asList(HIVE_DB_DEFAULT), false, false));
		resources.put(RESOURCE_TABLE, new RangerPolicyResource(WILDCARD_ASTERISK));
		resources.put(RESOURCE_COLUMN, new RangerPolicyResource(WILDCARD_ASTERISK));

		return resources;
	}

	private List<RangerPolicyItem> createDefaultDBPolicyItem() {
		List<RangerPolicyItemAccess> accesses = new ArrayList<RangerPolicyItemAccess>();

		accesses.add(new RangerPolicyItemAccess(ACCESS_TYPE_CREATE));

		RangerPolicyItem item = new RangerPolicyItem(accesses, null, Arrays.asList(RangerPolicyEngine.GROUP_PUBLIC), null, null, false);

		return Collections.singletonList(item);
	}

	private RangerPolicy createInformationSchemaPolicy() {
		RangerPolicy informationSchemaPolicy = new RangerPolicy();

		informationSchemaPolicy.setName(INFORMATION_SCHEMA_DB_POLICYNAME);
		informationSchemaPolicy.setService(serviceName);
		informationSchemaPolicy.setResources(createInformationSchemaPolicyResource());
		informationSchemaPolicy.setPolicyItems(createInformationSchemaPolicyItem());

		return informationSchemaPolicy;
	}

	private Map<String, RangerPolicyResource> createInformationSchemaPolicyResource() {
		Map<String, RangerPolicyResource> resources = new HashMap<>();

		resources.put(RESOURCE_DATABASE, new RangerPolicyResource(Arrays.asList(HIVE_DB_INFOMATION_SCHEMA), false, false));
		resources.put(RESOURCE_TABLE, new RangerPolicyResource(WILDCARD_ASTERISK));
		resources.put(RESOURCE_COLUMN, new RangerPolicyResource(WILDCARD_ASTERISK));

		return resources;
	}

	private List<RangerPolicyItem> createInformationSchemaPolicyItem() {
		List<RangerPolicyItemAccess> accesses = new ArrayList<RangerPolicyItemAccess>();

		accesses.add(new RangerPolicyItemAccess(ACCESS_TYPE_SELECT));
		RangerPolicyItem item = new RangerPolicyItem(accesses, null, Arrays.asList(RangerPolicyEngine.GROUP_PUBLIC), null, null, false);

		return Collections.singletonList(item);
	}
}

