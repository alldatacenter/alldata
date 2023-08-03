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

package org.apache.ranger.ldapconfigcheck;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang.NullArgumentException;

public class LdapConfig {

    public static final String CONFIG_FILE = "input.properties";

    private static final String LGSYNC_LDAP_URL = "ranger.usersync.ldap.url";

    private static final String LGSYNC_LDAP_BIND_DN = "ranger.usersync.ldap.binddn";

    private static final String LGSYNC_LDAP_BIND_PASSWORD = "ranger.usersync.ldap.ldapbindpassword";

    private static final String LGSYNC_LDAP_AUTHENTICATION_MECHANISM = "ranger.usersync.ldap.authentication.mechanism";
    private static final String DEFAULT_AUTHENTICATION_MECHANISM = "simple";

    private static final String LGSYNC_SEARCH_BASE = "ranger.usersync.ldap.searchBase";

    private static final String LGSYNC_USER_SEARCH_BASE = "ranger.usersync.ldap.user.searchbase";

    private static final String LGSYNC_USER_SEARCH_SCOPE = "ranger.usersync.ldap.user.searchscope";

    private static final String LGSYNC_USER_OBJECT_CLASS = "ranger.usersync.ldap.user.objectclass";

    private static final String LGSYNC_USER_SEARCH_FILTER = "ranger.usersync.ldap.user.searchfilter";

    private static final String LGSYNC_USER_NAME_ATTRIBUTE = "ranger.usersync.ldap.user.nameattribute";

    private static final String LGSYNC_USER_GROUP_NAME_ATTRIBUTE = "ranger.usersync.ldap.user.groupnameattribute";

    public static final String UGSYNC_NONE_CASE_CONVERSION_VALUE = "none";
    public static final String UGSYNC_LOWER_CASE_CONVERSION_VALUE = "lower";

    private static final String UGSYNC_USERNAME_CASE_CONVERSION_PARAM = "ranger.usersync.ldap.username.caseconversion";
    private static final String DEFAULT_UGSYNC_USERNAME_CASE_CONVERSION_VALUE = UGSYNC_NONE_CASE_CONVERSION_VALUE;

    private static final String UGSYNC_GROUPNAME_CASE_CONVERSION_PARAM = "ranger.usersync.ldap.groupname.caseconversion";
    private static final String DEFAULT_UGSYNC_GROUPNAME_CASE_CONVERSION_VALUE = UGSYNC_NONE_CASE_CONVERSION_VALUE;


    private static final String LGSYNC_PAGED_RESULTS_ENABLED = "ranger.usersync.pagedresultsenabled";
    private static final boolean DEFAULT_LGSYNC_PAGED_RESULTS_ENABLED = true;

    private static final String LGSYNC_PAGED_RESULTS_SIZE = "ranger.usersync.pagedresultssize";
    private static final int DEFAULT_LGSYNC_PAGED_RESULTS_SIZE = 500;

    private static final String LGSYNC_GROUP_SEARCH_ENABLED = "ranger.usersync.group.searchenabled";
    private static final boolean DEFAULT_LGSYNC_GROUP_SEARCH_ENABLED = false;

    private static final String LGSYNC_GROUP_SEARCH_BASE = "ranger.usersync.group.searchbase";

    private static final String LGSYNC_GROUP_SEARCH_SCOPE = "ranger.usersync.group.searchscope";

    private static final String LGSYNC_GROUP_OBJECT_CLASS = "ranger.usersync.group.objectclass";

    private static final String LGSYNC_GROUP_SEARCH_FILTER = "ranger.usersync.group.searchfilter";

    private static final String LGSYNC_GROUP_NAME_ATTRIBUTE = "ranger.usersync.group.nameattribute";

    private static final String LGSYNC_GROUP_MEMBER_ATTRIBUTE_NAME = "ranger.usersync.group.memberattributename";

    //Authentication related properties
    private static final String AUTHENTICATION_METHOD = "ranger.authentication.method";
    private static final String AD_DOMAIN = "ranger.ldap.ad.domain";
    private static final String USER_DN_PATTERN = "ranger.ldap.user.dnpattern";
    private static final String GROUP_ROLE_ATTRIBUTE = "ranger.ldap.group.roleattribute";
    private static final String GROUP_SEARCH_BASE = "ranger.ldap.group.searchbase";
    private static final String GROUP_SEARCH_FILTER = "ranger.ldap.group.searchfilter";
    private static final String AUTH_USERNAME = "ranger.admin.auth.sampleuser";
    private static final String AUTH_PASSWORD = "ranger.admin.auth.samplepassword";


    private Properties prop = new Properties();


    public LdapConfig(String configFile, String bindPasswd) {
        init(configFile, bindPasswd);
    }

    private void init(String configFile, String bindPasswd) {
        readConfigFile(configFile);
        prop.setProperty(LGSYNC_LDAP_BIND_PASSWORD, bindPasswd);
    }

    private void readConfigFile(String fileName) {
        try {
            InputStream in = getFileInputStream(fileName);
            if (in != null) {
                try {
                    System.out.println("Reading ldap properties from " + fileName);
                    prop.load(in);

                } finally {
                    try {
                        in.close();
                    } catch (IOException ioe) {
                        // Ignore IOE when closing stream
                        System.out.println(ioe);
                    }
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException("Unable to load configuration file [" + fileName + "]", e);
        }
    }


    /*private InputStream getFileInputStream(String path) throws FileNotFoundException {

        InputStream ret = null;

        File f = new File(path);

        if (f.exists()) {
            ret = new FileInputStream(f);
        }

        return ret;
    }*/

    private InputStream getFileInputStream(String path) throws FileNotFoundException {

		InputStream ret = null;

		File f = new File(path);

		if (f.exists()) {
			ret = new FileInputStream(f);
		} else {
			ret = getClass().getResourceAsStream(path);
			
			if (ret == null) {
				if (! path.startsWith("/")) {
					ret = getClass().getResourceAsStream("/" + path);
				}
			}
			
			if (ret == null) {
				ret = ClassLoader.getSystemClassLoader().getResourceAsStream(path);
				if (ret == null) {
					if (! path.startsWith("/")) {
						ret = ClassLoader.getSystemResourceAsStream("/" + path);
					}
				}
			}
		}

		return ret;
	}

    public String getLdapUrl() throws Throwable {
        String val = prop.getProperty(LGSYNC_LDAP_URL);
        if (val == null || val.trim().isEmpty()) {
            throw new NullArgumentException(LGSYNC_LDAP_URL);
        }
        return val;
    }


    public String getLdapBindDn() throws Throwable {
        String val = prop.getProperty(LGSYNC_LDAP_BIND_DN);
        if (val == null || val.trim().isEmpty()) {
            throw new NullArgumentException(LGSYNC_LDAP_BIND_DN);
        }
        return val;
    }


    public String getLdapBindPassword() {
        //update credential from keystore
        if (prop == null) {
            return null;
        }
        return prop.getProperty(LGSYNC_LDAP_BIND_PASSWORD);
    }


    public String getLdapAuthenticationMechanism() {
        String val = prop.getProperty(LGSYNC_LDAP_AUTHENTICATION_MECHANISM);
        if (val == null || val.trim().isEmpty()) {
            return DEFAULT_AUTHENTICATION_MECHANISM;
        }
        return val;
    }


    public String getUserSearchBase() {
        String val = prop.getProperty(LGSYNC_USER_SEARCH_BASE);
        if (val == null || val.trim().isEmpty()) {
            val = getSearchBase();
        }
        return val;
    }


    public int getUserSearchScope() {
        String val = prop.getProperty(LGSYNC_USER_SEARCH_SCOPE);
        if (val == null || val.trim().isEmpty()) {
            return 2; //subtree scope
        }

        val = val.trim().toLowerCase();
        if (val.equals("0") || val.startsWith("base")) {
            return 0; // object scope
        } else if (val.equals("1") || val.startsWith("one")) {
            return 1; // one level scope
        } else {
            return 2; // subtree scope
        }
    }


    public String getUserObjectClass() {
        String val = prop.getProperty(LGSYNC_USER_OBJECT_CLASS);
        return val;
    }

    public String getUserSearchFilter() {
        return prop.getProperty(LGSYNC_USER_SEARCH_FILTER);
    }


    public String getUserNameAttribute() {
        String val = prop.getProperty(LGSYNC_USER_NAME_ATTRIBUTE);
        return val;
    }

    public String getUserGroupNameAttribute() {
        String val = prop.getProperty(LGSYNC_USER_GROUP_NAME_ATTRIBUTE);
        return val;
    }

    public String getUserNameCaseConversion() {
        String ret = prop.getProperty(UGSYNC_USERNAME_CASE_CONVERSION_PARAM, DEFAULT_UGSYNC_USERNAME_CASE_CONVERSION_VALUE);
        return ret.trim().toLowerCase();
    }

    public String getGroupNameCaseConversion() {
        String ret = prop.getProperty(UGSYNC_GROUPNAME_CASE_CONVERSION_PARAM, DEFAULT_UGSYNC_GROUPNAME_CASE_CONVERSION_VALUE);
        return ret.trim().toLowerCase();
    }

    public String getSearchBase() {
        return prop.getProperty(LGSYNC_SEARCH_BASE);
    }

    public boolean isPagedResultsEnabled() {
        boolean pagedResultsEnabled;
        String val = prop.getProperty(LGSYNC_PAGED_RESULTS_ENABLED);
        if (val == null || val.trim().isEmpty()) {
            pagedResultsEnabled = DEFAULT_LGSYNC_PAGED_RESULTS_ENABLED;
        } else {
            pagedResultsEnabled = Boolean.valueOf(val);
        }
        return pagedResultsEnabled;
    }

    public int getPagedResultsSize() {
        int pagedResultsSize;
        String val = prop.getProperty(LGSYNC_PAGED_RESULTS_SIZE);
        if (val == null || val.trim().isEmpty()) {
            pagedResultsSize = DEFAULT_LGSYNC_PAGED_RESULTS_SIZE;
        } else {
            pagedResultsSize = Integer.parseInt(val);
        }
        if (pagedResultsSize < 1) {
            pagedResultsSize = DEFAULT_LGSYNC_PAGED_RESULTS_SIZE;
        }
        return pagedResultsSize;
    }

    public boolean isGroupSearchEnabled() {
        boolean groupSearchEnabled;
        String val = prop.getProperty(LGSYNC_GROUP_SEARCH_ENABLED);
        if (val == null || val.trim().isEmpty()) {
            groupSearchEnabled = DEFAULT_LGSYNC_GROUP_SEARCH_ENABLED;
        } else {
            groupSearchEnabled = Boolean.valueOf(val);
        }
        return groupSearchEnabled;
    }

    public String getGroupSearchBase() {
        String val = prop.getProperty(LGSYNC_GROUP_SEARCH_BASE);
        return val;
    }

    public int getGroupSearchScope() {
        String val = prop.getProperty(LGSYNC_GROUP_SEARCH_SCOPE);
        if (val == null || val.trim().isEmpty()) {
            return 2; //subtree scope
        }

        val = val.trim().toLowerCase();
        if (val.equals("0") || val.startsWith("base")) {
            return 0; // object scope
        } else if (val.equals("1") || val.startsWith("one")) {
            return 1; // one level scope
        } else {
            return 2; // subtree scope
        }
    }

    public String getGroupObjectClass() {
        String val = prop.getProperty(LGSYNC_GROUP_OBJECT_CLASS);
        return val;
    }

    public String getGroupSearchFilter() {
        return prop.getProperty(LGSYNC_GROUP_SEARCH_FILTER);
    }

    public String getUserGroupMemberAttributeName() {
        String val = prop.getProperty(LGSYNC_GROUP_MEMBER_ATTRIBUTE_NAME);
        return val;
    }

    public String getGroupNameAttribute() {
        String val = prop.getProperty(LGSYNC_GROUP_NAME_ATTRIBUTE);
        return val;
    }

    public String getAuthenticationMethod() {
        String val = prop.getProperty(AUTHENTICATION_METHOD);
        return val;
    }

    public String getAdDomain() {
        String val = prop.getProperty(AD_DOMAIN);
        return val;
    }

    public String getUserDnPattern() {
        String val = prop.getProperty(USER_DN_PATTERN);
        return val;
    }

    public String getGroupRoleAttribute() {
        String val = prop.getProperty(GROUP_ROLE_ATTRIBUTE);
        return val;
    }

    public String getAuthGroupSearchBase() {
        String val = prop.getProperty(GROUP_SEARCH_BASE);
        return val;
    }

    public String getAuthGroupSearchFilter() {
        String val = prop.getProperty(GROUP_SEARCH_FILTER);
        return val;
    }

    public String getAuthUsername() {
        return prop.getProperty(AUTH_USERNAME);
    }

    public String getAuthPassword() {
        return prop.getProperty(AUTH_PASSWORD);
    }

    public void updateInputPropFile(String ldapUrl, String bindDn, String bindPassword,
                                    String userSearchBase, String userSearchFilter,
                                    String authUser, String authPass) {
        try {
            Parameters params = new Parameters();
            FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                .configure(params.fileBased().setFileName(CONFIG_FILE));
            FileBasedConfiguration config = builder.getConfiguration();
            // Update properties in memory and update the file as well
            prop.setProperty(LGSYNC_LDAP_URL, ldapUrl);
            prop.setProperty(LGSYNC_LDAP_BIND_DN, bindDn);
            prop.setProperty(LGSYNC_LDAP_BIND_PASSWORD, bindPassword);
            prop.setProperty(LGSYNC_USER_SEARCH_BASE, userSearchBase);
            prop.setProperty(LGSYNC_USER_SEARCH_FILTER, userSearchFilter);
            prop.setProperty(AUTH_USERNAME, authUser);
            prop.setProperty(AUTH_PASSWORD, authPass);
            config.setProperty(LGSYNC_LDAP_URL, ldapUrl);
            config.setProperty(LGSYNC_LDAP_BIND_DN, bindDn);
            //config.setProperty(LGSYNC_LDAP_BIND_PASSWORD, bindPassword);
            config.setProperty(LGSYNC_USER_SEARCH_BASE, userSearchBase);
            config.setProperty(LGSYNC_USER_SEARCH_FILTER, userSearchFilter);
            config.setProperty(AUTH_USERNAME, authUser);
            //config.setProperty(AUTH_PASSWORD, authPass);
            builder.save();
        } catch (ConfigurationException e) {
            System.out.println("Failed to update " + CONFIG_FILE + ": " + e);
        }
    }
}





