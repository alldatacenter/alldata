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
require('views/main/alerts/add_alert_definition/step3_view');

var view;

describe('App.AddAlertDefinitionStep3View', function () {

  beforeEach(function () {
    view = App.AddAlertDefinitionStep3View.create({
      controller: Em.Object.create()
    });
  });

  describe("#willInsertElement()", function () {
    it("alertDefinitionToDisplay should be set", function () {
      view.set('controller.content', {
        formattedToRequestConfigs: 'formattedToRequestConfigs'
      });
      view.willInsertElement();
      expect(view.get('alertDefinitionToDisplay')).to.equal('\"formattedToRequestConfigs\"');
    });
  });
});
