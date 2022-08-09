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
 * Provide methods for config-groups loading from server and saving them into models
 *
 * @type {Em.Mixin}
 */
App.GroupsMappingMixin = Em.Mixin.create(App.TrackRequestMixin, {

  /**
   * Load config groups
   * @param {String[]} serviceNames
   * @returns {$.Deferred}
   * @method loadConfigGroups
   */
  loadConfigGroups: function (serviceNames) {
    var dfd = $.Deferred();
    if (!serviceNames || serviceNames.length === 0) {
      this.set('configGroupsAreLoaded', true);
      dfd.resolve();
    } else {
      this.trackRequest(App.ajax.send({
        name: 'configs.config_groups.load.services',
        sender: this,
        data: {
          serviceNames: serviceNames.join(','),
          dfd: dfd
        },
        success: 'saveConfigGroupsToModel'
      }));
    }
    return dfd.promise();
  },

  /**
   * Runs <code>configGroupsMapper<code>
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method saveConfigGroupsToModel
   */
  saveConfigGroupsToModel: function (data, opt, params) {
    App.configGroupsMapper.map(data, false, params.serviceNames.split(','));
    this.set('configGroupsAreLoaded', true);
    params.dfd.resolve();
  }

});
