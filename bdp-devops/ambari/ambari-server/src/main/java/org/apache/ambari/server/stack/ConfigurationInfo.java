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

package org.apache.ambari.server.stack;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.PropertyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Encapsulates configuration properties and attributes for a single type.
 */
public class ConfigurationInfo implements Validable{
  /**
   * Collection of properties
   */
  private Collection<PropertyInfo> properties;

  /**
   * Map of attribute category to a map of attribute name/value
   */
  private Map<String, Map<String, String>> attributes;

  /**
   * Logger instance
   */
  private final static Logger LOG = LoggerFactory.getLogger(ConfigurationInfo.class);

  /**
   * validity flag
   */
  protected boolean valid = true;
  
  /**
   * Constructor.
   *
   * @param properties  configuration properties
   * @param attributes  configuration attributes
   */
  public ConfigurationInfo(Collection<PropertyInfo> properties, Map<String, String> attributes) {
    this.properties = properties;
    setAttributes(attributes);
  }

  /**
   * Obtain the configuration properties.
   *
   * @return collection of properties
   */
  public Collection<PropertyInfo> getProperties() {
    return properties;
  }

  /**
   * Obtain the configuration attributes.
   *
   * @return map of attribute category to a map of attribute names/values
   */
  public  Map<String, Map<String, String>> getAttributes() {
    return attributes;
  }

  /**
   * Set the default value of all type attributes which are not already specified.
   */
  public void ensureDefaultAttributes() {
    Map<String, String> supportsAttributes = attributes.get(Supports.KEYWORD);
    for (Supports supportsProperty : Supports.values()) {
      String propertyName = supportsProperty.getPropertyName();
      if (! supportsAttributes.containsKey(propertyName)) {
        supportsAttributes.put(propertyName, supportsProperty.getDefaultValue());
      }
    }
  }

  /**
   *
   * Set the specified configuration type attributes.
   *
   * @param specifiedAttributes attributes that have been specified in configuration
   */
  private void setAttributes(Map<String, String> specifiedAttributes) {
    Map<String, Map<String, String>> attributes = new HashMap<>();
    Map<String, String> supportsAttributes = new HashMap<>();
    attributes.put(Supports.KEYWORD, supportsAttributes);

    for (Map.Entry<String, String> entry : specifiedAttributes.entrySet()) {
      String attributeName = entry.getKey();
      Supports s = Supports.attributeNameValueOf(attributeName);
      if (s != null) {
        supportsAttributes.put(s.getPropertyName(),
            Boolean.valueOf(entry.getValue()).toString());
      } else {
        LOG.warn("Unknown configuration type attribute is specified: {}={}", attributeName, entry.getValue());
      }
    }
    this.attributes = attributes;
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  private Set<String> errorSet = new HashSet<>();
  
  @Override
  public void addError(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection<String> getErrors() {
    return errorSet;
  }   

  @Override
  public void addErrors(Collection<String> errors) {
    this.errorSet.addAll(errors);
  }  
  
  /**
   * Service configuration-types can support different abilities. This
   * enumerates the various abilities that configuration-types can support.
   *
   * For example, Hadoop configuration types like 'core-site' and 'hdfs-site'
   * can support the ability to define certain configs as 'final'.
   */
  public enum Supports {

    FINAL("supports_final"),
    ADDING_FORBIDDEN("supports_adding_forbidden"),
    DO_NOT_EXTEND("supports_do_not_extend");

    public static final String KEYWORD = "supports";

    private String defaultValue;
    private String xmlAttributeName;

    Supports(String xmlAttributeName) {
      this(xmlAttributeName, Boolean.FALSE.toString());
    }

    Supports(String xmlAttributeName, String defaultValue) {
      this.defaultValue = defaultValue;
      this.xmlAttributeName = xmlAttributeName;
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public String getXmlAttributeName() {
      return xmlAttributeName;
    }

    public String getPropertyName() {
      return name().toLowerCase();
    }

    public static Supports attributeNameValueOf(String attributeName) {
      for (Supports s : values()) {
        if (s.getXmlAttributeName().equals(attributeName)) {
          return s;
        }
      }
      return null;
    }
  }
}
