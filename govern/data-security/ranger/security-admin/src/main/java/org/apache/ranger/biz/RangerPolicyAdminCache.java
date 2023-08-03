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

package org.apache.ranger.biz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ranger.authorization.hadoop.config.RangerPluginConfig;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.store.RoleStore;
import org.apache.ranger.plugin.policyengine.RangerPluginContext;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;
import org.apache.ranger.plugin.store.SecurityZoneStore;
import org.apache.ranger.plugin.store.ServiceStore;
import org.apache.ranger.plugin.util.RangerPolicyDeltaUtil;
import org.apache.ranger.plugin.util.RangerRoles;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerPolicyAdminCache {
	
	static class RangerPolicyAdminWrapper {
		final RangerPolicyAdmin policyAdmin;
		final Lock lock = new ReentrantLock();

		RangerPolicyAdminWrapper(RangerPolicyAdmin policyAdmin) {
			this.policyAdmin = policyAdmin;
		}

		RangerPolicyAdmin getPolicyAdmin() {
			return policyAdmin;
		}

		Lock getLock() {
			return lock;
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(RangerPolicyAdminCache.class);

	private final Map<String, RangerPolicyAdminWrapper> policyAdminCache = Collections.synchronizedMap(new HashMap<>());

	final RangerPolicyAdmin getServicePoliciesAdmin(String serviceName, ServiceStore svcStore, RoleStore roleStore, SecurityZoneStore zoneStore, RangerPolicyEngineOptions options) {

		if (serviceName == null || svcStore == null || roleStore == null || zoneStore == null) {
			LOG.warn("Cannot get policy-admin for null serviceName or serviceStore or roleStore or zoneStore");

			return null;
		}

		long        policyVersion;
		long        roleVersion;
		RangerRoles roles;
		boolean     isRolesUpdated = true;

		RangerPolicyAdminWrapper ret = policyAdminCache.get(serviceName);

		try {
			if (ret == null) {
				policyVersion = -1L;
				roleVersion   = -1L;
				roles         = roleStore.getRoles(serviceName, roleVersion);

				if (roles == null) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("There are no roles in ranger-admin for service:" + serviceName + "]");
					}
				}
			} else {
				policyVersion = ret.getPolicyAdmin().getPolicyVersion();
				roleVersion   = ret.getPolicyAdmin().getRoleVersion();
				roles         = roleStore.getRoles(serviceName, roleVersion);

				if (roles == null) { // No changes to roles
					roles          = roleStore.getRoles(serviceName, -1L);
					isRolesUpdated = false;
				}
			}

			ServicePolicies policies = svcStore.getServicePoliciesIfUpdated(serviceName, policyVersion, ServiceDBStore.isSupportsPolicyDeltas());

			if (policies != null) {
				ret = addOrUpdatePolicyAdmin(ret, policies, roles, options);

				if (ret == null) {
					LOG.error("getPolicyAdmin(" + serviceName + "): failed to build engine from policies from service-store");
				} else {
					if (isRolesUpdated) {
						ret.getPolicyAdmin().setRoles(roles);
					}
				}
			}
		} catch (Exception exception) {
			LOG.error("getPolicyAdmin(" + serviceName + "): failed to get latest policies from service-store", exception);
		}

		if (ret == null) {
			LOG.error("Policy-engine is not built! Returning null policy-engine!");
		} else {
			ret.getPolicyAdmin().setServiceStore(svcStore);
		}

		return ret == null ? null : ret.getPolicyAdmin();

	}

	private RangerPolicyAdminWrapper addOrUpdatePolicyAdmin(RangerPolicyAdminWrapper policyAdminWrapper, ServicePolicies policies, RangerRoles roles, RangerPolicyEngineOptions options) {
		final RangerPolicyAdminWrapper ret;

		RangerPolicyAdmin        policyAdmin          = null;
		boolean                  isPolicyEngineShared = false;

		RangerPolicyAdminImpl    oldPolicyAdmin       = policyAdminWrapper == null ? null : (RangerPolicyAdminImpl) policyAdminWrapper.getPolicyAdmin();
		Boolean                  hasPolicyDeltas      = RangerPolicyDeltaUtil.hasPolicyDeltas(policies);

		if (hasPolicyDeltas != null) {
			if (hasPolicyDeltas.equals(Boolean.TRUE)) {
				if (oldPolicyAdmin != null) {
					boolean isLocked = false;

					try {
						policyAdminWrapper.getLock().lockInterruptibly();
						isLocked = true;
					} catch (Exception e) {
						// Ignore
					}

					if (isLocked) {
						try {
							policyAdmin = RangerPolicyAdminImpl.getPolicyAdmin(oldPolicyAdmin, policies);
							if (policyAdmin != null) {
								policyAdmin.setRoles(roles);
								isPolicyEngineShared = true;
							}
						} finally {
							policyAdminWrapper.getLock().unlock();
						}
					}
				} else {
					LOG.error("Old policy engine is null! Cannot apply deltas without old policy engine!");
				}
			} else {
				if (policies.getPolicies() == null) {
					policies.setPolicies(new ArrayList<>());
				}
				policyAdmin = addPolicyAdmin(policies, roles, options);
			}
		} else {
			LOG.warn("Provided policies do not require policy change !! [" + policies + "]. Keeping old policy-engine!");
			policyAdmin = oldPolicyAdmin;
		}

		if (policyAdmin != null) {
			if (LOG.isDebugEnabled()) {
				if (oldPolicyAdmin == null) {
					LOG.debug("Adding policy-engine to cache with serviceName:[" + policies.getServiceName() + "] as key");
				} else {
					LOG.debug("Replacing policy-engine in cache with serviceName:[" + policies.getServiceName() + "] as key");
				}
			}
			ret = new RangerPolicyAdminWrapper(policyAdmin);
			policyAdminCache.put(policies.getServiceName(), ret);
			if (oldPolicyAdmin != null && oldPolicyAdmin != policyAdmin) {
				oldPolicyAdmin.releaseResources(!isPolicyEngineShared);
			}
		} else {
			LOG.warn("Could not build new policy-engine.");
			ret = null;
		}

		return ret;
	}

	private RangerPolicyAdmin addPolicyAdmin(ServicePolicies policies, RangerRoles roles, RangerPolicyEngineOptions options) {
		RangerServiceDef    serviceDef          = policies.getServiceDef();
		String              serviceType         = (serviceDef != null) ? serviceDef.getName() : "";
		RangerPluginContext rangerPluginContext = new RangerPluginContext(new RangerPluginConfig(serviceType, null, "ranger-admin", null, null, options));

		return new RangerPolicyAdminImpl(policies, rangerPluginContext, roles);
	}
}
