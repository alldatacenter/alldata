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

package org.apache.ambari.server.audit.request.creator;

import java.util.Collection;
import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

public class AuditEventCreatorTestBase {

  protected final static String userName = "testuser";

  @BeforeClass
  public static void beforeClass() {
    SecurityContextHolder.setContext(new SecurityContext() {
      @Override
      public Authentication getAuthentication() {
        return new Authentication() {
          @Override
          public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
          }

          @Override
          public Object getCredentials() {
            return null;
          }

          @Override
          public Object getDetails() {
            return null;
          }

          @Override
          public Object getPrincipal() {
            return new User(userName, "password", Collections.emptyList());
          }

          @Override
          public boolean isAuthenticated() {
            return true;
          }

          @Override
          public void setAuthenticated(boolean b) throws IllegalArgumentException {

          }

          @Override
          public String getName() {
            return ((User) getPrincipal()).getUsername();
          }
        };
      }

      @Override
      public void setAuthentication(Authentication authentication) {

      }
    });
  }
  @AfterClass
  public static void afterClass(){
    SecurityContextHolder.clearContext();
  }
}
