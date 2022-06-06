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

App.TestAliases.testAsComputedEmpty = function (context, propertyName, dependentKey) {

  describe('#' + propertyName + ' as Em.computed.empty', function () {

    var values = [
        {
          value: undefined,
          result: true
        },
        {
          value: null,
          result: true
        },
        {
          value: '',
          result: true,
          description: 'empty string'
        },
        {
          value: [],
          result: true,
          description: 'empty array'
        },
        {
          value: false,
          result: false
        },
        {
          value: true,
          result: false
        },
        {
          value: 0,
          result: false
        },
        {
          value: NaN,
          result: false
        },
        {
          value: {},
          result: false,
          description: 'empty object'
        },
        {
          value: '0',
          result: false,
          description: 'non-empty string'
        },
        {
          value: 1,
          result: false,
          description: 'non-zero number'
        },
        {
          value: [0],
          result: false,
          description: 'non-empty array'
        },
        {
          value: {
            i: 0
          },
          result: false,
          description: 'non-empty object'
        },
        {
          value: Em.K,
          result: false,
          description: 'function'
        }
      ],
      descs = Em.meta(context).descs;

    it('has valid dependent keys', function () {
      expect(descs[propertyName]._dependentKeys).to.eql([dependentKey]);
    });

    values.forEach(function (item) {

      var description = item.description || item.value,
        initialDefinition = Boolean(descs[dependentKey]) ? descs[dependentKey] : context.get(dependentKey);

      describe(dependentKey + ' is ' + description, function () {

        beforeEach(function () {
          helpers.reopenProperty(context, dependentKey, item.value);
          context.propertyDidChange(propertyName);
        });

        afterEach(function () {
          helpers.reopenProperty(context, dependentKey, initialDefinition);
        });

        it(propertyName + ' should be `' + item.result + '`', function () {
          expect(context.get(propertyName)).to.equal(item.result);
        });

      });

    });

  });

};