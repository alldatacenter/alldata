// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package surefilexml

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

//import (
//	"encoding/json"
//	"testing"
//
//	"github.com/sirupsen/logrus"
//	"github.com/stretchr/testify/assert"
//)
//
//func TestParserSuite(t *testing.T) {
//	p := DefaultParser{}
//	ts, err := p.Parse("127.0.0.1:9009", "accesskey", "secretkey", "test1", "d1.xml")
//	assert.Nil(t, err)
//	logrus.Info(ts)
//}
//
//func TestParserSuites(t *testing.T) {
//	p := DefaultParser{}
//	ts, err := p.Parse("127.0.0.1:9009", "accesskey", "secretkey", "test1", "TEST-TestSuite.xml")
//	assert.Nil(t, err)
//
//	js, err := json.Marshal(ts)
//	assert.Nil(t, err)
//	logrus.Info(string(js))
//}

func TestIngest(t *testing.T) {
	demo := `
<?xml version="1.0" encoding="UTF-8"?>
<testsuite xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd" name="io.terminus.demo.apmdemo.ApmDemoApplicationTests" time="7.124" tests="1" errors="1" skipped="0" failures="0">
  <properties>
    <property name="java.class.version" value="61.0"/>
    <property name="maven.test.failure.ignore" value="true"/>
  </properties>
  <testcase name="contextLoads" classname="io.terminus.demo.apmdemo.ApmDemoApplicationTests" time="0.002">
    <error message="Failed to load ApplicationContext" type="java.lang.IllegalStateException"><![CDATA[java.lang.IllegalStateException: Failed to load ApplicationContext
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'dubboApiController': Injection of @DubboReference dependencies is failed; nested exception is java.lang.IllegalStateException: Failed to check the status of the service io.terminus.demo.rpc.DubboService. No provider available for the service io.terminus.demo.rpc.DubboService from the url nacos://127.0.0.1:8848/org.apache.dubbo.registry.RegistryService?application=apm-demo-api&dubbo=2.0.2&init=false&interface=io.terminus.demo.rpc.DubboService&metadata-type=remote&methods=mysqlUsers,redisGet,httpRequest,hello,error&pid=24884&qos.enable=false&register.ip=30.43.49.72&release=2.7.8&side=consumer&sticky=false&timestamp=1652174418412 to the consumer 30.43.49.72 use dubbo version 2.7.8
Caused by: java.lang.IllegalStateException: Failed to check the status of the service io.terminus.demo.rpc.DubboService. No provider available for the service io.terminus.demo.rpc.DubboService from the url nacos://127.0.0.1:8848/org.apache.dubbo.registry.RegistryService?application=apm-demo-api&dubbo=2.0.2&init=false&interface=io.terminus.demo.rpc.DubboService&metadata-type=remote&methods=mysqlUsers,redisGet,httpRequest,hello,error&pid=24884&qos.enable=false&register.ip=30.43.49.72&release=2.7.8&side=consumer&sticky=false&timestamp=1652174418412 to the consumer 30.43.49.72 use dubbo version 2.7.8
]]></error>
  </testcase>
</testsuite>
`
	suites, err := Ingest([]byte(demo))
	assert.NoError(t, err)
	assert.Equal(t, 1, len(suites))
}
