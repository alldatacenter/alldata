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
package com.qlangtech.tis.hdfs.impl;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.fs.IPathInfo;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.offline.FileSystemFactory;
import com.qlangtech.tis.plugin.BaiscPluginTest;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.PluginStore;

import java.util.List;

/*
 * @create: 2020-04-11 19:11
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestHdfsFileSystemFactory extends BaiscPluginTest {

    public void testCreate() {
        IPluginStore<FileSystemFactory> pluginStore = TIS.getPluginStore(FileSystemFactory.class);
        assertNotNull(pluginStore);
        Describable<FileSystemFactory> plugin = pluginStore.getPlugin();
        assertNotNull(plugin);

        FileSystemFactory fsFactory = (FileSystemFactory) plugin;

        ITISFileSystem fileSystem = fsFactory.getFileSystem();

        List<IPathInfo> paths = fileSystem.listChildren(fileSystem.getPath("/"));
        for (IPathInfo i : paths) {
            System.out.println(i.getName());
        }

//        plugin.
//
//        assertTrue("real class:" + plugin.getClass().getName(), plugin instanceof HdfsFileSystemFactory);
//        HdfsFileSystemFactory fsFactory = (HdfsFileSystemFactory) plugin;
//        ITISFileSystem fileSystem = fsFactory.getFileSystem();
//        List<IPathInfo> paths = fileSystem.listChildren(fileSystem.getPath(fsFactory.getRootDir() + "/"));
//        for (IPathInfo i : paths) {
//            System.out.println(i.getName());
//        }
    }
}
