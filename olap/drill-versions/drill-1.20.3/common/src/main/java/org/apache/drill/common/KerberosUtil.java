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
package org.apache.drill.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkNotNull;
import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkState;

public final class KerberosUtil {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(KerberosUtil.class);

  // Per this link http://docs.oracle.com/javase/jndi/tutorial/ldap/security/gssapi.html
  // "... GSS-API SASL mechanism was retrofitted to mean only Kerberos v5 ..."
  public static final String KERBEROS_SASL_NAME = "GSSAPI";

  public static final String KERBEROS_SIMPLE_NAME = "KERBEROS";

  public static final String HOSTNAME_PATTERN = "_HOST";

  /**
   * Returns principal of format primary/instance@REALM.
   *
   * @param primary non-null primary component
   * @param instance non-null instance component, can be empty string
   * @param realm non-null realm component
   * @return principal of format primary/instance@REALM or primary@REALM
   */
  public static String getPrincipalFromParts(final String primary, final String instance, final String realm) {
    checkNotNull(primary);
    checkNotNull(realm);

    return primary +
        ((!"".equals(instance)) ? "/" + instance : "")
        + "@" + realm;
  }

  /**
   * Expects principal of the format primary/instance@REALM or primary@REALM.
   *
   * @param principal principal
   * @return components
   */
  public static String[] splitPrincipalIntoParts(final String principal) {
    final String[] components = principal.split("[/@]");
    checkState(components.length < 4);
    checkState(components.length > 1);
    checkNotNull(components[0]);
    checkNotNull(components[1]);

    if (components.length == 2) {
      return new String[] { components[0], "", components[1] };
    } else {
      checkNotNull(components[2]);
      return components;
    }
  }

  public static String canonicalizeInstanceName(String instanceName, final String canonicalName) {
    if (instanceName == null || HOSTNAME_PATTERN.equalsIgnoreCase(instanceName)) {
      instanceName = canonicalName;
    }

    final String lowercaseName = instanceName.toLowerCase();
    if (!instanceName.equals(lowercaseName)) {
      logger.warn("Converting service name ({}) to lowercase, see HADOOP-7988.", instanceName);
    }
    return lowercaseName;
  }

  public static String getDefaultRealm() throws ClassNotFoundException, NoSuchMethodException,
      IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    final Class<?> classRef = System.getProperty("java.vendor").contains("IBM") ?
        Class.forName("com.ibm.security.krb5.internal.Config") :
        Class.forName("sun.security.krb5.Config");

    final Method getInstanceMethod = classRef.getMethod("getInstance", new Class[0]);
    final Object kerbConf = getInstanceMethod.invoke(classRef, new Object[0]);
    final Method getDefaultRealmMethod = classRef.getDeclaredMethod("getDefaultRealm", new Class[0]);
    return (String) getDefaultRealmMethod.invoke(kerbConf, new Object[0]);
  }

  // prevent instantiation
  private KerberosUtil() {
  }
}
