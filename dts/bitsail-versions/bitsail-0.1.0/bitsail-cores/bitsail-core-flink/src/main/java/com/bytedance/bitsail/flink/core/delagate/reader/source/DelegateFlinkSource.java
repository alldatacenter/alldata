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

package com.bytedance.bitsail.flink.core.delagate.reader.source;

import com.bytedance.bitsail.base.connector.reader.v1.SourceSplit;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfoUtils;
import com.bytedance.bitsail.flink.core.delagate.reader.translation.BoundednessTranslation;
import com.bytedance.bitsail.flink.core.delagate.serializer.DelegateFlinkSourceSplitSerializer;
import com.bytedance.bitsail.flink.core.delagate.serializer.DelegateSimpleVersionedSerializer;
import com.bytedance.bitsail.flink.core.typeutils.NativeFlinkTypeInfoUtil;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.core.io.SimpleVersionedSerializer;

import java.io.Serializable;
import java.util.List;

public class DelegateFlinkSource<T, SplitT extends SourceSplit, StateT extends Serializable>
    implements Source<T, DelegateFlinkSourceSplit<SplitT>, StateT>, ResultTypeQueryable<T> {

  public final com.bytedance.bitsail.base.connector.reader.v1.Source<T, SplitT, StateT> source;

  private final BitSailConfiguration commonConfiguration;
  private final BitSailConfiguration readerConfiguration;
  private final TypeInfo<?>[] typeInfos;

  public DelegateFlinkSource(com.bytedance.bitsail.base.connector.reader.v1.Source<T, SplitT, StateT> source,
                             BitSailConfiguration commonConfiguration,
                             BitSailConfiguration readerConfiguration) {
    this.source = source;
    this.commonConfiguration = commonConfiguration;
    this.readerConfiguration = readerConfiguration;
    //todo exception for columns.
    List<ColumnInfo> columnInfos = readerConfiguration
        .get(ReaderOptions.BaseReaderOptions.COLUMNS);
    this.typeInfos = TypeInfoUtils
        .getTypeInfos(source.createTypeInfoConverter(),
            columnInfos);
  }

  @Override
  public Boundedness getBoundedness() {
    return BoundednessTranslation.to(source.getSourceBoundedness());
  }

  @Override
  public SourceReader<T, DelegateFlinkSourceSplit<SplitT>> createReader(SourceReaderContext readerContext) throws Exception {
    return new DelegateFlinkSourceReader<>(
        source::createReader,
        readerContext,
        source.getReaderName(),
        typeInfos,
        commonConfiguration,
        readerConfiguration
    );
  }

  @Override
  public SplitEnumerator<DelegateFlinkSourceSplit<SplitT>, StateT> createEnumerator(SplitEnumeratorContext<DelegateFlinkSourceSplit<SplitT>> enumContext)
      throws Exception {
    return restoreEnumerator(enumContext, null);
  }

  @Override
  public SplitEnumerator<DelegateFlinkSourceSplit<SplitT>, StateT> restoreEnumerator(SplitEnumeratorContext<DelegateFlinkSourceSplit<SplitT>> enumContext,
                                                                                     StateT checkpoint) throws Exception {
    return new DelegateFlinkSourceSplitEnumerator<>(source::createSplitCoordinator, enumContext, checkpoint);
  }

  @Override
  public SimpleVersionedSerializer<DelegateFlinkSourceSplit<SplitT>> getSplitSerializer() {
    return new DelegateFlinkSourceSplitSerializer<>(source.getSplitSerializer());
  }

  @Override
  public SimpleVersionedSerializer<StateT> getEnumeratorCheckpointSerializer() {
    return DelegateSimpleVersionedSerializer.delegate(source.getSplitCoordinatorCheckpointSerializer());
  }

  @Override
  public TypeInformation<T> getProducedType() {
    return (TypeInformation<T>) NativeFlinkTypeInfoUtil.getRowTypeInformation(typeInfos);
  }
}
