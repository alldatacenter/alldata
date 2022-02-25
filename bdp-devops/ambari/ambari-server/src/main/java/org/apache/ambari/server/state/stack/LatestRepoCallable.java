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

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Encapsulates the work to resolve the latest repo information for a stack.
 * This class must be used AFTER the stack has created its owned repositories.
 */
public class LatestRepoCallable implements Callable<Void> {
  private static final int LOOKUP_CONNECTION_TIMEOUT = 2000;
  private static final int LOOKUP_READ_TIMEOUT = 3000;

  private final static Logger LOG = LoggerFactory.getLogger(LatestRepoCallable.class);

  private String sourceUri = null;
  private File stackRepoFolder = null;
  private StackInfo stack = null;
  private OsFamily os_family;

  public LatestRepoCallable(String latestSourceUri, File stackRepoFolder, StackInfo stack, OsFamily os_family) {
    this.sourceUri = latestSourceUri;
    this.stackRepoFolder = stackRepoFolder;
    this.stack = stack;
    this.os_family = os_family;
  }

  @Override
  public Void call() throws Exception {

    Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
    Gson gson = new Gson();

    Map<String, Map<String, Object>> latestUrlMap = null;

    Long time = System.currentTimeMillis();
    try {
      if (sourceUri.startsWith("http")) {

        URLStreamProvider streamProvider = new URLStreamProvider(
            LOOKUP_CONNECTION_TIMEOUT, LOOKUP_READ_TIMEOUT,
            null, null, null);

        LOG.info("Loading latest URL info for stack {}-{} from {}", stack.getName(),
                stack.getVersion(), sourceUri);

        latestUrlMap = gson.fromJson(new InputStreamReader(
            streamProvider.readFrom(sourceUri)), type);
      } else {
        File jsonFile = null;
        if (sourceUri.charAt(0) == '.') {
          jsonFile = new File(stackRepoFolder, sourceUri);
        } else {
          jsonFile = new File(sourceUri);
        }

        if (jsonFile.exists()) {
          LOG.info("Loading latest URL info for stack {}-{} from {}", stack.getName(),
                  stack.getVersion(), jsonFile);
          latestUrlMap = gson.fromJson(new FileReader(jsonFile), type);
        }
      }
    } catch (Exception e) {
      LOG.info("Could not load the URI for stack {}-{} from {}, ({}).  Using default repository values",
          stack.getName(), stack.getVersion(), sourceUri, e.getMessage());
      throw e;
    } finally {
      LOG.info("Loaded uri {} in {}ms", sourceUri, System.currentTimeMillis() - time);
    }

    StackId stackId = new StackId(stack);
    if (latestUrlMap == null || !latestUrlMap.containsKey(stackId.toString())) {
      return null;
    }

    Map<String, Object> map = latestUrlMap.get(stackId.toString());
    if (null == map) {
      return null;
    }

    // !!! use this to prevent double loading of VDF.
    Map<URI, VersionDefinitionXml> parsedMap = new HashMap<>();

    if (map.containsKey("manifests")) {

      @SuppressWarnings("unchecked")
      Map<String, Object> versionMap = (Map<String, Object>) map.get("manifests");

      // EACH VDF is for ONLY ONE repository.  We must provide a merged view.
      // there is no good way around this, so we have to make some concessions

      // !!! each key is a version number, and the value is a map containing
      // os_family -> VDF link

      for (Entry<String, Object> entry : versionMap.entrySet()) {
        String version = entry.getKey();

        @SuppressWarnings("unchecked")
        Map<String, String> osMap = (Map<String, String>) entry.getValue();

        VersionDefinitionXml xml = mergeDefinitions(stackId, version, osMap, parsedMap);

        if (null != xml) {
          stack.addVersionDefinition(version, xml);
        }
      }
    }

    if (map.containsKey("latest-vdf")) {
      // !!! this is an os_family -> VDF link.  It's identical to the version entry in the 'manifests'
      // structure.  This is the source of truth for the default, unspecified version when
      // doing a bluerpint install
      @SuppressWarnings("unchecked")
      Map<String, String> osMap = (Map<String, String>) map.get("latest-vdf");

      VersionDefinitionXml xml = mergeDefinitions(stackId, null, osMap, parsedMap);
      xml.setStackDefault(true);
      stack.setLatestVersionDefinition(xml);
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
      Map<String, String> osMap, Map<URI, VersionDefinitionXml> parsedMap) throws Exception {

    Set<String> oses = new HashSet<>();
    for (RepositoryInfo ri : stack.getRepositories()) {
      if (null != os_family.find(ri.getOsType())) {
        oses.add(os_family.find(ri.getOsType()));
      }
    }

    VersionDefinitionXml.Merger merger = new VersionDefinitionXml.Merger();

    for (Entry<String, String> versionEntry : osMap.entrySet()) {

      String osFamily = os_family.find(versionEntry.getKey());

      // !!! check for aliases.  Moving this to OsFamily could result in incorrect behavior
      if (null == osFamily) {
        String alias = os_family.getAliases().get(versionEntry.getKey());
        if (null != alias) {
          osFamily = os_family.find(alias);
        }
      }

      // !!! if the family is not known OR not part of the stack, skip
      if (null == osFamily || !oses.contains(osFamily)) {
        LOG.info("Stack {} cannot resolve OS {} to the supported ones: {}. Family: {}",
            stackId, versionEntry.getKey(), StringUtils.join(oses, ','), osFamily);
        continue;
      }

      String uriString = versionEntry.getValue();

      if ('.' == uriString.charAt(0)) {
        uriString = new File(stackRepoFolder, uriString).toURI().toString();
      }

      try {
        URI uri = new URI(uriString);

        VersionDefinitionXml xml = parsedMap.containsKey(uri) ? parsedMap.get(uri) :
           timedVDFLoad(uri);

        version = (null == version) ? xml.release.version : version;
        merger.add(version, xml);

        if (!parsedMap.containsKey(uri)) {
          parsedMap.put(uri, xml);
        }

      } catch (Exception e) {
        LOG.warn("Could not load version definition for {} identified by {}. {}",
            stackId, uriString, e.getMessage(), e);
      }
    }

    return merger.merge();
  }

  /**
   * Resolves a base url given that certain OS types can be used interchangeably.
   * @param os the target os to find
   * @param osMap the map of os-to-baseurl
   * @return the url for an os.
   */
  private String resolveOsUrl(String os, Map<String, String> osMap) {

    // !!! look for the OS directly
    if (osMap.containsKey(os))
      return osMap.get(os);

    // !!! os not found, find and return the first compatible one
    Set<String> possibleTypes = os_family.findTypes(os);

    for (String type : possibleTypes) {
      if (osMap.containsKey(type))
        return osMap.get(type);
    }

    return null;
  }

  private VersionDefinitionXml timedVDFLoad(URI uri) throws Exception {
    long time = System.currentTimeMillis();

    try {
      return VersionDefinitionXml.load(uri.toURL());
    } finally {
      LOG.info("Loaded VDF {} in {}ms", uri, System.currentTimeMillis() - time);
    }

  }

}
