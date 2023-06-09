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
package org.apache.drill.metastore.rdbms;

import org.apache.drill.metastore.rdbms.transform.Transformer;

/**
 * Provides RDBMS Metastore component tools to transform, read or write data from / into RDBMS tables.
 *
 * @param <T> Metastore component unit metadata type
 */
public interface RdbmsMetastoreContext<T> {

  /**
   * Return executor provider which allows to execute SQL queries
   * using configured data source.
   *
   * @return executor provider instance
   */
  QueryExecutorProvider executorProvider();

  /**
   * Returns transformer which allows various
   * data, filters, operations transformation.
   *
   * @return transformer instance
   */
  Transformer<T> transformer();
}
