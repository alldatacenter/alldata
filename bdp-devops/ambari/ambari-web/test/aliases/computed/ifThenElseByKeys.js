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
 * @param {string} dependentKey
 * @param {string} trueKey
 * @param {string} falseKey
 */
App.TestAliases.testAsComputedIfThenElseByKeys = function (context, propertyName, dependentKey, trueKey, falseKey) {

  describe('#' + propertyName + ' as Em.computed.ifThenElseByKeys', function () {

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql([dependentKey, trueKey, falseKey]);
    });

    it('should be `trueKey`-value if ' + JSON.stringify(dependentKey) + ' is `true`', function () {
      var hash = {};
      var neededValue = 'trulyValue';
      hash[dependentKey] = true;
      hash[trueKey] = neededValue;
      helpers.smartStubGet(context, hash)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.equal(neededValue);
    });

    it('should be `falseKey`-value if ' + JSON.stringify(dependentKey) + ' is `false`', function () {
      var hash = {};
      var neededValue = 'falsyValue';
      hash[dependentKey] = false;
      hash[falseKey] = neededValue;
      helpers.smartStubGet(context, hash)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.equal(neededValue);
    });

  });

};