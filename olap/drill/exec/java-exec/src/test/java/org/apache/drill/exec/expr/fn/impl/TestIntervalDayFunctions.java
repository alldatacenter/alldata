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
package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.exec.vector.IntervalDayVector;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestIntervalDayFunctions extends ClusterTest {
  @BeforeClass
  public static void setUp() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
  }

  @Test
  public void testIntervalDaySubtractFunction() throws Exception {
    QueryBuilder.VectorQueryReader<String,IntervalDayVector> vectorQueryReader =
        (recordsCount, vector) -> vector.getAccessor().getAsStringBuilder(0).toString();

    String result = queryBuilder()
        .sql("select cast('P6D' as interval day) - cast('P5DT4M20S' as interval day) as i")
        .vectorValue("i", IntervalDayVector.class, vectorQueryReader);
    Assert.assertEquals("0 days 23:55:40", result);

    result = queryBuilder()
        .sql("select cast('P4D' as interval day) - cast('P4DT4M' as interval day) as i")
        .vectorValue("i", IntervalDayVector.class, vectorQueryReader);
    Assert.assertEquals("0 days 0:-4:00", result);

    result = queryBuilder()
        .sql("select cast('P4D' as interval day) - cast('PT4M' as interval day) as i")
        .vectorValue("i", IntervalDayVector.class, vectorQueryReader);
    Assert.assertEquals("3 days 23:56:00", result);

    result = queryBuilder()
        .sql("select cast('P4D' as interval day) - cast('P5D' as interval day) as i")
        .vectorValue("i", IntervalDayVector.class, vectorQueryReader);
    Assert.assertEquals("-1 day 0:00:00", result);

    result = queryBuilder()
        .sql("select cast('P4D' as interval day) - cast('P4DT23H59M59S' as interval day) as i")
        .vectorValue("i", IntervalDayVector.class, vectorQueryReader);
    Assert.assertEquals("0 days -23:-59:-59", result);

    result = queryBuilder()
        .sql("select cast('P4D' as interval day) - cast('P5DT23H59S' as interval day) as i")
        .vectorValue("i", IntervalDayVector.class, vectorQueryReader);
    Assert.assertEquals("-1 day -23:00:-59", result);

    result = queryBuilder()
        .sql("select cast('P2D' as interval day) - cast('P1DT4M' as interval day) as i")
        .vectorValue("i", IntervalDayVector.class, vectorQueryReader);
    Assert.assertEquals("0 days 23:56:00", result);
  }

  @Test
  public void testIntervalDayPlusFunction() throws Exception {
    QueryBuilder.VectorQueryReader<String,IntervalDayVector> vectorQueryReader =
        (recordsCount, vector) -> vector.getAccessor().getAsStringBuilder(0).toString();

    String result = queryBuilder()
        .sql("select cast('P1D' as interval day) + cast('P2DT1H' as interval day) as i")
        .vectorValue("i", IntervalDayVector.class, vectorQueryReader);
    Assert.assertEquals("3 days 1:00:00", result);

    result = queryBuilder()
        .sql("select cast('PT12H' as interval day) + cast('PT12H' as interval day) as i")
        .vectorValue("i", IntervalDayVector.class, vectorQueryReader);
    Assert.assertEquals("1 day 0:00:00", result);

    result = queryBuilder()
        .sql("select cast('PT11H' as interval day) + cast('PT12H59M60S' as interval day) as i")
        .vectorValue("i", IntervalDayVector.class, vectorQueryReader);
    Assert.assertEquals("1 day 0:00:00", result);
  }
}
