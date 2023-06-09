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
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsHelper;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsWriter;
import com.alibaba.datax.plugin.writer.hdfswriter.Key;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.IFSWriter;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.offline.FileSystemFactory;
import com.qlangtech.tis.plugin.StoreResourceType;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-08 17:47
 **/
public abstract class BasicHdfsWriterJob<T extends BasicFSWriter> extends HdfsWriter.Job {
    private static final Logger logger = LoggerFactory.getLogger(BasicHdfsWriterJob.class);

    protected Path tabDumpParentPath;

    private IFSWriter writerPlugin = null;
    private ITISFileSystem fileSystem = null;
    protected Configuration cfg = null;
    private String dumpTimeStamp;

    public static IFSWriter getHdfsWriterPlugin(Configuration cfg) {
        String dataxName = cfg.getString(DataxUtils.DATAX_NAME);
        StoreResourceType resType = StoreResourceType.parse(
                cfg.getString(StoreResourceType.KEY_STORE_RESOURCE_TYPE));
        return Objects.requireNonNull(BasicFSWriter.getWriterPlugin(dataxName, resType)
                , "dataName:" + dataxName + ",type:" + resType + " relevant writer can not be null");
    }


    protected T getWriterPlugin() {
        return (T) this.writerPlugin;
    }

    @Override
    public void init() {
        getPluginJobConf();
        try {
            if (this.tabDumpParentPath == null) {
                Path path = createPath();
                this.path = String.valueOf(path);
                Objects.requireNonNull(this.tabDumpParentPath, "tabDumpParentPath can not be null");
                FileSystemFactory hdfsFactory = writerPlugin.getFsFactory();
                logger.info("config param {}:{}", Key.PATH, hdfsFactory.getFSAddress());
                cfg.set(Key.DEFAULT_FS, hdfsFactory.getFSAddress());
            }
        } catch (Throwable e) {
            throw new JobPropInitializeException("pmodPath initial error", e);
        }

        Objects.requireNonNull(writerPlugin, "writerPlugin can not be null");

        super.init();
    }

    public ITISFileSystem getFileSystem() {
        if (fileSystem == null) {
            this.fileSystem = writerPlugin.getFsFactory().getFileSystem();
            Objects.requireNonNull(fileSystem, "fileSystem can not be null");
        }
        return fileSystem;
    }


    @Override
    protected HdfsHelper createHdfsHelper() {
        return createHdfsHelper(this.cfg, writerPlugin);

    }

    @Override
    public void prepare() {
        super.prepare();
        // 需要自动创建hive表
    }

    @Override
    public Configuration getPluginJobConf() {
        this.cfg = super.getPluginJobConf();
        this.writerPlugin = getHdfsWriterPlugin(this.cfg);

        return this.cfg;
    }

    protected abstract Path createPath() throws IOException;

    public static HdfsHelper createHdfsHelper(Configuration pluginJobConf, IFSWriter hiveWriter) {
        Objects.requireNonNull(pluginJobConf, "pluginJobConf can not be null");
        Objects.requireNonNull(hiveWriter, "hiveWriter can not be null");
        try {
            FileSystemFactory fs = hiveWriter.getFsFactory();
            FileSystem fileSystem = Objects.requireNonNull(fs.getFileSystem().unwrap(), "fileSystem can not be empty");
            HdfsHelper hdfsHelper = new HdfsHelper(fileSystem);

            org.apache.hadoop.conf.Configuration cfg = new org.apache.hadoop.conf.Configuration();
            cfg.setClassLoader(TIS.get().getPluginManager().uberClassLoader);

            org.apache.hadoop.mapred.JobConf conf = new JobConf(cfg);
            conf.set(FileSystem.FS_DEFAULT_NAME_KEY, pluginJobConf.getString("defaultFS"));
            conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
            conf.set(JobContext.WORKING_DIR, (fs.getFileSystem().getRootDir().unwrap(Path.class).toString()));
            hdfsHelper.conf = conf;
            return hdfsHelper;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class JobPropInitializeException extends RuntimeException {
        public JobPropInitializeException(String message, Throwable cause) {
            super(message, cause);
        }

        public JobPropInitializeException(String message) {
            super(message);
        }
    }

}
