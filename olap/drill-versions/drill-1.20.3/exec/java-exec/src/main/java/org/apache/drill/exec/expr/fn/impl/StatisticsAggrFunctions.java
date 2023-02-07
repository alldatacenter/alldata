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

/*
 * This class is automatically generated from AggrTypeFunctions2.tdd using FreeMarker.
 */

package org.apache.drill.exec.expr.fn.impl;

import io.netty.buffer.DrillBuf;
import javax.inject.Inject;
import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.DateHolder;
import org.apache.drill.exec.expr.holders.Decimal18Holder;
import org.apache.drill.exec.expr.holders.Decimal28DenseHolder;
import org.apache.drill.exec.expr.holders.Decimal28SparseHolder;
import org.apache.drill.exec.expr.holders.Decimal38DenseHolder;
import org.apache.drill.exec.expr.holders.Decimal38SparseHolder;
import org.apache.drill.exec.expr.holders.Decimal9Holder;
import org.apache.drill.exec.expr.holders.Float4Holder;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.expr.holders.IntervalHolder;
import org.apache.drill.exec.expr.holders.NullableBigIntHolder;
import org.apache.drill.exec.expr.holders.NullableBitHolder;
import org.apache.drill.exec.expr.holders.NullableDateHolder;
import org.apache.drill.exec.expr.holders.NullableDecimal18Holder;
import org.apache.drill.exec.expr.holders.NullableDecimal28DenseHolder;
import org.apache.drill.exec.expr.holders.NullableDecimal28SparseHolder;
import org.apache.drill.exec.expr.holders.NullableDecimal38DenseHolder;
import org.apache.drill.exec.expr.holders.NullableDecimal38SparseHolder;
import org.apache.drill.exec.expr.holders.NullableDecimal9Holder;
import org.apache.drill.exec.expr.holders.NullableFloat4Holder;
import org.apache.drill.exec.expr.holders.NullableFloat8Holder;
import org.apache.drill.exec.expr.holders.NullableIntHolder;
import org.apache.drill.exec.expr.holders.NullableIntervalHolder;
import org.apache.drill.exec.expr.holders.NullableTimeHolder;
import org.apache.drill.exec.expr.holders.NullableTimeStampHolder;
import org.apache.drill.exec.expr.holders.NullableVar16CharHolder;
import org.apache.drill.exec.expr.holders.NullableVarBinaryHolder;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.expr.holders.ObjectHolder;
import org.apache.drill.exec.expr.holders.TimeHolder;
import org.apache.drill.exec.expr.holders.TimeStampHolder;
import org.apache.drill.exec.expr.holders.Var16CharHolder;
import org.apache.drill.exec.expr.holders.VarBinaryHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

@SuppressWarnings("unused")
public class StatisticsAggrFunctions {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StatisticsAggrFunctions.class);

  /* IMPORTANT NOTE: Please make sure to create a new function for each datatype. See the examples below.
  * This will result in more performant generated code. Use switch-case/if-else statements judiciously
  * as it MAY cause the generated code to slow down considerably.
  * */
  @FunctionTemplate(name = "rowcount", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class RowCount implements DrillAggFunc {
    @Param FieldReader in;
    @Workspace BigIntHolder count;
    @Output NullableBigIntHolder out;

    @Override
    public void setup() {
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      count.value++;
    }

    @Override
    public void output() {
      out.isSet = 1;
      out.value = count.value;
    }

    @Override
    public void reset() {
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "nonnullrowcount", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NonNullRowCount implements DrillAggFunc {
    @Param FieldReader in;
    @Workspace BigIntHolder count;
    @Output NullableBigIntHolder out;

    @Override
    public void setup() {
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      if (in.isSet()) {
        count.value++;
      }
    }

    @Override
    public void output() {
      out.isSet = 1;
      out.value = count.value;
    }

    @Override
    public void reset() {
      count.value = 0;
    }
  }

  /**
   * The log2m parameter defines the accuracy of the counter.  The larger the
   * log2m the better the accuracy.
   * accuracy = 1.04/sqrt(2^log2m)
   * where
   * log2m - the number of bits to use as the basis for the HLL instance
   */
  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class HllFieldReader implements DrillAggFunc {
    @Param FieldReader in;
    @Workspace ObjectHolder work;
    @Output NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
            (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        int mode = in.getType().getMode().getNumber();
        int type = in.getType().getMinorType().getNumber();

        switch (mode) {
          case org.apache.drill.common.types.TypeProtos.DataMode.OPTIONAL_VALUE:
            if (!in.isSet()) {
              hll.offer(null);
              break;
            }
            // fall through //
          case org.apache.drill.common.types.TypeProtos.DataMode.REQUIRED_VALUE:
            switch (type) {
              case org.apache.drill.common.types.TypeProtos.MinorType.VARCHAR_VALUE:
                hll.offer(in.readText().toString());
                break;
              case org.apache.drill.common.types.TypeProtos.MinorType.BIGINT_VALUE:
                hll.offer(in.readLong());
                break;
              default:
                work.obj = null;
            }
            break;
          default:
            work.obj = null;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
            (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  /**
   * The log2m parameter defines the accuracy of the counter.  The larger the log2m the better the accuracy where:
   * accuracy = 1.04/sqrt(2^log2m)
   * log2m - the number of bits to use as the basis for the HLL instance
   * The parameter accepts integers in the range [0, 30]
   */
  @FunctionTemplate(name = "hll_decode", scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
  public static class HllDecode implements DrillSimpleFunc {

    @Param
    NullableVarBinaryHolder in;
    @Output
    BigIntHolder out;

    @Override
    public void setup() {
    }

    public void eval() {
      out.value = -1;

      if (in.isSet != 0) {
        byte[] din = new byte[in.end - in.start];
        in.buffer.getBytes(in.start, din);
        try {
          out.value = com.clearspring.analytics.stream.cardinality.HyperLogLog.Builder.build(din).cardinality();
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failure evaluating hll_decode", e);
        }
      }
    }
  }

  /**
   * The log2m parameter defines the accuracy of the counter.  The larger the log2m the better the accuracy where:
   * accuracy = 1.04/sqrt(2^log2m)
   * log2m - the number of bits to use as the basis for the HLL instance
   * The parameter accepts integers in the range [0, 30]
   */
  @FunctionTemplate(name = "hll_merge", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class HllMerge implements DrillAggFunc {
    @Param NullableVarBinaryHolder in;
    @Workspace ObjectHolder work;
    @Output NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        try {
          if (in.isSet != 0) {
            byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
                in.start, in.end, in.buffer).getBytes();
            com.clearspring.analytics.stream.cardinality.HyperLogLog other =
                com.clearspring.analytics.stream.cardinality.HyperLogLog.Builder.build(buf);
            hll.addAll(other);
          }
        } catch (Exception e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to merge HyperLogLog output", e);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  /**
   * The log2m parameter defines the accuracy of the counter.  The larger the log2m the better the accuracy where:
   * accuracy = 1.04/sqrt(2^log2m)
   * log2m - the number of bits to use as the basis for the HLL instance
   * The parameter accepts integers in the range [0, 30]
   */
  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class BitHLLFunction implements DrillAggFunc {
    @Param
    BitHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        hll.offer(in.value);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableBitHLLFunction implements DrillAggFunc {
    @Param
    NullableBitHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        hll.offer(in.value);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class IntHLLFunction implements DrillAggFunc {
    @Param
    IntHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        hll.offer(in.value);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableIntHLLFunction implements DrillAggFunc {
    @Param
    NullableIntHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        if (in.isSet == 1) {
          hll.offer(in.value);
        } else {
          hll.offer(null);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class BigIntHLLFunction implements DrillAggFunc {
    @Param
    BigIntHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        hll.offer(in.value);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableBigIntHLLFunction implements DrillAggFunc {
    @Param
    NullableBigIntHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        if (in.isSet == 1) {
          hll.offer(in.value);
        } else {
          hll.offer(null);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Float4HLLFunction implements DrillAggFunc {
    @Param
    Float4Holder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        hll.offer(in.value);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableFloat4HLLFunction implements DrillAggFunc {
    @Param
    NullableFloat4Holder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        if (in.isSet == 1) {
          hll.offer(in.value);
        } else {
          hll.offer(null);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Float8HLLFunction implements DrillAggFunc {
    @Param
    Float8Holder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        hll.offer(in.value);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableFloat8HLLFunction implements DrillAggFunc {
    @Param
    NullableFloat8Holder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        if (in.isSet == 1) {
          hll.offer(in.value);
        } else {
          hll.offer(null);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal9HLLFunction implements DrillAggFunc {
    @Param
    Decimal9Holder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        hll.offer(in.value);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal9HLLFunction implements DrillAggFunc {
    @Param
    NullableDecimal9Holder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        if (in.isSet == 1) {
          hll.offer(in.value);
        } else {
          hll.offer(null);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal18HLLFunction implements DrillAggFunc {
    @Param
    Decimal18Holder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        hll.offer(in.value);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal18HLLFunction implements DrillAggFunc {
    @Param
    NullableDecimal18Holder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        if (in.isSet == 1) {
          hll.offer(in.value);
        } else {
          hll.offer(null);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class DateHLLFunction implements DrillAggFunc {
    @Param
    DateHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        hll.offer(in.value);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDateHLLFunction implements DrillAggFunc {
    @Param
    NullableDateHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        if (in.isSet == 1) {
          hll.offer(in.value);
        } else {
          hll.offer(null);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class TimeHLLFunction implements DrillAggFunc {
    @Param
    TimeHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        hll.offer(in.value);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableTimeHLLFunction implements DrillAggFunc {
    @Param
    NullableTimeHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        if (in.isSet == 1) {
          hll.offer(in.value);
        } else {
          hll.offer(null);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class TimeStampHLLFunction implements DrillAggFunc {
    @Param
    TimeStampHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        hll.offer(in.value);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableTimeStampHLLFunction implements DrillAggFunc {
    @Param
    NullableTimeStampHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        if (in.isSet == 1) {
          hll.offer(in.value);
        } else {
          hll.offer(null);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class IntervalHLLFunction implements DrillAggFunc {
    @Param
    IntervalHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace
    ObjectHolder interval;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      interval = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
      interval.obj = new java.util.ArrayList<Integer>(3);
    }

    @Override
    public void add() {
      if (work.obj != null
          && interval.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
            (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        java.util.ArrayList<Integer> intervalList = (java.util.ArrayList<Integer>)interval.obj;
        intervalList.clear();
        intervalList.add(in.days);
        intervalList.add(in.months);
        intervalList.add(in.milliseconds);
        hll.offer(interval.obj);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
      interval.obj = new java.util.ArrayList<Integer>(3);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableIntervalHLLFunction implements DrillAggFunc {
    @Param
    NullableIntervalHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace
    ObjectHolder interval;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      interval = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
      interval.obj = new java.util.ArrayList<Integer>(3);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        if (in.isSet == 1) {
          if (interval.obj != null) {
            java.util.ArrayList<Integer> intervalList = (java.util.ArrayList<Integer>)interval.obj;
            intervalList.clear();
            intervalList.add(in.days);
            intervalList.add(in.months);
            intervalList.add(in.milliseconds);
            hll.offer(interval.obj);
          }
        } else {
          hll.offer(null);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
      interval.obj = new java.util.ArrayList<Integer>(3);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class VarCharHLLFunction implements DrillAggFunc {
    @Param
    VarCharHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
                in.start, in.end, in.buffer).getBytes();
        hll.offer(buf);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableVarCharHLLFunction implements DrillAggFunc {
    @Param
    NullableVarCharHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        if (in.isSet == 1) {
          byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
                  in.start, in.end, in.buffer).getBytes();
          hll.offer(buf);
        } else {
          hll.offer(null);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Var16CharHLLFunction implements DrillAggFunc {
    @Param
    Var16CharHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF16
                (in.start, in.end, in.buffer).getBytes();
        hll.offer(buf);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableVar16CharHLLFunction implements DrillAggFunc {
    @Param
    NullableVar16CharHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        if (in.isSet == 1) {
          byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF16
                  (in.start, in.end, in.buffer).getBytes();
          hll.offer(buf);
        } else {
          hll.offer(null);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class VarBinaryHLLFunction implements DrillAggFunc {
    @Param
    VarBinaryHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;

    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8
                (in.start, in.end, in.buffer).getBytes();
        hll.offer(buf);
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "hll", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableVarBinaryHLLFunction implements DrillAggFunc {
    @Param
    NullableVarBinaryHolder in;
    @Workspace
    ObjectHolder work;
    @Output
    NullableVarBinaryHolder out;
    @Inject OptionManager options;
    @Inject DrillBuf buffer;
    @Workspace IntHolder hllAccuracy;
    @Override
    public void setup() {
      work = new ObjectHolder();
      hllAccuracy.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.HLL_ACCURACY);
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;
        if (in.isSet == 1) {
          byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8
                  (in.start, in.end, in.buffer).getBytes();
          hll.offer(buf);
        } else {
          hll.offer(null);
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.cardinality.HyperLogLog hll =
                (com.clearspring.analytics.stream.cardinality.HyperLogLog) work.obj;

        try {
          byte[] ba = hll.getBytes();
          out.buffer = buffer = buffer.reallocIfNeeded(ba.length);
          out.start = 0;
          out.end = ba.length;
          out.buffer.setBytes(0, ba);
          out.isSet = 1;
        } catch (java.io.IOException e) {
          throw new org.apache.drill.common.exceptions.DrillRuntimeException("Failed to get HyperLogLog output", e);
        }
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      work.obj = new com.clearspring.analytics.stream.cardinality.HyperLogLog(hllAccuracy.value);
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class BitAvgWidthFunction implements DrillAggFunc {
    @Param BitHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 1;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableBitAvgWidthFunction implements DrillAggFunc {
    @Param NullableBitHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 8;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }
  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class IntAvgWidthFunction implements DrillAggFunc {
    @Param IntHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableIntAvgWidthFunction implements DrillAggFunc {
    @Param NullableIntHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }
  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class BigIntAvgWidthFunction implements DrillAggFunc {
    @Param BigIntHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Long.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableBigIntAvgWidthFunction implements DrillAggFunc {
    @Param NullableBigIntHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Long.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal9AvgWidthFunction implements DrillAggFunc {
    @Param Decimal9Holder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal9AvgWidthFunction implements DrillAggFunc {
    @Param NullableDecimal9Holder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }
  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal18AvgWidthFunction implements DrillAggFunc {
    @Param Decimal18Holder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Long.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal18AvgWidthFunction implements DrillAggFunc {
    @Param NullableDecimal18Holder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Long.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal28DenseAvgWidthFunction implements DrillAggFunc {
    @Param Decimal28DenseHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 12;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal28DenseAvgWidthFunction implements DrillAggFunc {
    @Param NullableDecimal28DenseHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 12;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal28SparseAvgWidthFunction implements DrillAggFunc {
    @Param Decimal28SparseHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 12;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal28SparseAvgWidthFunction implements DrillAggFunc {
    @Param NullableDecimal28SparseHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 12;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal38DenseAvgWidthFunction implements DrillAggFunc {
    @Param Decimal38DenseHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 16;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal38DenseAvgWidthFunction implements DrillAggFunc {
    @Param NullableDecimal38DenseHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 16;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal38SparseAvgWidthFunction implements DrillAggFunc {
    @Param Decimal38SparseHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 16;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal38SparseAvgWidthFunction implements DrillAggFunc {
    @Param NullableDecimal38SparseHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 16;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Float4AvgWidthFunction implements DrillAggFunc {
    @Param Float4Holder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Float.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableFloat4AvgWidthFunction implements DrillAggFunc {
    @Param NullableFloat4Holder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Float.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Float8AvgWidthFunction implements DrillAggFunc {
    @Param Float8Holder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Double.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableFloat8AvgWidthFunction implements DrillAggFunc {
    @Param NullableFloat8Holder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Double.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class DateAvgWidthFunction implements DrillAggFunc {
    @Param DateHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDateAvgWidthFunction implements DrillAggFunc {
    @Param NullableDateHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }
  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class TimeAvgWidthFunction implements DrillAggFunc {
    @Param TimeHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableTimeAvgWidthFunction implements DrillAggFunc {
    @Param NullableTimeHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }
  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class TimeStampAvgWidthFunction implements DrillAggFunc {
    @Param TimeStampHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Long.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableTimeStampAvgWidthFunction implements DrillAggFunc {
    @Param NullableTimeStampHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Long.SIZE;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value * 8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }
  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class IntervalAvgWidthFunction implements DrillAggFunc {
    @Param IntervalHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 12;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableIntervalAvgWidthFunction implements DrillAggFunc {
    @Param NullableIntervalHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 12;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class VarCharAvgWidthFunction implements DrillAggFunc {
    @Param VarCharHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
              in.start, in.end, in.buffer).getBytes().length;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableVarCharAvgWidthFunction implements DrillAggFunc {
    @Param NullableVarCharHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Workspace BigIntHolder nonNullCount;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
      nonNullCount = new BigIntHolder();
    }

    @Override
    public void add() {
      if (in.isSet == 1) {
        totWidth.value += org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
                in.start, in.end, in.buffer).getBytes().length;
        nonNullCount.value += 1;
      }
      count.value++;
    }

    @Override
    public void output() {
      if (nonNullCount.value > 0) {
        out.value = totWidth.value/((double)count.value);
      } else {
        out.value = 0;
      }
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
      nonNullCount.value = 0;
    }
  }
  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Var16CharAvgWidthFunction implements DrillAggFunc {
    @Param Var16CharHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF16(
              in.start, in.end, in.buffer).getBytes().length;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableVar16CharAvgWidthFunction implements DrillAggFunc {
    @Param NullableVar16CharHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Workspace BigIntHolder nonNullCount;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
      nonNullCount = new BigIntHolder();
    }

    @Override
    public void add() {
      if (in.isSet == 1) {
        totWidth.value += org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF16(
                in.start, in.end, in.buffer).getBytes().length;
        nonNullCount.value += 1;
      }
      count.value++;
    }

    @Override
    public void output() {
      if (nonNullCount.value > 0) {
        out.value = totWidth.value/((double)count.value);
      } else {
        out.value = 0;
      }
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
      nonNullCount.value = 0;
    }
  }
  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class VarBinaryAvgWidthFunction implements DrillAggFunc {
    @Param VarBinaryHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
              in.start, in.end, in.buffer).getBytes().length;
      count.value++;
    }

    @Override
    public void output() {
      out.value = totWidth.value/((double)count.value);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
    }
  }

  @FunctionTemplate(name = "avg_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableVarBinaryAvgWidthFunction implements DrillAggFunc {
    @Param NullableVarBinaryHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder count;
    @Workspace BigIntHolder nonNullCount;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      count = new BigIntHolder();
      nonNullCount = new BigIntHolder();
    }

    @Override
    public void add() {
      if (in.isSet == 1) {
        totWidth.value += org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
                in.start, in.end, in.buffer).getBytes().length;
        nonNullCount.value += 1;
      }
      count.value++;
    }

    @Override
    public void output() {
      if (nonNullCount.value > 0) {
        out.value = totWidth.value/((double)count.value);
      } else {
        out.value = 0;
      }
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      count.value = 0;
      nonNullCount.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class BitSumWidthFunction implements DrillAggFunc {
    @Param BitHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 8;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableBitSumWidthFunction implements DrillAggFunc {
    @Param NullableBitHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 8;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }
  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class IntSumWidthFunction implements DrillAggFunc {
    @Param IntHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableIntSumWidthFunction implements DrillAggFunc {
    @Param NullableIntHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }
  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class BigIntSumWidthFunction implements DrillAggFunc {
    @Param BigIntHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Long.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableBigIntSumWidthFunction implements DrillAggFunc {
    @Param NullableBigIntHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Long.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal9SumWidthFunction implements DrillAggFunc {
    @Param Decimal9Holder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal9SumWidthFunction implements DrillAggFunc {
    @Param NullableDecimal9Holder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }
  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal18SumWidthFunction implements DrillAggFunc {
    @Param Decimal18Holder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Long.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal18SumWidthFunction implements DrillAggFunc {
    @Param NullableDecimal18Holder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Long.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal28DenseSumWidthFunction implements DrillAggFunc {
    @Param Decimal28DenseHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 12;
    }

    @Override
    public void output() {
      out.value = totWidth.value;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal28DenseSumWidthFunction implements DrillAggFunc {
    @Param NullableDecimal28DenseHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 12;
    }

    @Override
    public void output() {
      out.value = totWidth.value;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal28SparseSumWidthFunction implements DrillAggFunc {
    @Param Decimal28SparseHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 12;
    }

    @Override
    public void output() {
      out.value = totWidth.value;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal28SparseSumWidthFunction implements DrillAggFunc {
    @Param NullableDecimal28SparseHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 12;
    }

    @Override
    public void output() {
      out.value = totWidth.value;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal38DenseSumWidthFunction implements DrillAggFunc {
    @Param Decimal38DenseHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 16;
    }

    @Override
    public void output() {
      out.value = totWidth.value;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal38DenseSumWidthFunction implements DrillAggFunc {
    @Param NullableDecimal38DenseHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 16;
    }

    @Override
    public void output() {
      out.value = totWidth.value;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal38SparseSumWidthFunction implements DrillAggFunc {
    @Param Decimal38SparseHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 16;
    }

    @Override
    public void output() {
      out.value = totWidth.value;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal38SparseSumWidthFunction implements DrillAggFunc {
    @Param NullableDecimal38SparseHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 16;
    }

    @Override
    public void output() {
      out.value = totWidth.value;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Float4SumWidthFunction implements DrillAggFunc {
    @Param Float4Holder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Float.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableFloat4SumWidthFunction implements DrillAggFunc {
    @Param NullableFloat4Holder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Float.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Float8SumWidthFunction implements DrillAggFunc {
    @Param Float8Holder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Double.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableFloat8SumWidthFunction implements DrillAggFunc {
    @Param NullableFloat8Holder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Double.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class DateSumWidthFunction implements DrillAggFunc {
    @Param DateHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDateSumWidthFunction implements DrillAggFunc {
    @Param NullableDateHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }
  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class TimeSumWidthFunction implements DrillAggFunc {
    @Param TimeHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableTimeSumWidthFunction implements DrillAggFunc {
    @Param NullableTimeHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Integer.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }
  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class TimeStampSumWidthFunction implements DrillAggFunc {
    @Param TimeStampHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Long.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableTimeStampSumWidthFunction implements DrillAggFunc {
    @Param NullableTimeStampHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += Long.SIZE;
    }

    @Override
    public void output() {
      out.value = totWidth.value/(8.0);
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }
  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class IntervalSumWidthFunction implements DrillAggFunc {
    @Param IntervalHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 12;
    }

    @Override
    public void output() {
      out.value = totWidth.value;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableIntervalSumWidthFunction implements DrillAggFunc {
    @Param NullableIntervalHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += 12;
    }

    @Override
    public void output() {
      out.value = totWidth.value;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class VarCharSumWidthFunction implements DrillAggFunc {
    @Param VarCharHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
              in.start, in.end, in.buffer).getBytes().length;
    }

    @Override
    public void output() {
      out.value = totWidth.value;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableVarCharSumWidthFunction implements DrillAggFunc {
    @Param NullableVarCharHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder nonNullCount;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      nonNullCount = new BigIntHolder();
    }

    @Override
    public void add() {
      if (in.isSet == 1) {
        totWidth.value += org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
            in.start, in.end, in.buffer).getBytes().length;
        nonNullCount.value += 1;
      }
    }

    @Override
    public void output() {
      if (nonNullCount.value > 0) {
        out.value = totWidth.value;
      } else {
        out.value = 0;
      }
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      nonNullCount.value = 0;
    }
  }
  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Var16CharSumWidthFunction implements DrillAggFunc {
    @Param Var16CharHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF16(
              in.start, in.end, in.buffer).getBytes().length;
    }

    @Override
    public void output() {
      out.value = totWidth.value;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableVar16CharSumWidthFunction implements DrillAggFunc {
    @Param NullableVar16CharHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder nonNullCount;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      nonNullCount = new BigIntHolder();
    }

    @Override
    public void add() {
      if (in.isSet == 1) {
        totWidth.value += org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF16(
                in.start, in.end, in.buffer).getBytes().length;
        nonNullCount.value += 1;
      }
    }

    @Override
    public void output() {
      if (nonNullCount.value > 0) {
        out.value = totWidth.value;
      } else {
        out.value = 0;
      }
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      nonNullCount.value = 0;
    }
  }
  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class VarBinarySumWidthFunction implements DrillAggFunc {
    @Param VarBinaryHolder in;
    @Workspace BigIntHolder totWidth;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
    }

    @Override
    public void add() {
      totWidth.value += org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
              in.start, in.end, in.buffer).getBytes().length;
    }

    @Override
    public void output() {
      out.value = totWidth.value;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
    }
  }

  @FunctionTemplate(name = "sum_width", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableVarBinarySumWidthFunction implements DrillAggFunc {
    @Param NullableVarBinaryHolder in;
    @Workspace BigIntHolder totWidth;
    @Workspace BigIntHolder nonNullCount;
    @Output NullableFloat8Holder out;

    @Override
    public void setup() {
      totWidth = new BigIntHolder();
      nonNullCount = new BigIntHolder();
    }

    @Override
    public void add() {
      if (in.isSet == 1) {
        totWidth.value += org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
                in.start, in.end, in.buffer).getBytes().length;
        nonNullCount.value += 1;
      }
    }

    @Override
    public void output() {
      if (nonNullCount.value > 0) {
        out.value = totWidth.value;
      } else {
        out.value = 0;
      }
      out.isSet = 1;
    }

    @Override
    public void reset() {
      totWidth.value = 0;
      nonNullCount.value = 0;
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class BitCntDupsFunction implements DrillAggFunc {
    @Param
    BitHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (!filter.isPresent(String.valueOf(in.value))) {
          filter.add(String.valueOf(in.value));
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableBitCntDupsFunction implements DrillAggFunc {
    @Param
    NullableBitHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          if (!filter.isPresent(String.valueOf(in.value))) {
            filter.add(String.valueOf(in.value));
          } else {
            dups.value++;
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class IntCntDupsFunction implements DrillAggFunc {
    @Param
    IntHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (!filter.isPresent(String.valueOf(in.value))) {
          filter.add(String.valueOf(in.value));
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableIntCntDupsFunction implements DrillAggFunc {
    @Param
    NullableIntHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          if (!filter.isPresent(String.valueOf(in.value))) {
            filter.add(String.valueOf(in.value));
          } else {
            dups.value++;
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class BigIntCntDupsFunction implements DrillAggFunc {
    @Param
    BigIntHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (!filter.isPresent(String.valueOf(in.value))) {
          filter.add(String.valueOf(in.value));
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableBigIntCntDupsFunction implements DrillAggFunc {
    @Param
    NullableBigIntHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          if (!filter.isPresent(String.valueOf(in.value))) {
            filter.add(String.valueOf(in.value));
          } else {
            dups.value++;
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Float4CntDupsFunction implements DrillAggFunc {
    @Param
    Float4Holder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (!filter.isPresent(String.valueOf(in.value))) {
          filter.add(String.valueOf(in.value));
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableFloat4CntDupsFunction implements DrillAggFunc {
    @Param
    NullableFloat4Holder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          if (!filter.isPresent(String.valueOf(in.value))) {
            filter.add(String.valueOf(in.value));
          } else {
            dups.value++;
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Float8CntDupsFunction implements DrillAggFunc {
    @Param
    Float8Holder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (!filter.isPresent(String.valueOf(in.value))) {
          filter.add(String.valueOf(in.value));
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableFloat8CntDupsFunction implements DrillAggFunc {
    @Param
    NullableFloat8Holder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          if (!filter.isPresent(String.valueOf(in.value))) {
            filter.add(String.valueOf(in.value));
          } else {
            dups.value++;
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal9CntDupsFunction implements DrillAggFunc {
    @Param
    Decimal9Holder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (!filter.isPresent(String.valueOf(in.value))) {
          filter.add(String.valueOf(in.value));
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal9CntDupsFunction implements DrillAggFunc {
    @Param
    NullableDecimal9Holder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          if (!filter.isPresent(String.valueOf(in.value))) {
            filter.add(String.valueOf(in.value));
          } else {
            dups.value++;
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Decimal18CntDupsFunction implements DrillAggFunc {
    @Param
    Decimal18Holder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (!filter.isPresent(String.valueOf(in.value))) {
          filter.add(String.valueOf(in.value));
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDecimal18CntDupsFunction implements DrillAggFunc {
    @Param
    NullableDecimal18Holder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          if (!filter.isPresent(String.valueOf(in.value))) {
            filter.add(String.valueOf(in.value));
          } else {
            dups.value++;
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class DateCntDupsFunction implements DrillAggFunc {
    @Param
    DateHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (!filter.isPresent(String.valueOf(in.value))) {
          filter.add(String.valueOf(in.value));
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableDateCntDupsFunction implements DrillAggFunc {
    @Param
    NullableDateHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          if (!filter.isPresent(String.valueOf(in.value))) {
            filter.add(String.valueOf(in.value));
          } else {
            dups.value++;
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class TimeCntDupsFunction implements DrillAggFunc {
    @Param
    TimeHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (!filter.isPresent(String.valueOf(in.value))) {
          filter.add(String.valueOf(in.value));
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableTimeCntDupsFunction implements DrillAggFunc {
    @Param
    NullableTimeHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          if (!filter.isPresent(String.valueOf(in.value))) {
            filter.add(String.valueOf(in.value));
          } else {
            dups.value++;
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class TimeStampCntDupsFunction implements DrillAggFunc {
    @Param
    TimeStampHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (!filter.isPresent(String.valueOf(in.value))) {
          filter.add(String.valueOf(in.value));
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableTimeStampCntDupsFunction implements DrillAggFunc {
    @Param
    NullableTimeStampHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          if (!filter.isPresent(String.valueOf(in.value))) {
            filter.add(String.valueOf(in.value));
          } else {
            dups.value++;
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class IntervalCntDupsFunction implements DrillAggFunc {
    @Param
    IntervalHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Workspace
    ObjectHolder interval;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      interval = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
      interval.obj = new int[3];
    }

    @Override
    public void add() {
      if (work.obj != null
              && interval.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        ((int[])interval.obj)[0] = in.days;
        ((int[])interval.obj)[1] = in.months;
        ((int[])interval.obj)[2] = in.milliseconds;
        if (!filter.isPresent(String.valueOf(interval.obj))) {
          filter.add(String.valueOf(interval.obj));
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
      interval.obj = new int[3];
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableIntervalCntDupsFunction implements DrillAggFunc {
    @Param
    NullableIntervalHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Workspace
    ObjectHolder interval;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      interval = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
      interval.obj = new int[3];
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          if (interval.obj != null) {
            ((int[]) interval.obj)[0] = in.days;
            ((int[]) interval.obj)[1] = in.months;
            ((int[]) interval.obj)[2] = in.milliseconds;
            if (!filter.isPresent(String.valueOf(interval.obj))) {
              filter.add(String.valueOf(interval.obj));
            } else {
              dups.value++;
            }
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
      interval.obj = new int[3];
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class VarCharCntDupsFunction implements DrillAggFunc {
    @Param
    VarCharHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
                in.start, in.end, in.buffer).getBytes();
        if (!filter.isPresent(buf)) {
          filter.add(buf);
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableVarCharCntDupsFunction implements DrillAggFunc {
    @Param
    NullableVarCharHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
                  in.start, in.end, in.buffer).getBytes();
          if (!filter.isPresent(buf)) {
            filter.add(buf);
          } else {
            dups.value++;
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class Var16CharCntDupsFunction implements DrillAggFunc {
    @Param
    Var16CharHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF16
                (in.start, in.end, in.buffer).getBytes();
        if (!filter.isPresent(buf)) {
          filter.add(buf);
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableVar16CharCntDupsFunction implements DrillAggFunc {
    @Param
    NullableVar16CharHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF16
                  (in.start, in.end, in.buffer).getBytes();
          if (!filter.isPresent(buf)) {
            filter.add(buf);
          } else {
            dups.value++;
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class VarBinaryCntDupsFunction implements DrillAggFunc {
    @Param
    VarBinaryHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8
                (in.start, in.end, in.buffer).getBytes();
        if (!filter.isPresent(buf)) {
          filter.add(buf);
        } else {
          dups.value++;
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }

  @FunctionTemplate(name = "approx_count_dups", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class NullableVarBinaryCntDupsFunction implements DrillAggFunc {
    @Param
    NullableVarBinaryHolder in;
    @Workspace
    ObjectHolder work;
    @Workspace BigIntHolder dups;
    @Output
    NullableBigIntHolder out;
    @Inject OptionManager options;
    @Workspace IntHolder ndvBloomFilterElts;
    @Workspace IntHolder ndvBloomFilterFPProb;

    @Override
    public void setup() {
      work = new ObjectHolder();
      dups.value = 0;
      ndvBloomFilterElts.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_ELEMENTS);
      ndvBloomFilterFPProb.value = (int) options.getLong(org.apache.drill.exec.ExecConstants.NDV_BLOOM_FILTER_FPOS_PROB);
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }

    @Override
    public void add() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        if (in.isSet == 1) {
          byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8
                  (in.start, in.end, in.buffer).getBytes();
          if (!filter.isPresent(buf)) {
            filter.add(buf);
          } else {
            dups.value++;
          }
        }
      }
    }

    @Override
    public void output() {
      if (work.obj != null) {
        com.clearspring.analytics.stream.membership.BloomFilter filter =
                (com.clearspring.analytics.stream.membership.BloomFilter ) work.obj;
        out.isSet = 1;
        out.value = dups.value;
      } else {
        out.isSet = 0;
      }
    }

    @Override
    public void reset() {
      dups.value = 0;
      work.obj = new com.clearspring.analytics.stream.membership.BloomFilter(ndvBloomFilterElts.value, ndvBloomFilterFPProb.value);
    }
  }
}