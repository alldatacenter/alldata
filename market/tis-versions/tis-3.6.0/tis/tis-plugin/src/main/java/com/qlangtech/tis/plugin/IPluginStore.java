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

package com.qlangtech.tis.plugin;

import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-12-07 18:28
 **/
public interface IPluginStore<T extends Describable> extends IRepositoryResource, IPluginStoreSave<T> {

    public T getPlugin();

    public List<T> getPlugins();

    public void cleanPlugins();

    public List<Descriptor<T>> allDescriptor();


    public default T find(String name) {
        return find(name, true);
    }

    public T find(String name, boolean throwNotFoundErr);

    interface Recyclable {
        // 是否已经是脏数据了，已经在PluginStore中被替换了
        boolean isDirty();
    }

    interface RecyclableController extends Recyclable {
        /**
         * 标记已经失效
         */
        void signDirty();
    }
}
