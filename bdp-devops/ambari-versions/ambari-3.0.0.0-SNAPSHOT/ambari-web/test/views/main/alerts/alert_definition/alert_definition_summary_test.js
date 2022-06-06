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

describe('App.AlertDefinitionSummary', function () {
  var view;
  beforeEach(function () {
    view = App.AlertDefinitionSummary.create();
  });

  describe('#definitionState', function () {
    it('should return empty array when no content provided', function () {
      expect(view.get('definitionState')).to.be.eql([]);
    });

    it('should return array of states without counts', function () {
      view.set('content', Em.Object.create({order: ['test1', 'test2'], summary: {}}));
      expect(view.get('definitionState')).to.be.eql([
        {
          state: 'alert-state-test1',
          count: '',
          maintenanceCount: ''
        },
        {
          state: 'alert-state-test2',
          count: '',
          maintenanceCount: ''
        }
      ])
    });

    it('should return array of states with counts if it is present in summary', function () {
      view.set('content', Em.Object.create({
        order: ['CRITICAL', 'WARNING', 'UNK', 'OK'],
        summary: {
          'CRITICAL': {
            count: 2
          },
          'OK': {
            maintenanceCount: 3
          }
        },
        hostCnt: 2
      }));
      expect(view.get('definitionState')).to.be.eql([
        {
          state: 'alert-state-CRITICAL',
          count: 'CRIT (2)',
          maintenanceCount: ''
        },
        {
          state: 'alert-state-WARNING',
          count: '',
          maintenanceCount: ''
        },
        {
          state: 'alert-state-UNK',
          count: '',
          maintenanceCount: ''
        },
        {
          state: 'alert-state-OK',
          count: '',
          maintenanceCount: 'OK (3)'
        }
      ])
    });

    it('should return array of states without counts if hostCnt is equal or less 1', function () {
      view.set('content', Em.Object.create({
        order: ['CRITICAL', 'WARNING', 'UNK', 'OK'],
        summary: {
          'CRITICAL': {
            count: 2
          },
          'OK': {
            maintenanceCount: 3
          }
        },
        hostCnt: 1
      }));
      expect(view.get('definitionState')).to.be.eql([
        {
          state: 'alert-state-CRITICAL',
          count: 'CRIT',
          maintenanceCount: ''
        },
        {
          state: 'alert-state-WARNING',
          count: '',
          maintenanceCount: ''
        },
        {
          state: 'alert-state-UNK',
          count: '',
          maintenanceCount: ''
        },
        {
          state: 'alert-state-OK',
          count: '',
          maintenanceCount: 'OK'
        }
      ])
    });
  });
});