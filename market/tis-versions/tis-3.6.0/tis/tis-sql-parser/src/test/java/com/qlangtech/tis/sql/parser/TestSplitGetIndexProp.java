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

import junit.framework.TestCase;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestSplitGetIndexProp extends TestCase {

    public void testGetIndexProp() throws Exception {
    // TableTupleCreator tuple = null;
    // try (InputStream read = this.getClass()
    // .getResourceAsStream(TestSplitGetIndexProp.class.getSimpleName() + ".txt")) {
    //
    // SqlTaskNode taskNode = new SqlTaskNode(EntityName.parse("testExportName"), NodeType.JOINER_SQL);
    // taskNode.setContent(IOUtils.toString(read, "utf8"));
    // tuple = taskNode.parse();
    // }
    //
    // ColName col = new ColName("customer_ids");
    // IDataTupleCreator colTuple = tuple.getColsRefs().colRefMap.get(col);
    // Assert.assertTrue(colTuple instanceof FunctionDataTupleCreator);
    // FunctionDataTupleCreator funcTuple = (FunctionDataTupleCreator) colTuple;
    // FunctionVisitor.FuncFormat funcFormat = new FunctionVisitor.FuncFormat();
    //
    // IScriptGenerateContext context = null;
    //
    // funcTuple.generateGroovyScript(funcFormat, context);
    // Assert.assertTrue(funcFormat.toString().length() > 1);
    //
    // System.out.println(funcFormat.toString());
    //
    // Assert.assertEquals(FunctionVisitor.SubscriptFunctionName
    // + "(split(row.getColumn(\"batch_msg\"),\"[\\\\w\\\\W]*\\\\|\"),1)", funcFormat.toString());
    }
}
