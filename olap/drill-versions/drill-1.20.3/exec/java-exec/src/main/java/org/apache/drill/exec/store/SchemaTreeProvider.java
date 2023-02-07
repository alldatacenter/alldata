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
package org.apache.drill.exec.store;

import java.util.List;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.ViewExpansionContext;
import org.apache.calcite.jdbc.DynamicSchema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.store.SchemaConfig.SchemaConfigInfoProvider;
import org.apache.drill.exec.util.ImpersonationUtil;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates new schema trees. It keeps track of newly created schema trees and
 * closes them safely as part of {@link #close()}.
 */
public class SchemaTreeProvider implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(SchemaTreeProvider.class);

  private final DrillbitContext dContext;
  private final List<SchemaPlus> schemaTreesToClose;
  private final boolean isImpersonationEnabled;

  public SchemaTreeProvider(final DrillbitContext dContext) {
    this.dContext = dContext;
    schemaTreesToClose = Lists.newArrayList();
    isImpersonationEnabled = dContext.getConfig().getBoolean(ExecConstants.IMPERSONATION_ENABLED);
  }

  /**
   * Return root schema for process user.
   *
   * @param options list of options
   * @return root of the schema tree
   */
  public SchemaPlus createRootSchema(final OptionManager options) {
    SchemaConfigInfoProvider schemaConfigInfoProvider = new SchemaConfigInfoProvider() {

      @Override
      public ViewExpansionContext getViewExpansionContext() {
        throw new UnsupportedOperationException("View expansion context is not supported");
      }

      @Override
      public OptionValue getOption(String optionKey) {
        return options.getOption(optionKey);
      }

      @Override public SchemaPlus getRootSchema(String userName) {
        return createRootSchema(userName, this);
      }

      @Override public String getQueryUserName() {
        return ImpersonationUtil.getProcessUserName();
      }
    };

    final SchemaConfig schemaConfig = SchemaConfig.newBuilder(
        ImpersonationUtil.getProcessUserName(), schemaConfigInfoProvider)
        .build();

    return createRootSchema(schemaConfig);
  }

  /**
   * Return root schema with schema owner as the given user.
   *
   * @param userName Name of the user who is accessing the storage sources.
   * @param provider {@link SchemaConfigInfoProvider} instance
   * @return Root of the schema tree.
   */
  public SchemaPlus createRootSchema(final String userName, final SchemaConfigInfoProvider provider) {
    final String schemaUser = isImpersonationEnabled ? userName : ImpersonationUtil.getProcessUserName();
    final SchemaConfig schemaConfig = SchemaConfig.newBuilder(schemaUser, provider).build();
    return createRootSchema(schemaConfig);
  }

  /**
   * Create and return a SchemaTree with given <i>schemaConfig</i>.
   * @param schemaConfig
   * @return Root of the schema tree.
   */
  public SchemaPlus createRootSchema(SchemaConfig schemaConfig) {
      SchemaPlus rootSchema = DynamicSchema.createRootSchema(dContext.getStorage(), schemaConfig,
        dContext.getAliasRegistryProvider());
      schemaTreesToClose.add(rootSchema);
      return rootSchema;
  }

  @Override
  public void close() throws Exception {
    for (SchemaPlus sp : schemaTreesToClose) {
      AutoCloseables.closeWithUserException(sp.unwrap(AbstractSchema.class));
    }
  }
}
