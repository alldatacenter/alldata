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

import com.qlangtech.tis.sql.parser.ColName;
import com.qlangtech.tis.sql.parser.TisGroupBy;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreator;
import com.qlangtech.tis.sql.parser.visitor.BlockScriptBuffer;
import com.qlangtech.tis.sql.parser.visitor.FuncFormat;

import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年6月18日
 */
public class PropGetter implements IScriptGenerateContext {

    protected final IDataTupleCreator tupleCreator;

    protected final ColName output;

    private final EntityName entityName;

    // private String groovyScript;
    // 在数据链中最后一个function
    private boolean lastFunctInChain = false;

    private PropGetter next;

    private PropGetter prev;

    public PropGetter(ColName output, EntityName entityName, IDataTupleCreator tupleCreator) {
        super();
        this.tupleCreator = tupleCreator;
        this.output = output;
        this.entityName = entityName;
    }

    public PropGetter getPrev() {
        return this.prev;
    }

    public void setPrev(PropGetter prev) {
        this.prev = prev;
    }

    @Override
    public boolean isNotDeriveFrom(EntityName entityName) {
        if (!(this.next.getTupleCreator() instanceof TableTupleCreator)) {
            return false;
        }
        TableTupleCreator nextTableTuple = (TableTupleCreator) this.next.getTupleCreator();
        return nextTableTuple.getEntityName().equals(entityName);
    }

    /**
     * 是否是聚合函数
     *
     * @return
     */
    public boolean isGroupByFunction() {
        if (!(this.tupleCreator instanceof FunctionDataTupleCreator)) {
            return false;
        }
        final FunctionDataTupleCreator tuple = this.getFunctionDataTuple();
        Optional<TisGroupBy> group = tuple.getGroupBy();
        return group.isPresent();
    }

    @Override
    public boolean isNextGroupByFunction() {
        PropGetter next = this.getNext();
        while (next != null) {
            if (next.tupleCreator instanceof FunctionDataTupleCreator) {
                return next.isGroupByFunction();
            }
            next = next.getNext();
        }
        return false;
    }

    public PropGetter getNextGroupByPropGetter() {
        PropGetter next = this.getNext();
        while (next != null) {
            if (next.tupleCreator instanceof FunctionDataTupleCreator && next.isGroupByFunction()) {
                return next;
            }
            next = next.getNext();
        }
        return null;
    }

    public boolean isNextFunctionTuple() {
        PropGetter next = this.getNext();
        while (next != null) {
            if (next.tupleCreator instanceof FunctionDataTupleCreator) {
                return next.isGroupByFunction();
            }
            next = next.getNext();
        }
        return false;
    }

    public PropGetter getNext() {
        return this.next;
    }

    public void setNext(PropGetter next) {
        this.next = next;
    }

    /**
     * 是否是两个数据源的匯聚點
     *
     * @return
     */
    @Override
    public boolean isJoinPoint() {
        return tupleCreator.refTableSourceCount() > 1;
    }

    public boolean shallCallableProcess() {
        // 不能是最后一个func节点 且 要是多源节点
        return (this.isJoinPoint() && this.isLastFunctInChain());
    }

    @Override
    public boolean isLastFunctInChain() {
        return this.lastFunctInChain;
    }

    public void setLastFunctInChain(boolean lastFunctInChain) {
        this.lastFunctInChain = lastFunctInChain;
    }

    public String getIdentityName() {
        return this.entityName + "." + output.getAliasName();
    }

    /**
     * 生成处理脚本
     *
     * @param rr
     * @param processAggregationResult 当前是否在处理聚合结果集的流程
     *                                 <p>
     *                                 for((k:GroupKey, v:GroupValues) <- instancedetails){
     *                                 // 在处理聚合结果的上下文中
     *                                 // processAggregationResult =  true
     *                                 }
     *                                 </p>
     */
    public void getGroovyScript(BlockScriptBuffer rr, boolean processAggregationResult) {
        if (tupleCreator == null) {
            return;
        }
        tupleCreator.generateGroovyScript(rr, this, processAggregationResult);
    }

    // public SqlTaskNode getTaskNode() {
    // return this.taskNode;
    // }

    public IDataTupleCreator getTupleCreator() {
        return this.tupleCreator;
    }

    @Override
    public ColName getOutputColName() {
        return this.output;
    }

    public EntityName getEntityName() {
        if (this.entityName == null) {
            throw new IllegalStateException("entityName can not be null");
        }
        return entityName;
    }

    @Override
    public FunctionDataTupleCreator getFunctionDataTuple() {
        if (!(this.tupleCreator instanceof FunctionDataTupleCreator)) {
            throw new IllegalStateException("this.tupleCreator is not type of FunctionDataTupleCreator :" + this.tupleCreator);
        }
        return (FunctionDataTupleCreator) this.tupleCreator;
    }
}
