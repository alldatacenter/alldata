/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.test;

import org.apache.hadoop.mapreduce.RssMRUtils;
import org.apache.hadoop.mapreduce.v2.app.RssMRAppMaster;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;

public class FailoverAppMaster {
  public static void main(String[] args) {
    ApplicationAttemptId applicationAttemptId = RssMRUtils.getApplicationAttemptId();
    if (applicationAttemptId.getAttemptId() == 1)  {
      System.exit(1);
    }
    RssMRAppMaster.main(args);
  }
}
