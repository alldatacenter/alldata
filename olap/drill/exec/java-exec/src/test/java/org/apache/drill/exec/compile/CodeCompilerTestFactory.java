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
package org.apache.drill.exec.compile;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkNotNull;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.LogicalPlanPersistence;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.store.sys.store.provider.LocalPersistentStoreProvider;

public class CodeCompilerTestFactory {
  public static CodeCompiler getTestCompiler(DrillConfig c) throws Exception {
    DrillConfig config = checkNotNull(c);
    LogicalPlanPersistence persistence = new LogicalPlanPersistence(config, ClassPathScanner.fromPrescan(config));
    LocalPersistentStoreProvider provider = new LocalPersistentStoreProvider(config);
    SystemOptionManager systemOptionManager = new SystemOptionManager(persistence, provider, config);
    return new CodeCompiler(config, systemOptionManager.init());
  }
}
