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

var blueprintUtils = require('utils/blueprint');

describe('utils/blueprint', function() {
  var masterBlueprint = {
    blueprint: {
      host_groups: [
        {
          name: "host-group-1",
          components: [
            { name: "ZOOKEEPER_SERVER" },
            { name: "NAMENODE" },
            { name: "HBASE_MASTER" }
          ]
        },
        {
          name: "host-group-2",
          components: [
            { name: "SECONDARY_NAMENODE" }
          ]
        }
      ]
    },
    blueprint_cluster_binding: {
      host_groups: [
        {
          name: "host-group-1",
          hosts: [
            { fqdn: "host1" },
            { fqdn: "host2" }
          ]
        },
        {
          name: "host-group-2",
          hosts: [
            { fqdn: "host3" }
          ]
        }
      ]
    }
  };

  var slaveBlueprint = {
    blueprint: {
      host_groups: [
        {
          name: "host-group-1",
          components: [
            { name: "DATANODE" }
          ]
        },
        {
          name: "host-group-2",
          components: [
            { name: "DATANODE" },
            { name: "HDFS_CLIENT" },
            { name: "ZOOKEEPER_CLIENT" }
          ]
        }
      ]
    },
    blueprint_cluster_binding: {
      host_groups: [
        {
          name: "host-group-1",
          hosts: [
            { fqdn: "host3" }
          ]
        },
        {
          name: "host-group-2",
          hosts: [
            { fqdn: "host4" },
            { fqdn: "host5" }
          ]
        }
      ]
    }
  };

  describe('#matchGroups', function() {
    it('should compose same host group into pairs', function() {
      expect(blueprintUtils.matchGroups(masterBlueprint, slaveBlueprint)).to.deep.equal([
        { g1: "host-group-1" },
        { g1: "host-group-2", g2: "host-group-1" },
        { g2: "host-group-2" }
      ]);
    });
  });

  describe('#filterByComponents', function() {
    it('should remove all components except', function() {
      expect(blueprintUtils.filterByComponents(masterBlueprint, ["NAMENODE"])).to.deep.equal({
        blueprint: {
          host_groups: [
            {
              name: "host-group-1",
              components: [
                { name: "NAMENODE" }
              ]
            }
          ]
        },
        blueprint_cluster_binding: {
          host_groups: [
            {
              name: "host-group-1",
              hosts: [
                { fqdn: "host1" },
                { fqdn: "host2" }
              ]
            }
          ]
        }
      });
    });
  });

  describe('#addComponentsToBlueprint', function() {
    it('should add components to blueprint', function() {
      var components = ["FLUME_HANDLER", "HCAT"];
      expect(blueprintUtils.addComponentsToBlueprint(masterBlueprint, components)).to.deep.equal({
        blueprint: {
          host_groups: [
            {
              name: "host-group-1",
              components: [
                { name: "ZOOKEEPER_SERVER" },
                { name: "NAMENODE" },
                { name: "HBASE_MASTER" },
                { name: "FLUME_HANDLER" },
                { name: "HCAT" }
              ]
            },
            {
              name: "host-group-2",
              components: [
                { name: "SECONDARY_NAMENODE" },
                { name: "FLUME_HANDLER" },
                { name: "HCAT" }
              ]
            }
          ]
        },
        blueprint_cluster_binding: {
          host_groups: [
            {
              name: "host-group-1",
              hosts: [
                { fqdn: "host1" },
                { fqdn: "host2" }
              ]
            },
            {
              name: "host-group-2",
              hosts: [
                { fqdn: "host3" }
              ]
            }
          ]
        }
      });
    });
  });

  describe('#mergeBlueprints', function() {
    it('should merge components', function() {
      expect(blueprintUtils.mergeBlueprints(masterBlueprint, slaveBlueprint)).to.deep.equal(
        {
          blueprint: {
            host_groups: [
              {
                name: "host-group-1",
                components: [
                  { name: "ZOOKEEPER_SERVER" },
                  { name: "NAMENODE" },
                  { name: "HBASE_MASTER" }
                ]
              },
              {
                name: "host-group-2",
                components: [
                  { name: "SECONDARY_NAMENODE" },
                  { name: "DATANODE" }
                ]
              },
              {
                name: "host-group-3",
                components: [
                  { name: "DATANODE" },
                  { name: "HDFS_CLIENT" },
                  { name: "ZOOKEEPER_CLIENT" }
                ]
              }
            ]
          },
          blueprint_cluster_binding: {
            host_groups: [
              {
                name: "host-group-1",
                hosts: [
                  { fqdn: "host1" },
                  { fqdn: "host2" }
                ]
              },
              {
                name: "host-group-2",
                hosts: [
                  { fqdn: "host3" }
                ]
              },
              {
                name: "host-group-3",
                hosts: [
                  { fqdn: "host4" },
                  { fqdn: "host5" }
                ]
              }
            ]
          }
        }
      );
    });
  });

  describe('#buildConfigsJSON', function () {
    var tests = [
      {
        "stepConfigs": [
          Em.Object.create({
            serviceName: "YARN",
            configs: [
              Em.Object.create({
                name: "p1",
                value: "v1",
                filename: "yarn-site.xml",
                isRequiredByAgent: true
              }),
              Em.Object.create({
                name: "p2",
                value: "v2",
                filename: "yarn-site.xml",
                isRequiredByAgent: true
              }),
              Em.Object.create({
                name: "p3",
                value: "v3",
                filename: "yarn-env.xml",
                isRequiredByAgent: true
              })
            ]
          }),
          Em.Object.create({
            serviceName: "MISC",
            configs: [
              Em.Object.create({
                name: "user",
                value: "yarn",
                filename: "yarn-env.xml",
                isRequiredByAgent: true
              })
            ]
          })
        ],
        "configurations": {
          "yarn-site": {
            "properties": {
              "p1": "v1",
              "p2": "v2"
            }
          },
          "yarn-env": {
            "properties": {
              "p3": "v3",
              "user": "yarn"
            }
          }
        }
      }
    ];
    tests.forEach(function (test) {
      it("generate configs for request (use in validation)", function () {
        expect(blueprintUtils.buildConfigsJSON(test.stepConfigs)).to.eql(test.configurations);
      });
    });
  });

  describe('#generateHostGroups()', function () {
    beforeEach(function() {
      sinon.stub(blueprintUtils, 'getComponentForHosts').returns({
        "host1": ["C1", "C2"],
        "host2": ["C1", "C3"]
      });
    });
    afterEach(function() {
      blueprintUtils.getComponentForHosts.restore();
    });

    var tests = [
      {
        "hostNames": ["host1", "host2"],
        "hostComponents": [
          Em.Object.create({
            componentName: "C1",
            hostName: "host1"
          }),
          Em.Object.create({
            componentName: "C2",
            hostName: "host1"
          }),
          Em.Object.create({
            componentName: "C1",
            hostName: "host2"
          }),
          Em.Object.create({
            componentName: "C3",
            hostName: "host2"
          })
        ],
        result: {
          blueprint: {
            host_groups: [
              {
                name: "host-group-1",
                "components": [
                  {
                    "name": "C1"
                  },
                  {
                    "name": "C2"
                  }
                ]
              },
              {
                name: "host-group-2",
                "components": [
                  {
                    "name": "C1"
                  },
                  {
                    "name": "C3"
                  }
                ]
              }
            ]
          },
          blueprint_cluster_binding: {
            host_groups: [
              {
                "name": "host-group-1",
                "hosts": [
                  {
                    "fqdn": "host1"
                  }
                ]
              },
              {
                "name": "host-group-2",
                "hosts": [
                  {
                    "fqdn": "host2"
                  }
                ]
              }
            ]
          }
        }
      }
    ];
    tests.forEach(function (test) {
      it("generate host groups", function () {
        expect(blueprintUtils.generateHostGroups(test.hostNames)).to.eql(test.result);
      });
    });
  });

  describe("#getComponentForHosts()", function() {
    var res;
    beforeEach(function() {
      sinon.stub(App.ClientComponent, 'find').returns([
        Em.Object.create({
          componentName: "C1",
          hostNames: ["host1", "host2"]
        })
      ]);
      sinon.stub(App.SlaveComponent, 'find').returns([
        Em.Object.create({
          componentName: "C2",
          hostNames: ["host2", "host3"]
        })
      ]);
      sinon.stub(App.MasterComponent, 'find').returns([
        Em.Object.create({
          componentName: "C3",
          hostNames: ["host3"]
        })
      ]);
      res = blueprintUtils.getComponentForHosts();
    });
    afterEach(function() {
      App.ClientComponent.find.restore();
      App.SlaveComponent.find.restore();
      App.MasterComponent.find.restore();
    });

    it('map for 3 items is created', function () {
      expect(Object.keys(res)).to.have.property('length').equal(3);
    });

    it("host1 map is valid", function() {
      expect(res.host1.toArray()).to.eql(['C1']);
    });
    it("host2 map is valid", function() {
      expect(res.host2.toArray()).to.eql(['C1', 'C2']);
    });
    it("host3 map is valid", function() {
      expect(res.host3.toArray()).to.eql(['C2', 'C3']);
    });
  });

  describe('#_generateHostMap', function() {
    it('generate map', function() {
      var map = blueprintUtils._generateHostMap({}, ['h1','h2', 'h1'],'c1');
      expect(map.h1[0]).to.be.equal('c1');
      expect(map.h2[0]).to.be.equal('c1');
    });

    it('skip generations as hosts is empty', function() {
      expect(blueprintUtils._generateHostMap({}, [],'c1')).to.eql({});
    });

    it('skip throws error when data is wrong (should assert error if no data returned from server)', function() {
      expect(function () {
        blueprintUtils._generateHostMap();
      }).to.throw(Error);
    });
  });

  describe('#getHostGroupByFqdn', function() {
    it('should return `null` if blueprint undefined', function() {
      expect(blueprintUtils.getHostGroupByFqdn(undefined, 'host1')).to.be.null;
    });

    it('should return `null` if blueprint not valid', function() {
      expect(blueprintUtils.getHostGroupByFqdn({not_valid_object: {}}, 'host1')).to.be.null;
    });

    it('should find host1-group by host1.name', function() {
      var bp = {
        blueprint_cluster_binding: {
          host_groups: [
            {
              hosts: [
                {fqdn: 'host2.name'}
              ],
              name: 'host2-group'
            },
            {
              hosts: [
                {fqdn: 'host1.name'}
              ],
              name: 'host1-group'
            }
          ]
        }
      };
      expect(blueprintUtils.getHostGroupByFqdn(bp, 'host1.name')).to.be.equal('host1-group');
    });
  });

  describe('#addComponentToHostGroup', function() {
    it('should add new component to host1-group', function() {
      var bp = {
        blueprint: {
          host_groups: [
            {
              components: [
                { name: 'COMPONENT1'}
              ],
              name: 'host1-group'
            }
          ]
        }
      };
      var expected = {
        blueprint: {
          host_groups: [
            {
              components: [
                { name: 'COMPONENT1'},
                { name: 'COMPONENT2'}
              ],
              name: 'host1-group'
            }
          ]
        }
      };
      expect(blueprintUtils.addComponentToHostGroup(bp, 'COMPONENT2', 'host1-group').toString()).to.eql(expected.toString());
    });

    it('should skip adding component since it already in host1-group', function() {
      var bp = {
        blueprint: {
          host_groups: [
            {
              components: [
                { name: 'COMPONENT1'}
              ],
              name: 'host1-group'
            }
          ]
        }
      };
      var expected = {
        blueprint: {
          host_groups: [
            {
              components: [
                { name: 'COMPONENT1'}
              ],
              name: 'host1-group'
            }
          ]
        }
      };
      expect(blueprintUtils.addComponentToHostGroup(bp, 'COMPONENT1', 'host1-group').toString()).to.eql(expected.toString());
    });

    it('should create components attribute and add component to host1-group', function() {
      var bp = {
        blueprint: {
          host_groups: [
            {
              name: 'host1-group'
            }
          ]
        }
      };
      var expected = {
        blueprint: {
          host_groups: [
            {
              components: [
                { name: 'COMPONENT1'}
              ],
              name: 'host1-group'
            }
          ]
        }
      };
      expect(blueprintUtils.addComponentToHostGroup(bp, 'COMPONENT1', 'host1-group').toString()).to.eql(expected.toString());
    });
  });
});
