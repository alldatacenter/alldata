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

package org.apache.ranger.plugin.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.plugin.contextenricher.RangerUserStoreEnricher;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemRowFilterInfo;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerPolicy.RangerRowFilterPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicyDelta;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerContextEnricherDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerDataMaskTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.policyengine.RangerPluginContext;
import org.apache.ranger.plugin.policyengine.RangerRequestScriptEvaluator;
import org.apache.ranger.plugin.store.AbstractServiceStore;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.util.ServicePolicies.SecurityZoneInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ServiceDefUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceDefUtil.class);

    private static final String USER_STORE_ENRICHER = RangerUserStoreEnricher.class.getCanonicalName();

    public static boolean getOption_enableDenyAndExceptionsInPolicies(RangerServiceDef serviceDef, RangerPluginContext pluginContext) {
        boolean ret = false;

        if(serviceDef != null) {
            Configuration config = pluginContext != null ? pluginContext.getConfig() : null;
            boolean enableDenyAndExceptionsInPoliciesHiddenOption = config == null || config.getBoolean("ranger.servicedef.enableDenyAndExceptionsInPolicies", true);
            boolean defaultValue = enableDenyAndExceptionsInPoliciesHiddenOption || StringUtils.equalsIgnoreCase(serviceDef.getName(), EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME);

            ret = ServiceDefUtil.getBooleanValue(serviceDef.getOptions(), RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES, defaultValue);
        }

        return ret;
    }

    public static RangerDataMaskTypeDef getDataMaskType(RangerServiceDef serviceDef, String typeName) {
        RangerDataMaskTypeDef ret = null;

        if(serviceDef != null && serviceDef.getDataMaskDef() != null) {
            List<RangerDataMaskTypeDef> maskTypes = serviceDef.getDataMaskDef().getMaskTypes();

            if(CollectionUtils.isNotEmpty(maskTypes)) {
                for(RangerDataMaskTypeDef maskType : maskTypes) {
                    if(StringUtils.equals(maskType.getName(), typeName)) {
                        ret = maskType;
                        break;
                    }
                }
            }
        }

        return ret;
    }

    public static RangerServiceDef normalize(RangerServiceDef serviceDef) {
        normalizeDataMaskDef(serviceDef);
        normalizeRowFilterDef(serviceDef);

        return serviceDef;
    }

    public static RangerResourceDef getResourceDef(RangerServiceDef serviceDef, String resource) {
        RangerResourceDef ret = null;

        if(serviceDef != null && resource != null && CollectionUtils.isNotEmpty(serviceDef.getResources())) {
            for(RangerResourceDef resourceDef : serviceDef.getResources()) {
                if(StringUtils.equalsIgnoreCase(resourceDef.getName(), resource)) {
                    ret = resourceDef;
                    break;
                }
            }
        }

        return ret;
    }

    public static Integer getLeafResourceLevel(RangerServiceDef serviceDef, Map<String, RangerPolicyResource> policyResource) {
        Integer ret = null;

        RangerResourceDef resourceDef = getLeafResourceDef(serviceDef, policyResource);

        if (resourceDef != null) {
            ret = resourceDef.getLevel();
        }

        return ret;
    }

    public static RangerResourceDef getLeafResourceDef(RangerServiceDef serviceDef, Map<String, RangerPolicyResource> policyResource) {
        RangerResourceDef ret = null;

        if(serviceDef != null && policyResource != null) {
            for(Map.Entry<String, RangerPolicyResource> entry : policyResource.entrySet()) {
                if (!isEmpty(entry.getValue())) {
                    String            resource    = entry.getKey();
                    RangerResourceDef resourceDef = ServiceDefUtil.getResourceDef(serviceDef, resource);

                    if (resourceDef != null && resourceDef.getLevel() != null) {
                        if (ret == null) {
                            ret = resourceDef;
                        } else if(ret.getLevel() < resourceDef.getLevel()) {
                            ret = resourceDef;
                        }
                    }
                }
            }
        }

        return ret;
    }

    public static boolean isAncestorOf(RangerServiceDef serviceDef, RangerResourceDef ancestor, RangerResourceDef descendant) {

        boolean ret = false;

        if (ancestor != null && descendant != null) {
            final String ancestorName = ancestor.getName();

            for (RangerResourceDef node = descendant; node != null; node = ServiceDefUtil.getResourceDef(serviceDef, node.getParent())) {
                if (StringUtils.equalsIgnoreCase(ancestorName, node.getParent())) {
                    ret = true;
                    break;
                }
            }
        }

        return ret;
    }

    public static boolean isEmpty(RangerPolicyResource policyResource) {
        boolean ret = true;
        if (policyResource != null) {
            List<String> resourceValues = policyResource.getValues();
            if (CollectionUtils.isNotEmpty(resourceValues)) {
                for (String resourceValue : resourceValues) {
                    if (StringUtils.isNotBlank(resourceValue)) {
                        ret = false;
                        break;
                    }
                }
            }
        }
        return ret;
    }

    public static String getOption(Map<String, String> options, String name, String defaultValue) {
        String ret = options != null && name != null ? options.get(name) : null;

        if(ret == null) {
            ret = defaultValue;
        }

        return ret;
    }

    public static boolean getBooleanOption(Map<String, String> options, String name, boolean defaultValue) {
        String val = getOption(options, name, null);

        return val == null ? defaultValue : Boolean.parseBoolean(val);
    }

    public static char getCharOption(Map<String, String> options, String name, char defaultValue) {
        String val = getOption(options, name, null);

        return StringUtils.isEmpty(val) ? defaultValue : val.charAt(0);
    }

    public static RangerServiceDef normalizeAccessTypeDefs(RangerServiceDef serviceDef, final String componentType) {

        if (serviceDef != null && StringUtils.isNotBlank(componentType)) {

            List<RangerServiceDef.RangerAccessTypeDef> accessTypeDefs = serviceDef.getAccessTypes();

            if (CollectionUtils.isNotEmpty(accessTypeDefs)) {

                String prefix = componentType + AbstractServiceStore.COMPONENT_ACCESSTYPE_SEPARATOR;

                List<RangerServiceDef.RangerAccessTypeDef> unneededAccessTypeDefs = null;

                for (RangerServiceDef.RangerAccessTypeDef accessTypeDef : accessTypeDefs) {

                    String accessType = accessTypeDef.getName();

                    if (StringUtils.startsWith(accessType, prefix)) {

                        String newAccessType = StringUtils.removeStart(accessType, prefix);

                        accessTypeDef.setName(newAccessType);

                        Collection<String> impliedGrants = accessTypeDef.getImpliedGrants();

                        if (CollectionUtils.isNotEmpty(impliedGrants)) {

                            Collection<String> newImpliedGrants = null;

                            for (String impliedGrant : impliedGrants) {

                                if (StringUtils.startsWith(impliedGrant, prefix)) {

                                    String newImpliedGrant = StringUtils.removeStart(impliedGrant, prefix);

                                    if (newImpliedGrants == null) {
                                        newImpliedGrants = new ArrayList<>();
                                    }

                                    newImpliedGrants.add(newImpliedGrant);
                                }
                            }
                            accessTypeDef.setImpliedGrants(newImpliedGrants);

                        }
                    } else if (StringUtils.contains(accessType, AbstractServiceStore.COMPONENT_ACCESSTYPE_SEPARATOR)) {
                        if(unneededAccessTypeDefs == null) {
                            unneededAccessTypeDefs = new ArrayList<>();
                        }

                        unneededAccessTypeDefs.add(accessTypeDef);
                    }
                }

                if(unneededAccessTypeDefs != null) {
                    accessTypeDefs.removeAll(unneededAccessTypeDefs);
                }
            }
        }

        return serviceDef;
    }

    private static void normalizeDataMaskDef(RangerServiceDef serviceDef) {
        if(serviceDef != null && serviceDef.getDataMaskDef() != null) {
            List<RangerResourceDef>   dataMaskResources   = serviceDef.getDataMaskDef().getResources();
            List<RangerAccessTypeDef> dataMaskAccessTypes = serviceDef.getDataMaskDef().getAccessTypes();

            if(CollectionUtils.isNotEmpty(dataMaskResources)) {
                List<RangerResourceDef> resources     = serviceDef.getResources();
                List<RangerResourceDef> processedDefs = new ArrayList<RangerResourceDef>(dataMaskResources.size());

                for(RangerResourceDef dataMaskResource : dataMaskResources) {
                    RangerResourceDef processedDef = dataMaskResource;

                    for(RangerResourceDef resourceDef : resources) {
                        if(StringUtils.equals(resourceDef.getName(), dataMaskResource.getName())) {
                            processedDef = ServiceDefUtil.mergeResourceDef(resourceDef, dataMaskResource);
                            break;
                        }
                    }

                    processedDefs.add(processedDef);
                }

                serviceDef.getDataMaskDef().setResources(processedDefs);
            }

            if(CollectionUtils.isNotEmpty(dataMaskAccessTypes)) {
                List<RangerAccessTypeDef> accessTypes   = serviceDef.getAccessTypes();
                List<RangerAccessTypeDef> processedDefs = new ArrayList<RangerAccessTypeDef>(accessTypes.size());

                for(RangerAccessTypeDef dataMaskAccessType : dataMaskAccessTypes) {
                    RangerAccessTypeDef processedDef = dataMaskAccessType;

                    for(RangerAccessTypeDef accessType : accessTypes) {
                        if(StringUtils.equals(accessType.getName(), dataMaskAccessType.getName())) {
                            processedDef = ServiceDefUtil.mergeAccessTypeDef(accessType, dataMaskAccessType);
                            break;
                        }
                    }

                    processedDefs.add(processedDef);
                }

                serviceDef.getDataMaskDef().setAccessTypes(processedDefs);
            }
        }
    }

    private static void normalizeRowFilterDef(RangerServiceDef serviceDef) {
        if(serviceDef != null && serviceDef.getRowFilterDef() != null) {
            List<RangerResourceDef>   rowFilterResources   = serviceDef.getRowFilterDef().getResources();
            List<RangerAccessTypeDef> rowFilterAccessTypes = serviceDef.getRowFilterDef().getAccessTypes();

            if(CollectionUtils.isNotEmpty(rowFilterResources)) {
                List<RangerResourceDef> resources     = serviceDef.getResources();
                List<RangerResourceDef> processedDefs = new ArrayList<RangerResourceDef>(rowFilterResources.size());

                for(RangerResourceDef rowFilterResource : rowFilterResources) {
                    RangerResourceDef processedDef = rowFilterResource;

                    for(RangerResourceDef resourceDef : resources) {
                        if(StringUtils.equals(resourceDef.getName(), rowFilterResource.getName())) {
                            processedDef = ServiceDefUtil.mergeResourceDef(resourceDef, rowFilterResource);
                            break;
                        }
                    }

                    processedDefs.add(processedDef);
                }

                serviceDef.getRowFilterDef().setResources(processedDefs);
            }

            if(CollectionUtils.isNotEmpty(rowFilterAccessTypes)) {
                List<RangerAccessTypeDef> accessTypes   = serviceDef.getAccessTypes();
                List<RangerAccessTypeDef> processedDefs = new ArrayList<RangerAccessTypeDef>(accessTypes.size());

                for(RangerAccessTypeDef rowFilterAccessType : rowFilterAccessTypes) {
                    RangerAccessTypeDef processedDef = rowFilterAccessType;

                    for(RangerAccessTypeDef accessType : accessTypes) {
                        if(StringUtils.equals(accessType.getName(), rowFilterAccessType.getName())) {
                            processedDef = ServiceDefUtil.mergeAccessTypeDef(accessType, rowFilterAccessType);
                            break;
                        }
                    }

                    processedDefs.add(processedDef);
                }

                serviceDef.getRowFilterDef().setAccessTypes(processedDefs);
            }
        }
    }

    private static RangerResourceDef mergeResourceDef(RangerResourceDef base, RangerResourceDef delta) {
        RangerResourceDef ret = new RangerResourceDef(base);

        // retain base values for: itemId, name, type, level, parent, lookupSupported

        if(Boolean.TRUE.equals(delta.getMandatory()))
            ret.setMandatory(delta.getMandatory());

        if(delta.getRecursiveSupported() != null)
            ret.setRecursiveSupported(delta.getRecursiveSupported());

        if(delta.getExcludesSupported() != null)
            ret.setExcludesSupported(delta.getExcludesSupported());

        if(StringUtils.isNotEmpty(delta.getMatcher()))
            ret.setMatcher(delta.getMatcher());

        if(MapUtils.isNotEmpty(delta.getMatcherOptions())) {
            if(ret.getMatcherOptions() == null) {
                ret.setMatcherOptions(new HashMap<String, String>());
            }

            for(Map.Entry<String, String> e : delta.getMatcherOptions().entrySet()) {
                ret.getMatcherOptions().put(e.getKey(), e.getValue());
            }
        }

        if(StringUtils.isNotEmpty(delta.getValidationRegEx()))
            ret.setValidationRegEx(delta.getValidationRegEx());

        if(StringUtils.isNotEmpty(delta.getValidationMessage()))
            ret.setValidationMessage(delta.getValidationMessage());

        ret.setUiHint(delta.getUiHint());

        if(StringUtils.isNotEmpty(delta.getLabel()))
            ret.setLabel(delta.getLabel());

        if(StringUtils.isNotEmpty(delta.getDescription()))
            ret.setDescription(delta.getDescription());

        if(StringUtils.isNotEmpty(delta.getRbKeyLabel()))
            ret.setRbKeyLabel(delta.getRbKeyLabel());

        if(StringUtils.isNotEmpty(delta.getRbKeyDescription()))
            ret.setRbKeyDescription(delta.getRbKeyDescription());

        if(StringUtils.isNotEmpty(delta.getRbKeyValidationMessage()))
            ret.setRbKeyValidationMessage(delta.getRbKeyValidationMessage());

        if(CollectionUtils.isNotEmpty(delta.getAccessTypeRestrictions()))
            ret.setAccessTypeRestrictions(delta.getAccessTypeRestrictions());

        boolean copyLeafValue = false;
        if (ret.getIsValidLeaf() != null) {
            if (!ret.getIsValidLeaf().equals(delta.getIsValidLeaf())) {
                copyLeafValue = true;
            }
        } else {
            if (delta.getIsValidLeaf() != null) {
                copyLeafValue = true;
            }
        }
        if (copyLeafValue)
            ret.setIsValidLeaf(delta.getIsValidLeaf());

        return ret;
    }

    private static RangerAccessTypeDef mergeAccessTypeDef(RangerAccessTypeDef base, RangerAccessTypeDef delta) {
        RangerAccessTypeDef ret = new RangerAccessTypeDef(base);

        // retain base values for: itemId, name, impliedGrants

        if(StringUtils.isNotEmpty(delta.getLabel()))
            ret.setLabel(delta.getLabel());

        if(StringUtils.isNotEmpty(delta.getRbKeyLabel()))
            ret.setRbKeyLabel(delta.getRbKeyLabel());

        return ret;
    }

    private static boolean getBooleanValue(Map<String, String> map, String elementName, boolean defaultValue) {
        boolean ret = defaultValue;

        if(MapUtils.isNotEmpty(map) && map.containsKey(elementName)) {
            String elementValue = map.get(elementName);

            if(StringUtils.isNotEmpty(elementValue)) {
                ret = Boolean.valueOf(elementValue.toString());
            }
        }

        return ret;
    }

    public static Map<String, Collection<String>> getExpandedImpliedGrants(RangerServiceDef serviceDef) {
        Map<String, Collection<String>> ret = new HashMap<>();

        if(serviceDef != null && !CollectionUtils.isEmpty(serviceDef.getAccessTypes())) {
            for(RangerAccessTypeDef accessTypeDef : serviceDef.getAccessTypes()) {
                if(!CollectionUtils.isEmpty(accessTypeDef.getImpliedGrants())) {

                    Collection<String> impliedAccessGrants = ret.get(accessTypeDef.getName());

                    if(impliedAccessGrants == null) {
                        impliedAccessGrants = new HashSet<>();

                        ret.put(accessTypeDef.getName(), impliedAccessGrants);
                    }

                    impliedAccessGrants.addAll(accessTypeDef.getImpliedGrants());
                    impliedAccessGrants.add(accessTypeDef.getName());
                } else {
                    ret.put(accessTypeDef.getName(), new HashSet<>(Collections.singleton(accessTypeDef.getName())));
                }
            }
        }
        return ret;
    }

    public static boolean isUserStoreEnricherPresent(ServicePolicies policies) {
        boolean                        ret          = false;
        RangerServiceDef               serviceDef   = policies != null ? policies.getServiceDef() : null;
        List<RangerContextEnricherDef> enricherDefs = serviceDef != null ? serviceDef.getContextEnrichers() : null;

        if (enricherDefs != null) {
            for (RangerContextEnricherDef enricherDef : enricherDefs) {
                if (StringUtils.equals(enricherDef.getEnricher(), USER_STORE_ENRICHER)) {
                    ret = true;

                    break;
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("isUserStoreEnricherPresent(service={}): ret={}", policies.getServiceName(), ret);
        }

        return ret;
    }

    public static boolean addUserStoreEnricher(ServicePolicies policies, String retrieverClassName, String retrieverPollIntMs) {
        boolean          ret         = false;
        RangerServiceDef serviceDef = policies != null ? policies.getServiceDef() : null;

        if (serviceDef != null && !isUserStoreEnricherPresent(policies)) {
            List<RangerContextEnricherDef> enricherDefs = serviceDef.getContextEnrichers();

            if (enricherDefs == null) {
                enricherDefs = new ArrayList<>();
            }

            long enricherItemId = enricherDefs.size() + 1;

            for (RangerServiceDef.RangerContextEnricherDef enricherDef : enricherDefs) {
                if (enricherDef.getItemId() >= enricherItemId) {
                    enricherItemId = enricherDef.getItemId() + 1;
                }
            }

            Map<String, String> enricherOptions = new HashMap<>();

            enricherOptions.put(RangerUserStoreEnricher.USERSTORE_RETRIEVER_CLASSNAME_OPTION, retrieverClassName);
            enricherOptions.put(RangerUserStoreEnricher.USERSTORE_REFRESHER_POLLINGINTERVAL_OPTION, retrieverPollIntMs);

            RangerServiceDef.RangerContextEnricherDef userStoreEnricher = new RangerServiceDef.RangerContextEnricherDef(enricherItemId, "userStoreEnricher", USER_STORE_ENRICHER, enricherOptions);

            enricherDefs.add(userStoreEnricher);

            serviceDef.setContextEnrichers(enricherDefs);

            LOG.info("addUserStoreEnricher(serviceName={}): added userStoreEnricher {}", policies.getServiceName(), userStoreEnricher);
        }

        return ret;
    }


    public static boolean addUserStoreEnricherIfNeeded(ServicePolicies policies, String retrieverClassName, String retrieverPollIntMs) {
        boolean          ret        = false;
        RangerServiceDef serviceDef = policies != null ? policies.getServiceDef() : null;

        if (serviceDef != null && !isUserStoreEnricherPresent(policies)) {
            boolean addEnricher = anyPolicyHasUserGroupAttributeExpression(policies.getPolicies());

            if (!addEnricher) {
                List<RangerPolicy> tagPolicies = policies.getTagPolicies() != null ? policies.getTagPolicies().getPolicies() : null;

                addEnricher = anyPolicyHasUserGroupAttributeExpression(tagPolicies);
            }

            if (!addEnricher) {
                addEnricher = anyPolicyDeltaHasUserGroupAttributeExpression(policies.getPolicyDeltas());
            }

            if (!addEnricher) {
                Map<String, SecurityZoneInfo> zoneInfos = policies.getSecurityZones();

                if (zoneInfos != null) {
                    for (SecurityZoneInfo zoneInfo : zoneInfos.values()) {
                        addEnricher = anyPolicyHasUserGroupAttributeExpression(zoneInfo.getPolicies());

                        if (!addEnricher) {
                            addEnricher = anyPolicyDeltaHasUserGroupAttributeExpression(zoneInfo.getPolicyDeltas());
                        }

                        if (addEnricher) {
                            break;
                        }
                    }
                }
            }

            if (addEnricher) {
                addUserStoreEnricher(policies, retrieverClassName, retrieverPollIntMs);

                ret = true;
            }
        }

        return ret;
    }

    private static boolean anyPolicyHasUserGroupAttributeExpression(List<RangerPolicy> policies) {
        boolean ret = false;

        if (policies != null) {
            for (RangerPolicy policy : policies) {
                if (policyHasUserGroupAttributeExpression(policy)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("addUserStoreEnricherIfNeeded(service={}): policy(id={}, name={}) has reference to user/group attribute. Adding enricher", policy.getService(), policy.getId(), policy.getName());
                    }

                    ret = true;

                    break;
                }
            }
        }

        return ret;
    }

    private static boolean anyPolicyDeltaHasUserGroupAttributeExpression(List<RangerPolicyDelta> policyDeltas) {
        boolean ret = false;

        if (policyDeltas != null) {
            for (RangerPolicyDelta policyDelta : policyDeltas) {
                RangerPolicy policy = policyDelta.getPolicy();

                if (policyHasUserGroupAttributeExpression(policy)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("addUserStoreEnricherIfNeeded(service={}): policy(id={}, name={}) has reference to user/group attribute. Adding enricher", policy.getService(), policy.getId(), policy.getName());
                    }

                    ret = true;

                    break;
                }
            }
        }

        return ret;
    }

    private static boolean policyHasUserGroupAttributeExpression(RangerPolicy policy) {
        boolean ret = false;

        if (policy != null) {
            if (MapUtils.isNotEmpty(policy.getResources())) {
                for (RangerPolicyResource resource : policy.getResources().values()) {
                    ret = RangerRequestExprResolver.hasUserGroupAttributeInExpression(resource.getValues());

                    if (ret) {
                        break;
                    }
                }
            }

            if (!ret) {
                ret = anyPolicyConditionHasUserGroupAttributeReference(policy.getConditions());
            }

            if (!ret) {
                ret = anyPolicyItemHasUserGroupAttributeExpression(policy.getPolicyItems()) ||
                      anyPolicyItemHasUserGroupAttributeExpression(policy.getDenyPolicyItems()) ||
                      anyPolicyItemHasUserGroupAttributeExpression(policy.getAllowExceptions()) ||
                      anyPolicyItemHasUserGroupAttributeExpression(policy.getDenyExceptions()) ||
                      anyPolicyItemHasUserGroupAttributeExpression(policy.getDataMaskPolicyItems()) ||
                      anyPolicyItemHasUserGroupAttributeExpression(policy.getRowFilterPolicyItems());
            }
        }

        return ret;
    }

    private static boolean anyPolicyItemHasUserGroupAttributeExpression(List<? extends RangerPolicyItem> policyItems) {
        boolean ret = false;

        if (policyItems != null) {
            for (RangerPolicyItem policyItem : policyItems) {
                if (policyItemHasUserGroupAttributeExpression(policyItem)) {
                    ret = true;

                    break;
                }
            }
        }

        return ret;
    }

    private static boolean policyItemHasUserGroupAttributeExpression(RangerPolicyItem policyItem) {
        boolean ret = false;

        if (policyItem != null) {
            ret = anyPolicyConditionHasUserGroupAttributeReference(policyItem.getConditions());

            if (!ret && policyItem instanceof RangerRowFilterPolicyItem) {
                RangerRowFilterPolicyItem rowFilterPolicyItem = (RangerRowFilterPolicyItem) policyItem;
                RangerPolicyItemRowFilterInfo rowFilterInfo       = rowFilterPolicyItem.getRowFilterInfo();
                String                        filterExpr          = rowFilterInfo != null ? rowFilterInfo.getFilterExpr() : "";

                ret = RangerRequestExprResolver.hasUserGroupAttributeInExpression(filterExpr);
            }
        }

        return ret;
    }

    private static boolean anyPolicyConditionHasUserGroupAttributeReference(List<RangerPolicyItemCondition> conditions) {
        boolean ret = false;

        if (conditions != null) {
            for (RangerPolicyItemCondition condition : conditions) {
                if (RangerRequestScriptEvaluator.hasUserGroupAttributeReference(condition.getValues())) {
                    ret = true;

                    break;
                }
            }
        }

        return ret;
    }
}
