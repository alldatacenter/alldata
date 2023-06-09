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

package com.qlangtech.tis.plugin.ds.split;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.datax.DataXJobInfo;
import com.qlangtech.tis.datax.DataXJobSubmit;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.ds.DBIdentity;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.SplitTableStrategy;
import com.qlangtech.tis.plugin.ds.TableInDB;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认分表策略
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-12-17 21:23
 **/
public class DefaultSplitTableStrategy extends SplitTableStrategy {
    private static final Logger logger = LoggerFactory.getLogger(DefaultSplitTableStrategy.class);

    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {})
    public String tabPattern;

    @Override
    public TableInDB createTableInDB(DBIdentity id) {
        return new SplitableTableInDB(id, getTabPattern());
    }

    public static String tabPatternPlaceholder() {
        return SplitTableStrategy.PATTERN_PHYSICS_TABLE.toString();
    }

    private Pattern getTabPattern() {
        return StringUtils.isNotBlank(this.tabPattern)
                ? Pattern.compile(this.tabPattern)
                : SplitTableStrategy.PATTERN_PHYSICS_TABLE;
    }

    @Override
    public List<String> getAllPhysicsTabs(DataSourceFactory dsFactory, String jdbcUrl, String sourceTableName) {
        TableInDB tablesInDB = dsFactory.getTablesInDB();
        if (!(tablesInDB instanceof SplitableTableInDB)) {
            if (dsFactory instanceof IRefreshable) {
                ((IRefreshable) dsFactory).refresh();
            }
            tablesInDB = dsFactory.getTablesInDB();
            if (!(tablesInDB instanceof SplitableTableInDB)) {
                throw new IllegalStateException("dbId:" + dsFactory.identityValue()
                        + " relevant TableInDB must be " + SplitableTableInDB.class.getName()
                        + ",but now is " + tablesInDB.getClass().getName());
            }
        }
        SplitableTableInDB tabsMapper = (SplitableTableInDB) tablesInDB;

        SplitableDB physics = tabsMapper.tabs.get(sourceTableName);
        if (physics != null) {
//            for (String ptab : ) {
//                return new DBPhysicsTable(jdbcUrl, EntityName.parse(ptab));
//            }
            List<String> ptabs = physics.getTabsInDB(jdbcUrl);
            if (CollectionUtils.isEmpty(ptabs)) {
                throw new IllegalStateException("jdbcUrl:" + jdbcUrl
                        + "\n,logicTable:" + sourceTableName
                        + "\n,dsFactory:" + dsFactory.identityValue()
                        + "\n relevant physicsTab can not be empty"
                        + "\n exist keys:" + String.join(",", physics.physicsTabInSplitableDB.keySet()));
            }
            return ptabs;
        }
        throw new IllegalStateException(sourceTableName + " relevant physics tabs can not be null");
    }


    /**
     * 一个逻辑表如果对应有多个物理分区表，则只取第一个匹配的物理表
     *
     * @param dsFactory
     * @param tabName
     * @return
     */
    @Override
    public DBPhysicsTable getMatchedPhysicsTable(DataSourceFactory dsFactory, String jdbcUrl, EntityName tabName) {
        //try {
//        TableInDB tablesInDB = dsFactory.getTablesInDB();// tabsInDBCache.get(dsFactory.identityValue());
//        if (!(tablesInDB instanceof SplitableTableInDB)) {
//            dsFactory.refresh();
//            tablesInDB = dsFactory.getTablesInDB();
//            if (!(tablesInDB instanceof SplitableTableInDB)) {
//                throw new IllegalStateException("dbId:" + dsFactory.identityValue()
//                        + " relevant TableInDB must be " + SplitableTableInDB.class.getName()
//                        + ",but now is " + tablesInDB.getClass().getName());
//            }
//        }
//        SplitableTableInDB tabsMapper = (SplitableTableInDB) tablesInDB;
        List<String> allPhysicsTabs = getAllPhysicsTabs(dsFactory, jdbcUrl, tabName.getTableName());
        for (String ptab : allPhysicsTabs) {
            return new DBPhysicsTable(jdbcUrl, EntityName.parse(ptab));
        }
        throw new IllegalStateException(tabName + " relevant physics tabs can not be null");
    }

    public static class SplitableDB {
        // key:jdbcUrl ,val:physicsTab
        private Map<String, List<String>> physicsTabInSplitableDB = Maps.newHashMap();

        public SplitableDB add(String jdbcUrl, String tab) {
            List<String> tabs = physicsTabInSplitableDB.get(jdbcUrl);
            if (tabs == null) {
                tabs = Lists.newArrayList();
                physicsTabInSplitableDB.put(jdbcUrl, tabs);
            }
            tabs.add(tab);
            return this;
        }

        public List<String> getTabsInDB(String jdbcUrl) {
            return this.physicsTabInSplitableDB.get(jdbcUrl);
        }
    }

    public static class SplitTablePhysics2LogicNameConverter implements Function<String, String>, Serializable {
        private final Map<String, String> physics2LogicTabNameConverter;

        public SplitTablePhysics2LogicNameConverter(SplitableTableInDB splitTabInDB) {
            this.physics2LogicTabNameConverter = Maps.newHashMap();
            for (Map.Entry<String, SplitableDB> dbEntry : splitTabInDB.tabs.entrySet()) {
                for (Map.Entry<String, List<String>> logicEntry : dbEntry.getValue().physicsTabInSplitableDB.entrySet()) {
                    for (String physicsTabName : logicEntry.getValue()) {
                        this.physics2LogicTabNameConverter.put(physicsTabName, dbEntry.getKey());
                    }
                }
            }
        }

        @Override
        public String apply(String physicsName) {
            String logicalTabName = this.physics2LogicTabNameConverter.get(physicsName);
            if (logicalTabName == null) {
                throw new IllegalStateException("physics tabName:" + physicsName
                        + " can not find relevant logicalTabName,repo size:" + this.physics2LogicTabNameConverter.size());
            }
            return logicalTabName;
        }
    }

    public static class SplitableTableInDB extends TableInDB {
        //<key:逻辑表名,List<String> 物理表列表>
        public Map<String, SplitableDB> tabs = new HashMap<>();

        private final Pattern splitTabPattern;

        public SplitableTableInDB(DBIdentity id, Pattern splitTabPattern) {
            super(id);
            this.splitTabPattern = splitTabPattern;
        }

        @Override
        public Function<String, String> getPhysicsTabName2LogicNameConvertor() {
            return new SplitTablePhysics2LogicNameConverter(this);
        }

        //        @Override
//        public List<String> getMatchedTabs(Optional<String> jdbcUrl, String tab) {
//
//            SplitableDB splitableDB = tabs.get(tab);
//            Objects.requireNonNull(splitableDB, "tab:" + tab + " relevant splitableDB can not be null");
//
//            if (jdbcUrl.isPresent()) {
//                return splitableDB.getTabsInDB(jdbcUrl.get());
//            } else {
//                Set<String> mergeAllTabs = new HashSet<>();
//                for (Map.Entry<String, List<String>> entry : splitableDB.physicsTabInSplitableDB.entrySet()) {
//                    mergeAllTabs.addAll(entry.getValue());
//                }
//                return Lists.newArrayList(mergeAllTabs);
//            }
//        }

        /**
         * @param jdbcUrl 可以标示是哪个分库的
         * @param tab
         */
        @Override
        public void add(String jdbcUrl, String tab) {
            Matcher matcher = PATTERN_PHYSICS_TABLE.matcher(tab);

            if (matcher.matches()) {
                String logicTabName = matcher.group(1);
                addPhysicsTab(jdbcUrl, logicTabName, tab);
            } else {
                addPhysicsTab(jdbcUrl, tab, tab);
                //  tabs.put(tab, (new SplitableDB()).add(jdbcUrl, tab));
            }
        }

        /**
         * @param jdbcUrl
         * @param logicTabName
         * @param tab          物理表名
         */
        private void addPhysicsTab(String jdbcUrl, String logicTabName, String tab) {
            SplitableDB physicsTabs = tabs.get(logicTabName);
            if (physicsTabs == null) {
                physicsTabs = new SplitableDB();
                tabs.put(logicTabName, physicsTabs);
            }
            physicsTabs.add(jdbcUrl, tab);
        }

        @Override
        public DataXJobInfo createDataXJobInfo(DataXJobSubmit.TableDataXEntity tabEntity) {

            SplitableDB splitableDB = tabs.get(tabEntity.getSourceTableName());
            Objects.requireNonNull(splitableDB, "SourceTableName:" + tabEntity.getSourceTableName() + " relevant splitableDB can not be null");

            List<String> matchedTabs = splitableDB.getTabsInDB(tabEntity.getDbIdenetity());
            if (CollectionUtils.isEmpty(matchedTabs)) {
                throw new IllegalStateException("jdbcUrl:" + tabEntity.getDbIdenetity() + " relevant matchedTabs can not be empty");
            }
            // 目前将所有匹配的表都在一个datax 单独进程中去执行，后期可以根据用户的配置单一个单独的dataX进程中执行部分split表以提高导入速度
            return DataXJobInfo.create(tabEntity.getFileName(), tabEntity, matchedTabs);
        }

        @Override
        public List<String> getTabs() {
            return Lists.newArrayList(tabs.keySet());
        }

        @Override
        public boolean contains(String tableName) {
            return this.tabs.containsKey(tableName);
        }


        @Override
        public boolean isEmpty() {
            return this.tabs.isEmpty();
        }
    }

    @TISExtension
    public static class DefatDesc extends Descriptor<SplitTableStrategy> {

        public boolean validateTabPattern(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {

            try {
                Pattern.compile(value);
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                msgHandler.addFieldError(context, fieldName, e.getMessage());
                return false;
            }

            return true;
        }


        @Override
        public String getDisplayName() {
            return SWITCH_ON;
        }
    }
}
