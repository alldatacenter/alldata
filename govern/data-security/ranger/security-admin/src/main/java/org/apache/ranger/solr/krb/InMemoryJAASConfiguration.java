/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.solr.krb;

import org.apache.commons.collections.MapUtils;
import org.apache.hadoop.security.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * InMemoryJAASConfiguration
 *
 * An utility class - which has a static method init to load all JAAS configuration from Application properties file (eg: kafka.properties) and
 * set it as part of the default lookup configuration for all JAAS configuration lookup.
 *
 * Example settings in application.properties:
 *
 * xasecure.audit.jaas.KafkaClient.loginModuleName = com.sun.security.auth.module.Krb5LoginModule
 * xasecure.audit.jaas.KafkaClient.loginModuleControlFlag = required
 * xasecure.audit.jaas.KafkaClient.option.useKeyTab = true
 * xasecure.audit.jaas.KafkaClient.option.storeKey = true
 * xasecure.audit.jaas.KafkaClient.option.serviceName = kafka
 * xasecure.audit.jaas.KafkaClient.option.keyTab = /etc/security/keytabs/kafka_client.keytab
 * xasecure.audit.jaas.KafkaClient.option.principal = kafka-client-1@EXAMPLE.COM

 * xasecure.audit.jaas.MyClient.0.loginModuleName = com.sun.security.auth.module.Krb5LoginModule
 * xasecure.audit.jaas.MyClient.0.loginModuleControlFlag = required
 * xasecure.audit.jaas.MyClient.0.option.useKeyTab = true
 * xasecure.audit.jaas.MyClient.0.option.storeKey = true
 * xasecure.audit.jaas.MyClient.0.option.serviceName = kafka
 * xasecure.audit.jaas.MyClient.0.option.keyTab = /etc/security/keytabs/kafka_client.keytab
 * xasecure.audit.jaas.MyClient.0.option.principal = kafka-client-1@EXAMPLE.COM
 *
 * xasecure.audit.jaas.MyClient.1.loginModuleName = com.sun.security.auth.module.Krb5LoginModule
 * xasecure.audit.jaas.MyClient.1.loginModuleControlFlag = optional
 * xasecure.audit.jaas.MyClient.1.option.useKeyTab = true
 * xasecure.audit.jaas.MyClient.1.option.storeKey = true
 * xasecure.audit.jaas.MyClient.1.option.serviceName = kafka
 * xasecure.audit.jaas.MyClient.1.option.keyTab = /etc/security/keytabs/kafka_client.keytab
 * xasecure.audit.jaas.MyClient.1.option.principal = kafka-client-1@EXAMPLE.COM

 * This will set the JAAS configuration - equivalent to the jaas.conf file entries:
 *  KafkaClient {
 *      com.sun.security.auth.module.Krb5LoginModule required
 *          useKeyTab=true
 *          storeKey=true
 *          serviceName=kafka
 *          keyTab="/etc/security/keytabs/kafka_client.keytab"
 *          principal="kafka-client-1@EXAMPLE.COM";
 *  };
 *  MyClient {
 *      com.sun.security.auth.module.Krb5LoginModule required
 *          useKeyTab=true
 *          storeKey=true
 *          serviceName=kafka keyTab="/etc/security/keytabs/kafka_client.keytab"
 *          principal="kafka-client-1@EXAMPLE.COM";
 *  };
 *  MyClient {
 *      com.sun.security.auth.module.Krb5LoginModule optional
 *          useKeyTab=true
 *          storeKey=true
 *          serviceName=kafka
 *          keyTab="/etc/security/keytabs/kafka_client.keytab"
 *          principal="kafka-client-1@EXAMPLE.COM";
 *  };
 *
 *  Here is the syntax for atlas.properties to add JAAS configuration:
 *
 *  The property name has to begin with   'xasecure.audit.jaas.' +  clientId (in case of Kafka client,
 *  it expects the clientId to be  KafkaClient).
 *  The following property must be there to specify the JAAS loginModule name
 *          'xasecure.audit.jaas.' +' +  clientId  + '.loginModuleName'
 *  The following optional property should be set to specify the loginModuleControlFlag
 *          'xasecure.audit.jaas.' +' + clientId + '.loginModuleControlFlag'
 *          Default value :  required ,  Possible values:  required, optional, sufficient, requisite
 *  Then you can add additional optional parameters as options for the configuration using the following
 *  syntax:
 *          'xasecure.audit.jaas.' +' + clientId + '.option.' + <optionName>  = <optionValue>
 *
 *  The current setup will lookup JAAS configration from the atlas-application.properties first, if not available,
 *  it will delegate to the original configuration
 *
 */

public final class InMemoryJAASConfiguration extends Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryJAASConfiguration.class);

    public static final String JAAS_CONFIG_PREFIX_PARAM                    = "xasecure.audit.jaas.";
    public static final String JAAS_CONFIG_LOGIN_MODULE_NAME_PARAM         = "loginModuleName";
    public static final String JAAS_CONFIG_LOGIN_MODULE_CONTROL_FLAG_PARAM = "loginModuleControlFlag";
    public static final String JAAS_CONFIG_LOGIN_OPTIONS_PREFIX            = "option";
    public static final String JAAS_PRINCIPAL_PROP                         = "principal";

    private final Configuration                            parent;
    private final Map<String, List<AppConfigurationEntry>> applicationConfigEntryMap = new HashMap<>();

    public static InMemoryJAASConfiguration init(String propFile) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> InMemoryJAASConfiguration.init( {} ) ", propFile);
        }

        InMemoryJAASConfiguration ret = null;
        InputStream               in  = null;

        try {
            Properties properties = new Properties();

            in = ClassLoader.getSystemResourceAsStream(propFile);

            if (in == null) {
                if (!propFile.startsWith("/")) {
                    in = ClassLoader.getSystemResourceAsStream("/" + propFile);
                }
                if (in == null) {
                    in = new FileInputStream(new File(propFile));
                }
            }

            properties.load(in);

            ret = init(properties);
        } catch (IOException e) {
            throw new Exception("Failed to load JAAS application properties", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== InMemoryJAASConfiguration.init( {} ) ", propFile);
        }

        return ret;
    }

    public static void init(org.apache.commons.configuration2.Configuration configuration) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> InMemoryJAASConfiguration.init()");
        }

        if (configuration != null && !configuration.isEmpty()) {
            Properties properties = new Properties();
            Iterator<String> iterator = configuration.getKeys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                properties.put(key, configuration.getProperty(key));
            }
            init(properties);
        } else {
            throw new Exception("Failed to load JAAS application properties: configuration NULL or empty!");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== InMemoryJAASConfiguration.init()");
        }
    }

    public static InMemoryJAASConfiguration init(Properties properties) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> InMemoryJAASConfiguration.init()");
        }

        InMemoryJAASConfiguration ret = null;

        if (properties != null && MapUtils.isNotEmpty(properties)) {
            ret = new InMemoryJAASConfiguration(properties);
        } else {
            throw new Exception("Failed to load JAAS application properties: properties NULL or empty!");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== InMemoryJAASConfiguration.init()");
        }

        return ret;
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> InMemoryJAASConfiguration.getAppConfigurationEntry( {} )", name);
        }

        AppConfigurationEntry[] ret = null;

        if (parent != null) {
            ret = parent.getAppConfigurationEntry(name);
        }

        if (ret == null || ret.length == 0) {
            List<AppConfigurationEntry> retList = applicationConfigEntryMap.get(name);

            if (retList != null && retList.size() > 0) {
                ret = retList.toArray(new AppConfigurationEntry[retList.size()]);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== InMemoryJAASConfiguration.getAppConfigurationEntry( {} ) : {}", name, toString(ret));
        }

        return ret;
    }

    private InMemoryJAASConfiguration(Properties prop) {
        parent = Configuration.getConfiguration();

        initialize(prop);
    }

    private void initialize(Properties properties) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> InMemoryJAASConfiguration.initialize()");
        }

        int                             prefixLen   = JAAS_CONFIG_PREFIX_PARAM.length();
        Map<String, SortedSet<Integer>> jaasClients = new HashMap<>();

        for(String key : properties.stringPropertyNames()) {
            if (key.startsWith(JAAS_CONFIG_PREFIX_PARAM)) {
                String          jaasKey    = key.substring(prefixLen);
                StringTokenizer tokenizer  = new StringTokenizer(jaasKey, ".");
                int             tokenCount = tokenizer.countTokens();

                if (tokenCount > 0) {
                    String             clientId  = tokenizer.nextToken();
                    SortedSet<Integer> indexList = jaasClients.get(clientId);

                    if (indexList == null) {
                        indexList = new TreeSet<>();

                        jaasClients.put(clientId, indexList);
                    }

                    String  indexStr      = tokenizer.nextToken();
                    int     indexId       = isNumeric(indexStr) ? Integer.parseInt(indexStr)  : -1;
                    Integer clientIdIndex = Integer.valueOf(indexId);

                    if (!indexList.contains(clientIdIndex)) {
                        indexList.add(clientIdIndex);
                    }
                }
            }
        }

        for(String jaasClient : jaasClients.keySet()) {
            for(Integer index :  jaasClients.get(jaasClient)) {
                String keyPrefix = JAAS_CONFIG_PREFIX_PARAM + jaasClient + ".";

                if (index > -1) {
                    keyPrefix = keyPrefix  + String.valueOf(index) + ".";
                }

                String keyParam        = keyPrefix + JAAS_CONFIG_LOGIN_MODULE_NAME_PARAM;
                String loginModuleName = properties.getProperty(keyParam);

                if (loginModuleName == null) {
                    LOG.error("Unable to add JAAS configuration for "
                            + "client [" + jaasClient + "] as it is missing param [" + keyParam + "]."
                            + " Skipping JAAS config for [" + jaasClient + "]");
                    continue;
                } else {
                    loginModuleName = loginModuleName.trim();
                }

                keyParam = keyPrefix + JAAS_CONFIG_LOGIN_MODULE_CONTROL_FLAG_PARAM;

                String controlFlag = properties.getProperty(keyParam);

                AppConfigurationEntry.LoginModuleControlFlag loginControlFlag = null;

                if (controlFlag != null) {
                    controlFlag = controlFlag.trim().toLowerCase();

                    if (controlFlag.equals("optional")) {
                        loginControlFlag = AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL;
                    } else if (controlFlag.equals("requisite")) {
                        loginControlFlag = AppConfigurationEntry.LoginModuleControlFlag.REQUISITE;
                    } else if (controlFlag.equals("sufficient")) {
                        loginControlFlag = AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
                    } else if (controlFlag.equals("required")) {
                        loginControlFlag = AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
                    } else {
                        String validValues = "optional|requisite|sufficient|required";
                        LOG.warn("Unknown JAAS configuration value for (" + keyParam
                                + ") = [" + controlFlag + "], valid value are [" + validValues
                                + "] using the default value, REQUIRED");
                        loginControlFlag = AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
                    }
                } else {
                    LOG.warn("Unable to find JAAS configuration ("
                            + keyParam + "); using the default value, REQUIRED");
                    loginControlFlag = AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
                }

                Map<String, String> options         = new HashMap<>();
                String              optionPrefix    = keyPrefix + JAAS_CONFIG_LOGIN_OPTIONS_PREFIX + ".";
                int                 optionPrefixLen = optionPrefix.length();

                for(String key : properties.stringPropertyNames()) {
                    if (key.startsWith(optionPrefix)) {
                        String optionKey = key.substring(optionPrefixLen);
                        String optionVal = properties.getProperty(key);

                        if (optionVal != null) {
                            optionVal = optionVal.trim();

                            try {
                                if (optionKey.equalsIgnoreCase(JAAS_PRINCIPAL_PROP)) {
                                    optionVal = SecurityUtil.getServerPrincipal(optionVal, (String) null);
                                }
                            } catch (IOException e) {
                                LOG.warn("Failed to build serverPrincipal. Using provided value:["
                                        + optionVal + "]");
                            }
                        }

                        options.put(optionKey, optionVal);
                    }
                }

                AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName, loginControlFlag, options);

                if (LOG.isDebugEnabled()) {
                    StringBuilder sb = new StringBuilder();

                    sb.append("Adding client: [").append(jaasClient).append("{").append(index).append("}]\n");
                    sb.append("\tloginModule: [").append(loginModuleName).append("]\n");
                    sb.append("\tcontrolFlag: [").append(loginControlFlag).append("]\n");

                    for (String key : options.keySet()) {
                        String val = options.get(key);

                        sb.append("\tOptions:  [").append(key).append("] => [").append(val).append("]\n");
                    }

                    LOG.debug(sb.toString());
                }

                List<AppConfigurationEntry> retList =  applicationConfigEntryMap.get(jaasClient);

                if (retList == null) {
                    retList = new ArrayList<>();

                    applicationConfigEntryMap.put(jaasClient, retList);
                }

                retList.add(entry);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== InMemoryJAASConfiguration.initialize()");
        }
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    private String toString(AppConfigurationEntry[] entries) {
        StringBuilder sb = new StringBuilder();

        sb.append('[');
        if (entries != null) {
            for (AppConfigurationEntry entry : entries) {
                sb.append("{ loginModuleName=").append(entry.getLoginModuleName())
                        .append(", controlFlag=").append(entry.getControlFlag())
                        .append(", options=").append(entry.getOptions())
                        .append("}");
            }
        }
        sb.append(']');

        return sb.toString();
    }
}
