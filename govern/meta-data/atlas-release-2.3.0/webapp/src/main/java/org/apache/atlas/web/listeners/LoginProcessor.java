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
package org.apache.atlas.web.listeners;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.security.SecurityProperties;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A class capable of performing a simple or kerberos login.
 */
public class LoginProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(LoginProcessor.class);
    public static final String ATLAS_AUTHENTICATION_PREFIX = "atlas.authentication.";
    public static final String AUTHENTICATION_KERBEROS_METHOD = ATLAS_AUTHENTICATION_PREFIX + "method.kerberos";
    public static final String AUTHENTICATION_PRINCIPAL = ATLAS_AUTHENTICATION_PREFIX + "principal";
    public static final String AUTHENTICATION_KEYTAB = ATLAS_AUTHENTICATION_PREFIX + "keytab";

    /**
     * Perform a SIMPLE login based on established OS identity or a kerberos based login using the configured
     * principal and keytab (via atlas-application.properties).
     */
    public void login() {
        // first, let's see if we're running in a hadoop cluster and have the env configured
        boolean isHadoopCluster = isHadoopCluster();
        Configuration hadoopConfig = isHadoopCluster ? getHadoopConfiguration() : new Configuration(false);
        org.apache.commons.configuration.Configuration configuration = getApplicationConfiguration();
        if (!isHadoopCluster) {
            // need to read the configured authentication choice and create the UGI configuration
            setupHadoopConfiguration(hadoopConfig, configuration);
        }
        doServiceLogin(hadoopConfig, configuration);
    }

    protected void doServiceLogin(Configuration hadoopConfig,
            org.apache.commons.configuration.Configuration configuration) {
        UserGroupInformation.setConfiguration(hadoopConfig);

        UserGroupInformation ugi = null;
        UserGroupInformation.AuthenticationMethod authenticationMethod =
                SecurityUtil.getAuthenticationMethod(hadoopConfig);
        try {
            if (authenticationMethod == UserGroupInformation.AuthenticationMethod.SIMPLE) {
                UserGroupInformation.loginUserFromSubject(null);
            } else if (authenticationMethod == UserGroupInformation.AuthenticationMethod.KERBEROS) {
                String bindAddress = getHostname(configuration);
                UserGroupInformation.loginUserFromKeytab(
                        getServerPrincipal(configuration.getString(AUTHENTICATION_PRINCIPAL), bindAddress),
                        configuration.getString(AUTHENTICATION_KEYTAB));
            }
            LOG.info("Logged in user {}", UserGroupInformation.getLoginUser());
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to perform %s login.", authenticationMethod), e);
        }
    }

    private String getHostname(org.apache.commons.configuration.Configuration configuration) {
        String bindAddress = configuration.getString(SecurityProperties.BIND_ADDRESS);
        if (bindAddress == null) {
            LOG.info("No host name configured.  Defaulting to local host name.");
            try {
                bindAddress = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new IllegalStateException(e);
            }
        }
        return bindAddress;
    }

    protected void setupHadoopConfiguration(Configuration hadoopConfig, org.apache.commons.configuration.Configuration
            configuration) {
        String authMethod = "";
        String kerberosAuthNEnabled = configuration != null ? configuration.getString(AUTHENTICATION_KERBEROS_METHOD) : null;
        // getString may return null, and would like to log the nature of the default setting
        if (kerberosAuthNEnabled == null || kerberosAuthNEnabled.equalsIgnoreCase("false")) {
            LOG.info("No authentication method configured.  Defaulting to simple authentication");
            authMethod = "simple";
        } else if (kerberosAuthNEnabled.equalsIgnoreCase("true")) {
            authMethod = "kerberos";
        }
        SecurityUtil
                .setAuthenticationMethod(UserGroupInformation.AuthenticationMethod.valueOf(authMethod.toUpperCase()),
                        hadoopConfig);
    }

    /**
     * Return a server (service) principal.  The token "_HOST" in the principal will be replaced with the local host
     * name (e.g. dgi/_HOST will be changed to dgi/localHostName)
     * @param principal the input principal containing an option "_HOST" token
     * @return the service principal.
     * @throws IOException
     */
    private String getServerPrincipal(String principal, String host) throws IOException {
        return SecurityUtil.getServerPrincipal(principal, host);
    }

    /**
     * Returns a Hadoop configuration instance.
     * @return the configuration.
     */
    protected Configuration getHadoopConfiguration() {
        return new Configuration();
    }

    /**
     * Returns the metadata application configuration.
     * @return the metadata configuration.
     * @throws ConfigurationException
     */
    protected org.apache.commons.configuration.Configuration getApplicationConfiguration() {
        try {
            return ApplicationProperties.get();
        } catch (AtlasException e) {
            LOG.warn("Error reading application configuration", e);
        }
        return null;
    }

    /**
     * Uses a hadoop shell to discern whether a hadoop cluster is available/configured.
     * @return true if a hadoop cluster is detected.
     */
    protected boolean isHadoopCluster() {
        boolean isHadoopCluster = false;
        try {
            isHadoopCluster = Shell.getHadoopHome() != null;
        } catch (IOException e) {
            // ignore - false is default setting
        }
        return isHadoopCluster;
    }
}
