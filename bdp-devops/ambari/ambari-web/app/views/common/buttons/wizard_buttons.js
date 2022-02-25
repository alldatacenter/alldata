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

App.WizardNextButton = App.ButtonProgressView.extend({
  classNames: ['pull-right'],
  isInProgressBinding: 'App.router.nextBtnClickInProgress',
  doSpinRight: false,
  buttonClassNames: 'btn-success pull-right',
  template: Em.Handlebars.compile('{{t common.next}} &rarr;'),
  disabledBinding: 'controller.isSubmitDisabled'
});

App.WizardBackButton = App.ButtonProgressView.extend({
  classNames: ['pull-left'],
  isInProgressBinding: 'App.router.backBtnClickInProgress',
  template: Em.Handlebars.compile('&larr; {{t common.back}}'),
  buttonClassNames: 'btn-default pull-left',
  disabledBinding: 'controller.isBackButtonDisabled'
});
