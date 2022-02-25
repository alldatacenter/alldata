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
package org.apache.ambari.server.controller.internal;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.resources.OperatingSystemResourceDefinition;
import org.apache.ambari.server.api.resources.RepositoryResourceDefinition;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.stack.RepoUtil;
import org.apache.ambari.server.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.RepoTag;
import org.apache.ambari.spi.RepositoryType;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * The {@link VersionDefinitionResourceProvider} class deals with managing Version Definition
 * files.
 */
@StaticallyInject
public class VersionDefinitionResourceProvider extends AbstractAuthorizedResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(VersionDefinitionResourceProvider.class);

  public static final String VERSION_DEF                             = "VersionDefinition";
  public static final String VERSION_DEF_BASE64_PROPERTY             = "version_base64";
  public static final String VERSION_DEF_STACK_NAME                  = "VersionDefinition/stack_name";
  public static final String VERSION_DEF_STACK_VERSION               = "VersionDefinition/stack_version";

  public static final String VERSION_DEF_ID                       = "VersionDefinition/id";
  protected static final String VERSION_DEF_TYPE_PROPERTY_ID         = "VersionDefinition/type";
  protected static final String VERSION_DEF_DEFINITION_URL           = "VersionDefinition/version_url";
  public static final String VERSION_DEF_AVAILABLE_DEFINITION     = "VersionDefinition/available";
  protected static final String VERSION_DEF_DEFINITION_BASE64        = PropertyHelper.getPropertyId(VERSION_DEF, VERSION_DEF_BASE64_PROPERTY);

  protected static final String VERSION_DEF_FULL_VERSION             = "VersionDefinition/repository_version";
  protected static final String VERSION_DEF_RELEASE_VERSION          = "VersionDefinition/release/version";
  protected static final String VERSION_DEF_RELEASE_BUILD            = "VersionDefinition/release/build";
  protected static final String VERSION_DEF_RELEASE_NOTES            = "VersionDefinition/release/notes";
  protected static final String VERSION_DEF_RELEASE_COMPATIBLE_WITH  = "VersionDefinition/release/compatible_with";
  protected static final String VERSION_DEF_AVAILABLE_SERVICES       = "VersionDefinition/services";
  protected static final String VERSION_DEF_STACK_SERVICES           = "VersionDefinition/stack_services";
  protected static final String VERSION_DEF_STACK_DEFAULT            = "VersionDefinition/stack_default";
  protected static final String VERSION_DEF_STACK_REPO_UPDATE_LINK_EXISTS = "VersionDefinition/stack_repo_update_link_exists";
  protected static final String VERSION_DEF_DISPLAY_NAME             = "VersionDefinition/display_name";
  protected static final String VERSION_DEF_VALIDATION               = "VersionDefinition/validation";
  protected static final String SHOW_AVAILABLE                       = "VersionDefinition/show_available";

  // !!! for convenience, bring over jdk compatibility information from the stack
  protected static final String VERSION_DEF_MIN_JDK                  = "VersionDefinition/min_jdk";
  protected static final String VERSION_DEF_MAX_JDK                  = "VersionDefinition/max_jdk";

  public static final String DIRECTIVE_SKIP_URL_CHECK = "skip_url_check";

  public static final String SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID  = new OperatingSystemResourceDefinition().getPluralName();

  @Inject
  private static RepositoryVersionDAO s_repoVersionDAO;

  @Inject
  private static Provider<AmbariMetaInfo> s_metaInfo;

  @Inject
  private static Provider<RepositoryVersionHelper> s_repoVersionHelper;

  @Inject
  private static StackDAO s_stackDAO;

  @Inject
  private static Configuration s_configuration;

  /**
   * Key property ids
   */
  private static final Set<String> PK_PROPERTY_IDS = Sets.newHashSet(
      VERSION_DEF_ID,
      VERSION_DEF_STACK_NAME,
      VERSION_DEF_STACK_VERSION,
      VERSION_DEF_FULL_VERSION);

  /**
   * The property ids for an version definition resource.
   */
  private static final Set<String> PROPERTY_IDS = Sets.newHashSet(
      VERSION_DEF_ID,
      VERSION_DEF_TYPE_PROPERTY_ID,
      VERSION_DEF_DEFINITION_URL,
      VERSION_DEF_DEFINITION_BASE64,
      VERSION_DEF_AVAILABLE_DEFINITION,
      VERSION_DEF_STACK_NAME,
      VERSION_DEF_STACK_VERSION,
      VERSION_DEF_FULL_VERSION,
      VERSION_DEF_RELEASE_NOTES,
      VERSION_DEF_RELEASE_COMPATIBLE_WITH,
      VERSION_DEF_RELEASE_VERSION,
      VERSION_DEF_RELEASE_BUILD,
      VERSION_DEF_AVAILABLE_SERVICES,
      VERSION_DEF_STACK_SERVICES,
      VERSION_DEF_STACK_DEFAULT,
      VERSION_DEF_STACK_REPO_UPDATE_LINK_EXISTS,
      VERSION_DEF_DISPLAY_NAME,
      VERSION_DEF_VALIDATION,
      VERSION_DEF_MIN_JDK,
      VERSION_DEF_MAX_JDK,
      SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID,
      SHOW_AVAILABLE);

  /**
   * The key property ids for an version definition resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS =
      Collections.singletonMap(Resource.Type.VersionDefinition, VERSION_DEF_ID);

  /**
   * Constructor.
   */
  VersionDefinitionResourceProvider() {
    super(Resource.Type.VersionDefinition, PROPERTY_IDS, KEY_PROPERTY_IDS);

    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS));
    setRequiredGetAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS));
  }

  @Override
  protected RequestStatus createResourcesAuthorized(final Request request)
      throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    Set<Map<String, Object>> requestProperties = request.getProperties();

    if (requestProperties.size() > 1) {
      throw new IllegalArgumentException("Cannot process more than one file per request");
    }

    final Map<String, Object> properties = requestProperties.iterator().next();

    if (!properties.containsKey(VERSION_DEF_DEFINITION_URL) &&
        !properties.containsKey(VERSION_DEF_DEFINITION_BASE64) &&
        !properties.containsKey(VERSION_DEF_AVAILABLE_DEFINITION)) {
      throw new IllegalArgumentException(String.format("Creation method is not known.  %s or %s is required or upload the file directly",
          VERSION_DEF_DEFINITION_URL, VERSION_DEF_AVAILABLE_DEFINITION));
    }

    if (properties.containsKey(VERSION_DEF_DEFINITION_URL) && properties.containsKey(VERSION_DEF_DEFINITION_BASE64)) {
      throw new IllegalArgumentException(String.format("Specify ONLY the url with %s or upload the file directly",
          VERSION_DEF_DEFINITION_URL));
    }

    final String definitionUrl = (String) properties.get(VERSION_DEF_DEFINITION_URL);
    final String definitionBase64 = (String) properties.get(VERSION_DEF_DEFINITION_BASE64);
    final String definitionName = (String) properties.get(VERSION_DEF_AVAILABLE_DEFINITION);
    final Set<String> validations = new HashSet<>();
    final boolean dryRun = request.isDryRunRequest();

    final boolean skipUrlCheck;
    if (null != request.getRequestInfoProperties()) {
      skipUrlCheck = BooleanUtils.toBoolean(request.getRequestInfoProperties().get(DIRECTIVE_SKIP_URL_CHECK));
    } else {
      skipUrlCheck = false;
    }

    XmlHolder xmlHolder = createResources(new Command<XmlHolder>() {
      @Override
      public XmlHolder invoke() throws AmbariException {

        XmlHolder holder = null;
        if (null != definitionUrl) {
          holder = loadXml(definitionUrl);
        } else if (null != definitionBase64) {
          holder = loadXml(Base64.decodeBase64(definitionBase64));
        } else if (null != definitionName) {
          VersionDefinitionXml xml = s_metaInfo.get().getVersionDefinition(definitionName);

          if (null == xml) {
            throw new AmbariException(String.format("Version %s not found", definitionName));
          }

          holder = new XmlHolder();
          holder.xml = xml;
          try {
            holder.xmlString = xml.toXml();
          } catch (Exception e) {
            throw new AmbariException(String.format("The available repository %s does not serialize", definitionName), e);
          }

        } else {
          throw new AmbariException("Cannot determine creation method");
        }

        // !!! must be in this anonymous method because things throw AmbariException

        toRepositoryVersionEntity(holder);

        try {
          RepositoryVersionResourceProvider.validateRepositoryVersion(s_repoVersionDAO,
              s_metaInfo.get(), holder.entity, skipUrlCheck);
        } catch (AmbariException e) {
          if (dryRun) {
            validations.add(e.getMessage());
          } else {
            throw e;
          }
        }

        checkForParent(holder);

        return holder;
      }
    });

    if (StringUtils.isNotBlank(ObjectUtils.toString(properties.get(VERSION_DEF_DISPLAY_NAME)))) {
      xmlHolder.xml.release.display = properties.get(VERSION_DEF_DISPLAY_NAME).toString();
      xmlHolder.entity.setDisplayName(properties.get(VERSION_DEF_DISPLAY_NAME).toString());
      // !!! also set the version string to the name for uniqueness reasons.  this is
      // getting replaced during install packages anyway.  this is just for the entity, the
      // VDF should stay the same.
      xmlHolder.entity.setVersion(properties.get(VERSION_DEF_DISPLAY_NAME).toString());
    }

    if (s_repoVersionDAO.findByDisplayName(xmlHolder.entity.getDisplayName()) != null) {
      String err = String.format("Repository version with name %s already exists.",
          xmlHolder.entity.getDisplayName());

      if (dryRun) {
        validations.add(err);
      } else {
        throw new ResourceAlreadyExistsException(err);
      }
    }

    if (s_repoVersionDAO.findByStackAndVersion(xmlHolder.entity.getStack(), xmlHolder.entity.getVersion()) != null) {
      String err = String.format("Repository version for stack %s and version %s already exists.",
              xmlHolder.entity.getStackId(), xmlHolder.entity.getVersion());

      if (dryRun) {
        validations.add(err);
      } else {
        throw new ResourceAlreadyExistsException(err);
      }
    }

    final Resource res;

    if (dryRun) {
      // !!! dry runs imply that the whole entity should be provided.  this is usually
      // done via sub-resources, but that model breaks down since we don't have a saved
      // entity yet
      Set<String> ids = Sets.newHashSet(
        VERSION_DEF_TYPE_PROPERTY_ID,
        VERSION_DEF_FULL_VERSION,
        VERSION_DEF_RELEASE_BUILD,
        VERSION_DEF_RELEASE_COMPATIBLE_WITH,
        VERSION_DEF_RELEASE_NOTES,
        VERSION_DEF_RELEASE_VERSION,
        VERSION_DEF_DISPLAY_NAME,
        VERSION_DEF_AVAILABLE_SERVICES,
        VERSION_DEF_VALIDATION,
        VERSION_DEF_MIN_JDK,
        VERSION_DEF_MAX_JDK,
        VERSION_DEF_STACK_SERVICES);

      res = toResource(null, xmlHolder.xml, ids, validations);
      // !!! if the definition name is not null, it can only be from available
      if (null != definitionName) {
        res.setProperty(SHOW_AVAILABLE, true);
      }

      // !!! for dry run, make sure the version is the entity display
      setResourceProperty(res, VERSION_DEF_FULL_VERSION, xmlHolder.entity.getVersion(), ids);

      addSubresources(res, xmlHolder.entity);
    } else {

      s_repoVersionDAO.create(xmlHolder.entity);

      res = toResource(xmlHolder.entity, Collections.emptySet());
      notifyCreate(Resource.Type.VersionDefinition, request);
    }

    RequestStatusImpl status = new RequestStatusImpl(null, Collections.singleton(res));

    return status;
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new HashSet<>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    if (propertyMaps.isEmpty()) {
      List<RepositoryVersionEntity> versions = s_repoVersionDAO.findRepositoriesWithVersionDefinitions();

      for (RepositoryVersionEntity entity : versions) {
        results.add(toResource(entity, requestPropertyIds));
      }
    } else {
      for (Map<String, Object> propertyMap : propertyMaps) {

        if (propertyMap.containsKey(SHOW_AVAILABLE) &&
            Boolean.parseBoolean(propertyMap.get(SHOW_AVAILABLE).toString())) {

          for (Entry<String, VersionDefinitionXml> entry : s_metaInfo.get().getVersionDefinitions().entrySet()) {
            Resource res = toResource(entry.getKey(), entry.getValue(), requestPropertyIds, null);
            res.setProperty(SHOW_AVAILABLE, true);
            results.add(res);
          }
        } else {
          String id = (String) propertyMap.get(VERSION_DEF_ID);

          if (null != id) {
            // id is either the repo version id from the db, or it's a phantom id from
            // the stack
            if (NumberUtils.isDigits(id)) {

              RepositoryVersionEntity entity = s_repoVersionDAO.findByPK(Long.parseLong(id));
              if (null != entity) {
                results.add(toResource(entity, requestPropertyIds));
              }
            } else {
              VersionDefinitionXml xml = s_metaInfo.get().getVersionDefinition(id);

              if (null == xml) {
                throw new NoSuchResourceException(String.format("Could not find version %s",
                    id));
              }
              Resource res = toResource(id, xml, requestPropertyIds, null);
              res.setProperty(SHOW_AVAILABLE, true);
              results.add(res);
            }
          } else {
            List<RepositoryVersionEntity> versions = s_repoVersionDAO.findRepositoriesWithVersionDefinitions();

            for (RepositoryVersionEntity entity : versions) {
              results.add(toResource(entity, requestPropertyIds));
            }
          }

        }
      }
    }
    return results;
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(final Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    throw new SystemException("Cannot update Version Definitions");
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Cannot delete Version Definitions");
  }

  /**
   * In the case of a patch, check if there is a parent repo.
   */
  private void checkForParent(XmlHolder holder) throws AmbariException {
    RepositoryVersionEntity entity = holder.entity;

    // only STANDARD types don't have a parent
    if (entity.getType() == RepositoryType.STANDARD) {
      return;
    }

    List<RepositoryVersionEntity> entities = s_repoVersionDAO.findByStackAndType(
        entity.getStackId(), RepositoryType.STANDARD);

    if (entities.isEmpty()) {
      throw new IllegalArgumentException(String.format("Patch %s was uploaded, but there are no repositories for %s",
          entity.getVersion(), entity.getStackId().toString()));
    }

    List<RepositoryVersionEntity> matching = new ArrayList<>();

    boolean emptyCompatible = StringUtils.isBlank(holder.xml.release.compatibleWith);

    for (RepositoryVersionEntity version : entities) {
      String baseVersion = version.getVersion();
      if (baseVersion.lastIndexOf('-') > -1) {
        baseVersion = baseVersion.substring(0,  baseVersion.lastIndexOf('-'));
      }

      if (emptyCompatible) {
        if (baseVersion.equals(holder.xml.release.version)) {
          matching.add(version);
        }
      } else {
        if (baseVersion.matches(holder.xml.release.compatibleWith)) {
          matching.add(version);
        }
      }
    }

    RepositoryVersionEntity parent = null;

    if (matching.isEmpty()) {
      String format = "No versions matched pattern %s";

      throw new IllegalArgumentException(String.format(format,
          emptyCompatible ? holder.xml.release.version : holder.xml.release.compatibleWith));
    } else if (matching.size() > 1) {

      Function<RepositoryVersionEntity, String> function = new Function<RepositoryVersionEntity, String>() {
        @Override
        public String apply(RepositoryVersionEntity input) {
          return input.getVersion();
        }
      };

      Collection<String> versions = Collections2.transform(matching, function);

      List<RepositoryVersionEntity> used = s_repoVersionDAO.findByServiceDesiredVersion(matching);

      if (used.isEmpty()) {
        throw new IllegalArgumentException(String.format("Could not determine which version " +
          "to associate patch %s. Remove one of %s and try again.",
          entity.getVersion(), StringUtils.join(versions, ", ")));
      } else if (used.size() > 1) {
        Collection<String> usedVersions = Collections2.transform(used, function);

        throw new IllegalArgumentException(String.format("Patch %s was found to match more " +
            "than one repository in use: %s. Move all services to a common version and try again.",
            entity.getVersion(), StringUtils.join(usedVersions, ", ")));
      } else {
        parent = used.get(0);
        LOG.warn("Patch {} was found to match more than one repository in {}. " +
            "Repository {} is in use and will be the parent.", entity.getVersion(),
               StringUtils.join(versions, ", "), parent.getVersion());
      }
    } else {
      parent = matching.get(0);
    }

    if (null == parent) {
      throw new IllegalArgumentException(String.format("Could not find any parent repository for %s.",
          entity.getVersion()));
    }

    entity.setParent(parent);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  @Override
  protected ResourceType getResourceType(Request request, Predicate predicate) {
    return ResourceType.AMBARI;
  }

  /**
   * Load the xml data from a posted Base64 stream
   * @param decoded the decoded Base64 data
   * @return the XmlHolder instance
   * @throws AmbariException
   */
  private XmlHolder loadXml(byte[] decoded) {
    XmlHolder holder = new XmlHolder();

    try {
      holder.xmlString = new String(decoded, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      holder.xmlString = new String(decoded);
    }

    try {
      holder.xml = VersionDefinitionXml.load(holder.xmlString);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }

    return holder;
  }

  /**
   * Load the xml data from a url
   * @param definitionUrl
   * @return the XmlHolder instance
   * @throws AmbariException
   */
  private XmlHolder loadXml(String definitionUrl) throws AmbariException {
    XmlHolder holder = new XmlHolder();
    holder.url = definitionUrl;

    int connectTimeout = s_configuration.getVersionDefinitionConnectTimeout();
    int readTimeout = s_configuration.getVersionDefinitionReadTimeout();

    try {
      URI uri = new URI(definitionUrl);
      InputStream stream = null;

      if (uri.getScheme().equalsIgnoreCase("file")) {
        if (!s_configuration.areFileVDFAllowed()) {
          throw new AmbariException("File URL for VDF are not enabled");
        }
        stream = uri.toURL().openStream();
      } else {
        URLStreamProvider provider = new URLStreamProvider(connectTimeout, readTimeout,
            ComponentSSLConfiguration.instance());

        stream = provider.readFrom(definitionUrl);
      }

      holder.xmlString = IOUtils.toString(stream, "UTF-8");
      holder.xml = VersionDefinitionXml.load(holder.xmlString);
    } catch (Exception e) {
      String err = String.format("Could not load url from %s.  %s",
          definitionUrl, e.getMessage());
      throw new AmbariException(err, e);
    }

    return holder;
  }

  /**
   * Transforms a XML version defintion to an entity
   *
   * @throws AmbariException if some properties are missing or json has incorrect structure
   */
  @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
    comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
  protected void toRepositoryVersionEntity(XmlHolder holder) throws AmbariException {

    // !!! TODO validate parsed object graph

    RepositoryVersionEntity entity = new RepositoryVersionEntity();

    StackId stackId = new StackId(holder.xml.release.stackId);

    StackEntity stackEntity = s_stackDAO.find(stackId.getStackName(), stackId.getStackVersion());

    entity.setStack(stackEntity);

    List<RepositoryInfo> repos = holder.xml.repositoryInfo.getRepositories();

    StackInfo stack = s_metaInfo.get().getStack(stackId);

    // Add service repositories (these are not contained by the VDF but are there in the stack model)
    ListMultimap<String, RepositoryInfo> stackReposByOs = stack.getRepositoriesByOs();
    repos.addAll(RepoUtil.getServiceRepos(repos, stackReposByOs));

    entity.addRepoOsEntities(s_repoVersionHelper.get().createRepoOsEntities(repos));

    entity.setVersion(holder.xml.release.getFullVersion(stack.getReleaseVersion()));

    if (StringUtils.isNotEmpty(holder.xml.release.display)) {
      entity.setDisplayName(holder.xml.release.display);
    } else {
      entity.setDisplayName(stackId.getStackName() + "-" + entity.getVersion());
    }

    entity.setType(holder.xml.release.repositoryType);
    entity.setVersionUrl(holder.url);
    entity.setVersionXml(holder.xmlString);
    entity.setVersionXsd(holder.xml.xsdLocation);

    holder.entity = entity;
  }

  /**
   * Converts a version definition to a resource
   * @param id            the definition id
   * @param xml           the version definition xml
   * @param requestedIds  the requested ids
   * @return the resource
   * @throws SystemException
   */
  private Resource toResource(String id, VersionDefinitionXml xml, Set<String> requestedIds,
      Set<String> validations) throws SystemException {

    Resource resource = new ResourceImpl(Resource.Type.VersionDefinition);
    resource.setProperty(VERSION_DEF_ID, id);

    StackId stackId = new StackId(xml.release.stackId);

    // !!! these are needed for href
    resource.setProperty(VERSION_DEF_STACK_NAME, stackId.getStackName());
    resource.setProperty(VERSION_DEF_STACK_VERSION, stackId.getStackVersion());

    StackInfo stack = null;
    try {
      stack = s_metaInfo.get().getStack(stackId.getStackName(), stackId.getStackVersion());
    } catch (AmbariException e) {
      throw new SystemException(String.format("Could not load stack %s", stackId));
    }

    setResourceProperty(resource, VERSION_DEF_TYPE_PROPERTY_ID, xml.release.repositoryType, requestedIds);
    setResourceProperty(resource, VERSION_DEF_FULL_VERSION, xml.release.getFullVersion(stack.getReleaseVersion()), requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_BUILD, xml.release.build, requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_COMPATIBLE_WITH, xml.release.compatibleWith, requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_NOTES, xml.release.releaseNotes, requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_VERSION, xml.release.version, requestedIds);
    setResourceProperty(resource, VERSION_DEF_STACK_DEFAULT, xml.isStackDefault(), requestedIds);
    setResourceProperty(resource, VERSION_DEF_STACK_REPO_UPDATE_LINK_EXISTS, (stack.getRepositoryXml().getLatestURI() != null), requestedIds);
    setResourceProperty(resource, VERSION_DEF_DISPLAY_NAME, xml.release.display, requestedIds);

    if (null != validations) {
      setResourceProperty(resource, VERSION_DEF_VALIDATION, validations, requestedIds);
    }

    setResourceProperty(resource, VERSION_DEF_AVAILABLE_SERVICES, xml.getAvailableServices(stack), requestedIds);
    setResourceProperty(resource, VERSION_DEF_STACK_SERVICES, xml.getStackServices(stack), requestedIds);
    setResourceProperty(resource, VERSION_DEF_MIN_JDK, stack.getMinJdk(), requestedIds);
    setResourceProperty(resource, VERSION_DEF_MAX_JDK, stack.getMaxJdk(), requestedIds);

    return resource;
  }

  /**
   * Convert the given {@link RepositoryVersionEntity} to a {@link Resource}.
   *
   * @param entity
   *          the entity to convert.
   * @param requestedIds
   *          the properties that were requested or {@code null} for all.
   * @return the resource representation of the entity (never {@code null}).
   */
  private Resource toResource(RepositoryVersionEntity entity, Set<String> requestedIds)
      throws SystemException {

    Resource resource = new ResourceImpl(Resource.Type.VersionDefinition);
    resource.setProperty(VERSION_DEF_ID, entity.getId());

    VersionDefinitionXml xml = null;
    try {
      xml = entity.getRepositoryXml();
    } catch (Exception e) {
      String msg = String.format("Could not load version definition %s", entity.getId());
      throw new SystemException(msg, e);
    }

    StackId stackId = new StackId(xml.release.stackId);

    // !!! these are needed for href
    resource.setProperty(VERSION_DEF_STACK_NAME, stackId.getStackName());
    resource.setProperty(VERSION_DEF_STACK_VERSION, stackId.getStackVersion());

    setResourceProperty(resource, VERSION_DEF_TYPE_PROPERTY_ID, entity.getType(), requestedIds);
    setResourceProperty(resource, VERSION_DEF_DEFINITION_URL, entity.getVersionUrl(), requestedIds);
    setResourceProperty(resource, VERSION_DEF_FULL_VERSION, entity.getVersion(), requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_BUILD, xml.release.build, requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_COMPATIBLE_WITH, xml.release.compatibleWith, requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_NOTES, xml.release.releaseNotes, requestedIds);
    setResourceProperty(resource, VERSION_DEF_RELEASE_VERSION, xml.release.version, requestedIds);
    setResourceProperty(resource, VERSION_DEF_STACK_DEFAULT, xml.isStackDefault(), requestedIds);
    setResourceProperty(resource, VERSION_DEF_DISPLAY_NAME, xml.release.display, requestedIds);

    StackInfo stack = null;

    if (isPropertyRequested(VERSION_DEF_AVAILABLE_SERVICES, requestedIds) ||
        isPropertyRequested(VERSION_DEF_STACK_SERVICES, requestedIds)) {
      try {
        stack = s_metaInfo.get().getStack(stackId.getStackName(), stackId.getStackVersion());
      } catch (AmbariException e) {
        throw new SystemException(String.format("Could not load stack %s", stackId));
      }
    }

    if (null != stack) {
      setResourceProperty(resource, VERSION_DEF_AVAILABLE_SERVICES, xml.getAvailableServices(stack), requestedIds);
      setResourceProperty(resource, VERSION_DEF_STACK_SERVICES, xml.getStackServices(stack), requestedIds);
      setResourceProperty(resource, VERSION_DEF_MIN_JDK, stack.getMinJdk(), requestedIds);
      setResourceProperty(resource, VERSION_DEF_MAX_JDK, stack.getMaxJdk(), requestedIds);
      setResourceProperty(resource, VERSION_DEF_STACK_REPO_UPDATE_LINK_EXISTS, (stack.getRepositoryXml().getLatestURI() != null), requestedIds);
    }

    return resource;
  }

  /**
   * Provide the dry-run entity with fake sub-resources.  These are not queryable by normal API.
   */
  private void addSubresources(Resource res, RepositoryVersionEntity entity) {
    JsonNodeFactory factory = JsonNodeFactory.instance;

    ArrayNode subs = factory.arrayNode();

    for (RepoOsEntity os : entity.getRepoOsEntities()) {
      ObjectNode osBase = factory.objectNode();

      ObjectNode osElement = factory.objectNode();
      osElement.put(PropertyHelper.getPropertyName(OperatingSystemResourceProvider.OPERATING_SYSTEM_AMBARI_MANAGED_REPOS),
          os.isAmbariManaged());
      osElement.put(PropertyHelper.getPropertyName(OperatingSystemResourceProvider.OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID),
          os.getFamily());

      osElement.put(PropertyHelper.getPropertyName(OperatingSystemResourceProvider.OPERATING_SYSTEM_STACK_NAME_PROPERTY_ID),
          entity.getStackName());
      osElement.put(PropertyHelper.getPropertyName(OperatingSystemResourceProvider.OPERATING_SYSTEM_STACK_VERSION_PROPERTY_ID),
          entity.getStackVersion());

      osBase.put(PropertyHelper.getPropertyCategory(OperatingSystemResourceProvider.OPERATING_SYSTEM_AMBARI_MANAGED_REPOS),
          osElement);

      ArrayNode reposArray = factory.arrayNode();
      for (RepoDefinitionEntity repo : os.getRepoDefinitionEntities()) {
        ObjectNode repoBase = factory.objectNode();

        ObjectNode repoElement = factory.objectNode();

        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID),
            repo.getBaseUrl());
        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_OS_TYPE_PROPERTY_ID),
            os.getFamily());
        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID),
            repo.getRepoID());
        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID),
            repo.getRepoName());
        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_DISTRIBUTION_PROPERTY_ID),
            repo.getDistribution());
        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_COMPONENTS_PROPERTY_ID),
            repo.getComponents());
        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_STACK_NAME_PROPERTY_ID),
            entity.getStackName());
        repoElement.put(PropertyHelper.getPropertyName(RepositoryResourceProvider.REPOSITORY_STACK_VERSION_PROPERTY_ID),
            entity.getStackVersion());

        @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
          comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
               ArrayNode applicableServicesNode = factory.arrayNode();
        if(repo.getApplicableServices() != null) {
          for (String applicableService : repo.getApplicableServices()) {
            applicableServicesNode.add(applicableService);
          }
        }
        repoElement.put(PropertyHelper.getPropertyName(
          RepositoryResourceProvider.REPOSITORY_APPLICABLE_SERVICES_PROPERTY_ID), applicableServicesNode);

        ArrayNode tagsNode = factory.arrayNode();
        for (RepoTag repoTag : repo.getTags()) {
          tagsNode.add(repoTag.toString());
        }
        repoElement.put(PropertyHelper.getPropertyName(
            RepositoryResourceProvider.REPOSITORY_TAGS_PROPERTY_ID), tagsNode);

        repoBase.put(PropertyHelper.getPropertyCategory(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID),
            repoElement);

        reposArray.add(repoBase);
      }

      osBase.put(new RepositoryResourceDefinition().getPluralName(), reposArray);

      subs.add(osBase);
    }

    res.setProperty(new OperatingSystemResourceDefinition().getPluralName(), subs);
  }


  /**
   * Convenience class to hold the xml String representation, the url, and the parsed object.
   */
  private static class XmlHolder {
    String url = null;
    String xmlString = null;
    VersionDefinitionXml xml = null;
    RepositoryVersionEntity entity = null;
  }


}
