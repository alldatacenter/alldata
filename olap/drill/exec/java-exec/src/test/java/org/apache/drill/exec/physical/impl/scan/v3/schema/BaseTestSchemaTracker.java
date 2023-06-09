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
package org.apache.drill.exec.physical.impl.scan.v3.schema;

import java.util.Collection;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.EmptyErrorContext;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanProjectionParser.ProjectionParseResult;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.BaseTest;

public class BaseTestSchemaTracker extends BaseTest {

  protected static final CustomErrorContext ERROR_CONTEXT = EmptyErrorContext.INSTANCE;
  protected static final String MOCK_PROP = "my.prop";
  protected static final String MOCK_VALUE = "my-value";

  protected static final TupleMetadata SCHEMA = new SchemaBuilder()
      .add("a", MinorType.INT)
      .addNullable("b", MinorType.VARCHAR)
      .build();

  protected static final TupleMetadata MAP_SCHEMA = new SchemaBuilder()
      .add("a", MinorType.INT)
      .addMap("m")
        .add("x", MinorType.BIGINT)
        .add("y", MinorType.VARCHAR)
        .resumeSchema()
      .buildSchema();

  protected static final TupleMetadata NESTED_MAP_SCHEMA = new SchemaBuilder()
      .add("a", MinorType.INT)
      .addMap("m")
        .add("x", MinorType.BIGINT)
        .add("y", MinorType.VARCHAR)
        .addMap("m2")
          .add("p", MinorType.BIGINT)
          .add("q", MinorType.VARCHAR)
          .resumeMap()
        .resumeSchema()
      .buildSchema();

  static {
    SCHEMA.metadata("a").setProperty(MOCK_PROP, MOCK_VALUE);
    MAP_SCHEMA.metadata("m").setProperty(MOCK_PROP, MOCK_VALUE);
  }

  protected static ProjectionSchemaTracker trackerFor(Collection<SchemaPath> projList) {
    ProjectionParseResult result = ScanProjectionParser.parse(projList);
    return new ProjectionSchemaTracker(result, true, ERROR_CONTEXT);
  }
}
