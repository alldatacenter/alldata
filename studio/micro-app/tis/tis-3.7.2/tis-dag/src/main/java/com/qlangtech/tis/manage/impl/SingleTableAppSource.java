///**
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.qlangtech.tis.manage.impl;
//
//import com.alibaba.citrus.turbine.Context;
//import com.google.common.collect.Maps;
//import com.qlangtech.tis.TIS;
//import com.qlangtech.tis.assemble.FullbuildPhase;
//import com.qlangtech.tis.compiler.streamcode.IDBTableNamesGetter;
//import com.qlangtech.tis.exec.ExecuteResult;
//import com.qlangtech.tis.exec.IExecChainContext;
//import com.qlangtech.tis.exec.ITaskPhaseInfo;
//import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
//import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
//import com.qlangtech.tis.fullbuild.taskflow.DataflowTask;
//import com.qlangtech.tis.fullbuild.taskflow.TISReactor;
//import com.qlangtech.tis.manage.ISingleTableAppSource;
//import com.qlangtech.tis.manage.ISolrAppSource;
//import com.qlangtech.tis.plugin.StoreResourceType;
//import com.qlangtech.tis.plugin.ds.*;
//import com.qlangtech.tis.runtime.module.misc.IMessageHandler;
//import com.qlangtech.tis.sql.parser.DBNode;
//import com.qlangtech.tis.sql.parser.er.TableMeta;
//import com.qlangtech.tis.sql.parser.er.*;
//import com.qlangtech.tis.sql.parser.meta.DependencyNode;
//import com.qlangtech.tis.sql.parser.meta.TabExtraMeta;
//import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
//import com.qlangtech.tis.sql.parser.tuple.creator.IEntityNameGetter;
//import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
//import com.qlangtech.tis.sql.parser.tuple.creator.IValChain;
//import com.qlangtech.tis.workflow.pojo.DatasourceDb;
//import com.qlangtech.tis.workflow.pojo.DatasourceTable;
//
//import java.util.*;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2021-03-31 11:20
// */
//public class SingleTableAppSource implements ISolrAppSource, IStreamIncrGenerateStrategy, ISingleTableAppSource {
//    private final DatasourceDb db;
//    //    private final DatasourceTable table;
//    private final Integer tabId;
//    private final String tabName;
//
//    public SingleTableAppSource(DatasourceDb db, DatasourceTable table) {
//        Objects.requireNonNull(db, "db can not be null");
//        Objects.requireNonNull(table, "table can not be null");
//        this.db = db;
//        this.tabId = table.getId();
//        this.tabName = table.getName();
//    }
//
//    public Integer getTabId() {
//        return tabId;
//    }
//
////    @Override
////    public <T> T accept(ISolrAppSourceVisitor<T> visitor) {
////        return visitor.visit(this);
////    }
//
//    @Override
//    public StoreResourceType getResType() {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public <T> T accept(IAppSourceVisitor<T> visitor) {
//        return visitor.visit(this);
//    }
//
//    @Override
//    public boolean isExcludeFacadeDAOSupport() {
//        return true;
//    }
//
//    @Override
//    public List<ColumnMetaData> reflectCols() {
//
//        try {
//            DataSourceFactory dataBase = TIS.getDataBasePlugin(new PostedDSProp(DBIdentity.parseId(db.getName())));
//            return dataBase.getTableMetadata(EntityName.parse(tabName));
//        } catch (TableNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//        //    TISTable table = dataBasePluginStore.loadTableMeta(tabName);
////    table.getReflectCols().stream().map((c)->{
////      ColName cname = new ColName(c.getKey());
////
////    });
//        //   return table.getReflectCols();
//    }
//
//    @Override
//    public ExecuteResult getProcessDataResults(IExecChainContext execChainContext, ISingleTableDumpFactory singleTableDumpFactory
//            , IDataProcessFeedback dataProcessFeedback, ITaskPhaseInfo taskPhaseInfo) throws Exception {
//        // 复杂数据导出
//
//        DumpPhaseStatus dumpPhaseStatus = taskPhaseInfo.getPhaseStatus(execChainContext, FullbuildPhase.FullDump);
//        List<DataflowTask> tabDump = null;
//
//        DependencyNode dump = new DependencyNode();
//        dump.setId(db.getName() + "." + tabName);
//        dump.setName(tabName);
//        dump.setDbName(db.getName());
//        dump.setTabid(String.valueOf(tabId));
//        dump.setDbid(String.valueOf(db.getId()));
//        Map<String, TISReactor.TaskAndMilestone> /*** taskid*/taskMap = Maps.newHashMap();
//        //for (DependencyNode dump : topology.getDumpNodes()) {
//        tabDump = singleTableDumpFactory.createSingleTableDump(dump, false, /* isHasValidTableDump */
//                "tableDump.getPt()", execChainContext.getZkClient(), execChainContext, dumpPhaseStatus, taskPhaseInfo, taskMap);
//
//        tabDump.forEach((task) -> {
//            try {
//                task.run();
//            } catch (Exception e) {
//            }
//        });
//
//        return ExecuteResult.SUCCESS;
//    }
//
//    @Override
//    public IPrimaryTabFinder getPrimaryTabFinder() {
//        return new DataFlowAppSource.DftTabFinder();
//    }
//
//    @Override
//    public EntityName getTargetEntity() {
//        return EntityName.parse(db.getName() + "." + this.tabName);
//    }
//
//    @Override
//    public boolean triggerFullIndexSwapeValidate(IMessageHandler msgHandler, Context ctx) {
//        return true;
//    }
//
//
//    //@Override
//    public Map<IEntityNameGetter, List<IValChain>> getTabTriggerLinker() {
//        return Collections.emptyMap();
//    }
//
//    // @Override
//    public Map<DBNode, List<String>> getDependencyTables(IDBTableNamesGetter dbTableNamesGetter) {
//        return Collections.emptyMap();
//    }
//
//    // @Override
//    public IERRules getERRule() {
//        return new SingleTableErRule();
//    }
//
//    private class SingleTableErRule implements IERRules {
//        @Override
//        public List<PrimaryTableMeta> getPrimaryTabs() {
//            TabExtraMeta tabExtraMeta = new TabExtraMeta();
//            tabExtraMeta.setPrimaryIndexTab(true);
//
//            PrimaryTableMeta tableMeta = new PrimaryTableMeta(tabName, tabExtraMeta);
//
//            return Collections.singletonList(tableMeta);
//        }
//
//        @Override
//        public boolean isTriggerIgnore(EntityName entityName) {
//            return false;
//        }
//
//        @Override
//        public List<TableRelation> getAllParent(EntityName entityName) {
//            return Collections.emptyList();
//        }
//
//        @Override
//        public List<TableRelation> getChildTabReference(EntityName entityName) {
//            return Collections.emptyList();
//        }
//
//        @Override
//        public Optional<TableMeta> getPrimaryTab(IDumpTable entityName) {
//            return Optional.empty();
//        }
//
//        @Override
//        public boolean hasSetTimestampVerColumn(EntityName entityName) {
//            return false;
//        }
//
//        @Override
//        public TimeCharacteristic getTimeCharacteristic() {
//            return TimeCharacteristic.ProcessTime;
//        }
//
//        @Override
//        public boolean isTimestampVerColumn(EntityName entityName, String name) {
//            return false;
//        }
//
//        @Override
//        public String getTimestampVerColumn(EntityName entityName) {
//            return null;
//        }
//
//        @Override
//        public List<TabFieldProcessor> getTabFieldProcessors() {
//            return Collections.emptyList();
//        }
//
//        @Override
//        public Optional<TableRelation> getFirstParent(String tabName) {
//            return Optional.empty();
//        }
//
//        @Override
//        public Optional<PrimaryTableMeta> isPrimaryTable(String tabName) {
//            return Optional.empty();
//        }
//    }
//}
