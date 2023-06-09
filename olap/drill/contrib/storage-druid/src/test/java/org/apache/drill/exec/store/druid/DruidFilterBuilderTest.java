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
package org.apache.drill.exec.store.druid;

import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.expression.BooleanOperator;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.store.druid.common.DruidSelectorFilter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DruidFilterBuilderTest {

  private static final String SOME_DATASOURCE_NAME = "some data source";
  private static final long SOME_DATASOURCE_SIZE = 500;
  private static final String SOME_DATASOURCE_MIN_TIME = "some min time";
  private static final String SOME_DATASOURCE_MAX_TIME = "some max time";

  @Mock
  private LogicalExpression logicalExpression;
  private DruidFilterBuilder druidFilterBuilder;
  private DruidScanSpec druidScanSpecLeft;
  private DruidScanSpec druidScanSpecRight;

  @Before
  public void setup() {
    logicalExpression = mock(LogicalExpression.class);
    DruidSelectorFilter someDruidSelectorFilter = new DruidSelectorFilter("some dimension", "some value");
    DruidSelectorFilter someOtherDruidSelectorFilter = new DruidSelectorFilter("some other dimension", "some other value");
    druidScanSpecLeft =
      new DruidScanSpec(
        SOME_DATASOURCE_NAME,
        someDruidSelectorFilter,
        SOME_DATASOURCE_SIZE,
        SOME_DATASOURCE_MIN_TIME,
        SOME_DATASOURCE_MAX_TIME
      );
    druidScanSpecRight =
      new DruidScanSpec(
        SOME_DATASOURCE_NAME,
        someOtherDruidSelectorFilter,
        SOME_DATASOURCE_SIZE,
        SOME_DATASOURCE_MIN_TIME,
        SOME_DATASOURCE_MAX_TIME
        );
    try {
      when(logicalExpression.accept(any(), any())).thenReturn(druidScanSpecRight);
    } catch (Exception ignored) { }

    DruidGroupScan druidGroupScan = new DruidGroupScan("some username", null, druidScanSpecLeft, null, 5);
    druidFilterBuilder = new DruidFilterBuilder(druidGroupScan, logicalExpression);
  }

  @Test
  public void parseTreeWithAndOfTwoSelectorFilters() {
    DruidScanSpec parsedSpec = druidFilterBuilder.parseTree();
    String expectedFilterJson = "{\"type\":\"and\",\"fields\":[{\"type\":\"selector\",\"dimension\":\"some dimension\",\"value\":\"some value\"},{\"type\":\"selector\",\"dimension\":\"some other dimension\",\"value\":\"some other value\"}]}";
    String actual = parsedSpec.getFilter().toJson();
    assertThat(actual).isEqualTo(expectedFilterJson);
  }

  @Test
  public void visitBooleanOperatorWithAndOperator() {
    LogicalExpression logicalExpression2 = mock(LogicalExpression.class);
    try {
      when(logicalExpression.accept(any(), any())).thenReturn(druidScanSpecLeft);
      when(logicalExpression2.accept(any(), any())).thenReturn(druidScanSpecRight);
    } catch (Exception ignored) {}
    BooleanOperator booleanOperator =
        new BooleanOperator(
            FunctionNames.AND,
            Stream.of(logicalExpression, logicalExpression2).collect(Collectors.toList()), null
        );
    DruidScanSpec druidScanSpec =
        druidFilterBuilder.visitBooleanOperator(booleanOperator, null);
    String expectedFilterJson = "{\"type\":\"and\",\"fields\":[{\"type\":\"selector\",\"dimension\":\"some dimension\",\"value\":\"some value\"},{\"type\":\"selector\",\"dimension\":\"some other dimension\",\"value\":\"some other value\"}]}";
    String actual = druidScanSpec.getFilter().toJson();
    assertThat(actual).isEqualTo(expectedFilterJson);
  }

  @Test
  public void visitBooleanOperatorWithOrOperator() {
    LogicalExpression logicalExpression2 = mock(LogicalExpression.class);
    try {
      when(logicalExpression.accept(any(), any())).thenReturn(druidScanSpecLeft);
      when(logicalExpression2.accept(any(), any())).thenReturn(druidScanSpecRight);
    } catch (Exception ignored) {}
    BooleanOperator booleanOperator =
        new BooleanOperator(
            FunctionNames.OR,
            Stream.of(logicalExpression, logicalExpression2).collect(Collectors.toList()), null
        );
    DruidScanSpec druidScanSpec =
        druidFilterBuilder.visitBooleanOperator(booleanOperator, null);
    String expectedFilterJson = "{\"type\":\"or\",\"fields\":[{\"type\":\"selector\",\"dimension\":\"some dimension\",\"value\":\"some value\"},{\"type\":\"selector\",\"dimension\":\"some other dimension\",\"value\":\"some other value\"}]}";
    String actual = druidScanSpec.getFilter().toJson();
    assertThat(actual).isEqualTo(expectedFilterJson);
  }
}
