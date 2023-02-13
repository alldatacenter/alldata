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
package com.qlangtech.tis.component;


/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class GlobalComponent {

    // 在页面上显示扩展点详细信息吗？显示则可以将页面当作一个产品文档很直观地看到每个扩展点的内容
    private boolean showExtensionDetail;

    public boolean isShowExtensionDetail() {
        return showExtensionDetail;
    }

    public GlobalComponent setShowExtensionDetail(boolean showExtensionDetail) {
        this.showExtensionDetail = showExtensionDetail;
        return this;
    }

    // private List<FileSystemFactory> fsFactories;
//    private List<TableDumpFactory> dsDumpFactories;
//
//    private List<IndexBuilderTriggerFactory> indexBuilderFactories;

    // private List<FlatTableBuilder> flatTableBuilders;
    // private IncrK8sConfig incrK8sConfig;
    // public IncrK8sConfig getIncrK8sConfig() {
    // return this.incrK8sConfig;
    // }
    //
    // public void setIncrK8sConfig(IncrK8sConfig incrK8sConfig) {
    // this.incrK8sConfig = incrK8sConfig;
    // }
    // public List<FlatTableBuilder> getFlatTableBuilders() {
    // if (flatTableBuilders == null) {
    // return Collections.emptyList();
    // }
    // return flatTableBuilders;
    // }
    //
    // public void setFlatTableBuilders(List<FlatTableBuilder> flatTableBuilders) {
    // this.flatTableBuilders = flatTableBuilders;
    // }
    // public List<FileSystemFactory> getFsFactories() {
    // if (this.fsFactories == null) {
    // return Collections.emptyList();
    // }
    // return this.fsFactories;
    // }
    //
    // public void setFsFactories(List<FileSystemFactory> fsFactories) {
    // this.fsFactories = fsFactories;
    // }
//    public List<TableDumpFactory> getDsDumpFactories() {
//        return this.dsDumpFactories;
//    }
//
//    public void setDsDumpFactories(List<TableDumpFactory> dsDumpFactories) {
//        this.dsDumpFactories = dsDumpFactories;
//    }
//
//    public List<IndexBuilderTriggerFactory> getIndexBuilderFactories() {
//        return indexBuilderFactories;
//    }
//
//    public void setIndexBuilderFactories(List<IndexBuilderTriggerFactory> indexBuilderFactories) {
//        this.indexBuilderFactories = indexBuilderFactories;
//    }
}
