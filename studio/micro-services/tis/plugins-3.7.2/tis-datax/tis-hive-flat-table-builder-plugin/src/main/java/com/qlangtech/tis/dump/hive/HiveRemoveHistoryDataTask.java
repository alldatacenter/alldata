/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.dump.hive;

import com.google.common.collect.Lists;
import com.qlangtech.tis.fs.FSHistoryFileUtils;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.taskflow.HiveTask;
import com.qlangtech.tis.order.dump.task.ITableDumpConstant;
import com.qlangtech.tis.plugin.datax.MREngine;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年11月26日 上午11:54:31
 */
public class HiveRemoveHistoryDataTask {

    private static final Logger logger = LoggerFactory.getLogger(HiveRemoveHistoryDataTask.class);


    // daily ps name
    private static final String pt = IDumpTable.PARTITION_PT;

    private final ITISFileSystem fileSystem;
    private final DataSourceMeta ds;
    private final MREngine mrEngine;

    public static void main(String[] arg) {
//        List<PathInfo> timestampList = new ArrayList<PathInfo>();
//        PathInfo path = null;
//        for (int i = 0; i < 100; i++) {
//            path = new PathInfo();
//            path.setTimeStamp(i);
//            timestampList.add(path);
//        }
//        sortTimestamp(timestampList);
//        for (PathInfo info : timestampList) {
//            System.out.println(info.timeStamp);
//        }
    }

    public HiveRemoveHistoryDataTask(MREngine mrEngine, ITISFileSystem fsFactory, DataSourceMeta ds) {
        super();
        this.fileSystem = fsFactory;
        this.ds = ds;
        this.mrEngine = mrEngine;
    }

    /**
     * @param hiveConnection
     * @throws Exception
     */
    public void deleteHdfsHistoryFile(EntityName dumpTable, DataSourceMeta.JDBCConnection hiveConnection) {
        try {
            logger.info("start deleteHdfsHistoryFile data[{}] files", dumpTable);
            this.fileSystem.deleteHistoryFile(dumpTable);
            // this.deleteMetadata(dumpTable);
            // this.deleteHdfsFile(dumpTable, false);
            // 索引数据: /user/admin/search4totalpay/all/0/output/20160104003306
            // this.deleteHdfsFile(dumpTable, true);
            this.dropHistoryHiveTable(dumpTable, hiveConnection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除特定timestamp下的文件，前一次用户已经导入了文件，后一次想立即重新导入一遍
     *
     * @param hiveConnection
     * @throws Exception
     */
    public void deleteHdfsHistoryFile(EntityName dumpTable, DataSourceMeta.JDBCConnection hiveConnection, String timestamp) {
        try {
            logger.info("start delete history data{} files", dumpTable);
//            this.deleteMetadata(dumpTable, timestamp);
//            this.deleteHdfsFile(dumpTable, false, /* isBuildFile */
//                    timestamp);
//            // 索引数据: /user/admin/search4totalpay/all/0/output/20160104003306
//            this.deleteHdfsFile(dumpTable, true, /* isBuildFile */
//                    timestamp);
            this.fileSystem.deleteHistoryFile(dumpTable, timestamp);
            this.dropHistoryHiveTable(dumpTable, hiveConnection, (r) -> StringUtils.equals(r, timestamp), 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    /**
//     * 删除dump的metadata<br>
//     * example:/user/admin/search4customerregistercard/all/20160106131304
//     * example:/user/admin/scmdb/supply_goods/all/20160106131304
//     */
//    private void deleteMetadata(EntityName dumpTable) throws Exception {
//        this.deleteMetadata(dumpTable, (r) -> {
//            return true;
//        }, ITableDumpConstant.MAX_PARTITION_SAVE);
//    }

//    private void deleteMetadata(EntityName dumpTable, String timestamp) throws Exception {
//        if (StringUtils.isEmpty(timestamp)) {
//            throw new IllegalArgumentException("param timestamp can not be null");
//        }
//        this.deleteMetadata(dumpTable, (r) -> {
//                    return StringUtils.equals(r.getName(), timestamp);
//                }, // MAX_PARTITION_SAVE
//                0);
//    }

//    /**
//     * 删除dump的metadata<br>
//     * example:/user/admin/search4customerregistercard/all/20160106131304
//     * example:/user/admin/scmdb/supply_goods/all/20160106131304
//     */
//    private void deleteMetadata(EntityName dumpTable, ITISFileSystem.IPathFilter pathFilter, int maxPartitionSave) throws Exception {
//        String hdfsPath = getJoinTableStorePath(this.fileSystem.getRootDir(), dumpTable) + "/all";
//        logger.info("hdfsPath:{}", hdfsPath);
//        ITISFileSystem fileSys = this.fileSystem;
//        IPath parent = fileSys.getPath(hdfsPath);
//        // Path parent = new Path(hdfsPath);
//        if (!fileSys.exists(parent)) {
//            return;
//        }
//        List<IPathInfo> child = fileSys.listChildren(parent, pathFilter);
//        List<PathInfo> timestampList = new ArrayList<>();
//        PathInfo pathinfo;
//        Matcher matcher;
//        for (IPathInfo c : child) {
//            matcher = DATE_PATTERN.matcher(c.getPath().getName());
//            if (matcher.matches()) {
//                pathinfo = new PathInfo();
//                pathinfo.pathName = c.getPath().getName();
//                pathinfo.timeStamp = Long.parseLong(matcher.group());
//                timestampList.add(pathinfo);
//            }
//        }
//        if (timestampList.size() > 0) {
//            deleteOldHdfsfile(fileSys, parent, timestampList, maxPartitionSave);
//        }
//    }


//    /**
//     * 删除历史索引build文件
//     *
//     * @throws IOException
//     * @throws FileNotFoundException
//     */
//    public void removeHistoryBuildFile(EntityName dumpTable) throws IOException, FileNotFoundException {
//        this.deleteHdfsFile(dumpTable, true);
//    }

//    private void deleteHdfsFile(EntityName dumpTable, boolean isBuildFile) throws IOException {
//        this.deleteHdfsFile(dumpTable, isBuildFile, (r) -> true, ITableDumpConstant.MAX_PARTITION_SAVE);
//    }


//    /**
//     * 删除hdfs中的文件
//     *
//     * @param isBuildFile
//     * @throws IOException
//     */
//    private void deleteHdfsFile(EntityName dumpTable, boolean isBuildFile, ITISFileSystem.IPathFilter filter, int maxPartitionSave) throws IOException {
//        // dump数据: /user/admin/scmdb/supply_goods/all/0/20160105003307
//        String hdfsPath = getJoinTableStorePath(fileSystem.getRootDir(), dumpTable) + "/all";
//        ITISFileSystem fileSys = fileSystem;
//        int group = 0;
//        List<IPathInfo> children = null;
//        while (true) {
//            IPath parent = fileSys.getPath(hdfsPath + "/" + (group++));
//            if (isBuildFile) {
//                parent = fileSys.getPath(parent, "output");
//            }
//            if (!fileSys.exists(parent)) {
//                break;
//            }
//            children = fileSys.listChildren(parent, filter);
//            // FileStatus[] child = fileSys.listStatus(parent, filter);
//            List<PathInfo> dumpTimestamps = new ArrayList<>();
//            for (IPathInfo f : children) {
//                try {
//                    PathInfo pathinfo = new PathInfo();
//                    pathinfo.pathName = f.getPath().getName();
//                    pathinfo.timeStamp = Long.parseLong(f.getPath().getName());
//                    dumpTimestamps.add(pathinfo);
//                } catch (Throwable e) {
//                }
//            }
//            deleteOldHdfsfile(fileSys, parent, dumpTimestamps, maxPartitionSave);
//        }
//    }


    public void dropHistoryHiveTable(EntityName dumpTable, DataSourceMeta.JDBCConnection conn) {
        this.dropHistoryHiveTable(dumpTable, conn, ITableDumpConstant.MAX_PARTITION_SAVE);
    }

    public List<FSHistoryFileUtils.PathInfo> dropHistoryHiveTable(EntityName dumpTable, DataSourceMeta.JDBCConnection conn, Integer partitionRetainNum) {
        return this.dropHistoryHiveTable(dumpTable, conn, (r) -> true, partitionRetainNum);
    }

    /**
     * 删除hive中的历史表
     */
    public List<FSHistoryFileUtils.PathInfo> dropHistoryHiveTable(EntityName dumpTable, DataSourceMeta.JDBCConnection conn, PartitionFilter filter, Integer maxPartitionSave) {
        if (maxPartitionSave < 1) {
            throw new IllegalArgumentException("param maxPartitionSave can not small than 1");
        }
        final EntityName table = dumpTable;
        if (StringUtils.isBlank(pt)) {
            throw new IllegalStateException("pt name shall be set");
        }
        String existTimestamp = null;
        FSHistoryFileUtils.PathInfo pathInfo = null;
        List<FSHistoryFileUtils.PathInfo> deletePts = Lists.newArrayList();
        try {
            // 判断表是否存在
            if (!HiveTask.isTableExists(this.ds, conn, table)) {
                logger.info(table + " is not exist");
                return Collections.emptyList();
            }
            List<String> ptList = getHistoryPts(this.ds, conn, filter, table);
            int count = 0;

            for (int i = ptList.size() - 1; i >= 0; i--) {
                if ((++count) > maxPartitionSave) {
                    existTimestamp = ptList.get(i);
                    pathInfo = new FSHistoryFileUtils.PathInfo();
                    pathInfo.setTimeStamp(Long.parseLong(existTimestamp));
                    pathInfo.setPathName(existTimestamp);
                    deletePts.add(pathInfo);
                    String alterSql = "alter table " + getFullTabName(table) + " drop partition (  " + pt + " = '" + existTimestamp + "' )";
                    try {
                        HiveDBUtils.execute(conn, alterSql);
                    } catch (Throwable e) {
                        logger.error("alterSql:" + alterSql, e);
                    }
                    logger.info("history table:" + table + ", partition:" + pt + "='" + existTimestamp + "', have been removed");
                }
            }
            logger.info("maxPartitionSave:" + maxPartitionSave + ",table:" + table.getFullName()
                    + " exist partitions:" + ptList.stream().collect(Collectors.joining(",")) + " dropped partitions:"
                    + deletePts.stream().map((p) -> p.getPathName()).collect(Collectors.joining(",")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return deletePts;
    }

    private String getFullTabName(EntityName table) {
        return table.getFullName((this.ds.getEscapeChar()));
    }

    public static List<String> getHistoryPts(DataSourceMeta mrEngine, DataSourceMeta.JDBCConnection conn, final EntityName table) throws Exception {
        return getHistoryPts(mrEngine, conn, (ps) -> true, table);
    }

    private static List<String> getHistoryPts(DataSourceMeta mrEngine, DataSourceMeta.JDBCConnection conn, PartitionFilter filter, final EntityName table) throws Exception {
        final Set<String> ptSet = new HashSet<>();
        final String showPartition = "show partitions " + table.getFullName((mrEngine.getEscapeChar()));
        final Pattern ptPattern = Pattern.compile(pt + "=(\\d+)");
        conn.query(showPartition, result -> {
            Matcher matcher = ptPattern.matcher(result.getString(1));
            if (matcher.find() && filter.accept(matcher.group(1))) {
                ptSet.add(matcher.group(1));
            } else {
                logger.warn(table + ",partition" + result.getString(1) + ",is not match pattern:" + ptPattern);
            }
            return true;
        });
        List<String> ptList = new LinkedList<>(ptSet);
        Collections.sort(ptList);
        return ptList;
    }

    private interface PartitionFilter {

        boolean accept(String ps);
    }
}
