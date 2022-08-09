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

package org.apache.ambari.server.view.configuration;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * View instance configuration.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class InstanceConfig {
  /**
   * The instance name.
   */
  private String name;

  /**
   * The public view name.
   */
  private String label;

  /**
   * The instance description.
   */
  private String description;

  /**
   * An optional short url for this instance
   */
  private String shortUrl;


  /**
   * Indicates whether or not the instance should be visible.
   */
  private boolean visible = true;

  /**
   * The icon path in the view archive.  Overrides the view
   * level icon attribute.
   */
  private String icon;

  /**
   * The big icon path in the view archive.  Overrides the view
   * level big icon attribute.
   */
  private String icon64;

  /**
   * The instance properties.
   */
  @XmlElement(name="property")
  private List<PropertyConfig> properties;

  /**
   * Get the instance name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the public view instance label.
   *
   * @return the view instance label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Get the  view instance description.
   *
   * @return the view instance description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Indicates whether or not the instance should be visible.
   *
   * @return true if the instance should be visible; false otherwise
   */
  public boolean isVisible() {
    return visible;
  }

  /**
   * Get the icon path in the view archive.  Overrides the view
   * level icon attribute.
   *
   * @return the icon path
   */
  public String getIcon() {
    return icon;
  }

  /**
   * Get the big icon path in the view archive. Overrides the view
   * level big icon attribute.
   *
   * @return the big icon path
   */
  public String getIcon64() {
    return icon64;
  }

  /**
   * Get the instance properties.
   *
   * @return the instance properties
   */
  public List<PropertyConfig> getProperties() {
    return properties == null ? Collections.emptyList() : properties;
  }

}
