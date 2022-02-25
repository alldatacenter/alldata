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
require('views/main/service/info/metrics/flume/flume_metric_graph');

describe('App.ChartServiceFlumeMetricGraph', function () {

  var view;

  beforeEach(function () {
    view = App.ChartServiceFlumeMetricGraph.create();
  });
  
  describe('#getDataForAjaxRequest', function() {
   
    it('should return url', function() {
      view.setProperties({
        metricItems: ['metric1'],
        metricType: 'type1',
        metricName: 'name1',
        customStartTime: 1000,
        customEndTime: 10000,
        currentTimeIndex: 8,
        hostName: 'host1'
      });
      
      expect(view.getDataForAjaxRequest().url).to.be.equal(
        '/api/v1/clusters/c1/hosts/host1/host_components/FLUME_HANDLER?fields=metrics/flume/flume/type1/metric1/name1[1,10,15]'
      );
    });
  });
  
  describe('#getData', function() {
    
    it('should extract data from json', function() {
      view.setProperties({
        metricType: 'type1',
        metricName: 'name1'
      });
      var json = {
        metrics: {
          flume: {
            flume: {
              type1: {
                'C1': {
                  'name1': {data: 'data'}
                }
              }
            }
          }
        }
      };
  
      expect(view.getData(json)).to.be.eql([{
        name: 'C1',
        data: {data: 'data'}
      }]);
    });
  });

});
