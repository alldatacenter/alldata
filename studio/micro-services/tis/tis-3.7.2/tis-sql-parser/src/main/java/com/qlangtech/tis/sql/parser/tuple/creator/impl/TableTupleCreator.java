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

import com.qlangtech.tis.sql.parser.meta.NodeType;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreatorVisitor;
import com.qlangtech.tis.sql.parser.visitor.BlockScriptBuffer;
import com.qlangtech.tis.sql.parser.visitor.FuncFormat;

/**
 * 表数据发生器
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年5月31日
 */
public class TableTupleCreator implements IDataTupleCreator {

    private final String mediaTabRef;

    private NodeType nodetype;

    private ColRef colsRefs = null;

    private EntityName realEntityName;

    public TableTupleCreator(String mediaTabRef, NodeType nodetype) {
        super();
        this.mediaTabRef = mediaTabRef;
        this.nodetype = nodetype;
    }

    @Override
    public int refTableSourceCount() {
        return 1;
    }

    @Override
    public void generateGroovyScript(BlockScriptBuffer rr, IScriptGenerateContext context, boolean processAggregationResult) {
        rr.append("tab:" + this.realEntityName);
    }

    @Override
    public void accept(IDataTupleCreatorVisitor visitor) {
        visitor.visit(this);
    }

    public void setNodetype(NodeType nodetype) {
        this.nodetype = nodetype;
    }

    @Override
    public EntityName getEntityName() {
        return this.realEntityName;
    }

    public String getMediaTabRef() {
        return this.mediaTabRef;
    }

    public void setRealEntityName(EntityName realEntityName) {
        this.realEntityName = realEntityName;
    }

    public ColRef getColsRefs() {
        return this.colsRefs;
    }

    public void setColsRefs(ColRef colsRefs) {
        this.colsRefs = colsRefs;
    }

    public NodeType getNodetype() {
        return this.nodetype;
    }

    @Override
    public String toString() {
        if (this.realEntityName == null) {
            return "ref:" + mediaTabRef;
        } else {
            return "ref:" + mediaTabRef + ",entity:" + this.realEntityName;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TableTupleCreator)) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        if (this.realEntityName == null) {
            throw new IllegalStateException("'realEntityName' can not be null");
        }
        return this.realEntityName.hashCode();
    }
}
