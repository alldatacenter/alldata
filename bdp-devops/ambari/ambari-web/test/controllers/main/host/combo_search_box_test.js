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
var testHelpers = require('test/helpers');

describe('MainHostComboSearchBoxController', function () {
  var controller;
  beforeEach(function() {
    controller = App.MainHostComboSearchBoxController.create({});
  });

  describe('#getPropertySuggestions', function () {
    it('Should send ajax request with host_name param if hostName as facet', function () {
      controller.getPropertySuggestions('hostName', 'test');
      var args = testHelpers.findAjaxRequest('name', 'hosts.with_searchTerm');
      expect(args[0].data).to.be.eql({
        facet: 'host_name',
        searchTerm: 'test',
        page_size: controller.get('page_size')
      });
    });

    it('Should send ajax request with faced param if facet is not hostName', function () {
      controller.getPropertySuggestions('test1', 'test2');
      var args = testHelpers.findAjaxRequest('name', 'hosts.with_searchTerm');
      expect(args[0].data).to.be.eql({
        facet: 'test1',
        searchTerm: 'test2',
        page_size: controller.get('page_size')
      })
    });
  });

  describe('#getPropertySuggestionsSuccess', function () {
    it ('Should call updateSuggestion with properly mapped data', function () {
      sinon.stub(controller, 'updateSuggestion');
      var data = {
        items: [
          {Hosts: {'test': 'test1', 'test1': 'test1'}},
          {Hosts: {'test': 'test2', 'test2': 'test2'}},
          {Hosts: {'test': 'test3', 'test3': 'test3'}}
        ]
      };
      controller.getPropertySuggestionsSuccess(data, null, {facet: 'test'});
      expect(controller.updateSuggestion.calledWith(['test1', 'test2', 'test3'])).to.be.true;
      controller.updateSuggestion.restore();
    });
  });

  describe('#updateSuggestion', function () {
    it('Should set currentSuggestion to current param', function () {
      var suggestion = [];
      controller.updateSuggestion(suggestion);
      expect(App.router.get('mainHostComboSearchBoxController').get('currentSuggestion')).to.be.eql(suggestion);
    });
  });

  describe('#isComponentStateFacet', function () {
    it('Should return state of facet service stack component', function () {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({isLoaded: true}));
      expect(controller.isComponentStateFacet('test')).to.be.true;
      App.StackServiceComponent.find.restore();
    });
  });

  describe('#createComboParamHash', function () {
    it('Should split param value with ":" character and return object with first val as key and second as value', function () {
      var result = controller.createComboParamHash({value: 'test1:test2:test3'});
      expect(result).to.be.eql({test1: 'test2'});
    });

    it('Should split every value if value prop is array', function () {
      var result = controller.createComboParamHash({value: ['test1:test2', 'test3:test4']});
      expect(result).to.be.eql({test1: 'test2', test3: 'test4'});
    });

    it('Should split every value if value prop is array and create array of values if there are same keys', function () {
      var result = controller.createComboParamHash({value: ['test1:test2', 'test3:test4', 'test3:test5']});
      expect(result).to.be.eql({test1: 'test2', test3: ['test4', 'test5']});
    });
  });

  describe('#createComboParamURL', function () {
    var expressions = [
      '{0}:{1}',
      '{0}-{1}',
      '{0}::{1}',
      '{0}--{1}'
    ];
    it('should concat all epressions on to one', function () {
      var result = controller.createComboParamURL({test1: 'STARTED', test2: 'ALL'}, expressions);
      expect(result).to.be.equal('test1-STARTED|test2:ALL')
    });

    it('should concat all epressions on to one and group arrays in quotes', function () {
      var result = controller.createComboParamURL({test1: 'STARTED', test3: ['STARTED', 'INSTALLED']}, expressions);
      expect(result).to.be.equal('test1-STARTED|(test3-STARTED|test3-INSTALLED)')
    });
  });
});