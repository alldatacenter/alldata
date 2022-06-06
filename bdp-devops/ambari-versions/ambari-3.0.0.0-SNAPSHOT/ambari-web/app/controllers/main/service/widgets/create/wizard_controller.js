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

App.WidgetWizardController = App.WizardController.extend({

  name: 'widgetWizardController',

  totalSteps: 3,

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,


  content: Em.Object.create({
    controllerName: 'widgetWizardController',
    widgetService: null,
    widgetType: "",
    isMetricsLoaded: false,

    /**
     * @type {Object}
     * @default null
     */
    layout: null,

    /**
     * Example:
     * {
     *  "display_unit": "%",
     *  "warning_threshold": 70,
     *  "error_threshold": 90
     * }
     */
    widgetProperties: {},

    /**
     * Example:
     * [{
     *  widget_id: "metrics/rpc/closeRegion_num_ops",
     *  name: "rpc.rpc.closeRegion_num_ops",
     *  pointInTime: true,
     *  temporal: true,
     *  category: "default"
     *  serviceName: "HBASE"
     *  componentName: "HBASE_CLIENT"
     *  type: "GANGLIA"//or JMX
     *  level: "COMPONENT"//or HOSTCOMPONENT
     * }]
     * @type {Array}
     */
    allMetrics: [],

    /**
     * Example:
     * [{
     *  "name": "regionserver.Server.percentFilesLocal",
     *  "serviceName": "HBASE",
     *  "componentName": "HBASE_REGIONSERVER"
     * }]
     */
    widgetMetrics: [],

    /**
     * Example:
     * [{
     *  "name": "Files Local",
     *  "value": "${regionserver.Server.percentFilesLocal}"
     * }]
     */
    widgetValues: [],
    widgetName: "",
    widgetDescription: "",
    widgetAuthor: Em.computed.alias('App.router.loginName'),
    widgetScope: null
  }),

  loadMap: {
    '1': [
      {
        type: 'sync',
        callback: function () {
          this.load('widgetService');
          this.load('widgetType');
          this.load('layout', true);
        }
      }
    ],
    '2': [
      {
        type: 'sync',
        callback: function () {
          this.load('widgetProperties', true);
          this.load('widgetValues', true);
          this.load('widgetMetrics', true);
          this.load('expressions', true);
          this.load('dataSets', true);
          this.load('templateValue', true);
        }
      },
      {
        type: 'async',
        callback: function () {
          return this.loadAllMetrics();
        }
      }
    ]
  },

  /**
   * set current step
   * @param {string} currentStep
   * @param {boolean} completed
   * @param {boolean} skipStateSave
   */
  setCurrentStep: function (currentStep, completed, skipStateSave) {
    this._super(currentStep, completed);
    if (App.get('testMode') || skipStateSave) {
      return;
    }
    this.saveClusterStatus('WIDGET_DEPLOY');
  },

  setStepsEnable: function () {
    for (var i = 1; i <= this.get('totalSteps'); i++) {
      var step = this.get('isStepDisabled').findProperty('step', i);
      if (i <= this.get('currentStep') && App.get('router.clusterController.isLoaded')) {
        step.set('value', false);
      } else {
        step.set('value', i != this.get('currentStep'));
      }
    }
  }.observes('currentStep', 'App.router.clusterController.isLoaded'),


  /**
   * save status of the cluster.
   * @param {object} clusterStatus
   */
  saveClusterStatus: function (clusterStatus) {
    App.clusterStatus.setClusterStatus({
      clusterState: clusterStatus,
      wizardControllerName: this.get('name'),
      localdb: App.db.data
    });
  },

  /**
   * save wizard properties to controller and localStorage
   * @param {string} name
   * @param value
   */
  save: function (name, value) {
    this.set('content.' + name, value);
    this._super(name);
  },

  /**
   * load widget metrics
   * on resolve deferred return array of widget metrics
   * @returns {$.Deferred}
   */
  loadAllMetrics: function () {
    var allMetrics = this.getDBProperty('allMetrics');
    var self = this;
    this.set("content.isMetricsLoaded", false);
    var dfd = $.Deferred();

    if (allMetrics.length === 0) {
      this.loadAllMetricsFromServer(function () {
        self.set("content.isMetricsLoaded", true);
        dfd.resolve(self.get('content.allMetrics'));
      });
    } else {
      this.set("content.isMetricsLoaded", true);
      this.set('content.allMetrics', allMetrics);
      dfd.resolve(allMetrics);
    }
    return dfd.promise();
  },

  /**
   * load metrics from server
   * @param {function} callback
   * @returns {$.ajax}
   */
  loadAllMetricsFromServer: function (callback) {
    return App.ajax.send({
      name: 'widgets.wizard.metrics.get',
      sender: this,
      data: {
        stackVersionURL: App.get('stackVersionURL'),
        serviceNames: App.Service.find().filter(function (item) {
          return App.StackService.find(item.get('id')).get('isServiceWithWidgets');
        }).mapProperty('serviceName').join(',')
      },
      callback: callback,
      success: 'loadAllMetricsFromServerCallback'
    });
  },

  /**
   *
   * @param {object} json
   */
  loadAllMetricsFromServerCallback: function (json) {
    var self = this;
    var result = [];
    var metrics = {};
    var slaveComponents = App.StackServiceComponent.find().filterProperty('isSlave').mapProperty('componentName');
    if (json) {
      json.items.forEach(function (service) {
        var data = service.artifacts[0].artifact_data[service.StackServices.service_name];
        for (var componentName in data) {
          var isSlave = slaveComponents.contains(componentName);
          for (var level in data[componentName]) {
            var metricTypes = data[componentName][level]; //Ganglia or JMX
            metricTypes.forEach(function (_metricType) {
              metrics = _metricType['metrics']['default'];
              var type = _metricType["type"].toUpperCase();
              if (!((type === 'JMX' && level.toUpperCase() === 'COMPONENT') || (isSlave && level.toUpperCase() === 'HOSTCOMPONENT'))) {
                for (var widgetId in metrics) {
                  var _metrics = metrics[widgetId];
                  var metricObj = {
                    widget_id: widgetId,
                    point_in_time: _metrics.pointInTime,
                    temporal: _metrics.temporal,
                    name: _metrics.name,
                    level: level.toUpperCase(),
                    type: type,
                    component_name: componentName,
                    service_name: service.StackServices.service_name
                  };
                  result.push(metricObj);
                  if (metricObj.level === 'HOSTCOMPONENT') {
                    self.insertHostComponentCriteria(metricObj);
                  }
                }
              }
            }, this);
          }
        }
      }, this);
    }
    if (!!App.YARNService.find("YARN")) {
      result = this.substitueQueueMetrics(result);
    }
    this.save('allMetrics', result);
  },


  /**
   * @name substitueQueueMetrics
   * @param metrics
   * @return {Array} array of metric objects with regex substituted with actual metrics names
   */
  substitueQueueMetrics: function (metrics) {
    var result = [];
    var queuePaths = App.YARNService.find("YARN").get("allQueueNames");
    var queueNames = [];
    var queueMetricName;
    queuePaths.forEach(function (_queuePath) {
      var queueName = _queuePath.replace(/\//g, ".");
      queueNames.push(queueName);
    }, this);
    var regexpForAMS = new RegExp("^yarn.QueueMetrics.Queue=\\(\\.\\+\\).*$");
    var regexpForJMX = new RegExp("\\(\\.\\+\\)", "g");
    var replaceRegexForMetricName = regexpForJMX;
    var replaceRegexForMetricPath = new RegExp("\\$\\d\\..*\\)(?=\\/)", "g");
    metrics.forEach(function (_metric) {
      var isAMSQueueMetric = regexpForAMS.test(_metric.name);
      var isJMXQueueMetrics = regexpForJMX.test(_metric.name);
      if ((_metric.type === 'GANGLIA' && isAMSQueueMetric) || (_metric.type === 'JMX' && isJMXQueueMetrics)) {
        queuePaths.forEach(function (_queuePath) {
          queueMetricName = '';
          if (_metric.type === 'GANGLIA') {
            queueMetricName = _queuePath.replace(/\//g, ".");
          } else if (_metric.type === 'JMX') {
            _queuePath.split("/").forEach(function(_metricName, index){
              queueMetricName = queueMetricName + ',q' + index + '=' + _metricName;
            }, this);
          }
          var metricName = _metric.name.replace(replaceRegexForMetricName, queueMetricName);
          var newMetricPath = _metric.widget_id.replace(replaceRegexForMetricPath, _queuePath);
          var newQueueMetric = $.extend(true, {}, _metric, {name: metricName, widget_id: newMetricPath});
          result.pushObject(newQueueMetric);
        }, this);
      } else {
        result.pushObject(_metric);
      }
    }, this);
    return result;
  },

  /**
   *
   * @param metricObj {Object}
   */
  insertHostComponentCriteria: function (metricObj) {
    switch (metricObj.component_name) {
      case 'NAMENODE':
        metricObj.host_component_criteria = 'host_components/metrics/dfs/FSNamesystem/HAState=active';
        break;
      case 'RESOURCEMANAGER':
        metricObj.host_component_criteria = 'host_components/HostRoles/ha_state=ACTIVE';
        break;
      case 'HBASE_MASTER':
        metricObj.host_component_criteria = 'host_components/metrics/hbase/master/IsActiveMaster=true';
        break;
      default:
        metricObj.host_component_criteria = ' ';
    }
  },

  /**
   * post widget definition to server
   * @returns {$.ajax}
   */
  postWidgetDefinition: function (data) {
    return App.ajax.send({
      name: 'widgets.wizard.add',
      sender: this,
      data: {
        data: data
      },
      success: 'postWidgetDefinitionSuccessCallback'
    });
  },

  /**
   * assign created widget to active layout if it present or to appropriate nameservice layout and all-nameservices layout
   * @param data
   * @param requestObject
   * @param requestData
   */
  postWidgetDefinitionSuccessCallback: function (data, requestObject, requestData) {
    if (Em.isNone(this.get('content.layout'))) return;
    var mainServiceInfoMetricsController = App.router.get('mainServiceInfoMetricsController');
    var self = this;
    var layout = this.get('content.layout');
    var newWidgeet = data.resources[0].WidgetInfo;
    var tag = requestData.data.WidgetInfo.tag;

    if (tag) {
      this.loadLayoutByName(mainServiceInfoMetricsController.get('userLayoutName') + '_nameservice_' + tag).done(function (data) {
        data.items[0].WidgetLayoutInfo.widgets = data.items[0].WidgetLayoutInfo.widgets.map(function(w) {
          return w.WidgetInfo.id;
        });
        layout = data.items[0].WidgetLayoutInfo;
        self.addWidgetToLayout(newWidgeet, layout).done(function () {
          self.loadLayoutByName(mainServiceInfoMetricsController.get('userLayoutName') + '_nameservice_all').done(function (data) {
            data.items[0].WidgetLayoutInfo.widgets = data.items[0].WidgetLayoutInfo.widgets.map(function(w) {
              return w.WidgetInfo.id;
            });
            self.addWidgetToLayout(newWidgeet, data.items[0].WidgetLayoutInfo).done(function () {
              mainServiceInfoMetricsController.updateActiveLayout();
            });
          });
        });
      });
    } else {
      this.addWidgetToLayout(newWidgeet, layout).done(function () {
        mainServiceInfoMetricsController.updateActiveLayout();
      });
    }
  },

  addWidgetToLayout: function (newWidget, layout) {
    var mainServiceInfoMetricsController = App.router.get('mainServiceInfoMetricsController');
    var widgets = layout.widgets.map(function(item){
      return Em.Object.create({id: item});
    });
    widgets.pushObject(Em.Object.create({
      id: newWidget.id
    }));
    return mainServiceInfoMetricsController.saveWidgetLayout(widgets, Em.Object.create(layout));
  },

  loadLayoutByName: function (name) {
    return App.ajax.send({
      name: 'widget.layout.name.get',
      sender: this,
      data: {
        name: name
      }
    });
  },

  /**
   * Remove all loaded data.
   * Created as copy for App.router.clearAllSteps
   */
  clearAllSteps: function () {
    this.clearInstallOptions();
    // clear temporary information stored during the install
    this.set('content.cluster', this.getCluster());
  },

  clearTasksData: function () {
    this.saveTasksStatuses(undefined);
    this.saveRequestIds(undefined);
    this.saveTasksRequestIds(undefined);
  },

  cancel: function () {
    var self = this;
    var step3Controller = App.router.get('widgetWizardStep3Controller');
    var isLastStep = parseInt(self.get('currentStep')) === self.get('totalSteps');
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      body: Em.I18n.t('dashboard.widgets.wizard.onClose.popup.body'),
      primary: isLastStep ? Em.I18n.t('common.save') : null,
      secondary: Em.I18n.t('dashboard.widgets.wizard.onClose.popup.discardAndExit'),
      third: Em.I18n.t('common.cancel'),
      disablePrimary: function () {
        return !(isLastStep && !step3Controller.get('isSubmitDisabled'));
      }.property(),
      onPrimary: function () {
        App.router.send('complete', step3Controller.collectWidgetData());
        this.onSecondary();
      },
      onSecondary: function () {
        this.hide();
        self.finishWizard();
      },
      onThird: function () {
        this.hide();
      }
    });
  },

  /**
   * finish wizard
   */
  finishWizard: function () {
    var service = App.Service.find(this.get('content.widgetService'));

    this.finish();
    var self = this;
    var successCallBack = function() {
      self.get('popup').hide();
      App.router.transitionTo('main.services.service.metrics', service);
      App.get('router.updateController').updateAll();
    };

    if (App.get('testMode')) {
      successCallBack();
    } else {
      App.clusterStatus.setClusterStatus({
        clusterName: App.router.getClusterName(),
        clusterState: 'DEFAULT',
        localdb: App.db.data
      }, {successCallback: successCallBack});
    }
  },


  /**
   * Clear all temporary data
   */
  finish: function () {
    this.setCurrentStep('1', false, true);
    this.save('widgetType', '');
    this.resetDbNamespace();
  }
});
