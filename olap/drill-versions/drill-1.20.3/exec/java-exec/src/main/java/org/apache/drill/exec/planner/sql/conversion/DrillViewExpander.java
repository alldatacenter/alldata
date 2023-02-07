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
package org.apache.drill.exec.planner.sql.conversion;

import java.util.List;

import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DrillViewExpander implements RelOptTable.ViewExpander {
  private static final Logger logger = LoggerFactory.getLogger(DrillViewExpander.class);

  private final SqlConverter sqlConverter;

  DrillViewExpander(SqlConverter sqlConverter) {
    this.sqlConverter = sqlConverter;
  }

  @Override
  public RelRoot expandView(RelDataType rowType, String queryString, List<String> schemaPath, List<String> viewPath) {
    DrillCalciteCatalogReader catalogReader = newCatalogReader(sqlConverter.getRootSchema(), schemaPath);
    SqlConverter parser = new SqlConverter(sqlConverter, sqlConverter.getDefaultSchema(),
        sqlConverter.getRootSchema(), catalogReader);
    return convertToRel(queryString, parser);
  }

  @Override
  public RelRoot expandView(RelDataType rowType, String queryString, SchemaPlus rootSchema, List<String> schemaPath) {
    final DrillCalciteCatalogReader catalogReader = newCatalogReader(rootSchema, schemaPath);
    SchemaPlus schema = findSchema(queryString, rootSchema, schemaPath);
    SqlConverter parser = new SqlConverter(sqlConverter, schema, rootSchema, catalogReader);
    return convertToRel(queryString, parser);
  }

  private RelRoot convertToRel(String queryString, SqlConverter converter) {
    converter.disallowTemporaryTables();
    final SqlNode parsedNode = converter.parse(queryString);
    final SqlNode validatedNode = converter.validate(parsedNode);
    return converter.toRel(validatedNode);
  }

  private DrillCalciteCatalogReader newCatalogReader(SchemaPlus rootSchema, List<String> schemaPath) {
    return new DrillCalciteCatalogReader(
        rootSchema,
        sqlConverter.isCaseSensitive(),
        schemaPath,
        sqlConverter.getTypeFactory(),
        sqlConverter.getDrillConfig(),
        sqlConverter.getSession(),
        sqlConverter.getTemporarySchema(),
        sqlConverter::useRootSchema,
        sqlConverter::getDefaultSchema);
  }

  private SchemaPlus findSchema(String queryString, SchemaPlus rootSchema, List<String> schemaPath) {
    SchemaPlus schema = rootSchema;
    for (String s : schemaPath) {
      SchemaPlus newSchema = schema.getSubSchema(s);
      if (newSchema == null) {
        throw UserException
            .validationError()
            .message("Failure while attempting to expand view. Requested schema %s not available in schema %s.",
                s, schema.getName())
            .addContext("View Context", Joiner.on(", ").join(schemaPath))
            .addContext("View SQL", queryString)
            .build(logger);
      }
      schema = newSchema;
    }
    return schema;
  }
}
