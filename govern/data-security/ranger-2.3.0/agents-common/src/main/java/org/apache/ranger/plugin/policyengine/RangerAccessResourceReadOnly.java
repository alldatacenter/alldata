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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.ranger.plugin.model.RangerServiceDef;

import java.util.*;

public class RangerAccessResourceReadOnly implements RangerAccessResource {

	private final RangerAccessResource source;
	private final Set<String> keys;
	private final Map<String, Object> map;

	public RangerAccessResourceReadOnly(final RangerAccessResource source) {
		this.source = source;

		// Cached here for reducing access overhead
		Set<String> sourceKeys = source.getKeys();

		if (CollectionUtils.isEmpty(sourceKeys)) {
			sourceKeys = new HashSet<>();
		}
		this.keys = Collections.unmodifiableSet(sourceKeys);

		Map<String, Object> sourceMap = source.getAsMap();

		if (MapUtils.isEmpty(sourceMap)) {
			sourceMap = new HashMap<>();
		}
		this.map = Collections.unmodifiableMap(sourceMap);
	}

	public String getOwnerUser() { return source.getOwnerUser(); }

	public boolean exists(String name) { return source.exists(name); }

	public Object getValue(String name) { return source.getValue(name); }

	public RangerServiceDef getServiceDef() { return source.getServiceDef(); }

	public Set<String> getKeys() { return keys; }

	public String getLeafName() { return source.getLeafName(); }

	public String getAsString() { return source.getAsString(); }

	public String getCacheKey() { return source.getCacheKey(); }

	public Map<String, Object> getAsMap() { return map; }

	public RangerAccessResource getReadOnlyCopy() { return this; }
}
