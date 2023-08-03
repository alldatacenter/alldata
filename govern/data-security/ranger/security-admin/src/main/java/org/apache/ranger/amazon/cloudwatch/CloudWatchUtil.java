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

package org.apache.ranger.amazon.cloudwatch;

import static org.apache.ranger.audit.destination.AmazonCloudWatchAuditDestination.CONFIG_PREFIX;
import static org.apache.ranger.audit.destination.AmazonCloudWatchAuditDestination.PROP_LOG_GROUP_NAME;
import static org.apache.ranger.audit.destination.AmazonCloudWatchAuditDestination.PROP_LOG_STREAM_PREFIX;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SearchField.SEARCH_TYPE;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.StringUtil;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.FilterLogEventsRequest;
import com.amazonaws.services.logs.model.FilterLogEventsResult;
import com.amazonaws.services.logs.model.FilteredLogEvent;

@Component
public class CloudWatchUtil {
	private static final Logger LOGGER = Logger.getLogger(CloudWatchUtil.class);

	@Autowired
	StringUtil stringUtil;

	String dateFormateStr = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormateStr);
	private String logGroupName;
	private String logStreamPrefix;

	public CloudWatchUtil() {
		logGroupName = PropertiesUtil.getProperty(CONFIG_PREFIX + "." + PROP_LOG_GROUP_NAME, "ranger_audits");
		logStreamPrefix = PropertiesUtil.getProperty(CONFIG_PREFIX + "." + PROP_LOG_STREAM_PREFIX, "");
		String timeZone = PropertiesUtil.getProperty("ranger.cloudwatch.timezone");
		if (timeZone != null) {
			LOGGER.info("Setting timezone to " + timeZone);
			try {
				dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
			} catch (Throwable t) {
				LOGGER.error("Error setting timezone. TimeZone = " + timeZone);
			}
		}
	}

	public List<FilteredLogEvent> searchResources(AWSLogs client, SearchCriteria searchCriteria,
			List<SearchField> searchFields, List<SortField> sortFieldList) {
		List<FilteredLogEvent> result = new ArrayList<FilteredLogEvent>();
		try {
			String nextToken = null;
			FilterLogEventsRequest filterLogEventsRequest = getFilterLogEventsRequest(client, searchCriteria, searchFields);
			boolean done = false;
			//TODO: Improve response time
			//This approach is slow as cloudwatch doesn't provide timestamp based sorting in descending order
			do {
				if (nextToken != null) {
					filterLogEventsRequest = filterLogEventsRequest.withNextToken(nextToken);
				}

				FilterLogEventsResult response = client.filterLogEvents(filterLogEventsRequest);
				if (response != null) {
					if (CollectionUtils.isNotEmpty(response.getEvents())) {
						//To handle outofmemory issue, max 10k records are stored in the list
						if (result.size() > 10000) {
							result.clear();
						}
						result.addAll(response.getEvents());
					} else {
						done = true;
						break;
					}
					// check if token is the same
					if (response.getNextToken().equals(nextToken)) {
						done = true;
						break;
					}
					// save new token
					nextToken = response.getNextToken();
					if (nextToken == null) {
						done = true;
						break;
					}
				}
			} while (!done);
			LOGGER.info("Successfully got CloudWatch log events!");
		} catch (Exception e) {
			LOGGER.error("Error searching records from CloudWatch", e);
		}
		return result;
	}

	public FilterLogEventsRequest getFilterLogEventsRequest(AWSLogs client, SearchCriteria searchCriteria,
			List<SearchField> searchFields) {
		FilterLogEventsRequest filterLogEventsRequest = null;
		StringBuilder filterPattern = new StringBuilder("");
		Date fromDate = null;
		Date toDate = null;

		if (searchCriteria.getParamList() != null) {
			List<String> filterExpr = new ArrayList<String>();

			for (SearchField searchField : searchFields) {
				Object paramValue = searchCriteria.getParamValue(searchField.getClientFieldName());
				if (paramValue == null || paramValue.toString().isEmpty()) {
					continue;
				}

				String fieldName = searchField.getFieldName();
				if (searchField.getDataType() == SearchField.DATA_TYPE.DATE) {
					if (!(paramValue instanceof Date)) {
						LOGGER.error("Search field is not a Java Date Object, paramValue = " + paramValue);
					} else {
						if (searchField.getSearchType() == SEARCH_TYPE.GREATER_EQUAL_THAN || searchField.getSearchType() == SEARCH_TYPE.GREATER_THAN) {
							fromDate = (Date) paramValue;
						} else if (searchField.getSearchType() == SEARCH_TYPE.LESS_EQUAL_THAN || searchField.getSearchType() == SEARCH_TYPE.LESS_THAN) {
							toDate = (Date) paramValue;
						}
					}
				} else if (paramValue instanceof Collection) {
					String fq = orList(fieldName, (Collection<?>) paramValue);
					if (StringUtils.isNotBlank(fq)) {
						filterExpr.add(fq);
					}
				} else {
					String fq = null;
					if (searchField.getSearchType() == SEARCH_TYPE.PARTIAL) {
						fq = setFieldForPartialSearch(fieldName, paramValue);
					} else {
						fq = setField(fieldName, paramValue);
					}
					if (StringUtils.isNotBlank(fq)) {
						filterExpr.add(fq);
					}
				}
			}

			if (fromDate == null) {
				fromDate = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
			}
			if (toDate == null) {
				Date today = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
				toDate = DateUtils.addDays(today, 1);
			}

			// Syntax : { ($.user.id = 1) && ($.users[0].email = "user@example.com") }
			if (CollectionUtils.isNotEmpty(filterExpr)) {
				String strExpr = "";
				int count = -1;
				for (String fq : filterExpr) {
					count++;
					if (count > 0) {
						strExpr += " &&";
					}
					strExpr = strExpr.concat("(" + fq + ")");
				}
				if (strExpr.endsWith("&&")) {
					strExpr = strExpr.substring(0, strExpr.length() - 3);
				}
				if (StringUtils.isNotBlank(strExpr)) {
					filterPattern.append("{" + strExpr + "}");
				}
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("filterExpression for cloudwatch request " + filterPattern.toString());
		}

		// Add FilterPattern which will only fetch logs required
		filterLogEventsRequest = new FilterLogEventsRequest()
				.withLogGroupName(logGroupName)
				.withStartTime(fromDate.getTime())
				.withEndTime(toDate.getTime())
				.withFilterPattern(filterPattern.toString());

		if (StringUtils.isNotBlank(logStreamPrefix)) {
			filterLogEventsRequest.setLogStreamNamePrefix(logStreamPrefix);
		}

		return filterLogEventsRequest;
	}

	//Syntax { $.user.email = "user@example.com" || $.coordinates[0][1] = nonmatch && $.actions[2] = nomatch }
	private String orList(String fieldName, Collection<?> valueList) {
		if (valueList == null || valueList.isEmpty()) {
			return null;
		}
		String expr = "";
		int count = -1;
		for (Object value : valueList) {
			count++;
			if (count > 0) {
				expr += " || ";
			}
			expr += setField(fieldName, value);
		}
		return expr;
	}

	private String setField(String fieldName, Object value) {
		if (value == null || StringUtils.isBlank(value.toString())) {
			return null;
		}
		if (value instanceof Integer || value instanceof Long) {
			if (fieldName.startsWith("-")) {
				fieldName = fieldName.substring(1);
				return "$." + fieldName + " != " + ClientUtils.escapeQueryChars(value.toString().trim().toLowerCase());
			}
			return "$." + fieldName + " = " + ClientUtils.escapeQueryChars(value.toString().trim().toLowerCase());
		}
		if (fieldName.startsWith("-")) {
			fieldName = fieldName.substring(1);
			return "$." + fieldName + " != \"" + ClientUtils.escapeQueryChars(value.toString().trim().toLowerCase()) + "\"";
		}
		return "$." + fieldName + " = \"" + ClientUtils.escapeQueryChars(value.toString().trim().toLowerCase()) + "\"";
	}

	private String setFieldForPartialSearch(String fieldName, Object value) {
		if (value == null || StringUtils.isBlank(value.toString())) {
			return null;
		}
		return "$." + fieldName + "= \"*" + ClientUtils.escapeQueryChars(value.toString().trim().toLowerCase()) + "*\"";
	}

}
