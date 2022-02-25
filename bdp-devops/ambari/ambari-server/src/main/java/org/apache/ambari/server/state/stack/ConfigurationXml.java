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
package org.apache.ambari.server.state.stack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;

import org.apache.ambari.server.stack.Validable;
import org.apache.ambari.server.state.PropertyInfo;

/**
 * The elements within a service's configuration file.
 */
@XmlRootElement(name="configuration")
public class ConfigurationXml implements Validable{
  
  @XmlAnyAttribute
  private Map<QName, String> attributes = new HashMap<>();

  @XmlElement(name="property")
  private List<PropertyInfo> properties = new ArrayList<>();

  @XmlTransient
  private boolean valid = true;

  /**
   * 
   * @return valid xml flag
   */
  @Override
  public boolean isValid() {
    return valid;
  }

  /**
   * 
   * @param valid set validity flag
   */
  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  @XmlTransient
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
   * @return the list of properties contained in a configuration file
   */
  public List<PropertyInfo> getProperties() {
    return properties;
  }

  public Map<QName, String> getAttributes() {
    return attributes;
  }

}
