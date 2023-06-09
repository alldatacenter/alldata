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
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.Float4Holder;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.expr.holders.RepeatedBigIntHolder;
import org.apache.drill.exec.expr.holders.RepeatedBitHolder;
import org.apache.drill.exec.expr.holders.RepeatedFloat4Holder;
import org.apache.drill.exec.expr.holders.RepeatedFloat8Holder;
import org.apache.drill.exec.expr.holders.RepeatedIntHolder;
import org.apache.drill.exec.expr.holders.RepeatedTinyIntHolder;
import org.apache.drill.exec.expr.holders.RepeatedVarCharHolder;
import org.apache.drill.exec.expr.holders.TinyIntHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;

public class SimpleRepeatedFunctions {

  private SimpleRepeatedFunctions() {
  }

  @FunctionTemplate(name = "repeated_contains", scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class ContainsBigInt implements DrillSimpleFunc {

    @Param RepeatedBigIntHolder listToSearch;
    @Param BigIntHolder targetValue;
    @Output BitHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = 0;
      for (int i = listToSearch.start; i < listToSearch.end; i++) {
        if (listToSearch.vector.getAccessor().get(i) == targetValue.value) {
          out.value = 1;
          break;
        }
      }
    }

  }

  @FunctionTemplate(name = "repeated_contains", scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class ContainsInt implements DrillSimpleFunc {

    @Param RepeatedIntHolder listToSearch;
    @Param IntHolder targetValue;
    @Output BitHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = 0;
      for (int i = listToSearch.start; i < listToSearch.end; i++) {
        if (listToSearch.vector.getAccessor().get(i) == targetValue.value) {
          out.value = 1;
          break;
        }
      }
    }

  }

  @FunctionTemplate(name = "repeated_contains", scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class ContainsTinyInt implements DrillSimpleFunc {

    @Param RepeatedTinyIntHolder listToSearch;
    @Param TinyIntHolder targetValue;
    @Output BitHolder out;

    public void setup() {
    }

    public void eval() {
      for (int i = listToSearch.start; i < listToSearch.end; i++) {
        if (listToSearch.vector.getAccessor().get(i) == targetValue.value) {
          out.value = 1;
          break;
        }
      }
    }

  }

  @FunctionTemplate(name = "repeated_contains", scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class ContainsBit implements DrillSimpleFunc {

    @Param RepeatedBitHolder listToSearch;
    @Param BitHolder targetValue;
    @Output BitHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = 0;
      for (int i = listToSearch.start; i < listToSearch.end; i++) {
        if (listToSearch.vector.getAccessor().get(i) == targetValue.value) {
          out.value = 1;
          break;
        }
      }
    }

  }

  @FunctionTemplate(name = "repeated_contains", scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class ContainsFloat4 implements DrillSimpleFunc {

    @Param RepeatedFloat4Holder listToSearch;
    @Param Float4Holder targetValue;
    @Output BitHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = 0;
      for (int i = listToSearch.start; i < listToSearch.end; i++) {
        if (listToSearch.vector.getAccessor().get(i) == targetValue.value) {
          out.value = 1;
          break;
        }
      }
    }

  }

  @FunctionTemplate(name = "repeated_contains", scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class ContainsFloat8 implements DrillSimpleFunc {

    @Param RepeatedFloat8Holder listToSearch;
    @Param Float8Holder targetValue;
    @Output BitHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = 0;
      for (int i = listToSearch.start; i < listToSearch.end; i++) {
        if (listToSearch.vector.getAccessor().get(i) == targetValue.value) {
          out.value = 1;
          break;
        }
      }
    }

  }

  @FunctionTemplate(name = "repeated_contains", scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class ContainsVarChar implements DrillSimpleFunc {

    @Param RepeatedVarCharHolder listToSearch;
    @Param VarCharHolder targetValue;
    @Workspace VarCharHolder currVal;
    @Workspace java.util.regex.Matcher matcher;
    @Workspace org.apache.drill.exec.expr.fn.impl.CharSequenceWrapper charSequenceWrapper;

    @Output BitHolder out;

    public void setup() {
      currVal = new VarCharHolder();
      matcher = java.util.regex.Pattern.compile(
          org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(targetValue.start,  targetValue.end,  targetValue.buffer)).matcher("");
      charSequenceWrapper = new org.apache.drill.exec.expr.fn.impl.CharSequenceWrapper();
      matcher.reset(charSequenceWrapper);
    }

    public void eval() {
      for (int i = listToSearch.start; i < listToSearch.end; i++) {
        out.value = 0;
        listToSearch.vector.getAccessor().get(i, currVal);
        charSequenceWrapper.setBuffer(currVal.start, currVal.end, currVal.buffer);
        // Reusing same charSequenceWrapper, no need to pass it in.
        // This saves one method call since reset(CharSequence) calls reset()
        matcher.reset();
        if(matcher.find()) {
             out.value = 1;
             break;
          }
       }
    }
  }
}