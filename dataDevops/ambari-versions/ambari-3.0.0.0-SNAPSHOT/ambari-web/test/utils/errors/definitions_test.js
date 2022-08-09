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
require('utils/errors/definitions');

describe('Error definitions', function () {
  describe('#App.NotNullTypeError', function () {
    var cases = [
      {
        input: null,
        result: 'Not null expected. ',
        title: 'default message'
      },
      {
        input: 'Error',
        result: 'Not null expected. Error',
        title: 'custom message'
      }
    ];
    cases.forEach(function (test) {
      it(test.title, function () {
        var errorObject = new App.NotNullTypeError(test.input);
        expect(errorObject.message).to.equal(test.result);
      });
    });
  });

  describe('#App.ObjectTypeError', function () {
    var cases = [
      {
        input: null,
        result: 'Object expected. ',
        title: 'default message'
      },
      {
        input: 'Error',
        result: 'Object expected. Error',
        title: 'custom message'
      }
    ];
    cases.forEach(function (test) {
      it(test.title, function () {
        var errorObject = new App.ObjectTypeError(test.input);
        expect(errorObject.message).to.equal(test.result);
      });
    });
  });

  describe('#App.ArrayTypeError', function () {
    var cases = [
      {
        input: null,
        result: 'Array expected. ',
        title: 'default message'
      },
      {
        input: 'Error',
        result: 'Array expected. Error',
        title: 'custom message'
      }
    ];
    cases.forEach(function (test) {
      it(test.title, function () {
        var errorObject = new App.ArrayTypeError(test.input);
        expect(errorObject.message).to.equal(test.result);
      });
    });
  });

  describe('#App.FunctionTypeError', function () {
    var cases = [
      {
        input: null,
        result: 'Function expected. ',
        title: 'default message'
      },
      {
        input: 'Error',
        result: 'Function expected. Error',
        title: 'custom message'
      }
    ];
    cases.forEach(function (test) {
      it(test.title, function () {
        var errorObject = new App.FunctionTypeError(test.input);
        expect(errorObject.message).to.equal(test.result);
      });
    });
  });

  describe('#App.EmberObjectTypeError', function () {
    var cases = [
      {
        input: null,
        result: 'Ember object expected. ',
        title: 'default message'
      },
      {
        input: 'Error',
        result: 'Ember object expected. Error',
        title: 'custom message'
      }
    ];
    cases.forEach(function (test) {
      it(test.title, function () {
        var errorObject = new App.EmberObjectTypeError(test.input);
        expect(errorObject.message).to.equal(test.result);
      });
    });
  });
});
