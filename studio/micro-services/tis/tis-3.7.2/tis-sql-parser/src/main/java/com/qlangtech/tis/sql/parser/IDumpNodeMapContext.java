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

import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TableTupleCreator;
import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IDumpNodeMapContext {

    public Map<EntityName, List<TableTupleCreator>> getDumpNodesMap();

    /**
     * @param tabname
     * @return
     */
    public EntityName accurateMatch(String tabname);

    /**
     * 没有匹配成功 可返回空
     *
     * @param tabname
     * @return
     */
    public EntityName nullableMatch(String tabname);

    public List<SqlTaskNode> getAllJoinNodes();

    public SqlTaskNode geTaskNode(final EntityName entityName) throws Exception;
}
