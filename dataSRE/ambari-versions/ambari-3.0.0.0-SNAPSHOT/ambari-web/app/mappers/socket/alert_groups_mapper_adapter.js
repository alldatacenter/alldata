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

App.alertGroupsMapperAdapter = App.QuickDataMapper.create({

  /**
   * @param {object} event
   */
  map: function(event) {
    event.groups.forEach((alertGroup) => {
      if (event.updateType === 'UPDATE' || event.updateType === 'CREATE') {
        const {definitions, targets} = alertGroup;
        if (definitions) {
          alertGroup.definitions = this.convertIdsToObjects(definitions);
        }
        if (targets) {
          alertGroup.targets = this.convertIdsToObjects(targets);
        }
        App.alertGroupsMapper.map({
          items: [
            {
              AlertGroup: alertGroup
            }
          ]
        }, true);
      } else if (event.updateType === 'DELETE') {
        this.deleteRecord(App.AlertGroup.find(alertGroup.id));
      }
    });
    App.router.get('manageAlertGroupsController').toggleProperty('changeTrigger');
  },

  convertIdsToObjects: function (arr) {
    return arr.map(id => ({
      id
    }));
  }
});
