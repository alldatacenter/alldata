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

App.WidgetEditController = App.WidgetWizardController.extend({

  name: 'widgetEditController',

  totalSteps: 2,

  content: Em.Object.create({
    controllerName: 'widgetEditController',
    widgetService: null,
    widgetType: '',

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
    widgetName: null,
    widgetDescription: null,
    widgetScope: null,
    widgetAuthor: null,
    widgetId: null,
    isMetricsLoaded: false
  }),

  loadMap: {
    '1': [
      {
        type: 'sync',
        callback: function () {
          this.load('widgetType');
          this.load('widgetService');
          this.load('widgetProperties', true);
          this.load('widgetValues', true);
          this.load('widgetMetrics', true);
        }
      },
      {
        type: 'async',
        callback: function () {
          return this.loadAllMetrics();
        }
      }
    ],
    '2': [
      {
        type: 'sync',
        callback: function () {
          this.load('widgetName');
          this.load('widgetDescription');
          this.load('widgetAuthor');
        }
      }
    ]
  },

  /**
   * post widget definition to server
   * @returns {$.ajax}
   */
  putWidgetDefinition: function (data) {
    return App.ajax.send({
      name: 'widgets.wizard.edit',
      sender: this,
      data: {
        data: data,
        widgetId: this.get('content.widgetId')
      },
      success: 'putWidgetDefinitionSuccessCallback'
    });
  },

  putWidgetDefinitionSuccessCallback: function() {

  },

  /**
   * Clear all temporary data
   */
  finish: function () {
    this.setCurrentStep('1', false, true);
    this.save('widgetType', '');
    this.save('widgetService', '');
    this.save('widgetProperties', null);
    this.save('widgetMetrics', []);
    this.save('widgetValues', []);
    this.save('widgetName', '');
    this.save('widgetDescription', '');
    this.save('widgetAuthor', '');
    this.save('widgetScope', '');
    this.save('allMetrics', []);
    this.save('expressions', []);
    this.save('dataSets', []);
    this.save('templateValue', '');
    this.resetDbNamespace();
  }
});
