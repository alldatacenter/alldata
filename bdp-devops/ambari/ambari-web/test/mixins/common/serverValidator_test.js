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

describe('App.ServerValidatorMixin', function () {
  var mixinObject = Em.Object.extend(App.ServerValidatorMixin, {});
  var instanceObject;
  beforeEach(function () {
    instanceObject = mixinObject.create();
  });
  describe('#collectAllIssues', function () {
    var result = [];
    var stepConfigs = [
      Em.Object.create({
        serviceName: 'service1',
        configs: [
          Em.Object.create({
            id: 'c1_f1',
            name: 'c1',
            filename: 'f1',
            isVisible: true,
            hiddenBySection: false
          }),
          Em.Object.create({
            id: 'c2_f2',
            name: 'c2',
            filename: 'f2',
            isVisible: true,
            hiddenBySection: false
          }),
          Em.Object.create({
            id: 'c3_f3',
            name: 'c3',
            filename: 'f3',
            isVisible: true,
            hiddenBySection: false,
            warnMessage: 'warn3'
          }),
          Em.Object.create({
            id: 'c4_f4',
            name: 'c4',
            filename: 'f4',
            isVisible: false,
            hiddenBySection: false
          })
        ]
      })
    ];

    var response = {
      configErrorsMap: {
        'c1_f1': {
          type: 'WARN',
          messages: ['warn1']
        },
        'c2_f2': {
          type: 'ERROR',
          messages: ['error2']
        },
        'c4_f4': {
          type: 'ERROR',
          messages: ['error4']
        },
        'c5_f5': {
          type: 'ERROR',
          messages: ['error5']
        }
      },
      generalErrors: [{
        type: 'GENERAL',
        messages: ['general issue']
      }]
    };

    beforeEach(function () {
      instanceObject.set('stepConfigs', stepConfigs);
      result = instanceObject.collectAllIssues(response.configErrorsMap, response.generalErrors);
    });

    it('should add server warnings', function () {
      var error = result.issues.find(function(r) { return r.propertyName === 'c1' && r.filename === 'f1'; });
      expect(error.type).to.equal('WARN');
      expect(error.messages).to.eql(['warn1']);
    });

    it('should add server errors', function () {
      var error = result.issues.find(function(r) { return r.propertyName === 'c2' && r.filename === 'f2'; });
      expect(error.type).to.equal('ERROR');
      expect(error.messages).to.eql(['error2']);
    });

    it('should add ui warning', function () {
      var error = result.issues.find(function(r) { return r.propertyName === 'c3' && r.filename === 'f3'; });
      expect(error.type).to.equal('WARN');
      expect(error.messages).to.eql(['warn3']);
    });

    it('should add general issues', function () {
      var error = result.issues.findProperty('type', 'GENERAL');
      expect(error.messages).to.eql(['general issue']);
    });

    it('should ignore issues for hidden configs', function () {
      var error = result.issues.find(function(r) { return r.propertyName === 'c4' && r.filename === 'f4'; });
      expect(error).to.be.undefined;
    });

    it('should add issues for deleted properties', function () {
      var error = result.issues.find(function(r) { return r.id === 'c5_f5'; });
      expect(error.messages).to.eql(['error5']);
    });
  });

  describe('#createErrorMessage', function() {
    var property = {
      id: 'p1_f1',
      name: 'p1',
      filename: 'f1',
      value: 'v1',
      description: 'd1'
    };
    beforeEach(function() {
      sinon.stub(App.StackService, 'find', function() {
        return Em.Object.create({
          displayName: 'sName'
        });
      });
    });

    afterEach(function() {
      App.StackService.find.restore();
    });

    it('creates warn object', function() {
      expect(instanceObject.createErrorMessage('WARN', property, ['msg1'])).to.eql({
        type: 'WARN',
        isCriticalError: false,
        isError: false,
        isWarn: true,
        isGeneral: false,
        messages: ['msg1'],
        propertyName: 'p1',
        filename: 'f1',
        value: 'v1',
        cssClass: 'warning',
        description: 'd1',
        serviceName: 'sName',
        id: 'p1_f1'
      });
    });

    it('creates error object', function() {
      expect(instanceObject.createErrorMessage('ERROR', $.extend({}, property, {serviceDisplayName: 'S Name'}), ['msg2'])).to.eql({
        type: 'ERROR',
        isCriticalError: false,
        isError: true,
        isWarn: false,
        isGeneral: false,
        messages: ['msg2'],
        propertyName: 'p1',
        filename: 'f1',
        value: 'v1',
        cssClass: 'error',
        description: 'd1',
        serviceName: 'S Name',
        id: 'p1_f1'
      });
    });

    it('creates general issue object', function() {
      expect(instanceObject.createErrorMessage('GENERAL', null, ['msg3'])).to.eql({
        type: 'GENERAL',
        cssClass: 'warning',
        isCriticalError: false,
        isError: false,
        isWarn: false,
        isGeneral: true,
        messages: ['msg3']
      });
    });

    it('creates WRONG TYPE issue object', function() {
      expect(instanceObject.createErrorMessage.bind(instanceObject, 'WRONG TYPE', null, ['msg3']))
        .to.throw(Error, 'Unknown config error type WRONG TYPE');
    });

    it('password config property', function () {
      expect(instanceObject.createErrorMessage('ERROR', $.extend({}, property, {
        displayType: 'password'
      })).value).to.equal('**');
    });
  });
});

