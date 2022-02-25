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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * View instance property configuration.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PropertyConfig {
  /**
   * The property key.
   */
  private String key;

  /**
   * The property value.
   */
  private String value;

  /**
   * Get the property key.
   *
   * @return the property key
   */
  public String getKey() {
    return key;
  }

  /**
   * Get the property value.
   *
   * @return the property value
   */
  public String getValue() {
    return value;
  }
}
