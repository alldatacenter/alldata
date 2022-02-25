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

package org.apache.ambari.server.security.credential;

/**
 * CredentialFactory create Credential instances after determing what type of Credential is represented
 * by the value data.
 */
public class CredentialFactory {
  /**
   * Give a credential value (assumed from a keystore), attempt to determine what type of Credential
   * is represented and create the appropriate Credential implementation.
   * <p/>
   * For example, if the raw value starts with "PrincipalKeyCredential" it is determined to be
   * a PrincipalKeyCredential.  A new PrincipalKeyCredential is created and then returned.
   *
   * @param value the raw credential value
   * @return a Credential
   * @throws InvalidCredentialValueException if an error occurs while parsing the raw credential value
   */
  public static Credential createCredential(char[] value) throws InvalidCredentialValueException {

    if (value == null) {
      return null;
    } else {
      String valueString = String.valueOf(value);

      if (PrincipalKeyCredential.isValidValue(valueString)) {
        return PrincipalKeyCredential.fromValue(valueString);
      } else {
        return GenericKeyCredential.fromValue(valueString);
      }
    }
  }
}
