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

describe('App.AlertInstancesPopupView', function () {
  var view;
  var data = [
    Em.Object.create({stateClass: 'test1'}),
    Em.Object.create({stateClass: 'test1'}),
    Em.Object.create({stateClass: 'test2'})];
  beforeEach(function () {
    view = App.AlertInstancesPopupView.create({
      content: data
    });
    sinon.stub(view, '$').returns({click: function () {}});
  });

  afterEach(function () {
    view.$.restore();
  });

  describe('#didInsertElement', function () {
    it('should call filter method', function () {
      sinon.stub(view, 'filter');
      view.didInsertElement();
      expect(view.filter.calledOnce).to.be.true;
      view.filter.restore();
    });

    it('should add observer filteringComplete to overlayObserver', function () {
      sinon.stub(view, 'addObserver');
      view.didInsertElement();
      expect(view.addObserver.calledWith('filteringComplete', view, view.overlayObserver)).to.be.true;
      view.addObserver.restore();
    });

    it('should call overlayObserver method', function () {
      sinon.stub(view, 'overlayObserver');
      view.didInsertElement();
      expect(view.overlayObserver.calledOnce).to.be.true;
      view.overlayObserver.restore();
    });
  });

  describe('#updateAlertInstances', function () {
    it('should do nothing when length is 0', function () {
      var updater = view.get('updater');
      sinon.stub(updater, 'set');
      view.set('displayLength', 0);
      expect(updater.set.notCalled).to.be.true;
      updater.set.restore();
    });

    it('should called updater set method with proper params', function () {
      var updater = view.get('updater');
      sinon.stub(updater, 'set');
      view.set('startIndex', 1);
      view.set('displayLength', 101);
      expect(updater.set.calledWith('queryParamsForUnhealthyAlertInstances', {from: 0, page_size: 101})).to.be.true;
      updater.set.restore();
    });

    it('should set filteringComplete to false', function () {
      view.set('startIndex', 1);
      view.set('displayLength', 101);
      expect(view.get('filteringComplete')).to.be.false;
    });

    it('should vall updater updateUnhealthyAlertInstances method', function () {
      var updater = view.get('updater');
      sinon.stub(updater, 'updateUnhealthyAlertInstances');
      view.set('displayLength', 101);
      expect(updater.updateUnhealthyAlertInstances.calledTwice).to.be.true;
      updater.updateUnhealthyAlertInstances.restore();
    });
  });

  describe('#categories', function () {
    var categoryObject = Em.Object.extend({
      value: '',
      count: 0,
      labelPath: '',
      label: function () {
        return Em.I18n.t(this.get('labelPath')).format(this.get('count'));
      }.property('count', 'labelPath')
    });
    var categories = [
      categoryObject.create({
        value: 'all',
        labelPath: 'alerts.dropdown.dialog.filters.all',
        count: 10
      }),
      categoryObject.create({
        value: 'alert-state-CRITICAL',
        labelPath: 'alerts.dropdown.dialog.filters.critical',
        count: 6
      }),
      categoryObject.create({
        value: 'alert-state-WARNING',
        labelPath: 'alerts.dropdown.dialog.filters.warning',
        count: 4
      })
    ];

    it('should return correct categories', function () {
      view.set('alertsNumber', 10);
      view.set('criticalNumber', 6);
      view.set('warningNumber', 4);
      expect(view.get('categories')[0].get('label')).to.equal(categories[0].get('label'));
      expect(view.get('categories')[1].get('label')).to.equal(categories[1].get('label'));
      expect(view.get('categories')[2].get('label')).to.equal(categories[2].get('label'));
    });
  });

  describe('#filter', function () {
    it('should display all items', function () {
      view.set('selectedCategory', Em.Object.create({value: 'all'}));
      expect(view.get('filteredContent')).to.eql(data);
    });

    it('should display items from selected category', function () {
      view.set('selectedCategory', Em.Object.create({value: 'test1'}));
      expect(view.get('filteredContent').filter(function (item) {
        return item.get('isVisible');
      }).length).to.equal(2);
    });
  });
});