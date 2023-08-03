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

import java.util.List;

public class PList<T> implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Start index for the result
	 */
	protected int startIndex;
	/**
	 * Page size used for the result
	 */
	protected int pageSize;
	/**
	 * Total records in the database for the given search conditions
	 */
	protected long totalCount;
	/**
	 * Number of rows returned for the search condition
	 */
	protected int resultSize;
	/**
	 * Sort type. Either desc or asc
	 */
	protected String sortType;
	/**
	 * Comma seperated list of the fields for sorting
	 */
	protected String sortBy;

	protected long queryTimeMS = System.currentTimeMillis();

	protected List<T> list;
	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public PList() {
		startIndex = 0;
		pageSize = 0;
		totalCount = 0;
		resultSize = 0;
		sortType = null;
		sortBy = null;
	}

	public PList(List<T> list, int startIndex, int pageSize, long totalCount, int resultSize, String sortType, String sortBy) {
		this.list = list;
		this.startIndex = startIndex;
		this.pageSize = pageSize;
		this.totalCount = totalCount;
		this.resultSize = resultSize;
		this.sortType = sortType;
		this.sortBy = sortBy;

	}

	public int getListSize() {
		return list == null ? 0 : list.size();
	}

	public void setList(List<T> list) {this.list = list;}

	public List<T> getList() {
		return list;
	}

	/**
	 * This method sets the value to the member attribute <b>startIndex</b>. You
	 * cannot set null to the attribute.
	 *
	 * @param startIndex
	 *            Value to set member attribute <b>startIndex</b>
	 */
	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}
	public int getStartIndex() { return startIndex; }


	/**
	 * This method sets the value to the member attribute <b>pageSize</b>. You
	 * cannot set null to the attribute.
	 *
	 * @param pageSize
	 *            Value to set member attribute <b>pageSize</b>
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	public int getPageSize() { return pageSize; }


	/**
	 * This method sets the value to the member attribute <b>totalCount</b>. You
	 * cannot set null to the attribute.
	 *
	 * @param totalCount
	 *            Value to set member attribute <b>totalCount</b>
	 */
	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}
	public long getTotalCount() { return totalCount; }



	/**
	 * This method sets the value to the member attribute <b>resultSize</b>. You
	 * cannot set null to the attribute.
	 *
	 * @param resultSize
	 *            Value to set member attribute <b>resultSize</b>
	 */
	public void setResultSize(int resultSize) {
		this.resultSize = resultSize;
	}

	/**
	 * Returns the value for the member attribute <b>resultSize</b>
	 *
	 * @return int - value of member attribute <b>resultSize</b>.
	 */
	public int getResultSize() {
		return getListSize();
	}

	/**
	 * This method sets the value to the member attribute <b>sortType</b>. You
	 * cannot set null to the attribute.
	 *
	 * @param sortType
	 *            Value to set member attribute <b>sortType</b>
	 */
	public void setSortType(String sortType) {
		this.sortType = sortType;
	}
	public String getSortType() { return sortType; }



	/**
	 * This method sets the value to the member attribute <b>sortBy</b>. You
	 * cannot set null to the attribute.
	 *
	 * @param sortBy
	 *            Value to set member attribute <b>sortBy</b>
	 */
	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}
	public String getSortBy() { return sortBy; }







	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PList [startIndex=" + startIndex + ", pageSize="
				+ pageSize + ", totalCount=" + totalCount
				+ ", resultSize=" + resultSize + ", sortType="
				+ sortType + ", sortBy=" + sortBy + ", queryTimeMS="
				+ queryTimeMS + "]";
	}
}