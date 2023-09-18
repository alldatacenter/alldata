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

package com.qlangtech.tis.plugin.datax;

import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsHelper;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsWriter;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsWriterErrorCode;
import com.alibaba.datax.plugin.writer.hdfswriter.Key;
import com.qlangtech.tis.datax.IFSWriter;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.hdfs.impl.HdfsPath;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-27 16:55
 **/
public class BasicDataXHdfsWriter extends Writer {

    public static class Job extends BasicHdfsWriterJob<BasicFSWriter> {

        @Override
        protected Path createPath() throws IOException {
            ITISFileSystem fs = this.getWriterPlugin().getFs().getFileSystem();
            this.tabDumpParentPath = createTabDumpParentPath(fs);
            HdfsPath p = new HdfsPath(this.tabDumpParentPath);
            if (!fs.exists(p)) {
                fs.mkdirs(p);
            }
            return this.tabDumpParentPath;
        }

        protected Path createTabDumpParentPath(ITISFileSystem fs) {
            return new Path(fs.getRootDir().unwrap(Path.class)
                    , this.cfg.getNecessaryValue(Key.PATH, HdfsWriterErrorCode.REQUIRED_VALUE));
        }
    }


    public static abstract class Task extends HdfsWriter.Task {
        protected IFSWriter writerPlugin;

        @Override
        public void init() {
            this.writerPlugin = BasicHdfsWriterJob.getHdfsWriterPlugin(this.getPluginJobConf());
            super.init();
        }

        @Override
        protected void csvFileStartWrite(
                RecordReceiver lineReceiver, Configuration config, String fileName, TaskPluginCollector taskPluginCollector) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected HdfsHelper createHdfsHelper() {
            return BasicHdfsWriterJob.createHdfsHelper(this.getPluginJobConf(), this.writerPlugin);
        }

        @Override
        protected void avroFileStartWrite(RecordReceiver lineReceiver
                , Configuration writerSliceConfig, String fileName, TaskPluginCollector taskPluginCollector) {
            throw new UnsupportedOperationException();
        }
    }

}
