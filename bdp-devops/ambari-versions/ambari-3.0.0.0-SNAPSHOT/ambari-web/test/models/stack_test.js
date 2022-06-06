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
require('models/stack');

describe('App.Stack', function () {

  var stack;

  beforeEach(function () {
    stack = App.Stack.createRecord();
  });

  describe('#stackNameVersion', function () {

    it('should concat stack name and stack version', function () {
      stack.setProperties({
        stackName: 'HDP',
        stackVersion: '2.5'
      });
      expect(stack.get('stackNameVersion')).to.equal('HDP-2.5');
    });

  });

  describe('#isPatch', function () {

    var cases = [
      {
        type: 'PATCH',
        isPatch: true
      },
      {
        type: 'STANDARD',
        isPatch: false
      }
    ];

    cases.forEach(function (item) {

      it(item.type, function () {
        stack.set('type', item.type);
        expect(stack.get('isPatch')).to.equal(item.isPatch);
      });

    });

  });

  describe('#displayName', function () {

    it('should concat stack name and stack version', function () {
      stack.setProperties({
        stackName: 'HDP',
        repositoryVersion: '2.5.0.0-999'
      });
      expect(stack.get('displayName')).to.equal('HDP-2.5.0.0-999');
    });

  });

  describe('#repositories', function () {

    beforeEach(function () {
      stack.reopen({
        operatingSystems: [
          Em.Object.create({
            isSelected: false,
            repositories: [
              {
                id: 0
              },
              {
                id: 1
              }
            ]
          }),
          Em.Object.create({
            isSelected: false,
            repositories: [
              {
                id: 2
              },
              {
                id: 3
              }
            ]
          }),
          Em.Object.create({
            isSelected: false,
            repositories: [
              {
                id: 4
              },
              {
                id: 5
              }
            ]
          })
        ]
      });
    });

    it('no OSes selected', function () {
      expect(stack.get('repositories')).to.be.empty;
    });

    it('some OSes selected', function () {
      stack.get('operatingSystems')[0].isSelected = true;
      stack.get('operatingSystems')[2].isSelected = true;
      expect(stack.get('repositories').toArray()).to.eql([
        {
          id: 0
        },
        {
          id: 1
        },
        {
          id: 4
        },
        {
          id: 5
        }
      ]);
    });

  });

  describe('#cleanReposBaseUrls', function () {

    beforeEach(function () {
      stack.reopen({
        operatingSystems: [
          Em.Object.create({
            isSelected: true,
            repositories: [
              Em.Object.create({
                baseUrl: 'http://localhost/repo0'
              }),
              Em.Object.create({
                baseUrl: 'http://localhost/repo1'
              })
            ]
          }),
          Em.Object.create({
            isSelected: true,
            repositories: [
              Em.Object.create({
                baseUrl: 'http://localhost/repo2'
              }),
              Em.Object.create({
                baseUrl: 'http://localhost/repo3'
              })
            ]
          })
        ]
      });
      stack.cleanReposBaseUrls();
    });

    it('should clear repo urls', function () {
      expect(stack.get('repositories').mapProperty('baseUrl')).to.eql(['', '', '', '']);
    });

  });

  describe('#restoreReposBaseUrls', function () {

    beforeEach(function () {
      stack.reopen({
        operatingSystems: [
          Em.Object.create({
            isSelected: true,
            repositories: [
              Em.Object.create({
                baseUrlInit: 'http://localhost/repo0'
              }),
              Em.Object.create({
                baseUrlInit: 'http://localhost/repo1'
              })
            ]
          }),
          Em.Object.create({
            isSelected: true,
            repositories: [
              Em.Object.create({
                baseUrlInit: 'http://localhost/repo2'
              }),
              Em.Object.create({
                baseUrlInit: 'http://localhost/repo3'
              })
            ]
          })
        ]
      });
      stack.restoreReposBaseUrls();
    });

    it('should reset repo urls', function () {
      expect(stack.get('repositories').mapProperty('baseUrl')).to.eql([
        'http://localhost/repo0',
        'http://localhost/repo1',
        'http://localhost/repo2',
        'http://localhost/repo3'
      ]);
    });

  });

});