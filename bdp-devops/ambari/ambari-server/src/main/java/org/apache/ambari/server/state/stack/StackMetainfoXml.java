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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.ambari.server.stack.Validable;

/**
 * Represents the stack <code>metainfo.xml</code> file.
 */
@XmlRootElement(name="metainfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class StackMetainfoXml implements Validable{

  public String getMinJdk() {
    return minJdk;
  }

  public String getMaxJdk() {
    return maxJdk;
  }

  @XmlElement(name="minJdk")
  private String minJdk = null;

  @XmlElement(name="maxJdk")
  private String maxJdk = null;

  @XmlElement(name="extends")
  private String extendsVersion = null;

  @XmlElement(name="versions")
  private Version version = new Version();

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
   * @return the parent stack version number
   */
  public String getExtends() {
    return extendsVersion;
  }

  /**
   * @return gets the version
   */
  public Version getVersion() {
    return version;
  }

  public void setVersion(Version version) {
    this.version = version;
  }

  public void setMinJdk(String minJdk) {
    this.minJdk = minJdk;
  }

  public void setMaxJdk(String maxJdk) {
    this.maxJdk = maxJdk;
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Version {
    public Version() {
    }
    private boolean active = false;
    private String stackReleaseVersion;

    /**
     * @return <code>true</code> if the stack is active
     */
    public boolean isActive() {
      return active;
    }

    public String getReleaseVersion() {
      return stackReleaseVersion;
    }

    public void setActive(boolean active) {
      this.active = active;
    }
  }

}



