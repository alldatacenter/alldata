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
require('mappers/cluster_mapper');

describe('App.clusterMapper', function () {
  var records = App.Cluster.find();

  before(function () {
    records.clear();
  });

  afterEach(function () {
    records.clear();
  });

  describe('#map', function () {
    it('should load mapped data to model', function () {
      App.clusterMapper.map({
        Clusters: {
          cluster_id: 'c',
          cluster_name: 'c',
          stack_name: 'HDP',
          version: '3.0.0',
          security_type: 'NONE',
          total_hosts: 3,
          credential_store_properties: {
            p: 'v'
          },
          desired_configs: {
            'core-site': {
              tag: 't0'
            },
            'hdfs-site': {
              tag: 't1',
              host_overrides: [
                {
                  host_name: 'h0',
                  tag: 't2'
                },
                {
                  host_name: 'h1',
                  tag: 't3'
                }
              ]
            }
          }
        }
      });
      testHelpers.nestedExpect([
        {
          id: 'c',
          clusterName: 'c',
          stackName: 'HDP',
          version: '3.0.0',
          securityType: 'NONE',
          totalHosts: 3,
          credentialStoreProperties: {
            p: 'v'
          },
          desiredConfigs: [
            App.ConfigSiteTag.create({
              site: 'core-site',
              tag: 't0',
              hostOverrides: {}
            }),
            App.ConfigSiteTag.create({
              site: 'hdfs-site',
              tag: 't1',
              hostOverrides: {
                h0: 't2',
                h1: 't3'
              }
            })
          ]
        }
      ], records.toArray());
    });
  });
});
