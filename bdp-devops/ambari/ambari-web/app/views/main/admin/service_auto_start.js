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

App.MainAdminServiceAutoStartView = Em.View.extend({
  templateName: require('templates/main/admin/service_auto_start'),

  /**
   * @type {boolean}
   */
  isSaveDisabled: Em.computed.or('!controller.isModified', 'controller.saveInProgress'),

  /**
   * @type {boolean}
   * @default false
   */
  isDisabled: false,

  /**
   * @type {boolean}
   */
  skipCyclicCall: false,

  /**
   * @type {boolean}
   */
  allComponentsChecked: false,

  /**
   * @type {?object}
   */
  switcher: null,

  didInsertElement: function () {
    this.set('isDisabled', !App.isAuthorized('CLUSTER.MANAGE_AUTO_START'));
    this.get('controller').load();
  },

  onValueChange: function () {
    if (this.get('switcher')) {
      this.get('switcher').bootstrapSwitch('state', this.get('controller.isGeneralRecoveryEnabled'));
    }
  }.observes('controller.isGeneralRecoveryEnabled'),

  /**
   * Init switcher plugin.
   *
   * @method initSwitcher
   */
  initSwitcher: function () {
    const self = this.get('parentView');
    if (self.get('controller.isLoaded')) {
      self.set('switcher', $(".general-auto-start>input").bootstrapSwitch({
        state: self.get('controller.isGeneralRecoveryEnabled'),
        onText: Em.I18n.t('common.enabled'),
        offText: Em.I18n.t('common.disabled'),
        offColor: 'default',
        onColor: 'success',
        disabled: self.get('isDisabled'),
        handleWidth: Math.max(Em.I18n.t('common.enabled').length, Em.I18n.t('common.disabled').length) * 8,
        onSwitchChange: (event, state) => {
          self.set('controller.isGeneralRecoveryEnabled', state);
        }
      }));
    }
  },

  observeAllComponentsChecked: function() {
    if (this.get('skipCyclicCall')) {
      this.set('skipCyclicCall', false);
    } else {
      this.get('controller.componentsConfigsGrouped').setEach('recoveryEnabled', this.get('allComponentsChecked'));
    }
  }.observes('allComponentsChecked'),

  observesEachComponentChecked: function() {
    const components = this.get('controller.componentsConfigsGrouped');
    if (this.get('allComponentsChecked') && components.someProperty('recoveryEnabled', false)) {
      this.set('skipCyclicCall', true);
      this.set('allComponentsChecked', false);
    } else if (!this.get('allComponentsChecked') && components.everyProperty('recoveryEnabled', true)) {
      this.set('skipCyclicCall', true);
      this.set('allComponentsChecked', true);
    }
  }.observes('controller.componentsConfigsGrouped.@each.recoveryEnabled')

});

