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
package org.apache.drill.exec.store.mock;

import org.apache.drill.exec.vector.accessor.ScalarWriter;

/**
 * Generates a mock string field of the given length. Fields are composed
 * of upper case letters uniformly distributed from A to Z, and repeated
 * or the length of the field. Exampled for a 4-character field:
 * DDDD, MMMM, AAAA, RRRR, ...
 */

public class StringGen extends AbstractFieldGen {

  private int length;

  @Override
  public void setup(ColumnDef colDef, ScalarWriter colLoader) {
    super.setup(colDef, colLoader);
    length = colDef.width;
  }

  private String value() {
    String c = Character.toString((char) (rand.nextInt(26) + 'A'));
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < length; i++) {
      buf.append(c);
    }
    return buf.toString();
  }

  @Override
  public void setValue() {
    colWriter.setString(value());
  }
}
