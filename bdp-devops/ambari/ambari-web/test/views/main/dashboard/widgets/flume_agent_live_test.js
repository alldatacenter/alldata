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

describe('App.FlumeAgentUpView', function () {

  var view;

  beforeEach(function () {
    view = App.FlumeAgentUpView.create();
  });

  describe('#hiddenInfo', function () {
    it('should return coorect array of string for hidden info depends on flumeAgentsLive and flumeAgentsDead', function () {
      view.set('flumeAgentsLive', [{}, {}, {}]);
      view.set('flumeAgentsDead', [{}]);
      expect(view.get('hiddenInfo')).to.be.eql([3 + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.live'), 1 + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.dead')]);
    });
  });

  describe('#data', function () {
    it('should return -1 if no flumeAgentComponents provided', function () {
      expect(view.get('data')).to.eql(-1);
    });

    it('should return correct data', function () {
      view.set('flumeAgentsLive', [{}, {}, {}, {}]);
      view.set('model.hostComponents', [{componentName: 'FLUME_HANDLER'}, {componentName: ''}, {componentName: 'FLUME_HANDLER'}, {componentName: ''}]);
      expect(view.get('data')).to.equal(200);
    });
  });

  describe('#filterStatusOnce', function () {
    it('should set correct dead and live flumeAgents ', function () {
      view.set('model.hostComponents', [
        {componentName: 'FLUME_HANDLER', workStatus: 'STARTED'},
        {componentName: 'FLUME_HANDLER', workStatus: 'INSTALLED'},
        {componentName: 'FLUME_HANDLER', workStatus: 'INSTALLED'},
        {componentName: ''}]);
      view.filterStatusOnce();
      expect(view.get('flumeAgentsLive').length).to.be.equal(1);
      expect(view.get('flumeAgentsDead').length).to.be.equal(2);
    });
  });

  describe('#hintInfo', function () {
    it('should build label based on maxValue', function () {
      view.set('maxValue', 5);
      expect(view.get('hintInfo')).to.be.equal(Em.I18n.t('dashboard.widgets.hintInfo.hint1').format(5));
      view.set('maxValue', 7);
      expect(view.get('hintInfo')).to.be.equal(Em.I18n.t('dashboard.widgets.hintInfo.hint1').format(7));
    });
  });
});