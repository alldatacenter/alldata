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

package com.qlangtech.tis.plugin.datax.hudi;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.config.hive.IHiveConn;
import com.qlangtech.tis.config.hive.IHiveConnGetter;
import com.qlangtech.tis.config.spark.ISparkConnGetter;
import com.qlangtech.tis.datax.IDataXBatchPost;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.TimeFormat;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.extension.impl.SuFormProperties;
import com.qlangtech.tis.fs.IPath;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.offline.FileSystemFactory;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.BasicFSWriter;
import com.qlangtech.tis.plugin.datax.HdfsWriterDescriptor;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.datax.hudi.spark.SparkSubmitParams;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Objects;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-21 13:02
 **/
@Public
public class DataXHudiWriter extends BasicFSWriter implements KeyedPluginStore.IPluginKeyAware, IHiveConn, IDataXBatchPost, IDataXHudiWriter {

    private static final Logger logger = LoggerFactory.getLogger(DataXHudiWriter.class);

    @FormField(ordinal = 0, type = FormFieldType.SELECTABLE, validate = {Validator.require})
    public String sparkConn;

    @FormField(ordinal = 1, validate = {Validator.require})
    public SparkSubmitParams sparkSubmitParam;

    @FormField(ordinal = 2, type = FormFieldType.SELECTABLE, validate = {Validator.require})
    public String hiveConn;

    @FormField(ordinal = 8, type = FormFieldType.ENUM, validate = {Validator.require})
    public String tabType;

    @FormField(ordinal = 9, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.db_col_name})
    public String partitionedBy;

//    @FormField(ordinal = 10, type = FormFieldType.ENUM, validate = {Validator.require})
//    // 目标源中是否自动创建表，这样会方便不少
//    public boolean autoCreateTable;


    @Override
    public ITISFileSystem getFileSystem() {
        return this.getFs().getFileSystem();
    }

    @Override
    public String getFsName() {
        return this.fsName;
    }

    @FormField(ordinal = 10, type = FormFieldType.INT_NUMBER, advance = true, validate = {Validator.require, Validator.integer})
    public Integer shuffleParallelism;

    @FormField(ordinal = 11, type = FormFieldType.ENUM, validate = {Validator.require})
    public String batchOp;


    @FormField(ordinal = 100, type = FormFieldType.TEXTAREA, advance = false, validate = {Validator.require})
    public String template;


    @Override
    public HudiWriteTabType getHudiTableType() {
        return HudiWriteTabType.parse(tabType);
    }

    @Override
    public IHiveConnGetter getHiveConnMeta() {
        return ParamsConfig.getItem(this.hiveConn, IHiveConnGetter.PLUGIN_NAME);
    }


//    /**
//     * @return
//     */
//    public static List<Option> allPrimaryKeys(Object contextObj) {
//        List<Option> pks = Lists.newArrayList();
//        pks.add(new Option("base_id"));
//        return pks;
//    }

    @Override
    protected HudiDataXContext getDataXContext(IDataxProcessor.TableMap tableMap) {
        return new HudiDataXContext(tableMap, this.dataXName);
    }


    public ISparkConnGetter getSparkConnGetter() {
        if (StringUtils.isEmpty(this.sparkConn)) {
            throw new IllegalArgumentException("param sparkConn can not be null");
        }
        return ISparkConnGetter.getConnGetter(this.sparkConn);
    }

    @Override
    public ExecutePhaseRange getPhaseRange() {
        return new ExecutePhaseRange(FullbuildPhase.FullDump, FullbuildPhase.JOIN);
    }

    public Connection getConnection() {
        try {
            ParamsConfig connGetter = (ParamsConfig) getHiveConnMeta();
            return connGetter.createConfigInstance();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setKey(KeyedPluginStore.Key key) {
        this.dataXName = key.keyVal.getVal();
    }

    @Override
    public String getTemplate() {
        return template;
    }

    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXHudiWriter.class, "DataXHudiWriter-tpl.json");
    }

    @Override
    public String getPartitionedBy() {
        return this.partitionedBy;
    }

    @TISExtension()
    public static class DefaultDescriptor extends HdfsWriterDescriptor implements DataxWriter.IRewriteSuFormProperties {
        private transient SuFormProperties rewriteSubFormProperties;

        public DefaultDescriptor() {
            super();
            this.registerSelectOptions(KEY_FIELD_NAME_SPARK_CONN, () -> ParamsConfig.getItems(ISparkConnGetter.PLUGIN_NAME));
            this.registerSelectOptions(KEY_FIELD_NAME_HIVE_CONN, () -> ParamsConfig.getItems(IHiveConnGetter.PLUGIN_NAME));
        }

        @Override
        public boolean isSupportIncr() {
            return true;
        }

        @Override
        public PluginVender getVender() {
            return PluginVender.TIS;
        }

        public boolean validateFsName(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            if (!HUDI_FILESYSTEM_NAME.equals(value)) {
                msgHandler.addFieldError(context, fieldName, "必须定义一个ID为'" + HUDI_FILESYSTEM_NAME + "'的配置项");
                return false;
            }
            return true;
        }

        public boolean validateTabType(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {

            try {
                HudiWriteTabType.parse(value);
            } catch (Throwable e) {
                msgHandler.addFieldError(context, fieldName, e.getMessage());
                return false;
            }
            return true;
        }

        public boolean validateBatchOp(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {

            try {
                BatchOpMode.parse(value);
            } catch (Throwable e) {
                msgHandler.addFieldError(context, fieldName, e.getMessage());
                return false;
            }
            return true;
        }


        @Override
        public SuFormProperties overwriteSubPluginFormPropertyTypes(SuFormProperties subformProps) throws Exception {

            if (rewriteSubFormProperties != null) {
                return rewriteSubFormProperties;
            }

            Descriptor<SelectedTab> newSubDescriptor = getRewriterSelectTabDescriptor();
            rewriteSubFormProperties = SuFormProperties.copy(
                    filterFieldProp(buildPropertyTypes(Optional.of(newSubDescriptor), newSubDescriptor.clazz))
                    , newSubDescriptor.clazz
                    , newSubDescriptor
                    , subformProps);
            return rewriteSubFormProperties;
        }

        @Override
        public Descriptor<SelectedTab> getRewriterSelectTabDescriptor() {
            Class targetClass = com.qlangtech.tis.plugin.datax.hudi.HudiSelectedTab.class;
            return Objects.requireNonNull(TIS.get().getDescriptor(targetClass)
                    , "subForm clazz:" + targetClass + " can not find relevant Descriptor");
        }

//        @Override
//        public SuFormProperties.SuFormPropertiesBehaviorMeta overwriteBehaviorMeta(
//                SuFormProperties.SuFormPropertiesBehaviorMeta behaviorMeta) throws Exception {


//            {
//                "clickBtnLabel": "设置",
//                    "onClickFillData": {
//                "cols": {
//                    "method": "getTableMetadata",
//                    "params": ["id"]
//                }
//            }
//            }

        // Map<String, SuFormProperties.SuFormPropertyGetterMeta> onClickFillData = behaviorMeta.getOnClickFillData();

//            SuFormProperties.SuFormPropertyGetterMeta propProcess = new SuFormProperties.SuFormPropertyGetterMeta();
//            propProcess.setMethod(DataSourceMeta.METHOD_GET_PRIMARY_KEYS);
//            propProcess.setParams(Collections.singletonList("id"));
//            onClickFillData.put(HudiSelectedTab.KEY_RECORD_FIELD, propProcess);
//
//            propProcess = new SuFormProperties.SuFormPropertyGetterMeta();
//            propProcess.setMethod(DataSourceMeta.METHOD_GET_PARTITION_KEYS);
//            propProcess.setParams(Collections.singletonList("id"));
//            onClickFillData.put(HudiSelectedTab.KEY_PARTITION_PATH_FIELD, propProcess);
//            onClickFillData.put(HudiSelectedTab.KEY_SOURCE_ORDERING_FIELD, propProcess);


//            return behaviorMeta;
//        }

        @Override
        public boolean isRdbms() {
            return false;
        }

        @Override
        public IEndTypeGetter.EndType getEndType() {
            return IEndTypeGetter.EndType.Hudi;
        }

        @Override
        public String getDisplayName() {
            return DATAX_NAME;
        }
    }

    //private transient AtomicReference<DataXCfgGenerator.GenerateCfgs> generateCfgs;

    @Override
    public IRemoteTaskTrigger createPostTask(IExecChainContext execContext, ISelectedTab tab, DataXCfgGenerator.GenerateCfgs genCfg) {

//        if (generateCfgs == null) {
//            generateCfgs = new AtomicReference<>();
//        }
//                = generateCfgs.updateAndGet((pre) -> {
//            if (pre == null) {
        //  return pre;
        //}
        //  return pre;
        //});
        return new HudiDumpPostTask(execContext, (HudiSelectedTab) tab, this, genCfg);
    }

    @Override
    public IRemoteTaskTrigger createPreExecuteTask(IExecChainContext execContext, ISelectedTab tab) {

        return new IRemoteTaskTrigger() {
            @Override
            public String getTaskName() {
                return IDataXBatchPost.getPreExecuteTaskName(tab);
            }

            @Override
            public void run() {
                // 创建hud schema

                FileSystemFactory fsFactory = getFs();
                IPath dumpDir = HudiTableMeta.getDumpDir(fsFactory.getFileSystem(), tab.getName(), TimeFormat.yyyyMMddHHmmss.format(execContext.getPartitionTimestampWithMillis()), getHiveConnMeta());
                logger.info("create schema on path:{}", HudiTableMeta.createFsSourceSchema(fsFactory.getFileSystem(), tab.getName(), dumpDir, (HudiSelectedTab) tab));
            }
        };
    }

    public class HudiDataXContext extends FSDataXContext {

        private final HudiSelectedTab hudiTab;

        public HudiDataXContext(IDataxProcessor.TableMap tabMap, String dataxName) {
            super(tabMap, dataxName);
            ISelectedTab tab = tabMap.getSourceTab();
            if (!(tab instanceof HudiSelectedTab)) {
                throw new IllegalStateException(" param tabMap.getSourceTab() must be type of "
                        + HudiSelectedTab.class.getSimpleName() + " but now is :" + tab.getClass());
            }
            this.hudiTab = (HudiSelectedTab) tab;
            if (this.hudiTab == null) {
                throw new IllegalArgumentException("param hudiTab can not be null");
            }
        }

        public String getRecordField() {
            if (this.hudiTab.keyGenerator == null) {
                throw new IllegalStateException("keyGenerator can not be null");
            }
            return this.hudiTab.keyGenerator.getLiteriaRecordFields();
        }


        public String getSourceOrderingField() {
            return this.hudiTab.sourceOrderingField;
        }

        public Integer getShuffleParallelism() {
            return shuffleParallelism;
        }

        public BatchOpMode getOpMode() {
            return BatchOpMode.parse(batchOp);
        }

        public HudiWriteTabType getTabType() {
            return getHudiTableType();
        }
    }
}
