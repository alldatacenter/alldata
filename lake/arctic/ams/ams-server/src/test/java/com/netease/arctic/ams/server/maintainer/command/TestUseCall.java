/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.maintainer.command;

import com.netease.arctic.TableTestHelpers;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;

public class TestUseCall extends CallCommandTestBase {

  @Test
  public void test() throws TException, CallCommand.FullTableNameException {
    Context context = new Context();
    callFactory.generateUseCall(TableTestHelpers.TEST_CATALOG_NAME).call(context);
    Assert.assertEquals(TableTestHelpers.TEST_CATALOG_NAME, context.getCatalog());
    callFactory.generateUseCall(TableTestHelpers.TEST_DB_NAME).call(context);
    Assert.assertEquals(TableTestHelpers.TEST_DB_NAME, context.getDb());

    Context context1 = new Context();
    callFactory.generateUseCall(String.format("%s.%s", TableTestHelpers.TEST_CATALOG_NAME, TableTestHelpers.TEST_DB_NAME))
        .call(context1);
    Assert.assertEquals(TableTestHelpers.TEST_CATALOG_NAME, context1.getCatalog());
    Assert.assertEquals(TableTestHelpers.TEST_DB_NAME, context1.getDb());
  }
}
