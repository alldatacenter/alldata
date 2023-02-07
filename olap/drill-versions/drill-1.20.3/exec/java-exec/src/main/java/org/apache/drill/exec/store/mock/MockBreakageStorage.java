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
package org.apache.drill.exec.store.mock;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.SchemaConfig;

import java.io.IOException;

public class MockBreakageStorage extends MockStorageEngine {

  /**
   * Each storage plugin requires a unique config class to allow
   * config --> impl lookups to be unique.
   */
  public static class MockBreakageStorageEngineConfig extends MockStorageEngineConfig {
    public static final MockBreakageStorageEngineConfig INSTANCE = new MockBreakageStorageEngineConfig("mock:///");

    public MockBreakageStorageEngineConfig(String url) {
      super(url);
    }
  }

  private boolean breakRegister;

  public int registerAttemptCount = 0;

  public MockBreakageStorage(MockBreakageStorageEngineConfig configuration, DrillbitContext context, String name) {
    super(configuration, context, name);
    breakRegister = false;
  }

  public void setBreakRegister(boolean breakRegister) {
    this.breakRegister = breakRegister;
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) throws IOException {
    if (breakRegister) {
      registerAttemptCount++;
      throw new IOException("mock breakRegister!");
    }
    super.registerSchemas(schemaConfig, parent);
  }
}
