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

package org.apache.ranger.elasticsearch;

import static org.apache.ranger.audit.destination.ElasticSearchAuditDestination.CONFIG_PREFIX;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import org.apache.ranger.audit.destination.ElasticSearchAuditDestination;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.view.VXAccessAuditList;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticSearchAccessAuditsServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchAccessAuditsServiceTest.class);

    @Test
    @Ignore // For manual execution only
    public void testQuery() {
        ElasticSearchAccessAuditsService elasticSearchAccessAuditsService = new ElasticSearchAccessAuditsService();
        Map<String, String> properties = PropertiesUtil.getPropertiesMap();
        properties.put("ranger.audit.elasticsearch.urls", "localhost");
        properties.put("ranger.audit.elasticsearch.user", "elastic");
        properties.put("ranger.audit.elasticsearch.password", "password1");
        properties.put("ranger.audit.elasticsearch.port", "9200");
        elasticSearchAccessAuditsService.elasticSearchMgr = new ElasticSearchMgr();
        elasticSearchAccessAuditsService.elasticSearchUtil = new ElasticSearchUtil();
        elasticSearchAccessAuditsService.elasticSearchUtil.stringUtil = new StringUtil();
        elasticSearchAccessAuditsService.setRestErrorUtil(new RESTErrorUtil());
        LOGGER.info("Running searchXAccessAudits:");
        VXAccessAuditList vxAccessAuditList = elasticSearchAccessAuditsService.searchXAccessAudits(getSearchCriteria());
        LOGGER.info(String.format("searchXAccessAudits results (%d items):", vxAccessAuditList.getListSize()));
        ObjectWriter writer = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writer();
        vxAccessAuditList.getVXAccessAudits().forEach(x -> {
            try {
                LOGGER.warn(writer.writeValueAsString(x));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }

    private SearchCriteria getSearchCriteria() {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setDistinct(false);
        searchCriteria.setGetChildren(false);
        searchCriteria.setGetCount(true);
        searchCriteria.setMaxRows(25);
        searchCriteria.setOwnerId(null);
        searchCriteria.setSortBy("eventTime");
        searchCriteria.setSortType("desc");
        searchCriteria.setStartIndex(0);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2019, 11,13);
        searchCriteria.getParamList().put("startDate", calendar.getTime());
        searchCriteria.getParamList().put("-repoType", 7);
        searchCriteria.getParamList().put("-requestUser", new ArrayList<>());
        searchCriteria.getParamList().put("requestUser", new ArrayList<>());
        searchCriteria.getParamList().put("zoneName", new ArrayList<>());
        return searchCriteria;
    }

    @Test
    @Ignore // For manual execution only
    public void testWrite() {
        ElasticSearchAuditDestination elasticSearchAuditDestination = new ElasticSearchAuditDestination();
        Properties properties = new Properties();
        properties.put(CONFIG_PREFIX + "." + ElasticSearchAuditDestination.CONFIG_URLS, "localhost");
        properties.put(CONFIG_PREFIX + "." + ElasticSearchAuditDestination.CONFIG_USER, "elastic");
        properties.put(CONFIG_PREFIX + "." + ElasticSearchAuditDestination.CONFIG_PWRD, "password1");
        elasticSearchAuditDestination.init(properties, CONFIG_PREFIX);
        assert elasticSearchAuditDestination.log(Arrays.asList(getAuthzAuditEvent()));
    }

    private AuthzAuditEvent getAuthzAuditEvent() {
        AuthzAuditEvent event = new AuthzAuditEvent();
        event.setAccessResult((short) 1);
        event.setAccessType("");
        event.setAclEnforcer("");
        event.setAction("");
        event.setAdditionalInfo("");
        event.setAgentHostname("");
        event.setAgentId("");
        event.setClientIP("");
        event.setClusterName("");
        event.setClientType("");
        event.setEventCount(1);
        event.setEventDurationMS(1);
        event.setEventId("");
        event.setEventTime(new Date());
        event.setLogType("");
        event.setPolicyId(1);
        event.setPolicyVersion(1l);
        event.setRepositoryName("");
        event.setRequestData("");
        event.setRepositoryName("");
        event.setRepositoryType(1);
        event.setResourcePath("");
        event.setResultReason("");
        event.setSeqNum(1);
        event.setSessionId("");
        event.setTags(new HashSet<>());
        event.setUser("");
        event.setZoneName("");
        return event;
    }
}
