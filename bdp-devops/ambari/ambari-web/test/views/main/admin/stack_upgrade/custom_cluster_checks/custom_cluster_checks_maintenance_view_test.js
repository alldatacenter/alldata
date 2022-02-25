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
var testHelpers = require('test/helpers');

describe('App.MasterMaintenanceDisabledCheckView', function () {
  var view;
  beforeEach(function () {
    view = App.MasterMaintenanceDisabledCheckView.create({});
  });

  describe('#maintananceOff', function () {
    it('should call api with proper params', function () {
      view.maintananceOff({context: {name: 'test1'}});
      var args = testHelpers.findAjaxRequest('name', 'bulk_request.hosts.passive_state');
      expect(args[0]).to.be.eql({
        name: 'bulk_request.hosts.passive_state',
        sender: view,
        data: {
          hostNames: 'test1',
          passive_state: 'OFF',
          requestInfo: Em.I18n.t('hosts.host.details.for.postfix').format(view.t('passiveState.turn' + ' off'))
        }
      });
    });
  });

  describe('hosts', function () {
    it('should map failed hosts', function () {
      view.set('check', Em.Object.create({failed_on: ['test1', 'test2']}));
      expect(view.get('hosts')).to.be.eql([{name: 'test1', processing: false}, {name: 'test2', processing: false}])
    });
  });
});