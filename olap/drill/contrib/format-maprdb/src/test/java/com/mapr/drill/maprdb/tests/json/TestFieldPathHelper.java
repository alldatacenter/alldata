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
package com.mapr.drill.maprdb.tests.json;

import static org.junit.Assert.assertEquals;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.store.mapr.db.json.FieldPathHelper;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.ojai.FieldPath;

public class TestFieldPathHelper extends BaseTest {

  @Test
  public void simeTests() {
    String[] pathStrs = {"a", "a.b", "a.b.c", "a[1].b[2].c", "a[0][1][2][3].b"};
    FieldPath[] fieldPaths = new FieldPath[pathStrs.length];
    SchemaPath[] schemaPaths = new SchemaPath[pathStrs.length];

    // build
    for (int i = 0; i < pathStrs.length; i++) {
      String path = pathStrs[i];
      fieldPaths[i] = FieldPath.parseFrom(path);
      schemaPaths[i] = SchemaPath.parseFromString(path);
    }

    //verify
    for (int i = 0; i < pathStrs.length; i++) {
      FieldPath fp = FieldPathHelper.schemaPath2FieldPath(schemaPaths[i]);
      assertEquals(fieldPaths[i], fp);

      SchemaPath sp = FieldPathHelper.fieldPath2SchemaPath(fieldPaths[i]);
      assertEquals(schemaPaths[i], sp);
    }
  }

}
