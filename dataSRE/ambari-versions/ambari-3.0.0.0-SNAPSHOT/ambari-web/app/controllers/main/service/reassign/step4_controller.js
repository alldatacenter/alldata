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

/**
 * Additional data that is used in the `Move Component Initializers`
 *
 * @typedef {object} reassignComponentDependencies
 * @property {string} sourceHostName host where component was before moving
 * @property {string} targetHostName host where component will be after moving
 */

App.ReassignMasterWizardStep4Controller = App.HighAvailabilityProgressPageController.extend(App.WizardEnableDone, {

  name: "reassignMasterWizardStep4Controller",

  commands: [
    'stopRequiredServices',
    'cleanMySqlServer',
    'createHostComponents',
    'putHostComponentsInMaintenanceMode',
    'reconfigure',
    'installHostComponents',
    'startZooKeeperServers',
    'startNameNode',
    'stopHostComponentsInMaintenanceMode',
    'deleteHostComponents',
    'configureMySqlServer',
    'startMySqlServer',
    'startNewMySqlServer',
    'startRequiredServices'
  ],

  // custom commands for Components with DB Configuration and Check
  commandsForDB: [
    'createHostComponents',
    'installHostComponents',
    'configureMySqlServer',
    'restartMySqlServer',
    'testDBConnection',
    'stopRequiredServices',
    'cleanMySqlServer',
    'putHostComponentsInMaintenanceMode',
    'reconfigure',
    'stopHostComponentsInMaintenanceMode',
    'deleteHostComponents',
    'configureMySqlServer',
    'startRequiredServices'
  ],

  clusterDeployState: 'REASSIGN_MASTER_INSTALLING',

  multiTaskCounter: 0,

  hostComponents: [],

  dependentHostComponents: [],

  dbPropertyMap: {
    'HIVE_SERVER': {
      type: 'hive-site',
      name: 'javax.jdo.option.ConnectionDriverName'
    },
    'HIVE_METASTORE': {
      type: 'hive-site',
      name: 'javax.jdo.option.ConnectionDriverName'
    },
    'OOZIE_SERVER': {
      type: 'oozie-site',
      name: 'oozie.service.JPAService.jdbc.url'
    }
  },

  /**
   * load step info
   */
  loadStep: function () {
    var componentName = this.get('content.reassign.component_name');
    if (componentName === 'NAMENODE' && App.get('isHaEnabled')) {
      this.set('hostComponents', ['NAMENODE', 'ZKFC']);
    } else {
      this.set('hostComponents', [componentName]);
    }
    this.setDependentHostComponents(componentName);
    this.set('serviceName', [this.get('content.reassign.service_id')]);
    this._super();
  },

  /**
   * Set dependent host-components to <code>dependentHostComponents</code>
   * @param {string} componentName
   */
  setDependentHostComponents: function (componentName) {
    var installedServices = App.Service.find().mapProperty('serviceName');
    var hostInstalledComponents = App.Host.find(this.get('content.reassignHosts.target'))
        .get('hostComponents')
        .mapProperty('componentName');
    var clusterInstalledComponents = App.MasterComponent.find().toArray()
        .concat(App.ClientComponent.find().toArray())
        .concat(App.SlaveComponent.find().toArray())
        .filter(function(service){
          return service.get("totalCount") > 0;
        })
        .mapProperty('componentName');
    var dependenciesToInstall = App.StackServiceComponent.find(componentName)
        .get('dependencies')
        .filter(function (component) {
          return !(component.scope == 'host' ? hostInstalledComponents : clusterInstalledComponents).contains(component.componentName) && (installedServices.contains(component.serviceName))
            && (componentName === 'NAMENODE' ? App.get('isHaEnabled'): true);
        })
        .mapProperty('componentName');
    this.set('dependentHostComponents', dependenciesToInstall);
  },

  /**
   * concat host-component names into string
   * @return {String}
   */
  getHostComponentsNames: function () {
    var hostComponentsNames = '';
    this.get('hostComponents').forEach(function (comp, index) {
      hostComponentsNames += index ? '+' : '';
      hostComponentsNames += comp === 'ZKFC' ? comp : App.format.role(comp, false);
    }, this);
    return hostComponentsNames;
  },

  /**
   * remove unneeded tasks
   */
  removeUnneededTasks: function () {
    var componentName = this.get('content.reassign.component_name');
    if (this.isComponentWithDB()) {
      var db_type = this.get('content.databaseType');
      var is_remote_db = this.get('content.serviceProperties.is_remote_db');


      if (is_remote_db || db_type !== 'mysql') {
        this.removeTasks(['configureMySqlServer', 'startMySqlServer', 'restartMySqlServer', 'cleanMySqlServer', 'configureMySqlServer']);
      }

      if (db_type === 'derby') {
        this.removeTasks(['testDBConnection']);
      }
    }

    if (componentName !== 'MYSQL_SERVER' && !this.isComponentWithDB()) {
      this.removeTasks(['configureMySqlServer', 'startMySqlServer', 'restartMySqlServer', 'cleanMySqlServer', 'startNewMySqlServer', 'configureMySqlServer']);
    }

    if (componentName === 'MYSQL_SERVER') {
      this.removeTasks(['cleanMySqlServer']);
    }

    if (this.get('content.hasManualSteps')) {
      if (componentName === 'NAMENODE' && App.get('isHaEnabled')) {
        // Only for reassign NameNode with HA enabled
        this.removeTasks(['stopHostComponentsInMaintenanceMode', 'deleteHostComponents', 'startRequiredServices']);
      } else {
        this.removeTasks(['startZooKeeperServers', 'startNameNode', 'stopHostComponentsInMaintenanceMode', 'deleteHostComponents', 'startRequiredServices']);
      }
    } else {
      this.removeTasks(['startZooKeeperServers', 'startNameNode']);
    }

    if (!this.get('wizardController.isComponentWithReconfiguration')) {
      this.removeTasks(['reconfigure']);
    }

    if (!this.get('content.reassignComponentsInMM.length')) {
      this.removeTasks(['stopHostComponentsInMaintenanceMode']);
    }
  },

  /**
   * initialize tasks
   */
  initializeTasks: function () {
    var commands = this.get('commands');
    var currentStep = App.router.get('reassignMasterController.currentStep');
    var hostComponentsNames = this.getHostComponentsNames();

    if (this.isComponentWithDB()) {
      commands = this.get('commandsForDB');
    }

    for (var i = 0; i < commands.length; i++) {
      var TaskLabel = i === 3 ? this.get('serviceName') : hostComponentsNames; //For Reconfigure task, show serviceName
      var title = Em.I18n.t('services.reassign.step4.tasks.' + commands[i] + '.title').format(TaskLabel);
      this.get('tasks').pushObject(Ember.Object.create({
        title: title,
        status: 'PENDING',
        id: i,
        command: commands[i],
        showRetry: false,
        showRollback: false,
        name: title,
        displayName: title,
        progress: 0,
        isRunning: false,
        hosts: []
      }));
    }
    this.removeUnneededTasks();
    this.set('isLoaded', true);
  },

  hideRollbackButton: function () {
    var failedTask = this.get('tasks').findProperty('showRollback');
    if (failedTask) {
      failedTask.set('showRollback', false);
    }
  }.observes('tasks.@each.showRollback'),

  onComponentsTasksSuccess: function () {
    this.decrementProperty('multiTaskCounter');
    if (this.get('multiTaskCounter') <= 0) {
      this.onTaskCompleted();
    }
  },

  /**
   * make server call to stop services
   */
  stopRequiredServices: function () {
    var componentName = this.get('content.reassign.component_name');
    var servicesToStop = this.get('wizardController.relatedServicesMap')[componentName];
    if (this.get('content.componentsToStopAllServices').contains(componentName)) {
      this.stopServices(servicesToStop, true, true);
    } else {
      this.stopServices(servicesToStop, true);
    }
  },

  createHostComponents: function () {
    var hostComponents = this.get('hostComponents').concat(this.get('dependentHostComponents'));
    var hostName = this.get('content.reassignHosts.target');
    this.set('multiTaskCounter', hostComponents.length);
    for (var i = 0; i < hostComponents.length; i++) {
      this.createComponent(hostComponents[i], hostName, this.get('content.reassign.service_id'));
    }
  },

  onCreateComponent: function () {
    this.onComponentsTasksSuccess();
  },

  putHostComponentsInMaintenanceMode: function () {
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassignHosts.source');
    this.set('multiTaskCounter', hostComponents.length);
    for (var i = 0; i < hostComponents.length; i++) {
      App.ajax.send({
        name: 'common.host.host_component.passive',
        sender: this,
        data: {
          hostName: hostName,
          passive_state: "ON",
          componentName: hostComponents[i]
        },
        success: 'onComponentsTasksSuccess',
        error: 'onTaskError'
      });
    }
  },

  installHostComponents: function () {
    var hostComponents = this.get('hostComponents').concat(this.get('dependentHostComponents'));
    var hostName = this.get('content.reassignHosts.target');
    this.set('multiTaskCounter', hostComponents.length);
    for (var i = 0; i < hostComponents.length; i++) {
      this.updateComponent(hostComponents[i], hostName, this.get('content.reassign.service_id'), "Install", hostComponents.length);
    }
  },

  reconfigure: function () {
    var configs = this.get('content.configs'),
      attributes = this.get('content.configsAttributes'),
      secureConfigs = this.get('content.secureConfigs'),
      componentName = this.get('content.reassign.component_name');
    this.saveClusterStatus(secureConfigs, this.getComponentDir(configs, componentName));
    this.saveConfigsToServer(configs, attributes);
  },

  /**
   * make PUT call to save configs to server
   * @param configs
   * @param attributes
   */
  saveConfigsToServer: function (configs, attributes) {
    App.ajax.send({
      name: 'common.across.services.configurations',
      sender: this,
      data: {
        data: '[' + this.getServiceConfigData(configs, attributes).toString() + ']'
      },
      success: 'onSaveConfigs',
      error: 'onTaskError'
    });
  },
  /**
   * gather and format config data before sending to server
   * @param configs
   * @param attributes
   * @return {Array}
   * @method getServiceConfigData
   */
  getServiceConfigData: function (configs, attributes) {
    var componentName = this.get('content.reassign.component_name');
    var configData = Object.keys(configs).map(function (_siteName) {
      return {
        type: _siteName,
        properties: configs[_siteName],
        properties_attributes: attributes[_siteName] || {},
        service_config_version_note: Em.I18n.t('services.reassign.step4.save.configuration.note').format(App.format.role(componentName, false))
      }
    });
    var allConfigData = [];

    App.Service.find().forEach(function (service) {
      var stackService = App.StackService.find().findProperty('serviceName', service.get('serviceName'));
      if (stackService) {
        var serviceConfigData = [];
        Object.keys(stackService.get('configTypesRendered')).forEach(function (type) {
          var serviceConfigTag = configData.findProperty('type', type);
          if (serviceConfigTag) {
            serviceConfigData.pushObject(serviceConfigTag);
          }
        }, this);
        allConfigData.pushObject(JSON.stringify({
          Clusters: {
            desired_config: serviceConfigData
          }
        }));
      }
    }, this);
    return allConfigData;
  },

  /**
   * Get the web address port when RM HA is enabled.
   * @param configs
   * @param webAddressKey (http vs https)
   * */
  getWebAddressPort: function (configs, webAddressKey){
    var result = null;
    var rmWebAddressValue = configs['yarn-site'][webAddressKey];
    if(rmWebAddressValue){
      var tokens = rmWebAddressValue.split(":");
      if(tokens.length > 1){
        result = tokens[1];
        result = result.replace(/^\s+|\s+$/g, '');
      }
    }

    if(result)  //only return non-empty result
      return result;
    else
      return null;
  },

  /**
   * derive component directory from configurations
   * @param configs
   * @param componentName
   * @return {String}
   */
  getComponentDir: function (configs, componentName) {
    if (componentName === 'NAMENODE') {
      return configs['hdfs-site']['dfs.namenode.name.dir'];
    } else if (componentName === 'SECONDARY_NAMENODE') {
      return configs['hdfs-site']['dfs.namenode.checkpoint.dir'];
    }
    return '';
  },

  /**
   * save cluster status to server
   *
   * @param secureConfigs
   * @param componentDir
   * @return {Boolean}
   */
  saveClusterStatus: function (secureConfigs, componentDir) {
    if (componentDir || secureConfigs.length) {
      App.router.get(this.get('content.controllerName')).saveComponentDir(componentDir);
      App.router.get(this.get('content.controllerName')).saveSecureConfigs(secureConfigs);
      App.clusterStatus.setClusterStatus({
        clusterName: this.get('content.cluster.name'),
        clusterState: this.get('clusterDeployState'),
        wizardControllerName: this.get('content.controllerName'),
        localdb: App.db.data
      });
      return true;
    }
    return false;
  },

  onSaveConfigs: function () {
    this.onTaskCompleted();
  },

  startZooKeeperServers: function () {
    var components = this.get('content.masterComponentHosts').filterProperty('component', 'ZOOKEEPER_SERVER');
    this.updateComponent('ZOOKEEPER_SERVER', components.mapProperty('hostName'), "ZOOKEEPER", "Start");
  },

  /**
   * Start the namenode that is not being moved (applicable only in NameNode HA environment)
   * @private {startNameNode}
   */
  startNameNode: function () {
    var components = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE');
    var sourceHost =  this.get('content.reassignHosts.source');
    var targetHost =  this.get('content.reassignHosts.target');
    var hostname = components.mapProperty('hostName').without(sourceHost).without(targetHost);
    this.updateComponent('NAMENODE', hostname, "HDFS", "Start");
  },

  /**
   * make server call to start services
   */
  startRequiredServices: function () {
    var relatedServices = this.get('wizardController.relatedServicesMap')[this.get('content.reassign.component_name')];
    if (relatedServices) {
      this.startServices(false, relatedServices, true);
    } else {
      this.startServices(true);
    }
  },

  /**
   * make DELETE call for each host component on host
   */
  deleteHostComponents: function () {
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassignHosts.source');
    this.set('multiTaskCounter', hostComponents.length);
    for (var i = 0; i < hostComponents.length; i++) {
      App.ajax.send({
        name: 'common.delete.host_component',
        sender: this,
        data: {
          hostName: hostName,
          componentName: hostComponents[i]
        },
        success: 'onComponentsTasksSuccess',
        error: 'onDeleteHostComponentsError'
      });
    }
  },

  onDeleteHostComponentsError: function (error) {
    if (error.responseText.indexOf('org.apache.ambari.server.controller.spi.NoSuchResourceException') !== -1) {
      this.onComponentsTasksSuccess();
    } else {
      this.onTaskError();
    }
  },

  done: function () {
    if (!this.get('isSubmitDisabled')) {
      this.removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
      if (this.get('content.hasManualSteps')) {
        App.router.send('next');
      } else {
        App.router.send('complete');
      }
    }
  },

  /**
   * make server call to clean MYSQL
   */
  cleanMySqlServer: function () {
    var hostname = App.HostComponent.find().findProperty('componentName', 'MYSQL_SERVER').get('hostName');

    if (this.get('content.reassign.component_name') === 'MYSQL_SERVER') {
      hostname = this.get('content.reassignHosts.target');
    }

    App.ajax.send({
      name: 'service.mysql.clean',
      sender: this,
      data: {
        host: hostname
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  /**
   * make server call to configure MYSQL
   */
  configureMySqlServer : function () {
    var hostname = App.HostComponent.find().findProperty('componentName', 'MYSQL_SERVER').get('hostName');

    if (this.get('content.reassign.component_name') === 'MYSQL_SERVER') {
      hostname = this.get('content.reassignHosts.target');
    }

    App.ajax.send({
      name: 'service.mysql.configure',
      sender: this,
      data: {
        host: hostname
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  startMySqlServer: function() {
    App.ajax.send({
      name: 'common.host.host_component.update',
      sender: this,
      data: {
        context: "Start MySQL Server",
        hostName: App.HostComponent.find().findProperty('componentName', 'MYSQL_SERVER').get('hostName'),
        serviceName: "HIVE",
        componentName: "MYSQL_SERVER",
        HostRoles: {
          state: "STARTED"
        }
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  restartMySqlServer: function() {
    var context = "Restart MySql Server";

    var resource_filters = {
      component_name: "MYSQL_SERVER",
      hosts: App.HostComponent.find().filterProperty('componentName', 'MYSQL_SERVER').get('firstObject.hostName'),
      service_name: "HIVE"
    };

    var operation_level = {
      level: "HOST_COMPONENT",
      cluster_name: this.get('content.cluster.name'),
      service_name: "HIVE",
      hostcomponent_name: "MYSQL_SERVER"
    };

    App.ajax.send({
      name: 'restart.hostComponents',
      sender: this,
      data: {
        context: context,
        resource_filters: [resource_filters],
        operation_level: operation_level
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  startNewMySqlServer: function() {
    App.ajax.send({
      name: 'common.host.host_component.update',
      sender: this,
      data: {
        context: "Start MySQL Server",
        hostName: this.get('content.reassignHosts.target'),
        serviceName: "HIVE",
        componentName: "MYSQL_SERVER",
        HostRoles: {
          state: "STARTED"
        }
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  testDBConnection: function() {
    this.prepareDBCheckAction();
  },

  isComponentWithDB: function() {
    return ['HIVE_SERVER', 'HIVE_METASTORE', 'OOZIE_SERVER'].contains(this.get('content.reassign.component_name'));
  },

  /** @property {Object} propertiesPattern - check pattern according to type of connection properties **/
  propertiesPattern: function() {
    return {
      user_name: /(username|dblogin)$/ig,
      user_passwd: /(dbpassword|password)$/ig,
      db_connection_url: /jdbc\.url|connectionurl/ig,
      driver_class: /ConnectionDriverName|jdbc\.driver/ig,
      schema_name: /db\.schema\.name/ig
    };
  }.property(),

  /** @property {Object} connectionProperties - service specific config values mapped for custom action request **/
  connectionProperties: function() {
    var propObj = {};
    for (var key in this.get('propertiesPattern')) {
      propObj[key] = this.getConnectionProperty(this.get('propertiesPattern')[key]);
    }
    return propObj;
  }.property('propertiesPattern'),

  getConnectionProperty: function(regexp) {
    var configType = this.get('requiredProperties.type'),
      propertyName = this.get('requiredProperties.names').filter(function(item) {
      return regexp.test(item);
    })[0];
    return Em.getWithDefault(this.get('content.configs'), configType, {})[propertyName];
  },

  /**
   * Properties that stores in local storage used for handling
   * last success connection.
   *
   * @property {Object} preparedDBProperties
   **/
  preparedDBProperties: function() {
    var propObj = {};
    for (var key in this.get('propertiesPattern')) {
      var propValue = this.getConnectionProperty(this.get('propertiesPattern')[key]);
      propObj[key] = propValue;
    }
    return propObj;
  }.property(),

  /** @property {object} requiredProperties - properties that necessary for database connection **/
  requiredProperties: function() {
    var propertiesMap = {
      OOZIE: {
        type: 'oozie-site',
        names: ['oozie.db.schema.name', 'oozie.service.JPAService.jdbc.username', 'oozie.service.JPAService.jdbc.password', 'oozie.service.JPAService.jdbc.driver', 'oozie.service.JPAService.jdbc.url']
      },
      HIVE: {
        type: 'hive-site',
        names: ['ambari.hive.db.schema.name', 'javax.jdo.option.ConnectionUserName', 'javax.jdo.option.ConnectionPassword', 'javax.jdo.option.ConnectionDriverName', 'javax.jdo.option.ConnectionURL']
      }
    };

    return propertiesMap[this.get('content.reassign.service_id')];
  }.property(),

  dbType: function() {
    var databaseTypes = /MySQL|PostgreS|Oracle|Derby|MSSQL|Anywhere/gi,
      dbPropertyMapItem = Em.getWithDefault(this.get('dbPropertyMap'), this.get('content.reassign.component_name'), null),
      databasePropMatch,
      databaseProp,
      result;

    if (dbPropertyMapItem) {
      databaseProp = Em.getWithDefault(this.get('content.configs'), dbPropertyMapItem.type, {})[dbPropertyMapItem.name];
      databasePropMatch = databaseProp && databaseProp.match(databaseTypes);
      if (databasePropMatch) {
        result = databasePropMatch[0];
      }
    }

    return result;
  }.property(),

  prepareDBCheckAction: function() {
    var params = this.get('preparedDBProperties');

    var ambariProperties = App.router.get('clusterController.ambariProperties');

    params['db_name'] = this.get('dbType');
    params['jdk_location'] = ambariProperties['jdk_location'];
    params['jdk_name'] = ambariProperties['jdk.name'];
    params['java_home'] = ambariProperties['java.home'];

    params['threshold'] = 60;
    params['ambari_server_host'] = location.hostname;
    params['check_execute_list'] = "db_connection_check";

    App.ajax.send({
      name: 'cluster.custom_action.create',
      sender: this,
      data: {
        requestInfo: {
          "context": "Check host",
          "action": "check_host",
          "parameters": params
        },
        filteredHosts: [this.get('content.reassignHosts.target')]
      },
      success: 'onCreateActionSuccess',
      error: 'onTaskError'
    });
  },

  onCreateActionSuccess: function(data) {
    this.set('checkDBRequestId', data.Requests.id);
    App.ajax.send({
      name: 'custom_action.request',
      sender: this,
      data: {
        requestId: this.get('checkDBRequestId')
      },
      success: 'setCheckDBTaskId'
    });
  },

  setCheckDBTaskId: function(data) {
    this.set('checkDBTaskId', data.items[0].Tasks.id);
    this.startDBCheckPolling();
  },

  startDBCheckPolling: function() {
      this.getDBConnTaskInfo();
  },

  getDBConnTaskInfo: function() {
    this.setTaskStatus(this.get('currentTaskId'), 'IN_PROGRESS');
    this.get('tasks').findProperty('id', this.get('currentTaskId')).set('progress', 100);

    this.set('logs', []);
    App.ajax.send({
      name: 'custom_action.request',
      sender: this,
      data: {
        requestId: this.get('checkDBRequestId'),
        taskId: this.get('checkDBTaskId')
      },
      success: 'getDBConnTaskInfoSuccess'
    });
  },

  getDBConnTaskInfoSuccess: function(data) {
    var task = data.Tasks;
    if (task.status === 'COMPLETED') {
      var structuredOut = task.structured_out.db_connection_check;
      if (structuredOut.exit_code != 0) {
        this.showConnectionErrorPopup(structuredOut.message);
        this.onTaskError();
      } else {
        this.onTaskCompleted();
      }
    }

    if (task.status === 'FAILED') {
      this.onTaskError();
    }

    if (/PENDING|QUEUED|IN_PROGRESS/.test(task.status)) {
      Em.run.later(this, function() {
        this.startDBCheckPolling();
      }, 3000);
    }
  },

  showConnectionErrorPopup: function(error) {
    var popup = App.showAlertPopup('Database Connection Error');
    popup.set('body', error);
  },

  testDBRetryTooltip: function() {
    var db_host = this.get('content.serviceProperties.database_hostname');
    var db_type = this.get('dbType');
    var db_props = this.get('preparedDBProperties');

    return Em.I18n.t('services.reassign.step4.tasks.testDBConnection.tooltip').format(
      db_host, db_type, db_props['schema_name'], db_props['user_name'],
      db_props['user_passwd'], db_props['driver_class'], db_props['db_connection_url']
    );
  }.property('dbProperties'),

  saveServiceProperties: function(configs) {
    App.router.get(this.get('content.controllerName')).saveServiceProperties(configs);
  },

  stopHostComponentsInMaintenanceMode: function () {
    var hostComponentsInMM = this.get('content.reassignComponentsInMM');
    var hostName = this.get('content.reassignHosts.source');
    var serviceName = this.get('content.reassign.service_id');
    hostComponentsInMM = hostComponentsInMM.map(function(componentName){
      return {
        hostName: hostName,
        serviceName: serviceName,
        componentName: componentName
      };
    });
    this.set('multiTaskCounter', hostComponentsInMM.length);
    this.updateComponentsState(hostComponentsInMM, 'INSTALLED');
  }

});
