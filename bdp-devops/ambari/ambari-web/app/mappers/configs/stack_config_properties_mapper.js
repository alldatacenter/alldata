/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.stackConfigPropertiesMapper = App.QuickDataMapper.create({
  /**
   * this is map for configs that will be stored as plan objects
   */
  configToPlain: {
    id: 'id',
    name: 'StackConfigurations.property_name',
    displayName: 'StackConfigurations.property_display_name',
    fileName: 'StackConfigurations.type',
    filename: 'StackConfigurations.type',
    description: 'StackConfigurations.property_description',
    value: 'StackConfigurations.property_value',
    recommendedValue: 'StackConfigurations.property_value',
    serviceName: 'StackConfigurations.service_name',
    stackName: 'StackConfigurations.stack_name',
    stackVersion: 'StackConfigurations.stack_version',
    isOverridable: 'StackConfigurations.property_value_attributes.overridable',
    isVisible: 'StackConfigurations.property_value_attributes.visible',
    showLabel: 'StackConfigurations.property_value_attributes.show_property_name',
    displayType: 'StackConfigurations.property_value_attributes.type',
    unit: 'StackConfigurations.property_value_attributes.unit',

    isRequired: 'is_required',
    isReconfigurable: 'is_reconfigurable',
    isEditable: 'is_editable',
    isRequiredByAgent: 'is_required_by_agent',

    isFinal: 'recommended_is_final',
    recommendedIsFinal: 'recommended_is_final',
    supportsFinal: 'supports_final',

    propertyDependedBy: 'StackConfigurations.property_depended_by',
    propertyDependsOn: 'StackConfigurations.property_depends_on',
    valueAttributes: 'StackConfigurations.property_value_attributes',

    /**** ui properties, ib future should be moved to BE ***/
    category: 'category',
    index: 'index',
    /*** temp properties until all radio buttons type will be moved to BE **/
    radioName: 'radioName',
    options: 'options',
    /*** temp property until visibility dependencies will be retrieved from stack **/
    dependentConfigPattern: 'dependentConfigPattern'
  },

  map: function (json) {
    console.time('App.stackConfigPropertiesMapper execution time');
    if (json && json.Versions) {
      //hack for cluster versions
      json = {items: [json]};
      var clusterConfigs = true;
    }
    if (json && json.items) {
      var configs = [];
      json.items.forEach(function(stackItem) {
        var configTypeInfo = clusterConfigs ? Em.get(stackItem, 'Versions.config_types') : Em.get(stackItem, 'StackServices.config_types');

        stackItem.configurations.forEach(function(config) {
          if (clusterConfigs) {
            config.StackConfigurations = config.StackLevelConfigurations;
          }
          var configType = App.config.getConfigTagFromFileName(config.StackConfigurations.type);
          config.id = App.config.configId(config.StackConfigurations.property_name, configType);
          config.recommended_is_final = config.StackConfigurations.final === "true";
          config.supports_final = !!configTypeInfo[configType] && configTypeInfo[configType].supports.final === "true";

          var attributes = config.StackConfigurations.property_value_attributes;
          if (attributes) {
            config.is_required = this._isRequired(attributes.empty_value_valid, config.StackConfigurations.property_value);
            config.is_reconfigurable = !(attributes.editable_only_at_install || config.StackConfigurations.type === 'cluster-env.xml');
            config.is_editable = !attributes.read_only;
            config.is_required_by_agent = !attributes.ui_only_property;
          }

          if (!config.StackConfigurations.property_display_name) {
            config.StackConfigurations.property_display_name = config.StackConfigurations.property_name;
          }

          if (!config.StackConfigurations.service_name) {
            config.StackConfigurations.service_name = 'MISC';
          }

          if (!attributes || !attributes.type) {
            if (!attributes) {
              config.StackConfigurations.property_value_attributes = {};
            }
            config.StackConfigurations.property_value_attributes.type = App.config.getDefaultDisplayType(config.StackConfigurations.property_value);
          }
          // Map from /dependencies to property_depended_by
          config.StackConfigurations.property_depended_by = [];
          if (config.dependencies && config.dependencies.length > 0) {
            config.dependencies.forEach(function(dep) {
              config.StackConfigurations.property_depended_by.push({
                type : dep.StackConfigurationDependency.dependency_type,
                name : dep.StackConfigurationDependency.dependency_name
              });
              var service = App.StackService.find(config.StackConfigurations.service_name);
              var dependentService = App.config.get('serviceByConfigTypeMap')[dep.StackConfigurationDependency.dependency_type];
              if (dependentService && service && dependentService.get('serviceName') != service.get('serviceName') && !service.get('dependentServiceNames').contains(dependentService.get('serviceName'))) {
                service.set('dependentServiceNames', service.get('dependentServiceNames').concat(dependentService.get('serviceName')));
              }
            });
          }
          if (Em.get(config, 'StackConfigurations.property_depends_on.length') > 0) {
            config.StackConfigurations.property_depends_on.forEach(function(dep) {
              var service = App.StackService.find(config.StackConfigurations.service_name);
              var dependentService = App.config.get('serviceByConfigTypeMap')[dep.type];
              if (dependentService && service && dependentService.get('serviceName') != service.get('serviceName') && !service.get('dependentServiceNames').contains(dependentService.get('serviceName'))) {
                service.set('dependentServiceNames', service.get('dependentServiceNames').concat(dependentService.get('serviceName')));
              }
            });
          }
          /**
           * merging stack info with that is stored on UI
           * for now is not used; uncomment in will be needed
           * this.mergeWithUI(config);
           */
          if (this.isMiscService(config.StackConfigurations.property_type)) {
            this.handleSpecialProperties(config);
          } else {
            this.mergeWithUI(config);
          }

          var staticConfigInfo = this.parseIt(config, this.get('configToPlain'));
          var v = Em.isNone(staticConfigInfo.recommendedValue) ? staticConfigInfo.recommendedValue : staticConfigInfo.value;
          staticConfigInfo.value = staticConfigInfo.recommendedValue = App.config.formatPropertyValue(staticConfigInfo, v);
          staticConfigInfo.isSecureConfig = App.config.getIsSecure(staticConfigInfo.name);
          staticConfigInfo.description = App.config.getDescription(staticConfigInfo.description, staticConfigInfo.displayType);
          staticConfigInfo.name = JSON.parse('"' + staticConfigInfo.name + '"');
          staticConfigInfo.isUserProperty = false;
          if (Em.isNone(staticConfigInfo.index)) {
            staticConfigInfo.index = Infinity;
          }
          App.configsCollection.add(staticConfigInfo);

        }, this);
      }, this);
      this.addUIOnlyProperties(configs);
    }
    console.timeEnd('App.stackConfigPropertiesMapper execution time');
  },

  /******************* METHODS TO MERGE STACK PROPERTIES WITH STORED ON UI *********************************/

  /**
   * Config should not be required if value from stack is null
   *
   * @param allowEmpty
   * @param propertyValue
   * @returns {*|boolean}
   * @private
   */
  _isRequired: function (allowEmpty, propertyValue) {
    return !allowEmpty && !Em.isNone(propertyValue);
  },

  /**
   * find UI config with current name and fileName
   * if there is such property - adds some info to config object
   * @param {Object} config
   * @method mergeWithUI
   */
  mergeWithUI: function(config) {
    var c = config.StackConfigurations;
    var uiConfigProperty = this.getUIConfig(c.property_name, c.type);

    config.category = uiConfigProperty && uiConfigProperty.category ? uiConfigProperty.category : App.config.getDefaultCategory(true, c.type);

    if (uiConfigProperty) {
      config.index = (uiConfigProperty.index !== undefined) ? uiConfigProperty.index : Infinity;
      if (uiConfigProperty.displayType) {
        c.property_value_attributes.type = uiConfigProperty.displayType;
        config.radioName = uiConfigProperty.radioName;
        config.options = uiConfigProperty.options;
      }
      config.dependentConfigPattern = uiConfigProperty.dependentConfigPattern;
    }
  },

  /**
   *
   * @param config
   */
  handleSpecialProperties: function(config) {
    var types = config.StackConfigurations.property_type;
    if (!types.contains('ADDITIONAL_USER_PROPERTY')) {
      config.index = App.StackService.displayOrder.indexOf(config.StackConfigurations.service_name) + 1 || 30;
    }
    // displayType from stack ignored, cause UID and GID should be shown along with service's user config
    if (types.contains('UID') || types.contains('GID')) {
      config.StackConfigurations.property_value_attributes.type = 'uid_gid';
    }
    config.StackConfigurations.service_name = 'MISC';
    config.category = 'Users and Groups';
  },

  /**
   * defines if property should refer to MISC tab
   * @param type
   * @returns {Boolean}
   */
  isMiscService: function(type) {
    return type.length &&
      (type.contains('USER')
      || type.contains('GROUP')
      || type.contains('ADDITIONAL_USER_PROPERTY')
      || type.contains('UID')
      || type.contains('GID'));
  },

  /**
   * add properties that doesn't have any info on stack definition
   * @param configs
   */
  addUIOnlyProperties: function(configs) {
    require('data/configs/ui_properties').concat(require('data/configs/alert_notification')).forEach(function(p) {
      if(p.name == "dfs.ha.fencing.methods" && !App.get('isHaEnabled')) return;

      configs.push({
        id: App.config.configId(p.name, p.filename),
        name: p.name,
        display_name: p.displayName,
        file_name: p.filename,
        description: p.description || '',
        is_required_by_agent: p.isRequiredByAgent !== false, // by default is_required_by_agent should be true
        service_name: p.serviceName,
        supports_final: false,
        category: p.category,
        index: p.index,
        stack_name: App.get('currentStackName'),
        stack_version: App.get('currentStackVersionNumber')
      });
      p.id = App.config.configId(p.name, p.filename);
      App.configsCollection.add(p);
    });
  },
  /**
   * returns config with such name and fileName if there is such on UI
   * otherwise returns null
   * @param propertyName
   * @param siteName
   * @returns {Object|null}
   * @method getUIConfig
   */
  getUIConfig: function(propertyName, siteName) {
    return App.config.get('preDefinedSitePropertiesMap')[App.config.configId(propertyName, siteName)];
  }
});
