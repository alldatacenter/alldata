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
//import com.qlangtech.tis.exec.ExecuteResult;
//import com.qlangtech.tis.exec.IExecChainContext;
//import com.qlangtech.tis.fs.ITISFileSystem;
//import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
//import com.qlangtech.tis.fullbuild.indexbuild.ITabPartition;
//import com.qlangtech.tis.fullbuild.indexbuild.IndexBuildSourcePathCreator;
//import com.qlangtech.tis.fullbuild.servlet.BuildTriggerServlet;
//import com.qlangtech.tis.trigger.jst.ImportDataProcessInfo;
//import org.apache.commons.lang.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * 当直接使用数据中心构建好的
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2016年3月11日
// */
//public final class IndexBuildWithHdfsPathInterceptor extends IndexBuildInterceptor {
//
//    private static final String HDFS_PATH = "hdfspath";
//
//    private static final Logger logger = LoggerFactory.getLogger(IndexBuildWithHdfsPathInterceptor.class);
//
//    @Override
//    protected ExecuteResult execute(IExecChainContext execContext) throws Exception {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    protected IndexBuildSourcePathCreator createIndexBuildSourceCreator(final IExecChainContext execContext, ITabPartition ps) {
//        return new IndexBuildSourcePathCreator() {
//            @Override
//            public String build(String group) {
//                final String hdfspath = execContext.getString(HDFS_PATH);
//                ITISFileSystem fs = execContext.getIndexBuildFileSystem();
//
//                String path = hdfspath + "/" + IDumpTable.PARTITION_PMOD + "=" + group;
//                try {
//                    if (fs.exists(fs.getPath(path))) {
//                        return path;
//                    }
//                    logger.info("sourcepath not exist:" + path);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//                final String targetPath = "0".equals(group) ? hdfspath : path;
//                logger.info("source hdfs path:" + targetPath);
//                return targetPath;
//            }
//        };
//    }
//
//    @Override
//    protected void setBuildTableTitleItems(String indexName, ImportDataProcessInfo processinfo, IExecChainContext execContext) {
//        processinfo.setBuildTableTitleItems(execContext.getString(BuildTriggerServlet.KEY_COLS));
//        processinfo.setHdfsdelimiter(
//                StringUtils.defaultIfEmpty(execContext.getString(ImportDataProcessInfo.KEY_DELIMITER)
//                        , ImportDataProcessInfo.DELIMITER_001));
//    }
//}
