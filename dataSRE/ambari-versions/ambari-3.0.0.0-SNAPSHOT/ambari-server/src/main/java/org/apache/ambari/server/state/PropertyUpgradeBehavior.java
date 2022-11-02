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
package org.apache.ambari.server.state;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Represents a behavior used during ambari upgrade for property
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PropertyUpgradeBehavior {

  /**
   * If true, add property during ambari-upgrade. If config type does
   * not exist, it will be created only if there are properties that
   * are going to be added
   */
  @XmlAttribute(name="add", required = false)
  private boolean add = true;

  /**
   * If true, then remove this property during ambari-upgrade
   */
  @XmlAttribute(name="delete", required = false)
  private boolean delete = false;

  /**
   * If true, during ambari upgrade property value will be blindly overwritten
   * with default value
   */
  @XmlAttribute(name="update", required = false)
  private boolean update = false;

  public PropertyUpgradeBehavior() {}

  public PropertyUpgradeBehavior(boolean add, boolean delete, boolean update) {
    this.add = add;
    this.delete = delete;
    this.update = update;
  }

  public void setAdd( boolean add )
  {
    this.add = add;
  }

  public void setDelete( boolean delete )
  {
    this.delete = delete;
  }

  public void setUpdate(boolean update )
  {
    this.update = update;
  }

  public boolean isAdd() {
    return add;
  }

  public boolean isDelete() {
    return delete;
  }

  public boolean isUpdate() {
    return update;
  }
}
