/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.store.graph.v2;

import com.google.common.collect.ImmutableList;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.DeleteType;
import org.apache.atlas.type.AtlasTypeUtil;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/**
 * Inverse reference update test with SoftDeleteHandlerV1
 */
public class InverseReferenceUpdateSoftDeleteV2Test extends InverseReferenceUpdateV2Test {

    public InverseReferenceUpdateSoftDeleteV2Test() {
        super(DeleteType.SOFT);
    }

    @Override
    protected void verify_testInverseReferenceAutoUpdate_NonComposite_OneToMany(AtlasEntity jane)
        throws Exception {

        // Max is still in the subordinates list, as the edge still exists with state DELETED
        verifyReferenceList(jane, "subordinates", ImmutableList.of(nameIdMap.get("John"), nameIdMap.get("Max")));
    }

    @Override
    protected void verify_testInverseReferenceAutoUpdate_NonCompositeManyToOne(AtlasEntity a1,
        AtlasEntity a2, AtlasEntity a3, AtlasEntity b) {

        verifyReferenceValue(a1, "oneB", b.getGuid());

        verifyReferenceValue(a2, "oneB", b.getGuid());

        verifyReferenceList(b, "manyA", ImmutableList.of(AtlasTypeUtil.getAtlasObjectId(a1), AtlasTypeUtil.getAtlasObjectId(a2), AtlasTypeUtil.getAtlasObjectId(a3)));
    }

    @Override
    protected void verify_testInverseReferenceAutoUpdate_NonComposite_OneToOne(AtlasEntity a1, AtlasEntity b) {

        verifyReferenceValue(a1, "b", b.getGuid());
    }

    @Override
    protected void verify_testInverseReferenceAutoUpdate_Map(AtlasEntity a1, AtlasEntity b1,
        AtlasEntity b2, AtlasEntity b3) {

        Object value = a1.getAttribute("mapToB");
        assertTrue(value instanceof Map);
        Map<String, AtlasObjectId> refMap = (Map<String, AtlasObjectId>) value;
        assertEquals(refMap.size(), 3);
        AtlasObjectId referencedEntityId = refMap.get("b3");
        assertEquals(referencedEntityId, AtlasTypeUtil.getAtlasObjectId(b3));
        verifyReferenceValue(b1, "mappedFromA", a1.getGuid());
        verifyReferenceValue(b2, "mappedFromA", a1.getGuid());
    }

}
