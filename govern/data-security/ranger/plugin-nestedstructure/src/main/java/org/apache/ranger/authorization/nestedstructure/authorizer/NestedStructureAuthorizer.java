/**
* Copyright 2022 Comcast Cable Communications Management, LLC
*
* Licensed under the Apache License, Version 2.0 (the ""License"");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an ""AS IS"" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or   implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* SPDX-License-Identifier: Apache-2.0
*/

package org.apache.ranger.authorization.nestedstructure.authorizer;

import org.apache.ranger.authorization.hadoop.config.RangerPluginConfig;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.plugin.util.RangerRoles;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.apache.ranger.plugin.util.ServiceTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class NestedStructureAuthorizer {
    static private final Logger logger = LoggerFactory.getLogger(NestedStructureAuthorizer.class);

    private static final String RANGER_CMT_SERVICETYPE = "nestedstructure";
    private static final String RANGER_CMT_APPID       = "nestedstructure";

    private static volatile NestedStructureAuthorizer instance;

    private final RangerBasePlugin plugin;

    private NestedStructureAuthorizer() {
        plugin = new RangerBasePlugin(RANGER_CMT_SERVICETYPE, RANGER_CMT_APPID);

        plugin.init();
    }

    // for testing purpose only
    public NestedStructureAuthorizer(ServicePolicies policies, ServiceTags tags, RangerRoles roles) {
        RangerPolicyEngineOptions options = new RangerPolicyEngineOptions();

        options.disablePolicyRefresher    = true;
        options.disableUserStoreRetriever = true;
        options.disableTagRetriever       = true;

        RangerPluginConfig pluginConfig = new RangerPluginConfig(RANGER_CMT_SERVICETYPE, policies.getServiceName(), RANGER_CMT_APPID, null, null, options);

        plugin = new RangerBasePlugin(pluginConfig, policies, tags, roles);
    }

    public static NestedStructureAuthorizer getInstance() {
        NestedStructureAuthorizer ret = NestedStructureAuthorizer.instance;

        if (ret == null) {
            synchronized (NestedStructureAuthorizer.class) {
                ret = NestedStructureAuthorizer.instance;

                if (ret == null) {
                    NestedStructureAuthorizer.instance = ret = new NestedStructureAuthorizer();
                }
            }
        }

        return ret;
    }

    /**
     *
     * @param schema atlas schema name
     * @param user atlas user name
     * @param userGroups atlas user groups
     * @param json the json of the record to be evaluated
     * @param accessType access type requested; must be included in NestedStructureAccessType.
     * @return if the user is authorized to access this data.  If there is no authorization, null is returned.
     * If there is partial authorization, a modified/masked json blob is returned
     */
    public AccessResult authorize(String schema, String user, Set<String> userGroups, String json, NestedStructureAccessType accessType) {
        AccessResult ret;

        NestedStructureAuditHandler auditHandler = new NestedStructureAuditHandler(plugin.getConfig());

        try {
            ret = privateAuthorize(schema, user, userGroups, json, accessType, auditHandler);
        } catch (Exception e) {
            logger.warn("exception during processing, user: " + user + "\n json: " + json, e);

            ret = new AccessResult(false, null).addError(e);
        } finally {
            auditHandler.flushAudit();
        }

        return ret;
    }

    private AccessResult privateAuthorize(String schema, String user, Set<String> userGroups, String json, NestedStructureAccessType accessType, NestedStructureAuditHandler auditHandler) {
        final AccessResult ret;

        if (!hasAccessToSchemaOrAnyField(schema, user, userGroups, accessType, auditHandler)) {
            ret = new AccessResult(false, null);
        } else if (!hasAccessToRecord(schema, user, userGroups, json, accessType, auditHandler)) {
            ret = new AccessResult(false, null);
        } else {
            boolean                accessDenied    = false;
            JsonManipulator        jsonManipulator = new JsonManipulator(json);
            List<FieldLevelAccess> fieldResults    = new ArrayList<>();

            //check each field individually - both if the user has access and if so, what masking is required
            for (String field : jsonManipulator.getFields()) {
                FieldLevelAccess fieldAccess =  hasFieldAccess(schema, user, userGroups, field, accessType, auditHandler);

                fieldResults.add(fieldAccess);

                if (!fieldAccess.hasAccess) {
                    accessDenied = true;

                    break;
                }
            }

            //the user must have access to all fields.
            // if the user doesn't have access to one of the fields return an empty/false AccessResult
            if (accessDenied) {
                ret = new AccessResult(false, null);
            } else {
                jsonManipulator.maskFields(fieldResults);

                ret = new AccessResult(true, jsonManipulator.getJsonString());
            }
        }

        return ret;
    }

    /**
     * Checks to see that the user has access to the specific field in this schema
     * @param schema atlas schema name
     * @param user atlas user name
     * @param userGroups atlas user groups
     * @param fld field name
     * @param accessType access type requested; must be included in NestedStructureAccessType.
     * @return a pojo describing access level and masking
     */
    private FieldLevelAccess hasFieldAccess(String schema, String user, Set<String> userGroups, String fld, NestedStructureAccessType accessType, NestedStructureAuditHandler auditHandler) {
        String atlasString = fld.replaceAll("\\.\\[\\*\\]\\.'", ".") //removes ".[*]."
                                .replaceAll("\\.\\*\\.", "."); //removes ".*."

        //   RangerAccessResource fldResource = new NestedStructure_Resource(Optional.of("json_object.cxt.cmt.product.vnull3"), Optional.of("partner"));
        NestedStructureResource resource = new NestedStructureResource(Optional.of(schema), Optional.of(atlasString));
        RangerAccessRequest     request  = new RangerAccessRequestImpl(resource, accessType.getValue(), user, userGroups, null);
        RangerAccessResult      result   = plugin.isAccessAllowed(request, auditHandler);

        if (result == null){
            throw new MaskingException("unable to determine access");
        }

        boolean          hasAccess = result.getIsAccessDetermined() && result.getIsAllowed();
        FieldLevelAccess ret;

        if (logger.isDebugEnabled()) {
            logger.debug("checking at line 123 " + accessType + " access to " + schema + "." + fld + " as " + atlasString + " for user: " + user +
                    " has access ? " + (hasAccess ? "yes" : "no") + " policyId:  " + result.getPolicyId());
        }

        if (!hasAccess) {
            ret = new FieldLevelAccess(fld, hasAccess, -1L, true, null, null);
        } else {
            RangerAccessResult maskResult = plugin.evalDataMaskPolicies(request, null);

            if (maskResult == null) {
                throw new MaskingException("unable to determine access");
            }

            boolean isMasked     = maskResult.isMaskEnabled();
            Long    maskPolicyId = maskResult.getPolicyId();

            // generate audit log for masking only when masking is enabled for the field
            if (isMasked) {
                auditHandler.processResult(maskResult);
            }

            if (logger.isDebugEnabled()) {
                String maskPolicy = isMasked ? (" policyId:  " + maskPolicyId) : "";

                logger.debug("attribute " + fld + " as " + atlasString + " masked ? " + (isMasked ? "yes" : "no") + maskPolicy);
            }

            ret = new FieldLevelAccess(fld, hasAccess, maskPolicyId, isMasked, maskResult.getMaskType(), maskResult.getMaskedValue());
        }

        return ret;
    }

    /**
     * record-level filtering of schema
     * note that while determining the filter to apply for a table, Apache Ranger policy engine evaluates
     * the policy-items in the order listed in the policy. The filter specified in the first policy-item
     * that matches the access-request (i.e. user/groups) will be used in the query.
     * @param schema atlas schema name
     * @param user atlas user name
     * @param userGroups atlas user groups
     * @param jsonString the json payload that needs to be evaluated
     * @param accessType access type requested; must be included in NestedStructureAccessType.
     * @return if the user is authorized to view this particular record
     */

    private boolean hasAccessToRecord(String schema, String user, Set<String> userGroups, String jsonString, NestedStructureAccessType accessType, NestedStructureAuditHandler auditHandler) {
        boolean                 ret      = true;
        NestedStructureResource resource = new NestedStructureResource(Optional.of(schema));
        RangerAccessRequest     request  = new RangerAccessRequestImpl(resource, accessType.getValue(), user, userGroups, null);
        RangerAccessResult      result   = plugin.evalRowFilterPolicies(request, null);

        if (result == null) {
            throw new MaskingException("unable to determine access");
        }

        if (result.isRowFilterEnabled()) {
            String filterExpr = result.getFilterExpr();

            if (logger.isDebugEnabled()) {
                logger.debug("row level filter enabled with expression: " + filterExpr);
            }

            ret = RecordFilterJavaScript.filterRow(user, filterExpr, jsonString);

            // generate audit log only when row-filter denies access to the record
            if (!ret) {
                result.setIsAllowed(false);

                auditHandler.processResult(result);
            }
        }

        return ret;
    }

    /**
     * Checks to see if this user has any access at all to this schema
     * @param schema atlas schema name
     * @param user atlas user name
     * @param accessType access type requested; must be included in NestedStructureAccessType.
     * @return if the user has access to this schema
     */
    private boolean hasAccessToSchemaOrAnyField(String schema, String user, Set<String> userGroups, NestedStructureAccessType accessType, NestedStructureAuditHandler auditHandler) {
        NestedStructureResource resource = new NestedStructureResource(Optional.of(schema));
        RangerAccessRequestImpl request  = new RangerAccessRequestImpl(resource, accessType.getValue(), user, userGroups, null);

        request.setResourceMatchingScope(RangerAccessRequest.ResourceMatchingScope.SELF_OR_DESCENDANTS);

        RangerAccessResult result = plugin.isAccessAllowed(request, null);

        if (result == null){
            throw new MaskingException("unable to determine access");
        }

        boolean ret = result.getIsAccessDetermined() && result.getIsAllowed();

        // generate audit log when the user doesn't have access to any field within the schema
        if (!ret) {
            auditHandler.processResult(result);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("checking LINE 202 " + accessType + " access to " + schema + " for user: " + user + " has access ? "
                    + (ret ? "yes" : "no") + " policyId:  " + result.getPolicyId());
        }

        return ret;
    }
}
