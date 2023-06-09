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
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.datax.IDataxGlobalCfg;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.manage.biz.dal.pojo.AppType;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.plugin.StoreResourceType;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
import com.qlangtech.tis.util.UploadPluginMeta;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * @author: baisui 百岁
 * @create: 2021-04-21 09:09
 **/
public class DefaultDataxProcessor extends DataxProcessor {

    public static final String KEY_FIELD_NAME = "globalCfg";

    @FormField(identity = true, ordinal = 0, validate = {Validator.require, Validator.identity})
    public String name;

    @FormField(ordinal = 1, type = FormFieldType.SELECTABLE, validate = {Validator.require})
    public String globalCfg;

    @FormField(ordinal = 2, type = FormFieldType.ENUM, validate = {Validator.require})
    public String dptId;
    @FormField(ordinal = 3, validate = {Validator.require})
    public String recept;

    @Override
    public StoreResourceType getResType() {
        return StoreResourceType.DataApp;
    }

    @Override
    public Application buildApp() {
        Application app = new Application();
        app.setProjectName(this.name);
        app.setDptId(Integer.parseInt(this.dptId));
        app.setRecept(this.recept);
        app.setAppType(AppType.DataXPipe.getType());
        return app;
    }

    @Override
    public String identityValue() {
        return this.name;
    }

    public IDataxGlobalCfg getDataXGlobalCfg() {
        IDataxGlobalCfg globalCfg = ParamsConfig.getItem(this.globalCfg, IDataxGlobalCfg.KEY_DISPLAY_NAME);
        Objects.requireNonNull(globalCfg, "dataX Global config can not be null");
        return globalCfg;
    }

    @Override
    public IStreamTemplateResource getFlinkStreamGenerateTplResource() {

        return writerPluginOverwrite((d) -> d.getFlinkStreamGenerateTplResource()
                , () -> DefaultDataxProcessor.super.getFlinkStreamGenerateTplResource());

//        TISSinkFactory sinKFactory = TISSinkFactory.getIncrSinKFactory(this.identityValue());
//        Objects.requireNonNull(sinKFactory, "writer plugin can not be null");
//        if (sinKFactory instanceof IStreamIncrGenerateStrategy) {
//            return ((IStreamIncrGenerateStrategy) sinKFactory).getFlinkStreamGenerateTemplateFileName();
//        }
//
//        return super.getFlinkStreamGenerateTemplateFileName();
    }

    @Override
    public IStreamTemplateData decorateMergeData(IStreamTemplateData mergeData) {
        return writerPluginOverwrite((d) -> d.decorateMergeData(mergeData), () -> mergeData);
    }

    private <T> T writerPluginOverwrite(Function<IStreamIncrGenerateStrategy, T> func, Callable<T> unmatchCreator) {
        try {
            TISSinkFactory sinKFactory = TISSinkFactory.getIncrSinKFactory(this.identityValue());
            Objects.requireNonNull(sinKFactory, "writer plugin can not be null");
            if (sinKFactory instanceof IStreamIncrGenerateStrategy) {
                return func.apply(((IStreamIncrGenerateStrategy) sinKFactory));
            }
            return unmatchCreator.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @TISExtension()
    public static class DescriptorImpl extends Descriptor<IAppSource> {

        public DescriptorImpl() {
            super();
            this.registerSelectOptions(KEY_FIELD_NAME, () -> ParamsConfig.getItems(IDataxGlobalCfg.KEY_DISPLAY_NAME));
        }

        public boolean validateName(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            UploadPluginMeta pluginMeta = (UploadPluginMeta) context.get(UploadPluginMeta.KEY_PLUGIN_META);
            Objects.requireNonNull(pluginMeta, "pluginMeta can not be null");
            if (pluginMeta.isUpdate()) {
                return true;
            }
            return msgHandler.validateBizLogic(IFieldErrorHandler.BizLogic.APP_NAME_DUPLICATE, context, fieldName, value);
        }

        @Override
        public String getDisplayName() {
            return DataxProcessor.DEFAULT_DATAX_PROCESSOR_NAME;
        }
    }


}
