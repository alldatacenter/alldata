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
package org.apache.drill.metastore.mongo.operate;

import com.mongodb.client.ClientSession;
import org.apache.drill.metastore.mongo.MongoMetastoreContext;
import org.apache.drill.metastore.mongo.transform.OperationTransformer;
import org.apache.drill.metastore.operate.AbstractModify;
import org.apache.drill.metastore.operate.Delete;
import org.apache.drill.metastore.operate.MetadataTypeValidator;
import org.apache.drill.metastore.operate.Modify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link Modify} interface based on {@link AbstractModify} parent class.
 * Modifies information in Mongo collection based on given overwrite or delete operations.
 * Executes given operations in one transaction.
 *
 * @param <T> Metastore component unit type
 */
public class MongoModify<T> extends AbstractModify<T> {
  private static final Logger logger = LoggerFactory.getLogger(MongoModify.class);

  private final OperationTransformer<T> transformer;
  private final MongoMetastoreContext<T> context;
  private final List<MongoOperation> operations = new ArrayList<>();

  public MongoModify(MetadataTypeValidator metadataTypeValidator, MongoMetastoreContext<T> context) {
    super(metadataTypeValidator);
    this.context = context;
    this.transformer = context.transformer().operation();
  }

  @Override
  public void execute() {
    if (operations.isEmpty()) {
      return;
    }
    executeOperations(operations);
  }

  @Override
  public void purge() {
    executeOperations(Collections.singletonList(transformer.toDeleteAll()));
  }

  @Override
  protected void addOverwrite(List<T> units) {
    operations.addAll(transformer.toOverwrite(units));
  }

  @Override
  protected void addDelete(Delete delete) {
    operations.add(transformer.toDelete(delete));
  }

  private void executeOperations(List<MongoOperation> operations) {
    ClientSession clientSession = context.client().startSession();
    String res = clientSession.withTransaction(() -> {
      operations.forEach(o -> o.execute(context.table()));
      return String.format("executed %s operations", operations.size());
    });
    logger.debug(res);
  }
}
