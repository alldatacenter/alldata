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
package com.qlangtech.tis.offline;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.fs.ITISFileSystemFactory;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.IdentityName;

import java.io.File;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
@Public
public abstract class FileSystemFactory implements Describable<FileSystemFactory>, ITISFileSystemFactory, IdentityName {

    public abstract <Configuration> Configuration getConfiguration();

    /**
     * 需要向本地默认的 配置文件夹中方式hdfs-site.xml配置文件
     *
     * @param cfgDir
     */
    public abstract void setConfigFile(File cfgDir);

    public static FileSystemFactory getFsFactory(String fsName) {
        IPluginStore<FileSystemFactory> pluginStore = TIS.getPluginStore(FileSystemFactory.class);
        return pluginStore.find(fsName);
    }

    @Override
    public Descriptor<FileSystemFactory> getDescriptor() {
        return TIS.get().getDescriptor(this.getClass());
    }
}
