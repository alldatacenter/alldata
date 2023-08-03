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

import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.util.SearchFilter;

import java.util.List;

public class ServicePredicateUtil extends AbstractPredicateUtil {
	private ServiceStore serviceStore;

	public ServicePredicateUtil(ServiceStore serviceStore) {
		super();
		this.serviceStore = serviceStore;
	}

	@Override
	public void addPredicates(SearchFilter filter, List<Predicate> predicates) {
		super.addPredicates(filter, predicates);

		addPredicateForServiceType(filter.getParam(SearchFilter.SERVICE_TYPE), predicates);
		addPredicateForServiceId(filter.getParam(SearchFilter.SERVICE_ID), predicates);
		addPredicateForTagSeviceName(filter.getParam(SearchFilter.TAG_SERVICE_NAME), predicates);
		addPredicateForTagSeviceId(filter.getParam(SearchFilter.TAG_SERVICE_ID), predicates);
	}

	private String getServiceType(String serviceName) {
		RangerService service = null;

		try {
			if (serviceStore != null) {
				service = serviceStore.getServiceByName(serviceName);
			}
		} catch(Exception excp) {
			// ignore
		}

		return service != null ? service.getType() : null;
	}

	private Long getServiceId(String serviceName) {
		RangerService service = null;

		try {
			if (serviceStore != null) {
				service = serviceStore.getServiceByName(serviceName);
			}
		} catch(Exception excp) {
			// ignore
		}

		return service != null ? service.getId() : null;
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

				if(object instanceof RangerPolicy) {
					RangerPolicy policy = (RangerPolicy)object;

					ret = StringUtils.equals(serviceType, getServiceType(policy.getService()));
				} else if(object instanceof RangerService) {
					RangerService service = (RangerService)object;

					ret = StringUtils.equals(serviceType, service.getType());
				} else if(object instanceof RangerServiceDef) {
					RangerServiceDef serviceDef = (RangerServiceDef)object;

					ret = StringUtils.equals(serviceType, serviceDef.getName());
				}

				return ret;
			}
		};

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForServiceId(final String serviceId, List<Predicate> predicates) {
		if(StringUtils.isEmpty(serviceId)) {
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
					Long         svcId  = getServiceId(policy.getService());

					if(svcId != null) {
						ret = StringUtils.equals(serviceId, svcId.toString());
					}
				} else if(object instanceof RangerService) {
					RangerService service = (RangerService)object;

					if(service.getId() != null) {
						ret = StringUtils.equals(serviceId, service.getId().toString());
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

	private Predicate addPredicateForTagSeviceName(final String tagServiceName, List<Predicate> predicates) {
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

		if(predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForTagSeviceId(final String tagServiceId, List<Predicate> predicates) {
		if(StringUtils.isEmpty(tagServiceId)) {
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

					if(! StringUtils.isEmpty(service.getTagService())) {
						RangerService tagService = null;

						try {
							tagService = serviceStore.getServiceByName(service.getTagService());
						} catch(Exception excp) {
						}

						ret = tagService != null && tagService.getId() != null && StringUtils.equals(tagServiceId, tagService.getId().toString());
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
}
