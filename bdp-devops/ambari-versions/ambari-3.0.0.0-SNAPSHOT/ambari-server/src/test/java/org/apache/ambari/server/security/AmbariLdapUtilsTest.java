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

package org.apache.ambari.server.security;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.ambari.server.security.authorization.AmbariLdapUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.support.LdapUtils;

public class AmbariLdapUtilsTest {

  private static final String USER_BASE_DN = "ou=hdp,ou=Users,dc=apache,dc=org";
  private static final String USER_RELATIVE_DN = "uid=myuser";
  private static final String USER_DN = USER_RELATIVE_DN + "," + USER_BASE_DN;

  @Test
  public void testIsUserPrincipalNameFormat_True() throws Exception {
    // Given
    String testLoginName = "testuser@domain1.d_1.com";

    // When
    boolean isUserPrincipalNameFormat = AmbariLdapUtils.isUserPrincipalNameFormat(testLoginName);

    // Then
    assertTrue(isUserPrincipalNameFormat);
  }

  @Test
  public void testIsUserPrincipalNameFormatMultipleAtSign_True() throws Exception {
    // Given
    String testLoginName = "@testuser@domain1.d_1.com";

    // When
    boolean isUserPrincipalNameFormat = AmbariLdapUtils.isUserPrincipalNameFormat(testLoginName);

    // Then
    assertTrue(isUserPrincipalNameFormat);
  }

  @Test
  public void testIsUserPrincipalNameFormat_False() throws Exception {
    // Given
    String testLoginName = "testuser";

    // When
    boolean isUserPrincipalNameFormat = AmbariLdapUtils.isUserPrincipalNameFormat(testLoginName);

    // Then
    assertFalse(isUserPrincipalNameFormat);
  }

  @Test
  public void testIsUserPrincipalNameFormatWithAtSign_False() throws Exception {
    // Given
    String testLoginName = "@testuser";

    // When
    boolean isUserPrincipalNameFormat = AmbariLdapUtils.isUserPrincipalNameFormat(testLoginName);

    // Then
    assertFalse(isUserPrincipalNameFormat);
  }

  @Test
  public void testIsUserPrincipalNameFormatWithAtSign1_False() throws Exception {
    // Given
    String testLoginName = "testuser@";

    // When
    boolean isUserPrincipalNameFormat = AmbariLdapUtils.isUserPrincipalNameFormat(testLoginName);

    // Then
    assertFalse(isUserPrincipalNameFormat);
  }

  @Test
  public void testIsLdapObjectOutOfScopeFromBaseDn() throws NamingException {
    // GIVEN
    Name fullDn = LdapUtils.newLdapName(USER_DN);

    DirContextAdapter adapter = createNiceMock(DirContextAdapter.class);
    expect(adapter.getDn()).andReturn(fullDn);
    expect(adapter.getNameInNamespace()).andReturn(USER_DN);

    replay(adapter);

    // WHEN
    boolean isOutOfScopeFromBaseDN = AmbariLdapUtils.isLdapObjectOutOfScopeFromBaseDn(adapter, "dc=apache,dc=org");
    // THEN
    assertFalse(isOutOfScopeFromBaseDN);

    verify(adapter);
  }

  @Test
  public void testIsLdapObjectOutOfScopeFromBaseDn_dnOutOfScope() throws NamingException {
    // GIVEN
    Name fullDn = LdapUtils.newLdapName(USER_DN);
    DirContextAdapter adapter = createNiceMock(DirContextAdapter.class);

    expect(adapter.getDn()).andReturn(fullDn);
    expect(adapter.getNameInNamespace()).andReturn(USER_DN);

    replay(adapter);

    // WHEN
    boolean isOutOfScopeFromBaseDN = AmbariLdapUtils.isLdapObjectOutOfScopeFromBaseDn(adapter, "dc=apache,dc=org,ou=custom");
    // THEN
    assertTrue(isOutOfScopeFromBaseDN);

    verify(adapter);
  }

  @Test
  public void testGetFullDn() throws Exception {

    DirContextAdapter adapterFullDn = createStrictMock(DirContextAdapter.class);
    expect(adapterFullDn.getNameInNamespace()).andReturn(USER_DN).anyTimes();

    DirContextAdapter adapterBaseDn = createStrictMock(DirContextAdapter.class);
    expect(adapterBaseDn.getNameInNamespace()).andReturn(USER_BASE_DN).anyTimes();

    Name absoluteDn = LdapUtils.newLdapName(USER_DN);
    Name relativeDn = LdapUtils.newLdapName(USER_RELATIVE_DN);

    replay(adapterFullDn, adapterBaseDn);

    Name fullDn;

    // ****************************
    // getFullDn(Name, Context)
    fullDn = AmbariLdapUtils.getFullDn(absoluteDn, adapterFullDn);
    Assert.assertEquals(absoluteDn, fullDn);

    fullDn = AmbariLdapUtils.getFullDn(absoluteDn, adapterBaseDn);
    Assert.assertEquals(absoluteDn, fullDn);

    fullDn = AmbariLdapUtils.getFullDn(relativeDn, adapterBaseDn);
    Assert.assertEquals(absoluteDn, fullDn);
    // ****************************


    // ****************************
    // getFullDn(String, Context)
    fullDn = AmbariLdapUtils.getFullDn(absoluteDn.toString(), adapterFullDn);
    Assert.assertEquals(absoluteDn, fullDn);

    fullDn = AmbariLdapUtils.getFullDn(absoluteDn.toString(), adapterBaseDn);
    Assert.assertEquals(absoluteDn, fullDn);

    fullDn = AmbariLdapUtils.getFullDn(relativeDn.toString(), adapterBaseDn);
    Assert.assertEquals(absoluteDn, fullDn);
    // ****************************

    // ****************************
    // getFullDn(Name, Name)
    Name nameInNamespaceFullDn = LdapUtils.newLdapName(adapterFullDn.getNameInNamespace());
    Name nameInNamespaceBaseDn = LdapUtils.newLdapName(adapterBaseDn.getNameInNamespace());

    fullDn = AmbariLdapUtils.getFullDn(absoluteDn, nameInNamespaceFullDn);
    Assert.assertEquals(absoluteDn, fullDn);

    fullDn = AmbariLdapUtils.getFullDn(absoluteDn, nameInNamespaceBaseDn);
    Assert.assertEquals(absoluteDn, fullDn);

    fullDn = AmbariLdapUtils.getFullDn(relativeDn, nameInNamespaceBaseDn);
    Assert.assertEquals(absoluteDn, fullDn);

    // Make sure nameInNamespace was not altered
    Assert.assertEquals(adapterFullDn.getNameInNamespace(), nameInNamespaceFullDn.toString());
    Assert.assertEquals(adapterBaseDn.getNameInNamespace(), nameInNamespaceBaseDn.toString());
    // ****************************

    verify(adapterFullDn, adapterBaseDn);
  }
}
