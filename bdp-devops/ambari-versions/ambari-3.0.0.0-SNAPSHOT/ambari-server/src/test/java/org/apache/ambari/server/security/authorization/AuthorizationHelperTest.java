/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.authorization;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authentication.AmbariUserAuthentication;
import org.apache.ambari.server.security.authentication.AmbariUserDetailsImpl;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.inject.Provider;

public class AuthorizationHelperTest  extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.NICE)
  private ServletRequestAttributes servletRequestAttributes;

  @Before
  public void setup() {
    // Ensure the security context has been clean up
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @After
  public void cleanup() {
    // Clean up the security context for the next test
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testConvertPrivilegesToAuthorities() throws Exception {
    Collection<PrivilegeEntity> privilegeEntities = new ArrayList<>();

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(1);
    resourceTypeEntity.setName("CLUSTER");

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(1L);
    resourceEntity.setResourceType(resourceTypeEntity);

    PrincipalTypeEntity principalTypeEntity = new PrincipalTypeEntity();
    principalTypeEntity.setId(1);
    principalTypeEntity.setName("USER");

    PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);
    principalEntity.setId(1L);

    PermissionEntity permissionEntity1 = new PermissionEntity();
    permissionEntity1.setPermissionName("Permission1");
    permissionEntity1.setResourceType(resourceTypeEntity);
    permissionEntity1.setId(2);
    permissionEntity1.setPermissionName("CLUSTER.USER");

    PermissionEntity permissionEntity2 = new PermissionEntity();
    permissionEntity2.setPermissionName("Permission1");
    permissionEntity2.setResourceType(resourceTypeEntity);
    permissionEntity2.setId(3);
    permissionEntity2.setPermissionName("CLUSTER.ADMINISTRATOR");

    PrivilegeEntity privilegeEntity1 = new PrivilegeEntity();
    privilegeEntity1.setId(1);
    privilegeEntity1.setPermission(permissionEntity1);
    privilegeEntity1.setPrincipal(principalEntity);
    privilegeEntity1.setResource(resourceEntity);

    PrivilegeEntity privilegeEntity2 = new PrivilegeEntity();
    privilegeEntity2.setId(1);
    privilegeEntity2.setPermission(permissionEntity2);
    privilegeEntity2.setPrincipal(principalEntity);
    privilegeEntity2.setResource(resourceEntity);

    privilegeEntities.add(privilegeEntity1);
    privilegeEntities.add(privilegeEntity2);

    Collection<GrantedAuthority> authorities = new AuthorizationHelper().convertPrivilegesToAuthorities(privilegeEntities);

    assertEquals("Wrong number of authorities", 2, authorities.size());

    Set<String> authorityNames = new HashSet<>();

    for (GrantedAuthority authority : authorities) {
      authorityNames.add(authority.getAuthority());
    }
    Assert.assertTrue(authorityNames.contains("CLUSTER.USER@1"));
    Assert.assertTrue(authorityNames.contains("CLUSTER.ADMINISTRATOR@1"));
  }

  @Test
  public void testAuthName() throws Exception {
    String user = AuthorizationHelper.getAuthenticatedName();
    Assert.assertNull(user);

    Authentication auth = new UsernamePasswordAuthenticationToken("admin", null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    user = AuthorizationHelper.getAuthenticatedName();
    Assert.assertEquals("admin", user);

  }

  @Test
  public void testAuthId() throws Exception {
    Integer userId = AuthorizationHelper.getAuthenticatedId();
    Assert.assertEquals(Integer.valueOf(-1), userId);

    PrincipalEntity principalEntity = new PrincipalEntity();
    UserEntity userEntity = new UserEntity();
    userEntity.setUserId(1);
    userEntity.setPrincipal(principalEntity);
    User user = new User(userEntity);
    Authentication auth = new AmbariUserAuthentication(null, new AmbariUserDetailsImpl(user, null, null));
    SecurityContextHolder.getContext().setAuthentication(auth);

    userId = AuthorizationHelper.getAuthenticatedId();
    Assert.assertEquals(Integer.valueOf(1), userId);
  }

  @Test
  public void testAuthWithoutId() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("admin", null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    Integer userId = AuthorizationHelper.getAuthenticatedId();
    Assert.assertEquals(Integer.valueOf(-1), userId);
  }

  @Test
  public void testIsAuthorized() {

    Provider viewInstanceDAOProvider = createNiceMock(Provider.class);
    Provider privilegeDAOProvider = createNiceMock(Provider.class);

    ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
    PrivilegeDAO privilegeDAO = createNiceMock(PrivilegeDAO.class);

    expect(viewInstanceDAOProvider.get()).andReturn(viewInstanceDAO).anyTimes();
    expect(privilegeDAOProvider.get()).andReturn(privilegeDAO).anyTimes();

    replayAll();

    AuthorizationHelper.viewInstanceDAOProvider = viewInstanceDAOProvider;
    AuthorizationHelper.privilegeDAOProvider = privilegeDAOProvider;



    RoleAuthorizationEntity readOnlyRoleAuthorizationEntity = new RoleAuthorizationEntity();
    readOnlyRoleAuthorizationEntity.setAuthorizationId(RoleAuthorization.CLUSTER_VIEW_METRICS.getId());

    RoleAuthorizationEntity privilegedRoleAuthorizationEntity = new RoleAuthorizationEntity();
    privilegedRoleAuthorizationEntity.setAuthorizationId(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS.getId());

    RoleAuthorizationEntity administratorRoleAuthorizationEntity = new RoleAuthorizationEntity();
    administratorRoleAuthorizationEntity.setAuthorizationId(RoleAuthorization.AMBARI_MANAGE_USERS.getId());

    ResourceTypeEntity ambariResourceTypeEntity = new ResourceTypeEntity();
    ambariResourceTypeEntity.setId(1);
    ambariResourceTypeEntity.setName(ResourceType.AMBARI.name());

    ResourceTypeEntity clusterResourceTypeEntity = new ResourceTypeEntity();
    clusterResourceTypeEntity.setId(1);
    clusterResourceTypeEntity.setName(ResourceType.CLUSTER.name());

    ResourceTypeEntity cluster2ResourceTypeEntity = new ResourceTypeEntity();
    cluster2ResourceTypeEntity.setId(2);
    cluster2ResourceTypeEntity.setName(ResourceType.CLUSTER.name());

    ResourceEntity ambariResourceEntity = new ResourceEntity();
    ambariResourceEntity.setResourceType(ambariResourceTypeEntity);
    ambariResourceEntity.setId(1L);

    ResourceEntity clusterResourceEntity = new ResourceEntity();
    clusterResourceEntity.setResourceType(clusterResourceTypeEntity);
    clusterResourceEntity.setId(1L);

    ResourceEntity cluster2ResourceEntity = new ResourceEntity();
    cluster2ResourceEntity.setResourceType(cluster2ResourceTypeEntity);
    cluster2ResourceEntity.setId(2L);

    PermissionEntity readOnlyPermissionEntity = new PermissionEntity();
    readOnlyPermissionEntity.addAuthorization(readOnlyRoleAuthorizationEntity);

    PermissionEntity privilegedPermissionEntity = new PermissionEntity();
    privilegedPermissionEntity.addAuthorization(readOnlyRoleAuthorizationEntity);
    privilegedPermissionEntity.addAuthorization(privilegedRoleAuthorizationEntity);

    PermissionEntity administratorPermissionEntity = new PermissionEntity();
    administratorPermissionEntity.addAuthorization(readOnlyRoleAuthorizationEntity);
    administratorPermissionEntity.addAuthorization(privilegedRoleAuthorizationEntity);
    administratorPermissionEntity.addAuthorization(administratorRoleAuthorizationEntity);

    PrivilegeEntity readOnlyPrivilegeEntity = new PrivilegeEntity();
    readOnlyPrivilegeEntity.setPermission(readOnlyPermissionEntity);
    readOnlyPrivilegeEntity.setResource(clusterResourceEntity);

    PrivilegeEntity readOnly2PrivilegeEntity = new PrivilegeEntity();
    readOnly2PrivilegeEntity.setPermission(readOnlyPermissionEntity);
    readOnly2PrivilegeEntity.setResource(cluster2ResourceEntity);

    PrivilegeEntity privilegedPrivilegeEntity = new PrivilegeEntity();
    privilegedPrivilegeEntity.setPermission(privilegedPermissionEntity);
    privilegedPrivilegeEntity.setResource(clusterResourceEntity);

    PrivilegeEntity privileged2PrivilegeEntity = new PrivilegeEntity();
    privileged2PrivilegeEntity.setPermission(privilegedPermissionEntity);
    privileged2PrivilegeEntity.setResource(cluster2ResourceEntity);

    PrivilegeEntity administratorPrivilegeEntity = new PrivilegeEntity();
    administratorPrivilegeEntity.setPermission(administratorPermissionEntity);
    administratorPrivilegeEntity.setResource(ambariResourceEntity);

    GrantedAuthority readOnlyAuthority = new AmbariGrantedAuthority(readOnlyPrivilegeEntity);
    GrantedAuthority readOnly2Authority = new AmbariGrantedAuthority(readOnly2PrivilegeEntity);
    GrantedAuthority privilegedAuthority = new AmbariGrantedAuthority(privilegedPrivilegeEntity);
    GrantedAuthority privileged2Authority = new AmbariGrantedAuthority(privileged2PrivilegeEntity);
    GrantedAuthority administratorAuthority = new AmbariGrantedAuthority(administratorPrivilegeEntity);

    Authentication noAccessUser = new TestAuthentication(Collections.<AmbariGrantedAuthority>emptyList());
    Authentication readOnlyUser = new TestAuthentication(Collections.singleton(readOnlyAuthority));
    Authentication privilegedUser = new TestAuthentication(Arrays.asList(readOnlyAuthority, privilegedAuthority));
    Authentication privileged2User = new TestAuthentication(Arrays.asList(readOnly2Authority, privileged2Authority));
    Authentication administratorUser = new TestAuthentication(Collections.singleton(administratorAuthority));

    SecurityContext context = SecurityContextHolder.getContext();

    // No user (explicit)...
    assertFalse(AuthorizationHelper.isAuthorized(null, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_VIEW_METRICS)));

    // No user (from context)
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_VIEW_METRICS)));

    // Explicit user tests...
    assertFalse(AuthorizationHelper.isAuthorized(noAccessUser, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_VIEW_METRICS)));
    assertFalse(AuthorizationHelper.isAuthorized(noAccessUser, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS)));
    assertFalse(AuthorizationHelper.isAuthorized(noAccessUser, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS)));

    assertTrue(AuthorizationHelper.isAuthorized(readOnlyUser, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_VIEW_METRICS)));
    assertFalse(AuthorizationHelper.isAuthorized(readOnlyUser, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS)));
    assertFalse(AuthorizationHelper.isAuthorized(readOnlyUser, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS)));

    assertTrue(AuthorizationHelper.isAuthorized(privilegedUser, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_VIEW_METRICS)));
    assertTrue(AuthorizationHelper.isAuthorized(privilegedUser, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS)));
    assertFalse(AuthorizationHelper.isAuthorized(privilegedUser, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS)));

    assertFalse(AuthorizationHelper.isAuthorized(privileged2User, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_VIEW_METRICS)));
    assertFalse(AuthorizationHelper.isAuthorized(privileged2User, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS)));
    assertFalse(AuthorizationHelper.isAuthorized(privileged2User, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS)));

    assertTrue(AuthorizationHelper.isAuthorized(administratorUser, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_VIEW_METRICS)));
    assertTrue(AuthorizationHelper.isAuthorized(administratorUser, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS)));
    assertTrue(AuthorizationHelper.isAuthorized(administratorUser, ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS)));

    // Context user tests...
    context.setAuthentication(noAccessUser);
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_VIEW_METRICS)));
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS)));
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS)));

    context.setAuthentication(readOnlyUser);
    assertTrue(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_VIEW_METRICS)));
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS)));
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS)));

    context.setAuthentication(privilegedUser);
    assertTrue(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_VIEW_METRICS)));
    assertTrue(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS)));
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS)));

    context.setAuthentication(privileged2User);
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_VIEW_METRICS)));
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS)));
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS)));

    context.setAuthentication(administratorUser);
    assertTrue(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_VIEW_METRICS)));
    assertTrue(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS)));
    assertTrue(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS)));
  }

  @Test
  public void testIsAuthorizedForSpecificView() {
    RoleAuthorizationEntity readOnlyRoleAuthorizationEntity = new RoleAuthorizationEntity();
    readOnlyRoleAuthorizationEntity.setAuthorizationId(RoleAuthorization.CLUSTER_VIEW_METRICS.getId());

    RoleAuthorizationEntity viewUseRoleAuthorizationEntity = new RoleAuthorizationEntity();
    viewUseRoleAuthorizationEntity.setAuthorizationId(RoleAuthorization.VIEW_USE.getId());

    RoleAuthorizationEntity administratorRoleAuthorizationEntity = new RoleAuthorizationEntity();
    administratorRoleAuthorizationEntity.setAuthorizationId(RoleAuthorization.AMBARI_MANAGE_USERS.getId());

    ResourceTypeEntity ambariResourceTypeEntity = new ResourceTypeEntity();
    ambariResourceTypeEntity.setId(1);
    ambariResourceTypeEntity.setName(ResourceType.AMBARI.name());

    ResourceTypeEntity clusterResourceTypeEntity = new ResourceTypeEntity();
    clusterResourceTypeEntity.setId(1);
    clusterResourceTypeEntity.setName(ResourceType.CLUSTER.name());

    ResourceTypeEntity viewResourceTypeEntity = new ResourceTypeEntity();
    viewResourceTypeEntity.setId(30);
    viewResourceTypeEntity.setName(ResourceType.VIEW.name());

    ResourceEntity ambariResourceEntity = new ResourceEntity();
    ambariResourceEntity.setResourceType(ambariResourceTypeEntity);
    ambariResourceEntity.setId(1L);

    ResourceEntity clusterResourceEntity = new ResourceEntity();
    clusterResourceEntity.setResourceType(clusterResourceTypeEntity);
    clusterResourceEntity.setId(1L);

    ResourceEntity viewResourceEntity = new ResourceEntity();
    viewResourceEntity.setResourceType(viewResourceTypeEntity);
    viewResourceEntity.setId(53L);

    PermissionEntity readOnlyPermissionEntity = new PermissionEntity();
    readOnlyPermissionEntity.addAuthorization(readOnlyRoleAuthorizationEntity);

    PermissionEntity viewUsePermissionEntity = new PermissionEntity();
    viewUsePermissionEntity.addAuthorization(readOnlyRoleAuthorizationEntity);
    viewUsePermissionEntity.addAuthorization(viewUseRoleAuthorizationEntity);

    PermissionEntity administratorPermissionEntity = new PermissionEntity();
    administratorPermissionEntity.addAuthorization(readOnlyRoleAuthorizationEntity);
    administratorPermissionEntity.addAuthorization(viewUseRoleAuthorizationEntity);
    administratorPermissionEntity.addAuthorization(administratorRoleAuthorizationEntity);

    PrivilegeEntity readOnlyPrivilegeEntity = new PrivilegeEntity();
    readOnlyPrivilegeEntity.setPermission(readOnlyPermissionEntity);
    readOnlyPrivilegeEntity.setResource(clusterResourceEntity);

    PrivilegeEntity viewUsePrivilegeEntity = new PrivilegeEntity();
    viewUsePrivilegeEntity.setPermission(viewUsePermissionEntity);
    viewUsePrivilegeEntity.setResource(viewResourceEntity);

    PrivilegeEntity administratorPrivilegeEntity = new PrivilegeEntity();
    administratorPrivilegeEntity.setPermission(administratorPermissionEntity);
    administratorPrivilegeEntity.setResource(ambariResourceEntity);

    GrantedAuthority readOnlyAuthority = new AmbariGrantedAuthority(readOnlyPrivilegeEntity);
    GrantedAuthority viewUseAuthority = new AmbariGrantedAuthority(viewUsePrivilegeEntity);
    GrantedAuthority administratorAuthority = new AmbariGrantedAuthority(administratorPrivilegeEntity);

    Authentication readOnlyUser = new TestAuthentication(Collections.singleton(readOnlyAuthority));
    Authentication viewUser = new TestAuthentication(Arrays.asList(readOnlyAuthority, viewUseAuthority));
    Authentication administratorUser = new TestAuthentication(Collections.singleton(administratorAuthority));

    SecurityContext context = SecurityContextHolder.getContext();
    Set<RoleAuthorization> permissionsViewUse = EnumSet.of(RoleAuthorization.VIEW_USE);

    context.setAuthentication(readOnlyUser);
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.VIEW, 53L, permissionsViewUse));
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.VIEW, 50L, permissionsViewUse));

    context.setAuthentication(viewUser);
    assertTrue(AuthorizationHelper.isAuthorized(ResourceType.VIEW, 53L, permissionsViewUse));
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.VIEW, 50L, permissionsViewUse));

    context.setAuthentication(administratorUser);
    assertTrue(AuthorizationHelper.isAuthorized(ResourceType.VIEW, 53L, permissionsViewUse));
    assertTrue(AuthorizationHelper.isAuthorized(ResourceType.VIEW, 50L, permissionsViewUse));
  }

  public void testAddLoginNameAlias() throws Exception {
    // Given
    reset(servletRequestAttributes);

    RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    servletRequestAttributes.setAttribute(eq("loginAlias"), eq("user"), eq(RequestAttributes.SCOPE_SESSION));
    expectLastCall().once();

    replay(servletRequestAttributes);

    // When
    AuthorizationHelper.addLoginNameAlias("user","loginAlias");

    // Then
    verify(servletRequestAttributes);
  }

  @Test
  public void testResolveLoginAliasToUserName() throws Exception {
    // Given
    reset(servletRequestAttributes);

    RequestContextHolder.setRequestAttributes(servletRequestAttributes);

    expect(servletRequestAttributes.getAttribute(eq("loginAlias1"), eq(RequestAttributes.SCOPE_SESSION)))
      .andReturn("user1").atLeastOnce();

    replay(servletRequestAttributes);

    // When
    String user = AuthorizationHelper.resolveLoginAliasToUserName("loginAlias1");

    // Then
    verify(servletRequestAttributes);

    assertEquals("user1", user);
  }

  @Test
  public void testResolveNoLoginAliasToUserName() throws Exception {
    reset(servletRequestAttributes);

    // No request attributes/http session available yet
    RequestContextHolder.setRequestAttributes(null);
    assertEquals("user", AuthorizationHelper.resolveLoginAliasToUserName("user"));


    // request attributes available but user doesn't have any login aliases
    RequestContextHolder.setRequestAttributes(servletRequestAttributes);

    expect(servletRequestAttributes.getAttribute(eq("nosuchalias"), eq(RequestAttributes.SCOPE_SESSION)))
      .andReturn(null).atLeastOnce();

    replay(servletRequestAttributes);

    // When
    String user = AuthorizationHelper.resolveLoginAliasToUserName("nosuchalias");

    // Then
    verify(servletRequestAttributes);

    assertEquals("nosuchalias", user);
  }

  private class TestAuthentication implements Authentication {
    private final Collection<? extends GrantedAuthority> grantedAuthorities;

    public TestAuthentication(Collection<? extends GrantedAuthority> grantedAuthorities) {
      this.grantedAuthorities = grantedAuthorities;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return grantedAuthorities;
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
      return null;
    }

    @Override
    public boolean isAuthenticated() {
      return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

    }

    @Override
    public String getName() {
      return null;
    }
  }
}
