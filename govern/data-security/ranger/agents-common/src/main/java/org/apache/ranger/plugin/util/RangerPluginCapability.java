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

import org.apache.ranger.authorization.utils.JsonUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RangerPluginCapability {

    /*
    - tag-policies
	- allowExceptions/deny/denyExceptions
	- masking/row-filtering
	- Macros - like ${USER}
	- tag-based masking/row-filtering
	- audit mode support
	- service-def changes - isValidLeaf
	- validity periods
	- policy priority
	- security zones
	- policy-level conditions
	- deny AllElse policies
	- roles
	- role download timer
	- Audit-excluded-users
	- Chained plugins
	- Super-user permission
	- UserStore download
	- Audit-policies
	- User/group/tag attributes in policy
	- additional resources in policy
     */
    private final long pluginCapabilities;
    private static final String baseRangerCapabilities = computeBaseCapabilities();

    // Any new RANGER_PLUGIN_CAPABILITY needs to be added to the end of this enum. Existing enumerator order should *NOT* be changed //
    public enum RangerPluginFeature {
        RANGER_PLUGIN_CAPABILITY_TAG_POLICIES("Tag Policies"),
        RANGER_PLUGIN_CAPABILITY_MASKING_AND_ROW_FILTERING("Masking and Row-filtering"),
        RANGER_PLUGIN_CAPABILITY_MACROS("Macros"),
        RANGER_PLUGIN_CAPABILITY_AUDIT_MODE("Audit Mode"),
        RANGER_PLUGIN_CAPABILITY_RESOURCE_IS_VALID_LEAF("Support for leaf node"),
        RANGER_PLUGIN_CAPABILITY_VALIDITY_PERIOD("Validity Period"),
        RANGER_PLUGIN_CAPABILITY_POLICY_PRIORITY("Policy Priority"),
        RANGER_PLUGIN_CAPABILITY_SECURITY_ZONE("Security Zone"),
        RANGER_PLUGIN_CAPABILITY_POLICY_LEVEL_CONDITION("Policy-level Condition"),
        RANGER_PLUGIN_CAPABILITY_DENY_ALL_ELSE_POLICY("Deny-all-else Policy"),
        RANGER_PLUGIN_CAPABILITY_ROLE("Role"),
        RANGER_PLUGIN_CAPABILITY_ROLE_DOWNLOAD_TIMER("Role Timer"),
        RANGER_PLUGIN_CAPABILITY_AUDIT_EXCLUDED_USERS("Audit-Excluded Users"),
        RANGER_PLUGIN_CAPABILITY_CHAINED_PLUGINS("Chained Plugins"),
        RANGER_PLUGIN_CAPABILITY_SUPERUSER_PERMISSIONS("Super-user Permissions"),
        RANGER_PLUGIN_CAPABILITY_USERSTORE_DOWNLOAD("UserStore Download"),
        RANGER_PLUGIN_CAPABILITY_AUDIT_POLICY("Audit Policy"),
        RANGER_PLUGIN_CAPABILITY_UGT_ATTRIBUTES_IN_POLICY("User/group/tag attributes in policy"),
        RANGER_PLUGIN_CAPABILITY_ADDITIONAL_RESOURCES_IN_POLICY("additional resources in policy");

        private final String name;
        RangerPluginFeature(String name) {
            this.name = name;
        }
        String getName() { return name; }
    }


    public RangerPluginCapability() {

        long vector = 0L;
        for (RangerPluginFeature feature : RangerPluginFeature.values()) {
            vector += 1L << feature.ordinal();
        }
        pluginCapabilities = vector;
    }

    public RangerPluginCapability(long pluginCapabilities) {
        this.pluginCapabilities = pluginCapabilities;
    }

    public RangerPluginCapability(List<String> capabilities) {

        long vector = 0L;

        for (String capability : capabilities) {
            RangerPluginFeature feature;
            try {
                feature = RangerPluginFeature.valueOf(capability);
                vector += 1L << feature.ordinal();
            } catch (Exception e) {
                // Ignore
            }
        }
        this.pluginCapabilities = vector;
    }

    public long getPluginCapabilities() {
        return pluginCapabilities;
    }

    public List<String> compare(RangerPluginCapability other) {
        final List<String> ret;

        if (pluginCapabilities != other.pluginCapabilities) {

            long mismatchedCapabilitiesVector = this.pluginCapabilities ^ other.pluginCapabilities;

            List<String> missingFeatures = toStrings(mismatchedCapabilitiesVector);

            if (mismatchedCapabilitiesVector > (1L << (RangerPluginFeature.values().length))) {
                missingFeatures.add("unknown");
            }

            ret = missingFeatures;
        } else {
            ret = Collections.EMPTY_LIST;
        }

        return ret;
    }

    @Override
    public String toString() {
        List<String> capabilities =  toStrings(pluginCapabilities);
        return JsonUtils.objectToJson(capabilities);
    }

    public static String getBaseRangerCapabilities() {
        return baseRangerCapabilities;
    }

    private static List<String> toStrings(long vector) {

        List<String> ret = new ArrayList<>();

        for (RangerPluginFeature feature : RangerPluginFeature.values()) {
            long test = 1L << feature.ordinal();
            if ((test & vector) > 0) {
                ret.add(feature.name());
            }
        }

        return ret;
    }


    private static String computeBaseCapabilities() {
        List<String> baseCapabilities = Arrays.asList(RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_TAG_POLICIES.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_MASKING_AND_ROW_FILTERING.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_MACROS.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_AUDIT_MODE.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_RESOURCE_IS_VALID_LEAF.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_VALIDITY_PERIOD.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_POLICY_PRIORITY.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_SECURITY_ZONE.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_POLICY_LEVEL_CONDITION.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_DENY_ALL_ELSE_POLICY.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_ROLE.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_ROLE_DOWNLOAD_TIMER.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_AUDIT_EXCLUDED_USERS.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_CHAINED_PLUGINS.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_SUPERUSER_PERMISSIONS.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_USERSTORE_DOWNLOAD.getName()
                , RangerPluginFeature.RANGER_PLUGIN_CAPABILITY_AUDIT_POLICY.getName());

        return Long.toHexString(new RangerPluginCapability(baseCapabilities).getPluginCapabilities());
    }

}
