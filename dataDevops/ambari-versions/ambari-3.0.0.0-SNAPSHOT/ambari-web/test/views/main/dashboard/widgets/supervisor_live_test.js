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

describe('App.SuperVisorUpView', function () {

  var view;

  beforeEach(function () {
    view = App.SuperVisorUpView.create();
  });

  describe('#hiddenInfo', function () {
    it('should return coorect array of string for hidden info depends on superVisorsLive and superVisorsDead', function () {
      view.set('model.superVisorsStarted', 3);
      view.set('model.superVisorsInstalled', 1);
      expect(view.get('hiddenInfo')).to.be.eql([3 + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.live'), 1 + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.dead')]);
    });
  });

  describe('#data', function () {
    it('should return -1 if no superVisorsTotal provided', function () {
      expect(view.get('data')).to.eql(-1);
    });

    it('should return correct data', function () {
      view.set('model.superVisorsStarted', 4);
      view.set('model.superVisorsTotal', 2);
      expect(view.get('data')).to.equal(200);
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