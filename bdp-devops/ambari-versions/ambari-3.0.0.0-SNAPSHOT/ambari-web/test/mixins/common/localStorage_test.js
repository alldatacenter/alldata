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

require('mixins/common/localStorage');

var localStorage,
  nameCases = [
    {
      toSet: {
        name: 'name'
      },
      toExpect: 'Name'
    },
    {
      toSet: {
        name: null,
        controller: {
          name: 'samplecontroller'
        }
      },
      toExpect: 'Samplecontroller'
    },
    {
      toSet: {
        controller: {
          name: 'sampleController'
        }
      },
      toExpect: 'Sample'
    }
  ];

describe('App.LocalStorage', function () {

  beforeEach(function () {
    localStorage = Em.Object.create(App.LocalStorage);
  });

  after(function () {
    App.db.cleanUp();
  });

  describe('#dbNamespace', function () {
    nameCases.forEach(function (item) {
      it('should be ' + item.toExpect, function () {
        localStorage.setProperties(item.toSet);
        expect(localStorage.get('dbNamespace')).to.equal(item.toExpect)
      });
    });
  });

  describe('#getDBProperty', function () {
    it('should take value from DB', function () {
      localStorage.set('name', 'name');
      localStorage.setDBProperty('key', 'value');
      expect(localStorage.getDBProperty('key')).to.equal('value');
    });
  });

});
