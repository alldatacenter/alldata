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
package org.apache.ambari.server.stack.upgrade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.CommandRepository;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ActionExecutionContext;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.OperatingSystemResourceProvider;
import org.apache.ambari.server.controller.internal.RepositoryResourceProvider;
import org.apache.ambari.server.controller.internal.RepositoryVersionResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.state.stack.RepoTag;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;


/**
 * Provides helper methods to manage repository versions.
 */
@Singleton
public class RepositoryVersionHelper {

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryVersionHelper.class);

  @Inject
  private Gson gson;

  @Inject
  private Provider<AmbariMetaInfo> ami;

  @Inject
  private Provider<Configuration> configuration;

  @Inject
  private Provider<OsFamily> os_family;

  @Inject Provider<Clusters> clusters;


  /**
   * Checks repo URLs against the current version for the cluster and make
   * adjustments to the Base URL when the current is different.
   *
   * @param cluster {@link Cluster} object
   * @param component resolve {@link RepositoryVersionEntity} for the component, could be {@code null}
   *
   * @return {@link RepositoryVersionEntity} retrieved for component if set or cluster if not
   */
  @Experimental(feature = ExperimentalFeature.PATCH_UPGRADES)
  private RepositoryVersionEntity getRepositoryVersionEntity(Cluster cluster, ServiceComponent component) throws SystemException {

    RepositoryVersionEntity repositoryEntity = null;

    // !!! try to find the component repo first
    if (null != component) {
      repositoryEntity = component.getDesiredRepositoryVersion();
    } else {
      LOG.info("Service component not passed in, attempt to resolve the repository for cluster {}",
        cluster.getClusterName());
    }

    if (null == repositoryEntity && null != component) {
      try {
        Service service = cluster.getService(component.getServiceName());
        repositoryEntity = service.getDesiredRepositoryVersion();
      } catch (AmbariException e) {
        throw new SystemException("Unhandled exception", e);
      }
    }

    if (null == repositoryEntity) {
      LOG.info("Cluster {} has no specific Repository Versions.  Using stack-defined values", cluster.getClusterName());
      return null;
    }

    return repositoryEntity;
  }

  /**
   * Parses operating systems json to a list of entities. Expects json like:
   * <pre>
   * [
   *    {
   *       "repositories":[
   *          {
   *             "Repositories/base_url":"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.2.0.0",
   *             "Repositories/repo_name":"HDP-UTILS",
   *             "Repositories/repo_id":"HDP-UTILS-1.1.0.20"
   *          },
   *          {
   *             "Repositories/base_url":"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.2.0.0",
   *             "Repositories/repo_name":"HDP",
   *             "Repositories/repo_id":"HDP-2.2"
   *          }
   *       ],
   *       "OperatingSystems/os_type":"redhat6"
   *    }
   * ]
   * </pre>
   * @param repositoriesJson operating systems json
   * @return list of operating system entities
   * @throws Exception if any kind of json parsing error happened
   */
  public List<RepoOsEntity> parseOperatingSystems(String repositoriesJson) throws Exception {
    final List<RepoOsEntity> operatingSystems = new ArrayList<>();
    final JsonArray rootJson = new JsonParser().parse(repositoriesJson).getAsJsonArray();
    for (JsonElement operatingSystemJson: rootJson) {
      JsonObject osObj = operatingSystemJson.getAsJsonObject();

      final RepoOsEntity operatingSystemEntity = new RepoOsEntity();

      operatingSystemEntity.setFamily(osObj.get(OperatingSystemResourceProvider.OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID).getAsString());

      if (osObj.has(OperatingSystemResourceProvider.OPERATING_SYSTEM_AMBARI_MANAGED_REPOS)) {
        operatingSystemEntity.setAmbariManaged(osObj.get(
            OperatingSystemResourceProvider.OPERATING_SYSTEM_AMBARI_MANAGED_REPOS).getAsBoolean());
      }

      for (JsonElement repositoryElement: osObj.get(RepositoryVersionResourceProvider.SUBRESOURCE_REPOSITORIES_PROPERTY_ID).getAsJsonArray()) {
        final RepoDefinitionEntity repositoryEntity = new RepoDefinitionEntity();
        final JsonObject repositoryJson = repositoryElement.getAsJsonObject();
        repositoryEntity.setBaseUrl(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID).getAsString());
        repositoryEntity.setRepoName(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID).getAsString());
        repositoryEntity.setRepoID(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID).getAsString());
        if (repositoryJson.get(RepositoryResourceProvider.REPOSITORY_DISTRIBUTION_PROPERTY_ID) != null) {
          repositoryEntity.setDistribution(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_DISTRIBUTION_PROPERTY_ID).getAsString());
        }
        if (repositoryJson.get(RepositoryResourceProvider.REPOSITORY_COMPONENTS_PROPERTY_ID) != null) {
          repositoryEntity.setComponents(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_COMPONENTS_PROPERTY_ID).getAsString());
        }
        if (repositoryJson.get(RepositoryResourceProvider.REPOSITORY_MIRRORS_LIST_PROPERTY_ID) != null) {
          repositoryEntity.setMirrors(repositoryJson.get(RepositoryResourceProvider.REPOSITORY_MIRRORS_LIST_PROPERTY_ID).getAsString());
        }
        if (repositoryJson.getAsJsonObject().get(RepositoryResourceProvider.REPOSITORY_UNIQUE_PROPERTY_ID) != null) {
          repositoryEntity.setUnique(repositoryJson.getAsJsonObject().get(RepositoryResourceProvider.REPOSITORY_UNIQUE_PROPERTY_ID).getAsBoolean());
        }
        if (repositoryJson.get(RepositoryResourceProvider.REPOSITORY_APPLICABLE_SERVICES_PROPERTY_ID) != null) {
          List<String> applicableServices = new LinkedList<>();
          JsonArray jsonArray = repositoryJson.get(RepositoryResourceProvider.REPOSITORY_APPLICABLE_SERVICES_PROPERTY_ID).getAsJsonArray();
          for (JsonElement je : jsonArray) {
            applicableServices.add(je.getAsString());
          }
          repositoryEntity.setApplicableServices(applicableServices);
        }

        if (null != repositoryJson.get(RepositoryResourceProvider.REPOSITORY_TAGS_PROPERTY_ID)) {
          Set<RepoTag> tags = new HashSet<>();

          JsonArray jsonArray = repositoryJson.get(RepositoryResourceProvider.REPOSITORY_TAGS_PROPERTY_ID).getAsJsonArray();
          for(JsonElement je : jsonArray) {
            tags.add(RepoTag.valueOf(je.getAsString()));
          }
          repositoryEntity.setTags(tags);
        }

        operatingSystemEntity.addRepoDefinition(repositoryEntity);
      }
      operatingSystems.add(operatingSystemEntity);
    }
    return operatingSystems;
  }

  public List<RepoOsEntity> createRepoOsEntities(List<RepositoryInfo> repositories) {

    List<RepoOsEntity> repoOsEntities = new ArrayList<>();
    final Multimap<String, RepositoryInfo> operatingSystems = ArrayListMultimap.create();
    for (RepositoryInfo repository : repositories) {
      operatingSystems.put(repository.getOsType(), repository);
    }
    for (Entry<String, Collection<RepositoryInfo>> operatingSystem : operatingSystems.asMap().entrySet()) {
      RepoOsEntity operatingSystemEntity = new RepoOsEntity();
      List<RepoDefinitionEntity> repositoriesList = new ArrayList<>();
      for (RepositoryInfo repository : operatingSystem.getValue()) {
        RepoDefinitionEntity repositoryDefinition = new RepoDefinitionEntity();
        repositoryDefinition.setBaseUrl(repository.getBaseUrl());
        repositoryDefinition.setRepoName(repository.getRepoName());
        repositoryDefinition.setRepoID(repository.getRepoId());
        repositoryDefinition.setDistribution(repository.getDistribution());
        repositoryDefinition.setComponents(repository.getComponents());
        repositoryDefinition.setMirrors(repository.getMirrorsList());
        repositoryDefinition.setUnique(repository.isUnique());

        repositoryDefinition.setTags(repository.getTags());
        repositoryDefinition.setApplicableServices(repository.getApplicableServices());

        repositoriesList.add(repositoryDefinition);
        operatingSystemEntity.setAmbariManaged(repository.isAmbariManagedRepositories());
      }
      operatingSystemEntity.addRepoDefinitionEntities(repositoriesList);
      operatingSystemEntity.setFamily(operatingSystem.getKey());
      repoOsEntities.add(operatingSystemEntity);
    }
    return repoOsEntities;
  }

  /**
   * Scans the given stack for upgrade packages which can be applied to update the cluster to given repository version.
   *
   * @param stackName stack name
   * @param stackVersion stack version
   * @param repositoryVersion target repository version
   * @param upgradeType if not {@code null} null, will only return upgrade packs whose type matches.
   * @return upgrade pack name
   * @throws AmbariException if no upgrade packs suit the requirements
   */
  public String getUpgradePackageName(String stackName, String stackVersion, String repositoryVersion, UpgradeType upgradeType) throws AmbariException {
    final Map<String, UpgradePack> upgradePacks = ami.get().getUpgradePacks(stackName, stackVersion);
    for (UpgradePack upgradePack : upgradePacks.values()) {
      final String upgradePackName = upgradePack.getName();

      if (null != upgradeType && upgradePack.getType() != upgradeType) {
        continue;
      }

      // check that upgrade pack has <target> node
      if (StringUtils.isBlank(upgradePack.getTarget())) {
        LOG.error("Upgrade pack " + upgradePackName + " is corrupted, it should contain <target> node");
        continue;
      }
      if (upgradePack.canBeApplied(repositoryVersion)) {
        return upgradePackName;
      }
    }
    throw new AmbariException("There were no suitable upgrade packs for stack " + stackName + " " + stackVersion +
        ((null != upgradeType) ? " and upgrade type " + upgradeType : ""));
  }

  /**
   * Build the role parameters for an install command.
   *
   * @param amc           the management controller.  Tests don't use the same instance that gets injected.
   * @param repoVersion   the repository version
   * @param osFamily      the os family
   * @param servicesOnHost the set of services to check for packages
   * @return a Map<String, String> to use in
   */
  public Map<String, String> buildRoleParams(AmbariManagementController amc, RepositoryVersionEntity repoVersion, String osFamily, Set<String> servicesOnHost)
    throws SystemException {

    StackId stackId = repoVersion.getStackId();

    List<ServiceOsSpecific.Package> packages = new ArrayList<>();

    for (String serviceName : servicesOnHost) {
      ServiceInfo info;

      try {
        if (ami.get().isServiceRemovedInStack(stackId.getStackName(), stackId.getStackVersion(), serviceName)) {
          LOG.info(String.format("%s has been removed from stack %s-%s. Skip calculating its installation packages", stackId.getStackName(), stackId.getStackVersion(), serviceName));
          continue; //No need to calculate install packages for removed services
        }

        info = ami.get().getService(stackId.getStackName(), stackId.getStackVersion(), serviceName);
      } catch (AmbariException e) {
        throw new SystemException(String.format("Cannot obtain stack information for %s-%s", stackId.getStackName(), stackId.getStackVersion()), e);
      }

      List<ServiceOsSpecific.Package> packagesForService = amc.getPackagesForServiceHost(info,
        new HashMap<>(), osFamily);

      List<String> blacklistedPackagePrefixes = configuration.get().getRollingUpgradeSkipPackagesPrefixes();

      for (ServiceOsSpecific.Package aPackage : packagesForService) {
        if (!aPackage.getSkipUpgrade()) {
          boolean blacklisted = false;
          for (String prefix : blacklistedPackagePrefixes) {
            if (aPackage.getName().startsWith(prefix)) {
              blacklisted = true;
              break;
            }
          }
          if (! blacklisted) {
            packages.add(aPackage);
          }
        }
      }
    }

    Map<String, String> roleParams = new HashMap<>();
    roleParams.put("stack_id", stackId.getStackId());
    // !!! TODO make roleParams <String, Object> so we don't have to do this awfulness.
    roleParams.put(KeyNames.PACKAGE_LIST, gson.toJson(packages));

    return roleParams;
  }


  /**
   * Return repositories available for target os version on host based on {@code repoVersion} repository definition
   * @param host target {@link Host} for providing repositories list
   * @param repoVersion {@link RepositoryVersionEntity} version definition with all available repositories
   *
   * @return {@link RepoOsEntity} with available repositories for host
   * @throws SystemException if no repository available for target {@link Host}
   */
  public RepoOsEntity getOSEntityForHost(Host host, RepositoryVersionEntity repoVersion) throws SystemException {
    String osFamily = host.getOsFamily();
    RepoOsEntity osEntity = null;
    for (RepoOsEntity operatingSystem : repoVersion.getRepoOsEntities()) {
      if (osFamily.equals(operatingSystem.getFamily())) {
        osEntity = operatingSystem;
        break;
      }
    }

    if (null == osEntity) {
      throw new SystemException(String.format("Operating System matching %s could not be found",
        osFamily));
    }

    return osEntity;
  }

  /**
   * Adds a command repository to the action context
   * @param osEntity      the OS family
   */
  public CommandRepository getCommandRepository(final RepositoryVersionEntity repoVersion,
                                                final RepoOsEntity osEntity) throws SystemException {

    final CommandRepository commandRepo = new CommandRepository();
    final boolean sysPreppedHost = configuration.get().areHostsSysPrepped().equalsIgnoreCase("true");

    if (null == repoVersion) {
      throw new SystemException("Repository version entity is not provided");
    }

    commandRepo.setRepositories(osEntity.getFamily(), osEntity.getRepoDefinitionEntities());
    commandRepo.setRepoVersion(repoVersion.getVersion());
    commandRepo.setRepositoryVersionId(repoVersion.getId());
    commandRepo.setResolved(repoVersion.isResolved());
    commandRepo.setStackName(repoVersion.getStackId().getStackName());
    commandRepo.getFeature().setPreInstalled(configuration.get().areHostsSysPrepped());
    commandRepo.getFeature().setIsScoped(!sysPreppedHost);

    if (!osEntity.isAmbariManaged()) {
      commandRepo.setNonManaged();
    } else {
      if (repoVersion.isLegacy()){
        commandRepo.setLegacyRepoFileName(repoVersion.getStackName(), repoVersion.getVersion());
        commandRepo.setLegacyRepoId(repoVersion.getVersion());
        commandRepo.getFeature().setIsScoped(false);
      } else {
        commandRepo.setRepoFileName(repoVersion.getStackName(), repoVersion.getId());
        commandRepo.setUniqueSuffix(String.format("-repo-%s", repoVersion.getId()));
      }
    }

    if (configuration.get().arePackagesLegacyOverridden()) {
      LOG.warn("Legacy override option is turned on, disabling CommandRepositoryFeature.scoped feature");
      commandRepo.getFeature().setIsScoped(false);
    }
    return commandRepo;
  }


  /**
   * Builds repository information for inclusion in a command.  This replaces escaping json on
   * a command.
   *
   * @param cluster the cluster
   * @param host    the host
   * @param component {@link ServiceComponent} object, could be null to return service-related repository
   * @return  the command repository
   * @throws SystemException
   */
  @Experimental(feature=ExperimentalFeature.PATCH_UPGRADES)
  public CommandRepository getCommandRepository(final Cluster cluster, ServiceComponent component, final Host host)
    throws SystemException {

    RepositoryVersionEntity repoVersion = getRepositoryVersionEntity(cluster, component);
    RepoOsEntity osEntity = getOSEntityForHost(host, repoVersion);

    return getCommandRepository(repoVersion, osEntity);
  }

  @Experimental(feature=ExperimentalFeature.PATCH_UPGRADES)
  public CommandRepository getCommandRepository(final Cluster cluster, final Service service, final Host host,
                                                final String componentName) throws AmbariException {
    CommandRepository commandRepository = null;
    try {
      if (null != componentName) {
        ServiceComponent serviceComponent = service.getServiceComponent(componentName);
        commandRepository = getCommandRepository(cluster, serviceComponent, host);
      } else {
        RepositoryVersionEntity repoVersion = service.getDesiredRepositoryVersion();
        RepoOsEntity osEntity = getOSEntityForHost(host, repoVersion);
        commandRepository = getCommandRepository(repoVersion, osEntity);
      }
    } catch (SystemException e){
      LOG.debug("Unable to find command repository with a correct operating system for host {}",
        host, e);
    }

    return commandRepository;
  }


  /**
   * This method builds and adds repo infoto hostLevelParams of action
   *
   * @param cluster cluster to which host level params belongs
   * @param actionContext context of the action. Must be not {@code null}
   * @param repositoryVersion repository version entity to use
   * @param hostLevelParams hasgmap with host level params. Must be not {@code null}
   * @param hostName host name to which add repo onfo
   * @throws AmbariException
   */
  @Deprecated
  public void addRepoInfoToHostLevelParams(final Cluster cluster, final ActionExecutionContext actionContext,
                                           final RepositoryVersionEntity repositoryVersion, final Map<String, String> hostLevelParams,
                                           final String hostName) throws AmbariException {

    // if the repo is null, see if any values from the context should go on the
    // host params and then return
    if (null == repositoryVersion) {
      // see if the action context has a repository set to use for the command
      if (null != actionContext.getStackId()) {
        StackId stackId = actionContext.getStackId();
        hostLevelParams.put(KeyNames.STACK_NAME, stackId.getStackName());
        hostLevelParams.put(KeyNames.STACK_VERSION, stackId.getStackVersion());
      }

      return;
    } else {
      StackId stackId = repositoryVersion.getStackId();
      hostLevelParams.put(KeyNames.STACK_NAME, stackId.getStackName());
      hostLevelParams.put(KeyNames.STACK_VERSION, stackId.getStackVersion());
    }
  }


  /**
   * Get repository info given a cluster and host.
   *
   * @param cluster  the cluster
   * @param host     the host
   *
   * @return the repo info
   *
   * @deprecated use {@link #getCommandRepository(Cluster, ServiceComponent, Host)} instead.
   * @throws SystemException if the repository information can not be obtained
   */
  @Deprecated
  public String getRepoInfo(Cluster cluster, ServiceComponent component, Host host) throws SystemException {
    final JsonArray jsonList = getBaseUrls(cluster, component, host);
    final RepositoryVersionEntity rve = getRepositoryVersionEntity(cluster, component);

    if (null == rve || null == jsonList) {
      return "";
    }

    final JsonArray result = new JsonArray();

    for (JsonElement e : jsonList) {
      JsonObject obj = e.getAsJsonObject();

      String repoId = obj.has("repoId") ? obj.get("repoId").getAsString() : null;
      String repoName = obj.has("repoName") ? obj.get("repoName").getAsString() : null;
      String baseUrl = obj.has("baseUrl") ? obj.get("baseUrl").getAsString() : null;
      String osType = obj.has("osType") ? obj.get("osType").getAsString() : null;

      if (null == repoId || null == baseUrl || null == osType || null == repoName) {
        continue;
      }

      for (RepoOsEntity ose : rve.getRepoOsEntities()) {
        if (ose.getFamily().equals(osType) && ose.isAmbariManaged()) {
          for (RepoDefinitionEntity re : ose.getRepoDefinitionEntities()) {
            if (re.getRepoName().equals(repoName) &&
              !re.getBaseUrl().equals(baseUrl)) {
              obj.addProperty("baseUrl", re.getBaseUrl());
            }
          }
          result.add(e);
        }
      }
    }
    return result.toString();
  }


  /**
   * Executed by two different representations of repos.  When we are comfortable with the new
   * implementation, this may be removed and called inline in {@link #getCommandRepository(Cluster, ServiceComponent, Host)}
   *
   * @param cluster   the cluster to isolate the stack
   * @param component the component
   * @param host      used to resolve the family for the repositories
   * @return JsonArray the type as defined by the supplied {@code function}.
   * @throws SystemException
   */
  @Deprecated
  private JsonArray getBaseUrls(Cluster cluster, ServiceComponent component, Host host) throws SystemException {

    String hostOsType = host.getOsType();
    String hostOsFamily = host.getOsFamily();
    String hostName = host.getHostName();

    StackId stackId = component.getDesiredStackId();
    Map<String, List<RepositoryInfo>> repos;

    try {
      repos = ami.get().getRepository(stackId.getStackName(), stackId.getStackVersion());
    }catch (AmbariException e) {
      throw new SystemException("Unhandled exception", e);
    }

    String family = os_family.get().find(hostOsType);
    if (null == family) {
      family = hostOsFamily;
    }

    final List<RepositoryInfo> repoInfoList;

    // !!! check for the most specific first
    if (repos.containsKey(hostOsType)) {
      repoInfoList = repos.get(hostOsType);
    } else if (null != family && repos.containsKey(family)) {
      repoInfoList = repos.get(family);
    } else {
      repoInfoList = null;
      LOG.warn("Could not retrieve repo information for host"
        + ", hostname=" + hostName
        + ", clusterName=" + cluster.getClusterName()
        + ", stackInfo=" + stackId.getStackId());
    }

    return (null == repoInfoList) ? null : (JsonArray) gson.toJsonTree(repoInfoList);
  }


  /**
   * Adds a command repository to the action context
   * @param context       the context
   * @param osEntity      the OS family
   */
  public void addCommandRepositoryToContext(ActionExecutionContext context,
      final RepositoryVersionEntity repoVersion, RepoOsEntity osEntity) throws SystemException {

    final CommandRepository commandRepo = getCommandRepository(repoVersion, osEntity);

    ClusterVersionSummary summary = null;

    if (RepositoryType.STANDARD != repoVersion.getType()) {
      try {
        final Cluster cluster = clusters.get().getCluster(context.getClusterName());

        VersionDefinitionXml xml = repoVersion.getRepositoryXml();
        summary = xml.getClusterSummary(cluster, ami.get());
      } catch (Exception e) {
        LOG.warn("Could not determine repository from %s/%s.  Will not pass cluster version.");
      }
    }

    final ClusterVersionSummary clusterSummary = summary;

    context.addVisitor(command -> {
      if (null == command.getRepositoryFile()) {
        command.setRepositoryFile(commandRepo);
      }

      if (null != clusterSummary) {
        Map<String, Object> params = command.getRoleParameters();
        if (null == params) {
          params = new HashMap<>();
          command.setRoleParameters(params);
        }
        params.put(KeyNames.CLUSTER_VERSION_SUMMARY, clusterSummary);
      }

    });
  }

  /** Get repository info given a cluster and host.
   *
   * @param cluster  the cluster
   * @param host     the host
   *
   * @return the repo info
   *
   * @throws AmbariException if the repository information can not be obtained
   */
  public String getRepoInfoString(Cluster cluster, ServiceComponent component, Host host) throws AmbariException, SystemException {
    return gson.toJson(getCommandRepository(cluster, component, host));
  }
}
