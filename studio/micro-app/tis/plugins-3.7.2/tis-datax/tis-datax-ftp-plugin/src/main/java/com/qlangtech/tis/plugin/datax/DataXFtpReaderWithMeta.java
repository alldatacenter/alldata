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
import com.alibaba.datax.plugin.ftp.common.FtpHelper;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.IDataxReaderContext;
import com.qlangtech.tis.datax.IGroupChildTaskIterator;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.annotation.SubForm;
import com.qlangtech.tis.plugin.datax.common.PluginFieldValidators;
import com.qlangtech.tis.plugin.datax.meta.DefaultMetaDataWriter;
import com.qlangtech.tis.plugin.datax.server.FTPServer;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.TableNotFoundException;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 导入FTP上有符合TIS要求的Metadata元数据文件的 FTP文件
 *
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 * @see com.alibaba.datax.plugin.reader.ftpreader.FtpReader
 * @see com.qlangtech.tis.plugin.datax.meta.DefaultMetaDataWriter
 **/
@Public
public class DataXFtpReaderWithMeta extends DataXFtpReader {


    @SubForm(desClazz = SelectedTab.class
            , idListGetScript = "return com.qlangtech.tis.plugin.datax.DataXFtpReaderWithMeta.getFTPFiles(filter);", atLeastOne = true)
    public transient List<SelectedTab> selectedTabs;

    @Override
    public List<ColumnMetaData> getTableMetadata(boolean inSink,EntityName table) throws TableNotFoundException {
        FTPServer server = FTPServer.getServer(this.linker);
        return server.useFtpHelper((ftp) -> {
            return getFTPFileMetaData(table, ftp);
        });
    }

    private List<ColumnMetaData> getFTPFileMetaData(EntityName table, FtpHelper ftp) {
        String content = null;
        final String ftpPath = this.path + IOUtils.DIR_SEPARATOR
                + table.getTabName() + IOUtils.DIR_SEPARATOR + FtpHelper.KEY_META_FILE;

        try (InputStream reader = Objects.requireNonNull(ftp.getInputStream(ftpPath), "path:" + ftpPath + " relevant InputStream can not null")) {
            content = IOUtils.toString(reader, TisUTF8.get());

            JSONArray fields = JSONArray.parseArray(content);
            return DefaultMetaDataWriter.deserialize(fields);
        } catch (Exception e) {
            throw new RuntimeException(content, e);
        }
    }

    @Override
    public IGroupChildTaskIterator getSubTasks(Predicate<ISelectedTab> filter) {

        final List<SelectedTab> tabs = selectedTabs;
        final int tabsLength = tabs.size();
        AtomicInteger selectedTabIndex = new AtomicInteger(0);
        ConcurrentHashMap<String, List<DataXCfgGenerator.DBDataXChildTask>> groupedInfo = new ConcurrentHashMap();

        FTPServer server = FTPServer.getServer(this.linker);

        final FtpHelper ftpHelper = server.createFtpHelper(server.timeout);

        return new IGroupChildTaskIterator() {
            int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return ((currentIndex = selectedTabIndex.getAndIncrement()) < tabsLength);
            }

            @Override
            public IDataxReaderContext next() {
                SelectedTab tab = tabs.get(currentIndex);

                ColumnMetaData.fillSelectedTabMeta(tab, (t) -> {
                    List<ColumnMetaData> colsMeta = getFTPFileMetaData(EntityName.parse(t.getName()), ftpHelper);
                    return colsMeta.stream().collect(Collectors.toMap((c) -> c.getKey(), (c) -> c));
                });

                List<DataXCfgGenerator.DBDataXChildTask> childTasks
                        = groupedInfo.computeIfAbsent(tab.getName(), (tabname) -> Lists.newArrayList());
                String childTask = tab.getName() + "_" + currentIndex;

                childTasks.add(new DataXCfgGenerator.DBDataXChildTask(StringUtils.EMPTY
                        , DataXFtpReaderContext.FTP_TASK, childTask));

                return new DataXFtpSelectTableReaderContext(DataXFtpReaderWithMeta.this, tab, currentIndex);
            }

            @Override
            public Map<String, List<DataXCfgGenerator.DBDataXChildTask>> getGroupedInfo() {
                return groupedInfo;
            }

            @Override
            public void close() throws IOException {
                try {
                    ftpHelper.close();
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        };
    }


    @Override
    public boolean hasMulitTable() {
        return CollectionUtils.isNotEmpty(this.selectedTabs);// getSelectedTabs().size() > 0;;
    }


    @Override
    public List<ISelectedTab> getSelectedTabs() {
        return this.selectedTabs.stream().collect(Collectors.toList());
    }

    public static final Pattern FTP_FILE_PATTERN
            = Pattern.compile(".+?([^/]+)" + IOUtils.DIR_SEPARATOR + StringUtils.replace(FtpHelper.KEY_META_FILE, ".", "\\."));

    public static List<String> getFTPFiles(IPropertyType.SubFormFilter filter) {
        DataXFtpReaderWithMeta reader = DataxReader.getDataxReader(filter);
        return getFTPFiles(reader);
    }


    public static List<String> getFTPFiles(DataXFtpReaderWithMeta reader) {


        FTPServer server = FTPServer.getServer(reader.linker);
        return server.useFtpHelper((ftp) -> {
            List<String> ftpFiles = Lists.newArrayList();
            Matcher matcher = null;
            HashSet<String> files = ftp.getListFiles(reader.path, 0, 2);
            for (String file : files) {
                matcher = FTP_FILE_PATTERN.matcher(file);
                if (matcher.matches()) {
                    ftpFiles.add(matcher.group(1));
                }
            }
            return ftpFiles;
        });
    }


    @TISExtension()
    public static class DefaultDescriptor extends DataXFtpReader.DefaultDescriptor {
        public DefaultDescriptor() {
            super();
        }

        public boolean validateColumn(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            return true;
        }

        public boolean validateCsvReaderConfig(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            return PluginFieldValidators.validateCsvReaderConfig(msgHandler, context, fieldName, value);
        }

        @Override
        public boolean isRdbms() {
            return true;
        }

        @Override
        protected boolean validateAll(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            return this.verify(msgHandler, context, postFormVals);
        }

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            DataXFtpReaderWithMeta dataxReader = (DataXFtpReaderWithMeta) postFormVals.newInstance(this, msgHandler);

            List<String> ftpFiles = DataXFtpReaderWithMeta.getFTPFiles(dataxReader);
            if (CollectionUtils.isEmpty(ftpFiles)) {
                msgHandler.addFieldError(context, KEY_FIELD_PATH, "该路径下未扫描到" + DATAX_NAME + "元数据文件:" + FtpHelper.KEY_META_FILE);
                return false;
            }

            return true;
        }

        @Override
        public String getDisplayName() {
            return super.getDisplayName() + "-Meta";
        }
    }
}
