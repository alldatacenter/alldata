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
var stringUtils = require('utils/string_utils');
require('utils/helper');
require('models/configs/objects/service_config_category');

var Dependency = Ember.Object.extend({
  name: Ember.computed('service', function() {
    return this.get('service.serviceName');
  }),

  displayName: Ember.computed('service', function() {
    return this.get('service.displayNameOnSelectServicePage');
  }),

  compatibleServices: function(services) {
    return services.filterProperty('serviceName', this.get('name'));
  },

  isMissing: function(selectedServices) {
    return Em.isEmpty(this.compatibleServices(selectedServices));
  }
});

var HcfsDependency = Dependency.extend({
  displayName: Ember.computed(function() {
    return Em.I18n.t('installer.step4.hcfs.displayName');
  }),

  compatibleServices: function(services) {
    return services.filterProperty("isDFS", true);
  }
});

Dependency.reopenClass({
  fromService: function(service) {
    return service.get('isDFS')
      ? HcfsDependency.create({service: service})
      : Dependency.create({service: service});
  }
});

var MissingDependency = Ember.Object.extend({
  hasMultipleOptions: Ember.computed('compatibleServices', function() {
    return this.get('compatibleServices').length > 1;
  }),

  selectFirstCompatible: function() {
    this.get('compatibleServices')[0].set('isSelected', true);
  },

  displayOptions: Ember.computed('compatibleServices', function() {
    return this.get('compatibleServices').mapProperty('serviceName').join(', ');
  })
});

App.FileSystem = Ember.ObjectProxy.extend({
  content: null,
  services: [],

  //special case for HDFS and OZONE. They can be selected together
  shouldAllSelectionBeCleared: function () {
    const dfsNames = this.get('services').mapProperty('serviceName');
    const coExistingDfs = ['HDFS', 'OZONE'],
      selectedServices = this.get('services').filterProperty('isSelected');
    for (let i = 0; i < selectedServices.length; i++) {
      if (!coExistingDfs.includes(selectedServices[i].get('serviceName'))) {
        return true;
      }
    }
    return !(dfsNames.includes('HDFS') && dfsNames.includes('OZONE') && coExistingDfs.includes(this.get('content.serviceName')));
  },

  isSelected: function(key, aBoolean) {
    if (arguments.length > 1) {
      if (this.shouldAllSelectionBeCleared()) {
        this.clearAllSelection();
      }
      this.get('content').set('isSelected', aBoolean);
    }
    return this.get('content.isSelected');
  }.property('content.isSelected', 'services.@each.isSelected'),

  clearAllSelection: function() {
    this.get('services').setEach('isSelected', false);
  }
});

/**
 * This model loads all services supported by the stack
 * The model maps to the  http://hostname:8080/api/v1/stacks/HDP/versions/${versionNumber}/services?fields=StackServices/*,serviceComponents/*
 * @type {*}
 */
App.StackService = DS.Model.extend({
  serviceName: DS.attr('string'),
  serviceType: DS.attr('string'),
  displayName: DS.attr('string'),
  comments: DS.attr('string'),
  configTypes: DS.attr('object'),
  serviceVersion: DS.attr('string'),
  serviceCheckSupported: DS.attr('boolean'),
  supportDeleteViaUi: DS.attr('boolean'),
  stackName: DS.attr('string'),
  stackVersion: DS.attr('string'),
  selection: DS.attr('string'),
  isMandatory: DS.attr('boolean', {defaultValue: false}),
  isSelected: DS.attr('boolean', {defaultValue: true}),
  isInstalled: DS.attr('boolean', {defaultValue: false}),
  isInstallable: DS.attr('boolean', {defaultValue: true}),
  isServiceWithWidgets: DS.attr('boolean', {defaultValue: false}),
  stack: DS.belongsTo('App.Stack'),
  serviceComponents: DS.hasMany('App.StackServiceComponent'),
  configs: DS.attr('array'),
  requiredServices: DS.attr('array', {defaultValue: []}),

  isDisabled: function () {
    return this.get('isInstalled') || (this.get('isMandatory') && !App.get('router.clusterInstallCompleted'));
  }.property('isMandatory', 'isInstalled', 'App.router.clusterInstallCompleted'),

  /**
   * @type {String[]}
   */
  configTypeList: function() {
    var configTypes = Object.keys(this.get('configTypes') || {});
    //Falcon has dependency on oozie-site but oozie-site advanced/custom section should not be shown on Falcon page
    if (this.get('serviceName') === 'FALCON') {
      configTypes = configTypes.without('oozie-site');
    }
    return configTypes;
  }.property('configTypes'),

  /**
   * contains array of serviceNames that have configs that
   * depends on configs from current service
   * @type {String[]}
   */
  dependentServiceNames: DS.attr('array', {defaultValue: []}),

  dependencies: function(availableServices) {
    var result = [];
    this.get('requiredServices').forEach(function(serviceName) {
      var service = availableServices.findProperty('serviceName', serviceName);
      if (service) {
        result.push(Dependency.fromService(service));
      }
    });
    return result;
  },

  /**
   * Add dependencies which are not already included in selectedServices to the given missingDependencies collection
  */
  collectMissingDependencies: function(selectedServices, availableServices, missingDependencies) {
    this._missingDependencies(selectedServices, availableServices).forEach(function (dependency) {
      this._addMissingDependency(dependency, availableServices, missingDependencies);
    }.bind(this));
  },

  _missingDependencies: function(selectedServices, availableServices) {
    return this.dependencies(availableServices).filter(function(dependency) {
      return dependency.isMissing(selectedServices);
    });
  },

  _addMissingDependency: function(dependency, availableServices, missingDependencies) {
    if(!missingDependencies.someProperty('serviceName', dependency.get('name'))) {
      missingDependencies.push(MissingDependency.create({
         serviceName: dependency.get('name'),
         displayName: dependency.get('displayName'),
         compatibleServices: dependency.compatibleServices(availableServices)
      }));
    }
  },

  // Is the service a distributed filesystem
  isDFS: function () {
    return this.get('serviceType') === 'HCFS' || ['HDFS', 'GLUSTERFS'].contains(this.get('serviceName'));
  }.property('serviceName', 'serviceType'),

  // Primary DFS. used if there is more than one DFS in a stack.
  // Only one service in the stack should be tagged as primary DFS.
  isPrimaryDFS: Em.computed.equal('serviceName', 'HDFS'),

  configTypesRendered: function () {
    var configTypes = this.get('configTypes');
    var renderedConfigTypes = $.extend(true, {}, configTypes);
    if (this.get('serviceName') == 'FALCON') {
      delete renderedConfigTypes['oozie-site'];
    }
    return renderedConfigTypes
  }.property('serviceName', 'configTypes'),

  displayNameOnSelectServicePage: function () {
    var displayName = this.get('displayName');
    var services = this.get('coSelectedServices').slice();
    var serviceDisplayNames = services.map(function (item) {
      return App.format.role(item, true);
    }, this);
    if (!!serviceDisplayNames.length) {
      serviceDisplayNames.unshift(displayName);
      displayName = serviceDisplayNames.join(" + ");
    }
    return displayName;
  }.property('coSelectedServices', 'serviceName'),

  isHiddenOnSelectServicePage: function () {
    return !this.get('isInstallable') || this.get('doNotShowAndInstall');
  }.property('isInstallable', 'doNotShowAndInstall'),

  doNotShowAndInstall: function () {
    var skipServices = [];
    if(!App.supports.installGanglia) {
      skipServices.push('GANGLIA');
    }
    return skipServices.contains(this.get('serviceName'));
  }.property('serviceName'),

  // Is the service required for monitoring of other hadoop ecosystem services
  isMonitoringService: Em.computed.existsIn('serviceName', ['GANGLIA']),

  // Is the service required for reporting host metrics
  isHostMetricsService: Em.computed.existsIn('serviceName', ['GANGLIA', 'AMBARI_METRICS']),

  // Is the service required for reporting hadoop service metrics
  isServiceMetricsService: Em.computed.existsIn('serviceName', ['GANGLIA']),

  coSelectedServices: function () {
    var coSelectedServices = App.StackService.coSelected[this.get('serviceName')];
    if (!!coSelectedServices) {
      return coSelectedServices;
    } else {
      return [];
    }
  }.property('serviceName'),

  hasClient: Em.computed.someBy('serviceComponents', 'isClient', true),

  hasMaster: Em.computed.someBy('serviceComponents', 'isMaster', true),

  hasSlave: Em.computed.someBy('serviceComponents', 'isSlave', true),

  hasNonMastersWithCustomAssignment: function () {
    var serviceComponents = this.get('serviceComponents');
    return serviceComponents.rejectProperty('isMaster').rejectProperty('cardinality', 'ALL').length > 0;
  }.property('serviceName'),

  isClientOnlyService: Em.computed.everyBy('serviceComponents', 'isClient', true),

  isNoConfigTypes: function () {
    var configTypes = this.get('configTypes');
    return !(configTypes && !!Object.keys(configTypes).length);
  }.property('configTypes'),

  customReviewHandler: function () {
    return App.StackService.reviewPageHandlers[this.get('serviceName')];
  }.property('serviceName'),

  hasHeatmapSection: Em.computed.existsIn('serviceName', ['HDFS', 'YARN', 'HBASE']),

  /**
   * configCategories are fetched from  App.StackService.configCategories.
   * Also configCategories that does not match any serviceComponent of a service and not included in the permissible default pattern are omitted
   */
  configCategories: function () {
    var configCategories = [];
    var configTypes = this.get('configTypes');
    var serviceComponents = this.get('serviceComponents');
    if (configTypes && Object.keys(configTypes).length) {
      var pattern = ["General", "ResourceType", "CapacityScheduler", "ContainerExecutor", "Registry", "FaultTolerance", "Isolation", "Performance", "HIVE_SERVER2", "KDC", "Kadmin","^Advanced", "Env$", "^Custom", "Falcon - Oozie integration", "FalconStartupSite", "FalconRuntimeSite", "MetricCollector", "Settings$", "AdvancedHawqCheck", "LogsearchAdminJson"];
      configCategories = App.StackService.configCategories.call(this).filter(function (_configCategory) {
        var serviceComponentName = _configCategory.get('name');
        var isServiceComponent = serviceComponents.someProperty('componentName', serviceComponentName);
        if (isServiceComponent) return  isServiceComponent;
        var result = false;
        pattern.forEach(function (_pattern) {
          var regex = new RegExp(_pattern);
          if (regex.test(serviceComponentName)) result = true;
        });
        return result;
      });
    }
    return configCategories;
  }.property('serviceName', 'configTypes', 'serviceComponents'),

  /**
   * Compare specified version with current service version
   * @param  {string} version [description]
   * @return {number} 0 - equal, -1 - less, +1 - more @see stringUtils#compareVersions
   */
  compareCurrentVersion: function(version) {
    var toMinor = this.get('serviceVersion').split('.').slice(0, 2).join('.');
    return stringUtils.compareVersions(toMinor, version);
  }
});

App.StackService.FIXTURES = [];

App.StackService.displayOrder = [
  'HDFS',
  'GLUSTERFS',
  'YARN',
  'MAPREDUCE2',
  'TEZ',
  'GANGLIA',
  'HIVE',
  'HAWQ',
  'PXF',
  'HBASE',
  'PIG',
  'SQOOP',
  'OOZIE',
  'ZOOKEEPER',
  'FALCON',
  'STORM',
  'FLUME',
  'ACCUMULO',
  'AMBARI_INFRA_SOLR',
  'AMBARI_METRICS',
  'ATLAS',
  'KAFKA',
  'KNOX',
  'LOGSEARCH',
  'RANGER',
  'RANGER_KMS',
  'SMARTSENSE',
  'SPARK',
  'SPARK2',
  'ZEPPELIN'
];

App.StackService.componentsOrderForService = {
  'HAWQ': ['HAWQMASTER', 'HAWQSTANDBY']
};

//@TODO: Write unit test for no two keys in the object should have any intersecting elements in their values
App.StackService.coSelected = {};


App.StackService.reviewPageHandlers = {
  'HIVE': {
    'Database': 'loadHiveDbValue'
  },
  'OOZIE': {
    'Database': 'loadOozieDbValue'
  }
};

App.StackService.configCategories = function () {
  var serviceConfigCategories = [];
  switch (this.get('serviceName')) {
    case 'HDFS':
      serviceConfigCategories.pushObject(App.ServiceConfigCategory.create({ name: 'NAMENODE', displayName: 'NameNode', showHost: true}));
      if (!App.get('isHaEnabled')) {
        serviceConfigCategories.pushObject(App.ServiceConfigCategory.create({ name: 'SECONDARY_NAMENODE', displayName: 'Secondary NameNode', showHost: true}));
      }
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'DATANODE', displayName: 'DataNode', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'}),
        App.ServiceConfigCategory.create({ name: 'NFS_GATEWAY', displayName: 'NFS Gateway', showHost: true})
      ]);
      break;
    case 'GLUSTERFS':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'YARN':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'RESOURCEMANAGER', displayName: 'Resource Manager', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'NODEMANAGER', displayName: 'Node Manager', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'APP_TIMELINE_SERVER', displayName: 'Application Timeline Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'}),
        App.ServiceConfigCategory.create({ name: 'ResourceTypes', displayName: 'Resource Types'}),
        App.ServiceConfigCategory.create({ name: 'FaultTolerance', displayName: 'Fault Tolerance'}),
        App.ServiceConfigCategory.create({ name: 'Isolation', displayName: 'Isolation'}),
        App.ServiceConfigCategory.create({ name: 'CapacityScheduler', displayName: 'Scheduler', siteFileName: 'capacity-scheduler.xml'}),
        App.ServiceConfigCategory.create({ name: 'ContainerExecutor', displayName: 'Container Executor', siteFileName: 'container-executor.xml'}),
        App.ServiceConfigCategory.create({ name: 'Registry', displayName: 'Registry'})
      ]);
      break;
    case 'MAPREDUCE2':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'HISTORYSERVER', displayName: 'History Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'HIVE':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'HIVE_METASTORE', displayName: 'Hive Metastore', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'WEBHCAT_SERVER', displayName: 'WebHCat Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'}),
        App.ServiceConfigCategory.create({ name: 'Performance', displayName: 'Performance'}),
        App.ServiceConfigCategory.create({ name: 'HIVE_SERVER2', displayName: 'Hive Server2'}),
        App.ServiceConfigCategory.create({ name: 'HIVE_CLIENT', displayName: 'Hive Client'})
      ]);
      break;
    case 'HBASE':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'HBASE_MASTER', displayName: 'HBase Master', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'HBASE_REGIONSERVER', displayName: 'RegionServer', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'ZOOKEEPER':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'ZOOKEEPER_SERVER', displayName: 'ZooKeeper Server', showHost: true})
      ]);
      break;
    case 'OOZIE':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'OOZIE_SERVER', displayName: 'Oozie Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'Falcon - Oozie integration', displayName: 'Falcon - Oozie integration'})
      ]);
      break;
    case 'FALCON':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'FALCON_SERVER', displayName: 'Falcon Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'Falcon - Oozie integration', displayName: 'Falcon - Oozie integration'}),
        App.ServiceConfigCategory.create({ name: 'FalconStartupSite', displayName: 'Falcon startup.properties'}),
        App.ServiceConfigCategory.create({ name: 'FalconRuntimeSite', displayName: 'Falcon runtime.properties'}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'STORM':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'NIMBUS', displayName: 'Nimbus', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'SUPERVISOR', displayName: 'Supervisor', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'STORM_UI_SERVER', displayName: 'Storm UI Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'STORM_REST_API', displayName: 'Storm REST API Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'DRPC_SERVER', displayName: 'DRPC Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'TEZ':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'FLUME':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'FLUME_HANDLER', displayName: 'flume.conf', siteFileName: 'flume-conf', canAddProperty: false})
      ]);
      break;
    case 'KNOX':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'KNOX_GATEWAY', displayName: 'Knox Gateway', showHost: true})
      ]);
      break;
    case 'KAFKA':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'KAFKA_BROKER', displayName: 'Kafka Broker', showHost: true})
      ]);
      break;
    case 'KERBEROS':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'KDC', displayName: 'KDC'}),
        App.ServiceConfigCategory.create({ name: 'Kadmin', displayName: 'Kadmin'}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'AMBARI_METRICS':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'}),
        App.ServiceConfigCategory.create({ name: 'MetricCollector', displayName: 'Metric Collector'})
      ]);
      break;
    case 'RANGER':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'RANGER_ADMIN', displayName: 'Admin Settings', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'DBSettings', displayName: 'DB Settings'}),
        App.ServiceConfigCategory.create({ name: 'RangerSettings', displayName: 'Ranger Settings'}),
        App.ServiceConfigCategory.create({ name: 'UnixAuthenticationSettings', displayName: 'Unix Authentication Settings'}),
        App.ServiceConfigCategory.create({ name: 'ADSettings', displayName: 'AD Settings'}),
        App.ServiceConfigCategory.create({ name: 'LDAPSettings', displayName: 'LDAP Settings'}),
        App.ServiceConfigCategory.create({ name: 'KnoxSSOSettings', displayName: 'Knox SSO Settings'})
      ]);
      break;
    case 'ACCUMULO':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'PIG':
      break;
    case 'SQOOP':
      break;
    case 'HAWQ':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'}),
        App.ServiceConfigCategory.create({ name: 'AdvancedHawqCheck', displayName: 'Advanced HAWQ Check'})
      ]);
      break;
    case 'LOGSEARCH':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'LogsearchAdminJson', displayName: 'Advanced logsearch-admin-json'})
      ]);
      break;
    default:
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
  }
  serviceConfigCategories.pushObject(App.ServiceConfigCategory.create({ name: 'Advanced', displayName: 'Advanced'}));

  var configTypes = this.get('configTypeList');

  // Add Advanced section for every configType to all the services
  configTypes.forEach(function (type) {
    serviceConfigCategories.pushObject(App.ServiceConfigCategory.create({
      name: 'Advanced ' + type,
      displayName: Em.I18n.t('common.advanced') + " " + type,
      canAddProperty: false
    }));
  }, this);

  // Add custom section for every configType to all the services
  configTypes.forEach(function (type) {
    if (Em.getWithDefault(this.get('configTypes')[type] || {}, 'supports.adding_forbidden', 'true') === 'false') {
      serviceConfigCategories.pushObject(App.ServiceConfigCategory.create({
        name: 'Custom ' + type,
        displayName: Em.I18n.t('common.custom') + " " + type,
        siteFileName: type + '.xml',
        canAddProperty: true
      }));
    }
  }, this);
  return serviceConfigCategories;
};
