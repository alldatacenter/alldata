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
import java.util.Map;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.view.VTrxLogAttr;
import org.apache.ranger.entity.XXAsset;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.util.RangerEnumUtil;
import org.apache.ranger.view.VXGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Scope("singleton")
public class XGroupService extends XGroupServiceBase<XXGroup, VXGroup> {

	private final Long createdByUserId;
	
	@Autowired
	RangerEnumUtil xaEnumUtil;
	
	@Autowired
	StringUtil stringUtil;
	
	static HashMap<String, VTrxLogAttr> trxLogAttrs = new HashMap<String, VTrxLogAttr>();
	static {
		trxLogAttrs.put("name", new VTrxLogAttr("name", "Group Name", false));
		trxLogAttrs.put("description", new VTrxLogAttr("description", "Group Description", false));
	}
	
	public XGroupService() {
		searchFields.add(new SearchField("name", "obj.name",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("groupSource", "obj.groupSource",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));

		searchFields.add(new SearchField("isVisible", "obj.isVisible",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL ));
		
		searchFields.add(new SearchField("userId", "groupUser.userId",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL,
				"XXGroupUser groupUser", "obj.id = groupUser.parentGroupId"));

		searchFields.add(new SearchField("syncSource", "obj.syncSource",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));

		createdByUserId = PropertiesUtil.getLongProperty("ranger.xuser.createdByUserId", 1);

		sortFields.add(new SortField("name", "obj.name",true,SortField.SORT_ORDER.ASC));
	}

	@Override
	protected void validateForCreate(VXGroup vObj) {
		XXGroup xxGroup = daoManager.getXXGroup().findByGroupName(
				vObj.getName());
		if (xxGroup != null) {
			throw restErrorUtil.createRESTException("XGroup already exists",
					MessageEnums.ERROR_DUPLICATE_OBJECT);
		}

	}

	@Override
	protected void validateForUpdate(VXGroup vObj, XXGroup mObj) {
		if (!vObj.getName().equalsIgnoreCase(mObj.getName())) {
			validateForCreate(vObj);
		}
	}

	public VXGroup getGroupByGroupName(String groupName) {
		XXGroup xxGroup = daoManager.getXXGroup().findByGroupName(groupName);

		if (xxGroup == null) {
			throw restErrorUtil.createRESTException(
					groupName + " is Not Found", MessageEnums.DATA_NOT_FOUND);
		}
		return super.populateViewBean(xxGroup);
	}
	
	public VXGroup createXGroupWithOutLogin(VXGroup vxGroup) {
		XXGroup xxGroup = daoManager.getXXGroup().findByGroupName(
				vxGroup.getName());
		boolean groupExists = true;

		if (xxGroup == null) {
			xxGroup = new XXGroup();
			groupExists = false;
		}

		xxGroup = mapViewToEntityBean(vxGroup, xxGroup, 0);
		XXPortalUser xXPortalUser = daoManager.getXXPortalUser().getById(createdByUserId);
		if (xXPortalUser != null) {
			xxGroup.setAddedByUserId(createdByUserId);
			xxGroup.setUpdatedByUserId(createdByUserId);
		}
		if (groupExists) {
			getDao().update(xxGroup);
		} else {
			getDao().create(xxGroup);
		}
		xxGroup = daoManager.getXXGroup().findByGroupName(vxGroup.getName());
		vxGroup = postCreate(xxGroup);
		return vxGroup;
	}

	public VXGroup readResourceWithOutLogin(Long id) {
		XXGroup resource = getDao().getById(id);
		if (resource == null) {
			// Returns code 400 with DATA_NOT_FOUND as the error message
			throw restErrorUtil.createRESTException(getResourceName()
					+ " not found", MessageEnums.DATA_NOT_FOUND, id, null,
					"preRead: " + id + " not found.");
		}

		VXGroup view = populateViewBean(resource);
		if(view!=null){
			view.setGroupSource(resource.getGroupSource());
		}
		return view;
	}

	public List<XXTrxLog> getTransactionLog(VXGroup vResource, String action){
		return getTransactionLog(vResource, null, action);
	}

	public List<XXTrxLog> getTransactionLog(VXGroup vObj, XXGroup mObj, String action){
		if(vObj == null || action == null || ("update".equalsIgnoreCase(action) && mObj == null)){
			return null;
		}
		
		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		try {

			Field nameField = vObj.getClass().getDeclaredField("name");
			nameField.setAccessible(true);
			String objectName = ""+nameField.get(vObj);
			Field[] fields = vObj.getClass().getDeclaredFields();
			
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
					String enumName = XXGroup.getEnumName(fieldName);
					int enumValue = field.get(vObj) == null ? 0 : Integer.parseInt(""+field.get(vObj));
					value = xaEnumUtil.getLabel(enumName, enumValue);
				} else {
					value = ""+field.get(vObj);
				}
				
				if("create".equalsIgnoreCase(action)){
					if(stringUtil.isEmpty(value)){
						continue;
					}
					xTrxLog.setNewValue(value);
				} else if("delete".equalsIgnoreCase(action)){
					xTrxLog.setPreviousValue(value);
				} else if("update".equalsIgnoreCase(action)){
					String oldValue = null;
					Field[] mFields = mObj.getClass().getDeclaredFields();
					for(Field mField : mFields){
						mField.setAccessible(true);
						String mFieldName = mField.getName();
						if(fieldName.equalsIgnoreCase(mFieldName)){
							if(isEnum){
								String enumName = XXAsset.getEnumName(mFieldName);
								int enumValue = mField.get(mObj) == null ? 0 : Integer.parseInt(""+mField.get(mObj));
								oldValue = xaEnumUtil.getLabel(enumName, enumValue);
							} else {
								oldValue = mField.get(mObj)+"";
							}
							break;
						}
					}
					if(value.equalsIgnoreCase(oldValue)){
						continue;
					}
					xTrxLog.setPreviousValue(oldValue);
					xTrxLog.setNewValue(value);
				}
				
				xTrxLog.setAction(action);
				xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_XA_GROUP);
				xTrxLog.setObjectId(vObj.getId());
				xTrxLog.setObjectName(objectName);
				trxLogList.add(xTrxLog);
				
			}
		} catch (IllegalArgumentException e) {
			logger.error("Transaction log failure.", e);
		} catch (IllegalAccessException e) {
			logger.error("Transaction log failure.", e);
		} catch (NoSuchFieldException e) {
			logger.error("Transaction log failure.", e);
		} catch (SecurityException e) {
			logger.error("Transaction log failure.", e);
		}
		
		return trxLogList;
	}
	
	@Override
	public VXGroup populateViewBean(XXGroup xGroup) {
		VXGroup vObj = super.populateViewBean(xGroup);
		vObj.setIsVisible(xGroup.getIsVisible());
		return vObj;
	}
	
	@Override
	protected XXGroup mapViewToEntityBean(VXGroup vObj, XXGroup mObj, int OPERATION_CONTEXT) {
		return super.mapViewToEntityBean(vObj, mObj, OPERATION_CONTEXT);
	}

	@Override
	protected VXGroup mapEntityToViewBean(VXGroup vObj, XXGroup mObj) {
		return super.mapEntityToViewBean(vObj, mObj);
        }

        public Map<Long, XXGroup> getXXGroupIdXXGroupMap(){
                Map<Long, XXGroup> xXGroupMap=new HashMap<Long, XXGroup>();
                try{
                        List<XXGroup> xXGroupList=daoManager.getXXGroup().getAll();
                        if(!CollectionUtils.isEmpty(xXGroupList)){
                                for(XXGroup xXGroup:xXGroupList){
                                        xXGroupMap.put(xXGroup.getId(), xXGroup);
                                }
                        }
                }catch(Exception ex){}
                return xXGroupMap;
        }

	public Map<Long, String> getXXGroupIdNameMap() {
		return daoManager.getXXGroup().getAllGroupIdNames();
	}
}
