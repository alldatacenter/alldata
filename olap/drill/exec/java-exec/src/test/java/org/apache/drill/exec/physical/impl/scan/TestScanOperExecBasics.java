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
package org.apache.drill.exec.physical.impl.scan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils.ScanFixture;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.record.VectorContainer;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests the basics of the scan operator protocol: error conditions,
 * etc.
 */
@Category(RowSetTests.class)
public class TestScanOperExecBasics extends BaseScanOperatorExecTest {

  /**
   * Pathological case that a scan operator is provided no readers.
   * It will throw a user exception because the downstream operators
   * can't handle this case so we choose to stop the show early to
   * avoid getting into a strange state.
   */
  @Test
  public void testNoReader() {

    // Create the scan operator

    ScanFixture scanFixture = simpleFixture();
    ScanOperatorExec scan = scanFixture.scanOp;

    try {
      scan.buildSchema();
    } catch (UserException e) {

      // Expected

      assertTrue(e.getCause() instanceof ExecutionSetupException);
    }

    // Must close the DAG (context and scan operator) even on failures

    scanFixture.close();
  }

  public final String ERROR_MSG = "My Bad!";

  @Test
  public void testExceptionOnOpen() {

    // Reader which fails on open with a known error message
    // using an exception other than UserException.

    MockEarlySchemaReader reader = new MockEarlySchemaReader() {
      @Override
      public boolean open(SchemaNegotiator schemaNegotiator) {
        openCalled = true;
        throw new IllegalStateException(ERROR_MSG);
      }

    };
    reader.batchLimit = 0;

    ScanFixture scanFixture = simpleFixture(reader);
    ScanOperatorExec scan = scanFixture.scanOp;

    try {
      scan.buildSchema();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(ERROR_MSG));
      assertTrue(e.getCause() instanceof IllegalStateException);
    }
    assertTrue(reader.openCalled);

    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
    assertTrue(reader.closeCalled);
  }

  @Test
  public void testUserExceptionOnOpen() {

    // Reader which fails on open with a known error message
    // using a UserException.

    MockEarlySchemaReader reader = new MockEarlySchemaReader() {
      @Override
      public boolean open(SchemaNegotiator schemaNegotiator) {
        openCalled = true;
        throw UserException.dataReadError()
            .addContext(ERROR_MSG)
            .build(logger);
      }

    };
    reader.batchLimit = 2;

    ScanFixture scanFixture = simpleFixture(reader);
    ScanOperatorExec scan = scanFixture.scanOp;

    try {
      scan.buildSchema();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(ERROR_MSG));
      assertNull(e.getCause());
    }
    assertTrue(reader.openCalled);

    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
    assertTrue(reader.closeCalled);
  }

  @Test
  public void testExceptionOnFirstNext() {
    MockEarlySchemaReader reader = new MockEarlySchemaReader() {
      @Override
      public boolean next() {
        super.next(); // Load some data
        throw new IllegalStateException(ERROR_MSG);
      }
    };
    reader.batchLimit = 2;

    ScanFixture scanFixture = simpleFixture(reader);
    ScanOperatorExec scan = scanFixture.scanOp;

    assertTrue(scan.buildSchema());

    try {
      scan.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(ERROR_MSG));
      assertTrue(e.getCause() instanceof IllegalStateException);
    }
    assertTrue(reader.openCalled);

    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
    assertTrue(reader.closeCalled);
  }

  @Test
  public void testUserExceptionOnFirstNext() {
    MockEarlySchemaReader reader = new MockEarlySchemaReader() {
      @Override
      public boolean next() {
        super.next(); // Load some data
        throw UserException.dataReadError()
            .addContext(ERROR_MSG)
            .build(logger);
      }
    };
    reader.batchLimit = 2;

    ScanFixture scanFixture = simpleFixture(reader);
    ScanOperatorExec scan = scanFixture.scanOp;

    assertTrue(scan.buildSchema());

    // EOF

    try {
      scan.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(ERROR_MSG));
      assertNull(e.getCause());
    }
    assertTrue(reader.openCalled);

    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
    assertTrue(reader.closeCalled);
  }

  /**
   * Test throwing an exception after the first batch, but while
   * "reading" the second. Note that the first batch returns data
   * and is spread over two next() calls, so the error is on the
   * third call to the scan operator next().
   */

  @Test
  public void testExceptionOnSecondNext() {
    MockEarlySchemaReader reader = new MockEarlySchemaReader() {
      @Override
      public boolean next() {
        if (batchCount == 1) {
          super.next(); // Load some data
          throw new IllegalStateException(ERROR_MSG);
        }
        return super.next();
      }
    };
    reader.batchLimit = 2;

    ScanFixture scanFixture = simpleFixture(reader);
    ScanOperatorExec scan = scanFixture.scanOp;

    // Schema

    assertTrue(scan.buildSchema());

    // First batch

    assertTrue(scan.next());
    scan.batchAccessor().release();

    // Fail

    try {
      scan.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(ERROR_MSG));
      assertTrue(e.getCause() instanceof IllegalStateException);
    }

    scanFixture.close();
    assertTrue(reader.closeCalled);
  }

  @Test
  public void testUserExceptionOnSecondNext() {
    MockEarlySchemaReader reader = new MockEarlySchemaReader() {
      @Override
      public boolean next() {
        if (batchCount == 1) {
          super.next(); // Load some data
          throw UserException.dataReadError()
              .addContext(ERROR_MSG)
              .build(logger);
        }
        return super.next();
      }
    };
    reader.batchLimit = 2;

    ScanFixture scanFixture = simpleFixture(reader);
    ScanOperatorExec scan = scanFixture.scanOp;

    // Schema

    assertTrue(scan.buildSchema());

    // First batch

    assertTrue(scan.next());
    scan.batchAccessor().release();

    // Fail

    try {
      scan.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(ERROR_MSG));
      assertNull(e.getCause());
    }

    scanFixture.close();
    assertTrue(reader.closeCalled);
  }

  @Test
  public void testExceptionOnClose() {
    MockEarlySchemaReader reader1 = new MockEarlySchemaReader() {
      @Override
      public void close() {
        super.close();
        throw new IllegalStateException(ERROR_MSG);
       }
    };
    reader1.batchLimit = 2;

    MockEarlySchemaReader reader2 = new MockEarlySchemaReader();
    reader2.batchLimit = 2;

    ScanFixture scanFixture = simpleFixture(reader1, reader2);
    ScanOperatorExec scan = scanFixture.scanOp;

    assertTrue(scan.buildSchema());

    assertTrue(scan.next());
    scan.batchAccessor().release();

    assertTrue(scan.next());
    scan.batchAccessor().release();

    // Fail on close of first reader

    try {
      scan.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(ERROR_MSG));
      assertTrue(e.getCause() instanceof IllegalStateException);
    }
    assertTrue(reader1.closeCalled);
    assertFalse(reader2.openCalled);

    scanFixture.close();
  }

  @Test
  public void testUserExceptionOnClose() {
    MockEarlySchemaReader reader1 = new MockEarlySchemaReader() {
      @Override
      public void close() {
        super.close();
        throw UserException.dataReadError()
            .addContext(ERROR_MSG)
            .build(logger);
       }
    };
    reader1.batchLimit = 2;

    MockEarlySchemaReader reader2 = new MockEarlySchemaReader();
    reader2.batchLimit = 2;

    ScanFixture scanFixture = simpleFixture(reader1, reader2);
    ScanOperatorExec scan = scanFixture.scanOp;

    assertTrue(scan.buildSchema());

    assertTrue(scan.next());
    scan.batchAccessor().release();

    assertTrue(scan.next());
    scan.batchAccessor().release();

    // Fail on close of first reader

    try {
      scan.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(ERROR_MSG));
      assertNull(e.getCause());
    }
    assertTrue(reader1.closeCalled);
    assertFalse(reader2.openCalled);

    scanFixture.close();
  }

  /**
   * Test multiple readers, all EOF on first batch.
   * The scan will return one empty batch to declare the
   * early schema. Results in an empty (rather than null)
   * result set.
   */
  @Test
  public void testMultiEOFOnFirstBatch() {
    MockEarlySchemaReader reader1 = new MockEarlySchemaReader();
    reader1.batchLimit = 0;
    MockEarlySchemaReader reader2 = new MockEarlySchemaReader();
    reader2.batchLimit = 0;

    ScanFixture scanFixture = simpleFixture(reader1, reader2);
    ScanOperatorExec scan = scanFixture.scanOp;

    // EOF

    assertTrue(scan.buildSchema());
    assertTrue(scan.next());
    VectorContainer container = scan.batchAccessor().container();
    assertEquals(0, container.getRecordCount());
    assertEquals(2, container.getNumberOfColumns());

    assertTrue(reader1.closeCalled);
    assertTrue(reader2.closeCalled);
    assertEquals(0, scan.batchAccessor().rowCount());
    assertFalse(scan.next());

    scanFixture.close();
  }
}
