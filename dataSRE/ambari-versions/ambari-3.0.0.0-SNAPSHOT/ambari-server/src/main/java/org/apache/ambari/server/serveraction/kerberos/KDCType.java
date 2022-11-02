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

package org.apache.ambari.server.serveraction.kerberos;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

/**
 * Enumerates the supported KDC types
 */
@XmlEnum
public enum KDCType {
  /**
   * Indicates not KDC type is relevant. This is expected when Ambari is not managing Kerberos identities.
   */
  @XmlEnumValue("none")
  NONE,

  /**
   * Indicates an MIT KDC (or similar)
   */
  @XmlEnumValue("mit-kdc")
  MIT_KDC,

  /**
   * Indicates a Microsoft Active Directory
   */
  @XmlEnumValue("active-directory")
  ACTIVE_DIRECTORY,

  /**
   * Indicates an IPA KDC
   */
  @XmlEnumValue("ipa")
  IPA;

  /**
   * Translates a String to a KDCType.
   * <p/>
   * The translation logic attempts to nicely convert a String to a KDCType by replacing all '-'
   * characters to '_' characters and converting the String to uppercase. Allowing for values like
   * "mit_kdc" to be translated to "MIT_KDC".
   *
   * @param value a String value to convert to a KDCType
   * @return A KDCType
   * @throws java.lang.IllegalArgumentException if this enum type has no constant with the specified name
   */
  public static KDCType translate(String value) {
    if((value == null) || value.isEmpty()) {
      return NONE;
    }else {
      return KDCType.valueOf(value.replace("-", "_").toUpperCase());
    }
  }
}
