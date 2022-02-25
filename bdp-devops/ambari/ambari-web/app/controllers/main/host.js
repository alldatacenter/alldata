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
var validator = require('utils/validator');

App.MainHostController = Em.ArrayController.extend(App.TableServerMixin, {
  name: 'mainHostController',

  clearFilters: null,

  filteredCount: 0,

  /**
   * total number of installed hosts
   * @type {number}
   */
  totalCount: Em.computed.alias('App.allHostNames.length'),

  /**
   * @type {boolean}
   * @default false
   */
  resetStartIndex: false,

  startIndex: 1,

  /**
   * true if any host page filter changed
   */
  filterChangeHappened: false,

  /**
   * if true, do not clean stored filter before hosts page rendering.
   */
  showFilterConditionsFirstLoad: false,

  saveSelection: false,

  content: App.Host.find(),

  allHostStackVersions: App.HostStackVersion.find(),
  /**
   * filterProperties support follow types of filter:
   * MATCH - match of RegExp
   * EQUAL - equality "="
   * LESS - "<"
   * MORE - ">"
   * MULTIPLE - multiple values to compare
   * CUSTOM - substitute values with keys "{#}" in alias
   */
  filterProperties: [
    {
      name: 'hostName',
      key: 'Hosts/host_name',
      type: 'MATCH'
    },
    {
      name: 'ip',
      key: 'Hosts/ip',
      type: 'MATCH'
    },
    {
      name: 'cpu',
      key: 'Hosts/cpu_count',
      type: 'EQUAL'
    },
    {
      name: 'memoryFormatted',
      key: 'Hosts/total_mem',
      type: 'EQUAL'
    },
    {
      name: 'loadAvg',
      key: 'metrics/load/load_one',
      type: 'EQUAL'
    },
    {
      name: 'rack',
      key: 'Hosts/rack_info',
      type: 'MATCH'
    },
    {
      name: 'hostComponents',
      key: 'host_components/HostRoles/component_name',
      type: 'EQUAL',
      isComponentRelatedFilter: true
    },
    {
      name: 'services',
      key: 'host_components/HostRoles/service_name',
      type: 'MATCH',
      isComponentRelatedFilter: true
    },
    {
      name: 'state',
      key: 'host_components/HostRoles/state',
      type: 'MATCH',
      isComponentRelatedFilter: true
    },
    {
      name: 'healthClass',
      key: 'Hosts/host_status',
      type: 'EQUAL'
    },
    {
      name: 'criticalWarningAlertsCount',
      key: '(alerts_summary/CRITICAL{0}|alerts_summary/WARNING{1})',
      type: 'CUSTOM'
    },
    {
      name: 'componentsWithStaleConfigsCount',
      key: 'host_components/HostRoles/stale_configs',
      type: 'EQUAL',
      isComponentRelatedFilter: true
    },
    {
      name: 'componentsInPassiveStateCount',
      key: 'host_components/HostRoles/maintenance_state',
      type: 'MULTIPLE',
      isComponentRelatedFilter: true
    },
    {
      name: 'selected',
      key: 'Hosts/host_name',
      type: 'MULTIPLE'
    },
    {
      name: 'version',
      key: 'stack_versions/repository_versions/RepositoryVersions/display_name',
      type: 'EQUAL'
    },
    {
      name: 'versionState',
      key: 'stack_versions/HostStackVersions/state',
      type: 'EQUAL'
    },
    {
      name: 'hostStackVersion',
      key: 'stack_versions',
      type: 'EQUAL'
    },
    {
      name: 'componentState',
      key: [
        '(host_components/HostRoles/component_name={0})',
        '(host_components/HostRoles/component_name={0}&host_components/HostRoles/state={1})',
        '(host_components/HostRoles/component_name={0}&host_components/HostRoles/desired_admin_state={1})',
        '(host_components/HostRoles/component_name={0}&host_components/HostRoles/maintenance_state={1})'
      ],
      type: 'COMBO',
      isComponentRelatedFilter: true
    }
  ],

  sortProps: [
    {
      name: 'hostName',
      key: 'Hosts/host_name'
    },
    {
      name: 'ip',
      key: 'Hosts/ip'
    },
    {
      name: 'cpu',
      key: 'Hosts/cpu_count'
    },
    {
      name: 'memoryFormatted',
      key: 'Hosts/total_mem'
    },
    {
      name: 'diskUsage',
      //TODO disk_usage is relative property and need support from API, metrics/disk/disk_free used temporarily
      key: 'metrics/disk/disk_free'
    },
    {
      name: 'rack',
      key: 'Hosts/rack_info'
    },
    {
      name: 'loadAvg',
      key: 'metrics/load/load_one'
    }
  ],

  /**
   * Validate and convert input string to valid url parameter.
   * Detect if user have passed string as regular expression or extend
   * string to regexp.
   *
   * @param {String} value
   * @return {String}
   **/
  getRegExp: function (value) {
    value = validator.isValidMatchesRegexp(value) ? value.replace(/(\.+\*?|(\.\*)+)$/, '') + '.*' : '^$';
    value = /^\.\*/.test(value) || value == '^$' ? value : '.*' + value;
    return value;
  },

  /**
   * Sort by host_name by default
   * @method getSortProps
   * @returns {{value: 'asc|desc', name: string, type: 'SORT'}[]}
   */
  getSortProps: function () {
    var controllerName = this.get('name'),
      db = App.db.getSortingStatuses(controllerName);
    if (db && db.everyProperty('status', 'sorting')) {
      App.db.setSortingStatuses(controllerName, {
        name: 'hostName',
        status: 'sorting_asc'
      });
    }
    return this._super();
  },

  /**
   * get query parameters computed from filter properties, sort properties and custom properties of view
   * @param {boolean} [skipNonFilterProperties]
   * @return {Array}
   * @method getQueryParameters
   */
  getQueryParameters: function (skipNonFilterProperties) {
    skipNonFilterProperties = skipNonFilterProperties || false;
    var queryParams = [],
      savedFilterConditions = App.db.getFilterConditions(this.get('name')) || [],
      colPropAssoc = this.get('colPropAssoc'),
      filterProperties = this.get('filterProperties'),
      sortProperties = this.get('sortProps'),
      oldProperties = App.router.get('updateController.queryParams.Hosts');

    this.set('resetStartIndex', false);

    queryParams.pushObjects(this.getPaginationProps());

    savedFilterConditions.forEach(function (filter) {
      var property = filterProperties.findProperty('name', colPropAssoc[filter.iColumn]);
      if (property && filter.value.length > 0 && !filter.skipFilter) {
        var result = {
          key: property.key,
          value: filter.value,
          type: property.type,
          isFilter: true,
          isComponentRelatedFilter: property.isComponentRelatedFilter
        };
        if (filter.type === 'string' && sortProperties.someProperty('name', colPropAssoc[filter.iColumn])) {
          if (Em.isArray(filter.value)) {
            for(var i = 0; i < filter.value.length; i++) {
              filter.value[i] = this.getRegExp(filter.value[i]);
            }
          } else {
            result.value = this.getRegExp(filter.value);
          }
        }
        if (filter.type === 'number' || filter.type === 'ambari-bandwidth') {
          result.type = this.getComparisonType(filter.value);
          result.value = this.getProperValue(filter.value);
        }
        // enter an exact number for RAM filter, need to do a range number match for this
        if (filter.type === 'ambari-bandwidth' && result.type == 'EQUAL' && result.value) {
          var valuePair = this.convertMemoryToRange(filter.value);
          queryParams.push({
            key: result.key,
            value: valuePair[0],
            type: 'MORE'
          });
          queryParams.push({
            key: result.key,
            value: valuePair[1],
            type: 'LESS'
          });
        } else if (filter.type === 'ambari-bandwidth' && result.type != 'EQUAL' && result.value){
          // enter a comparison type, eg > 1, just do regular match
          result.value = this.convertMemory(filter.value);
          queryParams.push(result);
        } else if (filter.type === 'sub-resource') {
          filter.value.forEach(function (item) {
            queryParams.push({
              key: result.key + "/" + item.property,
              value: item.value,
              type: 'EQUAL'
            });
          }, this);
        } else {
          queryParams.push(result);
        }

      }
    }, this);

    if (!oldProperties.findProperty('isHostDetails')) {
      // shouldn't reset start index after coming back from Host Details page
      if (queryParams.filterProperty('isFilter').length !== oldProperties.filterProperty('isFilter').length) {
        queryParams.findProperty('key', 'from').value = 0;
        this.set('resetStartIndex', true);
      } else {
        queryParams.filterProperty('isFilter').forEach(function (queryParam) {
          var oldProperty = oldProperties.filterProperty('isFilter').findProperty('key', queryParam.key);
          if (!oldProperty || JSON.stringify(oldProperty.value) !== JSON.stringify(queryParam.value)) {
            queryParams.findProperty('key', 'from').value = 0;
            this.set('resetStartIndex', true);
          }
        }, this);
      }
    }

    if (!skipNonFilterProperties) {
      queryParams.pushObjects(this.getSortProps());
    }

    return queryParams;
  },


  /**
   * Return value without predicate
   * @param {String} value
   * @return {String}
   */
  getProperValue: function (value) {
    return (['>', '<', '='].contains(value.charAt(0))) ? value.substr(1, value.length) : value;
  },

  /**
   * Return value converted to kilobytes
   * @param {String} value
   * @return {number}
   */
  convertMemory: function (value) {
    var scale = value.charAt(value.length - 1);
    // first char may be predicate for comparison
    value = this.getProperValue(value);
    var parsedValue = parseFloat(value);

    if (isNaN(parsedValue)) {
      return value;
    }

    switch (scale) {
      case 'g':
        parsedValue *= 1048576;
        break;
      case 'm':
        parsedValue *= 1024;
        break;
      case 'k':
        break;
      default:
        //default value in GB
        parsedValue *= 1048576;
    }
    return Math.round(parsedValue);
  },

  /**
   * Return value converted to a range of kilobytes
   * @param {String} value
   * @return {Array}
   */
  convertMemoryToRange: function (value) {
    var scale = value.charAt(value.length - 1);
    // first char may be predicate for comparison
    value = this.getProperValue(value);
    var parsedValue = parseFloat(value);
    if (isNaN(parsedValue)) {
      return [0, 0];
    }
    var parsedValuePair = this.rangeConvertNumber(parsedValue, scale);
    var multiplyingFactor = 1;
    switch (scale) {
      case 'g':
        multiplyingFactor = 1048576;
        break;
      case 'm':
        multiplyingFactor = 1024;
        break;
      case 'k':
        break;
      default:
        //default value in GB
        multiplyingFactor = 1048576;
    }
    parsedValuePair[0]  = Math.round( parsedValuePair[0] * multiplyingFactor);
    parsedValuePair[1]  = Math.round( parsedValuePair[1] * multiplyingFactor);
    return parsedValuePair;
  },

  /**
   * Return value converted to a range of kilobytes
   * eg, return value 1.83 g will target 1.82500 ~ 1.83499 g
   * eg, return value 1.8 k will target 1.7500 ~ 1.8499 k
   * eg, return value 1.8 m will target 1.7500 ~ 1.8499 m
   * @param {number} value
   * @param {String} scale
   * @return {Array}
   */
  rangeConvertNumber: function (value, scale) {
    if (isNaN(value)) {
      return [0, 0];
    }
    var valuePair = [];
    switch (scale) {
      case 'g':
        valuePair = [value - 0.005000, value + 0.004999999];
        break;
      case 'm':
      case 'k':
        valuePair = [value - 0.05000, value + 0.04999];
        break;
      default:
        //default value in GB
        valuePair = [value - 0.005000, value + 0.004999999];
    }
    return valuePair;
  },

  /**
   * Return comparison type depending on populated predicate
   * @param {string} value
   * @return {string}
   */
  getComparisonType: function (value) {
    var comparisonChar = value.charAt(0);
    var result = 'EQUAL';
    if (isNaN(comparisonChar)) {
      switch (comparisonChar) {
        case '>':
          result = 'MORE';
          break;
        case '<':
          result = 'LESS';
          break;
      }
    }
    return result;
  },

  labelValueMap: {},

  /**
   * Filter hosts by componentName of <code>component</code>
   * @param {App.HostComponent} component
   */
  filterByComponent: function (component) {
    if (!component) return;
    var componentName = component.get('componentName');
    var displayName = App.format.role(componentName, false);
    var colPropAssoc = this.get('colPropAssoc');
    var map = this.get('labelValueMap');

    var filterForComponent = {
      iColumn: 15,
      value: componentName + ':ALL',
      type: 'string'
    };
    map[displayName] = componentName;
    map['All'] = 'ALL';
    var filterStr = '"' + displayName + '"' + ': "All"';
    App.db.setFilterConditions(this.get('name'), [filterForComponent]);
    App.db.setComboSearchQuery(this.get('name'), filterStr);
  },

  /**
   * Filter hosts by stack version and state
   * @param {String} displayName
   * @param {Array} states
   */
  filterByStack: function (displayName, states) {
    if (Em.isNone(displayName) || Em.isNone(states) || !states.length) return;
    var colPropAssoc = this.get('colPropAssoc');
    var map = this.get('labelValueMap');
    var stateFilterStrs = [];

    var versionFilter = {
      iColumn: 16,
      value: displayName,
      type: 'string'
    };
    var stateFilter = {
      iColumn: 17,
      value: states,
      type: 'string'
    };
    map["Stack Version"] = colPropAssoc[versionFilter.iColumn];
    map["Version State"] = colPropAssoc[stateFilter.iColumn];
    stateFilter.value.forEach(function(state) {
      map[App.HostStackVersion.formatStatus(state)] = state;
      stateFilterStrs.push('"Version State": "' + App.HostStackVersion.formatStatus(state) + '"');
    });
    var versionFilterStr = '"Stack Version": "' + versionFilter.value + '"';
    App.db.setFilterConditions(this.get('name'), [versionFilter, stateFilter]);
    App.db.setComboSearchQuery(this.get('name'), [versionFilterStr, stateFilterStrs.join(' ')].join(' '));
  },

  goToHostAlerts: function (event) {
    var host = event && event.context;
    if (host) {
      App.router.transitionTo('main.hosts.hostDetails.alerts', host);
    }
  },

  /**
   * remove selected hosts
   */
  removeHosts: function () {
    var hosts = this.get('content');
    var selectedHosts = hosts.filterProperty('isChecked');
    this.get('fullContent').removeObjects(selectedHosts);
  },

  /**
   * remove hosts with id equal host_id
   * @param {String} host_id
   */
  checkRemoved: function (host_id) {
    var hosts = this.get('content');
    var selectedHosts = hosts.filterProperty('id', host_id);
    this.get('fullContent').removeObjects(selectedHosts);
  },

  /**
   * associations between host property and column index
   * @type {Array}
   */
  colPropAssoc: function () {
    var associations = [];
    associations[0] = 'healthClass';
    associations[1] = 'hostName';
    associations[2] = 'ip';
    associations[3] = 'cpu';
    associations[4] = 'memoryFormatted';
    associations[5] = 'loadAvg';
    associations[6] = 'hostComponents';
    associations[7] = 'criticalWarningAlertsCount';
    associations[8] = 'componentsWithStaleConfigsCount';
    associations[9] = 'componentsInPassiveStateCount';
    associations[10] = 'selected';
    associations[11] = 'hostStackVersion';
    associations[12] = 'rack';
    associations[13] = 'services';
    associations[14] = 'state';
    associations[15] = 'componentState';
    associations[16] = 'version';
    associations[17] = 'versionState';
    return associations;
  }.property()

});
