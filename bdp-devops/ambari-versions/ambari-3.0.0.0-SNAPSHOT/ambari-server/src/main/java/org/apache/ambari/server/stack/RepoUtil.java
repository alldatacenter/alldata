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
package org.apache.ambari.server.stack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.RepositoryResponse;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

/**
 * Utility functions for repository related tasks.
 */
public class RepoUtil {

  /**
   * logger instance
   */
  private final static Logger LOG = LoggerFactory.getLogger(RepoUtil.class);


  /**
   * repository directory name
   */
  final static String REPOSITORY_FOLDER_NAME = "repos";

  /**
   * repository file name
   */
  final static String REPOSITORY_FILE_NAME = "repoinfo.xml";

  private static final Function<RepoDefinitionEntity, String> REPO_ENTITY_TO_NAME = new Function<RepoDefinitionEntity, String>() {
    @Override
    public String apply(RepoDefinitionEntity input) {
      return input.getRepoName();
    }
  };


  /**
   * Parses the repository file for a stack/service if exists.
   *
   * @param directory stack/service base directory
   * @param subDirs stack/service directory sub directories
   * @param unmarshaller {@link ModuleFileUnmarshaller}, needed to parse repo XML
   * @throws AmbariException if unable to parse the repository file
   * @return The directory containing the repo file and the parsed repo file (if exists)
   */
  public static RepositoryFolderAndXml parseRepoFile(File directory,
                                                 Collection<String> subDirs,
                                                 ModuleFileUnmarshaller unmarshaller) {
    File repositoryFile = null;
    String repoDir = null;
    RepositoryXml repoFile = null;

    if (subDirs.contains(REPOSITORY_FOLDER_NAME)) {
      repoDir = directory.getAbsolutePath() + File.separator + REPOSITORY_FOLDER_NAME;
      repositoryFile = new File(directory.getPath()+ File.separator +
          REPOSITORY_FOLDER_NAME + File.separator + REPOSITORY_FILE_NAME);

      if (repositoryFile.exists()) {
        try {
          repoFile = unmarshaller.unmarshal(RepositoryXml.class, repositoryFile);
        } catch (Exception e) {
          repoFile = new RepositoryXml();
          repoFile.setValid(false);
          String msg = "Unable to parse repo file at location: " +
              repositoryFile.getAbsolutePath();
          repoFile.addError(msg);
          LOG.warn(msg);
        }
      }
    }

    return new RepositoryFolderAndXml(Optional.fromNullable(repoDir), Optional.fromNullable(repoFile));
  }

  /**
   * Checks the passed {@code operatingSystems} parameter if it contains all repositories from the stack model. If a
   *    repository is present in the stack model but missing in the operating system entity list, it is considered a
   *    service repository and will be added.
   * @param operatingSystems - A list of OperatingSystemEntity objects extracted from a RepositoryVersionEntity
   * @param stackReposByOs - Stack repositories loaded from the disk (including service repositories), grouped by os.
   * @return {@code true} if there were added repositories
   */
  @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
    comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
  public static boolean addServiceReposToOperatingSystemEntities(List<RepoOsEntity> operatingSystems,
                                                                 ListMultimap<String, RepositoryInfo> stackReposByOs) {
    Set<String> addedRepos = new HashSet<>();
    for (RepoOsEntity os : operatingSystems) {
      List<RepositoryInfo> serviceReposForOs = stackReposByOs.get(os.getFamily());
      ImmutableSet<String> repoNames = ImmutableSet.copyOf(Lists.transform(os.getRepoDefinitionEntities(), REPO_ENTITY_TO_NAME));
      for (RepositoryInfo repoInfo : serviceReposForOs)
        if (!repoNames.contains(repoInfo.getRepoName())) {
          os.addRepoDefinition(toRepositoryEntity(repoInfo));
          addedRepos.add(String.format("%s (%s)", repoInfo.getRepoId(), os.getFamily()));
        }
    }
    LOG.info("Added {} service repos: {}", addedRepos.size(),Iterables.toString(addedRepos));

    return CollectionUtils.isNotEmpty(addedRepos);
  }

  /**
   * Given a list of VDF repositorie and stack repositories (grouped by os) returns the service repositories.
   * A repository is considered a service repo if present in the stack model but missing in the VDF (check is performed
   * by repository name, per operating system).
   * @param vdfRepos the repositories coming from a version definition
   * @param stackReposByOs the repositories in the stack model (loaded from disks)
   * @return A list of service repositories
   */
  @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
    comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
  public static List<RepositoryInfo> getServiceRepos(List<RepositoryInfo> vdfRepos,
                                                   ListMultimap<String, RepositoryInfo> stackReposByOs) {
    Set<String> serviceRepoIds = new HashSet<>();
    List<RepositoryInfo> serviceRepos = new ArrayList<>();
    ListMultimap<String, RepositoryInfo> vdfReposByOs = Multimaps.index(vdfRepos, RepositoryInfo.GET_OSTYPE_FUNCTION);
    for(String os: vdfReposByOs.keySet()) {
      Set<String> vdfRepoNames = Sets.newHashSet(
          Lists.transform(vdfReposByOs.get(os), RepositoryInfo.GET_REPO_NAME_FUNCTION));
      for (RepositoryInfo repo: stackReposByOs.get(os)) {
        if (!vdfRepoNames.contains(repo.getRepoName())) {
          serviceRepos.add(repo);
          serviceRepoIds.add(repo.getRepoId());
        }
      }
    }
    LOG.debug("Found {} service repos: {}", serviceRepoIds.size(),Iterables.toString(serviceRepoIds));
    return serviceRepos;
  }

  /**
   * Convert a list of {@link RepositoryInfo} objects to a lost of {@link RepositoryResponse} objects
   * @param repositoryInfos the list of repository infos
   * @param versionDefinitionId the version definition id
   * @param stackName the stack name
   * @param stackVersion the stack version
   * @return a list of repository responses
   */
  public static List<RepositoryResponse> asResponses(List<RepositoryInfo> repositoryInfos,
                                                     @Nullable String versionDefinitionId,
                                                     @Nullable String stackName,
                                                     @Nullable String stackVersion) {
    List<RepositoryResponse> responses = new ArrayList<>(repositoryInfos.size());
    for (RepositoryInfo repoInfo: repositoryInfos) {
      RepositoryResponse response = repoInfo.convertToResponse();
      response.setVersionDefinitionId(versionDefinitionId);
      response.setStackName(stackName);
      response.setStackVersion(stackVersion);
      responses.add(response);
    }
    return responses;
  }

  @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
    comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
  private static RepoDefinitionEntity toRepositoryEntity(RepositoryInfo repoInfo) {
    RepoDefinitionEntity re = new RepoDefinitionEntity();
    re.setBaseUrl(repoInfo.getBaseUrl());
    re.setRepoName(repoInfo.getRepoName());
    re.setRepoID(repoInfo.getRepoId());
    re.setDistribution(repoInfo.getDistribution());
    re.setComponents(repoInfo.getComponents());
    re.setTags(repoInfo.getTags());
    re.setApplicableServices(repoInfo.getApplicableServices());
    return re;
  }

}

/**
 * Value class for a pair of repository folder and parsed repository XML.
 */
class RepositoryFolderAndXml {
    final Optional<String> repoDir;
    final Optional<RepositoryXml> repoXml;

    /**
     * @param repoDir Path to the repository directory (optional)
     * @param repoXml Parsed repository XML (optional)
     */
    public RepositoryFolderAndXml(Optional<String> repoDir, Optional<RepositoryXml> repoXml) {
      this.repoDir = repoDir;
      this.repoXml = repoXml;
    }
}
