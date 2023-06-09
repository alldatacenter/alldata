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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.impl.scan.RowBatchReader;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.v3.ScanLifecycleBuilder;
import org.apache.drill.exec.physical.impl.scan.v3.SchemaNegotiator;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Verifies proper handling of errors from a reader, including use of the
 * scan and reader error contexts.
 */
@Category(EvfTest.class)
public class TestReaderErrors extends BaseTestScanLifecycle {

  @Test
  public void testCtorError() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.errorContext(b -> b.addContext("Scan context"));
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new FailingReader(negotiator, "ctor");
      }
    });
    ScanLifecycle scan = buildScan(builder);
    RowBatchReader reader = scan.nextReader();
    try {
      reader.open();
      fail();
    } catch (UserException e) {
      String msg = e.getMessage();
      assertTrue(msg.contains("Oops ctor"));
      assertTrue(msg.contains("My custom context"));
      assertTrue(msg.contains("Scan context"));
      assertTrue(e.getCause() instanceof IllegalStateException);
    }
    scan.close();
  }

  @Test
  public void testCtorUserError() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.errorContext(b -> b.addContext("Scan context"));
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new FailingReader(negotiator, "ctor-u");
      }
    });
    ScanLifecycle scan = buildScan(builder);
    RowBatchReader reader = scan.nextReader();
    try {
      reader.open();
      fail();
    } catch (UserException e) {
      String msg = e.getMessage();
      assertTrue(msg.contains("Oops ctor"));
      assertTrue(msg.contains("My custom context"));
      assertTrue(msg.contains("Scan context"));
      assertNull(e.getCause());
    }
    scan.close();
  }

  @Test
  public void testNextError() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.errorContext(b -> b.addContext("Scan context"));
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new FailingReader(negotiator, "next");
      }
    });
    ScanLifecycle scan = buildScan(builder);
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    try {
      reader.next();
      fail();
    } catch (UserException e) {
      String msg = e.getMessage();
      assertTrue(msg.contains("Oops next"));
      assertTrue(msg.contains("My custom context"));
      assertTrue(msg.contains("Scan context"));
      assertTrue(e.getCause() instanceof IllegalStateException);
    }
    scan.close();
  }

  @Test
  public void testNextUserError() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.errorContext(b -> b.addContext("Scan context"));
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new FailingReader(negotiator, "next-u");
      }
    });
    ScanLifecycle scan = buildScan(builder);
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    try {
      reader.next();
      fail();
    } catch (UserException e) {
      String msg = e.getMessage();
      assertTrue(msg.contains("Oops next"));
      assertTrue(msg.contains("My custom context"));
      assertTrue(msg.contains("Scan context"));
      assertNull(e.getCause());
    }
    scan.close();
  }

  @Test
  public void testCloseError() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.errorContext(b -> b.addContext("Scan context"));
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new FailingReader(negotiator, "close");
      }
    });
    ScanLifecycle scan = buildScan(builder);
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertFalse(reader.next());

    try {
      reader.close();
      fail();
    } catch (UserException e) {
      // Expected
    }
    scan.close();
  }

  @Test
  public void testCloseUserError() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.errorContext(b -> b.addContext("Scan context"));
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new FailingReader(negotiator, "close-u");
      }
    });
    ScanLifecycle scan = buildScan(builder);
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertFalse(reader.next());

    // User exceptions fail the query. Handles case of, say, a
    // failed transaction, or something that tried, and failed,
    // to update an external system.
    try {
      reader.close();
      fail();
    } catch (UserException e) {
      String msg = e.getMessage();
      assertTrue(msg.contains("Oops close"));
      assertTrue(msg.contains("My custom context"));
      assertTrue(msg.contains("Scan context"));
      assertNull(e.getCause());
    }
    scan.close();
  }
}
