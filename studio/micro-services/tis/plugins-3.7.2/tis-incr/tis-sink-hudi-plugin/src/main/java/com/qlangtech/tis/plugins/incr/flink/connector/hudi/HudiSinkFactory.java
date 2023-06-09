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

package com.qlangtech.tis.plugins.incr.flink.connector.hudi;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.datax.common.util.Configuration;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
import com.qlangtech.tis.compiler.streamcode.CompileAndPackage;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.datax.IStreamTableMeataCreator;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.hudi.HudiSelectedTab;
import com.qlangtech.tis.plugin.datax.hudi.HudiTableMeta;
import com.qlangtech.tis.plugin.datax.hudi.IDataXHudiWriter;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import com.qlangtech.tis.plugins.incr.flink.connector.hudi.compaction.CompactionConfig;
import com.qlangtech.tis.plugins.incr.flink.connector.scripttype.IStreamScriptType;
import com.qlangtech.tis.plugins.incr.flink.connector.streamscript.BasicFlinkStreamScriptCreator;
import com.qlangtech.tis.realtime.BasicTISSinkFactory;
import com.qlangtech.tis.realtime.TabSinkFunc;
import com.qlangtech.tis.realtime.transfer.DTO;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.annotation.Public;
import org.apache.hudi.common.fs.IExtraHadoopFileSystemGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-14 14:39
 **/
@Public
public class HudiSinkFactory extends BasicTISSinkFactory<DTO> implements IStreamTableMeataCreator.ISinkStreamMetaCreator, IStreamIncrGenerateStrategy {
    public static final String DISPLAY_NAME_FLINK_CDC_SINK = "Flink-Hudi-Sink";
    public static final String HIVE_SYNC_MODE = "hms";

    private static final Logger logger = LoggerFactory.getLogger(HudiSinkFactory.class);

    @FormField(ordinal = 3, validate = {Validator.require})
    public IStreamScriptType scriptType;

    @FormField(ordinal = 4, type = FormFieldType.ENUM, validate = {Validator.require})
    public Boolean baseOnBach;

    @FormField(ordinal = 5, type = FormFieldType.ENUM, validate = {Validator.require})
    public String opType;


    @FormField(ordinal = 6, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer})
    public Integer currentLimit;

    @FormField(ordinal = 7, validate = {Validator.require})
    public CompactionConfig compaction;

    private transient BasicFlinkStreamScriptCreator streamTableCreator;

    private BasicFlinkStreamScriptCreator getStreamTableCreator() {
        if (streamTableCreator != null) {
            return streamTableCreator;
        }
        return streamTableCreator = this.scriptType.createStreamTableCreator(this);
    }

    public static IDataXHudiWriter getDataXHudiWriter(HudiSinkFactory sink) {
        return (IDataXHudiWriter) DataxWriter.load(null, sink.dataXName);
    }


    @Override
    public Map<TableAlias, TabSinkFunc<DTO>> createSinkFunction(IDataxProcessor dataxProcessor) {
        IDataXHudiWriter hudiWriter = getDataXHudiWriter(this);

        if (!IExtraHadoopFileSystemGetter.HUDI_FILESYSTEM_NAME.equals(hudiWriter.getFsName())) {
            throw new IllegalStateException("fsName of hudiWriter must be equal to '"
                    + IExtraHadoopFileSystemGetter.HUDI_FILESYSTEM_NAME + "', but now is " + hudiWriter.getFsName());
        }

        return Collections.emptyMap();
    }

    public String getDataXName() {
        if (StringUtils.isEmpty(this.dataXName)) {
            throw new IllegalStateException("prop dataXName can not be null");
        }
        return this.dataXName;
    }

    private transient Map<String, Pair<HudiSelectedTab, HudiTableMeta>> tableMetas = null;


    public Pair<HudiSelectedTab, HudiTableMeta> getTableMeta(String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            throw new IllegalArgumentException("param tableName can not empty");
        }
        if (tableMetas == null) {
            if (StringUtils.isEmpty(this.dataXName)) {
                throw new IllegalStateException("prop dataXName can not be null");
            }
            tableMetas = Maps.newHashMap();
            IDataxProcessor dataXProcessor = DataxProcessor.load(null, this.dataXName);

            IDataxReader reader = dataXProcessor.getReader(null);
            Map<String, HudiSelectedTab> selTabs
                    = reader.getSelectedTabs().stream()
                    .map((tab) -> (HudiSelectedTab) tab).collect(Collectors.toMap((tab) -> tab.getName(), (tab) -> tab));

            DataXCfgGenerator.GenerateCfgs dataxCfgFile = dataXProcessor.getDataxCfgFileNames(null);
            Configuration cfg = null;
            Configuration paramCfg = null;
            String table = null;
            HudiTableMeta tableMeta = null;
            for (DataXCfgGenerator.DataXCfgFile f : dataxCfgFile.getDataXCfgFiles()) {
                File file = f.getFile();
                cfg = Configuration.from(file);
                paramCfg = cfg.getConfiguration("job.content[0].writer.parameter");
                if (paramCfg == null) {
                    throw new NullPointerException("paramCfg can not be null,relevant path:" + file.getAbsolutePath());
                }
                table = paramCfg.getString("fileName");
                if (StringUtils.isEmpty(table)) {
                    throw new IllegalStateException("table can not be null:" + paramCfg.toJSON());
                }

                tableMetas.put(table, Pair.of(Objects.requireNonNull(selTabs.get(table), "tab:" + table + " relevant 'HudiSelectedTab' can not be null")
                        , new HudiTableMeta(paramCfg)));
            }
        }

        final Pair<HudiSelectedTab, HudiTableMeta> tabMeta = tableMetas.get(tableName);
        if (tabMeta == null || tabMeta.getRight().isColsEmpty()) {
            throw new IllegalStateException("table:" + tableName
                    + " relevant colMetas can not be null,exist tables:"
                    + tableMetas.keySet().stream().collect(Collectors.joining(",")));
        }
        return tabMeta;
    }

    /**
     * ------------------------------------------------------------------------------
     * start implements IStreamTableCreator
     * ------------------------------------------------------------------------------
     */
    @Override
    public IStreamTableMeta getStreamTableMeta(final String tableName) {

        return new IStreamTableMeta(){
            @Override
            public  List<IColMetaGetter> getColsMeta() {
                return getTableMeta(tableName).getRight().colMetas;
            }
        };


        // return getStreamTableCreator().getStreamTableMeta(tableName);
    }

    @Override
    public IStreamTemplateResource getFlinkStreamGenerateTplResource() {
        return getStreamTableCreator().getFlinkStreamGenerateTplResource();
    }

    @Override
    public IStreamTemplateData decorateMergeData(IStreamTemplateData mergeData) {
        return getStreamTableCreator().decorateMergeData(mergeData);
    }


    @Override
    public ICompileAndPackage getCompileAndPackageManager() {
//        TisMetaProps metaProps = Config.getMetaProps();
//        return new CompileAndPackage(Lists.newArrayList(
//                new PluginWrapper.Dependency("tis-sink-hudi-plugin", metaProps.getVersion(), false)
//                , new PluginWrapper.Dependency("tis-datax-hudi-plugin", metaProps.getVersion(), false)));
//        UberClassLoader uberClassLoader = TIS.get().getPluginManager().uberClassLoader;
//        uberClassLoader.
        // com.alibaba.datax.plugin.writer.hudi;HudiConfig
        // HudiSinkFactory
        return new CompileAndPackage(Sets.newHashSet(
                //  "tis-sink-hudi-plugin"
                HudiSinkFactory.class
                // "tis-datax-hudi-plugin"
                , "com.alibaba.datax.plugin.writer.hudi.HudiConfig"
        ));
    }


    /**
     * ------------------------------------------------------------------------------
     * End implements IStreamTableCreator
     * ------------------------------------------------------------------------------
     */

    @TISExtension
    public static class DefaultSinkFunctionDescriptor extends BaseSinkFunctionDescriptor {
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME_FLINK_CDC_SINK;
        }

        @Override
        protected boolean validateAll(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            if (!IDataXHudiWriter.HUDI_FILESYSTEM_NAME.equals(IExtraHadoopFileSystemGetter.HUDI_FILESYSTEM_NAME)) {
                throw new IllegalStateException("DataXHudiWriter.HUDI_FILESYSTEM_NAME:" + IDataXHudiWriter.HUDI_FILESYSTEM_NAME
                        + ",IExtraHadoopFileSystemGetter.HUDI_FILESYSTEM_NAME:"
                        + IExtraHadoopFileSystemGetter.HUDI_FILESYSTEM_NAME + " must be equal");
            }
            return super.validateAll(msgHandler, context, postFormVals);
        }

        @Override
        public PluginVender getVender() {
            return PluginVender.TIS;
        }

        @Override
        protected IEndTypeGetter.EndType getTargetType() {
            return IEndTypeGetter.EndType.Hudi;
        }
    }
}
