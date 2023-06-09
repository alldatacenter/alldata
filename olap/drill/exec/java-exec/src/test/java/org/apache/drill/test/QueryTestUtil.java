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
package org.apache.drill.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.client.LoggingResultsListener;
import org.apache.drill.exec.client.QuerySubmitter.Format;
import org.apache.drill.exec.compile.ClassTransformer;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.user.AwaitableUserResultsListener;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.rpc.user.UserResultsListener;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.store.dfs.ZipCodec;
import org.apache.drill.exec.util.VectorUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.drill.test.ClusterTest.dirTestWatcher;
import static org.junit.Assert.assertNotNull;

/**
 * Utilities useful for tests that issue SQL queries.
 */
public class QueryTestUtil {
  private static final Logger logger = LoggerFactory.getLogger(QueryTestUtil.class);

  /**
   * Constructor. All methods are static.
   */
  private QueryTestUtil() {
  }

  /**
   * Create a DrillClient that can be used to query a drill cluster.
   *
   * @param drillConfig
   * @param remoteServiceSet remote service set
   * @param maxWidth maximum width per node
   * @param props Connection properties contains properties such as "user", "password", "schema" etc
   * @return the newly created client
   * @throws RpcException if there is a problem setting up the client
   */
  public static DrillClient createClient(final DrillConfig drillConfig, final RemoteServiceSet remoteServiceSet,
      final int maxWidth, final Properties props) throws RpcException, OutOfMemoryException {
    final DrillClient drillClient = new DrillClient(drillConfig, remoteServiceSet.getCoordinator());
    drillClient.connect(props);

    final List<QueryDataBatch> results = drillClient.runQuery(
        QueryType.SQL, String.format("alter session set `%s` = %d",
            ExecConstants.MAX_WIDTH_PER_NODE_KEY, maxWidth));
    for (QueryDataBatch queryDataBatch : results) {
      queryDataBatch.release();
    }

    return drillClient;
  }

  /**
   * Normalize the query relative to the test environment.
   *
   * <p>Looks for "${WORKING_PATH}" in the query string, and replaces it the current
   * working patch obtained from {@link TestTools#WORKING_PATH}.
   *
   * @param query the query string
   * @return the normalized query string
   */
  public static String normalizeQuery(final String query) {
    if (query.contains("${WORKING_PATH}")) {
      return query.replaceAll(Pattern.quote("${WORKING_PATH}"), Matcher.quoteReplacement(TestTools.WORKING_PATH.toString()));
    } else if (query.contains("[WORKING_PATH]")) {
      return query.replaceAll(Pattern.quote("[WORKING_PATH]"), Matcher.quoteReplacement(TestTools.WORKING_PATH.toString()));
    }
    return query;
  }

  /**
   * Execute a SQL query, and output the results.
   *
   * @param drillClient drill client to use
   * @param type type of the query
   * @param queryString query string
   * @param print True to output results to stdout. False to log results.
   *
   * @return number of rows returned
   * @throws Exception An error while running the query.
   */
  private static int testRunAndOutput(final DrillClient drillClient,
                                      final QueryType type,
                                      final String queryString,
                                      final boolean print) throws Exception {
    final String query = normalizeQuery(queryString);
    DrillConfig config = drillClient.getConfig();
    AwaitableUserResultsListener resultListener =
      new AwaitableUserResultsListener(print ?
      new PrintingResultsListener(config, Format.TSV, VectorUtil.DEFAULT_COLUMN_WIDTH):
      new LoggingResultsListener(config, Format.TSV, VectorUtil.DEFAULT_COLUMN_WIDTH));
    drillClient.runQuery(type, query, resultListener);
    return resultListener.await();
  }

  /**
   * Execute one or more queries separated by semicolons, and output the results.
   *
   * @param drillClient drill client to use
   * @param queryString the query string
   * @param print True to output results to stdout. False to log results.
   * @throws Exception An error while running the query.
   */
  public static void testRunAndOutput(final DrillClient drillClient,
                                      final String queryString,
                                      final boolean print) throws Exception {
    final String query = normalizeQuery(queryString);
    String[] queries = query.split(";");
    for (String q : queries) {
      final String trimmedQuery = q.trim();
      if (trimmedQuery.isEmpty()) {
        continue;
      }
      testRunAndOutput(drillClient, QueryType.SQL, trimmedQuery, print);
    }
  }

  /**
   * Execute a SQL query, and log the results.
   *
   * @param drillClient drill client to use
   * @param type type of the query
   * @param queryString query string
   * @return number of rows returned
   * @throws Exception An error while running the query.
   */
  public static int testRunAndLog(final DrillClient drillClient,
                                  final QueryType type,
                                  final String queryString) throws Exception {
    return testRunAndOutput(drillClient, type, queryString, false);
  }

  /**
   * Execute one or more queries separated by semicolons, and log the results.
   *
   * @param drillClient drill client to use
   * @param queryString the query string
   * @throws Exception An error while running the queries.
   */
  public static void testRunAndLog(final DrillClient drillClient,
                                   final String queryString) throws Exception {
    testRunAndOutput(drillClient, queryString, false);
  }

  /**
   * Execute one or more queries separated by semicolons, and log the results, with the option to
   * add formatted arguments to the query string.
   *
   * @param drillClient drill client to use
   * @param query the query string; may contain formatting specifications to be used by
   *   {@link String#format(String, Object...)}.
   * @param args optional args to use in the formatting call for the query string
   * @throws Exception An error while running the query.
   */
  public static void testRunAndLog(final DrillClient drillClient, final String query, Object... args) throws Exception {
    testRunAndLog(drillClient, String.format(query, args));
  }

  /**
   * Execute a SQL query, and print the results.
   *
   * @param drillClient drill client to use
   * @param type type of the query
   * @param queryString query string
   * @return number of rows returned
   * @throws Exception An error while running the query.
   */
  public static int testRunAndPrint(final DrillClient drillClient,
                                    final QueryType type,
                                    final String queryString) throws Exception {
    return testRunAndOutput(drillClient, type, queryString, true);
  }

  /**
   * Execute one or more queries separated by semicolons, and print the results.
   *
   * @param drillClient drill client to use
   * @param queryString the query string
   * @throws Exception An error while running the queries.
   */
  public static void testRunAndPrint(final DrillClient drillClient,
                                     final String queryString) throws Exception{
    testRunAndOutput(drillClient, queryString, true);
  }

  /**
   * Execute one or more queries separated by semicolons, and print the results, with the option to
   * add formatted arguments to the query string.
   *
   * @param drillClient drill client to use
   * @param query the query string; may contain formatting specifications to be used by
   *   {@link String#format(String, Object...)}.
   * @param args optional args to use in the formatting call for the query string
   * @throws Exception An error while running the query.
   */
  public static void testRunAndPrint(final DrillClient drillClient, final String query, Object... args) throws Exception {
    testRunAndPrint(drillClient, String.format(query, args));
  }

  /**
   * Execute a single query with a user supplied result listener.
   *
   * @param drillClient drill client to use
   * @param type type of query
   * @param queryString the query string
   * @param resultListener the result listener
   */
  public static void testWithListener(final DrillClient drillClient, final QueryType type,
      final String queryString, final UserResultsListener resultListener) {
    final String query = QueryTestUtil.normalizeQuery(queryString);
    drillClient.runQuery(type, query, resultListener);
  }

  /**
   * Set up the options to test the scalar replacement retry option (see
   * ClassTransformer.java). Scalar replacement rewrites bytecode to replace
   * value holders (essentially boxed values) with their member variables as
   * locals. There is still one pattern that doesn't work, and occasionally new
   * ones are introduced. This can be used in tests that exercise failing patterns.
   *
   * <p>This also flushes the compiled code cache.
   *
   * @param drillbit the drillbit
   * @param srOption the scalar replacement option value to use
   * @return the original scalar replacement option setting (so it can be restored)
   */
  public static OptionValue setupScalarReplacementOption(
      final Drillbit drillbit, final ClassTransformer.ScalarReplacementOption srOption) {
    // set the system option
    final DrillbitContext drillbitContext = drillbit.getContext();
    final SystemOptionManager optionManager = drillbitContext.getOptionManager();
    final OptionValue originalOptionValue = optionManager.getOption(ExecConstants.SCALAR_REPLACEMENT_OPTION);
    optionManager.setLocalOption(ExecConstants.SCALAR_REPLACEMENT_OPTION, srOption.name().toLowerCase());

    // flush the code cache
    drillbitContext.getCompiler().flushCache();

    return originalOptionValue;
  }

  /**
   * Restore the original scalar replacement option returned from
   * setupScalarReplacementOption().
   *
   * <p>This also flushes the compiled code cache.
   *
   * @param drillbit the drillbit
   * @param srOption the scalar replacement option value to use
   */
  public static void restoreScalarReplacementOption(final Drillbit drillbit, final String srOption) {
    final DrillbitContext drillbitContext = drillbit.getContext();
    final OptionManager optionManager = drillbitContext.getOptionManager();
    optionManager.setLocalOption(ExecConstants.SCALAR_REPLACEMENT_OPTION, srOption);

    // flush the code cache
    drillbitContext.getCompiler().flushCache();
  }

  /**
   * Checks that port with specified number is free and returns it.
   * Otherwise, increases port number and checks until free port is found
   * or the number of attempts is reached specified numberOfAttempts
   *
   * @param portNumber     initial port number
   * @param numberOfAttempts max number of attempts to find port with greater number
   * @return free port number
   * @throws BindException if free port was not found and all attempts were used.
   */
  public static int getFreePortNumber(int portNumber, int numberOfAttempts) throws IOException {
    for (int i = portNumber; i <= portNumber + numberOfAttempts; i++) {
      try (ServerSocket socket = new ServerSocket(i)) {
        return socket.getLocalPort();
      } catch (BindException e) {
        logger.warn("Port {} is already in use.", i);
      }
    }

    throw new BindException(String.format("Free port could not be found in the range [%s-%s].\n" +
        "Please release any of used ports in this range.", portNumber, portNumber + numberOfAttempts));
  }

  /**
   * Generates a compressed version of the file for testing
   * @param fileName Name of the input file
   * @param codecName The desired CODEC to be used.
   * @param outFileName Name of generated compressed file
   * @throws IOException If function cannot generate file, throws IOException
   */
  public static void generateCompressedFile(String fileName, String codecName, String outFileName) throws IOException {
    FileSystem fs = ExecTest.getLocalFileSystem();
    Configuration conf = fs.getConf();
    conf.set(CommonConfigurationKeys.IO_COMPRESSION_CODECS_KEY, ZipCodec.class.getCanonicalName());
    CompressionCodecFactory factory = new CompressionCodecFactory(conf);

    CompressionCodec codec = factory.getCodecByName(codecName);
    assertNotNull(codecName + " is not found", codec);

    Path outFile = new Path(dirTestWatcher.getRootDir().getAbsolutePath(), outFileName);
    Path inFile = new Path(dirTestWatcher.getRootDir().getAbsolutePath(), fileName);

    try (InputStream inputStream = new FileInputStream(inFile.toUri().toString());
         OutputStream outputStream = codec.createOutputStream(fs.create(outFile))) {
      IOUtils.copyBytes(inputStream, outputStream, fs.getConf(), false);
    }
  }

  /**
   * When writing Drill unit tests, Drill will output strings for dates.  However,
   * these strings must be converted into timestamps (long) for use in unit tests.  This method
   * provides a convenient way to do so.
   * @param dateString An input date string from Drill output
   * @return The datestring in epoch/millis.
   */
  public static long ConvertDateToLong(String dateString) {
    Instant instant = Instant.parse(dateString);
    return instant.toEpochMilli();
  }
}
