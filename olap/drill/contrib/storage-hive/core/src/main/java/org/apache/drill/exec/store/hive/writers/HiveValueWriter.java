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
package org.apache.drill.exec.store.hive.writers;


/**
 * The writer is used to abstract writing of row values or values embedded into row
 * (for example, elements of List complex type).
 * <p>
 * {@link HiveValueWriterFactory} constructs top level writers for columns, which then mapped to Hive
 * table column and used for writing each column's row value into vectors.
 */
public interface HiveValueWriter {

  /**
   * Accepts row top or embedded value of concrete column and writes it into
   * appropriate value vector.
   *
   * @param value top or embedded row value of Hive column
   */
  void write(Object value);

}
