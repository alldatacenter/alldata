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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.phoenix.secured;

import static org.apache.hadoop.hbase.HConstants.HBASE_DIR;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.security.PrivilegedAction;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.LocalHBaseCluster;
import org.apache.hadoop.hbase.security.HBaseKerberosUtils;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.http.HttpConfig;
import org.apache.hadoop.minikdc.MiniKdc;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.phoenix.end2end.TlsUtil;
import org.apache.phoenix.query.ConfigurationFactory;
import org.apache.phoenix.queryserver.QueryServerProperties;
import org.apache.phoenix.queryserver.server.QueryServer;
import org.apache.phoenix.util.InstanceResolver;
import org.apache.phoenix.util.ThinClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a copy of class from `org.apache.phoenix:phoenix-queryserver-it`,
 * see original javadoc in {@link org.apache.phoenix.end2end.QueryServerEnvironment}.
 *
 * It is possible to use original QueryServerEnvironment, but need to solve several issues:
 * <ul>
 *   <li>TlsUtil.getClasspathDir(QueryServerEnvironment.class); in QueryServerEnvironment fails due to the path from jar.
 *   Can be fixed with copying TlsUtil in Drill project and changing getClasspathDir method to use
 *   SecuredPhoenixTestSuite.class instead of QueryServerEnvironment.class</li>
 *   <li>SERVICE_PRINCIPAL from QueryServerEnvironment is for `securecluster` not system user. So Test fails later
 *   in process of starting Drill cluster while creating udf area RemoteFunctionRegistry#createArea, it fails
 *   on checking Precondition ImpersonationUtil.getProcessUserName().equals(fileStatus.getOwner()),
 *   where ImpersonationUtil.getProcessUserName() is 'securecluster' and fileStatus.getOwner() is
 *   your local machine login user</li>
 * </ul>
 */
public class QueryServerEnvironment {
  private static final Logger LOG = LoggerFactory.getLogger(QueryServerEnvironment.class);

  private final File TEMP_DIR = new File(getTempDir());
  private final File KEYTAB_DIR = new File(TEMP_DIR, "keytabs");
  private final List<File> USER_KEYTAB_FILES = new ArrayList<>();

  private static final String LOCAL_HOST_REVERSE_DNS_LOOKUP_NAME;
  static final String LOGIN_USER;

  static {
    try {
      // uncomment it for debugging purposes
      // System.setProperty("sun.security.krb5.debug", "true");
      LOCAL_HOST_REVERSE_DNS_LOOKUP_NAME = InetAddress.getByName("127.0.0.1").getCanonicalHostName();
      String userName = System.getProperty("user.name");
      LOGIN_USER = userName != null ? userName : "securecluster";
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static final String SPNEGO_PRINCIPAL = "HTTP/" + LOCAL_HOST_REVERSE_DNS_LOOKUP_NAME;
  private static final String PQS_PRINCIPAL = "phoenixqs/" + LOCAL_HOST_REVERSE_DNS_LOOKUP_NAME;
  private static final String SERVICE_PRINCIPAL = LOGIN_USER + "/" + LOCAL_HOST_REVERSE_DNS_LOOKUP_NAME;
  private File KEYTAB;

  private MiniKdc KDC;
  private HBaseTestingUtility UTIL = new HBaseTestingUtility();
  private LocalHBaseCluster HBASE_CLUSTER;
  private int NUM_CREATED_USERS;

  private ExecutorService PQS_EXECUTOR;
  private QueryServer PQS;
  private int PQS_PORT;
  private String PQS_URL;

  private boolean tls;

  private static String getTempDir() {
    StringBuilder sb = new StringBuilder(32);
    sb.append(System.getProperty("user.dir")).append(File.separator);
    sb.append("target").append(File.separator);
    sb.append(QueryServerEnvironment.class.getSimpleName());
    sb.append("-").append(UUID.randomUUID());
    return sb.toString();
  }

  public int getPqsPort() {
    return PQS_PORT;
  }

  public String getPqsUrl() {
    return PQS_URL;
  }

  public boolean getTls() {
    return tls;
  }

  public HBaseTestingUtility getUtil() {
    return UTIL;
  }

  public String getServicePrincipal() {
    return SERVICE_PRINCIPAL;
  }

  public File getServiceKeytab() {
    return KEYTAB;
  }

  private static void updateDefaultRealm() throws Exception {
    // (at least) one other phoenix test triggers the caching of this field before the KDC is up
    // which causes principal parsing to fail.
    Field f = KerberosName.class.getDeclaredField("defaultRealm");
    f.setAccessible(true);
    // Default realm for MiniKDC
    f.set(null, "EXAMPLE.COM");
  }

  private void createUsers(int numUsers) throws Exception {
    assertNotNull("KDC is null, was setup method called?", KDC);
    NUM_CREATED_USERS = numUsers;
    for (int i = 1; i <= numUsers; i++) {
      String principal = "user" + i;
      File keytabFile = new File(KEYTAB_DIR, principal + ".keytab");
      KDC.createPrincipal(keytabFile, principal);
      USER_KEYTAB_FILES.add(keytabFile);
    }
  }

  public Map.Entry<String, File> getUser(int offset) {
    if (!(offset > 0 && offset <= NUM_CREATED_USERS)) {
      throw new IllegalArgumentException();
    }
    return new AbstractMap.SimpleImmutableEntry<String, File>("user" + offset, USER_KEYTAB_FILES.get(offset - 1));
  }

  /**
   * Setup the security configuration for hdfs.
   */
  private void setHdfsSecuredConfiguration(Configuration conf) throws Exception {
    // Set principal+keytab configuration for HDFS
    conf.set(DFSConfigKeys.DFS_NAMENODE_KERBEROS_PRINCIPAL_KEY,
      SERVICE_PRINCIPAL + "@" + KDC.getRealm());
    conf.set(DFSConfigKeys.DFS_NAMENODE_KEYTAB_FILE_KEY, KEYTAB.getAbsolutePath());
    conf.set(DFSConfigKeys.DFS_DATANODE_KERBEROS_PRINCIPAL_KEY,
      SERVICE_PRINCIPAL + "@" + KDC.getRealm());
    conf.set(DFSConfigKeys.DFS_DATANODE_KEYTAB_FILE_KEY, KEYTAB.getAbsolutePath());
    conf.set(DFSConfigKeys.DFS_WEB_AUTHENTICATION_KERBEROS_PRINCIPAL_KEY,
      SPNEGO_PRINCIPAL + "@" + KDC.getRealm());
    // Enable token access for HDFS blocks
    conf.setBoolean(DFSConfigKeys.DFS_BLOCK_ACCESS_TOKEN_ENABLE_KEY, true);
    // Only use HTTPS (required because we aren't using "secure" ports)
    conf.set(DFSConfigKeys.DFS_HTTP_POLICY_KEY, HttpConfig.Policy.HTTPS_ONLY.name());
    // Bind on localhost for spnego to have a chance at working
    conf.set(DFSConfigKeys.DFS_NAMENODE_HTTPS_ADDRESS_KEY, "localhost:0");
    conf.set(DFSConfigKeys.DFS_DATANODE_HTTPS_ADDRESS_KEY, "localhost:0");

    // Generate SSL certs
    File keystoresDir = new File(UTIL.getDataTestDir("keystore").toUri().getPath());
    keystoresDir.mkdirs();
    String sslConfDir = TlsUtil.getClasspathDir(QueryServerEnvironment.class);
    TlsUtil.setupSSLConfig(keystoresDir.getAbsolutePath(), sslConfDir, conf, false);

    // Magic flag to tell hdfs to not fail on using ports above 1024
    conf.setBoolean("ignore.secure.ports.for.testing", true);
  }

  private static void ensureIsEmptyDirectory(File f) throws IOException {
    if (f.exists()) {
      if (f.isDirectory()) {
        FileUtils.deleteDirectory(f);
      } else {
        assertTrue("Failed to delete keytab directory", f.delete());
      }
    }
    assertTrue("Failed to create keytab directory", f.mkdirs());
  }

  /**
   * Setup and start kerberosed, hbase
   * @throws Exception
   */
  public QueryServerEnvironment(final Configuration confIn, int numberOfUsers, boolean tls)
    throws Exception {
    this.tls = tls;

    Configuration conf = UTIL.getConfiguration();
    conf.addResource(confIn);
    // Ensure the dirs we need are created/empty
    ensureIsEmptyDirectory(TEMP_DIR);
    ensureIsEmptyDirectory(KEYTAB_DIR);
    KEYTAB = new File(KEYTAB_DIR, "test.keytab");
    // Start a MiniKDC
    KDC = UTIL.setupMiniKdc(KEYTAB);
    // Create a service principal and spnego principal in one keytab
    // NB. Due to some apparent limitations between HDFS and HBase in the same JVM, trying to
    // use separate identies for HBase and HDFS results in a GSS initiate error. The quick
    // solution is to just use a single "service" principal instead of "hbase" and "hdfs"
    // (or "dn" and "nn") per usual.
    KDC.createPrincipal(KEYTAB, SPNEGO_PRINCIPAL, PQS_PRINCIPAL, SERVICE_PRINCIPAL);
    // Start ZK by hand
    UTIL.startMiniZKCluster();

    // Create a number of unprivileged users
    createUsers(numberOfUsers);

    // Set configuration for HBase
    HBaseKerberosUtils.setPrincipalForTesting(SERVICE_PRINCIPAL + "@" + KDC.getRealm());
    HBaseKerberosUtils.setSecuredConfiguration(conf);
    setHdfsSecuredConfiguration(conf);
    UserGroupInformation.setConfiguration(conf);
    conf.setInt(HConstants.MASTER_PORT, 0);
    conf.setInt(HConstants.MASTER_INFO_PORT, 0);
    conf.setInt(HConstants.REGIONSERVER_PORT, 0);
    conf.setInt(HConstants.REGIONSERVER_INFO_PORT, 0);

    if (tls) {
      conf.setBoolean(QueryServerProperties.QUERY_SERVER_TLS_ENABLED, true);
      conf.set(QueryServerProperties.QUERY_SERVER_TLS_KEYSTORE,
        TlsUtil.getKeyStoreFile().getAbsolutePath());
      conf.set(QueryServerProperties.QUERY_SERVER_TLS_KEYSTORE_PASSWORD,
        TlsUtil.getKeyStorePassword());
      conf.set(QueryServerProperties.QUERY_SERVER_TLS_TRUSTSTORE,
        TlsUtil.getTrustStoreFile().getAbsolutePath());
      conf.set(QueryServerProperties.QUERY_SERVER_TLS_TRUSTSTORE_PASSWORD,
        TlsUtil.getTrustStorePassword());
    }

    // Secure Phoenix setup
    conf.set(QueryServerProperties.QUERY_SERVER_KERBEROS_HTTP_PRINCIPAL_ATTRIB_LEGACY,
      SPNEGO_PRINCIPAL + "@" + KDC.getRealm());
    conf.set(QueryServerProperties.QUERY_SERVER_HTTP_KEYTAB_FILENAME_ATTRIB,
      KEYTAB.getAbsolutePath());
    conf.set(QueryServerProperties.QUERY_SERVER_KERBEROS_PRINCIPAL_ATTRIB,
      PQS_PRINCIPAL + "@" + KDC.getRealm());
    conf.set(QueryServerProperties.QUERY_SERVER_KEYTAB_FILENAME_ATTRIB,
      KEYTAB.getAbsolutePath());
    conf.setBoolean(QueryServerProperties.QUERY_SERVER_DISABLE_KERBEROS_LOGIN, true);
    conf.setInt(QueryServerProperties.QUERY_SERVER_HTTP_PORT_ATTRIB, 0);
    // Required so that PQS can impersonate the end-users to HBase
    conf.set("hadoop.proxyuser.phoenixqs.groups", "*");
    conf.set("hadoop.proxyuser.phoenixqs.hosts", "*");

    // Clear the cached singletons so we can inject our own.
    InstanceResolver.clearSingletons();
    // Make sure the ConnectionInfo doesn't try to pull a default Configuration
    InstanceResolver.getSingleton(ConfigurationFactory.class, new ConfigurationFactory() {
      @Override
      public Configuration getConfiguration() {
        return conf;
      }

      @Override
      public Configuration getConfiguration(Configuration confToClone) {
        Configuration copy = new Configuration(conf);
        copy.addResource(confToClone);
        return copy;
      }
    });
    updateDefaultRealm();

    // Start HDFS
    UTIL.startMiniDFSCluster(1);
    // Use LocalHBaseCluster to avoid HBaseTestingUtility from doing something wrong
    // NB. I'm not actually sure what HTU does incorrect, but this was pulled from some test
    // classes in HBase itself. I couldn't get HTU to work myself (2017/07/06)
    Path rootdir = UTIL.getDataTestDirOnTestFS(QueryServerEnvironment.class.getSimpleName());
    // There is no setRootdir method that is available in all supported HBase versions.
    conf.set(HBASE_DIR, rootdir.toString());
    HBASE_CLUSTER = new LocalHBaseCluster(conf, 1);
    HBASE_CLUSTER.startup();

    // Then fork a thread with PQS in it.
    configureAndStartQueryServer(tls);
  }

  private void configureAndStartQueryServer(boolean tls) throws Exception {
    PQS = new QueryServer(new String[0], UTIL.getConfiguration());
    // Get the PQS ident for PQS to use
    final UserGroupInformation ugi =
      UserGroupInformation.loginUserFromKeytabAndReturnUGI(PQS_PRINCIPAL,
        KEYTAB.getAbsolutePath());
    PQS_EXECUTOR = Executors.newSingleThreadExecutor();
    // Launch PQS, doing in the Kerberos login instead of letting PQS do it itself (which would
    // break the HBase/HDFS logins also running in the same test case).
    PQS_EXECUTOR.submit(new Runnable() {
      @Override
      public void run() {
        ugi.doAs(new PrivilegedAction<Void>() {
          @Override
          public Void run() {
            PQS.run();
            return null;
          }
        });
      }
    });
    PQS.awaitRunning();
    PQS_PORT = PQS.getPort();
    PQS_URL =
      ThinClientUtil.getConnectionUrl(tls ? "https" : "http", "localhost", PQS_PORT)
        + ";authentication=SPNEGO" + (tls
        ? ";truststore=" + TlsUtil.getTrustStoreFile().getAbsolutePath()
        + ";truststore_password=" + TlsUtil.getTrustStorePassword()
        : "");
    LOG.debug("Phoenix Query Server URL: {}", PQS_URL);
  }

  public void stop() throws Exception {
    // Remove our custom ConfigurationFactory for future tests
    InstanceResolver.clearSingletons();
    if (PQS_EXECUTOR != null) {
      PQS.stop();
      PQS_EXECUTOR.shutdown();
      if (!PQS_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
        LOG.info("PQS didn't exit in 5 seconds, proceeding anyways.");
      }
    }
    if (HBASE_CLUSTER != null) {
      HBASE_CLUSTER.shutdown();
      HBASE_CLUSTER.join();
    }
    if (UTIL != null) {
      UTIL.shutdownMiniZKCluster();
    }
    if (KDC != null) {
      KDC.stop();
    }
  }
}
