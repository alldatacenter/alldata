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
package com.qlangtech.tis.sql.parser.visitor;

import com.qlangtech.tis.sql.parser.ColName;
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.IScriptGenerateContext;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.ColRef;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年6月21日
 */
public class FunctionGenerateScriptVisitor extends FunctionVisitor {

    public FunctionGenerateScriptVisitor(ColRef colRef, Map<ColName, IDataTupleCreator> params, BlockScriptBuffer funcFormat, IScriptGenerateContext generateContext, boolean processAggregationResult) {
        super(colRef, new UnWriteableMap(params), funcFormat, generateContext, processAggregationResult);
    }

    @Override
    protected void processJoinPointPram(ColName param) {
        IDataTupleCreator tuple = this.functionParams.get(param);
        if (this.generateContext.isNotDeriveFrom(tuple.getEntityName())) {
        // System.out.println("notderivefrom:" + param);
        }
    }

    private static class UnWriteableMap extends HashMap<ColName, IDataTupleCreator> {

        private static final long serialVersionUID = 1L;

        public UnWriteableMap(Map<? extends ColName, ? extends IDataTupleCreator> m) {
            super(m);
        }

        @Override
        public IDataTupleCreator put(ColName key, IDataTupleCreator value) {
            return null;
        }

        @Override
        public void putAll(Map<? extends ColName, ? extends IDataTupleCreator> m) {
        }

        @Override
        public IDataTupleCreator putIfAbsent(ColName key, IDataTupleCreator value) {
            return null;
        }
    }
}
