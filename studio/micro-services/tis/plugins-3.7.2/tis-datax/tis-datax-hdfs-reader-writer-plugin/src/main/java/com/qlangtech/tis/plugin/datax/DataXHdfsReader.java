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
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.IDataxReaderContext;
import com.qlangtech.tis.datax.IGroupChildTaskIterator;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.fs.ITISFileSystemFactory;
import com.qlangtech.tis.hdfs.impl.HdfsFileSystemFactory;
import com.qlangtech.tis.offline.FileSystemFactory;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.ValidatorCommons;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.common.PluginFieldValidators;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 * @see com.qlangtech.tis.plugin.datax.TisDataXHdfsReader
 **/
@Public
public class DataXHdfsReader extends DataxReader implements KeyedPluginStore.IPluginKeyAware {

    @FormField(ordinal = 0, type = FormFieldType.SELECTABLE, validate = {Validator.require})
    public String fsName;

    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String path;
    //    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String defaultFS;
    @FormField(ordinal = 2, type = FormFieldType.ENUM, validate = {Validator.require})
    public String fileType;
    @FormField(ordinal = 3, type = FormFieldType.TEXTAREA, validate = {Validator.require})
    public String column;

    @FormField(ordinal = 4, type = FormFieldType.INPUTTEXT, validate = {})
    public String fieldDelimiter;

    @FormField(ordinal = 5, type = FormFieldType.ENUM, validate = {})
    public String encoding;

    @FormField(ordinal = 6, type = FormFieldType.INPUTTEXT, validate = {})
    public String nullFormat;

//    @FormField(ordinal = 7, type = FormFieldType.INPUTTEXT, validate = {})
//    public String haveKerberos;
//
//    @FormField(ordinal = 8, type = FormFieldType.INPUTTEXT, validate = {})
//    public String kerberosKeytabFilePath;
//
//    @FormField(ordinal = 9, type = FormFieldType.INPUTTEXT, validate = {})
//    public String kerberosPrincipal;

    @FormField(ordinal = 10, type = FormFieldType.ENUM, validate = {})
    public String compress;

//    @FormField(ordinal = 11, type = FormFieldType.INPUTTEXT, validate = {})
//    public String hadoopConfig;

    @FormField(ordinal = 12, type = FormFieldType.TEXTAREA, validate = {})
    public String csvReaderConfig;

    @FormField(ordinal = 13, type = FormFieldType.TEXTAREA, advance = false, validate = {Validator.require})
    public String template;

    public String dataXName;

    @Override
    public void setKey(KeyedPluginStore.Key key) {
        this.dataXName = key.keyVal.getVal();
    }

    private HdfsFileSystemFactory fileSystem;

    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXHdfsReader.class, "DataXHdfsReader-tpl.json");
    }

    @Override
    public boolean hasMulitTable() {
        return false;
    }


    @Override
    public List<ParseColsResult.DataXReaderTabMeta> getSelectedTabs() {
        DefaultContext context = new DefaultContext();
        ParseColsResult parseOSSColsResult
                = ParseColsResult.parseColsCfg(this.dataXName, new MockFieldErrorHandler(), context, StringUtils.EMPTY, this.column);
        if (!parseOSSColsResult.success) {
            throw new IllegalStateException("parseOSSColsResult must be success");
        }
        return Collections.singletonList(parseOSSColsResult.tabMeta);

    }

    private static class MockFieldErrorHandler implements IFieldErrorHandler {
        @Override
        public void addFieldError(Context context, String fieldName, String msg, Object... params) {
        }

        @Override
        public boolean validateBizLogic(BizLogic logicType, Context context, String fieldName, String value) {
            return false;
        }
    }

    @Override
    public IGroupChildTaskIterator getSubTasks(Predicate<ISelectedTab> filter) {
        IDataxReaderContext readerContext = new HdfsReaderContext(this);
        return IGroupChildTaskIterator.create(readerContext);
    }

    public HdfsFileSystemFactory getFs() {
        if (fileSystem == null) {
            this.fileSystem = (HdfsFileSystemFactory) FileSystemFactory.getFsFactory(fsName);
        }
        Objects.requireNonNull(this.fileSystem, "fileSystem has not be initialized");
        return fileSystem;
    }


    @Override
    public String getTemplate() {
        return template;
    }

//    @Override
//    public List<String> getTablesInDB() {
//        throw new UnsupportedOperationException();
//    }


    @TISExtension()
    public static class DefaultDescriptor extends BaseDataxReaderDescriptor {
        private static final Pattern PATTERN_HDFS_RELATIVE_PATH = Pattern.compile("([\\w\\d\\.\\-_=]+/)*([\\w\\d\\.\\-_=]+|(\\*))");

        public DefaultDescriptor() {
            super();
            this.registerSelectOptions(ITISFileSystemFactory.KEY_FIELD_NAME_FS_NAME
                    , () -> TIS.getPluginStore(FileSystemFactory.class)
                            .getPlugins().stream().filter(((f) -> f instanceof HdfsFileSystemFactory)).collect(Collectors.toList()));
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

        public boolean validateColumn(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            return ParseColsResult.parseColsCfg(StringUtils.EMPTY, msgHandler, context, fieldName, value).success;
        }

        public boolean validatePath(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {

            Matcher matcher = PATTERN_HDFS_RELATIVE_PATH.matcher(value);
            if (!matcher.matches()) {
                msgHandler.addFieldError(context, fieldName, ValidatorCommons.MSG_RELATIVE_PATH_ERROR + ":" + PATTERN_HDFS_RELATIVE_PATH);
                return false;
            }

            return true;
        }


        public boolean validateCsvReaderConfig(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            return PluginFieldValidators.validateCsvReaderConfig(msgHandler, context, fieldName, value);
        }

        @Override
        public String getDisplayName() {
            return DataXHdfsWriter.DATAX_NAME;
        }
    }
}
