///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.offline;
//
//import com.qlangtech.tis.TIS;
//import com.qlangtech.tis.extension.Describable;
//import com.qlangtech.tis.extension.Descriptor;
//import com.qlangtech.tis.fs.IPath;
//import com.qlangtech.tis.fs.ITISFileSystem;
//import com.qlangtech.tis.fullbuild.indexbuild.IIndexBuildJobFactory;
//import com.qlangtech.tis.fullbuild.indexbuild.ITabPartition;
//import com.qlangtech.tis.fullbuild.indexbuild.IndexBuildSourcePathCreator;
//import com.qlangtech.tis.order.center.IJoinTaskContext;
//import com.qlangtech.tis.plugin.IPluginStore;
//import com.qlangtech.tis.plugin.IdentityName;
//import com.qlangtech.tis.plugin.PluginStore;
//
//import java.util.Objects;
//
///**
// * 索引build插件
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
//public abstract class IndexBuilderTriggerFactory implements Describable<IndexBuilderTriggerFactory>, IIndexBuildJobFactory, IdentityName {
//
//    public static IndexBuilderTriggerFactory get() {
//        IPluginStore<IndexBuilderTriggerFactory> pluginStore = TIS.getPluginStore(IndexBuilderTriggerFactory.class);
//        IndexBuilderTriggerFactory indexBuilderTriggerFactory = pluginStore.getPlugin();
//        Objects.requireNonNull(indexBuilderTriggerFactory, "indexBuilderTriggerFactory can not be null");
//        return indexBuilderTriggerFactory;
//    }
//
//
//
//    public abstract IndexBuildSourcePathCreator createIndexBuildSourcePathCreator(IJoinTaskContext execContext, ITabPartition ps);
//
//    /**
//     * 全量构建使用的文件系统
//     *
//     * @return
//     */
//    public abstract ITISFileSystem getFileSystem();
//
//
//    @Override
//    public Descriptor<IndexBuilderTriggerFactory> getDescriptor() {
//        return TIS.get().getDescriptor(this.getClass());
//    }
//}
