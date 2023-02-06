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

package diceyml

import (
	"encoding/json"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

const testyml = `environments:
  development:
    addons:
      mysql:
        as: MYSQL
        options:
          create_dbs: mall_eevee
          version: 5.7.29
        plan: mysql:basic
      oss:
        options:
          version: 1.0.0
        plan: alicloud-oss:basic
    envs:
      OSS_ENABLE: true
envs:
  CSRF_ENABLE: true
  ENABLE_SSR: false
  OSS_ENABLE: true
  SSR_FALLBACK: true
  TERMINUS_KEY: ((TERMINUS_KEY))
  TERMINUS_TA_COLLECTOR_URL: ((TERMINUS_TA_COLLECTOR_URL))
  TERMINUS_TA_ENABLE: ((TERMINUS_TA_ENABLE))
  TERMINUS_TA_URL: ((TERMINUS_TA_URL))
jobs: {}
services:
  gaia-mall:
    depends_on:
    - herd
    deployments:
      replicas: 1
    expose:
    - 80
    health_check:
      http:
        duration: 120
        path: /health/check
        port: 80
    image: addon-registry.default.svc.cluster.local:5000/xxx
    ports:
    - 80
    resources:
      cpu: 0.2
      disk: 4096
      mem: 512
  herd:
    deployments:
      replicas: 1
    health_check:
      http:
        duration: 120
        path: /health/check
        port: 8081
    image: addon-registry.default.svc.cluster.local:5000/aaaa
    ports:
    - 8081
    resources:
      cpu: 0.2
      disk: 10
      mem: 512
version: 2`

const testjson = `{"version":"2.0","meta":{},"services":{"datastore":{"image":"registry.cn-hangzhou.aliyuncs.com/xxxx","image_username":"","image_password":"","cmd":"","ports":[{"port":8080,"protocol":"TCP","l4_protocol":"TCP","expose":true,"default":false}],"envs":{"JAVA_OPTS":"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -server -XX:NewRatio=1 -Xms3072m -Xmx3072m"},"resources":{"cpu":3,"mem":3072,"max_cpu":0,"max_mem":0,"disk":0,"network":{"mode":"container"}},"deployments":{"replicas":1,"policies":""},"expose":[8080],"health_check":{"http":{},"exec":{"cmd":"curl -k http://127.0.0.1:8080/api/data/health"}},"traffic_security":{}},"datastore-search":{"image":"registry.cn-hangzhou.aliyuncs.com/xxxx","image_username":"","image_password":"","cmd":"","ports":[{"port":8080,"protocol":"TCP","l4_protocol":"TCP","expose":true,"default":false}],"envs":{"JAVA_OPTS":"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -server -XX:NewRatio=1 -Xms512m -Xmx512m"},"resources":{"cpu":0.5,"mem":1024,"max_cpu":0,"max_mem":0,"disk":0,"network":{"mode":"container"}},"deployments":{"replicas":1,"policies":""},"expose":[8080],"health_check":{"http":{"port":8080,"path":"/api/web-tool/search-model/health","duration":600},"exec":{}},"traffic_security":{}},"meta-store":{"image":"registry.cn-hangzhou.aliyuncs.com/xxxx","image_username":"","image_password":"","cmd":"","ports":[{"port":8080,"protocol":"TCP","l4_protocol":"TCP","expose":true,"default":false}],"envs":{"JAVA_OPTS":"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -server -Xmx3072m -Xms3072m -Xmn1024m -Xss512k -XX:NewRatio=1 -XX:+PrintGCDetails -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution -XX:+PrintHeapAtGC -XX:+UseContainerSupport"},"resources":{"cpu":4,"mem":4096,"max_cpu":0,"max_mem":0,"disk":0,"network":{"mode":"container"}},"deployments":{"replicas":1,"policies":"","selectors":{"location":{"not":false,"values":["datastore"]}}},"expose":[8080],"health_check":{"http":{"port":8080,"path":"/actuator/health","duration":900},"exec":{}},"traffic_security":{}},"trantor-console":{"image":"registry.cn-hangzhou.aliyuncs.com/xxxx","image_username":"","image_password":"","cmd":"","ports":[{"port":8099,"protocol":"TCP","l4_protocol":"TCP","expose":true,"default":false}],"resources":{"cpu":0.25,"mem":256,"max_cpu":0,"max_mem":0,"disk":0,"network":{"mode":"container"}},"deployments":{"replicas":1,"policies":""},"expose":[8099],"health_check":{"http":{"port":80,"path":"/","duration":120},"exec":{}},"traffic_security":{}}},"addons":{"api-gateway":{"plan":"api-gateway:basic"},"elasticsearch":{"plan":"terminus-elasticsearch:basic","options":{"version":"5.6.9"}},"registercenter":{"plan":"registercenter:basic"},"rocketmq":{"plan":"rocketmq:basic","options":{"version":"4.2.0"}},"trantor-gaia-master":{"plan":"redis:basic","options":{"version":"3.2.12"}},"trantor-gaia-mysql":{"plan":"mysql:basic","options":{"version":"5.7.23"}}}}`

const jobyml = `version: 2.0
jobs:
  job1:
    cmd: ls
    envs:
      env1: v1
  job2:
    cmd: ls -l
    envs:
      env2: v2
`

const yml = `version: 2.0

version: 2
envs:
  TERMINUS_APP_NAME: "TEST-global"
  TEST_PARAM: "param_value"
services:
  web:
    ports:
      - 8080
      - port: 20880
      - port: 1234
        protocol: "UDP"
      - port: 4321
        protocol: "HTTP"
      - port: 53
        protocol: "DNS"
        l4_protocol: "UDP"
        default: true
    k8s_snippet:
      container:
        stdin: true
        workingDir: aaa
        imagePullPolicy: Always
        securityContext:
          privileged: true
    health_check:
      exec:
        cmd: "echo 1"
    deployments:
      replicas: ${replicas}
    resources:
      cpu: ${cpu:0.1}
      mem: 512
      disk: 0
    expose:
      - 20880
    volumes:
      - storage: "nfs"
        path: "/data/file/resource"
addons:
  terminus-elasticsearch:
    plan: "terminus-elasticsearch:professional"
    options:
      version: "6.8.9"
  apigateway:
    plan: "api-gateway:basic"
    options:
      version: "2.0.0"
  xxx:
    plan: ${mysql-plan:"mysql:basic"}
    options:
      version: "5.7.23"
values:
  test:
    replicas: 1
    cpu: 0.5
  production:
    replicas: 2
    cpu: 1
    mysql-plan: "rds:basic"
  
`

const yml3 = `version: '2.0'
services:
  go-demo:
    ports:
      - port: 5000
        expose: true
    volumes:
      - /data/certs
    resources:
      cpu: 0.5
      mem: 500
    deployments:
      replicas: 1
      selectors:
        location: go-demo
addons:
  fdf:
    plan: mysql:basic
    options:
      version: 5.7.29
envs:
  TERMINUS_TRACE_ENABLE: false`

const yml4 = `version: "2.0"
services:
  nginx:
    image: registry.cn-hangzhou.aliyuncs.com/dice/dop:1.16-jacoco-server
    resources:
      cpu: 5
      mem: 4000
    volumes:
        - storage: nfs
          path: /app/cover
    deployments:
      replicas: 1
    ports:
      - port: 8801
        expose: true`

const yml5 = `addons:
  infos:
    options:
      version: 1.0.0
    plan: custom:basic
  mysql:
    options:
      version: 5.7.29
    plan: mysql:basic
environments:
  test:
    addons:
      infos:
        options:
          version: 1.0.0
        plan: custom:basic
      log-analysis:
        options:
          version: 2.0.0
        plan: log-analytics:basic
      mysql:
        options:
          version: 5.7.29
        plan: mysql:basic
envs:
  ERDA_SERVER_ADDR: erda-server:9095 
  CMP_ADDR: cmp:9027
  COLLECTOR_ADDR: collector:7076
  CORE_SERVICES_ADDR: core-services:9526
  DOP_ADDR: dop:9527
  ETCDCTL_API: "3"
  MONITOR_ADDR: monitor:7096
  MSP_ADDR: msp:8080
  OPENAPI_ADDR: openapi:9529
  PIPELINE_ADDR: pipeline:3081
jobs: {}
meta:
  ERDA_COMPONENT: ""
services:
  action-runner-scheduler:
    binds:
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    cmd: /app/action-runner-scheduler
    deployments:
      labels:
        GROUP: devops
      replicas: 1
    envs: {}
    expose: []
    health_check:
      exec: {}
      http:
        duration: 120
        path: /healthz
        port: 9500
    hosts: []
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 9500
      protocol: TCP
    resources:
      cpu: ${request_cpu:0.3}
      max_cpu: 0.3
      mem: 128
      network:
        mode: container
  admin:
    binds:
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    cmd: /app/admin
    deployments:
      labels:
        GROUP: dice
      replicas: 1
    envs:
      DEBUG: "false"
    health_check:
      exec: {}
      http:
        duration: 120
        path: /api/healthy
        port: 9095
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 9095
      protocol: TCP
    - l4_protocol: TCP
      port: 8096
      protocol: TCP
    resources:
      cpu: ${request_cpu:0.2}
      max_cpu: 0.2
      max_mem: 256
      mem: ${request_mem:128}
      network:
        mode: container
  cluster-agent:
    cmd: /app/cluster-agent
    deployments:
      labels:
        GROUP: dice
      replicas: 1
    envs:
      DEBUG: "false"
    health_check:
      exec:
        cmd: ls
      http: {}
    image: registry.erda.cloud/erda/cluster-agent:1.4.0-alpha-20211008033806-158b666
    ports: []
    resources:
      cpu: ${request_cpu:1}
      max_cpu: 1
      max_mem: 1024
      mem: ${request_mem:1024}
      network:
        mode: container
  cluster-manager:
    cmd: /app/cluster-manager
    deployments:
      labels:
        GROUP: dice
      replicas: ${replicas:1}
    envs:
      DEBUG: "false"
    health_check:
      exec: {}
      http: {}
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 9094
      protocol: TCP
    resources:
      cpu: ${request_cpu:0.2}
      max_cpu: 2
      max_mem: 2048
      mem: ${request_mem:256}
      network:
        mode: container
  cmp:
    binds:
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    cmd: /app/cmp
    deployments:
      labels:
        GROUP: dice
      replicas: ${replicas:1}
    envs:
      ERDA_HELM_CHART_VERSION: 1.1.0
      ERDA_NAMESPACE: default
      UC_CLIENT_ID: dice
      UC_CLIENT_SECRET: secret
    health_check:
      exec: {}
      http: {}
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 9027
      protocol: TCP
    - l4_protocol: TCP
      port: 9028
      protocol: TCP
    resources:
      cpu: ${request_cpu:2}
      max_cpu: 2
      max_mem: 6144
      mem: ${request_mem:6144}
      network:
        mode: container
  collector:
    binds: []
    cmd: /app/collector
    deployments:
      labels:
        GROUP: spot-v2
      replicas: ${replicas:2}
    envs:
      COLLECTOR_BROWSER_SAMPLING_RATE: "100"
      COLLECTOR_ENABLE: "true"
    expose:
    - 7076
    health_check:
      exec: {}
      http:
        duration: 120
        path: /api/health
        port: 7076
    hosts: []
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    labels:
      cpu_bound: "true"
    ports:
    - expose: true
      l4_protocol: TCP
      port: 7076
      protocol: TCP
    - l4_protocol: TCP
      port: 7098
      protocol: TCP
    resources:
      cpu: ${request_cpu:1}
      max_cpu: 1
      max_mem: 1024
      mem: ${request_mem:1024}
      network:
        mode: container
  core-services:
    binds:
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    - ${nfs_root:/netdata}/avatars:/avatars:rw
    - ${nfs_root:/netdata}/dice/cmdb/files:/files:rw
    cmd: /app/core-services
    deployments:
      labels:
        GROUP: dice
      replicas: 1
    envs:
      AVATAR_STORAGE_URL: file:///avatars
      CMDB_CONTAINER_TOPIC: spot-metaserver_container
      CMDB_GROUP: spot_cmdb_group
      CMDB_HOST_TOPIC: spot-metaserver_host
      CREATE_ORG_ENABLED: "false"
      DEBUG: "false"
      LICENSE_KEY: XWoPm8I3FZuDclhuOhZ+qRPVHjXKCwSgZEOTyrMgtJg6f0Kz7QR0CyVN1ZWgbiou/OyABe7HyK1yVxDdeP1JuXcfOoGOdChTyiQfP5sdXUbferq5UkK7S44lMjNmzURlbdX8smSa13+8FQyDqz2BpDcBKMRfn2kKuF4n6n9Ls7HyVV7oWSKreEyIH3991Ug2grNEpcKip3ISVY7eGJ3uoahC9zs4fla1dzR47e5dgppHtf5WBjFgiSS+5qRi2mYa
      LISTEN_ADDR: :9526
      UC_CLIENT_ID: dice
      UC_CLIENT_SECRET: secret
    health_check:
      exec: {}
      http:
        duration: 120
        path: /_api/health
        port: 9526
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 9526
      protocol: TCP
    - l4_protocol: TCP
      port: 9536
      protocol: TCP
    - l4_protocol: TCP
      port: 9537
      protocol: TCP
    resources:
      cpu: ${request_cpu:0.3}
      max_cpu: 0.3
      max_mem: 512
      mem: ${request_mem:512}
      network:
        mode: container
  dicehub:
    binds:
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    cmd: /app/dicehub
    deployments:
      labels:
        GROUP: dice
      replicas: ${replicas:1}
    envs:
      EXTENSION_MENU: '{"流水线任务":["source_code_management:代码管理","build_management:构建管理","deploy_management:部署管理","version_management:版本管理","test_management:测试管理","data_management:数据治理","custom_task:自定义任务"],"扩展服务":["database:存储","distributed_cooperation:分布式协作","search:搜索","message:消息","content_management:内容管理","security:安全","traffic_load:流量负载","monitoring&logging:监控&日志","content:文本处理","image_processing:图像处理","document_processing:文件处理","sound_processing:音频处理","custom:自定义","general_ability:通用能力","new_retail:新零售能力","srm:采供能力","solution:解决方案"]}'
      RELEASE_GC_SWITCH: "true"
      RELEASE_MAX_TIME_RESERVED: "72"
    health_check:
      exec: {}
      http:
        duration: 120
        path: /healthz
        port: 10000
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 10000
      protocol: TCP
    resources:
      cpu: ${request_cpu:0.15}
      max_cpu: 0.15
      max_mem: 1024
      mem: ${request_mem:1024}
      network:
        mode: container
  dop:
    binds:
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    cmd: /app/dop
    deployments:
      labels:
        GROUP: devops
      replicas: ${replicas:1}
    envs:
      DEBUG: "true"
      GOLANG_PROTOBUF_REGISTRATION_CONFLICT: ignore
    health_check:
      exec: {}
      http:
        duration: 120
        path: /_api/health
        port: 9527
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 9527
      protocol: TCP
    - l4_protocol: TCP
      port: 9529
      protocol: TCP
    resources:
      cpu: ${request_cpu:1}
      max_cpu: 1
      max_mem: 2048
      mem: ${request_mem:2048}
      network:
        mode: container
  ecp:
    cmd: /app/ecp
    deployments:
      labels:
        GROUP: dice
      replicas: 1
    health_check:
      exec: {}
      http: {}
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 9029
      protocol: TCP
    resources:
      cpu: ${request_cpu:0.2}
      max_cpu: 0.2
      mem: 128
      network:
        mode: container
  eventbox:
    binds:
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    cmd: /app/eventbox
    deployments:
      labels:
        GROUP: dice
      replicas: 1
    envs:
      DEBUG: "false"
    health_check:
      exec: {}
      http:
        duration: 120
        path: /api/dice/eventbox/version
        port: 9528
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 9528
      protocol: TCP
    resources:
      cpu: ${request_cpu:2}
      max_cpu: 2
      max_mem: 2560
      mem: ${request_mem:2560}
      network:
        mode: container
  gittar:
    binds:
    - ${gittar_root:/netdata/dice/gittar}:/repository:rw
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    cmd: /app/gittar
    deployments:
      labels:
        GROUP: devops
      replicas: ${replicas:1}
    envs:
      GITTAR_BRANCH_FILTER: master,develop,feature/*,support/*,release/*,hotfix/*
      GITTAR_PORT: "5566"
      UC_CLIENT_ID: dice
      UC_CLIENT_SECRET: secret
    expose:
    - 5566
    health_check:
      exec: {}
      http: {}
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - expose: true
      l4_protocol: TCP
      port: 5566
      protocol: TCP
    resources:
      cpu: ${request_cpu:1}
      max_cpu: 1
      max_mem: 1536
      mem: ${request_mem:1536}
      network:
        mode: container
  hepa:
    cmd: /app/hepa
    deployments:
      labels:
        GROUP: addons
      replicas: ${replicas:1}
    expose:
    - 8080
    health_check:
      exec: {}
      http:
        duration: 120
        path: /health
        port: 8080
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - expose: true
      l4_protocol: TCP
      port: 8080
      protocol: TCP
    resources:
      cpu: ${request_cpu:0.5}
      max_cpu: 0.5
      mem: 512
      network:
        mode: container
  log-service:
    cmd: /app/log-service
    deployments:
      labels:
        GROUP: spot-v2
      replicas: ${replicas:0}
    envs:
      LOG_KAFKA_TOPICS: spot-container-log
      LOG_METRICS_GROUP_ID: spot-log-metrics
      LOG_SERVICE_INSTANCE_ID: 30563290-f3a8-4f8f-b42b-cc5d3b8ac7c7
      LOG_TOPICS: spot-container-log
    health_check:
      exec: {}
      http:
        duration: 120
        path: /api/health
        port: 7099
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 7099
      protocol: TCP
    resources:
      cpu: ${request_cpu:1}
      max_cpu: 1.5
      max_mem: 1024
      mem: ${request_mem:1024}
      network:
        mode: container
  monitor:
    binds: []
    cmd: /app/monitor
    deployments:
      labels:
        GROUP: spot-v2
      replicas: ${replicas:2}
    envs:
      LOG_LEVEL: INFO
    expose: []
    health_check:
      exec: {}
      http:
        duration: 120
        path: /api/health
        port: 7096
    hosts: []
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 7096
      protocol: TCP
    - l4_protocol: TCP
      port: 7098
      protocol: TCP
    - l4_protocol: TCP
      port: 7080
      protocol: TCP
    resources:
      cpu: ${request_cpu:0.5}
      max_cpu: 1
      max_mem: 1024
      mem: ${request_mem:512}
      network:
        mode: container
  msp:
    binds:
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    cmd: /app/msp
    deployments:
      labels:
        GROUP: msp
      replicas: ${replicas:2}
    envs:
      GOLANG_PROTOBUF_REGISTRATION_CONFLICT: ignore
    expose: []
    health_check:
      exec: {}
      http:
        duration: 120
        path: /health
        port: 8080
    hosts: []
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 8080
      protocol: TCP
    - l4_protocol: TCP
      port: 7080
      protocol: TCP
    - l4_protocol: TCP
      port: 9080
      protocol: TCP
    resources:
      cpu: ${request_cpu:1}
      max_cpu: 1
      max_mem: 1024
      mem: ${request_mem:512}
      network:
        mode: container
  openapi:
    binds:
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    cmd: /app/openapi
    deployments:
      labels:
        GROUP: dice
      replicas: ${replicas:1}
    envs:
      CREATE_ORG_ENABLED: "false"
      GOLANG_PROTOBUF_REGISTRATION_CONFLICT: ignore
    expose:
    - 9529
    health_check:
      exec: {}
      http:
        duration: 120
        path: /health
        port: 9529
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - expose: true
      l4_protocol: TCP
      port: 9529
      protocol: TCP
    - l4_protocol: TCP
      port: 9432
      protocol: TCP
    - l4_protocol: TCP
      port: 9431
      protocol: TCP
    resources:
      cpu: ${request_cpu:0.5}
      max_cpu: 0.5
      max_mem: 512
      mem: ${request_mem:512}
      network:
        mode: container
  orchestrator:
    binds:
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    cmd: /app/orchestrator
    deployments:
      labels:
        GROUP: dice
      replicas: ${replicas:1}
    envs:
      DEBUG: "false"
      MSP_ADDR: msp:8080
      TENANT_GROUP_KEY: 58dcbf490ef3
    health_check:
      exec: {}
      http:
        duration: 120
        path: /info
        port: 8081
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 8081
      protocol: TCP
    resources:
      cpu: ${request_cpu:1}
      max_cpu: 1
      max_mem: 256
      mem: ${request_mem:256}
      network:
        mode: container
  pipeline:
    binds:
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    cmd: /app/pipeline
    deployments:
      labels:
        GROUP: devops
      replicas: ${replicas:1}
    envs:
      DEBUG: "false"
      PIPELINE_STORAGE_URL: file:///devops/storage
    health_check:
      exec: {}
      http:
        duration: 120
        path: /ping
        port: 3081
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - l4_protocol: TCP
      port: 3081
      protocol: TCP
    - l4_protocol: TCP
      port: 30810
      protocol: TCP
    resources:
      cpu: ${request_cpu:1}
      max_cpu: 1
      max_mem: 1536
      mem: ${request_mem:1536}
      network:
        mode: container
  streaming:
    binds:
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    cmd: /app/streaming
    deployments:
      labels:
        GROUP: spot-v2
      replicas: ${replicas:2}
    envs:
      BROWSER_ENABLE: "true"
      BROWSER_GROUP_ID: spot-monitor-browser
      LOG_GROUP_ID: spot-monitor-log
      LOG_LEVEL: INFO
      LOG_STORE_ENABLE: "true"
      LOG_TTL: 168h
      METRIC_ENABLE: "true"
      METRIC_GROUP_ID: spot-monitor-metrics
      METRIC_INDEX_TTL: 192h
      TRACE_ENABLE: "true"
      TRACE_GROUP_ID: spot-monitor-trace
      TRACE_TTL: 168h
    health_check:
      exec: {}
      http:
        duration: 120
        path: /api/health
        port: 7091
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    labels:
      cpu_bound: "true"
    ports:
    - l4_protocol: TCP
      port: 7091
      protocol: TCP
    - l4_protocol: TCP
      port: 7098
      protocol: TCP
    resources:
      cpu: ${request_cpu:0.5}
      max_cpu: 1.5
      max_mem: 1024
      mem: ${request_mem:1024}
      network:
        mode: container
  uc-adaptor:
    binds:
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-ca.pem:/certs/etcd-ca.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client.pem:/certs/etcd-client.pem:ro
    - ${nfs_root:/netdata}/dice-ops/dice-config/certificates/etcd-client-key.pem:/certs/etcd-client-key.pem:ro
    cmd: /app/uc-adaptor
    deployments:
      labels:
        GROUP: devops
      replicas: 1
    envs:
      DEBUG: "false"
      LISTEN_ADDR: :12580
      UC_AUDITOR_CRON: 0 */1 * * * ?
      UC_AUDITOR_PULL_SIZE: "30"
      UC_CLIENT_ID: dice
      UC_CLIENT_SECRET: secret
    expose:
    - 12580
    health_check:
      exec: {}
      http:
        duration: 120
        path: /healthy
        port: 12580
    image: registry.erda.cloud/erda/erda:1.4.0-alpha-20211008033801-158b666
    ports:
    - expose: true
      l4_protocol: TCP
      port: 12580
      protocol: TCP
    resources:
      cpu: ${request_cpu:0.2}
      max_cpu: 0.2
      mem: 64
      network:
        mode: container
values:
  production:
    gittar_root: <%$.Storage.GittarDataPath%>
    nfs_root: <%$.Storage.MountPoint%>
    replicas: 2
    request_cpu: 0.1
    request_mem: 128
version: "2.0"`

const wrongSnippetYml = `version: 2.0
services:
  web:
    ports:
      - 8080
      - port: 20880
      - port: 1234
        protocol: "UDP"
      - port: 4321
        protocol: "HTTP"
      - port: 53
        protocol: "DNS"
        l4_protocol: "UDP"
        default: true
    deployments:
      replicas: 1
    resources:
      cpu: 0.1
      mem: 512
    k8s_snippet:
      container:
        name: abc
        stdin: true
        workingDir: aaa
        imagePullPolicy: Always
        securityContext:
          privileged: true
`

func TestDiceYmlObj(t *testing.T) {
	d, err := New([]byte(yml), true)
	assert.Nil(t, err)
	obj := d.Obj()
	assert.Equal(t, "TCP", string(obj.Services["web"].Ports[0].Protocol))
	assert.Equal(t, "TCP", string(obj.Services["web"].Ports[0].L4Protocol))
	assert.Equal(t, "TCP", string(obj.Services["web"].Ports[1].Protocol))
	assert.Equal(t, "TCP", string(obj.Services["web"].Ports[1].L4Protocol))
	assert.Equal(t, "UDP", string(obj.Services["web"].Ports[2].Protocol))
	assert.Equal(t, "UDP", string(obj.Services["web"].Ports[2].L4Protocol))
	assert.Equal(t, "HTTP", string(obj.Services["web"].Ports[3].Protocol))
	assert.Equal(t, "TCP", string(obj.Services["web"].Ports[3].L4Protocol))
	assert.Equal(t, "DNS", string(obj.Services["web"].Ports[4].Protocol))
	assert.Equal(t, "UDP", string(obj.Services["web"].Ports[4].L4Protocol))
	assert.Equal(t, true, obj.Services["web"].Ports[4].Default)
}

func TestDicalYmlK8SSnippet(t *testing.T) {
	d, err := New([]byte(yml), true)
	assert.Nil(t, err)
	assert.NotNil(t, d.obj.Services["web"].K8SSnippet)
	assert.NotNil(t, d.obj.Services["web"].K8SSnippet.Container.SecurityContext)
	assert.Equal(t, true, *d.obj.Services["web"].K8SSnippet.Container.SecurityContext.Privileged)
	assert.EqualValues(t, "Always", d.obj.Services["web"].K8SSnippet.Container.ImagePullPolicy)
	_, err = New([]byte(wrongSnippetYml), true)
	assert.NotNil(t, err)
}

func TestDiceYmlFieldnameValidate(t *testing.T) {
	_, err := New([]byte(yml), true)
	fmt.Printf("%+v\n", err) // debug print
	assert.Nil(t, err)
}

func TestDiceYmlInsertJobImage(t *testing.T) {
	d, err := New([]byte(jobyml), false)
	err = d.InsertImage(map[string]string{"job1": "image1"}, nil)
	assert.Nil(t, err, "%v", err)
	fmt.Printf("%+v\n", d.Obj().Jobs["job1"]) // debug print

}

//func TestDiceYmlMergeValues(t *testing.T) {
//	d, err := NewDeployable([]byte(yml), "test", true)
//	assert.Nil(t, err)
//	assert.Equal(t, 0.5, d.Obj().Services["web"].Resources.CPU)
//	assert.Equal(t, 1, d.Obj().Services["web"].Deployments.Replicas)
//	assert.Equal(t, "mysql:basic", d.Obj().AddOns["xxx"].Plan)
//	d, err = NewDeployable([]byte(yml), "prod", true)
//	assert.Nil(t, err)
//	assert.Equal(t, float64(1), d.Obj().Services["web"].Resources.CPU)
//	assert.Equal(t, 2, d.Obj().Services["web"].Deployments.Replicas)
//	assert.Equal(t, "rds:basic", d.Obj().AddOns["xxx"].Plan)
//}

func TestNew(t *testing.T) {
	for i, text := range []string{yml3, yml4, yml5} {
		testNew(t, i, text)
	}
}

func testNew(t *testing.T, i int, text string) {
	y, err := New([]byte(text), true)
	if err != nil {
		t.Fatalf("[%v] failed to New DiceYml: %v", i, err)
	}
	t.Logf("[%v] %+v, %s", i, y.obj, string(y.data))

	data, err := y.YAML()
	if err != nil {
		t.Fatalf("[%v] failed to y.YAML: %v", i, err)
	}
	t.Logf("[%v] Yaml: %s", i, data)

	data, err = y.JSON()
	if err != nil {
		t.Fatalf("[%v] failed to y.JSON: %v", i, err)
	}
	t.Logf("[%v] Json: %s", i, data)
	var dice Object
	err = json.Unmarshal([]byte(testjson), &dice)
	if err != nil {
		t.Fatalf("json unmarshal failed, err:%+v", err)
	}
	dy, err := New([]byte(testyml), true)
	if err != nil {
		t.Fatal(err)
	}
	jstr, err := json.MarshalIndent(dy.obj, "", "\t")
	if err != nil {
		t.Fatal(err)
	}
	err = json.Unmarshal(jstr, &dice)
	if err != nil {
		t.Fatal(err)
	}
	if dice.Environments["development"].Envs["OSS_ENABLE"] != "true" {
		t.Fatalf("json unmarshal error, dice:%s", jstr)
	}
}
