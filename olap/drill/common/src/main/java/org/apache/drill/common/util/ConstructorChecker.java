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
package org.apache.drill.common.util;

import java.lang.reflect.Constructor;

public class ConstructorChecker {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConstructorChecker.class);

  private final String requirementString;
  private final Class<?>[] classes;

  public ConstructorChecker(Class<?>... classes) {
    super();
    this.classes = classes;
    StringBuffer sb = new StringBuffer();
    sb.append("The required constructor is (");
    for (int i =0; i < classes.length; i++) {
      if (i != 0) {
        sb.append(", ");
      }
      sb.append(classes[i].getName());
    }

    this.requirementString = sb.toString();
  }

  public boolean check(Constructor<?> c) {
    Class<?>[] params = c.getParameterTypes();
    if (params.length != classes.length) {
      return false;
    }
    for (int i =0; i < classes.length; i++) {
      if ( !classes[i].isAssignableFrom(params[i])) {
        return false;
      }
    }

    return true;
  }

  public String getRequirementString() {
    return requirementString;
  }

}
