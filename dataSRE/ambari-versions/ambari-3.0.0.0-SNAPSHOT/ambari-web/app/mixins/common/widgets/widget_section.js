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

App.WidgetSectionMixin = Ember.Mixin.create({
  
  /**
   *  @Type {App.WidgetLayout}
   */
  activeWidgetLayout: {},
  
  activeNSWidgetLayouts: [],
  
  selectedNSWidgetLayout: {},
  
  /**
   * UI default layout name
   */
  defaultLayoutName: function () {
    var heatmapType;
    if (this.get('content.serviceName')) {
      heatmapType = this.get('content.serviceName').toLowerCase();
    } else {
      heatmapType = "system";
    }
    return "default_" + heatmapType + this.layoutNameSuffix;
  }.property('content.serviceName'),

  /**
   * UI user default layout name
   */
  userLayoutName: function () {
    var heatmapType;
    var loginName = App.router.get('loginName');
    if (this.get('content.serviceName')) {
      heatmapType = this.get('content.serviceName').toLowerCase();
    } else {
      heatmapType = "system";
    }
    return loginName + "_" + heatmapType + this.layoutNameSuffix;
  }.property('content.serviceName'),

  /**
   * UI section name
   */
  sectionName: function () {
    if (this.get('content.serviceName')) {
      return this.get('content.serviceName') + this.sectionNameSuffix;
    } else {
      return "SYSTEM" + this.sectionNameSuffix
    }
  }.property('content.serviceName'),


  /**
   * @type {Em.A}
   */
  widgetLayouts: function () {
    return App.WidgetLayout.find();
  }.property('isWidgetLayoutsLoaded'),


  /**
   * Does Service has widget descriptor defined in the stack
   * @type {boolean}
   */
  isServiceWithEnhancedWidgets: function () {
    var isServiceWithWidgetdescriptor;
    var serviceName = this.get('content.serviceName');
    if (serviceName) {
      isServiceWithWidgetdescriptor = App.StackService.find(serviceName).get('isServiceWithWidgets');
    } else if (this.get('sectionName') === 'SYSTEM_HEATMAPS') {
      isServiceWithWidgetdescriptor = true;
    }
    return isServiceWithWidgetdescriptor;
  }.property('content.serviceName'),

  isHDFSFederatedSummary: function () {
    return this.get('content.serviceName') === 'HDFS' && this.get('sectionNameSuffix') === '_SUMMARY' && App.get('hasNameNodeFederation');
  }.property('content.serviceName', 'sectionNameSuffix', 'App.hasNameNodeFederation'),

  /**
   * @type {Em.A}
   */
  widgets: function () {
    if (this.get('isWidgetsLoaded') && this.get('activeWidgetLayout.widgets')) {
      return this.get('activeWidgetLayout.widgets').toArray();
    }
    return [];
  }.property('isWidgetsLoaded', 'activeWidgetLayout.widgets'),

  isAmbariMetricsInstalled: function () {
    return App.Service.find().someProperty('serviceName', 'AMBARI_METRICS');
  }.property('App.router.mainServiceController.content.length'),

  switchNameServiceLayout: function (data) {
    this.set('selectedNSWidgetLayout', data.context);
  },

  /**
   * load widgets defined by user
   * @returns {$.ajax}
   */
  getActiveWidgetLayout: function () {
    var sectionName = this.get('sectionName');
    var urlParams = 'WidgetLayoutInfo/section_name=' + sectionName;
    this.set('activeWidgetLayout', {});
    this.set('activeNSWidgetLayouts', []);
    this.set('isWidgetsLoaded', false);
    if (this.get('isServiceWithEnhancedWidgets')) {
      return App.ajax.send({
        name: 'widgets.layouts.active.get',
        sender: this,
        data: {
          userName: App.router.get('loginName'),
          sectionName: sectionName,
          urlParams: urlParams
        },
        success: 'getActiveWidgetLayoutSuccessCallback'
      });
    } else {
      this.set('isWidgetsLoaded', true);
    }
  },


  /**
   * success callback of <code>getActiveWidgetLayout()</code>
   * @param {object|null} data
   */
  getActiveWidgetLayoutSuccessCallback: function (data) {
    if (this.get('isHDFSFederatedSummary')) {
      this.getNameNodeWidgets().done((widgets) => {
        widgets = widgets.items;
        var widgetsNamespaces = widgets.mapProperty('WidgetInfo.tag').uniq().without(null);
        var namespaces = App.HDFSService.find('HDFS').get('masterComponentGroups').mapProperty('name');
        var defaultNNWidgets = widgets.filterProperty('WidgetInfo.tag', null);
        var widgetsToCreateCount;
        if (namespaces.length > widgetsNamespaces.length) {
          widgetsToCreateCount = (namespaces.length - widgetsNamespaces.length) * defaultNNWidgets.length;
          namespaces.forEach((namespace) => {
            if (!widgetsNamespaces.contains(namespace)) {
              defaultNNWidgets.forEach((widget) => {
                this.postNNWidgets(widget, widgetsToCreateCount, data, namespace);
              });
            }
          });
        } else {
          this.createLayouts(data);
        }
      })
    } else {
      this.createLayouts(data);
    }
  },
  
  /**
   *
   * @param widget
   * @param widgetsToCreateCount
   * @param data
   * @param namespace
   */
  postNNWidgets: function(widget, widgetsToCreateCount, data, namespace) {
    if (widget.href) {
      delete widget.href;
      delete widget.WidgetInfo.id;
      delete widget.WidgetInfo.cluster_name;
      delete widget.WidgetInfo.author;
      widget.WidgetInfo.metrics = JSON.parse(widget.WidgetInfo.metrics);
      widget.WidgetInfo.values = JSON.parse(widget.WidgetInfo.values);
    }
    widget.WidgetInfo.tag = namespace;
    this.postWidget(widget).done(() => {
      if (!--widgetsToCreateCount) {
        this.createLayouts(data);
      }
    });
  },

  getNameNodeWidgets: function () {
    return App.ajax.send({
      name: 'widgets.get',
      sender: this,
      data: {
        urlParams: 'WidgetInfo/widget_type.in(GRAPH,NUMBER,GAUGE)&WidgetInfo/scope=CLUSTER&WidgetInfo/metrics.matches(.*\"component_name\":\"NAMENODE\".*)&fields=*'
      }
    });
  },

  postWidget: function (data) {
    return App.ajax.send({
      name: 'widgets.wizard.add',
      sender: this,
      data: {
        data: data
      }
    });
  },

  createLayouts: function (data) {
    var self = this;
    if (data.items[0]) {
      var hdfs = App.HDFSService.find('HDFS');
      if (hdfs.get('isLoaded') && this.get('isHDFSFederatedSummary') && hdfs.get('masterComponentGroups').length + 2 !== data.items.length) {
        this.createFederationWidgetLayouts(data);
      } else {
        this.getWidgetLayoutSuccessCallback(data);
      }
    } else {
      this.getAllActiveWidgetLayouts().done(function (activeWidgetLayoutsData) {
        self.getDefaultWidgetLayoutByName(self.get('defaultLayoutName')).done(function (defaultWidgetLayoutData) {
          defaultWidgetLayoutData = defaultWidgetLayoutData.items[0].WidgetLayoutInfo;
          defaultWidgetLayoutData.layout_name = self.get('userLayoutName');
          self.createUserWidgetLayout(defaultWidgetLayoutData).done(function (userLayoutIdData) {
            self.createUserWidgetLayoutCallback(userLayoutIdData, activeWidgetLayoutsData);
          });
        });
      });
    }
  },
  
  /**
   *
   * @param userLayoutIdData
   * @param activeWidgetLayoutsData
   */
  createUserWidgetLayoutCallback: function(userLayoutIdData, activeWidgetLayoutsData) {
    var activeWidgetLayouts;
    var widgetLayouts = [];
    if (!!activeWidgetLayoutsData.items.length) {
      widgetLayouts = activeWidgetLayoutsData.items.map(function (item) {
        return {
          "id": item.WidgetLayoutInfo.id
        }
      });
    }
    widgetLayouts.push({id: userLayoutIdData.resources[0].WidgetLayoutInfo.id});
    activeWidgetLayouts = {
      "WidgetLayouts": widgetLayouts
    };
    this.saveActiveWidgetLayouts(activeWidgetLayouts).done(() => {
      this.getActiveWidgetLayout();
    });
  },

  getAllActiveWidgetLayouts: function () {
    return App.ajax.send({
      name: 'widgets.layouts.all.active.get',
      sender: this,
      data: {
        userName: App.router.get('loginName')
      }
    });
  },

  /**
   * success callback of <code>getWidgetLayout()</code>
   * @param {object|null} data
   */
  getWidgetLayoutSuccessCallback: function (data) {
    if (data) {
      data.items.forEach(function (item) {
        App.widgetMapper.map(item.WidgetLayoutInfo);
      });
      App.widgetLayoutMapper.map(data);
      this.set('activeWidgetLayout', App.WidgetLayout.find().findProperty('layoutName', this.get('userLayoutName')));
      if (this.get('isHDFSFederatedSummary')) {
        this.set('activeNSWidgetLayouts', App.WidgetLayout.find().filterProperty('sectionName', this.get('sectionName')).rejectProperty('layoutName', this.get('userLayoutName')));
        this.set('selectedNSWidgetLayout', this.get('activeNSWidgetLayouts')[0]);
      }
      this.set('isWidgetsLoaded', true);
    }
  },

  getDefaultWidgetLayoutByName: function (layoutName) {
    var urlParams = 'WidgetLayoutInfo/layout_name=' + layoutName;
    return App.ajax.send({
      name: 'widget.layout.get',
      sender: this,
      data: {
        urlParams: urlParams
      }
    });
  },

  createUserWidgetLayout: function (widgetLayoutData) {
    var layout = widgetLayoutData;
    var data = {
      "WidgetLayoutInfo": {
        "display_name": layout.display_name,
        "layout_name": layout.layout_name,
        "scope": "USER",
        "section_name": layout.section_name,
        "user_name": App.router.get('loginName'),
        "widgets": layout.widgets.map(function (widget) {
          return {
            "id": widget.WidgetInfo.id
          }
        })
      }
    };
    return App.ajax.send({
      name: 'widget.layout.create',
      sender: this,
      data: {
        data: data
      }
    });
  },

  updateUserWidgetLayout: function (widgetLayoutData) {
    var layout = widgetLayoutData;
    var data = {
      "WidgetLayoutInfo": {
        "display_name": layout.display_name,
        "layout_name": layout.layout_name,
        "id": layout.id,
        "scope": "USER",
        "section_name": layout.section_name,
        "user_name": App.router.get('loginName'),
        "widgets": layout.widgets.map(function (widget) {
          return {
            "id": widget.WidgetInfo.id
          }
        })
      }
    };
    return App.ajax.send({
      name: 'widget.layout.edit',
      sender: this,
      data: {
        layoutId: layout.id,
        data: data
      },
    });
  },

  saveActiveWidgetLayouts: function (activeWidgetLayouts) {
    return App.ajax.send({
      name: 'widget.activelayouts.edit',
      sender: this,
      data: {
        data: activeWidgetLayouts,
        userName: App.router.get('loginName')
      }
    });
  },


  /**
   * save layout after re-order widgets
   * @param {Array} widgets
   * @param {Object} widgetLayout:  Optional. by default active widget layout is honored.
   * return {$.ajax}
   */
  saveWidgetLayout: function (widgets, widgetLayout) {
    var activeLayout = widgetLayout || this.get('activeWidgetLayout');
    var data = {
      "WidgetLayoutInfo": {
        "display_name": activeLayout.get("displayName"),
        "id": activeLayout.get("id"),
        "layout_name": activeLayout.get("layoutName"),
        "scope": activeLayout.get("scope"),
        "section_name": activeLayout.get("sectionName"),
        "widgets": widgets.map(function (widget) {
          return {
            "id": widget.get('id')
          }
        })
      }
    };
    return App.ajax.send({
      name: 'widget.layout.edit',
      sender: this,
      data: {
        layoutId: activeLayout.get("id"),
        data: data
      }
    });
  },

  /**
   * After closing widget section, layout should be reset
   */
  clearActiveWidgetLayout: function () {
    this.set('activeWidgetLayout', {});
    this.set('activeNSWidgetLayouts', []);
  },

  createFederationWidgetLayouts: function (data) {
    var currentLayouts = data.items;
    
    this.getDefaultWidgetLayoutByName(this.get('defaultLayoutName')).done((defaultWidgetLayoutData) => {
      this.getNameNodeWidgets().done((widgets) => {
        var newLayout = defaultWidgetLayoutData.items[0].WidgetLayoutInfo;
        var nameServiceToWidgetMap = {all: []};
        var nonNameServiceSpecific = newLayout.widgets.slice();
        widgets.items.forEach((widget) => {
          var tag = widget.WidgetInfo.tag;
          nonNameServiceSpecific = nonNameServiceSpecific.without(nonNameServiceSpecific.findProperty('WidgetInfo.id', widget.WidgetInfo.id));
          if (tag) {
            if (!nameServiceToWidgetMap[tag]) {
              nameServiceToWidgetMap[tag] = [];
            }
            nameServiceToWidgetMap[tag].push(widget);
            nameServiceToWidgetMap.all.push(widget);
          }
        });
        if (currentLayouts.length === 1) {
          this.createSingleLayout(currentLayouts, newLayout, nonNameServiceSpecific, nameServiceToWidgetMap);
        } else {
          this.createMultipleLayouts(currentLayouts, newLayout, nameServiceToWidgetMap);
        }
      });
    });
  },
  
  /**
   *
   * @param currentLayouts
   * @param newLayout
   * @param nonNameServiceSpecific
   * @param nameServiceToWidgetMap
   */
  createSingleLayout: function(currentLayouts, newLayout, nonNameServiceSpecific, nameServiceToWidgetMap) {
    const userLayoutName = this.get('userLayoutName');
    const newLayoutsIds = [];
  
    this.removeWidgetLayout(currentLayouts[0].WidgetLayoutInfo.id).done(() => {
      newLayout.layout_name = userLayoutName;
      newLayout.widgets = nonNameServiceSpecific;
      this.createUserWidgetLayout(newLayout).done((data) => {
        newLayoutsIds.push(data.resources[0].WidgetLayoutInfo.id);
        Em.keys(nameServiceToWidgetMap).forEach((nameService) => {
          newLayout.layout_name = userLayoutName + '_nameservice_' + nameService;
          newLayout.display_name = nameService === 'all' ? 'All' : nameService;
          newLayout.widgets = nameServiceToWidgetMap[nameService];
          this.createUserWidgetLayout(newLayout).done((data) => {
            newLayoutsIds.push(data.resources[0].WidgetLayoutInfo.id);
            if (newLayoutsIds.length >= Em.keys(nameServiceToWidgetMap).length + 1) {
              this.saveActiveWidgetLayouts({
                "WidgetLayouts": newLayoutsIds.map(function (layout) {
                  return {id: layout};
                })
              }).done(() => {
                this.getActiveWidgetLayout();
              });
            }
          });
        })
      });
    });
  },
  
  /**
   *
   * @param currentLayouts
   * @param newLayout
   * @param nameServiceToWidgetMap
   */
  createMultipleLayouts: function(currentLayouts, newLayout, nameServiceToWidgetMap) {
    const newNameServices = [];
    const newLayoutsIds = [];
    let newWidgets = [];
    const userLayoutName = this.get('userLayoutName');
    const namespaces = App.HDFSService.find('HDFS').get('masterComponentGroups').mapProperty('name');
    const currentLayoutsNames = currentLayouts.map((l) => {
      return l.WidgetLayoutInfo.layout_name.split('_nameservice_')[1];
    }).without(undefined).without('all');
    namespaces.forEach(function (n) {
      if (!currentLayoutsNames.contains(n)) {
        newNameServices.push(n);
      }
    });
    newNameServices.forEach((nameService) => {
      newLayout.layout_name = userLayoutName + '_nameservice_' + nameService;
      newLayout.display_name = nameService;
      newLayout.widgets = nameServiceToWidgetMap[nameService];
      newWidgets = newWidgets.concat(nameServiceToWidgetMap[nameService]);
      this.createUserWidgetLayout(newLayout).done((data) => {
        newLayoutsIds.push(data.resources[0].WidgetLayoutInfo.id);
        if (newLayoutsIds.length >= newNameServices.length) {
          this.saveActiveWidgetLayouts({
            "WidgetLayouts": newLayoutsIds.concat(currentLayouts.mapProperty('WidgetLayoutInfo.id')).map(function (layout) {
              return {id: layout};
            })
          }).done(() => {
            var allNSLayout = currentLayouts.findProperty('WidgetLayoutInfo.display_name', 'All').WidgetLayoutInfo;
            allNSLayout.widgets = allNSLayout.widgets.concat(newWidgets);
            this.updateUserWidgetLayout(allNSLayout).done(() => {
              this.getActiveWidgetLayout();
            });
          });
        }
      });
    });
  },

  removeWidgetLayout: function (id) {
    return App.ajax.send({
      name: 'widget.layout.delete',
      sender: this,
      data: {
        layoutId: id
      },
    });
  }
});