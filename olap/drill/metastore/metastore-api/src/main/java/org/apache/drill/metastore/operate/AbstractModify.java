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

import java.util.List;

/**
 * Abstract implementation of {@link Modify<T>} interface which contains
 * all boilerplace code for collecting overwrite units and delete operations.
 * Delete operations metadata types are validated
 * before adding to the list of pending delete operations.
 *
 * @param <T> Metastore metadata unit
 */
public abstract class AbstractModify<T> implements Modify<T> {

  private final MetadataTypeValidator metadataTypeValidator;

  protected AbstractModify(MetadataTypeValidator metadataTypeValidator) {
    this.metadataTypeValidator = metadataTypeValidator;
  }

  @Override
  public final Modify<T> overwrite(List<T> units) {
    addOverwrite(units);
    return this;
  }

  @Override
  public final Modify<T> delete(Delete delete) {
    metadataTypeValidator.validate(delete.metadataTypes());
    addDelete(delete);
    return this;
  }

  /**
   * Adds overwrite operation to the list of pending operations.
   * Is used to ensure operations execution order.
   *
   * @param units list of Metastore metadata units
   */
  protected abstract void addOverwrite(List<T> units);

  /**
   * Adds delete operation to the list of pending operations.
   * Is used to ensure operations execution order.
   *
   * @param delete Metastore delete operation holder
   */
  protected abstract void addDelete(Delete delete);
}
