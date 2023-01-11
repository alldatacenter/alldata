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

package org.apache.ranger.plugin.policyengine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;

public class RangerAccessResourceImpl implements RangerMutableResource {
	private String              ownerUser;
	private Map<String, Object> elements;
	private String              stringifiedValue;
	private String              stringifiedCacheKeyValue;
	private String              leafName;
	private RangerServiceDef    serviceDef;

	public RangerAccessResourceImpl() {
		this(null, null);
	}

	public RangerAccessResourceImpl(Map<String, Object> elements) {
		this(elements, null);
	}

	public RangerAccessResourceImpl(Map<String, Object> elements, String ownerUser) {
		this.elements  = elements;
		this.ownerUser = ownerUser;
	}

	@Override
	public String getOwnerUser() {
		return ownerUser;
	}

	@Override
	public boolean exists(String name) {
		return elements != null && elements.containsKey(name);
	}

	@Override
	public Object getValue(String name) {
		Object ret = null;

		if(elements != null && elements.containsKey(name)) {
			ret = elements.get(name);
		}

		return ret;
	}

	@Override
	public Set<String> getKeys() {
		Set<String> ret = null;

		if(elements != null) {
			ret = elements.keySet();
		}

		return ret;
	}

	@Override
	public void setOwnerUser(String ownerUser) {
		this.ownerUser = ownerUser;
	}

	@Override
	public void setValue(String name, Object value) {
		if(value == null) {
			if(elements != null) {
				elements.remove(name);

				if(elements.isEmpty()) {
					elements = null;
				}
			}
		} else {
			if(elements == null) {
				elements = new HashMap<>();
			}
			elements.put(name, value);
		}

		// reset, so that these will be computed again with updated elements
		stringifiedValue = stringifiedCacheKeyValue = leafName = null;
	}

	@Override
	public void setServiceDef(final RangerServiceDef serviceDef) {
		this.serviceDef = serviceDef;
		this.stringifiedValue = this.stringifiedCacheKeyValue = this.leafName = null;
	}

	@Override
	public RangerServiceDef getServiceDef() {
		return this.serviceDef;
	}

	@Override
	public String getLeafName() {
		String ret = leafName;

		if(ret == null) {
			if(serviceDef != null && serviceDef.getResources() != null) {
				List<RangerResourceDef> resourceDefs = serviceDef.getResources();

				for(int idx = resourceDefs.size() - 1; idx >= 0; idx--) {
					RangerResourceDef resourceDef = resourceDefs.get(idx);

					if(resourceDef != null && exists(resourceDef.getName())) {
					    ret = leafName = resourceDef.getName();
					    break;
                    }
				}
			}
		}

		return ret;
	}

	@Override
	public String getAsString() {
		String ret = stringifiedValue;

		if(ret == null) {
			if(serviceDef != null && serviceDef.getResources() != null) {
				StringBuilder sb = new StringBuilder();

				for(RangerResourceDef resourceDef : serviceDef.getResources()) {
					if(resourceDef == null || !exists(resourceDef.getName())) {
						continue;
					}

					if(sb.length() > 0) {
						sb.append(RESOURCE_SEP);
					}

					sb.append(getValue(resourceDef.getName()));
				}

				if(sb.length() > 0) {
					ret = stringifiedValue = sb.toString();
				}
			}
		}

		return ret;
	}

	@Override
	public String getCacheKey() {
		String ret = stringifiedCacheKeyValue;

		if(ret == null) {
			if(serviceDef != null && serviceDef.getResources() != null) {
				StringBuilder sb = new StringBuilder();

				for(RangerResourceDef resourceDef : serviceDef.getResources()) {
					if(resourceDef == null || !exists(resourceDef.getName())) {
						continue;
					}

					if(sb.length() > 0) {
						sb.append(RESOURCE_SEP);
					}

					sb.append(resourceDef.getName()).append(RESOURCE_NAME_VAL_SEP).append(getValue(resourceDef.getName()));
				}

				if(sb.length() > 0) {
					ret = stringifiedCacheKeyValue = sb.toString();
				}
			}
		}

		return ret;
	}

	@Override
	public Map<String, Object> getAsMap() {
		return elements == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(elements);
	}

	@Override
	public RangerAccessResource getReadOnlyCopy() {
		return new RangerAccessResourceReadOnly(this);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof RangerAccessResourceImpl)) {
			return false;
		}

		if(this == obj) {
			return true;
		}

		RangerAccessResourceImpl other = (RangerAccessResourceImpl) obj;

		return ObjectUtils.equals(ownerUser, other.ownerUser) &&
			   ObjectUtils.equals(elements, other.elements);
	}

	@Override
	public int hashCode() {
		int ret = 7;

		ret = 31 * ret + ObjectUtils.hashCode(ownerUser);
		ret = 31 * ret + ObjectUtils.hashCode(elements);

		return ret;
	}

	@Override
	public String toString( ) {
		StringBuilder sb = new StringBuilder();

		toString(sb);

		return sb.toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("RangerResourceImpl={");

		sb.append("ownerUser={").append(ownerUser).append("} ");

		sb.append("elements={");
		if(elements != null) {
			for(Map.Entry<String, Object> e : elements.entrySet()) {
				sb.append(e.getKey()).append("=").append(e.getValue()).append("; ");
			}
		}
		sb.append("} ");

		sb.append("}");

		return sb;
	}

	protected String getStringifiedValue() { return stringifiedValue; }

	protected void setStringifiedValue(String val) { this.stringifiedValue = val; }
}
