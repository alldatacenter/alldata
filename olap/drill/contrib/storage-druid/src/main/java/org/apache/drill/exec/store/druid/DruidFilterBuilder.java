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
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.drill.exec.store.druid.common.DruidFilter;
import org.apache.drill.exec.store.druid.common.DruidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DruidFilterBuilder extends
  AbstractExprVisitor<DruidScanSpec, Void, RuntimeException> {

  private static final Logger logger = LoggerFactory.getLogger(DruidFilterBuilder.class);

  private final DruidGroupScan groupScan;
  private final LogicalExpression le;
  private final DruidScanSpecBuilder druidScanSpecBuilder;
  private boolean allExpressionsConverted = true;

  public DruidFilterBuilder(DruidGroupScan groupScan,
                            LogicalExpression conditionExp) {
    this.groupScan = groupScan;
    this.le = conditionExp;
    this.druidScanSpecBuilder = new DruidScanSpecBuilder();
  }

  public DruidScanSpec parseTree() {
    logger.debug("DruidScanSpec parseTree() called.");
    DruidScanSpec parsedSpec = le.accept(this, null);
    if (parsedSpec != null) {
      parsedSpec =
          mergeScanSpecs(
              FunctionNames.AND,
              this.groupScan.getScanSpec(),
              parsedSpec
          );
    }
    return parsedSpec;
  }

  private DruidScanSpec mergeScanSpecs(String functionName,
                                       DruidScanSpec leftScanSpec,
                                       DruidScanSpec rightScanSpec) {
    logger.debug("mergeScanSpecs called for functionName - {}", functionName);

    DruidFilter newFilter = null;

    switch (functionName) {
      case FunctionNames.AND:
        if (leftScanSpec.getFilter() != null
            && rightScanSpec.getFilter() != null) {
          newFilter =
            DruidUtils
              .andFilterAtIndex(
                leftScanSpec.getFilter(),
                rightScanSpec.getFilter()
              );
        } else if (leftScanSpec.getFilter() != null) {
          newFilter = leftScanSpec.getFilter();
        } else {
          newFilter = rightScanSpec.getFilter();
        }
        break;
      case FunctionNames.OR:
        newFilter =
          DruidUtils
            .orFilterAtIndex(
              leftScanSpec.getFilter(),
              rightScanSpec.getFilter()
            );
    }

    return new DruidScanSpec(
      groupScan.getScanSpec().getDataSourceName(),
      newFilter,
      groupScan.getScanSpec().getDataSourceSize(),
      groupScan.getScanSpec().getDataSourceMinTime(),
      groupScan.getScanSpec().getDataSourceMaxTime());
  }

  public boolean isAllExpressionsConverted() {
    return allExpressionsConverted;
  }

  @Override
  public DruidScanSpec visitUnknown(LogicalExpression e, Void value)
    throws RuntimeException {
    allExpressionsConverted = false;
    return null;
  }

  @Override
  public DruidScanSpec visitBooleanOperator(BooleanOperator op, Void value) {
    List<LogicalExpression> args = op.args();
    DruidScanSpec nodeScanSpec = null;
    String functionName = op.getName();

    logger.debug("visitBooleanOperator Called. FunctionName - {}", functionName);

    for (LogicalExpression arg : args) {
      switch (functionName) {
        case FunctionNames.AND:
        case FunctionNames.OR:
          if (nodeScanSpec == null) {
            nodeScanSpec = arg.accept(this, null);
          } else {
            DruidScanSpec scanSpec = arg.accept(this, null);
            if (scanSpec != null) {
              nodeScanSpec = mergeScanSpecs(functionName, nodeScanSpec, scanSpec);
            } else {
              allExpressionsConverted = false;
            }
          }
          break;
      }
    }
    return nodeScanSpec;
  }

  @Override
  public DruidScanSpec visitFunctionCall(FunctionCall call, Void value)
    throws RuntimeException {
    DruidScanSpec nodeScanSpec = null;
    String functionName = call.getName();
    List<LogicalExpression> args = call.args();

    logger.debug("visitFunctionCall Called. FunctionName - {}", functionName);

    if (DruidCompareFunctionProcessor.isCompareFunction(functionName)) {
      DruidCompareFunctionProcessor processor = DruidCompareFunctionProcessor
        .process(call);
      if (processor.isSuccess()) {
        DruidScanSpec scanSpec = groupScan.getScanSpec();
        nodeScanSpec =
          druidScanSpecBuilder
            .build(scanSpec.getDataSourceName(),
              scanSpec.getDataSourceSize(),
              scanSpec.getDataSourceMinTime(),
              scanSpec.getDataSourceMaxTime(),
              processor.getFunctionName(),
              processor.getPath(),
              processor.getValue()
            );
      }
    } else {
      switch (functionName) {
        case FunctionNames.AND:
        case FunctionNames.OR:
          DruidScanSpec leftScanSpec = args.get(0).accept(this, null);
          DruidScanSpec rightScanSpec = args.get(1).accept(this, null);
          if (leftScanSpec != null && rightScanSpec != null) {
            nodeScanSpec =
                mergeScanSpecs(functionName, leftScanSpec, rightScanSpec);
          } else {
            allExpressionsConverted = false;
            if (FunctionNames.AND.equals(functionName)) {
              nodeScanSpec = leftScanSpec == null ? rightScanSpec : leftScanSpec;
            }
          }
          break;
      }
    }

    if (nodeScanSpec == null) {
      allExpressionsConverted = false;
    }

    return nodeScanSpec;
  }
}
