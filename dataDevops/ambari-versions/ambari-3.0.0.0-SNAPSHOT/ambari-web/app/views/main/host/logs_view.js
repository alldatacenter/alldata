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
var filters = require('views/common/filter_view');
var sort = require('views/common/sort_view');
var fileUtils = require('utils/file_utils');

App.MainHostLogsView = App.TableView.extend({
  templateName: require('templates/main/host/logs'),

  isVisible: Em.computed.not('App.isClusterUser'),
  classNames: ['logs-tab-content'],

  /**
   * Filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredContentInfo: Em.computed.i18nFormat('tableView.filters.filteredLogsInfo', 'filteredCount', 'totalCount'),

  /**
   * @type {Ember.Object}
   */
  host: Em.computed.alias('App.router.mainHostDetailsController.content'),

  hostLogs: function() {
    return App.HostComponentLog.find().filterProperty('hostName', this.get('host.hostName'));
  }.property('App.HostComponentLog.length'),

  content: function() {
    var self = this,
        linkTailTpl = '/#/logs/serviceLogs;hosts={0};components={2};query=%5B%7B"id":0,"name":"path","label":"Path","value":"{1}","isExclude":false%7D%5D';
    
    return this.get('hostLogs').map(function(i) {
      return Em.Object.create({
        serviceName: i.get('hostComponent.service.serviceName'),
        serviceDisplayName: i.get('hostComponent.service.displayName'),
        componentName: i.get('hostComponent.componentName'),
        componentDisplayName: i.get('hostComponent.displayName'),
        hostName: self.get('host.hostName'),
        logComponentName: i.get('name'),
        fileNamesObject: i.get('logFileNames').map(function(filePath) {
          return {
            fileName: fileUtils.fileNameFromPath(filePath),
            filePath: filePath,
            linkTail: linkTailTpl.format(
              encodeURIComponent(i.get('hostName')),
              encodeURIComponent(filePath),
              encodeURIComponent(i.get('name'))
            )
          };
        }),
        fileNamesFilterValue: i.get('logFileNames').join(',')
      });
    });
  }.property('hostLogs.length'),

  /**
   * @type {Ember.View}
   */
  sortView: sort.wrapperView,

  serviceNameSort: sort.fieldView.extend({
    column: 1,
    name: 'serviceName',
    displayName: Em.I18n.t('common.service')
  }),

  componentNameSort: sort.fieldView.extend({
    column: 2,
    name: 'componentName',
    displayName: Em.I18n.t('common.component')
  }),

  fileExtensionsSort: sort.fieldView.extend({
    column: 3,
    name: 'extension',
    displayName: Em.I18n.t('common.extension')
  }),

  serviceNameFilterView: filters.createSelectView({
    column: 1,
    fieldType: 'filter-input-width',
    didInsertElement: function() {
      this.setValue(Em.getWithDefault(this, 'controller.serializedQuery.service_name', ''));
      this._super();
    },
    content: function() {
      return [{
        value: '',
        label: Em.I18n.t('common.all')
      }].concat(App.Service.find().mapProperty('serviceName').uniq().map(function(item) {
        return {
          value: item,
          label: App.Service.find().findProperty('serviceName', item).get('displayName')
        };
      }));
    }.property('App.router.clusterController.isLoaded'),
    onChangeValue: function() {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  componentNameFilterView: filters.createSelectView({
    column: 2,
    fieldType: 'filter-input-width',
    didInsertElement: function() {
      this.setValue(Em.getWithDefault(this, 'controller.serializedQuery.component_name', ''));
      this._super();
    },
    content: function() {
      var hostName = this.get('parentView').get('host.hostName'),
        hostComponents = App.HostComponent.find().filterProperty('hostName', hostName),
        componentsMap = hostComponents.map(function(item) {
          return {
            value: item.get('componentName'),
            label: item.get('displayName')
          };
        });
      return [{
        value: '',
        label: Em.I18n.t('common.all')
      }].concat(componentsMap);
    }.property('App.router.clusterController.isLoaded'),
    onChangeValue: function() {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  fileExtensionsFilter: filters.createSelectView({
    column: 3,
    fieldType: 'filter-input-width',
    type: 'string',
    didInsertElement: function() {
      this.setValue(Em.getWithDefault(this, 'controller.serializedQuery.file_extension', ''));
      this._super();
    },
    content: function() {
      return [{
        value: '',
        label: Em.I18n.t('common.all')
      }].concat([
        '.out',
         '.log'
       ].map(function(item) {
        return {
          value: item,
          label: item
        };
       }));
    }.property('App.router.clusterController.isLoaded'),
    onChangeValue: function() {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'file_extension');
    }
  }),

  /**
   * @type {string[]}
   */
  colPropAssoc: function () {
    var ret = [];
    ret[1] = 'serviceName';
    ret[2] = 'componentName';
    ret[3] = 'fileNamesFilterValue';
    return ret;
  }.property(),

  logFileRowView: Em.View.extend({
    tagName: 'tr',

    didInsertElement: function() {
      this._super();
      App.tooltip(this.$('[rel="log-file-name-tooltip"]'));
    },

    willDestroyElement: function() {
      this.$('[rel="log-file-name-tooltip"]').tooltip('destroy');
    }
  }),

  openLogFile: function(e) {
    var content = e.contexts,
        filePath = content[1],
        componentLog = content[0];
    if (e.contexts.length) {
      App.showLogTailPopup(Em.Object.create({
        logComponentName: componentLog.get('logComponentName'),
        hostName: componentLog.get('hostName'),
        filePath: filePath
      }));
    }
  }
});
