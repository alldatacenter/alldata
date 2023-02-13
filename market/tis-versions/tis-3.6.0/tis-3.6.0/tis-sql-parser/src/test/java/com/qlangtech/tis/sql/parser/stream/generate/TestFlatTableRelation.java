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
package com.qlangtech.tis.sql.parser.stream.generate;

import com.qlangtech.tis.sql.parser.BasicTestCase;
import com.qlangtech.tis.sql.parser.er.ERRules;
import com.qlangtech.tis.sql.parser.er.TableRelation;
import com.qlangtech.tis.sql.parser.er.TestERRules;
import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestFlatTableRelation extends BasicTestCase {

    public void testGetFinalLinkKey() {
        ERRules totalpayErRules = TestERRules.getTotalpayErRules();
        Optional<TableRelation> orderdetailParentRelation = totalpayErRules.getFirstParent("orderdetail");
        assertTrue(orderdetailParentRelation.isPresent());
        Optional<TableRelation> instancedetailParentRelation = totalpayErRules.getFirstParent("instancedetail");
        assertTrue(instancedetailParentRelation.isPresent());
        // orderdetailParentRelation.get().
        String primaryKey = "totalpay_id";
        String entityId = "entity_id";
        TableRelation.FinalLinkKey finalLinkKey = FlatTableRelation.getFinalLinkKey(primaryKey, orderdetailParentRelation.get().getCurrentTableRelation(true));
        // for (TableRelation r : totalpayErRules.getRelationList()) {
        // System.out.println(r);
        // }
        assertNotNull(finalLinkKey);
        assertEquals(primaryKey, finalLinkKey.linkKeyName);
        Optional<TableRelation> servicebillinfoParentRelation = totalpayErRules.getFirstParent("servicebillinfo");
        assertTrue(servicebillinfoParentRelation.isPresent());
        finalLinkKey = FlatTableRelation.getFinalLinkKey(primaryKey, servicebillinfoParentRelation.get().getCurrentTableRelation(true));
        assertNotNull(finalLinkKey);
        assertEquals("servicebill_id", finalLinkKey.linkKeyName);
        finalLinkKey = FlatTableRelation.getFinalLinkKey(entityId, servicebillinfoParentRelation.get().getCurrentTableRelation(true));
        assertNotNull(finalLinkKey);
        assertEquals(entityId, finalLinkKey.linkKeyName);
    }
}
