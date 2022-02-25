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
App.TestAliases.testAsComputedEveryByKey = function (context, propertyName, collectionName, keyName, neededValueKey) {

  describe('#' + propertyName + ' as Em.computed.everyByKey', function () {

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql([collectionName + '.@each.' + keyName, neededValueKey]);
    });

    it('should be `true` if ' + JSON.stringify(collectionName) + ' is empty', function () {
      helpers.smartStubGet(context, collectionName, [])
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.be.true;
    });

    it('should be `false` if ' + JSON.stringify(collectionName) + ' does not exist', function () {
      helpers.smartStubGet(context, collectionName, null)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.be.false;
    });

    it('should be `false` if no one object in the ' + JSON.stringify(collectionName) + ' does not have ' + JSON.stringify(keyName) + ' with value equal to the value in ' + JSON.stringify(neededValueKey), function () {
      var collection = [{}, {}, {}];
      var neededValue = Math.random();
      collection.setEach(keyName, !neededValue); // something that not equal to the `neededValue`
      helpers.smartStubGet(context, {collectionName: collection, neededValueKey: neededValue})
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.be.false;
    });

    it('should be `false` if at least one object in the ' + JSON.stringify(collectionName) + ' does not have ' + JSON.stringify(keyName) + ' with value equal to the value in ' + JSON.stringify(neededValueKey), function () {
      var collection = [{}, {}, {}];
      var neededValue = Math.random();
      collection.setEach(keyName, neededValue);
      collection[1][keyName] = !neededValue;
      helpers.smartStubGet(context, {collectionName: collection, neededValueKey: neededValue})
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.be.false;
    });

    it('should be `true` if all objects in the ' + JSON.stringify(collectionName) + ' have ' + JSON.stringify(keyName) + ' with value equal to the value in ' + JSON.stringify(neededValueKey), function () {
      var collection = [{}, {}, {}];
      var neededValue = Math.random();
      collection.setEach(keyName, neededValue);
      var hash = {};
      hash[collectionName] = collection;
      hash[neededValueKey] = neededValue;
      helpers.smartStubGet(context, hash)
        .propertyDidChange(context, propertyName);
      var value = helpers.smartGet(context, propertyName);
      expect(value).to.be.true;
    });

  });

};