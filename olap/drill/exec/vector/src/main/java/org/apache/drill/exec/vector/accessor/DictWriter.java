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
package org.apache.drill.exec.vector.accessor;

/**
 * Physically the writer is an array writer with special tuple writer as its element.
 * Its {@link #type()} returns {@code ARRAY} and {@link #entryType()} returns {@code TUPLE}.
 * To know whether it is a Dict logically, one may use {@code schema().isDict()}.
 *
 * @see org.apache.drill.exec.vector.accessor.writer.DictEntryWriter
 * @see org.apache.drill.exec.vector.accessor.writer.ObjectDictWriter
 * @see org.apache.drill.exec.vector.complex.DictVector
 */
public interface DictWriter extends ArrayWriter {

  /**
   * Returns scalar type of the key field.
   * @return type of the key
   */
  ValueType keyType();

  /**
   * Returns object type of the value field.
   * @return type of the value
   */
  ObjectType valueType();

  /**
   * Returns the writer associated with key field.
   * @return key writer
   */
  ScalarWriter keyWriter();

  /**
   * Returns the writer associated with value field.
   * @return value writer
   */
  ObjectWriter valueWriter();
}
