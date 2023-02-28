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

package com.qlangtech.tis;

import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.PluginStore;
import com.qlangtech.tis.util.IPluginContext;
import com.qlangtech.tis.util.Selectable;
import com.qlangtech.tis.util.UploadPluginMeta;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-29 14:59
 **/
public interface IPluginEnum<T extends Describable<T>> {

    public Class<T> getExtensionPoint();

    public String getIdentity();

    public String getCaption();

    public Selectable getSelectable();

    public <T> List<T> getPlugins(IPluginContext pluginContext, UploadPluginMeta pluginMeta);

    public IPluginStore getPluginStore(IPluginContext pluginContext, UploadPluginMeta pluginMeta);

    public <T extends Describable<T>> List<Descriptor<T>> descriptors();

    public boolean isIdentityUnique();

    public boolean isAppNameAware();
}
