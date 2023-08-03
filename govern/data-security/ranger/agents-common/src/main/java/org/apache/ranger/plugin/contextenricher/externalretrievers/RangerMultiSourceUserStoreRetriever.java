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

package org.apache.ranger.plugin.contextenricher.externalretrievers;

import org.apache.ranger.admin.client.RangerAdminClient;
import org.apache.ranger.plugin.contextenricher.RangerUserStoreRetriever;
import org.apache.ranger.plugin.util.RangerRoles;
import org.apache.ranger.plugin.util.RangerRolesUtil;
import org.apache.ranger.plugin.util.RangerUserStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Options examples for individual retrievers:
//   "retriever0_api":  "attrName=partner,userStoreURL=http://localhost:8000/security/getPartnersByUser",
//   "retriever1_role": "attrName=employee"

public class RangerMultiSourceUserStoreRetriever extends RangerUserStoreRetriever {
    private static final Logger LOG = LoggerFactory.getLogger(RangerMultiSourceUserStoreRetriever.class);

    private static final Pattern PATTERN_ROLE_RETRIEVER_NAME = Pattern.compile("\\d+_role");

    private Map<String, Map<String,String>> retrieverOptions = Collections.emptyMap();
    private RangerAdminClient               adminClient      = null;
    private RangerUserStore                 userStore        = null;
    private RangerRolesUtil                 rolesUtil        = new RangerRolesUtil(new RangerRoles());

    // options come from service-def
    @Override
    public void init(Map<String, String> options) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> init(options={})", options);
        }

        try {
            retrieverOptions = toRetrieverOptions(options);

            if (hasAnyRoleRetriever()) {
                adminClient = pluginContext.createAdminClient(pluginConfig);
            }
        } catch (Exception e) {
            LOG.error("init() failed", e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== init(options={})", options);
        }
    }

    @Override
    public RangerUserStore retrieveUserStoreInfo(long lastKnownVersion, long lastActivationTimeInMillis) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("=> retrieveUserStoreInfo(lastKnownVersion={}, lastActivationTimeInMillis={})", lastKnownVersion, lastActivationTimeInMillis);
        }

        // if there are any role type retrievers, get rangerRoles; otherwise don't bother
        if (adminClient != null) {
            try {
                RangerRoles roles        = rolesUtil.getRoles();
                long        rolesVersion = roles.getRoleVersion() != null ? roles.getRoleVersion() : -1;
                RangerRoles updatedRoles = adminClient.getRolesIfUpdated(rolesVersion, lastActivationTimeInMillis);

                if (updatedRoles != null) {
                    rolesUtil = new RangerRolesUtil(updatedRoles);
                }
            } catch (Exception e) {
                LOG.error("retrieveUserStoreInfo(lastKnownVersion={}) failed to retrieve roles", lastKnownVersion, e);
            }
        }

        Map<String, Map<String, String>> userAttrs = null;

        try {
            userAttrs = retrieveAll();
        } catch (Exception e) {
            LOG.error("retrieveUserStoreInfo(lastKnownVersion={}) failed", lastKnownVersion, e);
        }

        if (userAttrs != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("retrieveUserStoreInfo(lastKnownVersion={}): user-attributes={}", lastKnownVersion, userAttrs);
            }

            userStore = new RangerUserStore();

            userStore.setUserStoreVersion(System.currentTimeMillis());
            userStore.setUserAttrMapping(userAttrs);
        } else {
            LOG.error("retrieveUserStoreInfo(lastKnownVersion={}): failed to retrieve user-attributes", lastKnownVersion);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== retrieveUserStoreInfo(lastKnownVersion={}, lastActivationTimeInMillis={}): ret={}", lastKnownVersion, lastActivationTimeInMillis, userStore);
        }

        return userStore;
    }

    private Map<String, Map<String,String>> toRetrieverOptions(Map<String, String> enricherOptions) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> toRetrieverOptions({})", enricherOptions);
        }

        Map<String, Map<String, String>> ret = new HashMap<>();

        for (Map.Entry<String, String> entry : enricherOptions.entrySet()) {
            String retrieverName = entry.getKey();

            if (retrieverName.startsWith("retriever")) {
                String retrieverOptions = entry.getValue();

                ret.put(retrieverName, toRetrieverOptions(retrieverName, retrieverOptions));
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== toRetrieverOptions({}): ret={}", enricherOptions, ret);
        }

        return ret;
    }

    // Managing options for various retrievals
    private Map<String, String> toRetrieverOptions(String name, String options) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> toRetrieverOptions(name={}, options={})", name, options);
        }

        Properties prop = new Properties();

        options = options.replaceAll("\\s", "");
        options = options.replaceAll(",", "\n");

        try {
            prop.load(new StringReader(options));
        } catch (Exception e) {
            LOG.error("toRetrieverOptions(name={}, options={}): failed to parse retriever options", name, options, e);

            throw new Exception(name + ": failed to parse retriever options: " + options, e);
        }

        Map<String, String> ret = new HashMap<>();

        for (String key : prop.stringPropertyNames()) {
            ret.put(key, prop.getProperty(key));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== toRetrieverOptions(name={}, options={}): ret={}", name, options, ret);
        }

        return ret;
    }

    private boolean hasAnyRoleRetriever() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> hasAnyRoleRetriever()");
        }

        boolean ret = false;

        for (String retrieverName : retrieverOptions.keySet()) {
            Matcher matcher = PATTERN_ROLE_RETRIEVER_NAME.matcher(retrieverName);

            if (matcher.find()) {
                ret = true;

                break;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== hasAnyRoleRetriever(): ret={}", ret);
        }

        return ret;
    }

    // top-level retrieval management
    private Map<String, Map<String, String>> retrieveAll() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> retrieveAll()");
        }

        Map<String, Map<String, String>> ret = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : retrieverOptions.entrySet()) {
            String                           name    = entry.getKey();
            Map<String, String>              options = entry.getValue();
            String                           source  = name.replaceAll("\\w+_","");
            Map<String, Map<String, String>> userAttrs;

            switch (source) {
                case "api":
                    userAttrs = retrieveUserAttributes(name, options);
                break;

                case "role":
                    userAttrs = retrieveUserAttrFromRoles(name, options);
                break;

                default:
                    throw new Exception("unrecognized retriever source '" + source + "'. Valid values: api, role");
            }

            mergeUserAttributes(userAttrs, ret);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== retrieveAll(): ret={}", ret);
        }

        return ret;
    }

    // external retrieval
    private Map<String, Map<String, String>> retrieveUserAttributes(String retrieverName, Map<String, String> options) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> retrieveUserAttributes(name={}, options={})", retrieverName, options);
        }

        String attrName = options.get("attrName");
        String url      = options.get("userStoreURL");
        String dataFile = options.get("dataFile");

        if (attrName == null) {
            throw new Exception(retrieverName + ": attrName must be specified in retriever options");
        }

        if (url == null && dataFile == null) {
            throw new Exception(retrieverName + ": url or dataFile must be specified in retriever options");
        }

        Map<String, Map<String, String>> ret;

        if (url != null) {
            GetFromURL gu = new GetFromURL();

            String configFile = options.getOrDefault("configFile", "/var/ranger/security/" + attrName + ".conf");

            if (LOG.isDebugEnabled()) {
                LOG.debug("{}: configFile={}", retrieverName, configFile);
            }

            ret = gu.getFromURL(url, configFile);  // get user-Attrs mapping in UserStore format from an API call

            if (LOG.isDebugEnabled()) {
                LOG.debug("loaded attribute {} from URL {}: {}", attrName, url, ret);
            }
        } else {
            GetFromDataFile gf = new GetFromDataFile();

            ret = gf.getFromDataFile(dataFile, attrName);

            if (LOG.isDebugEnabled()) {
                LOG.debug("loaded attribute {} from file {}: {}", attrName, dataFile, ret);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== retrieveUserAttributes(name={}, options={}): ret={}", retrieverName, options, ret);
        }

        return ret;
    }

    // role-based retrieval
    /** retrieveSingleRoleUserAttrMapping:
     *
     * @param options includes the attribute name of interest, from which to create the UserStore attribute name
     *      and to identify the role of interest.
     * @return  In UserStore format, maps from user to attrName to attribute values
     *
     * rangerRoles: one object for each role; contains set of users who are members.  The important feature here
     *      *    is that it maps roles to users.  rolesUtil.getUserRoleMapping() returns the reverse:
     *      *    maps users to roles that they are members of. This is closer to the UserStore format.
     */
    public Map<String, Map<String, String>> retrieveUserAttrFromRoles(String retrieverName, Map<String, String> options) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> retrieveUserAttrFromRoles(name={}, options={})", retrieverName, options);
        }

        Map<String, Map<String, String>> ret         = new HashMap<>();
        Map<String, Set<String>>         userToRoles = rolesUtil.getUserRoleMapping();
        String                           attrName    = options.get("attrName");
        String                           rolePrefix  = attrName + ".";
        Pattern                          pattern     = Pattern.compile("^.*" + rolePrefix + ".*$");

        for (Map.Entry<String, Set<String>> entry : userToRoles.entrySet()) {
            String       user       = entry.getKey();
            Set<String>  roles      = entry.getValue();
            List<String> attrValues = new ArrayList<>();

            for (String role : roles) {
                Matcher matcher = pattern.matcher(role);

                if (matcher.find()) {
                    String value = matcher.group().replace(rolePrefix, "");

                    attrValues.add(value);
                }
            }

            if (!attrValues.isEmpty()) {
                Map<String, String> userAttrs = new HashMap<>();

                userAttrs.put(attrName, String.join(",", attrValues));

                ret.put(user, userAttrs);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== retrieveUserAttrFromRoles(name={}, options={}): ret={}", retrieverName, options, ret);
        }

        return ret;
    }

    private void mergeUserAttributes(Map<String, Map<String, String>> source, Map<String, Map<String, String>> dest) {
        if (dest.size() == 0) {
            dest.putAll(source);
        } else {
            for (Map.Entry<String, Map<String, String>> e : source.entrySet()) {
                String              userName  = e.getKey();
                Map<String, String> userAttrs = e.getValue();

                if (dest.containsKey(userName)) {
                    Map<String, String> existingAttrs = dest.get(userName);

                    existingAttrs.putAll(userAttrs);
                } else {
                    dest.put(userName, userAttrs);
                }
            }
        }
    }
}
