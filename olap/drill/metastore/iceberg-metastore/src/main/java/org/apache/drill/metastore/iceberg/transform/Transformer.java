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
package org.apache.drill.metastore.iceberg.transform;

/**
 * Provides various mechanism implementations to transform filters, data and operations.
 *
 * @param <T> component unit type
 */
public interface Transformer<T> {

  /**
   * Creates filter transformer. Since filter transformer does not
   * depend on specific Metastore component implementation, provides
   * it as default method.
   *
   * @return filter transformer
   */
  default FilterTransformer filter() {
    return new FilterTransformer();
  }

  /**
   * @return input data transformer for specific Metastore component
   */
  InputDataTransformer<T> inputData();

  /**
   * @return output data transformer for specific Metastore component
   */
  OutputDataTransformer<T> outputData();

  /**
   * @return operation transformer for specific Metastore component
   */
  OperationTransformer<T> operation();
}
