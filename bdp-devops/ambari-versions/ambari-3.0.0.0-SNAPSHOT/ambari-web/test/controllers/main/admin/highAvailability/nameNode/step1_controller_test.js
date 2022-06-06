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

describe('App.HighAvailabilityWizardStep1Controller', function () {
  var controller;

  beforeEach(function() {
    controller = App.HighAvailabilityWizardStep1Controller.create({
      content: Em.Object.create()
    });
  });

  describe('#isNameServiceIdValid', function () {

    var testCases = [
      {
        input: '',
        expected: false
      },
      {
        input: ' ',
        expected: false
      },
      {
        input: ' a',
        expected: false
      },
      {
        input: 'a ',
        expected: false
      },
      {
        input: '%',
        expected: false
      },
      {
        input: 'a',
        expected: true
      },
      {
        input: 'A',
        expected: true
      },
      {
        input: '1',
        expected: true
      },
      {
        input: '123456789012345678901234567890123456789012345678901234567891234',
        expected: true
      },
      {
        input: '1234567890123456789012345678901234567890123456789012345678912345',
        expected: false
      }
    ];

    testCases.forEach(function(test) {
      it('should return ' + test.expected
          + ' when nameServiceId=' + test.input, function() {
        controller.set('content.nameServiceId', test.input);
        expect(controller.get('isNameServiceIdValid')).to.be.equal(test.expected)
      });
    });
  });

  describe('#setHawqInstalled', function() {

    beforeEach(function() {
      this.mock = sinon.stub(App.Service, 'find');
    });

    afterEach(function() {
      this.mock.restore();
    });

    it('should set isHawqInstalled to true', function() {
      this.mock.returns([{serviceName: 'HAWQ'}]);
      controller.setHawqInstalled();
      expect(controller.get('isHawqInstalled')).to.be.true;
    });

    it('should set isHawqInstalled to false', function() {
      this.mock.returns([]);
      controller.setHawqInstalled();
      expect(controller.get('isHawqInstalled')).to.be.false;
    });
  });

  describe('#next', function() {

    beforeEach(function () {
      sinon.stub(App.router, 'send');
    });
    afterEach(function () {
      App.router.send.restore();
    });

    it('App.router.send should be called', function() {
      controller.reopen({
        isNameServiceIdValid: true
      });
      controller.next();
      expect(App.router.send.calledOnce).to.be.true;
    });

    it('App.router.send should not be called', function() {
      controller.reopen({
        isNameServiceIdValid: false
      });
      controller.next();
      expect(App.router.send.called).to.be.false;
    });
  });
});
