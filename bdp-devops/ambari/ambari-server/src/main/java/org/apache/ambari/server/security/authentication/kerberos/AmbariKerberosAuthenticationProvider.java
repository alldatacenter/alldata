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

import javax.inject.Inject;

import org.apache.ambari.server.security.authentication.AmbariProxiedUserDetailsImpl;
import org.apache.ambari.server.security.authentication.tproxy.TrustedProxyAuthenticationDetails;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.kerberos.authentication.KerberosServiceRequestToken;
import org.springframework.security.kerberos.authentication.KerberosTicketValidation;
import org.springframework.security.kerberos.authentication.KerberosTicketValidator;

/**
 * AmbariKerberosAuthenticationProvider is an {@link org.springframework.security.authentication.AuthenticationProvider}
 * implementation used to authenticate users using a Kerberos ticket.
 * <p>
 * In order for this provider to be usable, Kerberos authentication must be enabled. This may be done
 * using the <code>ambari-server setup-kerberos</code> CLI.
 * <p>
 * This implementation supports Trusted Proxy authentication.
 */
public class AmbariKerberosAuthenticationProvider implements AuthenticationProvider, InitializingBean {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariKerberosAuthenticationProvider.class);

  private AmbariAuthToLocalUserDetailsService authToLocalUserDetailsService;
  private AmbariProxiedUserDetailsService proxiedUserDetailsService;
  private KerberosTicketValidator ticketValidator;

  @Inject
  public AmbariKerberosAuthenticationProvider(AmbariAuthToLocalUserDetailsService authToLocalUserDetailsService,
                                              AmbariProxiedUserDetailsService proxiedUserDetailsService,
                                              KerberosTicketValidator ticketValidator) {
    this.authToLocalUserDetailsService = authToLocalUserDetailsService;
    this.proxiedUserDetailsService = proxiedUserDetailsService;
    this.ticketValidator = ticketValidator;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    if (authentication == null) {
      throw new BadCredentialsException("Missing credentials");
    } else if (authentication instanceof KerberosServiceRequestToken) {
      KerberosServiceRequestToken auth = (KerberosServiceRequestToken) authentication;
      byte[] token = auth.getToken();

      LOG.debug("Validating Kerberos token");
      KerberosTicketValidation ticketValidation = ticketValidator.validateTicket(token);
      LOG.debug("Kerberos token validated: {}", ticketValidation.username());

      Object requestDetails = authentication.getDetails();
      UserDetails userDetails;

      if (requestDetails instanceof TrustedProxyAuthenticationDetails) {
        TrustedProxyAuthenticationDetails trustedProxyAuthenticationDetails = (TrustedProxyAuthenticationDetails) requestDetails;
        String proxiedUserName = trustedProxyAuthenticationDetails.getDoAs();

        if (StringUtils.isNotEmpty(proxiedUserName)) {
          String localProxyUserName = authToLocalUserDetailsService.translatePrincipalName(ticketValidation.username());

          userDetails = new AmbariProxiedUserDetailsImpl(
              proxiedUserDetailsService.loadProxiedUser(proxiedUserName, localProxyUserName, trustedProxyAuthenticationDetails),
              new AmbariProxyUserKerberosDetailsImpl(ticketValidation.username(), localProxyUserName));
        } else {
          // This is not a trusted proxy request... handle normally
          userDetails = authToLocalUserDetailsService.loadUserByUsername(ticketValidation.username());
        }
      } else {
        // This is not a trusted proxy request... handle normally
        userDetails = authToLocalUserDetailsService.loadUserByUsername(ticketValidation.username());
      }

      KerberosServiceRequestToken responseAuth = new KerberosServiceRequestToken(userDetails, ticketValidation, userDetails.getAuthorities(), token);
      responseAuth.setDetails(requestDetails);
      return responseAuth;
    } else {
      throw new BadCredentialsException(String.format("Unexpected Authentication class: %s", authentication.getClass().getName()));
    }
  }

  @Override
  public boolean supports(Class<? extends Object> auth) {
    return KerberosServiceRequestToken.class.isAssignableFrom(auth);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
  }

  public void setAuthToLocalUserDetailsService(AmbariAuthToLocalUserDetailsService authToLocalUserDetailsService) {
    this.authToLocalUserDetailsService = authToLocalUserDetailsService;
  }

  public void setProxiedUserDetailsService(AmbariProxiedUserDetailsService proxiedUserDetailsService) {
    this.proxiedUserDetailsService = proxiedUserDetailsService;
  }

  public void setTicketValidator(KerberosTicketValidator ticketValidator) {
    this.ticketValidator = ticketValidator;
  }
}
