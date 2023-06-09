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
package org.apache.drill.exec.physical.impl;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.test.PhysicalOpUnitTestBase;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.store.mock.MockStorePOP;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;

public class BaseTestOpBatchEmitOutcome extends PhysicalOpUnitTestBase {
  //private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BaseTestOpBatchEmitOutcome.class);

  // input batch schema
  protected static TupleMetadata inputSchema;

  // default Empty input RowSet
  protected RowSet.SingleRowSet emptyInputRowSet;

  // default tNon-Empty input RowSet
  protected RowSet.SingleRowSet nonEmptyInputRowSet;

  // List of incoming containers
  protected final List<VectorContainer> inputContainer = new ArrayList<>(5);

  // List of SV2's
  protected final List<SelectionVector2> inputContainerSv2 = new ArrayList<>(5);

  // List of incoming IterOutcomes
  protected final List<RecordBatch.IterOutcome> inputOutcomes = new ArrayList<>(5);

  // output record count
  protected int outputRecordCount;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    inputSchema = new SchemaBuilder()
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.INT)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();
  }

  @Before
  public void beforeTest() throws Exception {
    emptyInputRowSet = operatorFixture.rowSetBuilder(inputSchema).build();
    nonEmptyInputRowSet = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();
    final PhysicalOperator mockPopConfig = new MockStorePOP(null);
    mockOpContext(mockPopConfig, 0, 0);
  }

  @After
  public void afterTest() throws Exception {
    emptyInputRowSet.clear();
    nonEmptyInputRowSet.clear();
    inputContainer.clear();
    inputOutcomes.clear();
    inputContainerSv2.clear();
    outputRecordCount = 0;
  }
}
