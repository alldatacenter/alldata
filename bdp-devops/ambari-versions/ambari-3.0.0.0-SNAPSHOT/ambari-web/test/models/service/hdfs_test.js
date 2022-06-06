/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * License); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

require('models/service/hdfs');

describe('App.HDFSService', function () {

    describe('#isNnHaEnabled', function () {
      var record = App.HDFSService.createRecord({
        id: 'hdfs'
      });
      it('ha disabled', function () {
        record.reopen({
          hostComponents: [Em.Object.create({componentName: 'NAMENODE'})],
          snameNode: true
        });
        record.propertyDidChange('isNnHaEnabled');
        expect(record.get('isNnHaEnabled')).to.be.false;
      });
      it('ha enabled', function () {
        record.setProperties({
          hostComponents: [
            Em.Object.create({componentName: 'NAMENODE'}),
            Em.Object.create({componentName: 'NAMENODE'})
          ],
          snameNode: null
        });
        record.propertyDidChange('isNnHaEnabled');
        expect(record.get('isNnHaEnabled')).to.be.true;
      });
    });


});
