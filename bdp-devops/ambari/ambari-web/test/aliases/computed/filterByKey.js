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
 * @param {string} neededValueKey
 */
App.TestAliases.testAsComputedFilterByKey = function (context, propertyName, collectionName, keyName, neededValueKey) {

  describe('#' + propertyName + ' as Em.computed.filterByKey', function () {

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql([collectionName + '.@each.' + keyName, neededValueKey]);
    });

    it('should be `[]` if ' + JSON.stringify(collectionName) + ' is empty', function () {
      helpers.smartStubGet(context, collectionName, [])
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.eql([]);
    });

    it('should be `[]` if ' + JSON.stringify(collectionName) + ' does not exist', function () {
      helpers.smartStubGet(context, collectionName, null)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.eql([]);
    });

    it('should be an array objects from  ' + JSON.stringify(collectionName) + ' with ' + JSON.stringify(keyName) + ' equal to the value in ' + JSON.stringify(neededValueKey), function () {
      var collection = [{}, {}, {}];
      var neededValue = Math.random();
      collection.forEach(function (item) {
        Ember.setFullPath(item, keyName, neededValue);
      });

      Em.set(collection[2], keyName, !neededValue);
      var hash = {};
      hash[collectionName] = collection;
      hash[neededValueKey] = neededValue;
      helpers.smartStubGet(context, hash)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.eql(collection.slice(0, 2));
    });

  });

};