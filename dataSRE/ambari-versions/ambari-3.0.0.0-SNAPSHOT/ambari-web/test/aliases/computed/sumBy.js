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
 * @param {string} collectionName
 * @param {string} keyName
 */
App.TestAliases.testAsComputedSumBy = function (context, propertyName, collectionName, keyName) {

  describe('#' + propertyName + ' as Em.computed.sumBy', function () {

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql([collectionName + '.@each.' + keyName]);
    });

    it('should be `0` if ' + JSON.stringify(collectionName) + ' is empty', function () {
      helpers.smartStubGet(context, collectionName, [])
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.equal(0);
    });

    it('should be `0` if ' + JSON.stringify(collectionName) + ' does not exist', function () {
      helpers.smartStubGet(context, collectionName, null)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.equal(0);
    });

    it('should be a sum of the values of each ' + JSON.stringify(keyName) + ' in the ' + JSON.stringify(collectionName), function () {
      var collection = [{}, {}, {}];
      collection.forEach(function (item, index) {
        Ember.setFullPath(item, keyName, index);
      });
      helpers.smartStubGet(context, collectionName, collection)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.equal(3); // 0 + 1 + 2
    });

  });

};