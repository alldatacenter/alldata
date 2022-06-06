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

package org.apache.ambari.server.security.authentication.pam;

import javax.inject.Singleton;

import org.apache.ambari.server.configuration.Configuration;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;

/**
 * PamAuthenticationFactory returns Pam library instances.
 */
@Singleton
public class PamAuthenticationFactory {
  private static final Logger LOG = LoggerFactory.getLogger(PamAuthenticationFactory.class);

  public PAM createInstance(Configuration configuration) {
    String pamConfig = (configuration == null) ? null : configuration.getPamConfigurationFile();
    return createInstance(pamConfig);
  }

  public PAM createInstance(String pamConfig) {
    try {
      //Set PAM configuration file (found under /etc/pam.d)
      return new PAM(pamConfig);
    } catch (PAMException e) {
      String message = String.format("Unable to Initialize PAM: %s", e.getMessage());
      LOG.error(message, e);
      throw new AuthenticationServiceException(message, e);
    }
  }
}
