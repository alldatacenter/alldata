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
package org.apache.drill.exec.rpc.security;

import javax.security.sasl.Sasl;
import java.util.HashMap;
import java.util.Map;

public final class SaslProperties {

  /**
   * All supported Quality of Protection values which can be negotiated
   */
  enum QualityOfProtection {
    AUTHENTICATION("auth"),
    INTEGRITY("auth-int"),
    PRIVACY("auth-conf");

    public final String saslQop;

    QualityOfProtection(String saslQop) {
      this.saslQop = saslQop;
    }

    public String getSaslQop() {
      return saslQop;
    }
  }

  /**
   * Get's the map of minimum set of SaslProperties required during negotiation process either for encryption
   * or authentication
   * @param encryptionEnabled - Flag to determine if property needed is for encryption or authentication
   * @param wrappedChunkSize  - Configured wrappedChunkSize to negotiate for.
   * @return Map of SaslProperties which will be used in negotiation.
   */
  public static Map<String, String> getSaslProperties(boolean encryptionEnabled, int wrappedChunkSize) {
    Map<String, String> saslProps = new HashMap<>();

    if (encryptionEnabled) {
      saslProps.put(Sasl.STRENGTH, "high");
      saslProps.put(Sasl.QOP, QualityOfProtection.PRIVACY.getSaslQop());
      saslProps.put(Sasl.MAX_BUFFER, Integer.toString(wrappedChunkSize));
      saslProps.put(Sasl.POLICY_NOPLAINTEXT, "true");
    } else {
      saslProps.put(Sasl.QOP, QualityOfProtection.AUTHENTICATION.getSaslQop());
    }

    return saslProps;
  }

  private SaslProperties() {

  }
}