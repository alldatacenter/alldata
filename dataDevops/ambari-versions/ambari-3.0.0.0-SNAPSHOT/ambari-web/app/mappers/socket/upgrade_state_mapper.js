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

App.upgradeStateMapper = App.QuickDataMapper.create({

  /**
   * @param {object} event
   */
  map: function (event) {
    var controller = App.router.get('mainAdminStackAndUpgradeController');
    if (event.type === 'CREATE') {
      controller.restoreLastUpgrade({Upgrade: event});
    }
    //TODO rename type to eventType
    if (event.type === 'UPDATE' && controller.get('upgradeId') === event.request_id) {
      if (!Em.isNone(event.request_status)) {
        App.set('upgradeState', event.request_status);
        controller.setDBProperty('upgradeState', event.request_status);
      }
      if (!Em.isNone(event.suspended)) {
        controller.set('isSuspended', event.suspended);
        controller.setDBProperty('isSuspended', event.suspended);
      }
    }
  }
});
