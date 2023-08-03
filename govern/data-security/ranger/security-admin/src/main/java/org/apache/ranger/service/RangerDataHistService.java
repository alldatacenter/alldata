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

import java.util.Date;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXDataHist;
import org.apache.ranger.plugin.model.RangerBaseModelObject;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class RangerDataHistService {

	@Autowired
	RESTErrorUtil restErrorUtil;
	
	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	JSONUtil jsonUtil;
	
	public static final String ACTION_CREATE = "Create";
	public static final String ACTION_UPDATE = "Update";
	public static final String ACTION_DELETE = "Delete";
	
	public void createObjectDataHistory(RangerBaseModelObject baseModelObj, String action) {
		if(baseModelObj == null || action == null) {
			throw restErrorUtil
					.createRESTException("Error while creating DataHistory. "
							+ "Object or Action can not be null.",
							MessageEnums.DATA_NOT_FOUND);
		}
		
		
		Integer classType = null;
		String objectName = null;
		String content = null;
		
		Long objectId = baseModelObj.getId();
		String objectGuid = baseModelObj.getGuid();
		Date currentDate = DateUtil.getUTCDate();
		
		XXDataHist xDataHist = new XXDataHist();
		
		xDataHist.setObjectId(baseModelObj.getId());
		xDataHist.setObjectGuid(objectGuid);
		xDataHist.setCreateTime(currentDate);
		xDataHist.setAction(action);
		xDataHist.setVersion(baseModelObj.getVersion());
		xDataHist.setUpdateTime(currentDate);
		xDataHist.setFromTime(currentDate);

		if(baseModelObj instanceof RangerServiceDef) {
			RangerServiceDef serviceDef = (RangerServiceDef) baseModelObj;
			objectName = serviceDef.getName();
			classType = AppConstants.CLASS_TYPE_XA_SERVICE_DEF;
			content = jsonUtil.writeObjectAsString(serviceDef);
		} else if(baseModelObj instanceof RangerService) {
			RangerService service = (RangerService) baseModelObj;
			objectName = service.getName();
			classType = AppConstants.CLASS_TYPE_XA_SERVICE;
			content = jsonUtil.writeObjectAsString(service);
		} else if(baseModelObj instanceof RangerPolicy) {
			RangerPolicy policy = (RangerPolicy) baseModelObj;
			objectName = policy.getName();
			classType = AppConstants.CLASS_TYPE_RANGER_POLICY;
			policy.setServiceType(policy.getServiceType());
			content = jsonUtil.writeObjectAsString(policy);
		}
		
		xDataHist.setObjectClassType(classType);
		xDataHist.setObjectName(objectName);
		xDataHist.setContent(content);
		xDataHist = daoMgr.getXXDataHist().create(xDataHist);
		
		if (ACTION_UPDATE.equalsIgnoreCase(action) || ACTION_DELETE.equalsIgnoreCase(action)) {
			XXDataHist prevHist = daoMgr.getXXDataHist().findLatestByObjectClassTypeAndObjectId(classType, objectId);
			
			if(prevHist == null) {
				throw restErrorUtil.createRESTException(
						"Error updating DataHistory Object. ObjectName: "
								+ objectName, MessageEnums.DATA_NOT_UPDATABLE);
			}
			
			prevHist.setUpdateTime(currentDate);
			prevHist.setToTime(currentDate);
			prevHist.setObjectName(objectName);
			prevHist = daoMgr.getXXDataHist().update(prevHist);
		}
	}

}
