/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.utils;

import java.util.List;
import java.util.ListIterator;

public class CustomStringUtils {
  /**
   * <code>CustomStringUtils</code> instances should NOT be constructed in
   * standard programming. Instead, the class should be used as
   * <code>CustomStringUtils.containsCaseInsensitive("foo", arrayList)</code>
   */
  public CustomStringUtils(){
    super();
  }

  /**
   * Returns <tt>true</tt> if this list contains the specified string element, ignoring case considerations.
   * @param s element whose presence in this list is to be tested
   * @param l list of strings, where presence of string element would be checked
   * @return <tt>true</tt> if this list contains the specified element
   */
  public static boolean containsCaseInsensitive(String s, List<String> l){
    for (String listItem : l){
      if (listItem.equalsIgnoreCase(s)){
        return true;
      }
    }
    return false;
  }

  /**
   * Make list of string lowercase
   * @param l list of strings, which need to be in lowercase
   */
  public static void toLowerCase(List<String> l) {
    ListIterator<String> iterator = l.listIterator();
    while (iterator.hasNext()) {
      iterator.set(iterator.next().toLowerCase());
    }
  }

  /**
   * Make list of string lowercase
   * @param l list of strings, which need to be in lowercase
   */
  public static void toUpperCase(List<String> l) {
    ListIterator<String> iterator = l.listIterator();
    while (iterator.hasNext()) {
      iterator.set(iterator.next().toUpperCase());
    }
  }
}
