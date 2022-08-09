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

/**
 * THIS IS NOT USED FOR NOW
 * FOR CONFIG GROUPS WE ARE USING OLD MODELS AND LOGIC
 */

var App = require('app');

App.ConfigAction = DS.Model.extend({

  componentName: DS.attr('string'),

  /**
   * Name of the config that is being affected with the condition
   */
  configName: DS.attr('string'),

  /**
   * File name to which the config getting affected belongs
   */
  fileName: DS.attr('string'),

  /**
   * conditional String which can be evaluated to boolean result.
   * If evaluated result of this staring is true then use the statement provided by `then` attribute.
   * Otherwise use the attribute provided by `else` attributes
   */
  if: DS.attr('string'),
  then: DS.attr('string'),
  else: DS.attr('string'),
  hostComponentConfig: DS.attr('object', {
    defaultValue: function () { return {}; }
  }),
  actionType: DS.attr('string'),
  popupProperties: DS.attr('object', {
    defaultValue: function () { return {}; }
  }),
  serviceName: DS.attr('string'),
  // TODO remove after stack advisor is able to handle this case
  // dependencies is used as workaround for hadoop.proxyuser.{{hiveUser}}.hosts after adding Hive Server Interactive from Install Wizard
  dependencies: DS.attr('object', {
    defaultValue: function () {
      return {};
    }
  })
});

App.ConfigAction.FIXTURES = [
  {
    id:1,
    component_name: 'HIVE_SERVER_INTERACTIVE',
    config_name: "enable_hive_interactive",
    file_name: "hive-interactive-env.xml",
    if:'${hive-interactive-env/enable_hive_interactive}',
    then:'add',
    else: 'delete',
    // TODO remove after stack advisor is able to handle this case
    // dependencies is used as workaround for hadoop.proxyuser.{{hiveUser}}.hosts after adding Hive Server Interactive from Install Wizard
    dependencies: {
      initializer: {
        name: 'AddHiveServerInteractiveInitializer',
        setupKeys: ['hiveUser'],
        componentNames: ['HIVE_SERVER', 'WEBHCAT_SERVER', 'HIVE_METASTORE', 'HIVE_SERVER_INTERACTIVE']
      },
      properties: [
        {
          fileName: 'core-site',
          nameTemplate: 'hadoop.proxyuser.{{hiveUser}}.hosts',
          isHostsList: true,
          isHostsArray: false
        }
      ],
      foreignKeys: [
        {
          key: 'hiveUser',
          fileName: 'hive-env.xml',
          propertyName: 'hive_user'
        }
      ]
    }
  },
  {
    id: 2,
    service_name: 'YARN',
    component_name: 'RESOURCEMANAGER',
    config_name: "capacity-scheduler",
    file_name: "capacity-scheduler.xml",
    action_type: "showPopup",
    popup_properties: {
      primaryButton: {
        label: Em.I18n.t('popup.confirmation.refreshYarnQueues.buttonText'),
        metaData: {
          name: 'service.item.refreshQueueYarnRequest',
          command: "REFRESHQUEUES",
          context: Em.I18n.t('services.service.actions.run.yarnRefreshQueues.context')
        }
      },
      body: Em.I18n.t('popup.confirmation.refreshYarnQueues.body'),
      conditionalWarning: true,
      warningMessage: Em.I18n.t('popup.warning.refreshYarnQueues.body'),
      errorMessage: Em.I18n.t('services.service.actions.run.yarnRefreshQueues.error')
    }
  }
];
