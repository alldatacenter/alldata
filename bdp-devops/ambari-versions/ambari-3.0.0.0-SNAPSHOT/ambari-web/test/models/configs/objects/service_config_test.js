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

require('models/configs/objects/service_config');

var serviceConfig,
    configs = [
      App.ServiceConfigProperty.create({
        'name': 'p1',
        'isVisible': true,
        'hiddenBySection': false,
        'hiddenBySubSection': false,
        'isRequiredByAgent': true,
        'isValid': true,
        'isValidOverride': true
      }),
      App.ServiceConfigProperty.create({
        'name': 'p2',
        'isVisible': false,
        'hiddenBySection': false,
        'hiddenBySubSection': false,
        'isRequiredByAgent': true,
        'isValid': true,
        'isValidOverride': true
      }),
      App.ServiceConfigProperty.create({
        'name': 'p3',
        'isVisible': true,
        'hiddenBySection': true,
        'hiddenBySubSection': true,
        'isRequiredByAgent': true,
        'isValid': true,
        'isValidOverride': true
      }),
      App.ServiceConfigProperty.create({
        'name': 'p4',
        'isVisible': true,
        'hiddenBySection': false,
        'hiddenBySubSection': false,
        'isRequiredByAgent': true,
        'isValid': false,
        'isValidOverride': true,
        'isNotInitialValue': true
      }),
      App.ServiceConfigProperty.create({
        'name': 'p5',
        'isVisible': true,
        'hiddenBySection': false,
        'hiddenBySubSection': false,
        'isRequiredByAgent': true,
        'isValid': true,
        'isValidOverride': false,
        'isNotInitialValue': true
      }),
      App.ServiceConfigProperty.create({
        'name': 'p6',
        'isVisible': true,
        'hiddenBySection': false,
        'hiddenBySubSection': false,
        'isRequiredByAgent': false,
        'isRequired': false,
        'isValid': true,
        'isValidOverride': false,
        'isNotInitialValue': true
      }),
      App.ServiceConfigProperty.create({
        'name': 'p7',
        'isVisible': true,
        'hiddenBySection': false,
        'hiddenBySubSection': false,
        'isRequiredByAgent': false,
        'isValid': true,
        'isRequired': true,
        'isValidOverride': false
      }),
      App.ServiceConfigProperty.create({
        'name': 'p8',
        'isVisible': false,
        'hiddenBySection': false,
        'hiddenBySubSection': false,
        'isRequiredByAgent': false,
        'isValid': true,
        'isRequired': true,
        'isValidOverride': true,
        'value': 'Undefined',
        'displayType': 'label'
      })
  ];

describe('App.ServiceConfig', function () {

  beforeEach(function () {
    serviceConfig = App.ServiceConfig.create({
      configs: configs
    });
  });

  describe('#activeProperties', function() {
    it('returns collection of properties that should be shown', function() {
      serviceConfig.setActivePropertiesOnce();
      expect(serviceConfig.get('activeProperties').mapProperty('name')).to.be.eql(['p1','p4','p5','p7', 'p8']);
    });
  });

  describe('#configsWithErrors', function() {
    it('returns collection of properties with errors', function() {
      serviceConfig.set('activeProperties', configs);
      serviceConfig.setConfigsWithErrorsOnce();
      expect(serviceConfig.get('configsWithErrors').mapProperty('name')).to.be.eql(['p4', 'p5', 'p6', 'p7']);
    });
  });

  describe('#errorCount', function() {
    it('returns collection of properties with errors', function() {
      serviceConfig.reopen({
        configsWithErrors: [{}, {}]
      });
      expect(serviceConfig.get('errorCount')).to.equal(2);
    });
  });
});
