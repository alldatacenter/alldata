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
var O = Em.Object;

describe('utils/helper', function() {

  describe('String helpers', function() {
    describe('#trim()', function(){
      it('should replace first space', function() {
        expect(' as d f'.trim()).to.be.equal('as d f');
      });
    });
    describe('#endsWith()', function() {
      it('`abcd` ends with `d`', function(){
        expect('abcd'.endsWith('d')).to.be.true;
      });
      it('`abcd` doesn\'t end with `f`', function(){
        expect('abcd'.endsWith('f')).to.be.false;
      });
    });
    describe('#contains()', function() {
      it('`abc` contains b', function(){
        expect('abc'.contains('b')).to.be.true;
      });
      it('`abc` doesn\'t contain d', function() {
        expect('abc'.contains('d')).to.be.false;
      });
    });
    describe('#capitalize()',function() {
      it('`abc d` should start with `A`', function() {
        expect('abc d'.capitalize()).to.be.equal('Abc d');
      });
    });
    describe('#findIn()', function(){
      var obj = {
        a: {
          a1: 'AVal1'
        },
        b: 'BVal',
        c: {
          c1: {
            c2: 'Cval2'
          },
          b: 'BVal'
        }
      };
      var testValue = function(key, value) {
        it('key `' + key + '` should have `' + JSON.stringify(value) + '` value', function() {
          expect(key.findIn(obj)).to.eql(value);
        });
      };
      it('expect return `null` on non-object input', function(){
        expect('a'.findIn('b')).to.null;
      });
      testValue('a', obj.a);
      testValue('c2', obj.c.c1.c2);
      testValue('b', obj.b);
      testValue('d', null);
    });
    describe('#format()', function(){
      it('should replace string correctly', function(){
        expect("{0} world{1}".format("Hello","!")).to.be.equal("Hello world!");
      });
    });
    describe('#highlight()', function() {
      var str = "Hello world! I want to highlight this word!";
      it('should highlight `word` with default template', function() {
        var result = str.highlight(['word']);
        expect(result).to.be.equal("Hello world! I want to highlight this <b>word</b>!");
      });
      it('should highlight `world` and `word` with template `<span class="yellow">{0}</span>`', function() {
        var result = str.highlight(["world", "word"], '<span class="yellow">{0}</span>');
        expect(result).to.be.equal('Hello <span class="yellow">world</span>! I want to highlight this <span class="yellow">word</span>!')
      });
      var str2 = "First word, second word";
      it('should highlight `word` multiply times with default template', function() {
        var result = str2.highlight(["word"]);
        expect(result).to.be.equal("First <b>word</b>, second <b>word</b>");
      });
    });
  });

  describe('Number helpers', function(){
    describe('#toDaysHoursMinutes()', function(){
      var time = 1000000000;
      var minute = 1000*60;
      var hour = 60*minute;
      var day = 24*hour;
      var result = time.toDaysHoursMinutes();
      var testDays = Math.floor(time/day);
      it('should correct convert days', function(){
        expect(testDays).to.eql(result.d);
      });
      it('should correct convert hours', function(){
        expect(Math.floor((time - testDays * day)/hour)).to.eql(result.h);
      });
      it('should correct convert minutes', function(){
        expect(((time - Math.floor((time - testDays*day)/hour)*hour - testDays*day)/minute).toFixed(2)).to.eql(result.m);
      });
    });
  });

  describe('Array helpers', function(){

    var tests = Em.A([
      {
        m: 'plain objects, no nesting',
        array: [{a: 1}, {a: 2}, {a: 3}],
        property: 'a',
        callback3: function (item) {
          return Em.get(item, 'a');
        },
        e1: {1: {a: 1}, 2: {a: 2}, 3: {a: 3}},
        e2: {1: true, 2: true, 3: true},
        e3: {1: 1, 2: 2, 3: 3}
      },
      {
        m: 'plain objects, nesting',
        array: [{a: {a: 1}}, {a: {a: 2}}, {a:{a: 3}}],
        property: 'a.a',
        callback3: function (item) {
          return Em.get(item, 'a.a');
        },
        e1: {1: {a: {a: 1}}, 2: {a: {a: 2}}, 3: {a: {a: 3}}},
        e2: {1: true, 2: true, 3: true},
        e3: {1: 1, 2: 2, 3: 3}
      },
      {
        m: 'Ember objects, no nesting',
        array: [O.create({a: 1}), O.create({a: 2}), O.create({a: 3})],
        property: 'a',
        callback3: function (item) {
          return Em.get(item, 'a');
        },
        e1: {1: O.create({a: 1}), 2: O.create({a: 2}), 3: O.create({a: 3})},
        e2: {1: true, 2: true, 3: true},
        e3: {1: 1, 2: 2, 3: 3}
      },
      {
        m: 'Ember objects, nesting',
        array: [O.create({a: {a: 1}}), O.create({a: {a: 2}}), O.create({a: {a: 3}})],
        property: 'a.a',
        callback3: function (item) {
          return Em.get(item, 'a.a');
        },
        e1: {1: O.create({a: {a: 1}}), 2: O.create({a: {a: 2}}), 3: O.create({a: {a: 3}})},
        e2: {1: true, 2: true, 3: true},
        e3: {1: 1, 2: 2, 3: 3}
      }
    ]);

    describe('#sortPropertyLight()', function(){
      var testable = [
        { a: 2 },
        { a: 1 },
        { a: 6},
        { a: 64},
        { a: 3},
        { a: 3}
      ];
      var result = testable.sortPropertyLight('a');
      it('should return array with same length', function(){
        expect(testable.length).to.eql(result.length);
      });
      it('should sort array', function() {
        expect(result.mapProperty('a')).to.be.eql([1, 2, 3, 3, 6, 64]);
      });
      it('should try to sort without throwing exception', function(){
        expect(testable.sortPropertyLight(['a'])).to.ok;
      });
    });

    describe('#toMapByProperty', function () {
      tests.forEach(function (test) {
        it(test.m, function () {
          expect(test.array.toMapByProperty(test.property)).to.eql(test.e1);
        });
      });
    });

    describe('#toWickMapByProperty', function () {
      tests.forEach(function (test) {
        it(test.m, function () {
          expect(test.array.toWickMapByProperty(test.property)).to.eql(test.e2);
        });
      });
    });

    describe('#toMapByCallback', function () {
      tests.forEach(function (test) {
        it(test.m, function () {
          expect(test.array.toMapByCallback(test.property, test.callback3)).to.eql(test.e3);
        });
      });
    });

    describe('#toWickMap', function () {

      it('should convert to wick map', function () {
        expect([1,2,3].toWickMap()).to.eql({1: true, 2: true, 3: true});
      });

    });

  });

  describe('App helpers', function(){
    var appendDiv = function() {
      $('body').append('<div id="tooltip-test"></div>');
    };
    var removeDiv = function() {
      $('body').remove('#tooltip-test');
    };
    describe('#isEmptyObject', function(){
      it('should return true on empty object', function() {
        expect(App.isEmptyObject({})).to.be.true;
      });
      it('should return false on non-empty object', function() {
        expect(App.isEmptyObject({ a: 1 })).to.be.false;
      });
    });

    describe('#tooltip()', function() {
      beforeEach(appendDiv);
      afterEach(removeDiv);
      it('should add tooltip', function() {
        App.tooltip($('#tooltip-test'));
        expect($('#tooltip-test').data('bs.tooltip').enabled).to.be.true;
      });
    });
    describe('#popover()', function() {
      beforeEach(appendDiv);
      afterEach(removeDiv);
      it('should add popover', function() {
        App.popover($('#tooltip-test'));
        expect($('#tooltip-test').data('bs.popover').enabled).to.be.true;
      });
    });
    describe('#App.format', function(){
      describe('#commandDetail()', function() {
        var command = "GANGLIA_MONITOR STOP";
        var customCommandDetail = "Remove_Logical_Mycomponent Mycomponent";
        var opsDisplayName = "Remove Logical Mycomponent";
        var ignored = "DECOMMISSION, NAMENODE";
        var removeString = "SERVICE/HDFS STOP";
        var nagiosState = "nagios_update_ignore ACTIONEXECUTE";
        var installRepo = "install_packages ACTIONEXECUTE";
        it('should convert command to readable info', function() {
          expect(App.format.commandDetail(command)).to.be.equal(' Ganglia Monitor Stop');
        });
        it('should use display name for operations if specified', function() {
          expect(App.format.commandDetail(customCommandDetail, null, opsDisplayName)).to.be.equal(' Remove Logical Mycomponent');
        });
        it('should ignore decommission command', function(){
          expect(App.format.commandDetail(ignored)).to.be.equal('  NameNode');
        });
        it('should remove SERVICE string from command', function(){
          expect(App.format.commandDetail(removeString)).to.be.equal(' HDFS Stop');
        });
        it('should return maintenance message', function() {
          expect(App.format.commandDetail(nagiosState)).to.be.equal(' Toggle Maintenance Mode');
        });
        it('should return install repo message', function() {
          expect(App.format.commandDetail(installRepo)).to.be.equal(Em.I18n.t('common.installRepo.task'));
        });
        it('should return raw text when is_add_or_delete_slave_request=true', function() {
          var inputs = '{"is_add_or_delete_slave_request":"true"}';
          expect(App.format.commandDetail('DECOMMISSION, Update Include/Exclude Files', inputs))
            .to.be.equal('  Update Include/Exclude Files');
        });
      });
      describe('#taskStatus()', function(){
        var testable = [
          { status: 'PENDING', expectable: 'pending'},
          { status: 'QUEUED', expectable: 'queued'},
          { status: 'COMPLETED', expectable: 'completed'}
        ];
        testable.forEach(function(testObj){
          it('should convert `' + testObj.status + '` to `' + testObj.expectable + '`', function(){
            expect(App.format.taskStatus(testObj.status)).to.eql(testObj.expectable);
          });
        });
      });

      describe('#normalizeNameBySeparators()', function() {
        var testMessage = '`{0}` should be converted to `{1}`';
        var tests = {
          'APP_TIMELINE_SERVER': 'App Timeline Server',
          'app_timeline_server': 'App Timeline Server',
          'APP-TIMELINE-SERVER': 'App Timeline Server',
          'app-timeline-server': 'App Timeline Server',
          'APP TIMELINE SERVER': 'App Timeline Server',
          'app timeline server': 'App Timeline Server',
          'run-hcat-sync': 'Run HCat Client Sync',
          'rUN_hCAt_syNc': 'Run HCat Client Sync',
          'FALCON': 'Falcon',
          'falcon': 'Falcon'
        };
        Object.keys(tests).forEach(function (inputName) {
          it(testMessage.format(inputName, tests[inputName]), function() {
            expect(App.format.normalizeNameBySeparators(inputName, ["-", "_", " "])).to.eql(tests[inputName]);
          });
        });
      });

      describe('#normalizeName()', function() {
        var testMessage = '`{0}` should be converted to `{1}`';
        var tests = {
          'APP_TIMELINE_SERVER': 'App Timeline Server',
          'DATANODE': 'DataNode',
          'DECOMMISSION_DATANODE': 'Update Exclude File',
          'DRPC_SERVER': 'DRPC Server',
          'FALCON': 'Falcon',
          'FALCON_CLIENT': 'Falcon Client',
          'FALCON_SERVER': 'Falcon Server',
          'FALCON_SERVICE_CHECK': 'Falcon Service Check',
          'FLUME_HANDLER': 'Flume',
          'FLUME_SERVICE_CHECK': 'Flume Service Check',
          'GANGLIA_MONITOR': 'Ganglia Monitor',
          'GANGLIA_SERVER': 'Ganglia Server',
          'GLUSTERFS_CLIENT': 'GLUSTERFS Client',
          'GLUSTERFS_SERVICE_CHECK': 'GLUSTERFS Service Check',
          'GMETAD_SERVICE_CHECK': 'Gmetad Service Check',
          'GMOND_SERVICE_CHECK': 'Gmond Service Check',
          'HADOOP_CLIENT': 'Hadoop Client',
          'HBASE_CLIENT': 'HBase Client',
          'HBASE_MASTER': 'HBase Master',
          'HBASE_REGIONSERVER': 'RegionServer',
          'HBASE_SERVICE_CHECK': 'HBase Service Check',
          'HCAT': 'HCat Client',
          'HDFS': 'HDFS',
          'HDFS_CLIENT': 'HDFS Client',
          'HDFS_SERVICE_CHECK': 'HDFS Service Check',
          'HISTORYSERVER': 'History Server',
          'HIVE_CLIENT': 'Hive Client',
          'HIVE_METASTORE': 'Hive Metastore',
          'HIVE_SERVER': 'HiveServer2',
          'HIVE_SERVICE_CHECK': 'Hive Service Check',
          'HUE_SERVER': 'Hue Server',
          'JAVA_JCE': 'Java JCE',
          'JOBTRACKER': 'JobTracker',
          'JOBTRACKER_SERVICE_CHECK': 'JobTracker Service Check',
          'JOURNALNODE': 'JournalNode',
          'KERBEROS_ADMIN_CLIENT': 'Kerberos Admin Client',
          'KERBEROS_CLIENT': 'Kerberos Client',
          'KERBEROS_SERVER': 'Kerberos Server',
          'MAPREDUCE2_CLIENT': 'MapReduce2 Client',
          'MAPREDUCE2_SERVICE_CHECK': 'MapReduce2 Service Check',
          'MYSQL_SERVER': 'MySQL Server',
          'NAMENODE': 'NameNode',
          'NAMENODE_SERVICE_CHECK': 'NameNode Service Check',
          'NIMBUS': 'Nimbus',
          'NODEMANAGER': 'NodeManager',
          'OOZIE_CLIENT': 'Oozie Client',
          'OOZIE_SERVER': 'Oozie Server',
          'OOZIE_SERVICE_CHECK': 'Oozie Service Check',
          'PIG': 'Pig',
          'PIG_SERVICE_CHECK': 'Pig Service Check',
          'RESOURCEMANAGER': 'ResourceManager',
          'SECONDARY_NAMENODE': 'SNameNode',
          'SQOOP': 'Sqoop',
          'SQOOP_SERVICE_CHECK': 'Sqoop Service Check',
          'STORM_REST_API': 'Storm REST API Server',
          'STORM_SERVICE_CHECK': 'Storm Service Check',
          'STORM_UI_SERVER': 'Storm UI Server',
          'SUPERVISOR': 'Supervisor',
          'TASKTRACKER': 'TaskTracker',
          'TEZ_CLIENT': 'Tez Client',
          'WEBHCAT_SERVER': 'WebHCat Server',
          'YARN_CLIENT': 'YARN Client',
          'YARN_SERVICE_CHECK': 'YARN Service Check',
          'ZKFC': 'ZKFailoverController',
          'ZOOKEEPER_CLIENT': 'ZooKeeper Client',
          'ZOOKEEPER_QUORUM_SERVICE_CHECK': 'ZK Quorum Service Check',
          'ZOOKEEPER_SERVER': 'ZooKeeper Server',
          'ZOOKEEPER_SERVICE_CHECK': 'ZooKeeper Service Check',
          'CLIENT': 'Client'
        };
        Object.keys(tests).forEach(function (inputName) {
          it(testMessage.format(inputName, tests[inputName]), function() {
            expect(App.format.normalizeName(inputName)).to.eql(tests[inputName]);
          });
        });
      });
      describe('#kdcErrorMsg()', function() {
        var tests = [
          {
            r: "1 Missing KDC administrator credentials. and some text",
            f: "Missing KDC administrator credentials."
          },
          {
            r: "2 Invalid KDC administrator credentials. and some text",
            f: "Invalid KDC administrator credentials."
          },
          {
            r: "3 Failed to find a KDC for the specified realm - kadmin and some text",
            f: "Failed to find a KDC for the specified realm - kadmin"
          },
          {
            r: "4 some text",
            f: null,
            s: true
          },
          {
            r: "4 some text",
            f: "4 some text",
            s: false
          }
        ];

        tests.forEach(function(t) {
          it("kdcErrorMsg for " + t.f + " with strict " + t.s, function() {
            expect(App.format.kdcErrorMsg(t.r, t.s)).to.be.equal(t.f);
          })
        });

      });

      describe("#role()", function() {
        before(function () {
          App.format.stackServiceRolesMap = {};
          App.format.stackComponentRolesMap = {};
        });
        beforeEach(function () {
          sinon.stub(App.StackService, 'find').returns([Em.Object.create({
            id: 'S1',
            displayName: 's1'
          })]);
          sinon.stub(App.StackServiceComponent, 'find').returns([Em.Object.create({
            id: 'C1',
            displayName: 'c1'
          })]);
        });
        afterEach(function () {
          App.StackService.find.restore();
          App.StackServiceComponent.find.restore();
        });
        it("S1 -> s1", function() {
          expect(App.format.role('S1', true)).to.equal('s1');
        });
        it("C1 -> c1", function() {
          expect(App.format.role('C1', false)).to.equal('c1');
        });
        it("stackRolesMap is not empty", function() {
          expect(App.format.stackServiceRolesMap).to.not.be.empty;
          expect(App.format.stackComponentRolesMap).to.not.be.empty;
        });
      });
    });
  });

  describe('#App.permit()', function() {
    var obj = {
      a1: 'v1',
      a2: 'v2',
      a3: 'v3'
    };

    var tests = [
      {
        keys: 'a1',
        e: {
          a1: 'v1'
        }
      },
      {
        keys: ['a2','a3','a4'],
        e: {
          a2: 'v2',
          a3: 'v3'
        }
      }
    ];

    tests.forEach(function(test) {
      it('should return object `{0}` permitted keys `{1}`'.format(JSON.stringify(test.e), JSON.stringify(test.keys)), function() {
        expect(App.permit(obj, test.keys)).to.deep.eql(test.e);
      });
    });
  });

  describe('#App.keysUnderscoreToCamelCase()', function() {
    var tests = [
      {
        object: {
          'key_upper': '2'
        },
        expected: {
          keyUpper: '2'
        },
        m: 'One level object, key should be camelCased'
      },
      {
        object: {
          'key_upper': '2',
          'key': '1'
        },
        expected: {
          keyUpper: '2',
          key: '1'
        },
        m: 'One level object, one key should be camelCased.'
      },
      {
        object: {
          'key_upper': '2',
          'key': '1'
        },
        expected: {
          keyUpper: '2',
          key: '1'
        },
        m: 'One level object, one key should be camelCased.'
      },
      {
        object: {
          'key_upper': '2',
          'key_upone_uptwo_upthree': '4',
          'key': '1'
        },
        expected: {
          keyUpper: '2',
          keyUponeUptwoUpthree: '4',
          key: '1'
        },
        m: 'One level object, two keys should be camelCased, few dots notation.'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        expect(App.keysUnderscoreToCamelCase(test.object)).to.deep.equal(test.expected);
      });
    });
  });

  describe('#App.keysDottedToCamelCase()', function() {
    var tests = [
      {
        object: {
          'key.upper': '2'
        },
        expected: {
          keyUpper: '2'
        },
        m: 'One level object, key should be camelCased'
      },
      {
        object: {
          'key.upper': '2',
          'key': '1'
        },
        expected: {
          keyUpper: '2',
          key: '1'
        },
        m: 'One level object, one key should be camelCased.'
      },
      {
        object: {
          'key.upper': '2',
          'key': '1'
        },
        expected: {
          keyUpper: '2',
          key: '1'
        },
        m: 'One level object, one key should be camelCased.'
      },
      {
        object: {
          'key.upper': '2',
          'key.upone.uptwo.upthree': '4',
          'key': '1'
        },
        expected: {
          keyUpper: '2',
          keyUponeUptwoUpthree: '4',
          key: '1'
        },
        m: 'One level object, two keys should be camelCased, few dots notation.'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        expect(App.keysDottedToCamelCase(test.object)).to.deep.equal(test.expected);
      });
    });
  });

  describe('#App.formatDateTimeWithTimeZone()', function () {

    beforeEach(function () {
      sinon.stub(App.router, 'get').withArgs('userSettingsController.userSettings.timezone').returns({
        zones: [{
          value: 'Europe/Amsterdam'
        }]
      });
    });

    afterEach(function () {
      App.router.get.restore();
    });

    it('should format date according to customized timezone', function () {
      expect(App.formatDateTimeWithTimeZone(1000000, 'YYYY-MM-DD HH:mm:ss (hh:mm A)')).to.equal('1970-01-01 01:16:40 (01:16 AM)');
    });

  });

});
