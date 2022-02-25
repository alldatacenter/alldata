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

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
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
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.dao.TopologyRequestDAO;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintConfiguration;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.orm.entities.BlueprintSettingEntity;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;
import org.apache.ambari.server.stack.NoSuchStackException;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.BlueprintFactory;
import org.apache.ambari.server.topology.GPLLicenseNotAcceptedException;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.SecurityConfiguration;
import org.apache.ambari.server.topology.SecurityConfigurationFactory;
import org.apache.ambari.server.utils.SecretReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;


/**
 * Resource Provider for Blueprint resources.
 */
public class BlueprintResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(BlueprintResourceProvider.class);

  // ----- Property ID constants ---------------------------------------------

  // Blueprints
  public static final String BLUEPRINT_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Blueprints", "blueprint_name");
  public static final String STACK_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Blueprints", "stack_name");
  public static final String STACK_VERSION_PROPERTY_ID =
      PropertyHelper.getPropertyId("Blueprints", "stack_version");

  public static final String BLUEPRINT_SECURITY_PROPERTY_ID =
    PropertyHelper.getPropertyId("Blueprints", "security");

  public static final String BLUEPRINTS_PROPERTY_ID = "Blueprints";

  // Host Groups
  public static final String HOST_GROUP_PROPERTY_ID = "host_groups";
  public static final String HOST_GROUP_NAME_PROPERTY_ID = "name";
  public static final String HOST_GROUP_CARDINALITY_PROPERTY_ID = "cardinality";

  // Host Group Components
  public static final String COMPONENT_PROPERTY_ID ="components";
  public static final String COMPONENT_NAME_PROPERTY_ID ="name";
  public static final String COMPONENT_PROVISION_ACTION_PROPERTY_ID = "provision_action";

  // Configurations
  public static final String CONFIGURATION_PROPERTY_ID = "configurations";

  // Setting
  public static final String SETTING_PROPERTY_ID = "settings";

  public static final String PROPERTIES_PROPERTY_ID = "properties";
  public static final String PROPERTIES_ATTRIBUTES_PROPERTY_ID = "properties_attributes";
  public static final String SCHEMA_IS_NOT_SUPPORTED_MESSAGE =
    "Configuration format provided in Blueprint is not supported";
  public static final String REQUEST_BODY_EMPTY_ERROR_MESSAGE =
    "Request body for Blueprint create request is empty";
  public static final String CONFIGURATION_LIST_CHECK_ERROR_MESSAGE =
    "Configurations property must be a List of Maps";
  public static final String CONFIGURATION_MAP_CHECK_ERROR_MESSAGE =
    "Configuration elements must be Maps";
  public static final String CONFIGURATION_MAP_SIZE_CHECK_ERROR_MESSAGE =
    "Configuration Maps must hold a single configuration type each";

  /**
   * The key property ids for a Blueprint resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Blueprint, BLUEPRINT_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a Blueprint resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      BLUEPRINT_NAME_PROPERTY_ID,
      STACK_NAME_PROPERTY_ID,
      STACK_VERSION_PROPERTY_ID,
      BLUEPRINT_SECURITY_PROPERTY_ID,
      HOST_GROUP_PROPERTY_ID,
      CONFIGURATION_PROPERTY_ID,
      SETTING_PROPERTY_ID);

  /**
   * Used to create Blueprint instances
   */
  private static BlueprintFactory blueprintFactory;

  /**
   * Used to create SecurityConfiguration instances
   */
  private static SecurityConfigurationFactory securityConfigurationFactory;

  /**
   * Blueprint Data Access Object
   */
  private static BlueprintDAO blueprintDAO;

  /**
   * Topology request dao
   */
  private static TopologyRequestDAO topologyRequestDAO;

  /**
   * Used to serialize to/from json.
   */
  private static Gson jsonSerializer;

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param controller      management controller
   */
  BlueprintResourceProvider(AmbariManagementController controller) {
    super(Resource.Type.Blueprint, propertyIds, keyPropertyIds, controller);
  }

  /**
   * Static initialization.
   *
   * @param factory   blueprint factory
   * @param bpDao       blueprint data access object
   * @param gson      json serializer
   */
  public static void init(BlueprintFactory factory, BlueprintDAO bpDao, TopologyRequestDAO trDao,
                          SecurityConfigurationFactory securityFactory, Gson gson, AmbariMetaInfo metaInfo) {
    blueprintFactory = factory;
    blueprintDAO = bpDao;
    topologyRequestDAO = trDao;
    securityConfigurationFactory = securityFactory;
    jsonSerializer = gson;
    ambariMetaInfo = metaInfo;
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException,
             ResourceAlreadyExistsException, NoSuchParentResourceException {

    for (Map<String, Object> properties : request.getProperties()) {
      try {
        createResources(getCreateCommand(properties, request.getRequestInfoProperties()));
      }catch(IllegalArgumentException e) {
        LOG.error("Exception while creating blueprint", e);
        throw e;
      }
    }
    notifyCreate(Resource.Type.Blueprint, request);

    return getRequestStatus(null);
  }

  @Override
  //todo: continue to use dao/entity directly or use blueprint factory?
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    List<BlueprintEntity> results        = null;
    boolean               applyPredicate = false;

    if (predicate != null) {
      Set<Map<String, Object>> requestProps = getPropertyMaps(predicate);
      if (requestProps.size() == 1 ) {
        String name = (String) requestProps.iterator().next().get(
            BLUEPRINT_NAME_PROPERTY_ID);

        if (name != null) {
          BlueprintEntity entity = blueprintDAO.findByName(name);
          results = entity == null ? Collections.emptyList() :
              Collections.singletonList(entity);
        }
      }
    }

    if (results == null) {
      applyPredicate = true;
      results = blueprintDAO.findAll();
    }

    Set<Resource> resources  = new HashSet<>();
    for (BlueprintEntity entity : results) {
      Resource resource = toResource(entity, getRequestPropertyIds(request, predicate));
      if (predicate == null || ! applyPredicate || predicate.evaluate(resource)) {
        resources.add(resource);
      }
    }

    if (predicate != null && resources.isEmpty()) {
      throw new NoSuchResourceException(
          "The requested resource doesn't exist: Blueprint not found, " + predicate);
    }

    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
             NoSuchResourceException, NoSuchParentResourceException {

    // no-op, blueprints are immutable.  Service doesn't support PUT so should never get here.
    return null;
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
             NoSuchResourceException, NoSuchParentResourceException {

    //TODO (jspeidel): Revisit concurrency control
    Set<Resource> setResources = getResources(
        new RequestImpl(null, null, null, null), predicate);

    List<TopologyRequestEntity> provisionRequests = topologyRequestDAO.findAllProvisionRequests();
    // Blueprints for which a provision request was submitted. This blueprints (should be zero or one) are read only.
    Set<String> provisionedBlueprints =
      provisionRequests.stream().map(TopologyRequestEntity::getBlueprintName).collect(toSet());

    for (final Resource resource : setResources) {
      final String blueprintName =
        (String) resource.getPropertyValue(BLUEPRINT_NAME_PROPERTY_ID);
      Preconditions.checkArgument(!provisionedBlueprints.contains(blueprintName),
        "Blueprint %s cannot be deleted as cluster provisioning was initiated on it.", blueprintName);
      LOG.info("Deleting Blueprint, name = " + blueprintName);
      modifyResources(() -> {
        blueprintDAO.removeByName(blueprintName);
        return null;
      });
    }

    notifyDelete(Resource.Type.Blueprint, predicate);
    return getRequestStatus(null);
  }

  /**
   * Used to get stack metainfo.
   */
  private static AmbariMetaInfo ambariMetaInfo;

  // ----- Instance Methods ------------------------------------------------

  /**
   * Create a resource instance from a blueprint entity.
   *
   * @param entity        blueprint entity
   * @param requestedIds  requested id's
   *
   * @return a new resource instance for the given blueprint entity
   */
  protected Resource toResource(BlueprintEntity entity, Set<String> requestedIds) throws NoSuchResourceException {
    StackEntity stackEntity = entity.getStack();
    Resource resource = new ResourceImpl(Resource.Type.Blueprint);
    setResourceProperty(resource, BLUEPRINT_NAME_PROPERTY_ID, entity.getBlueprintName(), requestedIds);
    setResourceProperty(resource, STACK_NAME_PROPERTY_ID, stackEntity.getStackName(), requestedIds);
    setResourceProperty(resource, STACK_VERSION_PROPERTY_ID, stackEntity.getStackVersion(), requestedIds);

    List<Map<String, Object>> listGroupProps = new ArrayList<>();
    Collection<HostGroupEntity> hostGroups = entity.getHostGroups();
    for (HostGroupEntity hostGroup : hostGroups) {
      Map<String, Object> mapGroupProps = new HashMap<>();
      mapGroupProps.put(HOST_GROUP_NAME_PROPERTY_ID, hostGroup.getName());
      listGroupProps.add(mapGroupProps);
      mapGroupProps.put(HOST_GROUP_CARDINALITY_PROPERTY_ID, hostGroup.getCardinality());

      List<Map<String, String>> listComponentProps = new ArrayList<>();
      Collection<HostGroupComponentEntity> components = hostGroup.getComponents();
      for (HostGroupComponentEntity component : components) {
        Map<String, String> mapComponentProps = new HashMap<>();
        mapComponentProps.put(COMPONENT_NAME_PROPERTY_ID, component.getName());

        if (component.getProvisionAction() != null) {
          mapComponentProps.put(COMPONENT_PROVISION_ACTION_PROPERTY_ID, component.getProvisionAction().toString());
        }

        listComponentProps.add(mapComponentProps);
      }
      mapGroupProps.put(COMPONENT_PROPERTY_ID, listComponentProps);
      mapGroupProps.put(CONFIGURATION_PROPERTY_ID, populateConfigurationList(
          hostGroup.getConfigurations()));
    }
    setResourceProperty(resource, HOST_GROUP_PROPERTY_ID, listGroupProps, requestedIds);
    setResourceProperty(resource, CONFIGURATION_PROPERTY_ID,
      populateConfigurationList(entity.getConfigurations()), requestedIds);
    setResourceProperty(resource, SETTING_PROPERTY_ID,
      populateSettingList(entity.getSettings()), requestedIds);

    if (entity.getSecurityType() != null) {
      Map<String, String> securityConfigMap = new LinkedHashMap<>();
      securityConfigMap.put(SecurityConfigurationFactory.TYPE_PROPERTY_ID, entity.getSecurityType().name());
      if(entity.getSecurityType() == SecurityType.KERBEROS) {
        securityConfigMap.put(SecurityConfigurationFactory.KERBEROS_DESCRIPTOR_REFERENCE_PROPERTY_ID, entity.getSecurityDescriptorReference());
      }
      setResourceProperty(resource, BLUEPRINT_SECURITY_PROPERTY_ID, securityConfigMap, requestedIds);
    }

    return resource;
  }

  /**
   * Populate a list of configuration property maps from a collection of configuration entities.
   *
   * @param configurations  collection of configuration entities
   *
   * @return list of configuration property maps
   */
  List<Map<String, Map<String, Object>>> populateConfigurationList(
      Collection<? extends BlueprintConfiguration> configurations) throws NoSuchResourceException {

    List<Map<String, Map<String, Object>>> listConfigurations = new ArrayList<>();
    for (BlueprintConfiguration config : configurations) {
      Map<String, Map<String, Object>> mapConfigurations = new HashMap<>();
      Map<String, Object> configTypeDefinition = new HashMap<>();
      String type = config.getType();

      if(config instanceof BlueprintConfigEntity) {
        Map<String, String> properties = jsonSerializer.<Map<String, String>>fromJson(
            config.getConfigData(), Map.class);

        StackEntity stack = ((BlueprintConfigEntity)config).getBlueprintEntity().getStack();
        StackInfo metaInfoStack;

        try {
          metaInfoStack = ambariMetaInfo.getStack(stack.getStackName(), stack.getStackVersion());
        } catch (AmbariException e) {
          throw new NoSuchResourceException(e.getMessage());
        }

        Map<org.apache.ambari.server.state.PropertyInfo.PropertyType, Set<String>> propertiesTypes =
            metaInfoStack.getConfigPropertiesTypes(type);

        SecretReference.replacePasswordsWithReferences(propertiesTypes, properties, type, -1l);

        configTypeDefinition.put(PROPERTIES_PROPERTY_ID, properties);
      } else {
        Map<String, Object> properties = jsonSerializer.<Map<String, Object>>fromJson(
            config.getConfigData(), Map.class);
        configTypeDefinition.put(PROPERTIES_PROPERTY_ID, properties);
      }

      Map<String, Map<String, String>> attributes = jsonSerializer.<Map<String, Map<String, String>>>fromJson(
          config.getConfigAttributes(), Map.class);
      if (attributes != null && !attributes.isEmpty()) {
        configTypeDefinition.put(PROPERTIES_ATTRIBUTES_PROPERTY_ID, attributes);
      }
      mapConfigurations.put(type, configTypeDefinition);
      listConfigurations.add(mapConfigurations);
    }

    return listConfigurations;
  }

  /**
   * Populate a list of setting property maps from a collection of setting entities.
   *
   * @param settings  collection of setting entities
   *
   * @return list of setting property maps
   */
  public static List<Map<String, Object>> populateSettingList(
          Collection<? extends BlueprintSettingEntity> settings) throws NoSuchResourceException {
    List<Map<String, Object>> listSettings = new ArrayList<>();

    if (settings != null) {
      for (BlueprintSettingEntity setting : settings) {
        List<Map<String, String>> propertiesList = jsonSerializer.<List<Map<String, String>>>fromJson(
                setting.getSettingData(), List.class);
        Map<String, Object> settingMap = new HashMap<>();
        settingMap.put(setting.getSettingName(), propertiesList);
        listSettings.add(settingMap);
      }
    }

    return listSettings;
  }

  /**
   * Populate blueprint configurations.
   *
   * @param propertyMaps  collection of configuration property maps
   * @param blueprint     blueprint entity to set configurations on
   */
  void createBlueprintConfigEntities(Collection<Map<String, String>> propertyMaps,
                                             BlueprintEntity blueprint) {

    Collection<BlueprintConfigEntity> configurations = new ArrayList<>();
    if (propertyMaps != null) {
      for (Map<String, String> configuration : propertyMaps) {
        BlueprintConfigEntity configEntity = new BlueprintConfigEntity();
        configEntity.setBlueprintEntity(blueprint);
        configEntity.setBlueprintName(blueprint.getBlueprintName());
        populateConfigurationEntity(configuration, configEntity);
        configurations.add(configEntity);
      }
    }
    blueprint.setConfigurations(configurations);
  }


  /**
   * Populate a configuration entity from properties.
   *
   * @param configuration  property map
   * @param configEntity   config entity to populate
   */
  void populateConfigurationEntity(Map<String, String> configuration, BlueprintConfiguration configEntity) {
    BlueprintConfigPopulationStrategy p = decidePopulationStrategy(configuration);
    p.applyConfiguration(configuration, configEntity);
  }

  BlueprintConfigPopulationStrategy decidePopulationStrategy(Map<String, String> configuration) {
    if (configuration != null && !configuration.isEmpty()) {
      String keyEntry = configuration.keySet().iterator().next();
      String[] keyNameTokens = keyEntry.split("/");
      int levels = keyNameTokens.length;
      String propertiesType = keyNameTokens[1];
      if (levels == 2) {
        return new BlueprintConfigPopulationStrategyV1();
      } else if ((levels == 3 && PROPERTIES_PROPERTY_ID.equals(propertiesType))
          || (levels == 4 && PROPERTIES_ATTRIBUTES_PROPERTY_ID.equals(propertiesType))) {
        return new BlueprintConfigPopulationStrategyV2();
      } else {
        throw new IllegalArgumentException(SCHEMA_IS_NOT_SUPPORTED_MESSAGE);
      }
    } else {
      return new BlueprintConfigPopulationStrategyV2();
    }
  }

  /**
   * Create a create command with all properties set.
   *
   * @param properties        properties to be applied to blueprint
   * @param requestInfoProps  request info properties
   *
   * @return a new create command
   */
  private Command<Void> getCreateCommand(final Map<String, Object> properties, final Map<String, String> requestInfoProps) {
    return new Command<Void>() {
      @SuppressWarnings("rawtypes")
      @Override
      public Void invoke() throws AmbariException {
        String rawRequestBody = requestInfoProps.get(Request.REQUEST_INFO_BODY_PROPERTY);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(rawRequestBody), REQUEST_BODY_EMPTY_ERROR_MESSAGE);

        Map<String, Object> rawBodyMap = jsonSerializer.<Map<String, Object>>fromJson(rawRequestBody, Map.class);
        Object configurationData = rawBodyMap.get(CONFIGURATION_PROPERTY_ID);

        if (configurationData != null) {
          Preconditions.checkArgument(configurationData instanceof List, CONFIGURATION_LIST_CHECK_ERROR_MESSAGE);
          for (Object map : (List) configurationData) {
            Preconditions.checkArgument(map instanceof Map, CONFIGURATION_MAP_CHECK_ERROR_MESSAGE);
            Preconditions.checkArgument(((Map) map).size() <= 1, CONFIGURATION_MAP_SIZE_CHECK_ERROR_MESSAGE);
          }
        }
        SecurityConfiguration securityConfiguration = securityConfigurationFactory
          .createSecurityConfigurationFromRequest((Map<String, Object>) rawBodyMap.get(BLUEPRINTS_PROPERTY_ID), true);

        Blueprint blueprint;
        try {
          blueprint = blueprintFactory.createBlueprint(properties, securityConfiguration);
        } catch (NoSuchStackException e) {
          throw new IllegalArgumentException("Specified stack doesn't exist: " + e, e);
        }

        if (blueprintDAO.findByName(blueprint.getName()) != null) {
          throw new DuplicateResourceException(
              "Attempted to create a Blueprint which already exists, blueprint_name=" +
              blueprint.getName());
        }

        try {
          blueprint.validateRequiredProperties();
        } catch (InvalidTopologyException | GPLLicenseNotAcceptedException e) {
          throw new IllegalArgumentException("Blueprint configuration validation failed: " + e.getMessage(), e);
        }

        String validateTopology =  requestInfoProps.get("validate_topology");
        if (validateTopology == null || ! validateTopology.equalsIgnoreCase("false")) {
          try {
            blueprint.validateTopology();
          } catch (InvalidTopologyException e) {
            throw new IllegalArgumentException(e.getMessage());
          }
        }

        LOG.info("Creating Blueprint, name=" + blueprint.getName());
        String blueprintSetting = blueprint.getSetting() == null ? "(null)" :
                jsonSerializer.toJson(blueprint.getSetting().getProperties());
        LOG.info("Blueprint setting=" + blueprintSetting);

        try {
          blueprintDAO.create(blueprint.toEntity());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        return null;
      }
    };
  }

  /**
   * The structure of blueprints is evolving where multiple resource
   * structures are to be supported. This class abstracts the population
   * of configurations which have changed from a map of key-value strings,
   * to an map containing 'properties' and 'properties_attributes' maps.
   *
   * Extending classes can determine how they want to populate the
   * configuration maps depending on input.
   */
  protected static abstract class BlueprintConfigPopulationStrategy {

    public void applyConfiguration(Map<String, String> configuration, BlueprintConfiguration blueprintConfiguration) {
      Map<String, String> configData = new HashMap<>();
      Map<String, Map<String, String>> configAttributes = new HashMap<>();

      if (configuration != null) {
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
          String absolutePropName = entry.getKey();
          String propertyValue = entry.getValue();
          String[] propertyNameTokens = absolutePropName.split("/");

          if (blueprintConfiguration.getType() == null) {
            blueprintConfiguration.setType(propertyNameTokens[0]);
          }

          addProperty(configData, configAttributes, propertyNameTokens, propertyValue);
        }
      }

      blueprintConfiguration.setConfigData(jsonSerializer.toJson(configData));
      blueprintConfiguration.setConfigAttributes(jsonSerializer.toJson(configAttributes));
    }

    protected abstract void addProperty(Map<String, String> configData,
                                        Map<String, Map<String, String>> configAttributes,
                                        String[] propertyNameTokens, String propertyValue);
  }

  /**
   * Original blueprint configuration format where configs were a map
   * of strings.
   */
  protected static class BlueprintConfigPopulationStrategyV1 extends BlueprintConfigPopulationStrategy {

    @Override
    protected void addProperty(Map<String, String> configData,
                               Map<String, Map<String, String>> configAttributes,
                               String[] propertyNameTokens, String propertyValue) {
      configData.put(propertyNameTokens[1], propertyValue);
    }

  }

  /**
   * New blueprint configuration format where configs are a map from 'properties' and
   * 'properties_attributes' to a map of strings.
   *
   * @since 1.7.0
   */
  protected static class BlueprintConfigPopulationStrategyV2 extends BlueprintConfigPopulationStrategy {

    @Override
    protected void addProperty(Map<String, String> configData,
                               Map<String, Map<String, String>> configAttributes,
                               String[] propertyNameTokens, String propertyValue) {
      if (PROPERTIES_PROPERTY_ID.equals(propertyNameTokens[1])) {
        configData.put(propertyNameTokens[2], propertyValue);
      } else if (PROPERTIES_ATTRIBUTES_PROPERTY_ID.equals(propertyNameTokens[1])) {
        addConfigAttribute(configAttributes, propertyNameTokens, propertyValue);
      }
    }

    private void addConfigAttribute(Map<String, Map<String, String>> configDependencyProperties,
                                    String[] propertyNameTokens, String value) {
      if (!configDependencyProperties.containsKey(propertyNameTokens[2])) {
        configDependencyProperties.put(propertyNameTokens[2], new HashMap<>());
      }
      Map<String, String> propertiesGroup = configDependencyProperties.get(propertyNameTokens[2]);
      propertiesGroup.put(propertyNameTokens[3], value);
    }
  }
}
