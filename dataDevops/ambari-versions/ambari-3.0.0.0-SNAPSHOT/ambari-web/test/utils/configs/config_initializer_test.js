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

require('models/configs/objects/service_config_property');
require('utils/configs/config_initializer');

var serviceConfigProperty;

describe('App.ConfigInitializer', function () {

  beforeEach(function () {
    serviceConfigProperty = App.ServiceConfigProperty.create();
  });

  describe('#setRecommendedValue', function () {
    it('should change the recommended value', function () {
      serviceConfigProperty.set('recommendedValue', 'value0');
      App.ConfigInitializer.setRecommendedValue(serviceConfigProperty, /\d/, '1');
      expect(serviceConfigProperty.get('recommendedValue')).to.equal('value1');
    });
  });

  describe('#initialValue', function () {

    var cases = {
      'kafka.ganglia.metrics.host': [
        {
          message: 'kafka.ganglia.metrics.host property should have the value of ganglia hostname when ganglia is selected',
          localDB: {
            masterComponentHosts: [
              {
                component: 'GANGLIA_SERVER',
                hostName: 'c6401'
              }
            ]
          },
          expected: 'c6401'
        },
        {
          message: 'kafka.ganglia.metrics.host property should have the value "localhost" when ganglia is not selected',
          localDB: {
            masterComponentHosts: [
              {
                component: 'NAMENODE',
                hostName: 'c6401'
              }
            ]
          },
          expected: 'localhost'
        }
      ],
      'hive_database': [
        {
          alwaysEnableManagedMySQLForHive: true,
          currentStateName: '',
          isManagedMySQLForHiveEnabled: false,
          receivedValue: 'New MySQL Database',
          value: 'New MySQL Database',
          options: [
            {
              displayName: 'New MySQL Database'
            }
          ],
          hidden: false
        },
        {
          alwaysEnableManagedMySQLForHive: false,
          currentStateName: 'configs',
          isManagedMySQLForHiveEnabled: false,
          receivedValue: 'New MySQL Database',
          value: 'New MySQL Database',
          options: [
            {
              displayName: 'New MySQL Database'
            }
          ],
          hidden: false
        },
        {
          alwaysEnableManagedMySQLForHive: false,
          currentStateName: '',
          isManagedMySQLForHiveEnabled: true,
          receivedValue: 'New MySQL Database',
          value: 'New MySQL Database',
          options: [
            {
              displayName: 'New MySQL Database'
            }
          ],
          hidden: false
        },
        {
          alwaysEnableManagedMySQLForHive: false,
          currentStateName: '',
          isManagedMySQLForHiveEnabled: false,
          receivedValue: 'New MySQL Database',
          value: 'Existing MySQL Database',
          options: [
            {
              displayName: 'New MySQL Database'
            }
          ],
          hidden: true
        },
        {
          alwaysEnableManagedMySQLForHive: false,
          currentStateName: '',
          isManagedMySQLForHiveEnabled: false,
          receivedValue: 'New PostgreSQL Database',
          value: 'New PostgreSQL Database',
          options: [
            {
              displayName: 'New MySQL Database'
            }
          ],
          hidden: true
        }
      ],
      'hbase.zookeeper.quorum': [
        {
          filename: 'hbase-site.xml',
          value: 'host0,host1',
          recommendedValue: 'host0,host1',
          title: 'should set ZooKeeper Server hostnames'
        },
        {
          filename: 'ams-hbase-site.xml',
          value: 'localhost',
          recommendedValue: null,
          title: 'should ignore ZooKeeper Server hostnames'
        }
      ],
      'hivemetastore_host': {
        localDB: {
          masterComponentHosts: [
            {
              component: 'HIVE_METASTORE',
              hostName: 'h0'
            },
            {
              component: 'HIVE_METASTORE',
              hostName: 'h1'
            }
          ]
        },
        value: ['h0', 'h1'],
        title: 'array that contains names of hosts with Hive Metastore'
      },
      'hive_master_hosts': {
        localDB: {
          masterComponentHosts: [
            {
              component: 'HIVE_SERVER',
              hostName: 'h0'
            },
            {
              component: 'HIVE_METASTORE',
              hostName: 'h0'
            },
            {
              component: 'HIVE_METASTORE',
              hostName: 'h1'
            },
            {
              component: 'WEBHCAT_SERVER',
              hostName: 'h2'
            }
          ]
        },
        value: 'h0,h1',
        title: 'comma separated list of hosts with Hive Server and Metastore'
      },
      'hive.metastore.uris': {
        localDB: {
          masterComponentHosts: [
            {
              component: 'HIVE_METASTORE',
              hostName: 'h0'
            },
            {
              component: 'HIVE_METASTORE',
              hostName: 'h1'
            }
          ]
        },
        dependencies: {
          'hive.metastore.uris': 'thrift://localhost:9083'
        },
        filename: 'hive-site.xml',
        recommendedValue: 'thrift://localhost:9083',
        value: 'thrift://h0:9083,thrift://h1:9083',
        title: 'comma separated list of Metastore hosts with thrift prefix and port'
      },
      'templeton.hive.properties': {
        localDB: {
          masterComponentHosts: [
            {
              component: 'HIVE_METASTORE',
              hostName: 'h0'
            },
            {
              component: 'HIVE_METASTORE',
              hostName: 'h1'
            }
          ]
        },
        dependencies: {
          'hive.metastore.uris': 'thrift://localhost:9083'
        },
        recommendedValue: 'hive.metastore.local=false,hive.metastore.uris=thrift://localhost:9083,hive.metastore.sasl.enabled=false',
        value: 'hive.metastore.local=false,hive.metastore.uris=thrift://h0:9083\\,thrift://h1:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true',
        title: 'should add relevant hive.metastore.uris value'
      },
      'yarn.resourcemanager.zk-address': {
        localDB: {
          masterComponentHosts: [
            {
              component: 'ZOOKEEPER_SERVER',
              hostName: 'h0'
            },
            {
              component: 'ZOOKEEPER_SERVER',
              hostName: 'h1'
            }
          ]
        },
        dependencies: {
          clientPort: '2182'
        },
        recommendedValue: 'localhost:2181',
        value: 'h0:2182,h1:2182',
        title: 'should add ZK host and port dynamically'
      },
      'knox_gateway_host': {
        localDB: {
          masterComponentHosts: [
            {
              component: 'KNOX_GATEWAY',
              hostName: 'h0'
            },
            {
              component: 'KNOX_GATEWAY',
              hostName: 'h1'
            }
          ]
        },
        value: ['h0', 'h1'],
        title: 'array that contains names of hosts with Knox Gateway'
      },
      'atlas.rest.address': [
        {
          localDB: {
            masterComponentHosts: [
              {
                component: 'ZOOKEEPER_SERVER',
                hostName: 'h0'
              },
              {
                component: 'ZOOKEEPER_SERVER',
                hostName: 'h1'
              }
            ]
          },
          dependencies: {
            'atlas.enableTLS': false,
            'atlas.server.http.port': 21000,
            'atlas.server.https.port': 21443
          },
          value: 'http://h0:21000,http://h1:21000',
          title: 'TLS is not enabled'
        },
        {
          localDB: {
            masterComponentHosts: [
              {
                component: 'ZOOKEEPER_SERVER',
                hostName: 'h0'
              },
              {
                component: 'ZOOKEEPER_SERVER',
                hostName: 'h1'
              }
            ]
          },
          dependencies: {
            'atlas.enableTLS': true,
            'atlas.server.http.port': 21000,
            'atlas.server.https.port': 21443
          },
          value: 'https://h0:21443,https://h1:21443',
          title: 'TLS is enabled'
        }
      ]
    };

    cases['atlas.rest.address'].forEach(function (test) {
      it(test.title, function () {
        serviceConfigProperty.setProperties({
          name: 'atlas.rest.address',
          value: ''
        });
        App.ConfigInitializer.initialValue(serviceConfigProperty, test.localDB, test.dependencies);
        expect(serviceConfigProperty.get('value')).to.equal(test.value);
        expect(serviceConfigProperty.get('recommendedValue')).to.equal(test.value);
      });
    });

    cases['kafka.ganglia.metrics.host'].forEach(function (item) {
      it(item.message, function () {
        serviceConfigProperty.setProperties({
          name: 'kafka.ganglia.metrics.host',
          value: 'localhost'
        });
        App.ConfigInitializer.initialValue(serviceConfigProperty, item.localDB, []);
        expect(serviceConfigProperty.get('value')).to.equal(item.expected);
      });
    });

    describe('hive_database', function () {

      beforeEach(function () {
        this.stub = sinon.stub(App, 'get');
      });

      afterEach(function () {
        App.get.restore();
      });

      cases.hive_database.forEach(function (item) {
        var title = 'hive_database value should be set to {0}';
        describe(title.format(item.value), function () {

          beforeEach(function () {
            this.stub
              .withArgs('supports.alwaysEnableManagedMySQLForHive').returns(item.alwaysEnableManagedMySQLForHive)
              .withArgs('router.currentState.name').returns(item.currentStateName)
              .withArgs('isManagedMySQLForHiveEnabled').returns(item.isManagedMySQLForHiveEnabled);
            serviceConfigProperty.setProperties({
              name: 'hive_database',
              value: item.receivedValue,
              options: item.options
            });
            App.ConfigInitializer.initialValue(serviceConfigProperty, {}, []);
          });

          it('value is ' + item.value, function () {
            expect(serviceConfigProperty.get('value')).to.equal(item.value);
          });

          it('`New MySQL Database` is ' + (item.hidden ? '' : 'not') + ' hidden', function () {
            expect(serviceConfigProperty.get('options').findProperty('displayName', 'New MySQL Database').hidden).to.equal(item.hidden);
          });

        });
      });

    });

    cases['hbase.zookeeper.quorum'].forEach(function (item) {
      it(item.title, function () {
        serviceConfigProperty.setProperties({
          name: 'hbase.zookeeper.quorum',
          value: 'localhost',
          'filename': item.filename
        });
        App.ConfigInitializer.initialValue(serviceConfigProperty, {
          masterComponentHosts: {
            filterProperty: function () {
              return {
                mapProperty: function () {
                  return ['host0', 'host1'];
                }
              };
            }
          }
        }, []);
        expect(serviceConfigProperty.get('value')).to.equal(item.value);
        expect(serviceConfigProperty.get('recommendedValue')).to.equal(item.recommendedValue);
      });
    });

    it(cases.hive_master_hosts.title, function () {
      serviceConfigProperty.set('name', 'hive_master_hosts');
      App.ConfigInitializer.initialValue(serviceConfigProperty, cases.hive_master_hosts.localDB, []);
      expect(serviceConfigProperty.get('value')).to.equal(cases.hive_master_hosts.value);
    });

    it(cases['hive.metastore.uris'].title, function () {
      serviceConfigProperty.setProperties({
        name: 'hive.metastore.uris',
        recommendedValue: cases['hive.metastore.uris'].recommendedValue,
        filename: 'hive-site.xml'
      });
      App.ConfigInitializer.initialValue(serviceConfigProperty, cases['hive.metastore.uris'].localDB, {'hive.metastore.uris': cases['hive.metastore.uris'].recommendedValue});
      expect(serviceConfigProperty.get('value')).to.equal(cases['hive.metastore.uris'].value);
      expect(serviceConfigProperty.get('recommendedValue')).to.equal(cases['hive.metastore.uris'].value);
    });

    it(cases['templeton.hive.properties'].title, function () {
      serviceConfigProperty.setProperties({
        name: 'templeton.hive.properties',
        recommendedValue: cases['templeton.hive.properties'].recommendedValue,
        value: cases['templeton.hive.properties'].recommendedValue
      });
      App.ConfigInitializer.initialValue(serviceConfigProperty, cases['templeton.hive.properties'].localDB, {'hive.metastore.uris': cases['templeton.hive.properties'].recommendedValue});
      expect(serviceConfigProperty.get('value')).to.equal(cases['templeton.hive.properties'].value);
      expect(serviceConfigProperty.get('recommendedValue')).to.equal(cases['templeton.hive.properties'].value);
    });

    it(cases['yarn.resourcemanager.zk-address'].title, function () {
      serviceConfigProperty.setProperties({
        name: 'yarn.resourcemanager.zk-address',
        recommendedValue: cases['yarn.resourcemanager.zk-address'].recommendedValue
      });
      App.ConfigInitializer.initialValue(serviceConfigProperty, cases['yarn.resourcemanager.zk-address'].localDB, cases['yarn.resourcemanager.zk-address'].dependencies);
      expect(serviceConfigProperty.get('value')).to.equal(cases['yarn.resourcemanager.zk-address'].value);
      expect(serviceConfigProperty.get('recommendedValue')).to.equal(cases['yarn.resourcemanager.zk-address'].value);
    });

    function getLocalDBForSingleComponent(component) {
      return {
        masterComponentHosts: [
          {
            component: component,
            hostName: 'h1'
          },
          {
            component: 'FAKE_COMPONENT',
            hostName: 'FAKE_HOST'
          }
        ]
      };
    }

    function getLocalDBForMultipleComponents(component, count) {
      var ret = {
        masterComponentHosts: [{
          component: 'FAKE_COMPONENT',
          hostName: 'FAKE_HOST'
        }]
      };
      for (var i = 1; i <= count; i++) {
        ret.masterComponentHosts.push({
          component: component,
          hostName: 'h' + i
        })
      }
      return ret;
    }

    Em.A([
      {
        config: 'dfs.namenode.rpc-address',
        localDB: getLocalDBForSingleComponent('NAMENODE'),
        rValue: 'c6401.ambari.apache.org:8020',
        expectedValue: 'h1:8020'
      },
      {
        config: 'dfs.http.address',
        localDB: getLocalDBForSingleComponent('NAMENODE'),
        rValue: 'c6401.ambari.apache.org:8020',
        expectedValue: 'h1:8020'
      },
      {
        config: 'dfs.namenode.http-address',
        localDB: getLocalDBForSingleComponent('NAMENODE'),
        rValue: 'c6401.ambari.apache.org:8020',
        expectedValue: 'h1:8020'
      },
      {
        config: 'dfs.https.address',
        localDB: getLocalDBForSingleComponent('NAMENODE'),
        rValue: 'c6401.ambari.apache.org:8020',
        expectedValue: 'h1:8020'
      },
      {
        config: 'dfs.namenode.https-address',
        localDB: getLocalDBForSingleComponent('NAMENODE'),
        rValue: 'c6401.ambari.apache.org:8020',
        expectedValue: 'h1:8020'
      },
      {
        config: 'fs.default.name',
        localDB: getLocalDBForSingleComponent('NAMENODE'),
        rValue: 'hdfs://c6401.ambari.apache.org:8020',
        expectedValue: 'hdfs://h1:8020'
      },
      {
        config: 'fs.defaultFS',
        localDB: getLocalDBForSingleComponent('NAMENODE'),
        rValue: 'hdfs://c6401.ambari.apache.org:8020',
        expectedValue: 'hdfs://h1:8020'
      },
      {
        config: 'hbase.rootdir',
        localDB: getLocalDBForSingleComponent('NAMENODE'),
        rValue: 'hdfs://c6401.ambari.apache.org:8020',
        expectedValue: 'hdfs://h1:8020'
      },
      {
        config: 'instance.volumes',
        localDB: getLocalDBForSingleComponent('NAMENODE'),
        rValue: 'hdfs://c6401.ambari.apache.org:8020',
        expectedValue: 'hdfs://h1:8020'
      },
      {
        config: 'dfs.secondary.http.address',
        localDB: getLocalDBForSingleComponent('SECONDARY_NAMENODE'),
        rValue: 'c6401.ambari.apache.org:50090',
        expectedValue: 'h1:50090'
      },
      {
        config: 'dfs.namenode.secondary.http-address',
        localDB: getLocalDBForSingleComponent('SECONDARY_NAMENODE'),
        rValue: 'c6401.ambari.apache.org:50090',
        expectedValue: 'h1:50090'
      },
      {
        config: 'yarn.log.server.url',
        localDB: getLocalDBForSingleComponent('HISTORYSERVER'),
        rValue: 'http://localhost:19888/jobhistory/logs',
        expectedValue: 'http://h1:19888/jobhistory/logs'
      },
      {
        config: 'mapreduce.jobhistory.webapp.address',
        localDB: getLocalDBForSingleComponent('HISTORYSERVER'),
        rValue: 'c6407.ambari.apache.org:19888',
        expectedValue: 'h1:19888'
      },
      {
        config: 'mapreduce.jobhistory.address',
        localDB: getLocalDBForSingleComponent('HISTORYSERVER'),
        rValue: 'c6407.ambari.apache.org:19888',
        expectedValue: 'h1:19888'
      },
      {
        config: 'yarn.resourcemanager.hostname',
        localDB: getLocalDBForSingleComponent('RESOURCEMANAGER'),
        rValue: 'c6407.ambari.apache.org',
        expectedValue: 'h1'
      },
      {
        config: 'yarn.resourcemanager.resource-tracker.address',
        localDB: getLocalDBForSingleComponent('RESOURCEMANAGER'),
        rValue: 'c6407.ambari.apache.org:123',
        expectedValue: 'h1:123'
      },
      {
        config: 'yarn.resourcemanager.webapp.https.address',
        localDB: getLocalDBForSingleComponent('RESOURCEMANAGER'),
        rValue: 'c6407.ambari.apache.org:123',
        expectedValue: 'h1:123'
      },
      {
        config: 'yarn.resourcemanager.webapp.address',
        localDB: getLocalDBForSingleComponent('RESOURCEMANAGER'),
        rValue: 'c6407.ambari.apache.org:123',
        expectedValue: 'h1:123'
      },
      {
        config: 'yarn.resourcemanager.scheduler.address',
        localDB: getLocalDBForSingleComponent('RESOURCEMANAGER'),
        rValue: 'c6407.ambari.apache.org:123',
        expectedValue: 'h1:123'
      },
      {
        config: 'yarn.resourcemanager.address',
        localDB: getLocalDBForSingleComponent('RESOURCEMANAGER'),
        rValue: 'c6407.ambari.apache.org:123',
        expectedValue: 'h1:123'
      },
      {
        config: 'yarn.resourcemanager.admin.address',
        localDB: getLocalDBForSingleComponent('RESOURCEMANAGER'),
        rValue: 'c6407.ambari.apache.org:123',
        expectedValue: 'h1:123'
      },
      {
        config: 'yarn.timeline-service.webapp.address',
        localDB: getLocalDBForSingleComponent('APP_TIMELINE_SERVER'),
        rValue: 'c6407.ambari.apache.org:432',
        expectedValue: 'h1:432'
      },
      {
        config: 'yarn.timeline-service.address',
        localDB: getLocalDBForSingleComponent('APP_TIMELINE_SERVER'),
        rValue: 'c6407.ambari.apache.org:432',
        expectedValue: 'h1:432'
      },
      {
        config: 'yarn.timeline-service.webapp.https.address',
        localDB: getLocalDBForSingleComponent('APP_TIMELINE_SERVER'),
        rValue: 'c6407.ambari.apache.org:432',
        expectedValue: 'h1:432'
      },
      {
        config: 'mapred.job.tracker',
        localDB: getLocalDBForSingleComponent('JOBTRACKER'),
        rValue: 'c6407.ambari.apache.org:111',
        expectedValue: 'h1:111'
      },
      {
        config: 'mapred.job.tracker.http.address',
        localDB: getLocalDBForSingleComponent('JOBTRACKER'),
        rValue: 'c6407.ambari.apache.org:111',
        expectedValue: 'h1:111'
      },
      {
        config: 'mapreduce.history.server.http.address',
        localDB: getLocalDBForSingleComponent('HISTORYSERVER'),
        rValue: 'c6407.ambari.apache.org:555',
        expectedValue: 'h1:555'
      },
      {
        config: 'oozie.base.url',
        localDB: getLocalDBForSingleComponent('OOZIE_SERVER'),
        rValue: 'http://localhost:11000/oozie',
        expectedValue: 'http://h1:11000/oozie'
      },
      {
        config: 'nimbus.host',
        localDB: getLocalDBForSingleComponent('NIMBUS'),
        rValue: 'localhost',
        expectedValue: 'h1'
      },
      {
        config: '*.broker.url',
        localDB: getLocalDBForSingleComponent('FALCON_SERVER'),
        rValue: 'tcp://localhost:61616',
        expectedValue: 'tcp://h1:61616'
      },
      {
        config: 'storm.zookeeper.servers',
        localDB: getLocalDBForMultipleComponents('ZOOKEEPER_SERVER', 3),
        rValue: "['c6401.ambari.apache.org','c6402.ambari.apache.org']",
        expectedValue: ['h1', 'h2', 'h3']
      },
      {
        config: 'nimbus.seeds',
        localDB: getLocalDBForMultipleComponents('NIMBUS', 3),
        rValue: "['c6401.ambari.apache.org','c6402.ambari.apache.org']",
        expectedValue: ['h1', 'h2', 'h3']
      },
      {
        config: 'hawq_master_address_host',
        localDB: getLocalDBForSingleComponent('HAWQMASTER'),
        rValue: 'localhost',
        expectedValue: 'h1'
      },
      {
        config: 'hawq_standby_address_host',
        localDB: getLocalDBForSingleComponent('HAWQSTANDBY'),
        rValue: 'localhost',
        expectedValue: 'h1'
      },
      {
        config: 'hawq_dfs_url',
        localDB: getLocalDBForSingleComponent('NAMENODE'),
        rValue: 'localhost:8020/hawq_data',
        expectedValue: 'h1:8020/hawq_data'
      },
      {
        config: 'hawq_rm_yarn_address',
        localDB: getLocalDBForSingleComponent('RESOURCEMANAGER'),
        rValue: 'localhost:8032',
        expectedValue: 'h1:8032'
      },
      {
        config: 'hawq_rm_yarn_scheduler_address',
        localDB: getLocalDBForSingleComponent('RESOURCEMANAGER'),
        rValue: 'localhost:8030',
        expectedValue: 'h1:8030'
      },
      {
        config: 'hadoop_host',
        localDB: getLocalDBForSingleComponent('NAMENODE'),
        rValue: 'localhost',
        expectedValue: 'h1'
      },
      {
        config: 'hive_master_hosts',
        localDB: getLocalDBForMultipleComponents('HIVE_METASTORE', 3),
        rValue: '',
        expectedValue: 'h1,h2,h3'
      },
      {
        config: 'hive_master_hosts',
        localDB: getLocalDBForMultipleComponents('HIVE_SERVER', 3),
        rValue: '',
        expectedValue: 'h1,h2,h3'
      },
      {
        config: 'zookeeper.connect',
        localDB: getLocalDBForMultipleComponents('ZOOKEEPER_SERVER', 3),
        rValue: 'localhost:2181',
        expectedValue: 'h1:2181,h2:2181,h3:2181'
      },
      {
        config: 'hive.zookeeper.quorum',
        localDB: getLocalDBForMultipleComponents('ZOOKEEPER_SERVER', 3),
        rValue: 'localhost:2181',
        expectedValue: 'h1:2181,h2:2181,h3:2181'
      },
      {
        config: 'templeton.zookeeper.hosts',
        localDB: getLocalDBForMultipleComponents('ZOOKEEPER_SERVER', 3),
        rValue: 'localhost:2181',
        expectedValue: 'h1:2181,h2:2181,h3:2181'
      },
      {
        config: 'hadoop.registry.zk.quorum',
        localDB: getLocalDBForMultipleComponents('ZOOKEEPER_SERVER', 3),
        rValue: 'localhost:2181',
        expectedValue: 'h1:2181,h2:2181,h3:2181'
      },
      {
        config: 'hive.cluster.delegation.token.store.zookeeper.connectString',
        localDB: getLocalDBForMultipleComponents('ZOOKEEPER_SERVER', 3),
        rValue: 'localhost:2181',
        expectedValue: 'h1:2181,h2:2181,h3:2181'
      },
      {
        config: 'instance.zookeeper.host',
        localDB: getLocalDBForMultipleComponents('ZOOKEEPER_SERVER', 3),
        rValue: 'localhost:2181',
        expectedValue: 'h1:2181,h2:2181,h3:2181'
      },
      {
        config: 'templeton.hive.properties',
        localDB: getLocalDBForMultipleComponents('HIVE_METASTORE', 2),
        rValue: 'hive.metastore.local=false,hive.metastore.uris=thrift://localhost:9933,hive.metastore.sasl.enabled=false',
        dependencies: {
          'hive.metastore.uris': 'thrift://localhost:9083'
        },
        expectedValue: 'hive.metastore.local=false,hive.metastore.uris=thrift://h1:9083\\,thrift://h2:9083,hive.metastore.sasl.enabled=false'
      },
      {
        config: 'hbase.zookeeper.quorum',
        m: 'hbase.zookeeper.quorum hbase-site.xml',
        localDB: getLocalDBForMultipleComponents('ZOOKEEPER_SERVER', 3),
        rValue: 'c6401.ambari.apache.org,c6402.ambari.apache.org',
        expectedValue: 'h1,h2,h3',
        filename: 'hbase-site.xml'
      },
      {
        config: 'hbase.zookeeper.quorum',
        m: 'hbase.zookeeper.quorum not-hbase-site.xml',
        localDB: getLocalDBForMultipleComponents('ZOOKEEPER_SERVER', 3),
        rValue: 'localhost',
        expectedValue: '',
        expectedRValue: 'localhost',
        filename: 'not-hbase-site.xml'
      },
      {
        config: 'yarn.resourcemanager.zk-address',
        localDB: getLocalDBForMultipleComponents('ZOOKEEPER_SERVER', 3),
        rValue: 'localhost:2181',
        dependencies: {
          'clientPort': '3333'
        },
        expectedValue: 'h1:3333,h2:3333,h3:3333'
      },
      {
        config: 'RANGER_HOST',
        localDB: getLocalDBForSingleComponent('RANGER_ADMIN'),
        rValue: 'locahost',
        expectedValue: 'h1'
      },
      {
        config: 'hive.metastore.uris',
        filename: 'hive-site.xml',
        localDB: getLocalDBForMultipleComponents('HIVE_METASTORE', 2),
        dependencies: {
          'hive.metastore.uris': 'thrift://localhost:9083'
        },
        rValue: 'thrift://localhost:9083',
        expectedValue: 'thrift://h1:9083,thrift://h2:9083'
      }
    ]).forEach(function (test) {
      describe(test.m || test.config, function () {

        beforeEach(function () {
          serviceConfigProperty.setProperties({
            name: test.config,
            recommendedValue: test.rValue,
            filename: test.filename
          });
          App.ConfigInitializer.initialValue(serviceConfigProperty, test.localDB, test.dependencies);
        });

        it('value is ' + test.expectedValue, function () {
          expect(serviceConfigProperty.get('value')).to.eql(test.expectedValue);
        });

        if (Em.isNone(test.expectedRValue)) {
          it('recommendedValue is ' + test.expectedValue, function () {
            expect(serviceConfigProperty.get('recommendedValue')).to.eql(test.expectedValue);
          });
        }
        else {
          it('recommendedValue is ' + test.expectedRValue, function () {
            expect(serviceConfigProperty.get('recommendedValue')).to.eql(test.expectedRValue);
          });
        }

      });
    });

  });

  describe('#getHiveMetastoreUris', function () {

    var cases = [
      {
        hosts: [
          {
            hostName: 'h0',
            component: 'HIVE_SERVER'
          },
          {
            hostName: 'h1',
            component: 'HIVE_METASTORE'
          },
          {
            hostName: 'h2',
            component: 'HIVE_METASTORE'
          }
        ],
        recommendedValue: 'thrift://localhost:9083',
        expected: 'thrift://h1:9083,thrift://h2:9083',
        title: 'typical case'
      },
      {
        hosts: [
          {
            hostName: 'h0',
            component: 'HIVE_SERVER'
          }
        ],
        recommendedValue: 'thrift://localhost:9083',
        expected: '',
        title: 'no Metastore hosts in DB'
      },
      {
        hosts: [
          {
            hostName: 'h0',
            component: 'HIVE_SERVER'
          },
          {
            hostName: 'h1',
            component: 'HIVE_METASTORE'
          },
          {
            hostName: 'h2',
            component: 'HIVE_METASTORE'
          }
        ],
        recommendedValue: '',
        expected: '',
        title: 'default value without port'
      },
      {
        hosts: [
          {
            hostName: 'h0',
            component: 'HIVE_SERVER'
          },
          {
            hostName: 'h1',
            component: 'HIVE_METASTORE'
          },
          {
            hostName: 'h2',
            component: 'HIVE_METASTORE'
          }
        ],
        expected: '',
        title: 'no default value specified'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(App.ConfigInitializer.getHiveMetastoreUris(item.hosts, item.recommendedValue)).to.equal(item.expected);
      });
    });

  });

  describe('initializerTypes', function () {
    var types = App.ConfigInitializer.get('initializerTypes');
    Em.keys(types).forEach(function(type) {
      it(type, function() {
        var methodName = types[type].method;
        expect(methodName).to.be.a.string;
        expect(methodName).to.have.length.above(0);
        expect(App.ConfigInitializer[methodName]).to.be.a.function;
      });
    });
  });

  describe('initializers', function () {

    var initializers = App.ConfigInitializer.get('initializers');
    var types = App.ConfigInitializer.get('initializerTypes');
    var typeNames = types.mapProperty('name');

    Em.keys(initializers).forEach(function (configName) {
      it(configName, function () {
        var type = initializers[configName].type;
        expect(typeNames).to.contain(type);
      });
    });

  });

  describe('uniqueInitializers', function () {

    var uniqueInitializers = App.ConfigInitializer.get('uniqueInitializers');
    var uniqueInitializersNames = Em.keys(uniqueInitializers).map(function (key) {
      return uniqueInitializers[key];
    });

    it('should contains only unique methods', function () {
      expect(uniqueInitializersNames.length).to.equal(uniqueInitializersNames.uniq().length);
    });

    uniqueInitializersNames.forEach(function (name) {
      it(name, function () {
        expect(App.ConfigInitializer[name]).to.be.a.function;
      });
    });

  });
});
