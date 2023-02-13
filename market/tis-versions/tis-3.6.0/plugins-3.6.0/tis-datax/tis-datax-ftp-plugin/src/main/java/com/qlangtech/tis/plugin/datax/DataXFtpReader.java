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
import com.alibaba.citrus.turbine.impl.DefaultContext;
import com.alibaba.datax.plugin.unstructuredstorage.Compress;
import com.alibaba.datax.plugin.unstructuredstorage.reader.UnstructuredStorageReaderUtil;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.IDataxReaderContext;
import com.qlangtech.tis.datax.IGroupChildTaskIterator;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.common.PluginFieldValidators;
import com.qlangtech.tis.plugin.datax.format.FileFormat;
import com.qlangtech.tis.plugin.datax.server.FTPServer;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.runtime.module.misc.impl.DefaultFieldErrorHandler;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 * @see com.alibaba.datax.plugin.reader.ftpreader.FtpReader
 **/
@Public
public class DataXFtpReader extends DataxReader {
    public static final String DATAX_NAME = "FTP";

    @FormField(ordinal = 1, validate = {Validator.require})
    public FTPServer linker;

    @FormField(ordinal = 7, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.absolute_path})
    public String path;
    @FormField(ordinal = 8, type = FormFieldType.TEXTAREA, validate = {Validator.require})
    public String column;

    @FormField(ordinal = 10, type = FormFieldType.ENUM, validate = {Validator.require})
    public String compress;
    @FormField(ordinal = 11, type = FormFieldType.ENUM, validate = {})
    public String encoding;
    //    @FormField(ordinal = 12, type = FormFieldType.ENUM, validate = {})
//    public Boolean skipHeader;
    @FormField(ordinal = 13, type = FormFieldType.INPUTTEXT, validate = {})
    public String nullFormat;
    @FormField(ordinal = 14, type = FormFieldType.INT_NUMBER, validate = {})
    public String maxTraversalLevel;

    public static List<Option> supportCompress() {
        return Arrays.stream(Compress.values()).map((c) -> new Option(c.name(), c.token)).collect(Collectors.toList());
    }


    @FormField(ordinal = 14, validate = {Validator.require})
    public FileFormat fileFormat;

    @FormField(ordinal = 16, type = FormFieldType.TEXTAREA, advance = false, validate = {Validator.require})
    public String template;

    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXFtpReader.class, "DataXFtpReader-tpl.json");
    }

    @Override
    public boolean hasMulitTable() {
        return false;
    }

    @Override
    public List<ParseColsResult.DataXReaderTabMeta> getSelectedTabs() {
        DefaultContext context = new DefaultContext();
        ParseColsResult parseColsResult = ParseColsResult.parseColsCfg(DataXFtpReaderContext.FTP_TASK,
                new DefaultFieldErrorHandler(), context, StringUtils.EMPTY, this.column);
        if (!parseColsResult.success) {
            throw new IllegalStateException("parseColsResult must be success");
        }
        return Collections.singletonList(parseColsResult.tabMeta);
    }

    @Override
    public IGroupChildTaskIterator getSubTasks() {
        IDataxReaderContext readerContext = new DataXFtpReaderContext(this);
        return IGroupChildTaskIterator.create(readerContext);
        //return Collections.singletonList(readerContext).iterator();
    }

    @Override
    public String getTemplate() {
        return template;
    }


    @TISExtension()
    public static class DefaultDescriptor extends BaseDataxReaderDescriptor {
        public DefaultDescriptor() {
            super();
        }

        @Override
        public boolean isSupportIncr() {
            return false;
        }

        @Override
        public EndType getEndType() {
            return EndType.FTP;
        }

        public boolean validateColumn(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            return ParseColsResult.parseColsCfg(DataXFtpReaderContext.FTP_TASK, msgHandler, context, fieldName, value,
                    (colIndex, col) -> {
                        String colType = ParseColsResult.getColType(col);
                        for (UnstructuredStorageReaderUtil.Type t : UnstructuredStorageReaderUtil.Type.values()) {
                            if (t.name().equalsIgnoreCase(colType)) {
                                return true;
                            }
                        }
                        msgHandler.addFieldError(context, fieldName
                                , "index为" + colIndex + "的字段列中，属性type必须为:"
                                        + Arrays.stream(UnstructuredStorageReaderUtil.Type.values()).map((t) -> String.valueOf(t)).collect(Collectors.joining(",")) + "中之一");
                        return false;
                    }
            ).success;
        }

        public boolean validateCsvReaderConfig(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            return PluginFieldValidators.validateCsvReaderConfig(msgHandler, context, fieldName, value);
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
}
