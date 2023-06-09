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

package com.alibaba.datax.plugin.writer.ftpwriter;

import com.alibaba.datax.core.job.IJobContainerContext;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.plugin.datax.DataXFtpWriter;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-04-05 15:38
 **/
public class TISFtpWriter extends FtpWriter {
    public static class Job extends FtpWriter.Job {
        @Override
        protected String createTargetPath(String tableName) {
            return getFtpTargetDir(tableName, this.containerContext);
        }
    }

    private static String getFtpTargetDir(String tableName, IJobContainerContext containerContext) {
        // IDataxProcessor processor = DataxProcessor.load(null, containerContext.getTISDataXName());
        DataXFtpWriter writer = (DataXFtpWriter) DataxWriter.load(null, containerContext.getTISDataXName());
        //  DataXFtpWriter writer = (DataXFtpWriter) processor.getWriter(null);
        return writer.writeMetaData.getFtpTargetDir(writer, tableName);
    }

    public static class Task extends FtpWriter.Task {
    }
}
