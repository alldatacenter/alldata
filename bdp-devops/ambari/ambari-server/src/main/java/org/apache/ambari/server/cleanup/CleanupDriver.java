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
package org.apache.ambari.server.cleanup;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.jpa.AmbariJpaPersistService;

/**
 * Class in charge for driving the cleanup process.
 */
public class CleanupDriver {
  private static final Logger LOGGER = LoggerFactory.getLogger(CleanupDriver.class);

  private static final String DATE_PATTERN = "yyyy-MM-dd";
  private static final String CLUSTER_NAME_ARG = "cluster-name";
  private static final String FROM_DATE_ARG = "from-date";

  private static Options getOptions() {
    Options options = new Options();
    options.addOption(Option.builder().longOpt(CLUSTER_NAME_ARG).desc("The cluster name").required().type(String.class).hasArg().valueSeparator(' ').build());
    options.addOption(Option.builder().longOpt(FROM_DATE_ARG).desc("Date up until data will be purged.").required().type(String.class).hasArg().valueSeparator(' ').build());
    return options;
  }

  private static CleanupContext processArguments(String... args) {
    CommandLineParser cmdLineParser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    DateFormat df = new SimpleDateFormat(DATE_PATTERN);
    CleanupContext ctx = null;

    try {
      CommandLine line = cmdLineParser.parse(getOptions(), args);
      String clusterName = (String) line.getParsedOptionValue(CLUSTER_NAME_ARG);
      Date fromDate = df.parse(line.getOptionValue(FROM_DATE_ARG));
      ctx = new CleanupContext(clusterName, fromDate.getTime());
    } catch (Exception exp) {
      System.err.println("Parsing failed.  Reason: " + exp.getMessage());
      LOGGER.error("Parsing failed.  Reason: ", exp);
      formatter.printHelp("db-purge-history", getOptions());
      System.exit(1);
    }
    return ctx;
  }


  public static void main(String... args) throws Exception {
    LOGGER.info("DB-PURGE - Starting the database purge process ...");

    CleanupContext cleanupContext = processArguments(args);

    // set up the guice context
    Injector injector = Guice.createInjector(new ControllerModule(), new AuditLoggerModule(), new CleanupModule(), new LdapModule());

    // explicitly starting the persist service
    injector.getInstance(AmbariJpaPersistService.class).start();

    CleanupServiceImpl cleanupService = injector.getInstance(CleanupServiceImpl.class);
    CleanupService.CleanupResult result = cleanupService.cleanup(new TimeBasedCleanupPolicy(cleanupContext.getClusterName(), cleanupContext.getFromDayTimestamp()));

    // explicitly stopping the persist service
    injector.getInstance(AmbariJpaPersistService.class).stop();

    if (result.getErrorCount() > 0) {
      LOGGER.warn("DB-PURGE - completed with error, check Ambari Server log for details ! Number of affected records [{}]", result.getAffectedRows());
      System.exit(2);
    }

    LOGGER.info("DB-PURGE - completed. Number of affected records [{}]", result.getAffectedRows());
  }

  /**
   * Context object that encapsulates values passed in as arguments to the driver class.
   * Represents the input for the cleanup process.
   */
  private static class CleanupContext {
    private String clusterName;
    private Long fromDayTimestamp;

    public CleanupContext(String clusterName, Long fromDayTimestamp) {
      this.clusterName = clusterName;
      this.fromDayTimestamp = fromDayTimestamp;
    }

    public String getClusterName() {
      return clusterName;
    }

    public Long getFromDayTimestamp() {
      return fromDayTimestamp;
    }
  }
}
