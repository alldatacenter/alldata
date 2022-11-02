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

package org.apache.ambari.server.security.authentication.jwt;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationEventHandler;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationException;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationFilter;
import org.apache.ambari.server.security.authentication.AmbariDelegatingAuthenticationFilter;
import org.apache.ambari.server.security.authentication.AmbariUserAuthentication;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;

/**
 * AmbariJwtAuthenticationFilter is used to validate JWT token and authenticate users.
 * <p>
 * This authentication filter is expected to be used withing an {@link AmbariDelegatingAuthenticationFilter}.
 *
 * @see AmbariDelegatingAuthenticationFilter
 */
@Component
@Order(1)
public class AmbariJwtAuthenticationFilter implements AmbariAuthenticationFilter {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariJwtAuthenticationFilter.class);

  /**
   * Ambari authentication event handler
   */
  private final AmbariAuthenticationEventHandler eventHandler;

  /**
   * Authentication entry point implementation
   */
  private final AuthenticationEntryPoint ambariEntryPoint;

  /**
   * The JWT authentication provider
   */
  private final AuthenticationProvider authenticationProvider;

  /**
   * Authentication properties provider for JWT authentication
   */
  private final JwtAuthenticationPropertiesProvider propertiesProvider;


  /**
   * Constructor.
   *
   * @param ambariEntryPoint   the Spring entry point
   * @param propertiesProvider a provider for the SSO-related Ambari configuration
   * @param eventHandler       the Ambari authentication event handler
   */
  AmbariJwtAuthenticationFilter(AuthenticationEntryPoint ambariEntryPoint,
                                JwtAuthenticationPropertiesProvider propertiesProvider,
                                AmbariJwtAuthenticationProvider authenticationProvider,
                                AmbariAuthenticationEventHandler eventHandler) {
    if (eventHandler == null) {
      throw new IllegalArgumentException("The AmbariAuthenticationEventHandler must not be null");
    }

    this.ambariEntryPoint = ambariEntryPoint;
    this.eventHandler = eventHandler;

    this.propertiesProvider = propertiesProvider;
    this.authenticationProvider = authenticationProvider;
  }

  /**
   * Tests to see if this JwtAuthenticationFilter shold be applied in the authentication
   * filter chain.
   *
   * <code>true</code> will be returned if JWT authentication is enabled and the HTTP request contains
   * a JWT authentication token cookie; otherwise <code>false</code> will be returned.
   *
   * @param httpServletRequest the HttpServletRequest the HTTP service request
   * @return <code>true</code> if the HTTP request contains the basic authentication header; otherwise <code>false</code>
   */
  @Override
  public boolean shouldApply(HttpServletRequest httpServletRequest) {
    boolean shouldApply = false;

    JwtAuthenticationProperties jwtProperties = propertiesProvider.get();
    if (jwtProperties != null && jwtProperties.isEnabledForAmbari()) {
      String serializedJWT = getJWTFromCookie(httpServletRequest);
      shouldApply = (serializedJWT != null && isAuthenticationRequired(serializedJWT));
    }

    return shouldApply;
  }

  @Override
  public boolean shouldIncrementFailureCount() {
    return false;
  }

  @Override
  public void init(FilterConfig filterConfig) {

  }

  /**
   * Checks whether the authentication information is filled. If it is not, then a login failed audit event is logged
   *
   * @param servletRequest  the request
   * @param servletResponse the response
   * @param chain           the Spring filter chain
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {

    eventHandler.beforeAttemptAuthentication(this, servletRequest, servletResponse);

    JwtAuthenticationProperties jwtProperties = propertiesProvider.get();
    if (jwtProperties == null || !jwtProperties.isEnabledForAmbari()) {
      //disable filter if not configured
      chain.doFilter(servletRequest, servletResponse);
      return;
    }

    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
    HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

    try {
      String serializedJWT = getJWTFromCookie(httpServletRequest);
      if (serializedJWT != null && isAuthenticationRequired(serializedJWT)) {
        try {
          SignedJWT jwtToken = SignedJWT.parse(serializedJWT);

          boolean valid = validateToken(jwtToken);

          if (valid) {
            String userName = jwtToken.getJWTClaimsSet().getSubject();

            Authentication authentication = authenticationProvider.authenticate(new JwtAuthenticationToken(userName, serializedJWT, null));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            eventHandler.onSuccessfulAuthentication(this, httpServletRequest, httpServletResponse, authentication);
          } else {
            throw new BadCredentialsException("Invalid JWT token");
          }
        } catch (ParseException e) {
          LOG.warn("Unable to parse the JWT token", e);
          throw new BadCredentialsException("Unable to parse the JWT token - " + e.getLocalizedMessage());
        }
      } else {
        LOG.trace("No JWT cookie found, do nothing");
      }

      chain.doFilter(servletRequest, servletResponse);
    } catch (AuthenticationException e) {
      LOG.warn("JWT authentication failed - {}", e.getLocalizedMessage());

      //clear security context if authentication was required, but failed
      SecurityContextHolder.clearContext();

      AmbariAuthenticationException cause;
      if (e instanceof AmbariAuthenticationException) {
        cause = (AmbariAuthenticationException) e;
      } else {
        cause = new AmbariAuthenticationException(null, e.getMessage(), false, e);
      }

      eventHandler.onUnsuccessfulAuthentication(this, httpServletRequest, httpServletResponse, cause);

      //used to indicate authentication failure, not used here as we have more than one filter
      ambariEntryPoint.commence(httpServletRequest, httpServletResponse, e);
    }
  }

  @Override
  public void destroy() {
  }

  /**
   * Do not try to validate JWT if user already authenticated via other provider
   *
   * @return true, if JWT validation required
   */
  private boolean isAuthenticationRequired(String token) {
    Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();

    //authenticate if no auth
    if (existingAuth == null || !existingAuth.isAuthenticated()) {
      return true;
    }

    //revalidate if token was changed
    if (existingAuth instanceof AmbariUserAuthentication && !StringUtils.equals(token, (String) existingAuth.getCredentials())) {
      return true;
    }

    //always try to authenticate in case of anonymous user
    return (existingAuth instanceof AnonymousAuthenticationToken);
  }

  /**
   * Encapsulate the acquisition of the JWT token from HTTP cookies within the
   * request.
   *
   * @param req servlet request to get the JWT token from
   * @return serialized JWT token
   */
  String getJWTFromCookie(HttpServletRequest req) {
    String serializedJWT = null;
    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      JwtAuthenticationProperties jwtProperties = propertiesProvider.get();
      String jwtCookieName = (jwtProperties == null) ? null : jwtProperties.getCookieName();
      if (StringUtils.isEmpty(jwtCookieName)) {
        jwtCookieName = AmbariServerConfigurationKey.SSO_JWT_COOKIE_NAME.getDefaultValue();
      }

      for (Cookie cookie : cookies) {
        if (jwtCookieName.equals(cookie.getName())) {
          LOG.info("{} cookie has been found and is being processed", jwtCookieName);
          serializedJWT = cookie.getValue();
          break;
        }
      }
    }
    return serializedJWT;
  }

  /**
   * This method provides a single method for validating the JWT for use in
   * request processing. It provides for the override of specific aspects of
   * this implementation through submethods used within but also allows for the
   * override of the entire token validation algorithm.
   *
   * @param jwtToken the token to validate
   * @return true if valid
   */
  private boolean validateToken(SignedJWT jwtToken) {
    boolean sigValid = validateSignature(jwtToken);
    if (!sigValid) {
      LOG.warn("Signature could not be verified");
    }
    boolean audValid = validateAudiences(jwtToken);
    if (!audValid) {
      LOG.warn("Audience validation failed.");
    }
    boolean expValid = validateExpiration(jwtToken);
    if (!expValid) {
      LOG.info("Expiration validation failed.");
    }

    return sigValid && audValid && expValid;
  }

  /**
   * Verify the signature of the JWT token in this method. This method depends
   * on the public key that was established during init based upon the
   * provisioned public key. Override this method in subclasses in order to
   * customize the signature verification behavior.
   *
   * @param jwtToken the token that contains the signature to be validated
   * @return valid true if signature verifies successfully; false otherwise
   */
  boolean validateSignature(SignedJWT jwtToken) {
    boolean valid = false;
    if (JWSObject.State.SIGNED == jwtToken.getState()) {
      LOG.debug("JWT token is in a SIGNED state");
      if (jwtToken.getSignature() != null) {
        LOG.debug("JWT token signature is not null");

        JwtAuthenticationProperties jwtProperties = propertiesProvider.get();
        RSAPublicKey publicKey = (jwtProperties == null) ? null : jwtProperties.getPublicKey();
        if (publicKey == null) {
          LOG.warn("SSO server public key has not be set, validation of the JWT token cannot be performed.");
        } else {
          try {
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (jwtToken.verify(verifier)) {
              valid = true;
              LOG.debug("JWT token has been successfully verified");
            } else {
              LOG.warn("JWT signature verification failed.");
            }
          } catch (JOSEException je) {
            LOG.warn("Error while validating signature", je);
          }
        }
      }
    }
    return valid;
  }

  /**
   * Validate whether any of the accepted audience claims is present in the
   * issued token claims list for audience. Override this method in subclasses
   * in order to customize the audience validation behavior.
   *
   * @param jwtToken the JWT token where the allowed audiences will be found
   * @return true if an expected audience is present, otherwise false
   */
  boolean validateAudiences(SignedJWT jwtToken) {
    boolean valid = false;
    try {
      List<String> tokenAudienceList = jwtToken.getJWTClaimsSet().getAudience();
      JwtAuthenticationProperties jwtProperties = propertiesProvider.get();
      List<String> audiences = (jwtProperties == null) ? null : jwtProperties.getAudiences();

      // if there were no expected audiences configured then just
      // consider any audience acceptable
      if (audiences == null) {
        valid = true;
      } else {
        // if any of the configured audiences is found then consider it
        // acceptable
        if (tokenAudienceList == null) {
          LOG.warn("JWT token has no audiences, validation failed.");
          return false;
        }
        LOG.info("Audience List: {}", audiences);
        for (String aud : tokenAudienceList) {
          LOG.info("Found audience: {}", aud);
          if (audiences.contains(aud)) {
            LOG.debug("JWT token audience has been successfully validated");
            valid = true;
            break;
          }
        }
        if (!valid) {
          LOG.warn("JWT audience validation failed.");
        }
      }
    } catch (ParseException pe) {
      LOG.warn("Unable to parse the JWT token.", pe);
    }
    return valid;
  }

  /**
   * Validate that the expiration time of the JWT token has not been violated.
   * If it has then throw an AuthenticationException. Override this method in
   * subclasses in order to customize the expiration validation behavior.
   *
   * @param jwtToken the token that contains the expiration date to validate
   * @return valid true if the token has not expired; false otherwise
   */
  boolean validateExpiration(SignedJWT jwtToken) {
    boolean valid = false;
    try {
      Date expires = jwtToken.getJWTClaimsSet().getExpirationTime();
      if (expires == null || new Date().before(expires)) {
        LOG.debug("JWT token expiration date has been successfully validated");
        valid = true;
      } else {
        LOG.warn("JWT expiration date validation failed.");
      }
    } catch (ParseException pe) {
      LOG.warn("JWT expiration date validation failed.", pe);
    }
    return valid;
  }
}
