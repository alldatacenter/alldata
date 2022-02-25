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
 * Provide methods for config-themes loading from server and saving them into models
 *
 * @type {Em.Mixin}
 */
App.ThemesMappingMixin = Em.Mixin.create({

  /**
   * Load config themes from server and save them into models
   * @param {string} serviceName
   * @returns {$.ajax}
   * @method loadConfigTheme
   */
  loadConfigTheme: function(serviceName) {
    const dfd = $.Deferred();
    if (App.Tab.find().mapProperty('serviceName').contains(serviceName)) {
      dfd.resolve();
    } else {
      App.ajax.send({
        name: 'configs.theme',
        sender: this,
        data: {
          serviceName: serviceName,
          stackVersionUrl: App.get('stackVersionURL')
        },
        success: '_saveThemeToModel'
      }).complete(dfd.resolve);
    }
    return dfd.promise();
  },

  /**
   * Success-callback for <code>loadConfigTheme</code>
   * runs <code>themeMapper<code>
   * @param {object} data
   * @param opt
   * @param params
   * @private
   * @method saveThemeToModel
   */
  _saveThemeToModel: function(data, opt, params) {
    App.themesMapper.map(data, [params.serviceName]);
  },

  /**
   * Load themes for specified services by one API call
   *
   * @method loadConfigThemeForServices
   * @param {String|String[]} serviceNames
   * @returns {$.ajax}
   */
  loadConfigThemeForServices: function (serviceNames) {
    return App.ajax.send({
      name: 'configs.theme.services',
      sender: this,
      data: {
        serviceNames: Em.makeArray(serviceNames).join(','),
        stackVersionUrl: App.get('stackVersionURL')
      },
      success: '_loadConfigThemeForServicesSuccess',
      error: '_loadConfigThemeForServicesError'
    });
  },

  /**
   * Success-callback for <code>loadConfigThemeForServices</code>
   * @param {object} data
   * @param opt
   * @param params
   * @private
   * @method _loadConfigThemeForServicesSuccess
   */
  _loadConfigThemeForServicesSuccess: function(data, opt, params) {
    if (!data.items.length) return;
    App.themesMapper.map({
      items: data.items.mapProperty('themes').reduce(function(p,c) {
        return p.concat(c);
      })
    }, params.serviceNames.split(','));
  },

  /**
   * Error-callback for <code>loadConfigThemeForServices</code>
   * @param {object} request
   * @param {object} ajaxOptions
   * @param {string} error
   * @param {object} opt
   * @param {object} params
   * @private
   * @method _loadConfigThemeForServicesError
   */
  _loadConfigThemeForServicesError: function(request, ajaxOptions, error, opt, params) {

  }

});