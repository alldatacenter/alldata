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
package org.apache.drill.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic representation of a column parsed from a query profile.
 * Idea is to use this to generate mock data that represents a
 * query obtained from a user. This is a work in progress.
 */

public class FieldDef {
  public enum Type { VARCHAR, DOUBLE };
  public enum TypeHint { DATE, TIME };

  public final String name;
  public final String typeStr;
  public final Type type;
  public int length;
  public TypeHint hint;

  public FieldDef(String name, String typeStr) {
    this.name = name;
    this.typeStr = typeStr;

    // Matches the type as provided in the query profile:
    // name:type(length)
    // Length is provided for VARCHAR fields. Examples:
    // count: INTEGER
    // customerName: VARCHAR(50)

    Pattern p = Pattern.compile("(\\w+)(?:\\((\\d+)\\))?");
    Matcher m = p.matcher(typeStr);
    if (! m.matches()) { throw new IllegalStateException(); }
    if (m.group(2) == null) {
      length = 0;
    } else {
      length = Integer.parseInt(m.group(2));
    }
    switch (m.group(1).toUpperCase()) {
    case "VARCHAR":
      type = Type.VARCHAR;
      break;
    case "DOUBLE":
      type = Type.DOUBLE;
      break;
    // TODO: Add other types over time.
    default:
      type = null;
    }

  }

  @Override
  public String toString() {
    String str = name + ": " + typeStr;
    if (type != null) {
      str += " - " + type.name();
      if (length != 0) {
        str += "(" + length + ")";
      }
    }
    return str;
  }
}
