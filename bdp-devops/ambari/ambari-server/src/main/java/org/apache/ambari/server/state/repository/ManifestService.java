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

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Represents a service definition in the manifest.  The service manifest is a list
 * of all services available in a repository.
 */
public class ManifestService {

  /**
   * The XML unique id for the service and version.
   */
  @XmlAttribute(name="id")
  public String serviceId;

  /**
   * Name of the service.
   */
  @XmlAttribute(name="name")
  public String serviceName;

  /**
   * Version of the service.  This is the publicly available version of the binary.
   */
  @XmlAttribute(name="version")
  public String version;

  /**
   * Version id of the service.  This may be a build number.
   */
  @XmlAttribute(name="version-id")
  public String versionId;

  /**
   * The release version of the service.  This is not the same as {@link #version}; that is a binary version.
   * This version is a build specific string that is independent of the binary and roughly matches
   * {@link Release#version} for upgrade orchestration.
   */
  @XmlAttribute(name="release-version")
  public String releaseVersion;
}
