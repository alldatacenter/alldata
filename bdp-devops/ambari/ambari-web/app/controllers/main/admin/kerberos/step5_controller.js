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
var stringUtils = require('utils/string_utils');
var fileUtils = require('utils/file_utils');

App.KerberosWizardStep5Controller = App.KerberosProgressPageController.extend(App.AddSecurityConfigs, {
  name: 'kerberosWizardStep5Controller',

  /**
   * @type {Array}
   */
  csvData: [],

  /**
   * @type {Array}
   */
  kdcProperties: [
    {
      key: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'),
      properties: ['kdc_type', 'kdc_hosts', 'realm', 'executable_search_paths']
    },
    {
      key: Em.I18n.t('admin.kerberos.wizard.step1.option.ad'),
      properties: ['kdc_type', 'kdc_hosts', 'realm', 'ldap_url', 'container_dn', 'executable_search_paths']
    },
    {
      key: Em.I18n.t('admin.kerberos.wizard.step1.option.ipa'),
      properties: ['kdc_type', 'kdc_hosts', 'realm', 'executable_search_paths']
    },
    {
      key: Em.I18n.t('admin.kerberos.wizard.step1.option.manual'),
      properties: ['kdc_type', 'realm', 'executable_search_paths']
    }
  ],

  isCSVRequestInProgress: false,

  submit: function() {
    App.router.send('next');
  },

  /**
   * get CSV data from the server
   */
  getCSVData: function (skipDownload) {
    this.set('isCSVRequestInProgress', true);
    return App.ajax.send({
      name: 'admin.kerberos.cluster.csv',
      sender: this,
      data: {
        'skipDownload': skipDownload
      },
      success: 'getCSVDataSuccessCallback',
      error: 'getCSVDataSuccessCallback'
    });
  },

  /**
   * get CSV data from server success callback
   * @param data {string}
   * @param opt {object}
   * @param params {object}
   */
  getCSVDataSuccessCallback: function (data, opt, params) {
    this.set('csvData', this.prepareCSVData(data.split('\n')));
    this.set('isCSVRequestInProgress', false);
    if (!Em.get(params, 'skipDownload')) {
      fileUtils.downloadTextFile(stringUtils.arrayToCSV(this.get('csvData')), 'csv', 'kerberos.csv');
    }
  },

  prepareCSVData: function (array) {
    for (var i = 0; i < array.length; i += 1) {
      array[i] = array[i].split(',');
    }

    return array;
  },



  /**
   * Send request to unkerberisze cluster
   * @returns {$.ajax}
   */
  unkerberizeCluster: function () {
    return App.ajax.send({
      name: 'admin.unkerberize.cluster',
      sender: this,
      success: 'goToNextStep',
      error: 'goToNextStep'
    });
  },


  goToNextStep: function() {
    this.clearStage();
    App.router.transitionTo('step5');
  },

  isSubmitDisabled: Em.computed.notExistsIn('status', ['COMPLETED', 'FAILED']),

  confirmProperties: function () {
    var wizardController = App.router.get('kerberosWizardController');
    var kdc_type = wizardController.get('content.kerberosOption'),
        kdcTypeProperties = this.get('kdcProperties').filter(function (item) {
          return item.key === kdc_type;
        }),
      properties = kdcTypeProperties.length ? kdcTypeProperties[0].properties : [];

    return wizardController.get('content.serviceConfigProperties').filter(function (item) {
      return properties.contains(item.name);
    }).map(function (item) {
      item['label'] = Em.I18n.t('admin.kerberos.wizard.step5.' + item['name'] + '.label');
      return item;
    });
  }.property('App.router.kerberosWizardController.content.@each.serviceConfigProperties')
});
