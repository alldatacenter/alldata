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
package com.qlangtech.tis.plugin.ds;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.impl.XmlFile;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.SetPluginsResult;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.util.IPluginContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DataSourceFactoryPluginStore extends KeyedPluginStore<DataSourceFactory> {
    private final boolean shallUpdateDB;

    public DataSourceFactoryPluginStore(Key key, boolean shallUpdateDB) {
        super(key);
        this.shallUpdateDB = shallUpdateDB;
    }

    public DSKey getDSKey() {
        return (DSKey) this.key;
    }

    public FacadeDataSource createFacadeDataSource() throws Exception {
        DataSourceFactory plugin = this.getPlugin();
        if (plugin == null) {
            throw new IllegalStateException("dbName:" + key.keyVal + " relevant facade datasource has not been defined,file:" + this.getSerializeFileName());
        }
        if (!(plugin instanceof IFacadeDataSource)) {
            throw new IllegalStateException("plugin:" + plugin.identityValue() + " is not instanceOf IFacadeDataSource");
        }

        plugin.getTablesInDB();
        return ((IFacadeDataSource) plugin).createFacadeDataSource();
    }

    public <DS extends DataSourceFactory> DS getDataSource() {
        return (DS) super.getPlugin();
    }

    @Override
    public DataSourceFactory getPlugin() {
        DataSourceFactory dsFactory = super.getPlugin();
        return dsFactory;
    }

    public void deleteDB() throws Exception {
        XmlFile targetFile = this.getTargetFile();
        if (getDSKey().isFacadeType()) {
            // 如果删除detail类型的数据库，则只删除facade类型
            FileUtils.forceDelete(targetFile.getFile());
        } else {
            // 如果删除detail类型的数据库，则要把整个数据库目录删除
            FileUtils.deleteDirectory(targetFile.getFile().getParentFile());
        }
    }

    @Override
    public void copyConfigFromRemote() {
        List<String> subFiles
                = CenterResource.getSubFiles(
                Config.KEY_TIS_PLUGIN_CONFIG + File.separator + this.key.getSubDirPath(), false, true);
        for (String f : subFiles) {
            CenterResource.copyFromRemote2Local(
                    Config.KEY_TIS_PLUGIN_CONFIG + File.separator + this.key.getSubDirPath() + File.separator + f, true);
        }
    }

    @Override
    public synchronized SetPluginsResult setPlugins(IPluginContext pluginContext, Optional<Context> context
            , List<Descriptor.ParseDescribable<DataSourceFactory>> dlist, boolean update) {
        if (dlist.size() != 1) {
            throw new IllegalArgumentException("size of dlist must equal to 1");
        }
        if (!context.isPresent()) {
            throw new IllegalArgumentException("Context shall exist");
        }
        Context ctx = context.get();
        final String dbName = this.key.keyVal.getVal();
        SetPluginsResult result = super.setPlugins(pluginContext, context, dlist, update);
        if (!result.success) {
            return result;
        }
        Descriptor.ParseDescribable<DataSourceFactory> dbDesc = dlist.get(0);
        pluginContext.addDb(dbDesc, dbName, ctx, (shallUpdateDB && !update));
        return new SetPluginsResult(!ctx.hasErrors(), result.cfgChanged);
    }


//    /**
//     * Save the table metadata info which will be used in dataflow define process
//     *
//     * @param tableName
//     * @throws Exception
//     */
//    public TableReflect saveTable(String tableName) throws Exception {
//        List<ColumnMetaData> colMetas = this.getPlugin().getTableMetadata(EntityName.parse(tableName));
//        return this.saveTable(tableName, colMetas);
//    }


//    private TableReflect saveTable(String tableName, List<ColumnMetaData> colMetas) throws Exception {
//        if (CollectionUtils.isEmpty(colMetas)) {
//            throw new IllegalStateException("tableName:" + tableName + " relevant colMetas can not be empty");
//        }
//        StringBuffer extractSQL = ColumnMetaData.buildExtractSQL(tableName, colMetas);
//        XmlFile configFile = getTableReflectSerializer(tableName);
//
//        TableReflect reflectTab = new TableReflect();
//        reflectTab.setSql(extractSQL.toString());
//        reflectTab.setCols(colMetas);
//        configFile.write(reflectTab, Collections.emptySet());
//        return reflectTab;
//    }

//    private XmlFile getTableReflectSerializer(String tableName) {
//        if (StringUtils.isEmpty(tableName)) {
//            throw new IllegalArgumentException("param tableName can not be empty");
//        }
//        String dbRoot = StringUtils.substringBeforeLast(this.getSerializeFileName(), File.separator);
//        return Descriptor.getConfigFile(dbRoot + File.separator + tableName);
//    }


//    public TISTable loadTableMeta(String tableName) {
//        try {
//            TISTable tisTable = new TISTable();
//            XmlFile tableReflectSerializer = this.getTableReflectSerializer(tableName);
//            if (!tableReflectSerializer.exists()) {
//                throw new IllegalStateException("file is not exist:" + tableReflectSerializer.getFile() + " for table:" + tableName);
//            }
//            TableReflect tableMeta = (TableReflect) tableReflectSerializer.read();
//            tisTable.setReflectCols(tableMeta.getCols());
//            tisTable.setSelectSql(tableMeta.getSql());
//            tisTable.setTableName(tableName);
//            tisTable.setDbName(this.key.keyVal.getVal());
//            return tisTable;
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

}
