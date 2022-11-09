/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.hudi.format;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.conversion.DataStructureConverter;
import org.apache.flink.table.data.conversion.DataStructureConverters;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.Row;

/**
 * This function convert Flink BinaryRowData type into Flink Row Type.
 * @param <I>
 * @param <O>
 */
public class RowDataToRowFunction<I extends RowData, O extends Row> extends RichMapFunction<I, O> {

  DataStructureConverter<Object, Object> converter;

  public RowDataToRowFunction(DataType outputSchema) {
    this.converter = DataStructureConverters.getConverter(outputSchema);
  }

  @Override
  public void open(Configuration parameters) {
    //do nothing
  }

  @Override
  public O map(I value) throws Exception {
    return (O) converter.toExternal(value);
  }
}
