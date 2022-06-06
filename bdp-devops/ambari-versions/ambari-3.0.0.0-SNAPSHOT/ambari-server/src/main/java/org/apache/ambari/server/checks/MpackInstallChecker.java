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
package org.apache.ambari.server.checks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;


/**
 * Mpack Install Checker
 */
public class MpackInstallChecker {

  private static final String MPACK_STACKS_ARG = "mpack-stacks";

  private static final Logger LOG = LoggerFactory.getLogger
      (MpackInstallChecker.class);

  private PersistService persistService;
  private DBAccessor dbAccessor;
  private Injector injector;
  private Connection connection;

  private boolean errorsFound = false;

  private static Options getOptions() {
    Options options = new Options();
    options.addOption(Option.builder().longOpt(MPACK_STACKS_ARG).desc(
        "List of stacks defined in the management pack").required().type(String.class).hasArg().valueSeparator(' ').build());
    return options;
  }

  private static MpackContext processArguments(String... args) {
    CommandLineParser cmdLineParser = new DefaultParser();
    MpackContext ctx = null;

    try {
      CommandLine line = cmdLineParser.parse(getOptions(), args);
      String mpackStacksStr = (String) line.getParsedOptionValue(MPACK_STACKS_ARG);
      HashSet<String> stacksInMpack = new HashSet<>(Arrays.asList(mpackStacksStr.split(",")));
      ctx = new MpackContext(stacksInMpack);
    } catch (Exception exp) {
      System.err.println("Parsing failed. Reason: " + exp.getMessage());
      LOG.error("Parsing failed. Reason: ", exp);
      System.exit(1);
    }
    return ctx;
  }

  public boolean isErrorsFound() {
    return errorsFound;
  }

  @Inject
  public MpackInstallChecker(DBAccessor dbAccessor,
      Injector injector,
      PersistService persistService) {
    this.dbAccessor = dbAccessor;
    this.injector = injector;
    this.persistService = persistService;
  }

  /**
   * Extension of audit logger module
   */
  public static class MpackCheckerAuditModule extends AuditLoggerModule {

    public MpackCheckerAuditModule() throws Exception {
    }

    @Override
    protected void configure() {
      super.configure();
    }
  }

  public void startPersistenceService() {
    persistService.start();
  }

  public void stopPersistenceService() {
    persistService.stop();
  }

  public Connection getConnection() {
    if (connection == null) {
      if (dbAccessor == null) {
        dbAccessor = injector.getInstance(DBAccessor.class);
      }
      connection = dbAccessor.getConnection();
    }
    return connection;
  }

  /**
   * Check if any clusters are deployed with a stack that is not included in the management pack
   * @param stacksInMpack List of stacks included in the management pack
   */
  public void checkClusters(HashSet<String> stacksInMpack) {

    ResultSet rs = null;
    Statement statement = null;
    Map<String, Map<String, String>>  clusterStackInfo = new HashMap<>();
    String GET_STACK_NAME_VERSION_QUERY = "select c.cluster_name, s.stack_name, s.stack_version from clusters c " +
        "join stack s on c.desired_stack_id = s.stack_id";

    Connection conn = getConnection();
    try {
      statement = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      rs = statement.executeQuery(GET_STACK_NAME_VERSION_QUERY);
      if (rs != null) {
        while (rs.next()) {
          Map<String, String> stackInfoMap = new HashMap<>();
          stackInfoMap.put(rs.getString("stack_name"), rs.getString("stack_version"));
          clusterStackInfo.put(rs.getString("cluster_name"), stackInfoMap);
        }
      }

      for (Map.Entry<String, Map<String, String>> clusterStackInfoEntry : clusterStackInfo.entrySet()) {
        String clusterName = clusterStackInfoEntry.getKey();
        Map<String, String> stackInfo = clusterStackInfoEntry.getValue();
        String stackName = stackInfo.keySet().iterator().next();
        String stackVersion = stackInfo.get(stackName);
        if(!stacksInMpack.contains(stackName)) {
          String errorMsg = String.format("This Ambari instance is already managing the cluster %s that has the " +
              "%s-%s stack installed on it. The management pack you are attempting to install only contains stack " +
              "definitions for %s. Since this management pack does not contain a stack that has already being " +
              "deployed by Ambari, the --purge option would cause your existing Ambari installation to be unusable. " +
              "Due to that we cannot install this management pack.",
              clusterName, stackName, stackVersion, stacksInMpack.toString());
          LOG.error(errorMsg);
          System.err.println(errorMsg);
          errorsFound = true;
        }
      }
    } catch (SQLException e) {
      System.err.println("SQL Exception occured during check for validating installed clusters. Reason: " + e.getMessage());
      LOG.error("SQL Exception occured during check for validating installed clusters. Reason: ", e);
      errorsFound = true;
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          System.err.println("SQL Exception occurred during result set closing procedure. Reason: " +  e.getMessage());
          LOG.error("SQL Exception occurred during result set closing procedure. Reason: " , e);
          errorsFound = true;
        }
      }
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          System.err.println("SQL Exception occurred during statement closing procedure. Reason: " + e.getMessage());
          LOG.error("SQL Exception occurred during statement closing procedure. Reason: ", e);
          errorsFound = true;
        }
      }
    }
  }

  /**
   * Main method for management pack installation checker
   */
  public static void main(String[] args) throws Exception {

    Injector injector = Guice.createInjector(new ControllerModule(), new MpackCheckerAuditModule(), new LdapModule());
    MpackInstallChecker mpackInstallChecker = injector.getInstance(MpackInstallChecker.class);
    MpackContext mpackContext = processArguments(args);

    mpackInstallChecker.startPersistenceService();

    mpackInstallChecker.checkClusters(mpackContext.getStacksInMpack());

    mpackInstallChecker.stopPersistenceService();

    if(mpackInstallChecker.isErrorsFound()) {
      LOG.error("Mpack installation checker failed!");
      System.err.println("Mpack installation checker failed!");
      System.exit(1);
    } else {
      LOG.info("No errors found");
      System.out.println("No errors found");
    }
  }

  /**
   * Context object that encapsulates values passed in as arguments to the {@link MpackInstallChecker} class.
   */
  private static class MpackContext {
    private HashSet<String> stacksInMpack;

    public MpackContext(HashSet<String> stacksInMpack) {
      this.stacksInMpack = stacksInMpack;
    }

    public HashSet<String> getStacksInMpack() {
      return stacksInMpack;
    }
  }
}
