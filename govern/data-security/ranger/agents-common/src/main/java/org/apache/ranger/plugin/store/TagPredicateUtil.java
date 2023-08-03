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
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerTagResourceMap;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.apache.ranger.plugin.util.SearchFilter;

import java.util.List;

public class TagPredicateUtil extends AbstractPredicateUtil {

	public TagPredicateUtil() { super(); }

	@Override
	public void addPredicates(SearchFilter filter, List<Predicate> predicates) {
		super.addPredicates(filter, predicates);

		addPredicateForTagDefId(filter.getParam(SearchFilter.TAG_DEF_ID), predicates);
		addPredicateForTagDefGuid(filter.getParam(SearchFilter.TAG_DEF_GUID), predicates);

		addPredicateForTagId(filter.getParam(SearchFilter.TAG_ID), predicates);
		addPredicateForTagGuid(filter.getParam(SearchFilter.TAG_GUID), predicates);
		addPredicateForTagType(filter.getParam(SearchFilter.TAG_TYPE), predicates);

		addPredicateForResourceId(filter.getParam(SearchFilter.TAG_RESOURCE_ID), predicates);
		addPredicateForResourceGuid(filter.getParam(SearchFilter.TAG_RESOURCE_GUID), predicates);
		addPredicateForServiceResourceServiceName(filter.getParam(SearchFilter.TAG_RESOURCE_SERVICE_NAME), predicates);
		addPredicateForResourceSignature(filter.getParam(SearchFilter.TAG_RESOURCE_SIGNATURE), predicates);

		addPredicateForTagResourceMapId(filter.getParam(SearchFilter.TAG_MAP_ID), predicates);
	}

	private Predicate addPredicateForTagDefId(final String id, List<Predicate> predicates) {
		if (StringUtils.isEmpty(id)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {

				boolean ret = false;

				if (object == null) {
					return ret;
				}

				if (object instanceof RangerTagDef) {
					RangerTagDef tagDef = (RangerTagDef) object;

					ret = StringUtils.equals(id, tagDef.getId().toString());
				}

				return ret;
			}
		};

		if (predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForTagDefGuid(final String guid, List<Predicate> predicates) {
		if (StringUtils.isEmpty(guid)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {

				boolean ret = false;

				if (object == null) {
					return ret;
				}

				if (object instanceof RangerTagDef) {
					RangerTagDef tagDef = (RangerTagDef) object;

					ret = StringUtils.equals(guid, tagDef.getGuid());
				}

				return ret;
			}
		};

		if (predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForTagId(final String id, List<Predicate> predicates) {
		if (StringUtils.isEmpty(id)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {

				boolean ret = false;

				if (object == null) {
					return ret;
				}

				if (object instanceof RangerTag) {
					RangerTag tag = (RangerTag) object;

					ret = StringUtils.equals(id, tag.getId().toString());
				} else if (object instanceof RangerTagResourceMap) {
					RangerTagResourceMap tagResourceMap = (RangerTagResourceMap) object;
					ret = StringUtils.equals(id, tagResourceMap.getTagId().toString());
				}

				return ret;
			}
		};

		if (predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForTagGuid(final String guid, List<Predicate> predicates) {
		if (StringUtils.isEmpty(guid)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {

				boolean ret = false;

				if (object == null) {
					return ret;
				}

				if (object instanceof RangerTag) {
					RangerTag tag = (RangerTag) object;

					ret = StringUtils.equals(guid, tag.getGuid());
				}

				return ret;
			}
		};

		if (predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForTagType(final String type, List<Predicate> predicates) {
		if (StringUtils.isEmpty(type)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {

				boolean ret = false;

				if (object == null) {
					return ret;
				}

				if (object instanceof RangerTagDef) {
					RangerTagDef tagDef = (RangerTagDef) object;

					ret = StringUtils.equals(type, tagDef.getName());
				} else if (object instanceof RangerTag) {
					RangerTag tag = (RangerTag) object;

					ret = StringUtils.equals(type, tag.getType());
				}

				return ret;
			}
		};

		if (predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForResourceId(final String id, List<Predicate> predicates) {
		if (StringUtils.isEmpty(id)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {

				boolean ret = false;

				if (object == null) {
					return ret;
				}

				if (object instanceof RangerServiceResource) {
					RangerServiceResource resource = (RangerServiceResource) object;

					ret = StringUtils.equals(id, resource.getId().toString());
				} else if(object instanceof RangerTagResourceMap) {
					RangerTagResourceMap tagResourceMap = (RangerTagResourceMap)object;

					ret = StringUtils.equals(id, tagResourceMap.getId().toString());
				}

				return ret;
			}
		};

		if (predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForResourceGuid(final String id, List<Predicate> predicates) {
		if (StringUtils.isEmpty(id)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {

				boolean ret = false;

				if (object == null) {
					return ret;
				}

				if (object instanceof RangerServiceResource) {
					RangerServiceResource resource = (RangerServiceResource) object;

					ret = StringUtils.equals(id, resource.getGuid());
				}

				return ret;
			}
		};

		if (predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForServiceResourceServiceName(final String serviceName, List<Predicate> predicates) {
		if (serviceName == null || StringUtils.isEmpty(serviceName)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {

				boolean ret = false;

				if (object == null) {
					return ret;
				}

				if (object instanceof RangerServiceResource) {
					RangerServiceResource resource = (RangerServiceResource) object;
					ret = StringUtils.equals(resource.getServiceName(), serviceName);
				}

				return ret;
			}
		};

		if (predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForResourceSignature(final String signature, List<Predicate> predicates) {
		if (StringUtils.isEmpty(signature)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {

				boolean ret = false;

				if (object == null) {
					return ret;
				}

				if (object instanceof RangerServiceResource) {
					RangerServiceResource resource = (RangerServiceResource) object;

					ret = StringUtils.equals(signature, resource.getResourceSignature());
				}

				return ret;
			}
		};

		if (predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}

	private Predicate addPredicateForTagResourceMapId(final String id, List<Predicate> predicates) {
		if (StringUtils.isEmpty(id)) {
			return null;
		}

		Predicate ret = new Predicate() {
			@Override
			public boolean evaluate(Object object) {

				boolean ret = false;

				if (object == null) {
					return ret;
				}

				if (object instanceof RangerTagResourceMap) {
					RangerTagResourceMap tagResourceMap = (RangerTagResourceMap) object;
					ret = StringUtils.equals(id, tagResourceMap.getId().toString());
				}

				return ret;
			}
		};

		if (predicates != null) {
			predicates.add(ret);
		}

		return ret;
	}
}
