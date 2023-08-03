/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.service;

import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SearchField.DATA_TYPE;
import org.apache.ranger.common.SearchField.SEARCH_TYPE;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.SortField.SORT_ORDER;
import org.apache.ranger.entity.XXPolicyLabel;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.util.SearchFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class RangerPolicyLabelsService<T extends XXPolicyLabel, V extends RangerPolicy>
		extends RangerBaseModelService<T, V> {

	@Autowired
	RangerAuditFields<?> rangerAuditFields;

	public RangerPolicyLabelsService() {
		super();
		searchFields.add(
				new SearchField(SearchFilter.POLICY_LABEL, "obj.policyLabel", DATA_TYPE.STRING, SEARCH_TYPE.PARTIAL));
		sortFields.add(new SortField(SearchFilter.POLICY_LABEL_ID, "obj.id", true, SORT_ORDER.ASC));
	}

	@Override
	protected T mapViewToEntityBean(V viewBean, T t, int OPERATION_CONTEXT) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected V mapEntityToViewBean(V viewBean, T t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void validateForCreate(V vObj) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void validateForUpdate(V vObj, T entityObj) {
		// TODO Auto-generated method stub

	}

}
