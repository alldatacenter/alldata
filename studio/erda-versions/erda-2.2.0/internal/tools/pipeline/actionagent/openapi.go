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

package actionagent

import (
	"os"
	"strconv"
	"sync"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
)

const (
	EnvDiceOpenapiPublicUrl = "DICE_OPENAPI_PUBLIC_URL"
	EnvDiceOpenapiAddr      = "DICE_OPENAPI_ADDR"
	EnvDiceIsEdge           = "DICE_IS_EDGE"
)

var getOpenAPILock sync.Once

func (agent *Agent) getOpenAPIInfo() {
	getOpenAPILock.Do(
		func() {
			pipelineAddr := os.Getenv(apistructs.EnvPipelineAddr)

			// 判断是否是边缘集群
			agent.isEdgeCluster()
			agent.isEdgePipeline()

			// 根据是否是边缘集群对 openapi 环境变量进行转换
			agent.convertEnvsByClusterLocation()
			openAPIAddr := os.Getenv(EnvDiceOpenapiAddr)
			agent.EasyUse.OpenAPIAddr = openAPIAddr
			agent.EasyUse.PipelineAddr = pipelineAddr
			if err := agent.checkCallbackAddr(); err != nil {
				agent.AppendError(err)
				return
			}
		})
}

func (agent *Agent) checkCallbackAddr() error {
	if agent.EasyUse.OpenAPIAddr == "" && !agent.EasyUse.IsEdgePipeline {
		return errors.Errorf("failed to get openapi addr, %s: %v, %s: %s",
			EnvDiceIsEdge, agent.EasyUse.IsEdgePipeline,
			EnvDiceOpenapiAddr, agent.EasyUse.OpenAPIAddr)
	}
	if agent.EasyUse.PipelineAddr == "" && agent.EasyUse.IsEdgePipeline {
		return errors.Errorf("failed to get pipeline addr, %s: %v, %s: %s",
			apistructs.EnvIsEdgePipeline, agent.EasyUse.IsEdgePipeline,
			apistructs.EnvPipelineAddr, agent.EasyUse.PipelineAddr)
	}
	return nil
}

// isEdgeCluster 是否是边缘集群
func (agent *Agent) isEdgeCluster() {
	isEdgeCluster, _ := strconv.ParseBool(os.Getenv(EnvDiceIsEdge))
	agent.EasyUse.IsEdgeCluster = isEdgeCluster
}

func (agent *Agent) isEdgePipeline() {
	isEdgePipeline, _ := strconv.ParseBool(os.Getenv(apistructs.EnvIsEdgePipeline))
	agent.EasyUse.IsEdgePipeline = isEdgePipeline
}
