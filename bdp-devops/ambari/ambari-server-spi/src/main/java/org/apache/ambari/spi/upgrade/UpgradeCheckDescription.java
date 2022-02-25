/**
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
package org.apache.ambari.spi.upgrade;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

/**
 * The {@link UpgradeCheckDescription} is used to provide information about an upgrade
 * check, such as its description, applicable upgrade types, and the various
 * reasons which could cause it to fail.
 */
public class UpgradeCheckDescription {

  /**
   * The default key to use when creating a new {@link UpgradeCheckDescription} failure.
   */
  public static final String DEFAULT = "default";

  /**
   * All of the instantiated {@link UpgradeCheckDescription}s.
   */
  private static final Set<UpgradeCheckDescription> s_values = new LinkedHashSet<>();

  /**
   * A unique identifier.
   */
  private final String m_name;
  private final UpgradeCheckType m_type;
  private final String m_description;
  private Map<String, String> m_fails;

  /**
   * Constructor.
   *
   * @param name
   *          a unique identifier for the description container.
   * @param type
   *          the area which is being checked.
   * @param description
   *          a description of what this check is.
   * @param failureReason
   *          a description of the single failure reason associated with this
   *          check. This will be associated with the {@link #DEFAULT} failure
   *          key.
   */
  public UpgradeCheckDescription(String name, UpgradeCheckType type, String description,
      String failureReason) {
    this(name, type, description, new ImmutableMap.Builder<String, String>().put(
        UpgradeCheckDescription.DEFAULT, failureReason).build());
  }

  /**
   * Constructor.
   *
   * @param name
   *          a unique identifier for the description container.
   * @param type
   *          the area which is being checked.
   * @param description
   *          a description of what this check is.
   * @param fails
   *          the mappings of failure reasons which could potentially happen in
   *          an upgrade check.
   */
  public UpgradeCheckDescription(String name, UpgradeCheckType type, String description, Map<String, String> fails) {
    m_name = name;
    m_type = type;
    m_description = description;
    m_fails = fails;

    if (s_values.contains(this)) {
      throw new RuntimeException("Unable to add the upgrade check description named " + m_name
          + " because it already is registered");
    }

    s_values.add(this);
  }

  /**
   * Gets the unique name of the upgrade check.
   *
   * @return the name of check
   */
  public String name() {
    return m_name;
  }

  /**
   * Gets all of the registered check descriptions.
   *
   * @return all of the registered {@link UpgradeCheckDescription} instances.
   */
  public Set<UpgradeCheckDescription> values() {
    return s_values;
  }

  /**
   * Gets the area in Ambari that this check is meant for.
   *
   * @return the type of check
   */
  public UpgradeCheckType getType() {
    return m_type;
  }

  /**
   * @return the text associated with the description
   */
  public String getText() {
    return m_description;
  }

  /**
   * @param key the failure text key
   * @return the fail text template.  Never {@code null}
   */
  public String getFailureReason(String key) {
    return m_fails.containsKey(key) ? m_fails.get(key) : "";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(m_name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object object) {
    if (null == object) {
      return false;
    }

    if (this == object) {
      return true;
    }

    if (object.getClass() != getClass()) {
      return false;
    }

    UpgradeCheckDescription that = (UpgradeCheckDescription) object;

    return Objects.equals(m_name, that.m_name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", m_name).toString();
  }

  /**
   * Gets the default failure reason associated with the {@link #DEFAULT} key.
   *
   * @return the default failure reason.
   */
  public String getDefaultFailureReason() {
    if(null == m_fails) {
      return null;
    }

    if(m_fails.size() == 1) {
      return m_fails.values().stream().findFirst().get();
    }

    return m_fails.get(DEFAULT);
  }
}