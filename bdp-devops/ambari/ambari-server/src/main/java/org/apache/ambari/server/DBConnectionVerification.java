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

package org.apache.ambari.server;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnectionVerification {
  public static void main(String[] args) throws Exception {
    String url = args[0];
    String username = args[1];
    String password = args[2];
    String driver = args[3];
 
    Connection conn = null;
    try {
       Class.forName(driver);
       if(url.contains("integratedSecurity=true")) {
         conn = DriverManager.getConnection(url);
       } else {
         conn = DriverManager.getConnection(url, username, password);
       }
       System.out.println("Connected to DB Successfully!");
    } catch (Throwable e) {
       System.out.println("ERROR: Unable to connect to the DB. Please check DB connection properties.");
       System.out.println(e);
       System.exit(1);
    } finally {
       conn.close();
    }
  }
}
