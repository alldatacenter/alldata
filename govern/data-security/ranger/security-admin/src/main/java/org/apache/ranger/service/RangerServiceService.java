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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.view.VTrxLogAttr;
import org.apache.ranger.db.XXServiceVersionInfoDao;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceConfigMap;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.entity.XXServiceVersionInfo;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.util.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;

@Service
@Scope("singleton")
public class RangerServiceService extends RangerServiceServiceBase<XXService, RangerService> {
	private static final Logger LOG = LoggerFactory.getLogger(RangerServiceService.class.getName());
	@Autowired
	JSONUtil jsonUtil;

	private String hiddenPasswordString;

	static HashMap<String, VTrxLogAttr> trxLogAttrs = new HashMap<String, VTrxLogAttr>();
	String actionCreate;
	String actionUpdate;
	String actionDelete;
	static {
		trxLogAttrs.put("name", new VTrxLogAttr("name", "Service Name", false));
		trxLogAttrs.put("displayName", new VTrxLogAttr("displayName", "Service Display Name", false));
		trxLogAttrs.put("description", new VTrxLogAttr("description", "Service Description", false));
		trxLogAttrs.put("isEnabled", new VTrxLogAttr("isEnabled", "Service Status", false));
		trxLogAttrs.put("configs", new VTrxLogAttr("configs", "Connection Configurations", false));
		trxLogAttrs.put("tagService", new VTrxLogAttr("tagService", "Tag Service Name", false));
	}
	
	public RangerServiceService() {
		super();
		hiddenPasswordString = PropertiesUtil.getProperty("ranger.password.hidden", "*****");
		actionCreate = "create";
		actionUpdate = "update";
		actionDelete = "delete";
	}

	@Override
	protected XXService mapViewToEntityBean(RangerService vObj, XXService xObj, int OPERATION_CONTEXT) {
		return super.mapViewToEntityBean(vObj, xObj, OPERATION_CONTEXT);
	}

	@Override
	protected RangerService mapEntityToViewBean(RangerService vObj, XXService xObj) {
		return super.mapEntityToViewBean(vObj, xObj);
	}
	
	@Override
	protected void validateForCreate(RangerService vObj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void validateForUpdate(RangerService vService, XXService xService) {
		
	}
	
	@Override
	protected RangerService populateViewBean(XXService xService) {
		RangerService vService = super.populateViewBean(xService);
		
		HashMap<String, String> configs = new HashMap<String, String>();
		List<XXServiceConfigMap> svcConfigMapList = daoMgr.getXXServiceConfigMap()
				.findByServiceId(xService.getId());
		for(XXServiceConfigMap svcConfMap : svcConfigMapList) {
			String configValue = svcConfMap.getConfigvalue();
			
			if(StringUtils.equalsIgnoreCase(svcConfMap.getConfigkey(), ServiceDBStore.CONFIG_KEY_PASSWORD)) {
				configValue = ServiceDBStore.HIDDEN_PASSWORD_STR;
			}
			configs.put(svcConfMap.getConfigkey(), configValue);
		}
		vService.setConfigs(configs);
		return vService;
	}
	
	public RangerService getPopulatedViewObject(XXService xService) {
		return this.populateViewBean(xService);
	}
	
	public List<RangerService> getAllServices() {
		List<XXService> xxServiceList = daoMgr.getXXService().getAll();
		List<RangerService> serviceList = new ArrayList<RangerService>();
		
		for(XXService xxService : xxServiceList) {
			RangerService service = populateViewBean(xxService);
			serviceList.add(service);
		}
		return serviceList;
	}
	
	public List<XXTrxLog> getTransactionLog(RangerService vService, int action){
		return getTransactionLog(vService, null, action);
	}

	public List<XXTrxLog> getTransactionLog(RangerService vObj, XXService mObj, int action) {
		if (vObj == null || action == 0 || (action == OPERATION_UPDATE_CONTEXT && mObj == null)) {
			return null;
		}
		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		Field[] fields = vObj.getClass().getDeclaredFields();

		try {
			Field nameField = vObj.getClass().getDeclaredField("name");
			nameField.setAccessible(true);
			String objectName = "" + nameField.get(vObj);

			for (Field field : fields) {
				if (!trxLogAttrs.containsKey(field.getName())) {
					continue;
				}
				XXTrxLog xTrxLog = processFieldToCreateTrxLog(field,
						objectName, vObj, mObj, action);
				if (xTrxLog != null) {
					trxLogList.add(xTrxLog);
				}
			}
			Field[] superClassFields = vObj.getClass().getSuperclass().getDeclaredFields();
			for(Field field : superClassFields) {
				if("isEnabled".equalsIgnoreCase(field.getName())) {
					XXTrxLog xTrx = processFieldToCreateTrxLog(field, objectName, vObj, mObj, action);
					if(xTrx != null) {
						trxLogList.add(xTrx);
					}
					break;
				}
			}
		} catch (IllegalAccessException e) {
			LOG.error("Transaction log failure.", e);
		} catch (NoSuchFieldException e) {
			LOG.error("Transaction log failure.", e);
		}
		return trxLogList;
	}

	@SuppressWarnings("unchecked")
	private XXTrxLog processFieldToCreateTrxLog(Field field, String objectName,
			RangerService vObj, XXService mObj, int action) {

		String actionString = "";

		field.setAccessible(true);
		String fieldName = field.getName();
		XXTrxLog xTrxLog = new XXTrxLog();

		try {
			VTrxLogAttr vTrxLogAttr = trxLogAttrs.get(fieldName);

			xTrxLog.setAttributeName(vTrxLogAttr.getAttribUserFriendlyName());

			String value = null;
			boolean isEnum = vTrxLogAttr.isEnum();
			if (!isEnum) {
			    if ("configs".equalsIgnoreCase(fieldName)) {
    				Map<String, String> configs = (field.get(vObj) != null) ? (Map<String, String>) field
    						.get(vObj) : new HashMap<String, String>();
    
    						value = jsonUtil.readMapToString(configs);
    			} else {
    				value = "" + field.get(vObj);
    			}
			}

			if (action == OPERATION_CREATE_CONTEXT) {
				if (stringUtil.isEmpty(value)) {
					return null;
				}
				xTrxLog.setNewValue(value);
				actionString = actionCreate;
			} else if (action == OPERATION_DELETE_CONTEXT) {
				xTrxLog.setPreviousValue(value);
				actionString = actionDelete;
			} else if (action == OPERATION_UPDATE_CONTEXT) {
				actionString = actionUpdate;
				String oldValue = null;
				Field[] mFields = mObj.getClass().getSuperclass().getDeclaredFields();
				for (Field mField : mFields) {
					mField.setAccessible(true);
					String mFieldName = mField.getName();
					if (fieldName.equalsIgnoreCase(mFieldName)) {
						if (!isEnum) {
							oldValue = mField.get(mObj) + "";
						}
						break;
					}
				}
				if ("configs".equalsIgnoreCase(fieldName)) {
					Map<String, String> vConfig = jsonUtil.jsonToMap(value);
					RangerService oldService = this.populateViewBean(mObj);
					Map<String, String> xConfig = oldService.getConfigs();

					Map<String, String> newConfig = new HashMap<String, String>();
					Map<String, String> oldConfig = new HashMap<String, String>();

					for (Entry<String, String> entry : vConfig.entrySet()) {

						String key = entry.getKey();
						if (!xConfig.containsKey(key)) {
							if(StringUtils.isNotEmpty(entry.getValue())) {
								newConfig.put(key, entry.getValue());
							}
						} else if (!entry.getValue().equalsIgnoreCase(
								xConfig.get(key))) {
							if ("password".equalsIgnoreCase(key)
									&& entry.getValue().equalsIgnoreCase(
											hiddenPasswordString)) {
								continue;
							}
							newConfig.put(key, entry.getValue());
							oldConfig.put(key, xConfig.get(key));
						}
					}
					for (Entry<String, String> entry : xConfig.entrySet()) {
						String key = entry.getKey();
						if (!vConfig.containsKey(key)) {
							oldConfig.put(key, entry.getValue());
							newConfig.put(key,null);
						}
					}
					oldValue = jsonUtil.readMapToString(oldConfig);
					value = jsonUtil.readMapToString(newConfig);
				}
				if ("tagService".equalsIgnoreCase(fieldName)) {
					if(!StringUtils.isEmpty(oldValue) && !"null".equalsIgnoreCase(oldValue)){
						RangerService oldService = this.populateViewBean(mObj);
						oldValue=oldService.getTagService();
					}
				}
				if (oldValue == null || value.equalsIgnoreCase(oldValue)) {
					return null;
				}
				xTrxLog.setPreviousValue(oldValue);
				xTrxLog.setNewValue(value);
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			LOG.error("Process field to create trx log failure." , e);
		}

		xTrxLog.setAction(actionString);
		xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_XA_SERVICE);
		xTrxLog.setObjectId(vObj.getId());
		xTrxLog.setObjectName(objectName);

		XXServiceDef parentObj = daoMgr.getXXServiceDef().findByName(vObj.getType());
		xTrxLog.setParentObjectClassType(AppConstants.CLASS_TYPE_XA_SERVICE_DEF);
		xTrxLog.setParentObjectId(parentObj.getId());
		xTrxLog.setParentObjectName(parentObj.getName());

		return xTrxLog;
	}

	public Map<String, String> getConfigsWithDecryptedPassword(RangerService service) throws Exception  {
		Map<String, String> configs = service.getConfigs();
		
		String pwd = configs.get(ServiceDBStore.CONFIG_KEY_PASSWORD);
		if(!stringUtil.isEmpty(pwd) && ServiceDBStore.HIDDEN_PASSWORD_STR.equalsIgnoreCase(pwd)) {
			XXServiceConfigMap pwdConfig = daoMgr.getXXServiceConfigMap().findByServiceAndConfigKey(service.getId(),
					ServiceDBStore.CONFIG_KEY_PASSWORD);

			if (pwdConfig != null) {
				String encryptedPwd = pwdConfig.getConfigvalue();
				if (encryptedPwd.contains(",")) {
					PasswordUtils util = PasswordUtils.build(encryptedPwd);
					String freeTextPasswordMetaData = Joiner.on(",").skipNulls().join(util.getCryptAlgo(),
							new String(util.getEncryptKey()), new String(util.getSalt()), util.getIterationCount(),
							PasswordUtils.needsIv(util.getCryptAlgo()) ? util.getIvAsString() : null);
					String decryptedPwd = PasswordUtils.decryptPassword(encryptedPwd);
					if (StringUtils
							.equalsIgnoreCase(
									freeTextPasswordMetaData + ","
											+ PasswordUtils
													.encryptPassword(freeTextPasswordMetaData + "," + decryptedPwd),
									encryptedPwd)) {
						configs.put(ServiceDBStore.CONFIG_KEY_PASSWORD, encryptedPwd); // XXX: method name is
																						// getConfigsWithDecryptedPassword,
																						// then why do we store the
																						// encryptedPwd?
					}
				} else {
					String decryptedPwd = PasswordUtils.decryptPassword(encryptedPwd);
					if (StringUtils.equalsIgnoreCase(PasswordUtils.encryptPassword(decryptedPwd), encryptedPwd)) {
						configs.put(ServiceDBStore.CONFIG_KEY_PASSWORD, encryptedPwd); // XXX: method name is
																						// getConfigsWithDecryptedPassword,
																						// then why do we store the
																						// encryptedPwd?
					}
				}
			}
		}
		return configs;
	}

	public RangerService postCreate(XXService xObj) {
		XXServiceVersionInfo serviceVersionInfo = new XXServiceVersionInfo();

		serviceVersionInfo.setServiceId(xObj.getId());
		serviceVersionInfo.setPolicyVersion(1L);
		serviceVersionInfo.setTagVersion(1L);
		serviceVersionInfo.setRoleVersion(1L);
		Date now = new Date();
		serviceVersionInfo.setPolicyUpdateTime(now);
		serviceVersionInfo.setTagUpdateTime(now);
		serviceVersionInfo.setRoleUpdateTime(now);

		XXServiceVersionInfoDao serviceVersionInfoDao = daoMgr.getXXServiceVersionInfo();

		XXServiceVersionInfo createdServiceVersionInfo = serviceVersionInfoDao.create(serviceVersionInfo);

		return createdServiceVersionInfo != null ? super.postCreate(xObj) : null;
	}

	protected XXService preDelete(Long id) {
		XXService ret = super.preDelete(id);

		if (ret != null) {
			XXServiceVersionInfoDao serviceVersionInfoDao = daoMgr.getXXServiceVersionInfo();

			XXServiceVersionInfo serviceVersionInfo = serviceVersionInfoDao.findByServiceId(id);

			if (serviceVersionInfo != null) {
				serviceVersionInfoDao.remove(serviceVersionInfo.getId());
			}
		}
		return ret;
	}
}
