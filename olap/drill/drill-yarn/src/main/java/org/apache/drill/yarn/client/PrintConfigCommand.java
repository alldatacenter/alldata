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
package org.apache.drill.yarn.client;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.drill.yarn.core.DrillOnYarnConfig;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

public class PrintConfigCommand extends ClientCommand {
  @Override
  public void run() {
    // Dump configuration if requested for diagnostic use.

    System.out.println("----------------------------------------------");
    System.out.println("Effective Drill-on-YARN Configuration");
    DrillOnYarnConfig.instance().dump();
    System.out.println("----------------------------------------------");

    // Dump YARN configuration.

    System.out.println("YARN, DFS and Hadoop Configuration");
    YarnConfiguration conf = new YarnConfiguration();
    try {
      YarnConfiguration.dumpConfiguration(conf,
          new OutputStreamWriter(System.out));
      System.out.println();
    } catch (IOException e) {
      // Ignore;
    }
    System.out.println("----------------------------------------------");
  }
}
