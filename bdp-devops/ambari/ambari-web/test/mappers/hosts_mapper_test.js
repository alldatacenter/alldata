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

require('models/host');
require('models/host_component');
require('mappers/server_data_mapper');
require('mappers/hosts_mapper');

describe('App.hostsMapper', function () {
  var mapper = App.hostsMapper;

  describe("#setMetrics()", function() {
    var data = {
      items: [
        {
          Hosts: {
            host_name: 'host1'
          },
          metrics: {
            load: {
              load_one: 1
            }
          }
        }
      ]
    };
    beforeEach(function(){
      this.mock = sinon.stub(App.Host, 'find')
    });
    afterEach(function(){
      this.mock.restore();
    });
    it("Host not in the model", function() {
      var host = Em.Object.create({
        hostName: 'host2'
      });
      var mockedModel = [host];
      mockedModel.content = mockedModel;
      this.mock.returns(mockedModel);
      mapper.setMetrics(data);
      expect(host.get('loadOne')).to.be.undefined;
    });
    it("Host should have updated metrics", function() {
      var host = Em.Object.create({
        hostName: 'host1'
      });
      var mockedModel = [host];
      mockedModel.content = mockedModel;
      this.mock.returns(mockedModel);
      mapper.setMetrics(data);
      expect(host.get('loadOne')).to.equal(1);
    });
  });
});
