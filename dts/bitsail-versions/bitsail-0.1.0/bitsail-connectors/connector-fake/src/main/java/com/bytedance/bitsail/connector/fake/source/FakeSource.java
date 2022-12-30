/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.connector.fake.source;

import com.bytedance.bitsail.base.connector.reader.v1.Boundedness;
import com.bytedance.bitsail.base.connector.reader.v1.SourceReader;
import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.connector.base.source.SimpleSourceBase;
import com.bytedance.bitsail.connector.base.source.SimpleSourceReaderBase;

public class FakeSource extends SimpleSourceBase<Row> {

  @Override
  public void configure(ExecutionEnviron execution,
                        BitSailConfiguration readerConfiguration) {
    super.configure(execution, readerConfiguration);
  }

  @Override
  public Boundedness getSourceBoundedness() {
    return Boundedness.BOUNDEDNESS;
  }

  @Override
  public SimpleSourceReaderBase<Row> createSimpleReader(BitSailConfiguration readerConfiguration,
                                                        SourceReader.Context readerContext) {
    return new FakeSourceReader(readerConfiguration, readerContext);
  }

  @Override
  public String getReaderName() {
    return "fake";
  }
}
