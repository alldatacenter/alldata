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

import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsHelper;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsWriter;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.offline.DataxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-27 16:55
 **/
public class TisDataXHiveWriter extends Writer {

    public static final String KEY_HIVE_TAB_NAME = "hiveTableName";


    static final Logger logger = LoggerFactory.getLogger(TisDataXHiveWriter.class);

    public static class Job extends BasicEngineJob<DataXHiveWriter> {

    }

    public static class Task extends TisDataXHdfsWriter.Task {
//        private BasicFSWriter writerPlugin;
//        @Override
//        public void init() {
//            this.writerPlugin = getHdfsWriterPlugin(this.getPluginJobConf());
//            super.init();
//        }
//
//        @Override
//        protected HdfsHelper createHdfsHelper() {
//            return BasicHdfsWriterJob.createHdfsHelper(this.getPluginJobConf(), this.writerPlugin);
//        }
    }




}
