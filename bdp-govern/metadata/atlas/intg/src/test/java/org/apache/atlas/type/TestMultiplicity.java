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

package org.apache.atlas.type;

import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.v1.model.typedef.AttributeDefinition;
import org.apache.atlas.v1.model.typedef.Multiplicity;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestMultiplicity {
    @Test
    public void verify() {

        assertMultiplicity(AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE, false, 1, 1);
        assertMultiplicity(AtlasStructDef.AtlasAttributeDef.Cardinality.LIST, false, 1, Integer.MAX_VALUE);
        assertMultiplicity(AtlasStructDef.AtlasAttributeDef.Cardinality.SET, true, 0, Integer.MAX_VALUE);
    }

    private void assertMultiplicity(AtlasStructDef.AtlasAttributeDef.Cardinality cardinality,
                                    boolean optionality, int expectedLower, int expectedUpper) {
        AtlasStructDef.AtlasAttributeDef attributeDef = new AtlasStructDef.AtlasAttributeDef();
        attributeDef.setCardinality(cardinality);
        attributeDef.setIsOptional(optionality);


        Multiplicity multiplicity = AtlasTypeUtil.getMultiplicity(attributeDef);
        assertNotNull(multiplicity);
        assertEquals(multiplicity.getLower(), expectedLower);
        assertEquals(multiplicity.getUpper(), expectedUpper);

        AttributeDefinition attributeDefinition = new AttributeDefinition();
        attributeDefinition.setMultiplicity(multiplicity);
        assertNotNull(AtlasType.toJson(attributeDefinition));
    }
}
