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
package org.apache.drill.exec.store.ischema;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.logical.DrillTableSelection;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.ischema.InfoSchemaTable.Catalogs;
import org.apache.drill.exec.store.ischema.InfoSchemaTable.Columns;
import org.apache.drill.exec.store.ischema.InfoSchemaTable.Files;
import org.apache.drill.exec.store.ischema.InfoSchemaTable.Partitions;
import org.apache.drill.exec.store.ischema.InfoSchemaTable.Schemata;
import org.apache.drill.exec.store.ischema.InfoSchemaTable.Tables;
import org.apache.drill.exec.store.ischema.InfoSchemaTable.Views;
import org.apache.drill.exec.store.pojo.PojoRecordReader;
import org.apache.drill.metastore.Metastore;
import org.apache.drill.metastore.MetastoreRegistry;
import org.apache.drill.metastore.exceptions.MetastoreException;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The set of tables / views in INFORMATION_SCHEMA.
 */
public enum InfoSchemaTableType implements DrillTableSelection {

  CATALOGS(new Catalogs()),
  SCHEMATA(new Schemata()),
  VIEWS(new Views()),
  COLUMNS(new Columns()),
  TABLES(new Tables()),
  PARTITIONS(new Partitions()),
  FILES(new Files());

  private static final Logger logger = getLogger(InfoSchemaTableType.class);

  private final InfoSchemaTable<?> tableDef;

  /**
   * Constructor to init {@link InfoSchemaTableType} class.
   *
   * @param  tableDef  the definition (columns and data generator) of the table
   */
  InfoSchemaTableType(InfoSchemaTable<?> tableDef) {
    this.tableDef = tableDef;
  }

  public <S> PojoRecordReader<S> getRecordReader(SchemaPlus rootSchema,
                                                 InfoSchemaFilter filter,
                                                 OptionManager optionManager,
                                                 MetastoreRegistry metastoreRegistry) {
    FilterEvaluator filterEvaluator = filter == null
      ? FilterEvaluator.NoFilterEvaluator.INSTANCE
      : new FilterEvaluator.InfoSchemaFilterEvaluator(filter);

    @SuppressWarnings("unchecked") InfoSchemaRecordGenerator<S> recordGenerator = (InfoSchemaRecordGenerator<S>) tableDef.getRecordGenerator(filterEvaluator);

    recordGenerator.registerRecordCollector(new RecordCollector.BasicRecordCollector(filterEvaluator, optionManager));

    if (optionManager.getBoolean(ExecConstants.METASTORE_ENABLED)) {
      try {
        Metastore metastore = metastoreRegistry.get();
        recordGenerator.registerRecordCollector(new RecordCollector.MetastoreRecordCollector(metastore, filterEvaluator));
      } catch (MetastoreException e) {
        logger.warn("Unable to init Drill Metastore: {}", e.getMessage(), e);
      }
    }

    recordGenerator.scanSchema(rootSchema);
    return recordGenerator.getRecordReader();
  }

  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    return tableDef.getRowType(typeFactory);
  }

  @Override
  public String digest() {
    return toString();
  }
}
