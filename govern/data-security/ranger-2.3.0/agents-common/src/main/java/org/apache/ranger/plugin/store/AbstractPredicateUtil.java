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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerBaseModelObject;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.util.SearchFilter;

public class AbstractPredicateUtil {
	private static Map<String, Comparator<RangerBaseModelObject>> sorterMap  = new HashMap<>();

	public void applyFilter(List<? extends RangerBaseModelObject> objList, SearchFilter filter) {
		if(CollectionUtils.isEmpty(objList)) {
			return;
		}

		Predicate pred = getPredicate(filter);

		if(pred != null) {
			CollectionUtils.filter(objList, pred);
		}

		Comparator<RangerBaseModelObject> sorter = getSorter(filter);

		if(sorter != null) {
			Collections.sort(objList, sorter);
		}
	}

	public Predicate getPredicate(SearchFilter filter) {
		if(filter == null || filter.isEmpty()) {
			return null;
		}

		List<Predicate> predicates = new ArrayList<>();
		
		addPredicates(filter, predicates);

		Predicate ret = CollectionUtils.isEmpty(predicates) ? null : PredicateUtils.allPredicate(predicates);

		return ret;
	}

	public void addPredicates(SearchFilter filter, List<Predicate> predicates) {
		addPredicateForServiceType(filter.getParam(SearchFilter.SERVICE_TYPE), predicates);
		addPredicateForServiceTypeId(filter.getParam(SearchFilter.SERVICE_TYPE_ID), predicates);
		addPredicateForServiceName(filter.getParam(SearchFilter.SERVICE_NAME), predicates);
		// addPredicateForServiceId(filter.getParam(SearchFilter.SERVICE_ID), predicates); // not supported
		addPredicateForPolicyName(filter.getParam(SearchFilter.POLICY_NAME), predicates);
		addPredicateForPolicyId(filter.getParam(SearchFilter.POLICY_ID), predicates);
		addPredicateForIsEnabled(filter.getParam(SearchFilter.IS_ENABLED), predicates);
		addPredicateForIsRecursive(filter.getParam(SearchFilter.IS_RECURSIVE), predicates);
		addPredicateForTagServiceName(filter.getParam(SearchFilter.TAG_SERVICE_NAME), predicates);
		// addPredicateForTagServiceId(filter.getParam(SearchFilter.TAG_SERVICE_ID), predicates); // not supported
		addPredicateForUserName(filter.getParam(SearchFilter.USER), predicates);
		addPredicateForGroupName(filter.getParam(SearchFilter.GROUP), predicates);
		addPredicateForRoleName(filter.getParam(SearchFilter.ROLE), predicates);
		addPredicateForResources(filter.getParamsWithPrefix(SearchFilter.RESOURCE_PREFIX, true), predicates);
		addPredicateForPolicyResource(filter.getParam(SearchFilter.POL_RESOURCE), predicates);
		addPredicateForPartialPolicyName(filter.getParam(SearchFilter.POLICY_NAME_PARTIAL), predicates);
		addPredicateForResourceSignature(filter.getParam(SearchFilter.RESOURCE_SIGNATURE), predicates);
		addPredicateForPolicyType(filter.getParam(SearchFilter.POLICY_TYPE), predicates);
		addPredicateForPolicyPriority(filter.getParam(SearchFilter.POLICY_PRIORITY), predicates);
		addPredicateForPartialPolicyLabels(filter.getParam(SearchFilter.POLICY_LABELS_PARTIAL), predicates);
		addPredicateForZoneName(filter.getParam(SearchFilter.ZONE_NAME), predicates);
		// addPredicateForZoneId(filter.getParam(SearchFilter.ZONE_ID), predicates); // not supported
	}

	public Comparator<RangerBaseModelObject> getSorter(SearchFilter filter) {
		String sortBy = filter == null ? null : filter.getSortBy();

		if(StringUtils.isEmpty(sortBy)) {
			return null;
		}

		Comparator<RangerBaseModelObject> ret = sorterMap.get(sortBy);

		return ret;
	}

	public final static Comparator<RangerBaseModelObject> idComparator = new Comparator<RangerBaseModelObject>() {
		@Override
		public int compare(RangerBaseModelObject o1, RangerBaseModelObject o2) {
			Long val1 = (o1 != null) ? o1.getId() : null;
			Long val2 = (o2 != null) ? o2.getId() : null;

			return ObjectUtils.compare(val1, val2);
		}
	};

	protected final static Comparator<RangerBaseModelObject> createTimeComparator = new Comparator<RangerBaseModelObject>() {
		@Override
		public int compare(RangerBaseModelObject o1, RangerBaseModelObject o2) {
			Date val1 = (o1 != null) ? o1.getCreateTime() : null;
			Date val2 = (o2 != null) ? o2.getCreateTime() : null;

			return ObjectUtils.compare(val1, val2);
		}
	};

	protected final static Comparator<RangerBaseModelObject> updateTimeComparator = new Comparator<RangerBaseModelObject>() {
		@Override
		public int compare(RangerBaseModelObject o1, RangerBaseModelObject o2) {
			Date val1 = (o1 != null) ? o1.getUpdateTime() : null;
			Date val2 = (o2 != null) ? o2.getUpdateTime() : null;

			return ObjectUtils.compare(val1, val2);
		}
	};

	protected final static Comparator<RangerBaseModelObject> serviceDefNameComparator = new Comparator<RangerBaseModelObject>() {
		@Override
		public int compare(RangerBaseModelObject o1, RangerBaseModelObject o2) {
			String val1 = null;
			String val2 = null;

			if(o1 != null) {
				if(o1 instanceof RangerServiceDef) {
					val1 = ((RangerServiceDef)o1).getName();
				} else if(o1 instanceof RangerService) {
					val1 = ((RangerService)o1).getType();
				}
			}

			if(o2 != null) {
				if(o2 instanceof RangerServiceDef) {
					val2 = ((RangerServiceDef)o2).getName();
				} else if(o2 instanceof RangerService) {
					val2 = ((RangerService)o2).getType();
				}
			}

			return ObjectUtils.compare(val1, val2);
		}
	};

	protected final static Comparator<RangerBaseModelObject> serviceNameComparator = new Comparator<RangerBaseModelObject>() {
		@Override
		public int compare(RangerBaseModelObject o1, RangerBaseModelObject o2) {
			String val1 = null;
			String val2 = null;

			if(o1 != null) {
				if(o1 instanceof RangerPolicy) {
					val1 = ((RangerPolicy)o1).getService();
				} else if(o1 instanceof RangerService) {
					val1 = ((RangerService)o1).getType();
				}
			}

			if(o2 != null) {
				if(o2 instanceof RangerPolicy) {
					val2 = ((RangerPolicy)o2).getService();
				} else if(o2 instanceof RangerService) {
					val2 = ((RangerService)o2).getType();
				}
			}

			return ObjectUtils.compare(val1, val2);
		}
	};

	protected final static Comparator<RangerBaseModelObject> policyNameComparator = new Comparator<RangerBaseModelObject>() {
		@Override
		public int compare(RangerBaseModelObject o1, RangerBaseModelObject o2) {
			String val1 = (o1 instanceof RangerPolicy) ? ((RangerPolicy)o1).getName() : null;
			String val2 = (o2 instanceof RangerPolicy) ? ((RangerPolicy)o2).getName() : null;

			return ObjectUtils.compare(val1, val2);
		}
	};

	public final static Comparator<RangerResourceDef> resourceLevelComparator = new Comparator<RangerResourceDef>() {
		@Override
		public int compare(RangerResourceDef o1, RangerResourceDef o2) {
			Integer val1 = (o1 != null) ? o1.getLevel() : null;
			Integer val2 = (o2 != null) ? o2.getLevel() : null;

			return ObjectUtils.compare(val1, val2);
		}
	};

    protected final static Comparator<RangerBaseModelObject> zoneNameComparator = new Comparator<RangerBaseModelObject>() {
        @Override
        public int compare(RangerBaseModelObject o1, RangerBaseModelObject o2) {
            String val1 = (o1 instanceof RangerSecurityZone) ? ((RangerSecurityZone)o1).getName() : null;
            String val2 = (o2 instanceof RangerSecurityZone) ? ((RangerSecurityZone)o2).getName() : null;

            return ObjectUtils.compare(val1, val2);
        }
    };

	static {
		sorterMap.put(SearchFilter.SERVICE_TYPE, serviceDefNameComparator);
		sorterMap.put(SearchFilter.SERVICE_TYPE_ID, idComparator);
		sorterMap.put(SearchFilter.SERVICE_NAME, serviceNameComparator);
		sorterMap.put(SearchFilter.SERVICE_TYPE_ID, idComparator);
		sorterMap.put(SearchFilter.POLICY_NAME, policyNameComparator);
		sorterMap.put(SearchFilter.POLICY_ID, idComparator);
		sorterMap.put(SearchFilter.CREATE_TIME, createTimeComparator);
		sorterMap.put(SearchFilter.UPDATE_TIME, updateTimeComparator);
		sorterMap.put(SearchFilter.ZONE_ID, idComparator);
		sorterMap.put(SearchFilter.ZONE_NAME, zoneNameComparator);
	}

	private Predicate addPredicateForServiceType(final String serviceType, List<Predicate> predicates) {
		if(StringUtils.isEmpty(serviceType)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if(object instanceof RangerServiceDef) {
					RangerServiceDef serviceDef = (RangerServiceDef)object;
					String           svcType    = serviceDef.getName();

					ret = StringUtils.equals(svcType, serviceType);
				} else {
					ret = true;
				}

				return ret;
			}
		};

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForServiceTypeId(final String serviceTypeId, List<Predicate> predicates) {
		if(StringUtils.isEmpty(serviceTypeId)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if(object instanceof RangerServiceDef) {
					RangerServiceDef serviceDef = (RangerServiceDef)object;
					Long             svcDefId   = serviceDef.getId();

					if(svcDefId != null) {
						ret = StringUtils.equals(serviceTypeId, svcDefId.toString());
					}
				} else {
					ret = true;
				}

				return ret;
			}
		};
		
		if(predicates != null) {
			predicates.add(ret);
		}
		
		return ret;
	}

	private Predicate addPredicateForServiceName(final String serviceName, List<Predicate> predicates) {
		if(StringUtils.isEmpty(serviceName)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if(object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy)object;

					ret = StringUtils.equals(serviceName, policy.getService());
				} else if(object instanceof RangerService) {
					RangerService service = (RangerService)object;

					ret = StringUtils.equals(serviceName, service.getName());
				} else {
					ret = true;
				}

				return ret;
			}
		};

		if(ret != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForPolicyName(final String policyName, List<Predicate> predicates) {
		if(StringUtils.isEmpty(policyName)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if(object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy)object;

					ret = StringUtils.equals(policyName, policy.getName());
				} else {
					ret = true;
				}

				return ret;
			}
		};

		if(predicates != null) {
			predicates.add(ret);
		}
			
		return ret;
	}

	private Predicate addPredicateForPartialPolicyName(final String policyName, List<Predicate> predicates) {
		if(StringUtils.isEmpty(policyName)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if(object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy)object;

					ret = StringUtils.containsIgnoreCase(policy.getName(), policyName);
				} else {
					ret = true;
				}

				return ret;
			}
		};

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForPolicyId(final String policyId, List<Predicate> predicates) {
		if(StringUtils.isEmpty(policyId)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if(object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy)object;

					if(policy.getId() != null) {
						ret = StringUtils.equals(policyId, policy.getId().toString());
					}
				} else {
					ret = true;
				}

				return ret;
			}
		};

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForUserName(final String userName, List<Predicate> predicates) {
		if(StringUtils.isEmpty(userName)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if(object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy)object;

					List<?>[] policyItemsList = new List<?>[] { policy.getPolicyItems(),
																policy.getDenyPolicyItems(),
																policy.getAllowExceptions(),
																policy.getDenyExceptions(),
																policy.getDataMaskPolicyItems(),
																policy.getRowFilterPolicyItems()
															  };

					for(List<?> policyItemsObj : policyItemsList) {
						@SuppressWarnings("unchecked")
						List<RangerPolicyItem> policyItems = (List<RangerPolicyItem>)policyItemsObj;

						for(RangerPolicyItem policyItem : policyItems) {
                                                        if(! policyItem.getUsers().isEmpty()) {
                                                                for(String user : policyItem.getUsers()) {
                                                                        if(StringUtils.containsIgnoreCase(user, userName)) {
                                                                                ret = true;
                                                                                break;
                                                                        }
                                                                }
							}
						}
						if (ret) {
							break;
						}
					}
				} else {
					ret = true;
				}

				return ret;
			}
		};

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForGroupName(final String groupName, List<Predicate> predicates) {
		if(StringUtils.isEmpty(groupName)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if(object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy)object;

					List<?>[] policyItemsList = new List<?>[] { policy.getPolicyItems(),
							policy.getDenyPolicyItems(),
							policy.getAllowExceptions(),
							policy.getDenyExceptions(),
							policy.getDataMaskPolicyItems(),
							policy.getRowFilterPolicyItems()
						  };

					for(List<?> policyItemsObj : policyItemsList) {
						@SuppressWarnings("unchecked")
						List<RangerPolicyItem> policyItems = (List<RangerPolicyItem>)policyItemsObj;

						for(RangerPolicyItem policyItem : policyItems) {
                                                        if(! policyItem.getGroups().isEmpty()) {
                                                                for(String group : policyItem.getGroups()) {
                                                                        if(StringUtils.containsIgnoreCase(group, groupName)) {
                                                                                ret = true;
                                                                                break;
                                                                        }
                                                                }
							}
						}
						if (ret) {
							break;
						}
					}
				} else {
					ret = true;
				}

				return ret;
			}
		};

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForRoleName(final String roleName, List<Predicate> predicates) {
		if(StringUtils.isEmpty(roleName)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if(object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy)object;

					List<?>[] policyItemsList = new List<?>[] { policy.getPolicyItems(),
																policy.getDenyPolicyItems(),
																policy.getAllowExceptions(),
																policy.getDenyExceptions(),
																policy.getDataMaskPolicyItems(),
																policy.getRowFilterPolicyItems()
															};
					for(List<?> policyItemsObj : policyItemsList) {
						@SuppressWarnings("unchecked")
						List<RangerPolicyItem> policyItems = (List<RangerPolicyItem>)policyItemsObj;

						for(RangerPolicyItem policyItem : policyItems) {
							if(! policyItem.getRoles().isEmpty()) {
								for(String role : policyItem.getRoles()) {
									if(StringUtils.containsIgnoreCase(role, roleName)) {
										ret = true;
										break;
									}
								}
							}
						}
						if (ret) {
							break;
						}
					}
				}else {
					ret = true;
				}

				return ret;
			}
		};

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;

	}

	private Predicate addPredicateForIsEnabled(final String status, List<Predicate> predicates) {
		if(StringUtils.isEmpty(status)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if(object instanceof RangerBaseModelObject) {
					RangerBaseModelObject obj = (RangerBaseModelObject)object;

					if(Boolean.parseBoolean(status)) {
						ret = obj.getIsEnabled();
					} else {
						ret = !obj.getIsEnabled();
					}
				} else {
					ret = true;
				}

				return ret;
			}
		};

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForResources(final Map<String, String> resources, List<Predicate> predicates) {
		if(MapUtils.isEmpty(resources)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if(object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy)object;

					if(! MapUtils.isEmpty(policy.getResources())) {
						int numFound = 0;
						for(String name : resources.keySet()) {
							boolean isMatch = false;

							RangerPolicyResource policyResource = policy.getResources().get(name);

							if(policyResource != null && !CollectionUtils.isEmpty(policyResource.getValues())) {
								String val = resources.get(name);

								if(policyResource.getValues().contains(val)) {
									isMatch = true;
								} else {
									for(String policyResourceValue : policyResource.getValues()) {
										if(FilenameUtils.wildcardMatch(val, policyResourceValue)) {
											isMatch = true;
											break;
										}
									}
								}
							}

							if(isMatch) {
								numFound++;
							} else {
								break;
							}
						}

						ret = numFound == resources.size();
					}
				} else {
					ret = true;
				}

				return ret;
			}
		};

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForPolicyResource(final String resourceValue, List<Predicate> predicates) {
		if(StringUtils.isEmpty(resourceValue)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if(object instanceof RangerPolicy) {
					RangerPolicy                      policy          = (RangerPolicy)object;
					Map<String, RangerPolicyResource> policyResources = policy.getResources();

					if(MapUtils.isNotEmpty(policyResources)) {

						for (Map.Entry<String, RangerPolicyResource> entry : policyResources.entrySet()) {

							RangerPolicyResource policyResource = entry.getValue();

							if (policyResource != null && CollectionUtils.isNotEmpty(policyResource.getValues())) {

								for (String policyResoureValue : policyResource.getValues()) {
									if (StringUtils.containsIgnoreCase(policyResoureValue, resourceValue)) {
										ret = true;

										break;
									}
								}
 							}

						}

					}
				} else {
					ret = true;
				}

				return ret;
			}
		};

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForIsRecursive(final String isRecursiveStr, List<Predicate> predicates) {
		if(StringUtils.isEmpty(isRecursiveStr)) {
			return null;
		}

		final boolean isRecursive = Boolean.parseBoolean(isRecursiveStr);

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = true;

				if(object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy)object;

					if(! MapUtils.isEmpty(policy.getResources())) {
						for(Map.Entry<String, RangerPolicyResource> e : policy.getResources().entrySet()) {
							RangerPolicyResource resValue = e.getValue();
							
							if(resValue.getIsRecursive() == null) {
								ret = !isRecursive;
							} else {
								ret = resValue.getIsRecursive().booleanValue() == isRecursive;
							}
							
							if(ret) {
								break;
							}
						}
					}
				}

				return ret;
			}
		};

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForTagServiceName(final String tagServiceName, List<Predicate> predicates) {
		if(StringUtils.isEmpty(tagServiceName)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if(object instanceof RangerService) {
					RangerService service = (RangerService)object;

					ret = StringUtils.equals(tagServiceName, service.getTagService());
				} else {
					ret = true;
				}

				return ret;
			}
		};

		if(ret != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForResourceSignature(String signature, List<Predicate> predicates) {

		Predicate ret = createPredicateForResourceSignature(signature);

		if(predicates != null && ret != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForPolicyType(final String policyType, List<Predicate> predicates) {
		if(StringUtils.isEmpty(policyType)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = true;

				if(object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy)object;

					if(policy.getPolicyType() != null) {
						ret = StringUtils.equalsIgnoreCase(policyType, policy.getPolicyType().toString());
					}
				}

				return ret;
			}
		};

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForPartialPolicyLabels(final String policyLabels, List<Predicate> predicates) {
		if (StringUtils.isEmpty(policyLabels)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if (object == null) {
					return false;
				}
				boolean ret = false;

				if (object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy) object;
					//	exact match
                                        /*if (policy.getPolicyLabels().contains(policyLabels)) {
                                                ret = true;
                                        }*/
					/*partial match*/
					for (String label :policy.getPolicyLabels()){
						ret = StringUtils.containsIgnoreCase(label, policyLabels);
						if(ret){
							return ret;
						}
					}

				} else {
					ret = true;
				}
				return ret;
			}
		};
		if (predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForPolicyPriority(final String policyPriority, List<Predicate> predicates) {
		if(StringUtils.isEmpty(policyPriority)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if (object == null) {
					return false;
				}

				boolean ret = true;

				if (object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy) object;

					Integer priority = policy.getPolicyPriority() != null ? policy.getPolicyPriority() : RangerPolicy.POLICY_PRIORITY_NORMAL;

					if (priority == RangerPolicy.POLICY_PRIORITY_NORMAL) {
						ret = StringUtils.equalsIgnoreCase(policyPriority, policy.POLICY_PRIORITY_NAME_NORMAL)
								|| StringUtils.equalsIgnoreCase(policyPriority, priority.toString());
					} else if (priority == RangerPolicy.POLICY_PRIORITY_OVERRIDE) {
						ret = StringUtils.equalsIgnoreCase(policyPriority, policy.POLICY_PRIORITY_NAME_OVERRIDE)
								|| StringUtils.equalsIgnoreCase(policyPriority, priority.toString());
					} else {
						ret = false;
					}
				}
				return ret;
			}

		};

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}


	public Predicate createPredicateForResourceSignature(final String policySignature) {

		if (StringUtils.isEmpty(policySignature)) {
			return null;
		}

		return new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				if(object == null) {
					return false;
				}

				boolean ret = false;

				if (object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy)object;

					ret = StringUtils.equals(policy.getResourceSignature(), policySignature);
				} else {
					ret = true;
				}

				return ret;
			}
		};
	}
    private Predicate addPredicateForZoneName(final String zoneName, List<Predicate> predicates) {

    if(StringUtils.isEmpty(zoneName)) {
    	return null;
    }

        Predicate ret = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if(object == null) {
                    return false;
                }

                final boolean ret;

                if(object instanceof RangerPolicy) {
                    RangerPolicy policy = (RangerPolicy)object;

                    if (policy.getZoneName() != null) {
                        ret = StringUtils.equals(zoneName, policy.getZoneName());
                    } else {
                        ret = StringUtils.isEmpty(zoneName);
                    }
                } else if (object instanceof RangerSecurityZone) {
                    RangerSecurityZone securityZone = (RangerSecurityZone)object;

                    return StringUtils.equals(securityZone.getName(), zoneName);
                } else {
                    ret = true;
                }

                return ret;
            }
        };

        if(predicates != null) {
            predicates.add(ret);
        }

        return ret;
    }
}
