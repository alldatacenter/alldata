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
require('models/master_component');

describe('App.MasterComponent', function () {

  var masterComponent;

  beforeEach(function () {
    masterComponent = App.MasterComponent.createRecord();
  });

  describe('#displayNamePluralized', function () {

    var cases = [
      {
        startedCount: 1,
        displayNamePluralized: 'Zookeeper Server',
        title: 'singular'
      },
      {
        startedCount: 2,
        displayNamePluralized: 'Zookeeper Servers',
        title: 'plural'
      }
    ];

    cases.forEach(function (item) {

      it(item.title, function () {
        masterComponent.setProperties({
          displayName: 'Zookeeper Server',
          startedCount: item.startedCount
        });
        expect(masterComponent.get('displayNamePluralized')).to.equal(item.displayNamePluralized);
      });

    });

  });

});