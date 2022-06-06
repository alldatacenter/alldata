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
var numberUtils = require('utils/number_utils');

var ComponentDependency = Ember.Object.extend({
  componentName: null,
  compatibleComponents: [],

  /**
   * Find the first compatible component which belongs to a service that is installed
   */
  chooseCompatible: function() {
    var compatibleComponent = this.get('compatibleComponents').find(function(component) {
      return App.Service.find().someProperty('serviceName', component.get('serviceName'))
    });
    return (compatibleComponent ? compatibleComponent : this.get('compatibleComponents')[0]).get('componentName');
  }
});

/**
 * This model loads all serviceComponents supported by the stack
 * @type {*}
 */
App.StackServiceComponent = DS.Model.extend({
  componentName: DS.attr('string'),
  displayName: DS.attr('string'),
  cardinality: DS.attr('string'),
  customCommands: DS.attr('array'),
  reassignAllowed: DS.attr('boolean'),
  decommissionAllowed: DS.attr('boolean'),
  hasBulkCommandsDefinition: DS.attr('boolean'),
  bulkCommandsDisplayName: DS.attr('string'),
  bulkCommandsMasterComponentName: DS.attr('string'),
  dependencies: DS.attr('array'),
  serviceName: DS.attr('string'),
  componentCategory: DS.attr('string'),
  rollingRestartSupported: DS.attr('boolean'),
  isMaster: DS.attr('boolean'),
  isClient: DS.attr('boolean'),
  componentType: DS.attr('string'),
  stackName: DS.attr('string'),
  stackVersion: DS.attr('string'),
  stackService: DS.belongsTo('App.StackService'),
  serviceComponentId: DS.attr('number', {defaultValue: 1}), // this is used on Assign Master page for multiple masters

  /**
   * Minimum required count for installation.
   *
   * @property {Number} minToInstall
   **/
  minToInstall: function() {
    return numberUtils.getCardinalityValue(this.get('cardinality'), false);
  }.property('cardinality'),

  /**
   * Maximum required count for installation.
   *
   * @property {Number} maxToInstall
   **/
  maxToInstall: function() {
    return numberUtils.getCardinalityValue(this.get('cardinality'), true);
  }.property('cardinality'),

  /**
   * Check if the given component is compatible with this one. Having the same name or componentType indicates compatibility.
   **/
  dependsOn: function(aStackServiceComponent, opt) {
    return this.get('dependencies').some(function(each) {
      return aStackServiceComponent.compatibleWith(App.StackServiceComponent.find(each.componentName));
    });
  },

  compatibleWith: function(aStackServiceComponent) {
    return this.get('componentName') === aStackServiceComponent.get('componentName')
      || (this.get('componentType') && this.get('componentType') === aStackServiceComponent.get('componentType'));
  },

  /**
   * Collect dependencies which are required by this component but not installed.
   * A compatible installed component (e.g.: componentType=HCFS_CLIENT) is not considered as a missing dependency.
   **/
  missingDependencies: function(installedComponents, opt) {
    opt = opt || {};
    opt.scope = opt.scope || '*';
    var dependencies = this.get('dependencies');
    dependencies = opt.scope === '*' ? dependencies : dependencies.filterProperty('scope', opt.scope);
    if (dependencies.length === 0) return [];
    installedComponents = installedComponents.map(function(each) { return App.StackServiceComponent.find(each); });
    var missingComponents = dependencies.filter(function (dependency) {
      return !installedComponents.some(function(each) {
        return each.compatibleWith(App.StackServiceComponent.find(dependency.componentName));
      });
    });
    return missingComponents.map(function (missingComponent) {
      return ComponentDependency.create({
        'componentName': missingComponent.componentName,
        'type': missingComponent.type,
        'compatibleComponents': App.StackServiceComponent.find().filter(function (each) {
          return each.compatibleWith(App.StackServiceComponent.find(missingComponent.componentName));
        })
      });
    });
  },

  /** @property {Boolean} isRequired - component required to install **/
  isRequired: Em.computed.gt('minToInstall', 0),

  /** @property {Boolean} isMultipleAllowed - component can be assigned for more than one host **/
  isMultipleAllowed: Em.computed.gt('maxToInstall', 1),
  
  /** @property {Boolean} isAddableAfterInstall - component can be added after it's service was installed **/
  isAddableAfterInstall: Em.computed.gtProperties('maxToInstall', 'minToInstall'),

  /** @property {Boolean} isSlave **/
  isSlave: Em.computed.equal('componentCategory', 'SLAVE'),

  /** @property {Boolean} isRestartable - component supports restart action **/
  isRestartable: Em.computed.not('isClient'),

  /** @property {Boolean} isReassignable - component supports reassign action **/
  isReassignable: function(){
    return this.get('reassignAllowed');
  }.property('reassignAllowed'),

  /** @property {Boolean} isNonHDPComponent - component not belongs to HDP services **/
  isNonHDPComponent: function() {
    return ['METRICS_COLLECTOR', 'METRICS_MONITOR'].contains(this.get('componentName'));
  }.property('componentName'),

  /** @property {Boolean} isRollinRestartAllowed - component supports rolling restart action **/
  isRollinRestartAllowed: function() {
    return this.get('isSlave') || this.get('rollingRestartSupported');
  }.property('componentName'),

  /** @property {Boolean} isDecommissionAllowed - component supports decommission action **/
  isDecommissionAllowed: function() {
   return this.get('decommissionAllowed');
  }.property('decommissionAllowed'),

  /** @property {Boolean} isRefreshConfigsAllowed - component supports refresh configs action **/
  isRefreshConfigsAllowed: Em.computed.existsIn('componentName', ['FLUME_HANDLER']),

  /** @property {Boolean} isAddableToHost - component can be added on host details page **/
  isAddableToHost: function() {
    return (this.get('isMaster') && !this.get('isMasterAddableOnlyOnHA') && this.get('isAddableAfterInstall'))
      || ((this.get('isSlave') || this.get('isClient'))
        && (!this.get('isHAComponentOnly') || (App.get('isHaEnabled') && this.get('componentName') === 'JOURNALNODE')));
  }.property('componentName'),

  /** @property {Boolean} isDeletable - component supports delete action **/
  isDeletable: function() {
    var ignored = [];
    return (this.get('isAddableToHost') && !ignored.contains(this.get('componentName'))) || (this.get('componentName') === 'MYSQL_SERVER');
  }.property('componentName'),

  /**
   * @type {boolean}
   */
  isInstallable: function() {
    const notInstallable = App.get('currentStackName') === 'HDF' ? ['ACTIVITY_ANALYZER', 'ACTIVITY_EXPLORER'] : [];
    return !notInstallable.contains(this.get('componentName'));
  }.property('componentName'),

  /** @property {Boolean} isShownOnInstallerAssignMasterPage - component visible on "Assign Masters" step of Install Wizard **/
  // Note: Components that are not visible on Assign Master Page are not saved as part of host component recommendation/validation layout
  isShownOnInstallerAssignMasterPage: function() {
    var component = this.get('componentName');
    var mastersNotShown = ['MYSQL_SERVER', 'POSTGRESQL_SERVER', 'HIVE_SERVER_INTERACTIVE'];
    return this.get('isMaster') && this.get('isInstallable') && !mastersNotShown.contains(component);
  }.property('isMaster','componentName', 'isInstallable'),

  /** @property {Boolean} isShownOnInstallerSlaveClientPage - component visible on "Assign Slaves and Clients" step of Install Wizard**/
  // Note: Components that are not visible on Assign Slaves and Clients Page are saved as part of host component recommendation/validation layout
  isShownOnInstallerSlaveClientPage: function() {
    var component = this.get('componentName');
    var slavesNotShown = ['JOURNALNODE','ZKFC','APP_TIMELINE_SERVER'];
    return this.get('isSlave') && !this.get('isRequiredOnAllHosts') && !slavesNotShown.contains(component);
  }.property('isSlave','componentName', 'isRequiredOnAllHosts'),

  /** @property {Boolean} isShownOnAddServiceAssignMasterPage - component visible on "Assign Masters" step of Add Service Wizard **/
  // Note: Components that are not visible on Assign Master Page are not saved as part of host component recommendation/validation layout
  isShownOnAddServiceAssignMasterPage: function() {
    var isVisible = this.get('isShownOnInstallerAssignMasterPage');
    if (App.get('isHaEnabled')) {
      isVisible =  isVisible && this.get('componentName') !== 'SECONDARY_NAMENODE';
    }
    return isVisible;
  }.property('isShownOnInstallerAssignMasterPage','App.isHaEnabled'),

  /** @property {Boolean} isMasterWithMultipleInstances **/
  isMasterWithMultipleInstances: function() {
    // @todo: safe removing JOURNALNODE from masters list
    return (this.get('isMaster') && this.get('isMultipleAllowed')) || this.get('componentName') == 'JOURNALNODE';
  }.property('componentName'),

  /**
   * Master component list that could be assigned for more than 1 host.
   * Some components like NameNode and ResourceManager have range cardinality value, so they are excluded using isMasterAddableOnlyOnHA property
   *
   * @property {Boolean} isMasterAddableInstallerWizard
   **/
  isMasterAddableInstallerWizard: Em.computed.and('isMaster', 'isMultipleAllowed', '!isMasterAddableOnlyOnHA'),

  /**
   * Master components with cardinality more than 1 (n+ or n-n) that could not be added in wizards
   * New instances of these components are added in appropriate HA wizards
   * @property {Boolean} isMasterAddableOnlyOnHA
   */
  isMasterAddableOnlyOnHA: Em.computed.existsIn('componentName', ['NAMENODE', 'RESOURCEMANAGER', 'RANGER_ADMIN']),

  /** @property {Boolean} isHAComponentOnly - Components that can be installed only if HA enabled **/
  isHAComponentOnly: Em.computed.existsIn('componentName', ['ZKFC','JOURNALNODE']),

  /** @property {Boolean} isRequiredOnAllHosts - Is It require to install the components on all hosts. used in step-6 wizard controller **/
  isRequiredOnAllHosts: Em.computed.equal('minToInstall', Infinity),

  /** @property {Number} defaultNoOfMasterHosts - default number of master hosts on Assign Master page: **/
  defaultNoOfMasterHosts: function() {
     if (this.get('isMasterAddableInstallerWizard')) {
       return this.get('componentName') === 'ZOOKEEPER_SERVER' ? 3 : this.get('minToInstall');
     }
  }.property('componentName'),

  /** @property {Boolean} coHostedComponents - components that are co-hosted with this component **/
  coHostedComponents: function() {
    var componentName = this.get('componentName');
    var key, coHostedComponents = [];
    for (key in App.StackServiceComponent.coHost) {
      if (App.StackServiceComponent.coHost[key] === componentName) {
        coHostedComponents.push(key)
      }
    }
    return coHostedComponents;
  }.property('componentName'),

  /** @property {Boolean} isOtherComponentCoHosted - Is any other component co-hosted with this component **/
  isOtherComponentCoHosted: Em.computed.bool('coHostedComponents.length'),

  /** @property {Boolean} isCoHostedComponent - Is this component co-hosted with other component **/
  isCoHostedComponent: function() {
    var componentName = this.get('componentName');
    return !!App.StackServiceComponent.coHost[componentName];
  }.property('componentName'),

  /** @property {Boolean} isNotAllowedOnSingleNodeCluster - is this component allowed on single node  **/
  isNotAllowedOnSingleNodeCluster: Em.computed.existsIn('componentName', ['HAWQSTANDBY'])

});

App.StackServiceComponent.FIXTURES = [];

App.StackServiceComponent.coHost = {};
