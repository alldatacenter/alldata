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
package org.apache.ambari.server.state.kerberos;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.ambari.server.AmbariException;
import org.apache.commons.collections.map.HashedMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.gson.Gson;

import junit.framework.Assert;

@Category({category.KerberosTest.class})
public class KerberosDescriptorTest {
  private static final KerberosDescriptorFactory KERBEROS_DESCRIPTOR_FACTORY = new KerberosDescriptorFactory();
  private static final KerberosServiceDescriptorFactory KERBEROS_SERVICE_DESCRIPTOR_FACTORY = new KerberosServiceDescriptorFactory();

  private static final String JSON_VALUE =
      "{" +
          "  \"properties\": {" +
          "      \"realm\": \"${cluster-env/kerberos_domain}\"," +
          "      \"keytab_dir\": \"/etc/security/keytabs\"" +
          "    }," +
          "  \"auth_to_local_properties\": [" +
          "      generic.name.rules" +
          "    ]," +
          "  \"services\": [" +
          KerberosServiceDescriptorTest.JSON_VALUE +
          "    ]" +
          "}";

  private static final String JSON_VALUE_IDENTITY_REFERENCES =
      "{" +
          "  \"identities\": [" +
          "    {" +
          "      \"keytab\": {" +
          "        \"file\": \"${keytab_dir}/spnego.service.keytab\"" +
          "      }," +
          "      \"name\": \"spnego\"," +
          "      \"principal\": {" +
          "        \"type\": \"service\"," +
          "        \"value\": \"HTTP/_HOST@${realm}\"" +
          "      }" +
          "    }" +
          "  ]," +
          "  \"services\": [" +
          "    {" +
          "      \"identities\": [" +
          "        {" +
          "          \"name\": \"service1_spnego\"," +
          "          \"reference\": \"/spnego\"" +
          "        }," +
          "        {" +
          "          \"name\": \"service1_identity\"" +
          "        }" +
          "      ]," +
          "      \"name\": \"SERVICE1\"" +
          "    }," +
          "    {" +
          "      \"identities\": [" +
          "        {" +
          "          \"name\": \"/spnego\"" +
          "        }," +
          "        {" +
          "          \"name\": \"service2_identity\"" +
          "        }" +
          "      ]," +
          "      \"components\": [" +
          "        {" +
          "          \"identities\": [" +
          "            {" +
          "              \"name\": \"component1_identity\"" +
          "            }," +
          "            {" +
          "              \"name\": \"service2_component1_service1_identity\"," +
          "              \"reference\": \"/SERVICE1/service1_identity\"" +
          "            }," +
          "            {" +
          "              \"name\": \"service2_component1_component1_identity\"," +
          "              \"reference\": \"./component1_identity\"" +
          "            }," +
          "            {" +
          "              \"name\": \"service2_component1_service2_identity\"," +
          "              \"reference\": \"../service2_identity\"" +
          "            }" +
          "          ]," +
          "          \"name\": \"COMPONENT21\"" +
          "        }," +
          "        {" +
          "          \"identities\": [" +
          "            {" +
          "              \"name\": \"component2_identity\"" +
          "            }" +
          "          ]," +
          "          \"name\": \"COMPONENT22\"" +
          "        }" +
          "      ]," +
          "      \"name\": \"SERVICE2\"" +
          "    }" +
          "  ]" +
          "}";

  private static final Map<String, Object> MAP_VALUE;

  static {
    Map<String, Object> keytabOwnerMap = new TreeMap<>();
    keytabOwnerMap.put(KerberosKeytabDescriptor.KEY_ACL_NAME, "root");
    keytabOwnerMap.put(KerberosKeytabDescriptor.KEY_ACL_ACCESS, "rw");

    Map<String, Object> keytabGroupMap = new TreeMap<>();
    keytabGroupMap.put(KerberosKeytabDescriptor.KEY_ACL_NAME, "hadoop");
    keytabGroupMap.put(KerberosKeytabDescriptor.KEY_ACL_ACCESS, "r");

    Map<String, Object> keytabMap = new TreeMap<>();
    keytabMap.put(KerberosKeytabDescriptor.KEY_FILE, "/etc/security/keytabs/subject.service.keytab");
    keytabMap.put(KerberosKeytabDescriptor.KEY_OWNER, keytabOwnerMap);
    keytabMap.put(KerberosKeytabDescriptor.KEY_GROUP, keytabGroupMap);
    keytabMap.put(KerberosKeytabDescriptor.KEY_CONFIGURATION, "service-site/service2.component.keytab.file");

    Map<String, Object> sharedIdentityMap = new TreeMap<>();
    sharedIdentityMap.put(KerberosIdentityDescriptor.KEY_NAME, "shared");
    sharedIdentityMap.put(KerberosIdentityDescriptor.KEY_PRINCIPAL, KerberosPrincipalDescriptorTest.MAP_VALUE);
    sharedIdentityMap.put(KerberosIdentityDescriptor.KEY_KEYTAB, keytabMap);

    Map<String, Object> servicesMap = new TreeMap<>();
    servicesMap.put((String) KerberosServiceDescriptorTest.MAP_VALUE.get(KerberosServiceDescriptor.KEY_NAME), KerberosServiceDescriptorTest.MAP_VALUE);

    Map<String, Object> identitiesMap = new TreeMap<>();
    identitiesMap.put("shared", sharedIdentityMap);

    Map<String, Object> clusterConfigProperties = new TreeMap<>();
    clusterConfigProperties.put("property1", "red");

    Map<String, Map<String, Object>> clusterConfigMap = new TreeMap<>();
    clusterConfigMap.put("cluster-conf", clusterConfigProperties);

    TreeMap<String, Map<String, Map<String, Object>>> configurationsMap = new TreeMap<>();
    configurationsMap.put("cluster-conf", clusterConfigMap);

    Collection<String> authToLocalRules = new ArrayList<>();
    authToLocalRules.add("global.name.rules");

    TreeMap<String, Object> properties = new TreeMap<>();
    properties.put("realm", "EXAMPLE.COM");
    properties.put("some.property", "Hello World");

    MAP_VALUE = new TreeMap<>();
    MAP_VALUE.put(KerberosDescriptor.KEY_PROPERTIES, properties);
    MAP_VALUE.put(KerberosDescriptor.KEY_AUTH_TO_LOCAL_PROPERTIES, authToLocalRules);
    MAP_VALUE.put(KerberosDescriptor.KEY_SERVICES, servicesMap.values());
    MAP_VALUE.put(KerberosDescriptor.KEY_CONFIGURATIONS, configurationsMap.values());
    MAP_VALUE.put(KerberosDescriptor.KEY_IDENTITIES, identitiesMap.values());
  }

  private static void validateFromJSON(KerberosDescriptor descriptor) {
    Assert.assertNotNull(descriptor);
    Assert.assertTrue(descriptor.isContainer());

    Map<String, String> properties = descriptor.getProperties();
    Assert.assertNotNull(properties);
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("${cluster-env/kerberos_domain}", properties.get("realm"));
    Assert.assertEquals("/etc/security/keytabs", properties.get("keytab_dir"));

    Set<String> authToLocalProperties = descriptor.getAuthToLocalProperties();
    Assert.assertNotNull(authToLocalProperties);
    Assert.assertEquals(1, authToLocalProperties.size());
    Assert.assertTrue(authToLocalProperties.contains("generic.name.rules"));

    authToLocalProperties = descriptor.getAllAuthToLocalProperties();
    Assert.assertNotNull(authToLocalProperties);
    Assert.assertEquals(3, authToLocalProperties.size());
    Assert.assertTrue(authToLocalProperties.contains("component.name.rules1"));
    Assert.assertTrue(authToLocalProperties.contains("generic.name.rules"));
    Assert.assertTrue(authToLocalProperties.contains("service.name.rules1"));

    Map<String, KerberosServiceDescriptor> serviceDescriptors = descriptor.getServices();
    Assert.assertNotNull(serviceDescriptors);
    Assert.assertEquals(1, serviceDescriptors.size());

    for (KerberosServiceDescriptor serviceDescriptor : serviceDescriptors.values()) {
      KerberosServiceDescriptorTest.validateFromJSON(serviceDescriptor);
    }

    Map<String, KerberosConfigurationDescriptor> configurations = descriptor.getConfigurations();

    Assert.assertNull(configurations);
  }

  private static void validateFromMap(KerberosDescriptor descriptor) throws AmbariException {
    Assert.assertNotNull(descriptor);
    Assert.assertTrue(descriptor.isContainer());

    Map<String, String> properties = descriptor.getProperties();
    Assert.assertNotNull(properties);
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("EXAMPLE.COM", properties.get("realm"));
    Assert.assertEquals("Hello World", properties.get("some.property"));

    Set<String> authToLocalProperties = descriptor.getAuthToLocalProperties();
    Assert.assertNotNull(authToLocalProperties);
    Assert.assertEquals(1, authToLocalProperties.size());
    Assert.assertEquals("global.name.rules", authToLocalProperties.iterator().next());

    Map<String, KerberosServiceDescriptor> services = descriptor.getServices();
    Assert.assertNotNull(services);
    Assert.assertEquals(1, services.size());

    for (KerberosServiceDescriptor service : services.values()) {
      KerberosComponentDescriptor component = service.getComponent("A_DIFFERENT_COMPONENT_NAME");
      Assert.assertNotNull(component);

      List<KerberosIdentityDescriptor> resolvedIdentities = component.getIdentities(true, null);
      KerberosIdentityDescriptor resolvedIdentity = null;
      Assert.assertNotNull(resolvedIdentities);
      Assert.assertEquals(3, resolvedIdentities.size());

      for (KerberosIdentityDescriptor item : resolvedIdentities) {
        if ("/shared".equals(item.getReference())) {
          resolvedIdentity = item;
          break;
        }
      }
      Assert.assertNotNull(resolvedIdentity);

      List<KerberosIdentityDescriptor> identities = component.getIdentities(false, null);
      Assert.assertNotNull(identities);
      Assert.assertEquals(3, identities.size());

      KerberosIdentityDescriptor identityReference = component.getIdentity("shared_identity");
      Assert.assertNotNull(identityReference);

      KerberosIdentityDescriptor referencedIdentity = descriptor.getIdentity("shared");
      Assert.assertNotNull(referencedIdentity);

      Assert.assertEquals(identityReference.getKeytabDescriptor(), resolvedIdentity.getKeytabDescriptor());
      Assert.assertEquals(referencedIdentity.getPrincipalDescriptor(), resolvedIdentity.getPrincipalDescriptor());

      Map<String, KerberosConfigurationDescriptor> configurations = service.getConfigurations(true);
      Assert.assertNotNull(configurations);
      Assert.assertEquals(2, configurations.size());
      Assert.assertNotNull(configurations.get("service-site"));
      Assert.assertNotNull(configurations.get("cluster-conf"));
    }

    Map<String, KerberosConfigurationDescriptor> configurations = descriptor.getConfigurations();

    Assert.assertNotNull(configurations);
    Assert.assertEquals(1, configurations.size());

    KerberosConfigurationDescriptor configuration = configurations.get("cluster-conf");

    Assert.assertNotNull(configuration);

    Map<String, String> configProperties = configuration.getProperties();

    Assert.assertEquals("cluster-conf", configuration.getType());
    Assert.assertNotNull(configProperties);
    Assert.assertEquals(1, configProperties.size());
    Assert.assertEquals("red", configProperties.get("property1"));
  }

  private void validateUpdatedData(KerberosDescriptor descriptor) {
    Assert.assertNotNull(descriptor);

    Map<String, String> properties = descriptor.getProperties();
    Assert.assertNotNull(properties);
    Assert.assertEquals(3, properties.size());
    Assert.assertEquals("EXAMPLE.COM", properties.get("realm"));
    Assert.assertEquals("/etc/security/keytabs", properties.get("keytab_dir"));
    Assert.assertEquals("Hello World", properties.get("some.property"));

    Set<String> authToLocalProperties = descriptor.getAuthToLocalProperties();
    Assert.assertNotNull(authToLocalProperties);
    Assert.assertEquals(2, authToLocalProperties.size());
    // guarantee ordering...
    Iterator<String> iterator = new TreeSet<>(authToLocalProperties).iterator();
    Assert.assertEquals("generic.name.rules", iterator.next());
    Assert.assertEquals("global.name.rules", iterator.next());

    Map<String, KerberosServiceDescriptor> serviceDescriptors = descriptor.getServices();
    Assert.assertNotNull(serviceDescriptors);
    Assert.assertEquals(2, serviceDescriptors.size());

    KerberosServiceDescriptorTest.validateFromJSON(descriptor.getService("SERVICE_NAME"));
    KerberosServiceDescriptorTest.validateFromMap(descriptor.getService("A_DIFFERENT_SERVICE_NAME"));

    Assert.assertNull(descriptor.getService("invalid service"));

    Map<String, KerberosConfigurationDescriptor> configurations = descriptor.getConfigurations();

    Assert.assertNotNull(configurations);
    Assert.assertEquals(1, configurations.size());

    KerberosConfigurationDescriptor configuration = configurations.get("cluster-conf");

    Assert.assertNotNull(configuration);

    Map<String, String> configProperties = configuration.getProperties();

    Assert.assertEquals("cluster-conf", configuration.getType());
    Assert.assertNotNull(configProperties);
    Assert.assertEquals(1, configProperties.size());
    Assert.assertEquals("red", configProperties.get("property1"));
  }

  private KerberosDescriptor createFromJSON() throws AmbariException {
    return KERBEROS_DESCRIPTOR_FACTORY.createInstance(JSON_VALUE);
  }

  private KerberosDescriptor createFromMap() throws AmbariException {
    return new KerberosDescriptor(MAP_VALUE);
  }

  @Test
  public void testFromMapViaGSON() throws AmbariException {
    Object data = new Gson().fromJson(JSON_VALUE, Object.class);

    Assert.assertNotNull(data);

    KerberosDescriptor descriptor = new KerberosDescriptor((Map<?, ?>) data);

    validateFromJSON(descriptor);
  }

  @Test
  public void testJSONDeserialize() throws AmbariException {
    validateFromJSON(createFromJSON());
  }

  @Test
  public void testMapDeserialize() throws AmbariException {
    validateFromMap(createFromMap());
  }

  @Test
  public void testInvalid() {
    // Invalid JSON syntax
    try {
      KERBEROS_SERVICE_DESCRIPTOR_FACTORY.createInstances(JSON_VALUE + "erroneous text");
      Assert.fail("Should have thrown AmbariException.");
    } catch (AmbariException e) {
      // This is expected
    } catch (Throwable t) {
      Assert.fail("Should have thrown AmbariException.");
    }
  }

  @Test
  public void testEquals() throws AmbariException {
    Assert.assertTrue(createFromJSON().equals(createFromJSON()));
    Assert.assertFalse(createFromJSON().equals(createFromMap()));
  }

  @Test
  public void testToMap() throws AmbariException {
    Gson gson = new Gson();
    KerberosDescriptor descriptor = createFromMap();
    Assert.assertNotNull(descriptor);
    Assert.assertEquals(gson.toJson(MAP_VALUE), gson.toJson(descriptor.toMap()));
  }

  @Test
  public void testUpdate() throws AmbariException {
    KerberosDescriptor descriptor = createFromJSON();
    KerberosDescriptor updatedDescriptor = createFromMap();

    Assert.assertNotNull(descriptor);
    Assert.assertNotNull(updatedDescriptor);

    descriptor.update(updatedDescriptor);

    validateUpdatedData(descriptor);
  }

  @Test
  public void testGetReferencedIdentityDescriptor() throws IOException {
    URL systemResourceURL = ClassLoader.getSystemResource("kerberos/test_get_referenced_identity_descriptor.json");
    Assert.assertNotNull(systemResourceURL);
    KerberosDescriptor descriptor = KERBEROS_DESCRIPTOR_FACTORY.createInstance(new File(systemResourceURL.getFile()));

    KerberosIdentityDescriptor identity;

    // Stack-level identity
    identity = descriptor.getReferencedIdentityDescriptor("/stack_identity");
    Assert.assertNotNull(identity);
    Assert.assertEquals("stack_identity", identity.getName());

    // Service-level identity
    identity = descriptor.getReferencedIdentityDescriptor("/SERVICE1/service1_identity");
    Assert.assertNotNull(identity);
    Assert.assertEquals("service1_identity", identity.getName());
    Assert.assertNotNull(identity.getParent());
    Assert.assertEquals("SERVICE1", identity.getParent().getName());

    // Component-level identity
    identity = descriptor.getReferencedIdentityDescriptor("/SERVICE2/SERVICE2_COMPONENT1/service2_component1_identity");
    Assert.assertNotNull(identity);
    Assert.assertEquals("service2_component1_identity", identity.getName());
    Assert.assertNotNull(identity.getParent());
    Assert.assertEquals("SERVICE2_COMPONENT1", identity.getParent().getName());
    Assert.assertNotNull(identity.getParent().getParent());
    Assert.assertEquals("SERVICE2", identity.getParent().getParent().getName());
  }

  @Test
  public void testGetReferencedIdentityDescriptor_NameCollisions() throws IOException {
    URL systemResourceURL = ClassLoader.getSystemResource("kerberos/test_get_referenced_identity_descriptor.json");
    Assert.assertNotNull(systemResourceURL);
    KerberosDescriptor descriptor = KERBEROS_DESCRIPTOR_FACTORY.createInstance(new File(systemResourceURL.getFile()));

    KerberosIdentityDescriptor identity;

    // Stack-level identity
    identity = descriptor.getReferencedIdentityDescriptor("/collision");
    Assert.assertNotNull(identity);
    Assert.assertEquals("collision", identity.getName());
    Assert.assertNotNull(identity.getParent());
    Assert.assertEquals(null, identity.getParent().getName());

    // Service-level identity
    identity = descriptor.getReferencedIdentityDescriptor("/SERVICE1/collision");
    Assert.assertNotNull(identity);
    Assert.assertEquals("collision", identity.getName());
    Assert.assertNotNull(identity.getParent());
    Assert.assertEquals("SERVICE1", identity.getParent().getName());

    // Component-level identity
    identity = descriptor.getReferencedIdentityDescriptor("/SERVICE2/SERVICE2_COMPONENT1/collision");
    Assert.assertNotNull(identity);
    Assert.assertEquals("collision", identity.getName());
    Assert.assertNotNull(identity.getParent());
    Assert.assertEquals("SERVICE2_COMPONENT1", identity.getParent().getName());
    Assert.assertNotNull(identity.getParent().getParent());
    Assert.assertEquals("SERVICE2", identity.getParent().getParent().getName());
  }

  @Test
  public void testGetReferencedIdentityDescriptor_RelativePath() throws IOException {
    URL systemResourceURL = ClassLoader.getSystemResource("kerberos/test_get_referenced_identity_descriptor.json");
    Assert.assertNotNull(systemResourceURL);

    KerberosDescriptor descriptor = KERBEROS_DESCRIPTOR_FACTORY.createInstance(new File(systemResourceURL.getFile()));
    Assert.assertNotNull(descriptor);

    KerberosServiceDescriptor serviceDescriptor = descriptor.getService("SERVICE2");
    Assert.assertNotNull(serviceDescriptor);

    KerberosComponentDescriptor componentDescriptor = serviceDescriptor.getComponent("SERVICE2_COMPONENT1");
    Assert.assertNotNull(componentDescriptor);

    KerberosIdentityDescriptor identity;
    identity = componentDescriptor.getReferencedIdentityDescriptor("../service2_identity");
    Assert.assertNotNull(identity);
    Assert.assertEquals("service2_identity", identity.getName());
    Assert.assertEquals(serviceDescriptor, identity.getParent());

    identity = serviceDescriptor.getReferencedIdentityDescriptor("../service2_identity");
    Assert.assertNull(identity);
  }

  @Test
  public void testGetReferencedIdentityDescriptor_Recursive() throws IOException {
    boolean identityFound;
    List<KerberosIdentityDescriptor> identities;

    URL systemResourceURL = ClassLoader.getSystemResource("kerberos/test_get_referenced_identity_descriptor.json");
    Assert.assertNotNull(systemResourceURL);

    KerberosDescriptor descriptor = KERBEROS_DESCRIPTOR_FACTORY.createInstance(new File(systemResourceURL.getFile()));
    Assert.assertNotNull(descriptor);

    KerberosServiceDescriptor serviceDescriptor = descriptor.getService("SERVICE2");
    Assert.assertNotNull(serviceDescriptor);

    identities = serviceDescriptor.getIdentities(true, null);
    Assert.assertNotNull(identities);

    identityFound = false;
    for (KerberosIdentityDescriptor identity : identities) {
      if ("service2_stack_reference".equals(identity.getName())) {

        // From base identity
        Assert.assertEquals("stack@${realm}", identity.getPrincipalDescriptor().getValue());

        // Overwritten by the "local" identity
        Assert.assertEquals("${keytab_dir}/service2_stack.keytab", identity.getKeytabDescriptor().getFile());
        Assert.assertEquals("/stack_identity", identity.getReference());
        Assert.assertEquals("service2/property1_principal", identity.getPrincipalDescriptor().getConfiguration());

        identityFound = true;
      }
    }
    Assert.assertTrue(identityFound);

    KerberosComponentDescriptor componentDescriptor = serviceDescriptor.getComponent("SERVICE2_COMPONENT1");
    Assert.assertNotNull(componentDescriptor);

    identities = componentDescriptor.getIdentities(true, null);
    Assert.assertNotNull(identities);

    identityFound = false;
    for (KerberosIdentityDescriptor identity : identities) {
      if ("component1_service2_stack_reference".equals(identity.getName())) {

        // From base identity
        Assert.assertEquals("stack@${realm}", identity.getPrincipalDescriptor().getValue());

        // Overwritten by the "referenced" identity
        Assert.assertEquals("${keytab_dir}/service2_stack.keytab", identity.getKeytabDescriptor().getFile());

        // Overwritten by the "local" identity
        Assert.assertEquals("/SERVICE2/service2_stack_reference", identity.getReference());
        Assert.assertEquals("component1_service2/property1_principal", identity.getPrincipalDescriptor().getConfiguration());

        identityFound = true;
      }
    }
    Assert.assertTrue(identityFound);
  }

  @Test
  public void testFiltersOutIdentitiesBasedonInstalledServices() throws IOException {
    URL systemResourceURL = ClassLoader.getSystemResource("kerberos/test_filtering_identity_descriptor.json");
    KerberosComponentDescriptor componentDescriptor = KERBEROS_DESCRIPTOR_FACTORY.createInstance(new File(systemResourceURL.getFile()))
        .getService("SERVICE1")
        .getComponent("SERVICE1_COMPONENT1");
    List<KerberosIdentityDescriptor> identities = componentDescriptor.getIdentities(true, new HashedMap() {{
      put("services", Collections.emptySet());
    }});
    Assert.assertEquals(0, identities.size());
    identities = componentDescriptor.getIdentities(true, new HashedMap() {{
      put("services", Arrays.asList("REF_SERVICE1"));
    }});
    Assert.assertEquals(1, identities.size());
  }

  @Test
  public void testCollectPrincipalNames() throws Exception {
    URL systemResourceURL = ClassLoader.getSystemResource("kerberos/test_get_referenced_identity_descriptor.json");
    KerberosDescriptor descriptor = KERBEROS_DESCRIPTOR_FACTORY.createInstance(new File(systemResourceURL.getFile()));
    Map<String, String> principalsPerComponent = descriptor.principals();
    Assert.assertEquals("service2_component1@${realm}", principalsPerComponent.get("SERVICE2/SERVICE2_COMPONENT1/service2_component1_identity"));
    Assert.assertEquals("service1@${realm}", principalsPerComponent.get("SERVICE1/service1_identity"));
  }

  @Test
  public void testIdentityReferences() throws Exception {
    KerberosDescriptor kerberosDescriptor = KERBEROS_DESCRIPTOR_FACTORY.createInstance(JSON_VALUE_IDENTITY_REFERENCES);
    KerberosServiceDescriptor serviceDescriptor;
    List<KerberosIdentityDescriptor> identities;

    // Reference is determined using the "reference" attribute
    serviceDescriptor = kerberosDescriptor.getService("SERVICE1");
    identities = serviceDescriptor.getIdentities(true, null);
    Assert.assertEquals(2, identities.size());
    for (KerberosIdentityDescriptor identity : identities) {
      if (identity.isReference()) {
        Assert.assertEquals("service1_spnego", identity.getName());
        Assert.assertEquals("/spnego", identity.getReference());
      } else {
        Assert.assertEquals("service1_identity", identity.getName());
        Assert.assertNull(identity.getReference());
      }
    }

    Assert.assertEquals("service1_identity", identities.get(1).getName());
    Assert.assertNull(identities.get(1).getReference());

    // Reference is determined using the "name" attribute
    serviceDescriptor = kerberosDescriptor.getService("SERVICE2");
    identities = serviceDescriptor.getIdentities(true, null);
    Assert.assertEquals(2, identities.size());
    for (KerberosIdentityDescriptor identity : identities) {
      if (identity.isReference()) {
        Assert.assertEquals("/spnego", identity.getName());
        Assert.assertNull(identity.getReference());
      } else {
        Assert.assertEquals("service2_identity", identity.getName());
        Assert.assertNull(identity.getReference());
      }
    }
  }

  @Test
  public void testGetPath() throws Exception {
    KerberosDescriptor kerberosDescriptor;
    KerberosServiceDescriptor serviceDescriptor;
    List<KerberosIdentityDescriptor> identities;

    kerberosDescriptor = KERBEROS_DESCRIPTOR_FACTORY.createInstance(JSON_VALUE);

    serviceDescriptor = kerberosDescriptor.getService("SERVICE_NAME");
    identities = serviceDescriptor.getIdentities(false, null);
    Assert.assertEquals(1, identities.size());
    Assert.assertEquals("/SERVICE_NAME/identity_1", identities.get(0).getPath());

    KerberosComponentDescriptor componentDescriptor = serviceDescriptor.getComponent("COMPONENT_NAME");
    identities = componentDescriptor.getIdentities(false, null);
    Assert.assertEquals(1, identities.size());
    Assert.assertEquals("/SERVICE_NAME/COMPONENT_NAME/identity_1", identities.get(0).getPath());


    kerberosDescriptor = KERBEROS_DESCRIPTOR_FACTORY.createInstance(JSON_VALUE_IDENTITY_REFERENCES);

    serviceDescriptor = kerberosDescriptor.getService("SERVICE1");
    identities = serviceDescriptor.getIdentities(true, null);
    Assert.assertEquals(2, identities.size());
    Assert.assertEquals("/SERVICE1/service1_spnego", identities.get(0).getPath());
    Assert.assertEquals("/SERVICE1/service1_identity", identities.get(1).getPath());
  }

  @Test
  public void testGetReferences() throws Exception {
    KerberosDescriptor kerberosDescriptor = KERBEROS_DESCRIPTOR_FACTORY.createInstance(JSON_VALUE_IDENTITY_REFERENCES);
    KerberosIdentityDescriptor identity;
    List<KerberosIdentityDescriptor> references;
    Set<String> paths;

    // Find all references to /spnego
    identity = kerberosDescriptor.getIdentity("spnego");
    references = identity.findReferences();

    Assert.assertNotNull(references);
    Assert.assertEquals(2, references.size());

    paths = collectPaths(references);
    Assert.assertTrue(paths.contains("/SERVICE1/service1_spnego"));
    Assert.assertTrue(paths.contains("/SERVICE2//spnego"));

    // Find all references to /SERVICE1/service1_identity
    identity = kerberosDescriptor.getService("SERVICE1").getIdentity("service1_identity");
    references = identity.findReferences();

    Assert.assertNotNull(references);
    Assert.assertEquals(1, references.size());

    paths = collectPaths(references);
    Assert.assertTrue(paths.contains("/SERVICE2/COMPONENT21/service2_component1_service1_identity"));

    // Find all references to /SERVICE2/COMPONENT21/component1_identity (testing ./)
    identity = kerberosDescriptor.getService("SERVICE2").getComponent("COMPONENT21").getIdentity("component1_identity");
    references = identity.findReferences();

    Assert.assertNotNull(references);
    Assert.assertEquals(1, references.size());

    paths = collectPaths(references);
    Assert.assertTrue(paths.contains("/SERVICE2/COMPONENT21/service2_component1_component1_identity"));

    // Find all references to /SERVICE2/component2_identity (testing ../)
    identity = kerberosDescriptor.getService("SERVICE2").getIdentity("service2_identity");
    references = identity.findReferences();

    Assert.assertNotNull(references);
    Assert.assertEquals(1, references.size());

    paths = collectPaths(references);
    Assert.assertTrue(paths.contains("/SERVICE2/COMPONENT21/service2_component1_service2_identity"));
  }

  private Set<String> collectPaths(List<KerberosIdentityDescriptor> identityDescriptors) {
    Set<String> paths = new HashSet<>();
    for (KerberosIdentityDescriptor identityDescriptor : identityDescriptors) {
      paths.add(identityDescriptor.getPath());
    }
    return paths;
  }

}