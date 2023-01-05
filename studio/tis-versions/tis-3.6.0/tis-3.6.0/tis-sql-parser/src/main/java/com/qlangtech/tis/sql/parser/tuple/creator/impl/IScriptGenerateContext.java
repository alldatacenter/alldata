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
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreator;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年6月18日
 */
public interface IScriptGenerateContext {

    boolean isLastFunctInChain();

    boolean isGroupByFunction();

    /**
     * 离开源表更近的tuple是聚合的计算类型？
     * @return
     */
    boolean isNextGroupByFunction();

    /**
     * 是否汇聚两个tule的计算单元
     *
     * @return
     */
    boolean isJoinPoint();

    public EntityName getEntityName();

    public ColName getOutputColName();

    public FunctionDataTupleCreator getFunctionDataTuple();
    public IDataTupleCreator getTupleCreator();

    public boolean isNotDeriveFrom(EntityName entityName);
}
