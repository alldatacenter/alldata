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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.ambari.server.stack.Validable;

/**
 * Represents the extension <code>metainfo.xml</code> file.
 *
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
@XmlRootElement(name="metainfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class ExtensionMetainfoXml implements Validable{

  @XmlElement(name="extends")
  private String extendsVersion = null;

  @XmlElement(name="versions")
  private Version version = new Version();

  @XmlElement(name="prerequisites")
  private Prerequisites prerequisites = new Prerequisites();

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Prerequisites {
    private Prerequisites() {
    }
    @XmlElementWrapper(name="min-stack-versions")
    @XmlElements(@XmlElement(name="stack"))
    private List<Stack> stacks = new ArrayList<>();

    @XmlElementWrapper(name="min-extension-versions")
    @XmlElements(@XmlElement(name="extension"))
    private List<Extension> extensions = new ArrayList<>();

    public List<Stack> getStacks() {
      return stacks;
    }

    public List<Extension> getExtensions() {
      return extensions;
    }
  }

  @XmlTransient
  private boolean valid = true;

  @XmlElement(name="auto-link")
  private boolean autoLink = false;

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

  public List<Stack> getStacks() {
    return prerequisites.getStacks();
  }

  public List<Extension> getExtensions() {
    return prerequisites.getExtensions();
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Version {
    private Version() {
    }
    private boolean active = false;
    private String upgrade = null;

    /**
     * @return <code>true</code> if the stack is active
     */
    public boolean isActive() {
      return active;
    }

    /**
     * @return the upgrade version number, if set
     */
    public String getUpgrade() {
      return upgrade;
    }
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Stack {
    private Stack() {
    }
    private String name = null;
    private String version = null;

    /**
     * @return the stack name
     */
    public String getName() {
      return name;
    }

    /**
     * @return the stack version, this may be something like 1.0.*
     */
    public String getVersion() {
      return version;
    }
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Extension {
    private Extension() {
    }
    private String name = null;
    private String version = null;

    /**
     * @return the extension name
     */
    public String getName() {
      return name;
    }

    /**
     * @return the extension version, this may be something like 1.0.*
     */
    public String getVersion() {
      return version;
    }
  }

  public boolean isAutoLink() {
    return autoLink;
  }

  public void setAutoLink(boolean autoLink) {
    this.autoLink = autoLink;
  }

}
