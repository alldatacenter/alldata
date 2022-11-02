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
package org.apache.ambari.server.controller.utilities.webserver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer;

/**
 *
 */
public class StartServer {

  public static void main(String[] args) throws IOException {
    Map<String, String> mapArgs = parseArgs(args);
    System.out.println("Starting Ambari API server using the following properties: " + mapArgs);
    System.setProperty("ambariapi.dbfile", mapArgs.get("db"));

    ResourceConfig config = new PackagesResourceConfig("org.apache.ambari.server.api.services");
    System.out.println("Starting server: http://localhost:" + mapArgs.get("port") + '/');
    HttpServer server = HttpServerFactory.create("http://localhost:" + mapArgs.get("port") + '/', config);
    server.start();

    System.out.println("SERVER RUNNING: http://localhost:" + mapArgs.get("port") + '/');
    System.out.println("Hit return to stop...");
    System.in.read();
    System.out.println("Stopping server");
    server.stop(0);
    System.out.println("Server stopped");
  }

  private static Map<String, String> parseArgs(String[] args) {
    Map<String, String> mapProps = new HashMap<>();
    mapProps.put("port", "9998");
    mapProps.put("db", "/var/db/hmc/data/data.db");

    for (int i = 0; i < args.length; i += 2) {
      String arg = args[i];
      if (arg.equals("-p")) {
        mapProps.put("port", args[i + 1]);
      } else if (arg.equals("-d")) {
        mapProps.put("db", args[i + 1]);
      } else {
        printUsage();
        throw new RuntimeException("Unexpected argument, See usage message.");
      }
    }
    return mapProps;
  }

  public static void printUsage() {
    System.err.println("Usage: java StartServer [-p portNum] [-d abs path to ambari db file]");
    System.err.println("Default Values: portNum=9998, ambariDb=/var/db/hmc/data/data.db");
  }
}
