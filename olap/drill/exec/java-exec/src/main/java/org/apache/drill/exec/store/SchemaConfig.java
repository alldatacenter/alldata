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

import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.exec.ops.ViewExpansionContext;
import org.apache.drill.exec.server.options.OptionValue;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;

/**
 * Contains information needed by
 * {@link org.apache.drill.exec.store.AbstractSchema} implementations.
 */
public class SchemaConfig {
  private final String userName;
  private final SchemaConfigInfoProvider provider;
  private final boolean ignoreAuthErrors;

  private SchemaConfig(final String userName, final SchemaConfigInfoProvider provider, final boolean ignoreAuthErrors) {
    this.userName = userName;
    this.provider = provider;
    this.ignoreAuthErrors = ignoreAuthErrors;
  }

  /**
   * Create new builder.
   * @param userName Name of the user accessing the storage sources.
   * @param provider Implementation {@link SchemaConfigInfoProvider}
   * @return
   */
  public static Builder newBuilder(final String userName, final SchemaConfigInfoProvider provider) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(userName), "A valid userName is expected");
    Preconditions.checkNotNull(provider, "Non-null SchemaConfigInfoProvider is expected");
    return new Builder(userName, provider);
  }

  public static class Builder {
    final String userName;
    final SchemaConfigInfoProvider provider;
    boolean ignoreAuthErrors;

    private Builder(final String userName, final SchemaConfigInfoProvider provider) {
      this.userName = userName;
      this.provider = provider;
    }

    public Builder setIgnoreAuthErrors(boolean ignoreAuthErrors) {
      this.ignoreAuthErrors = ignoreAuthErrors;
      return this;
    }

    public SchemaConfig build() {
      return new SchemaConfig(userName, provider, ignoreAuthErrors);
    }
  }

  /**
   * @return User whom to impersonate as while creating {@link SchemaPlus} instances
   * interact with the underlying storage.
   */
  public String getUserName() {
    return userName;
  }

  /**
   * @return Should ignore if authorization errors are reported while {@link SchemaPlus}
   * instances interact with the underlying storage.
   */
  public boolean getIgnoreAuthErrors() {
    return ignoreAuthErrors;
  }

  public OptionValue getOption(String optionKey) {
    return provider.getOption(optionKey);
  }

  public ViewExpansionContext getViewExpansionContext() {
    return provider.getViewExpansionContext();
  }

  /**
   * Interface to implement to provide required info for {@link org.apache.drill.exec.store.SchemaConfig}
   */
  public interface SchemaConfigInfoProvider {
    ViewExpansionContext getViewExpansionContext();

    SchemaPlus getRootSchema(String userName);

    String getQueryUserName();

    OptionValue getOption(String optionKey);
  }
}
