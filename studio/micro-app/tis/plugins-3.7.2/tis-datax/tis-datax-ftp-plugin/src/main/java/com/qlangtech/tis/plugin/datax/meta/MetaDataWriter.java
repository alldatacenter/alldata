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

package com.qlangtech.tis.plugin.datax.meta;

import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.plugin.datax.DataXFtpWriter;
import com.qlangtech.tis.plugin.ds.ISelectedTab;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-04-05 15:30
 **/
public abstract class MetaDataWriter implements Describable<MetaDataWriter> {



    public abstract IRemoteTaskTrigger createMetaDataWriteTask(
            DataXFtpWriter ftpWriter, IExecChainContext execContext, ISelectedTab tab);

    public String getFtpTargetDir(DataXFtpWriter writer, String tableName) {
        return writer.path;
    }
}
