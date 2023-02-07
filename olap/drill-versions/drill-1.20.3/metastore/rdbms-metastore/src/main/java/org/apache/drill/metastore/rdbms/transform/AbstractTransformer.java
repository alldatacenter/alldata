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
import org.apache.drill.metastore.rdbms.exception.RdbmsMetastoreException;
import org.apache.drill.metastore.rdbms.operate.RdbmsOperation;
import org.jooq.Condition;
import org.jooq.Record;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract implementation of {@link Transformer} interface which contains
 * common code for all Metastore component metadata types.
 *
 * @param <T> Metastore component metadata type
 */
public abstract class AbstractTransformer<T> implements Transformer<T> {

  @Override
  public List<RdbmsOperation.Delete> toDelete(org.apache.drill.metastore.operate.Delete delete) {
    Set<MetadataMapper<T, ? extends Record>> mappers = toMappers(delete.metadataTypes());
    return mappers.stream()
      .map(mapper -> new RdbmsOperation.Delete(mapper.table(), mapper.toCondition(delete.filter())))
      .collect(Collectors.toList());
  }

  @Override
  public List<RdbmsOperation.Delete> toDeleteAll() {
    Set<MetadataMapper<T, ? extends Record>> mappers = toMappers(Collections.singleton(MetadataType.ALL));
    return mappers.stream()
      .map(MetadataMapper::table)
      .map(RdbmsOperation.Delete::new)
      .collect(Collectors.toList());
  }

  protected RdbmsOperation.Overwrite toOverwrite(String metadataTypeString, List<T> units) {
    MetadataType metadataType = MetadataType.fromValue(metadataTypeString);
    if (metadataType == null) {
      throw new RdbmsMetastoreException("Metadata type must be specified during insert / update");
    } else {
      MetadataMapper<T, ? extends Record> mapper = toMapper(metadataType);

      List<Condition> deleteConditions = mapper.toDeleteConditions(units);

      List<? extends Record> records = units.stream()
        .map(mapper::toRecord)
        .collect(Collectors.toList());

      return new RdbmsOperation.Overwrite(mapper.table(), deleteConditions, records);
    }
  }
}
