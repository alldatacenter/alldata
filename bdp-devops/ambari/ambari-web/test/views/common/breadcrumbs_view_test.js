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

describe('App.BreadcrumbItem', function () {

  describe('#createLabel', function () {

    describe('#labelBindingPath', function () {

      beforeEach(function () {
        this.breadcrumb = App.BreadcrumbItem.create({labelBindingPath: 'App.router.somePath'});
      });

      it('Observer is added', function () {
        expect(Em.meta(this.breadcrumb).listeners).to.have.property('App.router.somePath:change');
      });

    });

  });

  describe('#transition', function() {

    beforeEach(function() {
      sinon.stub(App.router, "route");

      this.breadcrumb = App.BreadcrumbItem.create({
        label: "label",
        route: "route"
      });
    })

    afterEach(function() {
      App.router.route.restore();
    })

    it('App.router.route should be called', function() {
      this.breadcrumb.transition();
      expect(App.router.route.calledWith('main/' + this.breadcrumb.get("route"))).to.be.true;
    })

    it('action should be called when defined', function() {
      this.breadcrumb.action = sinon.stub();
      this.breadcrumb.transition();
      expect(this.breadcrumb.action.called).to.be.true;
      expect(App.router.route.called).to.be.false;
    })
  })

});

function getCurrentState(parentStateProps, currentStateProps) {
  var parentState = Em.Route.create(parentStateProps);
  var currentState = Em.Route.create(currentStateProps);
  currentState.set('parentState', parentState);
  currentState.set('parentState.parentState', null);
  return currentState;
}

var view;
describe('App.BreadcrumbsView', function () {

  beforeEach(function () {
    view = App.BreadcrumbsView.create();
  });

  describe('#items', function () {
    var currentState;

    beforeEach(function () {
      sinon.stub(App, 'get', function (key) {
        if (key === 'router.currentState') {
          return currentState;
        }
        return Em.get(App, key);
      });
    });

    afterEach(function () {
      App.get.restore();
    });

    it('predefined label', function () {
      currentState = getCurrentState({}, {name: '', breadcrumbs: {label: 'abc'}});
      expect(view.get('items.firstObject.formattedLabel')).to.be.equal('abc');
    });

    it('`name` as label', function () {
      currentState = getCurrentState({}, {name: 'abc abc', breadcrumbs: {}});
      expect(view.get('items.firstObject.formattedLabel')).to.be.equal('Abc abc');
    });

    it('label binding', function () {
      App.set('somePath', 'abc');
      currentState = getCurrentState({}, {name: '', breadcrumbs: {labelBindingPath: 'App.somePath'}});
      expect(view.get('items.firstObject.formattedLabel')).to.be.equal('abc');
      App.set('somePath', 'cba');
      expect(view.get('items.firstObject.formattedLabel')).to.be.equal('cba');
    });

    it('`index` route is ignored', function () {
      currentState = getCurrentState({}, {name: 'index'});
      expect(view.get('items')).to.be.empty;
    });

    it('`root` route is ignored', function () {
      currentState = getCurrentState({}, {name: 'root'});
      expect(view.get('items')).to.be.empty;
    });

    it('`step1` route is ignored', function () {
      currentState = getCurrentState({}, {name: 'step1'});
      expect(view.get('items')).to.be.empty;
    });

    it('last item is disabled by default', function () {
      currentState = getCurrentState({breadcrumbs: {label: 'parent'}}, {breadcrumbs: {label: 'child'}});
      expect(view.get('items.length')).to.be.equal(2);
      expect(view.get('items.lastObject.disabled')).to.be.true;
    });

    it('last item `isLast` is true', function () {
      currentState = getCurrentState({breadcrumbs: {label: 'parent'}}, {breadcrumbs: {label: 'child'}});
      expect(view.get('items.length')).to.be.equal(2);
      expect(view.get('items.lastObject.isLast')).to.be.true;
    });

  });

});
