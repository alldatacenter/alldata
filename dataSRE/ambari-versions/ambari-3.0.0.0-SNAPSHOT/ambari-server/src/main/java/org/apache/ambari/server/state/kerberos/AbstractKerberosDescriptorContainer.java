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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;

/**
 * AbstractKerberosDescriptorContainer is an abstract class implementing AbstractKerberosDescriptor
 * and providing facility to handle common descriptor container functionality.
 * <p/>
 * Each AbstractKerberosDescriptorContainer contains identities and configurations as well as a
 * name and a parent (which is inherited from AbstractKerberosDescriptor).
 * <p/>
 * An AbstractKerberosDescriptorContainer has the following properties:
 * <ul>
 * <li>identities</li>
 * <li>configurations</li>
 * </ul>
 * <p/>
 * The following (pseudo) JSON Schema will yield a valid AbstractKerberosDescriptorContainer
 * <pre>
 *   {
 *      "$schema": "http://json-schema.org/draft-04/schema#",
 *      "title": "AbstractKerberosDescriptorContainer",
 *      "description": "Describes an AbstractKerberosDescriptorContainer",
 *      "type": "object",
 *      "properties": {
 *        "identities": {
 *          "description": "A list of Kerberos identity descriptors",
 *          "type": "array",
 *          "items": {
 *            "title": "KerberosIdentityDescriptor"
 *            "type": "{@link org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor}"
 *          }
 *        },
 *        "configurations": {
 *          "description": "A list of relevant configuration blocks",
 *          "type": "array",
 *          "items": {
 *            "title": "KerberosConfigurationDescriptor"
 *            "type": "{@link org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor}"
 *          }
 *        },
 *        "auth_to_local": {
 *          "description": "A list of configuration properties declaring which properties are auth-to-local values
 *          "type": "array",
 *          "items": {
 *            "title": "String"
 *            "type": "{@link java.lang.String}"
 *          }
 *        }
 *      }
 *   }
 * </pre>
 * <p/>
 * This implementation does not set the
 * {@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor#name} value, it is
 * left up to the implementing class to do so.
 */
public abstract class AbstractKerberosDescriptorContainer extends AbstractKerberosDescriptor {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractKerberosDescriptorContainer.class);

  static final String KEY_IDENTITIES = Type.IDENTITY.getDescriptorPluralName();
  static final String KEY_CONFIGURATIONS = Type.CONFIGURATION.getDescriptorPluralName();
  static final String KEY_AUTH_TO_LOCAL_PROPERTIES = Type.AUTH_TO_LOCAL_PROPERTY.getDescriptorPluralName();

  /**
   * Regular expression pattern used to parse auth_to_local property specifications into the following
   * parts:
   * <ul>
   * <li>configuration type (optional, if _global_)</li>
   * <li>property name</li>
   * <li>concatenation type (optional, if using the default behavior)</li>
   * </ul>
   */
  public static final Pattern AUTH_TO_LOCAL_PROPERTY_SPECIFICATION_PATTERN = Pattern.compile("^(?:(.+?)/)?(.+?)(?:\\|(.+?))?$");

  /**
   * A List of KerberosIdentityDescriptors contained in this AbstractKerberosDescriptorContainer
   */
  private List<KerberosIdentityDescriptor> identities = null;

  /**
   * A Map of KerberosConfigurationDescriptors contained in this AbstractKerberosDescriptorContainer
   */
  private Map<String, KerberosConfigurationDescriptor> configurations = null;

  /**
   * A Set of configuration identifiers (config-type/property_name) that indicate which properties
   * contain auth_to_local values.
   */
  private Set<String> authToLocalProperties = null;

  /**
   * Constructs a new AbstractKerberosDescriptorContainer
   * <p/>
   * This constructor must be called from the constructor(s) of the implementing classes
   *
   * @param data a Map of data used for collecting groups of common descriptors
   */
  protected AbstractKerberosDescriptorContainer(Map<?, ?> data) {
    if (data != null) {
      Object list;

      // (Safely) Get the set of KerberosIdentityDescriptors
      list = data.get(KEY_IDENTITIES);
      if (list instanceof Collection) {
        for (Object item : (Collection) list) {
          if (item instanceof Map) {
            putIdentity(new KerberosIdentityDescriptor((Map<?, ?>) item));
          }
        }
      }

      // (Safely) Get the set of KerberosConfigurationDescriptors
      list = data.get(KEY_CONFIGURATIONS);
      if (list instanceof Collection) {
        for (Object item : (Collection) list) {
          if (item instanceof Map) {
            putConfiguration(new KerberosConfigurationDescriptor((Map<?, ?>) item));
          }
        }
      }

      // (Safely) Get the set of KerberosConfigurationDescriptors
      list = data.get(KEY_AUTH_TO_LOCAL_PROPERTIES);
      if (list instanceof Collection) {
        for (Object item : (Collection) list) {
          if (item instanceof String) {
            putAuthToLocalProperty((String) item);
          }
        }
      }
    }
  }

  /**
   * Returns the child containers associated with this container.
   *
   * @return an immutable collection of {@link AbstractKerberosDescriptorContainer}s
   */
  public abstract Collection<? extends AbstractKerberosDescriptorContainer> getChildContainers();

  /**
   * Returns a specific named child container
   *
   * @param name the name of the child container to retrieve
   * @return an {@link AbstractKerberosDescriptorContainer}
   */
  public abstract AbstractKerberosDescriptorContainer getChildContainer(String name);

  /**
   * Returns the raw List of KerberosIdentityDescriptors contained within this
   * AbstractKerberosDescriptorContainer.
   * <p/>
   * The returned KerberosIdentityDescriptors are not merged with data from referenced
   * KerberosConfigurationDescriptors. This is the same calling
   * {@link AbstractKerberosDescriptorContainer#getIdentities(boolean, Map)} and setting the
   * argument to 'false'
   *
   * @return the relevant List of KerberosIdentityDescriptors
   */
  public List<KerberosIdentityDescriptor> getIdentities() {
    try {
      return getIdentities(false, null);
    } catch (AmbariException e) {
      // AmbariException will not be thrown unless an error occurs while trying to dereference
      // identities.  This method does not attempt to dereference identities.
      return null;
    }
  }

  /**
   * Set the {@link KerberosIdentityDescriptor} for this {@link AbstractKerberosDescriptorContainer}.
   *
   * @param identities a {@link List} of {@link KerberosIdentityDescriptor}s
   */
  public void setIdentities(List<KerberosIdentityDescriptor> identities) {
    this.identities = (identities == null)
        ? null
        : new ArrayList<>(identities);
  }

  /**
   * Returns a List of KerberosIdentityDescriptors contained within this
   * AbstractKerberosDescriptorContainer.
   * <p/>
   * If resolveReferences is true, a "detached" set of KerberosIdentityDescriptors are returned.
   * Any KerberosIdentityDescriptor that implies it references some other KerberosIdentityDescriptor
   * in the hierarchy will be resolved. Meaning, if the name of the KerberosIdentityDescriptor
   * indicates a path to some other KerberosIdentityDescriptor (i.e, /spnego, /HDFS/NAMENODE/nn, etc...)
   * the referenced KerberosIdentityDescriptor is found, detached (or copied), and updated with
   * the information from the initial KerberosIdentityDescriptor.  Because of this, all of the
   * KerberosIdentityDescriptors to be included are copied into the resulting list, and dissociating
   * them with the rest of the hierarchy such that changes to them will not be reflected within the
   * entire KerberosDescriptor tree.
   * <p/>
   * If resolveReferences is false, the raw List of KerberosIdentityDescriptors are returned. This
   * data is not manipulated by resolving references and therefore it may be missing data, however
   * this List is of "attached" descriptors, so changes will be reflected within the KerberosDescriptor
   * hierarchy.
   *
   * @param resolveReferences a Boolean value indicating whether to resolve references (true) or not
   *                          (false)
   * @return a List of the requested KerberosIdentityDescriptors
   */
  public List<KerberosIdentityDescriptor> getIdentities(boolean resolveReferences, Map<String, Object> contextForFilter) throws AmbariException {
    if (identities == null) {
      return null;
    } else {
      List<KerberosIdentityDescriptor> list = new ArrayList<>();

      for (KerberosIdentityDescriptor identity : identities) {
        KerberosIdentityDescriptor identityToAdd;

        if (resolveReferences) {
          // Dereference this KerberosIdentityDescriptor, if necessary
          identityToAdd = dereferenceIdentity(identity);
        } else {
          identityToAdd = identity;
        }

        // Make sure this Kerberos Identity is not to be filtered out based on its "when" clause
        if ((identityToAdd != null) && ((contextForFilter == null) || identityToAdd.shouldInclude(contextForFilter))) {
          if (isReferredServiceInstalled(identity, contextForFilter)) {
            list.add(identityToAdd);
          } else {
            LOG.info("Skipping identity {} because referred service is not installed.", identityToAdd.getName());
          }
        }
      }
      return list;
    }
  }

  private static boolean isReferredServiceInstalled(KerberosIdentityDescriptor identity, Map<String, Object> contextForFilter) {
    if (contextForFilter == null || !(contextForFilter.get("services") instanceof Collection)) {
      return true;
    }
    Set<String> installedServices = Sets.newHashSet((Collection) contextForFilter.get("services"));
    return identity.getReferencedServiceName().transform(contains(installedServices)).or(true);
  }

  private static Function<String, Boolean> contains(final Set<String> installed) {
    return Functions.forPredicate(Predicates.in(installed));
  }

  /**
   * Return a KerberosIdentityDescriptor with the specified name.
   *
   * @param name a String declaring the name of the descriptor to retrieve
   * @return the requested KerberosIdentityDescriptor
   */
  public KerberosIdentityDescriptor getIdentity(String name) {
    KerberosIdentityDescriptor identity = null;

    if ((name != null) && (identities != null)) {
      // Iterate over the List of KerberosIdentityDescriptors to find one with the requested name
      // If one is found, break out of the loop.
      for (KerberosIdentityDescriptor descriptor : identities) {
        if (name.equals(descriptor.getName())) {
          identity = descriptor;
          break;
        }
      }
    }

    return identity;
  }

  /**
   * Adds the specified KerberosIdentityDescriptor to the list of KerberosIdentityDescriptor.
   * <p/>
   * This method attempts to ensure that the names or KerberosIdentityDescriptors are unique within
   * the List.
   *
   * @param identity the KerberosIdentityDescriptor to add
   */
  public void putIdentity(KerberosIdentityDescriptor identity) {
    if (identity != null) {
      String name = identity.getName();

      if (identities == null) {
        identities = new ArrayList<>();
      }

      // If the identity has a name, ensure that one one with that name is in the List
      // Note: this cannot be enforced since any AbstractKerberosDescriptor+'s name property can be
      // changed
      if ((name != null) && !name.isEmpty()) {
        removeIdentity(identity.getName());
      }

      identities.add(identity);

      // Set the identity's parent to this AbstractKerberosDescriptorContainer
      identity.setParent(this);
    }
  }

  /**
   * Remove all KerberosIdentityDescriptors have have the specified name
   * <p/>
   * One or more KerberosIdentityDescriptors may be removed if multiple KerberosIdentityDescriptors
   * have the same name.
   *
   * @param name a String declaring the name of the descriptors to remove
   */
  public void removeIdentity(String name) {
    if ((name != null) && (identities != null)) {
      Iterator<KerberosIdentityDescriptor> iterator = identities.iterator();

      while (iterator.hasNext()) {
        KerberosIdentityDescriptor identity = iterator.next();
        if (name.equals(identity.getName())) {
          identity.setParent(null);
          iterator.remove();
        }
      }
    }
  }

  /**
   * Sets the {@link Map} of {@link KerberosConfigurationDescriptor}s for this
   * {@link AbstractKerberosDescriptorContainer}.
   *
   * @param configurations a {@link Map} of {@link KerberosConfigurationDescriptor}s
   */
  public void setConfigurations(Map<String, KerberosConfigurationDescriptor> configurations) {
    this.configurations = (configurations == null)
        ? null
        : new TreeMap<>(configurations);
  }

  /**
   * Returns a Map of raw KerberosConfigurationDescriptors contained within this
   * AbstractKerberosDescriptorContainer.
   * <p/>
   * The returned KerberosConfigurationDescriptors are not merged with data from KerberosDescriptor
   * hierarchy. This is the same calling
   * {@link AbstractKerberosDescriptorContainer#getConfigurations(boolean)} and setting the argument
   * to 'false'
   *
   * @return a List of KerberosConfigurationDescriptors
   */
  public Map<String, KerberosConfigurationDescriptor> getConfigurations() {
    return getConfigurations(false);
  }

  /**
   * Returns a Map of KerberosConfigurationDescriptors contained within this
   * AbstractKerberosDescriptorContainer.
   * <p/>
   * If includeInherited is true, the Map will contain "detached" KerberosConfigurationDescriptor
   * instances, but the data will be merged with all relevant KerberosConfigurationDescriptors within
   * the hierarchy.  Data higher in the hierarchy (the service level is higher in the hierarchy than
   * the component level) will be overwritten by data lower in the hierarchy.
   * <p/>
   * If includeInherited is false, the Map will consist of attached (non-merged)
   * KerberosConfigurationDescriptors. This data is not manipulated by merging with data within the
   * KerberosDescriptor hierarchy and therefore it may be missing data, however this Map is of
   * "attached" descriptors, so changes will be reflected within the KerberosDescriptor
   * hierarchy.
   *
   * @param includeInherited a Boolean value indicating whether to include configuration within the
   *                         KerberosDescriptor hierarchy (true) or not (false)
   * @return a Map of Strings to KerberosConfigurationDescriptors, where the key is the type
   * (core-site, etc...)
   */
  public Map<String, KerberosConfigurationDescriptor> getConfigurations(boolean includeInherited) {
    if (includeInherited) {
      Map<String, KerberosConfigurationDescriptor> mergedConfigurations = new TreeMap<>();
      List<Map<String, KerberosConfigurationDescriptor>> configurationSets = new ArrayList<>();
      AbstractKerberosDescriptor currentDescriptor = this;

      // Walk up the hierarchy and collect the configuration sets.
      while (currentDescriptor != null) {
        if (currentDescriptor.isContainer()) {
          Map<String, KerberosConfigurationDescriptor> configurations = ((AbstractKerberosDescriptorContainer) currentDescriptor).getConfigurations();

          if (configurations != null) {
            configurationSets.add(configurations);
          }
        }

        currentDescriptor = currentDescriptor.getParent();
      }

      // Reverse the collection so that we can merge from top to bottom
      Collections.reverse(configurationSets);

      for (Map<String, KerberosConfigurationDescriptor> map : configurationSets) {
        for (Map.Entry<String, KerberosConfigurationDescriptor> entry : map.entrySet()) {
          // For each configuration type, copy it and determine if an entry exists or not.
          // ** If one exists, merge the current data into the existing data (potentially
          //    overwriting values).
          // ** If one does not exist, simply add a copy of the current one to the Map
          String currentType = entry.getKey();
          KerberosConfigurationDescriptor currentConfiguration = entry.getValue();

          if (currentConfiguration != null) {
            KerberosConfigurationDescriptor detachedConfiguration = new KerberosConfigurationDescriptor(currentConfiguration.toMap());
            KerberosConfigurationDescriptor mergedConfiguration = mergedConfigurations.get(entry.getKey());

            if (mergedConfiguration == null) {
              mergedConfigurations.put(currentType, detachedConfiguration);
            } else {
              mergedConfiguration.update(detachedConfiguration);
            }
          }
        }
      }

      return mergedConfigurations;
    } else {
      return configurations;
    }
  }

  /**
   * Adds the specified KerberosConfigurationDescriptor to the list of KerberosConfigurationDescriptors.
   * <p/>
   * If an entry exists of the same configuration type, it will be overwritten.
   *
   * @param configuration the KerberosConfigurationDescriptor to add
   */
  public void putConfiguration(KerberosConfigurationDescriptor configuration) {
    if (configuration != null) {
      String type = configuration.getType();

      if (type == null) {
        throw new IllegalArgumentException("The configuration type must not be null");
      }

      if (configurations == null) {
        configurations = new TreeMap<>();
      }

      configurations.put(type, configuration);

      // Set the configuration's parent to this AbstractKerberosDescriptorContainer
      configuration.setParent(this);
    }
  }

  /**
   * Returns the requested KerberosConfigurationDescriptor
   *
   * @param name a String declaring the name of the descriptor to retrieve
   * @return the requested KerberosConfigurationDescriptor or null if not found
   */
  public KerberosConfigurationDescriptor getConfiguration(String name) {
    return ((name == null) || (configurations == null)) ? null : configurations.get(name);
  }

  /**
   * Adds the specified property name to the set of <code>auth_to_local</code> property names.
   * <p/>
   * Each <code>auth_to_local</code> property name is expected to be in the following format:
   * <code>config-type/property_name</code>`
   *
   * @param authToLocalProperty the auth_to_local property to add
   */
  public void putAuthToLocalProperty(String authToLocalProperty) {
    if (authToLocalProperty != null) {
      if (authToLocalProperties == null) {
        authToLocalProperties = new TreeSet<>();
      }

      authToLocalProperties.add(authToLocalProperty);
    }
  }

  /**
   * Sets the set of <code>auth_to_local</code> property names.
   *
   * @param authToLocalProperties a Set of String values; or null if not set
   */
  public void setAuthToLocalProperties(Set<String> authToLocalProperties) {
    this.authToLocalProperties = (authToLocalProperties == null)
        ? null
        : new TreeSet<>(authToLocalProperties);
  }

  /**
   * Gets the set of <code>auth_to_local</code> property names.
   *
   * @return a Set of String values; or null if not set
   */
  public Set<String> getAuthToLocalProperties() {
    return authToLocalProperties;
  }

  /**
   * Test this AbstractKerberosDescriptor to see if it is a container.
   * <p/>
   * This implementation always returns true since it implements a descriptor container.
   *
   * @return true if this AbstractKerberosDescriptor is a container, false otherwise
   */
  @Override
  public boolean isContainer() {
    return true;
  }

  /**
   * Updates this AbstractKerberosDescriptorContainer using information from the supplied
   * AbstractKerberosDescriptorContainer.
   * <p/>
   * Information from updates will overwrite information in this AbstractKerberosDescriptorContainer.
   * More specifically, the name of this AbstractKerberosDescriptorContainer may be updated as well
   * as each individual KerberosIdentityDescriptor and KerberosConfigurationDescriptor contained
   * within it.  Any new KerberosIdentityDescriptors and KerberosConfigurationDescriptors will be
   * appended to there appropriate lists.
   *
   * @param updates an AbstractKerberosDescriptorContainer containing the updates to this
   *                AbstractKerberosDescriptorContainer
   */
  public void update(AbstractKerberosDescriptorContainer updates) {
    if (updates != null) {
      String updatedName = updates.getName();
      if (updatedName != null) {
        setName(updatedName);
      }

      Map<String, KerberosConfigurationDescriptor> updatedConfigurations = updates.getConfigurations();
      if (updatedConfigurations != null) {
        for (Map.Entry<String, KerberosConfigurationDescriptor> entry : updatedConfigurations.entrySet()) {
          KerberosConfigurationDescriptor existingConfiguration = getConfiguration(entry.getKey());

          // Copy this descriptor so we don't alter the hierarchy of existing data we don't intend to change
          KerberosConfigurationDescriptor clone = new KerberosConfigurationDescriptor(entry.getValue().toMap());

          if (existingConfiguration == null) {
            putConfiguration(clone);
          } else {
            existingConfiguration.update(clone);
          }
        }
      }

      List<KerberosIdentityDescriptor> updatedIdentities = updates.getIdentities();
      if (updatedIdentities != null) {
        for (KerberosIdentityDescriptor updatedIdentity : updatedIdentities) {
          KerberosIdentityDescriptor existing = getIdentity(updatedIdentity.getName());

          // Copy this descriptor so we don't alter the hierarchy of existing data we don't intend to change
          KerberosIdentityDescriptor clone = new KerberosIdentityDescriptor(updatedIdentity.toMap());

          if (existing == null) {
            putIdentity(clone);
          } else {
            existing.update(clone);
          }
        }
      }

      Set<String> updatedAuthToLocalProperties = updates.getAuthToLocalProperties();
      if (updatedAuthToLocalProperties != null) {
        for (String updatedAuthToLocalProperty : updatedAuthToLocalProperties) {
          putAuthToLocalProperty(updatedAuthToLocalProperty);
        }
      }
    }
  }

  /**
   * Attempts to find the KerberosIdentityDescriptor at the specified path.
   * <p/>
   * The path value is expected to be an "absolute" path through the Kerberos Descriptor hierarchy
   * to some specific KerberosIdentityDescriptor.  The path must be in one of the following forms:
   * <ul>
   * <li>/identity</li>
   * <li>/service/identity</li>
   * <li>/service/component/identity</li>
   * </ul>
   * <p/>
   * If the path starts with "../", the ".." will be translated to the path of the parent item.
   * In the following example, <code>../service_identity</code> will resolve to
   * <code>/SERVICE/service_identity</code>:
   * <pre>
   * {
   *  "name": "SERVICE",
   *  "identities": [
   *    {
   *      "name": "service_identity",
   *      ...
   *    }
   *  ],
   *  "components" : [
   *    {
   *      "name": "COMPONENT",
   *      "identities": [
   *        {
   *          "name": "./service_identity",
   *          ...
   *        },
   *        ...
   *      ]
   *    }
   *  ]
   * }
   * </pre>
   *
   * @param path a String declaring the path to a KerberosIdentityDescriptor
   * @return a KerberosIdentityDescriptor identified by the path or null if not found
   */
  public KerberosIdentityDescriptor getReferencedIdentityDescriptor(String path)
      throws AmbariException {
    KerberosIdentityDescriptor identityDescriptor = null;

    if (path != null) {
      if (path.startsWith("../")) {
        // Resolve parent path
        AbstractKerberosDescriptor parent = getParent();

        path = path.substring(2);

        while (parent != null) {
          String name = parent.getName();

          if (name != null) {
            path = String.format("/%s", name) + path;
          }

          parent = parent.getParent();
        }
      }

      if (path.startsWith("/")) {
        // The name indicates it is referencing an identity somewhere in the hierarchy... try to find it.
        // /[<service name>/[<component name>/]]<identity name>
        String[] pathParts = path.split("/");

        String serviceName = null;
        String componentName = null;
        String identityName;

        switch (pathParts.length) {
          case 4:
            serviceName = pathParts[1];
            componentName = pathParts[2];
            identityName = pathParts[3];
            break;
          case 3:
            serviceName = pathParts[1];
            identityName = pathParts[2];
            break;
          case 2:
            identityName = pathParts[1];
            break;
          case 1:
            identityName = pathParts[0];
            break;
          default:
            throw new AmbariException(String.format("Unexpected path length in %s", path));
        }

        if (identityName != null) {
          // Start at the top of the hierarchy
          AbstractKerberosDescriptor descriptor = getRoot();

          if (descriptor != null) {
            if ((serviceName != null) && !serviceName.isEmpty()) {
              descriptor = descriptor.getDescriptor(Type.SERVICE, serviceName);

              if ((descriptor != null) && (componentName != null) && !componentName.isEmpty()) {
                descriptor = descriptor.getDescriptor(Type.COMPONENT, componentName);
              }
            }

            if (descriptor != null) {
              descriptor = descriptor.getDescriptor(Type.IDENTITY, identityName);

              if (descriptor instanceof KerberosIdentityDescriptor) {
                identityDescriptor = (KerberosIdentityDescriptor) descriptor;
              }
            }
          }
        }
      }
    }

    return identityDescriptor;
  }

  /**
   * Gets the requested AbstractKerberosDescriptor implementation using a type name and a relevant
   * descriptor name.
   * <p/>
   * This implementation handles identity and configuration descriptors within this
   * AbstractKerberosDescriptorContainer.
   * <p/>
   * Implementing classes should override this to handle other types, but call this method to
   * ensure no data is missed.
   *
   * @param type a String indicating the type of the requested descriptor
   * @param name a String indicating the name of the requested descriptor
   * @return a AbstractKerberosDescriptor representing the requested descriptor or null if not found
   */
  @Override
  protected AbstractKerberosDescriptor getDescriptor(Type type, String name) {
    if (Type.IDENTITY == type) {
      return getIdentity(name);
    } else if (Type.CONFIGURATION == type) {
      return getConfiguration(name);
    } else {
      return null;
    }
  }

  /**
   * Creates a Map of values that can be used to create a copy of this AbstractKerberosDescriptorContainer
   * or generate the JSON structure described in
   * {@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer}
   *
   * @return a Map of values for this AbstractKerberosDescriptorContainer
   * @see org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer
   */
  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = super.toMap();

    if (identities != null) {
      // Use a TreeMap to force the identities definitions to be ordered by name, alphebetically.
      // This helps with readability and comparisons.
      Map<String, Map<String, Object>> list = new TreeMap<>();
      for (KerberosIdentityDescriptor identity : identities) {
        list.put(identity.getName(), identity.toMap());
      }
      map.put(KEY_IDENTITIES, list.values());
    }

    if (configurations != null) {
      // Use a TreeMap to force the configurations to be ordered by configuration type, alphebetically.
      // This helps with readability and comparisons.
      Map<String, Map<String, Object>> list = new TreeMap<>();
      for (KerberosConfigurationDescriptor configuration : configurations.values()) {
        list.put(configuration.getType(), configuration.toMap());
      }
      map.put(KEY_CONFIGURATIONS, list.values());
    }

    if (authToLocalProperties != null) {
      map.put(KEY_AUTH_TO_LOCAL_PROPERTIES, authToLocalProperties);
    }

    return map;
  }

  /**
   * @return identities which are not references to other identities
   */
  public List<KerberosIdentityDescriptor> getIdentitiesSkipReferences() {
    return nullToEmpty(getIdentities())
      .stream()
      .filter(identity -> !identity.getReferencedServiceName().isPresent() && !identity.isReference())
      .collect(toList());
  }

  @Override
  public int hashCode() {
    return super.hashCode() +
        ((getIdentities() == null)
            ? 0
            : getIdentities().hashCode()) +
        ((getAuthToLocalProperties() == null)
            ? 0
            : getAuthToLocalProperties().hashCode()) +
        ((getConfigurations() == null)
            ? 0
            : getConfigurations().hashCode());
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    } else if (object == this) {
      return true;
    } else if (object instanceof AbstractKerberosDescriptorContainer) {
      AbstractKerberosDescriptorContainer descriptor = (AbstractKerberosDescriptorContainer) object;
      return super.equals(object) &&
          (
              (getIdentities() == null)
                  ? (descriptor.getIdentities() == null)
                  : getIdentities().equals(descriptor.getIdentities())
          ) &&
          (
              (getAuthToLocalProperties() == null)
                  ? (descriptor.getAuthToLocalProperties() == null)
                  : getAuthToLocalProperties().equals(descriptor.getAuthToLocalProperties())
          ) &&
          (
              (getConfigurations() == null)
                  ? (descriptor.getConfigurations() == null)
                  : getConfigurations().equals(descriptor.getConfigurations())
          );
    } else {
      return false;
    }
  }

  /**
   * Recursively dereference a referenced identity.
   * <p>
   * Follows the path of references such that the top-most identity definition (one with no pointer
   * to a referenced identity) contains the base  information which is copied and updated with the
   * referencing identity's data. The composite identity is then update with the next referencing
   * identity's data, and so on until the initial identity is encountered.
   *
   * @param identity the initial identity to dereference
   * @return a (disconnected) {@link KerberosIdentityDescriptor} built by traversing the identity
   * references; or the input identity if it does not reference any other identities.
   * @throws AmbariException
   */
  private KerberosIdentityDescriptor dereferenceIdentity(KerberosIdentityDescriptor identity) throws AmbariException {
    KerberosIdentityDescriptor dereferencedIdentity = null;

    if (identity != null) {
      KerberosIdentityDescriptor referencedIdentity;
      try {
        if (identity.getReference() != null) {
          referencedIdentity = getReferencedIdentityDescriptor(identity.getReference());
        } else {
          // For backwards compatibility, see if the identity's name indicates a reference...
          referencedIdentity = getReferencedIdentityDescriptor(identity.getName());

          if(referencedIdentity != null) {
            // Log this since it is deprecated...
            LOG.warn("Referenced identities should be declared using the identity's \"reference\" attribute, not the identity's \"name\" attribute." +
                " This is a deprecated feature. Problems may occur in the future unless this is corrected: {}:{}", identity.getPath(), identity.getName());
          }
        }
      } catch (AmbariException e) {
        throw new AmbariException(String.format("Invalid Kerberos identity reference: %s", identity.getReference()), e);
      }

      if (referencedIdentity != null) {
        dereferencedIdentity = dereferenceIdentity(referencedIdentity);  // Dereference the "parent"...

        if (dereferencedIdentity != null) {
          dereferencedIdentity.update(identity);
        } else {
          dereferencedIdentity = new KerberosIdentityDescriptor(referencedIdentity.toMap());
          dereferencedIdentity.update(identity);
        }
      } else {
        dereferencedIdentity = new KerberosIdentityDescriptor(identity.toMap());
      }

      // Force the path for this identity descriptor to be the same as the original identity descriptor's.
      dereferencedIdentity.setPath(identity.getPath());
    }

    return dereferencedIdentity;
  }
}
