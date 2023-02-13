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
package com.qlangtech.tis.trigger.jst;

import com.qlangtech.tis.cloud.dump.DumpJobId;
import com.qlangtech.tis.cloud.dump.DumpJobStatus;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fs.ITISFileSystemFactory;
import com.qlangtech.tis.manage.common.ConfigFileReader;
import com.qlangtech.tis.manage.common.SnapshotDomain;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import com.qlangtech.tis.trigger.jst.AbstractIndexBuildJob.BuildResult;
import org.apache.commons.lang.StringUtils;
//import org.apache.solr.common.cloud.Replica;
//import org.apache.solr.common.cloud.ZkStateReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年6月18日
 */
public abstract class AbstractIndexBuildJob implements Callable<BuildResult> {

    public static final String SCHEMA = "schema";

    private static final Logger logger = LoggerFactory.getLogger(AbstractIndexBuildJob.class);

    protected final int groupNum;

    private final ITISFileSystem indexBuildFS;

    private final SnapshotDomain appDomain;

    static int jobid = 0;

    public AbstractIndexBuildJob(IExecChainContext execContext, ImportDataProcessInfo processInfo, int group, SnapshotDomain domain) {
        this.state = processInfo;
        if (StringUtils.isEmpty(processInfo.getTimepoint())) {
            throw new IllegalArgumentException("processInfo.getTimepoint() can not be null");
        }
        this.groupNum = (group);
        this.indexBuildFS = execContext.getIndexBuildFileSystem();
        this.appDomain = domain;
    }

    protected final ImportDataProcessInfo state;

    public BuildResult call() throws Exception {
        try {
            return startBuildIndex();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行单组build任務
     *
     * @return
     * @throws Exception
     */
    @SuppressWarnings("all")
    public final BuildResult startBuildIndex() throws Exception {
        // final String coreName = state.getIndexName() + '-' + groupNum;
        // + '-' + groupNum;
        final String coreName = state.getCoreName(groupNum);
        final String timePoint = state.getTimepoint();
        final DumpJobStatus status = new DumpJobStatus();
        // status.setUserName(userName);
        status.setTimepoint(state.getTimepoint());
        status.setDumpType("remote");
        DumpJobId dumpJobId = new DumpJobId("jtIdentifier", jobid++);
        status.setDumpJobID(dumpJobId);
        status.setCoreName(coreName);
        RunEnvironment runtime = RunEnvironment.getSysRuntime();
        long now = System.currentTimeMillis();
        final String outPath = state.getIndexBuildOutputPath((this.groupNum));
        logger.info("build out path:" + outPath);
        ITISFileSystem fileSystem = indexBuildFS;

        appDomain.writeResource2fs(fileSystem, coreName, ConfigFileReader.FILE_SCHEMA);
        appDomain.writeResource2fs(fileSystem, coreName, ConfigFileReader.FILE_SOLR);
        // writeResource2Hdfs(coreName, domain, ConfigFileReader.FILE_CORE_PROPERTIES, "config");
        // // TODO 为了兼容老的索引先加上，到时候要删除掉的
        // writeResource2Hdfs(coreName, domain, ConfigFileReader.FILE_SCHEMA, SCHEMA);
        // writeResource2Hdfs(coreName, domain, ConfigFileReader.FILE_APPLICATION, "app");
        // writeResource2Hdfs(coreName, domain, ConfigFileReader.FILE_CORE_PROPERTIES, "core");
        // TODO 为了兼容老的索引先加上，到时候要删除掉的 end
        logger.info("Excute  RemoteDumpJob: Sbumit Remote Job .....  ");
        status.setStartTime(now);
        // String[] core = this.coreName.split("-");
        String serviceName = state.getIndexName();
        // ///////////////////////////////////////////
        logger.info("Excute Remote Dump Job Status: Sbumit  ");
        return buildSliceIndex(coreName, timePoint, status, outPath, serviceName);
    }

    protected abstract BuildResult buildSliceIndex(final String coreName, final String timePoint, final DumpJobStatus status, final String outPath, String serviceName) throws Exception, IOException, InterruptedException;

    public static class BuildResult {

        public static BuildResult createFaild() {
            BuildResult buildResult = new BuildResult(Integer.MAX_VALUE, new ImportDataProcessInfo(0, null, null));
            return buildResult.setSuccess(false);
        }

        public static BuildResult clone(BuildResult from) {
            BuildResult buildResult = new BuildResult(from.groupIndex, from.processInfo);
            buildResult.setSuccess(true).setIndexSize(from.indexSize);
            return buildResult;
        }

//        private Replica replica;
//
//        public Replica getReplica() {
//            return replica;
//        }
//
//        public final String getNodeName() {
//            return this.replica.getNodeName();
//        }
//
//        public BuildResult setReplica(Replica replica) {
//            this.replica = replica;
//            return this;
//        }

        // private final RunningJob rj;
        private boolean success;

        private final ImportDataProcessInfo processInfo;

        public String getTimepoint() {
            return this.processInfo.getTimepoint();
        }

        public boolean isSuccess() {
            return success;
        }

        public BuildResult setSuccess(boolean success) {
            this.success = success;
            return this;
        }

        private final int groupIndex;

        // 索引磁盘容量
        private long indexSize;

        public long getIndexSize() {
            return indexSize;
        }

        public void setIndexSize(long indexSize) {
            this.indexSize = indexSize;
        }

        public int getGroupIndex() {
            return groupIndex;
        }

        public static final Pattern PATTERN_CORE = Pattern.compile("search4(.+?)_shard(\\d+?)_replica(\\d+?)");

//        public BuildResult(Replica replica, ImportDataProcessInfo processInfo) {
//            super();
//            String coreName = replica.getStr(ZkStateReader.CORE_NAME_PROP);
//            Matcher matcher = PATTERN_CORE.matcher(coreName);
//            if (!matcher.matches()) {
//                throw new IllegalStateException("coreName:" + coreName + " is not match the pattern:" + PATTERN_CORE);
//            }
//            this.groupIndex = Integer.parseInt(matcher.group(2)) - 1;
//            this.processInfo = processInfo;
//        }

        public BuildResult(int group, ImportDataProcessInfo processInfo) {
            super();
            this.groupIndex = group;
            this.processInfo = processInfo;
        }

//        public String getHdfsSourcePath(IExecChainContext ctx) {
//            return this.processInfo.getHdfsSourcePath().build(ctx,String.valueOf(groupIndex));
//        }
    }

}
