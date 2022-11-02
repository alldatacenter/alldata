/*
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
import { moduleFor, test } from 'ember-qunit';

moduleFor('service:property-extractor', 'Unit | Service | property extractor', {
  // Specify the other units that are required for this test.
  // needs: ['service:foo']
});

// Replace this with your real tests.
test('should match simple job property', function(assert) {
  let service = this.subject();
  assert.ok(service);
  assert.equal(service.get('simpleProperty').test("MainClass"), true);
  assert.equal(service.get('simpleProperty').test("Main_Class"), true);
  assert.equal(service.get('simpleProperty').test("MainClass/"), false);
  assert.equal(service.get('simpleProperty').test("12MainClass"), false);
});

test('should match dynamic job property', function(assert) {
  let service = this.subject();
  assert.equal(service.get('dynamicProperty').test("${MainClass}"), true);
  assert.equal(service.get('dynamicProperty').test("${Main_Class}"), true);
  assert.equal(service.get('dynamicProperty').test("${MainClass/}"), false);
  assert.equal(service.get('dynamicProperty').test("${Main_Class${}}"), false);
  assert.equal(service.get('dynamicProperty').test("${1MainClass}"), false);
});

test('should match dynamic job property with EL method', function(assert) {
  let service = this.subject();
  assert.equal(service.get('dynamicPropertyWithElMethod').test("${MainClass()}"), true);
  assert.equal(service.get('dynamicPropertyWithElMethod').test("${MainClass(test)}"), true);
  assert.equal(service.get('dynamicPropertyWithElMethod').test("${testMethod(test,test)}"), true);
  assert.equal(service.get('dynamicPropertyWithElMethod').test("${testMethod(test,)}"), false);
  assert.equal(service.get('dynamicPropertyWithElMethod').test("${MainClass/}"), false);
  assert.equal(service.get('dynamicPropertyWithElMethod').test("${1MainClass}"), false);
});

test('should match dynamic job property with WF method', function(assert) {
  let service = this.subject();
  assert.equal(service.get('dynamicPropertyWithWfMethod').test("${wf:MainClass()}"), true);
  assert.equal(service.get('dynamicPropertyWithWfMethod').test("${wf:MainClass(test)}"), true);
  assert.equal(service.get('dynamicPropertyWithWfMethod').test("${WF:testMethod(test,test)}"), true);
  assert.equal(service.get('dynamicPropertyWithWfMethod').test("${wf:testMethod(test,)}"), false);
  assert.equal(service.get('dynamicPropertyWithWfMethod').test("${MainClass/}"), false);
  assert.equal(service.get('dynamicPropertyWithWfMethod').test("${1MainClass}"), false);
});

test('should match job property with hadoop EL', function(assert) {
  let service = this.subject();
  assert.equal(service.get('hadoopEL').test("${hadoop:MainClass()}"), true);
  assert.equal(service.get('hadoopEL').test('${hadoop:counters("mr-node")["FileSystemCounters"]["FILE_BYTES_READ"]}'), true);
  assert.equal(service.get('hadoopEL').test("${hadoop:MAP_IN}"), true);
  assert.equal(service.get('hadoopEL').test("${test}"), false);
  assert.equal(service.get('hadoopEL').test("${wf:MainClass()}"), false);
  assert.equal(service.get('hadoopEL').test("${test()}"), false);
});

test('should match dynamic job properties',function(assert){
  let service = this.subject();
  var property = "${nameNode}/user/${wf:user()}/${examplesRoot}/output-data/${outputDir}";
  var matches = property.match(service.get('extractor'));
  assert.equal(matches.length, 4);
  assert.equal(matches[0], "${nameNode}");
});

test('should extract dynamic job properties',function(assert){
  let service = this.subject();
  var property = "${nameNode}/user/${wf:user()}/${examplesRoot}/output-data/${outputDir}";
  var matches = service.extract(property);
  assert.equal(matches.length, 3);
  assert.equal(matches[0], "${nameNode}");
  assert.equal(matches[1], "${examplesRoot}");
  assert.equal(matches[2], "${outputDir}");
  property = "${nameNode}/user/${concat(name,value)}/${examplesRoot}/output-data/${outputDir}";
  matches = service.extract(property);
  assert.equal(matches.length, 3);
  assert.equal(matches[0], "${nameNode}");
  assert.equal(matches[1], "${examplesRoot}");
  assert.equal(matches[2], "${outputDir}");
  property = "${nameNode}/user/${(name,value)}/${examplesRoot}/output-data/${outputDir}";
  matches = service.extract(property);
  assert.equal(matches.length, 3);
  assert.equal(matches[0], "${nameNode}");
  assert.equal(matches[1], "${examplesRoot}");
  assert.equal(matches[2], "${outputDir}");
  property = "${nameNode}/user/${hadoop:MAP_IN}/${examplesRoot}/output-data/${outputDir}";
  matches = service.extract(property);
  assert.equal(matches.length, 3);
  assert.equal(matches[0], "${nameNode}");
  assert.equal(matches[1], "${examplesRoot}");
  assert.equal(matches[2], "${outputDir}");
  property = "test";
  matches = service.extract(property);
  assert.equal(matches.length, 0);
  property = "$test";
  matches = service.extract(property);
  assert.equal(matches.length, 0);
  property = "test()";
  matches = service.extract(property);
  assert.equal(matches.length, 0);
  property = "wf:test()";
  matches = service.extract(property);
  assert.equal(matches.length, 0);
});

test('should extract dynamic job properties from xml',function(assert){
  let service = this.subject();
  var xml = "<workflow-app xmlns='uri:oozie:workflow:0.2'><start to='java'/><action name='java'><main-class>dfkfjlsaf</main-class><configuration><property><name>key2</name><value>val2</value></property><property><name>key1</name><value>val1</value></property></configuration><ok to='End'/></action><end name='End'/></workflow-app>";
  var matches = service.getDynamicProperties(xml);
  assert.equal(matches.size, 0);
});
