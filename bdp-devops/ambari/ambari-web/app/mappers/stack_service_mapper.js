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
var App = require('app');

App.stackServiceMapper = App.QuickDataMapper.create({
  model: App.StackService,
  component_model: App.StackServiceComponent,

  config: {
    id: 'service_name',
    stack_id: 'stack_id',
    service_name: 'service_name',
    service_type: 'service_type',
    display_name: 'display_name',
    config_types: 'config_types',
    comments: 'comments',
    service_version: 'service_version',
    stack_name: 'stack_name',
    stack_version: 'stack_version',
    selection: 'selection',
    is_mandatory: 'is_mandatory',
    is_selected: 'is_selected',
    is_installed: 'is_installed',
    is_installable: 'is_installable',
    is_service_with_widgets: 'is_service_with_widgets',
    required_services: 'required_services',
    service_check_supported: 'service_check_supported',
    support_delete_via_ui: 'support_delete_via_ui',
    service_components_key: 'service_components',
    service_components_type: 'array',
    service_components: {
      item: 'id'
    }
  },

  component_config: {
    id: 'component_name',
    component_name: 'component_name',
    display_name: 'display_name',
    cardinality: 'cardinality',
    custom_commands: 'custom_commands',
    reassign_allowed : 'reassign_allowed',
    decommission_allowed: 'decommission_allowed',
    has_bulk_commands_definition: 'has_bulk_commands_definition',
    bulk_commands_display_name: 'bulk_commands_display_name',
    bulk_commands_master_component_name: 'bulk_commands_master_component_name',
    service_name: 'service_name',
    component_category: 'component_category',
    rolling_restart_supported: 'rolling_restart_supported',
    is_master: 'is_master',
    is_client: 'is_client',
    component_type: 'component_type',
    stack_name: 'stack_name',
    stack_version: 'stack_version',
    stack_service_id: 'service_name',
    dependencies_key: 'dependencies',
    dependencies_type: 'array',
    dependencies: {
      item: 'Dependencies'
    }
  },

  mapStackServices: function(json) {
    App.set('isStackServicesLoaded',false);
    this.clearStackModels();
    App.resetDsStoreTypeMap(App.StackServiceComponent);
    App.resetDsStoreTypeMap(App.StackService);
    this.map(json);
    App.set('isStackServicesLoaded',true);
  },

  map: function (json) {
    var model = this.get('model');
    var result = [];
    var stackServiceComponents = [];
    var nonInstallableServices = ['KERBEROS'];
    var displayOrderLength = App.StackService.displayOrder.length;
    var items = json.items.map(function (item, index) {
      var displayOrderIndex = App.StackService.displayOrder.indexOf(item.StackServices.service_name);
      return $.extend(item, {
        index: displayOrderIndex == -1 ? displayOrderLength + index : displayOrderIndex
      });
    }).sortProperty('index');
    items.forEach(function (item) {
      var stackService = item.StackServices;
      var serviceComponents = [];
      item.components.forEach(function (serviceComponent) {
        var dependencies = serviceComponent.dependencies.map(function (dependecy) {
          return { Dependencies: App.keysUnderscoreToCamelCase(App.permit(dependecy.Dependencies, ['component_name', 'scope', 'service_name', 'type'])) };
        });
        serviceComponent.StackServiceComponents.id = serviceComponent.StackServiceComponents.component_name;
        serviceComponent.StackServiceComponents.dependencies = dependencies;
        serviceComponents.push(serviceComponent.StackServiceComponents);
        var parsedResult = this.parseIt(serviceComponent.StackServiceComponents, this.get('component_config'));
        if (parsedResult.id == 'MYSQL_SERVER') {
          parsedResult.custom_commands = parsedResult.custom_commands.without('CLEAN');
        }
        stackServiceComponents.push(parsedResult);
      }, this);
      stackService.stack_id = stackService.stack_name + '-' + stackService.stack_version;
      stackService.service_components = serviceComponents;
      stackService.is_service_with_widgets = item.artifacts.someProperty('Artifacts.artifact_name', 'widgets_descriptor');
      // @todo: replace with server response value after API implementation
      if (nonInstallableServices.contains(stackService.service_name)) {
        stackService.is_installable = false;
        stackService.is_selected = false;
      }
      if(stackService.selection === "MANDATORY") {
        stackService.is_mandatory = true;
      }
      result.push(this.parseIt(stackService, this.get('config')));
    }, this);
    App.store.safeLoadMany(this.get('component_model'), stackServiceComponents);
    App.store.safeLoadMany(model, result);
  },

  /**
   * Clean store from already loaded data.
   **/
  clearStackModels: function () {
    var models = [App.StackServiceComponent, App.StackService];
    models.forEach(function (model) {
      var records = App.get('store').findAll(model).filterProperty('id');
      records.forEach(function (rec) {
        Ember.run(this, function () {
          rec.deleteRecord();
          App.store.fastCommit();
        });
      }, this);
    }, this);
  }
});

