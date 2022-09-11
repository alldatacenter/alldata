/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.common.fileconfig;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.inlong.tubemq.corebase.config.TLSConfig;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.broker.exception.StartupException;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFileConfig {
    protected static final String SECT_TOKEN_MASTER = "master";
    protected static final String SECT_TOKEN_BROKER = "broker";
    protected static final String SECT_TOKEN_BDB = "bdbStore";
    protected static final String SECT_TOKEN_TLS = "tlsSetting";
    protected static final String SECT_TOKEN_ZKEEPER = "zookeeper";
    protected static final String SECT_TOKEN_REPLICATION = "replication";
    protected static final String SECT_TOKEN_META_BDB = "meta_bdb";
    protected static final String SECT_TOKEN_META_ZK = "meta_zookeeper";

    private static final Logger logger =
            LoggerFactory.getLogger(AbstractFileConfig.class);
    private String basePath;
    private String configPath;
    private long loadFileChkSum = -1;
    private long loadFileModified = -1;

    public AbstractFileConfig() {
        super();
    }

    public String getBasePath() {
        return basePath;
    }

    public String getConfigPath() {
        return configPath;
    }

    public long getLoadFileChkSum() {
        return loadFileChkSum;
    }

    public long getLoadFileModified() {
        return loadFileModified;
    }

    /**
     * load configuration from the specific path.
     *
     * @param path the path of the configuration file
     */
    public void loadFromFile(final String path) {
        this.configPath = path;
        load();
    }

    /**
     * reload the configuration file.
     */
    public void reload() {
        load();
    }

    /**
     * Get integer configuration value from a specific section with key. It returns a default value if no value
     * is found in the section of the file.
     *
     * @param section      the section of the key/value comes from.
     * @param key          the key of the configuration
     * @param defaultValue the default value if no value is found in the specific section
     * @return the integer value of the specific key
     */
    public int getInt(final Profile.Section section, final String key, final int defaultValue) {
        final String value = section.get(key);
        if (TStringUtils.isBlank(value)) {
            return defaultValue;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Integer.parseInt(" + value + ") failure for key=" + key);
                return defaultValue;
            }
        }
    }

    /**
     * Get integer configuration value from a specific section in the config file.
     *
     * @param section the specific section where the configure locates
     * @param key     the key of the configuration
     * @return the integer value of the configuration, if no value is found, it throws NPE
     */
    public int getInt(final Profile.Section section, final String key) {
        final String value = section.get(key);
        if (TStringUtils.isBlank(value)) {
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append("Blank value for ").append(key).toString());
        } else {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(new StringBuilder(256)
                        .append("Translate key(").append(key).append(")'s value ")
                        .append(value).append(" to int failure!").toString());
            }
        }
    }

    /**
     * Get boolean configuration value from a specific section in the config file.
     *
     * @param section the specific section where the configure locates
     * @param key     the key of the configuration
     * @return the boolean value of the configuration, if no value is found, it throws NPE
     */
    public boolean getBoolean(final Profile.Section section, final String key) {
        final String value = section.get(key);
        if (TStringUtils.isBlank(value)) {
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append("Blank value for ").append(key).toString());
        } else {
            try {
                return Boolean.parseBoolean(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(new StringBuilder(256)
                        .append("Translate key(").append(key).append(")'s value ")
                        .append(value).append(" to boolean failure!").toString());
            }
        }
    }

    /**
     * Get long configuration value from a specific section in the config file.
     *
     * @param section the specific section where the configure locates
     * @param key     the key of the configuration
     * @return the long value of the configuration, if no value is found, it throws NPE
     */
    public long getLong(final Profile.Section section, final String key) {
        final String value = section.get(key);
        if (TStringUtils.isBlank(value)) {
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append("Blank value for ").append(key).toString());
        } else {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(new StringBuilder(256)
                        .append("Translate key(").append(key).append(")'s value ")
                        .append(value).append(" to long failure!").toString());
            }
        }
    }

    /**
     * Retrieve similar configurations within a set of specific configurations according to the given configuration.
     *
     * @param section      the section within file to be searched.
     * @param configFields the set of configurations to be searched.
     * @param checkItem    the given configuration to be compared.
     */

    public void getSimilarConfigField(String section, Set<String> configFields, String checkItem) {
        if (!configFields.contains(checkItem)) {
            String best = this.findBestMatchField(configFields, checkItem);
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append("Config item ").append(checkItem)
                    .append(" is Required! in section ").append(section)
                    .append("! (a similar item :").append(best)
                    .append(" found)").toString());
        }
    }

    protected TLSConfig loadTlsSectConf(final Ini iniConf, int defTlsPort) {
        TLSConfig tlsConfig = new TLSConfig();
        tlsConfig.setTlsPort(defTlsPort);
        final Profile.Section tlsSect = iniConf.get(SECT_TOKEN_TLS);
        if (tlsSect == null) {
            return tlsConfig;
        }
        Set<String> configKeySet = tlsSect.keySet();
        if (configKeySet.isEmpty()) {
            return tlsConfig;
        }
        if (TStringUtils.isNotBlank(tlsSect.get("tlsEnable"))) {
            tlsConfig.setTlsEnable(getBoolean(tlsSect, "tlsEnable"));
        }
        if (tlsConfig.isTlsEnable()) {
            tlsConfig.setTlsPort(getInt(tlsSect, "tlsPort", defTlsPort));
            if (TStringUtils.isBlank(tlsSect.get("tlsKeyStorePath"))) {
                getSimilarConfigField(SECT_TOKEN_TLS, configKeySet, "tlsKeyStorePath");
            } else {
                tlsConfig.setTlsKeyStorePath(tlsSect.get("tlsKeyStorePath").trim());
            }
            if (TStringUtils.isBlank(tlsSect.get("tlsKeyStorePassword"))) {
                getSimilarConfigField(SECT_TOKEN_TLS, configKeySet, "tlsKeyStorePassword");
            } else {
                tlsConfig.setTlsKeyStorePassword(tlsSect.get("tlsKeyStorePassword").trim());
            }
            if (TStringUtils.isNotBlank(tlsSect.get("tlsTwoWayAuthEnable"))) {
                tlsConfig.setTlsTwoWayAuthEnable(getBoolean(tlsSect, "tlsTwoWayAuthEnable"));
            }
            if (tlsConfig.isTlsTwoWayAuthEnable()) {
                if (TStringUtils.isBlank(tlsSect.get("tlsTrustStorePath"))) {
                    getSimilarConfigField(SECT_TOKEN_TLS, configKeySet, "tlsTrustStorePath");
                } else {
                    tlsConfig.setTlsTrustStorePath(tlsSect.get("tlsTrustStorePath").trim());
                }
                if (TStringUtils.isBlank(tlsSect.get("tlsTrustStorePassword"))) {
                    getSimilarConfigField(SECT_TOKEN_TLS, configKeySet, "tlsTrustStorePassword");
                } else {
                    tlsConfig.setTlsTrustStorePassword(tlsSect.get("tlsTrustStorePassword").trim());
                }
            }
        }
        return tlsConfig;
    }

    protected ZKConfig loadZKeeperSectConf(final Ini iniConf) {
        final Profile.Section zkeeperSect = iniConf.get(SECT_TOKEN_ZKEEPER);
        if (zkeeperSect == null) {
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append(SECT_TOKEN_ZKEEPER).append(" configure section is required!").toString());
        }
        Set<String> configKeySet = zkeeperSect.keySet();
        if (configKeySet.isEmpty()) {
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append("Empty configure item in ").append(SECT_TOKEN_ZKEEPER)
                    .append(" section!").toString());
        }
        ZKConfig zkConfig = new ZKConfig();
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkServerAddr"))) {
            zkConfig.setZkServerAddr(zkeeperSect.get("zkServerAddr").trim());
        }
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkNodeRoot"))) {
            zkConfig.setZkNodeRoot(zkeeperSect.get("zkNodeRoot").trim());
        }
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkSessionTimeoutMs"))) {
            zkConfig.setZkSessionTimeoutMs(getInt(zkeeperSect, "zkSessionTimeoutMs"));
        }
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkConnectionTimeoutMs"))) {
            zkConfig.setZkConnectionTimeoutMs(getInt(zkeeperSect, "zkConnectionTimeoutMs"));
        }
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkSyncTimeMs"))) {
            zkConfig.setZkSyncTimeMs(getInt(zkeeperSect, "zkSyncTimeMs"));
        }
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkCommitPeriodMs"))) {
            zkConfig.setZkCommitPeriodMs(getLong(zkeeperSect, "zkCommitPeriodMs"));
        }
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkCommitFailRetries"))) {
            zkConfig.setZkCommitFailRetries(getInt(zkeeperSect, "zkCommitFailRetries"));
        }
        return zkConfig;
    }

    @Override
    public String toString() {
        return new StringBuilder(512)
                .append("\"loadFileAttr\":{\"basePath\":\"").append(basePath)
                .append("\",\"configPath\":\"").append(configPath)
                .append("\",\"loadFileChkSum\":").append(loadFileChkSum)
                .append(",\"loadFileModified\":").append(loadFileModified)
                .append("}").toString();
    }

    protected abstract void loadFileSectAttributes(Ini iniConf);

    private String findBestMatchField(Set<String> matchedSet, String checkItem) {
        String matchedField = null;
        int minDistance = Integer.MAX_VALUE;
        for (String matchItem : matchedSet) {
            int dis = TStringUtils.getLevenshteinDistance(checkItem, matchItem);
            if (dis < minDistance) {
                matchedField = matchItem;
                minDistance = dis;
            }
        }
        return matchedField;
    }

    private void loadConfigAttributes(final Ini iniConf) {
        loadFileSectAttributes(iniConf);
    }

    private void load() {
        try {
            final File file = new File(configPath);
            if (!file.exists()) {
                throw new StartupException(new StringBuilder(256).append("File ")
                        .append(configPath).append(" is not exists").toString());
            }
            basePath = file.getParent() == null ? "" : file.getParent();
            final Ini iniConf = this.createIni(file);
            this.loadConfigAttributes(iniConf);
        } catch (final IOException e) {
            throw new StartupException(new StringBuilder(256)
                    .append("Parse configuration failed,path=")
                    .append(configPath).toString(), e);
        }
    }

    private Ini createIni(final File file) throws IOException {
        final Ini conf = new Ini(file);
        this.loadFileModified = file.lastModified();
        this.loadFileChkSum = FileUtils.checksumCRC32(file);
        return conf;
    }

}
