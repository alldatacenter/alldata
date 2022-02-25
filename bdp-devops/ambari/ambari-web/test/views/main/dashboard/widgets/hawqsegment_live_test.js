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
require('views/main/dashboard/widgets/text_widget');
require('views/main/dashboard/widgets/hawqsegment_live');

var view;

function testCounterOrNa(propertyName, dependentKey) {
  describe('#' + propertyName, function () {

    beforeEach(function () {
      view.reopen({
        model: Em.Object.create()
      });
      view.get('model').set(dependentKey, []);
    });

    it('n/a (1)', function () {
      view.get('model').set(dependentKey, null);
      expect(view.get(propertyName)).to.be.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('n/a (2)', function () {
      view.get('model').set(dependentKey, undefined);
      expect(view.get(propertyName)).to.be.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('value exist', function () {
      view.get('model').set(dependentKey, 123);
      expect(view.get(propertyName)).to.be.equal(123);
    });

  });
}

describe('App.HawqSegmentUpView', function() {

  beforeEach(function () {
    view = App.HawqSegmentUpView.create();
  });

  testCounterOrNa('hawqSegmentsStarted', 'hawqSegmentsStarted');
  testCounterOrNa('hawqSegmentsInstalled', 'hawqSegmentsInstalled');
  testCounterOrNa('hawqSegmentsStarted', 'hawqSegmentsStarted');

});
