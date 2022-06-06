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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;

import com.google.gson.annotations.SerializedName;
/**
 * Represents service os-specific details (like repositories and packages). 
 * Represents <code>osSpecific</code>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceOsSpecific {

  private String osFamily;
  private Repo repo;

  public ServiceOsSpecific() {
  }

  public ServiceOsSpecific(String osFamily) {
    this.osFamily = osFamily;
  }

  @XmlElementWrapper(name="packages")
  @XmlElements(@XmlElement(name="package"))
  private List<Package> packages = new ArrayList<>();


  public String getOsFamily() {
    return osFamily;
  }


  public Repo getRepo() {
    return repo;
  }


  public List<Package> getPackages() {
    return packages;
  }

  public void addPackages(List<Package> packages) {
    this.packages.addAll(packages);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceOsSpecific that = (ServiceOsSpecific) o;

    if (osFamily != null ? !osFamily.equals(that.osFamily) : that.osFamily != null) return false;
    if (packages != null ? !packages.equals(that.packages) : that.packages != null) return false;
    if (repo != null ? !repo.equals(that.repo) : that.repo != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = osFamily != null ? osFamily.hashCode() : 0;
    result = 31 * result + (repo != null ? repo.hashCode() : 0);
    result = 31 * result + (packages != null ? packages.hashCode() : 0);
    return result;
  }

  /**
   * The <code>repo</code> tag. It has different set of fields compared to
   * <link>org.apache.ambari.server.state.RepositoryInfo</link>,
   * that's why we need another class
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Repo {

    @SerializedName("baseUrl")
    private String baseurl;
    @SerializedName("mirrorsList")
    private String mirrorslist;
    @SerializedName("repoId")
    private String repoid;
    @SerializedName("repoName")
    private String reponame;
    @SerializedName("distribution")
    private String distribution;
    @SerializedName("components")
    private String components;

    private Repo() {
    }
    
    /**
     * @return the base url
     */
    public String getBaseUrl() {
      return (null == baseurl || baseurl.isEmpty()) ? null : baseurl;
    }

    /**
     * @return the mirrorlist field
     */
    public String getMirrorsList() {
      return (null == mirrorslist || mirrorslist.isEmpty()) ? null : mirrorslist;
    }
    
    /**
     * @return the repo id
     */
    public String getRepoId() {
      return repoid;
    }
    
    /**
     * @return the repo name
     */
    public String getRepoName() {
      return reponame;
    }

    public String getDistribution() {
      return distribution;
    }

    public String getComponents() {
      return components;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Repo repo = (Repo) o;

      if (baseurl != null ? !baseurl.equals(repo.baseurl) : repo.baseurl != null) return false;
      if (mirrorslist != null ? !mirrorslist.equals(repo.mirrorslist) : repo.mirrorslist != null) return false;
      if (repoid != null ? !repoid.equals(repo.repoid) : repo.repoid != null) return false;
      if (reponame != null ? !reponame.equals(repo.reponame) : repo.reponame != null) return false;
      if (distribution != null ? !distribution.equals(repo.distribution) : repo.distribution != null) return false;
      if (components != null ? !components.equals(repo.components) : repo.components != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = baseurl != null ? baseurl.hashCode() : 0;
      result = 31 * result + (mirrorslist != null ? mirrorslist.hashCode() : 0);
      result = 31 * result + (repoid != null ? repoid.hashCode() : 0);
      result = 31 * result + (reponame != null ? reponame.hashCode() : 0);
      result = 31 * result + (distribution != null ? distribution.hashCode() : 0);
      result = 31 * result + (components != null ? components.hashCode() : 0);
      return result;
    }
  }



  /**
   * The <code>package</code> tag.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Package {
    private String name;
    private String condition = "";

    /**
     * If true, package will not be attempted to be upgraded during RU.
     * Typically, packages that are located outside of HDP* repositories,
     * should be marked as true
     */
    private Boolean skipUpgrade = Boolean.FALSE;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
    
    public String getCondition() {
      return condition;
    }

    public void setCondition(String condition) {
      this.condition = condition;
    }

    public Boolean getSkipUpgrade() {
      return skipUpgrade;
    }

    public void setSkipUpgrade(Boolean skipUpgrade) {
      this.skipUpgrade = skipUpgrade;
    }

    public Package() {
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Package aPackage = (Package) o;

      if (!name.equals(aPackage.name)) return false;
      if (!skipUpgrade.equals(aPackage.skipUpgrade)) return false;
      if (!condition.equals(aPackage.condition)) return false;
      
      return true;
    }

    @Override
    public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + skipUpgrade.hashCode();
      result = 31 * result + condition.hashCode();
      
      return result;
    }
  }
}

