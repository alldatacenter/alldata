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

var PreCondition = Ember.Object.extend({
  displayText: null,
  checked: false,
  visibilityCriteria: function() { return true; },

  hidden: function() {
    return !this.get('visibilityCriteria')();
  }.property('visibilityCriteria'),

  satisfied: function() {
    return this.get('checked') || this.get('hidden');
  }.property('checked', 'hidden')
});

App.KerberosWizardStep1Controller = Em.Controller.extend({
  name: "kerberosWizardStep1Controller",

  selectedItem: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'),

  isSubmitDisabled: Em.computed.someBy('selectedOption.preConditions', 'satisfied', false),

  options: Em.A([
    Em.Object.create({
      displayName: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'),
      value: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'),
      preConditions: [
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc.condition.1')
        }),
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc.condition.2')
        }),
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc.condition.3')
        }),
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc.condition.4'),
          visibilityCriteria: function() { return App.Service.find().someProperty('serviceName', 'ONEFS') }
        })
      ]
    }),
    Em.Object.create({
      displayName: Em.I18n.t('admin.kerberos.wizard.step1.option.ad'),
      value: Em.I18n.t('admin.kerberos.wizard.step1.option.ad'),
      preConditions: [
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.ad.condition.1')
        }),
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.ad.condition.2')
        }),
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.ad.condition.3')
        }),
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.ad.condition.4')
        }),
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.ad.condition.5')
        })
      ]
    }),
    Em.Object.create({
      displayName: Em.I18n.t('admin.kerberos.wizard.step1.option.ipa'),
      value: Em.I18n.t('admin.kerberos.wizard.step1.option.ipa'),
      preConditions: [
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.ipa.condition.1')
        }),
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.ipa.condition.2')
        }),
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.ipa.condition.3')
        })
      ]
    }),
    Em.Object.create({
      displayName: Em.I18n.t('admin.kerberos.wizard.step1.option.manual'),
      value: Em.I18n.t('admin.kerberos.wizard.step1.option.manual'),
      preConditions: [
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.manual.condition.1')
        }),
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.manual.condition.2')
        }),
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.manual.condition.3')
        }),
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.manual.condition.4')
        }),
        PreCondition.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.manual.condition.5')
        })
      ]
    })
  ]),

  /**
   * precondition for the selected KDC option
   * whenever the KDC type is changed, all checkboxes for the precondition should be unchecked
   */
  selectedOption: function () {
    var options = this.get('options');
    options.forEach(function (option) {
      option.preConditions.setEach('checked', false);
    })
    return this.get('options').findProperty('value', this.get('selectedItem'));
  }.property('selectedItem'),


  loadStep: function () {
  },

  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('next');
    }
  }
});
