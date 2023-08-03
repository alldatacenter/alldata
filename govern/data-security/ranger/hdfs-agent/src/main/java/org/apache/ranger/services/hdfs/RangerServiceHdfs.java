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
package org.apache.ranger.services.hdfs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer;
import org.apache.ranger.plugin.client.HadoopException;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.validation.RangerServiceDefHelper;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.resourcematcher.RangerAbstractResourceMatcher;
import org.apache.ranger.plugin.resourcematcher.RangerPathResourceMatcher;
import org.apache.ranger.plugin.service.RangerBaseService;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.services.hdfs.client.HdfsResourceMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerServiceHdfs extends RangerBaseService {

    private static final Logger LOG                     = LoggerFactory.getLogger(RangerServiceHdfs.class);
    private static final String AUDITTOHDFS_KMS_PATH    = "/ranger/audit/kms";
    private static final String AUDITTOHDFS_POLICY_NAME = "kms-audit-path";
    public static final String  ACCESS_TYPE_READ        = "read";

    private static final String HBASE_ARCHIVE_POLICY_NAME = "hbase-archive";
    private static final String HBASE_ARCHIVE_POLICY_PATH = "/hbase/archive";
    private static final String HBASE_ARCHIVE_POLICY_DESC = "Policy for hbase archive location";

	public RangerServiceHdfs() {
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
			LOG.debug("<== RangerServiceHdfs.validateConfig Service: (" + serviceName + " )");
		}
		
		if ( configs != null) {
			try  {
				ret = HdfsResourceMgr.connectionTest(serviceName, configs);
			} catch (HadoopException e) {
				LOG.error("<== RangerServiceHdfs.validateConfig Error: " + e.getMessage(),e);
				throw e;
			}
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceHdfs.validateConfig Response : (" + ret + " )");
		}
		
		return ret;
	}

	@Override
	public List<String> lookupResource(ResourceLookupContext context) throws Exception {
		List<String> ret = new ArrayList<String>();
		String 	serviceName  	   = getServiceName();
		String	serviceType		   = getServiceType();
		Map<String,String> configs = getConfigs();
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceHdfs.lookupResource Context: (" + context + ")");
		}
		
		if (context != null) {
			try {
				ret  = HdfsResourceMgr.getHdfsResources(serviceName, serviceType, configs,context);
			} catch (Exception e) {
			  LOG.error( "<==RangerServiceHdfs.lookupResource Error : " + e);
			  throw e;
			}
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceHdfs.lookupResource Response: (" + ret + ")");
		}
		
		return ret;
	}

	@Override
	public List<RangerPolicy> getDefaultRangerPolicies() throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceHdfs.getDefaultRangerPolicies() ");
		}

		List<RangerPolicy> ret = super.getDefaultRangerPolicies();

		String pathResourceName = RangerHdfsAuthorizer.KEY_RESOURCE_PATH;

		for (RangerPolicy defaultPolicy : ret) {
			if(defaultPolicy.getName().contains("all")){
				if (StringUtils.isNotBlank(lookUpUser)) {
					RangerPolicyItem policyItemForLookupUser = new RangerPolicyItem();
					policyItemForLookupUser.setUsers(Collections.singletonList(lookUpUser));
					policyItemForLookupUser.setAccesses(Collections.singletonList(new RangerPolicyItemAccess(ACCESS_TYPE_READ)));
					policyItemForLookupUser.setDelegateAdmin(false);
					defaultPolicy.getPolicyItems().add(policyItemForLookupUser);
				}

				RangerPolicy.RangerPolicyResource pathPolicyResource = defaultPolicy.getResources().get(pathResourceName);
				if (pathPolicyResource != null) {
					List<RangerServiceDef.RangerResourceDef> resourceDefs = serviceDef.getResources();
					RangerServiceDef.RangerResourceDef pathResourceDef = null;
					for (RangerServiceDef.RangerResourceDef resourceDef : resourceDefs) {
						if (resourceDef.getName().equals(pathResourceName)) {
							pathResourceDef = resourceDef;
							break;
						}
					}
					if (pathResourceDef != null) {
						String pathSeparator = pathResourceDef.getMatcherOptions().get(RangerPathResourceMatcher.OPTION_PATH_SEPARATOR);
						if (StringUtils.isBlank(pathSeparator)) {
							pathSeparator = Character.toString(RangerPathResourceMatcher.DEFAULT_PATH_SEPARATOR_CHAR);
						}
						String value = pathSeparator + RangerAbstractResourceMatcher.WILDCARD_ASTERISK;
						pathPolicyResource.setValue(value);
					} else {
						LOG.warn("No resourceDef found in HDFS service-definition for '" + pathResourceName + "'");
					}
				} else {
					LOG.warn("No '" + pathResourceName + "' found in default policy");
				}
			}
		}

        try {
            RangerServiceDefHelper serviceDefHelper = new RangerServiceDefHelper(serviceDef);
            for (List<RangerServiceDef.RangerResourceDef> aHierarchy : serviceDefHelper.filterHierarchies_containsOnlyMandatoryResources(RangerPolicy.POLICY_TYPE_ACCESS)) {
                // we need to create one policy for keyadmin user for audit to HDFS
                RangerPolicy policy = getPolicyForKMSAudit(aHierarchy);
                if (policy != null) {
                    ret.add(policy);
                }

                // default policy for hbase user to have access on archive location
                RangerPolicy hbaseArchivePolicy = getPolicyForHBaseArchive(aHierarchy);
                if (hbaseArchivePolicy != null) {
                    ret.add(hbaseArchivePolicy);
                }
            }
        } catch (Exception e) {
            LOG.error("Error creating policy for keyadmin for audit to HDFS : " + service.getName(), e);
        }

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceHdfs.getDefaultRangerPolicies() : " + ret);
		}
		return ret;
	}

	private RangerPolicy getPolicyForKMSAudit(List<RangerServiceDef.RangerResourceDef> resourceHierarchy) throws Exception {

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceHdfs.getPolicyForKMSAudit()");
		}

		RangerPolicy policy = new RangerPolicy();

		policy.setIsEnabled(true);
		policy.setVersion(1L);
		policy.setName(AUDITTOHDFS_POLICY_NAME);
		policy.setService(service.getName());
		policy.setDescription("Policy for " + AUDITTOHDFS_POLICY_NAME);
		policy.setIsAuditEnabled(true);
		policy.setResources(createPathBasedResourceMap(resourceHierarchy, AUDITTOHDFS_KMS_PATH));

		List<RangerPolicy.RangerPolicyItem> policyItems = new ArrayList<RangerPolicy.RangerPolicyItem>();
		//Create policy item for keyadmin
		RangerPolicy.RangerPolicyItem policyItem = new RangerPolicy.RangerPolicyItem();
		List<String> userKeyAdmin = new ArrayList<String>();
		userKeyAdmin.add("keyadmin");
		policyItem.setUsers(userKeyAdmin);
		policyItem.setAccesses(getAllowedAccesses(policy.getResources()));
		policyItem.setDelegateAdmin(false);

		policyItems.add(policyItem);
		policy.setPolicyItems(policyItems);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceHdfs.getPolicyForKMSAudit()" + policy);
		}

		return policy;
	}

    private RangerPolicy getPolicyForHBaseArchive(List<RangerServiceDef.RangerResourceDef> resourceHierarchy) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerServiceHdfs.getPolicyForHBaseArchive()");
        }

        RangerPolicy policy = new RangerPolicy();

        policy.setIsEnabled(true);
        policy.setVersion(1L);
        policy.setName(HBASE_ARCHIVE_POLICY_NAME);
        policy.setService(service.getName());
        policy.setDescription(HBASE_ARCHIVE_POLICY_DESC);
        policy.setIsAuditEnabled(true);
        policy.setResources(createPathBasedResourceMap(resourceHierarchy, HBASE_ARCHIVE_POLICY_PATH));

        List<RangerPolicy.RangerPolicyItem> policyItems = new ArrayList<RangerPolicy.RangerPolicyItem>();

        // create policy item
        RangerPolicy.RangerPolicyItem policyItem = new RangerPolicy.RangerPolicyItem();
        List<String>                  user       = new ArrayList<String>();
        user.add("hbase");
        policyItem.setUsers(user);

        policyItem.setAccesses(getAllowedAccesses(policy.getResources()));
        policyItem.setDelegateAdmin(false);

        policyItems.add(policyItem);
        policy.setPolicyItems(policyItems);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerServiceHdfs.getPolicyForHBaseArchive(): ret=" + policy);
        }
        return policy;
    }

    private Map<String, RangerPolicy.RangerPolicyResource> createPathBasedResourceMap(List<RangerServiceDef.RangerResourceDef> resourceHierarchy, String resourcePath) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerServiceHdfs.createPathBasedResourceMap()");
        }

        Map<String, RangerPolicy.RangerPolicyResource> ret          = super.createDefaultPolicyResource(resourceHierarchy);
        RangerPolicy.RangerPolicyResource              pathResource = ret.get(RangerHdfsAuthorizer.KEY_RESOURCE_PATH);

        if (pathResource != null) {
            pathResource.setValue(resourcePath);
        } else {
            LOG.error("Internal error: Could not find RangerPolicyResource corresponding to " + RangerHdfsAuthorizer.KEY_RESOURCE_PATH + " in default policy-resource");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerServiceHdfs.createPathBasedResourceMap(): ret="+ret);
        }

        return ret;
    }
}


