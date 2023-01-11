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

package org.apache.ranger.plugin.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

public class KeySearchFilter {
	public static final String KEY_NAME    = "name";// search, sort
	
	public static final String START_INDEX     = "startIndex";
	public static final String PAGE_SIZE       = "pageSize";
	public static final String SORT_BY         = "sortBy";
	
	private Map<String, String> params;
	private int                 startIndex;
	private int                 maxRows    = Integer.MAX_VALUE;
	private boolean             getCount   = true;
	private String              sortBy;
	private String              sortType;
	
	public KeySearchFilter() {
		this(null);
	}

	public KeySearchFilter(String name, String value) {
		setParam(name, value);
	}

	public KeySearchFilter(Map<String, String> values) {
		setParams(values);
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public String getParam(String name) {
		return params == null ? null : params.get(name);
	}

	public void setParam(String name, String value) {
		if(StringUtils.isEmpty(name) || StringUtils.isEmpty(value)) {
			return;
		}

		if(params == null) {
			params = new HashMap<>();
		}

		params.put(name, value);
	}
	public boolean isEmpty() {
		return MapUtils.isEmpty(params);
	}
	
	public int getStartIndex() {
		return startIndex;
	}
	
	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getMaxRows() {
		return maxRows;
	}

	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}
	
	public boolean isGetCount() {
		return getCount;
	}

	public void setGetCount(boolean getCount) {
		this.getCount = getCount;
	}
	
	public String getSortBy() {
		return sortBy;
	}

	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}
	
	public String getSortType() {
		return sortType;
	}

	public void setSortType(String sortType) {
		this.sortType = sortType;
	}

	@Override
	public boolean equals(Object object) {
		if (object == null || !(object instanceof KeySearchFilter)) {
			return false;
		}
		KeySearchFilter that = (KeySearchFilter)object;
		return Objects.equals(params, that.params);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(params);
	}
}
