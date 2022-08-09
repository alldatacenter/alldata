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

function prepareHash(dependentKeys, index) {
  var hash = {};
  dependentKeys.forEach(function (key, i) {
    hash[key] = i < index ? null : '' + i;
  });
  return hash;
}

/**
 *
 * @param {Em.Object} context
 * @param {string} propertyName
 * @param {string[]} dependentKeys
 */
App.TestAliases.testAsComputedFirstNotBlank = function (context, propertyName, dependentKeys) {

  describe('#' + propertyName + ' as Em.computed.firstNotBlank', function () {

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql(dependentKeys);
    });

    dependentKeys.forEach(function(dependentKey, index) {

      it('should be equal to the ' + JSON.stringify(dependentKey), function () {
        helpers.smartStubGet(context, prepareHash(dependentKeys, index))
          .propertyDidChange(context, propertyName);
        var value = helpers.smartGet(context, propertyName);
        expect(value).to.equal('' + index);
      });

    });

  });

};