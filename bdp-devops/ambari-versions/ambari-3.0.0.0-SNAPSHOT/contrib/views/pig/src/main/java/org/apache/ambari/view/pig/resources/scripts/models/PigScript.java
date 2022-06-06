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

package org.apache.ambari.view.pig.resources.scripts.models;

import org.apache.ambari.view.pig.persistence.utils.PersonalResource;
import org.apache.commons.beanutils.BeanUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;

/**
 * Bean to represent script
 */
public class PigScript implements Serializable, PersonalResource {
  private String id;

  private String title = "";
  private String pigScript = "";
  private String pythonScript = "";
  private String templetonArguments = "";
  private Date dateCreated;
  private String owner = "";

  private boolean opened = false;

  public PigScript() {
  }

  public PigScript(Map<String, Object> stringObjectMap) throws InvocationTargetException, IllegalAccessException {
    BeanUtils.populate(this, stringObjectMap);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PigScript)) return false;

    PigScript pigScript = (PigScript) o;

    if (!id.equals(pigScript.id)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getPigScript() {
    return pigScript;
  }

  public void setPigScript(String pigScript) {
    this.pigScript = pigScript;
  }

  public String getTempletonArguments() {
    return templetonArguments;
  }

  public void setTempletonArguments(String templetonArguments) {
    this.templetonArguments = templetonArguments;
  }

  public Date getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(Date dateCreated) {
    this.dateCreated = dateCreated;
  }

  public boolean isOpened() {
    return opened;
  }

  public void setOpened(boolean opened) {
    this.opened = opened;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getPythonScript() {
    return pythonScript;
  }

  public void setPythonScript(String pythonScript) {
    this.pythonScript = pythonScript;
  }
}
