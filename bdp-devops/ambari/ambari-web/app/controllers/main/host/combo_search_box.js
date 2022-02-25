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

App.MainHostComboSearchBoxController = Em.Controller.extend({
  name: 'mainHostComboSearchBoxController',
  currentSuggestion: [],
  page_size: 10,
  getPropertySuggestions: function(facet, searchTerm) {
    facet = (facet == 'hostName')? 'host_name' : facet;
    return App.ajax.send({
      name: 'hosts.with_searchTerm',
      sender: this,
      data: {
        facet: facet,
        searchTerm: searchTerm,
        page_size: this.get('page_size')
      },
      success: 'getPropertySuggestionsSuccess',
      error: 'commonSuggestionErrorCallback'
    });
  },

  getPropertySuggestionsSuccess: function(data, opt, params) {
    this.updateSuggestion(data.items.map(function(item) {
      return item.Hosts[params.facet];
    }));
  },

  updateSuggestion: function(data) {
    var controller = App.router.get('mainHostComboSearchBoxController');
    controller.set('currentSuggestion', data);
  },

  commonSuggestionErrorCallback:function() {
    // handle suggestion error
  },

  isComponentStateFacet: function(facet) {
    return App.StackServiceComponent.find(facet).get('isLoaded');
  },

  isComplexHealthStatusFacet: function(facet) {
    return ['health-status-WITH-ALERTS', 'health-status-RESTART', 'health-status-PASSIVE_STATE'].contains(facet);
  },

  generateQueryParam: function(param) {
    var expressions = param.key;
    var pHash = this.createComboParamHash(param);
    return this.createComboParamURL(pHash, expressions);
  },

  /**
   * @param pHash {k1:v1, k2:[v1,v2], ...}
   * @param expression
   * @returns {string} 'k1=v1&(k2=v1|k2=v2)'
   */
  createComboParamURL: function(pHash, expressions) {
    var self = this;
    var result = '';
    for (var key in pHash) {
      var v = pHash[key];
      if (Em.isArray(v)) {
        var ex = '(';
        v.forEach(function(item) {
          var expression = self.getComboParamURL(item, expressions);
          var toAdd = expression.replace('{0}', key);
          toAdd = toAdd.replace('{1}', item);
          ex += toAdd + '|';
        });
        ex = ex.substring(0, ex.length - 1);
        result += ex + ')';
      } else {
        var expression = self.getComboParamURL(v, expressions);
        var ex = expression.replace('{0}', key);
        ex = ex.replace('{1}', v);
        result += ex;
      }
      result += '|';
    }

    return result.substring(0, result.length - 1);
  },

  /**
   * @param value //value of component state
   * @returns expression //url of query of state
   */
  getComboParamURL: function(value, expressions) {
    var expression = expressions[1];
    switch (value) {
      case 'ALL':
        expression = expressions[0];
        break;
      case 'STARTED':
      case 'STARTING':
      case 'INSTALLED':
      case 'STOPPING':
      case 'INSTALL_FAILED':
      case 'INSTALLING':
      case 'UPGRADE_FAILED':
      case 'UNKNOWN':
      case 'DISABLED':
      case 'INIT':
        break;
      case 'INSERVICE':
      case 'DECOMMISSIONING':
      case 'DECOMMISSIONED':
      case 'RS_DECOMMISSIONED':
        expression = expressions[2];
        break;
      case 'ON':
      case 'OFF':
        expression = expressions[3];
        break;
    }
    return expression;
  },

  /**
   * @param param ['k1:v1','k2:v1', 'k2:v2'] or 'k1:v1'
   * @returns {k1:v1, k2:[v1,v2], ...}
   */
  createComboParamHash: function(param) {
    var pHash = {};
    if (Em.isArray(param.value)) {
      param.value.forEach(function(item) {
        var values = item.split(':');
        var k = values[0];
        var v = values[1];
        if (!pHash[k]) {
          pHash[k] = v;
        } else {
          if (Em.isArray(pHash[k])) {
            if (pHash[k].indexOf(v) == -1) {
              pHash[k].push(v);
            }
          } else {
            pHash[k] = [pHash[k], v];
          }
        }
      });
    } else {
      var values = param.value.split(':');
      pHash[values[0]] = values[1];
    }
    return pHash;
  }
});