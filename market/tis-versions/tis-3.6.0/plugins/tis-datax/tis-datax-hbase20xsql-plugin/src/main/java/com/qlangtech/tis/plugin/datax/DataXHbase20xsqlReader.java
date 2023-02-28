

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.IDataxReaderContext;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.mysql.MySQLDataSourceFactory;
import com.qlangtech.tis.util.Memoizer;
import org.apache.commons.lang.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 **/
public class DataXHbase20xsqlReader extends DataxReader {
    private static final String DATAX_NAME = "Hbase20xsql";
    @FormField(ordinal = 0, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String queryServerAddress;
    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {})
    public String serialization;
    @FormField(ordinal = 2, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String table;
    @FormField(ordinal = 3, type = FormFieldType.INPUTTEXT, validate = {})
    public String schema;
    @FormField(ordinal = 4, type = FormFieldType.INPUTTEXT, validate = {})
    public String column;
    @FormField(ordinal = 5, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String splitKey;
    @FormField(ordinal = 6, type = FormFieldType.INPUTTEXT, validate = {})
    public String splitPoints;
    @FormField(ordinal = 7, type = FormFieldType.INPUTTEXT, validate = {})
    public String where;
    @FormField(ordinal = 8, type = FormFieldType.INPUTTEXT, validate = {})
    public String querySql;

    @FormField(ordinal = 9, type = FormFieldType.TEXTAREA,advance = false , validate = {Validator.require})
    public String template;

    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXHbase20xsqlReader.class, "DataXHbase20xsqlReader-tpl.json");
    }


    @Override
    public Iterator<IDataxReaderContext> getSubTasks() {

        MySQLDataSourceFactory dsFactory = (MySQLDataSourceFactory) this.getDataSourceFactory();

        Memoizer<String, List<ColumnMetaData>> tabColsMap = new Memoizer<String, List<ColumnMetaData>>() {
            @Override
            public List<ColumnMetaData> compute(String tab) {
                return dsFactory.getTableMetadata(tab);
            }
        };

        AtomicInteger selectedTabIndex = new AtomicInteger(0);
        AtomicInteger taskIndex = new AtomicInteger(0);
        final int selectedTabsSize = this.selectedTabs.size();

        AtomicReference<Iterator<IDataSourceDumper>> dumperItRef = new AtomicReference<>();

        return new Iterator<IDataxReaderContext>() {
            @Override
            public boolean hasNext() {

                Iterator<IDataSourceDumper> dumperIt = initDataSourceDumperIterator();

                if (dumperIt.hasNext()) {
                    return true;
                } else {
                    if (selectedTabIndex.get() >= selectedTabsSize) {
                        return false;
                    } else {
                        dumperItRef.set(null);
                        initDataSourceDumperIterator();
                        return true;
                    }
                }
            }

            private Iterator<IDataSourceDumper> initDataSourceDumperIterator() {
                Iterator<IDataSourceDumper> dumperIt;
                if ((dumperIt = dumperItRef.get()) == null) {
                    SelectedTab tab = selectedTabs.get(selectedTabIndex.getAndIncrement());
                    if (StringUtils.isEmpty(tab.getName())) {
                        throw new IllegalStateException("tableName can not be null");
                    }
//                    List<ColumnMetaData> tableMetadata = null;
//                    IDataSourceDumper dumper = null;
                    DataDumpers dataDumpers = null;
                    TISTable tisTab = new TISTable();
                    tisTab.setTableName(tab.getName());

                    dataDumpers = dsFactory.getDataDumpers(tisTab);
                    dumperIt = dataDumpers.dumpers;
                    dumperItRef.set(dumperIt);
                }
                return dumperIt;
            }

            @Override
            public IDataxReaderContext next() {
                Iterator<IDataSourceDumper> dumperIterator = dumperItRef.get();
                Objects.requireNonNull(dumperIterator, "dumperIterator can not be null,selectedTabIndex:" + selectedTabIndex.get());
                IDataSourceDumper dumper = dumperIterator.next();
                SelectedTab tab = selectedTabs.get(selectedTabIndex.get() - 1);
                MySQLDataXReaderContext dataxContext = new MySQLDataXReaderContext(tab.getName() + "_" + taskIndex.getAndIncrement(), tab.getName());
                dataxContext.jdbcUrl = dumper.getDbHost();
                dataxContext.tabName = tab.getName();
                dataxContext.username = dsFactory.getUserName();
                dataxContext.password = dsFactory.getPassword();
                dataxContext.setWhere(tab.getWhere());

                List<ColumnMetaData> tableMetadata = tabColsMap.get(tab.getName());
                if (tab.isAllCols()) {
                    dataxContext.cols = tableMetadata.stream().map((t) -> t.getValue()).collect(Collectors.toList());
                } else {
                    dataxContext.cols = tableMetadata.stream().filter((col) -> {
                        return tab.containCol(col.getKey());
                    }).map((t) -> t.getValue()).collect(Collectors.toList());
                }

                return dataxContext;
            }
        };
    }


    @Override
    public String getTemplate() {
        return template;
    }

    public void setSelectedTabs(List<SelectedTab> selectedTabs) {
        this.selectedTabs = selectedTabs;
    }

    @Override
    public List<String> getTablesInDB() {
        DataSourceFactory plugin = getDataSourceFactory();
        return plugin.getTablesInDB();
    }

    protected DataSourceFactory getDataSourceFactory() {
        DataSourceFactoryPluginStore dsStore = TIS.getDataBasePluginStore(new PostedDSProp(this.dbName));
        return dsStore.getPlugin();
    }

    @Override
    public List<ColumnMetaData> getTableMetadata(String table) {
        DataSourceFactory plugin = getDataSourceFactory();
        return plugin.getTableMetadata(table);
    }

    @TISExtension()
    public static class DefaultDescriptor extends Descriptor<DataxReader> {
        public DefaultDescriptor() {
            super();
        }

        @Override
        public String getDisplayName() {
            return DATAX_NAME;
        }
    }
}
