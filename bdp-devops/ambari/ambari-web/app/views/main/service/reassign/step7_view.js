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

App.ReassignMasterWizardStep7View = App.HighAvailabilityProgressPageView.extend({

  headerTitle: Em.I18n.t('services.reassign.step7.header'),

  noticeInProgress: Em.I18n.t('services.reassign.step7.info'),

  noticeFailed: Em.I18n.t('services.reassign.step7.failed'),

  noticeCompleted: Em.I18n.t('services.reassign.step7.success'),

  submitButtonText: Em.I18n.t('common.complete') + ' &rarr;',

  templateName: require('templates/main/service/reassign/step7'),

  labelWidth: 'col-md-5'
});
