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
package com.qlangtech.tis.sql.parser.tuple.creator.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.sql.parser.ColName;
import com.qlangtech.tis.sql.parser.er.ERRules;
import com.qlangtech.tis.sql.parser.meta.NodeType;
import com.qlangtech.tis.sql.parser.tuple.creator.*;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年5月15日
 */
public class TaskNodeTraversesCreatorVisitor implements IDataTupleCreatorVisitor {

    public final Stack<PropGetter> propStack = new Stack<>();

    private final Map<TableTupleCreator, List<ValChain>> tabTriggerLinker = Maps.newHashMap();

    private final ERRules erRules;

    public TaskNodeTraversesCreatorVisitor(ERRules erRules) {
        this.erRules = erRules;
    }

    @Override
    public void visit(FunctionDataTupleCreator function) {
        PropGetter peek = getPeek();
        if (peek == null) {
            throw new IllegalStateException("peek can not be null");
        }
        function.getParams().entrySet().stream().forEach((r) -> /**
         * Map<ColName, IDataTupleCreator>
         */
        {
            // function
            this.pushPropGetter(// function
                    new ColName(r.getKey().getName(), peek.getOutputColName().getAliasName()), // function
                    peek.getTupleCreator().getEntityName(), r.getValue());
            try {
                r.getValue().accept(TaskNodeTraversesCreatorVisitor.this);
            } finally {
                propStack.pop();
            }
        });
    }

    @Override
    public void visit(TableTupleCreator tableTuple) {
        if (tableTuple.getNodetype() == NodeType.DUMP) {
            ValChain propGetters = new ValChain();
            PropGetter prop = null;
            for (int i = 0; i < propStack.size(); i++) {
                prop = propStack.get(i);
                propGetters.add(prop);
            }
            if (prop != null) {
                propGetters.add(new PropGetter(new ColName(prop.getOutputColName().getName()), tableTuple.getEntityName(), null));
            }
            List<ValChain> propsGetters = tabTriggerLinker.get(tableTuple);
            if (propsGetters == null) {
                propsGetters = Lists.newArrayList();
                tabTriggerLinker.put(tableTuple, propsGetters);
            }
            propsGetters.add(propGetters);
            return;
        } else if (tableTuple.getNodetype() == NodeType.JOINER_SQL) {
            PropGetter peek = getPeek();
            for (Map.Entry<ColName, IDataTupleCreator> /* colName */
                    centry : tableTuple.getColsRefs().getColRefMap().entrySet()) {
                if (peek == null || StringUtils.equals(peek.getOutputColName().getName(), centry.getKey().getAliasName())) {
                    this.pushPropGetter(centry.getKey(), tableTuple.getEntityName(), centry.getValue());
                    if (centry.getValue() == null) {
                        throw new IllegalStateException("centry.getKey():" + centry.getKey() + " relevant value IDataTupleCreator can not be null");
                    }
                    try {
                        centry.getValue().accept(this);
                    } finally {
                        this.propStack.pop();
                    }
                    if (peek != null) {
                        break;
                    }
                }
            }
        } else {
            throw new IllegalStateException("tableTuple.getNodetype():" + tableTuple.getNodetype() + " is illegal");
        }
    }

    public PropGetter getPeek() {
        PropGetter peek = null;
        if (!propStack.isEmpty()) {
            peek = propStack.peek();
        }
        // }
        return peek;
    }

    public int getPropGetterStackSize() {
        return this.propStack.size();
    }

    public void clearPropStack() {
        this.propStack.clear();
    }

    public void pushPropGetter(ColName output, EntityName entityName, IDataTupleCreator tupleCreator) {
        this.propStack.push(new PropGetter(output, entityName, tupleCreator));
    }

    public Map<IEntityNameGetter, List<IValChain>> getTabTriggerLinker() {
        //  final Map<TableTupleCreator, List<ValChain>> tabTuple = this.tabTriggerLinker;
        Map<IEntityNameGetter, List<IValChain>> convert = Maps.newHashMap();
        for (Map.Entry<TableTupleCreator, List<ValChain>> entry : this.tabTriggerLinker.entrySet()) {
            convert.put(entry.getKey(), entry.getValue().stream().collect(Collectors.toList()));
        }
        return convert;

//        tabTuple.entrySet().stream().map((e) -> (Map.Entry<TableTupleCreator, List<ValChain>>) e)
//                .collect(Collectors.toMap((e) -> {
//                    return (IEntityNameGetter) e.getKey();
//                }, (e) -> (IValChain) e.getValue()));
//
//        Map<IEntityNameGetter, List<IValChain>> convert = tabTuple.entrySet().stream()
//                .collect(Collectors.toMap((e) -> (IEntityNameGetter) e.getKey(), (e) -> (IValChain) e.getValue()));
//
//        return convert;
        // return this.tabTriggerLinker;
    }
}
