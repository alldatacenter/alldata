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

package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.expect;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.KerberosKeytabPrincipalDAO;
import org.apache.ambari.server.orm.dao.KerberosPrincipalDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalType;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;


/**
 * Tests for the host Kerberos identity resource provider.
 */
public class HostKerberosIdentityResourceProviderTest extends EasyMockSupport {
  @Test(expected = org.apache.ambari.server.controller.spi.SystemException.class)
  public void testCreateResources() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    ResourceProvider provider = new HostKerberosIdentityResourceProvider(managementController);

    // Create a property set of an single empty map.  It shouldn't make a difference what this is
    // since this HostKerberosIdentityResourceProvider is a read-only provider and should throw
    // a org.apache.ambari.server.controller.spi.SystemException exception
    Set<Map<String, Object>> propertySet = Collections.singleton(Collections.<String, Object>emptyMap());

    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);
  }

  @Test(expected = org.apache.ambari.server.controller.spi.SystemException.class)
  public void testUpdateResources() throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    ResourceProvider provider = new HostKerberosIdentityResourceProvider(managementController);

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_HOST_NAME_PROPERTY_ID, "Host100");
    properties.put(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_NAME_PROPERTY_ID, "principal@REALM");
    properties.put(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_LOCAL_USERNAME_PROPERTY_ID, "userA");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    Predicate predicate = new PredicateBuilder()
        .property(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_CLUSTER_NAME_PROPERTY_ID)
        .equals("Cluster100")
        .and()
        .property(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_HOST_NAME_PROPERTY_ID)
        .equals("Host100")
        .and()
        .property(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_NAME_PROPERTY_ID)
        .equals("principal@REALM").toPredicate();

    provider.updateResources(request, predicate);
  }

  @Test(expected = org.apache.ambari.server.controller.spi.SystemException.class)
  public void testDeleteResources() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    ResourceProvider provider = new HostKerberosIdentityResourceProvider(managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider) provider).addObserver(observer);

    Predicate predicate = new PredicateBuilder()
        .property(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_CLUSTER_NAME_PROPERTY_ID)
        .equals("Cluster100")
        .and()
        .property(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_HOST_NAME_PROPERTY_ID)
        .equals("Host100")
        .and()
        .property(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_NAME_PROPERTY_ID)
        .equals("principal@REALM").toPredicate();

    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);
  }


  @Test
  public void testGetResources() throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    KerberosPrincipalDescriptor principalDescriptor1 = createStrictMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor1.getValue()).andReturn("principal1@EXAMPLE.COM");
    expect(principalDescriptor1.getType()).andReturn(KerberosPrincipalType.USER).times(1);
    expect(principalDescriptor1.getLocalUsername()).andReturn("principal1");

    KerberosKeytabDescriptor keytabDescriptor1 = createStrictMock(KerberosKeytabDescriptor.class);
    expect(keytabDescriptor1.getFile()).andReturn("/etc/security/keytabs/principal1.headless.keytab").times(1);
    expect(keytabDescriptor1.getOwnerAccess()).andReturn("rw").once();
    expect(keytabDescriptor1.getGroupAccess()).andReturn("r").once();
    expect(keytabDescriptor1.getFile()).andReturn("/etc/security/keytabs/principal1.headless.keytab").times(1);
    expect(keytabDescriptor1.getOwnerName()).andReturn("principal1").once();
    expect(keytabDescriptor1.getGroupName()).andReturn("principal1").once();

    KerberosIdentityDescriptor identity1 = createStrictMock(KerberosIdentityDescriptor.class);
    expect(identity1.getPrincipalDescriptor()).andReturn(principalDescriptor1).times(1);
    expect(identity1.getKeytabDescriptor()).andReturn(keytabDescriptor1).times(1);
    expect(identity1.getName()).andReturn("identity1").times(1);

    KerberosPrincipalDescriptor principalDescriptor2 = createStrictMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor2.getValue()).andReturn("principal2/Host100@EXAMPLE.COM");
    expect(principalDescriptor2.getType()).andReturn(KerberosPrincipalType.SERVICE).times(1);
    expect(principalDescriptor2.getLocalUsername()).andReturn("principal2");

    KerberosIdentityDescriptor identity2 = createStrictMock(KerberosIdentityDescriptor.class);
    expect(identity2.getPrincipalDescriptor()).andReturn(principalDescriptor2).times(1);
    expect(identity2.getKeytabDescriptor()).andReturn(null).times(1);
    expect(identity2.getName()).andReturn("identity2").times(1);

    KerberosIdentityDescriptor identity3 = createStrictMock(KerberosIdentityDescriptor.class);
    expect(identity3.getPrincipalDescriptor()).andReturn(null).times(1);

    KerberosIdentityDescriptor identity4 = createStrictMock(KerberosIdentityDescriptor.class);
    expect(identity4.getPrincipalDescriptor()).andReturn(null).times(1);

    KerberosPrincipalDescriptor principalDescriptor5 = createStrictMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor5.getValue()).andReturn("principal5@EXAMPLE.COM");
    expect(principalDescriptor5.getType()).andReturn(KerberosPrincipalType.USER).times(1);
    expect(principalDescriptor5.getLocalUsername()).andReturn("principal5");

    KerberosKeytabDescriptor keytabDescriptor5 = createStrictMock(KerberosKeytabDescriptor.class);
    expect(keytabDescriptor5.getOwnerAccess()).andReturn("r").times(1);
    expect(keytabDescriptor5.getGroupAccess()).andReturn("r").times(1);
    expect(keytabDescriptor5.getFile()).andReturn("/etc/security/keytabs/principal5.headless.keytab").times(1);
    expect(keytabDescriptor5.getOwnerName()).andReturn("principal5").times(1);
    expect(keytabDescriptor5.getGroupName()).andReturn("hadoop").times(1);

    KerberosIdentityDescriptor identity5 = createStrictMock(KerberosIdentityDescriptor.class);
    expect(identity5.getPrincipalDescriptor()).andReturn(principalDescriptor5).times(1);
    expect(identity5.getKeytabDescriptor()).andReturn(keytabDescriptor5).times(1);
    expect(identity5.getName()).andReturn("identity5").times(1);

    KerberosPrincipalDAO kerberosPrincipalDAO = createStrictMock(KerberosPrincipalDAO.class);
    expect(kerberosPrincipalDAO.exists("principal1@EXAMPLE.COM")).andReturn(true).times(1);
    expect(kerberosPrincipalDAO.exists("principal2/Host100@EXAMPLE.COM")).andReturn(true).times(1);
    expect(kerberosPrincipalDAO.exists("principal5@EXAMPLE.COM")).andReturn(false).times(1);

    KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO = createStrictMock(KerberosKeytabPrincipalDAO.class);
    KerberosKeytabPrincipalEntity distributedEntity = new KerberosKeytabPrincipalEntity();
    distributedEntity.setDistributed(true);
    expect(kerberosKeytabPrincipalDAO.findByNaturalKey(100L,"/etc/security/keytabs/principal1.headless.keytab", "principal1@EXAMPLE.COM"))
      .andReturn(distributedEntity)
      .times(1);

    HostEntity host100 = createStrictMock(HostEntity.class);
    expect(host100.getHostId()).andReturn(100L).times(1);

    HostDAO hostDAO = createStrictMock(HostDAO.class);
    expect(hostDAO.findByName("Host100")).andReturn(host100).times(1);

    Collection<KerberosIdentityDescriptor> identities = new ArrayList<>();
    identities.add(identity1);
    identities.add(identity2);
    identities.add(identity3);
    identities.add(identity4);
    identities.add(identity5);

    Map<String, Collection<KerberosIdentityDescriptor>> activeIdentities = new HashMap<>();
    activeIdentities.put("Host100", identities);

    KerberosHelper kerberosHelper = createStrictMock(KerberosHelper.class);
    expect(kerberosHelper.getActiveIdentities("Cluster100", "Host100", null, null, true))
        .andReturn(activeIdentities)
        .times(1);

    // replay
    replayAll();

    ResourceProvider provider = new HostKerberosIdentityResourceProvider(managementController);

    // Set injected values...
    Field field;
    field = HostKerberosIdentityResourceProvider.class.getDeclaredField("kerberosHelper");
    field.setAccessible(true);
    field.set(provider, kerberosHelper);

    field = HostKerberosIdentityResourceProvider.class.getDeclaredField("kerberosPrincipalDAO");
    field.setAccessible(true);
    field.set(provider, kerberosPrincipalDAO);

    field = HostKerberosIdentityResourceProvider.class.getDeclaredField("kerberosKeytabPrincipalDAO");
    field.setAccessible(true);
    field.set(provider, kerberosKeytabPrincipalDAO);

    field = HostKerberosIdentityResourceProvider.class.getDeclaredField("hostDAO");
    field.setAccessible(true);
    field.set(provider, hostDAO);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_DESCRIPTION_PROPERTY_ID);
    propertyIds.add(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_NAME_PROPERTY_ID);
    propertyIds.add(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_TYPE_PROPERTY_ID);
    propertyIds.add(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_LOCAL_USERNAME_PROPERTY_ID);
    propertyIds.add(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_PATH_PROPERTY_ID);
    propertyIds.add(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_PROPERTY_ID);
    propertyIds.add(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_ACCESS_PROPERTY_ID);
    propertyIds.add(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_PROPERTY_ID);
    propertyIds.add(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_ACCESS_PROPERTY_ID);
    propertyIds.add(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_MODE_PROPERTY_ID);
    propertyIds.add(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_INSTALLED_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder()
        .property(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_CLUSTER_NAME_PROPERTY_ID)
        .equals("Cluster100")
        .and()
        .property(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_HOST_NAME_PROPERTY_ID)
        .equals("Host100").toPredicate();

    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());

    for (Resource resource : resources) {
      Assert.assertEquals("Cluster100",
          resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("Host100",
          resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_HOST_NAME_PROPERTY_ID));

      String principal = (String) resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_NAME_PROPERTY_ID);

      if ("principal1@EXAMPLE.COM".equals(principal)) {
        Assert.assertEquals("identity1",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_DESCRIPTION_PROPERTY_ID));

        Assert.assertEquals(KerberosPrincipalType.USER,
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_TYPE_PROPERTY_ID));

        Assert.assertEquals("principal1",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_LOCAL_USERNAME_PROPERTY_ID));

        Assert.assertEquals("/etc/security/keytabs/principal1.headless.keytab",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_PATH_PROPERTY_ID));

        Assert.assertEquals("principal1",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_PROPERTY_ID));

        Assert.assertEquals("rw",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_ACCESS_PROPERTY_ID));

        Assert.assertEquals("principal1",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_PROPERTY_ID));

        Assert.assertEquals("r",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_ACCESS_PROPERTY_ID));

        Assert.assertEquals("640",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_MODE_PROPERTY_ID));

        Assert.assertEquals("true",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_INSTALLED_PROPERTY_ID));
      } else if ("principal2/Host100@EXAMPLE.COM".equals(principal)) {
        Assert.assertEquals("identity2",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_DESCRIPTION_PROPERTY_ID));

        Assert.assertEquals(KerberosPrincipalType.SERVICE,
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_TYPE_PROPERTY_ID));

        Assert.assertEquals("principal2",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_LOCAL_USERNAME_PROPERTY_ID));

        Assert.assertNull(resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_PATH_PROPERTY_ID));
        Assert.assertNull(resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_PROPERTY_ID));
        Assert.assertNull(resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_ACCESS_PROPERTY_ID));
        Assert.assertNull(resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_PROPERTY_ID));
        Assert.assertNull(resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_ACCESS_PROPERTY_ID));
        Assert.assertNull(resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_MODE_PROPERTY_ID));

        Assert.assertEquals("false",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_INSTALLED_PROPERTY_ID));
      } else if ("principal5@EXAMPLE.COM".equals(principal)) {
        Assert.assertEquals("identity5",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_DESCRIPTION_PROPERTY_ID));

        Assert.assertEquals(KerberosPrincipalType.USER,
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_TYPE_PROPERTY_ID));

        Assert.assertEquals("principal5",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_PRINCIPAL_LOCAL_USERNAME_PROPERTY_ID));

        Assert.assertEquals("/etc/security/keytabs/principal5.headless.keytab",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_PATH_PROPERTY_ID));

        Assert.assertEquals("principal5",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_PROPERTY_ID));

        Assert.assertEquals("r",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_ACCESS_PROPERTY_ID));

        Assert.assertEquals("hadoop",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_PROPERTY_ID));

        Assert.assertEquals("r",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_ACCESS_PROPERTY_ID));

        Assert.assertEquals("440",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_MODE_PROPERTY_ID));

        Assert.assertEquals("unknown",
            resource.getPropertyValue(HostKerberosIdentityResourceProvider.KERBEROS_IDENTITY_KEYTAB_FILE_INSTALLED_PROPERTY_ID));
      } else {
        Assert.fail("Unexpected principal: " + principal);
      }
    }

    // verify
    verifyAll();
  }
}
