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
package org.apache.atlas;

import org.apache.atlas.security.SecurityUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.Properties;
import static org.apache.atlas.security.SecurityProperties.HADOOP_SECURITY_CREDENTIAL_PROVIDER_PATH;

/**
 * Application properties used by Atlas.
 */
public final class ApplicationProperties extends PropertiesConfiguration {
    public static final String ATLAS_CONFIGURATION_DIRECTORY_PROPERTY = "atlas.conf";
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationProperties.class);

    public static final String  APPLICATION_PROPERTIES          = "atlas-application.properties";

    public static final String  GRAPHDB_BACKEND_CONF            = "atlas.graphdb.backend";
    public static final String  STORAGE_BACKEND_CONF            = "atlas.graph.storage.backend";
    public static final String  INDEX_BACKEND_CONF              = "atlas.graph.index.search.backend";
    public static final String  INDEX_MAP_NAME_CONF             = "atlas.graph.index.search.map-name";
    public static final String  SOLR_WAIT_SEARCHER_CONF         = "atlas.graph.index.search.solr.wait-searcher";
    public static final String  ELASTICSEARCH_INDEX_NAME_CONF   = "atlas.graph.index.search.elasticsearch.index-name";
    public static final String  INDEX_RECOVERY_CONF             = "atlas.index.recovery.enable";
    public static final String  ENABLE_FULLTEXT_SEARCH_CONF     = "atlas.search.fulltext.enable";
    public static final String  ENABLE_FREETEXT_SEARCH_CONF     = "atlas.search.freetext.enable";
    public static final String  ATLAS_RUN_MODE                  = "atlas.run.mode";
    public static final String  GRAPHBD_BACKEND_JANUS           = "janus";
    public static final String  DEFAULT_INDEX_NAME              = "janusgraph";
    public static final String  STORAGE_BACKEND_HBASE           = "hbase";
    public static final String  STORAGE_BACKEND_HBASE2          = "hbase2";
    public static final String  INDEX_BACKEND_SOLR              = "solr";
    public static final String  INDEX_BACKEND_ELASTICSEARCH     = "elasticsearch";
    public static final String  LDAP_TYPE                       =  "atlas.authentication.method.ldap.type";
    public static final String  LDAP                            =  "LDAP";
    public static final String  AD                              =  "AD";
    public static final String  LDAP_AD_BIND_PASSWORD           =  "atlas.authentication.method.ldap.ad.bind.password";
    public static final String  LDAP_BIND_PASSWORD              =  "atlas.authentication.method.ldap.bind.password";
    public static final String  MASK_LDAP_PASSWORD              =  "********";
    public static final String  DEFAULT_GRAPHDB_BACKEND         = GRAPHBD_BACKEND_JANUS;
    public static final boolean DEFAULT_SOLR_WAIT_SEARCHER      = false;
    public static final boolean DEFAULT_INDEX_MAP_NAME          = false;
    public static final boolean DEFAULT_INDEX_RECOVERY          = true;
    public static final AtlasRunMode DEFAULT_ATLAS_RUN_MODE     = AtlasRunMode.PROD;
    public static final String INDEX_SEARCH_MAX_RESULT_SET_SIZE = "atlas.graph.index.search.max-result-set-size";

    public static final SimpleEntry<String, String> DB_CACHE_CONF               = new SimpleEntry<>("atlas.graph.cache.db-cache", "true");
    public static final SimpleEntry<String, String> DB_CACHE_CLEAN_WAIT_CONF    = new SimpleEntry<>("atlas.graph.cache.db-cache-clean-wait", "20");
    public static final SimpleEntry<String, String> DB_CACHE_SIZE_CONF          = new SimpleEntry<>("atlas.graph.cache.db-cache-size", "0.5");
    public static final SimpleEntry<String, String> DB_TX_CACHE_SIZE_CONF       = new SimpleEntry<>("atlas.graph.cache.tx-cache-size", "15000");
    public static final SimpleEntry<String, String> DB_CACHE_TX_DIRTY_SIZE_CONF = new SimpleEntry<>("atlas.graph.cache.tx-dirty-size", "120");

    private static volatile Configuration instance = null;

    public enum AtlasRunMode {
        PROD,
        DEV
    }

    private ApplicationProperties(URL url) throws ConfigurationException {
        super(url);
    }

    public static void forceReload() {
        if (instance != null) {
            synchronized (ApplicationProperties.class) {
                if (instance != null) {
                    instance = null;
                }
            }
        }
    }

    public static Configuration get() throws AtlasException {
        if (instance == null) {
            synchronized (ApplicationProperties.class) {
                if (instance == null) {
                    set(get(APPLICATION_PROPERTIES));
                }
            }
        }

        return instance;
    }

    public static Configuration set(Configuration configuration) throws AtlasException {
        synchronized (ApplicationProperties.class) {
            instance = configuration;
        }
        return instance;
    }

    public static Configuration get(String fileName) throws AtlasException {
        String confLocation = System.getProperty(ATLAS_CONFIGURATION_DIRECTORY_PROPERTY);
        try {
            URL url = null;

            if (confLocation == null) {
                LOG.info("Looking for {} in classpath", fileName);

                url = ApplicationProperties.class.getClassLoader().getResource(fileName);

                if (url == null) {
                    LOG.info("Looking for /{} in classpath", fileName);

                    url = ApplicationProperties.class.getClassLoader().getResource("/" + fileName);
                }
            } else {
                url = new File(confLocation, fileName).toURI().toURL();
            }

            LOG.info("Loading {} from {}", fileName, url);

            ApplicationProperties appProperties = new ApplicationProperties(url);

            appProperties.setDefaults();

            setLdapPasswordFromKeystore(appProperties);

            Configuration configuration = appProperties.interpolatedConfiguration();

            logConfiguration(configuration);
            return configuration;
        } catch (Exception e) {
            throw new AtlasException("Failed to load application properties", e);
        }
    }

    private static void logConfiguration(Configuration configuration) {
        if (LOG.isDebugEnabled()) {
            Iterator<String> keys = configuration.getKeys();
            LOG.debug("Configuration loaded:");
            while (keys.hasNext()) {
                String key = keys.next();
                LOG.debug("{} = {}", key, configuration.getProperty(key));
            }
        }
    }

    public static Configuration getSubsetConfiguration(Configuration inConf, String prefix) {
        return inConf.subset(prefix);
    }

    public static Properties getSubsetAsProperties(Configuration inConf, String prefix) {
        Configuration subset = inConf.subset(prefix);
        Properties   ret     = ConfigurationConverter.getProperties(subset);

        return ret;
    }

    public static Class getClass(Configuration configuration, String propertyName, String defaultValue,
                                 Class assignableClass) throws AtlasException {
        try {
            String propertyValue = configuration.getString(propertyName, defaultValue);
            Class<?> clazz = Class.forName(propertyValue);
            if (assignableClass == null || assignableClass.isAssignableFrom(clazz)) {
                return clazz;
            } else {
                String message = "Class " + clazz.getName() + " specified in property " + propertyName
                        + " is not assignable to class " + assignableClass.getName();
                LOG.error(message);
                throw new AtlasException(message);
            }
        } catch (Exception e) {
            throw new AtlasException(e);
        }
    }

    public static Class getClass(String fullyQualifiedClassName, Class assignableClass) throws AtlasException {
        try {
            Class<?> clazz = Class.forName(fullyQualifiedClassName);
            if (assignableClass == null || assignableClass.isAssignableFrom(clazz)) {
                return clazz;
            } else {
                String message = "Class " + clazz.getName() + " is not assignable to class " + assignableClass.getName();
                LOG.error(message);
                throw new AtlasException(message);
            }
        } catch (Exception e) {
            throw new AtlasException(e);
        }
    }

    /**
     * Get the specified property as an {@link InputStream}.
     * If the property is not set, then the specified default filename
     * is searched for in the following locations, in order of precedence:
     * 1. Atlas configuration directory specified by the {@link #ATLAS_CONFIGURATION_DIRECTORY_PROPERTY} system property
     * 2. relative to the working directory if {@link #ATLAS_CONFIGURATION_DIRECTORY_PROPERTY} is not set
     * 3. as a classloader resource
     *
     * @param configuration
     * @param propertyName
     * @param defaultFileName name of file to use by default if specified property is not set in the configuration- if null,
     * an {@link AtlasException} is thrown if the property is not set
     * @return an {@link InputStream}
     * @throws AtlasException if no file was found or if there was an error loading the file
     */
    public static InputStream getFileAsInputStream(Configuration configuration, String propertyName, String defaultFileName) throws AtlasException {
        File   fileToLoad = null;
        String fileName   = configuration.getString(propertyName);

        if (fileName == null) {
            if (defaultFileName == null) {
                throw new AtlasException(propertyName + " property not set and no default value specified");
            }

            LOG.info("{} property not set; defaulting to {}", propertyName, defaultFileName);

            fileName = defaultFileName;

            String atlasConfDir = System.getProperty(ATLAS_CONFIGURATION_DIRECTORY_PROPERTY);

            if (atlasConfDir != null) {
                // Look for default filename in Atlas config directory
                fileToLoad = new File(atlasConfDir, fileName);
            } else {
                // Look for default filename under the working directory
                fileToLoad = new File(fileName);
            }
        } else {
            // Look for configured filename
            fileToLoad = new File(fileName);
        }

        InputStream inStr = null;

        if (fileToLoad.exists()) {
            try {
                LOG.info("Loading file {} from {}", fileName, fileToLoad.getPath());

                inStr = new FileInputStream(fileToLoad);
            } catch (FileNotFoundException e) {
                throw new AtlasException("Error loading file " + fileName, e);
            }
        } else {
            // Look for file as class loader resource
            inStr = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);

            if (inStr == null) {
                String msg = fileName + " not found in file system or as class loader resource";

                LOG.error(msg);

                throw new AtlasException(msg);
            }

            LOG.info("Loaded {} as resource from {}", fileName, Thread.currentThread().getContextClassLoader().getResource(fileName).toString());
        }

        return inStr;
    }

    private static void setLdapPasswordFromKeystore(Configuration configuration) {
        String ldapType = configuration.getString(LDAP_TYPE);

        if (StringUtils.isNotEmpty(ldapType)) {
            try {
                if (ldapType.equalsIgnoreCase(LDAP)) {
                    String maskPasssword = configuration.getString(LDAP_BIND_PASSWORD);
                    if (MASK_LDAP_PASSWORD.equals(maskPasssword)) {
                        String password = SecurityUtil.getPassword(configuration, LDAP_BIND_PASSWORD, HADOOP_SECURITY_CREDENTIAL_PROVIDER_PATH);
                        configuration.clearProperty(LDAP_BIND_PASSWORD);
                        configuration.addProperty(LDAP_BIND_PASSWORD, password);
                    }
                } else if (ldapType.equalsIgnoreCase(AD)) {
                    String maskPasssword = configuration.getString(LDAP_AD_BIND_PASSWORD);
                    if (MASK_LDAP_PASSWORD.equals(maskPasssword)) {
                        String password = SecurityUtil.getPassword(configuration, LDAP_AD_BIND_PASSWORD, HADOOP_SECURITY_CREDENTIAL_PROVIDER_PATH);
                        configuration.clearProperty(LDAP_AD_BIND_PASSWORD);
                        configuration.addProperty(LDAP_AD_BIND_PASSWORD, password);
                    }
                }
            } catch (Exception e) {
                LOG.error("Error in getting secure password ", e);
            }
        }
    }

    private void setDefaults() {
        AtlasRunMode runMode = AtlasRunMode.valueOf(getString(ATLAS_RUN_MODE, DEFAULT_ATLAS_RUN_MODE.name()));

        // setting value for 'atlas.graphdb.backend' (default = 'janus')
        String graphDbBackend = getString(GRAPHDB_BACKEND_CONF);

        if (StringUtils.isEmpty(graphDbBackend)) {
            graphDbBackend = DEFAULT_GRAPHDB_BACKEND;
        }

        clearPropertyDirect(GRAPHDB_BACKEND_CONF);
        addPropertyDirect(GRAPHDB_BACKEND_CONF, graphDbBackend);
        LOG.info("Using graphdb backend '" + graphDbBackend + "'");

        // setting value for 'atlas.graph.storage.backend' (default = 'hbase2')
        String storageBackend = getString(STORAGE_BACKEND_CONF);

        if (StringUtils.isEmpty(storageBackend) || storageBackend.equalsIgnoreCase(STORAGE_BACKEND_HBASE)) {
            storageBackend = STORAGE_BACKEND_HBASE2;
        }

        clearPropertyDirect(STORAGE_BACKEND_CONF);
        addPropertyDirect(STORAGE_BACKEND_CONF, storageBackend);
        LOG.info("Using storage backend '" + storageBackend + "'");

        // setting value for 'atlas.graph.index.search.backend' (default = 'solr')
        String indexBackend = getString(INDEX_BACKEND_CONF);

        if (StringUtils.isEmpty(indexBackend)) {
            indexBackend = INDEX_BACKEND_SOLR;
        }

        clearPropertyDirect(INDEX_BACKEND_CONF);
        addPropertyDirect(INDEX_BACKEND_CONF, indexBackend);
        LOG.info("Using index backend '" + indexBackend + "'");

        // set the following if indexing backend is 'solr'
        if (indexBackend.equalsIgnoreCase(INDEX_BACKEND_SOLR)) {
            LOG.info("Atlas is running in MODE: {}.", runMode.name());

            if (runMode == AtlasRunMode.PROD) {
                if (!containsKey(SOLR_WAIT_SEARCHER_CONF)) {
                    addPropertyDirect(SOLR_WAIT_SEARCHER_CONF, DEFAULT_SOLR_WAIT_SEARCHER);
                }

                LOG.info("Setting solr.wait-searcher property '" + getBoolean(SOLR_WAIT_SEARCHER_CONF) + "'");

                clearPropertyDirect(INDEX_MAP_NAME_CONF);
                addPropertyDirect(INDEX_MAP_NAME_CONF, DEFAULT_INDEX_MAP_NAME);
                LOG.info("Setting index.search.map-name property '" + DEFAULT_INDEX_MAP_NAME + "'");
            }
        } else if (indexBackend.equalsIgnoreCase(INDEX_BACKEND_ELASTICSEARCH)){
           addPropertyDirect(ELASTICSEARCH_INDEX_NAME_CONF, DEFAULT_INDEX_NAME);

           LOG.info("Setting elasticsearch.index-name property '" + DEFAULT_INDEX_NAME + "'");
        }

        // setting value for 'atlas.graph.index.search.max-result-set-size' (default = 500000)
        int indexMaxResultSetSize = getInt(INDEX_SEARCH_MAX_RESULT_SET_SIZE, 500000);

        clearPropertyDirect(INDEX_SEARCH_MAX_RESULT_SET_SIZE);
        addPropertyDirect(INDEX_SEARCH_MAX_RESULT_SET_SIZE, indexMaxResultSetSize);

        LOG.info("Setting " + INDEX_SEARCH_MAX_RESULT_SET_SIZE + " = " + indexMaxResultSetSize);
        LOG.info("Setting " + SOLR_WAIT_SEARCHER_CONF + " = " + getBoolean(SOLR_WAIT_SEARCHER_CONF));

        setDbCacheConfDefaults();
    }

    void setDefault(SimpleEntry<String, String> keyValueDefault, String currentValue) {
        if (StringUtils.isNotEmpty(currentValue)) {
            return;
        }

        clearPropertyDirect(keyValueDefault.getKey());
        addPropertyDirect(keyValueDefault.getKey(), keyValueDefault.getValue());
        LOG.info("Property (set to default) {} = {}", keyValueDefault.getKey(), keyValueDefault.getValue());
    }

    private void setDbCacheConfDefaults() {
        SimpleEntry<String, String> keyValues[] = new SimpleEntry[]{ DB_CACHE_CONF, DB_CACHE_CLEAN_WAIT_CONF,
                                                                     DB_CACHE_SIZE_CONF, DB_TX_CACHE_SIZE_CONF,
                                                                     DB_CACHE_TX_DIRTY_SIZE_CONF };

        for(SimpleEntry<String, String> kv : keyValues) {
            String currentValue = getString(kv.getKey());

            setDefault(kv, currentValue);
        }
    }
}
