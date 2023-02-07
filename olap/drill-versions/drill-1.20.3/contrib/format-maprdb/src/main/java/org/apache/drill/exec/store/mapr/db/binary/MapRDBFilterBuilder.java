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
package org.apache.drill.exec.store.mapr.db.binary;

import java.util.Arrays;
import java.util.List;

import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.expression.BooleanOperator;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.drill.exec.store.hbase.DrillHBaseConstants;
import org.apache.drill.exec.store.hbase.HBaseRegexParser;
import org.apache.drill.exec.store.hbase.HBaseScanSpec;
import org.apache.drill.exec.store.hbase.HBaseUtils;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.ByteArrayComparable;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.NullComparator;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;

public class MapRDBFilterBuilder extends AbstractExprVisitor<HBaseScanSpec, Void, RuntimeException> implements DrillHBaseConstants {

  final private BinaryTableGroupScan groupScan;

  final private LogicalExpression le;

  private boolean allExpressionsConverted = true;

  private static Boolean nullComparatorSupported;

  public MapRDBFilterBuilder(BinaryTableGroupScan groupScan, LogicalExpression le) {
    this.groupScan = groupScan;
    this.le = le;
  }

  public HBaseScanSpec parseTree() {
    HBaseScanSpec parsedSpec = le.accept(this, null);
    if (parsedSpec != null) {
      parsedSpec = mergeScanSpecs(FunctionNames.AND, this.groupScan.getHBaseScanSpec(), parsedSpec);
      /*
       * If RowFilter is THE filter attached to the scan specification,
       * remove it since its effect is also achieved through startRow and stopRow.
       */
      Filter filter = parsedSpec.getFilter();
      if (filter instanceof RowFilter &&
          ((RowFilter)filter).getOperator() != CompareOp.NOT_EQUAL &&
          ((RowFilter)filter).getComparator() instanceof BinaryComparator) {
        parsedSpec = new HBaseScanSpec(parsedSpec.getTableName(), parsedSpec.getStartRow(), parsedSpec.getStopRow(), null);
      }
    }
    return parsedSpec;
  }

  public boolean isAllExpressionsConverted() {
    return allExpressionsConverted;
  }

  @Override
  public HBaseScanSpec visitUnknown(LogicalExpression e, Void value) throws RuntimeException {
    allExpressionsConverted = false;
    return null;
  }

  @Override
  public HBaseScanSpec visitBooleanOperator(BooleanOperator op, Void value) throws RuntimeException {
    return visitFunctionCall(op, value);
  }

  @Override
  public HBaseScanSpec visitFunctionCall(FunctionCall call, Void value) throws RuntimeException {
    HBaseScanSpec nodeScanSpec = null;
    String functionName = call.getName();
    List<LogicalExpression> args = call.args();

    if (MaprDBCompareFunctionsProcessor.isCompareFunction(functionName)) {
      /*
       * HBASE-10848: Bug in HBase versions (0.94.[0-18], 0.96.[0-2], 0.98.[0-1])
       * causes a filter with NullComparator to fail. Enable only if specified in
       * the configuration (after ensuring that the HBase cluster has the fix).
       */
      if (nullComparatorSupported == null) {
        nullComparatorSupported = groupScan.getHBaseConf().getBoolean("drill.hbase.supports.null.comparator", false);
      }

      MaprDBCompareFunctionsProcessor processor = MaprDBCompareFunctionsProcessor.createFunctionsProcessorInstance(call, nullComparatorSupported);
      if (processor.isSuccess()) {
        nodeScanSpec = createHBaseScanSpec(call, processor);
      }
    } else {
      switch (functionName) {
      case FunctionNames.AND:
      case FunctionNames.OR:
        HBaseScanSpec firstScanSpec = args.get(0).accept(this, null);
        for (int i = 1; i < args.size(); ++i) {
          HBaseScanSpec nextScanSpec = args.get(i).accept(this, null);
          if (firstScanSpec != null && nextScanSpec != null) {
            nodeScanSpec = mergeScanSpecs(functionName, firstScanSpec, nextScanSpec);
          } else {
            allExpressionsConverted = false;
            if (FunctionNames.AND.equals(functionName)) {
              nodeScanSpec = firstScanSpec == null ? nextScanSpec : firstScanSpec;
            }
          }
          firstScanSpec = nodeScanSpec;
        }
        break;
      }
    }

    if (nodeScanSpec == null) {
      allExpressionsConverted = false;
    }

    return nodeScanSpec;
  }

  private HBaseScanSpec mergeScanSpecs(String functionName, HBaseScanSpec leftScanSpec, HBaseScanSpec rightScanSpec) {
    Filter newFilter = null;
    byte[] startRow = HConstants.EMPTY_START_ROW;
    byte[] stopRow = HConstants.EMPTY_END_ROW;

    switch (functionName) {
    case FunctionNames.AND:
      newFilter = HBaseUtils.andFilterAtIndex(leftScanSpec.getFilter(), -1, rightScanSpec.getFilter()); //HBaseUtils.LAST_FILTER
      startRow = HBaseUtils.maxOfStartRows(leftScanSpec.getStartRow(), rightScanSpec.getStartRow());
      stopRow = HBaseUtils.minOfStopRows(leftScanSpec.getStopRow(), rightScanSpec.getStopRow());
      break;
    case FunctionNames.OR:
      newFilter = HBaseUtils.orFilterAtIndex(leftScanSpec.getFilter(), -1, rightScanSpec.getFilter()); //HBaseUtils.LAST_FILTER
      startRow = HBaseUtils.minOfStartRows(leftScanSpec.getStartRow(), rightScanSpec.getStartRow());
      stopRow = HBaseUtils.maxOfStopRows(leftScanSpec.getStopRow(), rightScanSpec.getStopRow());
    }
    return new HBaseScanSpec(groupScan.getTableName(), startRow, stopRow, newFilter);
  }

  private HBaseScanSpec createHBaseScanSpec(FunctionCall call, MaprDBCompareFunctionsProcessor processor) {
    String functionName = processor.getFunctionName();
    SchemaPath field = processor.getPath();
    byte[] fieldValue = processor.getValue();
    boolean sortOrderAscending = processor.isSortOrderAscending();
    boolean isRowKey = field.getRootSegmentPath().equals(ROW_KEY);
    if (!(isRowKey
        || (!field.getRootSegment().isLastPath()
            && field.getRootSegment().getChild().isLastPath()
            && field.getRootSegment().getChild().isNamed())
           )
        ) {
      /*
       * if the field in this function is neither the row_key nor a qualified HBase column, return.
       */
      return null;
    }

    if (processor.isRowKeyPrefixComparison()) {
      return createRowKeyPrefixScanSpec(call, processor);
    }

    CompareOp compareOp = null;
    boolean isNullTest = false;
    ByteArrayComparable comparator = new BinaryComparator(fieldValue);
    byte[] startRow = HConstants.EMPTY_START_ROW;
    byte[] stopRow = HConstants.EMPTY_END_ROW;
    switch (functionName) {
    case FunctionNames.EQ:
      compareOp = CompareOp.EQUAL;
      if (isRowKey) {
        startRow = fieldValue;
        /* stopRow should be just greater than 'value'*/
        stopRow = Arrays.copyOf(fieldValue, fieldValue.length+1);
        compareOp = CompareOp.EQUAL;
      }
      break;
    case FunctionNames.NE:
      compareOp = CompareOp.NOT_EQUAL;
      break;
    case FunctionNames.GE:
      if (sortOrderAscending) {
        compareOp = CompareOp.GREATER_OR_EQUAL;
        if (isRowKey) {
          startRow = fieldValue;
        }
      } else {
        compareOp = CompareOp.LESS_OR_EQUAL;
        if (isRowKey) {
          // stopRow should be just greater than 'value'
          stopRow = Arrays.copyOf(fieldValue, fieldValue.length+1);
        }
      }
      break;
    case FunctionNames.GT:
      if (sortOrderAscending) {
        compareOp = CompareOp.GREATER;
        if (isRowKey) {
          // startRow should be just greater than 'value'
          startRow = Arrays.copyOf(fieldValue, fieldValue.length+1);
        }
      } else {
        compareOp = CompareOp.LESS;
        if (isRowKey) {
          stopRow = fieldValue;
        }
      }
      break;
    case FunctionNames.LE:
      if (sortOrderAscending) {
        compareOp = CompareOp.LESS_OR_EQUAL;
        if (isRowKey) {
          // stopRow should be just greater than 'value'
          stopRow = Arrays.copyOf(fieldValue, fieldValue.length+1);
        }
      } else {
        compareOp = CompareOp.GREATER_OR_EQUAL;
        if (isRowKey) {
          startRow = fieldValue;
        }
      }
      break;
    case FunctionNames.LT:
      if (sortOrderAscending) {
        compareOp = CompareOp.LESS;
        if (isRowKey) {
          stopRow = fieldValue;
        }
      } else {
        compareOp = CompareOp.GREATER;
        if (isRowKey) {
          // startRow should be just greater than 'value'
          startRow = Arrays.copyOf(fieldValue, fieldValue.length+1);
        }
      }
      break;
    case FunctionNames.IS_NULL:
    case "isNull":
    case "is null":
      if (isRowKey) {
        return null;
      }
      isNullTest = true;
      compareOp = CompareOp.EQUAL;
      comparator = new NullComparator();
      break;
    case FunctionNames.IS_NOT_NULL:
    case "isNotNull":
    case "is not null":
      if (isRowKey) {
        return null;
      }
      compareOp = CompareOp.NOT_EQUAL;
      comparator = new NullComparator();
      break;
    case "like":
      /*
       * Convert the LIKE operand to Regular Expression pattern so that we can
       * apply RegexStringComparator()
       */
      HBaseRegexParser parser = new HBaseRegexParser(call).parse();
      compareOp = CompareOp.EQUAL;
      comparator = new RegexStringComparator(parser.getRegexString());

      /*
       * We can possibly do better if the LIKE operator is on the row_key
       */
      if (isRowKey) {
        String prefix = parser.getPrefixString();
        if (prefix != null) { // group 3 is literal
          /*
           * If there is a literal prefix, it can help us prune the scan to a sub range
           */
          if (prefix.equals(parser.getLikeString())) {
            /* The operand value is literal. This turns the LIKE operator to EQUAL operator */
            startRow = stopRow = fieldValue;
            compareOp = null;
          } else {
            startRow = prefix.getBytes(Charsets.UTF_8);
            stopRow = startRow.clone();
            boolean isMaxVal = true;
            for (int i = stopRow.length - 1; i >= 0; --i) {
              int nextByteValue = (0xff & stopRow[i]) + 1;
              if (nextByteValue < 0xff) {
                stopRow[i] = (byte) nextByteValue;
                isMaxVal = false;
                break;
              } else {
                stopRow[i] = 0;
              }
            }
            if (isMaxVal) {
              stopRow = HConstants.EMPTY_END_ROW;
            }
          }
        }
      }
      break;
    }

    if (compareOp != null || startRow != HConstants.EMPTY_START_ROW || stopRow != HConstants.EMPTY_END_ROW) {
      Filter filter = null;
      if (isRowKey) {
        if (compareOp != null) {
          filter = new RowFilter(compareOp, comparator);
        }
      } else {
        byte[] family = HBaseUtils.getBytes(field.getRootSegment().getPath());
        byte[] qualifier = HBaseUtils.getBytes(field.getRootSegment().getChild().getNameSegment().getPath());
        filter = new SingleColumnValueFilter(family, qualifier, compareOp, comparator);
        ((SingleColumnValueFilter)filter).setLatestVersionOnly(true);
        if (!isNullTest) {
          ((SingleColumnValueFilter)filter).setFilterIfMissing(true);
        }
      }
      return new HBaseScanSpec(groupScan.getTableName(), startRow, stopRow, filter);
    }
    // else
    return null;
  }

  private HBaseScanSpec createRowKeyPrefixScanSpec(FunctionCall call,
      MaprDBCompareFunctionsProcessor processor) {
    byte[] startRow = processor.getRowKeyPrefixStartRow();
    byte[] stopRow  = processor.getRowKeyPrefixStopRow();
    Filter filter   = processor.getRowKeyPrefixFilter();

    if (startRow != HConstants.EMPTY_START_ROW ||
      stopRow != HConstants.EMPTY_END_ROW ||
      filter != null) {
      return new HBaseScanSpec(groupScan.getTableName(), startRow, stopRow, filter);
    }

    // else
    return null;
  }
}
