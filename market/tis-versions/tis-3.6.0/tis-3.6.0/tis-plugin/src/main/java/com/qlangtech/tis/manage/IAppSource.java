/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.util.IPluginContext;

import java.util.Collections;
import java.util.Optional;

/**
 * 索引实例Srouce， 支持单表、dataflow
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-03-31 11:16
 */
@Public
public interface IAppSource extends Describable<IAppSource> {


    static <T extends IAppSource> KeyedPluginStore<T> getPluginStore(IPluginContext context, String appName) {
        // return (KeyedPluginStore<T>) new KeyedPluginStore(new DataxReader.AppKey(context, false, appName, IAppSource.class));
        return (KeyedPluginStore<T>) TIS.appSourcePluginStore.get(createAppSourceKey(context, appName));
    }

    static KeyedPluginStore.AppKey createAppSourceKey(IPluginContext context, String appName) {
        return new KeyedPluginStore.AppKey(context, false, appName, IAppSource.class);
    }

    static void cleanPluginStoreCache(IPluginContext context, String appName) {
        TIS.appSourcePluginStore.clear(createAppSourceKey(context, appName));
    }

    static void cleanAppSourcePluginStoreCache(IPluginContext context, String appName) {
        IAppSource.cleanPluginStoreCache(context, appName);
        DataxReader.cleanPluginStoreCache(context, false, appName);
        DataxWriter.cleanPluginStoreCache(context, appName);
    }

    static <T extends IAppSource> Optional<T> loadNullable(IPluginContext context, String appName) {
        KeyedPluginStore<T> pluginStore = getPluginStore(context, appName);
        IAppSource appSource = pluginStore.getPlugin();
        return (Optional<T>) Optional.ofNullable(appSource);
    }

    static <T extends IAppSource> T load(String appName) {
        return load(null, appName);
    }

    /**
     * save
     *
     * @param appname
     * @param appSource
     */
    static void save(IPluginContext pluginContext, String appname, IAppSource appSource) {
        KeyedPluginStore<IAppSource> pluginStore = getPluginStore(pluginContext, appname);
        Optional<Context> context = Optional.empty();
        pluginStore.setPlugins(pluginContext, context, Collections.singletonList(new Descriptor.ParseDescribable(appSource)));
    }


    /**
     * load
     *
     * @param appName
     * @return
     */
    static <T extends IAppSource> T load(IPluginContext pluginContext, String appName) {
        Optional<IAppSource> iAppSource = loadNullable(pluginContext, appName);
        if (!iAppSource.isPresent()) {
            throw new IllegalStateException("appName:" + appName + " relevant appSource can not be null");
        }
        return (T) iAppSource.get();
    }

    default Descriptor<IAppSource> getDescriptor() {
        return TIS.get().getDescriptor(this.getClass());
    }
}

