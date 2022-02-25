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
require('models/client_component');

describe('App.ClientComponent', function () {

  var clientComponent;

  beforeEach(function () {
    clientComponent = App.ClientComponent.createRecord();
  });

  describe('#allowToDelete', function () {

    var cases = [
      {
        statesCounts: {
          installedCount: 1,
          installFailedCount: 2,
          initCount: 3,
          unknownCount: 4,
          totalCount: 10
        },
        allowToDelete: true,
        title: 'delete allowed'
      },
      {
        statesCounts: {
          installedCount: 1,
          installFailedCount: 2,
          initCount: 3,
          unknownCount: 4,
          totalCount: 11
        },
        allowToDelete: false,
        title: 'delete not allowed'
      }
    ];

    cases.forEach(function (item) {

      it(item.title, function () {
        clientComponent.setProperties(item.statesCounts);
        expect(clientComponent.get('allowToDelete')).to.equal(item.allowToDelete);
      });

    });

  });

  describe('#summaryLabelClassName', function () {

    it('should use lower case of component name', function () {
      clientComponent.set('componentName', 'ZOOKEEPER_CLIENT');
      expect(clientComponent.get('summaryLabelClassName')).to.equal('label_for_zookeeper_client');
    });

  });

  describe('#summaryValueClassName', function () {

    it('should use lower case of component name', function () {
      clientComponent.set('componentName', 'ZOOKEEPER_CLIENT');
      expect(clientComponent.get('summaryValueClassName')).to.equal('value_for_zookeeper_client');
    });

  });

  describe('#displayNamePluralized', function () {

    var cases = [
      {
        installedCount: 1,
        displayNamePluralized: 'Zookeeper Client',
        title: 'singular'
      },
      {
        installedCount: 2,
        displayNamePluralized: 'Zookeeper Clients',
        title: 'plural'
      }
    ];

    cases.forEach(function (item) {

      it(item.title, function () {
        clientComponent.setProperties({
          displayName: 'Zookeeper Client',
          installedCount: item.installedCount
        });
        expect(clientComponent.get('displayNamePluralized')).to.equal(item.displayNamePluralized);
      });

    });

  });

});