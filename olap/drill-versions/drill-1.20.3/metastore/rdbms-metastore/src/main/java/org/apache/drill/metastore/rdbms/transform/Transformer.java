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
package org.apache.drill.metastore.rdbms.transform;

import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.operate.Delete;
import org.apache.drill.metastore.rdbms.operate.RdbmsOperation;
import org.jooq.Record;

import java.util.List;
import java.util.Set;

/**
 * Provides various methods for RDBMS Metastore data, filters, operations transformation.
 *
 * @param <T> Metastore component metadata type
 */
public interface Transformer<T> {

  /**
   * Returns set of metadata mappers corresponding to the given metadata types.
   *
   * @param metadataTypes set of metadata types
   * @return set of metadata mappers
   */
  Set<MetadataMapper<T, ? extends Record>> toMappers(Set<MetadataType> metadataTypes);

  /**
   * Returns metadata mappers corresponding to the given metadata type.
   *
   * @param metadataType metadata type
   * @return metadata mapper
   */
  MetadataMapper<T, ? extends Record> toMapper(MetadataType metadataType);

  /**
   * Converts given list of Metastore component metadata units into
   * RDBMS Metastore overwrite operations.
   *
   * @param units Metastore metadata units
   * @return list of RDBMS Metastore overwrite operations
   */
  List<RdbmsOperation.Overwrite> toOverwrite(List<T> units);

  /**
   * Converts Metastore delete operation holder into list of
   * RDBMS Metastore delete operations.
   *
   * @param delete Metastore delete operation holder
   * @return list of RDBMS Metastore delete operations
   */
  List<RdbmsOperation.Delete> toDelete(Delete delete);

  /**
   * Creates list of RDBMS Metastore delete operations which will
   * delete all data from corresponding Metastore component tables.
   *
   * @return list of RDBMS Metastore delete operations
   */
  List<RdbmsOperation.Delete> toDeleteAll();
}
