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
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.stack.StackModule;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.RepoUrlInfoCallable.RepoUrlInfoResult;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Encapsulates the work to resolve the latest repo information for a stack.
 * This class must be used AFTER the stack has created its owned repositories.
 */
public class RepoUrlInfoCallable implements Callable<Map<StackModule, RepoUrlInfoResult>> {
  private static final int LOOKUP_CONNECTION_TIMEOUT = 2000;
  private static final int LOOKUP_READ_TIMEOUT = 3000;

  private final static Logger LOG = LoggerFactory.getLogger(RepoUrlInfoCallable.class);

  private URI m_uri = null;
  private Set<StackModule> m_stacks = new HashSet<>();

  public RepoUrlInfoCallable(URI uri) {
    m_uri = uri;
  }

  public void addStack(StackModule stackModule) {
    m_stacks.add(stackModule);
  }

  @Override
  public Map<StackModule, RepoUrlInfoResult> call() throws Exception {

    Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
    Gson gson = new Gson();

    Map<String, Map<String, Object>> latestUrlMap = null;

    Set<String> ids = new HashSet<>();
    ids.addAll(Collections2.transform(m_stacks, new Function<StackModule, String>() {
      @Override
      public String apply(StackModule input) {
        // TODO Auto-generated method stub
        return new StackId(input.getModuleInfo()).toString();
      }
    }));

    String stackIds = StringUtils.join(ids, ',');

    Long time = System.nanoTime();

    try {
      if (m_uri.getScheme().startsWith("http")) {
        URLStreamProvider streamProvider = new URLStreamProvider(
            LOOKUP_CONNECTION_TIMEOUT, LOOKUP_READ_TIMEOUT,
            null, null, null);

        LOG.info("Loading latest URL info from {} for stacks {}", m_uri, stackIds);

        latestUrlMap = gson.fromJson(new InputStreamReader(
            streamProvider.readFrom(m_uri.toString())), type);
      } else {
        File jsonFile = new File(m_uri);

        if (jsonFile.exists()) {
          LOG.info("Loading latest URL info from file {} for stacks {}", m_uri, stackIds);
          latestUrlMap = gson.fromJson(new FileReader(jsonFile), type);
        }
      }
    } catch (Exception e) {
      LOG.info("Could not load the URI from {}, stack defaults will be used", m_uri);
      throw e;
    } finally {
      LOG.info("Loaded URI {} for stacks {} in {}ms", m_uri, stackIds,
          TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - time));
    }

    Map<StackModule, RepoUrlInfoResult> result = new HashMap<>();

    if (null == latestUrlMap) {
      LOG.error("Could not load latest data for URI {} and stacks {}", m_uri, stackIds);
      return result;
    }

    for (StackModule stackModule : m_stacks) {
      StackId stackId = new StackId(stackModule.getModuleInfo());

      Map<String, Object> map = latestUrlMap.get(stackId.toString());

      if (null == map) {
        continue;
      }

      RepoUrlInfoResult res = new RepoUrlInfoResult();

      if (map.containsKey("manifests")) {
        // versionMap is laid out like [version -> [os -> VDF uri]]
        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> versionMap = (Map<String, Map<String, String>>) map.get("manifests");

        for (Entry<String, Map<String, String>> versionEntry : versionMap.entrySet()) {
          String version = versionEntry.getKey();
          Map<String, URI> resolvedOsMap = resolveOsMap(stackModule, versionEntry.getValue());

          res.addVersion(version, resolvedOsMap);
        }
      }


      if (map.containsKey("latest-vdf")) {
        @SuppressWarnings("unchecked")
        Map<String, String> osMap = (Map<String, String>) map.get("latest-vdf");

        Map<String, URI> resolvedOsMap = resolveOsMap(stackModule, osMap);

        res.setLatest(resolvedOsMap);
      }

      result.put(stackModule, res);
    }


    return result;

  }

  private Map<String, URI> resolveOsMap(StackModule stackModule, Map<String, String> osMap) {

    Map<String, URI> resolved = new HashMap<>();

    for (Entry<String, String> osEntry : osMap.entrySet()) {

      String uriString = osEntry.getValue();

      URI uri = StackModule.getURI(stackModule, uriString);

      if (null == uri) {
        LOG.warn("Could not resolve URI {}", uriString);
      } else {
        resolved.put(osEntry.getKey(), uri);
      }
    }

    return resolved;
  }


  /**
   * Stores the results saved per StackModule
   */
  public static class RepoUrlInfoResult {

    private Map<String, Map<String, URI>> versions = new HashMap<>();
    private Map<String, URI> latestVdf = new HashMap<>();

    private void addVersion(String version, Map<String, URI> vdfMap) {
      versions.put(version, vdfMap);
    }

    private void setLatest(Map<String, URI> latestMap) {
      latestVdf = latestMap;
    }

    /**
     * Each version entry here should be loaded in it's entirety in a new thread
     */
    public Map<String, Map<String, URI>> getManifest() {

      return versions;
    }

    /**
     * @return the latest vdf map
     */
    public Map<String, URI> getLatestVdf() {

      return latestVdf;
    }
  }

}
