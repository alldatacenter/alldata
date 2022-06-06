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
require('views/common/configs/config_versions_dropdown_view');

describe('App.ConfigVersionsDropdownView', function () {
  var view;

  beforeEach(function() {
    view = App.ConfigVersionsDropdownView.create({
      parentView: Em.Object.create({
        compare: sinon.spy(),
        switchPrimaryInCompare: sinon.spy()
      })
    });
  });

  describe('#mainClickAction', function() {
    it('compare should be called', function() {
      view.set('isSecondary', true);
      view.mainClickAction({});
      expect(view.get('parentView').compare.calledWith({})).to.be.true;
    });

    it('switchPrimaryInCompare should be called', function() {
      view.set('isSecondary', false);
      view.mainClickAction({});
      expect(view.get('parentView').switchPrimaryInCompare.calledWith({})).to.be.true;
    });
  });

  describe('#filteredServiceVersions', function() {
    beforeEach(function() {
      view.set('serviceVersions', [
        Em.Object.create({
          version: 1,
          notes: 'lower case notes'
        }),
        Em.Object.create({
          version: 2,
          notes: 'UPPER CASE NOTES'
        })
      ]);
    });

    it('should show all versions when filter empty', function() {
      view.set('filterValue', '');
      expect(view.get('filteredServiceVersions').length).to.be.equal(2);
    });

    it('should show version 1 when filter "Version 1"', function() {
      view.set('filterValue', 'Version 1');
      expect(view.get('filteredServiceVersions').mapProperty('version')).to.be.eql([1]);
    });

    it('should show version 2 when filter "upper"', function() {
      view.set('filterValue', 'upper');
      expect(view.get('filteredServiceVersions').mapProperty('version')).to.be.eql([2]);
    });

    it('should show version 1 when filter "LOWER"', function() {
      view.set('filterValue', 'LOWER');
      expect(view.get('filteredServiceVersions').mapProperty('version')).to.be.eql([1]);
    });
  });
});
