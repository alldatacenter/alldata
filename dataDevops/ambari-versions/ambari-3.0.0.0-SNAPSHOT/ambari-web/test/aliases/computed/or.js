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

var objectUtils = require('utils/object_utils');

var helpers = App.TestAliases.helpers;

function getFalsyCombination(dependentKeys) {
  var hash = {};
  dependentKeys.forEach(function (key) {
    if (key.startsWith('!')) {
      hash[key.substr(1)] = true;
    }
    else {
      hash[key] = false;
    }
  });
  return hash;
}


/**
 *
 * @param {Em.Object} context
 * @param {string} propertyName
 * @param {string[]} dependentKeys
 */
App.TestAliases.testAsComputedOr = function (context, propertyName, dependentKeys) {

  var realKeys = dependentKeys.map(function (key) {
    return key.startsWith('!') ? key.substr(1) : key;
  });
  var falsyCombination = getFalsyCombination(dependentKeys);
  var binaryCombos = helpers.getBinaryCombos(realKeys);

  describe('#' + propertyName + ' as Em.computed.or', function () {

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql(realKeys);
    });

    binaryCombos.forEach(function (combo) {

      var expectedResult = !objectUtils.deepEqual(falsyCombination, combo);

      it('`' + expectedResult + '` for ' + JSON.stringify(combo), function() {
        helpers.smartStubGet(context, combo)
          .propertyDidChange(context, propertyName);
        var value = helpers.smartGet(context, propertyName);
        expect(value).to.equal(expectedResult);
      });

    });

  });

};