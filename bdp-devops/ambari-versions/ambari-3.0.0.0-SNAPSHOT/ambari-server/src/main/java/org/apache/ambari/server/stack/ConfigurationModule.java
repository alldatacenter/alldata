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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.PropertyInfo;


/**
 * Configuration module which provides functionality related to parsing and fully
 * resolving a configuration from the stack definition. Each instance is specific
 * to a configuration type.
 */
public class ConfigurationModule extends BaseModule<ConfigurationModule, ConfigurationInfo> implements Validable{
  /**
   * Configuration type
   */
  private String configType;

  /**
   * Associated configuration info
   */
  ConfigurationInfo info;

  /**
   * Specifies whether this configuration is marked as deleted
   */
  private boolean isDeleted;

  /**
   * validity flag
   */
  protected boolean valid = true;

  private Set<String> errorSet = new HashSet<>();
  
  @Override
  public void addError(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection<String> getErrors() {
    return errorSet;
  }   
  
  /**
   * Constructor.
   *
   * @param configType  configuration type
   * @param info        configuration info
   */
  public ConfigurationModule(String configType, ConfigurationInfo info) {
    this.configType = configType;
    this.info = info;
    if (info != null && !info.isValid()){
      setValid(info.isValid());
      addErrors(info.getErrors());
    }
  }

  @Override
  public void resolve(ConfigurationModule parent, Map<String, StackModule> allStacks,
	    Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions) throws AmbariException {
    // merge properties also removes deleted props so should be called even if extension is disabled
    if (parent != null) {
      if (parent.info != null) {
        if (!parent.isValid() || !parent.info.isValid()) {
          setValid(false);
          info.setValid(false);
          addErrors(parent.getErrors());
          addErrors(parent.info.getErrors());
          info.addErrors(parent.getErrors());
          info.addErrors(parent.info.getErrors());
        }
      }

      mergeProperties(parent);

      if (isExtensionEnabled()) {
        mergeAttributes(parent);
      }
    }
  }

  @Override
  public ConfigurationInfo getModuleInfo() {
    return info;
  }

  @Override
  public boolean isDeleted() {
    return isDeleted;
  }

  @Override
  public String getId() {
    return getConfigType();
  }

  /**
   * Obtain the configuration type.
   *
   * @return configuration type
   */
  public String getConfigType() {
    return configType;
  }


  /**
   * Set the deleted flag.
   *
   * @param isDeleted  whether the configuration has been marked for deletion
   */
  public void setDeleted(boolean isDeleted) {
    this.isDeleted = isDeleted;
  }

  /**
   * Merge configuration properties with the configurations parent.
   *
   * @param parent  parent configuration module
   */
  private void mergeProperties(ConfigurationModule parent) {
    Collection<String> existingProps = new HashSet<>();
    Iterator<PropertyInfo> iter = info.getProperties().iterator();
    while (iter.hasNext()) {
      PropertyInfo prop = iter.next();
      existingProps.add(prop.getFilename() + "/" + prop.getName());
      if (prop.isDeleted()) {
        iter.remove();
      }
    }

    if (isExtensionEnabled()) {
      for (PropertyInfo prop : parent.info.getProperties()) {
        if (! existingProps.contains(prop.getFilename() + "/" + prop.getName())) {
          info.getProperties().add(prop);
        }
      }
    }
  }

  /**
   * Merge configuration attributes with the parent configuration.
   *
   * @param parent  parent configuration module
   */
  private void mergeAttributes(ConfigurationModule parent) {

    for (Map.Entry<String, Map<String, String>> parentCategoryEntry : parent.info.getAttributes().entrySet()) {
      String category = parentCategoryEntry.getKey();
      Map<String, String> categoryAttributeMap = info.getAttributes().get(category);
      if (categoryAttributeMap == null) {
        categoryAttributeMap = new HashMap<>();
        info.getAttributes().put(category, categoryAttributeMap);
      }
      for (Map.Entry<String, String> parentAttributeEntry : parentCategoryEntry.getValue().entrySet()) {
        String attributeName = parentAttributeEntry.getKey();
        if (! categoryAttributeMap.containsKey(attributeName)) {
          categoryAttributeMap.put(attributeName, parentAttributeEntry.getValue());
        }
      }
    }
  }

  /**
   * Determine if the configuration should extend the parents configuration.
   *
   * @return true if this configuration should extend the parents; false otherwise
   */
  //todo: is this valuable as a generic module concept?
  private boolean isExtensionEnabled() {
    Map<String, String> supportsMap = getModuleInfo().getAttributes().get(ConfigurationInfo.Supports.KEYWORD);
    if (supportsMap == null) {
      return true;
    }

    String val = supportsMap.get(ConfigurationInfo.Supports.DO_NOT_EXTEND.getPropertyName());
    return val == null || val.equals("false");
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  @Override
  public void addErrors(Collection<String> errors) {
    this.errorSet.addAll(errors);
  }
}
