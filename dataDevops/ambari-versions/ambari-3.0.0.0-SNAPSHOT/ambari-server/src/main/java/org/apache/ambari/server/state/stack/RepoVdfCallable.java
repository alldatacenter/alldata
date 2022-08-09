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
package org.apache.ambari.server.state.stack;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.ambari.server.stack.StackModule;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the work to resolve the latest repo information for a stack.
 * This class must be used AFTER the stack has created its owned repositories.
 */
public class RepoVdfCallable implements Callable<Void> {

  private final static Logger LOG = LoggerFactory.getLogger(RepoVdfCallable.class);

  // !!! these are required for this callable to work
  private final StackInfo m_stack;
  private final OsFamily m_family;
  private final Map<String, URI> m_vdfMap;

  // !!! determines if this is for manifests or latest-vdf
  private String m_version;

  public RepoVdfCallable(StackModule stackModule,
      String version, Map<String, URI> vdfOsMap, OsFamily os_family) {
    m_stack = stackModule.getModuleInfo();
    m_family = os_family;
    m_version = version;
    m_vdfMap = vdfOsMap;
  }

  public RepoVdfCallable(StackModule stackModule,
      Map<String, URI> vdfOsMap, OsFamily os_family) {
    m_stack = stackModule.getModuleInfo();
    m_family = os_family;
    m_version = null;
    m_vdfMap = vdfOsMap;
  }

  @Override
  public Void call() throws Exception {
    if (MapUtils.isEmpty(m_vdfMap)) {
      return null;
    }

    boolean forLatest = (null == m_version);

    StackId stackId = new StackId(m_stack);

    VersionDefinitionXml xml = mergeDefinitions(stackId, m_version, m_vdfMap);

    if (null == xml) {
      return null;
    }

    if (forLatest) {
      xml.setStackDefault(true);
      m_stack.setLatestVersionDefinition(xml);
    } else {
      m_stack.addVersionDefinition(m_version, xml);
    }

    return null;
  }

  /**
   * Merges definitions loaded from the common file
   * @param stackId the stack id
   * @param version the version string
   * @param osMap   the map containing all the VDF for an OS
   * @return the merged version definition
   * @throws Exception
   */
  private VersionDefinitionXml mergeDefinitions(StackId stackId, String version,
      Map<String, URI> osMap) throws Exception {

    Set<String> oses = new HashSet<>();
    for (RepositoryInfo ri : m_stack.getRepositories()) {
      if (null != m_family.find(ri.getOsType())) {
        oses.add(m_family.find(ri.getOsType()));
      }
    }

    VersionDefinitionXml.Merger merger = new VersionDefinitionXml.Merger();

    for (Entry<String, URI> versionEntry : osMap.entrySet()) {

      String osFamily = m_family.find(versionEntry.getKey());
      URI uri = versionEntry.getValue();

      // !!! check for aliases.  Moving this to OsFamily could result in incorrect behavior
      if (null == osFamily) {
        String alias = m_family.getAliases().get(versionEntry.getKey());
        if (null != alias) {
          osFamily = m_family.find(alias);
        }
      }

      // !!! if the family is not known OR not part of the stack, skip
      if (null == osFamily || !oses.contains(osFamily)) {
        LOG.info("Stack {} cannot resolve OS {} to the supported ones: {}. Family: {}",
            stackId, versionEntry.getKey(), StringUtils.join(oses, ','), osFamily);
        continue;
      }

      try {
        VersionDefinitionXml xml = timedVDFLoad(uri);

        version = (null == version) ? xml.release.version : version;
        merger.add(version, xml);

      } catch (Exception e) {
        LOG.warn("Could not load version definition for {} identified by {}. {}",
            stackId, uri.toString(), e.getMessage(), e);
      }
    }

    return merger.merge();
  }

  private VersionDefinitionXml timedVDFLoad(URI uri) throws Exception {
    long time = System.currentTimeMillis();

    try {
      return VersionDefinitionXml.load(uri.toURL());
    } finally {
      LOG.debug("Loaded VDF {} in {}ms", uri, System.currentTimeMillis() - time);
    }

  }

}
