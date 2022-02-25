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


var App = require('app');
require('controllers/main/admin/serviceAccounts_controller');


describe('App.MainAdminServiceAccountsController', function () {

  var controller = App.MainAdminServiceAccountsController.create();

  describe('#sortByOrder()', function () {
    var testCases = [
      {
        title: 'sortOrder is null',
        content: {
          sortOrder: null,
          arrayToSort: [
            {
              name: 'one',
              displayName: 'one'
            }
          ]
        },
        result: ['one']
      },
      {
        title: 'sortOrder is empty',
        content: {
          sortOrder: [],
          arrayToSort: [
            {
              name: 'one',
              displayName: 'one'
            }
          ]
        },
        result: ['one']
      },
      {
        title: 'sortOrder items don\'t match items of array',
        content: {
          sortOrder: ['one'],
          arrayToSort: [
            {name: 'two'}
          ]
        },
        result: []
      },
      {
        title: 'sort items in reverse order',
        content: {
          sortOrder: ['two', 'one'],
          arrayToSort: [
            Em.Object.create({
              name: 'one',
              displayName: 'one'
            }),
            Em.Object.create({
              name: 'two',
              displayName: 'two'
            })
          ]
        },
        result: ['two', 'one']
      },
      {
        title: 'sort items in correct order',
        content: {
          sortOrder: ['one', 'two'],
          arrayToSort: [
            Em.Object.create({
              name: 'one',
              displayName: 'one'
            }),
            Em.Object.create({
              name: 'two',
              displayName: 'two'
            })
          ]
        },
        result: ['one', 'two']
      }
    ];
    testCases.forEach(function (test) {
      it(test.title, function () {
        expect(controller.sortByOrder(test.content.sortOrder, test.content.arrayToSort).mapProperty('displayName')).to.eql(test.result);
      });
    });
  });
});
