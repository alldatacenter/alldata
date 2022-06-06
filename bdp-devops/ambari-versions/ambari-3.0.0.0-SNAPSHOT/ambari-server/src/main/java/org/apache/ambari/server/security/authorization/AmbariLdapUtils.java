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

package org.apache.ambari.server.security.authorization;

import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.support.LdapUtils;

import com.google.common.base.Preconditions;

/**
 * Provides utility methods for LDAP related functionality
 */
public class AmbariLdapUtils {

  private static final Logger LOG = LoggerFactory.getLogger(AmbariLdapUtils.class);

  /**
   * Regexp to verify if user login name beside user contains domain information as well (User principal name format).
   */
  private static final Pattern UPN_FORMAT = Pattern.compile(".+@\\w+(\\.\\w+)*");

  /**
   * Returns true if the given user name contains domain name as well (e.g. username@domain)
   *
   * @param loginName the login name to verify if it contains domain information.
   * @return true if the given user name contains domain name as well; false otherwise
   */
  public static boolean isUserPrincipalNameFormat(String loginName) {
    return UPN_FORMAT.matcher(loginName).matches();
  }


  /**
   * Determine that the full DN of an LDAP object is in/out of the base DN scope.
   *
   * @param adapter used for get the full dn from the ldap query response
   * @param baseDn  the base distinguished name
   * @return true if the object is out of scope; false otherwise
   */
  public static boolean isLdapObjectOutOfScopeFromBaseDn(DirContextAdapter adapter, String baseDn) {
    boolean isOutOfScope = true;
    try {
      Name dn = adapter.getDn();
      Preconditions.checkArgument(dn != null, "DN cannot be null in LDAP response object");

      Name fullDn = getFullDn(dn, adapter);
      Name base = LdapUtils.newLdapName(baseDn);
      if (fullDn.startsWith(base)) {
        isOutOfScope = false;
      }
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
    return isOutOfScope;
  }

  /**
   * Ensures the given distinguished name is an absolute value rather than a name relative to the context.
   *
   * @param dn      a distinguished name
   * @param context the context containing the base distinguished name
   * @return the absolute distinguished name
   */
  public static Name getFullDn(String dn, Context context) throws NamingException {
    return getFullDn(LdapUtils.newLdapName(dn), context);
  }

  /**
   * Ensures the given distinguished name is an absolute value rather than a name relative to the context.
   *
   * @param dn      a distinguished name
   * @param context the context containing the base distinguished name
   * @return the absolute distinguished name
   */
  public static Name getFullDn(Name dn, Context context) throws NamingException {
    return getFullDn(LdapUtils.newLdapName(dn), LdapUtils.newLdapName(context.getNameInNamespace()));
  }

  /**
   * Ensures the given distinguished name is an absolute value rather than a name relative to the context.
   *
   * @param dn     a distinguished name
   * @param baseDn the base distinguished name
   * @return the absolute distinguished name
   */
  public static Name getFullDn(Name dn, Name baseDn) {

    if (dn.startsWith(baseDn)) {
      return dn;
    } else {
      try {
        //Copy the baseDN so we do not change the one that is passed in...
        baseDn = LdapUtils.newLdapName(baseDn);
        baseDn.addAll(dn);
      } catch (InvalidNameException e) {
        LOG.error(e.getMessage());
      }
      return baseDn;
    }
  }
}
