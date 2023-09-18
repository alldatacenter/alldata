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

package com.qlangtech.tis.util;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-11 08:03
 **/
public abstract class AdapterPluginContext implements IPluginContext {
    private final IPluginContext pluginContext;

    public AdapterPluginContext(IPluginContext pluginContext) {
        this.pluginContext = pluginContext;
    }

    @Override
    public String getExecId() {
        return pluginContext.getExecId();
    }

    @Override
    public boolean isCollectionAware() {
        return pluginContext.isCollectionAware();
    }

    @Override
    public boolean isDataSourceAware() {
        return pluginContext.isDataSourceAware();
    }

    @Override
    public void addDb(Descriptor.ParseDescribable<DataSourceFactory> dbDesc, String dbName, Context context, boolean shallUpdateDB) {
        pluginContext.addDb(dbDesc, dbName, context, shallUpdateDB);
    }

    @Override
    public String getCollectionName() {
        return pluginContext.getCollectionName();
    }

    @Override
    public void errorsPageShow(Context context) {
        pluginContext.errorsPageShow(context);
    }

    @Override
    public void addActionMessage(Context context, String msg) {
        pluginContext.addActionMessage(context, msg);
    }

    @Override
    public void setBizResult(Context context, Object result, boolean overwriteable) {
        pluginContext.setBizResult(context, result, overwriteable);
    }

    @Override
    public void addErrorMessage(Context context, String msg) {
        pluginContext.addErrorMessage(context, msg);
    }

    @Override
    public String getRequestHeader(String key) {
        return pluginContext.getRequestHeader(key);
    }
}
