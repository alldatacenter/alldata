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

require('utils/helper');
require('mappers/repository_version_mapper');

describe('App.repoVersionMapper', function () {

  describe("#convertToRepoScheme()", function () {

    it("json is null", function() {
      expect(App.repoVersionMapper.convertToRepoScheme(null)).to.be.eql({items: []});
    });

    it("json is correct", function() {
      var json = {
        items: [{
          versions: [{
            repository_versions: [{
              id: 1
            }]
          }]
        }]
      };
      expect(App.repoVersionMapper.convertToRepoScheme(json)).to.be.eql({items: [{id: 1}]});
    });
  });

});
