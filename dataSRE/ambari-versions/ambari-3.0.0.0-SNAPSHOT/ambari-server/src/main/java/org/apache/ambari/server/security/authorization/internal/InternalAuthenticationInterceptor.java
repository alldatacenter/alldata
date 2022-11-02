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
package org.apache.ambari.server.security.authorization.internal;


import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * Allows running a given code within a security context authenticated with InternalAuthenticationToken.
 * This only works for instances created by Guice. If there's already an Authentication in current security context
 * that will be restored after calling the annotated method.
 */
public class InternalAuthenticationInterceptor implements MethodInterceptor {

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {

    Authentication savedAuthContext = SecurityContextHolder.getContext().getAuthentication();
    try {
      RunWithInternalSecurityContext securityAuthContextAnnotation = invocation.getMethod().getAnnotation(RunWithInternalSecurityContext
        .class);
      InternalAuthenticationToken authenticationToken = new InternalAuthenticationToken(securityAuthContextAnnotation
        .token());
      authenticationToken.setAuthenticated(true);
      SecurityContextHolder.getContext().setAuthentication(authenticationToken);
      return invocation.proceed();
    } finally {
      SecurityContextHolder.getContext().setAuthentication(savedAuthContext);
    }

  }
}
