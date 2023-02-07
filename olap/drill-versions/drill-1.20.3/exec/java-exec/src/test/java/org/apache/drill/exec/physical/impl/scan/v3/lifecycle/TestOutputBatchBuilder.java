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
package org.apache.drill.exec.physical.impl.scan.v3.lifecycle;

import static org.apache.drill.test.rowSet.RowSetUtilities.mapValue;

import java.util.Arrays;
import java.util.Collections;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.OutputBatchBuilder.BatchSource;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(EvfTest.class)
public class TestOutputBatchBuilder extends SubOperatorTest {

  public TupleMetadata firstSchema() {
    return new SchemaBuilder()
      .add("d", MinorType.VARCHAR)
      .add("a", MinorType.INT)
      .buildSchema();
  }

  private VectorContainer makeFirst(TupleMetadata firstSchema) {
    return fixture.rowSetBuilder(firstSchema)
      .addRow("barney", 10)
      .addRow("wilma", 20)
      .build()
      .container();
  }

  public TupleMetadata secondSchema() {
    return new SchemaBuilder()
      .add("b", MinorType.INT)
      .add("c", MinorType.VARCHAR)
      .buildSchema();
  }

  private VectorContainer makeSecond(TupleMetadata secondSchema) {
    return fixture.rowSetBuilder(secondSchema)
      .addRow(1, "foo.csv")
      .addRow(2, "foo.csv")
      .build()
      .container();
  }

  @Test
  public void testSingleInput() {

    TupleMetadata schema = firstSchema();
    VectorContainer input = makeFirst(schema);

    OutputBatchBuilder builder = new OutputBatchBuilder(schema,
        Collections.singletonList(new BatchSource(schema, input)),
        fixture.allocator());
    builder.load(input.getRecordCount());
    VectorContainer output = builder.outputContainer();

    RowSetUtilities.verify(fixture.wrap(input), fixture.wrap(output));
  }

  @Test
  public void testReorder() {

    TupleMetadata schema = firstSchema();
    VectorContainer input = makeFirst(schema);

    TupleMetadata outputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("d", MinorType.VARCHAR)
        .buildSchema();
    OutputBatchBuilder builder = new OutputBatchBuilder(outputSchema,
        Collections.singletonList(new BatchSource(schema, input)),
        fixture.allocator());
    builder.load(input.getRecordCount());
    VectorContainer output = builder.outputContainer();

    RowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow(10, "barney")
        .addRow(20, "wilma")
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(output));
  }

  @Test
  public void testTwoInputs() {

    TupleMetadata schema1 = firstSchema();
    VectorContainer input1 = makeFirst(schema1);
    TupleMetadata schema2 = secondSchema();
    VectorContainer input2 = makeSecond(schema2);

    TupleMetadata outputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.INT)
        .add("c", MinorType.VARCHAR)
        .add("d", MinorType.VARCHAR)
        .buildSchema();
    OutputBatchBuilder builder = new OutputBatchBuilder(outputSchema,
        Lists.newArrayList(
            new BatchSource(schema1, input1),
            new BatchSource(schema2, input2)),
        fixture.allocator());
    builder.load(input1.getRecordCount());
    VectorContainer output = builder.outputContainer();

    SingleRowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow(10, 1, "foo.csv", "barney")
        .addRow(20, 2, "foo.csv", "wilma")
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(output));
  }

  @Test
  public void testMap() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .addMap("m")
          .add("x", MinorType.INT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    final VectorContainer input = fixture.rowSetBuilder(schema)
        .addRow("barney", mapValue(1, "betty"))
        .addRow("fred", mapValue(2, "wilma"))
        .build()
        .container();

    final OutputBatchBuilder builder = new OutputBatchBuilder(schema,
        Collections.singletonList(new BatchSource(schema, input)),
        fixture.allocator());
    builder.load(input.getRecordCount());
    VectorContainer output = builder.outputContainer();

    RowSetUtilities.verify(fixture.wrap(input), fixture.wrap(output));
  }

  @Test
  public void testTwoMaps() {
    final TupleMetadata schema1 = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .addMap("m")
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    final VectorContainer input1 = fixture.rowSetBuilder(schema1)
        .addRow("barney", mapValue("betty"))
        .addRow("fred", mapValue("wilma"))
        .build()
        .container();

    final TupleMetadata schema2 = new SchemaBuilder()
        .addMap("m")
          .add("x", MinorType.INT)
          .resumeSchema()
        .buildSchema();

    final VectorContainer input2 = fixture.rowSetBuilder(schema2)
        .addSingleCol(mapValue(1))
        .addSingleCol(mapValue(2))
        .build()
        .container();

    final TupleMetadata outputSchema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .addMap("m")
          .add("x", MinorType.INT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    final RowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow("barney", mapValue(1, "betty"))
        .addRow("fred", mapValue(2, "wilma"))
        .build();

    OutputBatchBuilder builder = new OutputBatchBuilder(outputSchema,
        Lists.newArrayList(
            new BatchSource(schema1, input1),
            new BatchSource(schema2, input2)),
        fixture.allocator());
    builder.load(input1.getRecordCount());
    VectorContainer output = builder.outputContainer();

    RowSetUtilities.verify(expected, fixture.wrap(output));
  }

  @Test
  public void testNestedMaps() {
    final TupleMetadata schema1 = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .addMap("m1")
          .add("p", MinorType.VARCHAR)
          .addMap("m2")
            .add("x", MinorType.VARCHAR)
            .resumeMap()
          .resumeSchema()
        .buildSchema();

    final VectorContainer input1 = fixture.rowSetBuilder(schema1)
        .addRow("barney", mapValue("betty", mapValue("pebbles")))
        .addRow("fred", mapValue("wilma", mapValue("bambam")))
        .build()
        .container();

    final TupleMetadata schema2 = new SchemaBuilder()
        .addMap("m1")
          .add("q", MinorType.INT)
          .addMap("m2")
            .add("y", MinorType.INT)
            .resumeMap()
          .resumeSchema()
        .buildSchema();

    final VectorContainer input2 = fixture.rowSetBuilder(schema2)
        .addSingleCol(mapValue(1, mapValue(10)))
        .addSingleCol(mapValue(2, mapValue(20)))
        .build()
        .container();

    final TupleMetadata outputSchema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .addMap("m1")
          .add("p", MinorType.VARCHAR)
          .add("q", MinorType.INT)
          .addMap("m2")
            .add("x", MinorType.VARCHAR)
            .add("y", MinorType.INT)
            .resumeMap()
          .resumeSchema()
        .buildSchema();
    final RowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow("barney", mapValue("betty", 1,  mapValue("pebbles", 10)))
        .addRow("fred", mapValue("wilma", 2, mapValue("bambam", 20)))
        .build();

    OutputBatchBuilder builder = new OutputBatchBuilder(outputSchema,
        Arrays.asList(
            new BatchSource(schema1, input1),
            new BatchSource(schema2, input2)),
        fixture.allocator());
    builder.load(input1.getRecordCount());
    VectorContainer output = builder.outputContainer();

    RowSetUtilities.verify(expected, fixture.wrap(output));
  }
}
