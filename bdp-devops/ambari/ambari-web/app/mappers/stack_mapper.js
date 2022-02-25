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

App.stackMapper = App.QuickDataMapper.create({
  modelStack: App.Stack,
  modelOS: App.OperatingSystem,
  modelRepo: App.Repository,
  modelServices: App.ServiceSimple,
  
  configStack: {
    id: 'id',
    stack_name: 'stack_name',
    stack_version: 'stack_version',
    stack_default: 'stack_default',
    stack_repo_update_link_exists: 'stack_repo_update_link_exists',
    show_available: 'show_available',
    type: 'type',
    repository_version: 'repository_version',
    active: 'active',
    parent_stack_version: 'parent_stack_version',
    min_upgrade_version: 'min_upgrade_version',
    min_jdk_version: 'min_jdk',
    max_jdk_version: 'max_jdk',
    is_selected: 'is_selected',
    config_types: 'config_types',
    use_redhat_satellite: 'use_redhat_satellite',
    stack_services_key: 'stack_services',
    stack_services_type: 'array',
    stack_services: {
      item: 'id'
    },
    operating_systems_key: 'operating_systems',
    operating_systems_type: 'array',
    operating_systems: {
      item: 'id'
    }
  },
  
  configOS: {
    id: 'id',
    os_type: 'os_type',
    stack_name: 'stack_name',
    stack_version: 'stack_version',
    stack_id: 'stack_id',
    is_selected: 'is_selected',
    repositories_key: 'repositories',
    repositories_type: 'array',
    repositories: {
      item: 'id'
    }
  },

  configService: {
    id: 'id',
    name: 'name',
    display_name: 'display_name',
    latest_version: 'latest_version'
  },
  
  configRepository: {
    id: 'id',
    base_url: 'base_url',
    base_url_init: 'base_url',
    default_base_url: 'default_base_url',
    latest_base_url: 'latest_base_url',
    mirrors_list: 'mirrors_list',
    os_type: 'os_type',
    original_repo_id: 'repo_id',
    repo_id: 'repo_id',
    repo_name: 'repo_name',
    stack_name: 'stack_name',
    stack_version: 'stack_version',
    operating_system_id: 'os_id',
    components: 'components',
    distribution: 'distribution',
    tags: 'tags',
    applicable_services: 'applicable_services'
  },
  
  map: function(json) {
    var modelStack = this.get('modelStack');
    var modelOS = this.get('modelOS');
    var modelRepo = this.get('modelRepo');
    var modelServices = this.get('modelServices');
    var resultOS = [];
    var resultRepo = [];
    var resultServices = [];

    var item = json;
    var stack = item.VersionDefinition;
    if (!stack.id) {
      stack.id = stack.stack_name + '-' + stack.stack_version + '-' + stack.repository_version; //HDP-2.5-2.5.0.0
    }
    var operatingSystemsArray = [];
    var servicesArray = [];

    item.operating_systems.forEach(function(ops) {
      var operatingSystems = ops.OperatingSystems;

      var repositoriesArray = [];
      ops.repositories.forEach(function(repo) {
        repo.Repositories.id = [stack.id, repo.Repositories.os_type, repo.Repositories.repo_id].join('-');
        repo.Repositories.os_id = [stack.id, repo.Repositories.os_type].join('-');
        if (!repo.Repositories.latest_base_url) repo.Repositories.latest_base_url = repo.Repositories.base_url;
        resultRepo.push(this.parseIt(repo.Repositories, this.get('configRepository')));
        repositoriesArray.pushObject(repo.Repositories);
      }, this);

      operatingSystems.id = stack.id + "-" + operatingSystems.os_type;
      operatingSystems.stack_id = operatingSystems.stack_name + "-" + operatingSystems.stack_version;
      operatingSystems.repositories = repositoriesArray;
      operatingSystems.is_selected = ops.isSelected == true || ops.isSelected == undefined;
      resultOS.push(this.parseIt(operatingSystems, this.get('configOS')));
      operatingSystemsArray.pushObject(operatingSystems);
        
    }, this);

    stack.stack_services.forEach(function(service) {
      var serviceObj = {
        id: service.name + '-' + stack.id,
        name: service.name,
        display_name: service.display_name,
        latest_version: service.versions? service.versions[0] : ''
      };
      resultServices.push(this.parseIt(serviceObj, this.get('configService')));
      servicesArray.pushObject(serviceObj);
    }, this);

    //In case ambari_managed_repositories is undefined, set use_redhat_satellite to be false
    stack.use_redhat_satellite = item.operating_systems[0].OperatingSystems.ambari_managed_repositories === false;
    stack.stack_services = servicesArray;
    stack.operating_systems = operatingSystemsArray;
    App.store.safeLoadMany(modelRepo, resultRepo);
    App.store.safeLoadMany(modelOS, resultOS);
    App.store.safeLoadMany(modelServices, resultServices);
    App.store.safeLoad(modelStack, this.parseIt(stack, this.get('configStack')));
  }
});
