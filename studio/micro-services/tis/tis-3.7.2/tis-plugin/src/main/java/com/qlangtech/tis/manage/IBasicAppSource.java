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

package com.qlangtech.tis.manage;

import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-04-27 15:26
 **/
public interface IBasicAppSource extends IAppSource, IStreamIncrGenerateStrategy {
    <T> T accept(IAppSourceVisitor<T> visitor);

    interface IAppSourceVisitor<T> {

        default T visit(ISingleTableAppSource single) {
            throw new UnsupportedOperationException();
        }

        default T visit(IDataFlowAppSource dataflow) {
            throw new UnsupportedOperationException();
        }


        default T visit(DataxProcessor app) {
            return null;
        }
    }
}
