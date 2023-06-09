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
package com.qlangtech.tis.sql.parser.tuple.creator;

import com.qlangtech.tis.sql.parser.tuple.creator.impl.IScriptGenerateContext;
import com.qlangtech.tis.sql.parser.visitor.BlockScriptBuffer;
import com.qlangtech.tis.sql.parser.visitor.FuncFormat;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IDataTupleCreator extends IEntityNameGetter{

    public void accept(IDataTupleCreatorVisitor visitor);

   // public EntityName getEntityName();

    public int refTableSourceCount();

    /**
     * 生成Groovy的執行腳本
     *
     * @return
     */
    public void generateGroovyScript(BlockScriptBuffer rr, IScriptGenerateContext context, boolean processAggregationResult);
    // 退出當前棧之後執行回調處理
    // public void process(Object val);
    // public Object getVal(String name, IDataContext dataContext);
}
