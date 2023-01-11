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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.view.VTrxLogAttr;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXServiceDao;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXRole;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.plugin.model.RangerPolicyDelta;
import org.apache.ranger.plugin.model.RangerRole;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.util.RangerEnumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Service
@Scope("singleton")
public class RangerRoleService extends RangerRoleServiceBase<XXRole, RangerRole> {

    @Autowired
    RangerEnumUtil xaEnumUtil;

    private static final Logger logger = LoggerFactory.getLogger(RangerRoleService.class);
    private static final Gson gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z").create();

    static HashMap<String, VTrxLogAttr> trxLogAttrs = new HashMap<>();

    static {
        trxLogAttrs.put("name", new VTrxLogAttr("name", "Role Name", false));
        trxLogAttrs.put("description", new VTrxLogAttr("description", "Role Description", false));
        trxLogAttrs.put("options", new VTrxLogAttr("options", "Options", false));
        trxLogAttrs.put("users", new VTrxLogAttr("users", "Users", false));
        trxLogAttrs.put("adminUsers", new VTrxLogAttr("adminUsers", "Admin Users", false));
        trxLogAttrs.put("groups", new VTrxLogAttr("groups", "Groups", false));
        trxLogAttrs.put("adminGroups", new VTrxLogAttr("adminGroups", "Admin Groups", false));
        trxLogAttrs.put("roles", new VTrxLogAttr("roles", "Roles", false));
        trxLogAttrs.put("adminRoles", new VTrxLogAttr("adminRoles", "Admin Roles", false));
        trxLogAttrs.put("createdByUser", new VTrxLogAttr("createdByUser", "Created By User", false)); }


    public RangerRoleService() {
        super();
    }

    @Override
    protected void validateForCreate(RangerRole vObj) {
    }

    @Override
    protected void validateForUpdate(RangerRole vObj, XXRole entityObj) {
    }

    @Override
    protected XXRole mapViewToEntityBean(RangerRole rangerRole, XXRole xxRole, int OPERATION_CONTEXT) {
        XXRole ret = super.mapViewToEntityBean(rangerRole, xxRole, OPERATION_CONTEXT);

        ret.setRoleText(gsonBuilder.toJson(rangerRole));

        return ret;
    }
    @Override
    protected RangerRole mapEntityToViewBean(RangerRole rangerRole, XXRole xxRole) {
        RangerRole ret = super.mapEntityToViewBean(rangerRole, xxRole);

        if (StringUtils.isNotEmpty(xxRole.getRoleText())) {
            if (logger.isDebugEnabled()) {
                logger.debug("roleText=" + xxRole.getRoleText());
            }
            RangerRole roleFromJsonData = gsonBuilder.fromJson(xxRole.getRoleText(), RangerRole.class);

            if (roleFromJsonData == null) {
                logger.info("Cannot read jsonData into RangerRole object in [" + xxRole.getRoleText() + "]!!");
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Role object built from JSON :[" + roleFromJsonData +"]");
                }
                ret.setOptions(roleFromJsonData.getOptions());
                ret.setUsers(roleFromJsonData.getUsers());
                ret.setGroups(roleFromJsonData.getGroups());
                ret.setRoles(roleFromJsonData.getRoles());
                ret.setCreatedByUser(roleFromJsonData.getCreatedByUser());
            }
        } else {
            logger.info("Empty string representing jsonData in [" + xxRole + "]!!");
        }

        return ret;
    }

    public List<XXTrxLog> getTransactionLog(RangerRole current, RangerRole former, String action) {
        if (current == null || action == null  || ("update".equalsIgnoreCase(action) && former == null)) {
            return null;
        }
        List<XXTrxLog> trxLogList = new ArrayList<>();
        Field[] fields = current.getClass().getDeclaredFields();
        String users   = RangerConstants.MODULE_USER_GROUPS.split("/")[0];
        String groups  = RangerConstants.MODULE_USER_GROUPS.split("/")[1];

        try {
            Field nameField = current.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            String objectName = "" + nameField.get(current);

            for (Field field : fields) {
                String fieldName = field.getName();
                if (!trxLogAttrs.containsKey(fieldName)) {
                    continue;
                }
                field.setAccessible(true);
                VTrxLogAttr vTrxLogAttr = trxLogAttrs.get(fieldName);
                XXTrxLog xTrxLog = new XXTrxLog();
                xTrxLog.setAttributeName(vTrxLogAttr
                        .getAttribUserFriendlyName());
                xTrxLog.setAction(action);
                xTrxLog.setObjectId(current.getId());
                xTrxLog.setObjectClassType(AppConstants.CLASS_TYPE_RANGER_ROLE);
                xTrxLog.setObjectName(objectName);

                if(!StringUtils.isNotBlank(current.getCreatedByUser())){
                    if (logger.isDebugEnabled()) {
                        logger.debug("Created User = " + current.getCreatedByUser());
                    }
                    XXPortalUser xXPortalUser = daoMgr.getXXPortalUser().findByLoginId(current.getCreatedByUser());
                    if(xXPortalUser != null){
                        if (logger.isDebugEnabled()) {
                            logger.debug("User Id for " + current.getCreatedByUser() + " = " + xXPortalUser.getId());
                        }
                        xTrxLog.setAddedByUserId(xXPortalUser.getId());
                        xTrxLog.setUpdatedByUserId(xXPortalUser.getId());
                    }
                }

                String value;
                if (vTrxLogAttr.isEnum()) {
                    String enumName = XXUser.getEnumName(fieldName);
                    int enumValue = field.get(current) == null ? 0 : Integer
                            .parseInt("" + field.get(current));
                    value = xaEnumUtil.getLabel(enumName, enumValue);
                } else {
                    value = "" + field.get(current);
                                        if (fieldName.equalsIgnoreCase(users) || fieldName.equalsIgnoreCase(groups)
                                                        || fieldName.equalsIgnoreCase(RangerConstants.ROLE_FIELD)) {
                                                if (fieldName.equalsIgnoreCase(users)) {
                                                        value = JsonUtils.listToJson(current.getUsers());
                                                }
                                                else if (fieldName.equalsIgnoreCase(groups)) {
                                                        value = JsonUtils.listToJson(current.getGroups());
                                                }
                                                else if (fieldName.equalsIgnoreCase(RangerConstants.ROLE_FIELD)) {
                                                        value = JsonUtils.listToJson(current.getRoles());
                                                }
                                        }
                    if ((value == null || "null".equalsIgnoreCase(value))
                            && !"update".equalsIgnoreCase(action)) {
                        continue;
                    }
                }
                if("options".equalsIgnoreCase(fieldName)) {
                    value = JsonUtils.mapToJson(current.getOptions());
                }
                if ("create".equalsIgnoreCase(action)) {
                    xTrxLog.setNewValue(value);
                    trxLogList.add(xTrxLog);
                }
                else if ("delete".equalsIgnoreCase(action)) {
                    xTrxLog.setPreviousValue(value);
                    trxLogList.add(xTrxLog);
                }
                else if ("update".equalsIgnoreCase(action)) {
                    String formerValue = null;
                    Field[] mFields = current.getClass().getDeclaredFields();
                    for (Field mField : mFields) {
                        mField.setAccessible(true);
                        String mFieldName = mField.getName();
                        if (fieldName.equalsIgnoreCase(mFieldName)) {
                            if("options".equalsIgnoreCase(mFieldName)) {
                                formerValue = JsonUtils.mapToJson(former.getOptions());
                            }
                            else {
                                formerValue = mField.get(former) + "";
                                                                if (fieldName.equalsIgnoreCase(users) || fieldName.equalsIgnoreCase(groups)
                                                                                || fieldName.equalsIgnoreCase(RangerConstants.ROLE_FIELD)) {
                                                                        if (fieldName.equalsIgnoreCase(users)) {
                                                                                formerValue = JsonUtils.listToJson(former.getUsers());
                                                                        }
                                                                        else if (fieldName.equalsIgnoreCase(groups)) {
                                                                                formerValue = JsonUtils.listToJson(former.getGroups());
                                                                        }
                                                                        else if (fieldName.equalsIgnoreCase(RangerConstants.ROLE_FIELD)) {
                                                                                formerValue = JsonUtils.listToJson(former.getRoles());
                                                                        }
                                                                }
                            }
                            break;
                        }
                    }
                    value = ((value == null) ? "" : value);
                    formerValue = ((formerValue == null) ? "" : formerValue);
                    if (formerValue.equalsIgnoreCase(value)) {
                        continue;
                    }
                    xTrxLog.setPreviousValue(formerValue);
                    xTrxLog.setNewValue(value);
                    trxLogList.add(xTrxLog);
                }
                if(logger.isDebugEnabled()) {
                    logger.debug("AddedByUserId for " + xTrxLog.getObjectName() + " = " + xTrxLog.getAddedByUserId());
                }
            }
            if (trxLogList.isEmpty()) {
                if(logger.isDebugEnabled()) {
                    logger.debug("trxLogList is empty!!");
                }
               trxLogList = null;
            }
        } catch (IllegalAccessException e) {
            logger.error("Transaction log failure.", e);
        } catch (NoSuchFieldException e) {
            logger.error("Transaction log failure.", e);
        }
        return trxLogList;
    }

    public void updatePolicyVersions(Long roleId) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> updatePolicyVersions(roleId=" + roleId + ")");
        }
        // Get all roles which include this role because change to this affects all these roles
        Set<Long> containingRoles = getContainingRoles(roleId);

        if (logger.isDebugEnabled()) {
            logger.debug("All containing Roles for roleId:[" + roleId +"] are [" + containingRoles + "]");
        }

        updatePolicyVersions(containingRoles);

        if (logger.isDebugEnabled()) {
            logger.debug("<== updatePolicyVersions(roleId=" + roleId + ")");
        }
    }

    private Set<Long> getContainingRoles(Long roleId) {
        Set<Long> ret = new HashSet<>();

        addContainingRoles(roleId, ret);

        return ret;
    }

    public void updateRoleVersions(Long roleId) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> updateRoleVersions(roleId=" + roleId + ")");
        }
        // Get all roles which include this role because change to this affects all these roles
        Set<Long> containingRoles = getContainingRoles(roleId);

        if (logger.isDebugEnabled()) {
            logger.debug("All containing Roles for roleId:[" + roleId +"] are [" + containingRoles + "]");
        }

        updateRoleVersions(containingRoles);

        if (logger.isDebugEnabled()) {
            logger.debug("<== updateRoleVersions(roleId=" + roleId + ")");
        }
    }

    private void addContainingRoles(Long roleId, Set<Long> allRoles) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> addContainingRoles(roleId=" + roleId + ")");
        }
        if (!allRoles.contains(roleId)) {
            allRoles.add(roleId);

            Set<Long> roles = daoMgr.getXXRoleRefRole().getContainingRoles(roleId);

            for (Long role : roles) {
                addContainingRoles(role, allRoles);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("<== addContainingRoles(roleId=" + roleId + ")");
        }
    }

    private void updatePolicyVersions(Set<Long> roleIds) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> updatePolicyVersions(roleIds=" + roleIds + ")");
        }

        if (CollectionUtils.isNotEmpty(roleIds)) {
            Set<Long> allAffectedServiceIds = new HashSet<>();

            for (Long roleId : roleIds) {
                List<Long> affectedServiceIds = daoMgr.getXXPolicy().findServiceIdsByRoleId(roleId);
                allAffectedServiceIds.addAll(affectedServiceIds);
            }

            if (CollectionUtils.isNotEmpty(allAffectedServiceIds)) {
                for (final Long serviceId : allAffectedServiceIds) {
                    Runnable serviceVersionUpdater = new ServiceDBStore.ServiceVersionUpdater(daoMgr, serviceId, ServiceDBStore.VERSION_TYPE.POLICY_VERSION, null, RangerPolicyDelta.CHANGE_TYPE_SERVICE_CHANGE, null);
                    daoMgr.getRangerTransactionSynchronizationAdapter().executeOnTransactionCommit(serviceVersionUpdater);
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== updatePolicyVersions(roleIds=" + roleIds + ")");
        }
    }

    private void updateRoleVersions(Set<Long> roleIds) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> updatePolicyVersions(roleIds=" + roleIds + ")");
        }

        if (CollectionUtils.isNotEmpty(roleIds)) {
            Set<Long> allAffectedServiceIds = new HashSet<>();

            for (Long roleId : roleIds) {
                List<Long> affectedServiceIds = daoMgr.getXXPolicy().findServiceIdsByRoleId(roleId);
                allAffectedServiceIds.addAll(affectedServiceIds);
            }

            XXServiceDao serviceDao = daoMgr.getXXService();
            if (CollectionUtils.isNotEmpty(allAffectedServiceIds)) {
                for (final Long serviceId : allAffectedServiceIds) {
                    Runnable serviceVersionUpdater = new ServiceDBStore.ServiceVersionUpdater(daoMgr, serviceId, ServiceDBStore.VERSION_TYPE.ROLE_VERSION, null, RangerPolicyDelta.CHANGE_TYPE_ROLE_UPDATE, null);
                    daoMgr.getRangerTransactionSynchronizationAdapter().executeOnTransactionCommit(serviceVersionUpdater);
                    XXService serviceDbObj = serviceDao.getById(serviceId);
                    boolean   isTagService = serviceDbObj.getType() == EmbeddedServiceDefsUtil.instance().getTagServiceDefId();
                    if (isTagService) {
                        updateRoleVersionOfAllServicesRefferingTag(daoMgr, serviceDao, serviceId);
                    }
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== updatePolicyVersions(roleIds=" + roleIds + ")");
        }
    }

    private void updateRoleVersionOfAllServicesRefferingTag(RangerDaoManager daoManager, XXServiceDao serviceDao, Long serviceId) {
        List<XXService> referringServices = serviceDao.findByTagServiceId(serviceId);
        if(CollectionUtils.isNotEmpty(referringServices)) {
            for(XXService referringService : referringServices) {
                final Long referringServiceId = referringService.getId();
                Runnable   roleVersionUpdater = new ServiceDBStore.ServiceVersionUpdater(daoManager, referringServiceId, ServiceDBStore.VERSION_TYPE.ROLE_VERSION, null, RangerPolicyDelta.CHANGE_TYPE_ROLE_UPDATE, null);
                daoMgr.getRangerTransactionSynchronizationAdapter().executeOnTransactionCommit(roleVersionUpdater);
            }
        }
    }
}

