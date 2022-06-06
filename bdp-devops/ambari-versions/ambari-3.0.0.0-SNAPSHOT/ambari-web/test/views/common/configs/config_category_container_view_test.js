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
var lazyLoading = require('utils/lazy_loading');

describe('App.ConfigCategoryContainerView', function () {
  var view;

  beforeEach(function () {
    view = App.ConfigCategoryContainerView.create();
  });

  describe('#willDestroyElement', function () {
    it('should call terminate method, if lazy loading is truthly', function () {
      sinon.stub(lazyLoading, 'terminate');
      view.set('lazyLoading', true);
      view.willDestroyElement();
      expect(lazyLoading.terminate.calledOnce).to.be.true;
      expect(lazyLoading.terminate.calledWith(true)).to.be.true;
      lazyLoading.terminate.restore();
    });

    it('should not call terminate method, if lazy loading is falsy', function () {
      sinon.stub(lazyLoading, 'terminate');
      view.set('lazyLoading', false);
      view.willDestroyElement();
      expect(lazyLoading.terminate.called).to.be.false;
      lazyLoading.terminate.restore();
    });
  });

  describe('#pushViews', function () {
    it('should do not set anything if there are no categories', function () {
      view.set('categories', null);
      sinon.stub(view, 'set');
      view.pushViews();
      expect(view.set.called).to.be.false;
      view.set.restore();
    });
    it('should set lazyLoading to proper object depends on categories view', function () {
      var categories = [{isCustomView: true, customView: App.ConfigDiffView}, {isCustomView: false}];
      view.set('categories', categories);
      sinon.stub(view, 'set');
      view.pushViews();
      expect(view.set.called).to.be.true;
      expect(view.get('lazyLoading')).to.be.truthy;
    });
  });
});