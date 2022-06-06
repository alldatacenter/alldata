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
require('views/main/service/info/metrics/ambari_metrics/regionserver_base');

describe('App.ChartServiceMetricsAMS_RegionServerBaseView', function () {

  var regionServerView;
  var jsonData = {
    "metrics": {
      "hbase": {
        "regionserver": {
          "test": [[11.0, 1424948261], [11.0, 1424948306], [11.0, 1424948321]]
        }
      }
    }
  };

  beforeEach(function () {
    regionServerView = App.ChartServiceMetricsAMS_RegionServerBaseView.extend({
      id: "service-metrics-ambari-metrics-region-server-test",
      title: 'test-title',
      ajaxIndex: 'service.metrics.ambari_metrics.region_server.regions',
      displayName: 'test-display-name',
      regionServerName: 'test'
    }).create();
  });

  describe('#transformToSeries', function () {

    beforeEach(function () {
      this.result = regionServerView.transformToSeries(jsonData);
    });

    it('displayName', function () {
      expect(this.result[0].name).to.be.equal(regionServerView.displayName);
    });

    it('data.length', function () {
      expect(this.result[0].data.length).to.equal(jsonData.metrics.hbase.regionserver.test.length);
    });

    it('y-property', function () {
      expect(this.result[0].data[0]).to.have.property('y').to.equal(11);
    });

    it('x-property', function () {
      expect(this.result[0].data[0]).to.have.property('x').to.equal(1424948261);
    });

  });

});