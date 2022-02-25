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
package org.apache.ambari.server.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.annotations.Markdown;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.CommandExecutionType;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.utilities.ScalingThreadPoolExecutor;
import org.apache.ambari.server.events.listeners.alerts.AlertReceivedListener;
import org.apache.ambari.server.orm.JPATableGenerationStrategy;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.ambari.server.security.authentication.kerberos.AmbariKerberosAuthenticationProperties;
import org.apache.ambari.server.security.encryption.CredentialProvider;
import org.apache.ambari.server.state.services.MetricsRetrievalService;
import org.apache.ambari.server.state.services.RetryUpgradeActionService;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.topology.addservice.GroupByComponentsStrategy;
import org.apache.ambari.server.topology.addservice.HostGroupStrategy;
import org.apache.ambari.server.upgrade.AbstractUpgradeCatalog;
import org.apache.ambari.server.utils.AmbariPath;
import org.apache.ambari.server.utils.DateUtils;
import org.apache.ambari.server.utils.HostUtils;
import org.apache.ambari.server.utils.PasswordUtils;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link Configuration} class is used to read from the
 * {{ambari.properties}} file and manage/expose the configuration properties.
 * Each property is wrapped in a {@link ConfigurationProperty} type, allowing
 * the coupling of the key, default value, and type.
 * <p/>
 * The {@link Configuration#main(String[])} method can be invoked to produce <a
 * href=https://en.wikipedia.org/wiki/Markdown>Markdown</a> of all of the
 * properties, along with their default values and descriptions. This should be
 * done for every release so that the documentation of the configuration
 * properties can be kept current. The {@code index_template.md} file is used to
 * generate the markdown.
 * <p/>
 * The markdown can also be generated by running the {@code site} phase in maven
 * or by running the {@code exec:java} goal directly:
 *
 * <pre>
 * mvn site
 * mvn exec:java
 * mvn exec:java@configuration-markdown (on maven 3.3.1 and later)
 * </pre>
 */
@Singleton
public class Configuration {
  /**
   * JVM property with optional path to Makrdown template file.
   */
  private static final String AMBARI_CONFIGURATION_MD_TEMPLATE_PROPERTY = "ambari.configuration.md.template";

  /**
   * The file to generate the complete Markdown documentation.
   */
  private static final String MARKDOWN_TEMPLATE_FILE = "index_template.md";

  /**
   * The key to replace with the content of all of the properties.
   */
  private static final String MARKDOWN_CONFIGURATION_TABLE_KEY = "$configuration-properties";

  /**
   * The key to replace with the content of recomended baseline values.
   */
  private static final String MARKDOWN_BASELINE_VALUES_KEY = "$baseline-values";

  /**
   * The template to use when appending a related property in the description.
   */
  private static final String MARKDOWN_RELATED_TO_TEMPLATE = " This property is related to `%s`.";

  /**
   * The HTML {@code <br/>} tag.
   */
  private static final String HTML_BREAK_TAG = "<br/>";

  private static final String AGENT_CONFIGS_DEFAULT_SECTION = "agentConfig";

  /**
   * Used to determine which repository validation strings should be used
   * depending on the OS.
   */
  @Inject
  private OsFamily osFamily;

  /**
   * The filename of the {@link Properties} file which contains all of the
   * configurations for Ambari.
   */
  private static final String CONFIG_FILE = "ambari.properties";

  /**
   * PREFIX_DIR is shared in ambari-agent.ini and should only be called by unit
   * tests. For all server-side processing, it should be retrieved from
   * <code>HostImpl.getPrefix()</code>
   */
  public static final String PREFIX_DIR = "/var/lib/ambari-agent/data";

  /**
   * The minimum JDK version supported by Ambari.
   */
  public static final float JDK_MIN_VERSION = 1.7f;

  /**
   * The prefix for any configuration property which will be appended to
   * {@code eclipselink.jdbc.property.} before being passed into EclipseLink.
   * These properties are driver-specific properties only and do not affect
   * EclipseLink.
   */
  public static final String SERVER_JDBC_PROPERTIES_PREFIX = "server.jdbc.properties.";

  /**
   * A prefix for any EclipseLink-specifc properties which are passed into
   * EclipseLink to alter its behavior. For example:
   * {@code server.persistence.properties.eclipselink.jdbc.batch-writing.size=25 ->
   * eclipselink.jdbc.batch-writing.size=25}
   */
  public static final String SERVER_PERSISTENCE_PROPERTIES_PREFIX = "server.persistence.properties.";

  /**
   * A replacement string to use for the RCA JDBC URLs in order to supply the
   * Ambari Server hostname.
   */
  public static final String HOSTNAME_MACRO = "{hostname}";

  /**
   * The name of the persistence unit in {@code persistence.xml} for the Ambari
   * Server.
   */
  public static final String JDBC_UNIT_NAME = "ambari-server";

  /**
   * The JDBC URL to use for local {@link DatabaseType#POSTGRES} connections.
   */
  public static final String JDBC_LOCAL_URL = "jdbc:postgresql://localhost/";

  /**
   * The schema to use when creating a {@link DatabaseType#DERBY} database for
   * unit tests.
   */
  public static final String DEFAULT_DERBY_SCHEMA = "ambari";

  /**
   * The schema to use when creating a {@link DatabaseType#H2} database for
   * unit tests.
   */
  public static final String DEFAULT_H2_SCHEMA = "ambari";

  /**
   * The JDBC URL to use when creating a {@link DatabaseType#H2} database for
   * unit tests.
   */
  public static final String JDBC_IN_MEMORY_URL = String.format(
      "jdbc:h2:mem:%1$s;ALIAS_COLUMN_NAME=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS %1$s\\;SET SCHEMA %1$s;",
      DEFAULT_DERBY_SCHEMA);

  /**
   * The H2 driver to use when creating a {@link DatabaseType#H2} database
   * for unit tests.
   */
  public static final String JDBC_IN_MEMORY_DRIVER = "org.h2.Driver";

  /**
   * The H2 default user driver to use when creating a {@link DatabaseType#H2} database
   * for unit tests.
   */
  public static final String JDBC_IN_MEMORY_USER = "sa";

  /**
   * The H2 default password to use when creating a {@link DatabaseType#H2} database
   * for unit tests.
   */
  public static final String JDBC_IN_MEMORY_PASSWORD = "";

  /**
   * The JSSE property which governs the location of the keystore file
   * containing the collection of CA certificates trusted by Ambari.
   */
  public static final String JAVAX_SSL_TRUSTSTORE = "javax.net.ssl.trustStore";

  /**
   * The JSSE property which governs password to the keystore file containing
   * the collection of CA certificates trusted by Ambari.
   */
  public static final String JAVAX_SSL_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";

  /**
   * The JSSE property which governs the type of the keystore file containing
   * the collection of CA certificates trusted by Ambari.
   */
  public static final String JAVAX_SSL_TRUSTSTORE_TYPE = "javax.net.ssl.trustStoreType";

  /**
   * The configuration tag for {@code mapreduce2-log4j}
   */
  public static final String MAPREDUCE2_LOG4J_CONFIG_TAG = "mapreduce2-log4j";

  /**
   * Threadpool sizing based on the number of available processors multiplied by
   * 2.
   */
  public static final int PROCESSOR_BASED_THREADPOOL_CORE_SIZE_DEFAULT = 2
      * Runtime.getRuntime().availableProcessors();

  /**
   * Threadpool sizing based on the number of available processors multiplied by
   * 4.
   */
  public static final int PROCESSOR_BASED_THREADPOOL_MAX_SIZE_DEFAULT = 4
      * Runtime.getRuntime().availableProcessors();

  /**
   * A set of all of the custom database connector JAR property names.
   */
  private static final Set<String> dbConnectorPropertyNames = Sets.newHashSet(
      "custom.mysql.jdbc.name", "custom.oracle.jdbc.name", "custom.postgres.jdbc.name",
      "custom.mssql.jdbc.name", "custom.hsqldb.jdbc.name", "custom.sqlanywhere.jdbc.name");

  /**
   * An environment property which can be used to specify the location of the
   * Ambari key services's master key.
   */
  public static final String MASTER_KEY_ENV_PROP = "AMBARI_SECURITY_MASTER_KEY";

  /**
   * The default name of the master key file.
   */
  public static final String MASTER_KEY_FILENAME_DEFAULT = "master";

  /**
   * The default name of the JSSE keystore file used by Ambari.
   */
  public static final String MASTER_KEYSTORE_FILENAME_DEFAULT = "credentials.jceks";

  /**
   * The key in the {@code metainfo} table that represents the version of Ambari
   * that the database is running.
   */
  public static final String SERVER_VERSION_KEY = "version";

  // Ambari server log4j file name
  public static final String AMBARI_LOG_FILE = "log4j.properties";

  /**
   * The directory on the Ambari Server file system used for storing
   * Ambari Agent bootstrap information such as request responses.
   */
  @Markdown(description = "The directory on the Ambari Server file system used for storing Ambari Agent bootstrap information such as request responses.")
  public static final ConfigurationProperty<String> BOOTSTRAP_DIRECTORY = new ConfigurationProperty<>(
      "bootstrap.dir", AmbariPath.getPath("/var/run/ambari-server/bootstrap"));

  /**
   * The directory on the Ambari Server file system used for expanding Views and
   * storing webapp work.
   */
  @Markdown(description = "The directory on the Ambari Server file system used for expanding Views and storing webapp work.")
  public static final ConfigurationProperty<String> VIEWS_DIRECTORY = new ConfigurationProperty<>(
      "views.dir", AmbariPath.getPath("/var/lib/ambari-server/resources/views"));

  /**
   * Determines whether to validate a View's configuration XML file against an XSD.
   */
  @Markdown(description = "Determines whether to validate a View's configuration XML file against an XSD.")
  public static final ConfigurationProperty<String> VIEWS_VALIDATE = new ConfigurationProperty<>(
      "views.validate", "false");

  /**
   * Determines whether the view directory watcher service should be disabled.
   */
  @Markdown(description = "Determines whether the view directory watcher service should be disabled.")
  public static final ConfigurationProperty<String> DISABLE_VIEW_DIRECTORY_WATCHER = new ConfigurationProperty<>(
      "views.directory.watcher.disable", "false");

  /**
   * Determines whether remove undeployed views from the Ambari database.
   */
  @Markdown(description = "Determines whether remove undeployed views from the Ambari database.")
  public static final ConfigurationProperty<String> VIEWS_REMOVE_UNDEPLOYED = new ConfigurationProperty<>(
      "views.remove.undeployed", "false");

  /**
   * The Ambari Server webapp root directory.
   */
  @Markdown(description = "The Ambari Server webapp root directory.")
  public static final ConfigurationProperty<String> WEBAPP_DIRECTORY = new ConfigurationProperty<>(
      "webapp.dir", "web");

  /**
   * The location and name of the Python script used to bootstrap new Ambari Agent hosts.
   */
  @Markdown(description = "The location and name of the Python script used to bootstrap new Ambari Agent hosts.")
  public static final ConfigurationProperty<String> BOOTSTRAP_SCRIPT = new ConfigurationProperty<>(
      "bootstrap.script", AmbariPath.getPath("/usr/lib/ambari-server/lib/ambari_server/bootstrap.py"));

  /**
   * The location and name of the Python script executed on the Ambari Agent
   * host during the bootstrap process.
   */
  @Markdown(description = "The location and name of the Python script executed on the Ambari Agent host during the bootstrap process.")
  public static final ConfigurationProperty<String> BOOTSTRAP_SETUP_AGENT_SCRIPT = new ConfigurationProperty<>(
      "bootstrap.setup_agent.script",
      AmbariPath.getPath("/usr/lib/ambari-server/lib/ambari_server/setupAgent.py"));

  /**
   * The password to set on the {@code AMBARI_PASSPHRASE} environment variable
   * before invoking the bootstrap script.
   */
  @Markdown(description = "The password to set on the `AMBARI_PASSPHRASE` environment variable before invoking the bootstrap script.")
  public static final ConfigurationProperty<String> BOOTSTRAP_SETUP_AGENT_PASSWORD = new ConfigurationProperty<>(
      "bootstrap.setup_agent.password", "password");

  /**
   * The host name of the Ambari Server which will be used by the Ambari Agents
   * for communication.
   */
  @Markdown(description = "The host name of the Ambari Server which will be used by the Ambari Agents for communication.")
  public static final ConfigurationProperty<String> BOOTSTRAP_MASTER_HOSTNAME = new ConfigurationProperty<>(
      "bootstrap.master_host_name", null);

  /**
   * The amount of time that recommendation API data is kept on the Ambari
   * Server file system. This is specified using a {@code hdwmy} syntax for
   * pairing the value with a time unit.
   *
   * @see DateUtils#getDateSpecifiedTimeAgo(String)
   */
  @Markdown(
      description = "The amount of time that Recommendation API data is kept on the Ambari Server file system. This is specified using a `hdwmy` syntax for pairing the value with a time unit (hours, days, weeks, months, years)",
      examples = { "8h", "2w", "1m" } )
  public static final ConfigurationProperty<String> RECOMMENDATIONS_ARTIFACTS_LIFETIME = new ConfigurationProperty<>(
      "recommendations.artifacts.lifetime", "1w");

    @Markdown(
            description = "Maximum number of recommendations artifacts at a given time",
            examples = {"50","10","100"} )
    public static final ConfigurationProperty<Integer> RECOMMENDATIONS_ARTIFACTS_ROLLOVER_MAX = new ConfigurationProperty<>(
            "recommendations.artifacts.rollover.max",100);

  /**
   * The directory on the Ambari Server file system used for storing
   * Recommendation API artifacts.
   */
  @Markdown(description = "The directory on the Ambari Server file system used for storing Recommendation API artifacts.")
  public static final ConfigurationProperty<String> RECOMMENDATIONS_DIR = new ConfigurationProperty<>(
      "recommendations.dir", AmbariPath.getPath("/var/run/ambari-server/stack-recommendations"));

  /**
   * The location and name of the Python stack advisor script executed when
   * configuring services.
   */
  @Markdown(description = "The location and name of the Python stack advisor script executed when configuring services.")
  public static final ConfigurationProperty<String> STACK_ADVISOR_SCRIPT = new ConfigurationProperty<>(
      "stackadvisor.script",
      AmbariPath.getPath("/var/lib/ambari-server/resources/scripts/stack_advisor.py"));

  /**
   * The name of the shell script used to wrap all invocations of Python by Ambari.
   */
  @Markdown(description = "The name of the shell script used to wrap all invocations of Python by Ambari. ")
  public static final ConfigurationProperty<String> AMBARI_PYTHON_WRAP = new ConfigurationProperty<>(
      "ambari.python.wrap", "ambari-python-wrap");

  /**
   * The username of the default user assumed to be executing API calls. When
   * set, authentication is not required in order to login to Ambari or use the
   * REST APIs.
   */
  @Markdown(description = "The username of the default user assumed to be executing API calls. When set, authentication is not required in order to login to Ambari or use the REST APIs.  ")
  public static final ConfigurationProperty<String> API_AUTHENTICATED_USER = new ConfigurationProperty<>(
      "api.authenticated.user", null);

  /**
   * Determines whether SSL is used in for secure connections to Ambari.
   */
  @Markdown(description = "Determines whether SSL is used in for secure connections to Ambari. When enabled, ambari-server setup-https must be run in order to properly configure keystores.")
  public static final ConfigurationProperty<String> API_USE_SSL = new ConfigurationProperty<>(
      "api.ssl", "false");

  /**
   * Determines whether
   * {@code org.apache.ambari.server.api.AmbariCsrfProtectionFilter} is used to
   * intercept requests and inspect the headers for an {@code X-Requested-By}
   * value. This helps to prevent Cross-Site Request Forgery attacks.
   */
  @Markdown(description = "Determines whether Cross-Site Request Forgery attacks are prevented by looking for the `X-Requested-By` header.")
  public static final ConfigurationProperty<String> API_CSRF_PREVENTION = new ConfigurationProperty<>(
      "api.csrfPrevention.enabled", "true");

  /**
   * Determines whether Gzip handler is enabled for Jetty.
   */
  @Markdown(description = "Determines whether jetty Gzip compression is enabled or not.")
  public static final ConfigurationProperty<String> GZIP_HANDLER_JETTY_ENABLED = new ConfigurationProperty<>(
    "gzip.handler.jetty.enabled", "true");

  /**
   * Determines whether HTTP body data is compressed with GZIP.
   */
  @Markdown(description = "Determines whether data sent to and from the Ambari service should be compressed.")
  public static final ConfigurationProperty<String> API_GZIP_COMPRESSION_ENABLED = new ConfigurationProperty<>(
      "api.gzip.compression.enabled", "true");

  /**
   * Used in conjunction with {@link #API_GZIP_COMPRESSION_ENABLED}, determines
   * the mininum size (in bytes) that an HTTP request must be before it should be
   * compressed.
   */
  @Markdown(description = "Used in conjunction with `api.gzip.compression.enabled`, determines the mininum size that an HTTP request must be before it should be compressed. This is measured in bytes.")
  public static final ConfigurationProperty<String> API_GZIP_MIN_COMPRESSION_SIZE = new ConfigurationProperty<>(
      "api.gzip.compression.min.size", "10240");

  /**
   * Determiens whether communication with the Ambari Agents should have the
   * JSON payloads compressed with GZIP.
   */
  @Markdown(description = "Determiens whether communication with the Ambari Agents should have the JSON payloads compressed with GZIP.")
  public static final ConfigurationProperty<String> AGENT_API_GZIP_COMPRESSION_ENABLED = new ConfigurationProperty<>(
      "agent.api.gzip.compression.enabled", "true");

  /**
   * Determines whether SSL is used to communicate between Ambari Server and Ambari Agents.
   */
  @Markdown(description = "Determines whether SSL is used to communicate between Ambari Server and Ambari Agents.")
  public static final ConfigurationProperty<String> AGENT_USE_SSL = new ConfigurationProperty<>(
      "agent.ssl", "true");

  /**
   * Determines whether the Ambari Agent host names should be validated against
   * a regular expression to ensure that they are well-formed.
   */
  @Markdown(description = "Determines whether the Ambari Agent host names should be validated against a regular expression to ensure that they are well-formed.<br><br>WARNING: By setting this value to false, host names will not be validated, allowing a possible security vulnerability as described in CVE-2014-3582. See https://cwiki.apache.org/confluence/display/AMBARI/Ambari+Vulnerabilities for more information.")
  public static final ConfigurationProperty<String> SRVR_AGENT_HOSTNAME_VALIDATE = new ConfigurationProperty<>(
      "security.agent.hostname.validate", "true");

  /**
   * Determines whether two-way SSL should be used between Ambari Server and
   * Ambari Agents so that the agents must also use SSL.
   *
   * @see HostUtils#isValidHostname(String)
   */
  @Markdown(description = "Determines whether two-way SSL should be used between Ambari Server and Ambari Agents so that the agents must also use SSL.")
  public static final ConfigurationProperty<String> SRVR_TWO_WAY_SSL = new ConfigurationProperty<>(
      "security.server.two_way_ssl", "false");

  /**
   * The port that the Ambari Server will use to communicate with the agents over SSL.
   */
  @Markdown(description = "The port that the Ambari Server will use to communicate with the agents over SSL.")
  public static final ConfigurationProperty<String> SRVR_TWO_WAY_SSL_PORT = new ConfigurationProperty<>(
      "security.server.two_way_ssl.port", "8441");

  /**
   * The port that the Ambari Agents will use to communicate with the Ambari Server over SSL.
   */
  @Markdown(description = "The port that the Ambari Agents will use to communicate with the Ambari Server over SSL.")
  public static final ConfigurationProperty<String> SRVR_ONE_WAY_SSL_PORT = new ConfigurationProperty<>(
      "security.server.one_way_ssl.port", "8440");

  /**
   * The directory on the Ambari Server where keystores are kept.
   */
  @Markdown(description = "The directory on the Ambari Server where keystores are kept.")
  public static final ConfigurationProperty<String> SRVR_KSTR_DIR = new ConfigurationProperty<>(
      "security.server.keys_dir", ".");

  /**
   * The name of the file that certificates are written to when using {@code openssl ca}.
   */
  @Markdown(description = "The name of the file located in the `security.server.keys_dir` directory where certificates will be generated when Ambari uses the `openssl ca` command.")
  public static final ConfigurationProperty<String> SRVR_CRT_NAME = new ConfigurationProperty<>(
      "security.server.cert_name", "ca.crt");

  /**
   * The name of the file that contains the CA certificate chain for certificate validation during 2-way SSL communication.
   */
  @Markdown(description = "The name of the file located in the `security.server.keys_dir` directory containing the CA certificate chain used to verify certificates during 2-way SSL communications.")
  public static final ConfigurationProperty<String> SRVR_CRT_CHAIN_NAME = new ConfigurationProperty<>(
      "security.server.cert_chain_name", "ca_chain.pem");

  /**
   * The name of the certificate request file used when generating certificates.
   */
  @Markdown(description = "The name of the certificate request file used when generating certificates.")
  public static final ConfigurationProperty<String> SRVR_CSR_NAME = new ConfigurationProperty<>(
      "security.server.csr_name", "ca.csr");

  /**
   * The name of the private key used to sign requests.
   */
  @Markdown(description = "The name of the private key used to sign requests.")
  public static final ConfigurationProperty<String> SRVR_KEY_NAME = new ConfigurationProperty<>(
      "security.server.key_name", "ca.key");

  /**
   * The name of the keystore file, located in {@link #SRVR_KSTR_DIR}.
   */
  @Markdown(description = "The name of the keystore file, located in `security.server.keys_dir`")
  public static final ConfigurationProperty<String> KSTR_NAME = new ConfigurationProperty<>(
      "security.server.keystore_name", "keystore.p12");

  /**
   * The type of the keystore file specified in {@link #KSTR_NAME}. By default
   * self-signed certificates are used and we can use keystore as truststore in
   * PKCS12 format. When CA signed certificates are used truststore should be
   * created in JKS format (truststore.jks)
   */
  @Markdown(description = "The type of the keystore file specified in `security.server.key_name`. Self-signed certificates can be `PKCS12` while CA signed certificates are `JKS`")
  public static final ConfigurationProperty<String> KSTR_TYPE = new ConfigurationProperty<>(
      "security.server.keystore_type", "PKCS12");

  /**
   * The name of the truststore file ambari uses to store trusted certificates.
   * Located in {@link #SRVR_KSTR_DIR}.
   */
  @Markdown(description = "The name of the truststore file ambari uses to store trusted certificates. Located in `security.server.keys_dir`")
  public static final ConfigurationProperty<String> TSTR_NAME = new ConfigurationProperty<>(
      "security.server.truststore_name", "keystore.p12");

  /**
   * The type of the truststore file specified in {@link #TSTR_NAME}. By default
   * self-signed certificates are used and we can use keystore as truststore in
   * PKCS12 format. When CA signed certificates are used truststore should be
   * created in JKS format (truststore.jks)
   */
  @Markdown(description = "The type of the truststore file specified in `security.server.truststore_name`. Self-signed certificates can be `PKCS12` while CA signed certificates are `JKS`")
  public static final ConfigurationProperty<String> TSTR_TYPE = new ConfigurationProperty<>(
      "security.server.truststore_type", "PKCS12");

  /**
   * The filename which contains the password for the keystores, truststores, and certificates.
   */
  @Markdown(description = "The filename which contains the password for the keystores, truststores, and certificates.")
  public static final ConfigurationProperty<String> SRVR_CRT_PASS_FILE = new ConfigurationProperty<>(
      "security.server.crt_pass_file", "pass.txt");

  /**
   * The password for the keystores, truststores, and certificates. If not specified, then {@link #SRVR_CRT_PASS_FILE} should be used.
   */
  @Markdown(description = "The password for the keystores, truststores, and certificates. If not specified, then `security.server.crt_pass_file` should be used")
  public static final ConfigurationProperty<String> SRVR_CRT_PASS = new ConfigurationProperty<>(
      "security.server.crt_pass", null);

  /**
   * The length of the randomly generated password for keystores and truststores.
   */
  @Markdown(description = "The length of the randomly generated password for keystores and truststores. ")
  public static final ConfigurationProperty<String> SRVR_CRT_PASS_LEN = new ConfigurationProperty<>(
      "security.server.crt_pass.len", "50");

  /**
   * An environment variable which can be used to supply the Ambari Server
   * password when bootstrapping new Ambari Agents.
   */
  @Markdown(description = "An environment variable which can be used to supply the Ambari Server password when bootstrapping new Ambari Agents.")
  public static final ConfigurationProperty<String> PASSPHRASE_ENV = new ConfigurationProperty<>(
      "security.server.passphrase_env_var", "AMBARI_PASSPHRASE");

  /**
   * The password to the Ambari Server to supply to new Ambari Agent hosts being
   * bootstrapped.
   */
  @Markdown(description = "The password to the Ambari Server to supply to new Ambari Agent hosts being bootstrapped.")
  public static final ConfigurationProperty<String> PASSPHRASE = new ConfigurationProperty<>(
      "security.server.passphrase", "AMBARI_PASSPHRASE");

  /**
   * A list of cipher suites which are not strong enough to use and will be excluded when creating SSL connections.
   */
  @Markdown(
      description = "A list of cipher suites which are not strong enough to use and will be excluded when creating SSL connections.",
      examples = {"SSL_RSA_WITH_RC4_128_MD5\\|SSL_RSA_WITH_RC4_12‌​8_MD5"})
  public static final ConfigurationProperty<String> SRVR_DISABLED_CIPHERS = new ConfigurationProperty<>(
      "security.server.disabled.ciphers", "");

  /**
   * The list of protocols which should not be used when creating SSL connections.
   */
  @Markdown(
      description = "The list of protocols which should not be used when creating SSL connections.",
      examples = { "TLSv1.1\\|TLSv1.2" })
  public static final ConfigurationProperty<String> SRVR_DISABLED_PROTOCOLS = new ConfigurationProperty<>(
      "security.server.disabled.protocols", "");

  /**
   * The location on the Ambari Server where all resources exist, including common services, stacks, and scripts.
   */
  @Markdown(description = "The location on the Ambari Server where all resources exist, including common services, stacks, and scripts.")
  public static final ConfigurationProperty<String> RESOURCES_DIR = new ConfigurationProperty<>(
      "resources.dir", AmbariPath.getPath("/var/lib/ambari-server/resources/"));

  /**
   * The location on the Ambari Server where the stack resources exist.
   */
  @Markdown(
      description = "The location on the Ambari Server where the stack resources exist.",
      examples = { "/var/lib/ambari-server/resources/stacks" })
  public static final ConfigurationProperty<String> METADATA_DIR_PATH = new ConfigurationProperty<>(
      "metadata.path", null);

  /**
   * The location on the Ambari Server where common service resources exist.
   * Stack services share the common service files.
   */
  @Markdown(
      description = "The location on the Ambari Server where common service resources exist. Stack services share the common service files.",
      examples = { "/var/lib/ambari-server/resources/common-services" })
  public static final ConfigurationProperty<String> COMMON_SERVICES_DIR_PATH = new ConfigurationProperty<>(
      "common.services.path", null);

  /**
   * Determines whether an existing local users will be updated as LDAP users.
   */
  @Markdown(
      description = "Determines how to handle username collision while updating from LDAP.",
      examples = {"skip", "convert", "add"}
  )
  public static final ConfigurationProperty<String> LDAP_SYNC_USERNAME_COLLISIONS_BEHAVIOR = new ConfigurationProperty<>(
      "ldap.sync.username.collision.behavior", "add");

  /**
   * The location on the Ambari Server where stack extensions exist.
   */
  @Markdown(
      description = "The location on the Ambari Server where stack extensions exist.",
      examples = { "/var/lib/ambari-server/resources/extensions" })
  public static final ConfigurationProperty<String> EXTENSIONS_DIR_PATH = new ConfigurationProperty<>(
      "extensions.path", null);

  /**
   * The Ambari Management Pack staging directory on the Ambari Server.
   */
  @Markdown(
      description = "The Ambari Management Pack staging directory on the Ambari Server.",
      examples = { "/var/lib/ambari-server/resources/mpacks" })
  public static final ConfigurationProperty<String> MPACKS_STAGING_DIR_PATH = new ConfigurationProperty<>(
      "mpacks.staging.path", null);

  /**
   * The Ambari Management Pack v2 staging directory on the Ambari Server.
   */
  @Markdown(
          description = "The Ambari Management Pack version-2 staging directory on the Ambari Server.",
          examples = { "/var/lib/ambari-server/resources/mpacks-v2" })
  public static final ConfigurationProperty<String> MPACKS_V2_STAGING_DIR_PATH = new ConfigurationProperty<>(
          "mpacks-v2.staging.path", null);

  /**
   * The full path to the file which contains the Ambari Server version.
   */
  @Markdown(
      description = "The full path to the file which contains the Ambari Server version. This is used to ensure that there is not a version mismatch between Ambari Agents and Ambari Server.",
      examples = { "/var/lib/ambari-server/resources/version" })
  public static final ConfigurationProperty<String> SERVER_VERSION_FILE = new ConfigurationProperty<>(
      "server.version.file", null);

  /**
   * Whether user accepted GPL license
   */
  @Markdown(
      description = "Whether user accepted GPL license.")
  public static final ConfigurationProperty<Boolean> GPL_LICENSE_ACCEPTED = new ConfigurationProperty<>(
      "gpl.license.accepted", false);

  /**
   * The location of the JDK on the Ambari Agent hosts.
   */
  @Markdown(
      description = "The location of the JDK on the Ambari Agent hosts. If stack.java.home exists, that is only used by Ambari Server (or you can find that as ambari_java_home in the commandParams on the agent side)",
      examples = { "/usr/jdk64/jdk1.8.0_112" })
  public static final ConfigurationProperty<String> JAVA_HOME = new ConfigurationProperty<>(
      "java.home", null);

  /**
   * The name of the JDK installation binary.
   */
  @Markdown(
      description = "The name of the JDK installation binary. If stack.jdk.name exists, that is only used by Ambari Server (or you can find that as ambari_jdk_name in the commandParams on the agent side)",
      examples = { "jdk-8u112-linux-x64.tar.gz" })
  public static final ConfigurationProperty<String> JDK_NAME = new ConfigurationProperty<>(
      "jdk.name", null);

  /**
   * The name of the JCE policy ZIP file.
   */
  @Markdown(
      description = "The name of the JCE policy ZIP file. If stack.jce.name exists, that is only used by Ambari Server (or you can find that as ambari_jce_name in the commandParams on the agent side)",
      examples = {"UnlimitedJCEPolicyJDK8.zip"})
  public static final ConfigurationProperty<String> JCE_NAME = new ConfigurationProperty<>(
      "jce.name", null);

  /**
   * The location of the JDK on the Ambari Agent hosts.
   */
  @Markdown(
    description = "The location of the JDK on the Ambari Agent hosts for stack services.",
    examples = { "/usr/jdk64/jdk1.7.0_45" })
  public static final ConfigurationProperty<String> STACK_JAVA_HOME = new ConfigurationProperty<>(
    "stack.java.home", null);

  /**
   * The name of the JDK installation binary.
   */
  @Markdown(
    description = "The name of the JDK installation binary for stack services.",
    examples = { "jdk-7u45-linux-x64.tar.gz" })
  public static final ConfigurationProperty<String> STACK_JDK_NAME = new ConfigurationProperty<>(
    "stack.jdk.name", null);

  /**
   * The name of the JCE policy ZIP file.
   */
  @Markdown(
    description = "The name of the JCE policy ZIP file for stack services.",
    examples = {"UnlimitedJCEPolicyJDK7.zip"})
  public static final ConfigurationProperty<String> STACK_JCE_NAME = new ConfigurationProperty<>(
    "stack.jce.name", null);

  /**
   * Java version of the stack
   */
  @Markdown(
    description = "JDK version of the stack, use in case of it differs from Ambari JDK version.",
    examples = {"1.7"})
  public static final ConfigurationProperty<String> STACK_JAVA_VERSION = new ConfigurationProperty<>(
    "stack.java.version", null);

  /**
   * The auto group creation by Ambari.
   */
  @Markdown(
      description = "The auto group creation by Ambari")
  public static final ConfigurationProperty<Boolean> AUTO_GROUP_CREATION = new ConfigurationProperty<>(
      "auto.group.creation", Boolean.FALSE);

  /**
   * The PAM configuration file.
   */
  @Markdown(description = "The PAM configuration file.")
  public static final ConfigurationProperty<String> PAM_CONFIGURATION_FILE = new ConfigurationProperty<>(
      "pam.configuration", null);

  /**
   * The type of authentication mechanism used by Ambari.
   *
   * @see ClientSecurityType
   */
  @Markdown(
      examples = { "local", "ldap", "pam" },
      description = "The type of authentication mechanism used by Ambari.")
  public static final ConfigurationProperty<String> CLIENT_SECURITY = new ConfigurationProperty<>(
      "client.security", null);

  /**
   * The port that client connections will use with the REST API.
   */
  @Markdown(description = "The port that client connections will use with the REST API. The Ambari Web client runs on this port.")
  public static final ConfigurationProperty<String> CLIENT_API_PORT = new ConfigurationProperty<>(
      "client.api.port", "8080");

  /**
   * The port that client connections will use with the REST API when using SSL.
   */
  @Markdown(description = "The port that client connections will use with the REST API when using SSL. The Ambari Web client runs on this port if SSL is enabled.")
  public static final ConfigurationProperty<String> CLIENT_API_SSL_PORT = new ConfigurationProperty<>(
      "client.api.ssl.port", "8443");

  /**
   * The location on the Ambari server where the REST API keystore and password files are stored if using SSL.
   */
  @Markdown(description = "The location on the Ambari server where the REST API keystore and password files are stored if using SSL.")
  public static final ConfigurationProperty<String> CLIENT_API_SSL_KSTR_DIR_NAME = new ConfigurationProperty<>(
      "client.api.ssl.keys_dir", null);

  /**
   * The name of the keystore used when the Ambari Server REST API is protected by SSL.
   */
  @Markdown(description = "The name of the keystore used when the Ambari Server REST API is protected by SSL.")
  public static final ConfigurationProperty<String> CLIENT_API_SSL_KSTR_NAME = new ConfigurationProperty<>(
      "client.api.ssl.keystore_name", "https.keystore.p12");

  /**
   * The type of the keystore file specified in {@link #CLIENT_API_SSL_KSTR_NAME}. By default
   * self-signed certificates are used and we can use keystore as truststore in
   * PKCS12 format. When CA signed certificates are used truststore should be
   * created in JKS format (truststore.jks)
   */
  @Markdown(description = "The type of the keystore file specified in `client.api.ssl.keystore_name`. Self-signed certificates can be `PKCS12` while CA signed certificates are `JKS`")
  public static final ConfigurationProperty<String> CLIENT_API_SSL_KSTR_TYPE = new ConfigurationProperty<>(
      "client.api.ssl.keystore_type", "PKCS12");

  /**
   * The name of the truststore used when the Ambari Server REST API is protected by SSL.
   */
  @Markdown(description = "The name of the truststore used when the Ambari Server REST API is protected by SSL.")
  public static final ConfigurationProperty<String> CLIENT_API_SSL_TSTR_NAME = new ConfigurationProperty<>(
      "client.api.ssl.truststore_name", "https.keystore.p12");

  /**
   * The type of the keystore file specified in {@link #CLIENT_API_SSL_KSTR_NAME}. By default
   * self-signed certificates are used and we can use keystore as truststore in
   * PKCS12 format. When CA signed certificates are used truststore should be
   * created in JKS format (truststore.jks)
   */
  @Markdown(description = "The type of the keystore file specified in `client.api.ssl.truststore_name`. Self-signed certificates can be `PKCS12` while CA signed certificates are `JKS`")
  public static final ConfigurationProperty<String> CLIENT_API_SSL_TSTR_TYPE = new ConfigurationProperty<>(
      "client.api.ssl.truststore_type", "PKCS12");

  /**
   * The filename which contains the password for the keystores, truststores,
   * and certificates for the REST API when it's protected by SSL.
   */
  @Markdown(description = "The filename which contains the password for the keystores, truststores, and certificates for the REST API when it's protected by SSL.")
  public static final ConfigurationProperty<String> CLIENT_API_SSL_CRT_PASS_FILE_NAME = new ConfigurationProperty<>(
      "client.api.ssl.cert_pass_file", "https.pass.txt");

  /**
   * The password for the keystores, truststores, and certificates for the REST
   * API when it's protected by SSL. If not specified, then
   * {@link #SRVR_CRT_PASS_FILE} should be used.
   */
  @Markdown(description = "The password for the keystores, truststores, and certificates for the REST API when it's protected by SSL. If not specified, then `client.api.ssl.cert_pass_file` should be used.")
  public static final ConfigurationProperty<String> CLIENT_API_SSL_CRT_PASS = new ConfigurationProperty<>(
      "client.api.ssl.crt_pass", null);

  /**
   * Determines whether the agents will automatically attempt to download updates to stack resources from the Ambari Server.
   */
  @Markdown(description = "Determines whether the agents will automatically attempt to download updates to stack resources from the Ambari Server.")
  public static final ConfigurationProperty<String> ENABLE_AUTO_AGENT_CACHE_UPDATE = new ConfigurationProperty<>(
      "agent.auto.cache.update", "true");

  /**
   * Determines whether the Ambari Agents will use the {@code df} or {@code df -l} command when checking disk mounts for capacity issues.
   */
  @Markdown(description = "Determines whether the Ambari Agents will use the `df` or `df -l` command when checking disk mounts for capacity issues. Auto-mounted remote directories can cause long delays.")
  public static final ConfigurationProperty<String> CHECK_REMOTE_MOUNTS = new ConfigurationProperty<>(
      "agent.check.remote.mounts", "false");

  /**
   * The timeout, used by the {@code timeout} command in linux, when checking mounts for free capacity.
   */
  @Markdown(description = "The timeout, used by the `timeout` command in linux, when checking mounts for free capacity.")
  public static final ConfigurationProperty<String> CHECK_MOUNTS_TIMEOUT = new ConfigurationProperty<>(
      "agent.check.mounts.timeout", "0");
  /**
   * The path of the file which lists the properties that should be masked from the api that returns ambari.properties
   */
  @Markdown(description = "The path of the file which lists the properties that should be masked from the api that returns ambari.properties")
  public static final ConfigurationProperty<String> PROPERTY_MASK_FILE = new ConfigurationProperty<>(
      "property.mask.file", null);

  /**
   * The name of the database.
   */
  @Markdown(description = "The name of the database.")
  public static final ConfigurationProperty<String> SERVER_DB_NAME = new ConfigurationProperty<>(
      "server.jdbc.database_name", "ambari");

  /**
   * The amount of time, in {@link TimeUnit#MILLISECONDS}, that views will wait
   * before timing out on HTTP(S) read operations.
   */
  @Markdown(description = "The amount of time, in milliseconds, that a view will wait before terminating an HTTP(S) read request.")
  public static final ConfigurationProperty<String> REQUEST_READ_TIMEOUT = new ConfigurationProperty<>(
      "views.request.read.timeout.millis", "10000");

  /**
   * The amount of time, in {@link TimeUnit#MILLISECONDS}, that views will wait
   * when trying to connect on HTTP(S) operations to a remote resource.
   */
  @Markdown(description = "The amount of time, in milliseconds, that a view will wait when trying to connect on HTTP(S) operations to a remote resource.")
  public static final ConfigurationProperty<String> REQUEST_CONNECT_TIMEOUT = new ConfigurationProperty<>(
      "views.request.connect.timeout.millis", "5000");

  /**
   * The amount of time, in {@link TimeUnit#MILLISECONDS}, that views will wait
   * before timing out on HTTP(S) read operations to the Ambari REST API.
   */
  @Markdown(description = "The amount of time, in milliseconds, that a view will wait before terminating an HTTP(S) read request to the Ambari REST API.")
  public static final ConfigurationProperty<String> AMBARI_REQUEST_READ_TIMEOUT = new ConfigurationProperty<>(
      "views.ambari.request.read.timeout.millis", "45000");

  /**
   * The amount of time, in {@link TimeUnit#MILLISECONDS}, that views will wait
   * when trying to connect on HTTP(S) operations to a remote resource.
   */
  @Markdown(description = "The amount of time, in milliseconds, that a view will wait when trying to connect on HTTP(S) operations to the Ambari REST API.")
  public static final ConfigurationProperty<String> AMBARI_REQUEST_CONNECT_TIMEOUT = new ConfigurationProperty<>(
      "views.ambari.request.connect.timeout.millis", "30000");

  /**
   * The schema within a named PostgreSQL database where Ambari's tables, users,
   * and constraints are stored.
   */
  @Markdown(description = "The schema within a named PostgreSQL database where Ambari's tables, users, and constraints are stored. ")
  public static final ConfigurationProperty<String> SERVER_JDBC_POSTGRES_SCHEMA_NAME = new ConfigurationProperty<>(
      "server.jdbc.postgres.schema", "");

  /**
   * The name of the Oracle JDBC JAR connector.
   */
  @Markdown(description = "The name of the Oracle JDBC JAR connector.")
  public static final ConfigurationProperty<String> OJDBC_JAR_NAME = new ConfigurationProperty<>(
      "db.oracle.jdbc.name", "ojdbc6.jar");

  /**
   * The name of the MySQL JDBC JAR connector.
   */
  @Markdown(description = "The name of the MySQL JDBC JAR connector.")
  public static final ConfigurationProperty<String> MYSQL_JAR_NAME = new ConfigurationProperty<>(
      "db.mysql.jdbc.name", "mysql-connector-java.jar");

  /**
   * Enable the profiling of internal locks.
   */
  @Markdown(description = "Enable the profiling of internal locks.")
  public static final ConfigurationProperty<Boolean> SERVER_LOCKS_PROFILING = new ConfigurationProperty<>("server.locks.profiling", Boolean.FALSE);

  /**
   * The size of the cache used to hold {@link HostRoleCommand} instances in-memory.
   */
  @Markdown(description = "The size of the cache which is used to hold current operations in memory until they complete.")
  public static final ConfigurationProperty<Long> SERVER_EC_CACHE_SIZE = new ConfigurationProperty<>(
      "server.ecCacheSize", 10000L);

  /**
   * Determines whether caching a requests's
   * {@link HostRoleCommandStatusSummaryDTO} is enabled.
   */
  @Markdown(description = "Determines whether an existing request's status is cached. This is enabled by default to prevent increases in database access when there are long running operations in progress.")
  public static final ConfigurationProperty<Boolean> SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED = new ConfigurationProperty<>(
      "server.hrcStatusSummary.cache.enabled", Boolean.TRUE);

  /**
   * The size of the cache which is used to hold a status of every operation in a request.
   */
  @Markdown(
      relatedTo = "server.hrcStatusSummary.cache.enabled",
      description = "The size of the cache which is used to hold a status of every operation in a request.")
  public static final ConfigurationProperty<Long> SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE = new ConfigurationProperty<>(
      "server.hrcStatusSummary.cache.size", 10000L);

  /**
   * The value is specified in {@link TimeUnit#MINUTES}.
   */
  @Markdown(
      relatedTo = "server.hrcStatusSummary.cache.enabled",
      description = "The expiration time, in minutes, of the request status cache.")
  public static final ConfigurationProperty<Long> SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION = new ConfigurationProperty<>(
      "server.hrcStatusSummary.cache.expiryDuration", 30L);

  /**
   * Determines when the stale configuration cache is enabled. If disabled, then
   * queries to determine if components need to be restarted will query the
   * database directly.
   */
  @Markdown(description = "Determines when the stale configuration cache is enabled. If disabled, then queries to determine if components need to be restarted will query the database directly.")
  public static final ConfigurationProperty<Boolean> SERVER_STALE_CONFIG_CACHE_ENABLED = new ConfigurationProperty<>(
      "server.cache.isStale.enabled", Boolean.TRUE);

  /**
   * The expiration time, in {@link TimeUnit#MINUTES}, that stale configuration information is
   * cached.
   *
   * @see #SERVER_STALE_CONFIG_CACHE_ENABLED
   */
  @Markdown(
      relatedTo = "server.cache.isStale.enabled",
      description = "The expiration time, in {@link TimeUnit#MINUTES}, that stale configuration information is cached.")
  public static final ConfigurationProperty<Integer> SERVER_STALE_CONFIG_CACHE_EXPIRATION = new ConfigurationProperty<>(
      "server.cache.isStale.expiration", 600);

  /**
   * The {@link PersistenceType} of the database.
   */
  @Markdown(
      examples = { "local", "remote" },
      description = "The type of database connection being used. Unless using an embedded PostgresSQL server, then this should be `remote`.")
  public static final ConfigurationProperty<String> SERVER_PERSISTENCE_TYPE = new ConfigurationProperty<>(
      "server.persistence.type", "local");

  /**
   * The user name used to login to the database.
   */
  @Markdown(description = "The user name used to login to the database.")
  public static final ConfigurationProperty<String> SERVER_JDBC_USER_NAME = new ConfigurationProperty<>(
      "server.jdbc.user.name", "ambari");

  /**
   * The password for the user when logging into the database.
   */
  @Markdown(description = "The password for the user when logging into the database.")
  public static final ConfigurationProperty<String> SERVER_JDBC_USER_PASSWD = new ConfigurationProperty<>(
      "server.jdbc.user.passwd", "bigdata");

  /**
   * The name of the PostgresSQL JDBC JAR connector.
   */
  @Markdown(description = "The name of the PostgresSQL JDBC JAR connector.")
  public static final ConfigurationProperty<String> SERVER_JDBC_DRIVER = new ConfigurationProperty<>(
      "server.jdbc.driver", "org.postgresql.Driver");

  /**
   * The full JDBC url used for in-memory database creation.
   */
  @Markdown(
      internal = true,
      description = "The full JDBC url used for in-memory database creation.")
  public static final ConfigurationProperty<String> SERVER_JDBC_URL = new ConfigurationProperty<>(
      "server.jdbc.url", null);

  /**
   * The size of the buffer to use, in bytes, for REST API HTTP header requests.
   */
  @Markdown(description = "The size of the buffer to use, in bytes, for REST API HTTP header requests.")
  public static final ConfigurationProperty<Integer> SERVER_HTTP_REQUEST_HEADER_SIZE = new ConfigurationProperty<>(
      "server.http.request.header.size", 64 * 1024);

  /**
   * The size of the buffer to use, in bytes, for REST API HTTP header responses.
   */
  @Markdown(description = "The size of the buffer to use, in bytes, for REST API HTTP header responses.")
  public static final ConfigurationProperty<Integer> SERVER_HTTP_RESPONSE_HEADER_SIZE = new ConfigurationProperty<>(
      "server.http.response.header.size", 64 * 1024);

  /**
   * A comma-separated list of packages which will be skipped during a stack upgrade.
   */
  @Markdown(description = "A comma-separated list of packages which will be skipped during a stack upgrade.")
  public static final ConfigurationProperty<String> ROLLING_UPGRADE_SKIP_PACKAGES_PREFIXES = new ConfigurationProperty<>(
      "rolling.upgrade.skip.packages.prefixes", "");

  /**
   * Determines whether pre-upgrade checks will be skipped when performing a stack upgrade.
   */
  @Markdown(description = "Determines whether pre-upgrade checks will be skipped when performing a rolling or express stack upgrade.")
  public static final ConfigurationProperty<Boolean> STACK_UPGRADE_BYPASS_PRECHECKS = new ConfigurationProperty<>(
      "stack.upgrade.bypass.prechecks", Boolean.FALSE);

  /**
   * If a host is shutdown or ambari-agent is stopped, then Ambari Server will
   * still keep waiting til the task timesout, say 10-20 mins. If the host comes
   * back online and ambari-agent is started, then need this retry property to
   * be greater; ideally, it should be greater than 2 * command_timeout in order
   * to retry at least 3 times in that amount of mins. Suggested value is 15-30
   * mins.
   */
  @Markdown(description = "The amount of time to wait in order to retry a command during a stack upgrade when an agent loses communication. This value must be greater than the `agent.task.timeout` value.")
  public static final ConfigurationProperty<Integer> STACK_UPGRADE_AUTO_RETRY_TIMEOUT_MINS = new ConfigurationProperty<>(
      "stack.upgrade.auto.retry.timeout.mins", 0);

  /**
   * If the stack.upgrade.auto.retry.timeout.mins property is positive, then run
   * {@link RetryUpgradeActionService} every x seconds.
   */
  @Markdown(
      relatedTo = "stack.upgrade.auto.retry.timeout.mins",
      description = "The amount of time to wait, in seconds, between checking for upgrade tasks to be retried. This value is only applicable if `stack.upgrade.auto.retry.timeout.mins` is positive.")
  public static final ConfigurationProperty<Integer> STACK_UPGRADE_AUTO_RETRY_CHECK_INTERVAL_SECS = new ConfigurationProperty<>(
      "stack.upgrade.auto.retry.check.interval.secs", 20);

  /**
   * If auto-retry during stack upgrade is enabled, skip any tasks whose custom command name contains at least one
   * of the strings in the following CSV property. Note that values have to be enclosed in quotes and separated by commas.
   */
  @Markdown(description = "A comma-separate list of upgrade tasks names to skip when retrying failed commands automatically.")
  public static final ConfigurationProperty<String> STACK_UPGRADE_AUTO_RETRY_CUSTOM_COMMAND_NAMES_TO_IGNORE = new ConfigurationProperty<>(
      "stack.upgrade.auto.retry.command.names.to.ignore",
      "\"ComponentVersionCheckAction\",\"FinalizeUpgradeAction\"");

  /**
   * If auto-retry during stack upgrade is enabled, skip any tasks whose command details contains at least one
   * of the strings in the following CSV property. Note that values have to be enclosed in quotes and separated by commas.
   */
  @Markdown(description = "A comma-separate list of upgrade tasks details to skip when retrying failed commands automatically.")
  public static final ConfigurationProperty<String> STACK_UPGRADE_AUTO_RETRY_COMMAND_DETAILS_TO_IGNORE = new ConfigurationProperty<>(
      "stack.upgrade.auto.retry.command.details.to.ignore", "\"Execute HDFS Finalize\"");

  /* =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
   * Kerberos authentication-specific properties
   * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= */
  /**
   * Determines whether to use Kerberos (SPNEGO) authentication when connecting Ambari.
   */
  @Markdown(description = "Determines whether to use Kerberos (SPNEGO) authentication when connecting Ambari.")
  public static final ConfigurationProperty<Boolean> KERBEROS_AUTH_ENABLED = new ConfigurationProperty<>(
      "authentication.kerberos.enabled", Boolean.FALSE);

  /**
   * The Kerberos principal name to use when verifying user-supplied Kerberos tokens for authentication via SPNEGO.
   */
  @Markdown(description = "The Kerberos principal name to use when verifying user-supplied Kerberos tokens for authentication via SPNEGO")
  public static final ConfigurationProperty<String> KERBEROS_AUTH_SPNEGO_PRINCIPAL = new ConfigurationProperty<>(
      "authentication.kerberos.spnego.principal", "HTTP/_HOST");

  /**
   * The Kerberos identity to use when verifying user-supplied Kerberos tokens for authentication via SPNEGO.
   */
  @Markdown(description = "The Kerberos keytab file to use when verifying user-supplied Kerberos tokens for authentication via SPNEGO")
  public static final ConfigurationProperty<String> KERBEROS_AUTH_SPNEGO_KEYTAB_FILE = new ConfigurationProperty<>(
      "authentication.kerberos.spnego.keytab.file", "/etc/security/keytabs/spnego.service.keytab");

  /**
   * The auth-to-local rules set to use when translating a user's principal name to a local user name
   * during authentication via SPNEGO.
   */
  @Markdown(description = "The auth-to-local rules set to use when translating a user's principal name to a local user name during authentication via SPNEGO.")
  public static final ConfigurationProperty<String> KERBEROS_AUTH_AUTH_TO_LOCAL_RULES  = new ConfigurationProperty<>(
      "authentication.kerberos.auth_to_local.rules", "DEFAULT");
  /* =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
   * Kerberos authentication-specific properties (end)
   * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= */


  @Markdown(description = "The number of times failed Kerberos operations should be retried to execute.")
  public static final ConfigurationProperty<Integer> KERBEROS_OPERATION_RETRIES = new ConfigurationProperty<>(
      "kerberos.operation.retries", 3);

  @Markdown(description = "The time to wait (in seconds) between failed Kerberos operations retries.")
  public static final ConfigurationProperty<Integer> KERBEROS_OPERATION_RETRY_TIMEOUT = new ConfigurationProperty<>(
      "kerberos.operation.retry.timeout", 10);

  /**
   * A flag indicating whether to validate the trust of an SSL certificate provided by a KDC when
   * performing Kerberos operations.
   *
   * For example, when communicating with an Active Directory using
   * LDAPS. The default behavior is to validate the trust.
   */
  @Markdown(description = "Validate the trust of the SSL certificate provided by the KDC when performing Kerberos operations over SSL.")
  public static final ConfigurationProperty<Boolean> KERBEROS_OPERATION_VERIFY_KDC_TRUST = new ConfigurationProperty<>(
      "kerberos.operation.verify.kdc.trust", Boolean.TRUE);

  /**
   * The type of connection pool to use with JDBC connections to the database.
   */
  @Markdown(
      examples = {"internal", "c3p0"},
      description = "The connection pool manager to use for database connections. If using MySQL, then `c3p0` is automatically chosen.")
  public static final ConfigurationProperty<String> SERVER_JDBC_CONNECTION_POOL = new ConfigurationProperty<>(
      "server.jdbc.connection-pool", ConnectionPoolType.INTERNAL.getName());

  /**
   * The minimum number of connections that should always exist in the
   * database connection pool. Only used with {@link ConnectionPoolType#C3P0}.
   */
  @Markdown(
      relatedTo = "server.jdbc.connection-pool",
      description = "The minimum number of connections that should always exist in the database connection pool. Only used with c3p0.")
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_MIN_SIZE = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.min-size", 5);

  /**
   * The maximum number of connections that should exist in the
   * database connection pool. Only used with {@link ConnectionPoolType#C3P0}.
   */
  @Markdown(
      relatedTo = "server.jdbc.connection-pool",
      description = "The maximum number of connections that should exist in the database connection pool. Only used with c3p0.")
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_MAX_SIZE = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.max-size", 32);

  /**
   * The number of connections that should be retrieved when the pool size must
   * increase. It's wise to set this higher than 1 since the assumption is that
   * a pool that needs to grow should probably grow by more than 1. Only used
   * with {@link ConnectionPoolType#C3P0}.
   */
  @Markdown(
      relatedTo = "server.jdbc.connection-pool",
      description = "The number of connections that should be retrieved when the pool size must increase. "
          + "This should be set higher than 1 since the assumption is that a pool that needs to grow should probably grow by more than 1. Only used with c3p0.")
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_AQUISITION_SIZE = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.acquisition-size", 5);

  /**
   * The maximum amount of time in {@link TimeUnit#SECONDS} any connection,
   * whether its been idle or active, should even be in the pool. This will
   * terminate the connection after the expiration age and force new connections
   * to be opened. Only used with {@link ConnectionPoolType#C3P0}.
   */
  @Markdown(
      relatedTo = "server.jdbc.connection-pool",
      description = " The maximum amount of time, in seconds, any connection, whether its been idle or active, should remain in the pool. "
          + "This will terminate the connection after the expiration age and force new connections to be opened. Only used with c3p0.")
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_MAX_AGE = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.max-age", 0);

  /**
   * The maximum amount of time in {@link TimeUnit#SECONDS} that an idle
   * connection can remain in the pool. This should always be greater than the
   * value returned from {@link #getConnectionPoolMaximumExcessIdle()}. Only used
   * with {@link ConnectionPoolType#C3P0}.
   */
  @Markdown(
      relatedTo = "server.jdbc.connection-pool",
      description = "The maximum amount of time, in seconds, that an idle connection can remain in the pool. "
          + "This should always be greater than the value returned from `server.jdbc.connection-pool.max-idle-time-excess`. Only used with c3p0.")
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.max-idle-time", 14400);

  /**
   * The maximum amount of time in {@link TimeUnit#SECONDS} that connections
   * beyond the minimum pool size should remain in the pool. This should always
   * be less than than the value returned from
   * {@link #getConnectionPoolMaximumIdle()}. Only used with
   * {@link ConnectionPoolType#C3P0}.
   */
  @Markdown(
      relatedTo = "server.jdbc.connection-pool",
      description = "The maximum amount of time, in seconds, that connections beyond the minimum pool size should remain in the pool. "
          + "This should always be less than than the value returned from `server.jdbc.connection-pool.max-idle-time`. Only used with c3p0.")
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME_EXCESS = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.max-idle-time-excess", 0);

  /**
   * The number of {@link TimeUnit#SECONDS} in between testing each idle
   * connection in the connection pool for validity. Only used with
   * {@link ConnectionPoolType#C3P0}.
   */
  @Markdown(
      relatedTo = "server.jdbc.connection-pool",
      description = "The number of seconds in between testing each idle connection in the connection pool for validity. Only used with c3p0.")
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_IDLE_TEST_INTERVAL = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.idle-test-interval", 7200);

  /**
   * The number of times connections should be retried to be acquired from
   * the database before giving up. Only used with {@link ConnectionPoolType#C3P0}.
   */
  @Markdown(
      relatedTo = "server.jdbc.connection-pool",
      description = "The number of times connections should be retried to be acquired from the database before giving up. Only used with c3p0.")
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_ATTEMPTS = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.acquisition-retry-attempts", 30);

  /**
   * The delay in {@link TimeUnit#MILLISECONDS} between connection acquisition
   * attempts. Only used with {@link ConnectionPoolType#C3P0}.
   */
  @Markdown(
      relatedTo = "server.jdbc.connection-pool",
      description = "The delay, in milliseconds, between connection acquisition attempts. Only used with c3p0.")
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_DELAY = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.acquisition-retry-delay", 1000);

  /**
   * The number of retry attempts for failed API and blueprint operations.
   */
  @Markdown(description = "The number of retry attempts for failed API and blueprint operations.")
  public static final ConfigurationProperty<Integer> OPERATIONS_RETRY_ATTEMPTS = new ConfigurationProperty<>(
      "server.operations.retry-attempts", 0);

  /**
   * The user name for connecting to the database which stores RCA information.
   */
  @Deprecated
  @Markdown(description = "The user name for connecting to the database which stores RCA information.")
  public static final ConfigurationProperty<String> SERVER_JDBC_RCA_USER_NAME = new ConfigurationProperty<>(
      "server.jdbc.rca.user.name", "mapred");

  /**
   * The password for the user when connecting to the database which stores RCA information.
   */
  @Deprecated
  @Markdown(description = "The password for the user when connecting to the database which stores RCA information.")
  public static final ConfigurationProperty<String> SERVER_JDBC_RCA_USER_PASSWD = new ConfigurationProperty<>(
      "server.jdbc.rca.user.passwd", "mapred");

  /**
   * The PostgresSQL driver name for the RCA database.
   */
  @Deprecated
  @Markdown(description = "The PostgresSQL driver name for the RCA database.")
  public static final ConfigurationProperty<String> SERVER_JDBC_RCA_DRIVER = new ConfigurationProperty<>(
      "server.jdbc.rca.driver", "org.postgresql.Driver");

  /**
   * The full JDBC URL for connecting to the RCA database.
   */
  @Deprecated
  @Markdown(description = "The full JDBC URL for connecting to the RCA database.")
  public static final ConfigurationProperty<String> SERVER_JDBC_RCA_URL = new ConfigurationProperty<>(
      "server.jdbc.rca.url", "jdbc:postgresql://" + HOSTNAME_MACRO + "/ambarirca");

  /**
   * The table generation strategy to use when initializing JPA.
   */
  @Markdown(description = "The table generation strategy to use when initializing JPA.")
  public static final ConfigurationProperty<JPATableGenerationStrategy> SERVER_JDBC_GENERATE_TABLES = new ConfigurationProperty<>(
      "server.jdbc.generateTables", JPATableGenerationStrategy.NONE);

  /**
   * The OS family for the cluster.
   */
  @Markdown(
      examples = { "redhat", "ubuntu" },
      description = "The operating system family for all hosts in the cluster. This is used when bootstrapping agents and when enabling Kerberos.")
  public static final ConfigurationProperty<String> OS_FAMILY = new ConfigurationProperty<>(
      "server.os_family", "");

  /**
   * The OS version for the cluster.
   */
  @Markdown(
      examples = { "6", "7" },
      description = "The operating system version for all hosts in the cluster. This is used when bootstrapping agents and when enabling Kerberos.")
  public static final ConfigurationProperty<String> OS_VERSION = new ConfigurationProperty<>(
      "server.os_type", "");

  /**
   * The location on the Ambari Server of the file which is used for mapping host names.
   */
  @Markdown(description = "The location on the Ambari Server of the file which is used for mapping host names.")
  public static final ConfigurationProperty<String> SRVR_HOSTS_MAPPING = new ConfigurationProperty<>(
      "server.hosts.mapping", null);

  /**
   * The location of the truststore to use when setting the
   * {@link #JAVAX_SSL_TRUSTSTORE} property.
   */
  @Markdown(description = "The location of the truststore to use when setting the `javax.net.ssl.trustStore` property.")
  public static final ConfigurationProperty<String> SSL_TRUSTSTORE_PATH = new ConfigurationProperty<>(
      "ssl.trustStore.path", null);

  /**
   * The password to use when setting the {@link #JAVAX_SSL_TRUSTSTORE_PASSWORD}
   * property.
   */
  @Markdown(description = "The password to use when setting the `javax.net.ssl.trustStorePassword` property")
  public static final ConfigurationProperty<String> SSL_TRUSTSTORE_PASSWORD = new ConfigurationProperty<>(
      "ssl.trustStore.password", null);

  /**
   * The type of truststore used by the {@link #JAVAX_SSL_TRUSTSTORE_TYPE} property.
   */
  @Markdown(description = "The type of truststore used by the `javax.net.ssl.trustStoreType` property.")
  public static final ConfigurationProperty<String> SSL_TRUSTSTORE_TYPE = new ConfigurationProperty<>(
      "ssl.trustStore.type", null);

  /**
   * The location on the Ambari Server of the master key file.
   */
  @Markdown(description = "The location on the Ambari Server of the master key file. This is the key to the master keystore.")
  public static final ConfigurationProperty<String> MASTER_KEY_LOCATION = new ConfigurationProperty<>(
      "security.master.key.location", null);

  /**
   * The location on the Ambari Server of the master keystore file.
   */
  @Markdown(description = "The location on the Ambari Server of the master keystore file.")
  public static final ConfigurationProperty<String> MASTER_KEYSTORE_LOCATION = new ConfigurationProperty<>(
      "security.master.keystore.location", null);

  /**
   * The time, in {@link TimeUnit#MINUTES}, that the temporary, in-memory
   * credential store retains values.
   */
  @Markdown(description = "The time, in minutes, that the temporary, in-memory credential store retains values.")
  public static final ConfigurationProperty<Long> TEMPORARYSTORE_RETENTION_MINUTES = new ConfigurationProperty<>(
      "security.temporary.keystore.retention.minutes", 90L);

  /**
   * Determines whether the temporary keystore should have keys actively purged
   * on a fixed internal, or only when requested after expiration.
   */
  @Markdown(description = "Determines whether the temporary keystore should have keys actively purged on a fixed internal. or only when requested after expiration.")
  public static final ConfigurationProperty<Boolean> TEMPORARYSTORE_ACTIVELY_PURGE = new ConfigurationProperty<>(
      "security.temporary.keystore.actibely.purge", Boolean.TRUE);

  /**
   * The URL to use when creating messages which should include the Ambari
   * Server URL.
   */
  @Markdown(
      examples = {"http://ambari.apache.org:8080"},
      description = "The URL to use when creating messages which should include the Ambari Server URL.")
  public static final ConfigurationProperty<String> AMBARI_DISPLAY_URL = new ConfigurationProperty<>(
      "ambari.display.url", null);

  /**
   * Key for repo validation suffixes.
   */
  @Markdown(description = "The suffixes to use when validating Ubuntu repositories.")
  public static final ConfigurationProperty<String> REPO_SUFFIX_KEY_UBUNTU = new ConfigurationProperty<>(
      "repo.validation.suffixes.ubuntu", "/dists/%s/Release");

  /**
   * The key for validating non-Ubuntu repository.
   */
  @Markdown(description = "The suffixes to use when validating most types of repositories.")
  public static final ConfigurationProperty<String> REPO_SUFFIX_KEY_DEFAULT = new ConfigurationProperty<>(
      "repo.validation.suffixes.default", "/repodata/repomd.xml");

  /**
   * Determines whether the Quartz rolling restart jobstore is clustered.
   */
  @Markdown(description = "Determines whether Quartz will use a clustered job scheduled when performing scheduled actions like rolling restarts.")
  public static final ConfigurationProperty<String> EXECUTION_SCHEDULER_CLUSTERED = new ConfigurationProperty<>(
      "server.execution.scheduler.isClustered", "false");

  /**
   * The number of threads that the Quartz job scheduler will use.
   */
  @Markdown(description = "The number of threads that the Quartz job scheduler will use when executing scheduled jobs.")
  public static final ConfigurationProperty<String> EXECUTION_SCHEDULER_THREADS = new ConfigurationProperty<>(
      "server.execution.scheduler.maxThreads", "5");

  /**
   * The number of concurrent database connections that the Quartz job scheduler can use.
   */
  @Markdown(description = "The number of concurrent database connections that the Quartz job scheduler can use.")
  public static final ConfigurationProperty<String> EXECUTION_SCHEDULER_CONNECTIONS = new ConfigurationProperty<>(
      "server.execution.scheduler.maxDbConnections", "5");

  /**
   * The maximum number of prepared statements cached per database connection.
   */
  @Markdown(description = "The maximum number of prepared statements cached per database connection.")
  public static final ConfigurationProperty<String> EXECUTION_SCHEDULER_MAX_STATEMENTS_PER_CONNECTION = new ConfigurationProperty<>(
      "server.execution.scheduler.maxStatementsPerConnection", "120");

  /**
   * The tolerance, in {@link TimeUnit#MINUTES}, that Quartz will allow a misfired job to run.
   */
  @Markdown(description = "The time, in minutes, that a scheduled job can be run after its missed scheduled execution time.")
  public static final ConfigurationProperty<Long> EXECUTION_SCHEDULER_MISFIRE_TOLERATION = new ConfigurationProperty<>(
      "server.execution.scheduler.misfire.toleration.minutes", 480L);

  /**
   * The delay, in {@link TimeUnit#SECONDS}, that a Quartz job must wait before it starts.
   */
  @Markdown(description = "The delay, in seconds, that a Quartz job must wait before it starts.")
  public static final ConfigurationProperty<Integer> EXECUTION_SCHEDULER_START_DELAY = new ConfigurationProperty<>(
      "server.execution.scheduler.start.delay.seconds", 120);

  /**
   * The time that the executions schduler will wait before checking for new
   * commands to schedule. Measure in {@link TimeUnit#SECONDS}.
   */
  @Markdown(description = "The time, in seconds, that the Quartz execution scheduler will wait before checking for new commands to schedule, such as rolling restarts.")
  public static final ConfigurationProperty<Long> EXECUTION_SCHEDULER_WAIT = new ConfigurationProperty<>(
      "server.execution.scheduler.wait", 1L);

  /**
   * The location on the Ambari Server where temporary artifacts can be created.
   */
  @Markdown(description = "The location on the Ambari Server where temporary artifacts can be created.")
  public static final ConfigurationProperty<String> SERVER_TMP_DIR = new ConfigurationProperty<>(
      "server.tmp.dir", AmbariPath.getPath("/var/lib/ambari-server/tmp"));

  /**
   * Request logs path .
   */
  @Markdown(description = "The location on the Ambari Server where request logs can be created.")
  public static final ConfigurationProperty<String> REQUEST_LOGPATH = new ConfigurationProperty<>(
      "server.requestlogs.path", null);

  /**
   * The pattern of request  logs .
   */
  @Markdown(description = "The pattern of request log file name")
  public static final ConfigurationProperty<String> REQUEST_LOGNAMEPATTERN = new ConfigurationProperty<>(
          "server.requestlogs.namepattern", "ambari-access-yyyy_mm_dd.log");

  /**
   * The number of days request logs can be retained.
   */
  @Markdown(description = "The number of days that request log would be retained.")
  public static final ConfigurationProperty<Integer> REQUEST_LOG_RETAINDAYS = new ConfigurationProperty<>(
          "server.requestlogs.retaindays", 15);

  /**
   * The time, in {@link TimeUnit#MILLISECONDS}, until an external script is killed.
   */
  @Markdown(description = "The time, in milliseconds, until an external script is killed.")
  public static final ConfigurationProperty<Integer> EXTERNAL_SCRIPT_TIMEOUT = new ConfigurationProperty<>(
      "server.script.timeout", 10000);

  /**
   * The time, in {@link TimeUnit#MILLISECONDS}, until an external script is killed.
   * n threads will execute n/2 scripts. one extra thread is needed to gather error/output stream of external script
   */
  @Markdown(description = "The number of threads that should be allocated to run external script.")
  public static final ConfigurationProperty<Integer> THREAD_POOL_SIZE_FOR_EXTERNAL_SCRIPT = new ConfigurationProperty<>(
    "server.script.threads", 20);

  public static final String DEF_ARCHIVE_EXTENSION;
  public static final String DEF_ARCHIVE_CONTENT_TYPE;

  /**
   * The port used to communicate with the Kerberos Key Distribution Center.
   */
  @Markdown(description = "The port used to communicate with the Kerberos Key Distribution Center.")
  public static final ConfigurationProperty<String> KDC_PORT = new ConfigurationProperty<>(
      "default.kdcserver.port", "88");

  /**
   * The timeout, in {@link TimeUnit#MILLISECONDS}, to wait when communicating
   * with a Kerberos Key Distribution Center.
   */
  @Markdown(description = "The timeout, in milliseconds, to wait when communicating with a Kerberos Key Distribution Center.")
  public static final ConfigurationProperty<Integer> KDC_CONNECTION_CHECK_TIMEOUT = new ConfigurationProperty<>(
      "kdcserver.connection.check.timeout", 10000);

  /**
   * The location on the Ambari Server where kerberos keytabs are cached.
   */
  @Markdown(description = "The location on the Ambari Server where Kerberos keytabs are cached.")
  public static final ConfigurationProperty<String> KERBEROSTAB_CACHE_DIR = new ConfigurationProperty<>(
      "kerberos.keytab.cache.dir", AmbariPath.getPath("/var/lib/ambari-server/data/cache"));

  /**
   * Determines whether Kerberos-enabled Ambari deployments should use JAAS to
   * validate login credentials.
   */
  @Markdown(description = "Determines whether Kerberos-enabled Ambari deployments should use JAAS to validate login credentials.")
  public static final ConfigurationProperty<Boolean> KERBEROS_CHECK_JAAS_CONFIGURATION = new ConfigurationProperty<>(
      "kerberos.check.jaas.configuration", Boolean.FALSE);

  /**
   * The type of automatic recovery of failed services and components to use.
   */
  @Markdown(
      examples = {"DEFAULT", "AUTO_START", "FULL"},
      description = "The type of automatic recovery of failed services and components to use.")
  public static final ConfigurationProperty<String> RECOVERY_TYPE = new ConfigurationProperty<>(
      "recovery.type", null);

  /**
   * The maximum number of recovery attempts of a failed component during the lifetime
   * of an Ambari Agent instance. This is reset when the Ambari Agent is
   * restarted.
   */
  @Markdown(
      description = "The maximum number of recovery attempts of a failed component during the lifetime of an Ambari Agent instance. "
      + "This is reset when the Ambari Agent is restarted.")
  public static final ConfigurationProperty<String> RECOVERY_LIFETIME_MAX_COUNT = new ConfigurationProperty<>(
      "recovery.lifetime_max_count", null);

  /**
   * The maximum number of recovery attempts of a failed component during a
   * specified recovery window.
   */
  @Markdown(description = "The maximum number of recovery attempts of a failed component during a specified recovery window.")
  public static final ConfigurationProperty<String> RECOVERY_MAX_COUNT = new ConfigurationProperty<>(
      "recovery.max_count", null);

  /**
   * The length of a recovery window, in {@link TimeUnit#MINUTES}, in which
   * recovery attempts can be retried.
   */
  @Markdown(
      relatedTo = "recovery.max_count",
      description = "The length of a recovery window, in minutes, in which recovery attempts can be retried.")
  public static final ConfigurationProperty<String> RECOVERY_WINDOW_IN_MIN = new ConfigurationProperty<>(
      "recovery.window_in_minutes", null);

  /**
   * The delay, in {@link TimeUnit#MINUTES}, between automatic retry windows.
   */
  @Markdown(description = "The delay, in minutes, between automatic retry windows.")
  public static final ConfigurationProperty<String> RECOVERY_RETRY_GAP = new ConfigurationProperty<>(
      "recovery.retry_interval", null);

  /**
   * A comma-separated list of component names which are not included in automatic recovery attempts.
   */
  @Markdown(
      examples = {"NAMENODE,ZOOKEEPER_SERVER"},
      description = "A comma-separated list of component names which are not included in automatic recovery attempts.")
  public static final ConfigurationProperty<String> RECOVERY_DISABLED_COMPONENTS = new ConfigurationProperty<>(
      "recovery.disabled_components", null);

  /**
   * A comma-separated list of component names which are included in automatic
   * recovery attempts.
   */
  @Markdown(
    examples = {"NAMENODE,ZOOKEEPER_SERVER"},
    description = "A comma-separated list of component names which are included in automatic recovery attempts.")
  public static final ConfigurationProperty<String> RECOVERY_ENABLED_COMPONENTS = new ConfigurationProperty<>(
      "recovery.enabled_components", null);

  /**
   * Allow proxy calls to these hosts and ports only
   */
  @Markdown(description = "A comma-separated whitelist of host and port values which Ambari Server can use to determine if a proxy value is valid.")
  public static final ConfigurationProperty<String> PROXY_ALLOWED_HOST_PORTS = new ConfigurationProperty<>(
      "proxy.allowed.hostports", "*:*");

  /**
   * This key defines whether stages of parallel requests are executed in
   * parallel or sequentally. Only stages from different requests
   * running on not interfering host sets may be executed in parallel.
   */
  @Markdown(description = "Determines whether operations in different execution requests can be run concurrently.")
  public static final ConfigurationProperty<Boolean> PARALLEL_STAGE_EXECUTION = new ConfigurationProperty<>(
      "server.stages.parallel", Boolean.TRUE);

  /**
   *
   * Property driving the view extraction.
   * It only applies to  blueprint deployments.
   *
   * If set to TRUE on ambari-server startup only the system views are loaded; non-system views are extracted upon a cluster
   * creation request is received and the cluster configuration is successfully performed
   *
   * It is advised to use this property only in cases when ambari-server startup time is critical (eg. cloud environments)
   *
   * By default this is FALSE so all views are extracted and deployed at server startup.
   */
  @Markdown(description = "Drives view extraction in case of blueprint deployments; non-system views are deployed when cluster configuration is successful")
  public static final ConfigurationProperty<Boolean> VIEW_EXTRACT_AFTER_CLUSTER_CONFIG =  new ConfigurationProperty<>("view.extract-after-cluster-config", Boolean.FALSE);


  /**
   * In case this is set to DEPENDENCY_ORDERED one stage is created for each request and command dependencies are
   * handled directly by ActionScheduler. In case of STAGE (which is the default) one or more stages are
   * created depending on dependencies.
   */
  @Markdown(description = "How to execute commands in one stage")
  public static final ConfigurationProperty<String> COMMAND_EXECUTION_TYPE = new ConfigurationProperty<>(
    "server.stage.command.execution_type", CommandExecutionType.STAGE.toString());

  /**
   * The time, in {@link TimeUnit#SECONDS}, before agent commands are killed.
   * This does not include package installation commands.
   */
  @Markdown(description = "The time, in seconds, before agent commands are killed. This does not include package installation commands.")
  public static final ConfigurationProperty<Long> AGENT_TASK_TIMEOUT = new ConfigurationProperty<>(
      "agent.task.timeout", 900L);

  /**
   * The time, in {@link TimeUnit#SECONDS}, before agent service check commands are killed.
   */
  @Markdown(description = "The time, in seconds, before agent service check commands are killed.")
  public static final ConfigurationProperty<Long> AGENT_SERVICE_CHECK_TASK_TIMEOUT = new ConfigurationProperty<>(
      "agent.service.check.task.timeout", 0L);

  /**
   * The time, in {@link TimeUnit#SECONDS}, before package installation commands are killed.
   */
  @Markdown(description = "The time, in seconds, before package installation commands are killed.")
  public static final ConfigurationProperty<Long> AGENT_PACKAGE_INSTALL_TASK_TIMEOUT = new ConfigurationProperty<>(
      "agent.package.install.task.timeout", 1800L);

  /**
   * Max number of tasks that may be executed within a single stage.
   * This limitation is used for tasks that when executed in a 1000+ node cluster,
   * may DDOS servers providing downloadable resources
   */
  @Markdown(description = "The maximum number of tasks which can run within a single operational request. If there are more tasks, then they will be broken up between multiple operations.")
  public static final ConfigurationProperty<Integer> AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT = new ConfigurationProperty<>(
      "agent.package.parallel.commands.limit", 100);

  /**
   * Server side task (default) timeout value
   */
  @Markdown(description = "The time, in seconds, before a server-side operation is terminated.")
  public static final ConfigurationProperty<Integer> SERVER_TASK_TIMEOUT = new ConfigurationProperty<>(
      "server.task.timeout", 1200);

  /**
   * A location of hooks folder relative to resources folder.
   */
  @Markdown(description = "A location of hooks folder relative to resources folder.")
  public static final ConfigurationProperty<String> HOOKS_FOLDER = new ConfigurationProperty<>(
      "stack.hooks.folder", "stack-hooks");

  /**
   * The location on the Ambari Server where custom actions are defined.
   */
  @Markdown(description = "The location on the Ambari Server where custom actions are defined.")
  public static final ConfigurationProperty<String> CUSTOM_ACTION_DEFINITION = new ConfigurationProperty<>(
      "custom.action.definitions",
      AmbariPath.getPath("/var/lib/ambari-server/resources/custom_action_definitions"));

  /**
   * The location on the Ambari Server where resources are stored. This is exposed via HTTP in order for Ambari Agents to access them.
   */
  @Markdown(description = "The location on the Ambari Server where resources are stored. This is exposed via HTTP in order for Ambari Agents to access them.")
  public static final ConfigurationProperty<String> SHARED_RESOURCES_DIR = new ConfigurationProperty<>(
      "shared.resources.dir",
      AmbariPath.getPath("/usr/lib/ambari-server/lib/ambari_commons/resources"));

  /**
   * The name of the user given to requests which are executed without any credentials.
   */
  @Markdown(description = "The name of the user given to requests which are executed without any credentials.")
  public static final ConfigurationProperty<String> ANONYMOUS_AUDIT_NAME = new ConfigurationProperty<>(
      "anonymous.audit.name", "_anonymous");

  /**
   * Indicator for sys prepped host
   * It is possible the some nodes are sys prepped and some are not. This can be enabled later
   * by agent over-writing global indicator from ambari-server
   */
  @Markdown(description = "Determines whether Ambari Agent instances have already have the necessary stack software installed")
  public static final ConfigurationProperty<String> SYS_PREPPED_HOSTS = new ConfigurationProperty<>(
      "packages.pre.installed", "false");

  @Markdown(description = "This property is used in specific testing circumstances only. Its use otherwise will lead to very unpredictable results with repository management and package installation")
  public static final ConfigurationProperty<String> LEGACY_OVERRIDE = new ConfigurationProperty<>(
    "repositories.legacy-override.enabled", "false");

  /**
   * The time, in {@link TimeUnit#MILLISECONDS}, that agent connections can remain open and idle.
   */
  @Markdown(description = "The time, in milliseconds, that Ambari Agent connections can remain open and idle.")
  public static final ConfigurationProperty<Integer> SERVER_CONNECTION_MAX_IDLE_TIME = new ConfigurationProperty<>(
      "server.connection.max.idle.millis", 900000);

  /**
   * The size of the Jetty connection pool used for handling incoming REST API requests.
   */
  @ConfigurationMarkdown(
      group = ConfigurationGrouping.JETTY_THREAD_POOL,
      scaleValues = {
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_10, value = "25"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_50, value = "35"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_100, value = "50"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_500, value = "65") },
      markdown = @Markdown(description = "The size of the Jetty connection pool used for handling incoming REST API requests. This should be large enough to handle requests from both web browsers and embedded Views."))
  public static final ConfigurationProperty<Integer> CLIENT_THREADPOOL_SIZE = new ConfigurationProperty<>(
      "client.threadpool.size.max", 25);

  /**
   * The size of the Jetty connection pool used for handling incoming Ambari
   * Agent requests.
   */
  @ConfigurationMarkdown(
      group = ConfigurationGrouping.JETTY_THREAD_POOL,
      scaleValues = {
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_10, value = "25"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_50, value = "35"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_100, value = "75"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_500, value = "100") },
      markdown = @Markdown(description = "The size of the Jetty connection pool used for handling incoming Ambari Agent requests."))
  public static final ConfigurationProperty<Integer> AGENT_THREADPOOL_SIZE = new ConfigurationProperty<>(
      "agent.threadpool.size.max", 25);

  /**
   * The thread pool size for spring messaging.
   */
  @Markdown(description = "Thread pool size for spring messaging")
  public static final ConfigurationProperty<Integer> MESSAGING_THREAD_POOL_SIZE = new ConfigurationProperty<>(
      "messaging.threadpool.size", 10);

  /**
   * The thread pool size for agents registration.
   */
  @Markdown(description = "Thread pool size for agents registration")
  public static final ConfigurationProperty<Integer> REGISTRATION_THREAD_POOL_SIZE = new ConfigurationProperty<>(
      "registration.threadpool.size", 10);

  /**
   * Maximal cache size for spring subscription registry.
   */
  @Markdown(description = "Maximal cache size for spring subscription registry.")
  public static final ConfigurationProperty<Integer> SUBSCRIPTION_REGISTRY_CACHE_MAX_SIZE = new ConfigurationProperty<>(
      "subscription.registry.cache.size", 1500);

  /**
   * Queue size for agents in registration.
   */
  @Markdown(description = "Queue size for agents in registration.")
  public static final ConfigurationProperty<Integer> AGENTS_REGISTRATION_QUEUE_SIZE = new ConfigurationProperty<>(
      "agents.registration.queue.size", 200);


  /**
   * Period in seconds with agents reports will be processed.
   */
  @Markdown(description = "Period in seconds with agents reports will be processed.")
  public static final ConfigurationProperty<Integer> AGENTS_REPORT_PROCESSING_PERIOD = new ConfigurationProperty<>(
      "agents.reports.processing.period", 1);

  /**
   * Timeout in seconds before start processing of agents' reports.
   */
  @Markdown(description = "Timeout in seconds before start processing of agents' reports.")
  public static final ConfigurationProperty<Integer> AGENTS_REPORT_PROCESSING_START_TIMEOUT = new ConfigurationProperty<>(
      "agents.reports.processing.start.timeout", 5);

  /**
   * Thread pool size for agents reports processing.
   */
  @Markdown(description = "Thread pool size for agents reports processing.")
  public static final ConfigurationProperty<Integer> AGENTS_REPORT_THREAD_POOL_SIZE = new ConfigurationProperty<>(
      "agents.reports.thread.pool.size", 10);

  /**
   * Server to API STOMP endpoint heartbeat interval in milliseconds.
   */
  @Markdown(description = "Server to API STOMP endpoint heartbeat interval in milliseconds.")
  public static final ConfigurationProperty<Integer> API_HEARTBEAT_INTERVAL = new ConfigurationProperty<>(
      "api.heartbeat.interval", 10000);

  /**
   * The maximum size of an incoming stomp text message. Default is 2 MB.
   */
  @Markdown(description = "The maximum size of an incoming stomp text message. Default is 2 MB.")
  public static final ConfigurationProperty<Integer> STOMP_MAX_INCOMING_MESSAGE_SIZE = new ConfigurationProperty<>(
      "stomp.max_incoming.message.size", 2*1024*1024);

  /**
   * The maximum size of a buffer for stomp message sending. Default is 5 MB.
   */
  @Markdown(description = "The maximum size of a buffer for stomp message sending. Default is 5 MB.")
  public static final ConfigurationProperty<Integer> STOMP_MAX_BUFFER_MESSAGE_SIZE = new ConfigurationProperty<>(
      "stomp.max_buffer.message.size", 5*1024*1024);

  /**
   * The number of attempts to emit execution command message to agent. Default is 4
   */
  @Markdown(description = "The number of attempts to emit execution command message to agent. Default is 4")
  public static final ConfigurationProperty<Integer> EXECUTION_COMMANDS_RETRY_COUNT = new ConfigurationProperty<>(
      "execution.command.retry.count", 4);

  /**
   * The interval in seconds between attempts to emit execution command message to agent. Default is 15
   */
  @Markdown(description = "The interval in seconds between attempts to emit execution command message to agent. Default is 15")
  public static final ConfigurationProperty<Integer> EXECUTION_COMMANDS_RETRY_INTERVAL = new ConfigurationProperty<>(
      "execution.command.retry.interval", 15);

  /**
   * The maximum number of threads used to extract Ambari Views when Ambari
   * Server is starting up.
   */
  @Markdown(description = "The maximum number of threads used to extract Ambari Views when Ambari Server is starting up.")
  public static final ConfigurationProperty<Integer> VIEW_EXTRACTION_THREADPOOL_MAX_SIZE = new ConfigurationProperty<>(
      "view.extraction.threadpool.size.max", 20);

  /**
   * The number of threads used to extract Ambari Views when Ambari Server is
   * starting up.
   */
  @Markdown(description = "The number of threads used to extract Ambari Views when Ambari Server is starting up.")
  public static final ConfigurationProperty<Integer> VIEW_EXTRACTION_THREADPOOL_CORE_SIZE = new ConfigurationProperty<>(
      "view.extraction.threadpool.size.core", 10);

  /**
   * The time, in {@link TimeUnit#MILLISECONDS}, that non-core threads will live
   * when extraction views on Ambari Server startup.
   */
  @Markdown(description = "The time, in milliseconds, that non-core threads will live when extraction views on Ambari Server startup.")
  public static final ConfigurationProperty<Long> VIEW_EXTRACTION_THREADPOOL_TIMEOUT = new ConfigurationProperty<>(
      "view.extraction.threadpool.timeout", 100000L);

  /**
   * The maximum number of threads which will be allocated to handling REST API
   * requests from embedded views. This value should be smaller than
   * {@link #AGENT_THREADPOOL_SIZE}.
   */
  @Markdown(
      relatedTo = "agent.threadpool.size.max",
      description = "The maximum number of threads which will be allocated to handling REST API requests from embedded views. This value should be smaller than `agent.threadpool.size.max`")
  public static final ConfigurationProperty<Integer> VIEW_REQUEST_THREADPOOL_MAX_SIZE = new ConfigurationProperty<>(
      "view.request.threadpool.size.max", 0);

  /**
   * The time, in {@link TimeUnit#MILLISECONDS}, that REST API requests from
   * embedded views can wait if the view threadpool size is currently exhausted.
   * Setting this value too low can result in errors loading views.
   */
  @Markdown(description = "The time, milliseconds, that REST API requests from embedded views can wait if there are no threads available to service the view's request. "
      + "Setting this too low can cause views to timeout.")
  public static final ConfigurationProperty<Integer> VIEW_REQUEST_THREADPOOL_TIMEOUT = new ConfigurationProperty<>(
      "view.request.threadpool.timeout", 2000);

  /**
   * The maximum number of threads that will be used to retrieve data from
   * {@link PropertyProvider}s such as remote JMX endpoints.
   */
  @Markdown(description = "The maximum number of threads that will be used to retrieve data from federated datasources, such as remote JMX endpoints.")
  public static final ConfigurationProperty<Integer> PROPERTY_PROVIDER_THREADPOOL_MAX_SIZE = new ConfigurationProperty<>(
      "server.property-provider.threadpool.size.max", PROCESSOR_BASED_THREADPOOL_MAX_SIZE_DEFAULT);

  /**
   * The core number of threads that will be used to retrieve data from
   * {@link PropertyProvider}s, such as remote JMX endpoints.
   */
  @Markdown(description = "The core number of threads that will be used to retrieve data from federated datasources, such as remote JMX endpoints.")
  public static final ConfigurationProperty<Integer> PROPERTY_PROVIDER_THREADPOOL_CORE_SIZE = new ConfigurationProperty<>(
      "server.property-provider.threadpool.size.core",
      PROCESSOR_BASED_THREADPOOL_CORE_SIZE_DEFAULT);

  /**
   * The maximum size of pending {@link PropertyProvider} requests which can be
   * queued before rejecting new requests.
   */
  @Markdown(description = "The maximum size of pending federated datasource requests, such as those to JMX endpoints, which can be queued before rejecting new requests.")
  public static final ConfigurationProperty<Integer> PROPERTY_PROVIDER_THREADPOOL_WORKER_QUEUE_SIZE = new ConfigurationProperty<>(
      "server.property-provider.threadpool.worker.size", Integer.MAX_VALUE);

  /**
   * The maximum time, in {@link TimeUnit#MILLISECONDS}, that a synchronous
   * request to a {@link PropertyProvider} can run before being terminated.
   */
  @Markdown(description = "The maximum time, in milliseconds, that federated requests for data can execute before being terminated. "
      + "Increasing this value could result in degraded performanc from the REST APIs.")
  public static final ConfigurationProperty<Long> PROPERTY_PROVIDER_THREADPOOL_COMPLETION_TIMEOUT = new ConfigurationProperty<>(
      "server.property-provider.threadpool.completion.timeout", 5000L);

  /**
   * The time, in {@link TimeUnit#SECONDS}, that HTTP requests remain valid when
   * inactive.
   */
  @Markdown(description = "The time, in seconds, that open HTTP sessions will remain valid while they are inactive.")
  public static final ConfigurationProperty<Integer> SERVER_HTTP_SESSION_INACTIVE_TIMEOUT = new ConfigurationProperty<>(
      "server.http.session.inactive_timeout", 1800);

  /**
   * Determines whether Ambari Metric data is cached.
   */
  @Markdown(description = "Determines whether Ambari Metric data is cached.")
  public static final ConfigurationProperty<Boolean> TIMELINE_METRICS_CACHE_DISABLE = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.disabled", Boolean.FALSE);

  /**
   * The time, in {@link TimeUnit#SECONDS}, that Ambari Metric timeline data is cached by Ambari Server.
   */
  @Markdown(
      relatedTo = "server.timeline.metrics.cache.disabled",
      description = "The time, in seconds, that Ambari Metric timeline data is cached by Ambari Server.")
  public static final ConfigurationProperty<Integer> TIMELINE_METRICS_CACHE_TTL = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.entry.ttl.seconds", 3600);

  /**
   * The time, in {@link TimeUnit#SECONDS}, that Ambari Metric data can remain in the cache without being accessed.
   */
  @Markdown(
      relatedTo = "server.timeline.metrics.cache.disabled",
      description = "The time, in seconds, that Ambari Metric data can remain in the cache without being accessed.")
  public static final ConfigurationProperty<Integer> TIMELINE_METRICS_CACHE_IDLE_TIME = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.entry.idle.seconds", 1800);

  /**
   * The time, in {@link TimeUnit#MILLISECONDS}, that initial requests made to
   * Ambari Metrics will wait while reading from the socket before timing out.
   */
  @Markdown(
      relatedTo = "server.timeline.metrics.cache.disabled",
      description = "The time, in milliseconds, that initial requests to populate metric data will wait while reading from Ambari Metrics.")
  public static final ConfigurationProperty<Integer> TIMELINE_METRICS_REQUEST_READ_TIMEOUT = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.read.timeout.millis", 10000);

  /**
   * The time, in {@link TimeUnit#MILLISECONDS}, that cache update requests made to
   * Ambari Metrics will wait while reading from the socket before timing out.
   */
  @Markdown(
      relatedTo = "server.timeline.metrics.cache.disabled",
      description = "The time, in milliseconds, that requests to update stale metric data will wait while reading from Ambari Metrics. "
          + "This allows for greater control by allowing stale values to be returned instead of waiting for Ambari Metrics to always populate responses with the latest data.")
  public static final ConfigurationProperty<Integer> TIMELINE_METRICS_REQUEST_INTERVAL_READ_TIMEOUT = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.interval.read.timeout.millis", 10000);

  /**
   * The time, in {@link TimeUnit#MILLISECONDS}, to wait while attempting to connect to Ambari Metrics.
   */
  @Markdown(
      relatedTo = "server.timeline.metrics.cache.disabled",
      description = "The time, in milliseconds, to wait while attempting to connect to Ambari Metrics.")
  public static final ConfigurationProperty<Integer> TIMELINE_METRICS_REQUEST_CONNECT_TIMEOUT = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.connect.timeout.millis", 5000);

  /**
   * The time, in {@link TimeUnit#MILLISECONDS}, that Ambari Metrics intervals should use when
   * extending the boundaries of the original request.
   */
  @Markdown(
      relatedTo = "server.timeline.metrics.cache.disabled",
      description = "The time, in milliseconds, that Ambari Metrics intervals should use when extending the boundaries of the original request.")
  public static final ConfigurationProperty<Long> TIMELINE_METRICS_REQUEST_CATCHUP_INTERVAL = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.catchup.interval", 300000L);

  /**
   * The amount of heap on the Ambari Server dedicated to the caching values from Ambari Metrics.
   */
  @Markdown(
      relatedTo = "server.timeline.metrics.cache.disabled",
      description = "The amount of heap on the Ambari Server dedicated to the caching values from Ambari Metrics. Measured as part of the total heap of Ambari Server.")
  public static final ConfigurationProperty<String> TIMELINE_METRICS_CACHE_HEAP_PERCENT = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.heap.percent", "15%");

  /**
   * Determines if a custom engine should be used to increase performance of
   * calculating the current size of the cache for Ambari Metric data.
   */
  @Markdown(
      relatedTo = "server.timeline.metrics.cache.disabled",
      description = "Determines if a custom engine should be used to increase performance of calculating the current size of the cache for Ambari Metric data.")
  public static final ConfigurationProperty<Boolean> TIMELINE_METRICS_CACHE_USE_CUSTOM_SIZING_ENGINE = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.use.custom.sizing.engine", Boolean.TRUE);

  /**
   * Timeline Metrics SSL settings
   */
  @Markdown(description = "Determines whether to use to SSL to connect to Ambari Metrics when retrieving metric data.")
  public static final ConfigurationProperty<Boolean> AMBARI_METRICS_HTTPS_ENABLED = new ConfigurationProperty<>(
      "server.timeline.metrics.https.enabled", Boolean.FALSE);

  /**
   * The full path to the XML file that describes the different alert templates.
   */
  @Markdown(description="The full path to the XML file that describes the different alert templates.")
  public static final ConfigurationProperty<String> ALERT_TEMPLATE_FILE = new ConfigurationProperty<>(
      "alerts.template.file", null);

  /**
   * The core number of threads which will handle published alert events.
   */
  @ConfigurationMarkdown(
      group = ConfigurationGrouping.ALERTS,
      scaleValues = {
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_10, value = "2"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_50, value = "2"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_100, value = "4"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_500, value = "4") },
      markdown = @Markdown(
          description = "The core number of threads used to process incoming alert events. The value should be increased as the size of the cluster increases."))
  public static final ConfigurationProperty<Integer> ALERTS_EXECUTION_SCHEDULER_THREADS_CORE_SIZE = new ConfigurationProperty<>(
      "alerts.execution.scheduler.threadpool.size.core", 2);

  /**
   * The maximum number of threads which will handle published alert events.
   */
  @ConfigurationMarkdown(
      group = ConfigurationGrouping.ALERTS,
      scaleValues = {
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_10, value = "2"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_50, value = "2"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_100, value = "8"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_500, value = "8") },
      markdown = @Markdown(
          description = "The number of threads used to handle alerts received from the Ambari Agents. The value should be increased as the size of the cluster increases."))
  public static final ConfigurationProperty<Integer> ALERTS_EXECUTION_SCHEDULER_THREADS_MAX_SIZE = new ConfigurationProperty<>(
      "alerts.execution.scheduler.threadpool.size.max", 2);

  /**
   * The size of the {@link BlockingQueue} used to control the
   * {@link ScalingThreadPoolExecutor} when handling incoming alert events.
   */
  @ConfigurationMarkdown(
      group = ConfigurationGrouping.ALERTS,
      scaleValues = {
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_10, value = "400"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_50, value = "2000"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_100, value = "4000"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_500, value = "20000") },
      markdown = @Markdown(
          description = "The number of queued alerts allowed before discarding old alerts which have not been handled. The value should be increased as the size of the cluster increases."))
  public static final ConfigurationProperty<Integer> ALERTS_EXECUTION_SCHEDULER_WORKER_QUEUE_SIZE = new ConfigurationProperty<>(
      "alerts.execution.scheduler.threadpool.worker.size", 2000);

  /**
   * If {@code true} then alert information is cached and not immediately
   * persisted in the database.
   */
  @ConfigurationMarkdown(
      group = ConfigurationGrouping.ALERTS,
      scaleValues = {
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_10, value = "false" ),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_50, value = "false"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_100, value = "false"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_500, value = "true") },
      markdown = @Markdown(
          description = "Determines whether current alerts should be cached. "
              + "Enabling this can increase performance on large cluster, but can also result in lost alert data if the cache is not flushed frequently."))
  public static final ConfigurationProperty<Boolean> ALERTS_CACHE_ENABLED = new ConfigurationProperty<>(
      "alerts.cache.enabled", Boolean.FALSE);

  /**
   * The time after which cached alert information is flushed to the database.
   * Measure in {@link TimeUnit#MINUTES}.
   */
  @ConfigurationMarkdown(
      group = ConfigurationGrouping.ALERTS,
      scaleValues = {
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_10, value = "10" ),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_50, value = "10"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_100, value = "10"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_500, value = "10") },
      markdown = @Markdown(
          relatedTo = "alerts.cache.enabled",
          description = "The time, in minutes, after which cached alert information is flushed to the database"))
  public static final ConfigurationProperty<Integer> ALERTS_CACHE_FLUSH_INTERVAL = new ConfigurationProperty<>(
      "alerts.cache.flush.interval", 10);

  /**
   * The size of the alert cache.
   */
  @ConfigurationMarkdown(
      group = ConfigurationGrouping.ALERTS,
      scaleValues = {
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_10, value = "50000" ),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_50, value = "50000"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_100, value = "100000"),
          @ClusterScale(clusterSize = ClusterSizeType.HOSTS_500, value = "100000") },
      markdown = @Markdown(
          relatedTo = "alerts.cache.enabled",
          description = "The size of the alert cache."))
  public static final ConfigurationProperty<Integer> ALERTS_CACHE_SIZE = new ConfigurationProperty<>(
      "alerts.cache.size", 50000);

  /**
   * When using SSL, this will be used to set the {@code Strict-Transport-Security} response header.
   */
  @Markdown(description = "When using SSL, this will be used to set the `Strict-Transport-Security` response header.")
  public static final ConfigurationProperty<String> HTTP_STRICT_TRANSPORT_HEADER_VALUE = new ConfigurationProperty<>(
      "http.strict-transport-security", "max-age=31536000");

  /**
   * The value that will be used to set the {@code X-Frame-Options} HTTP response header.
   */
  @Markdown(description = "The value that will be used to set the `X-Frame-Options` HTTP response header.")
  public static final ConfigurationProperty<String> HTTP_X_FRAME_OPTIONS_HEADER_VALUE = new ConfigurationProperty<>(
      "http.x-frame-options", "DENY");

  /**
   * The value that will be used to set the {@code X-XSS-Protection} HTTP response header.
   */
  @Markdown(description = "The value that will be used to set the `X-XSS-Protection` HTTP response header.")
  public static final ConfigurationProperty<String> HTTP_X_XSS_PROTECTION_HEADER_VALUE = new ConfigurationProperty<>(
      "http.x-xss-protection", "1; mode=block");

  /**
   * The value that will be used to set the {@code X-Content-Type} HTTP response header.
   */
  @Markdown(description = "The value that will be used to set the `X-CONTENT-TYPE` HTTP response header.")
  public static final ConfigurationProperty<String> HTTP_X_CONTENT_TYPE_HEADER_VALUE = new ConfigurationProperty<>(
      "http.x-content-type-options", "nosniff");

  /**
   * The value that will be used to set the {@code Cache-Control} HTTP response header.
   */
  @Markdown(description = "The value that will be used to set the `Cache-Control` HTTP response header.")
  public static final ConfigurationProperty<String> HTTP_CACHE_CONTROL_HEADER_VALUE = new ConfigurationProperty<>(
      "http.cache-control", "no-store");

  /**
   * The value that will be used to set the {@code PRAGMA} HTTP response header.
   */
  @Markdown(description = "The value that will be used to set the `PRAGMA` HTTP response header.")
  public static final ConfigurationProperty<String> HTTP_PRAGMA_HEADER_VALUE = new ConfigurationProperty<>(
      "http.pragma", "no-cache");

   /**
   * The value that will be used to set the {@code Charset} HTTP response header.
   */
  @Markdown(description = "The value that will be used to set the Character encoding to HTTP response header.")
  public static final ConfigurationProperty<String> HTTP_CHARSET = new ConfigurationProperty<>(
      "http.charset", "utf-8");


  /**
   * The value that will be used to set the {@code Strict-Transport-Security}
   * HTTP response header for Ambari View requests.
   */
  @Markdown(description = "The value that will be used to set the `Strict-Transport-Security` HTTP response header for Ambari View requests.")
  public static final ConfigurationProperty<String> VIEWS_HTTP_STRICT_TRANSPORT_HEADER_VALUE = new ConfigurationProperty<>(
      "views.http.strict-transport-security", "max-age=31536000");

  /**
   * The value that will be used to set the {@code X-Frame-Options}
   * HTTP response header for Ambari View requests.
   *
   */
  @Markdown(description = "The value that will be used to set the `X-Frame-Options` HTTP response header for Ambari View requests.")
  public static final ConfigurationProperty<String> VIEWS_HTTP_X_FRAME_OPTIONS_HEADER_VALUE = new ConfigurationProperty<>(
      "views.http.x-frame-options", "SAMEORIGIN");

  /**
   * The value that will be used to set the {@code X-XSS-Protection}
   * HTTP response header for Ambari View requests.
   */
  @Markdown(description = "The value that will be used to set the `X-XSS-Protection` HTTP response header for Ambari View requests.")
  public static final ConfigurationProperty<String> VIEWS_HTTP_X_XSS_PROTECTION_HEADER_VALUE = new ConfigurationProperty<>(
      "views.http.x-xss-protection", "1; mode=block");

  /**
   * The value that will be used to set the {@code X-Content-Type} HTTP response header.
   * HTTP response header for Ambari View requests.
   */
  @Markdown(description = "The value that will be used to set the `X-CONTENT-TYPE` HTTP response header for Ambari View requests.")
  public static final ConfigurationProperty<String> VIEWS_HTTP_X_CONTENT_TYPE_HEADER_VALUE = new ConfigurationProperty<>(
      "views.http.x-content-type-options", "nosniff");

  /**
   * The value that will be used to set the {@code Cache-Control} HTTP response header.
   * HTTP response header for Ambari View requests.
   */
  @Markdown(description = "The value that will be used to set the `Cache-Control` HTTP response header for Ambari View requests.")
  public static final ConfigurationProperty<String> VIEWS_HTTP_CACHE_CONTROL_HEADER_VALUE = new ConfigurationProperty<>(
      "views.http.cache-control", "no-store");

  /**
   * The value that is additional classpath for the views. It will take comma separated paths. If the individual path is jar
   * it will be included otherwise if it is a directory then all the files inside it will be included in the classpath. Directories
   * WILL NOT BE traversed recursively
   */
  @Markdown(description = "Additional class path added to each Ambari View. Comma separated jars or directories")
  public static final ConfigurationProperty<String> VIEWS_ADDITIONAL_CLASSPATH_VALUE = new ConfigurationProperty<>(
      "views.additional.classpath", "");

  /**
   * The value that will be used to set the {@code PRAGMA} HTTP response header.
   * HTTP response header for Ambari View requests.
   */
  @Markdown(description = "The value that will be used to set the `PRAGMA` HTTP response header for Ambari View requests.")
  public static final ConfigurationProperty<String> VIEWS_HTTP_PRAGMA_HEADER_VALUE = new ConfigurationProperty<>(
      "views.http.pragma", "no-cache");

   /**
   * The value that will be used to set the {@code CHARSET} to HTTP response header.
   */
  @Markdown(description = "The value that will be used to set the Character encoding to HTTP response header for Ambari View requests.")
  public static final ConfigurationProperty<String> VIEWS_HTTP_CHARSET = new ConfigurationProperty<>(
      "views.http.charset", "utf-8");


  /**
   * The time, in milliseconds, that requests to connect to a URL to retrieve
   * Version Definition Files (VDF) will wait before being terminated.
   */
  @Markdown(description = "The time, in milliseconds, that requests to connect to a URL to retrieve Version Definition Files (VDF) will wait before being terminated.")
  public static final ConfigurationProperty<Integer> VERSION_DEFINITION_CONNECT_TIMEOUT = new ConfigurationProperty<>(
      "server.version_definition.connect.timeout.millis", 5000);

  /**
   * The time, in milliseconds, that requests to read from a connected URL to retrieve
   * Version Definition Files (VDF) will wait before being terminated.
   */
  @Markdown(description = "The time, in milliseconds, that requests to read from a connected URL to retrieve Version Definition Files (VDF) will wait before being terminated.")
  public static final ConfigurationProperty<Integer> VERSION_DEFINITION_READ_TIMEOUT = new ConfigurationProperty<>(
      "server.version_definition.read.timeout.millis", 5000);

  /**
   * Determines whether agents should retrying installation commands when the
   * repository is not available.
   */
  @Markdown(description = "Determines whether agents should retrying installation commands when the repository is not available. "
      + "This can prevent false installation errors with repositories that are sporadically inaccessible.")
  public static final ConfigurationProperty<Boolean> AGENT_STACK_RETRY_ON_REPO_UNAVAILABILITY = new ConfigurationProperty<>(
      "agent.stack.retry.on_repo_unavailability", Boolean.FALSE);

  /**
   * The number of times an Ambari Agent should retry package installation when it fails due to a repository error.
   */
  @Markdown(
      relatedTo = "agent.stack.retry.on_repo_unavailability",
      description = "The number of times an Ambari Agent should retry package installation when it fails due to a repository error. ")
  public static final ConfigurationProperty<Integer> AGENT_STACK_RETRY_COUNT = new ConfigurationProperty<>(
      "agent.stack.retry.tries", 5);

  /**
   * Determines whether audit logging is enabled.
   */
  @Markdown(description = "Determines whether audit logging is enabled.")
  public static final ConfigurationProperty<Boolean> AUDIT_LOG_ENABLED = new ConfigurationProperty<>(
      "auditlog.enabled", Boolean.TRUE);

  /**
   * The size of the worker queue for audit logger events.
   */
  @Markdown(
      relatedTo = "auditlog.enabled",
      description = "The size of the worker queue for audit logger events.")
  public static final ConfigurationProperty<Integer> AUDIT_LOGGER_CAPACITY = new ConfigurationProperty<>(
      "auditlog.logger.capacity", 10000);

  /**
   * The UDP port to use when binding the SNMP dispatcher on Ambari Server startup.
   */
  @Markdown(description = "The UDP port to use when binding the SNMP dispatcher on Ambari Server startup. If no port is specified, then a random port will be used.")
  public static final ConfigurationProperty<String> ALERTS_SNMP_DISPATCH_UDP_PORT = new ConfigurationProperty<>(
      "alerts.snmp.dispatcher.udp.port", null);

  /**
   * The UDP port to use when binding the Ambari SNMP dispatcher on Ambari Server startup.
   */
  @Markdown(description = "The UDP port to use when binding the Ambari SNMP dispatcher on Ambari Server startup. If no port is specified, then a random port will be used.")
  public static final ConfigurationProperty<String> ALERTS_AMBARI_SNMP_DISPATCH_UDP_PORT = new ConfigurationProperty<>(
          "alerts.ambari.snmp.dispatcher.udp.port", null);

  /**
   * The amount of time, in {@link TimeUnit#MINUTES}, that the
   * {@link MetricsRetrievalService} will cache retrieved metric data.
   */
  @Markdown(description = "The amount of time, in minutes, that JMX and REST metrics retrieved directly can remain in the cache.")
  public static final ConfigurationProperty<Integer> METRIC_RETRIEVAL_SERVICE_CACHE_TIMEOUT = new ConfigurationProperty<>(
      "metrics.retrieval-service.cache.timeout", 30);

  /**
   * The priorty of the {@link Thread}s used by the
   * {@link MetricsRetrievalService}. This is a value in between
   * {@link Thread#MIN_PRIORITY} and {@link Thread#MAX_PRIORITY}.
   */
  @Markdown(description = "The priority of threads used by the service which retrieves JMX and REST metrics directly from their respective endpoints.")
  public static final ConfigurationProperty<Integer> METRIC_RETRIEVAL_SERVICE_THREAD_PRIORITY = new ConfigurationProperty<>(
      "server.metrics.retrieval-service.thread.priority", Thread.NORM_PRIORITY);

  /**
   * The maximum size of the threadpool for the {@link MetricsRetrievalService}.
   * This value is only applicable if the
   * {@link #METRIC_RETRIEVAL_SERVICE_THREADPOOL_WORKER_QUEUE_SIZE} is small
   * enough to trigger the {@link ThreadPoolExecutor} to create new threads.
   */
  @Markdown(description = "The maximum number of threads used to retrieve JMX and REST metrics directly from their respective endpoints.")
  public static final ConfigurationProperty<Integer> METRIC_RETRIEVAL_SERVICE_THREADPOOL_MAX_SIZE = new ConfigurationProperty<>(
      "server.metrics.retrieval-service.threadpool.size.max",
      PROCESSOR_BASED_THREADPOOL_MAX_SIZE_DEFAULT);

  /**
   * The core size of the threadpool for the {@link MetricsRetrievalService}.
   */
  @Markdown(description = "The core number of threads used to retrieve JMX and REST metrics directly from their respective endpoints.")
  public static final ConfigurationProperty<Integer> METRIC_RETRIEVAL_SERVICE_THREADPOOL_CORE_SIZE = new ConfigurationProperty<>(
      "server.metrics.retrieval-service.threadpool.size.core",
      PROCESSOR_BASED_THREADPOOL_CORE_SIZE_DEFAULT);

  /**
   * The size of the worker queue for the {@link MetricsRetrievalService}. The
   * larger this queue is, the less likely it will be to create more threads
   * beyond the core size.
   */
  @Markdown(description = "The number of queued requests allowed for JMX and REST metrics before discarding old requests which have not been fullfilled.")
  public static final ConfigurationProperty<Integer> METRIC_RETRIEVAL_SERVICE_THREADPOOL_WORKER_QUEUE_SIZE = new ConfigurationProperty<>(
      "server.metrics.retrieval-service.threadpool.worker.size",
      10 * METRIC_RETRIEVAL_SERVICE_THREADPOOL_MAX_SIZE.getDefaultValue());

  /**
   * {@code true} to enable a TTL per request made by the
   * {@link MetricsRetrievalService}. Enabling this property will prevent
   * requests to the same URL endpoint within a fixed amount of time allowing
   * requests to be throttled.
   */
  @Markdown(
      relatedTo = "metrics.retrieval-service.request.ttl",
      description = "Enables throttling requests to the same endpoint within a fixed amount of time. "
          + "This property will prevent Ambari from making new metric requests to update the cache for URLs which have been recently retrieved.")
  public static final ConfigurationProperty<Boolean> METRIC_RETRIEVAL_SERVICE_REQUEST_TTL_ENABLED = new ConfigurationProperty<>(
      "metrics.retrieval-service.request.ttl.enabled", Boolean.TRUE);

  /**
   * The amount of time, in {@link TimeUnit#SECONDS}, that requests to the same
   * URL by the {@link MetricsRetrievalService} must be separated. Requests to
   * the same URL which are too close together will not result in metrics
   * retrieval. This property is used to throttle requests to the same URL being
   * made too close together.
   */
  @Markdown(
      relatedTo = "metrics.retrieval-service.request.ttl.enabled",
      description = "The number of seconds to wait between issuing JMX or REST metric requests to the same endpoint. "
          + "This property is used to throttle requests to the same URL being made too close together")
  public static final ConfigurationProperty<Integer> METRIC_RETRIEVAL_SERVICE_REQUEST_TTL = new ConfigurationProperty<>(
      "metrics.retrieval-service.request.ttl", 5);

  /**
   * The number of tasks that can be queried from the database at once In the
   * case of more tasks, multiple queries are issued
   *
   * @return
   */
  @Markdown(description = "The maximum number of tasks which can be queried by ID from the database.")
  public static final ConfigurationProperty<Integer> TASK_ID_LIST_LIMIT = new ConfigurationProperty<>(
      "task.query.parameterlist.size", 999);

  /**
   * Indicates whether the current ambari server instance is the active instance.
   * If this property is missing, the value will be considered to be true.
   * If present, it should be explicitly set to "true" to set this as the active instance.
   * Any other value will be taken as a false.
   */
  @Markdown(description = "Indicates whether the current ambari server instance is active or not.")
  public static final ConfigurationProperty<Boolean> ACTIVE_INSTANCE = new ConfigurationProperty<>(
          "active.instance", Boolean.TRUE);

  @Markdown(description = "Indicates whether the post user creation is enabled or not. By default is false.")
  public static final ConfigurationProperty<Boolean> POST_USER_CREATION_HOOK_ENABLED = new ConfigurationProperty<>(
      "ambari.post.user.creation.hook.enabled", Boolean.FALSE);

  @Markdown(description = "The location of the post user creation hook on the ambari server hosting machine.")
  public static final ConfigurationProperty<String> POST_USER_CREATION_HOOK = new ConfigurationProperty<>(
      "ambari.post.user.creation.hook", "/var/lib/ambari-server/resources/scripts/post-user-creation-hook.sh");

  /**
   * PropertyConfigurator checks log4j.properties file change every LOG4JMONITOR_DELAY milliseconds.
   */
  @Markdown(description = "Indicates the delay, in milliseconds, for the log4j monitor to check for changes")
  public static final ConfigurationProperty<Long> LOG4JMONITOR_DELAY = new ConfigurationProperty<>(
          "log4j.monitor.delay", TimeUnit.MINUTES.toMillis(5));

  /**
   * Indicates whether parallel topology task creation is enabled for blueprint cluster provisioning.
   * Defaults to <code>false</code>.
   * @see #TOPOLOGY_TASK_PARALLEL_CREATION_THREAD_COUNT
   */
  @Markdown(description = "Indicates whether parallel topology task creation is enabled")
  public static final ConfigurationProperty<Boolean> TOPOLOGY_TASK_PARALLEL_CREATION_ENABLED = new ConfigurationProperty<>("topology.task.creation.parallel", Boolean.FALSE);

  /**
   * The number of threads to use for parallel topology task creation in blueprint cluster provisioning if enabled.
   * Defaults to 10.
   * @see #TOPOLOGY_TASK_PARALLEL_CREATION_ENABLED
   */
  @Markdown(description = "The number of threads to use for parallel topology task creation if enabled")
  public static final ConfigurationProperty<Integer> TOPOLOGY_TASK_PARALLEL_CREATION_THREAD_COUNT = new ConfigurationProperty<>("topology.task.creation.parallel.threads", 10);

  /**
   * The number of acceptor threads for the agent jetty connector.
   */
  @Markdown(description = "Count of acceptors to configure for the jetty connector used for Ambari agent.")
  public static final ConfigurationProperty<Integer> SRVR_AGENT_ACCEPTOR_THREAD_COUNT = new ConfigurationProperty<>(
      "agent.api.acceptor.count", null);

  /**
   * The number of acceptor threads for the api jetty connector.
   */
  @Markdown(description = "Count of acceptors to configure for the jetty connector used for Ambari API.")
  public static final ConfigurationProperty<Integer> SRVR_API_ACCEPTOR_THREAD_COUNT = new ConfigurationProperty<>(
      "client.api.acceptor.count", null);

  /**
   * The time, in milliseconds, that the Ambari Server will wait while attempting to connect to the
   * LogSearch Portal service.
   */
  @Markdown(description = "The time, in milliseconds, that the Ambari Server will wait while attempting to connect to the LogSearch Portal service.")
  public static final ConfigurationProperty<Integer> LOGSEARCH_PORTAL_CONNECT_TIMEOUT = new ConfigurationProperty<>(
          "logsearch.portal.connect.timeout", 5000);

  /**
   * The time, in milliseconds, that the Ambari Server will wait while attempting to read a response from the
   * LogSearch Portal service.
   */
  @Markdown(description = "The time, in milliseconds, that the Ambari Server will wait while attempting to read a response from the LogSearch Portal service.")
  public static final ConfigurationProperty<Integer> LOGSEARCH_PORTAL_READ_TIMEOUT = new ConfigurationProperty<>(
    "logsearch.portal.read.timeout", 5000);

  /**
   * External logsearch portal address, can be used with internal logfeeder, as the same logsearch portal can store logs for different clusters
   */
  @Markdown(description = "Address of an external LogSearch Portal service. (managed outside of Ambari) Using Ambari Credential store is required for this feature (credential: 'logsearch.admin.credential')")
  public static final ConfigurationProperty<String> LOGSEARCH_PORTAL_EXTERNAL_ADDRESS = new ConfigurationProperty<>(
    "logsearch.portal.external.address", "");

  /**
   * Global disable flag for AmbariServer Metrics.
   */
  @Markdown(description = "Global disable flag for AmbariServer Metrics.")
  public static final ConfigurationProperty<Boolean> AMBARISERVER_METRICS_DISABLE = new ConfigurationProperty<>(
    "ambariserver.metrics.disable", false);

  /**
   * The time, in hours, that the Ambari Server will hold Log File metadata in its internal cache before making
   *   a request to the LogSearch Portal to get the latest metadata.
   *
   * The logging metadata (in this case, log file names) is generally quite static, so the default should
   *   generally be quite long.
   *
   */
  @Markdown(description = "The time, in hours, that the Ambari Server will hold Log File metadata in its internal cache before making a request to the LogSearch Portal to get the latest metadata.")
  public static final ConfigurationProperty<Integer> LOGSEARCH_METADATA_CACHE_EXPIRE_TIMEOUT = new ConfigurationProperty<>(
    "logsearch.metadata.cache.expire.timeout", 24);

  /**
   * The time, in seconds, that the ambari-server Python script will wait for
   * Jetty to startup before returning an error code.
   */
  @Markdown(description = "The time, in seconds, that the ambari-server Python script will wait for Jetty to startup before returning an error code.")
  public static final ConfigurationProperty<Integer> SERVER_STARTUP_WEB_TIMEOUT = new ConfigurationProperty<>(
    "server.startup.web.timeout", 50);

  /**
   * The Ephemeral TLS Diffie-Hellman (DH) key size.
   * Supported from Java 8.
   */
  @Markdown(description = "The Ephemeral TLS Diffie-Hellman (DH) key size. Supported from Java 8.")
  public static final ConfigurationProperty<Integer> TLS_EPHEMERAL_DH_KEY_SIZE = new ConfigurationProperty<>(
    "security.server.tls.ephemeral_dh_key_size", 2048);

  /**
   * The directory for scripts which are used by the alert notification dispatcher.
   */
  @Markdown(description = "The directory for scripts which are used by the alert notification dispatcher.")
  public static final ConfigurationProperty<String> DISPATCH_PROPERTY_SCRIPT_DIRECTORY = new ConfigurationProperty<>(
          "notification.dispatch.alert.script.directory",AmbariPath.getPath("/var/lib/ambari-server/resources/scripts"));

  @Markdown(description = "Whether security password encryption is enabled or not. In case it is we store passwords in their own file(s); otherwise we store passwords in the Ambari credential store.")
  public static final ConfigurationProperty<Boolean> SECURITY_PASSWORD_ENCRYPTON_ENABLED = new ConfigurationProperty<>("security.passwords.encryption.enabled", false);

  @Markdown(description="Whether to encrypt sensitive data (at rest) on service level configuration.")
  public static final ConfigurationProperty<Boolean> SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED = new ConfigurationProperty<>("security.server.encrypt_sensitive_data", false);

  /**
   * The maximum number of authentication attempts permitted to a local user. Once the number of failures reaches this limit the user will be locked out. 0 indicates unlimited failures
   */
  @Markdown(description = "The maximum number of authentication attempts permitted to a local user. Once the number of failures reaches this limit the user will be locked out. 0 indicates unlimited failures.")
  public static final ConfigurationProperty<Integer> MAX_LOCAL_AUTHENTICATION_FAILURES = new ConfigurationProperty<>(
    "authentication.local.max.failures", 0);

  /**
   * A flag to determine whether locked out messages are to be shown to users, if relevant, when authenticating into Ambari
   */
  @Markdown(description = "Show or hide whether the user account is disabled or locked out, if relevant, when an authentication attempt fails.")
  public static final ConfigurationProperty<String> SHOW_LOCKED_OUT_USER_MESSAGE = new ConfigurationProperty<>(
    "authentication.local.show.locked.account.messages", "false");

  /**
   * The core pool size of the executor service that runs server side alerts.
   */
  @Markdown(description = "The core pool size of the executor service that runs server side alerts.")
  public static final ConfigurationProperty<Integer> SERVER_SIDE_ALERTS_CORE_POOL_SIZE = new ConfigurationProperty<>(
          "alerts.server.side.scheduler.threadpool.size.core", 4);

  /**
   * Default value of Max number of tasks to schedule in parallel for upgrades.
   */
  @Markdown(description = "Default value of max number of tasks to schedule in parallel for upgrades. Upgrade packs can override this value.")
  public static final ConfigurationProperty<Integer> DEFAULT_MAX_DEGREE_OF_PARALLELISM_FOR_UPGRADES = new ConfigurationProperty<>(
    "stack.upgrade.default.parallelism", 100);

  /**
   * Fully qualified class name of the strategy used to form host groups for add service request layout recommendation.
   */
  @Markdown(description = "Fully qualified class name of the strategy used to form host groups for add service request layout recommendation.")
  public static final ConfigurationProperty<String> ADD_SERVICE_HOST_GROUP_STRATEGY = new ConfigurationProperty<>(
    "addservice.hostgroup.strategy", GroupByComponentsStrategy.class.getName());

  /**
   * Gets whether file-based VDF are allowed to be used.
   */
  @Markdown(description = "Controls whether VDF can be read from the filesystem.")
  public static final ConfigurationProperty<Boolean> VDF_FROM_FILESYSTEM = new ConfigurationProperty<>(
      "server.version_definition.allow_from_filesystem", Boolean.FALSE);

  private static final Logger LOG = LoggerFactory.getLogger(
    Configuration.class);

  private Properties properties;
  private Properties log4jProperties = new Properties();
  private Set<String> propertiesToMask = null;
  private String ambariUpgradeConfigUpdatesFilePath;
  private JsonObject hostChangesJson;
  private Map<String, String> configsMap;
  private Map<String, Map<String,String>> agentConfigsMap;
  private Properties customDbProperties = null;
  private Properties customPersistenceProperties = null;
  private Long configLastModifiedDateForCustomJDBC = 0L;
  private Long configLastModifiedDateForCustomJDBCToRemove = 0L;
  private Map<String, String> databaseConnectorNames = new HashMap<>();
  private Map<String, String> databasePreviousConnectorNames = new HashMap<>();

  /**
   * The Kerberos authentication-specific properties container (for convenience)
   */
  private final AmbariKerberosAuthenticationProperties kerberosAuthenticationProperties;

  static {
    if (System.getProperty("os.name").contains("Windows")) {
      DEF_ARCHIVE_EXTENSION = ".zip";
      DEF_ARCHIVE_CONTENT_TYPE = "application/zip";
    }
    else {
      DEF_ARCHIVE_EXTENSION = ".tar.gz";
      DEF_ARCHIVE_CONTENT_TYPE = "application/x-ustar";
    }
  }

  /**
   * Ldap username collision handling behavior.
   * ADD - append the new LDAP entry to the set of existing authentication methods.
   * CONVERT - remove all authentication methods except for the new LDAP entry.
   * SKIP - skip existing local users.
   */
  public enum LdapUsernameCollisionHandlingBehavior {
    ADD,
    CONVERT,
    SKIP;

    /**
     * Safely translates a user-supplied behavior name to a {@link LdapUsernameCollisionHandlingBehavior}.
     * <p>
     * If the user-supplied value is empty or invalid, the default value is returned.
     *
     * @param value        a user-supplied behavior name value
     * @param defaultValue the default value
     * @return a {@link LdapUsernameCollisionHandlingBehavior}
     */
    public static LdapUsernameCollisionHandlingBehavior translate(String value, LdapUsernameCollisionHandlingBehavior defaultValue) {
      String processedValue = StringUtils.upperCase(StringUtils.trim(value));

      if (StringUtils.isEmpty(processedValue)) {
        return defaultValue;
      } else {
        try {
          return valueOf(processedValue);
        } catch (IllegalArgumentException e) {
          LOG.warn("Invalid LDAP username collision value ({}), using the default value ({})", value, defaultValue.name().toLowerCase());
          return defaultValue;
        }
      }
    }
  }

  /**
   * The {@link DatabaseType} enum represents the database being used.
   */
  public enum DatabaseType {
    POSTGRES("postgres"),
    ORACLE("oracle"),
    MYSQL("mysql"),
    DERBY("derby"),
    SQL_SERVER("sqlserver"),
    SQL_ANYWHERE("sqlanywhere"),
    H2("h2");

    private static final Map<String, DatabaseType> m_mappedTypes =
      new HashMap<>(5);

    static {
      for (DatabaseType databaseType : EnumSet.allOf(DatabaseType.class)) {
        m_mappedTypes.put(databaseType.getName(), databaseType);
      }
    }

    /**
     * The JDBC URL type name.
     */
    private String m_databaseType;

    /**
     * Constructor.
     *
     */
    DatabaseType(String databaseType) {
      m_databaseType = databaseType;
    }

    /**
     * Gets an internal name for this database type.
     *
     * @return the internal name for this database type.
     */
    public String getName() {
      return m_databaseType;
    }

    public DatabaseType get(String databaseTypeName) {
      return m_mappedTypes.get(databaseTypeName);
    }
  }

  /**
   * The {@link ConnectionPoolType} is used to define which pooling mechanism
   * JDBC should use.
   */
  public enum ConnectionPoolType {
    INTERNAL("internal"), C3P0("c3p0");

    /**
     * The connection pooling name.
     */
    private String m_name;

    /**
     * Constructor.
     *
     * @param name
     */
    ConnectionPoolType(String name) {
      m_name = name;
    }

    /**
     * Gets an internal name for this connection pool type.
     *
     * @return the internal name for this connection pool type.
     */
    public String getName() {
      return m_name;
    }
  }

  public Configuration() {
    this(readConfigFile());
  }

  /**
   * This constructor is called from default constructor and
   * also from most tests.
   * @param properties properties to use for testing and in production using
   * the Conf object.
   */
  public Configuration(Properties properties) {
    this.properties = properties;

    agentConfigsMap = new HashMap<>();
    agentConfigsMap.put(AGENT_CONFIGS_DEFAULT_SECTION, new HashMap<String, String>());

    Map<String,String> defaultAgentConfigsMap = agentConfigsMap.get(AGENT_CONFIGS_DEFAULT_SECTION);
    defaultAgentConfigsMap.put(CHECK_REMOTE_MOUNTS.getKey(), getProperty(CHECK_REMOTE_MOUNTS));
    defaultAgentConfigsMap.put(CHECK_MOUNTS_TIMEOUT.getKey(), getProperty(CHECK_MOUNTS_TIMEOUT));
    defaultAgentConfigsMap.put(ENABLE_AUTO_AGENT_CACHE_UPDATE.getKey(), getProperty(ENABLE_AUTO_AGENT_CACHE_UPDATE));
    defaultAgentConfigsMap.put(JAVA_HOME.getKey(), getProperty(JAVA_HOME));

    configsMap = new HashMap<>();
    configsMap.putAll(defaultAgentConfigsMap);
    configsMap.put(AMBARI_PYTHON_WRAP.getKey(), getProperty(AMBARI_PYTHON_WRAP));
    configsMap.put(SRVR_AGENT_HOSTNAME_VALIDATE.getKey(), getProperty(SRVR_AGENT_HOSTNAME_VALIDATE));
    configsMap.put(SRVR_TWO_WAY_SSL.getKey(), getProperty(SRVR_TWO_WAY_SSL));
    configsMap.put(SRVR_TWO_WAY_SSL_PORT.getKey(), getProperty(SRVR_TWO_WAY_SSL_PORT));
    configsMap.put(SRVR_ONE_WAY_SSL_PORT.getKey(), getProperty(SRVR_ONE_WAY_SSL_PORT));
    configsMap.put(SRVR_KSTR_DIR.getKey(), getProperty(SRVR_KSTR_DIR));
    configsMap.put(SRVR_CRT_NAME.getKey(), getProperty(SRVR_CRT_NAME));
    configsMap.put(SRVR_CRT_CHAIN_NAME.getKey(), getProperty(SRVR_CRT_CHAIN_NAME));
    configsMap.put(SRVR_KEY_NAME.getKey(), getProperty(SRVR_KEY_NAME));
    configsMap.put(SRVR_CSR_NAME.getKey(), getProperty(SRVR_CSR_NAME));
    configsMap.put(KSTR_NAME.getKey(), getProperty(KSTR_NAME));
    configsMap.put(KSTR_TYPE.getKey(), getProperty(KSTR_TYPE));
    configsMap.put(TSTR_NAME.getKey(), getProperty(TSTR_NAME));
    configsMap.put(TSTR_TYPE.getKey(), getProperty(TSTR_TYPE));
    configsMap.put(SRVR_CRT_PASS_FILE.getKey(), getProperty(SRVR_CRT_PASS_FILE));
    configsMap.put(PASSPHRASE_ENV.getKey(), getProperty(PASSPHRASE_ENV));
    configsMap.put(PASSPHRASE.getKey(), System.getenv(configsMap.get(PASSPHRASE_ENV.getKey())));
    configsMap.put(RESOURCES_DIR.getKey(), getProperty(RESOURCES_DIR));
    configsMap.put(SRVR_CRT_PASS_LEN.getKey(), getProperty(SRVR_CRT_PASS_LEN));
    configsMap.put(SRVR_DISABLED_CIPHERS.getKey(), getProperty(SRVR_DISABLED_CIPHERS));
    configsMap.put(SRVR_DISABLED_PROTOCOLS.getKey(), getProperty(SRVR_DISABLED_PROTOCOLS));

    configsMap.put(CLIENT_API_SSL_KSTR_DIR_NAME.getKey(),
        properties.getProperty(CLIENT_API_SSL_KSTR_DIR_NAME.getKey(),
            configsMap.get(SRVR_KSTR_DIR.getKey())));

    configsMap.put(CLIENT_API_SSL_KSTR_NAME.getKey(), getProperty(CLIENT_API_SSL_KSTR_NAME));
    configsMap.put(CLIENT_API_SSL_KSTR_TYPE.getKey(), getProperty(CLIENT_API_SSL_KSTR_TYPE));
    configsMap.put(CLIENT_API_SSL_TSTR_NAME.getKey(), getProperty(CLIENT_API_SSL_TSTR_NAME));
    configsMap.put(CLIENT_API_SSL_TSTR_TYPE.getKey(), getProperty(CLIENT_API_SSL_TSTR_TYPE));
    configsMap.put(CLIENT_API_SSL_CRT_PASS_FILE_NAME.getKey(), getProperty(CLIENT_API_SSL_CRT_PASS_FILE_NAME));
    configsMap.put(JAVA_HOME.getKey(), getProperty(JAVA_HOME));
    configsMap.put(PARALLEL_STAGE_EXECUTION.getKey(), getProperty(PARALLEL_STAGE_EXECUTION));
    configsMap.put(SERVER_TMP_DIR.getKey(), getProperty(SERVER_TMP_DIR));
    configsMap.put(REQUEST_LOGPATH.getKey(), getProperty(REQUEST_LOGPATH));
    configsMap.put(LOG4JMONITOR_DELAY.getKey(), getProperty(LOG4JMONITOR_DELAY));
    configsMap.put(REQUEST_LOG_RETAINDAYS.getKey(), getProperty(REQUEST_LOG_RETAINDAYS));
    configsMap.put(EXTERNAL_SCRIPT_TIMEOUT.getKey(), getProperty(EXTERNAL_SCRIPT_TIMEOUT));
    configsMap.put(THREAD_POOL_SIZE_FOR_EXTERNAL_SCRIPT.getKey(), getProperty(THREAD_POOL_SIZE_FOR_EXTERNAL_SCRIPT));
    configsMap.put(SHARED_RESOURCES_DIR.getKey(), getProperty(SHARED_RESOURCES_DIR));
    configsMap.put(KDC_PORT.getKey(), getProperty(KDC_PORT));
    configsMap.put(AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT.getKey(), getProperty(AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT));
    configsMap.put(PROXY_ALLOWED_HOST_PORTS.getKey(), getProperty(PROXY_ALLOWED_HOST_PORTS));
    configsMap.put(TLS_EPHEMERAL_DH_KEY_SIZE.getKey(), getProperty(TLS_EPHEMERAL_DH_KEY_SIZE));

    File passFile = new File(
        configsMap.get(SRVR_KSTR_DIR.getKey()) + File.separator
            + configsMap.get(SRVR_CRT_PASS_FILE.getKey()));

    String password = null;

    if (!passFile.exists()) {
      LOG.info("Generation of file with password");
      try {
        password = RandomStringUtils.randomAlphanumeric(Integer
            .parseInt(configsMap.get(SRVR_CRT_PASS_LEN.getKey())));
        FileUtils.writeStringToFile(passFile, password, Charset.defaultCharset());
        ShellCommandUtil.setUnixFilePermissions(
          ShellCommandUtil.MASK_OWNER_ONLY_RW, passFile.getAbsolutePath());
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(
          "Error reading certificate password from file");
      }
    } else {
      LOG.info("Reading password from existing file");
      try {
        password = FileUtils.readFileToString(passFile, Charset.defaultCharset());
        password = password.replaceAll("\\p{Cntrl}", "");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    configsMap.put(SRVR_CRT_PASS.getKey(), password);

    if (getApiSSLAuthentication()) {
      LOG.info("API SSL Authentication is turned on.");
      File httpsPassFile = new File(configsMap.get(CLIENT_API_SSL_KSTR_DIR_NAME.getKey())
          + File.separator + configsMap.get(CLIENT_API_SSL_CRT_PASS_FILE_NAME.getKey()));

      if (httpsPassFile.exists()) {
        LOG.info("Reading password from existing file");
        try {
          password = FileUtils.readFileToString(httpsPassFile, Charset.defaultCharset());
          password = password.replaceAll("\\p{Cntrl}", "");
        } catch (IOException e) {
          e.printStackTrace();
          throw new RuntimeException("Error reading certificate password from" +
            " file " + httpsPassFile.getAbsolutePath());
        }
      } else {
        LOG.error("There is no keystore for https UI connection.");
        LOG.error("Run \"ambari-server setup-https\" or set " + Configuration.API_USE_SSL.getKey()
            + " = false.");
        throw new RuntimeException("Error reading certificate password from " +
          "file " + httpsPassFile.getAbsolutePath());

      }

      configsMap.put(CLIENT_API_SSL_CRT_PASS.getKey(), password);
    }

    // Capture the Kerberos authentication-related properties
    kerberosAuthenticationProperties = createKerberosAuthenticationProperties();

    loadSSLParams();
  }

  /**
   * Get the property value for the given key.
   *
   * @return the property value
   */
  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  /**
   * Gets a copy of all of the configuration properties that back this
   * {@link Configuration} instance.
   *
   * @return a copy of all of the properties.
   */
  public Properties getProperties() {
    return new Properties(properties);
  }

  /**
   * Gets the value for the specified {@link ConfigurationProperty}. If the
   * value hasn't been set then the default value as specified in
   * {@link ConfigurationProperty#getDefaultValue()} will be returned.
   *
   * @param configurationProperty
   * @return
   */
  public <T> String getProperty(ConfigurationProperty<T> configurationProperty) {
    String defaultStringValue = null;
    if (null != configurationProperty.getDefaultValue()) {
      defaultStringValue = String.valueOf(configurationProperty.getDefaultValue());
    }

    return properties.getProperty(configurationProperty.getKey(), defaultStringValue);
  }

  /**
   * Sets the value for the specified {@link ConfigurationProperty}.
   *
   * @param configurationProperty the property to set (not {@code null}).
   * @param value the value to set on the property, or {@code null} for none.
   */
  public void setProperty(ConfigurationProperty<String> configurationProperty, String value) {
    properties.setProperty(configurationProperty.getKey(), value);
  }

  /**
   * Loads trusted certificates store properties
   */
  protected void loadSSLParams(){
    if (getProperty(SSL_TRUSTSTORE_PATH) != null) {
      System.setProperty(JAVAX_SSL_TRUSTSTORE, getProperty(SSL_TRUSTSTORE_PATH));
    }
    if (getProperty(SSL_TRUSTSTORE_PASSWORD) != null) {
      String ts_password = PasswordUtils.getInstance().readPasswordFromStore(getProperty(SSL_TRUSTSTORE_PASSWORD), this);
      if (ts_password != null) {
        System.setProperty(JAVAX_SSL_TRUSTSTORE_PASSWORD, ts_password);
      } else {
        System.setProperty(JAVAX_SSL_TRUSTSTORE_PASSWORD,
            getProperty(SSL_TRUSTSTORE_PASSWORD));
      }
    }
    if (getProperty(SSL_TRUSTSTORE_TYPE) != null) {
      System.setProperty(JAVAX_SSL_TRUSTSTORE_TYPE, getProperty(SSL_TRUSTSTORE_TYPE));
    }
  }

  /**
   * Find, read, and parse the configuration file.
   * @return the properties that were found or empty if no file was found
   */
  private static Properties readConfigFile() {
    Properties properties = new Properties();

    //Get property file stream from classpath
    InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream(CONFIG_FILE);

    if (inputStream == null) {
      throw new RuntimeException(CONFIG_FILE + " not found in classpath");
    }

    // load the properties
    try {
      properties.load(new InputStreamReader(inputStream, Charsets.UTF_8));
      inputStream.close();
    } catch (FileNotFoundException fnf) {
      LOG.info("No configuration file " + CONFIG_FILE + " found in classpath.", fnf);
    } catch (IOException ie) {
      throw new IllegalArgumentException("Can't read configuration file " +
        CONFIG_FILE, ie);
    }

    return properties;
  }

  /**
   * Writes the given properties into the configuration file
   *
   * @param propertiesToWrite
   *          the properties to be stored
   * @param append
   *          if {@code true} the given properties will be added at the end of the
   *          configuration file; otherwise a brand new configuration file will be
   *          produced
   * @throws AmbariException
   *           if there was any issue when clearing ambari.properties
   */
  private void writeConfigFile(Properties propertiesToStore, boolean append) throws AmbariException {
    File configFile = null;
    try {
      configFile = getConfigFile();
      propertiesToStore.store(new OutputStreamWriter(new FileOutputStream(configFile, append), Charsets.UTF_8), null);
    } catch (Exception e) {
      LOG.error("Cannot write properties [" + propertiesToStore + "] into configuration file [" + configFile + ", " + append + "] ");
      throw new AmbariException("Error while clearing ambari.properties", e);
    }
  }

  /**
   * Removing the given properties from ambari.properties (i.e. at upgrade time)
   *
   * @param propertiesToBeCleared
   *          the properties to be removed
   * @throws AmbariException
   *           if there was any issue when clearing ambari.properties
   */
  public void removePropertiesFromAmbariProperties(Collection<String> propertiesToBeRemoved) throws AmbariException {
    final Properties existingProperties = readConfigFile();
    propertiesToBeRemoved.forEach(key -> {
      existingProperties.remove(key);
    });
    writeConfigFile(existingProperties, false);

    // reloading properties
    this.properties = readConfigFile();
  }

  /**
   * Find, read, and parse the log4j.properties file.
   * @return the properties that were found or empty if no file was found
   */
  public Properties getLog4jProperties() {
    if (!log4jProperties.isEmpty()) {
      return log4jProperties;
    }

    //Get log4j.properties file stream from classpath
    InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream(AMBARI_LOG_FILE);

    if (inputStream == null) {
      throw new RuntimeException(AMBARI_LOG_FILE + " not found in classpath");
    }

    // load the properties
    try {
      log4jProperties.load(inputStream);
      inputStream.close();
    } catch (FileNotFoundException fnf) {
      LOG.info("No configuration file " + AMBARI_LOG_FILE + " found in classpath.", fnf);
    } catch (IOException ie) {
      throw new IllegalArgumentException("Can't read configuration file " +
              AMBARI_LOG_FILE, ie);
    }

    return log4jProperties;
  }


  public void writeToAmbariUpgradeConfigUpdatesFile(Multimap<AbstractUpgradeCatalog.ConfigUpdateType, Entry<String, String>> propertiesToLog,
                                                     String configType, String serviceName, String writeToAmbariUpgradeConfigUpdatesFile) {
    try {
      if (ambariUpgradeConfigUpdatesFilePath == null) {
        Properties log4jProperties = getLog4jProperties();
        if (log4jProperties != null) {
          String logPath = log4jProperties.getProperty("ambari.log.dir");
          String rootPath = log4jProperties.getProperty("ambari.root.dir");
          logPath = StringUtils.replace(logPath, "${ambari.root.dir}", rootPath);
          logPath = StringUtils.replace(logPath, "//", "/");
          if (StringUtils.isNotEmpty(logPath)) {
            ambariUpgradeConfigUpdatesFilePath = logPath + File.separator + writeToAmbariUpgradeConfigUpdatesFile;
          }
        } else {
          LOG.warn("Log4j properties are not available");
        }
      }
    } catch(Exception e) {
      LOG.warn("Failed to create log file name or get path for it:", e);
    }

    if (StringUtils.isNotEmpty(ambariUpgradeConfigUpdatesFilePath)) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      Writer fileWriter = null;
      try {
        JsonObject rootJson = readFileToJSON(ambariUpgradeConfigUpdatesFilePath);
        buildServiceJson(propertiesToLog, configType, serviceName, rootJson);

        fileWriter = new FileWriter(ambariUpgradeConfigUpdatesFilePath);
        gson.toJson(rootJson, fileWriter);
      } catch (IllegalArgumentException e) {
        JsonObject rootJson = new JsonObject();
        buildServiceJson(propertiesToLog, configType, serviceName, rootJson);

        try {
          fileWriter = new FileWriter(ambariUpgradeConfigUpdatesFilePath);
          gson.toJson(rootJson, fileWriter);
        } catch (IOException e1) {
          LOG.error("Unable to write data into " + ambariUpgradeConfigUpdatesFilePath, e);
        }
      } catch (IOException e) {
        LOG.error("Unable to write data into " + ambariUpgradeConfigUpdatesFilePath, e);
      } finally {
        try {
          if (fileWriter != null) {
            fileWriter.close();
          }
        } catch (IOException e) {
          LOG.error("Unable to close file " + ambariUpgradeConfigUpdatesFilePath, e);
        }
      }
    }
  }

  private void buildServiceJson(Multimap<AbstractUpgradeCatalog.ConfigUpdateType, Entry<String, String>> propertiesToLog,
                                String configType, String serviceName, JsonObject rootJson) {
    JsonElement serviceJson = null;
    serviceJson = rootJson.get(serviceName);
    JsonObject serviceJsonObject = null;
    if (serviceJson != null) {
      serviceJsonObject = serviceJson.getAsJsonObject();
    } else {
      serviceJsonObject = new JsonObject();
    }
    buildConfigJson(propertiesToLog, serviceJsonObject, configType);
    if (serviceName == null) {
      serviceName = "General";
    }

    rootJson.add(serviceName, serviceJsonObject);
  }

  private void buildConfigJson(Multimap<AbstractUpgradeCatalog.ConfigUpdateType, Entry<String, String>> propertiesToLog,
                               JsonObject serviceJson, String configType) {
    JsonElement configJson = null;
    configJson = serviceJson.get(configType);
    JsonObject configJsonObject = null;
    if (configJson != null) {
      configJsonObject = configJson.getAsJsonObject();
    } else {
      configJsonObject = new JsonObject();
    }
    buildConfigUpdateTypes(propertiesToLog, configJsonObject);
    serviceJson.add(configType, configJsonObject);
  }

  private void buildConfigUpdateTypes(Multimap<AbstractUpgradeCatalog.ConfigUpdateType, Entry<String, String>> propertiesToLog,
                                      JsonObject configJson) {
    for (AbstractUpgradeCatalog.ConfigUpdateType configUpdateType : propertiesToLog.keySet()) {
      JsonElement currentConfigUpdateType = configJson.get(configUpdateType.getDescription());
      JsonObject currentConfigUpdateTypeJsonObject = null;
      if (currentConfigUpdateType != null) {
        currentConfigUpdateTypeJsonObject = currentConfigUpdateType.getAsJsonObject();
      } else {
        currentConfigUpdateTypeJsonObject = new JsonObject();
      }
      for (Entry<String, String> property : propertiesToLog.get(configUpdateType)) {
        currentConfigUpdateTypeJsonObject.add(property.getKey(), new JsonPrimitive(property.getValue()));
      }
      configJson.add(configUpdateType.getDescription(), currentConfigUpdateTypeJsonObject);
    }
  }

  public Map<String, String> getDatabaseConnectorNames() {
    File file = getConfigFile();
    Long currentConfigLastModifiedDate = file.lastModified();
    if (currentConfigLastModifiedDate.longValue() != configLastModifiedDateForCustomJDBC.longValue()) {
      LOG.info("Ambari properties config file changed.");
      Properties properties = readConfigFile();

      for (String propertyName : dbConnectorPropertyNames) {
        String propertyValue = properties.getProperty(propertyName);
        if (StringUtils.isNotEmpty(propertyValue)) {
          databaseConnectorNames.put(propertyName.replace(".", "_"), propertyValue);
        }
      }

      configLastModifiedDateForCustomJDBC = currentConfigLastModifiedDate;
    }

    return databaseConnectorNames;
  }

  public File getConfigFile() {
    return new File(Configuration.class.getClassLoader().getResource(CONFIG_FILE).getPath());
  }

  public Map<String, String> getPreviousDatabaseConnectorNames() {
    File file = getConfigFile();
    Long currentConfigLastModifiedDate = file.lastModified();
    if (currentConfigLastModifiedDate.longValue() != configLastModifiedDateForCustomJDBCToRemove.longValue()) {
      LOG.info("Ambari properties config file changed.");
      Properties properties = readConfigFile();

      for (String propertyName : dbConnectorPropertyNames) {
        propertyName = "previous." + propertyName;
        String propertyValue = properties.getProperty(propertyName);
        if (StringUtils.isNotEmpty(propertyValue)) {
          databasePreviousConnectorNames.put(propertyName.replace(".", "_"), propertyValue);
        }
      }

      configLastModifiedDateForCustomJDBCToRemove = currentConfigLastModifiedDate;
    }

    return databasePreviousConnectorNames;
  }

  public JsonObject getHostChangesJson(String hostChangesFile) {
    if (hostChangesJson == null) {
      hostChangesJson = readFileToJSON(hostChangesFile);
    }
    return hostChangesJson;
  }

  private JsonObject readFileToJSON (String file) {

    // Read from File to String
    JsonObject jsonObject;

    try {
      JsonParser parser = new JsonParser();
      JsonElement jsonElement = parser.parse(new FileReader(file));
      jsonObject = jsonElement.getAsJsonObject();
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException("No file " + file, e);
    }

    return jsonObject;
  }

  /**
   * Get the views directory.
   *
   * @return the views directory
   */
  public File getViewsDir() {
    String fileName = getProperty(VIEWS_DIRECTORY);
    return new File(fileName);
  }

  /**
   * Determine whether or not view validation is enabled.
   *
   * @return true if view validation is enabled
   */
  public boolean isViewValidationEnabled() {
    return Boolean.parseBoolean(getProperty(VIEWS_VALIDATE));
  }

  /**
   * Determine whether or not a view that has been undeployed (archive deleted) should be removed from the database.
   *
   * @return true if undeployed views should be removed
   */
  public boolean isViewRemoveUndeployedEnabled() {
    return Boolean.parseBoolean(getProperty(VIEWS_REMOVE_UNDEPLOYED));
  }

  /**
   * Determines whether the view directory watcher service should be disabled
   *
   * @return true view directory watcher service should be disabled
   */
  public boolean isViewDirectoryWatcherServiceDisabled() {
    return Boolean.parseBoolean(getProperty(DISABLE_VIEW_DIRECTORY_WATCHER));
  }

  /**
   * @return conventional Java version number, e.g. 7.
   * Integer is used here to simplify comparisons during usage.
   * If java version is not supported, returns -1
   */
  public int getJavaVersion() {
    String versionStr = System.getProperty("java.version");
    if (versionStr.startsWith("1.6")) {
      return 6;
    } else if (versionStr.startsWith("1.7")) {
      return 7;
    } else if (versionStr.startsWith("1.8")) {
      return 8;
    } else { // Some unsupported java version
      return -1;
    }
  }

  public File getBootStrapDir() {
    String fileName = getProperty(BOOTSTRAP_DIRECTORY);
    return new File(fileName);
  }

  public String getBootStrapScript() {
    return getProperty(BOOTSTRAP_SCRIPT);
  }

  public String getBootSetupAgentScript() {
    return getProperty(BOOTSTRAP_SETUP_AGENT_SCRIPT);
  }

  public String getBootSetupAgentPassword() {
    String pass = configsMap.get(PASSPHRASE.getKey());

    if (null != pass) {
      return pass;
    }

    // fallback
    return getProperty(BOOTSTRAP_SETUP_AGENT_PASSWORD);
  }

  public File getRecommendationsDir() {
    String fileName = getProperty(RECOMMENDATIONS_DIR);
    return new File(fileName);
  }

  public String getRecommendationsArtifactsLifetime() {
    return getProperty(RECOMMENDATIONS_ARTIFACTS_LIFETIME);
  }

  public int getRecommendationsArtifactsRolloverMax() {
        int rollovermax = Integer.parseInt(getProperty(RECOMMENDATIONS_ARTIFACTS_ROLLOVER_MAX));
        return (rollovermax == 0) ? 100 : rollovermax;
    }

  public String areHostsSysPrepped(){
    return getProperty(SYS_PREPPED_HOSTS);
  }

  /**
   * Return {@code true} if we forced to work with legacy repositories
   *
   * @return {@link Boolean}
   */
  public boolean arePackagesLegacyOverridden(){
    return getProperty(LEGACY_OVERRIDE).equalsIgnoreCase("true");
  }

  public CommandExecutionType getStageExecutionType(){
    return CommandExecutionType.valueOf(getProperty(COMMAND_EXECUTION_TYPE));
  }

  public String getStackAdvisorScript() {
    return getProperty(STACK_ADVISOR_SCRIPT);
  }

  /**
   * @return a list of prefixes. Packages whose name starts with any of these
   * prefixes, should be skipped during upgrade.
   */
  public List<String> getRollingUpgradeSkipPackagesPrefixes() {
    String propertyValue = getProperty(ROLLING_UPGRADE_SKIP_PACKAGES_PREFIXES);
    ArrayList<String> res = new ArrayList<>();
    for (String prefix : propertyValue.split(",")) {
      if (! prefix.isEmpty()) {
        res.add(prefix.trim());
      }
    }
    return res;
  }

  /**
   * Determine whether or not a Rolling/Express upgrade can bypass the PreChecks. Default value should be false.
   *
   * @return true if RU/EU can bypass PreChecks, otherwise, false.
   */
  public boolean isUpgradePrecheckBypass() {
    return Boolean.parseBoolean(getProperty(STACK_UPGRADE_BYPASS_PRECHECKS));
  }

  /**
   * During stack upgrade, can auto-retry failures for up to x mins. This is useful to improve the robustness in unstable environments.
   * Suggested value is 0-30 mins.
   * @return
   */
  public int getStackUpgradeAutoRetryTimeoutMins() {
    Integer result = NumberUtils.toInt(getProperty(STACK_UPGRADE_AUTO_RETRY_TIMEOUT_MINS));
    return result >= 0 ? result : 0;
  }

  /**
   * If the stack.upgrade.auto.retry.timeout.mins property is positive, then run RetryUpgradeActionService every x
   * seconds.
   * @return Number of seconds between runs of {@link org.apache.ambari.server.state.services.RetryUpgradeActionService}
   */
  public int getStackUpgradeAutoRetryCheckIntervalSecs() {
    Integer result = NumberUtils.toInt(getProperty(STACK_UPGRADE_AUTO_RETRY_CHECK_INTERVAL_SECS));
    return result >= 0 ? result : 0;
  }

  /**
   * If auto-retry during stack upgrade is enabled, skip any tasks whose custom command name contains at least one
   * of the strings in the following CSV property. Note that values have to be enclosed in quotes and separated by commas.
   * @return
   */
  public List<String> getStackUpgradeAutoRetryCustomCommandNamesToIgnore() {
    String value = getProperty(STACK_UPGRADE_AUTO_RETRY_CUSTOM_COMMAND_NAMES_TO_IGNORE);
    List<String> list = convertCSVwithQuotesToList(value);
    listToLowerCase(list);
    return list;
  }

  /**
   * If auto-retry during stack upgrade is enabled, skip any tasks whose command details contains at least one
   * of the strings in the following CSV property. Note that values have to be enclosed in quotes and separated by commas.
   * @return
   */
  public List<String> getStackUpgradeAutoRetryCommandDetailsToIgnore() {
    String value = getProperty(STACK_UPGRADE_AUTO_RETRY_COMMAND_DETAILS_TO_IGNORE);
    List<String> list = convertCSVwithQuotesToList(value);
    listToLowerCase(list);
    return list;
  }

  /**
   * Convert quoted elements separated by commas into a list. Values cannot contain double quotes or commas.
   * @param value, e.g., String with value "a","b","c" => ["a", "b", "c"]
   * @return List of parsed values, or empty list if no values exist.
   */
  private List<String> convertCSVwithQuotesToList(String value) {
    List<String> list = new ArrayList<>();
    if (StringUtils.isNotEmpty(value)) {
      if (value.indexOf(",") >= 0) {
        for (String e : value.split(",")) {
          e = StringUtils.stripStart(e, "\"");
          e = StringUtils.stripEnd(e, "\"");
          list.add(e);
        }
      } else {
        list.add(value);
      }
    }
    return list;
  }

  /**
   * Convert the elements of a list to lowercase.
   * @param list
   */
  private void listToLowerCase(List<String> list) {
    if (list == null) {
      return;
    }
    for (int i = 0; i < list.size(); i++) {
      list.set(i, list.get(i).toLowerCase());
    }
  }

  /**
   * Get the map with server config parameters.
   * Keys - public constants of this class
   * @return the map with server config parameters
   */
  public Map<String, String> getConfigsMap() {
    return configsMap;
  }

  /**
   * Get the map with server config parameters related to agent configuration.
   * Keys - public constants of this class
   * @return the map with server config parameters related to agent configuration
   */
  public Map<String, Map<String,String>> getAgentConfigsMap() {
    return agentConfigsMap;
  }

  /**
   * Checks if CSRF protection enabled
   * @return true if CSRF protection filter should be enabled
   */
  public boolean csrfProtectionEnabled() {
    return Boolean.parseBoolean(getProperty(API_CSRF_PREVENTION));
  }

  /**
   * Gets client security type
   * @return appropriate ClientSecurityType
   */
  public ClientSecurityType getClientSecurityType() {
    return ClientSecurityType.fromString(getProperty(CLIENT_SECURITY));
  }

  public void setClientSecurityType(ClientSecurityType type) {
    setProperty(CLIENT_SECURITY, type.toString());
  }

  public String getWebAppDir() {
    return getProperty(WEBAPP_DIRECTORY.getKey());
  }

  /**
   * Get the file that will be used for host mapping.
   * @return null if such a file is not present, value if present.
   */
  public String getHostsMapFile() {
    LOG.info("Hosts Mapping File " + getProperty(SRVR_HOSTS_MAPPING));
    return getProperty(SRVR_HOSTS_MAPPING);
  }

  /**
   * Gets ambari stack-path
   * @return String
   */
  public String getMetadataPath() {
    return getProperty(METADATA_DIR_PATH);
  }

  /**
   * Gets ambari common services path
   * @return String
   */
  public String getCommonServicesPath() {
    return getProperty(COMMON_SERVICES_DIR_PATH);
  }

  /**
   * Gets ambari extensions-path
   * @return String
   */
  public String getExtensionsPath() {
    return getProperty(EXTENSIONS_DIR_PATH);
  }

  /**
   * Gets ambari management packs staging directory
   * @return String
   */
  public String getMpacksStagingPath() {
    return getProperty(MPACKS_STAGING_DIR_PATH);
  }

  /**
   * Gets ambari v2 management packs staging directory
   * @return String
   */
  public String getMpacksV2StagingPath() {
    return getProperty(MPACKS_V2_STAGING_DIR_PATH);
  }


  public String getServerVersionFilePath() {
    return getProperty(SERVER_VERSION_FILE);
  }

  /**
   * Gets ambari server version
   * @return version String
   */
  public String getServerVersion() {
    try {
      return FileUtils
              .readFileToString(new File(getServerVersionFilePath()), Charset.defaultCharset())
              .trim();
    } catch (IOException e) {
      LOG.error("Unable to read server version file", e);
    }
    return null;
  }

  /**
   * Gets the username of the default user assumed to be executing API calls.
   * <p/>
   * If this value is <code>null</code> or empty then no default user is set and one must be
   * specified when issuing API calls.
   *
   * @return the username of a user.
   */
  public String getDefaultApiAuthenticatedUser() {
    return properties.getProperty(API_AUTHENTICATED_USER.getKey());
  }

  /**
   * Gets ssl api port
   * @return int
   */
  public int getClientSSLApiPort() {
    return Integer.parseInt(getProperty(CLIENT_API_SSL_PORT));
  }

  /**
   * Check to see if the API should be authenticated via ssl or not
   * @return false if not, true if ssl needs to be used.
   */
  public boolean getApiSSLAuthentication() {
    return Boolean.parseBoolean(getProperty(API_USE_SSL));
  }

  /**
   * Check to see if the Agent should be authenticated via ssl or not
   * @return false if not, true if ssl needs to be used.
   */
  public boolean getAgentSSLAuthentication() {
    return Boolean.parseBoolean(getProperty(AGENT_USE_SSL));
  }

  /**
   * Get the value that should be set for the <code>Strict-Transport-Security</code> HTTP response header for Ambari Server UI.
   * <p/>
   * By default this will be <code>max-age=31536000; includeSubDomains</code>. For example:
   * <p/>
   * <code>
   * Strict-Transport-Security: max-age=31536000; includeSubDomains
   * </code>
   * <p/>
   * This value may be ignored when {@link #getApiSSLAuthentication()} is <code>false</code>.
   *
   * @return the Strict-Transport-Security value - null or "" indicates that the value is not set
   */
  public String getStrictTransportSecurityHTTPResponseHeader() {
    return getProperty(HTTP_STRICT_TRANSPORT_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>X-Frame-Options</code> HTTP response header for Ambari Server UI.
   * <p/>
   * By default this will be <code>DENY</code>. For example:
   * <p/>
   * <code>
   * X-Frame-Options: DENY
   * </code>
   *
   * @return the X-Frame-Options value - null or "" indicates that the value is not set
   */
  public String getXFrameOptionsHTTPResponseHeader() {
    return getProperty(HTTP_X_FRAME_OPTIONS_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>X-XSS-Protection</code> HTTP response header for Ambari Server UI.
   * <p/>
   * By default this will be <code>1; mode=block</code>. For example:
   * <p/>
   * <code>
   * X-XSS-Protection: 1; mode=block
   * </code>
   *
   * @return the X-XSS-Protection value - null or "" indicates that the value is not set
   */
  public String getXXSSProtectionHTTPResponseHeader() {
    return getProperty(HTTP_X_XSS_PROTECTION_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>X-Content-Type</code> HTTP response header for Ambari Server UI.
   * <p/>
   * By default this will be <code>nosniff</code>. For example:
   * <p/>
   * <code>
   * X-Content-Type: nosniff
   * </code>
   *
   * @return the X-Content-Type value - null or "" indicates that the value is not set
   */
  public String getXContentTypeHTTPResponseHeader() {
    return getProperty(HTTP_X_CONTENT_TYPE_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>Cache-Control</code> HTTP response header for Ambari Server UI.
   * <p/>
   * By default this will be <code>no-store</code>. For example:
   * <p/>
   * <code>
   * Cache-control: no-store
   * </code>
   *
   * @return the Cache-Control value - null or "" indicates that the value is not set
   */
  public String getCacheControlHTTPResponseHeader() {
    return getProperty(HTTP_CACHE_CONTROL_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>Pragma</code> HTTP response header for Ambari Server UI.
   * <p/>
   * By default this will be <code>no-cache</code>. For example:
   * <p/>
   * <code>
   * Pragma: no-cache
   * </code>
   *
   * @return the Pragma value - null or "" indicates that the value is not set
   */
  public String getPragmaHTTPResponseHeader() {
    return getProperty(HTTP_PRAGMA_HEADER_VALUE);
  }

   /**
   * Get the value that should be set for the <code>Charset</code> HTTP response header for Ambari Server UI.
   * <p/>
   * By default this will be <code>utf-8</code>. For example:
   * <p/>
   * <code>
   * utf-8
   * </code>
   *
   * @return the Charset value - null or "" indicates that the value is not set
   */
  public String getCharsetHTTPResponseHeader() {
    return getProperty(HTTP_CHARSET);
  }

  /**
   * Get the value that should be set for the <code>Strict-Transport-Security</code> HTTP response header for Ambari Views.
   * <p/>
   * By default this will be <code>max-age=31536000; includeSubDomains</code>. For example:
   * <p/>
   * <code>
   * Strict-Transport-Security: max-age=31536000; includeSubDomains
   * </code>
   * <p/>
   * This value may be ignored when {@link #getApiSSLAuthentication()} is <code>false</code>.
   *
   * @return the Strict-Transport-Security value - null or "" indicates that the value is not set
   */
  public String getViewsStrictTransportSecurityHTTPResponseHeader() {
    return getProperty(VIEWS_HTTP_STRICT_TRANSPORT_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>X-Frame-Options</code> HTTP response header for Ambari Views.
   * <p/>
   * By default this will be <code>DENY</code>. For example:
   * <p/>
   * <code>
   * X-Frame-Options: DENY
   * </code>
   *
   * @return the X-Frame-Options value - null or "" indicates that the value is not set
   */
  public String getViewsXFrameOptionsHTTPResponseHeader() {
    return getProperty(VIEWS_HTTP_X_FRAME_OPTIONS_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>X-XSS-Protection</code> HTTP response header for Ambari Views.
   * <p/>
   * By default this will be <code>1; mode=block</code>. For example:
   * <p/>
   * <code>
   * X-XSS-Protection: 1; mode=block
   * </code>
   *
   * @return the X-XSS-Protection value - null or "" indicates that the value is not set
   */
  public String getViewsXXSSProtectionHTTPResponseHeader() {
    return getProperty(VIEWS_HTTP_X_XSS_PROTECTION_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>X-Content-Type</code> HTTP response header for Ambari Views.
   * <p/>
   * By default this will be <code>nosniff</code>. For example:
   * <p/>
   * <code>
   * X-Content-Type: nosniff
   * </code>
   *
   * @return the X-Content-Type value - null or "" indicates that the value is not set
   */
  public String getViewsXContentTypeHTTPResponseHeader() {
    return getProperty(VIEWS_HTTP_X_CONTENT_TYPE_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>Cache-Control</code> HTTP response header for Ambari Views.
   * <p/>
   * By default this will be <code>no-store</code>. For example:
   * <p/>
   * <code>
   * Cache-control: no-store
   * </code>
   *
   * @return the Cache-Control value - null or "" indicates that the value is not set
   */
  public String getViewsCacheControlHTTPResponseHeader() {
    return getProperty(VIEWS_HTTP_CACHE_CONTROL_HEADER_VALUE);
  }

  /**
   * Get the comma separated additional classpath, that should be added to view's classloader.
   * <p/>
   * By default it will be empty. i.e. no additional classpath.
   * If present it will be comma separated path entries. Each entry can be a file or a directory.
   * If entry is a file it will be added as it is.
   * If entry is a directory, all the files inside this directory will be added to the classpath.
   * @return the view's additional classpath value - null or "" indicates that the value is not set
   */
  public String getViewsAdditionalClasspath() {
    return getProperty(VIEWS_ADDITIONAL_CLASSPATH_VALUE);
  }

  /**
   * Get the value that should be set for the <code>Pragma</code> HTTP response header for Ambari Views.
   * <p/>
   * By default this will be <code>no-cache</code>. For example:
   * <p/>
   * <code>
   * Pragma: no-cache
   * </code>
   *
   * @return the Pragma value - null or "" indicates that the value is not set
   */
  public String getViewsPragmaHTTPResponseHeader() {
    return getProperty(VIEWS_HTTP_PRAGMA_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>Charset</code> HTTP response header for Ambari Views.
   * <p/>
   * By default this will be <code>utf-8</code>. For example:
   * <p/>
   * <code>
   * utf-8
   * </code>
   *
   * @return the Charset value - null or "" indicates that the value is not set
   */
  public String getViewsCharsetHTTPResponseHeader() {
    return getProperty(VIEWS_HTTP_CHARSET);
  }

  /**
   * Check to see if the hostname of the agent is to be validated as a proper hostname or not
   *
   * @return true if agent hostnames should be checked as a valid hostnames; otherwise false
   */
  public boolean validateAgentHostnames() {
    return Boolean.parseBoolean(getProperty(SRVR_AGENT_HOSTNAME_VALIDATE));
  }

  /**
   * Check to see if two-way SSL auth should be used between server and agents
   * or not
   *
   * @return true two-way SSL authentication is enabled
   */
  public boolean isTwoWaySsl() {
    return Boolean.parseBoolean(getProperty(SRVR_TWO_WAY_SSL));
  }

  /**
   * Check to see if the API responses should be compressed via gzip or not
   * @return false if not, true if gzip compression needs to be used.
   */
  public boolean isApiGzipped() {
    return Boolean.parseBoolean(getProperty(API_GZIP_COMPRESSION_ENABLED));
  }


  /**
   * Check to see if the API responses should be compressed via gzip or not
   * @return false if not, true if gzip compression needs to be used.
   */
  public boolean isGzipHandlerEnabledForJetty() {
    return Boolean.parseBoolean(getProperty(GZIP_HANDLER_JETTY_ENABLED));
  }

  /**
   * Check to see if the agent API responses should be compressed via gzip or not
   * @return false if not, true if gzip compression needs to be used.
   */
  public boolean isAgentApiGzipped() {
    return Boolean.parseBoolean(getProperty(AGENT_API_GZIP_COMPRESSION_ENABLED));
  }

  /**
   * Check to see if the API responses should be compressed via gzip or not
   * Content will only be compressed if content length is either unknown or
   * greater this value
   * @return false if not, true if ssl needs to be used.
   */
  public String getApiGzipMinSize() {
    return getProperty(API_GZIP_MIN_COMPRESSION_SIZE);
  }

  /**
   * Check persistence type Ambari Server should use. Possible values:
   * in-memory - use in-memory Derby database to store data
   * local - use local Postgres instance
   * remote - use provided jdbc driver name and url to connect to database
   */
  public PersistenceType getPersistenceType() {
    String value = getProperty(SERVER_PERSISTENCE_TYPE);
    return PersistenceType.fromString(value);
  }

  public String getDatabaseDriver() {
    if (getPersistenceType() != PersistenceType.IN_MEMORY) {
      return getProperty(SERVER_JDBC_DRIVER);
    } else {
      return JDBC_IN_MEMORY_DRIVER;
    }
  }

  public String getDatabaseUrl() {
    if (getPersistenceType() != PersistenceType.IN_MEMORY) {
      String URI = getProperty(SERVER_JDBC_URL);
      if (URI != null) {
        return URI;
      } else {
        return getLocalDatabaseUrl();
      }
    } else {
      return JDBC_IN_MEMORY_URL;
    }
  }

  public String getLocalDatabaseUrl() {
    String dbName = properties.getProperty(SERVER_DB_NAME.getKey());
    if(dbName == null || dbName.isEmpty()) {
      throw new RuntimeException("Server DB Name is not configured!");
    }

    return JDBC_LOCAL_URL + dbName;
  }

  public String getDatabaseUser() {
    if (getPersistenceType() == PersistenceType.IN_MEMORY) {
      return JDBC_IN_MEMORY_USER;
    }
    return getProperty(SERVER_JDBC_USER_NAME);
  }

  public String getDatabasePassword() {
    if (getPersistenceType() == PersistenceType.IN_MEMORY) {
      return JDBC_IN_MEMORY_PASSWORD;
    }
    String passwdProp = properties.getProperty(SERVER_JDBC_USER_PASSWD.getKey());
    String dbpasswd = null;
    boolean isPasswordAlias = false;
    if (CredentialProvider.isAliasString(passwdProp)) {
      dbpasswd = PasswordUtils.getInstance().readPasswordFromStore(passwdProp, this);
      isPasswordAlias =true;
    }

    if (dbpasswd != null) {
      return dbpasswd;
    } else if (isPasswordAlias) {
      LOG.error("Can't read db password from keystore. Please, check master key was set correctly.");
      throw new RuntimeException("Can't read db password from keystore. Please, check master key was set correctly.");
    } else {
      return PasswordUtils.getInstance().readPasswordFromFile(passwdProp, SERVER_JDBC_USER_PASSWD.getDefaultValue());
    }
  }

  public String getRcaDatabaseDriver() {
    return getProperty(SERVER_JDBC_RCA_DRIVER);
  }

  public String getRcaDatabaseUrl() {
    return getProperty(SERVER_JDBC_RCA_URL);
  }

  public String getRcaDatabaseUser() {
    return getProperty(SERVER_JDBC_RCA_USER_NAME);
  }

  public String getRcaDatabasePassword() {
    String passwdProp = properties.getProperty(SERVER_JDBC_RCA_USER_PASSWD.getKey());
    return PasswordUtils.getInstance().readPassword(passwdProp, SERVER_JDBC_RCA_USER_PASSWD.getDefaultValue());
  }

  public String getServerOsType() {
    return getProperty(OS_VERSION);
  }

  public String getServerOsFamily() {
    return getProperty(OS_FAMILY);
  }

  public String getMasterHostname(String defaultValue) {
    return properties.getProperty(BOOTSTRAP_MASTER_HOSTNAME.getKey(), defaultValue);
  }

  public int getClientApiPort() {
    return Integer.parseInt(getProperty(CLIENT_API_PORT));
  }

  public String getOjdbcJarName() {
    return getProperty(OJDBC_JAR_NAME);
  }

  public String getJavaHome() {
    return getProperty(JAVA_HOME);
  }

  public String getJDKName() {
    return getProperty(JDK_NAME);
  }

  public String getJCEName() {
    return getProperty(JCE_NAME);
  }

  public String getStackJavaHome() {
    return getProperty(STACK_JAVA_HOME);
  }

  public String getStackJDKName() {
    return getProperty(STACK_JDK_NAME);
  }

  public String getStackJCEName() {
    return getProperty(STACK_JCE_NAME);
  }

  public String getStackJavaVersion() {
    return getProperty(STACK_JAVA_VERSION);
  }

  public String getAmbariBlacklistFile() {
    return getProperty(PROPERTY_MASK_FILE);
  }

  public String getServerDBName() {
    return getProperty(SERVER_DB_NAME);
  }

  public String getMySQLJarName() {
    return getProperty(MYSQL_JAR_NAME);
  }

  public JPATableGenerationStrategy getJPATableGenerationStrategy() {
    return JPATableGenerationStrategy.fromString(
        System.getProperty(SERVER_JDBC_GENERATE_TABLES.getKey()));
  }

  public int getConnectionMaxIdleTime() {
    return Integer.parseInt(getProperty(SERVER_CONNECTION_MAX_IDLE_TIME));
  }

  /**
   * @return the name to be used for audit information if there is no
   * logged-in user.  Default is '_anonymous'.
   */
  public String getAnonymousAuditName() {
    return getProperty(ANONYMOUS_AUDIT_NAME);
  }

  public boolean isMasterKeyPersisted() {
    File masterKeyFile = getMasterKeyLocation();
    return (masterKeyFile != null) && masterKeyFile.exists();
  }

  public File getServerKeyStoreDirectory() {
    String path = getProperty(SRVR_KSTR_DIR);
    return ((path == null) || path.isEmpty())
      ? new File(".")
      : new File(path);
  }

  /**
   * Returns a File pointing where master key file is expected to be
   * <p/>
   * The master key file is named 'master'. The directory that this file is to
   * be found in is calculated by obtaining the directory path assigned to the
   * Ambari property 'security.master.key.location'; else if that value is
   * empty, then the directory is determined by calling
   * {@link #getServerKeyStoreDirectory()}.
   * <p/>
   * If it exists, this file contains the key used to decrypt values stored in
   * the master keystore.
   *
   * @return a File that points to the master key file
   * @see #getServerKeyStoreDirectory()
   * @see #MASTER_KEY_FILENAME_DEFAULT
   */
  public File getMasterKeyLocation() {
    File location;
    String path = getProperty(MASTER_KEY_LOCATION);

    if (StringUtils.isEmpty(path)) {
      location = new File(getServerKeyStoreDirectory(), MASTER_KEY_FILENAME_DEFAULT);
      LOG.debug("Value of {} is not set, using {}", MASTER_KEY_LOCATION, location.getAbsolutePath());
    } else {
      location = new File(path, MASTER_KEY_FILENAME_DEFAULT);
      LOG.debug("Value of {} is {}", MASTER_KEY_LOCATION, location.getAbsolutePath());
    }

    return location;
  }

  /**
   * Returns the location of the master keystore file.
   * <p/>
   * The master keystore file is named 'credentials.jceks'. The directory that
   * this file is to be found in is calculated by obtaining the directory path
   * assigned to the Ambari property 'security.master.keystore.location'; else
   * if that value is empty, then the directory is determined by calling
   * {@link #getServerKeyStoreDirectory()}.
   * <p/>
   * The location is calculated by obtaining the Ambari property directory path
   * assigned to the key 'security.master.keystore.location'. If that value is
   * empty, then the directory is determined by
   * {@link #getServerKeyStoreDirectory()}.
   *
   * @return a File that points to the master keystore file
   * @see #getServerKeyStoreDirectory()
   * @see #MASTER_KEYSTORE_FILENAME_DEFAULT
   */
  public File getMasterKeyStoreLocation() {
    File location;
    String path = getProperty(MASTER_KEYSTORE_LOCATION);

    if (StringUtils.isEmpty(path)) {
      location = new File(getServerKeyStoreDirectory(), MASTER_KEYSTORE_FILENAME_DEFAULT);
      LOG.debug("Value of {} is not set, using {}", MASTER_KEYSTORE_LOCATION,
          location.getAbsolutePath());
    } else {
      location = new File(path, MASTER_KEYSTORE_FILENAME_DEFAULT);
      LOG.debug("Value of {} is {}", MASTER_KEYSTORE_LOCATION, location.getAbsolutePath());
    }

    return location;
  }

  /**
   * Gets the temporary keystore retention time in minutes.
   * <p/>
   * This value is retrieved from the Ambari property named 'security.temporary.keystore.retention.minutes'.
   * If not set, the default value of 90 (minutes) will be returned.
   *
   * @return a timeout value (in minutes)
   */
  public long getTemporaryKeyStoreRetentionMinutes() {
    long minutes;
    String value = getProperty(TEMPORARYSTORE_RETENTION_MINUTES);

    if(StringUtils.isEmpty(value)) {
      LOG.debug("Value of {} is not set, using default value ({})",
          TEMPORARYSTORE_RETENTION_MINUTES.getKey(),
          TEMPORARYSTORE_RETENTION_MINUTES.getDefaultValue());

      minutes = TEMPORARYSTORE_RETENTION_MINUTES.getDefaultValue();
    }
    else {
      try {
        minutes = Long.parseLong(value);
        LOG.debug("Value of {} is {}", TEMPORARYSTORE_RETENTION_MINUTES, value);
      } catch (NumberFormatException e) {
        LOG.warn("Value of {} ({}) should be a number, falling back to default value ({})",
            TEMPORARYSTORE_RETENTION_MINUTES.getKey(), value,
            TEMPORARYSTORE_RETENTION_MINUTES.getDefaultValue());
        minutes = TEMPORARYSTORE_RETENTION_MINUTES.getDefaultValue();
      }
    }

    return minutes;
  }

  /**
   * Gets a boolean value indicating whether to actively purge the temporary keystore when the retention
   * time expires (true) or to passively purge when credentials are queried (false).
   * <p/>
   * This value is retrieved from the Ambari property named 'security.temporary.keystore.actibely.purge'.
   * If not set, the default value of true.
   *
   * @return a Boolean value declaring whether to actively (true) or passively (false) purge the temporary keystore
   */
  public boolean isActivelyPurgeTemporaryKeyStore() {
    String value = getProperty(TEMPORARYSTORE_ACTIVELY_PURGE);

    if (StringUtils.isEmpty(value)) {
      LOG.debug("Value of {} is not set, using default value ({})",
          TEMPORARYSTORE_ACTIVELY_PURGE.getKey(), TEMPORARYSTORE_ACTIVELY_PURGE.getDefaultValue());
      return TEMPORARYSTORE_ACTIVELY_PURGE.getDefaultValue();
    } else if ("true".equalsIgnoreCase(value)) {
      LOG.debug("Value of {} is {}", TEMPORARYSTORE_ACTIVELY_PURGE.getKey(), value);
      return true;
    } else if ("false".equalsIgnoreCase(value)) {
      LOG.debug("Value of {} is {}", TEMPORARYSTORE_ACTIVELY_PURGE.getKey(), value);
      return false;
    } else {
      LOG.warn("Value of {} should be either \"true\" or \"false\" but is \"{}\", falling back to default value ({})",
          TEMPORARYSTORE_ACTIVELY_PURGE.getKey(), value,
          TEMPORARYSTORE_ACTIVELY_PURGE.getDefaultValue());
      return TEMPORARYSTORE_ACTIVELY_PURGE.getDefaultValue();
    }
  }

  public String getSrvrDisabledCiphers() {
    String disabledCiphers = getProperty(SRVR_DISABLED_CIPHERS);
    return disabledCiphers.trim();
  }

  public String getSrvrDisabledProtocols() {
    String disabledProtocols = getProperty(SRVR_DISABLED_PROTOCOLS);
    return disabledProtocols.trim();
  }

  public int getOneWayAuthPort() {
    return Integer.parseInt(getProperty(SRVR_ONE_WAY_SSL_PORT));
  }

  public int getTwoWayAuthPort() {
    return Integer.parseInt(getProperty(SRVR_TWO_WAY_SSL_PORT));
  }

  /**
   * Gets all properties that begin with {@value #SERVER_JDBC_PROPERTIES_PREFIX}
   * , removing the prefix. The properties are then pre-pending with
   * {@code eclipselink.jdbc.property.} before being returned.
   * <p/>
   * These properties are used to pass JDBC driver-specific connection
   * properties to EclipseLink.
   * <p/>
   * server.jdbc.properties.loginTimeout ->
   * eclipselink.jdbc.property.loginTimeout <br/>
   * server.jdbc.properties.oraclecustomname ->
   * eclipselink.jdbc.property.oraclecustomname
   *
   * @return custom properties for database connections
   */
  public Properties getDatabaseCustomProperties() {
    if (null != customDbProperties) {
      return customDbProperties;
    }

    customDbProperties = new Properties();

    for (Entry<Object, Object> entry : properties.entrySet()) {
      String key = entry.getKey().toString();
      String val = entry.getValue().toString();
      if (key.startsWith(SERVER_JDBC_PROPERTIES_PREFIX)) {
        key = "eclipselink.jdbc.property." + key.substring(SERVER_JDBC_PROPERTIES_PREFIX.length());
        customDbProperties.put(key, val);
      }
    }

    return customDbProperties;
  }

  /**
   * Gets all properties that begin with
   * {@value #SERVER_PERSISTENCE_PROPERTIES_PREFIX} , removing the prefix. These
   * properties are used to pass JPA-specific properties to the persistence
   * provider (such as EclipseLink).
   * <p/>
   * server.persistence.properties.eclipselink.jdbc.batch-writing.size=25 ->
   * eclipselink.jdbc.batch-writing.size=25
   *
   * @return custom properties for database connections
   */
  public Properties getPersistenceCustomProperties() {
    if (null != customPersistenceProperties) {
      return customPersistenceProperties;
    }

    customPersistenceProperties = new Properties();

    for (Entry<Object, Object> entry : properties.entrySet()) {
      String key = entry.getKey().toString();
      String val = entry.getValue().toString();
      if (key.startsWith(SERVER_PERSISTENCE_PROPERTIES_PREFIX)) {
        key = key.substring(SERVER_PERSISTENCE_PROPERTIES_PREFIX.length());
        customPersistenceProperties.put(key, val);
      }
    }

    return customPersistenceProperties;
  }

  /**
   * @return Custom property for request header size
   */
  public int getHttpRequestHeaderSize() {
    return Integer.parseInt(getProperty(SERVER_HTTP_REQUEST_HEADER_SIZE));
  }

  /**
   * @return Custom property for response header size
   */
  public int getHttpResponseHeaderSize() {
    return Integer.parseInt(getProperty(SERVER_HTTP_RESPONSE_HEADER_SIZE));
  }

  /**
   * @return the set of properties to mask in the api that
   * returns ambari.properties
   */
  public Set<String> getPropertiesToBlackList()
  {
    if (propertiesToMask != null) {
      return propertiesToMask;
    }
    Properties blacklistProperties = new Properties();
    String blacklistFile = getAmbariBlacklistFile();
    propertiesToMask = new HashSet<>();
    if(blacklistFile != null) {
      File propertiesMaskFile = new File(blacklistFile);
      InputStream inputStream = null;
      if(propertiesMaskFile.exists()) {
        try {
          inputStream = new FileInputStream(propertiesMaskFile);
	  blacklistProperties.load(inputStream);
	  propertiesToMask = blacklistProperties.stringPropertyNames();
        } catch (Exception e) {
	  String message = String.format("Blacklist properties file %s cannot be read", blacklistFile);
          LOG.error(message);
        } finally {
	  IOUtils.closeQuietly(inputStream);
        }
      }
    }
    return propertiesToMask;
  }

  public Map<String, String> getAmbariProperties() {

    Properties properties = readConfigFile();
    Map<String, String> ambariPropertiesMap = new HashMap<>();

    for(String key : properties.stringPropertyNames()) {
      ambariPropertiesMap.put(key, properties.getProperty(key));
    }
    return ambariPropertiesMap;
  }

  public long getExecutionCommandsCacheSize() {
    String stringValue = getProperty(SERVER_EC_CACHE_SIZE);
    long value = SERVER_EC_CACHE_SIZE.getDefaultValue();
    if (stringValue != null) {
      try {
        value = Long.parseLong(stringValue);
      } catch (NumberFormatException ignored) {
      }

    }

    return value;
  }

  /**
   * Caching of host role command status summary can be enabled/disabled
   * through the {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED} config property.
   * This method returns the value of {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED}
   * config property.
   * @return true if caching is to be enabled otherwise false.
   */
  public boolean getHostRoleCommandStatusSummaryCacheEnabled() {
    String stringValue = getProperty(SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED);
    boolean value = SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED.getDefaultValue();
    if (stringValue != null) {
      try {
        value = Boolean.valueOf(stringValue);
      }
      catch (NumberFormatException ignored) {
      }

    }

    return value;
  }

  /**
   * In order to avoid the cache storing host role command status summary objects exhaust
   * memory we set a max record number allowed for the cache. This limit can be configured
   * through {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE} config property. The method returns
   * the value of this config property.
   * @return the upper limit for the number of cached host role command summaries.
   */
  public long getHostRoleCommandStatusSummaryCacheSize() {
    String stringValue = getProperty(SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE);
    long value = SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE.getDefaultValue();
    if (stringValue != null) {
      try {
        value = Long.parseLong(stringValue);
      }
      catch (NumberFormatException ignored) {
      }

    }

    return value;
  }

  /**
   * As a safety measure the cache storing host role command status summaries should auto expire after a while.
   * The expiry duration is specified through the {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION} config property
   * expressed in minutes. The method returns the value of this config property.
   * @return the cache expiry duration in minutes
   */
  public long getHostRoleCommandStatusSummaryCacheExpiryDuration() {
    String stringValue = getProperty(SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION);
    long value = SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION.getDefaultValue();
    if (stringValue != null) {
      try {
        value = Long.parseLong(stringValue);
      }
      catch (NumberFormatException ignored) {
      }

    }

    return value;
  }



  /**
   * @return whether staleConfig's flag is cached.
   */
  public boolean isStaleConfigCacheEnabled() {
    return Boolean.parseBoolean(getProperty(SERVER_STALE_CONFIG_CACHE_ENABLED));
  }

  /**
   * @return expiration time of stale config cache
   */
  public Integer staleConfigCacheExpiration() {
    return Integer.parseInt(getProperty(SERVER_STALE_CONFIG_CACHE_EXPIRATION));
  }

  /**
   * @return a string array of suffixes used to validate repo URLs.
   */
  public String[] getRepoValidationSuffixes(String osType) {
    String repoSuffixes;

    if(osFamily.isUbuntuFamily(osType)) {
      repoSuffixes = getProperty(REPO_SUFFIX_KEY_UBUNTU);
    } else {
      repoSuffixes = getProperty(REPO_SUFFIX_KEY_DEFAULT);
    }

    return repoSuffixes.split(",");
  }


  public String isExecutionSchedulerClusterd() {
    return getProperty(EXECUTION_SCHEDULER_CLUSTERED);
  }

  public String getExecutionSchedulerThreads() {
    return getProperty(EXECUTION_SCHEDULER_THREADS);
  }

  public Integer getRequestReadTimeout() {
    return Integer.parseInt(getProperty(REQUEST_READ_TIMEOUT));
  }

  public Integer getRequestConnectTimeout() {
    return Integer.parseInt(getProperty(REQUEST_CONNECT_TIMEOUT));
  }

  /**
   * @return The read timeout value for views when trying to access ambari apis
   */
  public Integer getViewAmbariRequestReadTimeout() {
    return Integer.parseInt(getProperty(AMBARI_REQUEST_READ_TIMEOUT));
  }

  /**
   * @return The connection timeout value for views when trying to connect to ambari apis
   */
  public Integer getViewAmbariRequestConnectTimeout() {
    return Integer.parseInt(getProperty(AMBARI_REQUEST_CONNECT_TIMEOUT));
  }

  public String getExecutionSchedulerConnections() {
    return getProperty(EXECUTION_SCHEDULER_CONNECTIONS);
  }

  public String getExecutionSchedulerMaxStatementsPerConnection() {
    return getProperty(EXECUTION_SCHEDULER_MAX_STATEMENTS_PER_CONNECTION);
  }

  public Long getExecutionSchedulerMisfireToleration() {
    return Long.parseLong(getProperty(EXECUTION_SCHEDULER_MISFIRE_TOLERATION));
  }

  public Integer getExecutionSchedulerStartDelay() {
    return Integer.parseInt(getProperty(EXECUTION_SCHEDULER_START_DELAY));
  }

  public Long getExecutionSchedulerWait() {

    String stringValue = getProperty(EXECUTION_SCHEDULER_WAIT);
    long sleepTime = EXECUTION_SCHEDULER_WAIT.getDefaultValue();
    if (stringValue != null) {
      try {
        sleepTime = Long.parseLong(stringValue);
      } catch (NumberFormatException ignored) {
        LOG.warn("Value of {} ({}) should be a number, " +
            "falling back to default value ({})", EXECUTION_SCHEDULER_WAIT.getKey(), stringValue,
            EXECUTION_SCHEDULER_WAIT.getDefaultValue());
      }

    }

    if (sleepTime > 60) {
      LOG.warn("Value of {} ({}) should be a number between 1 adn 60, " +
          "falling back to maximum value ({})",
          EXECUTION_SCHEDULER_WAIT, sleepTime, 60);
      sleepTime = 60L;
    }
    return sleepTime*1000;
  }

  public Integer getExternalScriptTimeout() {
    return Integer.parseInt(getProperty(EXTERNAL_SCRIPT_TIMEOUT));
  }

  //THREAD_POOL_FOR_EXTERNAL_SCRIPT

  /**
   * Get the threadpool size for external script execution
   * @return {Integer}
   */
  public Integer getExternalScriptThreadPoolSize() {
    return Integer.parseInt(getProperty(THREAD_POOL_SIZE_FOR_EXTERNAL_SCRIPT));
  }

  public boolean getParallelStageExecution() {
    return Boolean.parseBoolean(configsMap.get(PARALLEL_STAGE_EXECUTION.getKey()));
  }

  public String getCustomActionDefinitionPath() {
    return getProperty(CUSTOM_ACTION_DEFINITION);
  }

  public int getAgentPackageParallelCommandsLimit() {
    int value = Integer.parseInt(getProperty(AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT));
    if (value < 1) {
      value = 1;
    }
    return value;
  }

  /**
   * @param isPackageInstallationTask true, if task is for installing packages
   * @return default task timeout in seconds (string representation). This value
   *         is used at python (agent) code.
   */
  public String getDefaultAgentTaskTimeout(boolean isPackageInstallationTask) {
    ConfigurationProperty<Long> configurationProperty = isPackageInstallationTask
        ? AGENT_PACKAGE_INSTALL_TASK_TIMEOUT
        : AGENT_TASK_TIMEOUT;

    String key = configurationProperty.getKey();
    Long defaultValue = configurationProperty.getDefaultValue();
    String value = getProperty(configurationProperty);
    if (StringUtils.isNumeric(value)) {
      return value;
    } else {
      LOG.warn(String.format("Value of %s (%s) should be a number, " +
          "falling back to default value (%s)",
        key, value, defaultValue));

      return String.valueOf(defaultValue);
    }
  }

  /**
   * @return overridden service check task timeout in seconds. This value
   *         is used at python (agent) code.
   */
  public Long getAgentServiceCheckTaskTimeout() {
    String value = getProperty(AGENT_SERVICE_CHECK_TASK_TIMEOUT);
    if (StringUtils.isNumeric(value)) {
      return Long.parseLong(value);
    } else {
      LOG.warn("Value of {} ({}) should be a number, falling back to default value ({})",
        AGENT_SERVICE_CHECK_TASK_TIMEOUT.getKey(), value, AGENT_SERVICE_CHECK_TASK_TIMEOUT.getDefaultValue());
      return AGENT_SERVICE_CHECK_TASK_TIMEOUT.getDefaultValue();
    }
  }

  /**
   * @return default server-side task timeout in seconds.
   */
  public Integer getDefaultServerTaskTimeout() {
    String value = getProperty(SERVER_TASK_TIMEOUT);
    if (StringUtils.isNumeric(value)) {
      return Integer.parseInt(value);
    } else {
      LOG.warn("Value of {} ({}) should be a number, falling back to default value ({})",
          SERVER_TASK_TIMEOUT.getKey(), value, SERVER_TASK_TIMEOUT.getDefaultValue());
      return SERVER_TASK_TIMEOUT.getDefaultValue();
    }
  }

  public String getResourceDirPath() {
    return getProperty(RESOURCES_DIR);
  }

  public String getSharedResourcesDirPath(){
    return getProperty(SHARED_RESOURCES_DIR);
  }

  public String getServerJDBCPostgresSchemaName() {
    return getProperty(SERVER_JDBC_POSTGRES_SCHEMA_NAME);
  }

  /**
   * @return max thread pool size for clients, default 25
   */
  public int getClientThreadPoolSize() {
    return Integer.parseInt(getProperty(CLIENT_THREADPOOL_SIZE));
  }

  /**
   * @return max thread pool size for clients, default 10
   */
  public int getSpringMessagingThreadPoolSize() {
    return Integer.parseInt(getProperty(MESSAGING_THREAD_POOL_SIZE));
  }

  /**
   * @return max thread pool size for agents registration, default 10
   */
  public int getRegistrationThreadPoolSize() {
    return Integer.parseInt(getProperty(REGISTRATION_THREAD_POOL_SIZE));
  }

  /**
   * @return max cache size for spring subscription registry.
   */
  public int getSubscriptionRegistryCacheSize() {
    return Integer.parseInt(getProperty(SUBSCRIPTION_REGISTRY_CACHE_MAX_SIZE));
  }

  /**
   * @return queue size for agents in registration.
   */
  public int getAgentsRegistrationQueueSize() {
    return Integer.parseInt(getProperty(AGENTS_REGISTRATION_QUEUE_SIZE));
  }


  /**
   * @return period in seconds with agents reports will be processed.
   */
  public int getAgentsReportProcessingPeriod() {
    return Integer.parseInt(getProperty(AGENTS_REPORT_PROCESSING_PERIOD));
  }

  /**
   * @return timeout in seconds before start processing of agents' reports.
   */
  public int getAgentsReportProcessingStartTimeout() {
    return Integer.parseInt(getProperty(AGENTS_REPORT_PROCESSING_START_TIMEOUT));
  }

  /**
   * @return thread pool size for agents reports processing.
   */
  public int getAgentsReportThreadPoolSize() {
    return Integer.parseInt(getProperty(AGENTS_REPORT_THREAD_POOL_SIZE));
  }

  /**
   * @return server to API STOMP endpoint heartbeat interval in milliseconds.
   */
  public int getAPIHeartbeatInterval() {
    return Integer.parseInt(getProperty(API_HEARTBEAT_INTERVAL));
  }

  /**
   * @return the maximum size of an incoming stomp text message. Default is 2 MB.
   */
  public int getStompMaxIncomingMessageSize() {
    return Integer.parseInt(getProperty(STOMP_MAX_INCOMING_MESSAGE_SIZE));
  }

  /**
   * @return the maximum size of a buffer for stomp message sending. Default is 5 MB.
   */
  public int getStompMaxBufferMessageSize() {
    return Integer.parseInt(getProperty(STOMP_MAX_BUFFER_MESSAGE_SIZE));
  }

  /**
   * @return the number of attempts to emit execution command message to agent. Default is 4
   */
  public int getExecutionCommandsRetryCount() {
    return Integer.parseInt(getProperty(EXECUTION_COMMANDS_RETRY_COUNT));
  }

  /**
   * @return the interval in seconds between attempts to emit execution command message to agent. Default is 15
   */
  public int getExecutionCommandsRetryInterval() {
    return Integer.parseInt(getProperty(EXECUTION_COMMANDS_RETRY_INTERVAL));
  }

  /**
   * @return max thread pool size for agents, default 25
   */
  public int getAgentThreadPoolSize() {
    return Integer.parseInt(getProperty(AGENT_THREADPOOL_SIZE));
  }

  /**
   * Get the view extraction thread pool max size.
   *
   * @return the view extraction thread pool max size
   */
  public int getViewExtractionThreadPoolMaxSize() {
    return Integer.parseInt(getProperty(VIEW_EXTRACTION_THREADPOOL_MAX_SIZE));
  }

  /**
   * Get the view extraction thread pool core size.
   *
   * @return the view extraction thread pool core size
   */
  public int getViewExtractionThreadPoolCoreSize() {
    return Integer.parseInt(getProperty(VIEW_EXTRACTION_THREADPOOL_CORE_SIZE));
  }

  /**
   * Get the maximum number of threads that will be allocated to fulfilling view
   * requests.
   *
   * @return the maximum number of threads that will be allocated for requests
   *         to load views.
   */
  public int getViewRequestThreadPoolMaxSize() {
    return Integer.parseInt(getProperty(VIEW_REQUEST_THREADPOOL_MAX_SIZE));
  }

  public Boolean extractViewsAfterClusterConfig() {
    return Boolean.parseBoolean(getProperty(VIEW_EXTRACT_AFTER_CLUSTER_CONFIG));
  }


  /**
   * Get the time, in ms, that a request to a view will wait for an available
   * thread to handle the request before returning an error.
   *
   * @return the time that requests for a view should wait for an available
   *         thread.
   */
  public int getViewRequestThreadPoolTimeout() {
    return Integer.parseInt(getProperty(VIEW_REQUEST_THREADPOOL_TIMEOUT));
  }

  /**
   * Get property-providers' thread pool core size.
   *
   * @return the property-providers' thread pool core size
   */
  public int getPropertyProvidersThreadPoolCoreSize() {
    return Integer.parseInt(getProperty(PROPERTY_PROVIDER_THREADPOOL_CORE_SIZE));
  }

  /**
   * Get property-providers' thread pool max size.
   *
   * @return the property-providers' thread pool max size
   */
  public int getPropertyProvidersThreadPoolMaxSize() {
    return Integer.parseInt(getProperty(PROPERTY_PROVIDER_THREADPOOL_MAX_SIZE));
  }

  /**
   * Get property-providers' worker queue size. This will return
   * {@link Integer#MAX_VALUE} if not specified which will allow an unbounded
   * queue and essentially a fixed core threadpool size.
   *
   * @return the property-providers' worker queue size.
   */
  public int getPropertyProvidersWorkerQueueSize() {
    return Integer.parseInt(getProperty(PROPERTY_PROVIDER_THREADPOOL_WORKER_QUEUE_SIZE));
  }

  /**
   * Get property-providers' timeout value in milliseconds for waiting on the
   * completion of submitted {@link Callable}s. This will return 5000
   * if not specified.
   *
   * @return the property-providers' completion srevice timeout, in millis.
   */
  public long getPropertyProvidersCompletionServiceTimeout() {
    return Long.parseLong(getProperty(PROPERTY_PROVIDER_THREADPOOL_COMPLETION_TIMEOUT));
  }

  /**
   * Get the view extraction thread pool timeout.
   *
   * @return the view extraction thread pool timeout
   */
  public long getViewExtractionThreadPoolTimeout() {
    return Integer.parseInt(getProperty(VIEW_EXTRACTION_THREADPOOL_TIMEOUT));
  }

  /**
   * Gets the inactivity timeout value, in seconds, for sessions created in
   * Jetty by Spring Security. Without this timeout value, each request to the
   * REST APIs will create new sessions that are never reaped since their
   * default time is -1.
   *
   * @return the time value or {@code 1800} seconds for default.
   */
  public int getHttpSessionInactiveTimeout() {
    return Integer.parseInt(getProperty(SERVER_HTTP_SESSION_INACTIVE_TIMEOUT));
  }

  /**
   * Gets the location of the XML alert template file which contains the
   * velocity templates for outbound notifications.
   *
   * @return the location of the template file, or {@code null} if not defined.
   */
  public String getAlertTemplateFile() {
    return StringUtils.strip(getProperty(ALERT_TEMPLATE_FILE));
  }

  /**
   * @return core thread pool size for AlertEventPublisher, default 2
   */
  public int getAlertEventPublisherCorePoolSize() {
    return Integer.parseInt(getProperty(ALERTS_EXECUTION_SCHEDULER_THREADS_CORE_SIZE));
  }

  /**
   * @return max thread pool size for AlertEventPublisher, default 2
   */
  public int getAlertEventPublisherMaxPoolSize() {
    return Integer.parseInt(getProperty(ALERTS_EXECUTION_SCHEDULER_THREADS_MAX_SIZE));
  }

  /**
   * @return the size of the queue for unhandled alert events
   */
  public int getAlertEventPublisherWorkerQueueSize() {
    return Integer.parseInt(getProperty(ALERTS_EXECUTION_SCHEDULER_WORKER_QUEUE_SIZE));
  }

  /**
   * Get the node recovery type DEFAULT|AUTO_START|FULL
   * @return
   */
  public String getNodeRecoveryType() {
    return getProperty(RECOVERY_TYPE);
  }

  /**
   * Get configured max count of recovery attempt allowed per host component in a window
   * This is reset when agent is restarted.
   * @return
   */
  public String getNodeRecoveryMaxCount() {
    return getProperty(RECOVERY_MAX_COUNT);
  }

  /**
   * Get configured max lifetime count of recovery attempt allowed per host component.
   * This is reset when agent is restarted.
   * @return
   */
  public String getNodeRecoveryLifetimeMaxCount() {
    return getProperty(RECOVERY_LIFETIME_MAX_COUNT);
  }

  /**
   * Get configured window size in minutes
   * @return
   */
  public String getNodeRecoveryWindowInMin() {
    return getProperty(RECOVERY_WINDOW_IN_MIN);
  }

  /**
   * Get the components for which recovery is disabled
   * @return
   */
  public String getRecoveryDisabledComponents() {
    return getProperty(RECOVERY_DISABLED_COMPONENTS);
  }

  /**
   * Get the components for which recovery is enabled
   * @return
   */
  public String getRecoveryEnabledComponents() {
    return getProperty(RECOVERY_ENABLED_COMPONENTS);
  }

  /**
   * Get the configured retry gap between tries per host component
   * @return
   */
  public String getNodeRecoveryRetryGap() {
    return getProperty(RECOVERY_RETRY_GAP);
  }

  /**
   * Gets the default KDC port to use when no port is specified in KDC hostname
   *
   * @return the default KDC port to use.
   */
  public String getDefaultKdcPort() {
    return getProperty(KDC_PORT);
  }

  /**
   * Gets the inactivity timeout value, in milliseconds, for socket connection
   * made to KDC Server for its reachability verification.
   *
   * @return the timeout value as configured in {@code ambari.properties}
   * 				 or {@code 10000 ms} for default.
   */
  public int getKdcConnectionCheckTimeout() {
    return Integer.parseInt(getProperty(KDC_CONNECTION_CHECK_TIMEOUT));
  }

  /**
   * Gets the directory where Ambari is to store cached keytab files.
   *
   * @return a File containing the path to the directory to use to store cached keytab files
   */
  public File getKerberosKeytabCacheDir() {
    return new File(getProperty(KERBEROSTAB_CACHE_DIR));
  }

  /**
   * Determine whether or not ambari server credentials validation is enabled.
   *
   * @return true if ambari server credentials check is enabled
   */
  public boolean isKerberosJaasConfigurationCheckEnabled() {
    return Boolean.parseBoolean(getProperty(KERBEROS_CHECK_JAAS_CONFIGURATION));
  }

  /**
   * Determines whether an existing local users will be skipped on updated during LDAP sync.
   *
   * @return true if ambari need to skip existing user during LDAP sync.
   */
  public LdapUsernameCollisionHandlingBehavior getLdapSyncCollisionHandlingBehavior() {
    return LdapUsernameCollisionHandlingBehavior.translate(
        getProperty(LDAP_SYNC_USERNAME_COLLISIONS_BEHAVIOR),
        LdapUsernameCollisionHandlingBehavior.ADD);
  }

  /**
   * Gets the type of database by examining the {@link #getDatabaseUrl()} JDBC
   * URL.
   *
   * @return the database type (never {@code null}).
   * @throws RuntimeException
   *           if there no known database type.
   */
  public DatabaseType getDatabaseType() {
    String dbUrl = getDatabaseUrl();
    DatabaseType databaseType;

    if (dbUrl.contains(DatabaseType.POSTGRES.getName())) {
      databaseType = DatabaseType.POSTGRES;
    } else if (dbUrl.contains(DatabaseType.ORACLE.getName())) {
      databaseType = DatabaseType.ORACLE;
    } else if (dbUrl.contains(DatabaseType.MYSQL.getName())) {
      databaseType = DatabaseType.MYSQL;
    } else if (dbUrl.contains(DatabaseType.DERBY.getName())) {
      databaseType = DatabaseType.DERBY;
    } else if (dbUrl.contains(DatabaseType.SQL_SERVER.getName())) {
      databaseType = DatabaseType.SQL_SERVER;
    } else if (dbUrl.contains(DatabaseType.SQL_ANYWHERE.getName())) {
      databaseType = DatabaseType.SQL_ANYWHERE;
    } else if (dbUrl.contains(DatabaseType.H2.getName())) {
      databaseType = DatabaseType.H2;
    } else {
      throw new RuntimeException(
        "The database type could be not determined from the JDBC URL "
          + dbUrl);
    }

    return databaseType;
  }

  /**
   * Gets the schema name of database
   *
   * @return the database schema name (can return {@code null} for any DB besides Postgres, MySQL, Oracle).
   */
  public String getDatabaseSchema() {
    DatabaseType databaseType = getDatabaseType();
    String databaseSchema;

    if (databaseType.equals(DatabaseType.POSTGRES)) {
      databaseSchema = getServerJDBCPostgresSchemaName();
    } else if (databaseType.equals(DatabaseType.MYSQL)) {
      databaseSchema = getServerDBName();
    } else if (databaseType.equals(DatabaseType.ORACLE)) {
      databaseSchema = getDatabaseUser();
    } else if (databaseType.equals(DatabaseType.DERBY)) {
      databaseSchema = DEFAULT_DERBY_SCHEMA;
    } else if (databaseType.equals(DatabaseType.H2)) {
      databaseSchema = DEFAULT_H2_SCHEMA;
    } else {
      databaseSchema = null;
    }

    return databaseSchema;
  }

  /**
   * Gets the type of connection pool that EclipseLink should use.
   *
   * @return default of {@link ConnectionPoolType#INTERNAL}.
   */
  public ConnectionPoolType getConnectionPoolType(){
    String connectionPoolType = getProperty(SERVER_JDBC_CONNECTION_POOL);

    if (connectionPoolType.equals(ConnectionPoolType.C3P0.getName())) {
      return ConnectionPoolType.C3P0;
    }

    return ConnectionPoolType.INTERNAL;
  }

  /**
   * Gets the minimum number of connections that should always exist in the
   * connection pool.
   */
  public int getConnectionPoolMinimumSize() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_MIN_SIZE));
  }

  /**
   * Gets the maximum number of connections that should even exist in the
   * connection pool.
   */
  public int getConnectionPoolMaximumSize() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_MAX_SIZE));
  }

  /**
   * Gets the maximum amount of time in seconds any connection, whether its been
   * idle or active, should even be in the pool. This will terminate the
   * connection after the expiration age and force new connections to be opened.
   */
  public int getConnectionPoolMaximumAge() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_MAX_AGE));
  }

  /**
   * Gets the maximum amount of time in seconds that an idle connection can
   * remain in the pool. This should always be greater than the value returned
   * from {@link #getConnectionPoolMaximumExcessIdle()}
   */
  public int getConnectionPoolMaximumIdle() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME));
  }

  /**
   * Gets the maximum amount of time in seconds that connections beyond the
   * minimum pool size should remain in the pool. This should always be less
   * than than the value returned from {@link #getConnectionPoolMaximumIdle()}
   */
  public int getConnectionPoolMaximumExcessIdle() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME_EXCESS));
  }

  /**
   * Gets the number of connections that should be retrieved when the pool size
   * must increase. It's wise to set this higher than 1 since the assumption is
   * that a pool that needs to grow should probably grow by more than 1.
   */
  public int getConnectionPoolAcquisitionSize() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_AQUISITION_SIZE));
  }

  /**
   * Gets the number of times connections should be retried to be acquired from
   * the database before giving up.
   */
  public int getConnectionPoolAcquisitionRetryAttempts() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_ATTEMPTS));
  }

  /**
   * Gets the delay in milliseconds between connection acquire attempts.
   */
  public int getConnectionPoolAcquisitionRetryDelay() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_DELAY));
  }


  /**
   * Gets the number of seconds in between testing each idle connection in the
   * connection pool for validity.
   */
  public int getConnectionPoolIdleTestInternval() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_IDLE_TEST_INTERVAL));
  }

  /**
   * Sets a property on the configuration.
   *
   * @param key
   *          the key (not {@code null}).
   * @param value
   *          the value, or {@code null} to remove it.
   */
  public void setProperty(String key, String value) {
    if (null == value) {
      properties.remove(key);
    } else {
      properties.setProperty(key, value);
    }
  }

  /**
   * Eviction time for entries in metrics cache.
   */
  public int getMetricCacheTTLSeconds() {
    return Integer.parseInt(getProperty(TIMELINE_METRICS_CACHE_TTL));
  }

  /**
   * Max time to idle for entries in the cache.
   */
  public int getMetricCacheIdleSeconds() {
    return Integer.parseInt(getProperty(TIMELINE_METRICS_CACHE_IDLE_TIME));
  }

  /**
   * Separate timeout settings for metrics cache.
   * @return milliseconds
   */
  public int getMetricsRequestReadTimeoutMillis() {
    return Integer.parseInt(getProperty(TIMELINE_METRICS_REQUEST_READ_TIMEOUT));
  }

  /**
   * Separate timeout settings for metrics cache.
   * Timeout on reads for update requests made for smaller time intervals.
   *
   * @return milliseconds
   */
  public int getMetricsRequestIntervalReadTimeoutMillis() {
    return Integer.parseInt(getProperty(TIMELINE_METRICS_REQUEST_INTERVAL_READ_TIMEOUT));
  }

  /**
   * Separate timeout settings for metrics cache.
   * @return milliseconds
   */
  public int getMetricsRequestConnectTimeoutMillis() {
    return Integer.parseInt(getProperty(TIMELINE_METRICS_REQUEST_CONNECT_TIMEOUT));
  }

  /**
   * Diable metrics caching.
   * @return true / false
   */
  public boolean isMetricsCacheDisabled() {
    return Boolean.parseBoolean(getProperty(TIMELINE_METRICS_CACHE_DISABLE));
  }

  /** @see #AMBARISERVER_METRICS_DISABLE */
  public boolean isMetricsServiceDisabled() {
    return Boolean.parseBoolean(getProperty(AMBARISERVER_METRICS_DISABLE));
  }

  /**
   * Constant fudge factor subtracted from the cache update requests to
   * account for unavailability of data on the trailing edge due to buffering.
   */
  public Long getMetricRequestBufferTimeCatchupInterval() {
    return Long.parseLong(getProperty(TIMELINE_METRICS_REQUEST_CATCHUP_INTERVAL));
  }

  /**
   * Percentage of total heap allocated to metrics cache, default is 15%.
   * Default heap setting for the server is 2 GB so max allocated heap size
   * for this cache is 300 MB.
   */
  public String getMetricsCacheManagerHeapPercent() {
    String percent = getProperty(TIMELINE_METRICS_CACHE_HEAP_PERCENT);
    return percent.trim().endsWith("%") ? percent.trim() : percent.trim() + "%";
  }

  /**
   * Allow disabling custom sizing engine.
   */
  public boolean useMetricsCacheCustomSizingEngine() {
    return Boolean.parseBoolean(getProperty(TIMELINE_METRICS_CACHE_USE_CUSTOM_SIZING_ENGINE));
  }

  /**
   * Gets the Kerberos authentication-specific properties container
   *
   * @return an AmbariKerberosAuthenticationProperties
   */
  public AmbariKerberosAuthenticationProperties getKerberosAuthenticationProperties() {
    return kerberosAuthenticationProperties;
  }

  /**
   * Ambari server temp dir
   * @return server temp dir
   */
  public String getServerTempDir() {
    return getProperty(SERVER_TMP_DIR);
  }

  /**
   * If {@code true}, then alerts processed by the {@link AlertReceivedListener}
   * will not write alert data to the database on every event. Instead, data
   * like timestamps and text will be kept in a cache and flushed out
   * periodically to the database.
   * <p/>
   * The default value is {@code false}.
   *
   * @return {@code true} if the cache is enabled, {@code false} otherwise.
   */
  @Experimental(feature = ExperimentalFeature.ALERT_CACHING)
  public boolean isAlertCacheEnabled() {
    return Boolean.parseBoolean(getProperty(ALERTS_CACHE_ENABLED));
  }

  /**
   * Gets the interval at which cached alert data is written out to the
   * database, if enabled.
   */
  @Experimental(feature = ExperimentalFeature.ALERT_CACHING)
  public int getAlertCacheFlushInterval() {
    return Integer.parseInt(getProperty(ALERTS_CACHE_FLUSH_INTERVAL));
  }

  /**
   * Gets the size of the alerts cache, if enabled.
   */
  @Experimental(feature = ExperimentalFeature.ALERT_CACHING)
  public int getAlertCacheSize() {
    return Integer.parseInt(getProperty(ALERTS_CACHE_SIZE));
  }

  /**
   * Get the ambari display URL
   * @return
   */
  public String getAmbariDisplayUrl() {
    return getProperty(AMBARI_DISPLAY_URL);
  }


  /**
   * @return number of retry attempts for api and blueprint operations
   */
  public int getOperationsRetryAttempts() {
    final int RETRY_ATTEMPTS_LIMIT = 10;
    String property = getProperty(OPERATIONS_RETRY_ATTEMPTS);
    int attempts = Integer.parseInt(property);
    if (attempts < 0) {
      LOG.warn("Invalid operations retry attempts number ({}), should be [0,{}]. Value reset to default {}",
          attempts, RETRY_ATTEMPTS_LIMIT, OPERATIONS_RETRY_ATTEMPTS.getDefaultValue());
      attempts = OPERATIONS_RETRY_ATTEMPTS.getDefaultValue();
    } else if (attempts > RETRY_ATTEMPTS_LIMIT) {
      LOG.warn("Invalid operations retry attempts number ({}), should be [0,{}]. Value set to {}",
        attempts, RETRY_ATTEMPTS_LIMIT, RETRY_ATTEMPTS_LIMIT);
      attempts = RETRY_ATTEMPTS_LIMIT;
    }
    if (attempts > 0) {
      LOG.info("Operations retry enabled. Number of retry attempts: {}", attempts);
    }
    return attempts;
  }

  /**
   * @return the connect timeout used when loading a version definition URL.
   */
  public int getVersionDefinitionConnectTimeout() {
    return NumberUtils.toInt(getProperty(VERSION_DEFINITION_CONNECT_TIMEOUT));
  }
  /**
   * @return the read timeout used when loading a version definition URL
   */
  public int getVersionDefinitionReadTimeout() {
    return NumberUtils.toInt(getProperty(VERSION_DEFINITION_READ_TIMEOUT));
  }

  public Boolean getGplLicenseAccepted(){
    Properties actualProps = readConfigFile();
    String defaultGPLAcceptedValue = null;
    if (null != GPL_LICENSE_ACCEPTED.getDefaultValue()) {
      defaultGPLAcceptedValue = String.valueOf(GPL_LICENSE_ACCEPTED.getDefaultValue());
    }
    return Boolean.valueOf(actualProps.getProperty(GPL_LICENSE_ACCEPTED.getKey(), defaultGPLAcceptedValue));
  }

  public String getAgentStackRetryOnInstallCount(){
    return getProperty(AGENT_STACK_RETRY_COUNT);
  }

  public String isAgentStackRetryOnInstallEnabled(){
    return getProperty(AGENT_STACK_RETRY_ON_REPO_UNAVAILABILITY);
  }

  public boolean isAuditLogEnabled() {
    return Boolean.parseBoolean(getProperty(AUDIT_LOG_ENABLED));
  }

  /**
   * @return true if lock profiling is enabled for Ambari Server, in which case LockFactory should create instrumented locks
   */
  public boolean isServerLocksProfilingEnabled() {
    return Boolean.parseBoolean(getProperty(SERVER_LOCKS_PROFILING));
  }

  /**
   * @return the capacity of async audit logger
   */
  public int getAuditLoggerCapacity() {
    return NumberUtils.toInt(getProperty(AUDIT_LOGGER_CAPACITY));
  }

  /**
   * Customized UDP port for SNMP dispatcher
   * @return Integer if property exists else null
   */
  public Integer getSNMPUdpBindPort() {
    String udpPort = getProperty(ALERTS_SNMP_DISPATCH_UDP_PORT);
    return StringUtils.isEmpty(udpPort) ? null : Integer.parseInt(udpPort);
  }

  /**
   * Customized UDP port for Ambari SNMP dispatcher
   * @return Integer if property exists else null
   */
  public Integer getAmbariSNMPUdpBindPort() {
    String udpPort = getProperty(ALERTS_AMBARI_SNMP_DISPATCH_UDP_PORT);
    return StringUtils.isEmpty(udpPort) ? null : Integer.parseInt(udpPort);
  }

  /**
   * Gets the hosts/ports that proxy calls are allowed to be made to.
   *
   * @return
   */
  public String getProxyHostAndPorts() {
    return getProperty(PROXY_ALLOWED_HOST_PORTS);
  }

  /**
   * Gets the number of minutes that data cached by the
   * {@link MetricsRetrievalService} is kept. The longer this value is, the
   * older the data will be when a user first logs in. After that first login,
   * data will be updated by the {@link MetricsRetrievalService} as long as
   * incoming REST requests are made.
   * <p/>
   * It is recommended that this value be longer rather than shorter since the
   * performance benefit of the cache greatly outweighs the data loaded after
   * first login.
   *
   * @return the number of minutes, defaulting to 30 if not specified.
   */
  public int getMetricsServiceCacheTimeout() {
    return Integer.parseInt(getProperty(METRIC_RETRIEVAL_SERVICE_CACHE_TIMEOUT));
  }

  /**
   * Gets the priority of the {@link Thread}s used by the
   * {@link MetricsRetrievalService}. This will be a value within the range of
   * {@link Thread#MIN_PRIORITY} and {@link Thread#MAX_PRIORITY}.
   *
   * @return the thread proprity.
   */
  public int getMetricsServiceThreadPriority() {
    int priority = Integer.parseInt(getProperty(METRIC_RETRIEVAL_SERVICE_THREAD_PRIORITY));
    if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
      priority = Thread.NORM_PRIORITY;
    }

    return priority;
  }

  /**
   * Gets the core pool size used for the {@link MetricsRetrievalService}.
   */
  public int getMetricsServiceThreadPoolCoreSize() {
    return Integer.parseInt(getProperty(METRIC_RETRIEVAL_SERVICE_THREADPOOL_CORE_SIZE));
  }

  /**
   * Gets the max pool size used for the {@link MetricsRetrievalService}.
   * Threads will only be increased up to this value of the worker queue is
   * exhausted and rejects the new task.
   * @see #getMetricsServiceWorkerQueueSize()
   */
  public int getMetricsServiceThreadPoolMaxSize() {
    return Integer.parseInt(getProperty(METRIC_RETRIEVAL_SERVICE_THREADPOOL_MAX_SIZE));
  }

  /**
   * Gets the queue size of the worker queue for the
   * {@link MetricsRetrievalService}.
   *
   * @return the worker queue size, or {@code 10 *}
   *         {@link #getMetricsServiceThreadPoolMaxSize()} if not specified.
   */
  public int getMetricsServiceWorkerQueueSize() {
    return Integer.parseInt(getProperty(METRIC_RETRIEVAL_SERVICE_THREADPOOL_WORKER_QUEUE_SIZE));
  }

  /**
   * Gets the number of seconds that requests made to the same URL will be discarded in order to
   * throttle the retrieval from the same endpoint.
   *
   * @return the number of seconds that must elapse between requests to the same endpoint.
   */
  public int getMetricsServiceRequestTTL() {
    return Integer.parseInt(getProperty(METRIC_RETRIEVAL_SERVICE_REQUEST_TTL));
  }

  /**
   * Gets whether the TTL request cache in the {@link MetricsRetrievalService}
   * is enabled. This evicting cache is used to prevent requests to the same URL
   * within a specified amount of time.
   *
   * @return {@code true} if enabled, {@code false} otherwise.
   */
  public boolean isMetricsServiceRequestTTLCacheEnabled() {
    return Boolean.parseBoolean(getProperty(METRIC_RETRIEVAL_SERVICE_REQUEST_TTL_ENABLED));
  }

  /**
   * Returns the number of tasks that can be queried from the database at once
   * In the case of more tasks, multiple queries are issued
   *
   * @return
   */
  public int getTaskIdListLimit() {
    return Integer.parseInt(getProperty(TASK_ID_LIST_LIMIT));
  }

  /**
   * Get whether the current ambari server instance the active instance
   *
   * @return true / false
   */
  public boolean isActiveInstance() {
    return Boolean.parseBoolean(getProperty(ACTIVE_INSTANCE));
  }

  /**
   * Indicates whether feature for user hook execution is enabled or not.
   *
   * @return true / false (defaults to false)
   */
  public boolean isUserHookEnabled() {
    return Boolean.parseBoolean(getProperty(POST_USER_CREATION_HOOK_ENABLED));
  }

  /**
   * @return the number of threads to use for parallel topology task creation if enabled
   */
  public int getParallelTopologyTaskCreationThreadPoolSize() {
    try {
      return Integer.parseInt(getProperty(TOPOLOGY_TASK_PARALLEL_CREATION_THREAD_COUNT));
    } catch (NumberFormatException e) {
      return TOPOLOGY_TASK_PARALLEL_CREATION_THREAD_COUNT.getDefaultValue();
    }
  }

  /**
   * @return true if parallel execution of task creation is enabled explicitly
   */
  public boolean isParallelTopologyTaskCreationEnabled() {
    return Boolean.parseBoolean(getProperty(TOPOLOGY_TASK_PARALLEL_CREATION_ENABLED));
  }

  /**
   * Get the connect timeout used for connecting to the LogSearch Portal Service
   *
   * @return
   */
  public int getLogSearchPortalConnectTimeout() {
    return NumberUtils.toInt(getProperty(LOGSEARCH_PORTAL_CONNECT_TIMEOUT));
  }

  /**
   * Get the read timeout used for connecting to the LogSearch Portal Service
   *
   * @return
   */
  public int getLogSearchPortalReadTimeout() {
    return NumberUtils.toInt(getProperty(LOGSEARCH_PORTAL_READ_TIMEOUT));
  }

  /**
   * External address of logsearch portal (managed outside of ambari)
   * @return Address string for logsearch portal (e.g.: https://c6401.ambari.apache.org:61888)
   */
  public String getLogSearchPortalExternalAddress() {
    return getProperty(LOGSEARCH_PORTAL_EXTERNAL_ADDRESS);
  }


  /**
   *
   * Get the max time, in hours, to hold data in the LogSearch
   *   metadata cache prior to expiring the cache and re-loading
   *   the data from the LogSearch Portal service.
   *
   * @return max number of hours that the LogSearch metadata is cached
   */
  public int getLogSearchMetadataCacheExpireTimeout() {
    return NumberUtils.toInt(getProperty(LOGSEARCH_METADATA_CACHE_EXPIRE_TIMEOUT));
  }

  /**
   * @return Ephemeral TLS DH key size
   */
  public int getTlsEphemeralDhKeySize() {
    int keySize = NumberUtils.toInt(getProperty(TLS_EPHEMERAL_DH_KEY_SIZE));
    if (keySize == 0) {
      throw new IllegalArgumentException("Invalid " + TLS_EPHEMERAL_DH_KEY_SIZE + " " + getProperty(TLS_EPHEMERAL_DH_KEY_SIZE));
    }
    return keySize;
  }

  /**
   * Gets the dispatch script directory.
   *
   * @return the dispatch script directory
   */
  public String getDispatchScriptDirectory() {
    return getProperty(DISPATCH_PROPERTY_SCRIPT_DIRECTORY);
  }

  /**
   * @return  whether security password encryption is enabled or not (defaults to {@code false})
   */
  public boolean isSecurityPasswordEncryptionEnabled() {
    return Boolean.parseBoolean(getProperty(SECURITY_PASSWORD_ENCRYPTON_ENABLED));
  }

  public boolean isSensitiveDataEncryptionEnabled() {
    return Boolean.parseBoolean(getProperty(SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED));
  }

  public boolean shouldEncryptSensitiveData() {
    return isSecurityPasswordEncryptionEnabled() && isSensitiveDataEncryptionEnabled();
  }

  /**
   * @return default value of number of tasks to run in parallel during upgrades
   */
  public int getDefaultMaxParallelismForUpgrades() {
    return Integer.parseInt(getProperty(DEFAULT_MAX_DEGREE_OF_PARALLELISM_FOR_UPGRADES));
  }

  /**
   * @return The class of the host group strategy for add service requests.
   * @throws ClassNotFoundException if the specified class is not found
   * @throws ClassCastException if the specified class is not a subclass of {@link HostGroupStrategy}
   */
  public Class<? extends HostGroupStrategy> getAddServiceHostGroupStrategyClass() throws ClassNotFoundException {
    return Class.forName(getProperty(ADD_SERVICE_HOST_GROUP_STRATEGY)).asSubclass(HostGroupStrategy.class);
  }

  /**
   * Generates a markdown table which includes:
   * <ul>
   * <li>Property key name</li>
   * <li>Description</li>
   * <li>Default value</li>
   * <li>Recommendended baseline values</li>
   * <ul>
   *
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    final String OUTPUT_ARGUMENT = "output";

    Options options = new Options();
    options.addOption(Option.builder().longOpt(OUTPUT_ARGUMENT).desc(
        "The absolute location of the index.md file to generate").required().type(
            String.class).hasArg().valueSeparator(' ').build());

    CommandLineParser parser = new DefaultParser();
    CommandLine line = parser.parse(options, args);
    String outputFile = (String) line.getParsedOptionValue(OUTPUT_ARGUMENT);

    // first, sort all of the fields by their keys
    SortedMap<String, Field> sortedFields = new TreeMap<>();
    List<Field> fields = new ArrayList<>(500);
    for (Field field : Configuration.class.getFields()) {
      if (field.getType() != ConfigurationProperty.class) {
        continue;
      }

      fields.add(field);
      ConfigurationProperty<?> configurationProperty = (ConfigurationProperty<?>) field.get(null);
      sortedFields.put(configurationProperty.getKey(), field);
    }

    // build the table header
    StringBuilder allPropertiesBuffer = new StringBuilder("| Property Name | Description | Default |");
    allPropertiesBuffer.append(System.lineSeparator());
    allPropertiesBuffer.append("| --- | --- | --- |");
    allPropertiesBuffer.append(System.lineSeparator());

    // now that the keys are ordered, generate  the bulk of the Markdown - the property table
    for (String fieldKey : sortedFields.keySet()) {
      // see which annotation the element has - we only care about the base Markdown one now
      Field field = sortedFields.get(fieldKey);
      Markdown markdown = field.getAnnotation(Markdown.class);
      if( null == markdown ){
        ConfigurationMarkdown configMarkdown = field.getAnnotation(ConfigurationMarkdown.class);
        markdown = configMarkdown.markdown();
      }

      // skip internal properties
      if( markdown.internal() ) {
        continue;
      }

      ConfigurationProperty<?> configurationProperty = (ConfigurationProperty<?>) field.get(null);
      String key = configurationProperty.getKey();
      Object defaultValue = configurationProperty.getDefaultValue();

      // | foo.key | description |
      // build the description, which includes examples and related properties
      allPropertiesBuffer.append("| ").append(key).append(" | ");
      StringBuilder description = new StringBuilder(markdown.description());
      if( StringUtils.isNotEmpty(markdown.relatedTo()) ){
        String relatedTo = String.format(MARKDOWN_RELATED_TO_TEMPLATE, markdown.relatedTo());
        description.append(HTML_BREAK_TAG).append(HTML_BREAK_TAG).append(relatedTo);
      }

      // use HTML to build a list of examples inside of the markdown table
      if( markdown.examples().length > 0 ){
        description.append(HTML_BREAK_TAG).append(HTML_BREAK_TAG);
        description.append("The following are examples of valid values:").append("<ul>");
        for( String example : markdown.examples() ){
          description.append("<li>").append("`").append(example).append("`");
        }
        description.append("</ul>");
      }

      allPropertiesBuffer.append(description);
      allPropertiesBuffer.append(" |");

      // append the default value and then close the table row
      if( null != defaultValue && StringUtils.isNotEmpty(defaultValue.toString()) ){
        allPropertiesBuffer.append("`").append(defaultValue).append("`");
      }

      allPropertiesBuffer.append(" | ").append(System.lineSeparator());
    }

    // now write out specific groupings
    StringBuilder baselineBuffer = new StringBuilder(1024);
    for( ConfigurationGrouping grouping : ConfigurationGrouping.values() ){
      baselineBuffer.append("#### ").append(grouping);
      baselineBuffer.append(System.lineSeparator());
      baselineBuffer.append("| Property Name | ");

      // 10 Hosts | 100 Hosts | 500 Hosts |
      for( ClusterSizeType clusterSizeType : ClusterSizeType.values() ){
        baselineBuffer.append(clusterSizeType).append( " | ");
      }

      // print the table heading separator
      baselineBuffer.append(System.lineSeparator());
      baselineBuffer.append("| --- | --- | --- | --- | --- |");
      baselineBuffer.append(System.lineSeparator());

      for (Field field : fields) {
        ConfigurationMarkdown configMarkdown = field.getAnnotation(ConfigurationMarkdown.class);
        if( null == configMarkdown || configMarkdown.group() != grouping ) {
          continue;
        }

        ConfigurationProperty<?> configurationProperty = (ConfigurationProperty<?>) field.get(null);

        ClusterScale[] scaleValues = configMarkdown.scaleValues();
        SortedMap<ClusterSizeType, String> miniSort = new TreeMap<>();
        for( ClusterScale clusterScale : scaleValues ){
          miniSort.put(clusterScale.clusterSize(), clusterScale.value());
        }

        baselineBuffer.append("| ").append(configurationProperty.getKey()).append(" | ");
        for( ClusterSizeType clusterSizeType : miniSort.keySet() ){
          baselineBuffer.append(miniSort.get(clusterSizeType)).append( " | ");
        }

        baselineBuffer.append(System.lineSeparator());
      }

      baselineBuffer.append(System.lineSeparator());
    }

    // replace the tokens in the markdown template and write out the final MD file
    InputStream inputStream = null;
    try {
      if (System.getProperties().containsKey(AMBARI_CONFIGURATION_MD_TEMPLATE_PROPERTY)) {
        inputStream = new FileInputStream(System.getProperties().getProperty(AMBARI_CONFIGURATION_MD_TEMPLATE_PROPERTY));
      } else {
        inputStream = Configuration.class.getResourceAsStream(MARKDOWN_TEMPLATE_FILE);
      }
      String template = IOUtils.toString(inputStream);
      String markdown = template.replace(MARKDOWN_CONFIGURATION_TABLE_KEY, allPropertiesBuffer.toString());
      markdown = markdown.replace(MARKDOWN_BASELINE_VALUES_KEY, baselineBuffer.toString());

      File file = new File(outputFile);
      FileUtils.writeStringToFile(file, markdown, Charset.defaultCharset());
      System.out.println("Successfully created " + outputFile);
      LOG.info("Successfully created {}", outputFile);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  /**
   * The {@link ConfigurationProperty} class is used to wrap an Ambari property
   * key, type, and default value.
   *
   * @param <T>
   */
  public static class ConfigurationProperty<T> implements Comparable<ConfigurationProperty<?>> {

    private final String m_key;
    private final T m_defaultValue;

    /**
     * Constructor.
     *
     * @param key
     *          the property key name (not {@code null}).
     * @param defaultValue
     *          the default value or {@code null} for none.
     */
    private ConfigurationProperty(String key, T defaultValue) {
      m_key = key;
      m_defaultValue = defaultValue;
    }

    /**
     * Gets the key.
     *
     * @return the key (never {@code null}).
     */
    public String getKey(){
      return m_key;
    }

    /**
     * Gets the default value for this key if its undefined.
     *
     * @return
     */
    public T getDefaultValue() {
      return m_defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return m_key.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      ConfigurationProperty<?> other = (ConfigurationProperty<?>) obj;
      return StringUtils.equals(this.m_key, other.m_key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return m_key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(ConfigurationProperty<?> o) {
      return this.m_key.compareTo(o.m_key);
    }
  }

  /**
   * The {@link ConfigurationGrouping} represents a logical grouping of configurations.
   */
  private enum ConfigurationGrouping {
    /**
     * Alerts & Notifications.
     */
    ALERTS("Alerts & Notifications"),

    /**
     * Jetty API & Agent Thread Pools
     */
    JETTY_THREAD_POOL("Jetty API & Agent Thread Pools");

    /**
     * A description of the grouping.
     */
    private String m_description;

    /**
     * Constructor.
     *
     * @param description
     */
    ConfigurationGrouping(String description){
      m_description = description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return m_description;
    }
  }

  /**
   * The {@link ClusterSizeType} is used to represent fixed sizes of clusters
   * for easy table generation when creating documentation.
   */
  private enum ClusterSizeType {
    /**
     * 10 Hosts.
     */
    HOSTS_10("10 Hosts"),

    /**
     * About 50 hosts.
     */
    HOSTS_50("~50 Hosts"),

    /**
     * About 100 hosts.
     */
    HOSTS_100("~100 Hosts"),

    /**
     * 500 or more hosts.
     */
    HOSTS_500("500+ Hosts");

    /**
     * A description of the number of cluster hosts.
     */
    private String m_description;

    /**
     * Constructor.
     *
     * @param description
     */
    ClusterSizeType(String description){
      m_description = description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return m_description;
    }
  }

  /**
   * The {@link ConfigurationMarkdown} is used to represent more complex
   * Markdown for {@link ConfigurationProperty} fields. It wraps the traditional
   * {@link Markdown} along with extra metadata used to generate documentation.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
  @interface ConfigurationMarkdown {
    /**
     * The base Markdown.
     *
     * @return
     */
    Markdown markdown();

    /**
     * The logic grouping that the configuration property belongs to.
     *
     * @return
     */
    ConfigurationGrouping group();

    /**
     * All of the recommended values for the property based on cluster size.
     *
     * @return
     */
    ClusterScale[] scaleValues() default {};
  }

  /**
   * The {@link ClusterScale} class is a representation of the size of the
   * cluster combined with a value. It's used to represent different
   * configuration values depending on how many hosts are in the cluster.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
  private @interface ClusterScale {
    ClusterSizeType clusterSize();
    String value();
  }

  /**
   * Creates an AmbariKerberosAuthenticationProperties instance containing the Kerberos authentication-specific
   * properties.
   *
   * The relevant properties are processed to set any default values or translate the propery values
   * into usable data for the Kerberos authentication logic.
   *
   * @return
   */
  private AmbariKerberosAuthenticationProperties createKerberosAuthenticationProperties() {
    AmbariKerberosAuthenticationProperties kerberosAuthProperties = new AmbariKerberosAuthenticationProperties();

    kerberosAuthProperties.setKerberosAuthenticationEnabled(Boolean.valueOf(getProperty(KERBEROS_AUTH_ENABLED)));

    // if Kerberos authentication is enabled, continue; else ignore the rest of related properties since
    // they will not be used.
    if (!kerberosAuthProperties.isKerberosAuthenticationEnabled()) {
      return kerberosAuthProperties;
    }

    // Get and process the SPNEGO principal name.  If it exists and contains the host replacement
    // indicator (_HOST), replace it with the hostname of the current host.
    String spnegoPrincipalName = getProperty(KERBEROS_AUTH_SPNEGO_PRINCIPAL);

    if ((spnegoPrincipalName != null) && (spnegoPrincipalName.contains("_HOST"))) {
      String hostName = StageUtils.getHostName();

      if (StringUtils.isEmpty(hostName)) {
        LOG.warn("Cannot replace _HOST in the configured SPNEGO principal name with the host name this host since it is not available");
      } else {
        LOG.info("Replacing _HOST in the configured SPNEGO principal name with the host name this host: {}", hostName);
        spnegoPrincipalName = spnegoPrincipalName.replaceAll("_HOST", hostName);
      }
    }

    kerberosAuthProperties.setSpnegoPrincipalName(spnegoPrincipalName);

    // Validate the SPNEGO principal name to ensure it was set.
    // Log any found issues.
    if (StringUtils.isEmpty(kerberosAuthProperties.getSpnegoPrincipalName())) {
      String message = String.format("The SPNEGO principal name specified in %s is empty. " +
              "This will cause issues authenticating users using Kerberos.",
          KERBEROS_AUTH_SPNEGO_PRINCIPAL.getKey());
      LOG.error(message);
      throw new IllegalArgumentException(message);
    }

    // Get the SPNEGO keytab file. There is nothing special to process for this value.
    kerberosAuthProperties.setSpnegoKeytabFilePath(getProperty(KERBEROS_AUTH_SPNEGO_KEYTAB_FILE));

    // Validate the SPNEGO keytab file to ensure it was set, it exists and it is readable by Ambari.
    // Log any found issues.
    if (StringUtils.isEmpty(kerberosAuthProperties.getSpnegoKeytabFilePath())) {
      String message = String.format("The SPNEGO keytab file path specified in %s is empty. " +
              "This will cause issues authenticating users using Kerberos.",
          KERBEROS_AUTH_SPNEGO_KEYTAB_FILE.getKey());
      LOG.error(message);
      throw new IllegalArgumentException(message);
    } else {
      File keytabFile = new File(kerberosAuthProperties.getSpnegoKeytabFilePath());
      if (!keytabFile.exists()) {
        String message = String.format("The SPNEGO keytab file path (%s) specified in %s does not exist. " +
                "This will cause issues authenticating users using Kerberos. . Make sure proper keytab file provided later.",
            keytabFile.getAbsolutePath(), KERBEROS_AUTH_SPNEGO_KEYTAB_FILE.getKey());
        LOG.error(message);
      } else if (!keytabFile.canRead()) {
        String message = String.format("The SPNEGO keytab file path (%s) specified in %s cannot be read. " +
                "This will cause issues authenticating users using Kerberos. . Make sure proper keytab file provided later.",
            keytabFile.getAbsolutePath(), KERBEROS_AUTH_SPNEGO_KEYTAB_FILE.getKey());
        LOG.error(message);
      }
    }

    // Get the auth-to-local rule set. There is nothing special to process for this value.
    kerberosAuthProperties.setAuthToLocalRules(getProperty(KERBEROS_AUTH_AUTH_TO_LOCAL_RULES));

    LOG.info("Kerberos authentication is enabled:\n " +
            "\t{}: {}\n" +
            "\t{}: {}\n" +
            "\t{}: {}\n" +
            "\t{}: {}\n",
        KERBEROS_AUTH_ENABLED.getKey(),
        kerberosAuthProperties.isKerberosAuthenticationEnabled(),
        KERBEROS_AUTH_SPNEGO_PRINCIPAL.getKey(),
        kerberosAuthProperties.getSpnegoPrincipalName(),
        KERBEROS_AUTH_SPNEGO_KEYTAB_FILE.getKey(),
        kerberosAuthProperties.getSpnegoKeytabFilePath(),
        KERBEROS_AUTH_AUTH_TO_LOCAL_RULES.getKey(),
        kerberosAuthProperties.getAuthToLocalRules());

    return kerberosAuthProperties;
  }

  public int getKerberosOperationRetries() {
    return Integer.parseInt(getProperty(KERBEROS_OPERATION_RETRIES));
  }

  public int getKerberosOperationRetryTimeout() {
    return Integer.parseInt(getProperty(KERBEROS_OPERATION_RETRY_TIMEOUT));
  }

  public boolean validateKerberosOperationSSLCertTrust() {
    return Boolean.parseBoolean(getProperty(KERBEROS_OPERATION_VERIFY_KDC_TRUST));
  }

  /**
   * Return configured acceptors for agent api connector. Default = null
   */
  public Integer getAgentApiAcceptors() {
    String acceptors = getProperty(SRVR_AGENT_ACCEPTOR_THREAD_COUNT);
    return StringUtils.isEmpty(acceptors) ? null : Integer.parseInt(acceptors);
  }

  /**
   * Return configured acceptors for server api connector. Default = null
   */
  public Integer getClientApiAcceptors() {
    String acceptors = getProperty(SRVR_API_ACCEPTOR_THREAD_COUNT);
    return StringUtils.isEmpty(acceptors) ? null : Integer.parseInt(acceptors);
  }

  public String getPamConfigurationFile() {
    return getProperty(PAM_CONFIGURATION_FILE);
  }

  public String getAutoGroupCreation() {
    return getProperty(AUTO_GROUP_CREATION);
  }

  public int getMaxAuthenticationFailures() {
    return Integer.parseInt(getProperty(MAX_LOCAL_AUTHENTICATION_FAILURES));
  }

  public boolean showLockedOutUserMessage() {
    return Boolean.parseBoolean(getProperty(SHOW_LOCKED_OUT_USER_MESSAGE));
  }

  public int getAlertServiceCorePoolSize() {
    return Integer.parseInt(getProperty(SERVER_SIDE_ALERTS_CORE_POOL_SIZE));
  }

  /**
   * @return {@code true} if local files can be specified in the API to consume VDF
   */
  public boolean areFileVDFAllowed() {
    return Boolean.parseBoolean(getProperty(VDF_FROM_FILESYSTEM));
  }
}
