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


window.App = require('app');

require('config');

var browserLanguage = '';

try {
  browserLanguage = navigator.language.slice(0, 2);
  require('locales/' + browserLanguage +'/messages');
} catch (error) {
  require('messages');
}

require('utils');
require('mixins');
require('models');
require('controllers');
require('templates');
require('views');
require('utils/handlebars_helpers');
require('router');

require('utils/ajax/ajax');
require('utils/ajax/ajax_queue');
require('utils/updater');
require('utils/action_sequence');
require('utils/load_timer');

require('mappers');

require('utils/http_client');
require('utils/host_progress_popup');

App.initialize();

console.log('after initialize');
console.log('TRACE: app.js-> localStorage:Ambari.authenticated=' + localStorage.getItem('Ambari' + 'authenticated'));
console.log('TRACE: app.js-> localStorage:currentStep=' + localStorage.getItem(App.get('router').getLoginName() + 'Installer' + 'currentStep'));
console.log('TRACE: app.js-> router.authenticated=' + App.get('router.loggedIn'));
