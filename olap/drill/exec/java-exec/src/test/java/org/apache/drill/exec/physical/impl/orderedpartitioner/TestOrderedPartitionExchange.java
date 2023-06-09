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
package org.apache.drill.exec.physical.impl.orderedpartitioner;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.server.BootStrapContext;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.vector.BigIntVector;
import org.apache.drill.exec.vector.Float8Vector;
import org.apache.drill.exec.vector.IntVector;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.junit.experimental.categories.Category;

/**
 * Tests the OrderedPartitionExchange Operator
 */
@Ignore("Disabled until alternative to distributed cache provided.")
@Category(OperatorTest.class)
public class TestOrderedPartitionExchange extends PopUnitTestBase {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestOrderedPartitionExchange.class);

  /**
   * Starts two drillbits and runs a physical plan with a Mock scan, project, OrderedParititionExchange, Union Exchange,
   * and sort. The final sort is done first on the partition column, and verifies that the partitions are correct, in that
   * all rows in partition 0 should come in the sort order before any row in partition 1, etc. Also verifies that the standard
   * deviation of the size of the partitions is less than one tenth the mean size of the partitions, because we expect all
   * the partitions to be roughly equal in size.
   * @throws Exception
   */
  @Test
  public void twoBitTwoExchangeRun() throws Exception {
    RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try(Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        Drillbit bit2 = new Drillbit(CONFIG, serviceSet);
        DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator());) {

      bit1.run();
      bit2.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
          Files.asCharSource(DrillFileUtils.getResourceAsFile("/sender/ordered_exchange.json"),
              Charsets.UTF_8).read());
      int count = 0;
      List<Integer> partitionRecordCounts = Lists.newArrayList();
      for(QueryDataBatch b : results) {
        if (b.getData() != null) {
          int rows = b.getHeader().getRowCount();
          count += rows;
          DrillConfig config = DrillConfig.create();
          RecordBatchLoader loader = new RecordBatchLoader(new BootStrapContext(config, SystemOptionManager.createDefaultOptionDefinitions(), ClassPathScanner.fromPrescan(config)).getAllocator());
          loader.load(b.getHeader().getDef(), b.getData());
          BigIntVector vv1 = (BigIntVector)loader.getValueAccessorById(BigIntVector.class, loader.getValueVectorId(
                  new SchemaPath("col1", ExpressionPosition.UNKNOWN)).getFieldIds()).getValueVector();
          Float8Vector vv2 = (Float8Vector)loader.getValueAccessorById(Float8Vector.class, loader.getValueVectorId(
                  new SchemaPath("col2", ExpressionPosition.UNKNOWN)).getFieldIds()).getValueVector();
          IntVector pVector = (IntVector)loader.getValueAccessorById(IntVector.class, loader.getValueVectorId(
                  new SchemaPath("partition", ExpressionPosition.UNKNOWN)).getFieldIds()).getValueVector();
          long previous1 = Long.MIN_VALUE;
          double previous2 = Double.MIN_VALUE;
          int partPrevious = -1;
          long current1 = Long.MIN_VALUE;
          double current2 = Double.MIN_VALUE;
          int partCurrent = -1;
          int partitionRecordCount = 0;
          for (int i = 0; i < rows; i++) {
            previous1 = current1;
            previous2 = current2;
            partPrevious = partCurrent;
            current1 = vv1.getAccessor().get(i);
            current2 = vv2.getAccessor().get(i);
            partCurrent = pVector.getAccessor().get(i);
            Assert.assertTrue(current1 >= previous1);
            if (current1 == previous1) {
              Assert.assertTrue(current2 <= previous2);
            }
            if (partCurrent == partPrevious || partPrevious == -1) {
              partitionRecordCount++;
            } else {
              partitionRecordCounts.add(partitionRecordCount);
              partitionRecordCount = 0;
            }
          }
          partitionRecordCounts.add(partitionRecordCount);
          loader.clear();
        }

        b.release();
      }
      double[] values = new double[partitionRecordCounts.size()];
      int i = 0;
      for (Integer rc : partitionRecordCounts) {
        values[i++] = rc.doubleValue();
      }
      StandardDeviation stdDev = new StandardDeviation();
      Mean mean = new Mean();
      double std = stdDev.evaluate(values);
      double m = mean.evaluate(values);
      assertEquals(31000, count);
    }
  }

}
