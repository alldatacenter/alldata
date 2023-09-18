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

package com.qlangtech.tis.plugin.datax;

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsColMeta;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsWriterErrorCode;
import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.TimeFormat;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.hdfs.impl.HdfsFileSystemFactory;
import com.qlangtech.tis.hdfs.impl.HdfsPath;
import com.qlangtech.tis.hive.HiveColumn;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-08 16:34
 **/
public abstract class BasicEngineJob<TT extends DataXHiveWriter> extends BasicHdfsWriterJob<TT> {
    private static final Logger logger = LoggerFactory.getLogger(BasicEngineJob.class);
    private EntityName dumpTable = null;
    private List<HiveColumn> colsExcludePartitionCols = null;
    private TimeFormat timeFormat;

    private Integer ptRetainNum;


    @Override
    public void init() {
        //try {
        super.init();
//        } catch (Throwable e) {
//            if (ExceptionUtils.indexOfType(e, JobPropInitializeException.class) > -1) {
//                throw new RuntimeException(e);
//            } else {
//                TisDataXHiveWriter.logger.warn("init alibaba hdfs writer Job faild,errmsg:" + StringUtils.substringBefore(e.getMessage(), "\n"));
//            }
//        }

        this.ptRetainNum = getPtRetainNum();
        TT writerPlugin = this.getWriterPlugin();
        Objects.requireNonNull(writerPlugin, "writerPlugin can not be null");
        //this.getDumpTable();

        // Objects.requireNonNull(this.dumpTimeStamp, "dumpTimeStamp can not be null");

    }

    protected int getPtRetainNum() {
        return Integer.parseInt(this.cfg.getNecessaryValue("ptRetainNum", HdfsWriterErrorCode.REQUIRED_VALUE));
    }

    @Override
    public void prepare() {
        super.prepare();

        this.colsExcludePartitionCols = getCols();
        int[] appendStartIndex = new int[]{colsExcludePartitionCols.size()};
        List<HiveColumn> cols = Lists.newArrayList(colsExcludePartitionCols);

        IDumpTable.preservedPsCols.forEach((c) -> {
            HiveColumn hiveCol = new HiveColumn();
            hiveCol.setName(c);
            hiveCol.setDataType(DataType.createVarChar(100));
            // hiveCol.setType(SupportHiveDataType.STRING.name());
            hiveCol.setIndex(appendStartIndex[0]++);
            cols.add(hiveCol);
        });
        // initializeHiveTable(cols);
    }

    @Override
    public Configuration getPluginJobConf() {
        Configuration cfg = super.getPluginJobConf();
        // this.writerPlugin = TisDataXHiveWriter.getHdfsWriterPlugin(cfg);
        // 写了一个默认的可以骗过父类校验
        return cfg;
    }

    protected Path createPath() throws IOException {
        // SimpleDateFormat timeFormat = new SimpleDateFormat();

        this.timeFormat = TimeFormat.parse(this.cfg.getNecessaryValue("ptFormat", HdfsWriterErrorCode.REQUIRED_VALUE));

        this.dumpTable = this.createDumpTable();
        TT writerPlugin = this.getWriterPlugin();
        this.tabDumpParentPath = new Path(writerPlugin.getFs().getFileSystem().getRootDir().unwrap(Path.class), getHdfsSubPath());
        Path pmodPath = getPmodPath();
        // 将path创建
        HdfsFileSystemFactory hdfsFactory = (HdfsFileSystemFactory) writerPlugin.getFs();
        hdfsFactory.getFileSystem().mkdirs(new HdfsPath(pmodPath));
        return pmodPath;
    }


    protected Path getPmodPath() {
        return new Path(tabDumpParentPath, "0");
    }

    protected String getHdfsSubPath() {
        Objects.requireNonNull(dumpTable, "dumpTable can not be null");
        Objects.requireNonNull(timeFormat, "timeFormat can not be null");
        return this.dumpTable.getNameWithPath() + "/" + timeFormat.format(DataxUtils.getDumpTimeStamp());
    }

    protected EntityName createDumpTable() {
        String hiveTableName = cfg.getString(TisDataXHiveWriter.KEY_HIVE_TAB_NAME);
        if (StringUtils.isBlank(hiveTableName)) {
            throw new IllegalStateException("config key " + TisDataXHiveWriter.KEY_HIVE_TAB_NAME + " can not be null");
        }
//        if (!(writerPlugin instanceof DataXHiveWriter)) {
//            throw new IllegalStateException("hiveWriterPlugin must be type of DataXHiveWriter");
//        }
        return EntityName.create(getWriterPlugin().getHiveConnGetter().getDbName(), hiveTableName);
    }


//    protected void initializeHiveTable(List<HiveColumn> cols) {
//        try {
//            TT writerPlugin = getWriterPlugin();
//            try (Connection conn = writerPlugin.getConnection()) {
//                Objects.requireNonNull(this.tabDumpParentPath, "tabDumpParentPath can not be null");
//                ITISFileSystem fs = this.getFileSystem();
//                JoinHiveTask.initializeHiveTable(fs
//                        , fs.getPath(new HdfsPath(this.tabDumpParentPath), "..")
//                        , writerPlugin.getEngineType(), parseFSFormat()
//                        , cols, colsExcludePartitionCols, conn, dumpTable, this.ptRetainNum);
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

//    private HdfsFormat parseFSFormat() {
//        //try {
//        HdfsFormat fsFormat = new HdfsFormat();
//
//        fsFormat.setFieldDelimiter(this.fieldDelimiter
//                //        (String) TisDataXHiveWriter.jobFieldDelimiter.get(this)
//        );
//        //  (String) TisDataXHiveWriter.jobFileType.get(this)
//        fsFormat.setFileType(HdfsFileType.parse(this.fileType));
//        return fsFormat;
//    }

    /**
     * https://cwiki.apache.org/confluence/display/hive/languagemanual+ddl#LanguageManualDDL-CreateTableCreate/Drop/TruncateTable
     *
     * @return
     */
    private List<HiveColumn> getCols() {
        List<Configuration> cols = this.columns;
        AtomicInteger index = new AtomicInteger();
        return cols.stream().map((c) -> {
            HiveColumn hivCol = new HiveColumn();
            DataType colType = DataType.ds(c.getString(HdfsColMeta.KEY_TYPE));
            //SupportHiveDataType columnType = DataType.convert2HiveType();
            String name = StringUtils.remove(c.getString(HdfsColMeta.KEY_NAME), "`");
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("col name can not be blank");
            }
            hivCol.setName(name);
            hivCol.setDataType(colType);
            //hivCol.setType(columnType.name());
            hivCol.setIndex(index.getAndIncrement());
            return hivCol;
        }).collect(Collectors.toList());
    }


    @Override
    public void post() {
        super.post();
        // 需要将刚导入的hdfs和hive的parition绑定
        // Set<EntityName> hiveTables = Collections.singleton(this.dumpTable);

        if (CollectionUtils.isEmpty(colsExcludePartitionCols)) {
            throw new IllegalStateException("table:" + this.dumpTable + " relevant colsExcludePartitionCols can not be empty");
        }
        Objects.requireNonNull(tabDumpParentPath, "tabDumpParentPath can not be null");


        // this.bindHiveTables();
    }

//    protected void bindHiveTables() {
//        try {
//            try (Connection hiveConn = this.getWriterPlugin().getConnection()) {
//
//                final Path dumpParentPath = this.tabDumpParentPath;
//                BindHiveTableTool.bindHiveTables(this.getWriterPlugin().getEngineType(), hiveConn, this.getFileSystem()
//                        , Collections.singletonMap(this.dumpTable, () -> {
//                                    return new BindHiveTableTool.HiveBindConfig(colsExcludePartitionCols, dumpParentPath);
//                                }
//                        )
//                        , DataxUtils.getDumpTimeStamp() //
//                );
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
