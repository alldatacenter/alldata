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
require('views/main/dashboard');

describe('App.MainDashboardView', function () {
  var view;

  beforeEach(function() {
    view = App.MainDashboardView.create()
  });

  describe("#NavItemView", function () {
    var navItemView;

    beforeEach(function() {
      navItemView = view.get('NavItemView').create();
    });

    describe("#elementId", function () {

      it("label is null", function() {
        navItemView.set('templateData', {
          keywords: {
            category: {
              label: null
            }
          }
        });
        navItemView.propertyDidChange('elementId');
        expect(navItemView.get('elementId')).to.be.empty;
      });

      it("label has value", function() {
        navItemView.set('templateData', {
          keywords: {
            category: {
              label: 'l1'
            }
          }
        });
        navItemView.propertyDidChange('elementId');
        expect(navItemView.get('elementId')).to.be.equal('dashboard-view-tab-l1');
      });
    });
  });
});
