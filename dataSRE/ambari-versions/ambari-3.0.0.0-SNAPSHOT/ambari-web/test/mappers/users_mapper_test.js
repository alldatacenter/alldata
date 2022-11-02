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

require('mappers/server_data_mapper');
require('mappers/users_mapper');

describe('App.usersMapper', function () {

  describe('#isAdmin', function() {
    var tests = [
      {i:["AMBARI.ADMINISTRATOR"],e:true,m:'has admin role'},
      {i:["CLUSTER.USER", "AMBARI.ADMINISTRATOR"],e:true,m:'has admin role'},
      {i:["VIEW.USER"],e:false,m:'doesn\'t have admin role'},
      {i:["CLUSTER.ADMINISTRATOR"],e:true,m:'has admin role'}
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        expect(App.usersMapper.isAdmin(test.i)).to.equal(test.e);
      });
    });
  });

  describe('#isClusterUser', function() {
    var tests = [
      {i:["AMBARI.ADMINISTRATOR", "CLUSTER.USER"],e:false,m:'is cluster user'},
      {i:["CLUSTER.USER"],e:true,m:'has admin role'}
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        expect(App.usersMapper.isClusterUser(test.i)).to.equal(test.e);
      });
    });
  });
});
