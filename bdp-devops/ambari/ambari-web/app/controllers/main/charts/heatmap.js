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

App.MainChartsHeatmapController = Em.Controller.extend(App.WidgetSectionMixin, {
  name: 'mainChartsHeatmapController',
  rackMap: {},
  racks: [],
  rackViews: [],

  /**
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * Heatmap metrics that are available choices  on the page
   */
  heatmapCategories: [],

  allHeatmaps:[],

  layoutNameSuffix: "_heatmap",

  sectionNameSuffix: "_HEATMAPS",

  loadRacksUrlParams: 'fields=Hosts/rack_info,Hosts/host_name,Hosts/public_host_name,Hosts/os_type,Hosts/ip,host_components,metrics/disk,metrics/cpu/cpu_system,metrics/cpu/cpu_user,metrics/memory/mem_total,metrics/memory/mem_free&minimal_response=true',

  /**
   * @type {string}
   */
  loadHeatmapsUrlParams: function() {
    var serviceName = this.get('content.serviceName');
    if (serviceName) {
      return 'WidgetInfo/widget_type=HEATMAP&WidgetInfo/scope=CLUSTER&WidgetInfo/metrics.matches(.*\"service_name\":\"' + serviceName + '\".*)&fields=WidgetInfo/metrics';
    } else {
      return 'WidgetInfo/widget_type=HEATMAP&WidgetInfo/scope=CLUSTER&fields=WidgetInfo/metrics';
    }
  }.property('content.serviceName'),


  selectedMetric: null,

  inputMaximum: '',

  /**
   * Heatmap widget currently shown on the page
   */
  activeWidget: Em.computed.alias('widgets.firstObject'),


  /**
   * This function is called from the bound view of the controller
   */
  loadPageData: function () {
    var self = this;

    this.loadRacks().always(function () {
      self.resetPageData();
      self.getAllHeatMaps().done(function (allHeatmapData) {
        self.set('isLoaded', true);
        allHeatmapData.items.forEach(function (_allHeatmapData) {
          self.get('allHeatmaps').pushObject(_allHeatmapData.WidgetInfo);
        });
        var categories = self.categorizeByServiceName(self.get('allHeatmaps'));
        self.set('heatmapCategories', categories);
        self.getActiveWidgetLayout();
      });
    });
  },

  /**
   * categorize heatmaps with respect to service names
   * @param {Array} allHeatmaps
   * @return {Array}
   */
  categorizeByServiceName: function (allHeatmaps) {
    var categories = [];

    allHeatmaps.forEach(function (_heatmap) {
      var serviceNames = JSON.parse(_heatmap.metrics).mapProperty('service_name').uniq();
      serviceNames.forEach(function (_serviceName) {
        var category = categories.findProperty('serviceName', _serviceName);
        if (!category) {
          categories.pushObject(Em.Object.create({
            serviceName: _serviceName,
            displayName: _serviceName === 'STACK' ? 'Host' : App.format.role(_serviceName, true),
            heatmaps: [_heatmap]
          }));
        } else {
          category.get('heatmaps').pushObject(_heatmap);
        }
      }, this);
    }, this);
    return categories;
  },

  /**
   * clears/resets the data. This function should be called every time user navigates to heatmap page
   */
  resetPageData: function() {
    this.get('heatmapCategories').clear();
    this.get('allHeatmaps').clear();
  },

  /**
   *  Gets all heatmap widgets that should be available in select metrics dropdown on heatmap page
   * @return {$.ajax}
   */
  getAllHeatMaps: function() {
    var urlParams = this.get('loadHeatmapsUrlParams');

    return App.ajax.send({
      name: 'widgets.get',
      sender: this,
      data: {
        urlParams: urlParams,
        sectionName: this.get('sectionName')
      }
    });
  },


  /**
   * get hosts from server
   */
  loadRacks: function () {
    this.get('racks').clear();
    this.set('rackMap', {});
    var urlParams = this.get('loadRacksUrlParams');
    return App.ajax.send({
      name: 'hosts.heatmaps',
      sender: this,
      data: {
        urlParams: urlParams
      },
      success: 'loadRacksSuccessCallback'
    });
  },

  loadRacksSuccessCallback: function (data, opt, params) {
    var hosts = [];
    data.items.forEach(function (item) {
      hosts.push({
        hostName: item.Hosts.host_name,
        publicHostName: item.Hosts.public_host_name,
        osType: item.Hosts.os_type,
        ip: item.Hosts.ip,
        rack: item.Hosts.rack_info,
        diskTotal: item.metrics ? item.metrics.disk.disk_total : 0,
        diskFree: item.metrics ? item.metrics.disk.disk_free : 0,
        cpuSystem: item.metrics ? item.metrics.cpu.cpu_system : 0,
        cpuUser: item.metrics ? item.metrics.cpu.cpu_user : 0,
        memTotal: item.metrics ? item.metrics.memory.mem_total : 0,
        memFree: item.metrics ? item.metrics.memory.mem_free : 0,
        hostComponents: item.host_components.mapProperty('HostRoles.component_name')
      });
    });
    var rackMap = this.indexByRackId(hosts);
    var racks = this.toList(rackMap);
    //this list has an empty host array property
    this.set('rackMap', rackMap);
    this.set('racks', racks);
  },

  /**
   * @param {Array} hosts
   * @returns {Object} rackMap
   */
  indexByRackId: function (hosts) {
    var rackMap = {};

    hosts.forEach(function (host) {
      var rackId = host.rack;
      if(!rackMap[rackId]) {
        rackMap[rackId] =
          Em.Object.create({
            name: rackId,
            rackId: rackId,
            hosts: [host]
          });
      } else {
        rackMap[rackId].hosts.push(host);
      }
    });
    return rackMap;
  },

  /**
   *
   * @param {Object} rackMap
   * @returns {Array} racks
   */
  toList: function (rackMap) {
    var racks = [];
    var i = 0;
    for (var rackKey in rackMap) {
      if (rackMap.hasOwnProperty(rackKey)) {
        racks.push(
          Em.Object.create({
            name: rackKey,
            rackId: rackKey,
            hosts: rackMap[rackKey].hosts,
            isLoaded: false,
            index: i++
          })
        );
      }
    }
    return racks;
  },

  validation: function () {
    if (this.get('selectedMetric')) {
      if (/^\d+$/.test(this.get('inputMaximum'))) {
        $('#inputMaximum').removeClass('error');
        this.set('selectedMetric.maximumValue', this.get('inputMaximum'));
      } else {
        $('#inputMaximum').addClass('error');
      }
    }
  }.observes('inputMaximum'),


  addRackView: function (view) {
    this.get('rackViews').push(view);
    if (this.get('rackViews').length == this.get('racks').length) {
      this.displayAllRacks();
    }
  },

  displayAllRacks: function () {
    if (this.get('rackViews').length) {
      this.get('rackViews').pop().displayHosts();
      this.displayAllRacks();
    }
  },

  showHeatMapMetric: function (event) {
    var self = this;
    var metricItem = Em.Object.create(event.context);
    this.saveWidgetLayout([metricItem]).done(function(){
      self.getActiveWidgetLayout();
    });
  },

  hostToSlotMap: Em.computed.alias('selectedMetric.hostToSlotMap'),

  /**
   * return class name for to be used for containing each rack.
   *
   * @this App.MainChartsHeatmapController
   */
  rackClass: function () {
    var rackCount = this.get('racks.length');
    if (rackCount < 2) {
      return "col-md-12";
    }
    if (rackCount === 2) {
      return "col-md-6";
    }
    return "col-md-4";
  }.property('racks.length')
});
