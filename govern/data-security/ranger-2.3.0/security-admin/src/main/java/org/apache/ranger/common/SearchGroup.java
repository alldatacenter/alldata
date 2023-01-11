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

import javax.persistence.Query;

public class SearchGroup {
	public enum CONDITION {
		AND, OR
	}

	CONDITION condition = CONDITION.AND;

	List<SearchValue> values = new ArrayList<SearchValue>();
	List<SearchGroup> searchGroups = new ArrayList<SearchGroup>();

	/**
	 * @param condition
	 */
	public SearchGroup(CONDITION condition) {
		this.condition = condition;
	}

	public String getWhereClause(String prefix) {
		if (values == null || values.isEmpty() || searchGroups == null || searchGroups.isEmpty()) {
			return "";
		}

		int count = -1;
		int innerCount = 0;
		StringBuilder whereClause = new StringBuilder("(");
		for (SearchValue value : values) {
			count++;
			if (count > 0) {
				if (CONDITION.AND.equals(condition)) {
					whereClause.append(" AND ");
				} else {
					whereClause.append(" OR ");
				}
			}
			SearchField searchField = value.getSearchField();
			if (value.isList()) {
				whereClause.append(" (");
				int listCount = value.getValueList().size();
				for (int i = 0; i < listCount; i++) {
					if (i > 0) {
						whereClause.append(" OR ");
					}
					whereClause
							.append(searchField.getFieldName())
							.append(" = :")
							.append(searchField.getClientFieldName() + "_"
									+ prefix + "_" + count + "_" + innerCount);
					innerCount++;
				}
				whereClause.append(") ");
			} else {
				whereClause
						.append(searchField.getFieldName())
						.append(" = :")
						.append(searchField.getClientFieldName() + "_" + prefix
								+ "_" + count);
			}
		}

		for (SearchGroup searchGroup : searchGroups) {
			count++;
			if (count > 0) {
				if (CONDITION.AND.equals(condition)) {
					whereClause.append(" AND ");
				} else {
					whereClause.append(" OR ");
				}
			}
			whereClause.append(" ")
					.append(searchGroup.getWhereClause(prefix + "_" + count))
					.append(" ");
		}
		whereClause.append(") ");
		return whereClause.toString();
	}

	/**
	 * @param query
	 */
	public void resolveValues(Query query, String prefix) {
		if ((values == null || values.isEmpty())
				|| (searchGroups == null || searchGroups.isEmpty())) {
			return;
		}

		int count = -1;
		int innerCount = 0;
		for (SearchValue value : values) {
			count++;
			SearchField searchField = value.getSearchField();
			if (value.isList()) {
				int listCount = value.getValueList().size();
				for (int i = 0; i < listCount; i++) {
					String paramName = searchField.getClientFieldName() + "_"
							+ prefix + "_" + count + "_" + innerCount;
					query.setParameter(paramName, value.getValueList().get(i));
					innerCount++;
				}
			} else {
				String paramName = searchField.getClientFieldName() + "_"
						+ prefix + "_" + count;
				query.setParameter(paramName, value.getValue());
			}
		}

		for (SearchGroup searchGroup : searchGroups) {
			count++;
			searchGroup.resolveValues(query, prefix + "_" + count);
		}
	}
}
