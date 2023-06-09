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
package org.apache.drill;

import org.apache.drill.exec.expr.holders.NullableBigIntHolder;

public class CompileClassWithIfs {

  public static void doSomething() {
    int a = 2;
    NullableBigIntHolder out0 = new NullableBigIntHolder();
    out0.isSet = 1;
    NullableBigIntHolder out4 = new NullableBigIntHolder();
    out4.isSet = 0;
    if (a == 0) {
      out0 = out4;
    } else {
    }

    if (out4.isSet == 0) {
      out0.isSet = 1;
    } else {
      out0.isSet = 0;
      assert false : "Incorrect class transformation. This code should never be executed.";
    }
  }
}
