/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.hive.HiveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import scala.collection.JavaConverters;
public class SparkCompactJobs {
    private static final Logger logger = LoggerFactory.getLogger(SparkCompactJobs.class);

    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 1) {
            logger.error("输入参数个数有误！");
            return;
        }
        String tableName = args[0];
        String appName = String.format("%s表文件合并任务算子", tableName);
        SparkConf conf = new SparkConf().setAppName(appName);
        JavaSparkContext sparkContext = new JavaSparkContext(conf);
        SparkSession sparkSession = SparkSession.builder().config(conf)
                .enableHiveSupport()
                .getOrCreate();
        HiveContext hiveContext = new HiveContext(sparkContext);
        String formatted = String.format("desc formatted %s", tableName);
        Dataset<Row> sql = hiveContext.sql(formatted);
        List<Row> list = sql.collectAsList();
        String hivePath = "";
        String tableType = "";
        String hiveFileType = "";
        for (Row row : list) {
            String colName = row.getString(0);
            // 数据格式是Location |hdfs://mycluster/warehouse/tablespace/managed/hive/ssb.db/t_part
            if (StringUtils.isNotEmpty(colName) && colName.contains("Location")) {
                hivePath = row.getString(1);
            }
            // 获取表存储格式
            if (StringUtils.isNotEmpty(colName) && colName.contains("OutputFormat")) {
                tableType = row.getString(1);
            }
            if (tableType.contains("orc")) {
                hiveFileType = "orc";
            } else if (tableType.contains("parquet")) {
                hiveFileType = "parquet";
            } else if (tableType.toLowerCase().contains("text")) {
                hiveFileType = "text";
            }
        }
        logger.info("table:{},formatted:{}", tableName, list);
        // 非orc表不执行小文件合并
        if (StringUtils.isEmpty(hivePath) || StringUtils.isEmpty(hiveFileType)) {
            logger.info("hivePath:{},tableType:{}", hivePath, tableType);
            return;
        }
        // 查看hdfs路径下文件大小
        Configuration hadoopConf = sparkContext.hadoopConfiguration();
        FileSystem fileSystem = FileSystem.get(hadoopConf);
        Path path = new Path(hivePath);
        FileStatus[] fileStatuses = fileSystem.listStatus(path);
        List<FileStatus> fileStatusesList = new ArrayList<FileStatus>();
        Collections.addAll(fileStatusesList, fileStatuses);
        // 把表根目录加入递归查询
        fileStatusesList.add(fileSystem.getFileStatus(path));
        FileStatus[] fileStatusesCopy = new FileStatus[fileStatusesList.size()];
        fileStatusesList.toArray(fileStatusesCopy);
        for (FileStatus fileStatus : fileStatusesCopy) {
            FileStatus[] temp = new FileStatus[1];
            temp[0] = fileStatus;
            // 递归连接小文件的dataset
            buildDataSet(hadoopConf, sparkSession, fileSystem, temp, hiveFileType);
            logger.info("小文件合并完成！table:{}", tableName);
        }

    }

    /**
     * 构建合并后的大文件.
     *
     * @param compactDirList 合并集合
     * @param deleteFiles    待删除文件
     * @param hiveFileType   文件类型
     */
    private static void buildFiles(String hiveFileType, List<Path[]> deleteFiles, Map<Path, CompactInfo> compactDirList) {
        // 日志记录删除的文件
        List<String> deletefileStr = new ArrayList<String>();
        for (Path[] deleteFile : deleteFiles) {
            deletefileStr.add(deleteFile[0].toUri().getPath());
        }
        logger.info("delete source files:{}, tempDir:{}", StringUtils.join(deletefileStr, ","), StringUtils.join(compactDirList.keySet(), ","));
        for (Path savePathDir : compactDirList.keySet()) {
            CompactInfo compactInfo = compactDirList.get(savePathDir);
            Dataset<Row> rowDataset = compactInfo.getDataset();
            if (null != rowDataset) {
                int partitionNum = (int) Math.max(compactInfo.getBytes() / 128000000, 1);
                rowDataset.repartition(partitionNum).write().mode(SaveMode.Overwrite).format(hiveFileType).save(savePathDir.toUri().getPath());
            }
        }
    }

    /**
     * 复制合并后的大文件.
     */
    private static void copyCompactFile(Configuration hadoopConf, FileSystem fileSystem, List<Path[]> deleteFiles, Map<Path, CompactInfo> compactDirList) throws IOException {
        //记录copy成功合并后的文件
        List<Path> copyEnd = new ArrayList<Path>();
        // 把合并后的大文件，copy到分区目录下面
        try {
            for (Path savePathDir : compactDirList.keySet()) {
                FileStatus[] listStatus = fileSystem.listStatus(savePathDir);
                for (FileStatus status : listStatus) {
                    if (status.isDirectory() || status.getPath().getName().contains("_SUCCESS")) {
                        continue;
                    }
                    FileUtil.copy(fileSystem, status.getPath(), fileSystem, savePathDir.getParent(), true, hadoopConf);
                    String targetPath = savePathDir.getParent().toUri().getPath() + "/" + status.getPath().getName();
                    copyEnd.add(new Path(targetPath));
                }
            }
        } catch (IOException e) {
            // 删除复制报错，则全部回退,删除已复制和合并的大文件
            rollbackCompact(fileSystem, compactDirList, copyEnd);
            logger.error("copy compact file faild! rollback file copy", e);
            throw e;
        }
        try {
            // 移动删除文件到临时目录
            for (Path[] deleteFile : deleteFiles) {
                if (fileSystem.exists(deleteFile[0])) {
                    fileSystem.rename(deleteFile[0], deleteFile[1]);
                    logger.info("fileSystem.rename:{}--->{}", deleteFile[0].toUri(), deleteFile[1].toUri());
                }
            }
        } catch (IOException e) {
            logger.error("移动文件失败", e);
            throw e;
        }
        // 删除临时目录
        for (Path savePathDir : compactDirList.keySet()) {
            if (fileSystem.exists(savePathDir)) {
                fileSystem.delete(savePathDir, true);
            }
        }
    }

    /**
     * 回滚合并大文件.
     *
     * @param fileSystem     fileSystem
     * @param compactDirList compactDirList
     * @param copyEnd        copyEnd
     */
    private static void rollbackCompact(FileSystem fileSystem, Map<Path, CompactInfo> compactDirList, List<Path> copyEnd) throws IOException {
        if (!copyEnd.isEmpty()) {
            for (Path copyPath : copyEnd) {
                if (fileSystem.exists(copyPath)) {
                    fileSystem.delete(copyPath, true);
                }
            }
            for (Path savePathDir : compactDirList.keySet()) {
                if (fileSystem.exists(savePathDir)) {
                    fileSystem.delete(savePathDir, true);
                }
            }
        }
    }

    /**
     * 递归连接小文件dataset.
     *
     * @param sparkSession sparkSession
     * @param fileSystem   fileSystem
     * @param parentStatus parentStatus
     */
    private static void buildDataSet(Configuration hadoopConf, SparkSession sparkSession, FileSystem fileSystem, FileStatus[] parentStatus, String hiveFileType) throws Exception {
        for (FileStatus fileStatus : parentStatus) {
            Path fileStatusPath = fileStatus.getPath();
            // 如果是.开头的目录跳过
            if (fileStatus.isDirectory() && fileStatusPath.getName().startsWith(".")) {
                continue;
            } else if (fileStatus.isDirectory() && hasFiles(fileSystem, fileStatusPath)) {
                String compactDir = fileStatusPath.toUri().getPath() + "/." + UUID.randomUUID();
                Path key = new Path(compactDir);
                FileStatus[] statuses = fileSystem.listStatus(fileStatusPath);
                List<Path[]> deleteFiles = new ArrayList<Path[]>();
                Map<Path, CompactInfo> compactDirList = new HashMap<Path, CompactInfo>();
                for (int i = 0; i < statuses.length; i++) {
                    FileStatus status = statuses[i];
                    Path statusPath = status.getPath();
                    if (status.isFile()) {
                        long length = status.getLen();
                        // 大于64M的文件不合并
                        if (length > 64000000) {
                            continue;
                        }
                        String path = statusPath.toUri().getPath();
                        try {
                            CompactInfo compactInfo = compactDirList.get(key);
                            if (null == compactInfo) {
                                compactInfo = new CompactInfo();
                            }
                            List<String> filePaths = compactInfo.getFilePaths();
                            if (null == filePaths) {
                                filePaths = new ArrayList<>();
                            }
                            filePaths.add(path);
                            compactInfo.setFilePaths(filePaths);
                            long bytes = length + compactInfo.getBytes();
                            compactInfo.setBytes(bytes);
                            compactDirList.put(key, compactInfo);
                        } catch (Exception e) {
                            //读取文件失败则不合并，把异常抛出，任务失败，避免丢失数据
                            logger.error("read orc error,path:{}", path, e);
                            throw e;
                        }
                        Path[] deleteFile = new Path[2];
                        deleteFile[0] = statusPath;
                        deleteFile[1] = key;
                        deleteFiles.add(deleteFile);
                    } else {
                        FileStatus[] fileStatuses = fileSystem.listStatus(statusPath);
                        List<FileStatus> fileStatusesList = new ArrayList<FileStatus>();
                        Collections.addAll(fileStatusesList, fileStatuses);
                        // 把表根目录加入递归查询
                        fileStatusesList.add(fileSystem.getFileStatus(statusPath));
                        FileStatus[] fileStatusesCopy = new FileStatus[fileStatusesList.size()];
                        fileStatusesList.toArray(fileStatusesCopy);
                        buildDataSet(hadoopConf, sparkSession, fileSystem, fileStatusesCopy, hiveFileType);
                    }
                    //一百个文件合并成一个文件，小于100个文件直接合并成一个文件，测试10万个小文件合并时间52分钟
                    if (i == statuses.length - 1 || (i != 0 && i % 100 == 0)) {
                        if (compactDirList.size() > 0) {
                            long start = System.currentTimeMillis();
                            logger.info("compactDirList={}", compactDirList.size());
                            for (Path path : compactDirList.keySet()) {
                                CompactInfo compactInfo = compactDirList.get(path);
                                List<String> filePaths = compactInfo.getFilePaths();
                                Dataset<Row> rowDataset = sparkSession.read().format(hiveFileType).load(JavaConverters
                                        .asScalaIteratorConverter(filePaths.iterator())
                                        .asScala()
                                        .toSeq());
                                compactInfo.setDataset(rowDataset);
                            }
                            buildFiles(hiveFileType, deleteFiles, compactDirList);
                            copyCompactFile(hadoopConf, fileSystem, deleteFiles, compactDirList);
                            deleteFiles.clear();
                            compactDirList.clear();
                            long end = System.currentTimeMillis();
                            logger.info("compactDirList using time:{}ms", (end - start));
                        }
                    }
                }
            }
        }
    }
    /**
     * 判断目录下面是否有文件.
     *
     * @param fileSystem fileSystem
     * @param checkPath  checkPath
     */
    private static boolean hasFiles(FileSystem fileSystem, Path checkPath) throws IOException {
        FileStatus[] fileStatuses = fileSystem.listStatus(checkPath);
        boolean hasFile = false;
        for (FileStatus fileStatus : fileStatuses) {
            if (fileStatus.isFile()) {
                hasFile = true;
            }
        }
        return hasFile;
    }


    /**
     * 文件合并信息实体类.
     */
    public static class CompactInfo implements Serializable {
        private Dataset<Row> dataset;
        private long bytes;
        private List<String> filePaths;

        public Dataset<Row> getDataset() {
            return dataset;
        }

        public void setDataset(Dataset<Row> dataset) {
            this.dataset = dataset;
        }

        public long getBytes() {
            return bytes;
        }

        public void setBytes(long bytes) {
            this.bytes = bytes;
        }

        public List<String> getFilePaths() {
            return filePaths;
        }

        public void setFilePaths(List<String> filePaths) {
            this.filePaths = filePaths;
        }
    }

}
