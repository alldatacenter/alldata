/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.maintainer.command;

import org.junit.Assert;
import org.junit.Test;

public class TestPropertyCall extends CallCommandTestBase {

  @Test
  public void test() throws Exception {
    Context context = new Context();
    {
      PropertyCall get = callFactory.generatePropertyCall(
          PropertyCall.PropertyOperate.GET,
          RepairProperty.MAX_ROLLBACK_SNAPSHOT_NUM,
          null);
      Assert.assertEquals("10", get.call(context));
    }

    PropertyCall set = callFactory.generatePropertyCall(PropertyCall.PropertyOperate.SET,
        RepairProperty.MAX_ROLLBACK_SNAPSHOT_NUM, "100");
    set.call(context);

    {
      PropertyCall get = callFactory.generatePropertyCall(
          PropertyCall.PropertyOperate.GET,
          RepairProperty.MAX_ROLLBACK_SNAPSHOT_NUM,
          null);
      Assert.assertEquals("100", get.call(context));
    }

  }
}
