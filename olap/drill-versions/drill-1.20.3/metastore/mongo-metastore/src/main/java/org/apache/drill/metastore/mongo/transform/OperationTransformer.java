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
package org.apache.drill.metastore.mongo.transform;

import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.mongo.MongoMetastoreContext;
import org.apache.drill.metastore.mongo.operate.MongoDelete;
import org.apache.drill.metastore.mongo.operate.MongoOperation;
import org.apache.drill.metastore.mongo.operate.Overwrite;
import org.apache.drill.metastore.operate.Delete;

import java.util.List;

/**
 * Base class to transforms given input into {@link MongoOperation} implementations.
 *
 * @param <T> Metastore component unit type
 */
public abstract class OperationTransformer<T> {

  protected final MongoMetastoreContext<T> context;

  protected OperationTransformer(MongoMetastoreContext<T> context) {
    this.context = context;
  }

  public MongoDelete toDeleteAll() {
    return new MongoDelete(context.transformer().filter().transform((FilterExpression) null));
  }

  public MongoDelete toDelete(Delete delete) {
    return new MongoDelete(context.transformer().filter().transform(delete.filter()));
  }

  /**
   * Converts given list of Metastore components units into list of overwrite operations.
   * Specific for each Metastore component.
   *
   * @param units Metastore component units
   * @return list of overwrite operations
   */
  public abstract List<Overwrite> toOverwrite(List<T> units);
}
