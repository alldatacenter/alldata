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
package com.mapr.drill.maprdb.tests.index;


import com.mapr.db.Admin;
import com.mapr.db.MapRDB;
import org.apache.drill.common.util.GuavaPatcher;

import java.util.HashMap;
import java.util.Map;

/**
* Copy classes to a MapR cluster node, then run a command like this:
* java -classpath /tmp/drill-cmd-1.9.0-SNAPSHOT.jar:/opt/mapr/drill/drill-1.9.0/jars/*:/opt/mapr/drill/drill-1.9.0/jars/3rdparty/*:/opt/mapr/drill/drill-1.9.0/jars/ext/*
*                 org.apache.drill.hbase.index.TableIndexGen -host 10.10.88.128 -port 5181 [-table pop3] [-size 1000000]
*/

class TestBigTable {

  Admin admin;
  boolean initialized = false;

  LargeTableGen gen;

  /*
    "hbase.zookeeper.quorum": "10.10.88.128",
    "hbase.zookeeper.property.clientPort": "5181"
   */
  void init(String host, String port) {
    try {
      admin = MapRDB.newAdmin();
      initialized = true;
      gen = new LargeTableGen(admin);
    } catch (Exception e) {
      System.out.println("Connection to HBase threw" + e.getMessage());
    }
  }
}


public class TableIndexCmd {

  public static Map<String,String> parseParameter(String[] params) {
    HashMap<String,String> retParams = new HashMap<String, String>();
    for (int i=0; i<params.length; ++i) {
      if (params[i].startsWith("-") && i<params.length - 1) {
        String paramName = params[i].replaceFirst("-*", "");
        retParams.put(paramName, params[i+1]);
        ++i;
      }
    }
    return retParams;
  }

  public static void pressEnterKeyToContinue()
  {
    System.out.println("Press any key to continue...");
    try {
      System.in.read();
    } catch(Exception e) {}
  }


  public static void main(String[] args) {
    GuavaPatcher.patch();

    String inHost = new String("localhost");
    String inPort = new String("5181");
    String inTable = new String("/tmp/population");
    String dictPath = "hbase";
    boolean waitKeyPress = true;
    long inSize = 10000;
    Map<String, String> params = parseParameter(args);
    if (args.length >= 2) {
      if (params.get("host") != null) {
        inHost = params.get("host");
      }
      if (params.get("port") != null) {
        inPort = params.get("port");
      }
      if (params.get("table") != null) {
        inTable = params.get("table");
      }
      if (params.get("size") != null) {
        inSize = Long.parseLong(params.get("size"));
      }
      if (params.get("dict") != null) {
        dictPath = params.get("dict");
      }
      if (params.get("wait") != null) {
        String answer = params.get("wait");
        waitKeyPress = answer.startsWith("y") || answer.startsWith("t")? true : false;
      }
    }
    if (waitKeyPress == true) {
      pressEnterKeyToContinue();
    }
    try {
      TestBigTable tbt = new TestBigTable();
      tbt.init(inHost, inPort);
      tbt.gen.generateTableWithIndex(inTable, (int)(inSize & 0xFFFFFFFFL), null);
    } catch(Exception e) {
      System.out.println("generate big table got exception:" + e.getMessage());
      e.printStackTrace();
    }
  }
}
