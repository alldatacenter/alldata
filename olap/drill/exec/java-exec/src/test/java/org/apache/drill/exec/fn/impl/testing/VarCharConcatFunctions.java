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
package org.apache.drill.exec.fn.impl.testing;

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;

import javax.inject.Inject;

public class VarCharConcatFunctions {

  @FunctionTemplate(name = "concat_varchar",
                    isVarArg = true,
                    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL,
                    scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class VarCharConcatFunction implements DrillSimpleFunc {
    @Param VarCharHolder[] inputs;
    @Output VarCharHolder out;
    @Inject DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      int length = 0;
      for (VarCharHolder input : inputs) {
        length += input.end - input.start;
      }

      out.buffer = buffer = buffer.reallocIfNeeded(length);
      out.start = out.end = 0;

      for (VarCharHolder input : inputs) {
        for (int id = input.start; id < input.end; id++) {
          out.buffer.setByte(out.end++, input.buffer.getByte(id));
        }
      }
    }
  }

  @FunctionTemplate(name = "concat_varchar",
                    isVarArg = true,
                    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL,
                    scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class NullableVarCharConcatFunction implements DrillSimpleFunc {
    @Param NullableVarCharHolder[] inputs;
    @Output VarCharHolder out;
    @Inject DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      int length = 0;
      for (NullableVarCharHolder input : inputs) {
        length += input.end - input.start;
      }

      out.buffer = buffer = buffer.reallocIfNeeded(length);
      out.start = out.end = 0;

      for (NullableVarCharHolder input : inputs) {
        for (int id = input.start; id < input.end; id++) {
          out.buffer.setByte(out.end++, input.buffer.getByte(id));
        }
      }
    }
  }

  @FunctionTemplate(name = "concat_delim",
                    isVarArg = true,
                    scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class VarCharConcatWsFunction implements DrillSimpleFunc {
    @Param VarCharHolder separator;
    @Param VarCharHolder[] inputs;
    @Output NullableVarCharHolder out;
    @Inject DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      int separatorLength = separator.end - separator.start;
      int length = inputs.length > 0 ? (inputs.length - 1) * separatorLength : 0;
      for (VarCharHolder input : inputs) {
        length += input.end - input.start;
      }

      out.isSet = 1;
      out.buffer = buffer = buffer.reallocIfNeeded(length);
      out.start = out.end = 0;

      boolean addSeparator = false;

      for (VarCharHolder input : inputs) {
        if (addSeparator) {
          for (int id = separator.start; id < separator.end; id++) {
            out.buffer.setByte(out.end++, separator.buffer.getByte(id));
          }
        }

        addSeparator = true;
        for (int id = input.start; id < input.end; id++) {
          out.buffer.setByte(out.end++, input.buffer.getByte(id));
        }

      }
    }
  }


  @FunctionTemplate(name = "concat_delim",
                    isVarArg = true,
                    scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class NullableVarCharConcatWsFunction implements DrillSimpleFunc {
    @Param VarCharHolder separator;
    @Param NullableVarCharHolder[] inputs;
    @Output NullableVarCharHolder out;
    @Inject DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      int separatorLength = separator.end - separator.start;
      int length = inputs.length > 0 ? (inputs.length - 1) * separatorLength : 0;
      for (NullableVarCharHolder input : inputs) {
        if (input.isSet > 0) {
          length += input.end - input.start;
        }
      }

      out.isSet = 1;
      out.buffer = buffer = buffer.reallocIfNeeded(length);
      out.start = out.end = 0;

      boolean addSeparator = false;

      for (NullableVarCharHolder input : inputs) {
        if (addSeparator) {
          for (int id = separator.start; id < separator.end; id++) {
            out.buffer.setByte(out.end++, separator.buffer.getByte(id));
          }
        }
        if (input.isSet > 0) {
          addSeparator = true;
          for (int id = input.start; id < input.end; id++) {
            out.buffer.setByte(out.end++, input.buffer.getByte(id));
          }
        }
      }
    }
  }


  @FunctionTemplate(name = "concat_delim",
                    isVarArg = true,
                    scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class VarCharNullableConcatWsFunction implements DrillSimpleFunc {
    @Param NullableVarCharHolder separator;
    @Param VarCharHolder[] inputs;
    @Output NullableVarCharHolder out;
    @Inject DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      if (separator.isSet == 0) {
        return;
      }
      int separatorLength = separator.end - separator.start;
      int length = inputs.length > 0 ? (inputs.length - 1) * separatorLength : 0;
      for (VarCharHolder input : inputs) {
        length += input.end - input.start;
      }

      out.isSet = 1;
      out.buffer = buffer = buffer.reallocIfNeeded(length);
      out.start = out.end = 0;

      boolean addSeparator = false;

      for (VarCharHolder input : inputs) {
        if (addSeparator) {
          for (int id = separator.start; id < separator.end; id++) {
            out.buffer.setByte(out.end++, separator.buffer.getByte(id));
          }
        }
        addSeparator = true;
        for (int id = input.start; id < input.end; id++) {
          out.buffer.setByte(out.end++, input.buffer.getByte(id));
        }
      }
    }
  }

  @FunctionTemplate(name = "concat_delim",
                    isVarArg = true,
                    scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class NullableVarCharNullableConcatWsFunction implements DrillSimpleFunc {
    @Param NullableVarCharHolder separator;
    @Param NullableVarCharHolder[] inputs;
    @Output NullableVarCharHolder out;
    @Inject DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      if (separator.isSet == 0) {
        return;
      }
      int separatorLength = separator.end - separator.start;
      int length = inputs.length > 0 ? (inputs.length - 1) * separatorLength : 0;
      for (NullableVarCharHolder input : inputs) {
        if (input.isSet > 0) {
          length += input.end - input.start;
        }
      }

      out.isSet = 1;
      out.buffer = buffer = buffer.reallocIfNeeded(length);
      out.start = out.end = 0;

      boolean addSeparator = false;

      for (NullableVarCharHolder input : inputs) {
        if (addSeparator) {
          for (int id = separator.start; id < separator.end; id++) {
            out.buffer.setByte(out.end++, separator.buffer.getByte(id));
          }
        }
        if (input.isSet > 0) {
          addSeparator = true;
          for (int id = input.start; id < input.end; id++) {
            out.buffer.setByte(out.end++, input.buffer.getByte(id));
          }
        }
      }
    }
  }
}
