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
var view;

describe('App.MainAdminStackMenuView', function () {

  beforeEach(function () {
    view = App.MainAdminStackMenuView.create({});
  });

  describe('#content', function () {

    beforeEach(function () {
      this.stub = sinon.stub(App, 'get');
    });

    afterEach(function () {
      App.get.restore();
    });

    Em.A([
        {
          stackVersionsAvailable: true,
          m: '`versions` is visible',
          e: false
        },
        {
          stackVersionsAvailable: false,
          m: '`versions` is invisible',
          e: true
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          this.stub.withArgs('stackVersionsAvailable').returns(test.stackVersionsAvailable);
          view.propertyDidChange('content');
          expect(view.get('content').findProperty('name', 'versions').get('hidden')).to.equal(test.e);
        });
      });

  });

});