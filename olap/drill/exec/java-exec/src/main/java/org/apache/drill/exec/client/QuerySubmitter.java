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
package org.apache.drill.exec.client;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.coord.zk.ZKClusterCoordinator;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.rpc.user.AwaitableUserResultsListener;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.util.VectorUtil;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;

public class QuerySubmitter {

  public static void main(String args[]) throws Exception {
    QuerySubmitter submitter = new QuerySubmitter();
    Options o = new Options();
    JCommander jc = null;
    try {
      jc = new JCommander(o, args);
      jc.setProgramName("./submit_plan");
    } catch (ParameterException e) {
      System.out.println(e.getMessage());
      String[] valid = {"-f", "file", "-t", "physical"};
      new JCommander(o, valid).usage();
      System.exit(-1);
    }
    if (o.help) {
      jc.usage();
      System.exit(0);
    }

    System.exit(submitter.submitQuery(o.location, o.queryString, o.planType, o.zk, o.local, o.bits, o.format, o.width));
  }

  static class Options {
    @Parameter(names = {"-f", "--file"}, description = "file containing plan", required=false)
    public String location = null;

    @Parameter(names = {"-q", "-e", "--query"}, description = "query string", required = false)
    public String queryString = null;

    @Parameter(names = {"-t", "--type"}, description = "type of query, sql/logical/physical", required=true)
    public String planType;

    @Parameter(names = {"-z", "--zookeeper"}, description = "zookeeper connect string.", required=false)
    public String zk = "localhost:2181";

    @Parameter(names = {"-l", "--local"}, description = "run query in local mode", required=false)
    public boolean local;

    @Parameter(names = {"-b", "--bits"}, description = "number of drillbits to run. local mode only", required=false)
    public int bits = 1;

    @Parameter(names = {"-h", "--help"}, description = "show usage", help=true)
    public boolean help = false;

    @Parameter(names = {"--format"}, description = "output format, csv,tsv,table", required = false)
    public String format = "table";

    @Parameter(names = {"-w", "--width"}, description = "max column width", required = false)
    public int width = VectorUtil.DEFAULT_COLUMN_WIDTH;
  }

  public enum Format {
    TSV, CSV, TABLE
  }

  public int submitQuery(String planLocation, String queryString, String type, String zkQuorum, boolean local, int bits, String format) throws Exception {
      return submitQuery(planLocation, queryString, type, zkQuorum, local, bits, format, VectorUtil.DEFAULT_COLUMN_WIDTH);
  }

  public int submitQuery(String planLocation, String queryString, String type, String zkQuorum, boolean local, int bits, String format, int width) throws Exception {
    DrillConfig config = DrillConfig.create();
    DrillClient client = null;

    Preconditions.checkArgument(!(planLocation == null && queryString == null), "Must provide either query file or query string");
    Preconditions.checkArgument(!(planLocation != null && queryString != null), "Must provide either query file or query string, not both");

    RemoteServiceSet serviceSet = null;
    Drillbit[] drillbits = null;

    try {
      if (local) {
        serviceSet = RemoteServiceSet.getLocalServiceSet();
        drillbits = new Drillbit[bits];
        for (int i = 0; i < bits; i++) {
          drillbits[i] = new Drillbit(config, serviceSet);
          drillbits[i].run();
        }
        client = new DrillClient(config, serviceSet.getCoordinator());
      } else {
        ZKClusterCoordinator clusterCoordinator = new ZKClusterCoordinator(config, zkQuorum);
        clusterCoordinator.start(10000);
        client = new DrillClient(config, clusterCoordinator);
      }
      client.connect();

      String plan;
      if (queryString == null) {
        plan = Charsets.UTF_8.decode(ByteBuffer.wrap(Files.readAllBytes(Paths.get(planLocation)))).toString();
      } else {
        plan = queryString;
      }
      return submitQuery(client, plan, type, format, width);

    } catch(Throwable th) {
      System.err.println("Query Failed due to : " + th.getMessage());
      return -1;
    } finally {
      if (client != null) {
        client.close();
      }
      if (local) {
        for (Drillbit b : drillbits) {
          b.close();
        }
        serviceSet.close();
      }
    }
  }

  public int submitQuery(DrillClient client, String plan, String type, String format, int width) throws Exception {

    String[] queries;
    QueryType queryType;
    type = type.toLowerCase();
    switch (type) {
      case "sql":
        queryType = QueryType.SQL;
        queries = plan.trim().split(";");
        break;
      case "logical":
        queryType = QueryType.LOGICAL;
        queries = new String[]{ plan };
        break;
      case "physical":
        queryType = QueryType.PHYSICAL;
        queries = new String[]{ plan };
        break;
      default:
        System.out.println("Invalid query type: " + type);
        return -1;
    }

    Format outputFormat;
    format = format.toLowerCase();
    switch (format) {
      case "csv":
        outputFormat = Format.CSV;
        break;
      case "tsv":
        outputFormat = Format.TSV;
        break;
      case "table":
        outputFormat = Format.TABLE;
        break;
      default:
        System.out.println("Invalid format type: " + format);
        return -1;
    }
    Stopwatch watch = Stopwatch.createUnstarted();
    for (String query : queries) {
      AwaitableUserResultsListener listener =
          new AwaitableUserResultsListener(new LoggingResultsListener(client.getConfig(), outputFormat, width));
      watch.start();
      client.runQuery(queryType, query, listener);
      int rows = listener.await();
      System.out.println(String.format("%d record%s selected (%f seconds)", rows, rows > 1 ? "s" : "", (float) watch.elapsed(TimeUnit.MILLISECONDS) / (float) 1000));
      if (query != queries[queries.length - 1]) {
        System.out.println();
      }
      watch.stop();
      watch.reset();
    }
    return 0;
  }
}
