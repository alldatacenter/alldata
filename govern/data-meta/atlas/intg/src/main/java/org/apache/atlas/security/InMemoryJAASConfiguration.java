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

package org.apache.atlas.security;

import org.apache.atlas.AtlasException;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.security.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;


/**
 * InMemoryJAASConfiguration
 * <p>
 * An utility class - which has a static method init to load all JAAS configuration from Application
 * properties file (eg: atlas.properties) and set it as part of the default lookup configuration for
 * all JAAS configuration lookup.
 * <p>
 * Example settings in jaas-application.properties:
 *
 * <pre class=code>
 * atlas.jaas.KafkaClient.loginModuleName = com.sun.security.auth.module.Krb5LoginModule
 * atlas.jaas.KafkaClient.loginModuleControlFlag = required
 * atlas.jaas.KafkaClient.option.useKeyTab = true
 * atlas.jaas.KafkaClient.option.storeKey = true
 * atlas.jaas.KafkaClient.option.serviceName = kafka
 * atlas.jaas.KafkaClient.option.keyTab = /etc/security/keytabs/kafka_client.keytab
 * atlas.jaas.KafkaClient.option.principal = kafka-client-1@EXAMPLE.COM

 * atlas.jaas.MyClient.0.loginModuleName = com.sun.security.auth.module.Krb5LoginModule
 * atlas.jaas.MyClient.0.loginModuleControlFlag = required
 * atlas.jaas.MyClient.0.option.useKeyTab = true
 * atlas.jaas.MyClient.0.option.storeKey = true
 * atlas.jaas.MyClient.0.option.serviceName = kafka
 * atlas.jaas.MyClient.0.option.keyTab = /etc/security/keytabs/kafka_client.keytab
 * atlas.jaas.MyClient.0.option.principal = kafka-client-1@EXAMPLE.COM
 * atlas.jaas.MyClient.1.loginModuleName = com.sun.security.auth.module.Krb5LoginModule
 * atlas.jaas.MyClient.1.loginModuleControlFlag = optional
 * atlas.jaas.MyClient.1.option.useKeyTab = true
 * atlas.jaas.MyClient.1.option.storeKey = true
 * atlas.jaas.MyClient.1.option.serviceName = kafka
 * atlas.jaas.MyClient.1.option.keyTab = /etc/security/keytabs/kafka_client.keytab
 * atlas.jaas.MyClient.1.option.principal = kafka-client-1@EXAMPLE.COM </pre>
 *
 * <p>
 * This will set the JAAS configuration - equivalent to the jaas.conf file entries:
 *
 * <pre class=code>
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
 *  }; </pre>
 * <p>
 * Here is the syntax for atlas.properties to add JAAS configuration:
 * <p>
 * The property name has to begin with   'atlas.jaas.' +  clientId (in case of Kafka client,
 * it expects the clientId to be  KafkaClient).
 * <p>
 * The following property must be there to specify the JAAS loginModule name
 * <pre>         'atlas.jaas.' +  clientId  + '.loginModuleName' </pre>
 * <p>
 * The following optional property should be set to specify the loginModuleControlFlag
 * <pre>         'atlas.jaas.' + clientId + '.loginModuleControlFlag'
 *          Default value :  required ,  Possible values:  required, optional, sufficient, requisite </pre>
 * <p>
 * Then you can add additional optional parameters as options for the configuration using the following
 *  syntax:
 * <pre>         'atlas.jaas.' + clientId + '.option.' + <optionName>  = <optionValue> </pre>
 * <p>
 * The current setup will lookup JAAS configration from the atlas-application.properties first,
 * if not available, it will delegate to the original configuration
 *
 */


public final class InMemoryJAASConfiguration extends Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryJAASConfiguration.class);

    private static final String JAAS_CONFIG_PREFIX_PARAM = "atlas.jaas.";
    private static final String JAAS_CONFIG_LOGIN_MODULE_NAME_PARAM = "loginModuleName";
    private static final String JAAS_CONFIG_LOGIN_MODULE_CONTROL_FLAG_PARAM = "loginModuleControlFlag";
    private static final String JAAS_CONFIG_LOGIN_OPTIONS_PREFIX = "option";
    private static final String JAAS_PRINCIPAL_PROP = "principal";
    private static final Map<String, String> CONFIG_SECTION_REDIRECTS = new HashMap<>();

    private Configuration parent = null;
    private Map<String, List<AppConfigurationEntry>> applicationConfigEntryMap = new HashMap<>();

    public static void init(String propFile) throws AtlasException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> InMemoryJAASConfiguration.init({})", propFile);
        }

        InputStream in = null;

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
            init(properties);
        } catch (IOException e) {
            throw new AtlasException("Failed to load JAAS application properties", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception exception) {
                    // Ignore
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== InMemoryJAASConfiguration.init({})", propFile);
        }
    }

    public static void init(org.apache.commons.configuration.Configuration atlasConfiguration) throws AtlasException {
        LOG.debug("==> InMemoryJAASConfiguration.init()");

        if (atlasConfiguration != null && !atlasConfiguration.isEmpty()) {
            Properties properties = ConfigurationConverter.getProperties(atlasConfiguration);
            init(properties);
        } else {
            throw new AtlasException("Failed to load JAAS application properties: configuration NULL or empty!");
        }

        LOG.debug("<== InMemoryJAASConfiguration.init()");
    }

    public static void init(Properties properties) throws AtlasException {
        LOG.debug("==> InMemoryJAASConfiguration.init()");

        if (properties != null && MapUtils.isNotEmpty(properties)) {
            InMemoryJAASConfiguration conf = new InMemoryJAASConfiguration(properties);
            Configuration.setConfiguration(conf);
        } else {
            throw new AtlasException("Failed to load JAAS application properties: properties NULL or empty!");
        }

        LOG.debug("<== InMemoryJAASConfiguration.init()");
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> InMemoryJAASConfiguration.getAppConfigurationEntry({})", name);
        }

        AppConfigurationEntry[] ret = null;
        List<AppConfigurationEntry> retList = null;
        String redirectedName = getConfigSectionRedirect(name);

        if (redirectedName != null) {
            retList = applicationConfigEntryMap.get(redirectedName);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Redirected jaasConfigSection ({} -> {}): ", name, redirectedName, retList);
            }
        }

        if (retList == null || retList.size() == 0) {
            retList = applicationConfigEntryMap.get(name);
        }

        if (retList == null || retList.size() == 0) {
            if (parent != null) {
                ret = parent.getAppConfigurationEntry(name);
            }
        } else {
            int sz = retList.size();
            ret = new AppConfigurationEntry[sz];
            ret = retList.toArray(ret);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== InMemoryJAASConfiguration.getAppConfigurationEntry({}): {}", name, ArrayUtils.toString(ret));
        }

        return ret;
    }

    private InMemoryJAASConfiguration(Properties prop) {
        parent = Configuration.getConfiguration();
        initialize(prop);
    }

    private void initialize(Properties properties) {
        LOG.debug("==> InMemoryJAASConfiguration.initialize()");

        int prefixLen = JAAS_CONFIG_PREFIX_PARAM.length();

        Map<String, SortedSet<Integer>> jaasClients = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(JAAS_CONFIG_PREFIX_PARAM)) {
                String jaasKey = key.substring(prefixLen);
                StringTokenizer tokenizer = new StringTokenizer(jaasKey, ".");
                int tokenCount = tokenizer.countTokens();
                if (tokenCount > 0) {
                    String clientId = tokenizer.nextToken();
                    SortedSet<Integer> indexList = jaasClients.get(clientId);
                    if (indexList == null) {
                        indexList = new TreeSet<>();
                        jaasClients.put(clientId, indexList);
                    }
                    String indexStr = tokenizer.nextToken();

                    int indexId = isNumeric(indexStr) ? Integer.parseInt(indexStr) : -1;

                    Integer clientIdIndex = Integer.valueOf(indexId);

                    if (!indexList.contains(clientIdIndex)) {
                        indexList.add(clientIdIndex);
                    }

                }
            }
        }
        for (String jaasClient : jaasClients.keySet()) {

            for (Integer index : jaasClients.get(jaasClient)) {

                String keyPrefix = JAAS_CONFIG_PREFIX_PARAM + jaasClient + ".";

                if (index > -1) {
                    keyPrefix = keyPrefix + String.valueOf(index) + ".";
                }

                String keyParam = keyPrefix + JAAS_CONFIG_LOGIN_MODULE_NAME_PARAM;
                String loginModuleName = properties.getProperty(keyParam);

                if (loginModuleName == null) {
                    LOG.error("Unable to add JAAS configuration for client [{}] as it is missing param [{}]. Skipping JAAS config for [{}]", jaasClient, keyParam, jaasClient);
                    continue;
                } else {
                    loginModuleName = loginModuleName.trim();
                }

                keyParam = keyPrefix + JAAS_CONFIG_LOGIN_MODULE_CONTROL_FLAG_PARAM;
                String controlFlag = properties.getProperty(keyParam);

                AppConfigurationEntry.LoginModuleControlFlag loginControlFlag = null;
                if (controlFlag != null) {
                    controlFlag = controlFlag.trim().toLowerCase();
                    switch (controlFlag) {
                        case "optional":
                            loginControlFlag = AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL;
                            break;
                        case "requisite":
                            loginControlFlag = AppConfigurationEntry.LoginModuleControlFlag.REQUISITE;
                            break;
                        case "sufficient":
                            loginControlFlag = AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
                            break;
                        case "required":
                            loginControlFlag = AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
                            break;
                        default:
                            String validValues = "optional|requisite|sufficient|required";
                            LOG.warn("Unknown JAAS configuration value for ({}) = [{}], valid value are [{}] using the default value, REQUIRED", keyParam, controlFlag, validValues);
                            loginControlFlag = AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
                            break;
                    }
                } else {
                    LOG.warn("Unable to find JAAS configuration ({}); using the default value, REQUIRED", keyParam);
                    loginControlFlag = AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
                }


                Map<String, String> options = new HashMap<>();
                String optionPrefix = keyPrefix + JAAS_CONFIG_LOGIN_OPTIONS_PREFIX + ".";
                int optionPrefixLen = optionPrefix.length();
                for (String key : properties.stringPropertyNames()) {
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
                                LOG.warn("Failed to build serverPrincipal. Using provided value:[{}]", optionVal);
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

                List<AppConfigurationEntry> retList = applicationConfigEntryMap.get(jaasClient);
                if (retList == null) {
                    retList = new ArrayList<>();
                    applicationConfigEntryMap.put(jaasClient, retList);
                }

                retList.add(entry);
            }
        }

        LOG.debug("<== InMemoryJAASConfiguration.initialize({})", applicationConfigEntryMap);
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    public static void setConfigSectionRedirect(String name, String redirectTo) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("setConfigSectionRedirect({}, {})", name, redirectTo);
        }

        if (name != null) {
            if (redirectTo != null) {
                CONFIG_SECTION_REDIRECTS.put(name, redirectTo);
            } else {
                CONFIG_SECTION_REDIRECTS.remove(name);
            }
        }
    }

    private static String getConfigSectionRedirect(String name) {
        return name != null ? CONFIG_SECTION_REDIRECTS.get(name) : null;
    }
}
