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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.RangerCommonEnums;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.view.VTrxLogAttr;
import org.apache.ranger.entity.XXGroupUser;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXPortalUserRole;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.util.RangerEnumUtil;
import org.apache.ranger.view.VXPortalUser;
import org.apache.ranger.view.VXUser;
import org.apache.ranger.view.VXUserList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Scope("singleton")
public class XUserService extends XUserServiceBase<XXUser, VXUser> {
	private final Long createdByUserId;

	@Autowired
	XPermMapService xPermMapService;

	@Autowired
	StringUtil stringUtil;

	@Autowired
	RangerEnumUtil xaEnumUtil;

	@Autowired
	RangerBizUtil xaBizUtil;

	String hiddenPasswordString;

	static HashMap<String, VTrxLogAttr> trxLogAttrs = new HashMap<String, VTrxLogAttr>();
	static {
		trxLogAttrs.put("name", new VTrxLogAttr("name", "Login ID", false));
		trxLogAttrs.put("firstName", new VTrxLogAttr("firstName", "First Name",
				false));
		trxLogAttrs.put("lastName", new VTrxLogAttr("lastName", "Last Name",
				false));
		trxLogAttrs.put("emailAddress", new VTrxLogAttr("emailAddress",
				"Email Address", false));
		trxLogAttrs.put("password", new VTrxLogAttr("password", "Password",
				false));
		trxLogAttrs.put("userRoleList", new VTrxLogAttr("userRoleList", "User Role",
				false));
		
	}

	public XUserService() {
		searchFields.add(new SearchField("name", "obj.name",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));

		searchFields.add(new SearchField("emailAddress", "xXPortalUser.emailAddress",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL,
				"XXPortalUser xXPortalUser", "xXPortalUser.loginId = obj.name "));

		searchFields.add(new SearchField("userName", "obj.name",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));

		searchFields.add(new SearchField("userSource", "xXPortalUser.userSource",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL,
				"XXPortalUser xXPortalUser", "xXPortalUser.loginId = obj.name "));
		
		searchFields.add(new SearchField("userRoleList", "xXPortalUserRole.userRole",
				SearchField.DATA_TYPE.STR_LIST, SearchField.SEARCH_TYPE.FULL,
				"XXPortalUser xXPortalUser, XXPortalUserRole xXPortalUserRole",
				"xXPortalUser.id=xXPortalUserRole.userId and xXPortalUser.loginId = obj.name "));
		
		searchFields.add(new SearchField("isVisible", "obj.isVisible",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL ));

		searchFields.add(new SearchField("status", "xXPortalUser.status",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL,
				"XXPortalUser xXPortalUser", "xXPortalUser.loginId = obj.name "));
		searchFields.add(new SearchField("userRole", "xXPortalUserRole.userRole",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL,
				"XXPortalUser xXPortalUser, XXPortalUserRole xXPortalUserRole",
				"xXPortalUser.id=xXPortalUserRole.userId and xXPortalUser.loginId = obj.name "));

		searchFields.add(new SearchField("syncSource", "obj.syncSource",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));

		createdByUserId = PropertiesUtil.getLongProperty("ranger.xuser.createdByUserId", 1);

		hiddenPasswordString = PropertiesUtil.getProperty("ranger.password.hidden","*****");

		sortFields.add(new SortField("name", "obj.name",true,SortField.SORT_ORDER.ASC));
		
	}

	@Override
	protected void validateForCreate(VXUser vObj) {

		XXUser xUser = daoManager.getXXUser().findByUserName(vObj.getName());
		if (xUser != null) {
			throw restErrorUtil.createRESTException(vObj.getName() + " already exists",
					MessageEnums.ERROR_DUPLICATE_OBJECT);
		}

	}

	@Override
	protected void validateForUpdate(VXUser vObj, XXUser mObj) {
		String vObjName = vObj.getName();
		String mObjName = mObj.getName();
		if (vObjName != null && mObjName != null) {
			if (!vObjName.trim().equalsIgnoreCase(mObjName.trim())) {
				validateForCreate(vObj);
			}
		}
	}

	public VXUser getXUserByUserName(String userName) {
		XXUser xxUser = daoManager.getXXUser().findByUserName(userName);
		if (xxUser == null) {
			throw restErrorUtil.createRESTException(userName + " is Not Found",
					MessageEnums.DATA_NOT_FOUND);
		}
		return super.populateViewBean(xxUser);

	}

	public VXUser createXUserWithOutLogin(VXUser vxUser) {
		XXUser xxUser = daoManager.getXXUser().findByUserName(vxUser.getName());
		boolean userExists = true;
		if (xxUser == null) {
			xxUser = new XXUser();
			userExists = false;
		}
        XXPortalUser xxPortalUser = daoManager.getXXPortalUser().findByLoginId(
                vxUser.getName());
        if (xxPortalUser != null
                && xxPortalUser.getUserSource() == RangerCommonEnums.USER_EXTERNAL) {
            vxUser.setIsVisible(xxUser.getIsVisible());
        }
		xxUser = mapViewToEntityBean(vxUser, xxUser, 0);
		XXPortalUser xXPortalUser = daoManager.getXXPortalUser().getById(createdByUserId);
		if (xXPortalUser != null) {
			xxUser.setAddedByUserId(createdByUserId);
			xxUser.setUpdatedByUserId(createdByUserId);
		}

		if (userExists) {
			xxUser = getDao().update(xxUser);
		} else {
			xxUser = getDao().create(xxUser);
		}
		vxUser = postCreate(xxUser);
		return vxUser;
	}

	public VXUser readResourceWithOutLogin(Long id) {
		XXUser resource = getDao().getById(id);
		if (resource == null) {
			// Returns code 400 with DATA_NOT_FOUND as the error message
			throw restErrorUtil.createRESTException(getResourceName()
					+ " not found", MessageEnums.DATA_NOT_FOUND, id, null,
					"preRead: " + id + " not found.");
		}

		VXUser vxUser = populateViewBean(resource);
		return vxUser;
	}

	@Override
	protected VXUser mapEntityToViewBean(VXUser vObj, XXUser mObj) {
		VXUser ret = super.mapEntityToViewBean(vObj, mObj);
		String userName = ret.getName();
		populateUserAttributes(userName, ret);
		return ret;
	}

	@Override
	public VXUser populateViewBean(XXUser xUser) {
		VXUser vObj = super.populateViewBean(xUser);
		vObj.setIsVisible(xUser.getIsVisible());
		populateGroupList(xUser.getId(), vObj);
		return vObj;
	}

	private void populateGroupList(Long xUserId, VXUser vObj) {
		List<XXGroupUser> xGroupUserList = daoManager.getXXGroupUser()
				.findByUserId(xUserId);
		Set<Long> groupIdList = new LinkedHashSet<Long>();
		Set<String> groupNameList = new LinkedHashSet<String>();
		if (xGroupUserList != null) {
			for (XXGroupUser xGroupUser : xGroupUserList) {
				groupIdList.add(xGroupUser.getParentGroupId());
				groupNameList.add(xGroupUser.getName());
			}
		}
		List<Long> groups = new ArrayList<Long>(groupIdList);
		List<String> groupNames = new ArrayList<String>(groupNameList);
		vObj.setGroupIdList(groups);
		vObj.setGroupNameList(groupNames);
	}

	private void populateUserAttributes(String userName, VXUser vObj) {
		if (userName != null && !userName.isEmpty()) {
			List<String> userRoleList =new ArrayList<String>();
			XXPortalUser xXPortalUser = daoManager.getXXPortalUser().findByLoginId(userName);
			if (xXPortalUser != null) {
				vObj.setFirstName(xXPortalUser.getFirstName());
				vObj.setLastName(xXPortalUser.getLastName());
				vObj.setPassword(PropertiesUtil.getProperty("ranger.password.hidden"));
				String emailAddress = xXPortalUser.getEmailAddress();
				if (emailAddress != null
						&& stringUtil.validateEmail(emailAddress)) {
					vObj.setEmailAddress(xXPortalUser.getEmailAddress());
				}
				vObj.setStatus(xXPortalUser.getStatus());
				vObj.setUserSource(xXPortalUser.getUserSource());
				List<XXPortalUserRole> gjUserRoleList = daoManager.getXXPortalUserRole().findByParentId(
						xXPortalUser.getId());
				
				for (XXPortalUserRole gjUserRole : gjUserRoleList) {
					userRoleList.add(gjUserRole.getUserRole());
				}
			}
			if(userRoleList==null || userRoleList.isEmpty()){
				userRoleList.add(RangerConstants.ROLE_USER);
			}			
			vObj.setUserRoleList(userRoleList);
		}
	}

	public List<XXTrxLog> getTransactionLog(VXUser vResource, String action) {
		return getTransactionLog(vResource, null, action);
	}

	public List<XXTrxLog> getTransactionLog(VXUser vObj, VXPortalUser mObj,
			String action) {

		if (vObj == null || action == null || ("update".equalsIgnoreCase(action) && mObj == null)) {
			return null;
		}

		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		try {
			Field nameField = vObj.getClass().getDeclaredField("name");
			nameField.setAccessible(true);
			String objectName = "" + nameField.get(vObj);
			Field[] fields = vObj.getClass().getDeclaredFields();

			for (Field field : fields) {
				field.setAccessible(true);
				String fieldName = field.getName();
				if (!trxLogAttrs.containsKey(fieldName)) {
					continue;
				}

				VTrxLogAttr vTrxLogAttr = trxLogAttrs.get(fieldName);

				XXTrxLog xTrxLog = new XXTrxLog();
				xTrxLog.setAttributeName(vTrxLogAttr
						.getAttribUserFriendlyName());

				String value = null;
				if (vTrxLogAttr.isEnum()) {
					String enumName = XXUser.getEnumName(fieldName);
					int enumValue = field.get(vObj) == null ? 0 : Integer
							.parseInt("" + field.get(vObj));
					value = xaEnumUtil.getLabel(enumName, enumValue);
				} else {
					value = "" + field.get(vObj);
					if ((value == null || "null".equalsIgnoreCase(value))
							&& !"update".equalsIgnoreCase(action)) {
						continue;
					}
				}

				if ("password".equalsIgnoreCase(fieldName)) {
					if (value.equalsIgnoreCase(hiddenPasswordString)) {
						continue;
					}
				}

				if ("create".equalsIgnoreCase(action)) {
					if (stringUtil.isEmpty(value)
							|| ("emailAddress".equalsIgnoreCase(fieldName) && !stringUtil
									.validateEmail(value))) {
						continue;
					}
					xTrxLog.setNewValue(value);
				} else if ("delete".equalsIgnoreCase(action)) {
					if ("emailAddress".equalsIgnoreCase(fieldName)
							&& !stringUtil.validateEmail(value)) {
						continue;
					}
					xTrxLog.setPreviousValue(value);
				} else if ("update".equalsIgnoreCase(action)) {
					String oldValue = null;
					Field[] mFields = mObj.getClass().getDeclaredFields();
					for (Field mField : mFields) {
						mField.setAccessible(true);
						String mFieldName = mField.getName();
						if ("loginId".equalsIgnoreCase(mFieldName)) {
							mFieldName = "name";
						}
						if (fieldName.equalsIgnoreCase(mFieldName)) {
							oldValue = mField.get(mObj) + "";
							break;
						}
					}
					if (oldValue == null || oldValue.equalsIgnoreCase(value)) {
						continue;
					}
					if ("emailAddress".equalsIgnoreCase(fieldName)) {
						if (stringUtil.validateEmail(oldValue)) {
							xTrxLog.setPreviousValue(oldValue);
						}
						if (stringUtil.validateEmail(value)) {
							xTrxLog.setNewValue(value);
						}
					} else {
						xTrxLog.setPreviousValue(oldValue);
						xTrxLog.setNewValue(value);
					}
				}

				xTrxLog.setAction(action);
				xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_XA_USER);
				xTrxLog.setObjectId(vObj.getId());
				xTrxLog.setObjectName(objectName);

				trxLogList.add(xTrxLog);
			}

			if (trxLogList.isEmpty()) {
				XXTrxLog xTrxLog = new XXTrxLog();
				xTrxLog.setAction(action);
				xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_XA_USER);
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
        public Map<Long, XXUser> getXXPortalUserIdXXUserMap(){
                Map<Long, XXUser> xXPortalUserIdXXUserMap=new HashMap<Long, XXUser>();
                try{
                        Map<String, XXUser> xXUserMap=new HashMap<String, XXUser>();
                        List<XXUser> xXUserList=daoManager.getXXUser().getAll();
                        if(!CollectionUtils.isEmpty(xXUserList)){
                                for(XXUser xxUser:xXUserList){
                                        xXUserMap.put(xxUser.getName(), xxUser);
                                }
                        }
                        xXUserList=null;
                        List<XXPortalUser> xXPortalUserList=daoManager.getXXPortalUser().getAll();
                        if(!CollectionUtils.isEmpty(xXPortalUserList)){
                                for(XXPortalUser xXPortalUser:xXPortalUserList){
                                        if(xXUserMap.containsKey(xXPortalUser.getLoginId())){
                                                xXPortalUserIdXXUserMap.put(xXPortalUser.getId(),xXUserMap.get(xXPortalUser.getLoginId()));
                                        }
                                }
                        }
                }catch(Exception ex){}
                return xXPortalUserIdXXUserMap;
        }

	public VXUserList lookupXUsers(SearchCriteria searchCriteria, VXUserList vXUserList) {
		List<VXUser> xUserList = new ArrayList<VXUser>();

		@SuppressWarnings("unchecked")
		List<XXUser> resultList = (List<XXUser>) searchResources(searchCriteria, searchFields, sortFields, vXUserList);

		for (XXUser xXUser : resultList) {
			VXUser vObj = super.mapEntityToViewBean(createViewObject(), xXUser);
			vObj.setIsVisible(xXUser.getIsVisible());
			xUserList.add(vObj);
		}

		vXUserList.setVXUsers(xUserList);
		return vXUserList;
	}

	public Map<Long, Object[]> getXXPortalUserIdXXUserNameMap() {
		Map<Long, Object[]> xXPortalUserIdXXUserMap = new HashMap<Long, Object[]>();
		try {
			List<Object[]> xxUserList = daoManager.getXXUser().getAllUserIdNames();
			if(!CollectionUtils.isEmpty(xxUserList)) {
				for (Object[] obj : xxUserList) {
					xXPortalUserIdXXUserMap.put((Long)obj[0], obj);
				}
			}
		} catch (Exception ex) {
		}
		return xXPortalUserIdXXUserMap;
	}

}
