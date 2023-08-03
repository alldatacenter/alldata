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

package org.apache.hadoop.crypto.key;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.plugin.util.AutoClosableLock.AutoClosableReadLock;
import org.apache.ranger.plugin.util.AutoClosableLock.AutoClosableTryWriteLock;
import org.apache.ranger.plugin.util.AutoClosableLock.AutoClosableWriteLock;
import org.apache.ranger.plugin.util.JsonUtilsV2;
import org.apache.hadoop.fs.Path;
import org.apache.ranger.credentialapi.CredentialReader;
import org.apache.ranger.kms.dao.DaoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InterfaceAudience.Private
public class RangerKeyStoreProvider extends KeyProvider {
    private static final Logger logger = LoggerFactory.getLogger(RangerKeyStoreProvider.class);

    public  static final String SCHEME_NAME                  = "dbks";
    public  static final String KMS_CONFIG_DIR               = "kms.config.dir";
    public  static final String DBKS_SITE_XML                = "dbks-site.xml";
    public  static final String ENCRYPTION_KEY               = "ranger.db.encrypt.key.password";
    private static final String KEY_METADATA                 = "KeyMetadata";
    private static final String CREDENTIAL_PATH              = "ranger.ks.jpa.jdbc.credential.provider.path";
    private static final String MK_CREDENTIAL_ALIAS          = "ranger.ks.masterkey.credential.alias";
    private static final String DB_CREDENTIAL_ALIAS          = "ranger.ks.jpa.jdbc.credential.alias";
    private static final String DB_PASSWORD                  = "ranger.ks.jpa.jdbc.password";
    private static final String HSM_ENABLED                  = "ranger.ks.hsm.enabled";
    private static final String HSM_PARTITION_PASSWORD_ALIAS = "ranger.ks.hsm.partition.password.alias";
    private static final String HSM_PARTITION_PASSWORD       = "ranger.ks.hsm.partition.password";
    private static final String KEYSECURE_ENABLED            = "ranger.kms.keysecure.enabled";
    private static final String KEYSECURE_USERNAME           = "ranger.kms.keysecure.login.username";
    private static final String KEYSECURE_PASSWORD_ALIAS     = "ranger.kms.keysecure.login.password.alias";
    private static final String KEYSECURE_PASSWORD           = "ranger.kms.keysecure.login.password";
    private static final String KEYSECURE_LOGIN              = "ranger.kms.keysecure.login";
    private static final String AZURE_KEYVAULT_ENABLED       = "ranger.kms.azurekeyvault.enabled";
    private static final String AZURE_CLIENT_SECRET_ALIAS    = "ranger.kms.azure.client.secret.alias";
    private static final String AZURE_CLIENT_SECRET          = "ranger.kms.azure.client.secret";
    private static final String TENCENT_KMS_ENABLED          = "ranger.kms.tencentkms.enabled";
    private static final String TENCENT_CLIENT_SECRET        = RangerTencentKMSProvider.TENCENT_CLIENT_SECRET;
    private static final String TENCENT_CLIENT_SECRET_ALIAS  = "ranger.kms.tencent.client.secret.alias";
    private static final String IS_GCP_ENABLED               = "ranger.kms.gcp.enabled";

    private final RangerKeyStore        dbStore;
    private final char[]                masterKey;
    private final Map<String, Metadata> cache = new HashMap<>();
    private final ReadWriteLock         lock  = new ReentrantReadWriteLock(true);
    private final boolean               keyVaultEnabled;
    private       boolean               changed = false;

    public RangerKeyStoreProvider(Configuration conf) throws Throwable {
        super(conf);

        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerKeyStoreProvider(conf)");
        }

        conf = getDBKSConf();

        getFromJceks(conf, CREDENTIAL_PATH, MK_CREDENTIAL_ALIAS, ENCRYPTION_KEY);
        getFromJceks(conf, CREDENTIAL_PATH, DB_CREDENTIAL_ALIAS, DB_PASSWORD);
        getFromJceks(conf, CREDENTIAL_PATH, HSM_PARTITION_PASSWORD_ALIAS, HSM_PARTITION_PASSWORD);

        String password = conf.get(ENCRYPTION_KEY);

        if (password == null || password.trim().equals("")  || password.trim().equals("_") || password.trim().equals("crypted")) {
            throw new IOException("The Ranger MasterKey Password is empty or not a valid Password");
        }

        boolean isHSMEnabled           = conf.getBoolean(HSM_ENABLED, false);
        boolean isKeySecureEnabled     = conf.getBoolean(KEYSECURE_ENABLED, false);
        boolean isAzureKeyVaultEnabled = conf.getBoolean(AZURE_KEYVAULT_ENABLED, false);
        boolean isGCPEnabled           = conf.getBoolean(IS_GCP_ENABLED, false);
        boolean isTencentKMSEnabled    = conf.getBoolean(TENCENT_KMS_ENABLED, false);

        this.keyVaultEnabled = isAzureKeyVaultEnabled || isGCPEnabled || isTencentKMSEnabled;

        final RangerKMSDB  rangerKMSDB = new RangerKMSDB(conf);
        final DaoManager   daoManager  = rangerKMSDB.getDaoManager();
        final RangerKMSMKI masterKeyProvider;

        if (isHSMEnabled) {
            logger.info("Ranger KMS HSM is enabled for storing master key.");

            String partitionPasswd = conf.get(HSM_PARTITION_PASSWORD);

            if (partitionPasswd == null || partitionPasswd.trim().equals("") || partitionPasswd.trim().equals("_") || partitionPasswd.trim().equals("crypted")) {
                throw new IOException("Partition Password doesn't exists");
            }

            masterKeyProvider = new RangerHSM(conf);
            dbStore           = new RangerKeyStore(daoManager);
            masterKey         = this.generateAndGetMasterKey(masterKeyProvider, password);
        } else if (isKeySecureEnabled) {
            logger.info("KeySecure is enabled for storing the master key.");

            getFromJceks(conf, CREDENTIAL_PATH, KEYSECURE_PASSWORD_ALIAS, KEYSECURE_PASSWORD);

            String keySecureLoginCred = conf.get(KEYSECURE_USERNAME).trim() + ":" + conf.get(KEYSECURE_PASSWORD);

            conf.set(KEYSECURE_LOGIN, keySecureLoginCred);

            masterKeyProvider = new RangerSafenetKeySecure(conf);
            dbStore           = new RangerKeyStore(daoManager);
            masterKey         = this.generateAndGetMasterKey(masterKeyProvider, password);
        } else if (isAzureKeyVaultEnabled) {
            logger.info("Azure Key Vault is enabled for storing the master key.");

            getFromJceks(conf, CREDENTIAL_PATH, AZURE_CLIENT_SECRET_ALIAS, AZURE_CLIENT_SECRET);

            try {
                masterKeyProvider = new RangerAzureKeyVaultKeyGenerator(conf);

                masterKeyProvider.onInitialization();

                // ensure master key exist
                masterKeyProvider.generateMasterKey(password);

                dbStore   = new RangerKeyStore(daoManager, true, masterKeyProvider);
                masterKey = null;
            } catch (Exception ex) {
                throw new Exception("Error while generating master key and master key secret in Azure Key Vault. Error : " + ex);
            }
        } else if (isTencentKMSEnabled) {
            logger.info("Ranger KMS Tencent KMS is enabled for storing master key.");

            getFromJceks(conf, CREDENTIAL_PATH, TENCENT_CLIENT_SECRET_ALIAS, TENCENT_CLIENT_SECRET);

            try {
                masterKeyProvider = new RangerTencentKMSProvider(conf);

                masterKeyProvider.onInitialization();

                // ensure master key exist
                masterKeyProvider.generateMasterKey(password);

                dbStore   = new RangerKeyStore(daoManager, true, masterKeyProvider);
                masterKey = null;
            } catch (Exception ex) {
                throw new Exception("Error while generating master key and master key secret in Tencent KMS. Error : " + ex);
            }
        } else if (isGCPEnabled) {
            logger.info("Google Cloud HSM is enabled for storing the master key.");

            masterKeyProvider = new RangerGoogleCloudHSMProvider(conf);

            masterKeyProvider.onInitialization();

            masterKeyProvider.generateMasterKey(password);

            dbStore   = new RangerKeyStore(daoManager, true, masterKeyProvider);
            masterKey = null;
        } else {
            logger.info("Ranger KMS Database is enabled for storing master key.");

            masterKeyProvider = new RangerMasterKey(daoManager);

            dbStore   = new RangerKeyStore(daoManager);
            masterKey = this.generateAndGetMasterKey(masterKeyProvider, password);
        }

        reloadKeys();
    }

    public static Configuration getDBKSConf() {
        Configuration newConfig = getConfiguration(true, DBKS_SITE_XML);

        getFromJceks(newConfig, CREDENTIAL_PATH, MK_CREDENTIAL_ALIAS, ENCRYPTION_KEY);
        getFromJceks(newConfig, CREDENTIAL_PATH, DB_CREDENTIAL_ALIAS, DB_PASSWORD);

        return newConfig;
    }

    @Override
    public KeyVersion createKey(String name, byte[] material, Options options) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> createKey({})", name);
        }

        KeyVersion ret;

        try (AutoClosableWriteLock ignored = new AutoClosableWriteLock(lock)) {
            reloadKeys();

            if (dbStore.engineContainsAlias(name) || cache.containsKey(name)) {
                throw new IOException("Key " + name + " already exists");
            }

            Metadata meta = new Metadata(options.getCipher(), options.getBitLength(), options.getDescription(), options.getAttributes(), new Date(), 1);

            if (options.getBitLength() != 8 * material.length) {
                throw new IOException("Wrong key length. Required " + options.getBitLength() + ", but got " + (8 * material.length));
            }

            String versionName = buildVersionName(name, 0);

            ret = innerSetKeyVersion(name, versionName, material, meta);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== createKey({})", name);
        }

        return ret;
    }

    @Override
    public void deleteKey(String name) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> deleteKey({})", name);
        }

        try (AutoClosableWriteLock ignored = new AutoClosableWriteLock(lock)) {
            reloadKeys();

            Metadata meta = getMetadata(name);

            if (meta == null) {
                throw new IOException("Key " + name + " does not exist");
            }

            for (int v = 0; v < meta.getVersions(); ++v) {
                String versionName = buildVersionName(name, v);

                try {
                    if (dbStore.engineContainsAlias(versionName)) {
                        dbStore.engineDeleteEntry(versionName);
                    }
                } catch (KeyStoreException e) {
                    throw new IOException("Problem removing " + versionName, e);
                }
            }

            try {
                if (dbStore.engineContainsAlias(name)) {
                    dbStore.engineDeleteEntry(name);
                }
            } catch (KeyStoreException e) {
                throw new IOException("Problem removing " + name + " from " + this, e);
            }

            cache.remove(name);

            changed = true;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== deleteKey({})", name);
        }
    }

    @Override
    public void flush() throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> flush()");
        }

        if (changed) {
            try (AutoClosableWriteLock ignored = new AutoClosableWriteLock(lock)) {
                try {
                    dbStore.engineStore(null, masterKey);

                    reloadKeys();
                } catch (NoSuchAlgorithmException e) {
                    throw new IOException("No such algorithm storing key", e);
                } catch (CertificateException e) {
                    throw new IOException("Certificate exception storing key", e);
                }

                changed = false;
            } catch (IOException ioe) {
                reloadKeys();

                throw ioe;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== flush()");
        }
    }

    @Override
    public KeyVersion getKeyVersion(String versionName) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> getKeyVersion({})", versionName);
        }

        KeyVersion ret = null;

        try (AutoClosableReadLock ignored = new AutoClosableReadLock(lock)) {
            if (keyVaultEnabled) {
                try {
                    boolean versionNameExists = dbStore.engineContainsAlias(versionName);

                    if (!versionNameExists) {
                        dbStore.engineLoad(null, masterKey);

                        versionNameExists = dbStore.engineContainsAlias(versionName);
                    }

                    if (versionNameExists) {
                        byte[] decryptKeyByte;

                        try {
                            decryptKeyByte = dbStore.engineGetDecryptedZoneKeyByte(versionName);
                        } catch (Exception e) {
                            throw new RuntimeException("Error while getting decrypted key." + e);
                        }

                        if (decryptKeyByte != null && decryptKeyByte.length > 0) {
                            ret = new KeyVersion(getBaseName(versionName), versionName, decryptKeyByte);
                        }
                    }

                } catch (NoSuchAlgorithmException e) {
                    throw new IOException("Can't get algorithm for key " + e.getMessage());
                } catch (CertificateException e) {
                    throw new IOException("Certificate exception storing key", e);
                }
            } else {
                SecretKeySpec key = null;
                try {
                    boolean versionNameExists = dbStore.engineContainsAlias(versionName);

                    if (!versionNameExists) {
                        dbStore.engineLoad(null, masterKey);

                        versionNameExists = dbStore.engineContainsAlias(versionName);
                    }

                    if (versionNameExists) {
                        key = (SecretKeySpec) dbStore.engineGetKey(versionName, masterKey);
                    }
                } catch (NoSuchAlgorithmException e) {
                    throw new IOException("Can't get algorithm for key " + key, e);
                } catch (UnrecoverableKeyException e) {
                    throw new IOException("Can't recover key " + key, e);
                } catch (CertificateException e) {
                    throw new IOException("Certificate exception storing key", e);
                }

                if (key != null) {
                    ret = new KeyVersion(getBaseName(versionName), versionName, key.getEncoded());
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== getKeyVersion({})", versionName);
        }

        return ret;
    }

    @Override
    public List<KeyVersion> getKeyVersions(String name) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> getKeyVersions({})", name);
        }

        List<KeyVersion> ret = new ArrayList<>();

        try (AutoClosableReadLock ignored = new AutoClosableReadLock(lock)) {
            Metadata km = getMetadata(name);

            if (km != null) {
                int latestVersion = km.getVersions();

                for (int i = 0; i < latestVersion; i++) {
                    String     versionName = buildVersionName(name, i);
                    KeyVersion v           = getKeyVersion(versionName);

                    if (v != null) {
                        ret.add(v);
                    }
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== getKeyVersions({}): count={}", name, ret.size());
        }

        return ret;
    }

    @Override
    public List<String> getKeys() throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> getKeys()");
        }

        ArrayList<String> ret = new ArrayList<>();

        reloadKeys();

        Enumeration<String> e = dbStore.engineAliases();

        while (e.hasMoreElements()) {
            String alias = e.nextElement();

            // only include the metadata key names in the list of names
            if (!alias.contains("@")) {
                ret.add(alias);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== getKeys(): count={}", ret.size());
        }

        return ret;
    }

    @Override
    public Metadata getMetadata(String name) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> getMetadata({})", name);
        }

        Metadata ret;
        boolean  addToCache = false;

        try (AutoClosableReadLock ignored = new AutoClosableReadLock(lock)) {
            ret = cache.get(name);

            if (ret == null) {
                if (!dbStore.engineContainsAlias(name)) {
                    dbStore.engineLoad(null, masterKey);
                }

                if (dbStore.engineContainsAlias(name)) {
                    if (keyVaultEnabled) {
                        ret = dbStore.engineGetKeyMetadata(name);

                        addToCache = ret != null;
                    } else {
                        Key key = dbStore.engineGetKey(name, masterKey);

                        if (key != null) {
                            ret = ((KeyMetadata) key).metadata;

                            addToCache = ret != null;
                        }
                    }
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Can't get algorithm for " + name, e);
        } catch (UnrecoverableKeyException e) {
            throw new IOException("Can't recover key for " + name, e);
        } catch (Exception e) {
            throw new IOException("Please try again ", e);
        }

        if (ret != null && addToCache) {
            try (AutoClosableTryWriteLock writeLock = new AutoClosableTryWriteLock(lock)) {
                if (writeLock.isLocked()) {
                    cache.put(name, ret);
                } else {
                    logger.debug("{} not added to cache - writeLock couldn't be obtained", name);
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== getMetadata({}): ret={}", name, ret);
        }

        return ret;
    }

    @Override
    public KeyVersion rollNewVersion(String name, byte[] material) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> rollNewVersion({})", name);
        }

        KeyVersion ret = null;

        try (AutoClosableWriteLock ignored = new AutoClosableWriteLock(lock)) {
            reloadKeys();

            Metadata meta = getMetadata(name);

            if (meta == null) {
                throw new IOException("Key " + name + " not found");
            }

            if (meta.getBitLength() != 8 * material.length) {
                throw new IOException("Wrong key length. Required " + meta.getBitLength() + ", but got " + (8 * material.length));
            }

            int    nextVersion = meta.addVersion();
            String versionName = buildVersionName(name, nextVersion);

            ret = innerSetKeyVersion(name, versionName, material, meta);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== rollNewVersion({}): ret={}", name, ret);
        }

        return ret;
    }

    private static Configuration getConfiguration(boolean loadHadoopDefaults, String... resources) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> getConfiguration()");
        }

        Configuration conf    = new Configuration(loadHadoopDefaults);
        String        confDir = System.getProperty(KMS_CONFIG_DIR);

        if (confDir != null) {
            try {
                Path confPath = new Path(confDir);

                if (!confPath.isUriPathAbsolute()) {
                    throw new RuntimeException("System property '" + KMS_CONFIG_DIR + "' must be an absolute path: " + confDir);
                }

                for (String resource : resources) {
                    conf.addResource(new URL("file://" + new Path(confDir, resource).toUri()));
                }
            } catch (MalformedURLException ex) {
                logger.error("getConfiguration() error", ex);

                throw new RuntimeException(ex);
            }
        } else {
            for (String resource : resources) {
                conf.addResource(resource);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== getConfiguration()");
        }

        return conf;
    }

    private static void getFromJceks(Configuration conf, String path, String alias, String key) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> getFromJceks()");
        }

        // update credential from keystore
        if (conf != null) {
            String pathValue  = conf.get(path);
            String aliasValue = conf.get(alias);

            if (pathValue != null && aliasValue != null) {
                String storeType    = conf.get("ranger.keystore.file.type", KeyStore.getDefaultType());
                String xaDBPassword = CredentialReader.getDecryptedString(pathValue.trim(), aliasValue.trim(), storeType);

                if (xaDBPassword != null && !xaDBPassword.trim().isEmpty() && !xaDBPassword.trim().equalsIgnoreCase("none")) {
                    conf.set(key, xaDBPassword);
                } else {
                    logger.info("Credential keystore password not applied for KMS; clear text password shall be applicable");
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== getFromJceks()");
        }
    }

    private char[] generateAndGetMasterKey(final RangerKMSMKI masterKeyProvider, final String password) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> generateAndGetMasterKey()");
        }

        char[] ret;

        try {
            masterKeyProvider.generateMasterKey(password);
        } catch (Throwable cause) {
            throw new RuntimeException("Error while generating Ranger Master key, Error - ", cause);
        }

        try {
            ret = masterKeyProvider.getMasterKey(password).toCharArray();
        } catch (Throwable cause) {
            throw new RuntimeException("Error while getting Ranger Master key, Error - ", cause);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== generateAndGetMasterKey()");
        }

        return ret;
    }

    private void loadKeys(char[] masterKey) throws NoSuchAlgorithmException, CertificateException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> loadKeys()");
        }

        dbStore.engineLoad(null, masterKey);

        if (logger.isDebugEnabled()) {
            logger.debug("<== loadKeys()");
        }
    }

    private KeyVersion innerSetKeyVersion(String name, String versionName, byte[] material, Metadata meta) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> innerSetKeyVersion(name={}, versionName={})", name, versionName);
        }

        saveKey(name, meta);

        try {
            String cipher      = meta.getCipher();
            int    bitLength   = meta.getBitLength();
            String description = meta.getDescription();
            int    version     = meta.getVersions();
            String attribute   = JsonUtilsV2.mapToJson(meta.getAttributes());

            if (keyVaultEnabled) {
                dbStore.addSecureKeyByteEntry(versionName, new SecretKeySpec(material, cipher), cipher, bitLength, description, version, attribute);
            } else {
                dbStore.addKeyEntry(versionName, new SecretKeySpec(material, cipher), masterKey, cipher, bitLength, description, version, attribute);
            }
        } catch (Exception e) {
            throw new IOException("Can't store key " + versionName, e);
        }

        changed = true;

        KeyVersion ret = new KeyVersion(name, versionName, material);

        if (logger.isDebugEnabled()) {
            logger.debug("<== innerSetKeyVersion(name={}, versionName={}): ret={}", name, versionName, ret);
        }

        return ret;
    }

    private void saveKey(String name, Metadata metadata) throws IOException {
        try {
            String attributes = JsonUtilsV2.mapToJson(metadata.getAttributes());

            if (keyVaultEnabled) {
                Key ezkey = new KeyMetadata(metadata);

                if (ezkey.getEncoded().length == 0) {
                    KeyGenerator keyGenerator = KeyGenerator.getInstance(metadata.getAlgorithm());

                    keyGenerator.init(metadata.getBitLength());

                    byte[] key = keyGenerator.generateKey().getEncoded();

                    ezkey = new SecretKeySpec(key, metadata.getCipher());
                }

                dbStore.addSecureKeyByteEntry(name, ezkey, metadata.getCipher(), metadata.getBitLength(),
                                              metadata.getDescription(), metadata.getVersions(), attributes);
            } else {
                dbStore.addKeyEntry(name, new KeyMetadata(metadata), masterKey, metadata.getAlgorithm(),
                                    metadata.getBitLength(), metadata.getDescription(), metadata.getVersions(), attributes);
            }

            cache.put(name, metadata);
        } catch (Exception e) {
            throw new IOException("Can't set metadata key " + name, e);
        }
    }

    private void reloadKeys() throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> reloadKeys()");
        }

        try (AutoClosableWriteLock ignored  = new AutoClosableWriteLock(lock)) {
            cache.clear();

            loadKeys(masterKey);
        } catch (NoSuchAlgorithmException|CertificateException e) {
            throw new IOException("Can't load Keys");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== reloadKeys()");
        }
    }

    /**
     * The factory to create JksProviders, which is used by the ServiceLoader.
     */
    public static class Factory extends KeyProviderFactory {
        @Override
        public KeyProvider createProvider(URI providerName, Configuration conf) {
            if (logger.isDebugEnabled()) {
                logger.debug("==> createProvider({})", providerName);
            }

            KeyProvider ret = null;

            try {
                if (SCHEME_NAME.equals(providerName.getScheme())) {
                    ret = new RangerKeyStoreProvider(conf);
                } else {
                    logger.warn(providerName.getScheme() + ": unrecognized schema");
                }
            } catch (Throwable e) {
                logger.error("createProvider() error", e);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("<== createProvider({})", providerName);
            }

            return ret;
        }
    }

    /**
     * An adapter between a KeyStore Key and our Metadata. This is used to store
     * the metadata in a KeyStore even though isn't really a key.
     */
    public static class KeyMetadata implements Key, Serializable {
        private final static long serialVersionUID = 8405872419967874451L;

        Metadata metadata;

        protected KeyMetadata(Metadata meta) {
            this.metadata = meta;
        }

        @Override
        public String getAlgorithm() {
            return metadata.getCipher();
        }

        @Override
        public String getFormat() {
            return KEY_METADATA;
        }

        @Override
        public byte[] getEncoded() {
            return new byte[0];
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            byte[] serialized = metadata.serialize();

            out.writeInt(serialized.length);
            out.write(serialized);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            byte[] buf = new byte[in.readInt()];

            in.readFully(buf);
            metadata = new Metadata(buf);
        }
    }
}
