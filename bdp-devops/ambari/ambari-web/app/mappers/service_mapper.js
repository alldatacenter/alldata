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
var misc = require('utils/misc');

App.serviceMapper = App.QuickDataMapper.create({
  model: App.Service,
  config: {
    id: 'ServiceInfo.service_name',
    service_name: 'ServiceInfo.service_name',
    work_status: 'ServiceInfo.state',
    desired_repository_version_id: 'ServiceInfo.desired_repository_version_id'
  },
  initialAppLoad: false,
  passiveStateMap: {},
  map: function (json) {
    console.time("App.serviceMapper execution time");
    var self = this;
    var passiveStateMap = this.get('passiveStateMap');
    json.items.forEach(function (service) {
      var cachedService = App.cache['services'].findProperty('ServiceInfo.service_name', service.ServiceInfo.service_name);
      if (cachedService) {
        // restore service workStatus
        App.Service.find(cachedService.ServiceInfo.service_name).set('workStatus', service.ServiceInfo.state);
        cachedService.ServiceInfo.state = service.ServiceInfo.state;
      } else {
        var serviceData = {
          ServiceInfo: {
            service_name: service.ServiceInfo.service_name,
            state: service.ServiceInfo.state,
            desired_repository_version_id: service.ServiceInfo.desired_repository_version_id
          },
          host_components: [],
          components: []
        };
        App.cache['services'].push(serviceData);
      }
      passiveStateMap[service.ServiceInfo.service_name] = service.ServiceInfo.maintenance_state;
    });

    if (!this.get('initialAppLoad')) {
      var parsedCacheServices = App.cache['services'].map(function(item){
        App.serviceMetricsMapper.mapExtendedModel(item);
        return self.parseIt(item, self.get('config'));
      });
      parsedCacheServices = misc.sortByOrder(App.StackService.find().mapProperty('serviceName'), parsedCacheServices);
      App.store.safeLoadMany(this.get('model'), parsedCacheServices);
      this.set('initialAppLoad', true);
    }
    this.servicesLoading().done(function setMaintenanceState() {
      for (var service in passiveStateMap) {
        if (passiveStateMap.hasOwnProperty(service)) {
          App.Service.find(service).set('passiveState', passiveStateMap[service]);
        }
      }
    });

    console.timeEnd("App.serviceMapper execution time");
  },

  servicesLoading: function () {
    var dfd = $.Deferred();
    var isAllServicesLoaded = App.store.findAll(App.Service).everyProperty('isLoaded', true);
    if (isAllServicesLoaded) {
      dfd.resolve();
    } else {
      var interval = setInterval(function checkIfServicesLoaded() {
        var isAllServicesLoaded = App.store.findAll(App.Service).everyProperty('isLoaded', true);
        if (isAllServicesLoaded) {
          dfd.resolve();
          clearInterval(interval);
        }
      }, 5);
    }
    return dfd.promise();
  }
});
