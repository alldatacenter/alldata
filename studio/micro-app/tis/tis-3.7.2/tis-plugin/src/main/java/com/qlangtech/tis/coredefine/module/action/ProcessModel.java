/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.coredefine.module.action;

import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataXBasicProcessMeta;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.StoreResourceType;
import com.qlangtech.tis.util.IPluginContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;


/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-01-25 17:43
 **/
public enum ProcessModel {

  CreateWorkFlow("createWorkFlow"  //
    , (pluginContext, dataxPipeName, reader, writer) -> {
    DataxWriter.BaseDataxWriterDescriptor writerDesc
      = (DataxWriter.BaseDataxWriterDescriptor) TIS.get().getDescriptor(writer.getString("impl"));
    return getDataXBasicProcessMeta(Optional.empty(), writerDesc);
  }
    , () -> {
    return DataxProcessor.getPluginDescMeta(DataxProcessor.DEFAULT_WORKFLOW_PROCESSOR_NAME);
  }, StoreResourceType.DataFlow
  ) //
  , CreateDatax("createDatax", (pluginContext, dataxPipeName, reader, writer) -> {
    DataxReader.BaseDataxReaderDescriptor readerDesc
      = (DataxReader.BaseDataxReaderDescriptor) TIS.get().getDescriptor(reader.getString("impl"));
    DataxWriter.BaseDataxWriterDescriptor writerDesc
      = (DataxWriter.BaseDataxWriterDescriptor) TIS.get().getDescriptor(writer.getString("impl"));
    DataXBasicProcessMeta processMeta = getDataXBasicProcessMeta(Optional.of(readerDesc), writerDesc);
    FileUtils.write(IDataxProcessor.getWriterDescFile(
      pluginContext, dataxPipeName), writerDesc.getId(), TisUTF8.get(), false);
    return processMeta;
  } //
    , () -> {
    return DataxProcessor.getPluginDescMeta(DataxProcessor.DEFAULT_DATAX_PROCESSOR_NAME);
  }, StoreResourceType.DataApp);

  private final String val;
  public final StoreResourceType resType;
  //  private final ValdateReaderAndWriter valdateReaderAndWriter;
  private final IProcessMetaCreator processMetaCreator;
  private final Supplier<Descriptor<IAppSource>> targetProcessDescsGetter;

  public static DataXBasicProcessMeta getDataXBasicProcessMeta(
    Optional<DataxReader.BaseDataxReaderDescriptor> readerDesc, DataxWriter.BaseDataxWriterDescriptor writerDesc) {
    Objects.requireNonNull(readerDesc, "readerDesc can not be null");
    Objects.requireNonNull(writerDesc, "writerDesc can not be null");
    DataXBasicProcessMeta processMeta = DataXBasicProcessMeta.getDataXBasicProcessMetaByReader(readerDesc);
    processMeta.setWriterRDBMS(writerDesc.isRdbms());
    processMeta.setWriterSupportMultiTableInReader(writerDesc.isSupportMultiTable());

    return processMeta;
  }

  public static ProcessModel parse(String val) {
    if (StringUtils.isEmpty(val)) {
      //throw new IllegalArgumentException(" pram val can not be null");
      return CreateDatax;
    }

    for (ProcessModel m : ProcessModel.values()) {
      if (m.val.equals(val)) {
        return m;
      }
    }
    return CreateDatax;
    // throw new IllegalStateException("illegal val:" + val);
  }

  /**
   * @param val
   * @param processMetaCreator
   * @param targetProcessDescsGetter
   * @param resType
   */
  private ProcessModel(String val
    , IProcessMetaCreator processMetaCreator
    , Supplier<Descriptor<IAppSource>> targetProcessDescsGetter, StoreResourceType resType) {
    this.val = val;
    // this.valdateReaderAndWriter = valdateReaderAndWriter;
    this.processMetaCreator = processMetaCreator;
    this.targetProcessDescsGetter = targetProcessDescsGetter;
    this.resType = resType;
  }

//  public boolean valdateReaderAndWriter(JSONObject reader, JSONObject writer, BasicModule module, Context context) {
//    return this.valdateReaderAndWriter.valdateReaderAndWriter(reader, writer, module, context);
//  }

  public DataXBasicProcessMeta createProcessMeta(IPluginContext pluginContext
    , String dataXName, JSONObject reader, JSONObject writer) throws Exception {
    return this.processMetaCreator.createProcessMeta(pluginContext, dataXName, reader, writer);
  }

  public Descriptor<IAppSource> getPluginDescMeta() {
    return this.targetProcessDescsGetter.get();
  }

  public DataxWriter loadWriter(IPluginContext pluginContext, JSONObject writerDesc, String name) {
    IDataxProcessor processor = (IDataxProcessor) loadDataXProcessor(pluginContext, name);
//    if (this == CreateDatax) {
//      DataxReader.load(pluginContext, name);
//    }
    DataxWriter writer = (DataxWriter) processor.getWriter(pluginContext, false);
    final String requestDescId = writerDesc.getString("impl");
    if (this == CreateDatax && writer != null && StringUtils.equals(writer.getDescriptor().getId(), requestDescId)) {
      DataxReader readerPlugin = DataxReader.load(pluginContext, name);
      DataxWriter.BaseDataxWriterDescriptor writerDescriptor = (DataxWriter.BaseDataxWriterDescriptor) writer.getDescriptor();
      if (!writerDescriptor.isSupportMultiTable() && readerPlugin.getSelectedTabs().size() > 1) {
        // 这种情况是不允许的，例如：elastic这样的writer中对于column的设置比较复杂，需要在writer plugin页面中完成，所以就不能支持在reader中选择多个表了
        throw new IllegalStateException("status is not allowed:!writerDescriptor.isSupportMultiTable() && readerPlugin.hasMulitTable()");
      }
      return writer;
      // pluginInfo.put("item", (new DescribableJSON(writer)).getItemJson());
    }

    return writer;
  }

  /**
   * 在页面UI上编辑流程显示的DataXReader控件，WorkFlow流程中是通过 dataSource 对应了多个DataXReader，在编辑流程中不直接编辑，所以返回为空
   *
   * @param pluginContext
   * @param name
   * @return
   */
  public Optional<DataxReader> getDataXReader(IPluginContext pluginContext, String name) {
    IDataxProcessor processor = (IDataxProcessor) this.loadDataXProcessor(pluginContext, name);
    if (this == CreateDatax) {
      return Optional.of((DataxReader) processor.getReader(pluginContext));
    } else if (this == CreateWorkFlow) {
      return Optional.empty();
    } else {
      throw new IllegalStateException("illegal process model:" + this);
    }
  }

  public IAppSource loadDataXProcessor(IPluginContext pluginContext, String name) {
//    KeyedPluginStore.StoreResourceType resType = null;
//    if (this == CreateDatax) {
//      resType = KeyedPluginStore.StoreResourceType.DataApp;
//    } else if (this == CreateWorkFlow) {
//      resType = KeyedPluginStore.StoreResourceType.DataFlow;
//    } else {
//      throw new IllegalStateException("illega type:" + this);
//    }

    return (IAppSource) DataxProcessor.load(pluginContext, this.resType, name);
  }
}
