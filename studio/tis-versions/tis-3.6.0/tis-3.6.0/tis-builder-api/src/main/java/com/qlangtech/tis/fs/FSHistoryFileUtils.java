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
package com.qlangtech.tis.fs;

import com.qlangtech.tis.dump.INameWithPathGetter;
import com.qlangtech.tis.order.dump.task.ITableDumpConstant;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

/**
 * 文件系统历史文件删除
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-03-04 09:04
 */
public class FSHistoryFileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FSHistoryFileUtils.class);

    /**
     * 删除历史文件
     */
    public static void deleteHistoryFile(ITISFileSystem fileSystem, EntityName dumpTable) throws Exception {
        deleteMetadata(fileSystem, dumpTable);
        deleteHdfsFile(fileSystem, dumpTable, false);
        // 索引数据: /user/admin/search4totalpay/all/0/output/20160104003306
        deleteHdfsFile(fileSystem, dumpTable, true);
    }

    /**
     * 删除索引构建历史文件
     *
     * @param fileSystem
     * @param indexName
     */
    public static void removeHistoryBuildFile(ITISFileSystem fileSystem, String indexName) {
        try {
            deleteHdfsFile(fileSystem, () -> indexName, true);
        } catch (IOException e) {
            throw new RuntimeException("delete index:" + indexName + " history build file", e);
        }
    }

    public static void deleteHistoryFile(ITISFileSystem fileSystem, EntityName dumpTable, String timestamp) throws Exception {
        deleteMetadata(fileSystem, dumpTable, timestamp);
        deleteHdfsFile(fileSystem, dumpTable, false, timestamp);
        // 索引数据: /user/admin/search4totalpay/all/0/output/20160104003306
        deleteHdfsFile(fileSystem, dumpTable, true, timestamp);
    }

    private static void deleteHdfsFile(ITISFileSystem fileSystem, EntityName dumpTable, boolean isBuildFile, String timestamp) throws IOException {

        deleteHdfsFile(fileSystem,
                dumpTable, //
                isBuildFile, //
                (r) -> StringUtils.equals(r.getName(), timestamp), // MAX_PARTITION_SAVE
                0);
    }

    private static void deleteHdfsFile(ITISFileSystem fileSystem, INameWithPathGetter entityName, boolean isBuildFile) throws IOException {
        deleteHdfsFile(fileSystem, entityName, isBuildFile, (r) -> true, ITableDumpConstant.MAX_PARTITION_SAVE);
    }

    /**
     * 删除dump的metadata<br>
     * example:/user/admin/search4customerregistercard/all/20160106131304
     * example:/user/admin/scmdb/supply_goods/all/20160106131304
     */
    private static void deleteMetadata(ITISFileSystem fileSystem, EntityName dumpTable) throws Exception {
        deleteMetadata(fileSystem, dumpTable, (r) -> {
            return true;
        }, ITableDumpConstant.MAX_PARTITION_SAVE);
    }

    private static void deleteMetadata(ITISFileSystem fileSystem, EntityName dumpTable, String timestamp) throws Exception {
        if (StringUtils.isEmpty(timestamp)) {
            throw new IllegalArgumentException("param timestamp can not be null");
        }
        deleteMetadata(fileSystem, dumpTable, (r) -> {
                    return StringUtils.equals(r.getName(), timestamp);
                }, // MAX_PARTITION_SAVE
                0);
    }

    /**
     * 删除dump的metadata<br>
     * example:/user/admin/search4customerregistercard/all/20160106131304
     * example:/user/admin/scmdb/supply_goods/all/20160106131304
     */
    private static void deleteMetadata(ITISFileSystem fileSystem, EntityName dumpTable
            , ITISFileSystem.IPathFilter pathFilter, int maxPartitionSave) throws Exception {
        String hdfsPath = getJoinTableStorePath(fileSystem.getRootDir(), dumpTable) + "/all";
        logger.info("hdfsPath:{}", hdfsPath);
        ITISFileSystem fileSys = fileSystem;
        IPath parent = fileSys.getPath(hdfsPath);
        // Path parent = new Path(hdfsPath);
        if (!fileSys.exists(parent)) {
            return;
        }
        List<IPathInfo> child = fileSys.listChildren(parent, pathFilter);
        List<PathInfo> timestampList = new ArrayList<>();
        PathInfo pathinfo;
        Matcher matcher;
        for (IPathInfo c : child) {
            matcher = ITISFileSystem.DATE_PATTERN.matcher(c.getPath().getName());
            if (matcher.matches()) {
                pathinfo = new PathInfo();
                pathinfo.pathName = c.getPath().getName();
                pathinfo.timeStamp = Long.parseLong(matcher.group());
                timestampList.add(pathinfo);
            }
        }
        if (timestampList.size() > 0) {
            deleteOldHdfsfile(fileSys, parent, timestampList, maxPartitionSave);
        }
    }

    /**
     * 删除hdfs中的文件
     *
     * @param isBuildFile
     * @throws IOException
     */
    private static void deleteHdfsFile(ITISFileSystem fileSystem, INameWithPathGetter dumpTable
            , boolean isBuildFile, ITISFileSystem.IPathFilter filter, int maxPartitionSave) throws IOException {
        // dump数据: /user/admin/scmdb/supply_goods/all/0/20160105003307
        String hdfsPath = getJoinTableStorePath(fileSystem.getRootDir(), dumpTable) + "/all";
        ITISFileSystem fileSys = fileSystem;
        int group = 0;
        List<IPathInfo> children = null;
        while (true) {
            IPath parent = fileSys.getPath(hdfsPath + "/" + (group++));
            if (isBuildFile) {
                parent = fileSys.getPath(parent, "output");
            }
            if (!fileSys.exists(parent)) {
                break;
            }
            children = fileSys.listChildren(parent, filter);
            // FileStatus[] child = fileSys.listStatus(parent, filter);
            List<PathInfo> dumpTimestamps = new ArrayList<>();
            for (IPathInfo f : children) {
                try {
                    PathInfo pathinfo = new PathInfo();
                    pathinfo.pathName = f.getPath().getName();
                    pathinfo.timeStamp = Long.parseLong(f.getPath().getName());
                    dumpTimestamps.add(pathinfo);
                } catch (Throwable e) {
                }
            }
            deleteOldHdfsfile(fileSys, parent, dumpTimestamps, maxPartitionSave);
        }
    }

    /**
     * @param fileSys
     * @param parent
     * @param timestampList
     * @throws IOException
     */
    public static void deleteOldHdfsfile(ITISFileSystem fileSys, IPath parent, List<PathInfo> timestampList, int maxPartitionSave) throws IOException {
        // 倒序排列，保留最近的一次
        sortTimestamp(timestampList);
        IPath toDelete = null;
        for (int index = (maxPartitionSave); index < timestampList.size(); index++) {
            toDelete = fileSys.getPath(parent, timestampList.get(index).pathName);
            if (!fileSys.exists(toDelete)) {
                logger.warn("toDelete path:`" + toDelete + "` is not exist,skip the delete process ");
                continue;
            }
            // toDelete = new Path(parent, timestampList.get(index).pathName);
            // 删除历史数据
            logger.info("history old file path:" + toDelete.toString() + " delete,success:" + fileSys.delete(toDelete, true) + ",getMaxPartitionSave:" + maxPartitionSave);
        }
    }

    /**
     * @param timestampList
     */
    private static void sortTimestamp(List<PathInfo> timestampList) {
        // 最大的应该的index为0的位置上
        Collections.sort(timestampList, (o1, o2) -> (int) (o2.timeStamp - o1.timeStamp));
    }

    public static String getJoinTableStorePath(IPath rootDir, INameWithPathGetter pathGetter) {
        return rootDir.getName() + "/" + pathGetter.getNameWithPath();
    }

    public static class PathInfo {

        private long timeStamp;

        private String pathName;

        public long getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        public String getPathName() {
            return pathName;
        }

        public void setPathName(String pathName) {
            this.pathName = pathName;
        }
    }
}
