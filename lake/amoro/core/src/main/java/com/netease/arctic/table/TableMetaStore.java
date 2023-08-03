/*
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

package com.netease.arctic.table;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.relocated.com.google.common.base.Charsets;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.base.Strings;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.hash.Hashing;
import org.apache.iceberg.relocated.com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.krb5.KrbException;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores hadoop config files for {@link ArcticTable}
 */
public class TableMetaStore implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(TableMetaStore.class);

  private static final ConcurrentHashMap<TableMetaStore, TableMetaStore>
      CACHE = new ConcurrentHashMap<>();

  public static final String HADOOP_CONF_DIR = "conf.hadoop.dir";
  public static final String HIVE_SITE = "hive-site";
  public static final String HDFS_SITE = "hdfs-site";
  public static final String CORE_SITE = "core-site";
  public static final String KRB5_CONF = "krb.conf";
  public static final String KEYTAB = "krb.keytab";
  public static final String AUTH_METHOD = "auth.method";
  public static final String KEYTAB_LOGIN_USER = "krb.principal";
  public static final String SIMPLE_USER_NAME = "simple.user.name";
  public static final String AUTH_METHOD_SIMPLE = "SIMPLE";
  public static final String AUTH_METHOD_KERBEROS = "KERBEROS";

  private static final String KRB_CONF_FILE_NAME = "krb5.conf";
  private static final String KEY_TAB_FILE_NAME = "krb.keytab";
  private static final String META_STORE_SITE_FILE_NAME = "hive-site.xml";
  private static final String HADOOP_USER_PROPERTY = "HADOOP_USER_NAME";
  private static final String KRB5_CONF_PROPERTY = "java.security.krb5.conf";

  private static Field UGI_PRINCIPLE_FIELD;
  private static Field UGI_KEYTAB_FIELD;
  private static boolean UGI_REFLECT;

  static {
    try {
      // We must reset the private static variables in UserGroupInformation when re-login
      UGI_PRINCIPLE_FIELD = UserGroupInformation.class.getDeclaredField("keytabPrincipal");
      UGI_PRINCIPLE_FIELD.setAccessible(true);
      UGI_KEYTAB_FIELD = UserGroupInformation.class.getDeclaredField("keytabFile");
      UGI_KEYTAB_FIELD.setAccessible(true);
      UGI_REFLECT = true;
    } catch (NoSuchFieldException e) {
      // Do not need to reflect if hadoop-common version is 3.1.0+
      UGI_REFLECT = false;
      LOG.warn("Fail to reflect UserGroupInformation", e);
    }
  }

  private final byte[] metaStoreSite;
  private final byte[] hdfsSite;
  private final byte[] coreSite;
  private final String authMethod;
  private final String hadoopUsername;
  private final byte[] krbKeyTab;
  private final byte[] krbConf;
  private final String krbPrincipal;
  private final boolean disableAuth;

  private transient Configuration configuration;
  private transient UserGroupInformation ugi;
  private transient Path confCachePath;
  private transient String authInformation;

  /**
   * For Kerberos authentication, krb5.conf and keytab files need
   * to be written in TM, and method files need to be locked
   * in-process exclusive locking
   */
  private static final Object lock = new Object();

  public static final TableMetaStore EMPTY = TableMetaStore.builder()
      .withConfiguration(new Configuration()).buildForTest();

  public static Builder builder() {
    return new Builder();
  }

  private TableMetaStore(
      byte[] metaStoreSite, byte[] hdfsSite, byte[] coreSite, String authMethod,
      String hadoopUsername, byte[] krbKeyTab, byte[] krbConf, String krbPrincipal, boolean disableAuth) {
    Preconditions.checkArgument(
        authMethod == null || AUTH_METHOD_SIMPLE.equals(authMethod) || AUTH_METHOD_KERBEROS.equals(authMethod),
        "Error auth method:%s", authMethod);
    this.metaStoreSite = metaStoreSite;
    this.hdfsSite = hdfsSite;
    this.coreSite = coreSite;
    this.authMethod = authMethod;
    this.hadoopUsername = hadoopUsername;
    this.krbKeyTab = krbKeyTab;
    this.krbConf = krbConf;
    this.krbPrincipal = krbPrincipal;
    this.disableAuth = disableAuth;
  }

  private TableMetaStore(
      byte[] metaStoreSite, byte[] hdfsSite, byte[] coreSite, String authMethod,
      String hadoopUsername, byte[] krbKeyTab, byte[] krbConf, String krbPrincipal,
      Configuration configuration) {
    this.metaStoreSite = metaStoreSite == null ? new byte[0] : metaStoreSite;
    this.hdfsSite = hdfsSite == null ? new byte[0] : hdfsSite;
    this.coreSite = coreSite == null ? new byte[0] : coreSite;
    this.authMethod = authMethod == null ? AUTH_METHOD_SIMPLE : authMethod.toUpperCase();
    this.hadoopUsername = hadoopUsername == null ? System.getProperty("user.name") : hadoopUsername;
    this.krbKeyTab = krbKeyTab == null ? new byte[0] : krbKeyTab;
    this.krbConf = krbConf == null ? new byte[0] : krbConf;
    this.krbPrincipal = krbPrincipal;
    this.configuration = configuration;
    this.disableAuth = false;
  }

  public byte[] getMetaStoreSite() {
    return metaStoreSite;
  }

  public byte[] getHdfsSite() {
    return hdfsSite;
  }

  public byte[] getCoreSite() {
    return coreSite;
  }

  public byte[] getKrbKeyTab() {
    return krbKeyTab;
  }

  public byte[] getKrbConf() {
    return krbConf;
  }

  public String getKrbPrincipal() {
    return krbPrincipal;
  }

  public String getAuthMethod() {
    return authMethod;
  }

  public boolean isKerberosAuthMethod() {
    return AUTH_METHOD_KERBEROS.equalsIgnoreCase(authMethod);
  }

  public String getHadoopUsername() {
    return hadoopUsername;
  }

  public synchronized Configuration getConfiguration() {
    if (configuration == null) {
      configuration = buildConfiguration(this);
    }
    return configuration;
  }

  public synchronized UserGroupInformation getUGI() {
    if (ugi == null) {
      try {
        if (TableMetaStore.AUTH_METHOD_SIMPLE.equals(authMethod)) {
          UserGroupInformation currentUser = UserGroupInformation.getCurrentUser();
          if (currentUser == null || !currentUser.getAuthenticationMethod().equals(
              UserGroupInformation.AuthenticationMethod.valueOf(authMethod)) ||
              !currentUser.getUserName().equals(hadoopUsername)) {
            System.setProperty(HADOOP_USER_PROPERTY, hadoopUsername);
            UserGroupInformation.setConfiguration(getConfiguration());
            UserGroupInformation.loginUserFromSubject(null);
            ugi = UserGroupInformation.getLoginUser();
          } else {
            ugi = currentUser;
          }
        } else if (TableMetaStore.AUTH_METHOD_KERBEROS.equals(authMethod)) {
          generateKrbConfPath();
          constructUgi();
        }
        LOG.info("Complete to build ugi {}", authInformation());
      } catch (IOException | KrbException e) {
        throw new RuntimeException("Fail to init user group information", e);
      }
    } else {
      if (TableMetaStore.AUTH_METHOD_KERBEROS.equals(authMethod)) {
        // re-construct
        if (!ugi.getAuthenticationMethod().toString().equals(authMethod) ||
            !ugi.getUserName().equals(krbPrincipal)) {
          try {
            constructUgi();
            LOG.info("Complete to re-build ugi {}", authInformation());
          } catch (Exception e) {
            throw new RuntimeException("Fail to init user group information", e);
          }
        } else {
          // re-login
          synchronized (UserGroupInformation.class) {
            String oldKeytabFile = null;
            String oldPrincipal = null;
            if (UGI_REFLECT) {
              try {
                // use reflection to set private static field of UserGroupInformation for re-login
                // to fix static field reuse bug before hadoop-common version 3.1.0
                oldKeytabFile = (String) UGI_KEYTAB_FIELD.get(null);
                oldPrincipal = (String) UGI_PRINCIPLE_FIELD.get(null);
              } catch (IllegalAccessException e) {
                UGI_REFLECT = false;
                LOG.warn("Fail to reflect UserGroupInformation", e);
              }
            }

            String oldSystemPrincipal = System.getProperty("sun.security.krb5.principal");
            boolean systemPrincipalChanged = false;
            try {
              if (!UserGroupInformation.isSecurityEnabled()) {
                UserGroupInformation.setConfiguration(getConfiguration());
                LOG.info(
                    "Reset authentication method to Kerberos. now security env is \n" +
                        "isSecurityEnabled {}, AuthenticationMethod {}, isKeytab {}",
                    UserGroupInformation.isSecurityEnabled(),
                    ugi.getAuthenticationMethod().toString(),
                    ugi.isFromKeytab());
              }

              if (UGI_REFLECT) {
                try {
                  UGI_PRINCIPLE_FIELD.set(null, krbPrincipal);
                  UGI_KEYTAB_FIELD.set(null, getConfPath(confCachePath, KEY_TAB_FILE_NAME));
                } catch (IllegalAccessException e) {
                  UGI_REFLECT = false;
                  LOG.warn("Fail to reflect UserGroupInformation", e);
                }
              }

              if (oldSystemPrincipal != null && !oldSystemPrincipal.equals(krbPrincipal)) {
                System.setProperty("sun.security.krb5.principal", krbPrincipal);
                systemPrincipalChanged = true;
              }
              ugi.checkTGTAndReloginFromKeytab();
            } catch (Exception e) {
              throw new RuntimeException("Re-login from keytab failed", e);
            } finally {
              if (UGI_REFLECT) {
                try {
                  UGI_PRINCIPLE_FIELD.set(null, oldPrincipal);
                  UGI_KEYTAB_FIELD.set(null, oldKeytabFile);
                } catch (IllegalAccessException e) {
                  UGI_REFLECT = false;
                  LOG.warn("Fail to reflect UserGroupInformation", e);
                }
              }
              if (systemPrincipalChanged) {
                System.setProperty("sun.security.krb5.principal", oldSystemPrincipal);
              }
            }
          }
        }
      }
    }
    return ugi;
  }

  private String authInformation() {
    if (authInformation == null) {
      StringBuilder stringBuilder = new StringBuilder();
      if (disableAuth) {
        stringBuilder.append("disable authentication");
      } else if (AUTH_METHOD_KERBEROS.equalsIgnoreCase(authMethod)) {
        stringBuilder.append(authMethod).append("(").append(krbPrincipal).append(")");
      } else if (AUTH_METHOD_SIMPLE.equalsIgnoreCase(authMethod)) {
        stringBuilder.append(authMethod).append("(").append(hadoopUsername).append(")");
      }
      authInformation = stringBuilder.toString();
    }
    return authInformation;
  }

  private void constructUgi() throws IOException, KrbException {
    String krbConfFile = saveConfInPath(confCachePath, KRB_CONF_FILE_NAME, krbConf);
    String keyTabFile = saveConfInPath(confCachePath, KEY_TAB_FILE_NAME, krbKeyTab);
    System.clearProperty(HADOOP_USER_PROPERTY);
    System.setProperty(KRB5_CONF_PROPERTY, krbConfFile);
    sun.security.krb5.Config.refresh();
    UserGroupInformation.setConfiguration(getConfiguration());
    KerberosName.resetDefaultRealm();
    ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(krbPrincipal, keyTabFile);
  }

  public <T> T doAs(Callable<T> callable) {
    // if disableAuth, use process ugi to execute
    if (disableAuth) {
      return doAsUgi(callable);
    }
    return Objects.requireNonNull(getUGI()).doAs((PrivilegedAction<T>) () -> doAsUgi(callable));
  }

  /**
   * Login with configured catalog user and create a proxy user ugi. Then the operations are performed
   * within the doAs method of this proxy user ugi.
   */
  public <T> T doAsImpersonating(String proxyUser, Callable<T> callable) {
    // if disableAuth, use process ugi to execute
    if (disableAuth) {
      return doAsUgi(callable);
    }
    // create proxy user ugi and execute
    UserGroupInformation proxyUgi = UserGroupInformation.createProxyUser(proxyUser, Objects.requireNonNull(getUGI()));
    LOG.debug("proxy user {} with catalog ugi {}, and run with ugi {}.", proxyUser, getUGI(), proxyUgi);
    return proxyUgi.doAs((PrivilegedAction<T>) () -> doAsUgi(callable));
  }

  private <T> T doAsUgi(Callable<T> callable) {
    try {
      return callable.call();
    } catch (Throwable e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("run with ugi request failed.", e);
    }
  }

  public synchronized Optional<URL> getHiveSiteLocation() {
    try {
      Path confPath = generateKrbConfPath();
      if (ArrayUtils.isEmpty(metaStoreSite)) {
        return Optional.empty();
      }
      Path hiveSitePath = Paths.get(confPath.toAbsolutePath().toString(), "hive-site.xml");
      if (!hiveSitePath.toFile().exists()) {
        hiveSitePath = Paths.get(saveConfInPath(confPath, META_STORE_SITE_FILE_NAME, metaStoreSite));
      }
      org.apache.hadoop.fs.Path hadoopPath = new org.apache.hadoop.fs.Path(hiveSitePath.toAbsolutePath().toString());
      org.apache.hadoop.fs.Path hadoopPathWithSchema = new org.apache.hadoop.fs.Path("file://" +
          hadoopPath.toUri().toString());
      return Optional.of(hadoopPathWithSchema.toUri().toURL());
    } catch (MalformedURLException e) {
      throw new RuntimeException("Fail to generate hive site location", e);
    }
  }

  private Path generateKrbConfPath() {
    if (confCachePath == null) {
      String path = Paths.get("").toAbsolutePath().toString();
      String confPath = String.format("%s/%s/%s", path, "arctic_krb_conf", md5() + "_" +
          ManagementFactory.getRuntimeMXBean().getName());
      LOG.info("generate Krb conf path: {}", confPath);
      Path p = Paths.get(confPath);
      if (!p.toFile().exists()) {
        p.toFile().mkdirs();
      }
      this.confCachePath = p;
    }
    return this.confCachePath;
  }

  private String saveConfInPath(Path confPath, String confName, byte[] confValues) {
    String confFile = getConfPath(confPath, confName);
    String threadName = Thread.currentThread().getName();
    synchronized (lock) {
      if (!Paths.get(confFile).toFile().exists()) {
        LOG.info("{} do copy {}.", threadName, confFile);
        try (FileOutputStream fileOutputStream = new FileOutputStream(confFile)) {
          ByteStreams.copy(new ByteArrayInputStream(confValues), fileOutputStream);
          LOG.info("{} finish copy.", threadName);
        } catch (IOException e) {
          throw new UncheckedIOException("Fail to save conf files in work space", e);
        }
      } else {
        LOG.info("{} {} is exists.", threadName, confFile);
      }
    }
    return confFile;
  }

  private static String getConfPath(Path confPath, String confName) {
    return String.format("%s/%s", confPath.toString(), confName);
  }

  private static Configuration buildConfiguration(TableMetaStore metaStore) {
    Configuration configuration = new Configuration();
    configuration.addResource(new ByteArrayInputStream(metaStore.getCoreSite()));
    configuration.addResource(new ByteArrayInputStream(metaStore.getHdfsSite()));
    if (!ArrayUtils.isEmpty(metaStore.getMetaStoreSite())) {
      configuration.addResource(new ByteArrayInputStream(metaStore.getMetaStoreSite()));
    }
    configuration.set(CommonConfigurationKeys.IPC_CLIENT_FALLBACK_TO_SIMPLE_AUTH_ALLOWED_KEY, "true");
    //Enforce configuration resolve resources
    configuration.get(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY);

    //Avoid check hive version
    configuration.set("hive.metastore.schema.verification", "false");

    //It will encounter error(Required table missing : "DBS" in Catalog "" Schema "") when there is not this param
    configuration.set("datanucleus.schema.autoCreateAll", "true");

    return configuration;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TableMetaStore that = (TableMetaStore) o;
    return Arrays.equals(metaStoreSite, that.metaStoreSite) &&
        Arrays.equals(hdfsSite, that.hdfsSite) &&
        Arrays.equals(coreSite, that.coreSite) &&
        Objects.equals(authMethod, that.authMethod) &&
        Objects.equals(hadoopUsername, that.hadoopUsername) &&
        Arrays.equals(krbKeyTab, that.krbKeyTab) &&
        Arrays.equals(krbConf, that.krbConf) &&
        Objects.equals(krbPrincipal, that.krbPrincipal) &&
        Objects.equals(disableAuth, that.disableAuth);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(disableAuth, authMethod, hadoopUsername, krbPrincipal);
    result = 31 * result + Arrays.hashCode(metaStoreSite);
    result = 31 * result + Arrays.hashCode(hdfsSite);
    result = 31 * result + Arrays.hashCode(coreSite);
    result = 31 * result + Arrays.hashCode(krbKeyTab);
    result = 31 * result + Arrays.hashCode(krbConf);
    return result;
  }

  public static class Builder {
    private byte[] metaStoreSite;
    private byte[] hdfsSite;
    private byte[] coreSite;
    private String authMethod;
    private String hadoopUsername;
    private byte[] krbKeyTab;
    private byte[] krbConf;
    private String krbPrincipal;
    private boolean disableAuth = true;
    private final Map<String, String> properties = Maps.newHashMap();
    private Configuration configuration;

    public Builder withMetaStoreSitePath(String metaStoreSitePath) {
      this.metaStoreSite = readBytesFromFile(metaStoreSitePath);
      return this;
    }

    public Builder withMetaStoreSite(byte[] metaStoreSiteBytes) {
      this.metaStoreSite = metaStoreSiteBytes;
      return this;
    }

    public Builder withBase64MetaStoreSite(String encodedMetaStoreSite) {
      this.metaStoreSite = StringUtils.isBlank(encodedMetaStoreSite) ? null :
          Base64.getDecoder().decode(encodedMetaStoreSite);
      return this;
    }

    public Builder withHdfsSitePath(String hdfsSitePath) {
      this.hdfsSite = readBytesFromFile(hdfsSitePath);
      return this;
    }

    public Builder withHdfsSite(byte[] hdfsSiteBytes) {
      this.hdfsSite = hdfsSiteBytes;
      return this;
    }

    public Builder withBase64HdfsSite(String encodedHdfsSite) {
      this.hdfsSite = StringUtils.isBlank(encodedHdfsSite) ? null :
          Base64.getDecoder().decode(encodedHdfsSite);
      return this;
    }

    public Builder withCoreSitePath(String coreSitePath) {
      this.coreSite = readBytesFromFile(coreSitePath);
      return this;
    }

    public Builder withCoreSite(byte[] coreSiteBytes) {
      this.coreSite = coreSiteBytes;
      return this;
    }

    public Builder withBase64CoreSite(String encodedCoreSite) {
      this.coreSite = StringUtils.isBlank(encodedCoreSite) ? null :
          Base64.getDecoder().decode(encodedCoreSite);
      return this;
    }

    public Builder withSimpleAuth(String hadoopUsername) {
      this.disableAuth = false;
      this.authMethod = AUTH_METHOD_SIMPLE;
      this.hadoopUsername = hadoopUsername;
      return this;
    }

    public Builder withKrbAuth(String krbKeyTabPath, String krbConfPath, String krbPrincipal) {
      this.disableAuth = false;
      this.authMethod = AUTH_METHOD_KERBEROS;
      this.krbKeyTab = readBytesFromFile(krbKeyTabPath);
      this.krbConf = readBytesFromFile(krbConfPath);
      this.krbPrincipal = krbPrincipal;
      return this;
    }

    public Builder withKrbAuth(byte[] krbKeyTabBytes, byte[] krbConfBytes, String krbPrincipal) {
      this.disableAuth = false;
      this.authMethod = AUTH_METHOD_KERBEROS;
      this.krbKeyTab = krbKeyTabBytes;
      this.krbConf = krbConfBytes;
      this.krbPrincipal = krbPrincipal;
      return this;
    }

    public Builder withBase64KrbAuth(String encodedKrbKeytab, String encodedKrbConf, String krbPrincipal) {
      return withKrbAuth(
          Base64.getDecoder().decode(encodedKrbKeytab),
          Base64.getDecoder().decode(encodedKrbConf),
          krbPrincipal
      );
    }

    public Builder withAuth(
        String authMethod, String hadoopUsername, byte[] krbKeyTabBytes, byte[] krbConfBytes,
        String krbPrincipal) {
      this.disableAuth = false;
      this.authMethod = authMethod == null ? null : authMethod.toUpperCase();
      this.hadoopUsername = hadoopUsername;
      this.krbKeyTab = krbKeyTabBytes;
      this.krbConf = krbConfBytes;
      this.krbPrincipal = krbPrincipal;
      return this;
    }

    public Builder withBase64Auth(
        String authMethod, String hadoopUsername, String encodedKrbKeytab, String encodedKrbConf,
        String krbPrincipal) {
      this.disableAuth = false;
      byte[] keytab = null;
      if (encodedKrbKeytab != null) {
        keytab = Base64.getDecoder().decode(encodedKrbKeytab);
      }
      byte[] krbConf = null;
      if (krbPrincipal != null) {
        krbConf = Base64.getDecoder().decode(encodedKrbConf);
      }
      return withAuth(authMethod, hadoopUsername, keytab,
          krbConf, krbPrincipal);
    }

    public Builder withProperties(Map<String, String> properties) {
      this.properties.putAll(properties);
      return this;
    }

    public Builder withConfiguration(Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    private byte[] readBytesFromFile(String filePath) {
      try {
        return IOUtils.toByteArray(new FileInputStream(filePath));
      } catch (IOException e) {
        throw new UncheckedIOException("Read config failed:" + filePath, e);
      }
    }

    private void readProperties() {
      String krbConfPath = null;
      String keyTabPath = null;
      if (properties.containsKey(HADOOP_CONF_DIR)) {
        String hadoopConfDir = properties.get(HADOOP_CONF_DIR);
        if (properties.containsKey(CORE_SITE)) {
          withCoreSitePath(String.format("%s/%s", hadoopConfDir, properties.get(CORE_SITE)));
        }
        if (properties.containsKey(HDFS_SITE)) {
          withHdfsSitePath(String.format("%s/%s", hadoopConfDir, properties.get(HDFS_SITE)));
        }
        if (properties.containsKey(HIVE_SITE)) {
          withMetaStoreSitePath(String.format("%s/%s", hadoopConfDir, properties.get(HIVE_SITE)));
        }
        if (properties.containsKey(KRB5_CONF)) {
          krbConfPath = String.format("%s/%s", hadoopConfDir, properties.get(KRB5_CONF));
        }
        if (properties.containsKey(KEYTAB)) {
          keyTabPath = String.format("%s/%s", hadoopConfDir, properties.get(KEYTAB));
        }
      }
      if (properties.containsKey(AUTH_METHOD)) {
        String authMethod = properties.get(AUTH_METHOD).toUpperCase();
        if (AUTH_METHOD_SIMPLE.equals(authMethod) && properties.containsKey(SIMPLE_USER_NAME)) {
          withSimpleAuth(properties.get(SIMPLE_USER_NAME));
        } else if (AUTH_METHOD_KERBEROS.equals(authMethod) && properties.containsKey(KEYTAB_LOGIN_USER) &&
            !Strings.isNullOrEmpty(krbConfPath) && !Strings.isNullOrEmpty(keyTabPath)) {
          withKrbAuth(keyTabPath, krbConfPath, properties.get(KEYTAB_LOGIN_USER));
        }
      }
    }

    public TableMetaStore build() {
      readProperties();
      Preconditions.checkNotNull(hdfsSite);
      Preconditions.checkNotNull(coreSite);
      if (AUTH_METHOD_SIMPLE.equals(authMethod)) {
        Preconditions.checkNotNull(hadoopUsername);
      } else if (AUTH_METHOD_KERBEROS.equals(authMethod)) {
        Preconditions.checkNotNull(krbConf);
        Preconditions.checkNotNull(krbKeyTab);
        Preconditions.checkNotNull(krbPrincipal);
      } else if (authMethod != null) {
        throw new IllegalArgumentException("Unsupported auth method:" + authMethod);
      }

      LOG.info("Construct TableMetaStore with authMethod:{}, hadoopUsername:{}, krbPrincipal:{}",
          authMethod, hadoopUsername, krbPrincipal);
      TableMetaStore metaStore =
          new TableMetaStore(metaStoreSite, hdfsSite, coreSite, authMethod, hadoopUsername,
              krbKeyTab, krbConf, krbPrincipal, disableAuth);
      // If the ugi object is not closed, it will lead to a memory leak, so here need to cache the metastore object
      TableMetaStore cachedMetaStore = CACHE.putIfAbsent(metaStore, metaStore);
      if (cachedMetaStore == null) {
        return metaStore;
      } else {
        return cachedMetaStore;
      }
    }

    @VisibleForTesting
    public TableMetaStore buildForTest() {
      readProperties();
      return new TableMetaStore(metaStoreSite, hdfsSite, coreSite, authMethod, hadoopUsername,
          krbKeyTab, krbConf, krbPrincipal, configuration);
    }
  }

  private String md5() {
    return Hashing.md5().newHasher().putString("tableMetaStore:" +
        base64(getHdfsSite()) +
        base64(getCoreSite()) +
        base64(getMetaStoreSite()) +
        base64(getKrbConf()) +
        base64(getKrbKeyTab()) +
        getKrbPrincipal(), Charsets.UTF_8).hash().toString();
  }

  private String base64(byte[] bytes) {
    if (bytes == null) {
      return "";
    } else {
      return Base64.getEncoder().encodeToString(bytes);
    }
  }

  @Override
  public String toString() {
    return "TableMetaStore{" +
        "authMethod='" + authMethod + '\'' +
        ", hadoopUsername='" + hadoopUsername + '\'' +
        ", krbPrincipal='" + krbPrincipal + '\'' +
        '}';
  }
}
