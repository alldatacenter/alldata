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
package org.apache.ranger.patch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.biz.SecurityZoneDBStore;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXAccessTypeDefDao;
import org.apache.ranger.db.XXAccessTypeDefGrantsDao;
import org.apache.ranger.entity.XXAccessTypeDef;
import org.apache.ranger.entity.XXAccessTypeDefGrants;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerSecurityZone.RangerSecurityZoneService;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.plugin.model.validation.RangerServiceDefValidator;
import org.apache.ranger.plugin.model.validation.RangerValidator.Action;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PatchForSolrSvcDefAndPoliciesUpdate_J10055 extends BaseLoader {
    private static final Logger logger             = LoggerFactory.getLogger(PatchForSolrSvcDefAndPoliciesUpdate_J10055.class);
    private static final String ACCESS_TYPE_UPDATE = "update";
    private static final String ACCESS_TYPE_QUERY  = "query";
    private static final String ACCESS_TYPE_ADMIN  = "solr_admin";
    private static final String ACCESS_TYPE_OTHERS = "others";

    //TAG type solr:permissions
    private static final String ACCESS_TYPE_UPDATE_TAG = "solr:update";
    private static final String ACCESS_TYPE_QUERY_TAG  = "solr:query";
    private static final String ACCESS_TYPE_ADMIN_TAG  = "solr:solr_admin";
    private static final String ACCESS_TYPE_OTHERS_TAG = "solr:others";
    private enum NEW_RESOURCE { admin, config, schema }

    private static final String SVC_ACCESS_TYPE_CONFIG_SUFFIX = "accessTypes";

    private static final String     SOLR_SVC_DEF_NAME      = EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_SOLR_NAME;
    private static RangerServiceDef embeddedSolrServiceDef = null;

    @Autowired
    private RangerDaoManager daoMgr;

    @Autowired
    ServiceDBStore svcDBStore;

    @Autowired
    private SecurityZoneDBStore secZoneDBStore;

    @Autowired
    private RangerValidatorFactory validatorFactory;

	@Autowired
	@Qualifier(value = "transactionManager")
	PlatformTransactionManager txManager;

    public static void main(String[] args) {
        logger.info("main()");
        try {
            PatchForSolrSvcDefAndPoliciesUpdate_J10055 loader = (PatchForSolrSvcDefAndPoliciesUpdate_J10055) CLIUtil.getBean(PatchForSolrSvcDefAndPoliciesUpdate_J10055.class);
            loader.init();
            while (loader.isMoreToProcess()) {
                loader.load();
            }
            logger.info("Load complete. Exiting!!!");
            System.exit(0);
        } catch (Exception e) {
            logger.error("Error loading", e);
            System.exit(1);
        }
    }

    @Override
    public void init() throws Exception {
        // DO NOTHING
    }

    @Override
    public void printStats() {
        logger.info("PatchForSolrSvcDefAndPoliciesUpdate_J10055 logs ");
    }

    @Override
    public void execLoad() {
        logger.info("==> PatchForSolrSvcDefAndPoliciesUpdate_J10055.execLoad()");
        try {
            embeddedSolrServiceDef = EmbeddedServiceDefsUtil.instance().getEmbeddedServiceDef(SOLR_SVC_DEF_NAME);
            if(embeddedSolrServiceDef == null) {
                logger.error("The embedded Solr service-definition does not exist.");
                System.exit(1);
            }

			TransactionTemplate txTemplate = new TransactionTemplate(txManager);
			txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			try {
				txTemplate.execute(new TransactionCallback<Object>() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						if (updateSolrSvcDef() == null) {
							throw new RuntimeException("Error while updating " + SOLR_SVC_DEF_NAME + " service-def");
						}
						return null;
					}
				});
			} catch (Throwable ex) {
				logger.error("Error while updating " + SOLR_SVC_DEF_NAME + " service-def");
				throw new RuntimeException("Error while updating " + SOLR_SVC_DEF_NAME + " service-def");
			}

			final Long resTypeSvcDefId = embeddedSolrServiceDef.getId();
			final Long tagSvcDefId = EmbeddedServiceDefsUtil.instance().getTagServiceDefId();
			updateExistingRangerResPolicy(resTypeSvcDefId);
			updateExistingRangerTagPolicies(tagSvcDefId);

			deleteOldAccessTypeRefs(resTypeSvcDefId);
			deleteOldAccessTypeRefs(tagSvcDefId);
        } catch (Exception e) {
            logger.error("Error whille executing PatchForSolrSvcDefAndPoliciesUpdate_J10055, Error - ", e);
            System.exit(1);
        }

		try {
			// For RANGER-3725 - Update atlas default audit filter
			updateDefaultAuditFilter(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME);
		} catch (Throwable t) {
			logger.error("Failed to update atlas default audit filter - ", t);
			System.exit(1);
		}

		logger.info("<== PatchForSolrSvcDefAndPoliciesUpdate_J10055.execLoad()");
    }

	private void updateExistingRangerResPolicy(Long svcDefId) throws Exception {
		logger.info("<== PatchForSolrSvcDefAndPoliciesUpdate_J10055.updateExistingRangerResPolicy(...)");
		List<XXService> dbServices = daoMgr.getXXService().findByServiceDefId(svcDefId);
		if (CollectionUtils.isNotEmpty(dbServices)) {
			for (XXService dbService : dbServices) {
				SearchFilter filter = new SearchFilter();
				filter.setParam(SearchFilter.SERVICE_NAME, dbService.getName());
				filter.setParam(SearchFilter.FETCH_ZONE_UNZONE_POLICIES, "true");
				updateResPolicies(svcDBStore.getServicePolicies(dbService.getId(), filter));
				updateZoneResourceMapping(dbService);
				updateServiceConfig(dbService);
			}
		}
		logger.info("<== PatchForSolrSvcDefAndPoliciesUpdate_J10055.updateExistingRangerResPolicy(...)");
	}

	private void updateZoneResourceMapping(final XXService solrDBSvc) throws Exception {
		logger.info("==> PatchForSolrSvcDefAndPoliciesUpdate_J10055.updateZoneResourceMapping(...)");
		// Update Zone Resource Mapping For Solr Services
		final String svcName = solrDBSvc.getName();
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.SERVICE_NAME, svcName);
		List<RangerSecurityZone> secZoneList = this.secZoneDBStore.getSecurityZones(filter);
		long index = 1;
		for (RangerSecurityZone secZone : secZoneList) {
			logger.info("updateZoneResourceMapping() processing: [" + index + "/" + secZoneList.size() + "]");
			TransactionTemplate txTemplate = new TransactionTemplate(txManager);
			txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			try {
				txTemplate.execute(new TransactionCallback<Object>() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						try {
							updateZone(secZone, svcName);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
						return null;
					}
				});
			} catch (Throwable ex) {
				logger.error("updateZoneResourceMapping(): Failed to update zone: " + secZone.getName() + " ", ex);
				throw new RuntimeException(ex);
			}
			index++;
		}
		logger.info("<== PatchForSolrSvcDefAndPoliciesUpdate_J10055.updateZoneResourceMapping(...)");
	}

	private void updateZone(RangerSecurityZone secZone, String svcName) throws Exception {
		RangerSecurityZoneService secZoneSvc = secZone.getServices().get(svcName);// get secZoneSvc only for this svcName
		List<HashMap<String, List<String>>> solrZoneSvcResourcesMapList = secZoneSvc.getResources();

		final Set<HashMap<String, List<String>>> updatedResMapSet = new HashSet<HashMap<String, List<String>>>();
		for (HashMap<String, List<String>> existingResMap : solrZoneSvcResourcesMapList) {
			boolean isAllResource = false; // *
			for (Map.Entry<String, List<String>> resNameValueListMap : existingResMap.entrySet()) {

				updatedResMapSet.add(existingResMap);
				final List<String> resourceValueList = resNameValueListMap.getValue();

				if (CollectionUtils.isNotEmpty(resourceValueList) && resourceValueList.indexOf("*") >= 0) {
					updatedResMapSet.clear();
					updatedResMapSet.add(existingResMap);
					isAllResource = true;
					break;
				} else {
					HashMap<String, List<String>> updatedResMap = new HashMap<String, List<String>>();
					updatedResMap.put(NEW_RESOURCE.schema.name(), resourceValueList);
					updatedResMapSet.add(updatedResMap);
				}
			}

			if (isAllResource) {
				final List<String> allResVal = Arrays.asList("*");
				for (NEW_RESOURCE newRes : NEW_RESOURCE.values()) {
					HashMap<String, List<String>> updatedResMap = new HashMap<String, List<String>>();
					updatedResMap.put(newRes.name(), allResVal);
					updatedResMapSet.add(updatedResMap);
				}
				secZoneSvc.setResources(new ArrayList<HashMap<String, List<String>>>(updatedResMapSet));
				break;
			}
			secZoneSvc.setResources(new ArrayList<HashMap<String, List<String>>>(updatedResMapSet));
		}
		this.secZoneDBStore.updateSecurityZoneById(secZone);
	}

	private void updateExistingRangerTagPolicies(Long svcDefId) throws Exception {
		List<XXService> dbServices = daoMgr.getXXService().findByServiceDefId(svcDefId);
		if (CollectionUtils.isNotEmpty(dbServices)) {
			for (XXService dbService : dbServices) {
				SearchFilter filter = new SearchFilter();
				filter.setParam(SearchFilter.SERVICE_NAME, dbService.getName());
				updateTagPolicies(svcDBStore.getServicePolicies(dbService.getId(), filter));
			}
		}
	}

	private void updateTagPolicies(List<RangerPolicy> tagServicePolicies) {
		if (CollectionUtils.isNotEmpty(tagServicePolicies)) {
			long index = 1;
			for (RangerPolicy exPolicy : tagServicePolicies) {
				logger.info("updateTagPolicies() processing: [" + index + "/" + tagServicePolicies.size() + "]");
				TransactionTemplate txTemplate = new TransactionTemplate(txManager);
				txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				try {
					txTemplate.execute(new TransactionCallback<Object>() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							updateTagPolicyItemAccess(exPolicy.getPolicyItems());
							updateTagPolicyItemAccess(exPolicy.getAllowExceptions());
							updateTagPolicyItemAccess(exPolicy.getDenyPolicyItems());
							updateTagPolicyItemAccess(exPolicy.getDenyExceptions());
							try {
								svcDBStore.updatePolicy(exPolicy);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
							return null;
						}
					});
				} catch (Throwable ex) {
					logger.error("updateTagPolicies(): Failed to update policy: " + exPolicy.getName() + " ", ex);
					throw new RuntimeException(ex);
				}
				index++;
			}
		}
	}

	private void updateResPolicies(List<RangerPolicy> policies) {
		if (CollectionUtils.isNotEmpty(policies)) {
			long index = 1;
			for (RangerPolicy exPolicy : policies) {
				logger.info("updateResPolicies() processing: [" + index + "/" + policies.size() + "]");
				TransactionTemplate txTemplate = new TransactionTemplate(txManager);
				txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				try {
					txTemplate.execute(new TransactionCallback<Object>() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							createOrUpdatePolicy(exPolicy);
							return null;
						}
					});
				} catch (Throwable ex) {
					logger.error("updateResPolicies(): Failed to create/update policy: " + exPolicy.getName() + " ", ex);
					throw new RuntimeException(ex);
				}
				index++;
			}
		}
	}

	private void createOrUpdatePolicy(RangerPolicy exPolicy) {
		// Filter policy items which are eligible for admin,config and schema resources
		final List<RangerPolicy.RangerPolicyItem> filteredAllowPolciyItems = filterPolicyItemsForAdminPermission(exPolicy.getPolicyItems());
		final List<RangerPolicy.RangerPolicyItem> filteredAllowExcpPolItems = filterPolicyItemsForAdminPermission(exPolicy.getAllowExceptions());
		final List<RangerPolicy.RangerPolicyItem> filteredDenyPolItems = filterPolicyItemsForAdminPermission(exPolicy.getDenyPolicyItems());
		final List<RangerPolicy.RangerPolicyItem> filteredDenyExcpPolItems = filterPolicyItemsForAdminPermission(exPolicy.getDenyExceptions());

		// check if there is a need to create additional policies with
		// admin/config/schema resource(s)
		final boolean splitPolicy = (filteredAllowPolciyItems.size() > 0 || filteredAllowExcpPolItems.size() > 0 || filteredDenyPolItems.size() > 0 || filteredDenyExcpPolItems.size() > 0);
		if (splitPolicy) {
			RangerPolicy newPolicyForNewResource = new RangerPolicy();
			newPolicyForNewResource.setService(exPolicy.getService());
			newPolicyForNewResource.setServiceType(exPolicy.getServiceType());
			newPolicyForNewResource.setPolicyPriority(exPolicy.getPolicyPriority());

			RangerPolicyResource newRes = new RangerPolicyResource();
			boolean isAllResources = false;
			// Only one entry expected
			for (Map.Entry<String, RangerPolicyResource> entry : exPolicy.getResources().entrySet()) {
				RangerPolicyResource exPolRes = entry.getValue();
				newRes.setIsExcludes(exPolRes.getIsExcludes());
				newRes.setIsRecursive(exPolRes.getIsRecursive());
				newRes.setValues(exPolRes.getValues());
				if (CollectionUtils.isNotEmpty(exPolRes.getValues()) && exPolRes.getValues().indexOf("*") >= 0) {
					isAllResources = true;
				}
			}

			newPolicyForNewResource.setPolicyItems(filteredAllowPolciyItems);
			newPolicyForNewResource.setAllowExceptions(filteredAllowExcpPolItems);
			newPolicyForNewResource.setDenyPolicyItems(filteredDenyPolItems);
			newPolicyForNewResource.setDenyExceptions(filteredDenyExcpPolItems);
			newPolicyForNewResource.setOptions(exPolicy.getOptions());
			newPolicyForNewResource.setValiditySchedules(exPolicy.getValiditySchedules());
			newPolicyForNewResource.setPolicyLabels(exPolicy.getPolicyLabels());
			newPolicyForNewResource.setConditions(exPolicy.getConditions());
			newPolicyForNewResource.setIsDenyAllElse(exPolicy.getIsDenyAllElse());
			newPolicyForNewResource.setZoneName(exPolicy.getZoneName());

			try {
				if (isAllResources) {
					for (NEW_RESOURCE resType : NEW_RESOURCE.values()) {
						createNewPolicy(resType.name(), newPolicyForNewResource, newRes, exPolicy.getName());
					}
				} else {
					createNewPolicy(NEW_RESOURCE.schema.name(), newPolicyForNewResource, newRes, exPolicy.getName());
				}

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		try {
			// update policy items
			updateResPolicyItemAccess(exPolicy.getPolicyItems());
			updateResPolicyItemAccess(exPolicy.getAllowExceptions());
			updateResPolicyItemAccess(exPolicy.getDenyPolicyItems());
			updateResPolicyItemAccess(exPolicy.getDenyExceptions());
			this.svcDBStore.updatePolicy(exPolicy);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

    private void createNewPolicy(final String resType, final RangerPolicy newPolicy, final RangerPolicyResource newRes, final String exPolicyName) throws Exception {
        final String newPolicyName = resType + " - '" + exPolicyName + "'";
        newPolicy.setName(newPolicyName);
        newPolicy.setDescription(newPolicyName);

        final Map<String, RangerPolicy.RangerPolicyResource> resForNewPol = new HashMap<String, RangerPolicy.RangerPolicyResource>();
        resForNewPol.put(resType, newRes);
        newPolicy.setResources(resForNewPol);
        newPolicy.setResourceSignature(null);
        newPolicy.setGuid(null);
        this.svcDBStore.createPolicy(newPolicy);
    }

    private void updateResPolicyItemAccess(List<RangerPolicyItem> policyItems) {
        Set<RangerPolicyItemAccess>         newRangerPolicyItemAccess = new HashSet<RangerPolicyItemAccess>();
        if (CollectionUtils.isNotEmpty(policyItems)) {
            for (RangerPolicyItem exPolicyItem : policyItems) {
                if (exPolicyItem != null) {
                    List<RangerPolicyItemAccess> exPolicyItemAccessList = exPolicyItem.getAccesses();
                    if (CollectionUtils.isNotEmpty(exPolicyItemAccessList)) {
                        newRangerPolicyItemAccess = new HashSet<RangerPolicyItemAccess>();
                        for (RangerPolicyItemAccess aPolicyItemAccess : exPolicyItemAccessList) {
                            if (aPolicyItemAccess != null) {
                                final String  accessType = aPolicyItemAccess.getType();
                                final Boolean isAllowed  = aPolicyItemAccess.getIsAllowed();
                                if (ACCESS_TYPE_ADMIN.equalsIgnoreCase(accessType)) {
                                    newRangerPolicyItemAccess.add(new RangerPolicyItemAccess(ACCESS_TYPE_QUERY, isAllowed));
                                    newRangerPolicyItemAccess.add(new RangerPolicyItemAccess(ACCESS_TYPE_UPDATE, isAllowed));
                                    break;
                                } else if (ACCESS_TYPE_UPDATE.equalsIgnoreCase(accessType)) {
                                    newRangerPolicyItemAccess.add(aPolicyItemAccess);
                                } else if (ACCESS_TYPE_QUERY.equalsIgnoreCase(accessType)) {
                                    newRangerPolicyItemAccess.add(aPolicyItemAccess);
                                } else if (ACCESS_TYPE_OTHERS.equalsIgnoreCase(accessType)) {
                                    newRangerPolicyItemAccess.add(new RangerPolicyItemAccess(ACCESS_TYPE_QUERY, isAllowed));
                                }
                            }
                        }
                        exPolicyItem.setAccesses(new ArrayList<RangerPolicy.RangerPolicyItemAccess>(newRangerPolicyItemAccess));
                    }
                }
            }
        }
    }

    private void updateTagPolicyItemAccess(List<RangerPolicyItem> policyItems) {
        List<RangerPolicy.RangerPolicyItem> newPolicyItems            = new ArrayList<RangerPolicy.RangerPolicyItem>();
        Set<RangerPolicyItemAccess>         newRangerPolicyItemAccess = new HashSet<RangerPolicyItemAccess>();
        if (CollectionUtils.isNotEmpty(policyItems)) {
            for (RangerPolicyItem exPolicyItem : policyItems) {
                if (exPolicyItem != null) {
                    List<RangerPolicyItemAccess> exPolicyItemAccessList = exPolicyItem.getAccesses();
                    if (CollectionUtils.isNotEmpty(exPolicyItemAccessList)) {
                        newRangerPolicyItemAccess = new HashSet<RangerPolicyItemAccess>();
                        for (RangerPolicyItemAccess aPolicyItemAccess : exPolicyItemAccessList) {
                            if (aPolicyItemAccess != null) {
                                final String  accessType = aPolicyItemAccess.getType();
                                final Boolean isAllowed  = aPolicyItemAccess.getIsAllowed();
                                if (ACCESS_TYPE_ADMIN_TAG.equalsIgnoreCase(accessType)) {
                                    newRangerPolicyItemAccess.add(new RangerPolicyItemAccess(ACCESS_TYPE_QUERY_TAG, isAllowed));
                                    newRangerPolicyItemAccess.add(new RangerPolicyItemAccess(ACCESS_TYPE_UPDATE_TAG, isAllowed));
                                } else if (ACCESS_TYPE_UPDATE_TAG.equalsIgnoreCase(accessType)) {
                                    newRangerPolicyItemAccess.add(aPolicyItemAccess);
                                } else if (ACCESS_TYPE_QUERY_TAG.equalsIgnoreCase(accessType)) {
                                    newRangerPolicyItemAccess.add(aPolicyItemAccess);
                                } else if (ACCESS_TYPE_OTHERS_TAG.equalsIgnoreCase(accessType)) {
                                    newRangerPolicyItemAccess.add(new RangerPolicyItemAccess(ACCESS_TYPE_QUERY_TAG, isAllowed));
                                } else {
                                    newRangerPolicyItemAccess.add(aPolicyItemAccess);
                                }
                            }
                        }
                        exPolicyItem.setAccesses(new ArrayList<RangerPolicy.RangerPolicyItemAccess>(newRangerPolicyItemAccess));
                        newPolicyItems.add(exPolicyItem);
                    }
                }
            }
        }
    }

    private List<RangerPolicy.RangerPolicyItem> filterPolicyItemsForAdminPermission(List<RangerPolicy.RangerPolicyItem> policyItems) {
        // Add only those policy items who's access permission list contains 'solr_admin' permission
        List<RangerPolicy.RangerPolicyItem> filteredPolicyItems       = new ArrayList<RangerPolicy.RangerPolicyItem>();
        Set<RangerPolicyItemAccess>         newRangerPolicyItemAccess = new HashSet<RangerPolicyItemAccess>();
        policyItems.forEach(exPolicyItem -> exPolicyItem.getAccesses().forEach(polItemAcc -> {
            if (ACCESS_TYPE_ADMIN.equalsIgnoreCase(polItemAcc.getType())) {
                newRangerPolicyItemAccess.add(new RangerPolicyItemAccess(ACCESS_TYPE_QUERY, polItemAcc.getIsAllowed()));
                newRangerPolicyItemAccess.add(new RangerPolicyItemAccess(ACCESS_TYPE_UPDATE, polItemAcc.getIsAllowed()));
                RangerPolicyItem newPolicyItem = new RangerPolicyItem(new ArrayList<RangerPolicy.RangerPolicyItemAccess>(newRangerPolicyItemAccess), exPolicyItem.getUsers(), exPolicyItem.getGroups(),
                exPolicyItem.getRoles(), exPolicyItem.getConditions(), exPolicyItem.getDelegateAdmin());
                filteredPolicyItems.add(newPolicyItem);
            }
        }));
        return filteredPolicyItems;
    }

    private RangerServiceDef updateSolrSvcDef() {
        logger.info("==> PatchForSolrSvcDefAndPoliciesUpdate_J10055.updateSolrSvcDef()");
        RangerServiceDef                         ret                      = null;
        RangerServiceDef                         embeddedSolrServiceDef   = null;
        XXServiceDef                             xXServiceDefObj          = null;
        RangerServiceDef                         dbSolrServiceDef         = null;
        List<RangerServiceDef.RangerResourceDef> embeddedSolrResourceDefs = null;
        try {
            embeddedSolrServiceDef = EmbeddedServiceDefsUtil.instance().getEmbeddedServiceDef(SOLR_SVC_DEF_NAME);
            if (embeddedSolrServiceDef != null) {
                xXServiceDefObj = daoMgr.getXXServiceDef().findByName(SOLR_SVC_DEF_NAME);
                if (xXServiceDefObj == null) {
                    logger.info(xXServiceDefObj + ": service-def not found. No patching is needed");
                    System.exit(0);
                }

                embeddedSolrResourceDefs = embeddedSolrServiceDef.getResources();                 // ResourcesType
                dbSolrServiceDef         = this.svcDBStore.getServiceDefByName(SOLR_SVC_DEF_NAME);
                dbSolrServiceDef.setResources(embeddedSolrResourceDefs);

                RangerServiceDefValidator validator = validatorFactory.getServiceDefValidator(this.svcDBStore);
                validator.validate(dbSolrServiceDef, Action.UPDATE);
                ret = this.svcDBStore.updateServiceDef(dbSolrServiceDef);
            }
        } catch (Exception e) {
            logger.error("Error while updating " + SOLR_SVC_DEF_NAME + " service-def", e);
            throw new RuntimeException(e);
        }
        logger.info("<== PatchForSolrSvcDefAndPoliciesUpdate_J10055.updateSolrSvcDef()");
        return ret;
    }

    private void deleteOldAccessTypeRefs(Long svcDefId) {
        logger.info("==> PatchForSolrSvcDefAndPoliciesUpdate_J10055.deleteOldAccessTypeRefs(" + svcDefId + ")");
        List<XXAccessTypeDef>    solrAccessDefTypes       = daoMgr.getXXAccessTypeDef().findByServiceDefId(svcDefId);
        XXAccessTypeDefDao       accessTypeDefDao         = daoMgr.getXXAccessTypeDef();
        XXAccessTypeDefGrantsDao xxAccessTypeDefGrantsDao = daoMgr.getXXAccessTypeDefGrants();
        for (XXAccessTypeDef xXAccessTypeDef : solrAccessDefTypes) {
            if (xXAccessTypeDef != null) {
                final String accessTypeName = xXAccessTypeDef.getName();
                final Long   id             = xXAccessTypeDef.getId();  // atd_id in x_access_type_def_grants tbl
                // remove solr_admin refs from implied grants refs tbl
                for (XXAccessTypeDefGrants xXAccessTypeDefGrants : xxAccessTypeDefGrantsDao.findByATDId(id)) {
                    if (xXAccessTypeDefGrants != null) {
                        xxAccessTypeDefGrantsDao.remove(xXAccessTypeDefGrants.getId());
                    }
                }
                // remove no longer supported accessTyeDef's (others,solr_admin, solr:others, solr:solr_admin)
                if (ACCESS_TYPE_ADMIN.equalsIgnoreCase(accessTypeName) || ACCESS_TYPE_OTHERS.equalsIgnoreCase(accessTypeName) || ACCESS_TYPE_OTHERS_TAG.equalsIgnoreCase(accessTypeName)
                || ACCESS_TYPE_ADMIN_TAG.equalsIgnoreCase(accessTypeName)) {
                    accessTypeDefDao.remove(xXAccessTypeDef.getId());
                }
            }
        }
        logger.info("<== PatchForSolrSvcDefAndPoliciesUpdate_J10055.deleteOldAccessTypeRefs(" + svcDefId + ")");
    }

	private void updateServiceConfig(final XXService dbService) throws Exception {
		logger.info("==> PatchForSolrSvcDefAndPoliciesUpdate_J10055.updateServiceConfig()");
		final RangerService rangerSvc = this.svcDBStore.getService(dbService.getId());
		final Map<String, String> configMap = rangerSvc != null ? rangerSvc.getConfigs() : null;
		Set<String> accessTypeSet = new HashSet<String>();

		if (MapUtils.isNotEmpty(configMap)) {
			for (final Map.Entry<String, String> entry : configMap.entrySet()) {
				final String configKey = entry.getKey();
				final String configValue = entry.getValue();
				accessTypeSet = new HashSet<String>();
				if (StringUtils.endsWith(configKey, SVC_ACCESS_TYPE_CONFIG_SUFFIX) && StringUtils.isNotEmpty(configValue)) {
					final String[] accessTypeArray = configValue.split(",");
					for (String access : accessTypeArray) {
						if (!ACCESS_TYPE_OTHERS.equalsIgnoreCase(access) && !ACCESS_TYPE_ADMIN.equalsIgnoreCase(access)) {
							accessTypeSet.add(access);
						} else {
							if (ACCESS_TYPE_ADMIN.equalsIgnoreCase(access)) {
								accessTypeSet.add(ACCESS_TYPE_QUERY);
								accessTypeSet.add(ACCESS_TYPE_UPDATE);
							} else if (ACCESS_TYPE_OTHERS.equalsIgnoreCase(access)) {
								accessTypeSet.add(ACCESS_TYPE_QUERY);
							}
						}
					}
					configMap.put(configKey, StringUtils.join(accessTypeSet, ","));
				}
			}
			rangerSvc.setConfigs(configMap);
			this.svcDBStore.updateService(rangerSvc, null);
		}
		logger.info("<== PatchForSolrSvcDefAndPoliciesUpdate_J10055.updateServiceConfig()");
	}

	private void updateDefaultAuditFilter(final String svcDefName) throws Exception {
		logger.info("==> PatchForSolrSvcDefAndPoliciesUpdate_J10055.updateAtlasDefaultAuditFilter()");
		final RangerServiceDef embeddedAtlasServiceDef = EmbeddedServiceDefsUtil.instance()
				.getEmbeddedServiceDef(svcDefName);
		final List<RangerServiceConfigDef> embdSvcConfDefList = embeddedAtlasServiceDef != null ? embeddedAtlasServiceDef.getConfigs() : new ArrayList<RangerServiceConfigDef>();
		String embdAuditFilterStr = StringUtils.EMPTY;

		if (CollectionUtils.isNotEmpty(embdSvcConfDefList)) {
			for (RangerServiceConfigDef embdSvcConfDef : embdSvcConfDefList) {
				if (StringUtils.equals(embdSvcConfDef.getName(), ServiceDBStore.RANGER_PLUGIN_AUDIT_FILTERS)) {
					embdAuditFilterStr = embdSvcConfDef.getDefaultValue(); // new audit filter str
					break;
				}
			}
		}

		if (StringUtils.isNotEmpty(embdAuditFilterStr)) {
			final RangerServiceDef serviceDbDef = this.svcDBStore.getServiceDefByName(svcDefName);
			for (RangerServiceConfigDef dbSvcDefConfig : serviceDbDef.getConfigs()) {
				if (dbSvcDefConfig != null && StringUtils.equals(dbSvcDefConfig.getName(), ServiceDBStore.RANGER_PLUGIN_AUDIT_FILTERS)) {
					final String dbAuditFilterStr = dbSvcDefConfig.getDefaultValue();
					if (!StringUtils.equalsIgnoreCase(dbAuditFilterStr, embdAuditFilterStr)) {
						dbSvcDefConfig.setDefaultValue(embdAuditFilterStr);
						this.svcDBStore.updateServiceDef(serviceDbDef);
						logger.info("Updated " + serviceDbDef.getName() + " service default audit filter.");
					}
					break;
				}
			}
		}
		logger.info("<== PatchForSolrSvcDefAndPoliciesUpdate_J10055.updateAtlasDefaultAuditFilter()");
	}
}