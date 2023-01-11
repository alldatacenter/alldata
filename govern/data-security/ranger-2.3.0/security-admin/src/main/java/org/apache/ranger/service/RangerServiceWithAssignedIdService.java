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

import java.util.HashMap;
import java.util.List;

import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.entity.XXServiceConfigMap;
import org.apache.ranger.entity.XXServiceWithAssignedId;
import org.apache.ranger.plugin.model.RangerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RangerServiceWithAssignedIdService extends RangerServiceServiceBase<XXServiceWithAssignedId, RangerService> {

	@Autowired
	JSONUtil jsonUtil;

	@Override
	protected XXServiceWithAssignedId mapViewToEntityBean(RangerService vObj, XXServiceWithAssignedId xObj, int OPERATION_CONTEXT) {
		return super.mapViewToEntityBean(vObj, xObj, OPERATION_CONTEXT);
	}

	@Override
	protected RangerService mapEntityToViewBean(RangerService vObj, XXServiceWithAssignedId xObj) {
		return super.mapEntityToViewBean(vObj, xObj);
	}
	
	@Override
	protected void validateForCreate(RangerService vObj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void validateForUpdate(RangerService vService, XXServiceWithAssignedId xService) {
		
	}
	
	@Override
	protected RangerService populateViewBean(XXServiceWithAssignedId xService) {
		RangerService vService = super.populateViewBean(xService);
		
		HashMap<String, String> configs = new HashMap<String, String>();
		List<XXServiceConfigMap> svcConfigMapList = daoMgr.getXXServiceConfigMap()
				.findByServiceId(xService.getId());
		for(XXServiceConfigMap svcConfMap : svcConfigMapList) {
			configs.put(svcConfMap.getConfigkey(), svcConfMap.getConfigvalue());
		}
		vService.setConfigs(configs);
		
		return vService;
	}
	
	public RangerService getPopulatedViewObject(XXServiceWithAssignedId xService) {
		return this.populateViewBean(xService);
	}

}
