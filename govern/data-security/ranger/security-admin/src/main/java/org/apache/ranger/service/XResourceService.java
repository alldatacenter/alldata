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

import org.apache.commons.lang.ArrayUtils;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SearchField.DATA_TYPE;
import org.apache.ranger.common.SearchField.SEARCH_TYPE;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.common.view.VTrxLogAttr;
import org.apache.ranger.entity.XXAsset;
import org.apache.ranger.entity.XXAuditMap;
import org.apache.ranger.entity.XXPermMap;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXResource;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.util.RangerEnumUtil;
import org.apache.ranger.view.VXAuditMap;
import org.apache.ranger.view.VXPermMap;
import org.apache.ranger.view.VXResource;
import org.apache.ranger.view.VXResourceList;
import org.apache.ranger.view.VXResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
@Service
@Scope("singleton")
public class XResourceService extends
		XResourceServiceBase<XXResource, VXResource> {

	@Autowired
	XPermMapService xPermMapService;

	@Autowired
	XAuditMapService xAuditMapService;

	@Autowired
	XUserService xUserService;

	@Autowired
	StringUtil stringUtil;
	
	@Autowired
	RangerBizUtil xaBizUtil;
	
	@Autowired
	RangerEnumUtil xaEnumUtil;
	
	@Autowired
	XPolicyService xPolicyService;

	static HashMap<String, VTrxLogAttr> trxLogAttrs = new HashMap<String, VTrxLogAttr>();
	
	static String fileSeparator = PropertiesUtil.getProperty("ranger.file.separator", "/");
	
	static {
		trxLogAttrs.put("name", new VTrxLogAttr("name", "Resource Path", false));
		trxLogAttrs.put("description", new VTrxLogAttr("description", "Policy Description", false));
		trxLogAttrs.put("resourceType", new VTrxLogAttr("resourceType", "Policy Type", true));
		trxLogAttrs.put("isEncrypt", new VTrxLogAttr("isEncrypt", "Policy Encryption", true));
		trxLogAttrs.put("isRecursive", new VTrxLogAttr("isRecursive", "Is Policy Recursive", true));
		trxLogAttrs.put("databases", new VTrxLogAttr("databases", "Databases", false));
		trxLogAttrs.put("tables", new VTrxLogAttr("tables", "Tables", false));
		trxLogAttrs.put("columnFamilies", new VTrxLogAttr("columnFamilies", "Column Families", false));
		trxLogAttrs.put("columns", new VTrxLogAttr("columns", "Columns", false));
		trxLogAttrs.put("udfs", new VTrxLogAttr("udfs", "UDF", false));
		trxLogAttrs.put("resourceStatus", new VTrxLogAttr("resourceStatus", "Policy Status", true));
		trxLogAttrs.put("tableType", new VTrxLogAttr("tableType", "Table Type", true));
		trxLogAttrs.put("columnType", new VTrxLogAttr("columnType", "Column Type", true));
		trxLogAttrs.put("policyName", new VTrxLogAttr("policyName", "Policy Name", false));
		trxLogAttrs.put("topologies", new VTrxLogAttr("topologies", "Topologies", false));
		trxLogAttrs.put("services", new VTrxLogAttr("services", "Services", false));
		trxLogAttrs.put("assetType", new VTrxLogAttr("assetType", "Repository Type", true));
	}

	public XResourceService() {
		searchFields.add(new SearchField("name", "obj.name",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("fullname", "obj.name",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("policyName", "obj.policyName",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("fullPolicyName", "obj.policyName",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("columns", "obj.columns",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("columnFamilies",
				"obj.columnFamilies", SearchField.DATA_TYPE.STRING,
				SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("tables", "obj.tables",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("udfs", "obj.udfs",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("databases", "obj.databases",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("assetId", "obj.assetId",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("resourceType", "obj.resourceType",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("isEncrypt", "obj.isEncrypt",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("isRecursive", "obj.isRecursive",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
		
		searchFields.add(new SearchField("groupName", "xxGroup.name",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL,
				"XXPermMap xxPermMap, XXGroup xxGroup", "xxPermMap.resourceId "
						+ "= obj.id and xxPermMap.groupId = xxGroup.id"));

		searchFields.add(new SearchField("userName", "xUser.name",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL,
				"XXPermMap xxPermMap, XXUser xUser", "xxPermMap.resourceId "
						+ "= obj.id and xxPermMap.userId = xUser.id"));

		searchFields.add(new SearchField("userId", "xxPermMap.userId",
				SearchField.DATA_TYPE.INT_LIST, SearchField.SEARCH_TYPE.FULL,
				"XXPermMap xxPermMap", "xxPermMap.resourceId = obj.id "));

		searchFields.add(new SearchField("groupId", "xxPermMap.groupId",
				SearchField.DATA_TYPE.INT_LIST, SearchField.SEARCH_TYPE.FULL,
				"XXPermMap xxPermMap", "xxPermMap.resourceId = obj.id"));
		
		searchFields.add(new SearchField("assetType", "xxAsset.assetType",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL,
				"XXAsset xxAsset", "xxAsset.id = obj.assetId "));

		searchFields.add(new SearchField("id", "obj.id",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("topologies", "obj.topologies",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("services", "obj.services",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("tableType", "obj.tableType",
				DATA_TYPE.INTEGER, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("columnType", "obj.columnType",
				DATA_TYPE.INTEGER, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("repositoryName", "xxAsset.name",
				DATA_TYPE.STRING, SEARCH_TYPE.PARTIAL, "XXAsset xxAsset",
				"xxAsset.id = obj.assetId"));
		searchFields.add(new SearchField("resourceStatus",
				"obj.resourceStatus", DATA_TYPE.INT_LIST, SEARCH_TYPE.FULL));

		sortFields.add(new SortField("name", "obj.name"));
		sortFields.add(new SortField("isRecursive", "obj.isRecursive"));
		sortFields.add(new SortField("isEncrypt", "obj.isEncrypt"));
		
	}

	@Override
	protected void validateForCreate(VXResource vObj) {
		if(vObj == null){
			throw restErrorUtil.createRESTException("Policy not provided.",
					MessageEnums.DATA_NOT_FOUND);
		}
		Long assetId = vObj.getAssetId();
		if(assetId != null){
			XXAsset xAsset = daoManager.getXXAsset().getById(assetId);
			if(xAsset == null){
				throw restErrorUtil.createRESTException("The repository for which "
						+ "the policy is created, doesn't exist in the system.",
						MessageEnums.OPER_NOT_ALLOWED_FOR_STATE);
			}
		} else {
			logger.debug("Asset id not provided.");
			throw restErrorUtil.createRESTException("Please provide repository"
					+ " id for policy.", MessageEnums.OPER_NOT_ALLOWED_FOR_STATE);
		}
		
		String resourceName = vObj.getName();
//		Long resourceId = vObj.getId();
//		int isRecursive = vObj.getIsRecursive();
		if(stringUtil.isEmpty(resourceName)){
			logger.error("Resource name not found for : " + vObj.toString());
			throw restErrorUtil.createRESTException("Please provide valid resources.",
					MessageEnums.INVALID_INPUT_DATA);
		}
		
//		String[] resourceNameList = stringUtil.split(resourceName, ",");
//		for(String resName : resourceNameList){
//			List<XXResource> xXResourceList = null;
//			if (assetType == AppConstants.ASSET_HDFS) {
//				xXResourceList = appDaoManager.getXXResource()
//						.findByResourceNameAndAssetIdAndRecursiveFlag(resName, assetId, isRecursive);			
//			} else {
//				xXResourceList = appDaoManager.getXXResource()
//						.findByResourceNameAndAssetIdAndResourceType(vObj.getName(),
//								vObj.getAssetId(), vObj.getResourceType());
//			}
//			
//			if (xXResourceList != null) {
//				boolean similarPolicyFound = false;
//				for(XXResource xxResource : xXResourceList){
//					String dbResourceName = xxResource.getName();
//					// Not checking dbResourceName to be null or empty
//					// as this should never be the case
//					String[] resources = stringUtil.split(dbResourceName, ",");
//					for(String dbResource: resources){
//						if(dbResource.equalsIgnoreCase(resName)){
//							if(resourceId!=null){
//								Long dbResourceId = xxResource.getId();
//								if(!resourceId.equals(dbResourceId)){
//									similarPolicyFound = true;
//									break;
//								}
//							} else {
//								similarPolicyFound = true;
//								break;
//							}
//						}
//					}
//					if(similarPolicyFound){
//						break;
//					}
//				}
//				if(similarPolicyFound){
//					throw restErrorUtil.createRESTException(
//							"Similar policy already exists for the resource : " + resName,
//							MessageEnums.ERROR_DUPLICATE_OBJECT);
//				}
//			}
//		}
		
//		if(vObj.getAssetType())
		
	}

	@Override
	protected void validateForUpdate(VXResource vObj, XXResource mObj) {
		if (vObj != null && vObj.getAssetType() == AppConstants.ASSET_HDFS) {
			if (!(vObj.getName() != null) || vObj.getName().isEmpty()) {
				throw restErrorUtil.createRESTException("Please provide the "
						+ "resource path.", MessageEnums.INVALID_INPUT_DATA);
			}
		}
		if ((vObj != null && mObj != null) &&
				(!vObj.getName().equalsIgnoreCase(mObj.getName()) ||
				vObj.getIsRecursive()!=mObj.getIsRecursive() ||
				vObj.getResourceType() != mObj.getResourceType())) {
			validateForCreate(vObj);
		}
		
	}

	@Override
	public VXResource createResource(VXResource vXResource) {

		VXResource resource = super.createResource(vXResource);
		
		List<VXAuditMap> newAuditMapList = new ArrayList<VXAuditMap>();
		List<VXAuditMap> vxAuditMapList = vXResource.getAuditList();
		if (vxAuditMapList != null) {
			for (VXAuditMap vxAuditMap : vxAuditMapList) {
				vxAuditMap.setResourceId(resource.getId());
				vxAuditMap = xAuditMapService.createResource(vxAuditMap);
				newAuditMapList.add(vxAuditMap);
			}
		}
		
		List<VXPermMap> newPermMapList = new ArrayList<VXPermMap>();
		List<VXPermMap> vxPermMapList = vXResource.getPermMapList();
		if (vxPermMapList != null) {
			for (VXPermMap permMap : vxPermMapList) {
				if (permMap.getUserId() == null && permMap.getGroupId() == null
						&& vxAuditMapList == null){
					if(vxAuditMapList == null){
						throw restErrorUtil.createRESTException("Please provide"
								+ " valid group/user permissions for policy.",
								MessageEnums.INVALID_INPUT_DATA);
					}
				} else {
					permMap.setResourceId(resource.getId());
					permMap = xPermMapService.createResource(permMap);
					newPermMapList.add(permMap);
				}
			}
		}

			
		resource.setPermMapList(newPermMapList);
		resource.setAuditList(newAuditMapList);
		return resource;
	}
	
	@Override
	public VXResource populateViewBean(XXResource xXResource) {
		VXResource vXResource = super.populateViewBean(xXResource);
		populateAssetProperties(vXResource);
		populatePermList(vXResource);
		return vXResource;
	}

	private void populateAssetProperties(VXResource vXResource) {
		XXAsset xxAsset = daoManager.getXXAsset().getById(
				vXResource.getAssetId());
		if (xxAsset != null) {
			vXResource.setAssetName(xxAsset.getName());
            vXResource.setAssetType(xxAsset.getAssetType());
        }
	}

	private void populateAuditList(VXResource vXResource) {

		List<XXAuditMap> xAuditMapList = daoManager.getXXAuditMap().findByResourceId(vXResource.getId());
		List<VXAuditMap> vXAuditMapList = new ArrayList<VXAuditMap>();

		for (XXAuditMap xAuditMap : xAuditMapList) {
			vXAuditMapList.add(xAuditMapService.populateViewBean(xAuditMap));
		}
		vXResource.setAuditList(vXAuditMapList);
	}

	private void populatePermList(VXResource vXResource) {

		List<XXPermMap> xPermMapList = daoManager.getXXPermMap().findByResourceId(vXResource.getId());
		List<VXPermMap> vXPermMapList = new ArrayList<VXPermMap>();

		for (XXPermMap xPermMap : xPermMapList) {
			vXPermMapList.add(xPermMapService.populateViewBean(xPermMap));
		}
		vXResource.setPermMapList(vXPermMapList);
	}

	@Override
	public VXResourceList searchXResources(SearchCriteria searchCriteria) {

		VXResourceList returnList;
		UserSessionBase currentUserSession = ContextUtil
				.getCurrentUserSession();
		// If user is system admin
		if (currentUserSession != null && currentUserSession.isUserAdmin()) {
			returnList = super.searchXResources(searchCriteria);
			
		} else {// need to be optimize
			returnList = new VXResourceList();
			int startIndex = searchCriteria.getStartIndex();
			int pageSize = searchCriteria.getMaxRows();
			searchCriteria.setStartIndex(0);
			searchCriteria.setMaxRows(Integer.MAX_VALUE);
			List<XXResource> resultList = (List<XXResource>) searchResources(
					searchCriteria, searchFields, sortFields, returnList);
			List<XXResource> adminPermResourceList = new ArrayList<XXResource>();
			for (XXResource xXResource : resultList) {
				VXResponse vXResponse = xaBizUtil.hasPermission(populateViewBean(xXResource),
						AppConstants.XA_PERM_TYPE_ADMIN);
				if(vXResponse.getStatusCode() == VXResponse.STATUS_SUCCESS){
					adminPermResourceList.add(xXResource);
				}
			}

			if (!adminPermResourceList.isEmpty()) {
				populatePageList(adminPermResourceList, startIndex, pageSize,
						returnList);
			}
		}
		if(returnList!=null && returnList.getResultSize()>0){
			for (VXResource vXResource : returnList.getVXResources()) {
				populateAuditList(vXResource);
			}
		}
		return returnList;
	}

	private void populatePageList(List<XXResource> resourceList,
			int startIndex, int pageSize, VXResourceList vxResourceList) {
		List<VXResource> onePageList = new ArrayList<VXResource>();
		for (int i = startIndex; i < pageSize + startIndex
				&& i < resourceList.size(); i++) {
			VXResource vXResource = populateViewBean(resourceList.get(i));
			onePageList.add(vXResource);
		}
		vxResourceList.setVXResources(onePageList);
		vxResourceList.setStartIndex(startIndex);
		vxResourceList.setPageSize(pageSize);
		vxResourceList.setResultSize(onePageList.size());
		vxResourceList.setTotalCount(resourceList.size());

	}

	@Override
	protected XXResource mapViewToEntityBean(VXResource vObj, XXResource mObj, int OPERATION_CONTEXT) {
	    XXResource ret = null;
        if(vObj!=null && mObj!=null){
		    ret = super.mapViewToEntityBean(vObj, mObj, OPERATION_CONTEXT);
		    ret.setUdfs(vObj.getUdfs());
			XXPortalUser xXPortalUser= null;
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
	protected VXResource mapEntityToViewBean(VXResource vObj, XXResource mObj) {
	    VXResource ret = null;
        if(mObj!=null && vObj!=null){
            ret = super.mapEntityToViewBean(vObj, mObj);
		    ret.setUdfs(mObj.getUdfs());
		    populateAssetProperties(ret);
			XXPortalUser xXPortalUser= null;
			if(stringUtil.isEmpty(ret.getOwner())){
				xXPortalUser=daoManager.getXXPortalUser().getById(mObj.getAddedByUserId());
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

	public List<XXTrxLog> getTransactionLog(VXResource vResource, String action){
		return getTransactionLog(vResource, null, action);
	}
	
	public List<XXTrxLog> getTransactionLog(VXResource vObj, XXResource mObj, String action){
		if(vObj == null || action == null || ("update".equalsIgnoreCase(action) && mObj == null)) {
			return null;
		}

		XXAsset xAsset = daoManager.getXXAsset().getById(vObj.getAssetId());
		String parentObjectName = xAsset.getName();
		
		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		Field[] fields = vObj.getClass().getDeclaredFields();

		Field nameField;
		try {
			nameField = vObj.getClass().getDeclaredField("name");
			nameField.setAccessible(true);
			String objectName = ""+nameField.get(vObj);

			for(Field field : fields){
				field.setAccessible(true);
				String fieldName = field.getName();
				if(!trxLogAttrs.containsKey(fieldName)){
					continue;
				}
				
				int policyType = vObj.getAssetType();
				if(policyType == AppConstants.ASSET_HDFS){
					String[] ignoredAttribs = {"tableType", "columnType", "isEncrypt", "databases",
							"tables", "columnFamilies",  "columns", "udfs"};
					if(ArrayUtils.contains(ignoredAttribs, fieldName)){
						continue;
					}
				} else if(policyType == AppConstants.ASSET_HIVE) {
					String[] ignoredAttribs = {"name", "isRecursive", "isEncrypt", "columnFamilies"};
					if(ArrayUtils.contains(ignoredAttribs, fieldName)){
						continue;
					}
				} else if(policyType == AppConstants.ASSET_HBASE){
					String[] ignoredAttribs = {"name", "tableType", "columnType", "isRecursive", "databases",
							"udfs"};
					if(ArrayUtils.contains(ignoredAttribs, fieldName)){
						continue;
					}
				} else if(policyType == AppConstants.ASSET_KNOX || policyType == AppConstants.ASSET_STORM){
					String[] ignoredAttribs = {"name", "tableType", "columnType", "isEncrypt", "databases",
							"tables", "columnFamilies",  "columns", "udfs"};
					if(ArrayUtils.contains(ignoredAttribs, fieldName)){
						continue;
					}
				}
				
				VTrxLogAttr vTrxLogAttr = trxLogAttrs.get(fieldName);
				
				XXTrxLog xTrxLog = new XXTrxLog();
				xTrxLog.setAttributeName(vTrxLogAttr.getAttribUserFriendlyName());
			
				String value = null;
				boolean isEnum = vTrxLogAttr.isEnum();
				if(isEnum){
					String enumName = XXResource.getEnumName(fieldName);
					if(enumName==null && "assetType".equals(fieldName)){
						enumName="CommonEnums.AssetType";
					}
					int enumValue = field.get(vObj) == null ? 0 : Integer.parseInt(""+field.get(vObj));
					value = xaEnumUtil.getLabel(enumName, enumValue);
				} else {
					value = ""+field.get(vObj);
					if(value == null || "null".equalsIgnoreCase(value)){
						continue;
					}
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
								String enumName = XXResource.getEnumName(mFieldName);
								if(enumName==null && "assetType".equals(mFieldName)){
									enumName="CommonEnums.AssetType";
								}
								int enumValue = mField.get(mObj) == null ? 0 : Integer.parseInt(""+mField.get(mObj));
								oldValue = xaEnumUtil.getLabel(enumName, enumValue);
							} else {
								oldValue = mField.get(mObj)+"";
							}
							break;
						}
					}
					if(value.equalsIgnoreCase(oldValue) && !"policyName".equals(fieldName)){
						continue;
					}
					xTrxLog.setPreviousValue(oldValue);
					xTrxLog.setNewValue(value);
				}
				
				xTrxLog.setAction(action);
				xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_XA_RESOURCE);
				xTrxLog.setObjectId(vObj.getId());
				xTrxLog.setParentObjectClassType(AppConstants.CLASS_TYPE_XA_ASSET);
				xTrxLog.setParentObjectId(vObj.getAssetId());
				xTrxLog.setParentObjectName(parentObjectName);
				xTrxLog.setObjectName(objectName);
				trxLogList.add(xTrxLog);
			
			}
				
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		if(trxLogList.isEmpty()){
			XXTrxLog xTrxLog = new XXTrxLog();
			xTrxLog.setAction(action);
			xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_XA_RESOURCE);
			xTrxLog.setObjectId(vObj.getId());
			xTrxLog.setObjectName(vObj.getName());
			xTrxLog.setParentObjectClassType(AppConstants.CLASS_TYPE_XA_ASSET);
			xTrxLog.setParentObjectId(vObj.getAssetId());
			xTrxLog.setParentObjectName(parentObjectName);
			trxLogList.add(xTrxLog);
		}
		
		return trxLogList;
	}

	@Override
	public VXResource readResource(Long id){
		VXResource vXResource = super.readResource(id);
		
		VXResponse vXResponse = xaBizUtil.hasPermission(vXResource,
				AppConstants.XA_PERM_TYPE_ADMIN);
		if (vXResponse.getStatusCode() == VXResponse.STATUS_ERROR) {
			throw restErrorUtil.createRESTException(
					"You don't have permission to perform this action",
					MessageEnums.OPER_NO_PERMISSION, id, "Resource",
					"Trying to read unauthorized resource.");
		}
		
		populateAssetProperties(vXResource);
		populatePermList(vXResource);
		populateAuditList(vXResource);
		return vXResource;
	}
	
	public VXResourceList searchXResourcesWithoutLogin(SearchCriteria searchCriteria) {	
		VXResourceList returnList = super.searchXResources(searchCriteria);		
		if(returnList!=null && returnList.getResultSize()>0){
			for (VXResource vXResource : returnList.getVXResources()) {
				populateAuditList(vXResource);
			}
		}
		return returnList;
	}
}
