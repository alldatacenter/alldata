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
package org.apache.drill.exec.planner.physical.visitor;

import java.util.Collections;

import org.apache.drill.exec.planner.physical.ComplexToJsonPrel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.ScreenPrel;

public class ComplexToJsonPrelVisitor extends BasePrelVisitor<Prel, Void, RuntimeException> {

  private static final ComplexToJsonPrelVisitor INSTANCE = new ComplexToJsonPrelVisitor();

  public static Prel addComplexToJsonPrel(Prel prel) {
    return prel.accept(INSTANCE, null);
  }

  @Override
  public Prel visitScreen(ScreenPrel prel, Void value) throws RuntimeException {
    return prel.copy(prel.getTraitSet(), Collections.singletonList(new ComplexToJsonPrel((Prel) prel.getInput())));
  }

}
