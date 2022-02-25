/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;



import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.services.parsers.BodyParseException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.MpackRequest;
import org.apache.ambari.server.controller.MpackResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.MpackDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Module;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang.Validate;

import com.google.inject.Inject;


/**
 * ResourceProvider for Mpack instances
 */
@StaticallyInject
public class MpackResourceProvider extends AbstractControllerResourceProvider {

  public static final String RESPONSE_KEY = "MpackInfo";
  public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";
  public static final String MPACK_RESOURCE_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "id";
  public static final String REGISTRY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "registry_id";
  public static final String MPACK_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_id";
  public static final String MPACK_NAME = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_name";
  public static final String MPACK_VERSION = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_version";
  public static final String MPACK_DESCRIPTION = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_description";
  public static final String MPACK_DISPLAY_NAME = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_display_name";
  public static final String MPACK_URI = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_uri";
  public static final String MODULES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "modules";
  public static final String STACK_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "stack_name";
  public static final String STACK_VERSION_PROPERTY_ID =
    RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "stack_version";

  private static Set<String> pkPropertyIds = new HashSet<>(
    Arrays.asList(MPACK_RESOURCE_ID, STACK_NAME_PROPERTY_ID, STACK_VERSION_PROPERTY_ID));

  /**
   * The property ids for an mpack resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  /**
   * The key property ids for a mpack resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  @Inject
  protected static MpackDAO mpackDAO;

  @Inject
  protected static StackDAO stackDAO;

  @Inject
  protected static RepositoryVersionDAO repositoryVersionDAO;

  static {
    // properties
    PROPERTY_IDS.add(MPACK_RESOURCE_ID);
    PROPERTY_IDS.add(REGISTRY_ID);
    PROPERTY_IDS.add(MPACK_ID);
    PROPERTY_IDS.add(MPACK_NAME);
    PROPERTY_IDS.add(MPACK_VERSION);
    PROPERTY_IDS.add(MPACK_URI);
    PROPERTY_IDS.add(MPACK_DESCRIPTION);
    PROPERTY_IDS.add(MODULES);
    PROPERTY_IDS.add(STACK_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(STACK_VERSION_PROPERTY_ID);
    PROPERTY_IDS.add(MPACK_DISPLAY_NAME);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Mpack, MPACK_RESOURCE_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Stack, STACK_NAME_PROPERTY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.StackVersion, STACK_VERSION_PROPERTY_ID);

  }

  MpackResourceProvider(AmbariManagementController controller) {
    super(Resource.Type.Mpack, PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public RequestStatus createResources(final Request request)
          throws SystemException, UnsupportedPropertyException,
          ResourceAlreadyExistsException, NoSuchParentResourceException, IllegalArgumentException {
    Set<Resource> associatedResources = new HashSet<>();
    try {
      MpackRequest mpackRequest = getRequest(request);
      if (mpackRequest == null) {
        throw new BodyParseException("Please provide " + MPACK_NAME + " ," + MPACK_VERSION + " ," + MPACK_URI);
      }
      validateCreateRequest(mpackRequest);
      MpackResponse response = getManagementController().registerMpack(mpackRequest);
      if (response != null) {
        notifyCreate(Resource.Type.Mpack, request);
        Resource resource = new ResourceImpl(Resource.Type.Mpack);
        resource.setProperty(MPACK_RESOURCE_ID, response.getId());
        resource.setProperty(MPACK_ID, response.getMpackId());
        resource.setProperty(MPACK_NAME, response.getMpackName());
        resource.setProperty(MPACK_VERSION, response.getMpackVersion());
        resource.setProperty(MPACK_URI, response.getMpackUri());
        resource.setProperty(MPACK_DESCRIPTION, response.getDescription());
        resource.setProperty(REGISTRY_ID, response.getRegistryId());
        resource.setProperty(MPACK_DISPLAY_NAME, response.getDisplayName());
        associatedResources.add(resource);
        return getRequestStatus(null, associatedResources);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (BodyParseException e1) {
      e1.printStackTrace();
    }
    return null;
  }

  /***
   * Validates the request body for the required properties in order to create an Mpack resource.
   *
   * @param mpackRequest
   */
  private void validateCreateRequest(MpackRequest mpackRequest) {
    final String mpackName = mpackRequest.getMpackName();
    final String mpackUrl = mpackRequest.getMpackUri();
    final Long registryId = mpackRequest.getRegistryId();
    final String mpackVersion = mpackRequest.getMpackVersion();

    if (registryId == null) {
      Validate.isTrue(mpackUrl != null);
      LOG.info("Received a createMpack request"
        + ", mpackUrl=" + mpackUrl);
    } else {
      Validate.notNull(mpackName, "MpackName should not be null");
      Validate.notNull(mpackVersion, "MpackVersion should not be null");
      LOG.info("Received a createMpack request"
        + ", mpackName=" + mpackName
        + ", mpackVersion=" + mpackVersion
        + ", registryId=" + registryId);
    }
    try {
      URI uri = new URI(mpackUrl);
      URL url = uri.toURL();
    } catch (Exception e) {
      Validate.isTrue(e == null,
        e.getMessage() + " is an invalid mpack uri. Please check the download link for the mpack again.");
    }
  }

  public MpackRequest getRequest(Request request) throws AmbariException {
     MpackRequest mpackRequest = new MpackRequest();
    Set<Map<String, Object>> properties = request.getProperties();
    for (Map propertyMap : properties) {
      //Mpack Download url is either given in the request body or is fetched using the registry id
      if (!propertyMap.containsKey(MPACK_URI) && !propertyMap.containsKey(REGISTRY_ID)) {
        return null;
      } else if (!propertyMap.containsKey(MPACK_URI)) {
        // Retrieve mpack download url using the given registry id
        mpackRequest.setRegistryId(Long.valueOf((String) propertyMap.get(REGISTRY_ID)));
        mpackRequest.setMpackName((String) propertyMap.get(MPACK_NAME));
        mpackRequest.setMpackVersion((String) propertyMap.get(MPACK_VERSION));
      }
      else {
        //Directly download the mpack using the given url
        mpackRequest.setMpackUri((String) propertyMap.get(MPACK_URI));
      }
    }
    return mpackRequest;
  }


  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException,
    NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new LinkedHashSet<>();
    Long mpackId = null;
    //Fetch all mpacks
    if (predicate == null) {
      // Fetch all mpacks
      Set<MpackResponse> responses = getManagementController().getMpacks();
      if (null == responses) {
        responses = Collections.emptySet();
      }

      for (MpackResponse response : responses) {
        Resource resource = setResources(response);
        Set<String> requestIds = getRequestPropertyIds(request, predicate);
        if (requestIds.contains(MODULES)) {
          List<Module> modules = getManagementController().getModules(response.getId());
          resource.setProperty(MODULES, modules);
        }
        results.add(resource);
      }
    } else {
      // Fetch a particular mpack based on id
      Map<String, Object> propertyMap = new HashMap<>(PredicateHelper.getProperties(predicate));
      if (propertyMap.containsKey(MPACK_RESOURCE_ID)) {
        Object objMpackId = propertyMap.get(MPACK_RESOURCE_ID);
        if (objMpackId != null) {
          mpackId = Long.valueOf((String) objMpackId);
        }
        MpackResponse response = getManagementController().getMpack(mpackId);

        if (null != response) {
          Resource resource = setResources(response);
          List<Module> modules = getManagementController().getModules(response.getId());
          resource.setProperty(MODULES, modules);
          results.add(resource);
        }
      } //Fetch an mpack based on a stackVersion query
      else if (propertyMap.containsKey(STACK_NAME_PROPERTY_ID)
              && propertyMap.containsKey(STACK_VERSION_PROPERTY_ID)) {
        String stackName = (String) propertyMap.get(STACK_NAME_PROPERTY_ID);
        String stackVersion = (String) propertyMap.get(STACK_VERSION_PROPERTY_ID);
        StackEntity stackEntity = stackDAO.find(stackName, stackVersion);
        mpackId = stackEntity.getMpackId();
        MpackResponse response = getManagementController().getMpack(mpackId);

        if (null != response) {
          Resource resource = setResources(response);
          resource.setProperty(STACK_NAME_PROPERTY_ID, stackName);
          resource.setProperty(STACK_VERSION_PROPERTY_ID, stackVersion);
          results.add(resource);
        }
      }
      if (null == mpackId) {
        throw new IllegalArgumentException(
                "Either the management pack ID or the stack name and version are required when searching");
      }

      if (results.isEmpty()) {
        throw new NoSuchResourceException("The requested resource doesn't exist: " + predicate);
      }
    }
    return results;
  }

  private Resource setResources(MpackResponse response) {
    Resource resource = new ResourceImpl(Resource.Type.Mpack);
    resource.setProperty(MPACK_RESOURCE_ID, response.getId());
    resource.setProperty(MPACK_ID, response.getMpackId());
    resource.setProperty(MPACK_NAME, response.getMpackName());
    resource.setProperty(MPACK_VERSION, response.getMpackVersion());
    resource.setProperty(MPACK_URI, response.getMpackUri());
    resource.setProperty(MPACK_DESCRIPTION, response.getDescription());
    resource.setProperty(REGISTRY_ID, response.getRegistryId());
    resource.setProperty(MPACK_DISPLAY_NAME, response.getDisplayName());
    return resource;
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(final Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Long mpackId;
    Map<String, Object> propertyMap = new HashMap<>(PredicateHelper.getProperties(predicate));
    DeleteStatusMetaData deleteStatusMetaData = null;

    // Allow deleting mpack only if there are no cluster services deploying using this mpack.
    // Support deleting mpacks only if no cluster has been deployed
    // (i.e. you should be able to delete an mpack during install wizard only).
    // TODO : Relax the rule
    if (getManagementController().getClusters().getClusters().size() > 0) {
      throw new SystemException("Delete request cannot be completed since there is a cluster deployed");
    } else {
      if (propertyMap.containsKey(MPACK_RESOURCE_ID)) {
        Object objMpackId = propertyMap.get(MPACK_RESOURCE_ID);
        if (objMpackId != null) {
          mpackId = Long.valueOf((String) objMpackId);
          LOG.info("Deleting Mpack, id = " + mpackId.toString());

          MpackEntity mpackEntity = mpackDAO.findById(mpackId);
          StackEntity stackEntity = stackDAO.findByMpack(mpackId);

          try {
            getManagementController().removeMpack(mpackEntity, stackEntity);
            if (mpackEntity != null) {
              deleteStatusMetaData = modifyResources(new Command<DeleteStatusMetaData>() {
                @Override
                public DeleteStatusMetaData invoke() throws AmbariException {
                  if (stackEntity != null) {
                    repositoryVersionDAO
                      .removeByStack(new StackId(stackEntity.getStackName() + "-" + stackEntity.getStackVersion()));
                    stackDAO.removeByMpack(mpackId);
                    notifyDelete(Resource.Type.Stack, predicate);
                  }
                  mpackDAO.removeById(mpackId);

                  return new DeleteStatusMetaData();
                }
              });
              notifyDelete(Resource.Type.Mpack, predicate);
              deleteStatusMetaData.addDeletedKey(mpackId.toString());
            } else {
              throw new NoSuchResourceException("The requested resource doesn't exist: " + predicate);
            }
          } catch (IOException e) {
            throw new SystemException("There is an issue with the Files");
          }
        }
      } else {
        throw new UnsupportedPropertyException(Resource.Type.Mpack, null);
      }

      return getRequestStatus(null, null, deleteStatusMetaData);
    }
  }
}

