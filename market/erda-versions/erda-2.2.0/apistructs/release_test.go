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

package apistructs

import (
	"testing"

	"gopkg.in/stretchr/testify.v1/assert"
)

func Test_ConvertToQueryParams(t *testing.T) {
	req := ReleaseListRequest{
		ReleaseName: "fake-release",
		Cluster:     "fake-cluster",
	}

	m := req.ConvertToQueryParams()
	assert.Equal(t, m["cluster"], []string{"fake-cluster"})
	assert.Equal(t, m["releaseName"], []string{"fake-release"})
}

const testDiceyml = `
addons: {}
envs: {}
jobs: {}
services:
  doc:
    deployments:
      replicas: 1
    expose:
    - 80
    health_check:
      http:
        duration: 300
        path: /
        port: 80
    image: registry.cn-hangzhou.aliyuncs.com/dspo/docs:i20211229-0001
    ports:
    - 80
    resources:
      cpu: 0.1
      mem: 128
  web:
    deployments:
      replicas: 1
    image: registry.cn-hangzhou.aliyuncs.com/dspo/web:latest
    ports:
    - 80
version: "2.0"
`

const testDiceyml2 = `
environments:
  development:
    addons:
      api-gateway:
        plan: api-gateway:basic
      elasticsearch:
        plan: terminus-elasticsearch:basic
      member-mysql-dev:
        options:
          version: 1.0.0
        plan: alicloud-rds:basic
      ons-dev:
        options:
          version: 1.0.0
        plan: alicloud-ons:basic
      redis-dice-dev:
        plan: redis:basic
      registercenter:
        plan: registercenter:basic
      rocketmq:
        options:
          version: 4.3.0
        plan: rocketmq:basic
    envs:
      JAVA_OPTS: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5055
      SPRING_PROFILES_ACTIVE: dev
  production:
    addons:
      api-gateway:
        plan: api-gateway:basic
      registercenter:
        plan: registercenter:basic
      yunuo-es-prod:
        options:
          version: 1.0.0
        plan: custom:basic
      yunuo-mysql-prod2:
        options:
          version: 1.0.0
        plan: alicloud-rds:basic
      yunuo-redis-prod2:
        plan: alicloud-redis:basic
      yunuo-rocketmq-prod2:
        plan: custom:basic
    envs:
      SPRING_PROFILES_ACTIVE: prod
  staging:
    addons:
      api-gateway:
        plan: api-gateway:basic
      es-uat:
        plan: custom:basic
      log-service-uat:
        options:
          version: 1.0.0
        plan: log-service:basic
      redis-uat:
        plan: alicloud-redis:basic
      registercenter:
        plan: registercenter:basic
      yunuo-mysql-uat:
        options:
          version: 1.0.0
        plan: alicloud-rds:basic
      yunuo-rocketmq-uat:
        plan: custom:basic
    envs:
      JAVA_OPTS: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5055
        -Xms8192m -Xmx8192m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
        -XX:+PrintGCDateStamps  -XX:+PrintGCDetails -XX:NewRatio=2 -XX:+UseParallelGC
        -XX:+UseParallelOldGC
      SPRING_PROFILES_ACTIVE: uat
  test:
    addons:
      api-gateway:
        plan: api-gateway:basic
      elasticsearch:
        plan: terminus-elasticsearch:basic
      member-mysql-test:
        options:
          version: 1.0.0
        plan: alicloud-rds:basic
      redis-test:
        plan: alicloud-redis:basic
      registercenter:
        plan: registercenter:basic
      rocketmq:
        options:
          version: 4.3.0
        plan: rocketmq:basic
    envs:
      JAVA_OPTS: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5055
      SPRING_PROFILES_ACTIVE: test
envs: {}
jobs: {}
services:
  rm-demo-webapp:
    cmd: java ${JAVA_OPTS} -javaagent:/spot-agent/spot-agent.jar -jar /target/rpc.jar
    deployments:
      replicas: ${replicas:1}
    health_check:
      http:
        duration: 10
        path: /actuator/health
        port: 8083
    image: addon-registry.default.svc.cluster.local:5000/yishu-ec/romens-data-process:rm-demo-webapp-1640919775372511996
    ports:
    - expose: true
      port: 8083
    resources:
      cpu: ${webcpu:1}
      mem: ${webmem:2048}
values:
  development:
    replicas: 1
    webcpu: 0.5
    webmem: 1024
  production:
    replicas: 2
    webcpu: 2
    webmem: 4096
  staging:
    replicas: 1
    webcpu: 4
    webmem: 10000
  test:
    replicas: 1
    webcpu: 1
    webmem: 1024
version: "2.0"
`

func TestReleaseGetResponseData_ReLoadImages(t *testing.T) {
	var data ReleaseGetResponseData
	data.Modes = map[string]ReleaseDeployModeSummary{
		"default": {
			ApplicationReleaseList: [][]*ApplicationReleaseSummary{
				{
					{
						DiceYml: testDiceyml,
					},
				},
			},
		},
	}
	if err := data.ReLoadImages(); err != nil {
		t.Fatal(err)
	}
	assertServices(t, data.Modes["default"].ApplicationReleaseList[0][0].Services)

	data.Diceyml = testDiceyml
	if err := data.ReLoadImages(); err != nil {
		t.Fatal(err)
	}
	assertServices(t, data.ServiceImages)
}

func TestReleaseGetResponseData_ReLoadImages2(t *testing.T) {
	var data ReleaseGetResponseData
	data.Diceyml = testDiceyml2
	if err := data.ReLoadImages(); err != nil {
		t.Fatal(err)
	}
}

func assertServices(t *testing.T, services []*ServiceImagePair) {
	if len(services) != 2 {
		t.Fatal("services count error")
	}
	for _, service := range services {
		switch {
		case service.ServiceName == "doc" && service.Image == "registry.cn-hangzhou.aliyuncs.com/dspo/docs:i20211229-0001":
		case service.ServiceName == "web" && service.Image == "registry.cn-hangzhou.aliyuncs.com/dspo/web:latest":
		default:
			t.Fatalf("service name or image parse error, serviceName: %s, image: %s",
				service.ServiceName, service.Image)
		}
	}
}
