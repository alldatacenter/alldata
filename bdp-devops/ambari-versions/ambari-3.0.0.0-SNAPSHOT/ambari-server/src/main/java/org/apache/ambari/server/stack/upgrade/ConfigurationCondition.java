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
package org.apache.ambari.server.stack.upgrade;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.MoreObjects;

/**
 * The {@link ConfigurationCondition} class is used to represent a condition on
 * a property.
 */
@XmlType(name = "config")
@XmlAccessorType(XmlAccessType.FIELD)
public final class ConfigurationCondition extends Condition {

  /**
   * The type of comparison to make.
   */
  @XmlEnum
  public enum ComparisonType {

    /**
     * Equals comparison.
     */
    @XmlEnumValue("equals")
    EQUALS,

    /**
     * Not equals comparison.
     */
    @XmlEnumValue("not-equals")
    NOT_EQUALS,

    /**
     * String contains.
     */
    @XmlEnumValue("contains")
    CONTAINS,

    /**
     * Does not contain.
     */
    @XmlEnumValue("not-contains")
    NOT_CONTAINS,

    /**
     * Exists with any value.
     */
    @XmlEnumValue("exists")
    EXISTS,

    /**
     * Does not exist.
     */
    @XmlEnumValue("not-exists")
    NOT_EXISTS;
  }

  /**
   * The configuration type, such as {@code hdfs-site}.
   */
  @XmlAttribute(name = "type")
  public String type;

  /**
   * The configuration property key.
   */
  @XmlAttribute(name = "property")
  public String property;

  /**
   * The value to compare against; only valid if comparison type is in (=, !=, contains, !contains).
   */
  @XmlAttribute(name = "value")
  public String value;

  /**
   * The value to return if comparison type is in (=, !=, contains, !contains) and the config is missing.
   */
  @XmlAttribute(name = "return_value_if_config_missing")
  public boolean returnValueIfConfigMissing;

  /**
   * The type of comparison to make.
   */
  @XmlAttribute(name = "comparison")
  public ComparisonType comparisonType;

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("type", type).add("property", property).add("value",
        value).add("comparison", comparisonType).omitNullValues().toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSatisfied(UpgradeContext upgradeContext) {
    Cluster cluster = upgradeContext.getCluster();

    boolean propertyExists = false;
    Config config = cluster.getDesiredConfigByType(type);
    Map<String, String> properties = null;
    if (null != config) {
      properties = config.getProperties();
      if (properties.containsKey(property)) {
        propertyExists = true;
      }
    }

    if (comparisonType == ComparisonType.EXISTS) {
      return propertyExists;
    }
    if (comparisonType == ComparisonType.NOT_EXISTS) {
      return !propertyExists;
    }

    // If property doesn't exist, we cannot make any claims using =, !=, contains !contains.
    // Therefore, check if the Upgrade Pack provided a default return value when the config is missing.
    if (!propertyExists) {
      return returnValueIfConfigMissing;
    }

    String propertyValue = properties.get(property);
    switch (comparisonType) {
      case EQUALS:
        return StringUtils.equals(propertyValue, value);
      case NOT_EQUALS:
        return !StringUtils.equals(propertyValue, value);
      case CONTAINS:
        return StringUtils.contains(propertyValue, value);
      case NOT_CONTAINS:
        return !StringUtils.contains(propertyValue, value);
      default:
        return false;
    }
  }
}

