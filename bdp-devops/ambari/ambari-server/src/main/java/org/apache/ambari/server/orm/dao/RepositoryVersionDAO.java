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
package org.apache.ambari.server.orm.dao;

import java.text.MessageFormat;
import java.util.List;

import javax.persistence.TypedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.spi.RepositoryType;

import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * DAO for repository versions.
 *
 */
@Singleton
public class RepositoryVersionDAO extends CrudDAO<RepositoryVersionEntity, Long> {
  /**
   * Constructor.
   */
  public RepositoryVersionDAO() {
    super(RepositoryVersionEntity.class);
  }


  /**
   * Creates entity.
   *
   * @param entity entity to create
   */
  @Override
  @Transactional
  public void create(RepositoryVersionEntity entity){
    super.create(entity);
  }

  /**
   * Retrieves repository version by name.
   *
   * @param displayName display name
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  public RepositoryVersionEntity findByDisplayName(String displayName) {
    // TODO, this assumes that the display name is unique, but neither the code nor the DB schema enforces this.
    final TypedQuery<RepositoryVersionEntity> query = entityManagerProvider.get().createNamedQuery("repositoryVersionByDisplayName", RepositoryVersionEntity.class);
    query.setParameter("displayname", displayName);
    return daoUtils.selectSingle(query);
  }

  /**
   * Retrieves repository version by stack.
   *
   * @param stackId
   *          stackId
   * @param version
   *          version
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  public RepositoryVersionEntity findByStackAndVersion(StackId stackId,
      String version) {
    return findByStackNameAndVersion(stackId.getStackName(), version);
  }

  /**
   * Retrieves repository version by stack.
   *
   * @param stackEntity Stack entity
   * @param version version
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  public RepositoryVersionEntity findByStackAndVersion(StackEntity stackEntity,
      String version) {
    return findByStackNameAndVersion(stackEntity.getStackName(), version);
  }

  /**
   * Retrieves repository version, which is unique in this stack.
   *
   * @param stackName Stack name such as HDP, HDPWIN, BIGTOP
   * @param version version
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  public RepositoryVersionEntity findByStackNameAndVersion(String stackName, String version) {
    // TODO, need to make a unique composite key in DB using the repo_version's stack_id and version.
    // Ideally, 1.0-1234 foo should be unique in all HDP stacks.
    // The composite key is slightly more relaxed since it would only prevent 2.3.0-1234 multiple times in the HDP 2.3 stack.
    // There's already business logic to prevent creating 2.3-1234 in the wrong stack such as HDP 2.2.
    final TypedQuery<RepositoryVersionEntity> query = entityManagerProvider.get().createNamedQuery("repositoryVersionByStackNameAndVersion", RepositoryVersionEntity.class);
    query.setParameter("stackName", stackName);
    query.setParameter("version", version);
    return daoUtils.selectSingle(query);
  }

  /**
   * Retrieves repository version by stack.
   *
   * @param stackId stack id
   *          stack with major version (like HDP-2.2)
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  public List<RepositoryVersionEntity> findByStack(StackId stackId) {
    final TypedQuery<RepositoryVersionEntity> query = entityManagerProvider.get().createNamedQuery("repositoryVersionByStack", RepositoryVersionEntity.class);
    query.setParameter("stackName", stackId.getStackName());
    query.setParameter("stackVersion", stackId.getStackVersion());
    return daoUtils.selectList(query);
  }

  /**
   * Retrieves repository version by stack.
   *
   * @param stackId
   *          stack id stack with major version (like HDP-2.2)
   * @param type
   *          the repository type
   *
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  public List<RepositoryVersionEntity> findByStackAndType(StackId stackId, RepositoryType type) {
    final TypedQuery<RepositoryVersionEntity> query = entityManagerProvider.get().createNamedQuery(
        "repositoryVersionByStackAndType", RepositoryVersionEntity.class);
    query.setParameter("stackName", stackId.getStackName());
    query.setParameter("stackVersion", stackId.getStackVersion());
    query.setParameter("type", type);
    return daoUtils.selectList(query);
  }

  /**
   * Validates and creates an object.
   * The version must be unique within this stack name (e.g., HDP, HDPWIN, BIGTOP).
   * @param stackEntity Stack entity.
   * @param version Stack version, e.g., 2.2 or 2.2.0.1-885
   * @param displayName Unique display name
   * @param repoOsEntities structure of repository URLs for each OS
   * @return Returns the object created if successful, and throws an exception otherwise.
   * @throws AmbariException
   */
  public RepositoryVersionEntity create(StackEntity stackEntity,
                                        String version, String displayName,
                                        List<RepoOsEntity> repoOsEntities) throws AmbariException {
    return create(stackEntity, version, displayName, repoOsEntities,
          RepositoryType.STANDARD);
  }

  /**
   * Validates and creates an object.
   * The version must be unique within this stack name (e.g., HDP, HDPWIN, BIGTOP).
   * @param stackEntity Stack entity.
   * @param version Stack version, e.g., 2.2 or 2.2.0.1-885
   * @param displayName Unique display name
   * @param repoOsEntities structure of repository URLs for each OS
   * @param type  the repository type
   * @return Returns the object created if successful, and throws an exception otherwise.
   * @throws AmbariException
   */
  @Transactional
  public RepositoryVersionEntity create(StackEntity stackEntity,
                                        String version, String displayName, List<RepoOsEntity> repoOsEntities,
                                        RepositoryType type) throws AmbariException {

    if (stackEntity == null || version == null || version.isEmpty()
        || displayName == null || displayName.isEmpty()) {
      throw new AmbariException("At least one of the required properties is null or empty");
    }

    RepositoryVersionEntity existingByDisplayName = findByDisplayName(displayName);

    if (existingByDisplayName != null) {
      throw new AmbariException("Repository version with display name '" + displayName + "' already exists");
    }

    RepositoryVersionEntity existingVersionInStack = findByStackNameAndVersion(stackEntity.getStackName(), version);

    if (existingVersionInStack != null) {
      throw new AmbariException(MessageFormat.format("Repository Version for version {0} already exists, in stack {1}-{2}",
          version, existingVersionInStack.getStack().getStackName(), existingVersionInStack.getStack().getStackVersion()));
    }


    StackId stackId = new StackId(stackEntity.getStackName(), stackEntity.getStackVersion() );
    if (!RepositoryVersionEntity.isVersionInStack(stackId, version)) {
      throw new AmbariException(MessageFormat.format("Version {0} needs to belong to stack {1}", version, stackEntity.getStackName() + "-" + stackEntity.getStackVersion()));
    }

    RepositoryVersionEntity newEntity = new RepositoryVersionEntity(
        stackEntity, version, displayName, repoOsEntities);
    newEntity.setType(type);
    this.create(newEntity);
    return newEntity;
  }

  /**
   * Retrieves repository version when they are loaded by a version definition
   * file. This will not return all repositories - it will only return those
   * which have a non-NULL VDF.
   *
   * @return a list of repositories created by VDF, or an empty list when there
   *         are none.
   */
  @RequiresSession
  public List<RepositoryVersionEntity> findRepositoriesWithVersionDefinitions() {
    final TypedQuery<RepositoryVersionEntity> query = entityManagerProvider.get().createNamedQuery(
        "repositoryVersionsFromDefinition", RepositoryVersionEntity.class);
    return daoUtils.selectList(query);
  }

  /**
   * @param repositoryVersion
   * @return
   */
  @RequiresSession
  public RepositoryVersionEntity findByVersion(String repositoryVersion) {
    TypedQuery<RepositoryVersionEntity> query = entityManagerProvider.get().createNamedQuery("repositoryVersionByVersion", RepositoryVersionEntity.class);

    query.setParameter("version", repositoryVersion);

    return daoUtils.selectOne(query);
  }

  /**
   * Retrieves the repo versions matching the provided ones that are currently being used
   * for a service.
   *
   * @param matching  the list of repo versions
   */
  @RequiresSession
  public List<RepositoryVersionEntity> findByServiceDesiredVersion(List<RepositoryVersionEntity> matching) {
    TypedQuery<RepositoryVersionEntity> query = entityManagerProvider.get().
            createNamedQuery("findByServiceDesiredVersion", RepositoryVersionEntity.class);

    return daoUtils.selectList(query, matching);
  }
   /**
   * Removes the specified repoversion entry based on stackid.
   *
   * @param stackId
   *
   */
  @Transactional
  public void removeByStack(StackId stackId) {
    List<RepositoryVersionEntity> repoVersionDeleteCandidates = findByStack(stackId);
    for(RepositoryVersionEntity repositoryVersionEntity : repoVersionDeleteCandidates) {
      entityManagerProvider.get().remove(repositoryVersionEntity);
    }
  }
}
