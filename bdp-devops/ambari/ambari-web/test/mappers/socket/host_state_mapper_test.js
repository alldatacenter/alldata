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

require('mappers/socket/host_state_mapper');

describe('App.hostStateMapper', function () {

  describe('#map', function() {
    beforeEach(function() {
      sinon.stub(App.Host, 'find');
      sinon.stub(App.hostStateMapper, 'updatePropertiesByConfig');
    });
    afterEach(function() {
      App.Host.find.restore();
      App.hostStateMapper.updatePropertiesByConfig.restore();
    });

    it('updatePropertiesByConfig should be called', function() {
      const event = {
        host_name: 'S1',
        state: 'HEARTBEAT_LOST'
      };
      App.hostStateMapper.map(event);
      expect(App.hostStateMapper.updatePropertiesByConfig.calledOnce).to.be.true;
    });
  });
});
