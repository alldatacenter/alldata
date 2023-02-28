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
//package com.qlangtech.tis.exec.impl;
//
//import com.qlangtech.tis.exec.IExecChainContext;
//import com.qlangtech.tis.fullbuild.indexbuild.ITabPartition;
//import com.qlangtech.tis.fullbuild.indexbuild.IndexBuildSourcePathCreator;
//import com.qlangtech.tis.manage.IAppSource;
//import com.qlangtech.tis.manage.ISolrAppSource;
//import com.qlangtech.tis.manage.impl.DataFlowAppSource;
//import com.qlangtech.tis.plugin.ds.ColumnMetaData;
//import com.qlangtech.tis.trigger.jst.ImportDataProcessInfo;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
//public class WorkflowIndexBuildInterceptor extends IndexBuildInterceptor {
//
//    private static final Logger logger = LoggerFactory.getLogger(WorkflowIndexBuildInterceptor.class);
//
//    public WorkflowIndexBuildInterceptor() {
//    }
//
//    @Override
//    protected IndexBuildSourcePathCreator createIndexBuildSourceCreator(IExecChainContext execContext, ITabPartition ps) {
//
//        return execContext.getIndexBuilderFactory().createIndexBuildSourcePathCreator(execContext, ps);
//
////        // 需要构建倒排索引的表名称
////        EntityName targetTableName = execContext.getAttribute(IExecChainContext.KEY_BUILD_TARGET_TABLE_NAME);
////        String fsPath = FSHistoryFileUtils.getJoinTableStorePath(execContext.getIndexBuildFileSystem().getRootDir(), targetTableName)  // execContext.getTableDumpFactory().getJoinTableStorePath(targetTableName)
////                + "/" + IDumpTable.PARTITION_PT + "=%s/" + IDumpTable.PARTITION_PMOD + "=%s";
////        logger.info("hdfs sourcepath:" + fsPath);
////        return (ctx, group, partition) -> String.format(fsPath, ps.getPt(), group);
//    }
//
//    @Override
//    protected void setBuildTableTitleItems(String indexName, ImportDataProcessInfo processinfo, IExecChainContext execContext) {
//        try {
//            ISolrAppSource appSource = execContext.getAppSource(); //DataFlowAppSource.load(indexName);
//            List<ColumnMetaData> finalNode = appSource.reflectCols();
////            SqlTaskNodeMeta.SqlDataFlowTopology topology = execContext.getTopology();
////            List<ColumnMetaData> finalNode = topology.getFinalTaskNodeCols();
//            processinfo.setBuildTableTitleItems(
//                    finalNode.stream().map((k) -> k.getKey()).collect(Collectors.joining(",")));
//        } catch (Exception e) {
//            throw new RuntimeException("workflow:" + execContext.getWorkflowName(), e);
//        }
//    }
//}
