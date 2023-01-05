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
//import com.qlangtech.tis.build.task.TaskMapper;
//import com.qlangtech.tis.extension.Describable;
//import com.qlangtech.tis.extension.Descriptor;
//import com.qlangtech.tis.fs.IFs2Table;
//import com.qlangtech.tis.fullbuild.indexbuild.*;
//import com.qlangtech.tis.fullbuild.taskflow.ITableBuildTaskContext;
//import com.qlangtech.tis.plugin.IdentityName;
//
///**
// * 导入表的方式，可以使用本地，k8s容器，YARN容器等方式
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
//public abstract class TableDumpFactory implements Describable<TableDumpFactory>, ITableDumpJobFactory
//        , IFs2Table, ITableBuildTaskContext, IdentityName {
//
//    public static final ITableDumpJobFactory NO_OP = new ITableDumpJobFactory() {
//
//        @Override
//        public void startTask(TaskMapper taskMapper, TaskContext taskContext) {
//        }
//
//        @Override
//        public IRemoteJobTrigger createSingleTableDumpJob(IDumpTable table, TaskContext context) {
//            return new IRemoteJobTrigger() {
//
//                @Override
//                public void submitJob() {
//                }
//
//                @Override
//                public RunningStatus getRunningStatus() {
//                    return new RunningStatus(1f, true, true);
//                }
//            };
//        }
//    };
//
//    @Override
//    public Descriptor<TableDumpFactory> getDescriptor() {
//        return TIS.get().getDescriptor(this.getClass());
//    }
//}
