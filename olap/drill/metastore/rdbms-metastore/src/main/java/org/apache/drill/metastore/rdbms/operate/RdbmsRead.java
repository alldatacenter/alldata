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

import org.apache.drill.metastore.operate.AbstractRead;
import org.apache.drill.metastore.operate.MetadataTypeValidator;
import org.apache.drill.metastore.operate.Read;
import org.apache.drill.metastore.rdbms.RdbmsMetastoreContext;
import org.apache.drill.metastore.rdbms.exception.RdbmsMetastoreException;
import org.apache.drill.metastore.rdbms.transform.MetadataMapper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation of {@link Read} interface based on {@link AbstractRead} parent class.
 * Reads information from RDBMS tables based on given filter expression.
 * Supports reading information for specific columns.
 *
 * @param <T> Metastore component unit type
 */
public class RdbmsRead<T> extends AbstractRead<T> {

  private static final Logger logger = LoggerFactory.getLogger(RdbmsRead.class);

  private final RdbmsMetastoreContext<T> context;

  public RdbmsRead(MetadataTypeValidator metadataTypeValidator, RdbmsMetastoreContext<T> context) {
    super(metadataTypeValidator);
    this.context = context;
  }

  @Override
  protected List<T> internalExecute() {
    Set<MetadataMapper<T, ? extends Record>> mappers = context.transformer().toMappers(metadataTypes);

    if (mappers.isEmpty()) {
      return Collections.emptyList();
    }

    List<T> units = new ArrayList<>();
    try (DSLContext executor = context.executorProvider().executor()) {
      for (MetadataMapper<T, ? extends Record> mapper : mappers) {
        Condition condition = mapper.toCondition(filter);
        logger.debug("Query data from RDBMS Metastore table {} using condition: {}", mapper.table(), condition);

        List<Field<?>> fields = columns.isEmpty()
          ? Arrays.asList(mapper.table().fields())
          : mapper.toFields(columns);

        try {
          if (fields.isEmpty()) {
            units.addAll(countRecords(executor, mapper, condition));
          } else {
            units.addAll(queryRecords(executor, mapper, fields, condition));
          }
        } catch (DataAccessException e) {
          throw new RdbmsMetastoreException("Error when reading data from Metastore: " + e.getMessage(), e);
        }
      }
      return units;
    }
  }

  /**
   * Counts number of records which qualifies given condition,
   * returns the same number of empty Metastore component units.
   * Is used when no fields matching fields in the table are requested.
   *
   * @param executor query executor
   * @param mapper table mapper
   * @param condition query condition
   * @return list of Metastore component units
   * @throws DataAccessException if unable to query data from table
   */
  private List<T> countRecords(DSLContext executor,
                               MetadataMapper<T, ? extends Record> mapper,
                               Condition condition) throws DataAccessException {
    Integer recordsNumber = executor
      .selectCount()
      .from(mapper.table())
      .where(condition)
      .fetchOne()
      .value1();

    return recordsNumber == null || recordsNumber == 0
      ? Collections.emptyList()
      : IntStream.range(0, recordsNumber)
         .mapToObj(i -> mapper.emptyUnit())
         .collect(Collectors.toList());
  }

  /**
   * Programmatically constructs query to the given table based on filter condition
   * and requested fields, converts the results into the list of Metastore component units.
   *
   * @param executor query executor
   * @param mapper table mapper
   * @param fields list of requested fields
   * @param condition query condition
   * @return list of Metastore component units
   * @throws DataAccessException if unable to query data from table
   */
  private List<T> queryRecords(DSLContext executor,
                               MetadataMapper<T, ? extends Record> mapper,
                               List<Field<?>> fields,
                               Condition condition) throws DataAccessException {
    Result<? extends Record> records = executor
      .select(fields)
      .from(mapper.table())
      .where(condition)
      .fetchInto(mapper.table());

    return records.stream()
      .map(mapper::toUnit)
      .collect(Collectors.toList());
  }
}
