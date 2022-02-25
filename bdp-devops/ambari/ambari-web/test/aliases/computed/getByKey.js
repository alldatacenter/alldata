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

var helpers = App.TestAliases.helpers;

/**
 *
 * @param {Em.Object} context
 * @param {string} propertyName
 * @param {string} objectKey
 * @param {string} propertyKey
 * @param {{defaultValue: *, map: object}} [checks]
 */
App.TestAliases.testAsComputedGetByKey = function (context, propertyName, objectKey, propertyKey, checks) {

  var _checks = checks || {};
  var obj = _checks.map || Em.get(context, objectKey);
  var defaultValueIsSet = _checks.hasOwnProperty('defaultValue');

  describe('#' + propertyName + ' as Em.computed.getByKey', function () {

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql([objectKey, propertyKey]);
    });

    Object.keys(obj).forEach(function (key) {
      var expectedValue = obj[key];
      it('should be `' + JSON.stringify(expectedValue) + '` if ' + JSON.stringify(propertyKey) + ' is ' + JSON.stringify(key), function () {
        helpers.smartStubGet(context, propertyKey, key)
          .propertyDidChange(context, propertyName);
        var value = helpers.smartGet(context, propertyName);
        expect(value).to.be.eql(expectedValue);
      });
    });

    if (defaultValueIsSet) {
      var defaultValue = _checks.defaultValue;
      it('should be `' + JSON.stringify(defaultValue) + '` if ' + JSON.stringify(propertyKey) + ' is not exist in the tested object', function () {
        helpers.smartStubGet(context, propertyKey, '' + Math.random())
          .propertyDidChange(context, propertyName);
        var value = helpers.smartGet(context, propertyName);
        expect(value).to.be.eql(defaultValue);
      });
    }

  });

};