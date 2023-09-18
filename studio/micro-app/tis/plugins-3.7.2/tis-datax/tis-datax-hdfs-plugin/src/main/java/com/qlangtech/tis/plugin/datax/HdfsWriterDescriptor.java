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

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.fs.ITISFileSystemFactory;
import com.qlangtech.tis.hdfs.impl.HdfsFileSystemFactory;
import com.qlangtech.tis.offline.FileSystemFactory;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;

import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-02-09 12:44
 **/
public abstract class HdfsWriterDescriptor extends DataxWriter.BaseDataxWriterDescriptor {
    public HdfsWriterDescriptor() {
        super();
        this.registerSelectOptions(ITISFileSystemFactory.KEY_FIELD_NAME_FS_NAME
                , () -> TIS.getPluginStore(FileSystemFactory.class)
                        .getPlugins().stream().filter(((f) -> f instanceof HdfsFileSystemFactory)).collect(Collectors.toList()));
    }

    public boolean validateFsName(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
        FileSystemFactory fsFactory = FileSystemFactory.getFsFactory(value);
        if (fsFactory == null) {
            throw new IllegalStateException("can not find FileSystemFactory relevant with:" + value);
        }
        if (!(fsFactory instanceof HdfsFileSystemFactory)) {
            msgHandler.addFieldError(context, fieldName, "必须是HDFS类型的文件系统");
            return false;
        }
        return true;
    }


    @Override
    public boolean isSupportIncr() {
        return false;
    }

    @Override
    public EndType getEndType() {
        return EndType.HDFS;
    }

    @Override
    public boolean isRdbms() {
        return false;
    }


}
