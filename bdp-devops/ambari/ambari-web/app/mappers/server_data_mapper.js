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

/**
 * initialize common cache container for mappers
 * App.cache contains shared data, used for syncronizing incoming server data among mappers
 */
App.cache = {
  'previousHostStatuses': {},
  'previousComponentStatuses': {},
  'previousComponentPassiveStates': {},
  'staleConfigsComponentHosts': {},
  'services': [],
  'currentConfigVersions': {}
};

App.cache.clear = function () {
  var clear = App.cache.clear;
  App.cache = {
    'previousHostStatuses': {},
    'previousComponentStatuses': {},
    'previousComponentPassiveStates': {},
    'services': [],
    'currentConfigVersions': {}
  };
  App.cache.clear = clear;
};

App.store.reopen({
  safeLoadMany: function(model, records) {
    try {
      this.loadMany(model, records);
    } catch (e) {
      console.debug('Resolve uncommitted records before load');
      this.fastCommit();
      this.loadMany(model, records);
    }
  },

  safeLoad: function(model, record) {
    try {
      this.load(model, record);
    } catch (e) {
      console.debug('Resolve uncommitted record before load');
      this.fastCommit();
      this.load(model, record);
    }
  },

  /**
   * App.store.commit() - creates new transaction,
   * and them move all records from old to new transactions which is expensive
   *
   * We should use only defaultTransaction,
   * and then we can stub <code>removeCleanRecords</code> method,
   * because it would remove from and add records to the same (default) transaction
   */
  fastCommit: function() {
    console.time('store commit');
    App.store.defaultTransaction.commit();
    console.timeEnd('store commit');
  }
});

App.ServerDataMapper = Em.Object.extend({
  jsonKey: false,
  map: function (json) {
    if (json) {
      var model = this.get('model');
      var jsonKey = this.get('jsonKey');

      if (jsonKey && json[jsonKey]) { // if data come as { hdfs: {...} }
        json = json[jsonKey];
      }

      $.each(json, function (field, value) {
        model.set(field, value);
      })
    }
  }
});


App.QuickDataMapper = App.ServerDataMapper.extend({
  config: {},
  model: null,
  map: function (json) {
    if (!this.get('model')) {
      return;
    }

    if (json.items) {
      var result = [];

      json.items.forEach(function (item) {
        result.push(this.parseIt(item, this.config));
      }, this);

      App.store.safeLoadMany(this.get('model'), result);
    }
  },

  parseIt: function (data, config) {
    var result = {};
    for ( var i in config) {
      if (i.substr(0, 1) === '$') {
        i = i.substr(1, i.length);
        result[i] = config['$' + i];
      } else {
        var isSpecial = false;
        if (i.substr(-5) == '_type') {
          var prefix = i.substr(0, i.length - 5);
          isSpecial = config[prefix + '_key'] != null;
        } else if (i.substr(-4) == '_key') {
          var prefix = i.substr(0, i.length - 4);
          isSpecial = config[prefix + '_type'] != null;
        }
        if (!isSpecial && typeof config[i] == 'string') {
          result[i] = this.getJsonProperty(data, config[i]);
        } else if (typeof config[i] == 'object') {
          result[i] = [];
          var _data = this.getJsonProperty(data, config[i + '_key']);
          var _type = config[i + '_type'];
          var l = _data.length;
          for ( var index = 0; index < l; index++) {
            if (_type == 'array') {
              result[i].push(this.getJsonProperty(_data[index], config[i].item));
            } else {
              result[i].push(this.parseIt(_data[index], config[i]));
            }
          }
          // As for 'widgets', just show the original order
          if(_type == 'array' && i != 'widgets'){
            result[i] = result[i].sort();
          }
        }
      }
    }
    return result;
  },

  getJsonProperty: function (json, path) {
    var pathArr = path.split('.');
    var current = json;
    while (pathArr.length && current) {
      if (pathArr[0].substr(-1) == ']') {
        var index = parseInt(pathArr[0].substr(-2, 1));
        var attr = pathArr[0].substr(0, pathArr[0].length - 3);
        if (attr in current) {
          current = current[attr][index];
        }
      } else {
        current = current[pathArr[0]];
      }
      pathArr.splice(0, 1);
    }
    return current;
  },

  /**
   * properly delete record from model
   * @param item
   */
  deleteRecord: function (item) {
    if (item.get('isLoaded')) {
      item.deleteRecord();
      App.store.fastCommit();
      item.get('stateManager').transitionTo('loading');
    }
  },
  /**
   * check mutable fields whether they have been changed and if positive
   * return host object only with properties, that contains new value
   * @param current
   * @param previous
   * @param fields
   * @return {*}
   */
  getDiscrepancies: function (current, previous, fields) {
    var result = {};
    if (previous) {
      fields.forEach(function (field) {
        if (Array.isArray(current[field])) {
          if (JSON.stringify(current[field]) !== JSON.stringify(previous[field])) {
            result[field] = current[field];
            result.isLoadNeeded = true;
          }
        } else {
          if (current[field] != previous[field]) result[field] = current[field];
        }
      });
      return result;
    }
    return current;
  },

  /**
   * Binary search <code>searchElement</code> in the array (should be sorted!)
   * @param {number[]|string[]} array
   * @param {number|string} searchElement
   * @returns {number} position of the needed element or negative value, if value wasn't found
   * @method binaryIndexOf
   */
  binaryIndexOf: function (array, searchElement) {
    var minIndex = 0;
    var maxIndex = array.length - 1;
    var currentIndex;
    var currentElement;
    var resultIndex;

    if (array[0] > searchElement || array[array.length - 1] < searchElement) {
      return -1;
    }
    while (minIndex <= maxIndex) {
      resultIndex = currentIndex = (minIndex + maxIndex) / 2 | 0;
      currentElement = array[currentIndex];

      if (currentElement < searchElement) {
        minIndex = currentIndex + 1;
      }
      else
      if (currentElement > searchElement) {
        maxIndex = currentIndex - 1;
      }
      else {
        return currentIndex;
      }
    }

    return ~maxIndex;
  },

  /**
   * @param {Em.Object} record
   * @param {object} event
   * @param {object} config
   */
  updatePropertiesByConfig: function(record, event, config) {
    if (record.get('isLoaded')) {
      for (var internalProp in config) {
        let externalProp = config[internalProp];
        if (!Em.isNone(event[externalProp])) {
          record.set(internalProp, event[externalProp]);
        }
      }
    }
  }

});
