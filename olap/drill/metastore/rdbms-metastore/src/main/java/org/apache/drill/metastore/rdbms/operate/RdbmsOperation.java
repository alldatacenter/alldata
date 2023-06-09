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

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * RDBMS operation main goal is to execute SQL code using provided query executor.
 */
public interface RdbmsOperation {

  void execute(DSLContext executor);

  /**
   * Executes overwrite operation steps for the given table.
   * First deletes data based on given delete conditions, than inserts new table records.
   */
  class Overwrite implements RdbmsOperation {

    private static final Logger logger = LoggerFactory.getLogger(Overwrite.class);

    private final Table<? extends Record> table;
    private final List<Condition> deleteConditions;
    private final List<? extends Record> records;

    public Overwrite(Table<? extends Record> table,
                     List<Condition> deleteConditions,
                     List<? extends Record> records) {
      this.table = table;
      this.deleteConditions = deleteConditions;
      this.records = records;
    }

    public Table<? extends Record> table() {
      return table;
    }

    public List<Condition> deleteConditions() {
      return deleteConditions;
    }

    public List<? extends Record> records() {
      return records;
    }

    @Override
    public void execute(DSLContext executor) {
      deleteConditions.forEach(condition -> {
        logger.debug("Deleting data from RDBMS Metastore table {} during overwrite using condition: {}", table, condition);
        executor.deleteFrom(table)
          .where(condition)
          .execute();

      });

      records.forEach(record -> {
        logger.debug("Inserting data into RDBMS Metastore table {}:\n{}", table, record);
        executor.insertInto(table)
          .set(record)
          .execute();
      });
    }
  }

  /**
   * Executes delete operation steps for the given table.
   * Deletes data based on given delete condition.
   */
  class Delete implements RdbmsOperation {

    private static final Logger logger = LoggerFactory.getLogger(Delete.class);

    private final Table<? extends Record> table;
    private final Condition condition;

    public Delete(Table<? extends Record> table) {
      this(table, DSL.trueCondition());
    }

    public Delete(Table<? extends Record> table, Condition condition) {
      this.table = table;
      this.condition = condition;
    }

    public Table<? extends Record> table() {
      return table;
    }

    public Condition condition() {
      return condition;
    }

    @Override
    public void execute(DSLContext executor) {
      logger.debug("Deleting data from RDBMS Metastore table {} using condition: {}", table, condition);
      executor.deleteFrom(table)
        .where(condition)
        .execute();
    }
  }
}
