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

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.DateHolder;
import org.apache.drill.exec.expr.holders.VarDecimalHolder;
import org.apache.drill.exec.expr.holders.NullableVarDecimalHolder;
import org.apache.drill.exec.expr.holders.Float4Holder;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.expr.holders.NullableBigIntHolder;
import org.apache.drill.exec.expr.holders.NullableBitHolder;
import org.apache.drill.exec.expr.holders.NullableDateHolder;
import org.apache.drill.exec.expr.holders.NullableFloat4Holder;
import org.apache.drill.exec.expr.holders.NullableFloat8Holder;
import org.apache.drill.exec.expr.holders.NullableIntHolder;
import org.apache.drill.exec.expr.holders.NullableTimeHolder;
import org.apache.drill.exec.expr.holders.NullableTimeStampHolder;
import org.apache.drill.exec.expr.holders.NullableVar16CharHolder;
import org.apache.drill.exec.expr.holders.NullableVarBinaryHolder;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.expr.holders.TimeHolder;
import org.apache.drill.exec.expr.holders.TimeStampHolder;
import org.apache.drill.exec.expr.holders.Var16CharHolder;
import org.apache.drill.exec.expr.holders.VarBinaryHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;

@SuppressWarnings("unused")
public class Hash32Functions {

  @FunctionTemplate(names = {"hash", "hash32"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL )
  public static class NullableFloatHash implements DrillSimpleFunc {

    @Param NullableFloat4Holder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      if (in.isSet == 0) {
        out.value = 0;
      } else {
        out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
      }
    }
  }

  @FunctionTemplate(names = {"hash", "hash32"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL )
  public static class FloatHash implements DrillSimpleFunc {

    @Param Float4Holder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
    }
  }

  @FunctionTemplate(names = {"hash", "hash32"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL )
  public static class NullableDoubleHash implements DrillSimpleFunc {

    @Param NullableFloat8Holder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      if (in.isSet == 0) {
        out.value = 0;
      } else {
        out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
      }
    }
  }

  @FunctionTemplate(names = {"hash", "hash32"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL )
  public static class DoubleHash implements DrillSimpleFunc {

    @Param Float8Holder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
    }
  }

  @FunctionTemplate(names = {"hash", "hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL )
  public static class NullableVarBinaryHash implements DrillSimpleFunc {

    @Param NullableVarBinaryHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      if (in.isSet == 0) {
        out.value = 0;
      } else {
        out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.start, in.end, in.buffer, 0);
      }
    }
  }

  @FunctionTemplate(names = {"hash", "hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL )
  public static class NullableVarCharHash implements DrillSimpleFunc {

    @Param NullableVarCharHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      if (in.isSet == 0) {
        out.value = 0;
      } else {
        out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.start, in.end, in.buffer, 0);
      }
    }
  }

  @FunctionTemplate(names = {"hash", "hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL )
  public static class NullableVar16CharHash implements DrillSimpleFunc {

    @Param NullableVar16CharHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      if (in.isSet == 0) {
        out.value = 0;
      } else {
        out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.start, in.end, in.buffer, 0);
      }
    }
  }

  @FunctionTemplate(names = {"hash", "hash32"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class NullableBigIntHash implements DrillSimpleFunc {

    @Param NullableBigIntHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      if (in.isSet == 0) {
        out.value = 0;
      }
      else {
        out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
      }
    }
  }

  @FunctionTemplate(names = {"hash", "hash32"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class NullableIntHash implements DrillSimpleFunc {
    @Param NullableIntHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      if (in.isSet == 0) {
        out.value = 0;
      }
      else {
        out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
      }
    }
  }

  @FunctionTemplate(names = {"hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class VarBinaryHash implements DrillSimpleFunc {

    @Param VarBinaryHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.start, in.end, in.buffer, 0);
    }
  }

  @FunctionTemplate(names = {"hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class VarCharHash implements DrillSimpleFunc {

    @Param VarCharHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.start, in.end, in.buffer, 0);
    }
  }

  @FunctionTemplate(names = {"hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class Var16CharHash implements DrillSimpleFunc {

    @Param Var16CharHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.start, in.end, in.buffer, 0);
    }
  }

  @FunctionTemplate(names = {"hash", "hash32"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class BigIntHash implements DrillSimpleFunc {

    @Param BigIntHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
    }
  }

  @FunctionTemplate(names = {"hash", "hash32"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class IntHash implements DrillSimpleFunc {
    @Param IntHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
    }
  }
  @FunctionTemplate(names = {"hash", "hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class DateHash implements DrillSimpleFunc {
    @Param  DateHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
    }
  }

  @FunctionTemplate(names = {"hash", "hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class NullableDateHash implements DrillSimpleFunc {
    @Param  NullableDateHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      if (in.isSet == 0) {
        out.value = 0;
      } else {
        out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
      }
    }
  }

  @FunctionTemplate(names = {"hash", "hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class TimeStampHash implements DrillSimpleFunc {
    @Param  TimeStampHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
    }
  }

  @FunctionTemplate(names = {"hash", "hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class NullableTimeStampHash implements DrillSimpleFunc {
    @Param  NullableTimeStampHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      if (in.isSet == 0) {
        out.value = 0;
      } else {
        out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
      }
    }
  }

  @FunctionTemplate(names = {"hash", "hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class TimeHash implements DrillSimpleFunc {
    @Param  TimeHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
    }
  }

  @FunctionTemplate(names = {"hash", "hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class NullableTimeHash implements DrillSimpleFunc {
    @Param  NullableTimeHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      if (in.isSet == 0) {
        out.value = 0;
      } else {
        out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
      }
    }
  }

  @FunctionTemplate(names = {"hash", "hash32"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class VarDecimalHash implements DrillSimpleFunc {
    @Param  VarDecimalHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.start, in.end, in.buffer, 0);
    }
  }

  @FunctionTemplate(names = {"hash", "hash32"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class NullableVarDecimalHash implements DrillSimpleFunc {
    @Param  NullableVarDecimalHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      if (in.isSet == 0) {
        out.value = 0;
      } else {
        out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.start, in.end, in.buffer, 0);
      }
    }
  }

  @FunctionTemplate(names = {"hash", "hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL )
  public static class NullableBitHash implements DrillSimpleFunc {

    @Param NullableBitHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      if (in.isSet == 0) {
        out.value = 0;
      } else {
        out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
      }
    }
  }

  @FunctionTemplate(names = {"hash", "hash32", "hash32AsDouble"}, scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL )
  public static class BitHash implements DrillSimpleFunc {

    @Param BitHolder in;
    @Output IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.expr.fn.impl.HashHelper.hash32(in.value, 0);
    }
  }

}
