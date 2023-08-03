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
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXPermMap;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.util.RangerEnumUtil;
import org.apache.ranger.view.VXGroup;
import org.apache.ranger.view.VXPermMap;
import org.apache.ranger.view.VXUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class XPermMapService extends XPermMapServiceBase<XXPermMap, VXPermMap> {

	@Autowired
	XGroupService xGroupService;
	
	@Autowired
	XUserService xUserService;
	
	@Autowired
	RangerEnumUtil xaEnumUtil;

	@Autowired
	RangerBizUtil rangerBizUtil;

	@Autowired
	XResourceService xResourceService;

	static HashMap<String, VTrxLogAttr> trxLogAttrs = new HashMap<String, VTrxLogAttr>();
	static {
//		trxLogAttrs.put("groupId", new VTrxLogAttr("groupId", "Group Permission", false));
//		trxLogAttrs.put("userId", new VTrxLogAttr("userId", "User Permission", false));
		trxLogAttrs.put("permType", new VTrxLogAttr("permType", "Permission Type", true));
		trxLogAttrs.put("ipAddress", new VTrxLogAttr("ipAddress", "IP Address", false));
	}

	
	public XPermMapService() {
		searchFields.add(new SearchField("resourceId", "obj.resourceId",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("permType", "obj.permType",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("permFor", "obj.permFor",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("userId", "obj.userId",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("groupId", "obj.groupId",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
	}

	@Override
	protected void validateForCreate(VXPermMap vObj) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void validateForUpdate(VXPermMap vObj, XXPermMap mObj) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public VXPermMap populateViewBean(XXPermMap xXPermMap){
		VXPermMap map = super.populateViewBean(xXPermMap);		
		if(map.getPermFor() == AppConstants.XA_PERM_FOR_GROUP) {
			String groupName = getGroupName(map.getGroupId());
			if(groupName != null){
				map.setGroupName(groupName);
			}
		} else if(map.getPermFor() == AppConstants.XA_PERM_FOR_USER) {
			String username = getUserName(map.getUserId());
			if(username != null){
				map.setUserName(username);
			}
		}
		return map;
	}
	
	public String getGroupName(Long groupId){
		if(groupId!=null && groupId!=0){
		VXGroup vXGroup = xGroupService.readResource(groupId);
			return vXGroup.getName();
		}
		else
			return null;
	}
	
	public String getUserName(Long userId){
		if(userId!=null && userId!=0){
		VXUser vXUser = xUserService.readResource(userId);
			return vXUser.getName();
		}
		else
			return null;
	}

	public List<XXTrxLog> getTransactionLog(VXPermMap vXPermMap, String action){
		return getTransactionLog(vXPermMap, null, action);
	}
	
	public List<XXTrxLog> getTransactionLog(VXPermMap vObj, VXPermMap mObj, String action){
		if(vObj == null || action == null || ("update".equalsIgnoreCase(action) && mObj == null)){
			return null;
		}
		
		boolean isGroupPolicy = true;
		if(vObj.getGroupId() == null){
			isGroupPolicy = false;
		}
		
		Long groupId = null;
		Long userId = null;
		String groupName = null;
		String userName = null;
		
		if(isGroupPolicy){
			groupId = vObj.getGroupId();
			XXGroup xGroup = daoManager.getXXGroup().getById(groupId);
			groupName = xGroup.getName();
		} else {
			userId = vObj.getUserId();
			XXUser xUser = daoManager.getXXUser().getById(userId);
			userName = xUser.getName();
		}

		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		Field[] fields = vObj.getClass().getDeclaredFields();
		
		try {
			for(Field field : fields){
				field.setAccessible(true);
				String fieldName = field.getName();
				if(!trxLogAttrs.containsKey(fieldName)){
					continue;
//				int policyType = vObj.getIpAddress();
				/*if(policyType == AppConstants.ASSET_HDFS){
					String[] ignoredAttribs = {"ipAddress"};
					if(ArrayUtils.contains(ignoredAttribs, fieldName)){
						continue;
					}
				}*/	
//				} else {
//					if(isGroupPolicy){
//						if(fieldName.equalsIgnoreCase("userId")){
//							continue;
//						}
//					} else {
//						if (fieldName.equalsIgnoreCase("groupId")){
//							continue;
//						}
//					}
				}
				Long assetId = daoManager.getXXResource().getById(vObj.getResourceId()).getAssetId();
				int policyType = daoManager.getXXAsset().getById(assetId).getAssetType();
				if(policyType != AppConstants.ASSET_KNOX){
					if("ipAddress".equals(fieldName))
						continue;
				}
				
				VTrxLogAttr vTrxLogAttr = trxLogAttrs.get(fieldName);
				
				XXTrxLog xTrxLog = new XXTrxLog();
				xTrxLog.setAttributeName(vTrxLogAttr.getAttribUserFriendlyName());
			
				String value = null,prevValue = "";
				boolean isEnum = vTrxLogAttr.isEnum();
				if(isEnum){
					String enumName = XXPermMap.getEnumName(fieldName);
					int enumValue = field.get(vObj) == null ? 0 : Integer.parseInt(""+field.get(vObj));
					value = xaEnumUtil.getLabel(enumName, enumValue);
				} else {
					value = ""+field.get(vObj);
//					XXUser xUser = rangerDaoManager.getXXUser().getById(Long.parseLong(value));
//					value = xUser.getName();
					if("ipAddress".equals(fieldName) && "update".equalsIgnoreCase(action)){
						prevValue = "" + field.get(mObj);
						value = "null".equalsIgnoreCase(value) ? "" : value;
					}
					else if(value == null || "null".equalsIgnoreCase(value) || stringUtil.isEmpty(value)){
						continue;
					}
				}
				
				if("create".equalsIgnoreCase(action)){
					xTrxLog.setNewValue(value);
				} else if("delete".equalsIgnoreCase(action)){
					xTrxLog.setPreviousValue(value);
				} else if("update".equalsIgnoreCase(action)){
					// Not Changed.
					xTrxLog.setNewValue(value);
					xTrxLog.setPreviousValue(value);
					if("ipAddress".equals(fieldName)){
						xTrxLog.setPreviousValue(prevValue);
					}
				}
				
				xTrxLog.setAction(action);
				xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_XA_PERM_MAP);
				xTrxLog.setObjectId(vObj.getId());
				if(isGroupPolicy){
					xTrxLog.setParentObjectClassType(AppConstants.CLASS_TYPE_XA_GROUP);
					xTrxLog.setParentObjectId(groupId);
					xTrxLog.setParentObjectName(groupName);
				} else {
					xTrxLog.setParentObjectClassType(AppConstants.CLASS_TYPE_XA_USER);
					xTrxLog.setParentObjectId(userId);
					xTrxLog.setParentObjectName(userName);
				}
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
	protected XXPermMap mapViewToEntityBean(VXPermMap vObj, XXPermMap mObj, int OPERATION_CONTEXT) {
	    XXPermMap ret = null;
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
	protected VXPermMap mapEntityToViewBean(VXPermMap vObj, XXPermMap mObj) {
	    VXPermMap ret = null;
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
