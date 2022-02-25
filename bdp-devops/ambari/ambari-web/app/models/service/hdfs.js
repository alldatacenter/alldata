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

App.HDFSService = App.Service.extend({
  version: DS.attr('string'),
  nameNode: DS.belongsTo('App.HostComponent'),
  snameNode: DS.belongsTo('App.HostComponent'),

  activeNameNodes: DS.hasMany('App.HostComponent', {
    defaultValue: []
  }),
  standbyNameNodes: DS.hasMany('App.HostComponent', {
    defaultValue: []
  }),
  isNnHaEnabled: function() {
    return !this.get('snameNode') && this.get('hostComponents').filterProperty('componentName', 'NAMENODE').length > 1;
  }.property('snameNode','hostComponents'),
  dataNodesStarted: DS.attr('number'),
  dataNodesInstalled: DS.attr('number'),
  dataNodesTotal: DS.attr('number'),
  nfsGatewaysStarted: DS.attr('number', {defaultValue: 0}),
  nfsGatewaysInstalled: DS.attr('number', {defaultValue: 0}),
  nfsGatewaysTotal: DS.attr('number', {defaultValue: 0}),
  journalNodes: DS.hasMany('App.HostComponent'),
  nameNodeStartTimeValues: DS.attr('object', {
    defaultValue: {}
  }),
  jvmMemoryHeapUsedValues: DS.attr('object', {
    defaultValue: {}
  }),
  jvmMemoryHeapMaxValues: DS.attr('object', {
    defaultValue: {}
  }),
  decommissionDataNodes: DS.hasMany('App.HostComponent'),
  liveDataNodes: DS.hasMany('App.HostComponent'),
  deadDataNodes: DS.hasMany('App.HostComponent'),
  capacityUsed: DS.attr('number'),
  capacityTotal: DS.attr('number'),
  capacityRemaining: DS.attr('number'),
  capacityNonDfsUsed: DS.attr('number'),
  dfsTotalBlocksValues: DS.attr('object', {
    defaultValue: {}
  }),
  dfsCorruptBlocksValues: DS.attr('object', {
    defaultValue: {}
  }),
  dfsMissingBlocksValues: DS.attr('object', {
    defaultValue: {}
  }),
  dfsUnderReplicatedBlocksValues: DS.attr('object', {
    defaultValue: {}
  }),
  dfsTotalFilesValues: DS.attr('object', {
    defaultValue: {}
  }),
  workStatusValues: DS.attr('object', {
    defaultValue: {}
  }),
  healthStatusValues: function () {
    const workStatusValues = Object.keys(this.get('workStatusValues'));
    return workStatusValues.reduce((acc, key) => Object.assign({}, acc, {
      [key]: this.get('healthStatusMap')[workStatusValues[key]] || 'yellow'
    }), {});
  }.property('workStatusValues'),
  upgradeStatusValues: DS.attr('object', {
    defaultValue: {}
  }),
  safeModeStatusValues: DS.attr('object', {
    defaultValue: {}
  }),
  nameNodeRpcValues: DS.attr('object', {
    defaultValue: {}
  }),
  metricsNotAvailable: DS.attr('boolean'),
  masterComponentGroups: function () {
    let result = [];
    this.get('hostComponents').forEach(component => {
      if (component.get('componentName') === 'NAMENODE') {
        const nameSpace = component.get('haNameSpace') || 'default',
          hostName = component.get('hostName'),
          clusterId = component.get('clusterIdValue') || 'default',
          existingNameSpace = result.findProperty('name', nameSpace),
          currentNameSpace = existingNameSpace || {
              name: nameSpace,
              title: nameSpace,
              hosts: [],
              components: ['NAMENODE', 'ZKFC'],
              clusterId
            };
        if (!existingNameSpace) {
          result.push(currentNameSpace);
    }
        if (!currentNameSpace.hosts.contains(hostName)) {
          currentNameSpace.hosts.push(hostName);
        }
      }
    });
    return result.sortProperty('name');
  }.property('hostComponents.length', 'App.router.clusterController.isHDFSNameSpacesLoaded')
});

App.HDFSService.FIXTURES = [];
