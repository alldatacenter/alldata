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
package org.apache.drill.exec.physical.impl.scan.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.impl.scan.ScanOperatorExec;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader.EarlyEofException;
import org.apache.drill.exec.record.VectorContainer;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the basics of the scan operator protocol: error conditions,
 * etc.
 */
@Category(EvfTest.class)
public class TestScanBasics extends BaseScanTest {
  private static final Logger logger = LoggerFactory.getLogger(TestScanBasics.class);

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

  public static final String ERROR_MSG = "My Bad!";

  private static class GenericCrashReader implements ManagedReader {
    public GenericCrashReader(SchemaNegotiator schemaNegotiator) {
      throw new IllegalStateException(ERROR_MSG);
    }

    @Override
    public boolean next() { return false; }

    @Override
    public void close() { }
  }

  @Test
  public void testExceptionOnOpen() {

    // Reader which fails on open with a known error message
    // using an exception other than UserException.
    ObservableCreator creator = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        return new GenericCrashReader(negotiator);
      }
    };

    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;

    try {
      scan.buildSchema();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(ERROR_MSG));
      assertTrue(e.getCause() instanceof IllegalStateException);
    }

    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
  }

  private static class UserExceptionCrashReader implements ManagedReader {
    public UserExceptionCrashReader(SchemaNegotiator schemaNegotiator) {
      throw UserException.dataReadError()
        .message(ERROR_MSG)
        .build(logger);
    }

    @Override
    public boolean next() { return false; }

    @Override
    public void close() { }
  }

  @Test
  public void testUserExceptionOnOpen() {

    // Reader which fails on open with a known error message
    // using a UserException.

    ObservableCreator creator = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        return new UserExceptionCrashReader(negotiator);
      }
    };

    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;

    try {
      scan.buildSchema();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(ERROR_MSG));
      assertNull(e.getCause());
    }

    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
  }

  @Test
  public void testExceptionOnFirstNext() {
    ObservableCreator creator = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator) {
          @Override
          public boolean next() {
            super.next(); // Load some data
            throw new IllegalStateException(ERROR_MSG);
          }
        };
        reader.batchLimit = 2;
        return reader;
      }
    };

    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;

    assertTrue(scan.buildSchema());

    try {
      scan.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(ERROR_MSG));
      assertTrue(e.getCause() instanceof IllegalStateException);
    }

    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
    MockEarlySchemaReader reader = creator.reader();
    assertTrue(reader.closeCalled);
  }

  @Test
  public void testUserExceptionOnFirstNext() {
    ObservableCreator creator = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator) {
          @Override
          public boolean next() {
            super.next(); // Load some data
            throw UserException.dataReadError()
              .message(ERROR_MSG)
              .build(logger);
          }
        };
        reader.batchLimit = 2;
        return reader;
      }
    };

    ScanFixture scanFixture = simpleFixture(creator);
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

    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
    MockEarlySchemaReader reader = creator.reader();
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
    ObservableCreator creator = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator) {
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
        return reader;
      }
    };

    ScanFixture scanFixture = simpleFixture(creator);
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
    MockEarlySchemaReader reader = creator.reader();
    assertTrue(reader.closeCalled);
  }

  @Test
  public void testUserExceptionOnSecondNext() {
    ObservableCreator creator = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator) {
          @Override
          public boolean next() {
            boolean result = super.next(); // Load some data
            if (batchCount == 2) {
              throw UserException.dataReadError()
                  .message(ERROR_MSG)
                  .build(logger);
            }
            return result;
          }
        };
        reader.batchLimit = 2;
        return reader;
      }
    };

    ScanFixture scanFixture = simpleFixture(creator);
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
    MockEarlySchemaReader reader = creator.reader();
    assertTrue(reader.closeCalled);
  }

  @Test
  public void testExceptionOnClose() {
    ObservableCreator creator1 = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator) {
          @Override
          public void close() {
            super.close();
            throw new IllegalStateException(ERROR_MSG);
           }
        };
        reader.batchLimit = 2;
        return reader;
      }
    };

    ObservableCreator creator2 = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
        reader.batchLimit = 2;
        return reader;
      }
    };

    ScanFixture scanFixture = simpleFixture(creator1, creator2);
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
    assertNull(creator2.reader);

    scanFixture.close();
  }

  @Test
  public void testUserExceptionOnClose() {
    ObservableCreator creator1 = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator) {
          @Override
          public void close() {
            super.close();
            throw UserException.dataReadError()
                .message(ERROR_MSG)
                .build(logger);
           }
        };
        reader.batchLimit = 2;
        return reader;
      }
    };

    ObservableCreator creator2 = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
        reader.batchLimit = 2;
        return reader;
      }
    };

    ScanFixture scanFixture = simpleFixture(creator1, creator2);
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
    assertNull(creator2.reader);

    scanFixture.close();
  }

  @Test
  public void testEOFOnFirstOpen() {
    ObservableCreator creator1 = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) throws EarlyEofException {
        return new EofOnOpenReader(negotiator);
      }
    };

    ObservableCreator creator2 = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
        reader.batchLimit = 2;
        return reader;
      }
    };

    ScanFixture scanFixture = simpleFixture(creator1, creator2);
    ScanOperatorExec scan = scanFixture.scanOp;

    assertTrue(scan.buildSchema());

    assertTrue(scan.next());
    scan.batchAccessor().release();

    assertTrue(scan.next());
    scan.batchAccessor().release();

    assertFalse(scan.next());
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
    ObservableCreator creator1 = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
        reader.batchLimit = 0;
        return reader;
      }
    };

    ObservableCreator creator2 = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
        reader.batchLimit = 0;
        return reader;
      }
    };

    ScanFixture scanFixture = simpleFixture(creator1, creator2);
    ScanOperatorExec scan = scanFixture.scanOp;

    // EOF

    assertTrue(scan.buildSchema());
    assertTrue(scan.next());
    VectorContainer container = scan.batchAccessor().container();
    assertEquals(0, container.getRecordCount());
    assertEquals(2, container.getNumberOfColumns());
    assertEquals(0, scan.batchAccessor().rowCount());
    scan.batchAccessor().release();

    MockEarlySchemaReader reader1 = creator1.reader();
    assertTrue(reader1.closeCalled);
    MockEarlySchemaReader reader2 = creator2.reader();
    assertTrue(reader2.closeCalled);

    assertFalse(scan.next());
    scanFixture.close();
  }

  @Test
  public void testEarlyScanClose() {
    ObservableCreator creator = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
        reader.batchLimit = 2;
        return reader;
      }
    };

    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;

    assertTrue(scan.buildSchema());
    assertTrue(scan.next());
    scan.batchAccessor().release();

    scanFixture.close();
    MockEarlySchemaReader reader = creator.reader();
    assertTrue(reader.closeCalled);
  }
}
