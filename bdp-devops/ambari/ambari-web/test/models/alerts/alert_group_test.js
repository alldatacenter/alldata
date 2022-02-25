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

function getModel() {
  return App.AlertGroup.createRecord();
}

var model;

describe('App.AlertGroup', function() {

  beforeEach(function () {
    model = getModel();
  });

  App.TestAliases.testAsComputedAlias(getModel(), 'isAddDefinitionsDisabled', 'default', 'boolean');

  describe('#displayName', function () {

    [
      {name: 'abc', default: true, e: 'Abc Default'},
      {name: 'abc', default: false, e: 'Abc'},
      {name: 'ABC', default: false, e: 'Abc'},
      {name: '12345678901234567890', default: true, e: '123456789...234567890 Default'},
      {name: '12345678901234567890', default: false, e: '123456789...234567890'},
    ].forEach(function (test) {
      it(test.name + ' ' + test.default, function () {
        model.setProperties({
          name: test.name,
          default: test.default
        });
        expect(model.get('displayName')).to.be.equal(test.e);
      });
    });

  });

});