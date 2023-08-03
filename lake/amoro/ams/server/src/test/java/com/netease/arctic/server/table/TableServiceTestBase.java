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

package com.netease.arctic.server.table;

import com.netease.arctic.server.utils.Configurations;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;

public abstract class TableServiceTestBase {

  @ClassRule
  public static DerbyPersistence DERBY = new DerbyPersistence();

  private static DefaultTableService TABLE_SERVICE = null;

  @BeforeClass
  public static void initTableService() {
    TABLE_SERVICE = new DefaultTableService(new Configurations());
    TABLE_SERVICE.initialize();
  }

  @AfterClass
  public static void disposeTableService() {
    TABLE_SERVICE.dispose();
  }

  protected DefaultTableService tableService() {
    return TABLE_SERVICE;
  }

}
