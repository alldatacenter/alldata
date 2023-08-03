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

 /**
 *
 */
package org.apache.ranger.common;

import java.util.ArrayList;
import java.util.List;

public class SearchField {
	public enum DATA_TYPE {
		INTEGER, STRING, INT_LIST, STR_LIST, BOOLEAN, DATE
	};

	public enum SEARCH_TYPE {
		FULL, PARTIAL, LESS_THAN, LESS_EQUAL_THAN, GREATER_THAN, GREATER_EQUAL_THAN
	};

	private String clientFieldName;
	private String fieldName;
	private DATA_TYPE dataType;
	private SEARCH_TYPE searchType;
	private String regEx;
	private String enumName;
	private int maxValue;
	private List<String> joinTables;
	private String joinCriteria;
	private String customCondition;

	/**
	 * default constructor
	 */
	public SearchField(String clientFieldName, String fieldName,
			DATA_TYPE dtype, SEARCH_TYPE stype, String joinTables,
			String joinCriteria) {
		this.clientFieldName = clientFieldName;
		this.fieldName = fieldName;
		dataType = dtype;
		searchType = stype;

		setJoinTables(joinTables);
		this.joinCriteria = joinCriteria;
	}

	/**
	 * constructor
	 */
	public SearchField(String clientFieldName, String fieldName,
			DATA_TYPE dtype, SEARCH_TYPE stype) {
		this.clientFieldName = clientFieldName;
		this.fieldName = fieldName;
		dataType = dtype;
		searchType = stype;
	}

	/**
	 * constructor
	 */
	public SearchField(String clientFieldName, String fieldName) {
		this.clientFieldName = clientFieldName;
		this.fieldName = fieldName;
		dataType = DATA_TYPE.STRING;
		searchType = SEARCH_TYPE.FULL;
	}

	static public SearchField createString(String clientFieldName,
			String fieldName, SEARCH_TYPE stype, String regEx) {
		SearchField searchField = new SearchField(clientFieldName, fieldName,
				DATA_TYPE.STRING, stype);
		searchField.setRegEx(regEx);
		return searchField;
	}

	static public SearchField createLong(String clientFieldName,
			String fieldName) {
		SearchField searchField = new SearchField(clientFieldName, fieldName,
				DATA_TYPE.INTEGER, SEARCH_TYPE.FULL);
		return searchField;
	}

	static public SearchField createEnum(String clientFieldName,
			String fieldName, String enumName, int maxValue) {
		SearchField searchField = new SearchField(clientFieldName, fieldName,
				DATA_TYPE.INT_LIST, SEARCH_TYPE.FULL);
		searchField.setEnumName(enumName);
		searchField.setMaxValue(maxValue);
		return searchField;
	}

	public String getClientFieldName() {
		return clientFieldName;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public DATA_TYPE getDataType() {
		return dataType;
	}

	public void setDataType(DATA_TYPE dataType) {
		this.dataType = dataType;
	}

	public SEARCH_TYPE getSearchType() {
		return searchType;
	}

	/**
	 * @param regEx
	 *            the regEx to set
	 */
	public void setRegEx(String regEx) {
		this.regEx = regEx;
	}

    public String getRegEx() {
        return regEx;
    }

	/**
	 * @param enumName
	 *            the enumName to set
	 */
	public void setEnumName(String enumName) {
		this.enumName = enumName;
	}

    public String getEnumName() {
        return enumName;
    }

	/**
	 * @param maxValue
	 *            the maxValue to set
	 */
	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}

    public int getMaxValue() {
        return maxValue;
    }

	/**
	 * @return the joinTables
	 */
	public List<String> getJoinTables() {
		return joinTables;
	}

	/**
	 * @param joinTables
	 *            the joinTables to set
	 */
	public void setJoinTables(List<String> joinTables) {
		this.joinTables = joinTables;
	}

	/**
	 * @param joinTables
	 *            the joinTables to set (comma separated)
	 */
	public void setJoinTables(String joinTables) {
		if (joinTables != null) {
			if (this.joinTables == null) {
				this.joinTables = new ArrayList<String>();
			}

			for (String table : joinTables.split(",")) {
				if (table == null) {
					continue;
				}
				table = table.trim();

				if (!table.isEmpty() && !this.joinTables.contains(table)) {
					this.joinTables.add(table);
				}
			}

		}
	}

	/**
	 * @return the joinCriteria
	 */
	public String getJoinCriteria() {
		return joinCriteria;
	}

	/**
	 * @param joinCriteria
	 *            the joinCriteria to set
	 */
	public void setJoinCriteria(String joinCriteria) {
		this.joinCriteria = joinCriteria;
	}

	/**
	 * @return the customCondition
	 */
	public String getCustomCondition() {
		return customCondition;
	}
	public void setCustomCondition(String conditions) {
		customCondition=conditions;
	}
}
