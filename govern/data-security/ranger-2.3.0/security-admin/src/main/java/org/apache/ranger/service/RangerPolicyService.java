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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.biz.RangerPolicyRetriever;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.view.VTrxLogAttr;
import org.apache.ranger.entity.XXDataMaskTypeDef;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerDataMaskPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerPolicy.RangerRowFilterPolicyItem;
import org.apache.ranger.plugin.model.RangerValiditySchedule;
import org.apache.ranger.plugin.util.JsonUtilsV2;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class RangerPolicyService extends RangerPolicyServiceBase<XXPolicy, RangerPolicy> {
	private static final Logger logger = LoggerFactory.getLogger(RangerPolicyService.class);

	@Autowired
	JSONUtil jsonUtil;
	
	public static final String POLICY_RESOURCE_CLASS_FIELD_NAME = "resources";
	public static final String POLICY_ITEM_CLASS_FIELD_NAME = "policyItems";
	public static final String POLICY_NAME_CLASS_FIELD_NAME = "name";
	public static final String POLICY_DESCRIPTION_CLASS_FIELD_NAME = "description";
	public static final String DENYPOLICY_ITEM_CLASS_FIELD_NAME = "denyPolicyItems";
	public static final String ALLOW_EXCEPTIONS_CLASS_FIELD_NAME="allowExceptions";
	public static final String DENY_EXCEPTIONS_CLASS_FIELD_NAME="denyExceptions";
	public static final String DATAMASK_POLICY_ITEM_CLASS_FIELD_NAME="dataMaskPolicyItems";
	public static final String ROWFILTER_POLICY_ITEM_CLASS_FIELD_NAME="rowFilterPolicyItems";
	public static final String IS_ENABLED_CLASS_FIELD_NAME="isEnabled";
	public static final String IS_AUDIT_ENABLED_CLASS_FIELD_NAME="isAuditEnabled";
        public static final String POLICY_LABELS_CLASS_FIELD_NAME="policyLabels";
        public static final String POLICY_VALIDITYSCHEDULES_CLASS_FIELD_NAME="validitySchedules";
        public static final String POLICY_PRIORITY_CLASS_FIELD_NAME="policyPriority";
        public static final String POLICY_CONDITION_CLASS_FIELD_NAME="conditions";
        public static final String POLICY_IS_DENY_ALL_ELSE_CLASS_FIELD_NAME="isDenyAllElse";
        public static final String POLICY_ZONE_NAME_CLASS_FIELD_NAME="zoneName";

	static HashMap<String, VTrxLogAttr> trxLogAttrs = new HashMap<String, VTrxLogAttr>();
	String actionCreate;
	String actionImportCreate;
	String actionUpdate;
	String actionDelete;
	String actionImportDelete;

	static {
		trxLogAttrs.put("name", new VTrxLogAttr("name", "Policy Name", false));
		trxLogAttrs.put("description", new VTrxLogAttr("description", "Policy Description", false));
		trxLogAttrs.put("isEnabled", new VTrxLogAttr("isEnabled", "Policy Status", false));
		trxLogAttrs.put("resources", new VTrxLogAttr("resources", "Policy Resources", false));
                trxLogAttrs.put("conditions", new VTrxLogAttr("conditions", "Policy Conditions", false));
		trxLogAttrs.put("policyItems", new VTrxLogAttr("policyItems", "Policy Items", false));
		trxLogAttrs.put("denyPolicyItems", new VTrxLogAttr("denyPolicyItems", "DenyPolicy Items", false));
		trxLogAttrs.put("allowExceptions", new VTrxLogAttr("allowExceptions", "Allow Exceptions", false));
		trxLogAttrs.put("denyExceptions", new VTrxLogAttr("denyExceptions", "Deny Exceptions", false));
		trxLogAttrs.put("dataMaskPolicyItems", new VTrxLogAttr("dataMaskPolicyItems", "Masked Policy Items", false));
		trxLogAttrs.put("rowFilterPolicyItems", new VTrxLogAttr("rowFilterPolicyItems", "Row level filter Policy Items", false));
		trxLogAttrs.put("isAuditEnabled", new VTrxLogAttr("isAuditEnabled", "Audit Status", false));
		trxLogAttrs.put("policyLabels", new VTrxLogAttr("policyLabels", "Policy Labels", false));
		trxLogAttrs.put("validitySchedules", new VTrxLogAttr("validitySchedules", "Validity Schedules", false));
		trxLogAttrs.put("policyPriority", new VTrxLogAttr("policyPriority", "Priority", false));
		trxLogAttrs.put("zoneName", new VTrxLogAttr("zoneName", "Zone Name", false));
                trxLogAttrs.put("isDenyAllElse", new VTrxLogAttr("isDenyAllElse", "Deny All Other Accesses", false));
	}
	
	public RangerPolicyService() {
		super();
		actionCreate = "create";
		actionImportCreate = "Import Create";
		actionImportDelete = "Import Delete";
		actionUpdate = "update";
		actionDelete = "delete";
	}

	@Override
	protected XXPolicy mapViewToEntityBean(RangerPolicy vObj, XXPolicy xObj, int OPERATION_CONTEXT) {
		return super.mapViewToEntityBean(vObj, xObj, OPERATION_CONTEXT);
	}

	@Override
	protected RangerPolicy mapEntityToViewBean(RangerPolicy vObj, XXPolicy xObj) {
		return super.mapEntityToViewBean(vObj, xObj);
	}
	
	@Override
	protected void validateForCreate(RangerPolicy vObj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void validateForUpdate(RangerPolicy vObj, XXPolicy entityObj) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected RangerPolicy populateViewBean(XXPolicy xPolicy) {
		RangerPolicyRetriever retriever = new RangerPolicyRetriever(daoMgr);

		RangerPolicy vPolicy = retriever.getPolicy(xPolicy);
		
		return vPolicy;
	}
	
	public RangerPolicy getPopulatedViewObject(XXPolicy xPolicy) {
		return this.populateViewBean(xPolicy);
	}
	
	public List<XXTrxLog> getTransactionLog(RangerPolicy vPolicy, int action) {
		return getTransactionLog(vPolicy, null, null, action);
	}

	public List<XXTrxLog> getTransactionLog(RangerPolicy vObj, XXPolicy mObj, RangerPolicy oldPolicy, int action) {
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
				XXTrxLog xTrxLog = processFieldToCreateTrxLog(field, objectName, vObj, mObj, oldPolicy, action);
				if (xTrxLog != null) {
					trxLogList.add(xTrxLog);
				}
			}

			Field[] superClassFields = vObj.getClass().getSuperclass()
					.getDeclaredFields();
			for (Field field : superClassFields) {
				if ("isEnabled".equalsIgnoreCase(field.getName())) {
					XXTrxLog xTrx = processFieldToCreateTrxLog(field, objectName, vObj, mObj, oldPolicy, action);
					if (xTrx != null) {
						trxLogList.add(xTrx);
					}
					break;
				}
			}
		} catch (IllegalAccessException illegalAcc) {
			logger.error("Transaction log failure.", illegalAcc);
		} catch (NoSuchFieldException noSuchField) {
			logger.error("Transaction log failure.", noSuchField);
		}
		
		return trxLogList;
	}

	public String restrictIsDenyAllElseLogForMaskingAndRowfilterPolicy(String fieldName, RangerPolicy vObj) {
		if (logger.isDebugEnabled()) {
			logger.debug("==> RangerPolicyService( Field Name : (" + fieldName +") RangerPolicy : ("+ vObj + ")");
		}
		String ret = "";
		if (StringUtils.isNotBlank(fieldName)
				&& StringUtils.equalsIgnoreCase(fieldName.trim(), POLICY_IS_DENY_ALL_ELSE_CLASS_FIELD_NAME)
				&& vObj != null) {
			Integer policyType = vObj.getPolicyType();
			if (policyType == null || policyType == RangerPolicy.POLICY_TYPE_ACCESS) {
				return ret;
			} else if (policyType == RangerPolicy.POLICY_TYPE_ROWFILTER
						|| policyType == RangerPolicy.POLICY_TYPE_DATAMASK) {
					ret = null;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("<== RangerPolicyService( Field Name : (" + fieldName +") RangerPolicy : ("+ vObj + ") ret : ( "+ret+" )");
		}
		return ret;
	}
	private XXTrxLog processFieldToCreateTrxLog(Field field, String objectName,
			RangerPolicy vObj, XXPolicy mObj, RangerPolicy oldPolicy, int action) {

		String actionString = "";

		field.setAccessible(true);
		String fieldName = field.getName();
		XXTrxLog xTrxLog = new XXTrxLog();
                XXService parentObj = daoMgr.getXXService().findByName(vObj.getService());
		try {
			VTrxLogAttr vTrxLogAttr = trxLogAttrs.get(fieldName);

			xTrxLog.setAttributeName(vTrxLogAttr.getAttribUserFriendlyName());

			String value = null;
			boolean isEnum = vTrxLogAttr.isEnum();
			if (!isEnum) {
			    if (POLICY_RESOURCE_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
    				value = processPolicyResourcesForTrxLog(field.get(vObj));
				} else if (POLICY_CONDITION_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					value = processPolicyItemsForTrxLog(field.get(vObj));
    			} else if (POLICY_ITEM_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
    				value = processPolicyItemsForTrxLog(field.get(vObj));
    			} else if (DENYPOLICY_ITEM_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
    				value = processPolicyItemsForTrxLog(field.get(vObj));
    			} else if (POLICY_NAME_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
    				value = processPolicyNameForTrxLog(field.get(vObj));
    			} else if (ALLOW_EXCEPTIONS_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
    				value = processPolicyItemsForTrxLog(field.get(vObj));
    			} else if (DENY_EXCEPTIONS_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
    				value = processPolicyItemsForTrxLog(field.get(vObj));
    			} else if (DATAMASK_POLICY_ITEM_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
    				value = processDataMaskPolicyItemsForTrxLog(field.get(vObj));
    				if(vObj.getDataMaskPolicyItems() != null && CollectionUtils.isNotEmpty(vObj.getDataMaskPolicyItems())) {
    					for(RangerDataMaskPolicyItem policyItem : vObj.getDataMaskPolicyItems()) {
    						if(policyItem.getDataMaskInfo() != null && policyItem.getDataMaskInfo().getDataMaskType() != null) {
    							List<XXDataMaskTypeDef> xDataMaskDef = daoMgr.getXXDataMaskTypeDef().getAll();
    							if(CollectionUtils.isNotEmpty(xDataMaskDef) && xDataMaskDef != null ) {
    								for (XXDataMaskTypeDef xxDataMaskTypeDef : xDataMaskDef) {
    									if(xxDataMaskTypeDef.getName().equalsIgnoreCase(policyItem.getDataMaskInfo().getDataMaskType())) {
    										String label = xxDataMaskTypeDef.getLabel();
    										StringBuilder sbValue = new StringBuilder(value);
    										label = ",\"DataMasklabel\":\""+label+"\"";
    										int sbValueIndex = sbValue.lastIndexOf("}]");
    										sbValue.insert(sbValueIndex, label);
    										value = sbValue.toString();
    										break;
    									}
    								}
    							}
    						}
    					}
    				}
    			} else if (ROWFILTER_POLICY_ITEM_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
    				value = processRowFilterPolicyItemForTrxLog(field.get(vObj));
				} else if (IS_ENABLED_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					value = processIsEnabledClassFieldNameForTrxLog(field.get(vObj));
				} else if (POLICY_LABELS_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					value = processPolicyLabelsClassFieldNameForTrxLog(field.get(vObj));
				} else if (POLICY_VALIDITYSCHEDULES_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					value = processValiditySchedulesClassFieldNameForTrxLog(field.get(vObj));
				} else if (POLICY_PRIORITY_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
    				value = processPriorityClassFieldNameForTrxLog(field.get(vObj));
				} else if (IS_AUDIT_ENABLED_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					value = processIsAuditEnabledClassFieldNameForTrxLog(field.get(vObj));
				} else if (POLICY_IS_DENY_ALL_ELSE_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					value = processIsAuditEnabledClassFieldNameForTrxLog(field.get(vObj));
				} else if (POLICY_ZONE_NAME_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					value = processPolicyNameForTrxLog(field.get(vObj));
				}
                                else {
    				value = "" + field.get(vObj);
    			}
			}

			if (action == OPERATION_CREATE_CONTEXT) {
				if(restrictIsDenyAllElseLogForMaskingAndRowfilterPolicy(fieldName, vObj) == null) {
					return null;
				}
				if (stringUtil.isEmpty(value)) {
					return null;
				}
				xTrxLog.setNewValue(value);
				actionString = actionCreate;
			} else if (action == OPERATION_DELETE_CONTEXT) {
				if(restrictIsDenyAllElseLogForMaskingAndRowfilterPolicy(fieldName, vObj) == null) {
					return null;
				}
				xTrxLog.setPreviousValue(value);
				actionString = actionDelete;
			} else if (action == OPERATION_UPDATE_CONTEXT) {
				if(restrictIsDenyAllElseLogForMaskingAndRowfilterPolicy(fieldName, vObj) == null) {
					return null;
				}
				actionString = actionUpdate;
				String oldValue = null;
				Field[] mFields = mObj.getClass().getDeclaredFields();
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
				if (POLICY_RESOURCE_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if (oldPolicy != null) {
						oldValue = processPolicyResourcesForTrxLog(oldPolicy.getResources());
					}
				} else if (POLICY_ITEM_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if (oldPolicy != null) {
						oldValue = processPolicyItemsForTrxLog(oldPolicy.getPolicyItems());
					}
				} else if (DENYPOLICY_ITEM_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if (oldPolicy != null) {
						oldValue = processPolicyItemsForTrxLog(oldPolicy.getDenyPolicyItems());
					}
				} else if (POLICY_NAME_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if (oldPolicy != null) {
						oldValue = processPolicyNameForTrxLog(oldPolicy.getName());
					}
				} else if (POLICY_DESCRIPTION_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if (oldPolicy != null) {
						oldValue = processPolicyNameForTrxLog(oldPolicy.getDescription());
					}
				}  else if (ALLOW_EXCEPTIONS_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if (oldPolicy != null) {
						oldValue = processPolicyItemsForTrxLog(oldPolicy.getAllowExceptions());
					}
				} else if (DENY_EXCEPTIONS_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if (oldPolicy != null) {
						oldValue = processPolicyItemsForTrxLog(oldPolicy.getDenyExceptions());
					}
				} else if (DATAMASK_POLICY_ITEM_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if (oldPolicy != null) {
						oldValue = processDataMaskPolicyItemsForTrxLog(oldPolicy.getDataMaskPolicyItems());
						if(oldPolicy.getDataMaskPolicyItems() != null && CollectionUtils.isNotEmpty(oldPolicy.getDataMaskPolicyItems())) {
							for(RangerDataMaskPolicyItem oldPolicyItem : oldPolicy.getDataMaskPolicyItems()) {
								if(oldPolicyItem.getDataMaskInfo() != null && oldPolicyItem.getDataMaskInfo().getDataMaskType() != null) {
									List<XXDataMaskTypeDef> xDataMaskDef = daoMgr.getXXDataMaskTypeDef().getAll();
									if(CollectionUtils.isNotEmpty(xDataMaskDef) && xDataMaskDef != null ) {
										for (XXDataMaskTypeDef xxDataMaskTypeDef : xDataMaskDef) {
											if(xxDataMaskTypeDef.getName().equalsIgnoreCase(oldPolicyItem.getDataMaskInfo().getDataMaskType())) {
												String oldLabel = xxDataMaskTypeDef.getLabel();
												StringBuilder sbOldValue = new StringBuilder(oldValue);
												oldLabel = ",\"DataMasklabel\":\""+oldLabel+"\"";
												int sbValueIndex = sbOldValue.lastIndexOf("}]");
												sbOldValue.insert(sbValueIndex, oldLabel);
												oldValue = sbOldValue.toString();
												break;
											}
										}
									}
								}
							}
						}
					}
				} else if (ROWFILTER_POLICY_ITEM_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if (oldPolicy != null) {
						oldValue = processRowFilterPolicyItemForTrxLog(oldPolicy.getRowFilterPolicyItems());
					}
				}else if (IS_ENABLED_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if (oldPolicy != null) {
						oldValue = processIsEnabledClassFieldNameForTrxLog(oldPolicy.getIsEnabled());
					}
				} else if (IS_AUDIT_ENABLED_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if (oldPolicy != null) {
						oldValue = processIsAuditEnabledClassFieldNameForTrxLog(oldPolicy.getIsAuditEnabled());
					}
				}else if (POLICY_LABELS_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					oldValue = processPolicyLabelsClassFieldNameForTrxLog(oldPolicy.getPolicyLabels());
				} else if (POLICY_VALIDITYSCHEDULES_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					oldValue = processValiditySchedulesClassFieldNameForTrxLog(oldPolicy.getValiditySchedules());
				} else if (POLICY_PRIORITY_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					oldValue = processPriorityClassFieldNameForTrxLog(oldPolicy.getPolicyPriority());
				}
                                else if (POLICY_CONDITION_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
                                        if (oldPolicy != null) {
                                                oldValue = processPolicyItemsForTrxLog(oldPolicy.getConditions());
                                        }
                                }
				else if (POLICY_ZONE_NAME_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					oldValue = oldPolicy != null ? processPolicyNameForTrxLog(oldPolicy.getZoneName()) : "";

				} else if (POLICY_IS_DENY_ALL_ELSE_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					oldValue = oldPolicy != null
							? processIsAuditEnabledClassFieldNameForTrxLog(String.valueOf(oldPolicy.getIsDenyAllElse()))
							: "";
				}
				//start comparing old and new values
				if (POLICY_RESOURCE_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					// Compare old and new resources
					if (compareTwoPolicyResources(value, oldValue)) {
						return null;
					}
				} else if (POLICY_ITEM_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					//Compare old and new policyItems
					if(compareTwoPolicyItemList(value, oldValue)) {
						return null;
					}
				} else if (POLICY_NAME_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					//compare old and new policyName
					if(compareTwoPolicyName(value, oldValue)) {
						return null;
					}
				} else if (DENYPOLICY_ITEM_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					//compare old and new denyPolicyItem
					if(compareTwoPolicyItemList(value, oldValue)) {
						return null;
					}
				} else if (ALLOW_EXCEPTIONS_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					//compare old and new allowExceptions
					if(compareTwoPolicyItemList(value, oldValue)) {
						return null;
					}
				} else if (DENY_EXCEPTIONS_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					//compare old and new denyExceptions
					if(compareTwoPolicyItemList(value, oldValue)) {
						return null;
					}
				} else if (POLICY_DESCRIPTION_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					//compare old and new Description
					if(StringUtils.equals(value, oldValue)) {
						return null;
					}
				} else if (DATAMASK_POLICY_ITEM_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					//compare old and new dataMaskPolicyItems
					if(compareTwoDataMaskingPolicyItemList(value, oldValue)) {
						return null;
					}
				} else if (ROWFILTER_POLICY_ITEM_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					//compare old and new rowFilterPolicyItems
					if(compareTwoRowFilterPolicyItemList(value, oldValue)) {
						return null;
					}
				}else if (IS_AUDIT_ENABLED_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if(compareTwoPolicyName(value, oldValue)) {
					    return null;
					}
				} else if (IS_ENABLED_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if(compareTwoPolicyName(value, oldValue)) {
					    return null;
					}
				} else if (IS_AUDIT_ENABLED_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if (compareTwoPolicyName(value, oldValue)) {
						return null;
					}
				} else if (POLICY_LABELS_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if (compareTwoPolicyLabelList(value, oldValue)) {
						return null;
					}
				}
				else if (POLICY_ZONE_NAME_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					if(StringUtils.isBlank(oldValue)) {
						if (!(stringUtil.isEmpty(value) && compareTwoPolicyName(value, oldValue))) {
							oldValue=value;
						}else {
							return null;
						}
					}
				}
				else if (POLICY_IS_DENY_ALL_ELSE_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
					// comparing old and new value for isDenyAllElse
					if (compareTwoPolicyName(value, oldValue)) {
						return null;
					}
				}else if (POLICY_PRIORITY_CLASS_FIELD_NAME.equalsIgnoreCase(fieldName)) {
				 if(StringUtils.equals(value, oldValue)) {
					 return null;
				 }
				}

				xTrxLog.setPreviousValue(oldValue);
				xTrxLog.setNewValue(value);
			}
			else if (action == OPERATION_IMPORT_CREATE_CONTEXT) {
				if(restrictIsDenyAllElseLogForMaskingAndRowfilterPolicy(fieldName, vObj) == null) {
					return null;
				}
				if (stringUtil.isEmpty(value)) {
					return null;
				}
				xTrxLog.setNewValue(value);
				actionString = actionImportCreate;
			} else if (action == OPERATION_IMPORT_DELETE_CONTEXT) {
				if(restrictIsDenyAllElseLogForMaskingAndRowfilterPolicy(fieldName, vObj) == null) {
					return null;
				}
				xTrxLog.setPreviousValue(value);
				actionString = actionImportDelete;
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Process field to create trx log failure.", e);
		}

		xTrxLog.setAction(actionString);
		xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_RANGER_POLICY);
		xTrxLog.setObjectId(vObj.getId());
		xTrxLog.setObjectName(objectName);

		xTrxLog.setParentObjectClassType(AppConstants.CLASS_TYPE_XA_SERVICE);
		xTrxLog.setParentObjectId(parentObj.getId());
		xTrxLog.setParentObjectName(parentObj.getName());

		return xTrxLog;
	}

        private boolean compareTwoPolicyLabelList(String value, String oldValue) {
                if (value == null && oldValue == null) {
                        return true;
                }
                if (value == "" && oldValue == "") {
                        return true;
                }
                if (stringUtil.isEmpty(value) || stringUtil.isEmpty(oldValue)) {
                        return false;
                }
                ObjectMapper mapper = JsonUtilsV2.getMapper();
                try {
                        List<String> obj = mapper.readValue(value, new TypeReference<List<String>>() {
                        });
                        List<String> oldObj = mapper.readValue(oldValue, new TypeReference<List<String>>() {
                        });
                        int oldListSize = oldObj.size();
                        int listSize = obj.size();
                        if (oldListSize != listSize) {
                                return false;
                        }
                        for (String polItem : obj) {
                                if (!oldObj.contains(polItem)) {
                                        return false;
                                }
                        }
                        return true;
                } catch (JsonParseException e) {
                        throw restErrorUtil.createRESTException("Invalid input data: " + e.getMessage(),
                                        MessageEnums.INVALID_INPUT_DATA);
                } catch (JsonMappingException e) {
                        throw restErrorUtil.createRESTException("Invalid input data: " + e.getMessage(),
                                        MessageEnums.INVALID_INPUT_DATA);
                } catch (IOException e) {
                        throw restErrorUtil.createRESTException("Invalid input data: " + e.getMessage(),
                                        MessageEnums.INVALID_INPUT_DATA);
                }
        }

	private boolean compareTwoPolicyItemList(String value, String oldValue) {
		if (value == null && oldValue == null) {
			return true;
		}
		if (value == "" && oldValue == "") {
			return true;
		}
		if (stringUtil.isEmpty(value) || stringUtil.isEmpty(oldValue)) {
			return false;
		}

		ObjectMapper mapper = JsonUtilsV2.getMapper();
		try {
			List<RangerPolicyItem> obj = mapper.readValue(value,
					new TypeReference<List<RangerPolicyItem>>() {
					});
			List<RangerPolicyItem> oldObj = mapper.readValue(oldValue,
					new TypeReference<List<RangerPolicyItem>>() {
					});
			
			int oldListSize = oldObj.size();
			int listSize = obj.size();
			if(oldListSize != listSize) {
				return false;
			}
			
			for(RangerPolicyItem polItem : obj) {
				if(!oldObj.contains(polItem)) {
					return false;
				}
			}
			return true;
		} catch (JsonParseException e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		} catch (JsonMappingException e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		} catch (IOException e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		}
	}

	private boolean compareTwoPolicyResources(String value, String oldValue) {
		if (value == null && oldValue == null) {
			return true;
		}
		if (value == "" && oldValue == "") {
			return true;
		}
		if (stringUtil.isEmpty(value) || stringUtil.isEmpty(oldValue)) {
			return false;
		}

		ObjectMapper mapper = JsonUtilsV2.getMapper();
		try {
			Map<String, RangerPolicyResource> obj = mapper.readValue(value,
					new TypeReference<Map<String, RangerPolicyResource>>() {
					});
			Map<String, RangerPolicyResource> oldObj = mapper.readValue(oldValue,
					new TypeReference<Map<String, RangerPolicyResource>>() {
					});
			
			if (obj.size() != oldObj.size()) {
				return false;
			}
			
			for (Map.Entry<String, RangerPolicyResource> entry : obj.entrySet()) {
				if (!entry.getValue().equals(oldObj.get(entry.getKey()))) {
					return false;
				}
			}
			return true;
		} catch (JsonParseException e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		} catch (JsonMappingException e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		} catch (IOException e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		}
	}

	@SuppressWarnings("unchecked")
	private String processPolicyItemsForTrxLog(Object value) {
		if(value == null) {
			return "";
		}
		List<RangerPolicyItem> rangerPolicyItems = (List<RangerPolicyItem>) value;
		if(rangerPolicyItems == null || rangerPolicyItems.isEmpty()) {
			return "";
		}
		String ret = jsonUtil.readListToString(rangerPolicyItems);
		if(ret == null) {
			return "";
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	private String processPolicyResourcesForTrxLog(Object value) {
		if (value == null) {
			return "";
		}
		Map<String, RangerPolicyResource> resources = (Map<String, RangerPolicyResource>) value;
		String ret = jsonUtil.readMapToString(resources);
		if(ret == null) {
			return "";
		}
		return ret;
	}

	private boolean compareTwoPolicyName(String value, String oldValue) {
		return StringUtils.equals(value, oldValue);
	}

	private String processPolicyNameForTrxLog(Object value) {
		if (value == null) {
			return "";
		}
		String name = (String) value;
		return name;
	}

	@SuppressWarnings("unchecked")
        private String processPolicyLabelsClassFieldNameForTrxLog(Object value) {
                if (value == null) {
                        return "";
                }
                List<String> policyLabels = (List<String>) value;
                String ret = jsonUtil.readListToString(policyLabels);
                return ret;
        }
	@SuppressWarnings("unchecked")
	private String processValiditySchedulesClassFieldNameForTrxLog(Object value) {
		if (value == null) {
			return "";
		}
		List<RangerValiditySchedule> validitySchedules = (List<RangerValiditySchedule>) value;
		String ret = jsonUtil.readListToString(validitySchedules);
		return ret;
	}
	@SuppressWarnings("unchecked")
	private String processPriorityClassFieldNameForTrxLog(Object value) {
		if (value == null) {
			return "";
		}
		Integer policyPriority = (Integer) value;
		return policyPriority.toString();
	}
	@SuppressWarnings("unchecked")
	private String processDataMaskPolicyItemsForTrxLog(Object value) {
		if(value == null) {
			return "";
		}
		List<RangerDataMaskPolicyItem> rangerPolicyItems = (List<RangerDataMaskPolicyItem>) value;
		if(rangerPolicyItems == null || rangerPolicyItems.isEmpty()) {
			return "";
		}
		String ret = jsonUtil.readListToString(rangerPolicyItems);
		if(ret == null) {
			return "";
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	private String processRowFilterPolicyItemForTrxLog(Object value) {
		if(value == null) {
			return "";
		}
		List<RangerRowFilterPolicyItem> rangerPolicyItems = (List<RangerRowFilterPolicyItem>) value;
		if(rangerPolicyItems == null || rangerPolicyItems.isEmpty()) {
			return "";
		}
		String ret = jsonUtil.readListToString(rangerPolicyItems);
		if(ret == null) {
			return "";
		}
		return ret;
	}
	private String processIsEnabledClassFieldNameForTrxLog(Object value) {
		if(value == null)
			return null;
		String isEnabled = String.valueOf(value);
			return isEnabled;
	}

	private String processIsAuditEnabledClassFieldNameForTrxLog(Object value) {
		if(value == null)
			return null;
		String isAuditEnabled = String.valueOf(value);
		return isAuditEnabled;
	}

	private boolean compareTwoDataMaskingPolicyItemList(String value, String oldValue) {
		if (value == null && oldValue == null) {
			return true;
		}
		if (value == "" && oldValue == "") {
			return true;
		}
		if (stringUtil.isEmpty(value) || stringUtil.isEmpty(oldValue)) {
			return false;
		}
		ObjectMapper mapper = JsonUtilsV2.getMapper();
		try {
			List<RangerDataMaskPolicyItem> obj = mapper.readValue(value,
					new TypeReference<List<RangerDataMaskPolicyItem>>() {
					});
			List<RangerDataMaskPolicyItem> oldObj = mapper.readValue(oldValue,
					new TypeReference<List<RangerDataMaskPolicyItem>>() {
					});
			int oldListSize = oldObj.size();
			int listSize = obj.size();
			if(oldListSize != listSize) {
				return false;
			}
			for(RangerDataMaskPolicyItem polItem : obj) {
				if(!oldObj.contains(polItem)) {
					return false;
				}
			}
			return true;
		} catch (JsonParseException e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		} catch (JsonMappingException e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		} catch (IOException e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		}
	}

	private boolean compareTwoRowFilterPolicyItemList(String value, String oldValue) {
		if (value == null && oldValue == null) {
			return true;
		}
		if (value == "" && oldValue == "") {
			return true;
		}
		if (stringUtil.isEmpty(value) || stringUtil.isEmpty(oldValue)) {
			return false;
		}
		ObjectMapper mapper = JsonUtilsV2.getMapper();
		try {
			List<RangerRowFilterPolicyItem> obj = mapper.readValue(value,
					new TypeReference<List<RangerRowFilterPolicyItem>>() {
					});
			List<RangerRowFilterPolicyItem> oldObj = mapper.readValue(oldValue,
					new TypeReference<List<RangerRowFilterPolicyItem>>() {
					});
			int oldListSize = oldObj.size();
			int listSize = obj.size();
			if(oldListSize != listSize) {
				return false;
			}
			for(RangerRowFilterPolicyItem polItem : obj) {
				if(!oldObj.contains(polItem)) {
					return false;
				}
			}
			return true;
		} catch (JsonParseException e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		} catch (JsonMappingException e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		} catch (IOException e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		}
	}
}
