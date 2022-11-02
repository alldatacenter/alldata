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
package org.apache.ambari.server.security.authentication;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.security.authorization.UserName;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;

public class AmbariLocalAuthenticationProviderTest extends AbstractAuthenticationProviderTest {

  private static final String TEST_USER_PASS = "userPass";
  private static final String TEST_USER_INCORRECT_PASS = "userIncorrectPass";

  @Override
  protected AuthenticationProvider getAuthenticationProvider(Injector injector) {
    return injector.getInstance(AmbariLocalAuthenticationProvider.class);
  }

  @Override
  protected Authentication getAuthentication(boolean correctUsername, boolean correctCredential) {
    return new UsernamePasswordAuthenticationToken(
        correctUsername ? TEST_USER_NAME : "incorrect_username",
        correctCredential ? TEST_USER_PASS : TEST_USER_INCORRECT_PASS
    );
  }

  @Override
  protected UserEntity getUserEntity(Injector injector, String username, int consecutiveFailures, boolean active) {
    PrincipalEntity principalEntity = new PrincipalEntity();

    UserAuthenticationEntity userAuthenticationEntity = new UserAuthenticationEntity();
    userAuthenticationEntity.setAuthenticationType(UserAuthenticationType.LOCAL);
    userAuthenticationEntity.setAuthenticationKey(injector.getInstance(PasswordEncoder.class).encode(TEST_USER_PASS));

    UserEntity userEntity = new UserEntity();
    userEntity.setUserId(1);
    userEntity.setUserName(UserName.fromString(username).toString());
    userEntity.setPrincipal(principalEntity);
    userEntity.setAuthenticationEntities(Collections.singletonList(userAuthenticationEntity));
    userEntity.setConsecutiveFailures(consecutiveFailures);
    userEntity.setActive(active);
    return userEntity;
  }

  @Override
  protected Module getAdditionalModule() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
      }
    };
  }

  @Override
  protected void validateAuthenticationResult(AmbariUserAuthentication result) {
    assertEquals((Integer) 1, result.getUserId());
    assertEquals((Integer) 1, (result.getPrincipal()).getUserId());
  }

}
