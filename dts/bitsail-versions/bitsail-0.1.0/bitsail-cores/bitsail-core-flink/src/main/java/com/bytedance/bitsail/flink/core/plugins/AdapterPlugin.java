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

import com.bytedance.bitsail.base.dirty.AbstractDirtyCollector;
import com.bytedance.bitsail.base.dirty.DirtyCollectorFactory;
import com.bytedance.bitsail.base.messenger.Messenger;
import com.bytedance.bitsail.base.messenger.MessengerFactory;
import com.bytedance.bitsail.base.messenger.common.MessengerGroup;
import com.bytedance.bitsail.base.messenger.context.MessengerContext;
import com.bytedance.bitsail.base.messenger.context.SimpleMessengerContext;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.ColumnCast;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.flink.core.legacy.connector.Pluggable;
import com.bytedance.bitsail.flink.core.parser.RowBytesParser;
import com.bytedance.bitsail.flink.core.runtime.RuntimeContextInjectable;
import com.bytedance.bitsail.flink.core.typeutils.ColumnFlinkTypeInfoUtil;
import com.bytedance.bitsail.flink.core.typeutils.NativeFlinkTypeInfoUtil;

import lombok.Getter;
import lombok.Setter;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * @class: AdapterPlugin
 * @desc:
 **/

public abstract class AdapterPlugin extends RichFlatMapFunction<Row, Row> implements ResultTypeQueryable, Pluggable {
  private static final Logger LOG = LoggerFactory.getLogger(AdapterPlugin.class);

  @Setter
  protected MessengerContext messengerContext;
  protected Messenger<Row> messenger;
  private AbstractDirtyCollector dirtyCollector;

  @Getter
  private BitSailConfiguration adapterConf;

  @Getter
  private RowTypeInfo flinkRowTypeInfo;

  @Getter
  private RowTypeInfo bitSailRowTypeInfo;

  @Getter
  private RowBytesParser rowBytesParser;

  private String instanceId;
  private BitSailConfiguration commonWithReaderConf;

  public void initFromConf(BitSailConfiguration commonWithReaderConf, BitSailConfiguration adapterConf, RowTypeInfo flinkRowTypeInfo) throws Exception {
    this.adapterConf = adapterConf;
    this.flinkRowTypeInfo = flinkRowTypeInfo;
    this.instanceId = commonWithReaderConf.getNecessaryOption(CommonOptions.INTERNAL_INSTANCE_ID, CommonErrorCode.CONFIG_ERROR);
    this.rowBytesParser = new RowBytesParser();
    this.commonWithReaderConf = commonWithReaderConf;
    this.initPlugin();
    this.messengerContext = SimpleMessengerContext
        .builder()
        .messengerGroup(getMessengerGroup())
        .instanceId(instanceId)
        .build();
    this.messenger = MessengerFactory.initMessenger(commonWithReaderConf, messengerContext);
    dirtyCollector = DirtyCollectorFactory.initDirtyCollector(commonWithReaderConf, messengerContext);
  }

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    ColumnCast.initColumnCast(commonWithReaderConf);
    if (messenger instanceof RuntimeContextInjectable) {
      ((RuntimeContextInjectable) messenger).setRuntimeContext(getRuntimeContext());
    }
    if (dirtyCollector instanceof RuntimeContextInjectable) {
      ((RuntimeContextInjectable) dirtyCollector).setRuntimeContext(getRuntimeContext());
    }
    messenger.open();
  }

  @Override
  public void close() throws Exception {
    if (Objects.nonNull(messenger)) {
      messenger.commit();
      messenger.close();
    }
    if (Objects.nonNull(dirtyCollector)) {
      dirtyCollector.storeDirtyRecords();
      dirtyCollector.close();
    }
  }

  @Override
  public void flatMap(Row inputRow, Collector<Row> output) throws IOException {
    try {
      Row outputRow = new Row(inputRow.getArity());
      transform(outputRow, inputRow);
      output.collect(outputRow);
    } catch (BitSailException e) {
      messenger.addFailedRecord(inputRow, e);
      dirtyCollector.collectDirty(inputRow, e, System.currentTimeMillis());
      LOG.debug("Transform one record failed. - " + inputRow.toString(), e);
    } catch (Exception e) {
      throw new IOException("Couldn't transform data - " + inputRow.toString(), e);
    }
  }

  protected void setBitSailTypeInfoFromFlink() {
    bitSailRowTypeInfo = getCustomTypeInfoFromFlink();
  }

  private RowTypeInfo getCustomTypeInfoFromFlink() {
    TypeInfo<?>[] typeInfos = NativeFlinkTypeInfoUtil.toTypeInfos(flinkRowTypeInfo);
    return ColumnFlinkTypeInfoUtil.getRowTypeInformation(typeInfos);
  }

  public abstract Row transform(Row outputRow, Row inputRow) throws BitSailException;

  protected abstract MessengerGroup getMessengerGroup();
}
