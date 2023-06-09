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
package org.apache.drill.metastore.rdbms.operate;

import org.apache.drill.metastore.operate.AbstractModify;
import org.apache.drill.metastore.operate.Delete;
import org.apache.drill.metastore.operate.MetadataTypeValidator;
import org.apache.drill.metastore.operate.Modify;
import org.apache.drill.metastore.rdbms.RdbmsMetastoreContext;
import org.apache.drill.metastore.rdbms.exception.RdbmsMetastoreException;
import org.apache.drill.metastore.rdbms.transform.Transformer;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link Modify} interface based on {@link AbstractModify} parent class.
 * Modifies information in RDBMS tables based on given overwrite or delete operations.
 * Executes given operations in one transaction.
 *
 * @param <T> Metastore component unit type
 */
public class RdbmsModify<T> extends AbstractModify<T> {

  private final RdbmsMetastoreContext<T> context;
  private final Transformer<T> transformer;
  private final List<RdbmsOperation> operations = new ArrayList<>();

  public RdbmsModify(MetadataTypeValidator metadataTypeValidator, RdbmsMetastoreContext<T> context) {
    super(metadataTypeValidator);
    this.context = context;
    this.transformer = context.transformer();
  }

  @Override
  public void execute() {
    executeOperations(operations);
  }

  @Override
  public void purge() {
    // not using truncate since we want to rollback if something goes wrong
    List<RdbmsOperation> deletes = new ArrayList<>(transformer.toDeleteAll());
    executeOperations(deletes);
  }

  /**
   * Executes list of provided RDBMS operations in one transaction.
   *
   * @param operations list of RDBMS operations
   */
  private void executeOperations(List<RdbmsOperation> operations) {
    try (DSLContext executor = context.executorProvider().executor()) {
      executor.transaction(configuration -> {
        DSLContext transactionalExecutor = DSL.using(configuration);
        operations.forEach(operation -> operation.execute(transactionalExecutor));
      });
    } catch (RuntimeException e) {
      throw new RdbmsMetastoreException("Error during Metastore modify operation execution: " + e.getMessage(), e);
    }
  }

  @Override
  protected void addOverwrite(List<T> units) {
    operations.addAll(transformer.toOverwrite(units));
  }

  @Override
  protected void addDelete(Delete delete) {
    operations.addAll(transformer.toDelete(delete));
  }
}
