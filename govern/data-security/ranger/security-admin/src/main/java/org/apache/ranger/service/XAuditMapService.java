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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.view.VTrxLogAttr;
import org.apache.ranger.entity.XXAuditMap;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.util.RangerEnumUtil;
import org.apache.ranger.view.VXAuditMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class XAuditMapService extends
		XAuditMapServiceBase<XXAuditMap, VXAuditMap> {

	@Autowired
	RangerEnumUtil xaEnumUtil;
	
	@Autowired
	RangerBizUtil rangerBizUtil;
	
	@Autowired
	XResourceService xResourceService;

	static HashMap<String, VTrxLogAttr> trxLogAttrs = new HashMap<String, VTrxLogAttr>();
	static {
//		trxLogAttrs.put("groupId", new VTrxLogAttr("groupId", "Group Audit", false));
//		trxLogAttrs.put("userId", new VTrxLogAttr("userId", "User Audit", false));
		trxLogAttrs.put("auditType", new VTrxLogAttr("auditType", "Audit Type", true));
	}

	public XAuditMapService() {
		searchFields.add(new SearchField("resourceId", "obj.resourceId",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("userId", "obj.userId",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("groupId", "obj.groupId",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
	}

	@Override
	protected void validateForCreate(VXAuditMap vObj) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void validateForUpdate(VXAuditMap vObj, XXAuditMap mObj) {
		// TODO Auto-generated method stub

	}

	public List<XXTrxLog> getTransactionLog(VXAuditMap vXAuditMap, String action){
		return getTransactionLog(vXAuditMap, null, action);
	}

	public List<XXTrxLog> getTransactionLog(VXAuditMap vObj, VXAuditMap mObj, String action){
		if(vObj == null || action == null || ("update".equalsIgnoreCase(action) && mObj == null)){
			return null;
		}
		
		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		Field[] fields = vObj.getClass().getDeclaredFields();
		
		try {
			for(Field field : fields){
				field.setAccessible(true);
				String fieldName = field.getName();
				if(!trxLogAttrs.containsKey(fieldName)){
					continue;
				}
				
				VTrxLogAttr vTrxLogAttr = trxLogAttrs.get(fieldName);
				
				XXTrxLog xTrxLog = new XXTrxLog();
				xTrxLog.setAttributeName(vTrxLogAttr.getAttribUserFriendlyName());
			
				String value = null;
				boolean isEnum = vTrxLogAttr.isEnum();
				if(isEnum){
					String enumName = XXAuditMap.getEnumName(fieldName);
					int enumValue = field.get(vObj) == null ? 0 : Integer.parseInt(""+field.get(vObj));
					value = xaEnumUtil.getLabel(enumName, enumValue);
				} else {
					value = ""+field.get(vObj);
					XXUser xUser = daoManager.getXXUser().getById(Long.parseLong(value));
					value = xUser.getName();
				}
				
				if("create".equalsIgnoreCase(action)){
					xTrxLog.setNewValue(value);
				} else if("delete".equalsIgnoreCase(action)){
					xTrxLog.setPreviousValue(value);
				} else if("update".equalsIgnoreCase(action)){
					// Not Changed.
					xTrxLog.setNewValue(value);
					xTrxLog.setPreviousValue(value);
				}
				
				xTrxLog.setAction(action);
				xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_XA_AUDIT_MAP);
				xTrxLog.setObjectId(vObj.getId());
				xTrxLog.setParentObjectClassType(AppConstants.CLASS_TYPE_XA_RESOURCE);
				xTrxLog.setParentObjectId(vObj.getResourceId());
//				xTrxLog.setParentObjectName(vObj.get);
//				xTrxLog.setObjectName(objectName);
				trxLogList.add(xTrxLog);
				
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		return trxLogList;
	}

	@Override
	protected XXAuditMap mapViewToEntityBean(VXAuditMap vObj, XXAuditMap mObj, int OPERATION_CONTEXT) {
	    XXAuditMap ret = null;
		if(vObj!=null && mObj!=null){
			ret = super.mapViewToEntityBean(vObj, mObj, OPERATION_CONTEXT);
			XXPortalUser xXPortalUser=null;
			if(ret.getAddedByUserId()==null || ret.getAddedByUserId()==0){
				if(!stringUtil.isEmpty(vObj.getOwner())){
					xXPortalUser=daoManager.getXXPortalUser().findByLoginId(vObj.getOwner());
					if(xXPortalUser!=null){
						ret.setAddedByUserId(xXPortalUser.getId());
					}
				}
			}
			if(ret.getUpdatedByUserId()==null || ret.getUpdatedByUserId()==0){
				if(!stringUtil.isEmpty(vObj.getUpdatedBy())){
					xXPortalUser= daoManager.getXXPortalUser().findByLoginId(vObj.getUpdatedBy());
					if(xXPortalUser!=null){
						ret.setUpdatedByUserId(xXPortalUser.getId());
					}		
				}
			}
		}
		return ret;
	}

	@Override
	protected VXAuditMap mapEntityToViewBean(VXAuditMap vObj, XXAuditMap mObj) {
	    VXAuditMap ret = null;
		if(mObj!=null && vObj!=null){
			ret = super.mapEntityToViewBean(vObj, mObj);
			XXPortalUser xXPortalUser=null;
			if(stringUtil.isEmpty(ret.getOwner())){
				xXPortalUser= daoManager.getXXPortalUser().getById(mObj.getAddedByUserId());
				if(xXPortalUser!=null){
					ret.setOwner(xXPortalUser.getLoginId());
				}
			}
			if(stringUtil.isEmpty(ret.getUpdatedBy())){
				xXPortalUser= daoManager.getXXPortalUser().getById(mObj.getUpdatedByUserId());
				if(xXPortalUser!=null){
					ret.setUpdatedBy(xXPortalUser.getLoginId());
				}
			}
		}
		return ret;
	}

}
