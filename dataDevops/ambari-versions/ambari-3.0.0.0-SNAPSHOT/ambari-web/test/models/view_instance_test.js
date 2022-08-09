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

var model;

describe('App.ViewInstance', function () {

  beforeEach(function () {
    model = App.ViewInstance.create();
  });

  describe('#internalAmbariUrl', function () {

    [
      {
        props: {
          shortUrl: '',
          viewName: 'CAPACITY-SCHEDULER',
          version: '1.0.0',
          instanceName: 'AUTO_CS_INSTANCE'
        },
        e: '#/main/views/CAPACITY-SCHEDULER/1.0.0/AUTO_CS_INSTANCE',
        m: '`shortUrl` does not exist'
      },
      {
        props: {
          shortUrl: 'auto_cs_instance',
          viewName: 'CAPACITY-SCHEDULER',
          version: '1.0.0',
          instanceName: 'AUTO_CS_INSTANCE'
        },
        e: '#/main/view/CAPACITY-SCHEDULER/auto_cs_instance',
        m: '`shortUrl` exists'
      }
    ].forEach(function (test) {
      it(test.m, function () {
        model.setProperties(test.props);
        expect(model.get('internalAmbariUrl')).to.be.equal(test.e);
      });
    });

  });

});
