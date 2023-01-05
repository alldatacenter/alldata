/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.sql.parser;

import com.google.common.collect.Lists;
import com.qlangtech.tis.realtime.transfer.UnderlineUtils;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TableTupleCreator;
import org.apache.commons.lang.StringUtils;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TisGroupBy {

    private List<TisGroup> groups = Lists.newArrayList();

    private TisGroup last = null;

    public boolean add(TisGroup e) {
        this.last = e;
        return this.groups.add(e);
    }

    public String getGroupsLiteria() {
        return this.linkGroupKeys(false);
    }

    public String getGroupKeyAsParamsLiteria() {
        return this.linkGroupKeys(true);
    }

    private String linkGroupKeys(boolean addQuotes) {
        return groups.stream().map((r) -> (addQuotes ? "\"" : StringUtils.EMPTY) + r.getColname() + (addQuotes ? "\"" : StringUtils.EMPTY)).collect(Collectors.joining(","));
    }

    /**
     * 聚合groupby的名称
     *
     * @return
     */
    public String getGroupAggrgationName() {
        // .getTabName() + "s";
        return this.last.getTabTuple().getEntityName().entities();
    }

    public List<TisGroup> getGroups() {
        return groups;
    }

    public static class TisColumn {

        protected final String colname;

        public TisColumn(String colname) {
            if (StringUtils.isBlank(colname)) {
                throw new IllegalArgumentException("colname can not be null");
            }
            this.colname = colname;
        }

        public String getColname() {
            return this.colname;
        }

        public String getJavaVarName() {
            return UnderlineUtils.removeUnderline(this.colname).toString();
        }

        public String buildDefineParam() {
            return "val " + this.getJavaVarName() + " = row.getColumn(\"" + this.colname + "\")";
        }

        public String buildDefineGetPkRouterVar() {
            return "val " + this.getJavaVarName() + " = pk.getRouterVal(\"" + this.colname + "\")";
        }

        public String buildPropCriteriaEqualLiteria() {
            return buildPropCriteriaEqualLiteria(this.getJavaVarName());
        }

        public String buildPropCriteriaEqualLiteria(String pkVar) {
            return ".and" + StringUtils.capitalize(this.getJavaVarName()) + "EqualTo(" + pkVar + ")";
        }
    }

    public static class TisGroup extends TisColumn {

        private final String tabRef;

        // tabRef 对应的entitiy對象
        private TableTupleCreator tabTuple;

        boolean tupleSetted;

        public TisGroup(String tabRef, String colname) {
            super(colname);
            // this.colname = colname;
            this.tabRef = tabRef;
        }

        public TableTupleCreator getTabTuple() {
            if (tabTuple == null) {
                throw new IllegalStateException("tabRef:" + tabRef + ",col:" + getColname() + " relevant tabletuple can not be null");
            }
            return this.tabTuple;
        }

        public void setTabTuple(TableTupleCreator tabTuple) {
            this.tabTuple = tabTuple;
            this.tupleSetted = true;
        }

        public boolean isTupleSetted() {
            return tupleSetted;
        }

        public String getTabRef() {
            return this.tabRef;
        }
    }
}
