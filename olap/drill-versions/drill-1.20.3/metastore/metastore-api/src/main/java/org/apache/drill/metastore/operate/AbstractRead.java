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
package org.apache.drill.metastore.operate;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.metadata.MetadataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract implementation of {@link Read<T>} interface which contains
 * all boilerplace code for collecting metadata types, columns and filter condition.
 * Given metadata types are also validated during execution.
 *
 * @param <T> Metastore metadata unit
 */
public abstract class AbstractRead<T> implements Read<T> {

  protected final Set<MetadataType> metadataTypes = new HashSet<>();
  protected final List<MetastoreColumn> columns = new ArrayList<>();
  protected FilterExpression filter;

  private final MetadataTypeValidator metadataTypeValidator;

  protected AbstractRead (MetadataTypeValidator metadataTypeValidator) {
    this.metadataTypeValidator = metadataTypeValidator;
  }

  @Override
  public Read<T> metadataTypes(Set<MetadataType> metadataTypes) {
    this.metadataTypes.addAll(metadataTypes);
    return this;
  }

  @Override
  public Read<T> filter(FilterExpression filter) {
    this.filter = filter;
    return this;
  }

  @Override
  public Read<T> columns(List<MetastoreColumn> columns) {
    this.columns.addAll(columns);
    return this;
  }

  @Override
  public final List<T> execute() {
    metadataTypeValidator.validate(metadataTypes);
    return internalExecute();
  }

  protected abstract List<T> internalExecute();
}
