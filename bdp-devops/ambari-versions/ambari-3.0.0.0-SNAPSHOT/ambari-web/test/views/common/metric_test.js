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

describe('App.MetricFilteringWidget', function () {
  var view;
  beforeEach(function () {
    view = App.MetricFilteringWidget.create({
      controller: Em.Object.create({})
    });
  });

  describe('#chosenMetrics', function () {
    it('should return array with chosenMetric if it is provided', function () {
      var chosenMetric = Em.Object.create({ label:Em.I18n.t('metric.default'), value:null});
      view.set('chosenMetric', chosenMetric);
      expect(view.get('chosenMetrics')).to.be.eql([chosenMetric]);
    });

    it('should return array with metrics if bo chosen metric', function () {
      expect(view.get('chosenMetrics')).to.be.eql(view.get('defaultMetrics'));
    });
  });

  describe('#defaultMetrics', function () {
    it('should return mapped not null metrics values', function () {
      expect(view.get('defaultMetrics')).to.be.eql(["cpu", "memory", "network", "io"]);
    });
  });

  describe('#bindToController', function () {
    it('should set metricWidget property of controller to current context', function () {
      view.bindToController();
      expect(view.get('controller').get('metricWidget')).to.be.eql(view);
    });
  });

  describe('#toggleMore', function () {
    it('should toggle showMore property', function () {
      view.set('showMore', 4);
      view.toggleMore();
      expect(view.get('showMore')).to.be.equal(-3);
    });
  });

  describe('#init', function () {
    it('should call bindToController method', function () {
      sinon.stub(view, 'bindToController');
      view.init();
      expect(view.bindToController.calledOnce).to.be.true;
      view.bindToController.restore();
    });
  });

  describe('#activate', function () {
    it('should set chosenMetric', function () {
      view.activate({context: 5});
      expect(view.get('chosenMetric')).to.be.equal(5);
    });
  });
});