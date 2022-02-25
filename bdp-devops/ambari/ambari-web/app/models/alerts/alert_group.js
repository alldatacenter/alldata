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
 * Represents an alert-group on the cluster.
 * A alert group is a collection of alert definitions
 *
 * Alert group hierarchy is at 2 levels. For
 * each service there is a 'Default' alert group
 * containing all definitions , this group is read-only
 *
 * User can create new alert group containing alert definitions from
 * any service.
 */
App.AlertGroup = DS.Model.extend({

  name: DS.attr('string'),

  description: DS.attr('string'),

  /**
   * Is this group default for some service
   * @type {boolean}
   */
  default: DS.attr('boolean'),

  /**
   * @type {App.AlertDefinition[]}
   */
  definitions: DS.hasMany('App.AlertDefinition'),

  /**
   * @type {App.AlertNotification[]}
   */
  targets: DS.hasMany('App.AlertNotification'),

  /**
   * @type {string}
   */
  displayName: function () {
    var name = App.config.truncateGroupName(App.format.role(this.get('name'), true));
    return this.get('default') ? name + ' Default' : name;
  }.property('name', 'default'),

  /**
   * @type {string}
   */
  displayNameDefinitions: Em.computed.format('{0} ({1})', 'displayName', 'definitions.length'),

  isAddDefinitionsDisabled: Em.computed.alias('default')

});
App.AlertGroup.FIXTURES = [];


