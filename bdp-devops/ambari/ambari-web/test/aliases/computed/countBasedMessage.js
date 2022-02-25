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
 * @param {string} zeroMessage
 * @param {string} oneMessage
 * @param {string} manyMessage
 */
App.TestAliases.testAsComputedCountBasedMessage = function (context, propertyName, dependentKey, zeroMessage, oneMessage, manyMessage) {

  describe('#' + propertyName + ' as Em.computed.countBasedMessage', function () {

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql([dependentKey]);
    });

    it('should be equal to `zeroMessage` if ' + JSON.stringify(dependentKey) + ' is 0', function () {
      helpers.smartStubGet(context, dependentKey, 0)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.equal(zeroMessage);
    });

    it('should be equal to `oneMessage` if ' + JSON.stringify(dependentKey) + ' is 1', function () {
      helpers.smartStubGet(context, dependentKey, 1)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.equal(oneMessage);
    });

    it('should be equal to `manyMessage` if ' + JSON.stringify(dependentKey) + ' is 1+', function () {
      helpers.smartStubGet(context, dependentKey, 2)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.equal(manyMessage);
    });

  });

};