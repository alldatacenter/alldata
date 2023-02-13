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

var ORCHESTRATOR_RUNTIME_CREATE = apis.ApiSpec{
	Path:        "/api/runtimes",
	BackendPath: "/api/runtimes",
	Host:        "orchestrator.marathon.l4lb.thisdcos.directory:8081",
	Scheme:      "http",
	Method:      "POST",
	CheckLogin:  false,
	Doc: `
summary: 创建 Runtime
consumes:
  - application/json
parameters:
  - in: body
    description: runtime to create
    schema:
      type: object
      properties:
        name:
          type: string
          description: Runtime Name (pipeline 请用分支名)
        releaseId:
          type: string
          description: diceHub 的 releaseId
        operator:
          type: string
          description: 操作人用户 ID
        clusterName:
          type: string
          description: 告知发布的集群 (aka "az")
        source:
          type: string
          description: PIPELINE / RUNTIMEADDON / ABILITY
        extra:
          type: object
          description: |
            格式为 key(string) / value(object):

                {
                  "k1": "v1",
                  "k2": 123,
                  "k3": ["1", "2", "3"],
                  "k4": {
                    "f1": "g1",
                    "f2": "g2"
                  }
                }

            若为 PIPELINE, 需要传
              - orgId (integer)
              - projectId (integer)
              - applicationId (integer)
              - workspace
              - buildId (integer)

            若为 RUNTIMEADDON, 需要传
              - orgId (integer)
              - projectId (integer)
              - applicationId (integer)
              - workspace
              - instanceId (string)

            若为 ABILITY, 需要传
              - orgId (integer)
              - applicationId (integer) 或 applicationName (自动创建 application)
              - workspace
              - clusterId (string)
              - addonActions (map[string]interface{})
produces:
  - application/json
responses:
  '200':
    description: ok
    schema:
      type: object
      properties:
        success:
          type: boolean
        err:
          type: object
          properties:
            code:
              type: string
            msg:
              type: string
            ctx:
              type: object
        data:
          type: object
          properties:
            deploymentId:
              type: integer
            applicationId:
              type: integer
            runtimeId:
              type: integer
  '400':
    description: bad request
`,
}
