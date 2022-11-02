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
require('views/main/service/info/metrics/flume/flume_agent_metrics_section');

describe('App.FlumeAgentMetricsSectionView', function () {

  var view;

  beforeEach(function () {
    view = App.FlumeAgentMetricsSectionView.create();
  });

  describe('#id', function () {
    it('should be set depending on index', function () {
      view.set('index', 1);
      expect(view.get('id')).to.equal('metric1');
    });
  });

  describe('#toggleIndex', function () {
    it('should be set depending on id', function () {
      view.reopen({
        id: 'metric1'
      });
      expect(view.get('toggleIndex')).to.equal('#metric1');
    });
  });

  describe('#header', function () {
    it('should be set depending on metric type and host name', function () {
      view.setProperties({
        metricTypeKey: 'sinkName',
        metricViewData: {
          agent: {
            hostName: 'h0'
          }
        }
      });
      expect(view.get('header')).to.equal(Em.I18n.t('services.service.info.metrics.flume.sinkName').
        format(Em.I18n.t('common.metrics')) + ' - h0');
    });
  });

});
