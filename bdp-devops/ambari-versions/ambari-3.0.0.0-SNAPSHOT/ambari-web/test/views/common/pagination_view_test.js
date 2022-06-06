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
require('views/common/pagination_view');

describe('App.PaginationSelectMixin', function () {

  var obj;

  beforeEach(function () {
    obj = Em.Object.create(App.PaginationSelectMixin);
  });

  describe('#change', function () {

    var dataView = {
      saveDisplayLength: Em.K
    };

    beforeEach(function () {
      sinon.spy(dataView, 'saveDisplayLength');
      obj.set('parentView', {
        dataView: dataView
      });
      obj.change();
    });

    it('should call saveDisplayLength', function () {
      expect(dataView.saveDisplayLength.calledOnce).to.be.true;
    });

  });

});

describe('App.PaginationView', function () {

  var dataView = {},
    getView = function () {
      return App.PaginationView.create({
        parentView: dataView
      });
    };

  describe('#dataView', function () {

    it('should be bound to parentView', function () {
      var view = getView();
      expect(view.get('dataView')).to.eql(dataView);
    });

  });

  App.TestAliases.testAsComputedOr(getView(), 'isPreviousDisabled', ['dataView.isCurrentPageFirst', '!isDataLoaded']);

  App.TestAliases.testAsComputedOr(getView(), 'isNextDisabled', ['dataView.isCurrentPageLast', '!isDataLoaded']);

});