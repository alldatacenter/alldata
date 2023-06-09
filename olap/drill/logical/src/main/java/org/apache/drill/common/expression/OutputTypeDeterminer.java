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
package org.apache.drill.common.expression;

import java.util.List;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;

public interface OutputTypeDeterminer {

  public static OutputTypeDeterminer FIXED_BIT = new FixedType(Types.required(MinorType.BIT));
  public static OutputTypeDeterminer FIXED_INT = new FixedType(Types.required(MinorType.INT));
  public static OutputTypeDeterminer FIXED_BIGINT = new FixedType(Types.required(MinorType.BIGINT));
  public static OutputTypeDeterminer FIXED_FLOAT4 = new FixedType(Types.required(MinorType.FLOAT4));
  public static OutputTypeDeterminer FIXED_FLOAT8 = new FixedType(Types.required(MinorType.FLOAT8));
  public static OutputTypeDeterminer FIXED_VARCHAR = new FixedType(Types.required(MinorType.VARCHAR));
  public static OutputTypeDeterminer FIXED_VARBINARY = new FixedType(Types.required(MinorType.VARBINARY));
  public static OutputTypeDeterminer FIXED_VAR16CHAR = new FixedType(Types.required(MinorType.VAR16CHAR));

  public MajorType getOutputType(List<LogicalExpression> expressions);


  public static class FixedType implements OutputTypeDeterminer{
    public MajorType outputType;


    public FixedType(MajorType outputType) {
      super();
      this.outputType = outputType;
    }


    @Override
    public MajorType getOutputType(List<LogicalExpression> expressions) {
      return outputType;
    }

  }

  public static class SameAsFirstInput implements OutputTypeDeterminer{
    @Override
    public MajorType getOutputType(List<LogicalExpression> expressions) {
      return expressions.get(0).getMajorType();
    }
  }

  public static class SameAsAnySoft implements OutputTypeDeterminer{
    @Override
    public MajorType getOutputType(List<LogicalExpression> expressions) {
      for(LogicalExpression e : expressions){
        if(e.getMajorType().getMode() == DataMode.OPTIONAL){
          return e.getMajorType();
        }
      }
      return expressions.get(0).getMajorType();
    }
  }

  public static class NullIfNullType implements OutputTypeDeterminer{
    public MinorType outputMinorType;


    public NullIfNullType(MinorType outputType) {
      super();
      this.outputMinorType = outputType;
    }

    @Override
    public MajorType getOutputType(List<LogicalExpression> expressions) {
      for(LogicalExpression e : expressions){
        if(e.getMajorType().getMode() == DataMode.OPTIONAL){
          return Types.optional(outputMinorType);
        }
      }
      return Types.required(outputMinorType);
    }
  }

}
