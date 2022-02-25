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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.collections.Predicate;
import org.apache.ambari.server.collections.PredicateUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Optional;

/**
 * KerberosIdentityDescriptor is an implementation of an AbstractKerberosDescriptor that
 * encapsulates data related to a Kerberos identity - including its principal and keytab file details.
 * <p/>
 * A KerberosIdentityDescriptor has the following properties:
 * <ul>
 * <li>name</li>
 * <li>principal</li>
 * <li>keytab</li>
 * </ul>
 * <p/>
 * The following (pseudo) JSON Schema will yield a valid KerberosIdentityDescriptor
 * <pre>
 *   {
 *      "$schema": "http://json-schema.org/draft-04/schema#",
 *      "title": "KerberosIdentityDescriptor",
 *      "description": "Describes a Kerberos identity",
 *      "type": "object",
 *      "properties": {
 *        "name": {
 *          "description": "An identifying name for this identity. The name may reference another
 *                          KerberosIdentityDescriptor by declaring the path to it",
 *          "type": "string"
 *        },
 *        "principal": {
 *          "description": "Details about this identity's principal",
 *          "type": "{@link org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor}",
 *        }
 *        "keytab": {
 *          "description": "Details about this identity's keytab",
 *          "type": "{@link org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor}",
 *          }
 *        }
 *      }
 *   }
 * </pre>
 * <p/>
 * In this implementation,
 * {@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor#name} will hold the
 * KerberosIdentityDescriptor#name value
 */
public class KerberosIdentityDescriptor extends AbstractKerberosDescriptor {

  static final String KEY_REFERENCE = "reference";
  static final String KEY_PRINCIPAL = Type.PRINCIPAL.getDescriptorName();
  static final String KEY_KEYTAB = Type.KEYTAB.getDescriptorName();
  static final String KEY_WHEN = "when";

  /**
   * The path to the Kerberos Identity definitions this {@link KerberosIdentityDescriptor} references
   */
  private String reference = null;

  /**
   * The KerberosPrincipalDescriptor containing the principal details for this Kerberos identity
   */
  private KerberosPrincipalDescriptor principal = null;

  /**
   * The KerberosKeytabDescriptor containing the keytab details for this Kerberos identity
   */
  private KerberosKeytabDescriptor keytab = null;

  /**
   * An expression used to determine when this {@link KerberosIdentityDescriptor} is relevant for the
   * cluster. If the process expression is not <code>null</code> and evaluates to <code>false</code>
   * then this {@link KerberosIdentityDescriptor} will be ignored when processing identities.
   */
  private Predicate when = null;

  private String path = null;

  /**
   * Creates a new KerberosIdentityDescriptor
   *
   * @param name      the name of this identity descriptor
   * @param reference an optional path to a referenced KerberosIdentityDescriptor
   * @param principal a KerberosPrincipalDescriptor
   * @param keytab    a KerberosKeytabDescriptor
   * @param when      a predicate
   */
  public KerberosIdentityDescriptor(String name, String reference, KerberosPrincipalDescriptor principal, KerberosKeytabDescriptor keytab, Predicate when) {
    setName(name);
    setReference(reference);
    setPrincipalDescriptor(principal);
    setKeytabDescriptor(keytab);
    setWhen(when);
  }

  /**
   * Creates a new KerberosIdentityDescriptor
   * <p/>
   * See {@link org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor} for the JSON
   * Schema that may be used to generate this map.
   *
   * @param data a Map of values use to populate the data for the new instance
   * @see org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor
   */
  public KerberosIdentityDescriptor(Map<?, ?> data) {
    // The name for this KerberosIdentityDescriptor is stored in the "name" entry in the map
    // This is not automatically set by the super classes.
    setName(getStringValue(data, "name"));

    setReference(getStringValue(data, KEY_REFERENCE));

    if (data != null) {
      Object item;

      item = data.get(KEY_PRINCIPAL);
      if (item instanceof Map) {
        setPrincipalDescriptor(new KerberosPrincipalDescriptor((Map<?, ?>) item));
      }

      item = data.get(KEY_KEYTAB);
      if (item instanceof Map) {
        setKeytabDescriptor(new KerberosKeytabDescriptor((Map<?, ?>) item));
      }

      item = data.get(KEY_WHEN);
      if (item instanceof Map) {
        setWhen(PredicateUtils.fromMap((Map<?, ?>) item));
      }
    }
  }

  /**
   * Gets the path to the referenced Kerberos identity definition
   *
   * @return the path to the referenced Kerberos identity definition or <code>null</code> if not set
   */
  public String getReference() {
    return reference;
  }

  /**
   * Gets the absolute path to the referenced Kerberos identity definition
   *
   * @return the path to the referenced Kerberos identity definition or <code>null</code> if not set
   */
  public String getReferenceAbsolutePath() {
    String absolutePath;
    if(StringUtils.isEmpty(reference)) {
      absolutePath = getName();
    }
    else {
      absolutePath = reference;
    }

    if(!StringUtils.isEmpty(absolutePath) && !absolutePath.startsWith("/")) {
      String path = getPath();
      if(path == null) {
        path = "";
      }

      if(absolutePath.startsWith("..")) {
        AbstractKerberosDescriptor parent = getParent();
        if(parent != null) {
          parent = parent.getParent();

          if(parent != null) {
            absolutePath = absolutePath.replace("..", parent.getPath());
          }
        }
      }
      else if(absolutePath.startsWith(".")) {
        AbstractKerberosDescriptor parent = getParent();
        if (parent != null) {
          absolutePath = absolutePath.replace(".", parent.getPath());
        }
      }
    }

    return absolutePath;
  }

  /**
   * Sets the path to the referenced Kerberos identity definition
   *
   * @param reference the path to the referenced Kerberos identity definition or <code>null</code>
   *                  to indicate no reference
   */
  public void setReference(String reference) {
    this.reference = reference;
  }

  /**
   * Gets the KerberosPrincipalDescriptor related to this KerberosIdentityDescriptor
   *
   * @return the KerberosPrincipalDescriptor related to this KerberosIdentityDescriptor
   */
  public KerberosPrincipalDescriptor getPrincipalDescriptor() {
    return principal;
  }

  /**
   * Sets the KerberosPrincipalDescriptor related to this KerberosIdentityDescriptor
   *
   * @param principal the KerberosPrincipalDescriptor related to this KerberosIdentityDescriptor
   */
  public void setPrincipalDescriptor(KerberosPrincipalDescriptor principal) {
    this.principal = principal;

    if (this.principal != null) {
      this.principal.setParent(this);
    }
  }

  /**
   * Gets the KerberosKeytabDescriptor related to this KerberosIdentityDescriptor
   *
   * @return the KerberosKeytabDescriptor related to this KerberosIdentityDescriptor
   */
  public KerberosKeytabDescriptor getKeytabDescriptor() {
    return keytab;
  }

  /**
   * Sets the KerberosKeytabDescriptor related to this KerberosIdentityDescriptor
   *
   * @param keytab the KerberosKeytabDescriptor related to this KerberosIdentityDescriptor
   */
  public void setKeytabDescriptor(KerberosKeytabDescriptor keytab) {
    this.keytab = keytab;

    if (this.keytab != null) {
      this.keytab.setParent(this);
    }
  }

  /**
   * Gets the expression (or {@link Predicate}) to use to determine when to include this Kerberos
   * identity while processing Kerberos identities.
   * <p>
   * <code>null</code> indicates there is nothing to evaluate and this Kerberos identity is to always
   * be included when processing Kerberos identities.
   *
   * @return a predicate
   */
  public Predicate getWhen() {
    return when;
  }

  /**
   * Sets the expression (or {@link Predicate}) to use to determine when to include this Kerberos
   * identity while processing Kerberos identities.
   * <p>
   * <code>null</code> indicates there is nothing to evaluate and this Kerberos identity is to always
   * be included when processing Kerberos identities.
   *
   * @param when a predicate
   */
  public void setWhen(Predicate when) {
    this.when = when;
  }

  /**
   * Processes the expression indicating when this {@link KerberosIdentityDescriptor} is to be included
   * in the set of Kerberos identities to process.
   * <p>
   * <code>True</code> will be returned if the expression is <code>null</code> or if it evaluates
   * as such.
   *
   * @param context A Map of context values, including at least the list of services and available configurations
   * @return true if this {@link KerberosIdentityDescriptor} is to be included when processing the
   * Kerberos identities; otherwise false.
   */
  public boolean shouldInclude(Map<String, Object> context) {
    return (this.when == null) || this.when.evaluate(context);
  }

  /**
   * Updates this KerberosIdentityDescriptor with data from another KerberosIdentityDescriptor
   * <p/>
   * Properties will be updated if the relevant updated values are not null.
   *
   * @param updates the KerberosIdentityDescriptor containing the updated values
   */
  public void update(KerberosIdentityDescriptor updates) {
    if (updates != null) {
      setName(updates.getName());

      setReference(updates.getReference());

      KerberosPrincipalDescriptor existingPrincipal = getPrincipalDescriptor();
      if (existingPrincipal == null) {
        setPrincipalDescriptor(updates.getPrincipalDescriptor());
      } else {
        existingPrincipal.update(updates.getPrincipalDescriptor());
      }

      KerberosKeytabDescriptor existingKeytabDescriptor = getKeytabDescriptor();
      if (existingKeytabDescriptor == null) {
        setKeytabDescriptor(updates.getKeytabDescriptor());
      } else {
        existingKeytabDescriptor.update(updates.getKeytabDescriptor());
      }

      Predicate updatedWhen = updates.getWhen();
      if (updatedWhen != null) {
        setWhen(updatedWhen);
      }
    }
  }

  /**
   * Creates a Map of values that can be used to create a copy of this KerberosIdentityDescriptor
   * or generate the JSON structure described in
   * {@link org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor}
   *
   * @return a Map of values for this KerberosIdentityDescriptor
   * @see org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor
   */
  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> dataMap = super.toMap();

    if (reference != null) {
      dataMap.put(KEY_REFERENCE, reference);
    }

    if (principal != null) {
      dataMap.put(KEY_PRINCIPAL, principal.toMap());
    }

    if (keytab != null) {
      dataMap.put(KEY_KEYTAB, keytab.toMap());
    }

    if (when != null) {
      dataMap.put(KEY_WHEN, PredicateUtils.toMap(when));
    }

    return dataMap;
  }

  /***
   * A name that refers to a service has a format like /[<service name>/[<component name>/]]<identity name>
   * @return an optional referenced service name
   */
  public Optional<String> getReferencedServiceName() {
    return parseServiceName(reference).or(parseServiceName(getName()));
  }

  private Optional<String> parseServiceName(String name) {
    if (name != null && name.startsWith("/") && name.split("/").length > 2) {
      return Optional.of(name.split("/")[1]);
    } else {
      return Optional.absent();
    }
  }


  /**
   * @return true if the given identity has the same principal or keytab as me
   */
  public boolean isShared(KerberosIdentityDescriptor that) {
    return hasSamePrincipal(that) || hasSameKeytab(that);
  }

  private boolean hasSameKeytab(KerberosIdentityDescriptor that) {
    try {
      return this.getKeytabDescriptor().getFile().equals(that.getKeytabDescriptor().getFile());
    } catch (NullPointerException e) {
      return false;
    }
  }

  private boolean hasSamePrincipal(KerberosIdentityDescriptor that) {
    try {
      return this.getPrincipalDescriptor().getValue().equals(that.getPrincipalDescriptor().getValue());
    } catch (NullPointerException e) {
      return false;
    }
  }

  /**
   * Determines whether this {@link KerberosIdentityDescriptor} indicates it is a refrence to some
   * other {@link KerberosIdentityDescriptor}.
   * <p>
   * A KerberosIdentityDescriptor is a reference if it's <code>reference</code> attibute is set
   * or if (for backwards compatibility), its name indicates a path. For exmaple:
   * <ul>
   * <li><code>SERVICE/COMPONENT/identitiy_name</code></li>
   * <li><code>/identity_name</code></li>
   * <li><code>./identity_name</code></li>
   * </ul>
   *
   * @return true if this {@link KerberosIdentityDescriptor} indicates a reference; otherwise false
   */
  public boolean isReference() {
    String name = getName();
    return !StringUtils.isEmpty(reference) ||
        (!StringUtils.isEmpty(name) && (name.startsWith("/") || name.startsWith("./")));
  }

  /**
   * Calculate the path to this identity descriptor for logging purposes.
   * Examples:
   * /
   * /SERVICE
   * /SERVICE/COMPONENT
   * /SERVICE/COMPONENT/identity_name
   * <p>
   * This implementation calculates and caches the path if the path has not been previously set.
   *
   * @return a path
   */
  @Override
  public String getPath() {
    if (path == null) {
      path = super.getPath();
    }

    return path;
  }

  /**
   * Explicitly set the path to this {@link KerberosIdentityDescriptor}.
   * <p>
   * This is useful when creating detached identity descriptors while dereferencing identity references
   * so that the path information is not lost.
   *
   * @param path a path
   */
  void setPath(String path) {
    this.path = path;
  }

  @Override
  public int hashCode() {
    return super.hashCode() +
        ((getReference() == null)
            ? 0
            : getReference().hashCode()) +
        ((getPrincipalDescriptor() == null)
            ? 0
            : getPrincipalDescriptor().hashCode()) +
        ((getKeytabDescriptor() == null)
            ? 0
            : getKeytabDescriptor().hashCode()) +
        ((getWhen() == null)
            ? 0
            : getWhen().hashCode());
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    } else if (object == this) {
      return true;
    } else if (object.getClass() == KerberosIdentityDescriptor.class) {
      KerberosIdentityDescriptor descriptor = (KerberosIdentityDescriptor) object;
      return super.equals(object) &&
          (
              (getReference() == null)
                  ? (descriptor.getReference() == null)
                  : getReference().equals(descriptor.getReference())
          ) &&
          (
              (getPrincipalDescriptor() == null)
                  ? (descriptor.getPrincipalDescriptor() == null)
                  : getPrincipalDescriptor().equals(descriptor.getPrincipalDescriptor())
          ) &&
          (
              (getKeytabDescriptor() == null)
                  ? (descriptor.getKeytabDescriptor() == null)
                  : getKeytabDescriptor().equals(descriptor.getKeytabDescriptor())
          ) &&
          (
              (getWhen() == null)
                  ? (descriptor.getWhen() == null)
                  : getWhen().equals(descriptor.getWhen())
          );
    } else {
      return false;
    }
  }

  /**
   * Find all of the {@link KerberosIdentityDescriptor}s that reference this {@link KerberosIdentityDescriptor}
   *
   * @return a list of {@link KerberosIdentityDescriptor}s
   */
  public List<KerberosIdentityDescriptor> findReferences() {
    AbstractKerberosDescriptor root = getRoot();
    if(root instanceof AbstractKerberosDescriptorContainer) {
      return findIdentityReferences((AbstractKerberosDescriptorContainer)root, getPath());
    }
    else {
      return null;
    }
  }

  /**
   * Given a root, recursively traverse the tree of {@link AbstractKerberosDescriptorContainer}s looking for
   * {@link KerberosIdentityDescriptor}s that declare the given path as the referenced Kerberos identity.
   *
   * @param root the starting point
   * @param path the path to the referenced {@link KerberosIdentityDescriptor} in the {@link KerberosDescriptor}
   * @return a list of {@link KerberosIdentityDescriptor}s
   */
  private List<KerberosIdentityDescriptor> findIdentityReferences(AbstractKerberosDescriptorContainer root, String path) {
    if (root == null) {
      return null;
    }

    List<KerberosIdentityDescriptor> references = new ArrayList<>();

    // Process the KerberosIdentityDescriptors found in this node.
    List<KerberosIdentityDescriptor> identityDescriptors = root.getIdentities();
    if (identityDescriptors != null) {
      for (KerberosIdentityDescriptor identityDescriptor : identityDescriptors) {
        if (identityDescriptor.isReference()) {
          String reference = identityDescriptor.getReferenceAbsolutePath();

          if (!StringUtils.isEmpty(reference) && path.equals(reference)) {
            references.add(identityDescriptor);
          }
        }
      }
    }

    // Process the children of the node
    Collection<? extends AbstractKerberosDescriptorContainer> children = root.getChildContainers();
    if(!CollectionUtils.isEmpty(children)) {
      for (AbstractKerberosDescriptorContainer child : children) {
        Collection<KerberosIdentityDescriptor> childReferences = findIdentityReferences(child, path);
        if (!CollectionUtils.isEmpty(childReferences)) {
          // If references were found in the current child, add them to this node's list of references.
          references.addAll(childReferences);
        }
      }
    }

    return references;
  }
}
