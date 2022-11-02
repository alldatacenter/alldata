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
require('views/main/dashboard/widget');

var dashboard = App.MainDashboardWidgetsView.create();
describe('App.DashboardWidgetView', function () {

  var view;

  beforeEach(function() {
    view = App.DashboardWidgetView.create({
      widget: Em.Object.create(),
      parentView: Em.Object.create({
        userPreferences: {},
        setDBProperty: Em.K,
        postUserPref: Em.K,
        hideWidget: dashboard.hideWidget,
        saveWidgetsSettings: dashboard.saveWidgetsSettings,
        renderWidgets: Em.K
      })
    });
  });

  describe('#model', function() {

    beforeEach(function() {
      sinon.stub(view, 'findModelBySource').returns(Em.Object.create({
        serviceName: 'S1'
      }));
    });

    afterEach(function() {
      view.findModelBySource.restore();
    });

    it('sourceName is null', function() {
      view.set('widget.sourceName', null);
      view.propertyDidChange('model');
      expect(view.get('model')).to.be.empty;
    });

    it('sourceName is S1', function() {
      view.set('widget.sourceName', 'S1');
      view.propertyDidChange('model');
      expect(view.get('model')).to.not.be.empty;
    });
  });

  describe('#thresholdMin', function() {

    it('threshold is empty', function() {
      view.set('widget.threshold', null);
      expect(view.get('thresholdMin')).to.be.equal(0);
    });

    it('threshold is set', function() {
      view.set('widget.threshold', [1, 2]);
      expect(view.get('thresholdMin')).to.be.equal(1);
    });
  });

  describe('#thresholdMax', function() {

    it('threshold is empty', function() {
      view.set('widget.threshold', null);
      expect(view.get('thresholdMax')).to.be.equal(0);
    });

    it('threshold is set', function() {
      view.set('widget.threshold', [1, 2]);
      expect(view.get('thresholdMax')).to.be.equal(2);
    });
  });

  describe('#didInsertElement()', function() {

    beforeEach(function() {
      sinon.stub(App, 'tooltip');
    });

    afterEach(function() {
      App.tooltip.restore();
    });

    it('App.tooltip should be called', function() {
      view.didInsertElement();
      expect(App.tooltip).to.be.calledOnce;
    });
  });

  describe('#findModelBySource()', function() {

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns(Em.Object.create({serviceName: 'S1'}));
      sinon.stub(App.HDFSService, 'find').returns(Em.Object.create({serviceName: 'HDFS'}));
      this.mockGet = sinon.stub(App, 'get');
    });

    afterEach(function() {
      App.Service.find.restore();
      App.HDFSService.find.restore();
      this.mockGet.restore();
    });

    it('source = HOST_METRICS', function() {
      this.mockGet.returns([{}]);
      expect(view.findModelBySource('HOST_METRICS')).to.be.eql([{}]);
    });

    it('source = S1', function() {
      expect(view.findModelBySource('S1')).to.be.eql(Em.Object.create({serviceName: 'S1'}));
    });

    it('source = HDFS', function() {
      expect(view.findModelBySource('HDFS')).to.be.eql(Em.Object.create({serviceName: 'HDFS'}));
    });
  });

  describe('#deleteWidget()', function() {

    beforeEach(function() {
      view.get('parentView').setProperties({
        allWidgets: [
          Em.Object.create({
            id: 1,
            isVisible: true
          })
        ],
        userPreferences: {
          visible: [1],
          hidden: [],
          threshold: [],
          groups: {}
        },
        widgetGroups: []
      });
      view.set('widget.id', 1);
      view.deleteWidget();
    });

    it('userPreferences are set correctly', function() {
      expect(view.get('parentView.userPreferences')).to.be.eql({
        visible: [],
        hidden: [1],
        threshold: [],
        groups: {}
      });
    });

  });

  describe('#editWidget()', function() {
    it('App.ModalPopup.show should be called', function() {
      view.editWidget();
      expect(App.ModalPopup.show).to.be.calledOnce;
    });
  });

});
