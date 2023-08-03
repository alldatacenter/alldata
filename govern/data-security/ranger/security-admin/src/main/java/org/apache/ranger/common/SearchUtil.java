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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class SearchUtil {
	final static Logger logger = LoggerFactory.getLogger(SearchUtil.class);

	@Autowired
	RESTErrorUtil restErrorUtil;

	@Autowired
	RangerConfigUtil configUtil;

	// @Autowired
	// AKADomainObjectSecurityHandler securityHandler;

	@Autowired
	StringUtil stringUtil;

	int minInListLength = 20;
	String defaultDateFormat="MM/dd/yyyy";

	public SearchUtil() {
		minInListLength = PropertiesUtil.getIntProperty("ranger.db.min_inlist", minInListLength);
		defaultDateFormat = PropertiesUtil.getProperty("ranger.ui.defaultDateformat", defaultDateFormat);
	}

	/**
	 * @param request
	 * @param sortFields
	 * @return
	 */
	public SearchCriteria extractCommonCriterias(HttpServletRequest request,
			List<SortField> sortFields) {
		SearchCriteria searchCriteria = new SearchCriteria();

		int startIndex = restErrorUtil.parseInt(
				request.getParameter("startIndex"), 0,
				"Invalid value for parameter startIndex",
				MessageEnums.INVALID_INPUT_DATA, null, "startIndex");
		startIndex = startIndex < 0 ? 0 : startIndex;
		searchCriteria.setStartIndex(startIndex);

		int pageSize = restErrorUtil.parseInt(request.getParameter("pageSize"),
				configUtil.getDefaultMaxRows(),
				"Invalid value for parameter pageSize",
				MessageEnums.INVALID_INPUT_DATA, null, "pageSize");
		searchCriteria.setMaxRows(pageSize);

		// is count needed
		searchCriteria.setGetCount(restErrorUtil.parseBoolean(
				request.getParameter("getCount"), true));

		searchCriteria.setOwnerId(restErrorUtil.parseLong(
				request.getParameter("ownerId"), null));
		searchCriteria.setGetChildren(restErrorUtil.parseBoolean(
				request.getParameter("getChildren"), false));

		String sortBy = restErrorUtil.validateString(
				request.getParameter("sortBy"), StringUtil.VALIDATION_ALPHA,
				"Invalid value for parameter sortBy",
				MessageEnums.INVALID_INPUT_DATA, null, "sortBy");

		boolean sortSet = false;
		if (!stringUtil.isEmpty(sortBy)) {
			for (SortField sortField : sortFields) {
				if (sortField.getParamName().equalsIgnoreCase(sortBy)) {
					searchCriteria.setSortBy(sortField.getParamName());
					String sortType = restErrorUtil.validateString(
							request.getParameter("sortType"),
							StringUtil.VALIDATION_ALPHA,
							"Invalid value for parameter sortType",
							MessageEnums.INVALID_INPUT_DATA, null, "sortType");
					searchCriteria.setSortType(sortType);
					sortSet = true;
					break;
				}
			}
		}

		if (!sortSet && !stringUtil.isEmpty(sortBy)) {
			logger.info("Invalid or unsupported sortBy field passed. sortBy="
					+ sortBy, new Throwable());
		}

		return searchCriteria;
	}

	

	public Long extractLong(HttpServletRequest request,
			SearchCriteria searchCriteria, String paramName,
			String userFriendlyParamName) {
		String[] values = getParamMultiValues(request, paramName, paramName);
		if (values != null && values.length > 1) {
			List<Long> multiValues = extractLongList(request, searchCriteria,
					paramName, userFriendlyParamName, paramName);
			if (multiValues != null && !multiValues.isEmpty()) {
				return multiValues.get(0);
			} else {
				return null;
			}
		} else {
			Long value = restErrorUtil.parseLong(
					request.getParameter(paramName), "Invalid value for "
							+ userFriendlyParamName,
					MessageEnums.INVALID_INPUT_DATA, null, paramName);
			if (value != null) {
				searchCriteria.getParamList().put(paramName, value);
			}
			return value;
		}
	}

	public Integer extractInt(HttpServletRequest request,
			SearchCriteria searchCriteria, String paramName,
			String userFriendlyParamName) {
		Integer value = restErrorUtil.parseInt(request.getParameter(paramName),
				"Invalid value for " + userFriendlyParamName,
				MessageEnums.INVALID_INPUT_DATA, null, paramName);
		if (value != null) {
			searchCriteria.getParamList().put(paramName, value);
		}
		return value;
	}

	/**
	 *
	 * @param request
	 * @param searchCriteria
	 * @param paramName
	 * @param userFriendlyParamName
	 * @param dateFormat
	 * @return
	 */
	public Date extractDate(HttpServletRequest request,
			SearchCriteria searchCriteria, String paramName,
			String userFriendlyParamName, String dateFormat) {
		Date value = null;
		if (dateFormat == null || dateFormat.isEmpty()) {
			dateFormat = defaultDateFormat;
		}
		value = restErrorUtil.parseDate(request.getParameter(paramName),
                                "Invalid value for " + userFriendlyParamName,
				MessageEnums.INVALID_INPUT_DATA, null, paramName, dateFormat);
		if (value != null) {
			searchCriteria.getParamList().put(paramName, value);
		}

		return value;
	}

	public String extractString(HttpServletRequest request,
			SearchCriteria searchCriteria, String paramName,
			String userFriendlyParamName, String regEx) {
		String value = request.getParameter(paramName);
		if (!stringUtil.isEmpty(value)) {
			value = value.trim();
			// TODO need to handle this in more generic way
			// 		so as to take care of all possible special
			//		characters.
			if(value.contains("%")){
				value = value.replaceAll("%", "\\\\%");
			}
			if (!stringUtil.isEmpty(regEx)) {
				restErrorUtil.validateString(value, regEx, "Invalid value for "
						+ userFriendlyParamName,
						MessageEnums.INVALID_INPUT_DATA, null, paramName);
			}
			searchCriteria.getParamList().put(paramName, value);
		}
		return value;
	}

	public String extractRoleString(HttpServletRequest request,
			SearchCriteria searchCriteria, String paramName,
			String userFriendlyParamName, String regEx) {
		String value = extractString(request, searchCriteria, paramName, userFriendlyParamName, regEx);
		if(!RangerConstants.VALID_USER_ROLE_LIST.contains(value)) {
			restErrorUtil.validateString(value, regEx, "Invalid value for "
					+ userFriendlyParamName,
					MessageEnums.INVALID_INPUT_DATA, null, paramName);
		}
		return value;
	}

	public List<Integer> extractEnum(HttpServletRequest request,
			SearchCriteria searchCriteria, String paramName,
			String userFriendlyParamName, String listName, int maxValue) {

		ArrayList<Integer> valueList = new ArrayList<Integer>();
		String[] values = getParamMultiValues(request, paramName, listName);
		for (int i = 0; values != null && i < values.length; i++) {
			Integer value = restErrorUtil.parseInt(values[i],
					"Invalid value for " + userFriendlyParamName,
					MessageEnums.INVALID_INPUT_DATA, null, paramName);

			restErrorUtil.validateMinMax(value == null ? Integer.valueOf(-1) : value, 0, maxValue,
					"Invalid value for " + userFriendlyParamName, null,
					paramName);
			valueList.add(value);
		}
		if (!valueList.isEmpty()) {
			searchCriteria.getParamList().put(listName, valueList);
		}
		return valueList;
	}

	/**
	 * @param request
	 * @param paramName
	 * @param listName
	 * @return
	 */
	String[] getParamMultiValues(HttpServletRequest request, String paramName,
			String listName) {
		String[] values = request.getParameterValues(paramName);
		if (values == null || values.length == 0) {
			values = request.getParameterValues(paramName + "[]");
			if (listName != null && (values == null || values.length == 0)) {
				values = request.getParameterValues(listName);
				if (values == null || values.length == 0) {
					// Let's try after appending []
					values = request.getParameterValues(listName + "[]");
				}
			}
		}
		return values;
	}

	public List<String> extractStringList(HttpServletRequest request,
			SearchCriteria searchCriteria, String paramName,
			String userFriendlyParamName, String listName,
			String[] validValues, String regEx) {
		ArrayList<String> valueList = new ArrayList<String>();
		String[] values = getParamMultiValues(request, paramName, listName);

		for (int i = 0; values != null && i < values.length; i++) {
			if (!stringUtil.isEmpty(regEx)) {
				restErrorUtil.validateString(values[i], regEx,
						"Invalid value for " + userFriendlyParamName,
						MessageEnums.INVALID_INPUT_DATA, null, paramName);
			}
			valueList.add(values[i]);
		}
		searchCriteria.getParamList().put(listName, valueList);
		return valueList;
	}

	public List<Long> extractLongList(HttpServletRequest request,
			SearchCriteria searchCriteria, String paramName,
			String userFriendlyParamName, String listName) {
		ArrayList<Long> valueList = new ArrayList<Long>();
		String[] values = getParamMultiValues(request, paramName, listName);

		for (int i = 0; values != null && i < values.length; i++) {
			Long value = restErrorUtil.parseLong(
					values[i], "Invalid value for "
							+ userFriendlyParamName,
					MessageEnums.INVALID_INPUT_DATA, null, paramName);
			valueList.add(value);
		}
		searchCriteria.getParamList().put(listName, valueList);
		return valueList;
	}

	public void updateQueryPageSize(Query query, SearchCriteria searchCriteria) {
		// Set max records
		int pageSize = validatePageSize(searchCriteria.getMaxRows());

		query.setMaxResults(pageSize);

		// Set hint for max records
		query.setHint("eclipselink.jdbc.max-rows", "" + pageSize);

	}

	public int validatePageSize(int inputPageSize) {
		int pageSize = inputPageSize;

		if (pageSize < 1) {
			// Use default max Records
			pageSize = configUtil.getDefaultMaxRows();
		}
		return pageSize;
	}

	/**
	 * @param searchCriteria
	 * @param sortFields
	 * @return
	 */
	public String constructSortClause(SearchCriteria searchCriteria,
			List<SortField> sortFields) {
		String sortBy = searchCriteria.getSortBy();
		String querySortBy = null;
		if (!stringUtil.isEmpty(sortBy)) {
			sortBy = sortBy.trim();
			for (SortField sortField : sortFields) {
				if (sortBy.equalsIgnoreCase(sortField.getParamName())) {
					querySortBy = sortField.getFieldName();
					// Override the sortBy using the normalized value
					searchCriteria.setSortBy(sortField.getParamName());
					break;
				}
			}
		}

		if (querySortBy == null) {
			for (SortField sortField : sortFields) {
				if (sortField.isDefault()) {
					querySortBy = sortField.getFieldName();
					// Override the sortBy using the default value
					searchCriteria.setSortBy(sortField.getParamName());
					searchCriteria.setSortType(sortField.getDefaultOrder()
							.name());
					break;
				}
			}
		}

		if (querySortBy != null) {
			// Add sort type
			String sortType = searchCriteria.getSortType();
			String querySortType = "asc";
			if (sortType != null) {
				if ("asc".equalsIgnoreCase(sortType)
						|| "desc".equalsIgnoreCase(sortType)) {
					querySortType = sortType;
				} else {
					logger.error("Invalid sortType. sortType=" + sortType);
				}
			}
			// Override the sortType using the final value
			if(querySortType!=null){
				searchCriteria.setSortType(querySortType.toLowerCase());
			}
			String sortClause = " ORDER BY " + querySortBy + " "
					+ querySortType;

			return sortClause;
		}
		return null;
	}

	protected StringBuilder buildWhereClause(SearchCriteria searchCriteria,
			List<SearchField> searchFields) {
		return buildWhereClause(searchCriteria, searchFields, false, false);
	}

	@SuppressWarnings("unchecked")
	protected StringBuilder buildWhereClause(SearchCriteria searchCriteria,
			List<SearchField> searchFields, boolean isNativeQuery,
			boolean excludeWhereKeyword) {

		Map<String, Object> paramList = searchCriteria.getParamList();

		StringBuilder whereClause = new StringBuilder(excludeWhereKeyword ? ""
				: "WHERE 1 = 1 ");

		List<String> joinTableList = new ArrayList<String>();

		String addedByFieldName = isNativeQuery ? "added_by_id"
				: "addedByUserId";

		Number ownerId = searchCriteria.getOwnerId();
		if (ownerId != null) {
			whereClause.append(" and obj.").append(addedByFieldName)
					.append(" = :ownerId");
		}

		// Let's handle search groups first
		int groupCount = -1;
		for (SearchGroup searchGroup : searchCriteria.getSearchGroups()) {
			groupCount++;
			whereClause.append(" and ").append(
					searchGroup.getWhereClause("" + groupCount));
//			searchGroup.getJoinTableList(joinTableList, searchGroup);
		}
		
	

		for (SearchField searchField : searchFields) {
			int startWhereLen = whereClause.length();

			if (searchField.getFieldName() == null
					&& searchField.getCustomCondition() == null) { // this field
				// is used
				// only for
				// binding!
				continue;
			}

			Object paramValue = paramList.get(searchField.getClientFieldName());
			boolean isListValue = false;
			if (paramValue != null && paramValue instanceof Collection) {
				isListValue = true;
			}

			if (searchCriteria.getNullParamList().contains(
					searchField.getClientFieldName())) {
				whereClause.append(" and ").append(searchField.getFieldName())
						.append(" is null");
			} else if (searchCriteria.getNotNullParamList().contains(
					searchField.getClientFieldName())) {
				whereClause.append(" and ").append(searchField.getFieldName())
						.append(" is not null");

			} else if (searchField.getDataType() == SearchField.DATA_TYPE.INT_LIST
					|| isListValue
					&& searchField.getDataType() == SearchField.DATA_TYPE.INTEGER) {
				Collection<Number> intValueList = null;
				if (paramValue != null
						&& (paramValue instanceof Integer || paramValue instanceof Long)) {
					intValueList = new ArrayList<Number>();
					intValueList.add((Number) paramValue);
				} else {
					intValueList = (Collection<Number>) paramValue;
				}

				if (intValueList != null && !intValueList.isEmpty()) {
					if (searchField.getCustomCondition() == null) {
						if (intValueList.size() <= minInListLength) {
							whereClause.append(" and ");
							if (intValueList.size() > 1) {
								whereClause.append(" ( ");
							}
							for (int count = 0; count < intValueList.size(); count++) {
								if (count > 0) {
									whereClause.append(" or ");
								}
								whereClause
										.append(searchField.getFieldName())
										.append(" = :")
										.append(searchField
												.getClientFieldName()
												+ "_"
												+ count);
							}

							if (intValueList.size() > 1) {
								whereClause.append(" ) ");
							}

						} else {
							whereClause.append(" and ")
									.append(searchField.getFieldName())
									.append(" in ( :")
									.append(searchField.getClientFieldName())
									.append(")");
						}
					} else {
						whereClause.append(" and ").append(
								searchField.getCustomCondition());
					}
				}

			} else if (searchField.getDataType() == SearchField.DATA_TYPE.STR_LIST) {
				if (paramValue != null
						&& (((Collection) paramValue).size()) >=1) {
					whereClause.append(" and ")
							.append(searchField.getFieldName())
							.append(" in :")
							.append(searchField.getClientFieldName());
				}
			}
			else if (searchField.getDataType() == SearchField.DATA_TYPE.INTEGER) {
				Number intFieldValue = (Number) paramList.get(searchField
						.getClientFieldName());
				if (intFieldValue != null) {
					if (searchField.getCustomCondition() == null) {
						whereClause.append(" and ")
								.append(searchField.getFieldName())
								.append("=:")
								.append(searchField.getClientFieldName());
					} else {
						whereClause.append(" and ").append(
								searchField.getCustomCondition());
					}
				}
			} else if (searchField.getDataType() == SearchField.DATA_TYPE.STRING) {
				String strFieldValue = (String) paramList.get(searchField
						.getClientFieldName());
				if (strFieldValue != null) {
					if (searchField.getCustomCondition() == null) {
						whereClause.append(" and ").append("LOWER(")
								.append(searchField.getFieldName()).append(")");
						if (searchField.getSearchType() == SearchField.SEARCH_TYPE.FULL) {
							whereClause.append("= :").append(
									searchField.getClientFieldName());
						} else {
							whereClause.append("like :").append(
									searchField.getClientFieldName());
						}
					} else {
						whereClause.append(" and ").append(
								searchField.getCustomCondition());
					}
				}
			} else if (searchField.getDataType() == SearchField.DATA_TYPE.BOOLEAN) {
				Boolean boolFieldValue = (Boolean) paramList.get(searchField
						.getClientFieldName());
				if (boolFieldValue != null) {
					if (searchField.getCustomCondition() == null) {
						whereClause.append(" and ")
								.append(searchField.getFieldName())
								.append("=:")
								.append(searchField.getClientFieldName());
					} else {
						whereClause.append(" and ").append(
								searchField.getCustomCondition());
					}
				}
			} else if (searchField.getDataType() == SearchField.DATA_TYPE.DATE) {
				Date fieldValue = (Date) paramList.get(searchField
						.getClientFieldName());
				if (fieldValue != null) {
					if (searchField.getCustomCondition() == null) {
						whereClause.append(" and ").append(
								searchField.getFieldName());
						if (SearchField.SEARCH_TYPE.LESS_THAN.equals(searchField.getSearchType())) {
							whereClause.append("< :");
						} else if (SearchField.SEARCH_TYPE.LESS_EQUAL_THAN.equals(searchField.getSearchType())) {
							whereClause.append("<= :");
						} else if (SearchField.SEARCH_TYPE.GREATER_THAN.equals(searchField.getSearchType())) {
							whereClause.append("> :");
						} else if (SearchField.SEARCH_TYPE.GREATER_EQUAL_THAN.equals(searchField.getSearchType())) {
							whereClause.append(">= :");
						}
						whereClause.append(searchField.getClientFieldName());
					} else {
						whereClause.append(" and ").append(
								searchField.getCustomCondition());
					}
				}

			}

			if (whereClause.length() > startWhereLen
					&& searchField.getJoinTables() != null) {
				for (String table : searchField.getJoinTables()) {
					if (!joinTableList.contains(table)) {
						joinTableList.add(table);
					}
				}

				whereClause.append(" and (")
						.append(searchField.getJoinCriteria()).append(")");
			}
		} // for

		for (String joinTable : joinTableList) {
			whereClause.insert(0, ", " + joinTable + " ");
		}

		return whereClause;
	}

	protected void addOrderByClause(StringBuilder queryClause, String sortClause) {
		if (sortClause != null) {
			queryClause.append(sortClause);
		}
	}

	@SuppressWarnings("unchecked")
	protected void resolveQueryParams(Query query, SearchCriteria searchCriteria,
			List<SearchField> searchFields) {

		Map<String, Object> paramList = searchCriteria.getParamList();

		Number ownerId = searchCriteria.getOwnerId();
		if (ownerId != null) {
			query.setParameter("ownerId", ownerId);
		}

		// Let's handle search groups first
		int groupCount = -1;
		for (SearchGroup searchGroup : searchCriteria.getSearchGroups()) {
			groupCount++;
			searchGroup.resolveValues(query, "" + groupCount);
		}

		for (SearchField searchField : searchFields) {
			Object paramValue = paramList.get(searchField.getClientFieldName());
			boolean isListValue = false;
			if (paramValue != null && paramValue instanceof Collection) {
				isListValue = true;
			}

			if (searchCriteria.getNullParamList().contains(
					searchField.getClientFieldName())
					|| searchCriteria.getNotNullParamList().contains(
							searchField.getClientFieldName())) { //NOPMD
				// Already addressed while building where clause
			} else if (searchField.getDataType() == SearchField.DATA_TYPE.INT_LIST
					|| isListValue
					&& searchField.getDataType() == SearchField.DATA_TYPE.INTEGER) {
				Collection<Number> intValueList = null;
				if (paramValue != null
						&& (paramValue instanceof Integer || paramValue instanceof Long)) {
					intValueList = new ArrayList<Number>();
					intValueList.add((Number) paramValue);
				} else {
					intValueList = (Collection<Number>) paramValue;
				}

				if (intValueList != null && !intValueList.isEmpty()
						&& intValueList.size() <= minInListLength) {
					int count = -1;
					for (Number value : intValueList) {
						count++;
						query.setParameter(searchField.getClientFieldName()
								+ "_" + count, value);

					}

				} else if (intValueList != null && intValueList.size() > 1) {
					query.setParameter(searchField.getClientFieldName(),
							intValueList);
				}

			}else if (searchField.getDataType() == SearchField.DATA_TYPE.STR_LIST) {
				if (paramValue != null
						&& (((Collection) paramValue).size()) >=1) {
					query.setParameter(searchField.getClientFieldName(),
							paramValue);
				}			
			}
			else if (searchField.getDataType() == SearchField.DATA_TYPE.INTEGER) {
				Number intFieldValue = (Number) paramList.get(searchField
						.getClientFieldName());
				if (intFieldValue != null) {
					query.setParameter(searchField.getClientFieldName(),
							intFieldValue);
				}
			} else if (searchField.getDataType() == SearchField.DATA_TYPE.STRING) {
				String strFieldValue = (String) paramList.get(searchField
						.getClientFieldName());
				if (strFieldValue != null) {
					if (searchField.getSearchType() == SearchField.SEARCH_TYPE.FULL) {
						query.setParameter(searchField.getClientFieldName(),
								strFieldValue.trim().toLowerCase());
					} else {
						query.setParameter(searchField.getClientFieldName(),
								"%" + strFieldValue.trim().toLowerCase() + "%");
					}
				}
			} else if (searchField.getDataType() == SearchField.DATA_TYPE.BOOLEAN) {
				Boolean boolFieldValue = (Boolean) paramList.get(searchField
						.getClientFieldName());
				if (boolFieldValue != null) {
					query.setParameter(searchField.getClientFieldName(),
							boolFieldValue);
				}
			} else if (searchField.getDataType() == SearchField.DATA_TYPE.DATE) {
				Date fieldValue = (Date) paramList.get(searchField
						.getClientFieldName());
				if (fieldValue != null) {
					query.setParameter(searchField.getClientFieldName(),
							fieldValue);
				}
			}

		} // for
	}

	public Query createSearchQuery(EntityManager em, String queryStr, String sortClause,
			SearchCriteria searchCriteria, List<SearchField> searchFields,
			boolean hasAttributes, boolean isCountQuery) {

		// [1] Build where clause
		StringBuilder queryClause = buildWhereClause(searchCriteria,
				searchFields);

		// [2] Add domain-object-security clause if needed
		// if (objectClassType != -1
		// && !ContextUtil.getCurrentUserSession().isUserAdmin()) {
		// addDomainObjectSecuirtyClause(queryClause, hasAttributes);
		// }

		// [2] Add order by clause
		addOrderByClause(queryClause, sortClause);

		// [3] Create Query Object
		Query query = em.createQuery(
				queryStr + queryClause);

		// [4] Resolve query parameters with values
		resolveQueryParams(query, searchCriteria, searchFields);

		// [5] Resolve domain-object-security parameters
		// if (objectClassType != -1 &&
		// !securityHandler.hasModeratorPermission()) {
		// resolveDomainObjectSecuirtyParams(query, objectClassType);
		// }

		if (!isCountQuery) {
			query.setFirstResult(searchCriteria.getStartIndex());
			updateQueryPageSize(query, searchCriteria);
		}

		return query;
	}
	
	public List<Integer> extractIntList(HttpServletRequest request,
			SearchCriteria searchCriteria, String paramName,
			String userFriendlyParamName, String listName) {
		ArrayList<Integer> valueList = new ArrayList<Integer>();
		String[] values = getParamMultiValues(request, paramName, listName);

		for (int i = 0; values != null && i < values.length; i++) {
			Integer value = restErrorUtil.parseInt(
					values[i], "Invalid value for "
							+ userFriendlyParamName,
					MessageEnums.INVALID_INPUT_DATA, null, paramName);
			valueList.add(value);
		}
		searchCriteria.getParamList().put(listName, valueList);
		return valueList;
	}		

	public Boolean extractBoolean(HttpServletRequest request,
			SearchCriteria searchCriteria, String paramName,
			String userFriendlyParamName) {
		Boolean value = restErrorUtil.parseBoolean(
				request.getParameter(paramName), "Invalid value for "
						+ userFriendlyParamName,
				MessageEnums.INVALID_INPUT_DATA, null, paramName);
		if (value != null) {
			searchCriteria.getParamList().put(paramName, value);
		}
		return value;
	}
	
}
