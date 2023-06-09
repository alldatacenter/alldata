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
package org.apache.drill.exec.physical.resultSet.impl;

import static org.apache.drill.test.rowSet.RowSetUtilities.map;
import static org.apache.drill.test.rowSet.RowSetUtilities.objArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.validate.BatchValidator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.DictWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.complex.DictVector;
import org.apache.drill.exec.vector.complex.RepeatedDictVector;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test dict array support in the result set loader.
 */
@Category(RowSetTests.class)
public class TestResultSetLoaderDictArray extends SubOperatorTest {

  @Test
  public void testBasics() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addDictArray("d", MinorType.INT)
          .value(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    // Verify structure and schema
    TupleMetadata actualSchema = rootWriter.tupleSchema();
    assertEquals(2, actualSchema.size());
    assertTrue(actualSchema.metadata(1).isArray());
    assertTrue(actualSchema.metadata(1).isDict());
    assertEquals(2, actualSchema.metadata("d").tupleSchema().size());
    assertEquals(2, actualSchema.column("d").getChildren().size());
    DictWriter dictWriter = rootWriter.array("d").dict();
    assertSame(actualSchema.metadata("d").tupleSchema(), dictWriter.schema().tupleSchema());

    // Write a couple of rows with arrays.
    rsLoader.startBatch();
    rootWriter
        .addRow(10, objArray(
            map(110, "d1.1", 111, "d1.2", 112, "d1.3"),
            map(120, "d2.2"))
        )
        .addRow(20, objArray())
        .addRow(30, objArray(
            map(310, "d3.1", 311, "d3.2", 313, "d3.4", 317, "d3.9"),
            map(320, "d4.2"),
            map(332, "d5.1", 339, "d5.5", 337, "d5.6"))
        );

    // Verify the batch
    RowSet actual = fixture.wrap(rsLoader.harvest());
    RepeatedDictVector repeatedDictVector = (RepeatedDictVector) actual.container().getValueVector(1).getValueVector();
    MaterializedField dictArrayField = repeatedDictVector.getField(); // RepeatedDictVector contains one child - DictVector
    assertEquals(1, dictArrayField.getChildren().size());
    DictVector dictVector = (DictVector) repeatedDictVector.getDataVector();
    Iterator<MaterializedField> iter = dictVector.getField().getChildren().iterator();
    assertTrue(dictWriter.keyWriter().schema().schema().isEquivalent(iter.next()));
    assertTrue(dictWriter.valueWriter().scalar().schema().schema().isEquivalent(iter.next()));

    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, objArray(
            map(110, "d1.1", 111, "d1.2", 112, "d1.3"),
            map(120, "d2.2"))
        )
        .addRow(20, objArray())
        .addRow(30, objArray(
            map(310, "d3.1", 311, "d3.2", 313, "d3.4", 317, "d3.9"),
            map(320, "d4.2"),
            map(332, "d5.1", 339, "d5.5", 337, "d5.6"))
        )
        .build();
    RowSetUtilities.verify(expected, actual);

    rsLoader.close();
  }

  @Test
  public void testArrayValue() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addDictArray("d", MinorType.INT)
          .repeatedValue(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    // Write a couple of rows
    rsLoader.startBatch();
    rootWriter
        .addRow(10, objArray(
            map(110, strArray("d1.1.1", "d1.1.2"), 111, strArray("d1.1.3", "d1.1.4"), 112, strArray("d1.1.5", "d1.1.6")),
            map(120, strArray("d1.2.1", "d1.2.2"))))
        .addRow(20, objArray())
        .addRow(30, objArray(
            map(310, strArray("d3.1.1", "d3.2.2"), 311, strArray("d3.1.3", "d3.2.4", "d3.1.5", "d3.1.6")),
            map(320, strArray(), 321, strArray("d3.2.2")),
            map(330, strArray("d3.3.1", "d1.2.2"))));

    // Verify the batch
    RowSet actual = fixture.wrap(rsLoader.harvest());
    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, objArray(
            map(110, strArray("d1.1.1", "d1.1.2"), 111, strArray("d1.1.3", "d1.1.4"), 112, strArray("d1.1.5", "d1.1.6")),
            map(120, strArray("d1.2.1", "d1.2.2"))))
        .addRow(20, objArray())
        .addRow(30, objArray(
            map(310, strArray("d3.1.1", "d3.2.2"), 311, strArray("d3.1.3", "d3.2.4", "d3.1.5", "d3.1.6")),
            map(320, strArray(), 321, strArray("d3.2.2")),
            map(330, strArray("d3.3.1", "d1.2.2"))))
        .build();
    RowSetUtilities.verify(expected, actual);

    rsLoader.close();
  }

  @Test
  public void testScalarValue() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addDictArray("d", MinorType.VARCHAR)
          .value(MinorType.INT)
          .resumeSchema()
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    // Write a couple of rows
    rsLoader.startBatch();
    rootWriter
        .addRow(10, objArray(
            map("a", 1, "b", 2, "d", 4),
            map("a", 2, "c", 3, "d", 1, "e", 4)
        ))
        .addRow(20, objArray())
        .addRow(30, objArray(
            map("a", 2, "c", 4, "d", 5, "e", 6, "f", 11),
            map("a", 1, "d", 6, "c", 3),
            map("b", 2, "a", 3))
        );

    // Verify the batch
    RowSet actual = fixture.wrap(rsLoader.harvest());
    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, objArray(
            map("a", 1, "b", 2, "d", 4),
            map("a", 2, "c", 3, "d", 1, "e", 4)
        ))
        .addRow(20, objArray())
        .addRow(30, objArray(
            map("a", 2, "c", 4, "d", 5, "e", 6, "f", 11),
            map("a", 1, "d", 6, "c", 3),
            map("b", 2, "a", 3))
        )
        .build();
    RowSetUtilities.verify(expected, actual);

    rsLoader.close();
  }

  @Test
  public void testDictValue() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addDictArray("d", MinorType.VARCHAR)
          .dictValue()
            .key(MinorType.VARCHAR)
            .value(MinorType.VARCHAR)
            .resumeDict()
        .resumeSchema()
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    // Write a couple of rows
    rsLoader.startBatch();
    rootWriter
        .addRow(10, objArray(
            map("a", map("a", "a1", "b", "a2", "c", "a3"), "b", map("d", "a4"), "c", map()),
            map("b", map("b", "a2"))
        ))
        .addRow(20, objArray())
        .addRow(30, objArray(
            map("a", map("a", "b1", "b", "b1")),
            map("b", map("e", "b2"), "a", map("h", "b1", "g", "b3"), "c", map("a", "b4")),
            map("b", map("a", "b3", "c", "c3"), "a", map())));

    // Verify the batch
    RowSet actual = fixture.wrap(rsLoader.harvest());
    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, objArray(
            map("a", map("a", "a1", "b", "a2", "c", "a3"), "b", map("d", "a4"), "c", map()),
            map("b", map("b", "a2"))
        ))
        .addRow(20, objArray())
        .addRow(30, objArray(
            map("a", map("a", "b1", "b", "b1")),
            map("b", map("e", "b2"), "a", map("h", "b1", "g", "b3"), "c", map("a", "b4")),
            map("b", map("a", "b3", "c", "c3"), "a", map())))
        .build();
    RowSetUtilities.verify(expected, actual);

    rsLoader.close();
  }

  /**
   * Test that memory is released if the loader is closed with an active
   * batch (that is, before the batch is harvested.)
   */
  @Test
  public void testCloseWithoutHarvest() {
    TupleMetadata schema = new SchemaBuilder()
        .addDictArray("d", MinorType.INT)
          .value(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    ArrayWriter arrayWriter = rootWriter.array("d");
    DictWriter dictWriter = arrayWriter.dict();
    rsLoader.startBatch();
    for (int i = 0; i < 40; i++) {
      rootWriter.start();
      for (int j = 0; j < 3; j++) {
        dictWriter.keyWriter().setInt(i);
        dictWriter.valueWriter().scalar().setString("b-" + i);
        arrayWriter.save();
      }
      rootWriter.save();
    }

    // Don't harvest the batch. Allocator will complain if the
    // loader does not release memory.
    rsLoader.close();
  }

  @Test
  public void testKeyOverflow() {
    TupleMetadata schema = new SchemaBuilder()
        .addDictArray("d", MinorType.VARCHAR)
          .value(MinorType.INT)
          .resumeSchema()
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    byte[] key = new byte[523];
    Arrays.fill(key, (byte) 'X');

    int arraySize = 3; // number of dicts in each row
    int dictSize = 1; // number of entries in each dict

    // Number of rows should be driven by vector size.
    // Our row count should include the overflow row
    ArrayWriter arrayDictWriter = rootWriter.array(0);
    DictWriter dictWriter = arrayDictWriter.dict();
    ScalarWriter keyWriter = dictWriter.keyWriter();
    ScalarWriter valueWriter = dictWriter.valueWriter().scalar();

    int expectedCount = ValueVector.MAX_BUFFER_SIZE / (key.length * dictSize * arraySize);
    //System.out.println("expectedCoutn: " + expectedCount);
    {
      int count = 0;
      while (! rootWriter.isFull()) {
        rootWriter.start();
        for (int i = 0; i < arraySize; i++) {
          for (int j = 0; j < dictSize; j++) {
            keyWriter.setBytes(key, key.length);
            valueWriter.setInt(0); // acts as a placeholder, the actual value is not important
            dictWriter.save(); // not necessary for scalars, just for completeness
          }
          arrayDictWriter.save();
        }
        rootWriter.save();
        count++;
      }

      assertEquals(expectedCount + 1, count);
      //System.out.println("count: " + count);

      // Loader's row count should include only "visible" rows
      assertEquals(expectedCount, rootWriter.rowCount());

      // Total count should include invisible and look-ahead rows.
      assertEquals(expectedCount + 1, rsLoader.totalRowCount());

      // Result should exclude the overflow row
      VectorContainer container = rsLoader.harvest();
      BatchValidator.validate(container);
      RowSet result = fixture.wrap(container);
      assertEquals(expectedCount, result.rowCount());
      result.clear();
    }

    // Next batch should start with the overflow row
    {
      rsLoader.startBatch();
      assertEquals(1, rootWriter.rowCount());
      assertEquals(expectedCount + 1, rsLoader.totalRowCount());
      VectorContainer container = rsLoader.harvest();
      BatchValidator.validate(container);
      RowSet result = fixture.wrap(container);
      assertEquals(1, result.rowCount());
      result.clear();
    }

    rsLoader.close();
  }

  @Test
  public void testValueOverflow() {
    TupleMetadata schema = new SchemaBuilder()
        .addDictArray("d", MinorType.INT)
          .value(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    byte[] value = new byte[523];
    Arrays.fill(value, (byte) 'X');

    int arraySize = 2; // number of dicts in each row; array size is the same for every row to find expected row count easier
    int dictSize = 4; // number of entries in each dict

    // Number of rows should be driven by vector size.
    // Our row count should include the overflow row
    ArrayWriter arrayDictWriter = rootWriter.array(0);
    DictWriter dictWriter = arrayDictWriter.dict();
    ScalarWriter keyWriter = dictWriter.keyWriter();
    ScalarWriter valueWriter = dictWriter.valueWriter().scalar();

    int expectedCount = ValueVector.MAX_BUFFER_SIZE / (value.length * dictSize * arraySize);
    {
      int count = 0;
      while (! rootWriter.isFull()) {
        rootWriter.start();
        for (int i = 0; i < arraySize; i++) {
          for (int j = 0; j < dictSize; j++) {
            keyWriter.setInt(0); // acts as a placeholder, the actual value is not important
            valueWriter.setBytes(value, value.length);
            dictWriter.save(); // not necessary for scalars, just for completeness
          }
          arrayDictWriter.save();
        }
        rootWriter.save();
        count++;
      }

      assertEquals(expectedCount + 1, count);

      // Loader's row count should include only "visible" rows
      assertEquals(expectedCount, rootWriter.rowCount());

      // Total count should include invisible and look-ahead rows.
      assertEquals(expectedCount + 1, rsLoader.totalRowCount());

      // Result should exclude the overflow row
      VectorContainer container = rsLoader.harvest();
      BatchValidator.validate(container);
      RowSet result = fixture.wrap(container);
      assertEquals(expectedCount, result.rowCount());
      result.clear();
    }

    // Next batch should start with the overflow row
    {
      rsLoader.startBatch();
      assertEquals(1, rootWriter.rowCount());
      assertEquals(expectedCount + 1, rsLoader.totalRowCount());
      VectorContainer container = rsLoader.harvest();
      BatchValidator.validate(container);
      RowSet result = fixture.wrap(container);
      assertEquals(1, result.rowCount());
      result.clear();
    }

    rsLoader.close();
  }
}
