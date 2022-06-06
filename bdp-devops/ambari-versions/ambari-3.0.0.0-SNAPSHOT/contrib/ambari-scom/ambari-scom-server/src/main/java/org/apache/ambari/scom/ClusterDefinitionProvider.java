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

package org.apache.ambari.scom;

import org.apache.ambari.server.configuration.Configuration;

import java.io.InputStream;

/**
 * Provider for a input stream to the cluster definition file.
 */
public class ClusterDefinitionProvider {

  /**
   * The file name.
   */
  private String fileName;

  /**
   * The cluster name.
   */
  private String clusterName;

  /**
   * The hadoop version Id.
   */
  private String versionId;

  /**
   * The singleton.
   */
  private static ClusterDefinitionProvider singleton = new ClusterDefinitionProvider();

  // ----- Constants ---------------------------------------------------------

  protected static final String SCOM_CLUSTER_DEFINITION_FILENAME         = "scom.cluster.definition.filename";
  protected static final String DEFAULT_SCOM_CLUSTER_DEFINITION_FILENAME = "clusterproperties.txt";

  protected static final String SCOM_CLUSTER_NAME    = "scom.cluster.name";
  protected static final String DEFAULT_CLUSTER_NAME = "ambari";

  protected static final String SCOM_VERSION_ID    = "scom.version.id";
  protected static final String DEFAULT_VERSION_ID = "HDP-1.3.0";


  // ----- Constructor -------------------------------------------------------

  protected ClusterDefinitionProvider() {
  }


  // ----- ClusterDefinitionProvider -----------------------------------

  /**
   * Initialize with the given configuration.
   *
   * @param configuration  the configuration
   */
  public void init(Configuration configuration) {
    fileName = configuration.getProperty(SCOM_CLUSTER_DEFINITION_FILENAME);
    if (fileName == null) {
      fileName = DEFAULT_SCOM_CLUSTER_DEFINITION_FILENAME;
    }

    clusterName = configuration.getProperty(SCOM_CLUSTER_NAME);
    if (clusterName == null) {
      clusterName = DEFAULT_CLUSTER_NAME;
    }

    versionId = configuration.getProperty(SCOM_VERSION_ID);
    if (versionId == null) {
      versionId = DEFAULT_VERSION_ID;
    }
  }

  /**
   * Get the singleton instance.
   *
   * @return the singleton instance
   */
  public static ClusterDefinitionProvider instance() {
    return singleton;
  }

  /**
   * Get the cluster definition file name.
   *
   * @return the file name
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * Get the cluster name.
   *
   * @return the cluster name
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Get the hadoop version Id.
   *
   * @return the version Id
   */
  public String getVersionId() {
    return versionId;
  }

  /**
   * Set the associated filename.
   *
   * @param fileName  the file name
   */
  protected void setFileName(String fileName) {
    this.fileName = fileName;
  }

  /**
   * Set the cluster name.
   *
   * @param clusterName  the cluster name
   */
  protected void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * Set the version id.
   *
   * @param versionId  the version id
   */
  protected void setVersionId(String versionId) {
    this.versionId = versionId;
  }

  /**
   * Get an input stream to the cluster definition file.
   *
   * @return an input stream
   */
  public InputStream getInputStream() {
    InputStream is;
    String name = this.fileName == null ? DEFAULT_SCOM_CLUSTER_DEFINITION_FILENAME : this.fileName;

    try {
      is = this.getClass().getClassLoader().getResourceAsStream(name);

      if (is == null) {
        throw new IllegalStateException("Can't find the resource " + name + " in the classpath.");
      }
    } catch (Exception e) {
      String msg = "Caught exception reading " + name + ".";
      throw new IllegalStateException(msg, e);
    }
    return is;
  }
}
