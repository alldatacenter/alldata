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

var routeClass = require('routes/views');

describe('routes/views', function() {

  var route = routeClass.create().get('viewDetails').create();

  describe("#parseViewPath", function() {
    [
      {
        url: 'viewName',
        result: ''
      },
      {
        url: 'viewName?foo=bar&count=1',
        result: '?foo=bar&count=1'
      },
      {
        url: 'viewName?viewPath=%2Fuser%2Fadmin%2Faddress',
        result: '/user/admin/address'
      },
      {
        url: 'viewName?viewPath=%2Fuser%2Fadmin%2Faddress&foo=bar&count=1',
        result: '/user/admin/address?foo=bar&count=1'
      }
    ].forEach(function(test){
        it("url = " + test.url, function() {
          expect(route.parseViewPath(test.url)).to.equal(test.result);
        });
      });
  });

});
