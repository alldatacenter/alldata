/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.flink.core.plugins;

import com.bytedance.bitsail.base.messenger.common.MessengerGroup;
import com.bytedance.bitsail.common.BitSailException;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.types.Row;

/**
 * @class: OutputAdapter
 * @desc:
 **/

public class OutputAdapter extends AdapterPlugin {

  @Override
  public void initPlugin() {

  }

  @Override
  public Row transform(Row flinkRow, Row bitSailRow) throws BitSailException {
    return getRowBytesParser().parseBitSailRow(flinkRow, bitSailRow, getFlinkRowTypeInfo(), getAdapterConf());
  }

  @Override
  protected MessengerGroup getMessengerGroup() {
    return MessengerGroup.WRITER;
  }

  @Override
  public TypeInformation<?> getProducedType() {
    return getFlinkRowTypeInfo();
  }

  @Override
  public String getType() {
    return "OutputAdapter";
  }
}

