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

import java.util.List;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;


/**
 * This class should be instantiated on server startup and its {@link #process()} method invoked.
 * The class is part of management pack support. Management packs can contain services which define
 * their own (yum/apt/ect) repositories. If a management pack is installed on an Ambari with an existing
 * cluster, the cluster's repository version entity must be updated with the custom repos provided by the
 * management pack. The class takes care of this.
 */
@Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
  comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
public class UpdateActiveRepoVersionOnStartup {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateActiveRepoVersionOnStartup.class);

  ClusterDAO clusterDao;
  RepositoryVersionDAO repositoryVersionDao;
  RepositoryVersionHelper repositoryVersionHelper;
  StackManager stackManager;

  @Inject
  public UpdateActiveRepoVersionOnStartup(ClusterDAO clusterDao,
      RepositoryVersionDAO repositoryVersionDao,
      RepositoryVersionHelper repositoryVersionHelper,
      AmbariMetaInfo metaInfo) {
    this.clusterDao = clusterDao;
    this.repositoryVersionDao = repositoryVersionDao;
    this.repositoryVersionHelper = repositoryVersionHelper;
    this.stackManager = metaInfo.getStackManager();
  }

  /**
   * Updates the active {@link RepositoryVersionEntity} for clusters with add-on services defined in management packs.
   * @throws AmbariException
   */
  @Transactional
  public void process() throws AmbariException {
    LOG.info("Updating existing repo versions with service repos.");

    try {

      List<ClusterEntity> clusters = clusterDao.findAll();
      for (ClusterEntity cluster: clusters) {
        for (ClusterServiceEntity service : cluster.getClusterServiceEntities()) {
          RepositoryVersionEntity repositoryVersion = service.getServiceDesiredStateEntity().getDesiredRepositoryVersion();

          StackId stackId = repositoryVersion.getStackId();
          StackInfo stack = stackManager.getStack(stackId.getStackName(), stackId.getStackVersion());
        
          if (stack !=null) {
            if (updateRepoVersion(stack, repositoryVersion)) {
              repositoryVersionDao.merge(repositoryVersion);
            }
          } else {
	    throw new AmbariException(String.format("Stack %s %s was not found on the file system. In the event that it was removed, " +
              "please ensure that it exists before starting Ambari Server.",  stackId.getStackName(), stackId.getStackVersion()));
          } 
        }
      }
    }
    catch(Exception ex) {
      throw new AmbariException(
          "An error occured during updating current repository versions with stack repositories.",
          ex);
    }
  }

  private boolean updateRepoVersion(StackInfo stackInfo, RepositoryVersionEntity repoVersion) throws Exception {
    ListMultimap<String, RepositoryInfo> serviceReposByOs = stackInfo.getRepositoriesByOs();

    List<RepoOsEntity> operatingSystems = repoVersion.getRepoOsEntities();
    boolean changed = RepoUtil.addServiceReposToOperatingSystemEntities(operatingSystems, serviceReposByOs);
    if (changed) {
      repoVersion.addRepoOsEntities(operatingSystems);
    }
    return changed;
  }

}
