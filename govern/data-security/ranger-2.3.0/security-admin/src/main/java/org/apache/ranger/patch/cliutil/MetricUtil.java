/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ranger.patch.cliutil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

import org.apache.ranger.common.DateUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.biz.AssetMgr;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.biz.XUserMgr;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.patch.BaseLoader;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerContextEnricherDef;
import org.apache.ranger.plugin.store.PList;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.util.CLIUtil;
import org.apache.ranger.util.RestUtil;
import org.apache.ranger.view.VXAccessAuditList;
import org.apache.ranger.view.VXGroupList;
import org.apache.ranger.view.VXMetricContextEnricher;
import org.apache.ranger.view.VXMetricAuditDetailsCount;
import org.apache.ranger.view.VXMetricServiceCount;
import org.apache.ranger.view.VXMetricPolicyCount;
import org.apache.ranger.view.VXMetricUserGroupCount;
import org.apache.ranger.view.VXUserList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Component
public class MetricUtil extends BaseLoader  {
	
	private static final Logger logger = LoggerFactory.getLogger(MetricUtil.class);
	
	public static String metricType;
		
	@Autowired
	XUserMgr xUserMgr;
	
	@Autowired
	AssetMgr assetMgr;
	
	@Autowired
	ServiceDBStore svcStore;
	
	@Autowired
	RangerBizUtil xaBizUtil;
	
	@Autowired
	RESTErrorUtil restErrorUtil;
	
	public static void main(String[] args) {
		/* LOG4J2: TODO
		logger.getRootLogger().setLevel(Level.OFF);
		 */
		logger.info("MetricUtil : main()");
		try {
			MetricUtil loader = (MetricUtil) CLIUtil.getBean(MetricUtil.class);
			loader.init();
			if (args.length != 2) {
				System.out.println("type: Incorrect Arguments usage : -type policies | audits | usergroup | services | database | contextenrichers | denyconditions");
			} else {
				if (!("-type".equalsIgnoreCase(args[0])) || !("policies".equalsIgnoreCase(args[1]) || "audits".equalsIgnoreCase(args[1]) || "usergroup".equalsIgnoreCase(args[1]) || "services".equalsIgnoreCase(args[1]) || "database".equalsIgnoreCase(args[1]) || "contextenrichers".equalsIgnoreCase(args[1]) || "denyconditions".equalsIgnoreCase(args[1]))) {
					System.out.println("type: Incorrect Arguments usage : -type policies | audits | usergroup | services | database | contextenrichers | denyconditions");
				} else {
					metricType = args[1];
					if (logger.isDebugEnabled()) {
						logger.debug("Metric Type : " + metricType);
					}
				}
			}
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
		logger.info("==> MetricUtil.init()");
	}
	
	@Override
	public void execLoad() {
		logger.info("==> MetricUtil.execLoad()");
		metricCalculation(metricType);
		logger.info("<== MetricUtil.execLoad()");
	}

	@Override
	public void printStats() {		
	}
	
	private void metricCalculation(String caseValue) {
		logger.info("Metric Type : " + caseValue);		
		try {
			SearchCriteria searchCriteria = new SearchCriteria();
			searchCriteria.setStartIndex(0);
			searchCriteria.setMaxRows(100);
			searchCriteria.setGetCount(true);
			searchCriteria.setSortType("asc");			
		
			switch (caseValue.toLowerCase()) {
				case "usergroup":
					try {
						VXGroupList vxGroupList = xUserMgr.searchXGroups(searchCriteria);
	
						long groupCount = vxGroupList.getTotalCount();

                        ArrayList<String> userAdminRoleCount = new ArrayList<String>();
                        userAdminRoleCount.add(RangerConstants.ROLE_SYS_ADMIN);
                        long userSysAdminCount = getUserCountBasedOnUserRole(userAdminRoleCount);

                        ArrayList<String> userAdminAuditorRoleCount = new ArrayList<String>();
                        userAdminAuditorRoleCount.add(RangerConstants.ROLE_ADMIN_AUDITOR);
                        long userSysAdminAuditorCount = getUserCountBasedOnUserRole(userAdminAuditorRoleCount);

						ArrayList<String> userRoleListKeyRoleAdmin = new ArrayList<String>();
						userRoleListKeyRoleAdmin.add(RangerConstants.ROLE_KEY_ADMIN);
						long userKeyAdminCount = getUserCountBasedOnUserRole(userRoleListKeyRoleAdmin);

                        ArrayList<String> userRoleListKeyadminAduitorRole = new ArrayList<String>();
                        userRoleListKeyadminAduitorRole.add(RangerConstants.ROLE_KEY_ADMIN_AUDITOR);
                        long userKeyadminAuditorCount = getUserCountBasedOnUserRole(userRoleListKeyadminAduitorRole);

						ArrayList<String> userRoleListUser = new ArrayList<String>();
						userRoleListUser.add(RangerConstants.ROLE_USER);
						long userRoleCount = getUserCountBasedOnUserRole(userRoleListUser);
	
                                                long userTotalCount = userSysAdminCount + userKeyAdminCount + userRoleCount + userKeyadminAuditorCount + userSysAdminAuditorCount;
	
						VXMetricUserGroupCount metricUserGroupCount = new VXMetricUserGroupCount();
						metricUserGroupCount.setUserCountOfUserRole(userRoleCount);
						metricUserGroupCount.setUserCountOfKeyAdminRole(userKeyAdminCount);
						metricUserGroupCount.setUserCountOfSysAdminRole(userSysAdminCount);
                                                metricUserGroupCount.setUserCountOfKeyadminAuditorRole(userKeyadminAuditorCount);
                                                metricUserGroupCount.setUserCountOfSysAdminAuditorRole(userSysAdminAuditorCount);
						metricUserGroupCount.setUserTotalCount(userTotalCount);
						metricUserGroupCount.setGroupCount(groupCount);
						Gson gson = new GsonBuilder().create();
						final String jsonUserGroupCount = gson.toJson(metricUserGroupCount);
						System.out.println(jsonUserGroupCount);
					} catch (Exception e) {
						logger.error("Error calculating Metric for usergroup : " + e.getMessage());
					}
					break;								
				case "audits":
					try{
						 int clientTimeOffsetInMinute = RestUtil.getClientTimeOffset();
						String defaultDateFormat="MM/dd/yyyy";
						DateFormat formatter = new SimpleDateFormat(defaultDateFormat);
						
						VXMetricAuditDetailsCount auditObj = new VXMetricAuditDetailsCount();
						DateUtil dateUtilTwoDays = new DateUtil();
						Date startDateUtilTwoDays = dateUtilTwoDays.getDateFromNow(-2);
						Date dStart2 = restErrorUtil.parseDate(formatter.format(startDateUtilTwoDays),
								"Invalid value for startDate",
								MessageEnums.INVALID_INPUT_DATA, null, "startDate", defaultDateFormat);
						
						Date endDateTwoDays = MiscUtil.getUTCDate();
						Date dEnd2 = restErrorUtil.parseDate(formatter.format(endDateTwoDays),
								"Invalid value for endDate",
								MessageEnums.INVALID_INPUT_DATA, null, "endDate", defaultDateFormat);
						dEnd2 = dateUtilTwoDays.getDateFromGivenDate(dEnd2, 0, 23, 59, 59);
						dEnd2 = dateUtilTwoDays.addTimeOffset(dEnd2, clientTimeOffsetInMinute);
						VXMetricServiceCount deniedCountObj = getAuditsCount(0,dStart2,dEnd2);
						auditObj.setDenialEventsCountTwoDays(deniedCountObj);
						
						VXMetricServiceCount allowedCountObj = getAuditsCount(1,dStart2,dEnd2);
						auditObj.setAccessEventsCountTwoDays(allowedCountObj);
						
						long totalAuditsCountTwoDays = deniedCountObj.getTotalCount() + allowedCountObj.getTotalCount();	
						auditObj.setSolrIndexCountTwoDays(totalAuditsCountTwoDays);
						
						DateUtil dateUtilWeek = new DateUtil();
						Date startDateUtilWeek = dateUtilWeek.getDateFromNow(-7);
						Date dStart7 = restErrorUtil.parseDate(formatter.format(startDateUtilWeek),
								"Invalid value for startDate",
								MessageEnums.INVALID_INPUT_DATA, null, "startDate", defaultDateFormat);
						
						Date endDateWeek = MiscUtil.getUTCDate();
						DateUtil dateUtilweek = new DateUtil();
						Date dEnd7 = restErrorUtil.parseDate(formatter.format(endDateWeek),
								"Invalid value for endDate",
								MessageEnums.INVALID_INPUT_DATA, null, "endDate", defaultDateFormat);
						dEnd7 = dateUtilweek.getDateFromGivenDate(dEnd7,0, 23, 59, 59 );
						dEnd7 = dateUtilweek.addTimeOffset(dEnd7, clientTimeOffsetInMinute);
						VXMetricServiceCount deniedCountObjWeek =  getAuditsCount(0,dStart7,dEnd7);
						auditObj.setDenialEventsCountWeek(deniedCountObjWeek);
						
						VXMetricServiceCount allowedCountObjWeek = getAuditsCount(1,dStart7,dEnd7);
						auditObj.setAccessEventsCountWeek(allowedCountObjWeek);
						
						long totalAuditsCountWeek = deniedCountObjWeek.getTotalCount() + allowedCountObjWeek.getTotalCount();	
						auditObj.setSolrIndexCountWeek(totalAuditsCountWeek);
						
						Gson gson = new GsonBuilder().create();
						final String jsonAudit = gson.toJson(auditObj);
						System.out.println(jsonAudit);
					}catch (Exception e) {
						logger.error("Error calculating Metric for audits : "+e.getMessage());
					}
					break;	
				case "services" : 	
					try {
						SearchFilter serviceFilter = new SearchFilter();
						serviceFilter.setMaxRows(200);
						serviceFilter.setStartIndex(0);
						serviceFilter.setGetCount(true);
						serviceFilter.setSortBy("serviceId");
						serviceFilter.setSortType("asc");
						VXMetricServiceCount vXMetricServiceCount = new VXMetricServiceCount();
						PList<RangerService> paginatedSvcs = svcStore.getPaginatedServices(serviceFilter);
						long totalServiceCount = paginatedSvcs.getTotalCount();
						List<RangerService> rangerServiceList = paginatedSvcs.getList();
						Map<String, Long> services = new HashMap<String, Long>();
						for (Object rangerService : rangerServiceList) {
							RangerService RangerServiceObj = (RangerService) rangerService;
							String serviceName = RangerServiceObj.getType();
							if (!(services.containsKey(serviceName))) {
								serviceFilter.setParam("serviceType", serviceName);
								PList<RangerService> paginatedSvcscount = svcStore.getPaginatedServices(serviceFilter);
								services.put(serviceName, paginatedSvcscount.getTotalCount());
							}
						}
						vXMetricServiceCount.setServiceBasedCountList(services);
						vXMetricServiceCount.setTotalCount(totalServiceCount);
						Gson gson = new GsonBuilder().create();
						final String jsonServices = gson.toJson(vXMetricServiceCount);
						System.out.println(jsonServices);
					} catch (Exception e) {
						logger.error("Error calculating Metric for services : " + e.getMessage());
					}
					break;				
				case "policies" :   
					try {
						SearchFilter policyFilter = new SearchFilter();
						policyFilter.setMaxRows(200);
						policyFilter.setStartIndex(0);
						policyFilter.setGetCount(true);
						policyFilter.setSortBy("serviceId");
						policyFilter.setSortType("asc");
						VXMetricPolicyCount vXMetricPolicyCount = new VXMetricPolicyCount();
						PList<RangerPolicy> paginatedSvcsList = svcStore.getPaginatedPolicies(policyFilter);
						vXMetricPolicyCount.setTotalCount(paginatedSvcsList.getTotalCount());
						Map<String, VXMetricServiceCount> servicesWithPolicy = new HashMap<String, VXMetricServiceCount>();
						for (int k = 2; k >= 0; k--) {
							String policyType = String.valueOf(k);
							VXMetricServiceCount vXMetricServiceCount = getVXMetricServiceCount(policyType);
							if (k == 2) {
								servicesWithPolicy.put("rowFilteringPolicies", vXMetricServiceCount); }
							else if (k == 1) {
								servicesWithPolicy.put("maskingPolicies", vXMetricServiceCount); }
							else if (k == 0) {
								servicesWithPolicy.put("resourceAccessPolicies", vXMetricServiceCount);}
						}
						boolean tagFlag = false;
						if (tagFlag == false) {
							policyFilter.setParam("serviceType", "tag");
							PList<RangerPolicy> policiestype = svcStore.getPaginatedPolicies(policyFilter);
							Map<String, Long> tagMap = new HashMap<String, Long>();
							long tagCount = policiestype.getTotalCount();
							tagMap.put("tag", tagCount);
							VXMetricServiceCount vXMetricServiceCount = new VXMetricServiceCount();
							vXMetricServiceCount.setServiceBasedCountList(tagMap);
							vXMetricServiceCount.setTotalCount(tagCount);
							servicesWithPolicy.put("tagAccessPolicies", vXMetricServiceCount);
							tagFlag = true;
						}
						vXMetricPolicyCount.setPolicyCountList(servicesWithPolicy);
						Gson gson = new GsonBuilder().create();
						final String jsonPolicies = gson.toJson(vXMetricPolicyCount);
						System.out.println(jsonPolicies);
					} catch (Exception e) {
						logger.error("Error calculating Metric for policies : " + e.getMessage());
					}
					break;
				case "database" :
					try {
						int dbFlavor = RangerBizUtil.getDBFlavor();
						String dbFlavourType = "Unknow ";
						if (dbFlavor == AppConstants.DB_FLAVOR_MYSQL) {
							dbFlavourType = "MYSQL ";
						} else if (dbFlavor == AppConstants.DB_FLAVOR_ORACLE) {
							dbFlavourType = "ORACLE ";
						} else if (dbFlavor == AppConstants.DB_FLAVOR_POSTGRES) {
							dbFlavourType = "POSTGRES ";
						} else if (dbFlavor == AppConstants.DB_FLAVOR_SQLANYWHERE) {
							dbFlavourType = "SQLANYWHERE ";
						} else if (dbFlavor == AppConstants.DB_FLAVOR_SQLSERVER) {
							dbFlavourType = "SQLSERVER ";
						}
						String dbDetail = dbFlavourType + xaBizUtil.getDBVersion();
						Gson gson = new GsonBuilder().create();
						final String jsonDBDetail = gson.toJson(dbDetail);
						System.out.println(jsonDBDetail);
					} catch (Exception e) {
						logger.error("Error calculating Metric for database : " + e.getMessage());
					}
					break;
				case "contextenrichers":
					try {
						SearchFilter filter = new SearchFilter();
						filter.setStartIndex(0);
						VXMetricContextEnricher serviceWithContextEnrichers = new VXMetricContextEnricher();
						PList<RangerServiceDef> paginatedSvcDefs = svcStore.getPaginatedServiceDefs(filter);
						List<RangerServiceDef> repoTypeList = paginatedSvcDefs.getList();
						if (repoTypeList != null) {
							for (RangerServiceDef repoType : repoTypeList) {
								RangerServiceDef rangerServiceDefObj = (RangerServiceDef) repoType;
								String name = rangerServiceDefObj.getName();
								List<RangerContextEnricherDef> contextEnrichers = rangerServiceDefObj.getContextEnrichers();
								if (contextEnrichers != null && !contextEnrichers.isEmpty()) {
									serviceWithContextEnrichers.setServiceName(name);
									serviceWithContextEnrichers.setTotalCount(contextEnrichers.size());
								}
							}
						}
						Gson gson = new GsonBuilder().create();
						final String jsonContextEnrichers = gson.toJson(serviceWithContextEnrichers);
						System.out.println(jsonContextEnrichers);
					} catch (Exception e) {
						logger.error("Error calculating Metric for contextenrichers : " + e.getMessage());
					}
					break;
				case "denyconditions":
					try {
						SearchFilter policyFilter1 = new SearchFilter();
						policyFilter1.setMaxRows(200);
						policyFilter1.setStartIndex(0);
						policyFilter1.setGetCount(true);
						policyFilter1.setSortBy("serviceId");
						policyFilter1.setSortType("asc");
	
						int denyCount = 0;
						Map<String, Integer> denyconditionsonMap = new HashMap<String, Integer>();
						PList<RangerServiceDef> paginatedSvcDefs = svcStore.getPaginatedServiceDefs(policyFilter1);
						if (paginatedSvcDefs != null) {
							List<RangerServiceDef> rangerServiceDefs = paginatedSvcDefs.getList();
							if (rangerServiceDefs != null && !rangerServiceDefs.isEmpty()) {
								for (RangerServiceDef rangerServiceDef : rangerServiceDefs) {
									if (rangerServiceDef != null) {
										String serviceDef = rangerServiceDef.getName();
										if (!StringUtils.isEmpty(serviceDef)) {
											policyFilter1.setParam("serviceType", serviceDef);
											policyFilter1.setParam("denyCondition", "true");
											PList<RangerPolicy> policiesList = svcStore.getPaginatedPolicies(policyFilter1);
											if (policiesList != null && policiesList.getListSize() > 0) {
												int policyListCount = policiesList.getListSize();
												if (policyListCount > 0 && policiesList.getList() != null) {
													List<RangerPolicy> policies = policiesList.getList();
													for (RangerPolicy policy : policies) {
														if (policy != null) {
															List<RangerPolicyItem> policyItem = policy.getDenyPolicyItems();
															if (policyItem != null && !policyItem.isEmpty()) {
																if (denyconditionsonMap.get(serviceDef) != null) {
																	denyCount = denyconditionsonMap.get(serviceDef) + denyCount + policyItem.size();
																} else {
																	denyCount = denyCount + policyItem.size();
																}
															}
															List<RangerPolicyItem> policyItemExclude = policy.getDenyExceptions();
															if (policyItemExclude != null && !policyItemExclude.isEmpty()) {
																if (denyconditionsonMap.get(serviceDef) != null) {
																	denyCount = denyconditionsonMap.get(serviceDef) + denyCount + policyItemExclude.size();
																} else {
																	denyCount = denyCount + policyItemExclude.size();
																}
															}
														}
													}
												}
											}
											policyFilter1.removeParam("serviceType");
										}
										denyconditionsonMap.put(serviceDef, denyCount);
										denyCount = 0;
									}
								}
							}
						}
						Gson gson = new GsonBuilder().create();
						String jsonContextDenyCondtionOn = gson.toJson(denyconditionsonMap);
						System.out.println(jsonContextDenyCondtionOn);
					} catch (Exception e) {
						logger.error("Error calculating Metric for denyconditions : " + e.getMessage());
					}
					break;
				default:
					System.out.println("type: Incorrect Arguments usage : -type policies | audits | usergroup | services | database | contextenrichers | denyconditions");
					logger.info("Please enter the valid arguments for Metric Calculation");
					break;
			}
		} catch(Exception e) {
			logger.error("Error calculating Metric : "+e.getMessage());
		}		
	}
	
	private VXMetricServiceCount getVXMetricServiceCount(String policyType)
			throws Exception {
		SearchFilter policyFilter1 = new SearchFilter();
		policyFilter1.setMaxRows(200);
		policyFilter1.setStartIndex(0);
		policyFilter1.setGetCount(true);
		policyFilter1.setSortBy("serviceId");
		policyFilter1.setSortType("asc");
		policyFilter1.setParam("policyType", policyType);
		PList<RangerPolicy> policies = svcStore.getPaginatedPolicies(policyFilter1);
		PList<RangerService> paginatedSvcsSevice = svcStore.getPaginatedServices(policyFilter1);

		List<RangerService> rangerServiceList = paginatedSvcsSevice.getList();

		Map<String, Long> servicesforPolicyType = new HashMap<String, Long>();
		long tagCount = 0;
		for (Object rangerService : rangerServiceList) {
			RangerService rangerServiceObj = (RangerService) rangerService;
			String serviceName = rangerServiceObj.getType();
			if (!(servicesforPolicyType.containsKey(serviceName))) {
				policyFilter1.setParam("serviceType", serviceName);
				PList<RangerPolicy> policiestype = svcStore.getPaginatedPolicies(policyFilter1);
				long count = policiestype.getTotalCount();
				if (count != 0) {
					if (!"tag".equalsIgnoreCase(serviceName)) {
						servicesforPolicyType.put(serviceName, count);
					} else {
						tagCount = count;
					}
				}
			}
		}
		VXMetricServiceCount vXMetricServiceCount = new VXMetricServiceCount();
		vXMetricServiceCount.setServiceBasedCountList(servicesforPolicyType);
		long totalCountOfPolicyType = policies.getTotalCount() - tagCount;
		vXMetricServiceCount.setTotalCount(totalCountOfPolicyType);
		return vXMetricServiceCount;
	}
	
	private VXMetricServiceCount getAuditsCount(int accessResult,
			Date startDate, Date endDate) throws Exception {
		long totalCountOfAudits = 0;
		SearchFilter filter = new SearchFilter();
		filter.setStartIndex(0);
		Map<String, Long> servicesRepoType = new HashMap<String, Long>();
		VXMetricServiceCount vXMetricServiceCount = new VXMetricServiceCount();
		PList<RangerServiceDef> paginatedSvcDefs = svcStore.getPaginatedServiceDefs(filter);
		Iterable<RangerServiceDef> repoTypeGet = paginatedSvcDefs.getList();
		for (Object repo : repoTypeGet) {
			RangerServiceDef rangerServiceDefObj = (RangerServiceDef) repo;
			long id = rangerServiceDefObj.getId();
			String serviceRepoName = rangerServiceDefObj.getName();
			SearchCriteria searchCriteriaWithType = new SearchCriteria();
			searchCriteriaWithType.getParamList().put("repoType", id);
			searchCriteriaWithType.getParamList().put("accessResult", accessResult);
			searchCriteriaWithType.addParam("startDate", startDate);
			searchCriteriaWithType.addParam("endDate", endDate);
			searchCriteriaWithType.setMaxRows(0);
			searchCriteriaWithType.setGetCount(true);
			VXAccessAuditList vXAccessAuditListwithType = assetMgr.getAccessLogs(searchCriteriaWithType);
			long toltalCountOfRepo = vXAccessAuditListwithType.getTotalCount();
			if (toltalCountOfRepo != 0) {
				servicesRepoType.put(serviceRepoName, toltalCountOfRepo);
				totalCountOfAudits += toltalCountOfRepo;
			}
		}
		vXMetricServiceCount.setServiceBasedCountList(servicesRepoType);
		vXMetricServiceCount.setTotalCount(totalCountOfAudits);
		return vXMetricServiceCount;
	}
	
	private Long getUserCountBasedOnUserRole(@SuppressWarnings("rawtypes") List userRoleList) {
		SearchCriteria searchCriteria = new SearchCriteria();
		searchCriteria.setStartIndex(0);
		searchCriteria.setMaxRows(100);
		searchCriteria.setGetCount(true);
		searchCriteria.setSortType("asc");	
		searchCriteria.addParam("userRoleList", userRoleList);
		VXUserList VXUserListKeyAdmin = xUserMgr.searchXUsers(searchCriteria);
		long userCount = VXUserListKeyAdmin.getTotalCount();
		return userCount;
	}
}
