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

package org.apache.ambari.server.security.authentication.kerberos;

import org.apache.ambari.server.configuration.Configuration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.kerberos.authentication.KerberosTicketValidation;
import org.springframework.security.kerberos.authentication.KerberosTicketValidator;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
import org.springframework.stereotype.Component;

/**
 * AmbariKerberosTicketValidator is a {@link KerberosTicketValidator} implementation that delegates
 * to a {@link SunJaasKerberosTicketValidator}, if Kerberos authentication is enabled.
 * <p>
 * If Kerberos authentication is enabled, the following properties are set:
 * <ul>
 * <li>{@link SunJaasKerberosTicketValidator#setServicePrincipal(String)} using the Ambari server property from {@link Configuration#KERBEROS_AUTH_SPNEGO_PRINCIPAL}</li>
 * <li>{@link SunJaasKerberosTicketValidator#setKeyTabLocation(Resource)} using the Ambari server property from {@link Configuration#KERBEROS_AUTH_SPNEGO_KEYTAB_FILE}</li>
 * </ul>
 */
@Component
public class AmbariKerberosTicketValidator implements KerberosTicketValidator, InitializingBean {

  private final SunJaasKerberosTicketValidator kerberosTicketValidator;

  /**
   * Creates a new AmbariKerberosTicketValidator
   *
   * @param configuration the Ambari server configuration
   */
  public AmbariKerberosTicketValidator(Configuration configuration) {

    AmbariKerberosAuthenticationProperties properties = (configuration == null)
        ? null
        : configuration.getKerberosAuthenticationProperties();

    if ((properties != null) && properties.isKerberosAuthenticationEnabled()) {
      kerberosTicketValidator = new SunJaasKerberosTicketValidator();
      kerberosTicketValidator.setServicePrincipal(properties.getSpnegoPrincipalName());

      if (properties.getSpnegoKeytabFilePath() != null) {
        kerberosTicketValidator.setKeyTabLocation(new FileSystemResource(properties.getSpnegoKeytabFilePath()));
      }
    } else {
      // Don't create the SunJaasKerberosTicketValidator if Kerberos authentication is not enabled.
      kerberosTicketValidator = null;
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    // If Kerberos authentication is enabled, forward this method invocation to the backing
    // SunJaasKerberosTicketValidator instance.
    if (kerberosTicketValidator != null) {
      kerberosTicketValidator.afterPropertiesSet();
    }
  }

  @Override
  public KerberosTicketValidation validateTicket(byte[] bytes) throws BadCredentialsException {
    // If Kerberos authentication is enabled, forward this method invocation to the backing
    // SunJaasKerberosTicketValidator instance.
    return (kerberosTicketValidator == null)
        ? null
        : kerberosTicketValidator.validateTicket(bytes);
  }

  public void setDebug(boolean debug) {
    // If Kerberos authentication is enabled, forward this method invocation to the backing
    // SunJaasKerberosTicketValidator instance.
    if (kerberosTicketValidator != null) {
      kerberosTicketValidator.setDebug(debug);
    }
  }
}
