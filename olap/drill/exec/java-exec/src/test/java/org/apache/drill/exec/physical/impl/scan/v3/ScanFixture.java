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

import java.util.List;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.base.AbstractSubScan;
import org.apache.drill.exec.physical.base.Scan;
import org.apache.drill.exec.physical.impl.scan.ScanOperatorExec;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.test.OperatorFixture;

/**
 * Holds the scan operator and its context to simplify tests.
 */
public class ScanFixture {

  private final OperatorContext opContext;
  public ScanOperatorExec scanOp;

  public ScanFixture(OperatorContext opContext, ScanOperatorExec scanOp) {
    this.opContext = opContext;
    this.scanOp = scanOp;
  }

  public void close() {
    try {
      scanOp.close();
    } finally {
      opContext.close();
    }
  }

  /**
   * Wraps a scan lifecycle builder to build the scan fixture.
   */
  public static abstract class ScanFixtureBuilder {

    public final OperatorFixture opFixture;
    // All tests are designed to use the schema batch
    public boolean enableSchemaBatch = true;

    public ScanFixtureBuilder(OperatorFixture opFixture) {
      this.opFixture = opFixture;
    }

    public abstract ScanLifecycleBuilder builder();

    public void projectAll() {
      builder().projection(RowSetTestUtils.projectAll());
    }

    public void setProjection(String... projCols) {
      builder().projection(RowSetTestUtils.projectList(projCols));
    }

    public void setProjection(List<SchemaPath> projection) {
      builder().projection(projection);
    }

    public ScanFixture build() {
      builder().enableSchemaBatch(enableSchemaBatch);
      ScanOperatorExec scanOp = builder().buildScan();
      Scan scanConfig = new AbstractSubScan("bob") {

        @Override
        public String getOperatorType() {
          return "";
        }
      };
      OperatorContext opContext = opFixture.newOperatorContext(scanConfig);
      scanOp.bind(opContext);
      return new ScanFixture(opContext, scanOp);
    }
  }
}
