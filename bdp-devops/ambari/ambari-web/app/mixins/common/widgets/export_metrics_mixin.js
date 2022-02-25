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

require('views/common/export_metrics_menu_view');
var stringUtils = require('utils/string_utils');
var fileUtils = require('utils/file_utils');

App.ExportMetricsMixin = Em.Mixin.create({

  /**
   * Used as argument passed from template to indicate that resulting format is CSV instead of JSON
   */
  exportToCSVArgument: true,

  isExportMenuHidden: true,

  isExportButtonHidden: false,

  exportMetricsMenuView: App.ExportMetricsMenuView.extend(),

  targetView: function () {
    return this.get('exportTargetView') || this;
  }.property(),

  hideMenuForNoData: function () {
    if (this.get('isExportButtonHidden')) {
      this.set('isExportMenuHidden', true);
    }
  }.observes('isExportButtonHidden'),

  toggleFormatsList: function () {
    this.toggleProperty('isExportMenuHidden');
  },

  exportGraphData: function (event) {
    this.set('isExportMenuHidden', true);
    var ajaxIndex = this.get('targetView.ajaxIndex');
    App.ajax.send({
      name: ajaxIndex,
      data: $.extend(this.get('targetView').getDataForAjaxRequest(), {
        isCSV: !!event.context
      }),
      sender: this,
      success: 'exportGraphDataSuccessCallback',
      error: 'exportGraphDataErrorCallback'
    });
  },

  exportGraphDataSuccessCallback: function (response, request, params) {
    var seriesData = this.get('targetView').getData(response);
    if (!seriesData.length) {
      App.showAlertPopup(Em.I18n.t('graphs.noData.title'), Em.I18n.t('graphs.noData.tooltip.title'));
    } else {
      var fileType = params.isCSV ? 'csv' : 'json',
        fileName = 'data.' + fileType,
        data = params.isCSV ? this.prepareCSV(seriesData) : JSON.stringify(seriesData, this.jsonReplacer(), 4);
      fileUtils.downloadTextFile(data, fileType, fileName);
    }
  },

  exportGraphDataErrorCallback: function (jqXHR, ajaxOptions, error, opt) {
    App.ajax.defaultErrorHandler(jqXHR, opt.url, opt.type, jqXHR.status);
  },

  prepareCSV: function (data) {
    var displayUnit = this.get('targetView.displayUnit'),
      titles,
      ticksNumber,
      metricsNumber,
      metricsArray;
    this.checkGraphDataForValidity(data);
    titles = data.map(function (item) {
      return displayUnit ? item.name + ' (' + displayUnit + ')' : item.name;
    }, this);
    titles.unshift(Em.I18n.t('common.timestamp'));
    ticksNumber = data[0].data.length;
    metricsNumber = data.length;
    metricsArray = [titles];
    for (var i = 0; i < ticksNumber; i++) {
      metricsArray.push([data[0].data[i][1]]);
      for (var j = 0; j < metricsNumber; j++) {
         metricsArray[i + 1].push(data[j].data[i][0]);
      };
    };
    return stringUtils.arrayToCSV(metricsArray);
  },

  checkGraphDataForValidity: function (data) {
    data.sort(function (a, b) {
      return b.data.length - a.data.length
    });

    var maxLength = data[0].data.length;

    for (var i = 1; i < data.length; i ++) {
      if (data[i].data.length !== maxLength) this.fillGraphDataArrayWithMockedData(data[i], maxLength);
    }
  },

  fillGraphDataArrayWithMockedData: function (dataArray, neededLength) {
    var startIndex = dataArray.data.length,
      timestampInterval = dataArray.data[2][1] - dataArray.data[1][1];

    for (var i = startIndex; i < neededLength; i++) {
      var previousTimestamp = dataArray.data[i - 1][1];

      dataArray.data.push([null, previousTimestamp + timestampInterval]);
    }
  },

  jsonReplacer: function () {
    var displayUnit = this.get('targetView.displayUnit');
    return function (key, value) {
      if (['name', 'data'].contains(key) || (!isNaN(key))) {
        return key == 'name' && displayUnit ? value + ' (' + displayUnit + ')' : value;
      }
    }
  }

});
