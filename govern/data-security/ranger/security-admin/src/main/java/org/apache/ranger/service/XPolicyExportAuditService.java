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

 package org.apache.ranger.service;

import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SearchField.DATA_TYPE;
import org.apache.ranger.common.SearchField.SEARCH_TYPE;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.SortField.SORT_ORDER;
import org.apache.ranger.entity.XXPolicyExportAudit;
import org.apache.ranger.view.VXPolicyExportAudit;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class XPolicyExportAuditService extends XPolicyExportAuditServiceBase<XXPolicyExportAudit, VXPolicyExportAudit> {

	public XPolicyExportAuditService(){
		searchFields.add(new SearchField("httpRetCode", "obj.httpRetCode",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("clientIP", "obj.clientIP",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("agentId", "obj.agentId",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("repositoryName", "obj.repositoryName",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("cluster", "obj.clusterName",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("startDate", "obj.createTime",
				DATA_TYPE.DATE, SEARCH_TYPE.GREATER_EQUAL_THAN));
		searchFields.add(new SearchField("endDate", "obj.createTime",
				DATA_TYPE.DATE, SEARCH_TYPE.LESS_EQUAL_THAN));
		
		sortFields.add(new SortField("createDate", "obj.createTime", true, SORT_ORDER.DESC));
	}
	
	@Override
	protected void validateForCreate(VXPolicyExportAudit vObj) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void validateForUpdate(VXPolicyExportAudit vObj, XXPolicyExportAudit mObj) {
		// TODO Auto-generated method stub

	}

}
