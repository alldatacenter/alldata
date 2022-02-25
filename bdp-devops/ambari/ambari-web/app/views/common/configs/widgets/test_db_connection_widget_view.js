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

require('views/common/controls_view');

var App = require('app');
var dbUtils = require('utils/configs/database');

//@Todo merge with CheckDBConnectionView
App.TestDbConnectionWidgetView = App.ConfigWidgetView.extend({
  templateName: require('templates/common/configs/widgets/test_db_connection_widget'),
  classNames: ['widget'],
  dbInfo: require('data/db_properties_info'),

  /** @property {string} btnCaption - text for button **/
  btnCaption: Em.computed.alias('config.stackConfigProperty.widget.display-name'),
  /** @property {string} responseCaption - text for status link **/
  responseCaption: null,
  /** @property {boolean} isConnecting - is request to server activated **/
  isConnecting: false,
  /** @property {boolean} isValidationPassed - check validation for required fields **/
  isValidationPassed: null,
  /** @property {string} db_type- name of current database **/
  db_type: null,
  /** @property {string} db_type_label - label of current database **/
  db_type_label: null,
  /** @property {boolean} isRequestResolved - check for finished request to server **/
  isRequestResolved: false,
  /** @property {boolean} isConnectionSuccess - check for successful connection to database **/
  isConnectionSuccess: null,
  /** @property {string} responseFromServer - message from server response **/
  responseFromServer: null,
  /** @property {Object} ambariRequiredProperties - properties that need for custom action request **/
  ambariRequiredProperties: null,
  /** @property {Number} currentRequestId - current custom action request id **/
  currentRequestId: null,
  /** @property {Number} currentTaskId - current custom action task id **/
  currentTaskId: null,
  /** @property {jQuery.Deferred} request - current $.ajax request **/
  request: null,
  /** @property {Number} pollInterval - timeout interval for ajax polling **/
  pollInterval: 3000,
  /** @property {Object} logsPopup - popup with DB connection check info **/
  logsPopup: null,
  /** @property {Array} or {String} masterHostName: The name of hosts from which the db connection will happen**/
  masterHostName: null,
  /** @property {String} db_connection_url: The jdbc urlfor performing db connection**/
  db_connection_url: null,
  /** @property {String} user_name: The user name to be used for performing db connection**/
  user_name: null,
  /** @property {String} user_passwd: password for the  user name to be used for performing db connection**/
  user_passwd: null,

  someRequiredPropertyIsInvalid: Em.computed.someBy('requiredProperties', 'isValid', false),
  /** @property {boolean} isBtnDisabled - disable button on failed validation or active request **/
  isBtnDisabled: Em.computed.or('someRequiredPropertyIsInvalid', 'isConnecting'),
  /** @property {object} requiredProperties - properties that necessary for database connection **/
  requiredProperties: [],

  // define if label of selected database contains "new"
  isNewSelected: function () {
    return /new/i.test(this.get('db_type_label.value'));
  }.property('db_type_label.value'),

  /** Check validation and load ambari properties **/
  didInsertElement: function () {
    var requiredProperties = this.get('config.stackConfigProperty.widget.required-properties');
    var serviceName = this.get('config.serviceName');
    var serviceConfigs = this.get('controller.stepConfigs').findProperty('serviceName',serviceName).get('configs');
    var requiredServiceConfigs = Object.keys(requiredProperties).map(function(key){
      var split = requiredProperties[key].split('/');
      var fileName =  split[0] + '.xml';
      var configName = split[1];
      var requiredConfig = serviceConfigs.filterProperty('filename',fileName).findProperty('name', configName);
      if (!requiredConfig) {
        var componentName = App.config.getComponentName(configName);
        var stackComponent = App.StackServiceComponent.find(componentName);
        if (stackComponent && stackComponent.get('componentName')) {
          var value = this.get('controller').getComponentHostValue(componentName,
            this.get('controller.wizardController.content.masterComponentHosts'),
            this.get('controller.wizardController.content.slaveComponentHosts'));
          var hProperty = App.config.createHostNameProperty(serviceName, componentName, value, stackComponent);
          return App.ServiceConfigProperty.create(hProperty);
        }
      } else {
        return requiredConfig;
      }
    }, this);

    this.set('requiredProperties', requiredServiceConfigs);
    this.setDbProperties(requiredProperties);
    this.getAmbariProperties();
  },

  /** On view destroy **/
  willDestroyElement: function () {
    this.set('isConnecting', false);
    this._super();
  },


  /**
   *  This function is used to set Database name and master host name
   * @param requiredProperties: `config.stackConfigProperty.widget.required-properties` as stated in the theme
   */
  setDbProperties: function(requiredProperties) {
    var dbProperties = {
      'db.connection.source.host' : 'masterHostName',
      'db.type' : 'db_type',
      'db.connection.user': 'user_name',
      'db.connection.password': 'user_passwd',
      'jdbc.driver.url': 'db_connection_url',
      'db.type.label': 'db_type_label'
    };

    for (var key in dbProperties) {
      var masterHostNameProperty = requiredProperties[key];
      if (masterHostNameProperty) {
        var split = masterHostNameProperty.split('/');
        var fileName = split[0] + '.xml';
        var configName = split[1];
        var dbConfig = this.get('requiredProperties').filterProperty('filename', fileName).findProperty('name', configName);
        this.set(dbProperties[key], dbConfig);
      }
    }
  },

  /**
   * Set up ambari properties required for custom action request
   *
   * @method getAmbariProperties
   **/
  getAmbariProperties: function () {
    var clusterController = App.router.get('clusterController');
    var _this = this;
    if (!App.isEmptyObject(App.db.get('tmp', 'ambariProperties')) && !this.get('ambariProperties')) {
      this.set('ambariProperties', App.db.get('tmp', 'ambariProperties'));
      return;
    }
    if (App.isEmptyObject(clusterController.get('ambariProperties'))) {
      clusterController.loadAmbariProperties().done(function (data) {
        _this.formatAmbariProperties(data.RootServiceComponents.properties);
      });
    } else {
      this.formatAmbariProperties(clusterController.get('ambariProperties'));
    }
  },

  formatAmbariProperties: function (properties) {
    var defaults = {
      threshold: "60",
      ambari_server_host: location.hostname,
      check_execute_list: "db_connection_check"
    };
    var properties = App.permit(properties, ['jdk.name', 'jdk_location', 'java.home']);
    var renameKey = function (oldKey, newKey) {
      if (properties[oldKey]) {
        defaults[newKey] = properties[oldKey];
        delete properties[oldKey];
      }
    };
    renameKey('java.home', 'java_home');
    renameKey('jdk.name', 'jdk_name');
    $.extend(properties, defaults);
    App.db.set('tmp', 'ambariProperties', properties);
    this.set('ambariProperties', properties);
  },
  /**
   * `Action` method for starting connect to current database.
   *
   * @method connectToDatabase
   **/
  connectToDatabase: function () {
    if (this.get('isBtnDisabled')) return;
    this.set('isRequestResolved', false);
    App.db.set('tmp', this.get('parentView.service.serviceName') + '_connection', {});
    this.setConnectingStatus(true);
    if (App.get('testMode')) {
      this.startPolling();
    } else {
      this.runCheckConnection();
    }
  },

  /**
   * runs check connections methods depending on service
   * @return {void}
   * @method runCheckConnection
   */
  runCheckConnection: function () {
    this.createCustomAction();
  },


  /**
   * Run custom action for database connection.
   *
   * @method createCustomAction
   **/
  createCustomAction: function () {
    var connectionProperties = this.getProperties('db_connection_url','user_name', 'user_passwd');
    var db_name = this.dbInfo.dpPropertiesMap[dbUtils.getDBType(this.get('db_type').value)].db_type;
    var isServiceInstalled = App.Service.find(this.get('config.serviceName')).get('isLoaded');
    for (var key in connectionProperties) {
      if (connectionProperties.hasOwnProperty(key)) {
        connectionProperties[key] = connectionProperties[key].value;
      }
    }
    var params = $.extend(true, {}, {db_name: db_name}, connectionProperties, this.get('ambariProperties'));
    var filteredHosts =  Array.isArray(this.get('masterHostName.value')) ? this.get('masterHostName.value') : [this.get('masterHostName.value')];
    App.ajax.send({
      name: (isServiceInstalled) ? 'cluster.custom_action.create' : 'custom_action.create',
      sender: this,
      data: {
        requestInfo: {
          parameters: params
        },
        filteredHosts: filteredHosts
      },
      success: 'onCreateActionSuccess',
      error: 'onCreateActionError'
    });
  },
  /**
   * Run updater if task is created successfully.
   *
   * @method onConnectActionS
   **/
  onCreateActionSuccess: function (data) {
    this.set('currentRequestId', data.Requests.id);
    App.ajax.send({
      name: 'custom_action.request',
      sender: this,
      data: {
        requestId: this.get('currentRequestId')
      },
      success: 'setCurrentTaskId'
    });
  },

  setCurrentTaskId: function (data) {
    this.set('currentTaskId', data.items[0].Tasks.id);
    this.startPolling();
  },

  startPolling: function () {
    if (this.get('isConnecting'))
      this.getTaskInfo();
  },

  getTaskInfo: function () {
    var request = App.ajax.send({
      name: 'custom_action.request',
      sender: this,
      data: {
        requestId: this.get('currentRequestId'),
        taskId: this.get('currentTaskId')
      },
      success: 'getTaskInfoSuccess'
    });
    this.set('request', request);
  },

  preparedDBProperties: function() {
    var propObj = {};
    var serviceName = this.get('config.serviceName');
    var serviceConfigs = this.get('controller.stepConfigs').findProperty('serviceName',serviceName).get('configs');
    for (var key in this.get('propertiesPattern')) {
      var propName = this.getConnectionProperty(this.get('propertiesPattern')[key], true);
      propObj[propName] = serviceConfigs.findProperty('name', propName).get('value');
    }
    return propObj;
  }.property(),

  requiredProps: function() {
    var ranger = App.StackService.find().findProperty('serviceName', 'RANGER');
    var propertiesMap = {
      OOZIE: ['oozie.db.schema.name', 'oozie.service.JPAService.jdbc.username', 'oozie.service.JPAService.jdbc.password', 'oozie.service.JPAService.jdbc.driver', 'oozie.service.JPAService.jdbc.url'],
      HIVE: ['ambari.hive.db.schema.name', 'javax.jdo.option.ConnectionUserName', 'javax.jdo.option.ConnectionPassword', 'javax.jdo.option.ConnectionDriverName', 'javax.jdo.option.ConnectionURL'],
      KERBEROS: ['kdc_hosts'],
      RANGER: ranger && ranger.compareCurrentVersion('0.5') > -1 ?
        ['db_user', 'db_password', 'db_name', 'ranger.jpa.jdbc.url', 'ranger.jpa.jdbc.driver'] :
        ['db_user', 'db_password', 'db_name', 'ranger_jdbc_connection_url', 'ranger_jdbc_driver'],
      RANGER_KMS: ['db_user', 'db_password', 'ranger.ks.jpa.jdbc.url', 'ranger.ks.jpa.jdbc.driver']
    };
    return propertiesMap[this.get('parentView.content.serviceName')];
  }.property(),

  getConnectionProperty: function (regexp, isGetName) {
    var serviceName = this.get('config.serviceName');
    var serviceConfigs = this.get('controller.stepConfigs').findProperty('serviceName',serviceName).get('configs');
    var propertyName = this.get('requiredProps').filter(function (item) {
      return regexp.test(item);
    })[0];
    return (isGetName) ? propertyName : serviceConfigs.findProperty('name', propertyName).get('value');
  },

  propertiesPattern: function() {
    var patterns = {
      db_connection_url: /jdbc\.url|connection_url|connectionurl|kdc_hosts/ig
    };
    if (this.get('parentView.service.serviceName') != "KERBEROS") {
      patterns.user_name = /(username|dblogin|db_user)$/ig;
      patterns.user_passwd = /(dbpassword|password|db_password)$/ig;
    }
    return patterns;
  }.property('parentView.service.serviceName'),

  getTaskInfoSuccess: function (data) {
    var task = data.Tasks;
    this.set('responseFromServer', {
      stderr: task.stderr,
      stdout: task.stdout
    });
    if (task.status === 'COMPLETED') {
      var structuredOut = task.structured_out.db_connection_check;
      if (structuredOut.exit_code != 0) {
        this.set('responseFromServer', {
          stderr: task.stderr,
          stdout: task.stdout,
          structuredOut: structuredOut.message
        });
        this.setResponseStatus('failed');
      } else {
        App.db.set('tmp', this.get('parentView.service.serviceName') + '_connection', this.get('preparedDBProperties'));
        this.setResponseStatus('success');
      }
    }
    if (task.status === 'FAILED') {
      this.setResponseStatus('failed');
    }
    if (/PENDING|QUEUED|IN_PROGRESS/.test(task.status)) {
      Em.run.later(this, function () {
        this.startPolling();
      }, this.get('pollInterval'));
    }
  },

  onCreateActionError: function (jqXhr, status, errorMessage) {
    this.setResponseStatus('failed');
    this.set('responseFromServer', errorMessage);
  },

  setResponseStatus: function (isSuccess) {
    var db_type = this.dbInfo.dpPropertiesMap[dbUtils.getDBType(this.get('db_type').value)].db_type.toUpperCase();
    var isSuccess = isSuccess == 'success';
    this.setConnectingStatus(false);
    this.set('responseCaption', isSuccess ? Em.I18n.t('services.service.config.database.connection.success') : Em.I18n.t('services.service.config.database.connection.failed'));
    this.set('isConnectionSuccess', isSuccess);
    this.set('isRequestResolved', true);
    if (this.get('logsPopup')) {
      var statusString = isSuccess ? 'common.success' : 'common.error';
      this.set('logsPopup.header', Em.I18n.t('services.service.config.connection.logsPopup.header').format(db_type, Em.I18n.t(statusString)));
    }
  },
  /**
   * Switch captions and statuses for active/non-active request.
   *
   * @method setConnectionStatus
   * @param {Boolean} [active]
   */
  setConnectingStatus: function (active) {
    if (active) {
      this.set('responseCaption', Em.I18n.t('services.service.config.database.connection.inProgress'));
    }
    this.set('controller.testConnectionInProgress', !!active);
    this.set('btnCaption', !!active ? Em.I18n.t('services.service.config.database.btn.connecting') : Em.I18n.t('services.service.config.database.btn.idle'));
    this.set('isConnecting', !!active);
  },
  /**
   * Set view to init status.
   *
   * @method restore
   **/
  restore: function () {
    if (this.get('request')) {
      this.get('request').abort();
      this.set('request', null);
    }
    this.set('responseCaption', null);
    this.set('responseFromServer', null);
    this.setConnectingStatus(false);
    this.set('isRequestResolved', false);
  },
  /**
   * `Action` method for showing response from server in popup.
   *
   * @method showLogsPopup
   **/
  showLogsPopup: function () {
    if (this.get('isConnectionSuccess')) return;
    var _this = this;
    var db_type = this.dbInfo.dpPropertiesMap[dbUtils.getDBType(this.get('db_type').value)].db_type.toUpperCase();
    var statusString = this.get('isRequestResolved') ? 'common.error' : 'common.testing';
    var popup = App.showAlertPopup(Em.I18n.t('services.service.config.connection.logsPopup.header').format(db_type, Em.I18n.t(statusString)), null, function () {
      _this.set('logsPopup', null);
    });
    popup.reopen({
      onClose: function () {
        this._super();
        _this.set('logsPopup', null);
      }
    });
    if (typeof this.get('responseFromServer') == 'object') {
      popup.set('bodyClass', Em.View.extend({
        checkDBConnectionView: _this,
        templateName: require('templates/common/error_log_body'),
        openedTask: function () {
          return this.get('checkDBConnectionView.responseFromServer');
        }.property('checkDBConnectionView.responseFromServer.stderr', 'checkDBConnectionView.responseFromServer.stdout', 'checkDBConnectionView.responseFromServer.structuredOut')
      }));
    } else {
      popup.set('body', this.get('responseFromServer'));
    }
    this.set('logsPopup', popup);
    return popup;
  }
});
