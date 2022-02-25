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

App.MainConfigHistoryController = Em.ArrayController.extend(App.TableServerMixin, {
  name: 'mainConfigHistoryController',

  content: App.ServiceConfigVersion.find(),
  totalCount: 0,
  filteredCount: 0,
  resetStartIndex: true,
  mockUrl: '/data/configurations/service_versions.json',
  realUrl: function () {
    return App.apiPrefix + '/clusters/' + App.get('clusterName') + '/configurations/service_config_versions?<parameters>fields=service_config_version,user,group_id,group_name,is_current,createtime,service_name,hosts,service_config_version_note,is_cluster_compatible,stack_id&minimal_response=true';
  }.property('App.clusterName'),

  /**
   * @type {boolean}
   * @default false
   */
  showFilterConditionsFirstLoad: false,

  /**
   * associations between host property and column index
   * @type {Array}
   */
  colPropAssoc: function () {
    var associations = [];
    associations[1] = 'serviceVersion';
    associations[2] = 'configGroup';
    associations[3] = 'createTime';
    associations[4] = 'author';
    associations[5] = 'notes';
    return associations;
  }.property(),

  filterProps: [
    {
      name: 'serviceVersion',
      key: 'service_name',
      type: 'EQUAL'
    },
    {
      name: 'configGroup',
      key: 'group_name',
      type: 'EQUAL'
    },
    {
      name: 'createTime',
      key: 'createtime',
      type: 'MORE'
    },
    {
      name: 'author',
      key: 'user',
      type: 'MATCH'
    },
    {
      name: 'notes',
      key: 'service_config_version_note',
      type: 'MATCH'
    }
  ],

  sortProps: [
    {
      name: 'serviceVersion',
      key: 'service_name'
    },
    {
      name: 'configGroup',
      key: 'group_name'
    },
    {
      name: 'createTime',
      key: 'createtime'
    },
    {
      name: 'author',
      key: 'user'
    },
    {
      name: 'notes',
      key: 'service_config_version_note'
    }
  ],

  /**
   * load all data components required by config history table
   *  - total counter of service config versions(called in parallel)
   *  - current versions
   *  - filtered versions
   * @param {boolean} shouldUpdateCounter
   * @return {*}
   */
  load: function (shouldUpdateCounter = false) {
    const dfd = $.Deferred();
    this.loadConfigVersionsToModel().done(() => {
      if (shouldUpdateCounter) {
        this.updateTotalCounter();
      }
      dfd.resolve();
    });
    return dfd.promise();
  },

  /**
   * get filtered service config versions from server and push it to model
   * @return {*}
   */
  loadConfigVersionsToModel: function () {
    var dfd = $.Deferred();
    var queryParams = this.getQueryParameters();

    App.HttpClient.get(this.getUrl(queryParams), App.serviceConfigVersionsMapper, {
      complete: function () {
        dfd.resolve();
      }
    });
    return dfd.promise();
  },

  updateTotalCounter: function () {
    return App.ajax.send({
      name: 'service.serviceConfigVersions.get.total',
      sender: this,
      data: {},
      success: 'updateTotalCounterSuccess'
    })
  },

  updateTotalCounterSuccess: function (data, opt, params) {
    this.set('totalCount', Number(data.itemTotal));
  },

  getUrl: function (queryParams) {
    var params = '';
    if (App.get('testMode')) {
      return this.get('mockUrl');
    } else {
      if (queryParams) {
        params = App.router.get('updateController').computeParameters(queryParams);
      }
      params = (params.length > 0) ? params + "&" : params;
      return this.get('realUrl').replace('<parameters>', params);
    }
  },

  subscribeToUpdates: function() {
    App.StompClient.addHandler('/events/configs', 'history', this.load.bind(this, true));
  },

  unsubscribeOfUpdates: function() {
    App.StompClient.removeHandler('/events/configs', 'history');
  },

  /**
   * get sort properties from local db. A overridden funtion from parent
   * @return {Array}
   */
  getSortProps: function () {
    var savedSortConditions = App.db.getSortingStatuses(this.get('name')) || [],
      sortProperties = this.get('sortProps'),
      sortParams = [];
  
    savedSortConditions.forEach(function (sort) {
      var property = sortProperties.findProperty('name', sort.name);
      if (property && (sort.status === 'sorting_asc' || sort.status === 'sorting_desc')) {
        property.value = sort.status.replace('sorting_', '');
        property.type = 'SORT';
        if (property.name === 'serviceVersion') {
          property.key = "service_name." + sort.status.replace('sorting_', '') + ",service_config_version";
          property.value = "desc";
        }
        if (property.name === 'configGroup') {
          property.key = "group_name." + sort.status.replace('sorting_', '') + ",service_config_version";
          property.value = "desc";
        }
      
        sortParams.push(property);
      }
    });
    return sortParams;
  },

  /**
   *
   * @param {string} name
   */
  getSearchBoxSuggestions: function(name) {
    const dfd = $.Deferred();
    const key = this.get('filterProps').findProperty('name', name).key;
    App.ajax.send({
      name: 'service.serviceConfigVersions.get.suggestions',
      sender: this,
      data: {
        key
      }
    })
    .done((data) => {dfd.resolve(data.items.mapProperty(key).uniq());})
    .fail(() => {dfd.resolve([]);});
    return dfd.promise();
  }
});
