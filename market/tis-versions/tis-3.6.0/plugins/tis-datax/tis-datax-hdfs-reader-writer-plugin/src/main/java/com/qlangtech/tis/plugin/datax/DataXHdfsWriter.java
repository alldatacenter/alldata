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
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.fs.ITISFileSystemFactory;
import com.qlangtech.tis.hdfs.impl.HdfsFileSystemFactory;
import com.qlangtech.tis.offline.FileSystemFactory;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;

import java.util.stream.Collectors;

/**
 * https://github.com/alibaba/DataX/blob/master/hdfswriter/doc/hdfswriter.md
 *
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 * @see com.qlangtech.tis.plugin.datax.TisDataXHdfsWriter
 **/
@Public
public class DataXHdfsWriter extends BasicFSWriter {
    public static final String DATAX_NAME = "Hdfs";

    @FormField(ordinal = 5, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.relative_path})
    public String path;

    @FormField(ordinal = 15, type = FormFieldType.TEXTAREA,advance = false , validate = {Validator.require})
    public String template;

    @Override
    public String getTemplate() {
        return this.template;
    }


    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXHdfsWriter.class, "DataXHdfsWriter-tpl.json");
    }

    @Override
    protected FSDataXContext getDataXContext(IDataxProcessor.TableMap tableMap) {
        return new HdfsDataXContext(tableMap, this.dataXName);
    }

    public class HdfsDataXContext extends FSDataXContext {
        public HdfsDataXContext(IDataxProcessor.TableMap tabMap, String dataxName) {
            super(tabMap, dataxName);
        }

        public String getPath() {
            return path;
        }
    }


    @TISExtension()
    public static class DefaultDescriptor extends DataxWriter.BaseDataxWriterDescriptor {
        public DefaultDescriptor() {
            super();
//            this.registerSelectOptions(HiveFlatTableBuilder.KEY_FIELD_NAME_FS_NAME
//                    , () -> TIS.getPluginStore(FileSystemFactory.class).getPlugins());
            this.registerSelectOptions(ITISFileSystemFactory.KEY_FIELD_NAME_FS_NAME
                    , () -> TIS.getPluginStore(FileSystemFactory.class)
                            .getPlugins().stream().filter(((f) -> f instanceof HdfsFileSystemFactory)).collect(Collectors.toList()));
        }

        public boolean validateFsName(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            return DataXHdfsWriter.validateFsName(msgHandler, context, fieldName, value);
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

        @Override
        public String getDisplayName() {
            return DATAX_NAME;
        }
    }

    protected static boolean validateFsName(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
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
}
