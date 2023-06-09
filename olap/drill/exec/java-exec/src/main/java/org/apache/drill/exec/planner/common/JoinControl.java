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
package org.apache.drill.exec.planner.common;

/**
 * For the int type control,
 * the meaning of each bit start from lowest:
 * bit 0: intersect or not, 0 -- default(no intersect), 1 -- INTERSECT (DISTINCT as default)
 * bit 1: intersect type, 0 -- default (DISTINCT), 1 -- INTERSECT_ALL
 */
public class JoinControl {
  public final static int DEFAULT = 0;
  public final static int INTERSECT_DISTINCT = 0x01;//0001
  public final static int INTERSECT_ALL = 0x03; //0011
  public final static int INTERSECT_MASK = 0x03;
  private final int joinControl;

  public JoinControl(int intControl) {
    joinControl = intControl;
  }

  public boolean isIntersect() {
    return (joinControl & INTERSECT_MASK) != 0;
  }

  public boolean isIntersectDistinct() {
    return (joinControl & INTERSECT_MASK) == INTERSECT_DISTINCT;
  }

  public boolean isIntersectAll() {
    return (joinControl & INTERSECT_MASK) == INTERSECT_ALL;
  }

  public int asInt() {
    return joinControl;
  }

}
