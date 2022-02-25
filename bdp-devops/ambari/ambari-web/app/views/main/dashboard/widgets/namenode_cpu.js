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

App.NameNodeCpuPieChartView = App.PieChartDashboardWidgetView.extend(App.NameNodeWidgetMixin, {

  widgetHtmlId: Em.computed.format('widget-nn-cpu-{0}', 'subGroupId'),
  cpuWio: null,
  nnHostName: "",
  intervalId: null,

  activeNameNodes: Em.computed.alias('model.activeNameNodes'),
  nameNode: Em.computed.alias('model.nameNode'),

  willDestroyElement: function () {
    clearInterval(this.get("intervalId"));
  },

  didInsertElement: function () {
    this._super();
    let intervalId;
    App.router.get('mainController').isLoading.call(App.router.get('clusterController'), 'isServiceContentFullyLoaded').done(() => {
      if (App.get('isHaEnabled')) {
        const nn = this.get('activeNameNodes').findProperty('haNameSpace', this.get('subGroupId'));
        if (nn) {
          this.set('nnHostName', nn.get('hostName'));
        }
      } else {
        this.set('nnHostName', this.get('nameNode.hostName'));
      }
      if (this.get('nnHostName')) {
        this.getValue();
        intervalId = setInterval(() => this.getValue(), App.componentsUpdateInterval);
        this.set('intervalId', intervalId);
      }
    });
  },

  getValue: function () {
    App.ajax.send({
      name: 'namenode.cpu_wio',
      sender: this,
      data: {
        nnHost: this.get('nnHostName')
      },
      success: 'updateValueSuccess',
      error: 'updateValueError'
    });
  },

  updateValueError: function () {
    this.calc();
  },

  updateValueSuccess: function (response) {
    this.set('cpuWio', Em.get(response, 'metrics.cpu.cpu_wio'));
    this.calc();
  },

  calcHiddenInfo: function () {
    var value = this.get('cpuWio');
    var obj1;
    if (value) {
      value = value >= 100 ? 100 : value;
      obj1 = (value + 0).toFixed(2) + '%';
    }
    else {
      obj1 = Em.I18n.t('services.service.summary.notAvailable');
    }
    return [
      obj1,
      'CPU wait I/O'
    ];
  },

  calcIsPieExists: function () {
    return !Em.isNone(this.get('cpuWio'));
  },

  calcDataForPieChart: function () {
    var value = this.get('cpuWio');
    value = value >= 100 ? 100 : value;
    var percent = (value + 0).toFixed(1);
    var percentPrecise = (value + 0).toFixed(2);
    return [percent, percentPrecise];
  }
});