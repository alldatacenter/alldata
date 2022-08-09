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
var validator = require('utils/validator');

require('controllers/main/admin/serviceAccounts_controller');

App.HighAvailabilityWizardStep1Controller = Em.Controller.extend({
  name: "highAvailabilityWizardStep1Controller",

  isHawqInstalled: false,

  isNameServiceIdValid: function () {
    return validator.isValidNameServiceId(this.get('content.nameServiceId'));
  }.property('content.nameServiceId'),

  setHawqInstalled: function() {
    this.set('isHawqInstalled', App.Service.find().someProperty('serviceName', 'HAWQ'));
  },

  next: function () {
    if (this.get('isNameServiceIdValid')) {
      App.router.send('next');
    }
  }

});

