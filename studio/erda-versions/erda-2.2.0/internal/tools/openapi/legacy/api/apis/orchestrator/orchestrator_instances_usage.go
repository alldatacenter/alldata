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

package orchestrator

import "github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"

var ORCHESTRATOR_INSTANCES_USAGE = apis.ApiSpec{
	Path:        "/api/instances-usage",
	BackendPath: "/api/instances-usage",
	Host:        "orchestrator.marathon.l4lb.thisdcos.directory:8081",
	Scheme:      "http",
	Method:      "GET",
	CheckLogin:  true,
	Doc: `
summary: 获取某类实例集合的资源使用情况
parameters:
  - in: path
    name: cluster
    type: string
    required: true
    description: 集群名或者ID
  - in: query
    name: type
    type: string
    required: true
    enum: [cluster, host, project, application, runtime, service, component, addon]
    description: 指定请求实例的类型
    example: project
  - in: query
    name: host
    type: string
    description: type 等于 host时，若输入 host，则获取指定 host 信息；如果没有输入，则获取整个集群所有的 hosts 信息
  - in: query
    name: project
    type: string
    description: type 等于 project时，若输入 project，则获取指定 project 实例信息；如果没有输入，则获取整个集群所有的 projects 实例信息
  - in: query
    name: application
    type: string
    description: type 等于 application时，若输入 application，则获取指定 application 实例信息；如果没有输入，则获取整个集群所有的 applications 实例信息
  - in: query
    name: runtime
    type: string
    description: type 等于 runtime时，若输入 runtime，则获取指定 runtime 实例信息；如果没有输入，则获取整个集群所有的 runtimes 实例信息
  - in: query
    name: service
    type: string
    description: type 等于 service时，输入 runtime 必要参数。若输入 service，则获取指定 service 实例信息;如果没有输入，则获取指定runtime下所有的 services 实例信息
  - in: query
    name: component
    type: string
    description: type 等于 component时。若输入 component，则获取指定 component 实例信息;如果没有输入，则获取指定集群所有的 component 实例信息
  - in: query
    name: addon
    type: string
    description: type 等于 addon时。若输入 addon，则获取指定 addon 实例信息;如果没有输入，则获取指定集群所有的 addon 实例信息
produces:
  - application/json

responses:
  '200':
    description: OK
    schema:
      type: string
      example: 参考 https://yuque.antfin-inc.com/terminus_paas_dev/paas/gosn9b#va3psl
components:
  schemas:
    ComponentUsage:
      type: object
      properties:
        name:
            type: string
            description: 组件名
        instance:
            type: integer
            description: 实例数
        memory:
            type: number
            format: double
            description: 分配的内存（MB）
        cpu:
            type: number
            format: double
            description: 分配的cpu数
        disk:
            type: number
            format: double
            description: 分配的磁盘空间（MB）
    AddonUsage:
      type: object
      properties:
        name:
            type: string
            description: 组件名
        instance:
            type: integer
            description: 实例数
        memory:
            type: number
            format: double
            description: 分配的内存（MB）
        cpu:
            type: number
            format: double
            description: 分配的cpu数
        disk:
            type: number
            format: double
            description: 分配的磁盘空间（MB）
    ProjectUsage:
      type: object
      properties:
        id:
            type: string
            description: 项目ID
        name:
            type: string
            description: 项目名
        instance:
            type: integer
            description: 实例数
        memory:
            type: number
            format: double
            description: 分配的内存（MB）
        cpu:
            type: number
            format: double
            description: 分配的cpu数
        disk:
            type: number
            format: double
            description: 分配的磁盘空间（MB）
    ApplicationUsage:
      type: object
      properties:
        id:
            type: string
            description: 应用ID
        name:
            type: string
            description: 应用名
        instance:
            type: integer
            description: 实例数
        memory:
            type: number
            format: double
            description: 分配的内存（MB）
        cpu:
            type: number
            format: double
            description: 分配的cpu数
        disk:
            type: number
            format: double
            description: 分配的磁盘空间（MB）
    RuntimeUsage:
      type: object
      properties:
        id:
            type: string
            description: runtime ID
        name:
            type: string
            description: runtime名
        application:
            type: string
            description: 应用名
        instance:
            type: integer
            description: 实例数
        memory:
            type: number
            format: double
            description: 分配的内存（MB）
        cpu:
            type: number
            format: double
            description: 分配的cpu数
        disk:
            type: number
            format: double
            description: 分配的磁盘空间（MB）
    ServiceUsage:
      type: object
      properties:
        name:
            type: string
            description: service名
        runtime:
            type: string
            description: runtime名
        instance:
            type: integer
            description: 实例数
        memory:
            type: number
            format: double
            description: 分配的内存（MB）
        cpu:
            type: number
            format: double
            description: 分配的cpu数
        disk:
            type: number
            format: double
            description: 分配的磁盘空间（MB）
`,
}
