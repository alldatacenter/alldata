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
package org.apache.drill.exec.server.options;

/**
 * Contains information about the scopes in which an option can be set, and an option's visibility.
 */
public class OptionMetaData {
  public static final OptionMetaData DEFAULT = new OptionMetaData(OptionValue.AccessibleScopes.ALL, false, false);

  private final OptionValue.AccessibleScopes type;
  private final  boolean adminOnly;
  private final boolean internal;

  public OptionMetaData(OptionValue.AccessibleScopes type, boolean adminOnly, boolean internal) {
    this.type = type;
    this.adminOnly = adminOnly;
    this.internal = internal;
  }

  public OptionValue.AccessibleScopes getAccessibleScopes() {
    return type;
  }

  public boolean isAdminOnly() {
    return adminOnly;
  }

  public boolean isInternal() {
    return internal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()){
      return false;
    }

    OptionMetaData metaData = (OptionMetaData) o;

    return adminOnly == metaData.adminOnly;
  }

  @Override
  public int hashCode() {
    return (adminOnly ? 1 : 0);
  }
}
