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
package org.apache.drill.exec.vector;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.holders.ValueHolder;

public class UntypedNullHolder implements ValueHolder {
  public static final TypeProtos.MajorType TYPE = Types.optional(TypeProtos.MinorType.NULL);
  public static final int WIDTH = 0;
  public int isSet = 0;

  public TypeProtos.MajorType getType() {return TYPE;}

  @Deprecated
  public int hashCode(){
    throw new UnsupportedOperationException();
  }

  /*
   * Reason for deprecation is that ValueHolders are potential scalar replacements
   * and hence we don't want any methods to be invoked on them.
   */
  @Deprecated
  public String toString(){
    throw new UnsupportedOperationException();
  }

}
