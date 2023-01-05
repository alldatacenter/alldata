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
package com.qlangtech.tis.sql.parser.utils;

import com.qlangtech.tis.sql.parser.IDumpNodeMapContext;
import com.qlangtech.tis.sql.parser.SqlTaskNode;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TableTupleCreator;
import org.apache.commons.lang.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DefaultDumpNodeMapContext implements IDumpNodeMapContext {

    private final Map<EntityName, List<TableTupleCreator>> dumpNodsContext;

    private List<SqlTaskNode> allJoinNodes;

    public DefaultDumpNodeMapContext(Map<EntityName, List<TableTupleCreator>> dumpNodsContext) {
        this.dumpNodsContext = dumpNodsContext;
    }

    public void setAllJoinNodes(List<SqlTaskNode> allJoinNodes) {
        this.allJoinNodes = allJoinNodes;
    }

    @Override
    public List<SqlTaskNode> getAllJoinNodes() {
        return this.allJoinNodes;
    }

    public SqlTaskNode geTaskNode(final EntityName entityName) throws Exception {
        if (entityName == null) {
            throw new IllegalArgumentException("param entityName can not be null");
        }
        List<SqlTaskNode> allNodes = this.getAllJoinNodes();
        if (allNodes == null) {
            throw new IllegalStateException("entityName:" + entityName + " relevant join node can not be null");
        }
        Optional<SqlTaskNode> node = allNodes.stream().filter((r) ->
                org.apache.commons.lang.StringUtils.equals(r.getExportName().getTabName(), entityName.getTabName())).findFirst();
        if (!node.isPresent()) {
            throw new IllegalStateException("nodename:" + entityName.getTabName() + " can not be find ,all:["
                    + allNodes.stream().map((e) -> e.getExportName().getTabName()).collect(Collectors.joining(",")) + "] ");
        }
        return node.get();
    }

    @Override
    public Map<EntityName, List<TableTupleCreator>> getDumpNodesMap() {
        return this.dumpNodsContext;
    }

    @Override
    public EntityName accurateMatch(String tabname) {
        //
        List<EntityName> names = this.dumpNodsContext.keySet().stream().filter((e) -> StringUtils.equals(e.getTabName(), tabname)).collect(Collectors.toList());
        if (names.size() != 1) {
            throw new IllegalStateException("table:" + tabname + " relevant tab not equal with 1 ,size:" + names.size());
        }
        return names.get(0);
    }

    @Override
    public EntityName nullableMatch(String tabname) {
        //
        List<EntityName> names = this.dumpNodsContext.keySet().stream().filter((e) -> StringUtils.equals(e.getTabName(), tabname)).collect(Collectors.toList());
        if (names.size() < 1) {
            return null;
        }
        if (names.size() != 1) {
            throw new IllegalStateException("table:" + tabname + " relevant tab not equal with 1 ,size:" + names.size());
        }
        return names.get(0);
    }
}
