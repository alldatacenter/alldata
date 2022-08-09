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

package org.apache.ambari.server.state.kerberos;

/**
 * KerberosPrincipalType enumerates the different types of expected Kerberos principals
 */
public enum KerberosPrincipalType {
  /**
   * User principal.
   * <p/>
   * Typically in the form <code>user@REALM</code>, but may sometimes be in the form
   * <code>user/group@REALM</code>.
   */
  USER,

  /**
   * Service principal.
   * <p/>
   * Typically in the form <code>service/host@REALM</code>.
   */
  SERVICE;

  public static KerberosPrincipalType translate(String string) {
    if(string == null)
      return null;
    else {
      string = string.trim();

      if(string.isEmpty())
        return null;
      else {
        return valueOf(string.toUpperCase());
      }
    }
  }

  public static String translate(KerberosPrincipalType type) {
    return (type == null)
        ? null
        : type.name().toLowerCase();
  }
}
