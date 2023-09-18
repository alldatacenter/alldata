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

package com.qlangtech.tis.plugin.datax.common;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.IGroupChildTaskIterator;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.lang.TisException;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.SubForm;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.util.IPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-05 09:54
 **/
public abstract class BasicDataXRdbmsReader<DS extends DataSourceFactory>
        extends DataxReader implements IDataSourceFactoryGetter, KeyedPluginStore.IPluginKeyAware {

    private static final Logger logger = LoggerFactory.getLogger(BasicDataXRdbmsReader.class);
    @FormField(ordinal = 0, type = FormFieldType.ENUM, validate = {Validator.require})
    public String dbName;

    @FormField(ordinal = 98, type = FormFieldType.INT_NUMBER, validate = {Validator.require})
    public Integer fetchSize;

    @FormField(ordinal = 99, type = FormFieldType.TEXTAREA, advance = false, validate = {Validator.require})
    public String template;

    @SubForm(desClazz = SelectedTab.class
            , idListGetScript = "return com.qlangtech.tis.coredefine.module.action.DataxAction.getTablesInDB(filter);", atLeastOne = true)
    public transient List<SelectedTab> selectedTabs;

    private transient int preSelectedTabsHash;
    public String dataXName;

    // @Override
    public Integer getRowFetchSize() {
        return this.fetchSize;
    }

    @Override
    public final List<SelectedTab> getSelectedTabs() {

        if (selectedTabs == null) {
            return Collections.emptyList();
        }

        if (this.preSelectedTabsHash == selectedTabs.hashCode()) {
            return selectedTabs;
        }

        try (TableColsMeta tabsMeta = getTabsMeta()) {

            this.selectedTabs = this.selectedTabs.stream().map((tab) -> {
                ColumnMetaData.fillSelectedTabMeta(tab, (t) -> {
                    return tabsMeta.get(t.getName());
                });
                return tab;
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.preSelectedTabsHash = selectedTabs.hashCode();
        return this.selectedTabs;

    }


    protected abstract RdbmsReaderContext createDataXReaderContext(String jobName, SelectedTab tab, IDataSourceDumper dumper);


    @Override
    public void setKey(KeyedPluginStore.Key key) {
        this.dataXName = key.keyVal.getVal();
    }

//    @Override
//    public DBConfig getDbConfig() {
//        return getBasicDataSource().getDbConfig();
//    }
//
//    @Override
//    public BasicDataSourceFactory getBasicDataSource() {
//        return (BasicDataSourceFactory) getDataSourceFactory();
//    }

    @Override
    public final IGroupChildTaskIterator getSubTasks(Predicate<ISelectedTab> filter) {
        Objects.requireNonNull(this.selectedTabs, "selectedTabs can not be null");
        List<SelectedTab> tabs = this.selectedTabs.stream().filter(filter).collect(Collectors.toList());
        // DS dsFactory = this.getDataSourceFactory();

        TableColsMeta tabColsMap = getTabsMeta();

        return new DataXRdbmsGroupChildTaskIterator(this, this.isFilterUnexistCol(), tabs, tabColsMap);

//        AtomicInteger selectedTabIndex = new AtomicInteger(0);
//        AtomicInteger taskIndex = new AtomicInteger(0);
//
//        final int selectedTabsSize = tabs.size();
//        ConcurrentHashMap<String, List<DataXCfgGenerator.DBDataXChildTask>> groupedInfo = new ConcurrentHashMap();
//        AtomicReference<Iterator<IDataSourceDumper>> dumperItRef = new AtomicReference<>();
//
//        return new IGroupChildTaskIterator() {
//            @Override
//            public Map<String, List<DataXCfgGenerator.DBDataXChildTask>> getGroupedInfo() {
//                return groupedInfo;
//            }
//
//            @Override
//            public boolean hasNext() {
//
//                Iterator<IDataSourceDumper> dumperIt = initDataSourceDumperIterator();
//                if (dumperIt.hasNext()) {
//                    return true;
//                } else {
//                    if (selectedTabIndex.get() >= selectedTabsSize) {
//                        return false;
//                    } else {
//                        dumperItRef.set(null);
//                        initDataSourceDumperIterator();
//                        return true;
//                    }
//                }
//            }
//
//            private Iterator<IDataSourceDumper> initDataSourceDumperIterator() {
//                Iterator<IDataSourceDumper> dumperIt;
//                if ((dumperIt = dumperItRef.get()) == null) {
//                    SelectedTab tab = tabs.get(selectedTabIndex.getAndIncrement());
//                    if (StringUtils.isEmpty(tab.getName())) {
//                        throw new IllegalStateException("tableName can not be null");
//                    }
////                    List<ColumnMetaData> tableMetadata = null;
////                    IDataSourceDumper dumper = null;
//                    DataDumpers dataDumpers = null;
//                    TISTable tisTab = new TISTable();
//                    tisTab.setTableName(tab.getName());
//                    int[] index = {0};
//                    tisTab.setReflectCols(tab.getCols().stream().map((c) -> {
//                        return createColumnMetaData(index, c.getName());
//                    }).collect(Collectors.toList()));
//
//                    dataDumpers = dsFactory.getDataDumpers(tisTab);
//                    dumperIt = dataDumpers.dumpers;
//                    dumperItRef.set(dumperIt);
//                }
//                return dumperIt;
//            }
//
//            @Override
//            public IDataxReaderContext next() {
//                Iterator<IDataSourceDumper> dumperIterator = dumperItRef.get();
//                Objects.requireNonNull(dumperIterator, "dumperIterator can not be null,selectedTabIndex:" + selectedTabIndex.get());
//                IDataSourceDumper dumper = dumperIterator.next();
//                SelectedTab tab = tabs.get(selectedTabIndex.get() - 1);
//                String childTask = tab.getName() + "_" + taskIndex.getAndIncrement();
//                List<DataXCfgGenerator.DBDataXChildTask> childTasks
//                        = groupedInfo.computeIfAbsent(tab.getName(), (tabname) -> Lists.newArrayList());
//                childTasks.add(new DataXCfgGenerator.DBDataXChildTask(dumper.getDbHost(), childTask));
//                RdbmsReaderContext dataxContext = createDataXReaderContext(childTask, tab, dumper);
//
//                dataxContext.setWhere(tab.getWhere());
//
//                if (isFilterUnexistCol()) {
//                    Map<String, ColumnMetaData> tableMetadata = tabColsMap.get(tab.getName());
//
//                    dataxContext.setCols(tab.cols.stream()
//                            .filter((c) -> tableMetadata.containsKey(c)).collect(Collectors.toList()));
//                } else {
//                    dataxContext.setCols(tab.cols);
//                }
//                return dataxContext;
//            }
//        };
    }

//    public static ColumnMetaData createColumnMetaData(int[] index, String colName) {
//        return new ColumnMetaData(index[0]++, colName, new DataType(-999), false, true);
//    }

    protected boolean isFilterUnexistCol() {
        return false;
    }


    private TableColsMeta getTabsMeta() {


        return new TableColsMeta(getDataSourceFactory(), this.dbName);


//        return new Memoizer<String, Map<String, ColumnMetaData>>() {
//            @Override
//            public Map<String, ColumnMetaData> compute(String tab) {
//
//
//                Objects.requireNonNull(datasource, "ds:" + dbName + " relevant DataSource can not be find");
//
//                try {
//                    return datasource.getTableMetadata(conn.get(), EntityName.parse(tab))
//                            .stream().collect(
//                                    Collectors.toMap(
//                                            (m) -> m.getKey()
//                                            , (m) -> m
//                                            , (c1, c2) -> c1));
//                } catch (TableNotFoundException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        };
    }


    @Override
    public final String getTemplate() {
        return template;
    }

    public final void setSelectedTabs(List<SelectedTab> selectedTabs) {
        this.selectedTabs = selectedTabs;
    }

    @Override
    public final TableInDB getTablesInDB() {
        DataSourceFactory plugin = getDataSourceFactory();
        return plugin.getTablesInDB();
    }

    @Override
    public final void refresh() {
        getDataSourceFactory().refresh();
    }

    @Override
    public DS getDataSourceFactory() {
        return TIS.getDataBasePlugin(PostedDSProp.parse(this.dbName));
        //  return (DS) dsStore.getPlugin();
    }

    public final List<ColumnMetaData> getTableMetadata(EntityName table) throws TableNotFoundException {
        return this.getTableMetadata(false, table);
    }

    @Override
    public final List<ColumnMetaData> getTableMetadata(boolean inSink, EntityName table) throws TableNotFoundException {
        DataSourceFactory plugin = getDataSourceFactory();
        return plugin.getTableMetadata(inSink, table);
    }

//    /**
//     * 取表的主键
//     *
//     * @param table
//     * @return
//     */
//    public List<ColumnMetaData> getPrimaryKeys(String table) {
//        return this.getTableMetadata(table).stream()
//                .filter((col) -> col.isPk()).collect(Collectors.toList());
//    }
//
//
//    public List<ColumnMetaData> getPartitionKeys(String table) {
//        return this.getTableMetadata(table).stream()
//                .filter((col) -> {
//                    switch (col.getType().getCollapse()) {
//                        // case STRING:
//                        case INT:
//                        case Long:
//                        case Date:
//                            return true;
//                    }
//                    return false;
//                }).collect(Collectors.toList());
//    }


    @Override
    protected Class<BasicDataXRdbmsReaderDescriptor> getExpectDescClass() {
        return BasicDataXRdbmsReaderDescriptor.class;
    }

    public static abstract class BasicDataXRdbmsReaderDescriptor extends DataxReader.BaseDataxReaderDescriptor
            implements FormFieldType.IMultiSelectValidator {
        public BasicDataXRdbmsReaderDescriptor() {
            super();
        }

        @Override
        public final boolean isRdbms() {
            return true;
        }

        public boolean validateFetchSize(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            try {
                int fetchSize = Integer.parseInt(value);
                if (fetchSize < 1) {
                    msgHandler.addFieldError(context, fieldName, "不能小于1");
                }
                if (fetchSize > 2048) {
                    msgHandler.addFieldError(context, fieldName, "不能大于2048,以免进程OOM");
                    return false;
                }
            } catch (Throwable e) {
                msgHandler.addFieldError(context, fieldName, e.getMessage());
            }
            return true;
        }


        @Override
        protected boolean validateAll(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {

            try {
                ParseDescribable<Describable> readerDescribable
                        = this.newInstance((IPluginContext) msgHandler, postFormVals.rawFormData, Optional.empty());
                BasicDataXRdbmsReader rdbmsReader = readerDescribable.getInstance();
                rdbmsReader.getTablesInDB();
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
                // msgHandler.addErrorMessage(context, );
                msgHandler.addFieldError(context, BasicDataXRdbmsWriter.KEY_DB_NAME_FIELD_NAME, "数据源连接不正常," + TisException.getErrMsg(e));
                return false;
            }

            return true;
        }

        @Override
        public boolean validate(IFieldErrorHandler msgHandler, Optional<IPropertyType.SubFormFilter> subFormFilter
                , Context context, String fieldName, List<FormFieldType.SelectedItem> items) {

            return true;
        }


    }
}
