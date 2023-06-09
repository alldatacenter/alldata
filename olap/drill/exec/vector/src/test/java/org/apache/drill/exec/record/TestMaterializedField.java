/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.record;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.apache.drill.common.types.Types;
import org.junit.Test;

public class TestMaterializedField {

  @Test
  public void testHashCodeContact() {
    MaterializedField childField = MaterializedField.create(new String("child"), Types.OPTIONAL_BIT);
    MaterializedField field = MaterializedField.create(new String("field"), Types.OPTIONAL_INT);
    MaterializedField equalField = MaterializedField.create(new String("field"), Types.OPTIONAL_INT);
    field.addChild(childField);
    equalField.addChild(childField);

    //equal fields
    assertEquals(field.hashCode(), equalField.hashCode());

    //equal fields with different case of field name
    equalField = MaterializedField.create(new String("FIELD"), Types.OPTIONAL_INT);
    equalField.addChild(childField);
    assertEquals(field.hashCode(), equalField.hashCode());

    //not equal fields
    MaterializedField differentField = MaterializedField.create(new String("other"), Types.OPTIONAL_BIT);
    differentField.addChild(childField);
    assertNotEquals(field.hashCode(), differentField.hashCode());

    //no NullPointerException on field with null name
    field = MaterializedField.create(null, Types.OPTIONAL_INT);
    field.hashCode();
  }

  @Test
  public void testEqualsContract() {
    MaterializedField childField = MaterializedField.create(new String("child"), Types.OPTIONAL_BIT);
    MaterializedField field = MaterializedField.create(new String("field"), Types.OPTIONAL_INT);
    MaterializedField equalField = MaterializedField.create(new String("field"), Types.OPTIONAL_INT);
    field.addChild(childField);
    equalField.addChild(childField);

    //reflexivity
    assertEquals(field, field);

    //symmetry
    assertEquals(field, equalField);
    assertEquals(equalField, field);

    //comparison with null
    assertNotEquals(field, null);

    //different type
    MaterializedField differentField = MaterializedField.create(new String("field"), Types.OPTIONAL_BIT);
    differentField.addChild(childField);
    assertNotEquals(field, differentField);

    //different name
    differentField = MaterializedField.create(new String("other"), Types.OPTIONAL_INT);
    differentField.addChild(childField);
    assertNotEquals(field, differentField);

    //different child
    equalField.addChild(MaterializedField.create("field", Types.OPTIONAL_BIT));
    assertEquals(field, equalField);

    //different name case
    equalField = MaterializedField.create("FIELD", Types.OPTIONAL_INT);
    equalField.addChild(childField);
    assertEquals(field, equalField);
  }
}
