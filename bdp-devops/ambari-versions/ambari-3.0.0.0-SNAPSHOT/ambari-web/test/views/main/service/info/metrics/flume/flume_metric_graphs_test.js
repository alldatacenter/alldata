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
require('views/main/service/info/metrics/flume/flume_metric_graphs');
var testHelpers = require('test/helpers');

describe('App.MainServiceInfoFlumeGraphsView', function () {

  var view;

  beforeEach(function () {
    view = App.MainServiceInfoFlumeGraphsView.create();
  });
  
  describe('#loadMetrics', function() {
    
    it('App.ajax.send should be called', function() {
      view.set('viewData', {
        metricType: 'type1',
        agent: Em.Object.create({
          hostName: 'host1'
        })
      });
      view.loadMetrics();
      var ajax = testHelpers.findAjaxRequest('name', 'host.host_component.flume.metrics')[0];
      expect(ajax.data).to.be.eql({
        hostName: 'host1',
        flumeComponent: 'type1'
      });
    });
  });
  
  describe('#didInsertElement', function() {
    beforeEach(function() {
      sinon.stub(view, 'loadMetrics');
    });
    afterEach(function() {
      view.loadMetrics.restore();
    });
    
    it('loadMetrics should be called', function() {
      view.didInsertElement();
      expect(view.loadMetrics.calledOnce).to.be.true;
    });
  });
  
  describe('#onLoadMetricsSuccess', function() {
    beforeEach(function() {
      sinon.stub(App.ChartServiceFlumeMetricGraph, 'extend', function(object) {
        return Em.Object.create(object);
      });
    });
    afterEach(function() {
      App.ChartServiceFlumeMetricGraph.extend.restore();
    });
    
    it('should add metrics to serviceMetricGraphs', function() {
      view.set('viewData', {
        metricType: 'type1',
        agent: Em.Object.create({
          hostName: 'host1'
        })
      });
      var data = {
        metrics: {
          flume: {
            flume: {
              type1: {
                name1: {
                  metric1: {}
                }
              }
            }
          }
        }
      };
      view.onLoadMetricsSuccess(data);
      expect(view.get('serviceMetricGraphs')[0][0]).to.be.eql(Em.Object.create({
        metricType: 'type1',
        metricName: 'metric1',
        hostName: 'host1',
        metricItems: ['name1']
      }));
    });
  });

});
