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
package com.qlangtech.tis.datax.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.*;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.DescriptorExtensionList;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.manage.IBasicAppSource;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
import com.qlangtech.tis.util.AttrValMap;
import com.qlangtech.tis.util.IPluginContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * DataX任务执行方式的抽象
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-07 16:46
 */
public abstract class DataxProcessor implements IBasicAppSource, IdentityName, IDataxProcessor, IStreamIncrGenerateStrategy {

    protected static final String DEFAULT_DATAX_PROCESSOR_NAME = "DataxProcessor";
    public static final String DATAX_CFG_DIR_NAME = "dataxCfg";
    public static final String DATAX_CREATE_DDL_DIR_NAME = "createDDL";


    public interface IDataxProcessorGetter {
        DataxProcessor get(String dataXName);
    }

    // for TEST
    public static IDataxProcessorGetter processorGetter;

    public static DataxProcessor load(IPluginContext pluginContext, String dataXName) {
        if (processorGetter != null) {
            return processorGetter.get(dataXName);
        }
        Optional<DataxProcessor> appSource = IAppSource.loadNullable(pluginContext, dataXName);
        if (appSource.isPresent()) {
            return appSource.get();
        } else {
            Descriptor<IAppSource> pluginDescMeta = DataxProcessor.getPluginDescMeta();
            Map<String, /** * attr key */com.alibaba.fastjson.JSONObject> formData
                    = new HashMap<String, com.alibaba.fastjson.JSONObject>() {
                @Override
                public JSONObject get(Object key) {
                    JSONObject o = new JSONObject();
                    o.put(Descriptor.KEY_primaryVal, null);
                    return o;
                }
            };
            Descriptor.ParseDescribable<Describable> appSourceParseDescribable
                    = pluginDescMeta.newInstance(pluginContext, AttrValMap.IAttrVals.rootForm(formData), Optional.empty());
            return (DataxProcessor) appSourceParseDescribable.getInstance();
        }
    }

    public static Descriptor<IAppSource> getPluginDescMeta() {
        DescriptorExtensionList<IAppSource, Descriptor<IAppSource>> descs = TIS.get().getDescriptorList(IAppSource.class);
        Optional<Descriptor<IAppSource>> dataxProcessDescs
                = descs.stream().filter((des) -> DEFAULT_DATAX_PROCESSOR_NAME.equals(des.getDisplayName())).findFirst();
        if (!dataxProcessDescs.isPresent()) {
            throw new IllegalStateException("dataX process descriptor:" + DEFAULT_DATAX_PROCESSOR_NAME + " relevant descriptor can not be null");
        }
        return dataxProcessDescs.get();
    }

    public static DataXCreateProcessMeta getDataXCreateProcessMeta(IPluginContext pluginContext, String dataxPipeName) {
        DataxWriter writer = DataxWriter.load(pluginContext, dataxPipeName);
        DataxWriter.BaseDataxWriterDescriptor writerDesc = (DataxWriter.BaseDataxWriterDescriptor) writer.getDescriptor();
        DataxReader dataxReader = DataxReader.load(pluginContext, dataxPipeName);
        DataxReader.BaseDataxReaderDescriptor descriptor = (DataxReader.BaseDataxReaderDescriptor) dataxReader.getDescriptor();

        DataXCreateProcessMeta processMeta = new DataXCreateProcessMeta(writer, dataxReader);
        // 使用这个属性来控制是否要进入创建流程的第三步
        processMeta.setReaderRDBMS(descriptor.isRdbms());
        processMeta.setReaderHasExplicitTable(descriptor.hasExplicitTable());
        processMeta.setWriterRDBMS(writerDesc.isRdbms());
        processMeta.setWriterSupportMultiTableInReader(writerDesc.isSupportMultiTable());

        return processMeta;
    }

    public abstract Application buildApp();

    private List<TableAlias> tableMaps;

    @Override
    public <T> T accept(IAppSourceVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /**
     * key:Source Table Name
     *
     * @return
     */
    @Override
    public TableAliasMapper getTabAlias() {
        if (tableMaps == null) {
            return TableAliasMapper.Null;//Collections.emptyMap();
        }
        return new TableAliasMapper(this.tableMaps.stream()
                .collect(Collectors.toMap((m) -> {
                    if (StringUtils.isEmpty(m.getFrom())) {
                        throw new IllegalArgumentException("table mapper from can not be empty");
                    }
                    return m.getFrom();
                }, (m) -> {
                    return m;
                })));
    }

    public void saveCreateTableDDL(IPluginContext pluginCtx, StringBuffer createDDL, String sqlFileName, boolean overWrite) throws IOException {
        if (StringUtils.isEmpty(sqlFileName)) {
            throw new IllegalArgumentException("param sqlFileName can not be empty");
        }
        File createDDLDir = this.getDataxCreateDDLDir(pluginCtx);
        File f = new File(createDDLDir, sqlFileName);
        // 判断文件是否已经存在，如果已经存在的话就不需要生成了
        if (overWrite || !f.exists()) {
            FileUtils.write(f, createDDL.toString(), TisUTF8.get());
        }
    }

    @Override
    public final boolean isReaderUnStructed(IPluginContext pluginCtx) {
        DataXCreateProcessMeta dataXCreateProcessMeta = getDataXCreateProcessMeta(pluginCtx, this.identityValue());
        return dataXCreateProcessMeta.isReaderUnStructed();
    }

    @Override
    public boolean isRDBMS2UnStructed(IPluginContext pluginCtx) {
        DataXCreateProcessMeta dataXCreateProcessMeta = getDataXCreateProcessMeta(pluginCtx, this.identityValue());
        return dataXCreateProcessMeta.isReaderRDBMS() && !dataXCreateProcessMeta.isWriterRDBMS();
    }

    @Override
    public boolean isRDBMS2RDBMS(IPluginContext pluginCtx) {
        DataXCreateProcessMeta dataXCreateProcessMeta = getDataXCreateProcessMeta(pluginCtx, this.identityValue());
        return dataXCreateProcessMeta.isReaderRDBMS() && dataXCreateProcessMeta.isWriterRDBMS();
    }

    @Override
    public boolean isWriterSupportMultiTableInReader(IPluginContext pluginCtx) {
        DataXCreateProcessMeta dataXCreateProcessMeta = getDataXCreateProcessMeta(pluginCtx, this.identityValue());
        return dataXCreateProcessMeta.isWriterSupportMultiTableInReader();
    }

    @Override
    public IDataxReader getReader(IPluginContext pluginCtx) {
        return DataxReader.load(pluginCtx, this.identityValue());
    }

    @Override
    public IDataxWriter getWriter(IPluginContext pluginCtx) {
        return DataxWriter.load(pluginCtx, this.identityValue());
    }

    public void setTableMaps(List<TableAlias> tableMaps) {
        this.tableMaps = tableMaps;
    }

    @Override
    public File getDataxCfgDir(IPluginContext pluginContext) {
        File dataXWorkDir = getDataXWorkDir(pluginContext);
        return new File(dataXWorkDir, DATAX_CFG_DIR_NAME);
    }

    @Override
    public File getDataxCreateDDLDir(IPluginContext pluginContext) {
        File dataXWorkDir = getDataXWorkDir(pluginContext);
        File ddlDir = new File(dataXWorkDir, DATAX_CREATE_DDL_DIR_NAME);
        try {
            FileUtils.forceMkdir(ddlDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ddlDir;
    }

    @Override
    public File getDataXWorkDir(IPluginContext pluginContext) {
        return IDataxProcessor.getDataXWorkDir(pluginContext, this.identityValue());
    }

    /**
     * 创建一个临时工作目录
     *
     * @param execId
     * @throws Exception
     */
    public void makeTempDir(String execId) throws Exception {

        File workingDir = getDataXWorkDir((IPluginContext) null);
        FileUtils.copyDirectory(workingDir, new File(workingDir.getParentFile(), KeyedPluginStore.TMP_DIR_NAME + workingDir.getName() + "-" + execId));
    }

    /**
     * dataX配置文件列表
     *
     * @return
     */
    @Override
    public DataXCfgGenerator.GenerateCfgs getDataxCfgFileNames(IPluginContext pluginContext) {
        File dataxCfgDir = getDataxCfgDir(pluginContext);
        if (!dataxCfgDir.exists()) {
            throw new IllegalStateException("dataxCfgDir is not exist:" + dataxCfgDir.getAbsolutePath());
        }
        if (dataxCfgDir.list().length < 1) {
            throw new IllegalStateException("dataxCfgDir is empty can not find any files:" + dataxCfgDir.getAbsolutePath());
        }

        //List<File> dataXConf = Lists.newArrayList();

        DataXCfgGenerator.GenerateCfgs genCfgs = DataXCfgGenerator.GenerateCfgs.readFromGen(dataxCfgDir);

        return genCfgs;
//        File dataXCfg = null;
//        for (String child : genCfgs.getDataxFiles()) {
//            dataXCfg = new File(dataxCfgDir, child);
//            if (!dataXCfg.exists()) {
//                throw new IllegalStateException("dataXCfg is not exist, path:" + dataXCfg.getAbsolutePath());
//            }
//            dataXConf.add(dataXCfg);
//        }
//
//        return dataXConf;
    }

    public static class DataXCreateProcessMeta extends DataXBasicProcessMeta {


        private final DataxWriter writer;
        private final DataxReader reader;

        public DataXCreateProcessMeta(DataxWriter writer, DataxReader reader) {
            this.writer = writer;
            this.reader = reader;
        }

        @JSONField(serialize = false)
        public DataxWriter getWriter() {
            return writer;
        }

        @JSONField(serialize = false)
        public DataxReader getReader() {
            return reader;
        }


    }

//     "setting": {
//        "speed": {
//            "channel": 3
//        },
//        "errorLimit": {
//            "record": 0,
//                    "percentage": 0.02
//        }
//    },


    /**
     * =======================================
     * impl:IStreamIncrGenerateStrategy
     * =========================================
     */
    @Override
    public boolean isExcludeFacadeDAOSupport() {
        return true;
    }
//
//    @Override
//    public Map<IEntityNameGetter, List<IValChain>> getTabTriggerLinker() {
//
//        Map<IEntityNameGetter, List<IValChain>> tabColsMapper = Maps.newHashMap();
//        IDataxReader reader = this.getReader(null);
//        Objects.requireNonNull(reader, "dataXReader can not be null");
//        List<ISelectedTab> selectedTabs = reader.getSelectedTabs();
//        for (ISelectedTab tab : selectedTabs) {
//            tabColsMapper.put(() -> EntityName.parse(tab.getName()), Collections.emptyList());
//        }
//
//        return tabColsMapper;
//    }
//
//    @Override
//    public Map<DBNode, List<String>> getDependencyTables(IDBTableNamesGetter dbTableNamesGetter) {
//        return Collections.emptyMap();
//    }
//
//    @Override
//    public IERRules getERRule() {
//        ERRules erRules = new ERRules() {
//            public List<PrimaryTableMeta> getPrimaryTabs() {
//                TabExtraMeta tabMeta = new TabExtraMeta();
//                PrimaryTableMeta ptab = new PrimaryTableMeta("tabName", tabMeta);
//                return Collections.singletonList(ptab);
//            }
//        };
//
//        return erRules;
//    }
}
