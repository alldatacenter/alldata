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
import com.alibaba.datax.plugin.reader.hdfsreader.AbstractDFSUtil;
import com.alibaba.datax.plugin.reader.hdfsreader.HdfsReader;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsWriterErrorCode;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.offline.DataxUtils;
import org.apache.hadoop.fs.FileSystem;

import java.io.IOException;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-11 19:13
 **/
public class TisDataXHdfsReader extends HdfsReader {

    public static class Job extends HdfsReader.Job {
        @Override
        protected AbstractDFSUtil createDfsUtil() {
            return new TISDFSUtil(this.readerOriginConfig);
        }
    }

    public static class Task extends HdfsReader.Task {
        @Override
        protected AbstractDFSUtil createDfsUtil() {
            return new TISDFSUtil(this.taskConfig);
        }
    }

    private static class TISDFSUtil extends AbstractDFSUtil {
        private DataXHdfsReader dataXReader;

        public TISDFSUtil(com.alibaba.datax.common.util.Configuration cfg) {
            super(new org.apache.hadoop.conf.Configuration());
            this.dataXReader = getHdfsReaderPlugin(cfg);
        }

        private DataXHdfsReader getHdfsReader() {
            return this.dataXReader;
        }

        @Override
        protected FileSystem getFileSystem() throws IOException {
            return getHdfsReader().getFs().getFileSystem().unwrap();
        }
    }

    private static DataXHdfsReader getHdfsReaderPlugin(Configuration cfg) {
        String dataxName = cfg.getNecessaryValue(DataxUtils.DATAX_NAME, HdfsWriterErrorCode.REQUIRED_VALUE);
        DataxReader dataxReader = DataxReader.load(null, dataxName);
        if (!(dataxReader instanceof DataXHdfsReader)) {
            throw new BasicHdfsWriterJob.JobPropInitializeException(
                    "datax reader must be type of 'DataXHdfsReader',but now is:" + dataxReader.getClass());
        }
        return (DataXHdfsReader) dataxReader;
    }
}
