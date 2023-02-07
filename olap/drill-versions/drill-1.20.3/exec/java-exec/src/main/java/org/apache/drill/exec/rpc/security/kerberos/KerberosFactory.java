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
package org.apache.drill.exec.rpc.security.kerberos;

import org.apache.drill.common.KerberosUtil;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.exec.rpc.security.AuthenticatorFactory;
import org.apache.drill.exec.rpc.security.FastSaslClientFactory;
import org.apache.drill.exec.rpc.security.FastSaslServerFactory;
import org.apache.drill.exec.rpc.security.SecurityConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.security.HadoopKerberosName;
import org.apache.hadoop.security.UserGroupInformation;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

public class KerberosFactory implements AuthenticatorFactory {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(KerberosFactory.class);

  private static final String DRILL_SERVICE_NAME = System.getProperty("drill.principal.primary", "drill");

  @Override
  public String getSimpleName() {
    return KerberosUtil.KERBEROS_SIMPLE_NAME;
  }

  @Override
  public UserGroupInformation createAndLoginUser(final Map<String, ?> properties) throws IOException {
    final Configuration conf = new SecurityConfiguration();
    conf.set(CommonConfigurationKeys.HADOOP_SECURITY_AUTHENTICATION,
        UserGroupInformation.AuthenticationMethod.KERBEROS.toString());
    UserGroupInformation.setConfiguration(conf);

    final String keytab = (String) properties.get(DrillProperties.KEYTAB);
    final boolean assumeSubject = properties.containsKey(DrillProperties.KERBEROS_FROM_SUBJECT) &&
        Boolean.parseBoolean((String) properties.get(DrillProperties.KERBEROS_FROM_SUBJECT));
    try {
      final UserGroupInformation ugi;
      if (assumeSubject) {
        ugi = UserGroupInformation.getUGIFromSubject(Subject.getSubject(AccessController.getContext()));
        logger.debug("Assuming subject for {}.", ugi.getShortUserName());
      } else {
        if (keytab != null) {
          ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(
              (String) properties.get(DrillProperties.USER), keytab);
          logger.debug("Logged in {} using keytab.", ugi.getShortUserName());
        } else {
          // includes Kerberos ticket login
          ugi = UserGroupInformation.getCurrentUser();
          logger.debug("Logged in {} using ticket.", ugi.getShortUserName());
        }
      }
      return ugi;
    } catch (final IOException e) {
      logger.debug("Login failed.", e);
      final Throwable cause = e.getCause();
      if (cause instanceof LoginException) {
        throw new SaslException("Failed to login.", cause);
      }
      throw new SaslException("Unexpected failure trying to login.", cause);
    }
  }

  @Override
  public SaslServer createSaslServer(final UserGroupInformation ugi, final Map<String, ?> properties)
      throws SaslException {
    final String qopValue = properties.containsKey(Sasl.QOP) ? properties.get(Sasl.QOP).toString() : "auth";
    try {
      final String primaryName = ugi.getShortUserName();
      final String instanceName = new HadoopKerberosName(ugi.getUserName()).getHostName();

      final SaslServer saslServer = ugi.doAs(new PrivilegedExceptionAction<SaslServer>() {
        @Override
        public SaslServer run() throws Exception {
          return FastSaslServerFactory.getInstance()
              .createSaslServer(KerberosUtil.KERBEROS_SASL_NAME, primaryName, instanceName, properties,
                  new KerberosServerCallbackHandler());
        }
      });
      logger.trace("GSSAPI SaslServer created with QOP {}.", qopValue);
      return saslServer;
    } catch (final UndeclaredThrowableException e) {
      final Throwable cause = e.getCause();
      logger.debug("Authentication failed.", cause);
      if (cause instanceof SaslException) {
        throw (SaslException) cause;
      } else {
        throw new SaslException(String.format("Unexpected failure trying to authenticate using Kerberos with QOP %s",
            qopValue), cause);
      }
    } catch (final IOException | InterruptedException e) {
      logger.debug("Authentication failed.", e);
      throw new SaslException(String.format("Unexpected failure trying to authenticate using Kerberos with QOP %s",
          qopValue), e);
    }
  }

  @Override
  public SaslClient createSaslClient(final UserGroupInformation ugi, final Map<String, ?> properties)
      throws SaslException {
    final String servicePrincipal = getServicePrincipal(properties);

    final String parts[] = KerberosUtil.splitPrincipalIntoParts(servicePrincipal);
    final String serviceName = parts[0];
    final String serviceHostName = parts[1];
    final String qopValue = properties.containsKey(Sasl.QOP) ? properties.get(Sasl.QOP).toString() : "auth";

    // ignore parts[2]; GSSAPI gets the realm info from the ticket
    try {
      final SaslClient saslClient = ugi.doAs((PrivilegedExceptionAction<SaslClient>) () ->
        FastSaslClientFactory.getInstance().createSaslClient(new String[]{ KerberosUtil.KERBEROS_SASL_NAME },
          null /* authorization ID */, serviceName, serviceHostName, properties,
          callbacks -> {
            throw new UnsupportedCallbackException(callbacks[0]);
          }));
      logger.debug("GSSAPI SaslClient created to authenticate to {} running on {} with QOP value {}",
          serviceName, serviceHostName, qopValue);
      return saslClient;
    } catch (final UndeclaredThrowableException e) {
      logger.debug("Authentication failed.", e);
      throw new SaslException(String.format("Unexpected failure trying to authenticate to %s using GSSAPI with QOP %s",
          serviceHostName, qopValue), e.getCause());
    } catch (final IOException | InterruptedException e) {
      logger.debug("Authentication failed.", e);
      if (e instanceof SaslException) {
        throw (SaslException) e;
      }
      throw new SaslException(String.format("Unexpected failure trying to authenticate to %s using GSSAPI with QOP %s",
          serviceHostName, qopValue), e);
    }
  }

  @Override
  public void close() throws Exception {
    // no-op
  }

  private static class KerberosServerCallbackHandler implements CallbackHandler {

    @Override
    public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      for (final Callback callback : callbacks) {
        if (callback instanceof AuthorizeCallback) {
          final AuthorizeCallback authorizeCallback = (AuthorizeCallback) callback;
          if (!authorizeCallback.getAuthenticationID()
              .equals(authorizeCallback.getAuthorizationID())) {
            throw new SaslException("Drill expects authorization ID and authentication ID to match. " +
                "Use inbound impersonation feature so one entity can act on behalf of another.");
          } else {
            authorizeCallback.setAuthorized(true);
          }
        } else {
          throw new UnsupportedCallbackException(callback);
        }
      }
    }
  }

  private static String getServicePrincipal(final Map<String, ?> properties) throws SaslException {
    final String principal = (String) properties.get(DrillProperties.SERVICE_PRINCIPAL);
    if (principal != null) {
      return principal;
    }

    final String serviceHostname = (String) properties.get(DrillProperties.SERVICE_HOST);
    if (serviceHostname == null) {
      throw new SaslException("Unknown Drillbit hostname. Check connection parameters?");
    }

    final String serviceName = (String) properties.get(DrillProperties.SERVICE_NAME);
    final String realm = (String) properties.get(DrillProperties.REALM);
    try {
      return KerberosUtil.getPrincipalFromParts(
          serviceName == null ? DRILL_SERVICE_NAME : serviceName,
          serviceHostname.toLowerCase(), // see HADOOP-7988
          realm == null ? KerberosUtil.getDefaultRealm() : realm
      );
    } catch (final ClassNotFoundException | NoSuchMethodException |
        IllegalAccessException | InvocationTargetException e) {
      throw new SaslException("Could not resolve realm information. Please set explicitly in connection parameters.");
    }
  }
}
