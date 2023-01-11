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

package org.apache.ranger.solr;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.SearchField.SEARCH_TYPE;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SolrUtil {
	private static final Logger logger = LoggerFactory.getLogger(SolrUtil.class);

	@Autowired
	RESTErrorUtil restErrorUtil;

	@Autowired
	StringUtil stringUtil;

	@Autowired
	SolrMgr solrMgr;

	SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");

	public SolrUtil() {
		String timeZone = PropertiesUtil.getProperty("xa.solr.timezone");
		if (timeZone != null) {
			logger.info("Setting timezone to " + timeZone);
			try {
				dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
			} catch (Throwable t) {
                                logger.error("Error setting timezone. TimeZone = " + timeZone);
			}
		}
	}

    public QueryResponse runQuery(SolrClient solrClient, SolrQuery solrQuery) throws Throwable {
        if (solrQuery != null) {
            try {
                QueryRequest req      = new QueryRequest(solrQuery, METHOD.POST);
                String       username = PropertiesUtil.getProperty("ranger.solr.audit.user");
                String       password = PropertiesUtil.getProperty("ranger.solr.audit.user.password");
                if (username != null && password != null) {
                    req.setBasicAuthCredentials(username, password);
                }

                return solrMgr.queryToSolr(req);
            } catch (Throwable e) {
                logger.error("Error from Solr server. ", e);
                throw e;
            }
        }
        return null;
    }

	public QueryResponse searchResources(SearchCriteria searchCriteria,
			List<SearchField> searchFields, List<SortField> sortFieldList,
			SolrClient solrClient) {
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*");
		if (searchCriteria.getParamList() != null) {
			// For now assuming there is only date field where range query will
			// be done. If we there are more than one, then we should create a
			// hashmap for each field name
			Date fromDate = null;
			Date toDate = null;
			String dateFieldName = null;

			for (SearchField searchField : searchFields) {
				Object paramValue = searchCriteria.getParamValue(searchField.getClientFieldName());
				if (paramValue == null || paramValue.toString().isEmpty()) {
					continue;
				}
				String fieldName = searchField.getFieldName();
				if (paramValue instanceof Collection) {
					String fq = orList(fieldName, (Collection<?>) paramValue);
					if (fq != null) {
						query.addFilterQuery(fq);
					}
				} else if (searchField.getDataType() == SearchField.DATA_TYPE.DATE) {
					if (!(paramValue instanceof Date)) {
                                                logger.error("Search field is not a Java Date Object, paramValue = " + paramValue);
					} else {
						if (searchField.getSearchType() == SEARCH_TYPE.GREATER_EQUAL_THAN
								|| searchField.getSearchType() == SEARCH_TYPE.GREATER_THAN) {
							fromDate = (Date) paramValue;
							dateFieldName = fieldName;
						} else if (searchField.getSearchType() == SEARCH_TYPE.LESS_EQUAL_THAN
								|| searchField.getSearchType() == SEARCH_TYPE.LESS_THAN) {
							toDate = (Date) paramValue;
							dateFieldName = fieldName;
						}
					}
				} else if (searchField.getSearchType() == SEARCH_TYPE.GREATER_EQUAL_THAN
						|| searchField.getSearchType() == SEARCH_TYPE.GREATER_THAN
						|| searchField.getSearchType() == SEARCH_TYPE.LESS_EQUAL_THAN
						|| searchField.getSearchType() == SEARCH_TYPE.LESS_THAN) { //NOPMD
					// TODO: Need to handle range here
				} else {
					String fq = setField(fieldName, paramValue);

					if (searchField.getSearchType() == SEARCH_TYPE.PARTIAL) {
						fq = setFieldForPartialSearch(fieldName, paramValue);
					}

					if (fq != null) {
						query.addFilterQuery(fq);
					}
				}
			}
			if (fromDate != null || toDate != null) {
				String fq = setDateRange(dateFieldName, fromDate, toDate);
				if (fq != null) {
					query.addFilterQuery(fq);
				}
			}
		}

		setSortClause(searchCriteria, sortFieldList, query);
		query.setStart(searchCriteria.getStartIndex());
		query.setRows(searchCriteria.getMaxRows());

		// Fields to get
		// query.setFields("myClassType", "id", "score", "globalId");
		if (logger.isDebugEnabled()) {
                        logger.debug("SOLR QUERY = " + query);
                }
                QueryResponse response = null;
                try {
                        response = runQuery(solrClient, query);
                } catch (Throwable e) {
                        logger.error("Error running solr query. Query = " + query
                                        + ", response = " + response);
                        throw restErrorUtil.createRESTException(
                                        "Error running solr query, please check solr configs. "
                                                        + e.getMessage(), MessageEnums.ERROR_SYSTEM);
		}
		if (response == null || response.getStatus() != 0) {
                        logger.error("Error running solr query. Query = " + query
                                        + ", response = " + response);
                        throw restErrorUtil.createRESTException(
                                        "Unable to connect to Audit store !!",
					MessageEnums.ERROR_SYSTEM);
		}
		return response;
	}

	private String setFieldForPartialSearch(String fieldName, Object value) {
		if (value == null || value.toString().trim().length() == 0) {
			return null;
		}
		return fieldName + ":*" + ClientUtils.escapeQueryChars(value.toString().trim().toLowerCase()) + "*";
	}

	public String setField(String fieldName, Object value) {
		if (value == null || value.toString().trim().length() == 0) {
			return null;
		}
		return fieldName
				+ ":"
				+ ClientUtils.escapeQueryChars(value.toString().trim()
						.toLowerCase());
	}

	public String setDateRange(String fieldName, Date fromDate, Date toDate) {
		String fromStr = "*";
		String toStr = "NOW";
		if (fromDate != null) {
			fromStr = dateFormat.format(fromDate);
		}
		if (toDate != null) {
			toStr = dateFormat.format(toDate);
		}
		return fieldName + ":[" + fromStr + " TO " + toStr + "]";
	}

	public String orList(String fieldName, Collection<?> valueList) {
		if (valueList == null || valueList.isEmpty()) {
			return null;
		}
		String expr = "";
		int count = -1;
		for (Object value : valueList) {
			count++;
			if (count > 0) {
				expr += " OR ";
			}
			expr += fieldName
					+ ":"
					+ ClientUtils.escapeQueryChars(value.toString()
							.toLowerCase());
		}
		if (valueList.isEmpty()) {
			return expr;
		} else {
			return "(" + expr + ")";
		}

	}

	public String andList(String fieldName, Collection<?> valueList) {
		if (valueList == null || valueList.isEmpty()) {
			return null;
		}
		String expr = "";
		int count = -1;
		for (Object value : valueList) {
			count++;
			if (count > 0) {
				expr += " AND ";
			}
			expr += fieldName
					+ ":"
					+ ClientUtils.escapeQueryChars(value.toString()
							.toLowerCase());
		}
		if (valueList.isEmpty()) {
			return expr;
		} else {
			return "(" + expr + ")";
		}
	}

	public void setSortClause(SearchCriteria searchCriteria,
			List<SortField> sortFields, SolrQuery query) {

		// TODO: We are supporting single sort field only for now
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
			ORDER order = ORDER.asc;
			if (sortType != null && "desc".equalsIgnoreCase(sortType)) {
				order = ORDER.desc;

			}
			query.addSort(querySortBy, order);
		}
	}

}
