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
 * @param {*} trueValue
 * @param {*} falseValue
 */
App.TestAliases.testAsComputedIfThenElse = function (context, propertyName, dependentKey, trueValue, falseValue) {

  describe('#' + propertyName + ' as Em.computed.ifThenElse', function () {

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql([dependentKey]);
    });

    it('should be `trueValue` if ' + JSON.stringify(dependentKey) + ' is `true`', function () {
      helpers.smartStubGet(context, dependentKey, true)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.equal(trueValue);
    });

    it('should be `falseValue` if ' + JSON.stringify(dependentKey) + ' is `false`', function () {
      helpers.smartStubGet(context, dependentKey, false)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.equal(falseValue);
    });

  });

};