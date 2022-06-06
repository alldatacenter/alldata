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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * The {@link UpgradeActionOperations} is used to instruct Ambari Server to
 * perform actions during an upgrade. It is returned by
 * {@link UpgradeAction#getOperations(org.apache.ambari.spi.ClusterInformation, UpgradeInformation)}
 */
public class UpgradeActionOperations {

  /**
   * Any configuration changes which should be made.
   */
  private List<ConfigurationChanges> m_configurationChanges;

  /**
   * Any configuration types which should be completely removed.
   */
  private Set<String> m_configurationTypeRemovals;

  /**
   * A buffer that the {@link UpgradeAction} can use to pass messages back to
   * Ambari to display to the user.
   */
  private String m_standardOutput;

  /**
   * Sets configuration changes which are a part of the actions to be performed
   * during the upgrade for a single configuration type only. If multiple
   * configuration types are being updated, then
   * {@link #setConfigurationChanges(List)} should be used.
   *
   * @param configurationChanges
   *          the configuration changes to make.
   * @return an instance of the {@link UpgradeActionOperations} with the value
   *         set.
   * @see #setConfigurationChanges(List)
   */
  public UpgradeActionOperations setConfigurationChanges(
      ConfigurationChanges configurationChanges) {
    setConfigurationChanges(Collections.singletonList(configurationChanges));
    return this;
  }

  /**
   * Sets configuration changes which are a part of the actions to be performed
   * during the upgrade.
   *
   * @param configurationChanges
   *          the configuration changes to make.
   * @return an instance of the {@link UpgradeActionOperations} with the value
   *         set.
   */
  public UpgradeActionOperations setConfigurationChanges(
      List<ConfigurationChanges> configurationChanges) {
    m_configurationChanges = configurationChanges;
    return this;
  }

  /**
   * Adds a single configuration change to the operations which should be
   * performed. This might be more useful thank the bulk methods, such as
   * {@link #setConfigurationChanges(List)}.
   *
   * @param configurationChanges
   *          the configuration changes to make.
   * @return an instance of the {@link UpgradeActionOperations} with the value
   *         set.
   */
  public UpgradeActionOperations addConfigurationChange(ConfigurationChanges configurationChanges) {
    if (null == m_configurationChanges) {
      m_configurationChanges = new ArrayList<>();
    }

    m_configurationChanges.add(configurationChanges);
    return this;
  }

  /**
   * Sets any configuration types which should be completely removed.
   *
   * @param configurationTypeRemovals
   *          the configuration types to be removed, if any.
   * @return an instance of the {@link UpgradeActionOperations} with the value
   *         set.
   */
  public UpgradeActionOperations setConfigurationTypeRemoval(
      Set<String> configurationTypeRemovals) {
    m_configurationTypeRemovals = configurationTypeRemovals;
    return this;
  }

  /**
   * Sets the standard output which will be used by the server to display
   * information about what actions are being performed.
   *
   * @param standardOutput
   *          the output which will be displayed by the upgrade process.
   * @return an instance of the {@link UpgradeActionOperations} with the value
   *         set.
   */
  public UpgradeActionOperations setStandardOutput(String standardOutput) {
    m_standardOutput = standardOutput;
    return this;
  }

  /**
   * Gets the configurations changes which should be performed by the server.
   *
   * @return the configuration changes.
   */
  public List<ConfigurationChanges> getConfigurationChanges() {
    return m_configurationChanges;
  }

  /**
   * Gets the configuration types which should be removed, if any.
   *
   * @return the configuration types which should be completely removed, if any.
   */
  public Set<String> getConfigurationTypeRemovals() {
    return m_configurationTypeRemovals;
  }

  /**
   * Gets the standard output, if any, for the server to display as part of the
   * action being run.
   *
   * @return any messages that should be display along with the command's
   *         status.
   */
  public String getStandardOutput() {
    return m_standardOutput;
  }

  /**
   * The type of configuration change being made.
   */
  public enum ChangeType {
    /**
     * Adds or updates the property key along with the supplied value.
     */
    SET,

    /**
     * Removes the specified property key from the configuration type.
     */
    REMOVE
  }

  /**
   * The additions, updates, and removals for a specific configuration type,
   * such as {@code foo-site}.
   */
  public static class ConfigurationChanges {
    /**
     * The configuration type, such as {@code foo-site}.
     */
    private final String m_configType;

    /**
     * The property changes for this configuration type.
     */
    private final List<PropertyChange> m_changes = new ArrayList<>();

    /**
     * {@code true} if the only changes included are removals (or there are no
     * changes at all), {@code false} if there are additions and/or
     * modifications as well.
     */
    private boolean m_onlyRemovals = true;

    /**
     * Constructor.
     *
     * @param configType
     *          the name of the configuration type, such as {@code foo-site}.
     */
    public ConfigurationChanges(String configType) {
      m_configType = configType;
    }

    /**
     * Sets either a new or an update to an existing configuration property.
     *
     * @param propertyName
     *          the name of the property.
     * @param propertyValue
     *          the value for the property.
     * @return an instance of this class with the value set.
     */
    public ConfigurationChanges set(String propertyName, String propertyValue) {
      m_onlyRemovals = false;

      PropertyChange propertyChange = new PropertyChange(ChangeType.SET, propertyName,
          propertyValue);

      m_changes.add(propertyChange);
      return this;
    }

    /**
     * Marks a configuration property for removal.
     *
     * @param propertyName
     *          the name of the property to remove.
     * @return an instance of this class with the property marked for removal.
     */
    public ConfigurationChanges remove(String propertyName) {
      PropertyChange propertyChange = new PropertyChange(ChangeType.REMOVE, propertyName, null);
      m_changes.add(propertyChange);
      return this;
    }

    /**
     * Gets the name of the configuration tyoe, such as {@code foo-site}.
     *
     * @return the config type name.
     */
    public String getConfigType() {
      return m_configType;
    }

    /**
     * Gets all of the additions, updates, and removals for this configuration
     * type.
     *
     * @return all of the various property changes, including additions and
     *         removals.
     */
    public List<PropertyChange> getPropertyChanges() {
      return m_changes;
    }

    /**
     * Gets whether the only changes included for this configuration type are
     * removals (or there are no changes at all of any kind). If there are any
     * additions or modifications, this will be {@code false}.
     *
     * @return whether there are only removals in this change request, or if
     *         there are also additions or modifications to properties.
     */
    public boolean isOnlyRemovals() {
      return m_onlyRemovals;
    }

    /**
     * Gets whether there are no changes yet for this configuration change type.
     *
     * @return {@code true} if there are no changes yet, otherwise
     *         {@code false}.
     */
    public boolean isEmpty() {
      return null == m_changes || m_changes.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      if(m_changes.isEmpty()) {
        return "There are no configuration changes";
      }

      StringBuilder buffer = new StringBuilder(m_configType);
      buffer.append(System.lineSeparator());

      for( PropertyChange propertyChange : m_changes ) {
        switch(propertyChange.getChangeType()) {
          case REMOVE:
            buffer.append("  Removed " ).append(propertyChange.getPropertyName());
            break;
          case SET:
            buffer
              .append("  Set " )
              .append(propertyChange.getPropertyName())
              .append(" to " )
              .append(StringUtils.abbreviateMiddle(propertyChange.getPropertyValue(), "\u2026", 30));
            break;
          default:
            break;

        }
      }

      return buffer.toString();
    }
  }

  /**
   * The {@link PropertyChange} class represents either the addition, setting,
   * or removal of a configuration property.
   */
  public static class PropertyChange {
    /**
     * The change type.
     */
    private final ChangeType m_changeType;

    /**
     * The name of the property.
     */
    private final String m_propertyName;

    /**
     * The value to use if the type is {@link ConfigurationChangeType#SET}.
     */
    private final String m_propertyValue;

    /**
     * Constructor.
     *
     * @param changeType
     *          the type of configuration change.
     * @param propertyName
     *          the name of the property being added, updated, or removed.
     * @param propertyValue
     *          the value to add/update if the type is {@link ChangeType#SET}.
     */
    public PropertyChange(ChangeType changeType,
        String propertyName, String propertyValue) {
      m_changeType = changeType;
      m_propertyName = propertyName;
      m_propertyValue = propertyValue;
    }

    /**
     * Gets the type of configuration change.
     *
     * @return the change type.
     */
    public ChangeType getChangeType() {
      return m_changeType;
    }

    /**
     * Gets the name of the property to add, update, or remove.
     *
     * @return the property name.
     */
    public String getPropertyName() {
      return m_propertyName;
    }

    /**
     * Gets the name of the property value to set if the configuration type is
     * {@link ChangeType#SET}
     *
     * @return the property value.
     */
    public String getPropertyValue() {
      return m_propertyValue;
    }
  }
}
