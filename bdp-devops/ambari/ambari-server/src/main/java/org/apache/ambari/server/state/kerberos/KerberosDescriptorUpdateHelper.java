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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KerberosDescriptorUpdateHelper provides routines for upgrading the user-specified Kerberos descriptor
 * when changing stacks.
 * <p>
 * This implementation should work for stack upgrades and downgrades.
 */
public class KerberosDescriptorUpdateHelper {
  private static final Logger LOG = LoggerFactory.getLogger(KerberosDescriptorUpdateHelper.class);

  /**
   * The entry point into upgrading a user-specified Kerberos descriptor.
   * <p>
   * The supplied Kerberos descriptors will remain untouched and new Kerberos descriptor instance will
   * created and returned with the update data.
   *
   * @param beginningStackKerberosDescriptor the Kerberos descriptor for the previous stack version
   * @param endingStackKerberosDescriptor    the Kerberos descriptor for the new stack version
   * @param userKerberosDescriptor           the user-specified Kerberos descriptor
   * @return a new Kerberos descriptor containing the updated user-specified data
   */
  public static KerberosDescriptor updateUserKerberosDescriptor(KerberosDescriptor beginningStackKerberosDescriptor,
                                                                KerberosDescriptor endingStackKerberosDescriptor,
                                                                KerberosDescriptor userKerberosDescriptor) {
    KerberosDescriptor updated = new KerberosDescriptor(userKerberosDescriptor.toMap());

    updated.setProperties(processProperties(
        beginningStackKerberosDescriptor.getProperties(),
        endingStackKerberosDescriptor.getProperties(),
        updated.getProperties()));

    updated.setConfigurations(processConfigurations(
        beginningStackKerberosDescriptor.getConfigurations(),
        endingStackKerberosDescriptor.getConfigurations(),
        updated.getConfigurations()));

    updated.setIdentities(processIdentities(
        beginningStackKerberosDescriptor.getIdentities(),
        endingStackKerberosDescriptor.getIdentities(),
        updated.getIdentities()));

    updated.setAuthToLocalProperties(processAuthToLocalProperties(
        beginningStackKerberosDescriptor.getAuthToLocalProperties(),
        endingStackKerberosDescriptor.getAuthToLocalProperties(),
        updated.getAuthToLocalProperties()));

    updated.setServices(processServices(
        beginningStackKerberosDescriptor.getServices(),
        endingStackKerberosDescriptor.getServices(),
        updated.getServices()));

    return updated;
  }

  /**
   * Processes the service-level Kerberos descriptors to add, remove, or update data in the user-specified
   * Kerberos descriptor.
   *
   * @param previousStackServices a map of {@link KerberosServiceDescriptor}s from the previous stack version's Kerberos descriptor
   * @param newStackServices      a map of {@link KerberosServiceDescriptor}s from the new stack version's Kerberos descriptor
   * @param userServices          a map of {@link KerberosServiceDescriptor}s from the user-supplied Kerberos descriptor
   * @return a map of updated {@link KerberosServiceDescriptor}s
   */
  private static Map<String, KerberosServiceDescriptor> processServices(Map<String, KerberosServiceDescriptor> previousStackServices,
                                                                        Map<String, KerberosServiceDescriptor> newStackServices,
                                                                        Map<String, KerberosServiceDescriptor> userServices) {
    if ((userServices == null) || userServices.isEmpty() || ((previousStackServices == null) && (newStackServices == null))) {
      return userServices;
    }

    Map<String, KerberosServiceDescriptor> updatedServices = new TreeMap<>();

    if (previousStackServices == null) {
      previousStackServices = Collections.emptyMap();
    }

    if (newStackServices == null) {
      newStackServices = Collections.emptyMap();
    }

    for (Map.Entry<String, KerberosServiceDescriptor> entry : userServices.entrySet()) {
      String name = entry.getKey();
      KerberosServiceDescriptor userValue = entry.getValue();

      if (userValue != null) {
        if (newStackServices.containsKey(name)) {
          KerberosServiceDescriptor oldValue = previousStackServices.get(name);
          KerberosServiceDescriptor newValue = newStackServices.get(name);

          LOG.debug("Processing service {} for modifications", name);
          updatedServices.put(name, processService(oldValue, newValue, userValue));
        } else if (previousStackServices.containsKey(name)) {
          LOG.debug("Removing service {} from user-specified Kerberos Descriptor", name);
          // Nothing to do here, just don't add it to the updated configurations map...
        } else {
          LOG.debug("Leaving service {} in user-specified Kerberos Descriptor unchanged since it was user-defined.", name);
          updatedServices.put(name, userValue);
        }
      }
    }

    // Note: there is no need to add service definitions that do not exist since they will get
    // added dynamically when merged with the stack default value.

    return updatedServices;
  }

  /**
   * Processes a {@link KerberosServiceDescriptor} to change the user-supplied data based on the changes
   * observed between the previous stack version's data and the new stack version's data.
   *
   * @param previousStackService a {@link KerberosServiceDescriptor} from the previous stack version's Kerberos descriptor
   * @param newStackService      a {@link KerberosServiceDescriptor} from the new stack version's Kerberos descriptor
   * @param userService          a {@link KerberosServiceDescriptor} from the user-specified Kerberos descriptor
   * @return the updated {@link KerberosServiceDescriptor}
   */
  private static KerberosServiceDescriptor processService(KerberosServiceDescriptor previousStackService,
                                                          KerberosServiceDescriptor newStackService,
                                                          KerberosServiceDescriptor userService) {

    KerberosServiceDescriptor updatedService = new KerberosServiceDescriptor(userService.toMap());

    updatedService.setAuthToLocalProperties(processAuthToLocalProperties(
        (previousStackService == null) ? null : previousStackService.getAuthToLocalProperties(),
        (newStackService == null) ? null : newStackService.getAuthToLocalProperties(),
        updatedService.getAuthToLocalProperties()));

    updatedService.setConfigurations(processConfigurations(
        (previousStackService == null) ? null : previousStackService.getConfigurations(),
        (newStackService == null) ? null : newStackService.getConfigurations(),
        updatedService.getConfigurations()));

    updatedService.setIdentities(processIdentities(
        (previousStackService == null) ? null : previousStackService.getIdentities(),
        (newStackService == null) ? null : newStackService.getIdentities(),
        updatedService.getIdentities()));

    Map<String, KerberosComponentDescriptor> userServiceComponents = updatedService.getComponents();
    Map<String, KerberosComponentDescriptor> newServiceComponents = (newStackService == null) ? null : newStackService.getComponents();
    Map<String, KerberosComponentDescriptor> oldServiceComponents = (previousStackService == null) ? null : previousStackService.getComponents();

    if (newServiceComponents == null) {
      newServiceComponents = Collections.emptyMap();
    }

    if (oldServiceComponents == null) {
      oldServiceComponents = Collections.emptyMap();
    }

    if (userServiceComponents != null) {
      Iterator<Map.Entry<String, KerberosComponentDescriptor>> iterator = userServiceComponents.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<String, KerberosComponentDescriptor> entry = iterator.next();
        String name = entry.getKey();
        KerberosComponentDescriptor userValue = entry.getValue();

        if (userValue == null) {
          iterator.remove();  // This is a bad entry... remove it.
        } else if (newServiceComponents.containsKey(name)) {
          KerberosComponentDescriptor oldValue = oldServiceComponents.get(name);
          KerberosComponentDescriptor newValue = newServiceComponents.get(name);

          LOG.debug("Processing component {}/{} for modifications", updatedService.getName(), name);
          processComponent(oldValue, newValue, userValue);
        } else {
          LOG.debug("Removing component {}/{} from user-specified Kerberos Descriptor", updatedService.getName(), name);
          iterator.remove();
        }
      }
    }

    return updatedService;
  }

  /**
   * Processes a {@link KerberosComponentDescriptor} to change the user-supplied data based on the changes
   * observed between the previous stack version's data and the new stack version's data.
   * <p>
   * The supplied userComponent value is updated in place.
   *
   * @param previousStackComponent a {@link KerberosComponentDescriptor} from the previous stack version's Kerberos descriptor
   * @param newStackComponent      a {@link KerberosComponentDescriptor} from the new stack version's Kerberos descriptor
   * @param userComponent          a {@link KerberosComponentDescriptor} from the user-specified Kerberos descriptor
   * @return the updated {@link KerberosComponentDescriptor}
   */
  private static KerberosComponentDescriptor processComponent(KerberosComponentDescriptor previousStackComponent,
                                                              KerberosComponentDescriptor newStackComponent,
                                                              KerberosComponentDescriptor userComponent) {
    userComponent.setAuthToLocalProperties(processAuthToLocalProperties(
        (previousStackComponent == null) ? null : previousStackComponent.getAuthToLocalProperties(),
        (newStackComponent == null) ? null : newStackComponent.getAuthToLocalProperties(),
        userComponent.getAuthToLocalProperties()));

    userComponent.setConfigurations(processConfigurations(
        (previousStackComponent == null) ? null : previousStackComponent.getConfigurations(),
        (newStackComponent == null) ? null : newStackComponent.getConfigurations(),
        userComponent.getConfigurations()));

    userComponent.setIdentities(processIdentities(
        (previousStackComponent == null) ? null : previousStackComponent.getIdentities(),
        (newStackComponent == null) ? null : newStackComponent.getIdentities(),
        userComponent.getIdentities()));

    return userComponent;
  }

  /**
   * Processes a the list of configuration specification (<code>&lt;configuration type&gt;/&lt;property name&gt;</code>)
   * identifying the properties that should be automatically updated with generated auth-to-local rules.
   * <p>
   * If no user-specified properties are set, <code>null</code> is returned.
   * <p>
   * Else the configuration specifications from the previous stack are removed from the user-specified
   * data and the configuration specifications from the new stack are added to the user-specified,
   * leaving the new list of configuration specifications  as well as any user-specified changes.
   *
   * @param previousStackAuthToLocalProperties the auth-to-local properties from the previous stack version's Kerberos descriptor
   * @param newStackAuthToLocalProperties      the auth-to-local properties from the new stack version's Kerberos descriptor
   * @param userAuthToLocalProperties          the auth-to-local properties from the user-specified Kerberos descriptor
   * @return an updated {@link Set} of configuration specifications
   */
  private static Set<String> processAuthToLocalProperties(Set<String> previousStackAuthToLocalProperties,
                                                          Set<String> newStackAuthToLocalProperties,
                                                          Set<String> userAuthToLocalProperties) {
    if (userAuthToLocalProperties == null) {
      return null;
    }

    TreeSet<String> updatedAuthToLocalProperties = new TreeSet<>(userAuthToLocalProperties);

    // Remove old configuration specifications, leaving the user-specified ones.
    if (previousStackAuthToLocalProperties != null) {
      updatedAuthToLocalProperties.removeAll(previousStackAuthToLocalProperties);
    }

    // Add the new configuration specifications
    if (newStackAuthToLocalProperties != null) {
      updatedAuthToLocalProperties.addAll(newStackAuthToLocalProperties);
    }

    return updatedAuthToLocalProperties;
  }

  /**
   * Processes the identity-level Kerberos descriptors to add, remove, or update data in the user-specified
   * Kerberos descriptor.
   *
   * @param previousStackIdentities a map of {@link KerberosIdentityDescriptor}s from the previous stack version's Kerberos descriptor
   * @param newStackIdentities      a map of {@link KerberosIdentityDescriptor}s from the new stack version's Kerberos descriptor
   * @param userIdentities          a map of {@link KerberosIdentityDescriptor}s from the user-supplied Kerberos descriptor
   * @return a list of updated {@link KerberosIdentityDescriptor}s
   */
  private static List<KerberosIdentityDescriptor> processIdentities(List<KerberosIdentityDescriptor> previousStackIdentities,
                                                                    List<KerberosIdentityDescriptor> newStackIdentities,
                                                                    List<KerberosIdentityDescriptor> userIdentities) {

    if ((userIdentities == null) || userIdentities.isEmpty() || ((previousStackIdentities == null) && (newStackIdentities == null))) {
      return userIdentities;
    }

    // Create maps to make processing easier....
    Map<String, KerberosIdentityDescriptor> previousStackIdentityMap = toMap(previousStackIdentities);
    Map<String, KerberosIdentityDescriptor> newStackIdentityMap = toMap(newStackIdentities);
    Map<String, KerberosIdentityDescriptor> userStackIdentityMap = toMap(userIdentities);

    Map<String, KerberosIdentityDescriptor> updatedIdentities = new TreeMap<>();

    if (previousStackIdentityMap == null) {
      previousStackIdentityMap = Collections.emptyMap();
    }

    if (newStackIdentityMap == null) {
      newStackIdentityMap = Collections.emptyMap();
    }

    // Find identities to modify or remove
    for (Map.Entry<String, KerberosIdentityDescriptor> entry : userStackIdentityMap.entrySet()) {
      String name = entry.getKey();
      KerberosIdentityDescriptor userValue = entry.getValue();

      if (userValue != null) {
        if (newStackIdentityMap.containsKey(name)) {
          // Modify the new stack identity value by changing on the principal value and/or keytab
          // file value since they are the only fields in this structure that should be changed
          // by a user. However, the new stack identity may have been converted to a pure reference
          // where the user changes will then be ignored.
          KerberosIdentityDescriptor newValue = newStackIdentityMap.get(name);
          KerberosIdentityDescriptor previousValue = previousStackIdentityMap.get(name);

          updatedIdentities.put(name, processIdentity(previousValue, newValue, userValue));

        } else if (previousStackIdentityMap.containsKey(name)) {
          LOG.debug("Removing identity named {} from user-specified Kerberos Descriptor", name);
          // Nothing to do here, just don't add it to the updated identity map...
        } else {
          LOG.debug("Leaving identity named {} in user-specified Kerberos Descriptor unchanged since it was user-defined.", name);
          updatedIdentities.put(name, userValue);
        }
      }
    }

    // Note: there is no need to add identity definitions that do not exist since they will get
    // added dynamically when merged with the stack default value.

    return new ArrayList<>(updatedIdentities.values());
  }


  /**
   * Processes a {@link KerberosIdentityDescriptor} to change the user-supplied data based on the changes
   * observed between the previous stack version's data and the new stack version's data.
   * <p>
   * It is expected that <code>newStackIdentities</code> and <code>userIdentities</code> are not null.
   * However, <code>previousStackIdentities</code> may be null in the event the user added a Kerberos
   * identity that was then added in the new Kerberos descriptor.  In this case, the user's values
   * for the principal name and keytab file are kept while adding any other changes from tne new stack.
   *
   * @param previousStackIdentity a {@link KerberosIdentityDescriptor} from the previous stack version's Kerberos descriptor
   * @param newStackIdentity      a {@link KerberosIdentityDescriptor} from the new stack version's Kerberos descriptor
   * @param userIdentity          a {@link KerberosIdentityDescriptor} from the user-specified Kerberos descriptor
   * @return a new, updated, {@link KerberosIdentityDescriptor}
   */
  private static KerberosIdentityDescriptor processIdentity(KerberosIdentityDescriptor previousStackIdentity,
                                                            KerberosIdentityDescriptor newStackIdentity,
                                                            KerberosIdentityDescriptor userIdentity) {

    KerberosIdentityDescriptor updatedValue = new KerberosIdentityDescriptor(newStackIdentity.toMap());
    KerberosPrincipalDescriptor updatedValuePrincipal = updatedValue.getPrincipalDescriptor();
    KerberosKeytabDescriptor updatedValueKeytab = updatedValue.getKeytabDescriptor();

    // If the new identity definition is a reference and no longer has a principal definition,
    // Ignore any user changes to the old principal definition.
    if (updatedValuePrincipal != null) {
      KerberosPrincipalDescriptor oldValuePrincipal = (previousStackIdentity == null) ? null : previousStackIdentity.getPrincipalDescriptor();
      String previousValuePrincipalValue = null;
      KerberosPrincipalDescriptor userValuePrincipal = userIdentity.getPrincipalDescriptor();
      String userValuePrincipalValue = null;

      if (oldValuePrincipal != null) {
        previousValuePrincipalValue = oldValuePrincipal.getValue();
      }

      if (userValuePrincipal != null) {
        userValuePrincipalValue = userValuePrincipal.getValue();
      }

      // If the user changed the stack default, replace the new stack default value with the user's
      // changed value
      if ((userValuePrincipalValue != null) && !userValuePrincipalValue.equals(previousValuePrincipalValue)) {
        updatedValuePrincipal.setValue(userValuePrincipalValue);
      }
    }

    // If the new identity definition is a reference and no longer has a keytab definition,
    // Ignore any user changes to the old keytab definition.
    if (updatedValueKeytab != null) {
      KerberosKeytabDescriptor oldValueKeytab = (previousStackIdentity == null) ? null : previousStackIdentity.getKeytabDescriptor();
      String previousValueKeytabFile = null;
      KerberosKeytabDescriptor userValueKeytab = userIdentity.getKeytabDescriptor();
      String userValueKeytabFile = null;

      if (oldValueKeytab != null) {
        previousValueKeytabFile = oldValueKeytab.getFile();
      }

      if (userValueKeytab != null) {
        userValueKeytabFile = userValueKeytab.getFile();
      }

      // If the user changed the stack default, replace the new stack default value with the user's
      // changed value
      if ((userValueKeytabFile != null) && !userValueKeytabFile.equals(previousValueKeytabFile)) {
        updatedValueKeytab.setFile(userValueKeytabFile);
      }
    }

    // Remove the when clause
    updatedValue.setWhen(null);

    return updatedValue;
  }

  /**
   * Processes the configuration-level Kerberos descriptors to add, remove, or update data in the user-specified
   * Kerberos descriptor.
   *
   * @param previousStackConfigurations a map of {@link KerberosConfigurationDescriptor}s from the previous stack version's Kerberos descriptor
   * @param newStackConfigurations      a map of {@link KerberosConfigurationDescriptor}s from the new stack version's Kerberos descriptor
   * @param userConfigurations          a map of {@link KerberosConfigurationDescriptor}s from the user-supplied Kerberos descriptor
   * @return a map of updated {@link KerberosConfigurationDescriptor}s
   */
  private static Map<String, KerberosConfigurationDescriptor> processConfigurations(Map<String, KerberosConfigurationDescriptor> previousStackConfigurations,
                                                                                    Map<String, KerberosConfigurationDescriptor> newStackConfigurations,
                                                                                    Map<String, KerberosConfigurationDescriptor> userConfigurations) {

    if ((userConfigurations == null) || ((previousStackConfigurations == null) && (newStackConfigurations == null))) {
      return userConfigurations;
    }

    Map<String, KerberosConfigurationDescriptor> updatedConfigurations = new TreeMap<>();

    if (previousStackConfigurations == null) {
      previousStackConfigurations = Collections.emptyMap();
    }

    if (newStackConfigurations == null) {
      newStackConfigurations = Collections.emptyMap();
    }

    // Find configurations to modify or remove
    for (Map.Entry<String, KerberosConfigurationDescriptor> entry : userConfigurations.entrySet()) {
      String name = entry.getKey();
      KerberosConfigurationDescriptor userValue = entry.getValue();

      if (userValue != null) {
        if (newStackConfigurations.containsKey(name)) {
          KerberosConfigurationDescriptor oldValue = previousStackConfigurations.get(name);
          KerberosConfigurationDescriptor newValue = newStackConfigurations.get(name);

          LOG.debug("Processing configuration type {} for modifications", name);
          updatedConfigurations.put(name, processConfiguration(oldValue, newValue, userValue));
        } else if (previousStackConfigurations.containsKey(name)) {
          LOG.debug("Removing configuration type {} from user-specified Kerberos Descriptor", name);
          // Nothing to do here, just don't add it to the updated configurations map...
        } else {
          LOG.debug("Leaving configuration type {} in user-specified Kerberos Descriptor unchanged since it was user-defined.", name);
          updatedConfigurations.put(name, userValue);
        }
      }
    }

    // Note: there is no need to add configuration definitions that do not exist in the user-specified
    // descriptor since they will get added dynamically when merged with the stack default value.

    return updatedConfigurations;
  }

  /**
   * Processes a {@link KerberosConfigurationDescriptor} to change the user-supplied data based on the changes
   * observed between the previous stack version's data and the new stack version's data.
   *
   * @param previousStackConfiguration a {@link KerberosConfigurationDescriptor} from the previous stack version's Kerberos descriptor
   * @param newStackConfiguration      a {@link KerberosConfigurationDescriptor} from the new stack version's Kerberos descriptor
   * @param userConfiguration          a {@link KerberosConfigurationDescriptor} from the user-specified Kerberos descriptor
   * @return an updated {@link KerberosConfigurationDescriptor}
   */
  private static KerberosConfigurationDescriptor processConfiguration(KerberosConfigurationDescriptor previousStackConfiguration,
                                                                      KerberosConfigurationDescriptor newStackConfiguration,
                                                                      KerberosConfigurationDescriptor userConfiguration) {

    KerberosConfigurationDescriptor updatedValue = new KerberosConfigurationDescriptor((userConfiguration == null) ? null : userConfiguration.toMap());

    Map<String, String> previousValue = (previousStackConfiguration == null) ? null : previousStackConfiguration.getProperties();
    Map<String, String> newValue = (newStackConfiguration == null) ? null : newStackConfiguration.getProperties();
    Map<String, String> userValue = updatedValue.getProperties();

    updatedValue.setProperties(processProperties(previousValue, newValue, userValue));

    return updatedValue;
  }

  /**
   * Processes a map of global or configuration properties to change the user-supplied data based on
   * the changes observed between the previous stack version's data and the new stack version's data.
   * <p>
   * If a property exists in both the previous and new stacks, and the user has not changed it; then
   * the value of the property will be updated to the new stack version's value.  Else, if the user
   * changed the value, the changed value will be left as-is.
   * <p>
   * If a property exists only in the previous stack, then it will be removed.
   * <p>
   * If a property exists only in the new stack, then it will be added.
   *
   * @param previousStackProperties properties from the previous stack version's Kerberos descriptor
   * @param newStackProperties      properties from the new stack version's Kerberos descriptor
   * @param userProperties          properties from the user-specified Kerberos descriptor
   * @return a new map of updated properties
   */
  private static Map<String, String> processProperties(Map<String, String> previousStackProperties,
                                                       Map<String, String> newStackProperties,
                                                       Map<String, String> userProperties) {

    if ((previousStackProperties == null) && (newStackProperties == null)) {
      return userProperties;
    } else {
      Map<String, String> updatedProperties = new TreeMap<>();
      if (userProperties != null) {
        updatedProperties.putAll(userProperties);
      }

      if (previousStackProperties == null) {
        previousStackProperties = Collections.emptyMap();
      }

      if (newStackProperties == null) {
        newStackProperties = Collections.emptyMap();
      }

      // Find properties to modify and remove
      for (Map.Entry<String, String> entry : previousStackProperties.entrySet()) {
        String name = entry.getKey();

        if (newStackProperties.containsKey(name)) {
          String previousValue = entry.getValue();
          String newValue = newStackProperties.get(name);
          String userValue = updatedProperties.get(name);

          // See if the user property should be modified...
          // Test if the old property value is different than the new property value and that the user did
          // not update the value from the old default. If the user updated the value, then it would be
          // risky to change it to the new stack default value.
          if (((previousValue == null) ? (newValue != null) : !previousValue.equals(newValue)) &&
              ((previousValue == null) ? (userValue == null) : previousValue.equals(userValue))) {
            LOG.debug("Modifying property named {} from user-specified Kerberos Descriptor", name);
            updatedProperties.put(name, newValue);
          }
        } else {
          LOG.debug("Removing property named {} from user-specified Kerberos Descriptor", name);
          updatedProperties.remove(name);
        }
      }

      // Find properties to add
      for (Map.Entry<String, String> entry : newStackProperties.entrySet()) {
        String name = entry.getKey();

        if (!previousStackProperties.containsKey(name) && !updatedProperties.containsKey(name)) {
          // A new property was found, add it...
          LOG.debug("Adding property named {} to user-specified Kerberos Descriptor", name);
          updatedProperties.put(name, entry.getValue());
        }
      }

      return updatedProperties;
    }
  }

  /**
   * A convenience method used to change a list of {@link KerberosIdentityDescriptor} items into a map
   * of identity names or {@link org.apache.ambari.server.api.services.HostKerberosIdentityService}
   * items.
   *
   * @param identities a list of {@link KerberosIdentityDescriptor}s
   * @return a map of identity names or {@link org.apache.ambari.server.api.services.HostKerberosIdentityService}
   */
  private static Map<String, KerberosIdentityDescriptor> toMap(List<KerberosIdentityDescriptor> identities) {
    if (identities == null) {
      return null;
    } else {
      Map<String, KerberosIdentityDescriptor> map = new TreeMap<>();

      for (KerberosIdentityDescriptor identity : identities) {
        map.put(identity.getName(), identity);
      }

      return map;
    }
  }
}
