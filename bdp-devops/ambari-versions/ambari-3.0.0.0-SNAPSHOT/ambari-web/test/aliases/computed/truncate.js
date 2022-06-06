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

var strTemplate = '0123456789';

/**
 * 10 -> '0123456789'
 *  5 -> '01234'
 * 15 -> '012345678901234'
 *
 * @param length
 * @returns {string}
 */
function getStr(length) {
  var ret = '';
  var n = Math.floor(length / 10);
  var m = length % 10;
  for (var i = 0; i < n; i++) {
    ret += strTemplate;
  }
  ret += strTemplate.substr(0, m);
  return ret;
}

/**
 *
 * @param {Em.Object} context
 * @param {string} propertyName
 * @param {string} dependentKey
 * @param {number} maxLength
 * @param {number} reduceTo
 * @param {string} [replacer]
 */
App.TestAliases.testAsComputedTruncate = function (context, propertyName, dependentKey, maxLength, reduceTo, replacer) {

  var _replacer = arguments.length > 5 ? replacer : '...';

  describe('#' + propertyName + ' as Em.computed.truncate', function () {

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql([dependentKey]);
    });

    it('should be truncated if `maxLength` > ' + JSON.stringify(dependentKey) + ' length', function () {
      var val = getStr(maxLength + 1);
      var expectedValue = val.substr(0, reduceTo) + _replacer;
      helpers.smartStubGet(context, dependentKey, val)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.be.equal(expectedValue);
    });

    it('should not be truncated if `maxLength` = ' + JSON.stringify(dependentKey) + ' length', function () {
      var val = getStr(maxLength);
      var expectedValue = val;
      helpers.smartStubGet(context, dependentKey, val)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.be.equal(expectedValue);
    });

    it('should not be truncated if `maxLength` < ' + JSON.stringify(dependentKey) + ' length', function () {
      var val = getStr(maxLength - 1);
      var expectedValue = val;
      helpers.smartStubGet(context, dependentKey, val)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.be.equal(expectedValue);
    });

    it('should be "" if ' + JSON.stringify(dependentKey) + ' value is empty', function () {
      var val = null;
      var expectedValue = '';
      helpers.smartStubGet(context, dependentKey, val)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.be.equal(expectedValue);
    });

  });

};