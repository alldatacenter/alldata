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
package org.apache.drill.exec.physical.impl.agg;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.OperatorTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(OperatorTest.class)
public class TestHashAggr extends BaseTestQuery{

  @Test
  public void testQ6() throws Exception{
    testPhysicalFromFile("agg/hashagg/q6.json");
  }

  @Test
  public void testQ7_1() throws Exception{
    testPhysicalFromFile("agg/hashagg/q7_1.json");
  }

  @Test
  public void testQ7_2() throws Exception{
    testPhysicalFromFile("agg/hashagg/q7_2.json");
  }

  @Test
  public void testQ7_3() throws Exception{
    testPhysicalFromFile("agg/hashagg/q7_3.json");
  }

  @Ignore // ignore temporarily since this shows memory leak in ParquetRecordReader (DRILL-443)
  @Test
  public void testQ8_1() throws Exception{
    testPhysicalFromFile("agg/hashagg/q8_1.json");
  }

  @Ignore // ignore temporarily since this shows memory leak in ParquetRecordReader (DRILL-443)
  @Test
  public void test8() throws Exception{
    testPhysicalFromFile("agg/hashagg/q8.json");
  }

}
