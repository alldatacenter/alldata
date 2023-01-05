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

package com.qlangtech.tis.indexbuild;

import com.google.common.collect.Maps;
import com.qlangtech.tis.build.task.TaskMapper;
import com.qlangtech.tis.build.yarn.IndexBuildNodeMaster;
import com.qlangtech.tis.dump.LocalTableDumpFactory;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fs.local.LocalFileSystem;
import com.qlangtech.tis.fullbuild.indexbuild.*;
import com.qlangtech.tis.manage.common.ConfigFileReader;
import com.qlangtech.tis.manage.common.IndexBuildParam;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.offline.IndexBuilderTriggerFactory;
import com.qlangtech.tis.order.center.IJoinTaskContext;
import com.qlangtech.tis.order.center.IParamContext;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.trigger.jst.ImportDataProcessInfo;

import java.io.File;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Objects;

/**
 * 在Assemble节点内执行全量索引构建
 *
 * @author: baisui 百岁
 * @create: 2021-03-04 10:25
 **/
public class LocalIndexBuilderTriggerFactory extends IndexBuilderTriggerFactory {

    @FormField(identity = true, ordinal = 0, validate = {Validator.require, Validator.identity})
    public String name;

//    @FormField(ordinal = 1, validate = {Validator.require})
//    public String rootDir;

    private transient ITISFileSystem fileSystem;

    @Override
    public IndexBuildSourcePathCreator createIndexBuildSourcePathCreator(IJoinTaskContext ctx, ITabPartition ps) {

        return (group) -> {
            // 需要构建倒排索引的表名称
            EntityName targetTableName = ctx.getAttribute(IParamContext.KEY_BUILD_TARGET_TABLE_NAME);
            Objects.requireNonNull(targetTableName, "targetTableName can not be null");
            int groupIndex = Integer.parseInt(group);
            if (groupIndex > 0) {
                throw new IllegalArgumentException("single table dump is not support multi slice index Build");
            }
            File sourDir = new File(LocalTableDumpFactory.getLocalOfflineRootDir()
                    , targetTableName.getDbName() + "/" + targetTableName.getTabName() + "/all/" + ps.getPt());
            return sourDir.getAbsolutePath();
        };

    }
    /**
     * 执行索引触发任务
     *
     * @param timePoint
     * @param indexName
     * @param groupNum
     * @param buildParam
     * @return
     * @throws Exception
     */
    @Override
    public IRemoteJobTrigger createBuildJob(IJoinTaskContext execContext, String timePoint, String indexName, String groupNum, IIndexBuildParam buildParam) throws Exception {

        String coreName = buildParam.getCoreName(Integer.parseInt(groupNum));
        Map<String, String> params = Maps.newHashMap();
        params.put(IndexBuildParam.INDEXING_BUILD_TABLE_TITLE_ITEMS, buildParam.getBuildTableTitleItems());
        params.put(IndexBuildParam.JOB_TYPE, IndexBuildParam.JOB_TYPE_INDEX_BUILD);
        params.put(IndexBuildParam.INDEXING_MAX_DOC_FAILD_LIMIT, "100");

        ITISFileSystem fs = this.getFileSystem();
        params.put(IndexBuildParam.INDEXING_SOLRCONFIG_PATH, ConfigFileReader.FILE_SOLR.getFsPath(fs, coreName));
        params.put(IndexBuildParam.INDEXING_SCHEMA_PATH, ConfigFileReader.FILE_SCHEMA.getFsPath(fs, coreName));

        params.put(IndexBuildParam.INDEXING_MAX_NUM_SEGMENTS, "1");
        params.put(IndexBuildParam.INDEXING_GROUP_NUM, "0");

        params.put(IndexBuildParam.INDEXING_SERVICE_NAME, indexName);
        //params.put(IndexBuildParam.INDEXING_BUILDER_TRIGGER_FACTORY, "yarn-index-build");
        params.put(IndexBuildParam.INDEXING_INCR_TIME, timePoint);
//        IndexBuildSourcePathCreator pathCreator = buildParam.getHdfsSourcePath();
//        Objects.requireNonNull(pathCreator, "pathCreator can not be null");

        // File dumpRoot = LocalTableDumpFactory.getLocalOfflineRootDir();
//        File tableRoot = new File(dumpRoot, DB_EMPLOYEES + "/" + TABLE_EMPLOYEES + "/all");

        // IJoinTaskContext execContext, String group, ITabPartition ps
        IndexBuildSourcePathCreator indexBuildSourcePathCreator = this.createIndexBuildSourcePathCreator(execContext, () -> timePoint);
        String sourcePath = URLEncoder.encode(indexBuildSourcePathCreator.build(groupNum), TisUTF8.getName());

        params.put(IndexBuildParam.INDEXING_SOURCE_PATH, sourcePath);

        // "/user/admin/search4totalpay/all/0/output/20200525134425"
        params.put(IndexBuildParam.INDEXING_OUTPUT_PATH
                , ImportDataProcessInfo.createIndexDir(this.getFileSystem(), timePoint, groupNum, indexName, false));
        params.put(IndexBuildParam.INDEXING_CORE_NAME, coreName);
        params.put(IParamContext.KEY_TASK_ID, String.valueOf(buildParam.getTaskId()));
        params.put(IndexBuildParam.INDEXING_ROW_COUNT, "999");

        TaskContext context = TaskContext.create(params);
        context.setCoordinator(buildParam.getCoordinator());

        return LocalTableDumpFactory.triggerTask(context, (rpc) -> {
            IndexBuilderTriggerFactory buildTrigger = LocalIndexBuilderTriggerFactory.this;
            IndexBuildNodeMaster.executeIndexBuild(context, buildTrigger, rpc, false);
        });
    }

    @Override
    public void startTask(TaskMapper taskMapper, TaskContext taskContext) throws Exception {
        // 远端任务容器内执行禁止
        throw new UnsupportedOperationException();
    }


    @Override
    public ITISFileSystem getFileSystem() {
        if (fileSystem == null) {
            fileSystem = new LocalFileSystem(LocalTableDumpFactory.getLocalOfflineRootDir().getAbsolutePath());
        }
        return fileSystem;
    }


    @TISExtension()
    public static class DefaultDescriptor extends Descriptor<IndexBuilderTriggerFactory> {
        @Override
        public String getDisplayName() {
            return "localIndexBuild";
        }

//        public boolean validateRootDir(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
//            File rootDir = new File(value);
//            if (!rootDir.exists()) {
//                msgHandler.addFieldError(context, fieldName, "path:" + rootDir.getAbsolutePath() + " is not exist");
//                return false;
//            }
//            return true;
//        }

    }
}
