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

import com.qlangtech.tis.sql.parser.TisGroupBy.TisGroup;
import com.qlangtech.tis.sql.parser.meta.NodeType;
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.ColRef;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.FunctionDataTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TableTupleCreator;
import junit.framework.Assert;
import java.util.Map;
import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestTmpPaySqlParse extends SqlTaskBaseTestCase {

    public void testParse() throws Exception {
        String tmpPay = "tmp_pay";
        final TableTupleCreator tmpPayTask = this.parseSqlTaskNode(tmpPay);
        final ColRef colRef = tmpPayTask.getColsRefs();
        Assert.assertNotNull(colRef);
        final ColRef.ListMap /* colName */
        colRefMap = colRef.getColRefMap();
        Assert.assertEquals(5, colRefMap.size());
        // Map<String, IDataTupleCreator> /* ref */
        // baseRefMap = colRef.baseRefMap;
        Assert.assertEquals(1, colRef.getBaseRefSize());
        // baseRefMap.get("aa");
        IDataTupleCreator aaTupleCreator = colRef.getTupleCreator("aa");
        Assert.assertNotNull(aaTupleCreator);
        Assert.assertTrue(aaTupleCreator instanceof TableTupleCreator);
        TableTupleCreator aaTabTupleCreator = (TableTupleCreator) aaTupleCreator;
        // EntitiyRef entityRef = aaTabTupleCreator.getEntityRef();
        // Assert.assertNotNull(entityRef);
        Assert.assertEquals("innertab_aa", aaTabTupleCreator.getEntityName());
        // Assert.assertNotNull(entityRef.getTaskNode());
        // SqlTaskNode taskNode = entityRef.getTaskNode();
        Assert.assertEquals(NodeType.JOINER_SQL, aaTabTupleCreator.getNodetype());
        // Assert.assertEquals("innertab_aa", taskNode.getExportName());
        Assert.assertEquals("aa", aaTabTupleCreator.getMediaTabRef());
        final ColRef aaTabTupleColRef = aaTabTupleCreator.getColsRefs();
        // Set<Map.Entry<String, SqlTaskNode>> required = taskNode.getRequired();
        // .baseRefMap.size());
        Assert.assertEquals(1, aaTabTupleColRef.getBaseRefSize());
        // ===============================================================
        ColName pay_customer_ids = new ColName("pay_customer_ids");
        IDataTupleCreator pay_customer_idsTuple = aaTabTupleColRef.getColRefMap().get(pay_customer_ids);
        Assert.assertTrue(pay_customer_idsTuple instanceof FunctionDataTupleCreator);
        FunctionDataTupleCreator pay_customer_idsTupleFunc = (FunctionDataTupleCreator) pay_customer_idsTuple;
        Optional<TisGroupBy> pay_customer_idsTupleFuncGroup = pay_customer_idsTupleFunc.getGroupBy();
        Assert.assertTrue(pay_customer_idsTupleFuncGroup.isPresent());
        Assert.assertEquals(2, pay_customer_idsTupleFuncGroup.get().getGroups().size());
        TisGroup group = pay_customer_idsTupleFuncGroup.get().getGroups().get(0);
        Assert.assertEquals("totalpay_id", group.getColname());
        Assert.assertNotNull(group.getTabTuple());
        group = pay_customer_idsTupleFuncGroup.get().getGroups().get(1);
        Assert.assertEquals("kindpay_id", group.getColname());
        Assert.assertNotNull(group.getTabTuple());
        // ===============================================================
        Optional<Map.Entry<String, IDataTupleCreator>> dependency = aaTabTupleColRef.getBaseRefEntities().stream().findFirst();
        Assert.assertTrue(dependency.isPresent());
        Assert.assertEquals("p", dependency.get().getKey());
        Assert.assertTrue(dependency.get().getValue() instanceof TableTupleCreator);
        TableTupleCreator payTabTuple = (TableTupleCreator) dependency.get().getValue();
        Assert.assertEquals(NodeType.DUMP, payTabTuple.getNodetype());
        // ==================================================================================
        ColName kindpay = new ColName("kindpay");
        IDataTupleCreator kindPayCreator = colRefMap.get(kindpay);
        Assert.assertNotNull(kindPayCreator);
        Assert.assertTrue(kindPayCreator instanceof FunctionDataTupleCreator);
        FunctionDataTupleCreator kindPayFuncCreator = (FunctionDataTupleCreator) kindPayCreator;
        Map<ColName, IDataTupleCreator> kindpayFuncParams = kindPayFuncCreator.getParams();
        Assert.assertEquals(1, kindpayFuncParams.size());
        ColName innerAkindpay = new ColName("aakindpay");
        IDataTupleCreator innerATuple = kindpayFuncParams.get(innerAkindpay);
        Assert.assertNotNull(innerATuple);
        Assert.assertTrue(innerATuple instanceof TableTupleCreator);
        TableTupleCreator innerATableTuple = (TableTupleCreator) innerATuple;
        Assert.assertEquals("aa", innerATableTuple.getMediaTabRef());
        innerATuple = innerATableTuple.getColsRefs().getColRefMap().get(innerAkindpay);
        Assert.assertNotNull(innerATuple);
        Assert.assertTrue(innerATuple instanceof FunctionDataTupleCreator);
        Assert.assertEquals(6, ((FunctionDataTupleCreator) innerATuple).getParams().size());
        // ===================================================================================
        ColName payCustomerIds = new ColName("pay_customer_ids");
        IDataTupleCreator payCustomerIdsTuple = colRef.getColRefMap().get(payCustomerIds);
        Assert.assertTrue(payCustomerIdsTuple instanceof FunctionDataTupleCreator);
        FunctionDataTupleCreator pay_customer_idsFuncTuple = (FunctionDataTupleCreator) payCustomerIdsTuple;
        Optional<TisGroupBy> pay_customer_idsFuncTupleGroupBy = pay_customer_idsFuncTuple.getGroupBy();
        Assert.assertTrue(pay_customer_idsFuncTupleGroupBy.isPresent());
        Assert.assertEquals(1, pay_customer_idsFuncTupleGroupBy.get().getGroups().size());
        pay_customer_idsFuncTupleGroupBy.get().getGroups().forEach((r) -> {
            Assert.assertNotNull("TabRef:" + r.getTabRef(), r.getTabTuple());
        });
    }
}
