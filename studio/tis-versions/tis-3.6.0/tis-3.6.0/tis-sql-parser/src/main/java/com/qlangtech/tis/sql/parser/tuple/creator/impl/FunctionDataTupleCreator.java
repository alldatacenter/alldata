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
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreatorVisitor;
import com.qlangtech.tis.sql.parser.visitor.BlockScriptBuffer;
import com.qlangtech.tis.sql.parser.visitor.FuncFormat;
import com.qlangtech.tis.sql.parser.visitor.FunctionGenerateScriptVisitor;
import com.qlangtech.tis.sql.parser.visitor.FunctionVisitor;
import com.facebook.presto.sql.tree.Expression;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.*;

/**
 * 通过多个参数，算出一個結果
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年5月10日
 */
public class FunctionDataTupleCreator implements IDataTupleCreator {

    public final Map<ColName, IDataTupleCreator> params;

    // params中关联的表实体对象的数量，数量如果大于1 ，那说明计算源来自两个tuple
    private final int refTableSourceCount;

    private final Expression expression;

    private final ColRef colRef;

    private Optional<TisGroupBy> groupBy = Optional.empty();

    public FunctionDataTupleCreator(Expression expression, ColRef colRef) {
        this.expression = expression;
        // 对expression解析之后需要向baseRefMap中添加引用
        this.colRef = colRef;
        Map<ColName, IDataTupleCreator> p = Maps.newHashMap();
        FunctionVisitor functionVisitor = new FunctionVisitor(this.getColRef(), p, new FuncFormat(), false);
        functionVisitor.process(this.expression, null);
        // 将参数固定住
        this.params = Collections.unmodifiableMap(p);
        final Set<String> mediaTabRef = Sets.newHashSet();
        TableTupleCreator tab = null;
        for (IDataTupleCreator tuple : this.params.values()) {
            tab = (TableTupleCreator) tuple;
            mediaTabRef.add(tab.getMediaTabRef());
        }
        this.refTableSourceCount = mediaTabRef.size();
    }

    // params中关联的表实体对象的数量，数量如果大于1 ，那说明计算源来自两个tuple
    @Override
    public int refTableSourceCount() {
        return this.refTableSourceCount;
    }

    public void generateGroovyScript(BlockScriptBuffer rr, IScriptGenerateContext context, boolean processAggregationResult) {
        final FunctionVisitor //
        functionVisitor = new FunctionGenerateScriptVisitor(this.colRef, this.params, rr, context, processAggregationResult);
        functionVisitor.process(this.expression, null);
        functionVisitor.getFuncFormat();
    }

    @Override
    public EntityName getEntityName() {
        Iterator<String> paramsIt = params.entrySet().stream().map((c) -> c.getValue().getEntityName() + "." + c.getKey().getName()).iterator();
        StringBuffer buffer = new StringBuffer();
        if (groupBy.isPresent()) {
            buffer.append("{");
        }
        buffer.append("func(" + Joiner.on(",").join(paramsIt) + ")");
        if (groupBy.isPresent()) {
            buffer.append(",groupby[" + Joiner.on(",").join(groupBy.get().getGroups().stream().map((r) -> r.getTabTuple().getEntityName() + "." + r.getColname()).iterator()) + "]}");
        }
        return EntityName.createFuncEntity(buffer);
    }

    public final Optional<TisGroupBy> getGroupBy() {
        return this.groupBy;
    }

    public void setGroupBy(TisGroupBy groupBy) {
        this.groupBy = Optional.of(groupBy);
    }

    @Override
    public void accept(IDataTupleCreatorVisitor visitor) {
        visitor.visit(this);
    }

    public Map<ColName, IDataTupleCreator> getParams() {
        return this.params;
    }

    public Expression getExpression() {
        return expression;
    }

    public ColRef getColRef() {
        return this.colRef;
    }

    @Override
    public String toString() {
        return "FunctionDataTuple";
    }
}
