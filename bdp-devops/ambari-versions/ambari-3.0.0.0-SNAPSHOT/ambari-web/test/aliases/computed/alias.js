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
 * @param {string} [type]
 */
App.TestAliases.testAsComputedAlias = function (context, propertyName, dependentKey, type) {
  var testsCases = [];
  var typesMap = {
    string: ['1234', '', 'abc', '{}'],
    number: [1234, 0, -1234, 1.2, -1.2],
    boolean: [true, false],
    object: [{a: 12345}, {}],
    array: [[1,2,3], [], [{}, {a: 1}]]
  };

  if (type) {
    testsCases = typesMap[type] || [];
  }
  else {
   // all
    testsCases = [].concat(Object.keys(typesMap).map(function (key) {return typesMap[key]}));
  }

  describe('#' + propertyName + ' as Em.computed.alias', function () {

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql([dependentKey]);
    });

    testsCases.forEach(function (testedValue) {
      it('should be equal to the ' + JSON.stringify(dependentKey) + ' (' + Em.typeOf(testedValue) + ')', function () {
        helpers.smartStubGet(context, dependentKey, testedValue)
          .propertyDidChange(context, propertyName);
        var value = helpers.smartGet(context, propertyName);
        expect(value).to.eql(testedValue);
      });
    });

  });

};