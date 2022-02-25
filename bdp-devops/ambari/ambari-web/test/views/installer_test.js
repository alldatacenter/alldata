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

require('views/installer');

var view,
  stepsCount = 11,
  isStepDisabled = [],
  properties = [];

for (var i = 0; i < stepsCount; i++ ) {
  isStepDisabled.push(Em.Object.create({
    step: i,
    value: Boolean(Math.floor(Math.random() * 2))
  }));
  properties.push('isStep' + i + 'Disabled');
}

describe('App.InstallerView', function () {

  beforeEach(function () {
    view = App.InstallerView.create({
      controller: {
        isStepDisabled: isStepDisabled
      }
    });
  });

  properties.forEach(function (item, index) {
    describe(item, function () {
      it('should take value from isStepDisabled', function () {
        expect(view.get(item)).to.equal(isStepDisabled.findProperty('step', index).get('value'));
      });
    });
  });

});
