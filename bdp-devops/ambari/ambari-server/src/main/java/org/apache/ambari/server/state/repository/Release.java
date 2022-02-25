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
package org.apache.ambari.server.state.repository;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.stack.StackReleaseInfo;
import org.apache.ambari.spi.stack.StackReleaseVersion;

/**
 * Release information for a repository.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Release {

  /**
   * The type of repository dictates how the repo should be installed and its upgradability.
   */
  @XmlElement(name="type")
  public RepositoryType repositoryType;

  /**
   * The stack id for the repository.
   */
  @XmlElement(name="stack-id")
  public String stackId;

  /**
   * The stack-based release version.
   */
  @XmlElement(name="version")
  public String version;

  /**
   * The build identifier.
   */
  @XmlElement(name="build")
  public String build;

  /**
   * The hotfix number.
   */
  @XmlElement(name="hotfix")
  public String hotfix;

  /**
   * The compatability regex.  This is used to relate the release to another release.
   */
  @XmlElement(name="compatible-with")
  public String compatibleWith;

  /**
   * The release notes or link.
   */
  @XmlElement(name="release-notes")
  public String releaseNotes;

  /**
   * The optional display name
   */
  @XmlElement(name="display")
  public String display;

  /**
   * @return the full version
   */
  public String getFullVersion(StackReleaseVersion stackVersion) {
    return stackVersion.getFullVersion(new StackReleaseInfo(
        version, hotfix, build));
  }

  /**
   * @return the release info for the VDF
   */
  public StackReleaseInfo getReleaseInfo() {
    return new StackReleaseInfo(version, hotfix, build);
  }

}
