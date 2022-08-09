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
 * @param {string} dependentKey1
 * @param {string} dependentKey2
 */
App.TestAliases.testAsComputedGtProperties = function (context, propertyName, dependentKey1, dependentKey2) {

  describe('#' + propertyName + ' as Em.computed.gtProperties', function () {

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql([dependentKey1, dependentKey2]);
    });

    it('should be `true` if ' + JSON.stringify(dependentKey1) + ' is greater than ' + JSON.stringify(dependentKey2), function () {
      var hash = {};
      hash[dependentKey1] = 6;
      hash[dependentKey2] = 5;
      helpers.smartStubGet(context, hash)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.be.true;
    });

    it('should be `false` if ' + JSON.stringify(dependentKey1) + ' is equal to ' + JSON.stringify(dependentKey2), function () {
      var hash = {};
      hash[dependentKey1] = 5;
      hash[dependentKey2] = 5;
      helpers.smartStubGet(context, hash)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.be.false;
    });

    it('should be `true` if ' + JSON.stringify(dependentKey1) + ' is lower than ' + JSON.stringify(dependentKey2), function () {
      var hash = {};
      hash[dependentKey1] = 4;
      hash[dependentKey2] = 5;
      helpers.smartStubGet(context, hash)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.be.false;
    });

  });

};
