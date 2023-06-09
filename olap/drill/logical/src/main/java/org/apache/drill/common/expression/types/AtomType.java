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
package org.apache.drill.common.expression.types;


public class AtomType extends DataType {
  private String name;
  private Comparability comparability;
  private boolean isNumericType;

  public AtomType(String name, Comparability comparability, boolean isNumericType) {
    super();
    this.name = name;
    this.comparability = comparability;
    this.isNumericType = isNumericType;
  }


  @Override
  public boolean isNumericType() {
    return isNumericType;
  }


  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isLateBind() {
    return false;
  }

  @Override
  public boolean hasChildType() {
    return false;
  }

  @Override
  public DataType getChildType() {
    return null;
  }

  @Override
  public Comparability getComparability() {
    return comparability;
  }




}
