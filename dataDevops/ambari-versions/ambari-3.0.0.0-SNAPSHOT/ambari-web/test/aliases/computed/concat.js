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

App.TestAliases.testAsComputedConcat = function (context, propertyName, separator, dependentValues) {

  describe('#' + propertyName + ' as Em.computed.concat', function () {

    var cases = [
        {
          firstItem: undefined
        },
        {
          firstItem: null
        },
        {
          firstItem: '',
          description: 'empty string'
        },
        {
          firstItem: 0,
          description: 'number'
        },
        {
          firstItem: [1, 2],
          description: 'array'
        },
        {
          firstItem: {
            property: 'value'
          },
          description: 'object'
        },
        {
          firstItem: NaN
        },
        {
          firstItem: Em.K,
          description: 'function'
        }
      ],
      values = dependentValues.map(function (key, index) {
        return index.toString();
      }),
      valuesHash = {};

    dependentValues.forEach(function (key, index) {
      valuesHash[key] = index;
    });

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql(dependentValues);
    });

    cases.forEach(function (item) {

      var description = item.description || item.firstItem;

      it('values contain ' + description, function () {
        if (item.hasOwnProperty('firstItem')) {
          values[0] = item.firstItem;
          valuesHash[dependentValues[0]] = item.firstItem;
        }
        helpers.smartStubGet(context, valuesHash).propertyDidChange(context, propertyName);
        expect(helpers.smartGet(context, propertyName)).to.equal(values.join(separator));
      });

    });

  });

};