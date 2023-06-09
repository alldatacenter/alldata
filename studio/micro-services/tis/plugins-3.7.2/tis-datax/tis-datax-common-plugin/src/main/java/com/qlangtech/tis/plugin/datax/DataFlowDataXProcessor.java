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

package com.qlangtech.tis.plugin.datax;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.datax.*;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.StoreResourceType;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.IDataSourceFactoryGetter;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta;
import com.qlangtech.tis.sql.parser.TopologyDir;
import com.qlangtech.tis.sql.parser.meta.DependencyNode;
import com.qlangtech.tis.util.IPluginContext;
import com.qlangtech.tis.util.UploadPluginMeta;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-01-05 22:10
 **/
public class DataFlowDataXProcessor implements IDataxProcessor, IAppSource, IdentityName {


    @FormField(ordinal = 1, identity = true, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.identity})
    public String name;
    @FormField(ordinal = 2, type = FormFieldType.SELECTABLE, validate = {Validator.require})
    public String globalCfg;

    @Override
    public String identityValue() {
        return this.name;
    }

    @Override
    public final StoreResourceType getResType() {
        return StoreResourceType.DataFlow;
    }

    @Override
    public IDataxReader getReader(IPluginContext pluginCtx) {
        // return DataxReader.load(pluginCtx, true, this.name);
        throw new UnsupportedOperationException("dataflow processor not support single reader getter");
    }

    @Override
    public TableAliasMapper getTabAlias(IPluginContext pluginCtx) {
        return TableAliasMapper.Null;
    }

//    @Override
//    public TableAliasMapper getTabAlias() {
//        return TableAliasMapper.Null;
//    }

    @Override
    public List<IDataxReader> getReaders(IPluginContext pluginCtx) {

        try {
            List<IDataxReader> readers = Lists.newArrayList();

            SqlTaskNodeMeta.SqlDataFlowTopology topology = getTopology();

            List<DependencyNode> dumpNodes = topology.getDumpNodes();


            Map<String, Set<String>> dbIds = Maps.newHashMap();
            Set<String> tabs = null;
            for (DependencyNode dump : dumpNodes) {
                tabs = dbIds.get(dump.getDbName());
                if (tabs == null) {
                    tabs = Sets.newHashSet();
                    dbIds.put(dump.getDbName(), tabs);
                }
                tabs.add(dump.getName());
                // dbIds.add(dump.getDbName());
            }

            dbIds.entrySet().forEach((entry) -> {
                readers.add(new AdapterDataxReader(DataxReader.load(null, true, entry.getKey())) {
                    @Override
                    public IGroupChildTaskIterator getSubTasks() {
                        return super.getSubTasks((tab) -> entry.getValue().contains(tab.getName()));
                    }
                    //                    @Override
//                    public <T extends ISelectedTab> List<T> getSelectedTabs() {
//                        List<T> tabs = super.getSelectedTabs();
//                        return tabs.stream().filter((tab) -> entry.getValue().contains(tab.getName())).collect(Collectors.toList());
//                    }
                });
            });

            return readers;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SqlTaskNodeMeta.SqlDataFlowTopology getTopology() {
        try {
            return SqlTaskNodeMeta.getSqlDataFlowTopology(this.name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IDataxGlobalCfg getDataXGlobalCfg() {
        IDataxGlobalCfg globalCfg = ParamsConfig.getItem(this.globalCfg, IDataxGlobalCfg.KEY_DISPLAY_NAME);
        Objects.requireNonNull(globalCfg, "dataX Global config can not be null");
        return globalCfg;
    }


    @Override
    public IDataxWriter getWriter(IPluginContext pluginCtx, boolean validateNull) {
        return DataxWriter.load(pluginCtx, StoreResourceType.DataFlow, this.name, validateNull);
    }


    public DataSourceFactory getWriterDataSourceFactory() {
        DataSourceFactory writerDS = null;

        IDataxWriter writer = this.getWriter(null);
        if (!(writer instanceof IDataSourceFactoryGetter)) {
            throw new IllegalStateException("writer:"
                    + writer.getClass().getName() + " must be type of " + IDataSourceFactoryGetter.class.getName());
        }

        writerDS = ((IDataSourceFactoryGetter) writer).getDataSourceFactory();
        return writerDS;
    }

    @Override
    public File getDataXWorkDir(IPluginContext pluginContext) {

        KeyedPluginStore.KeyVal keyVal = KeyedPluginStore.AppKey.calAppName(pluginContext, this.name);
        TopologyDir topoDir = SqlTaskNodeMeta.getTopologyDir(this.name);
        File localSubFileDir = topoDir.getLocalSubFileDir();
        if (StringUtils.isEmpty(keyVal.getSuffix())) {
            return localSubFileDir;
        } else {
            return new File(localSubFileDir.getParentFile(), keyVal.getKeyVal());
        }
    }

    @Override
    public File getDataxCfgDir(IPluginContext pluginCtx) {
        return DataxProcessor.getDataxCfgDir(pluginCtx, this);
    }

    @Override
    public File getDataxCreateDDLDir(IPluginContext pluginContext) {
        return DataxProcessor.getDataxCreateDDLDir(pluginContext, this);
    }

    @Override
    public void saveCreateTableDDL(IPluginContext pluginCtx, StringBuffer createDDL, String sqlFileName, boolean overWrite) throws IOException {
        File createDDLDir = this.getDataxCreateDDLDir(pluginCtx);
        DataxProcessor.saveCreateTableDDL(createDDL, createDDLDir, sqlFileName, overWrite);
    }


    @Override
    public boolean isReaderUnStructed(IPluginContext pluginCtx) {
        return false;
    }

    @Override
    public boolean isRDBMS2UnStructed(IPluginContext pluginCtx) {
        return false;
    }

    @Override
    public boolean isRDBMS2RDBMS(IPluginContext pluginCtx) {
        return true;
    }

    @Override
    public boolean isWriterSupportMultiTableInReader(IPluginContext pluginCtx) {
        return true;
    }

    @Override
    public DataXCfgGenerator.GenerateCfgs getDataxCfgFileNames(IPluginContext pluginCtx) {
        return DataxProcessor.getDataxCfgFileNames(pluginCtx, this);
    }


    @TISExtension()
    public static class DescriptorImpl extends Descriptor<IAppSource> {

        public DescriptorImpl() {
            super();
            this.registerSelectOptions(DefaultDataxProcessor.KEY_FIELD_NAME, () -> ParamsConfig.getItems(IDataxGlobalCfg.KEY_DISPLAY_NAME));
        }

        public boolean validateName(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            UploadPluginMeta pluginMeta = (UploadPluginMeta) context.get(UploadPluginMeta.KEY_PLUGIN_META);
            Objects.requireNonNull(pluginMeta, "pluginMeta can not be null");
            if (pluginMeta.isUpdate()) {
                return true;
            }
            return msgHandler.validateBizLogic(IFieldErrorHandler.BizLogic.WORKFLOW_NAME_DUPLICATE, context, fieldName, value);
        }

        @Override
        public String getDisplayName() {
            return DataxProcessor.DEFAULT_WORKFLOW_PROCESSOR_NAME;
        }
    }
}
